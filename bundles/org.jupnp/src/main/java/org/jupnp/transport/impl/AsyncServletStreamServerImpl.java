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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jupnp.model.message.Connection;
import org.jupnp.servlet.AsyncContext;
import org.jupnp.servlet.AsyncEvent;
import org.jupnp.servlet.AsyncListener;
import org.jupnp.servlet.http.AsyncHttpServlet;
import org.jupnp.servlet.http.AsyncHttpServletRequest;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on Servlet 3.0 API.
 * <p>
 * Refactor the code back to the official Servlet 3.0 API if Servlet 2.5 support should be
 * given up. There are minor changes needed <i>only</i> in that class to upgrade to the 
 * Servlet 3.0 API again.
 *
 * @author Christian Bauer
 * @author Michael Grammling - Refactored so that it can also work with Servlet 2.5 containers
 */
public class AsyncServletStreamServerImpl implements StreamServer<AsyncServletStreamServerConfigurationImpl> {

    private static final Logger log = LoggerFactory.getLogger(AsyncServletStreamServerImpl.class);

    protected final AsyncServletStreamServerConfigurationImpl configuration;
    protected int localPort;


    public AsyncServletStreamServerImpl(AsyncServletStreamServerConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public AsyncServletStreamServerConfigurationImpl getConfiguration() {
        return configuration;
    }

    public synchronized void init(InetAddress bindAddress, final Router router)
            throws InitializationException {

        try {
            log.debug("Setting executor service on servlet container adapter");
            getConfiguration().getServletContainerAdapter().setExecutorService(
                router.getConfiguration().getStreamServerExecutorService()
            );

            log.debug("Adding connector: " + bindAddress + ":" + getConfiguration().getListenPort());
            localPort = getConfiguration().getServletContainerAdapter().addConnector(
                bindAddress.getHostAddress(),
                getConfiguration().getListenPort()
            );
            String contextPath = router.getConfiguration().getNamespace().getBasePath().getPath();
            getConfiguration().getServletContainerAdapter().registerServlet(
                    contextPath, createServlet(router));
        } catch (Exception ex) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName()
                    + ": " + ex.toString(), ex);
        }
    }

    synchronized public int getPort() {
        return this.localPort;
    }

    synchronized public void stop() {
        getConfiguration().getServletContainerAdapter().stopIfRunning();
    }

    public void run() {
        getConfiguration().getServletContainerAdapter().startIfNotRunning();
    }

    private int mCounter = 0;

    protected Servlet createServlet(final Router router) {
        // (Opt.: Create only an HttpServlet if you want to use the official Servlet 3.0 API functionality).
        return new AsyncHttpServlet() {
            // Override the method AsyncHttpServlet#service(HttpServletRequest, HttpServletResponse)
            // if you want to use the official Servlet 3.0 API functionality. 
            @Override
            protected void service(AsyncHttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            	final long startTime = System.currentTimeMillis();
            	final int counter = mCounter++;
            	log.info(String.format("HttpServlet.service(): id: %3d, request URI: %s", counter, req.getRequestURI()));
                log.debug("Handling Servlet request asynchronously: " + req);

                // Use the method HttpServletRequest#startAsync() to use the official Servlet 3.0 API functionality.
                AsyncContext async = req.startAsynchronous();
                async.setTimeout(getConfiguration().getAsyncTimeoutSeconds() * 1000);

                async.addListener(new AsyncListener() {
                    @Override
                    public void onTimeout(AsyncEvent arg0) throws IOException {
                        long duration = System.currentTimeMillis() - startTime;
                        log.warn(String.format("AsyncListener.onTimeout(): id: %3d, duration: %,4d, request: %s", counter, duration, arg0.getSuppliedRequest()));
                    }

                    @Override
                    public void onStartAsync(AsyncEvent arg0) throws IOException {
                        // not needed
                        log.debug(String.format("AsyncListener.onStartAsync(): id: %3d, request: %s", counter, arg0.getSuppliedRequest()));
                    }

                    @Override
                    public void onError(AsyncEvent arg0) throws IOException {
                        long duration = System.currentTimeMillis() - startTime;
                        log.warn(String.format("AsyncListener.onError(): id: %3d, duration: %,4d, response: %s", counter, duration, arg0.getSuppliedResponse()));
                    }

                    @Override
                    public void onComplete(AsyncEvent arg0) throws IOException {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info(String.format("AsyncListener.onComplete(): id: %3d, duration: %,4d, response: %s", counter, duration, arg0.getSuppliedResponse()));
                    }
                });

                AsyncServletUpnpStream stream =
                    new AsyncServletUpnpStream(router.getProtocolFactory(), async, req) {
                        @Override
                        protected Connection createConnection() {
                            return new AsyncServletConnection(getRequest());
                        }
                    };

                router.received(stream);
            }
        };
    }

    /**
     * Override this method if you can check, at a low level, if the client connection is still open
     * for the given request. This will likely require access to proprietary APIs of your servlet
     * container to obtain the socket/channel for the given request.
     *
     * @return By default <code>true</code>.
     */
    protected boolean isConnectionOpen(HttpServletRequest request) {
        return true;
    }

    protected class AsyncServletConnection implements Connection {

        protected HttpServletRequest request;

        public AsyncServletConnection(HttpServletRequest request) {
            this.request = request;
        }

        public HttpServletRequest getRequest() {
            return request;
        }

        @Override
        public boolean isOpen() {
            return AsyncServletStreamServerImpl.this.isConnectionOpen(getRequest());
        }

        @Override
        public InetAddress getRemoteAddress() {
            try {
                return InetAddress.getByName(getRequest().getRemoteAddr());
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public InetAddress getLocalAddress() {
            try {
                return InetAddress.getByName(getRequest().getLocalAddr());
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
