/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.protocol.sync;

import org.jupnp.UpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.IncomingEventRequestMessage;
import org.jupnp.model.message.gena.OutgoingEventResponseMessage;
import org.jupnp.model.resource.ServiceEventCallbackResource;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming GENA event messages.
 * <p>
 * Attempts to find an outgoing (remote) subscription matching the callback and subscription identifier.
 * Once found, the GENA event message payload will be transformed and the
 * {@link org.jupnp.model.gena.RemoteGENASubscription#receive(org.jupnp.model.types.UnsignedIntegerFourBytes,
 * java.util.Collection)} method will be called asynchronously using the executor
 * returned by {@link org.jupnp.UpnpServiceConfiguration#getRegistryListenerExecutor()}.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingEvent extends ReceivingSync<StreamRequestMessage, OutgoingEventResponseMessage> {

    final private Logger log = LoggerFactory.getLogger(ReceivingEvent.class);

    public ReceivingEvent(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected OutgoingEventResponseMessage executeSync() throws RouterException{

        if (!getInputMessage().isContentTypeTextUDA()) {
            log.warn("Received without or with invalid Content-Type: " + getInputMessage());
            // We continue despite the invalid UPnP message because we can still hope to convert the content
            // return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.UNSUPPORTED_MEDIA_TYPE));
        }

        ServiceEventCallbackResource resource =
                getUpnpService().getRegistry().getResource(
                        ServiceEventCallbackResource.class,
                        getInputMessage().getUri()
                );

        if (resource == null) {
            log.trace("No local resource found: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.NOT_FOUND));
        }

        final IncomingEventRequestMessage requestMessage =
                new IncomingEventRequestMessage(getInputMessage(), resource.getModel());

        // Error conditions UDA 1.0 section 4.2.1
        if (requestMessage.getSubscrptionId() == null) {
            log.trace("Subscription ID missing in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        if (!requestMessage.hasValidNotificationHeaders()) {
            log.trace("Missing NT and/or NTS headers in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.BAD_REQUEST));
        }

        if (!requestMessage.hasValidNotificationHeaders()) {
            log.trace("Invalid NT and/or NTS headers in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        if (requestMessage.getSequence() == null) {
            log.trace("Sequence missing in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        try {

            getUpnpService().getConfiguration().getGenaEventProcessor().readBody(requestMessage);

		} catch (final UnsupportedDataException ex) {
            log.trace("Can't read event message request body, " + ex);

            // Pass the parsing failure on to any listeners, so they can take action if necessary
            final RemoteGENASubscription subscription =
                getUpnpService().getRegistry().getRemoteSubscription(requestMessage.getSubscrptionId());
            if (subscription != null) {
                getUpnpService().getConfiguration().getRegistryListenerExecutor("upnp-receive").execute(
                    new Runnable() {
                        public void run() {
                            subscription.invalidMessage(ex);
                        }
                    }
                );
            }

            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR));
        }

        // get the remove subscription, if the subscription can't be found, wait for pending subscription
        // requests to finish
        final RemoteGENASubscription subscription =
                getUpnpService().getRegistry().getWaitRemoteSubscription(requestMessage.getSubscrptionId());

        if (subscription == null) {
            log.debug("Invalid subscription ID, no active subscription: " + requestMessage);
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        getUpnpService().getConfiguration().getRegistryListenerExecutor("upnp-receive").execute(
                new Runnable() {
                    public void run() {
                        log.trace("Calling active subscription with event state variable values");
                        subscription.receive(
                                requestMessage.getSequence(),
                                requestMessage.getStateVariableValues()
                        );
                    }
                }
        );

        return new OutgoingEventResponseMessage();

    }
}
