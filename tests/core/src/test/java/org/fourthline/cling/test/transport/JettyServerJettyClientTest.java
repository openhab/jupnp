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
package org.fourthline.cling.test.transport;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.impl.jetty.StreamClientImpl;
import org.jupnp.transport.impl.jetty.JettyServletContainer;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

import org.jupnp.test.transport.StreamServerClientTest;

/**
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - adapted to JUPnP API
 */
public class JettyServerJettyClientTest extends StreamServerClientTest {

    @Override
    public StreamServer createStreamServer(int port) {
        final ServletStreamServerConfigurationImpl configuration =
            new ServletStreamServerConfigurationImpl(
                JettyServletContainer.INSTANCE,
                port
            );

        return new ServletStreamServerImpl(configuration);
    }

    @Override
    public StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return new StreamClientImpl(
            new StreamClientConfigurationImpl(
                configuration.getSyncProtocolExecutorService(),
                3
            )
        );
    }
}
