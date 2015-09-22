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

package org.jupnp;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.RecoveringUDA10ServiceDescriptorBinderSAXImpl;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.model.ModelUtil;
import org.jupnp.model.Namespace;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceType;
import org.jupnp.transport.impl.DatagramIOConfigurationImpl;
import org.jupnp.transport.impl.DatagramIOImpl;
import org.jupnp.transport.impl.DatagramProcessorImpl;
import org.jupnp.transport.impl.GENAEventProcessorImpl;
import org.jupnp.transport.impl.MulticastReceiverConfigurationImpl;
import org.jupnp.transport.impl.MulticastReceiverImpl;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.impl.apache.StreamClientConfigurationImpl;
import org.jupnp.transport.impl.apache.StreamClientImpl;
import org.jupnp.transport.impl.apache.StreamServerConfigurationImpl;
import org.jupnp.transport.impl.apache.StreamServerImpl;
import org.jupnp.transport.impl.osgi.HttpServiceServletContainerAdapter;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.util.Exceptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default configuration data of a typical UPnP stack.
 * <p>
 * This configuration utilizes the default network transport implementation found in {@link org.jupnp.transport.impl}.
 * </p>
 * <p>
 * This configuration utilizes the DOM default descriptor binders found in {@link org.jupnp.binding.xml}.
 * </p>
 * <p>
 * The thread <code>Executor</code> is an <code>Executors.newCachedThreadPool()</code> with a custom
 * {@link JUPnPThreadFactory} (it only sets a thread name).
 * </p>
 * <p>
 * Note that this pool is effectively unlimited, so the number of threads will grow (and shrink) as needed - or
 * restricted by your JVM.
 * </p>
 * <p>
 * The default {@link org.jupnp.model.Namespace} is configured without any base path or prefix.
 * </p>
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - introduced bounded thread pool and http service streaming server
 */
public class OSGiUpnpServiceConfiguration implements UpnpServiceConfiguration {

    private Logger log = LoggerFactory.getLogger(OSGiUpnpServiceConfiguration.class);

    // configurable properties
    private int threadPoolSize = 20;
    private int threadQueueSize = 1000;
    private int multicastResponsePort;
    private int httpProxyPort = -1;
    private int streamListenPort;
    private Namespace callbackURI = new Namespace("http://localhost/upnpcallback");

    private ExecutorService defaultExecutorService;

    private DatagramProcessor datagramProcessor;
    private SOAPActionProcessor soapActionProcessor;
    private GENAEventProcessor genaEventProcessor;

    private DeviceDescriptorBinder deviceDescriptorBinderUDA10;
    private ServiceDescriptorBinder serviceDescriptorBinderUDA10;

    private Namespace namespace;

    private BundleContext context;

    @SuppressWarnings("rawtypes")
    private ServiceRegistration serviceReg;

    @SuppressWarnings("rawtypes")
    private ServiceReference httpServiceReference;

    /**
     * Defaults to port '0', ephemeral.
     */
    public OSGiUpnpServiceConfiguration() {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT);
    }

    public OSGiUpnpServiceConfiguration(int streamListenPort) {
        this(streamListenPort, NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT, true);
    }

    public OSGiUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort) {
        this(streamListenPort, multicastResponsePort, true);
    }

    protected OSGiUpnpServiceConfiguration(boolean checkRuntime) {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT,
                NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT, checkRuntime);
    }

    protected OSGiUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort, boolean checkRuntime) {
        if (checkRuntime && ModelUtil.ANDROID_RUNTIME) {
            throw new Error("Unsupported runtime environment, use org.jupnp.android.AndroidUpnpServiceConfiguration");
        }

        this.streamListenPort = streamListenPort;
        this.multicastResponsePort = multicastResponsePort;

    }

    protected void activate(BundleContext context, Map<String, Object> configProps) throws ConfigurationException {

        this.context = context;

        createConfiguration(configProps);

        defaultExecutorService = createDefaultExecutorService();

        datagramProcessor = createDatagramProcessor();
        soapActionProcessor = createSOAPActionProcessor();
        genaEventProcessor = createGENAEventProcessor();

        deviceDescriptorBinderUDA10 = createDeviceDescriptorBinderUDA10();
        serviceDescriptorBinderUDA10 = createServiceDescriptorBinderUDA10();

        namespace = createNamespace();
    }

    protected void deactivate() {
        if (serviceReg != null) {
            serviceReg.unregister();
        }

        if (httpServiceReference != null) {
            context.ungetService(httpServiceReference);
        }

        shutdown();
    }

    @Override
    public DatagramProcessor getDatagramProcessor() {
        return datagramProcessor;
    }

    @Override
    public SOAPActionProcessor getSoapActionProcessor() {
        return soapActionProcessor;
    }

    @Override
    public GENAEventProcessor getGenaEventProcessor() {
        return genaEventProcessor;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StreamClient createStreamClient() {
        return new StreamClientImpl(new StreamClientConfigurationImpl(getSyncProtocolExecutorService()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl(new MulticastReceiverConfigurationImpl(
                networkAddressFactory.getMulticastGroup(), networkAddressFactory.getMulticastPort()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
        return new DatagramIOImpl(new DatagramIOConfigurationImpl());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        ServiceReference serviceReference = context.getServiceReference(HttpService.class.getName());

        if (serviceReference != null) {

            if (httpServiceReference != null) {
                context.ungetService(httpServiceReference);
            }

            httpServiceReference = serviceReference;

            HttpService httpService = (HttpService) context.getService(serviceReference);

            if (httpService != null) {
                return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(
                        HttpServiceServletContainerAdapter.getInstance(httpService, context),
                        httpProxyPort != -1 ? httpProxyPort : callbackURI.getBasePath().getPort()));
            }
        }

        return new StreamServerImpl(new StreamServerConfigurationImpl());

    }

    @Override
    public ExecutorService getMulticastReceiverExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public ExecutorService getDatagramIOExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public ExecutorService getStreamServerExecutorService() {
        return getDefaultExecutorService();
    }

    @Override
    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
        return deviceDescriptorBinderUDA10;
    }

    @Override
    public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
        return serviceDescriptorBinderUDA10;
    }

    @Override
    public ServiceType[] getExclusiveServiceTypes() {
        return new ServiceType[0];
    }

    /**
     * @return Defaults to <code>false</code>.
     */
    @Override
    public boolean isReceivedSubscriptionTimeoutIgnored() {
        return false;
    }

    @Override
    public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity) {
        return null;
    }

    @Override
    public UpnpHeaders getEventSubscriptionHeaders(RemoteService service) {
        return null;
    }

    /**
     * @return Defaults to 1000 milliseconds.
     */
    @Override
    public int getRegistryMaintenanceIntervalMillis() {
        return 1000;
    }

    /**
     * @return Defaults to zero, disabling ALIVE flooding.
     */
    @Override
    public int getAliveIntervalMillis() {
        return 0;
    }

    @Override
    public Integer getRemoteDeviceMaxAgeSeconds() {
        return null;
    }

    @Override
    public ExecutorService getAsyncProtocolExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public ExecutorService getSyncProtocolExecutorService() {
        return getDefaultExecutorService();
    }

    @Override
    public Namespace getNamespace() {
        return namespace;
    }

    @Override
    public Executor getRegistryMaintainerExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public Executor getRegistryListenerExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public NetworkAddressFactory createNetworkAddressFactory() {
        return createNetworkAddressFactory(streamListenPort, multicastResponsePort);
    }

    @Override
    public void shutdown() {
        if (getDefaultExecutorService() != null) {
            log.debug("Shutting down default executor service");
            getDefaultExecutorService().shutdownNow();

            // create the executor again ready for reuse in case the runtime is started up again.
            defaultExecutorService = createDefaultExecutorService();
        }
    }

    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort, int multicastResponsePort) {
        return new NetworkAddressFactoryImpl(streamListenPort, multicastResponsePort);
    }

    protected DatagramProcessor createDatagramProcessor() {
        return new DatagramProcessorImpl();
    }

    protected SOAPActionProcessor createSOAPActionProcessor() {
        return new SOAPActionProcessorImpl();
    }

    protected GENAEventProcessor createGENAEventProcessor() {
        return new GENAEventProcessorImpl();
    }

    protected DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {
        return new RecoveringUDA10DeviceDescriptorBinderImpl();
    }

    protected ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
        return new RecoveringUDA10ServiceDescriptorBinderSAXImpl();
    }

    protected Namespace createNamespace() {
        return callbackURI;
    }

    protected ExecutorService getDefaultExecutorService() {
        return defaultExecutorService;
    }

    protected ExecutorService createDefaultExecutorService() {
        return new JUPnPExecutor();
    }

    public class JUPnPExecutor extends ThreadPoolExecutor {

        public JUPnPExecutor() {
            this(new JUPnPThreadFactory(), new ThreadPoolExecutor.DiscardPolicy() {
                // The pool is bounded and rejections will happen during shutdown
                @Override
                public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                    // Log and discard
                    log.warn("Thread pool rejected execution of " + runnable.getClass());
                    super.rejectedExecution(runnable, threadPoolExecutor);
                }
            });
        }

        public JUPnPExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
            // This is the same as Executors.newCachedThreadPool
            super(threadPoolSize, threadPoolSize, 10L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(threadQueueSize), threadFactory, rejectedHandler);
            allowCoreThreadTimeOut(true);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            if (throwable != null) {
                Throwable cause = Exceptions.unwrap(throwable);
                if (cause instanceof InterruptedException) {
                    // Ignore this, might happen when we shutdownNow() the executor. We can't
                    // log at this point as the logging system might be stopped already (e.g.
                    // if it's a CDI component).
                    return;
                }
                // Log only
                log.warn("Thread terminated " + runnable + " abruptly with exception: " + throwable);
                log.warn("Root cause: " + cause);
            }
        }

        @Override
        public void shutdown() {
            super.shutdown();
        }
    }

    // Executors.DefaultThreadFactory is package visibility (...no touching, you unworthy JDK user!)
    public static class JUPnPThreadFactory implements ThreadFactory {

        protected final ThreadGroup group;
        protected final AtomicInteger threadNumber = new AtomicInteger(1);
        protected final String namePrefix = "jupnp-";

        public JUPnPThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }

    private void createConfiguration(Map<String, Object> properties) throws ConfigurationException {
        if (properties == null)
            return;

        Object prop = properties.get("threadPoolSize");
        if (prop instanceof String) {
            try {
                threadPoolSize = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                log.error("Invalid value '{}' for threadPoolSize - using default value '{}'", prop, threadPoolSize);
            }
        } else if (prop instanceof Integer) {
            threadPoolSize = (Integer) prop;
        }

        prop = properties.get("threadQueueSize");
        if (prop instanceof String) {
            try {
                threadQueueSize = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                log.error("Invalid value '{}' for threadQueueSize - using default value '{}'", prop, threadQueueSize);
            }
        } else if (prop instanceof Integer) {
            threadQueueSize = (Integer) prop;
        }

        prop = properties.get("multicastResponsePort");
        if (prop instanceof String) {
            try {
                multicastResponsePort = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                log.error("Invalid value '{}' for multicastResponsePort - using default value '{}'", prop,
                        multicastResponsePort);
            }
        } else if (prop instanceof Integer) {
            multicastResponsePort = (Integer) prop;
        }

        prop = properties.get("streamListenPort");
        if (prop instanceof String) {
            try {
                streamListenPort = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                log.error("Invalid value '{}' for streamListenPort - using default value '{}'", prop, streamListenPort);
            }
        } else if (prop instanceof Integer) {
            streamListenPort = (Integer) prop;
        }

        prop = properties.get("callbackURI");
        if (prop instanceof String) {
            try {
                callbackURI = new Namespace((String) prop);
            } catch (Exception e) {
                log.error("Invalid value '{}' for callbackURI - using default value '{}'", prop, callbackURI);
            }
        }

        prop = properties.get("httpProxyPort");
        if (prop instanceof String) {
            try {
                httpProxyPort = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                log.error("Invalid value '{}' for httpProxyPort - using default value '{}'", prop, httpProxyPort);
            }
        } else if (prop instanceof Integer) {
            httpProxyPort = (Integer) prop;
        }
    }

}
