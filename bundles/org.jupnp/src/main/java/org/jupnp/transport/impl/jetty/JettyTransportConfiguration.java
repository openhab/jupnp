package org.jupnp.transport.impl.jetty;

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;

/**
 * Implementation of {@link TransportConfiguration} for Jetty HTTP components.
 *
 * @author Victor Toni - initial contribution
 */
public class JettyTransportConfiguration
    implements TransportConfiguration {

    public static final TransportConfiguration INSTANCE = new JettyTransportConfiguration();

    @Override
    public StreamClient createStreamClient(final ExecutorService executorService, final StreamClientConfiguration configuration) {
        StreamClientConfigurationImpl clientConfiguration = new StreamClientConfigurationImpl(executorService);

        clientConfiguration.setRetryAfterSeconds(configuration.getRetryAfterSeconds());
        clientConfiguration.setRetryIterations(configuration.getRetryIterations());
        clientConfiguration.setTimeoutSeconds(configuration.getTimeoutSeconds());
	
        return new JettyStreamClientImpl(clientConfiguration);
    }

    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return new ServletStreamServerImpl(
                new ServletStreamServerConfigurationImpl(
                        JettyServletContainer.INSTANCE,
                        listenerPort
                )
            );
    }

}
