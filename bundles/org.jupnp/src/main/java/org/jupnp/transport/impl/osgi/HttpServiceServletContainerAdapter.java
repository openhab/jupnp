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

package org.jupnp.transport.impl.osgi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jupnp.transport.spi.ServletContainerAdapter;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a servlet container adapter for an OSGi http service.
 * 
 * @author Kai Kreuzer
 * @author Michael Grammling - FIXED an issue when the servlet is registered multiple times.
 */
public class HttpServiceServletContainerAdapter implements ServletContainerAdapter {

	private final static Logger logger = LoggerFactory.getLogger(HttpServiceServletContainerAdapter.class);
	
	protected HttpService httpService;

	private List<String> contextPaths;


	public HttpServiceServletContainerAdapter(HttpService httpService) {
		this.httpService = httpService;
		this.contextPaths = new ArrayList<>(1);
	}

	@Override
	public void setExecutorService(ExecutorService executorService) {
	}

	@Override
	public int addConnector(String host, int port) throws IOException {
		return 0;
	}

	@Override
	public synchronized void registerServlet(String contextPath, Servlet servlet) {
	    if ((this.httpService != null) && (contextPath != null)
	            && (servlet != null) && (!this.contextPaths.contains(contextPath))) {

            Dictionary<?, ?> params = new Properties();
            try {
                logger.info("Registering UPnP callback servlet as {}...", contextPath);
                httpService.registerServlet(contextPath, servlet, params, null);
                this.contextPaths.add(contextPath);
            } catch (ServletException e) {
                logger.error("Failed to register UPnP servlet!", e);
            } catch (NamespaceException e) {
                logger.error("Failed to register UPnP servlet!", e);
            }
	    }
	}

	@Override
	public void startIfNotRunning() {
	}

	@Override
	public synchronized void stopIfRunning() {
	    if (this.httpService != null) {
	        for (String contextPath : this.contextPaths) {
	            try {
	                this.httpService.unregister(contextPath);
	            } catch (Exception ex) {
	                logger.error("Cannot unregister context path '" + contextPath + "'!", ex);
	            }
	        }

	        this.contextPaths.clear();
	    }
	}

}
