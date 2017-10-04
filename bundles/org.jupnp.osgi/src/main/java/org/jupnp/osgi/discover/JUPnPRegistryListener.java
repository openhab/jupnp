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

package org.jupnp.osgi.discover;

import java.util.Hashtable;
import java.util.Map;

import org.jupnp.UpnpService;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.osgi.impl.UPnPDeviceImpl;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors and handles the addition and removal of remote devices.
 * <p>
 * When a device is added:
 * </p>
 * <ul>
 * <li>Wrap the device inside of a UPnPDevice implementation.</li>
 * <li>Create and open a UPnPEventListener tracker for that device.</li>
 * <li>Register the new UPnPDevice with the OSGi Framework.</li>
 * </ul>
 * <p>
 * When a device is removed:
 * </p>
 * <ul>
 * <li>Unregister the UPnPDevice with the OSGi Framework.</li>
 * <li>Close the UPnPEventListener tracker for that device.</li>
 * </ul>
 *
 * @author Bruce Green
 */
class JUPnPRegistryListener extends DefaultRegistryListener {

    private final Logger log = LoggerFactory.getLogger(JUPnPRegistryListener.class);

    private Map<Device, UPnPDeviceBinding> deviceBindings = new Hashtable<Device, UPnPDeviceBinding>();
    private BundleContext context;
    private UpnpService upnpService;

    class UPnPDeviceBinding {
        private ServiceRegistration reference;
        private ServiceTracker tracker;

        UPnPDeviceBinding(ServiceRegistration reference, ServiceTracker tracker) {
            this.reference = reference;
            this.tracker = tracker;
        }

        public ServiceRegistration getServiceRegistration() {
            return reference;
        }

        public ServiceTracker getServiceTracker() {
            return tracker;
        }
    }

    public JUPnPRegistryListener(BundleContext context, UpnpService upnpService) {
        this.context = context;
        this.upnpService = upnpService;
    }

    /*
      * When an external device is discovered wrap it with UPnPDeviceImpl,
      * create a tracker for any listener to this device or its services,
      * and register the UPnPDevice.
      */
    @Override
    public void deviceAdded(Registry registry, @SuppressWarnings("rawtypes") Device device) {
		log.trace("ENTRY {}.{}: {} {}", this.getClass().getName(), "deviceAdded", registry, device);

        UPnPDeviceImpl upnpDevice = new UPnPDeviceImpl(device);
        if (device instanceof RemoteDevice) {
            String string = String.format("(%s=%s)",
                                          Constants.OBJECTCLASS, UPnPEventListener.class.getName()
            );
            try {
                Filter filter = context.createFilter(string);
                UPnPEventListenerTracker tracker = new UPnPEventListenerTracker(context, filter, upnpService, upnpDevice);
                tracker.open();

                ServiceRegistration registration = context.registerService(UPnPDevice.class.getName(), upnpDevice, upnpDevice.getDescriptions(null));
                deviceBindings.put(device, new UPnPDeviceBinding(registration, tracker));
            } catch (InvalidSyntaxException e) {
                log.error("Cannot add remote ({}).", device.getIdentity().getUdn().toString());
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public void deviceRemoved(Registry registry, @SuppressWarnings("rawtypes") Device device) {
		log.trace("ENTRY {}.{}: {} {}", this.getClass().getName(), "deviceRemoved", registry, device);

        if (device instanceof RemoteDevice) {
            UPnPDeviceBinding data = deviceBindings.get(device);
            if (data == null) {
                log.warn("Unknown device {} removed.", device.getIdentity().getUdn().toString());
            } else {
                data.getServiceRegistration().unregister();
                data.getServiceTracker().close();
                deviceBindings.remove(device);
            }
        }
    }
}
