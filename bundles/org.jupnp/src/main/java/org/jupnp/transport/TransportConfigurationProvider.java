/**
 * Copyright (C) 2017 Deutsche Telekom AG, Germany
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
package org.jupnp.transport;

import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServerConfiguration;

import org.jupnp.transport.impl.apache.ApacheTransportConfiguration;

/**
 * This is the central place to switch between transport implementations.
 * 
 * @author Victor Toni - inital contribution
 *
 */
public final class TransportConfigurationProvider {

    public static <SCC extends StreamClientConfiguration, SSC extends StreamServerConfiguration> TransportConfiguration<SCC, SSC> getDefaultTransportConfiguration() {
        final TransportConfiguration<SCC, SSC> transportConfiguration = new ApacheTransportConfiguration();

        return transportConfiguration;
    }
}
