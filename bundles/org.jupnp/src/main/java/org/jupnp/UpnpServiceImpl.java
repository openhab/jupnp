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


import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.controlpoint.ControlPointImpl;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.ProtocolFactoryImpl;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryImpl;
import org.jupnp.transport.Router;
import org.jupnp.transport.RouterException;
import org.jupnp.transport.RouterImpl;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link UpnpService}, starts immediately on construction.
 * <p>
 * If no {@link UpnpServiceConfiguration} is provided it will automatically
 * instantiate {@link DefaultUpnpServiceConfiguration}. This configuration <strong>does not
 * work</strong> on Android! Use the {@link org.jupnp.android.AndroidUpnpService}
 * application component instead
 * </p>
 * <p>
 * Override the various <tt>create...()</tt> methods to customize instantiation of protocol factory,
 * router, etc.
 * </p>
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - OSGiified the service
 */
public class UpnpServiceImpl implements UpnpService {

    private static Logger log = LoggerFactory.getLogger(UpnpServiceImpl.class);

    protected boolean isConfigured = false;    
    protected Boolean isRunning = false;
    
    protected UpnpServiceConfiguration configuration;
    protected ProtocolFactory protocolFactory;
    protected Registry registry;

    protected ControlPoint controlPoint;
    protected Router router;

    public UpnpServiceImpl() {
    	this(new DefaultUpnpServiceConfiguration());
    }

    public UpnpServiceImpl(UpnpServiceConfiguration configuration) {
        this.configuration = configuration;

        this.protocolFactory = createProtocolFactory();
    	
        this.registry = createRegistry(protocolFactory);
    }

    protected void setOSGiUpnpServiceConfiguration(OSGiUpnpServiceConfiguration configuration) {
    	this.configuration = configuration;
    	if (isRunning) {
    		restart(true);
    	}
    }

    protected void unsetOSGiUpnpServiceConfiguration(OSGiUpnpServiceConfiguration configuration) {
        if (this.configuration == configuration) {
            this.configuration = null;
        }
    }

    protected ProtocolFactory createProtocolFactory() {
        return new ProtocolFactoryImpl(this);
    }

    protected Registry createRegistry(ProtocolFactory protocolFactory) {
        return new RegistryImpl(this);
    }

    protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
        return new RouterImpl(getConfiguration(), protocolFactory);
    }

    protected ControlPoint createControlPoint(ProtocolFactory protocolFactory, Registry registry) {
        return new ControlPointImpl(getConfiguration(), protocolFactory, registry);
    }

    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    public ControlPoint getControlPoint() {
        return controlPoint;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public Registry getRegistry() {
        return registry;
    }

    public Router getRouter() {
        return router;
    }

    synchronized public void shutdown() {
        shutdown(false);
    }

    protected void shutdown(boolean separateThread) {
    	Runnable shutdown = new Runnable() {
            @Override
            public void run() {
            	synchronized (isRunning) {
            		if(isRunning) {
	                    log.info("Shutting down UPnP service...");
	                    shutdownRegistry();
	                    shutdownRouter();
	                    shutdownConfiguration();
	                    log.info("UPnP service shutdown completed");
	                    isRunning = false;
            		}
        		}
            }
        };
        if (separateThread) {
            // This is not a daemon thread, it has to complete!
            new Thread(shutdown).start();
        } else {
            shutdown.run();
        }
    }

    private void restart(boolean separateThread) {
        Runnable restart = new Runnable() {
            @Override
            public void run() {
            	shutdown();
            	startup();
            }
        };
        if (separateThread) {
            // This is not a daemon thread, it has to complete!
            new Thread(restart).start();
        } else {
        	restart.run();
        }
    }
    
    protected void shutdownRegistry() {
        getRegistry().shutdown();
    }

    protected void shutdownRouter() {
        try {
            getRouter().shutdown();
        } catch (RouterException ex) {
            Throwable cause = Exceptions.unwrap(ex);
            if (cause instanceof InterruptedException) {
                log.debug("Router shutdown was interrupted: " + ex, cause);
            } else {
                throw new RuntimeException("Router error on shutdown: " + ex, ex);
            }
        }
    }

    protected void shutdownConfiguration() {
        getConfiguration().shutdown();
    }

    public void startup() {
    	synchronized (isRunning) {
    		if(!isRunning) {
	            log.info("Starting UPnP service...");
	
	            // Instantiation order is important: Router needs to start its network services after registry is ready
	
	            log.debug("Using configuration: " + getConfiguration().getClass().getName());
	
	            this.router = createRouter(protocolFactory, registry);
	
	            try {
	                this.router.enable();
	            } catch (RouterException ex) {
	                throw new RuntimeException("Enabling network router failed: " + ex, ex);
	            }
	
	            this.controlPoint = createControlPoint(protocolFactory, registry);
	
	            log.debug("UPnP service started successfully");

	            isRunning = true;

	            controlPoint.search(new STAllHeader());
    		}
		}
    }

    protected void activate() {
    	startup();
    }

    protected void deactivate() {
    	shutdown();
    }

}
