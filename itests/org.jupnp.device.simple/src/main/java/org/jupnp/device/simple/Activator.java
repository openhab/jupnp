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

package org.jupnp.device.simple;

import java.util.ArrayList;
import java.util.List;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.util.tracker.ServiceTracker;
import org.jupnp.device.simple.devices.BaseUPnPDevice;
import org.jupnp.device.simple.devices.SimpleTestDevice;
import org.jupnp.device.simple.model.Simple;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {
	private static Activator plugin;
	private static BundleContext context;
	private ServiceTracker tracker;
	private List<ServiceReference> references = new ArrayList<ServiceReference>();

	public static Activator getPlugin() {
		return plugin;
	}

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.plugin = this;
		Activator.context = bundleContext;
		String string = String.format("(%s=%s)",
				Constants.OBJECTCLASS , EventAdmin.class.getName()
				);
		try {
			Filter filter = context.createFilter(string);
		
			tracker = new ServiceTracker(context, filter, null);
			tracker.open();
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		
		BaseUPnPDevice simpleTestDevice = new SimpleTestDevice(new Simple());

		ServiceReference reference;
		ServiceRegistration registration;
		
		registration = context.registerService(UPnPDevice.class.getName(), simpleTestDevice, simpleTestDevice.getDescriptions(null));
		reference = registration.getReference();
		references.add(reference);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		for (ServiceReference reference : references) {
			context.ungetService(reference);
		}
		Activator.context = null;
	}

	public EventAdmin getEventAdmin() {
		return (EventAdmin) tracker.getService();
	}
}
