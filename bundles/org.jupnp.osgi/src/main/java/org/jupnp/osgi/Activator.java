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

package org.jupnp.osgi;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.osgi.discover.UPnPDiscover;
import org.jupnp.osgi.present.UPnPPresent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green - initial contribution
 * @author Victor Toni - centralized transport implementation selection
 */
public class Activator implements BundleActivator {

    final private static Logger log = LoggerFactory.getLogger(Activator.class);

    private static Activator plugin;
    private BundleContext context;
    private UpnpService upnpService;
    private UPnPPresent present;
    private UPnPDiscover discover;

    public static Activator getPlugin() {
        return plugin;
    }

    public BundleContext getContext() {
        return context;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        Activator.plugin = this;
        this.context = context;

        upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration());
        discover = new UPnPDiscover(context, upnpService);
        present = new UPnPPresent(context, upnpService);
        upnpService.getControlPoint().search();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        upnpService.shutdown();
    }
}
