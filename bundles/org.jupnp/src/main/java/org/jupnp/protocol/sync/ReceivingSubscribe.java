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

import java.net.URL;
import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.IncomingSubscribeRequestMessage;
import org.jupnp.model.message.gena.OutgoingSubscribeResponseMessage;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.resource.ServiceEventSubscriptionResource;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.RouterException;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of GENA event subscription (initial and renewal) messages.
 * <p>
 * This protocol tries to find a local event subscription URI matching the requested URI,
 * then creates a new {@link org.jupnp.model.gena.LocalGENASubscription} if no
 * subscription identifer was supplied.
 * </p>
 * <p>
 * The subscription is however only registered with the local service, and monitoring
 * of state changes is established, if the response of this protocol was successfully
 * delivered to the client which requested the subscription.
 * </p>
 * <p>
 * Once registration and monitoring is active, an initial event with the current
 * state of the service is send to the subscriber. This will only happen after the
 * subscription response message was successfully delivered to the subscriber.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingSubscribe extends ReceivingSync<StreamRequestMessage, OutgoingSubscribeResponseMessage> {

    final private Logger log = LoggerFactory.getLogger(ReceivingSubscribe.class);

    protected LocalGENASubscription subscription;

    public ReceivingSubscribe(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected OutgoingSubscribeResponseMessage executeSync() throws RouterException {

        ServiceEventSubscriptionResource resource =
                getUpnpService().getRegistry().getResource(
                        ServiceEventSubscriptionResource.class,
                        getInputMessage().getUri()
        );

        if (resource == null) {
            log.trace("No local resource found: " + getInputMessage());
            return null;
        }

        log.trace("Found local event subscription matching relative request URI: " + getInputMessage().getUri());

        IncomingSubscribeRequestMessage requestMessage =
                new IncomingSubscribeRequestMessage(getInputMessage(), resource.getModel());

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (requestMessage.getSubscriptionId() != null &&
                (requestMessage.hasNotificationHeader() || requestMessage.getCallbackURLs() != null)) {
            log.trace("Subscription ID and NT or Callback in subscribe request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.BAD_REQUEST);
        }

        if (requestMessage.getSubscriptionId() != null) {
            return processRenewal(resource.getModel(), requestMessage);
        } else if (requestMessage.hasNotificationHeader() && requestMessage.getCallbackURLs() != null){
            return processNewSubscription(resource.getModel(), requestMessage);
        } else {
            log.trace("No subscription ID, no NT or Callback, neither subscription or renewal: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

    }

    protected OutgoingSubscribeResponseMessage processRenewal(LocalService service,
                                                              IncomingSubscribeRequestMessage requestMessage) {

        subscription = getUpnpService().getRegistry().getLocalSubscription(requestMessage.getSubscriptionId());

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (subscription == null) {
            log.trace("Invalid subscription ID for renewal request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        log.trace("Renewing subscription: " + subscription);
        subscription.setSubscriptionDuration(requestMessage.getRequestedTimeoutSeconds());
        if (getUpnpService().getRegistry().updateLocalSubscription(subscription)) {
            return new OutgoingSubscribeResponseMessage(subscription);
        } else {
            log.trace("Subscription went away before it could be renewed: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }
    }

    protected OutgoingSubscribeResponseMessage processNewSubscription(LocalService service,
                                                                      IncomingSubscribeRequestMessage requestMessage) {
        List<URL> callbackURLs = requestMessage.getCallbackURLs();

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (callbackURLs == null || callbackURLs.size() == 0) {
            log.trace("Missing or invalid Callback URLs in subscribe request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        if (!requestMessage.hasNotificationHeader()) {
            log.trace("Missing or invalid NT header in subscribe request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        Integer timeoutSeconds; 
        if(getUpnpService().getConfiguration().isReceivedSubscriptionTimeoutIgnored()) {
        	timeoutSeconds = null; // Use default value
        } else {
        	timeoutSeconds = requestMessage.getRequestedTimeoutSeconds();
        }
        
        try {
            subscription = new LocalGENASubscription(service, timeoutSeconds, callbackURLs) {
                public void established() {
                }

                public void ended(CancelReason reason) {
                }

                public void eventReceived() {
                    // The only thing we are interested in, sending an event when the state changes
                    getUpnpService().getConfiguration().getSyncProtocolExecutorService("upnp-subscribe").execute(
                            getUpnpService().getProtocolFactory().createSendingEvent(this)
                    );
                }
            };
        } catch (Exception ex) {
            log.warn("Couldn't create local subscription to service: " + Exceptions.unwrap(ex));
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }

        log.trace("Adding subscription to registry: " + subscription);
        getUpnpService().getRegistry().addLocalSubscription(subscription);

        log.trace("Returning subscription response, waiting to send initial event");
        return new OutgoingSubscribeResponseMessage(subscription);
    }

    @Override
    public void responseSent(StreamResponseMessage responseMessage) {
        if (subscription == null) return; // Preconditions failed very early on
        if (responseMessage != null
                && !responseMessage.getOperation().isFailed()
                && subscription.getCurrentSequence().getValue() == 0) { // Note that renewals should not have 0

            // This is a minor concurrency issue: If we now register on the service and henceforth send a new
            // event message whenever the state of the service changes, there is still a chance that the initial
            // event message arrives later than the first on-change event message. Shouldn't be a problem as the
            // subscriber is supposed to figure out what to do with out-of-sequence messages. I would be
            // surprised though if actual implementations won't crash!
            log.trace("Establishing subscription");
            subscription.registerOnService();
            subscription.establish();

            log.trace("Response to subscription sent successfully, now sending initial event asynchronously");
            getUpnpService().getConfiguration().getAsyncProtocolExecutor("upnp-subscribe").execute(
                    getUpnpService().getProtocolFactory().createSendingEvent(subscription)
            );

        } else if (subscription.getCurrentSequence().getValue() == 0) {
            log.trace("Subscription request's response aborted, not sending initial event");
            if (responseMessage == null) {
                log.trace("Reason: No response at all from subscriber");
            } else {
                log.trace("Reason: " + responseMessage.getOperation());
            }
            log.trace("Removing subscription from registry: " + subscription);
            getUpnpService().getRegistry().removeLocalSubscription(subscription);
        }
    }

    @Override
    public void responseException(Throwable t) {
        if (subscription == null) return; // Nothing to do, we didn't get that far
        log.trace("Response could not be send to subscriber, removing local GENA subscription: " + subscription);
        getUpnpService().getRegistry().removeLocalSubscription(subscription);
    }
}
