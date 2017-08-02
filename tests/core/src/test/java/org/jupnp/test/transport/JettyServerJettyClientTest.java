/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jupnp.test.transport;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.transport.impl.jetty.JettyTransportConfiguration;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Testing interaction of Jetty server with Jetty client.
 * 
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - adapted to JUPnP API
 * @author Victor Toni - changed to use TransportConfiguration
 */
public class JettyServerJettyClientTest extends StreamServerClientTest {
    @SuppressWarnings("rawtypes")
    private final TransportConfiguration jettyConfiguration = new JettyTransportConfiguration();

    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return jettyConfiguration.createStreamServer(listenerPort);
    }

    @Override
    public StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return jettyConfiguration.createStreamClient(configuration.getSyncProtocolExecutorService());
    }
}
