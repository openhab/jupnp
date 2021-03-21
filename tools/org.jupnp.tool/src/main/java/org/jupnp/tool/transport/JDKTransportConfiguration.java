package org.jupnp.tool.transport;

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Implementation of {@link TransportConfiguration} for Jetty HTTP components.
 *
 * @author Victor Toni - initial contribution
 */
public class JDKTransportConfiguration
    implements TransportConfiguration {

    public static final TransportConfiguration INSTANCE = new JDKTransportConfiguration();

    @Override
    public StreamClient createStreamClient(final ExecutorService executorService) {
        return new StreamClientImpl(
                new StreamClientConfigurationImpl(
                        executorService
                )
        );
    }

    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return new StreamServerImpl(
                new StreamServerConfigurationImpl(listenerPort)
        );
    }

}
