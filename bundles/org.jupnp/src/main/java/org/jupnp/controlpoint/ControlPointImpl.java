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

package org.jupnp.controlpoint;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.controlpoint.event.ExecuteAction;
import org.jupnp.controlpoint.event.Search;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation.
 * <p>
 * This implementation uses the executor returned by
 * {@link org.jupnp.UpnpServiceConfiguration#getSyncProtocolExecutorService()}.
 * </p>
 *
 * @author Christian Bauer
 */
public class ControlPointImpl implements ControlPoint {

    private Logger log = LoggerFactory.getLogger(ControlPointImpl.class);

    protected UpnpServiceConfiguration configuration;
    protected ProtocolFactory protocolFactory;
    protected Registry registry;

    protected ControlPointImpl() {
    }

    public ControlPointImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory, Registry registry) {
        log.trace("Creating ControlPoint: " + getClass().getName());
        
        this.configuration = configuration;
        this.protocolFactory = protocolFactory;
        this.registry = registry;
    }

    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void search(Search search) {
        search(search.getSearchType(), search.getMxSeconds());
    }

    public void search() {
        search(new STAllHeader(), MXHeader.DEFAULT_VALUE);
    }

    public void search(UpnpHeader searchType) {
        search(searchType, MXHeader.DEFAULT_VALUE);
    }

    public void search(int mxSeconds) {
        search(new STAllHeader(), mxSeconds);
    }

    public void search(UpnpHeader searchType, int mxSeconds) {
        log.trace("Sending asynchronous search for: " + searchType.getString());
        getConfiguration().getAsyncProtocolExecutor("upnp-control").execute(
                getProtocolFactory().createSendingSearch(searchType, mxSeconds)
        );
    }

    public void execute(ExecuteAction executeAction) {
        execute(executeAction.getCallback());
    }

    public Future execute(ActionCallback callback) {
        log.trace("Invoking action in background: " + callback);
        callback.setControlPoint(this);
        ExecutorService executor = getConfiguration().getSyncProtocolExecutorService("upnp-control");
        return executor.submit(callback);
    }

    public void execute(SubscriptionCallback callback) {
        log.trace("Invoking subscription in background: " + callback);
        callback.setControlPoint(this);
        getConfiguration().getSyncProtocolExecutorService("upnp-control").execute(callback);
    }
}
