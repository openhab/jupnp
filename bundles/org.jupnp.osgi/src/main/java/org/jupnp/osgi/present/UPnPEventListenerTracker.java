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

package org.jupnp.osgi.present;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For debugging purposes, ServiceTracker is sufficient in a production environment.
 *
 * @author Bruce Green
 */
class UPnPEventListenerTracker extends ServiceTracker {

    private final Logger log = LoggerFactory.getLogger(UPnPEventListenerTracker.class);

    public UPnPEventListenerTracker(BundleContext context, Filter filter, ServiceTrackerCustomizer customizer) {
        super(context, filter, null);
    }

    @Override
    public Object addingService(ServiceReference reference) {
        log.trace("ENTRY {}.{}: {}", this.getClass().getName(), "addingService", reference);

        return super.addingService(reference);
    }
}
