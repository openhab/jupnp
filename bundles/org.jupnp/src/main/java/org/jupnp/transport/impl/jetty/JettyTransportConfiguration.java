package org.jupnp.transport.impl.jetty;

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;


/**
 * Implementation of {@link TransportConfiguration} for Jetty HTTP library.
 * 
 * @author Victor Toni - initial contribution
 */
public class JettyTransportConfiguration
        implements TransportConfiguration {

    @Override
    public StreamClient createStreamClient(final ExecutorService executorService) {
        return new StreamClientImpl(new StreamClientConfigurationImpl(executorService));
    }

    @Override
    public StreamServer createStreamServer(final int listenPort) {
        final ServletStreamServerConfigurationImpl configuration = new ServletStreamServerConfigurationImpl(
                JettyServletContainer.INSTANCE, listenPort);

        return new ServletStreamServerImpl(configuration);
    }

}
