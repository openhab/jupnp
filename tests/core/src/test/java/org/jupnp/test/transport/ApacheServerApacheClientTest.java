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

package org.jupnp.test.transport;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.apache.ApacheTransportConfiguration;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Testing interaction of Apache server with Apache client.
 * 
 * @author Christian Bauer
 * @author Victor Toni - changed to use TransportConfiguration
 * 
 */
public class ApacheServerApacheClientTest extends StreamServerClientTest {
    @SuppressWarnings("rawtypes")
    private final TransportConfiguration apacheConfiguration = new ApacheTransportConfiguration();

    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return apacheConfiguration.createStreamServer(listenerPort);
    }

    @Override
    public StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return apacheConfiguration.createStreamClient(configuration.getSyncProtocolExecutorService());
    }
}
