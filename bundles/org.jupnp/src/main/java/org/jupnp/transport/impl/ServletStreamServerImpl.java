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

package org.jupnp.transport.impl;

import java.net.InetAddress;

import javax.servlet.Servlet;

import org.jupnp.transport.Router;
import org.jupnp.transport.impl.async.AsyncServlet;
import org.jupnp.transport.impl.async.AsyncUtil;
import org.jupnp.transport.impl.blocking.BlockingServlet;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet stream server implementation.
 *
 * @author Christian Bauer - Initial contribution to work with Servlet 3.0
 * @author Ivan Iliev - Added support for runtime switch to Servlet 2.4
 */
public class ServletStreamServerImpl implements StreamServer<ServletStreamServerConfigurationImpl> {

    private final Logger log = LoggerFactory.getLogger(ServletStreamServerImpl.class);

    protected final ServletStreamServerConfigurationImpl configuration;
    protected int localPort;

    public ServletStreamServerImpl(ServletStreamServerConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServletStreamServerConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized void init(InetAddress bindAddress, final Router router) throws InitializationException {
        try {
            log.debug("Setting executor service on servlet container adapter");
            getConfiguration().getServletContainerAdapter().setExecutorService(
                    router.getConfiguration().getStreamServerExecutorService("upnp-stream"));

            log.debug("Adding connector: " + bindAddress + ":" + getConfiguration().getListenPort());
            localPort = getConfiguration().getServletContainerAdapter().addConnector(bindAddress.getHostAddress(),
                    getConfiguration().getListenPort());

            String contextPath = router.getConfiguration().getNamespace().getBasePath().getPath();

            // Instantiate async or blocking servlet depending on javax.servlet runtime version
            Servlet servlet = null;
            if (AsyncUtil.SERVLET3_SUPPORT) {
                servlet = createAsyncServlet(router);
            } else {
                servlet = createBlockingServlet(router);
            }

            getConfiguration().getServletContainerAdapter().registerServlet(contextPath, servlet);
        } catch (Exception ex) {
            throw new InitializationException(
                    "Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
        }
    }

    @Override
    public synchronized int getPort() {
        return this.localPort;
    }

    @Override
    public synchronized void stop() {
        getConfiguration().getServletContainerAdapter().stopIfRunning();
    }

    @Override
    public void run() {
        getConfiguration().getServletContainerAdapter().startIfNotRunning();
    }

    protected Servlet createAsyncServlet(final Router router) {
        return new AsyncServlet(router, getConfiguration());
    }

    protected Servlet createBlockingServlet(final Router router) {
        return new BlockingServlet(router, getConfiguration());

    }
}
