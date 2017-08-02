/*
 * Copyright (C) 2017 Deutsche Telekom AG, Germany
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
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.apache.ApacheTransportConfiguration;
import org.jupnp.transport.impl.jetty.JettyTransportConfiguration;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Testing interaction of Apache server with Jetty client.
 * 
 * @author Victor Toni - initial contribution
 * @author Victor Toni - changed to use TransportConfiguration
 * 
 */
public class ApacheServerJettyClientTest extends StreamServerClientTest {
    @SuppressWarnings("rawtypes")
    private final TransportConfiguration apacheConfiguration = new ApacheTransportConfiguration();

    @SuppressWarnings("rawtypes")
    private final TransportConfiguration jettyConfiguration = new JettyTransportConfiguration();
    
    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return apacheConfiguration.createStreamServer(listenerPort);
    }

    @Override
    public StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return jettyConfiguration.createStreamClient(configuration.getSyncProtocolExecutorService());
    }
}
