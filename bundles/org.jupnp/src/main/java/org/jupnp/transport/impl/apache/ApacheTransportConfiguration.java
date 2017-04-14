package org.jupnp.transport.impl.apache;

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Implementation of {@link TransportConfiguration} for Apache HTTP components.
 * 
 * @author Victor Toni - initial contribution
 */
public class ApacheTransportConfiguration 
    implements TransportConfiguration {

    @Override
    public StreamClient createStreamClient(final ExecutorService executorService) {
        return new StreamClientImpl(
            new StreamClientConfigurationImpl(executorService)
        );
    }

    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return new StreamServerImpl(
            new StreamServerConfigurationImpl(listenerPort)
        );
    }

}
