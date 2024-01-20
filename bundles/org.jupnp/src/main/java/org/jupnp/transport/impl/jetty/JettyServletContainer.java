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

package org.jupnp.transport.impl.jetty;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jupnp.transport.spi.ServletContainerAdapter;

/**
 * A singleton wrapper of a <code>org.eclipse.jetty.server.Server</code>.
 * <p>
 * This {@link org.fourthline.cling.transport.spi.ServletContainerAdapter} starts
 * a Jetty 9.3.x instance on its own and stops it. Only one single context and servlet
 * is registered, to handle UPnP requests.
 * </p>
 * <p>
 * This implementation might work on Android (not tested within JUPnP), dependencies are <code>jetty-server</code>
 * and <code>jetty-servlet</code> Maven modules.
 * </p>
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - refactoring for JUPnP
 */
public class JettyServletContainer implements ServletContainerAdapter {

    private Logger log = LoggerFactory.getLogger(JettyServletContainer.class.getName());

    // Singleton
    public static final JettyServletContainer INSTANCE = new JettyServletContainer();

    protected Server server;

    private JettyServletContainer() {
        resetServer();
    }

    @Override
    public synchronized void setExecutorService(ExecutorService executorService) {
        // the Jetty server has its own QueuedThreadPool
    }

    @Override
    public synchronized int addConnector(String host, int port) throws IOException {
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);
        return port;
    }

    @Override
    public synchronized void registerServlet(String contextPath, Servlet servlet) {
        if (server.getHandler() != null) {
            log.trace("Server handler is already set: {}", server.getHandler() );
            return;
        }
        log.info("Registering UPnP servlet under context path: {}", contextPath);
        ServletContextHandler servletHandler =
            new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        if (contextPath != null && contextPath.length() > 0) {
            servletHandler.setContextPath(contextPath);
        }
        final ServletHolder s = new ServletHolder(servlet);
        servletHandler.addServlet(s, "/*");
        server.setHandler(servletHandler);
    }

    @Override
    public synchronized void startIfNotRunning() {
        if (!server.isStarted() && !server.isStarting()) {
            log.info("Starting Jetty server... ");
            try {
                server.start();
            } catch (Exception ex) {
                log.error("Couldn't start Jetty server", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public synchronized void stopIfRunning() {
        if (!server.isStopped() && !server.isStopping()) {
            log.info("Stopping Jetty server...");
            try {
                server.stop();
            } catch (Exception ex) {
                log.error("Couldn't stop Jetty server", ex);
                throw new RuntimeException(ex);
            } finally {
                resetServer();
            }
        }
    }

    protected void resetServer() {
        server = new Server(); // Has its own QueuedThreadPool
    }

}
