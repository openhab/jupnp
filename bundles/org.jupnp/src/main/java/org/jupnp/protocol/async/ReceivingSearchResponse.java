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

package org.jupnp.protocol.async;

import java.util.logging.Logger;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.discovery.IncomingSearchResponse;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.ReceivingAsync;
import org.jupnp.protocol.RetrieveRemoteDescriptors;
import org.jupnp.transport.RouterException;

/**
 * Handles reception of search response messages.
 * <p>
 * This protocol implementation is basically the same as
 * the {@link org.jupnp.protocol.async.ReceivingNotification} protocol for
 * an <em>ALIVE</em> message.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingSearchResponse extends ReceivingAsync<IncomingSearchResponse> {

    final private static Logger log = Logger.getLogger(ReceivingSearchResponse.class.getName());

    public ReceivingSearchResponse(UpnpService upnpService, IncomingDatagramMessage<UpnpResponse> inputMessage) {
        super(upnpService, new IncomingSearchResponse(inputMessage));
    }

    protected void execute() throws RouterException {
        if (!getInputMessage().isSearchResponseMessage()) {
            log.fine("Ignoring invalid search response message: " + getInputMessage());
            return;
        }

        UDN udn = getInputMessage().getRootDeviceUDN();
        if (udn == null) {
            log.fine("Ignoring search response message without UDN: " + getInputMessage());
            return;
        }

        RemoteDeviceIdentity rdIdentity = new RemoteDeviceIdentity(getInputMessage());
        log.fine("Received device search response: " + rdIdentity);

        if (getUpnpService().getRegistry().update(rdIdentity)) {
            log.fine("Remote device was already known: " + udn);
            return;
        }

        RemoteDevice rd;
        try {
            rd = new RemoteDevice(rdIdentity);
        } catch (ValidationException ex) {
            log.warning("Validation errors of device during discovery: " + rdIdentity);
            for (ValidationError validationError : ex.getErrors()) {
                log.warning(validationError.toString());
            }
            return;
        }

        if (rdIdentity.getDescriptorURL() == null) {
            log.finer("Ignoring message without location URL header: " + getInputMessage());
            return;
        }

        if (rdIdentity.getMaxAgeSeconds() == null) {
            log.finer("Ignoring message without max-age header: " + getInputMessage());
            return;
        }

        // Unfortunately, we always have to retrieve the descriptor because at this point we
        // have no idea if it's a root or embedded device
        UpnpService upnpService = getUpnpService();
        if (upnpService != null) {
            UpnpServiceConfiguration configuration = upnpService.getConfiguration();
            if (configuration != null) {
                configuration.getAsyncProtocolExecutor().execute(
                        new RetrieveRemoteDescriptors(getUpnpService(), rd));
            } else {
                log.finer("The configuration for the async protocol executor is null.");
            }
        } else {
            log.finer("The UPnP service is null.");
        }
    }

}
