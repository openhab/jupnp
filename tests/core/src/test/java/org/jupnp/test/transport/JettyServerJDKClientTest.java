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
import org.jupnp.transport.impl.JDKTransportConfiguration;
import org.jupnp.transport.impl.StreamClientConfigurationImpl;
import org.jupnp.transport.impl.StreamClientImpl;
import org.jupnp.transport.impl.jetty.JettyTransportConfiguration;
import org.jupnp.transport.spi.StreamClient;

/**
 * @author Christian Bauer
 */
public class JettyServerJDKClientTest extends JettyServerJettyClientTest {

    private TransportConfiguration jdkTransportConfiguration = JDKTransportConfiguration.INSTANCE;

    @Override
    public StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return jdkTransportConfiguration.createStreamClient(
                configuration.getSyncProtocolExecutorService()
        );
    }

    // DISABLED, NOT SUPPORTED

    @Override
    public void cancelled() throws Exception {
    }

    @Override
    public void checkAlive() throws Exception {
    }

    @Override
    public void checkAliveExpired() throws Exception {
    }

    @Override
    public void checkAliveCancelled() throws Exception {
    }
}
