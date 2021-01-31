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

package org.jupnp.registry;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.DiscoveryOptions;
import org.jupnp.model.ExpirationDetails;
import org.jupnp.model.ServiceReference;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.Service;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.ProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link Registry}.
 *
 * @author Christian Bauer
 */
public class RegistryImpl implements Registry {

    private Logger log = LoggerFactory.getLogger(Registry.class);

    protected UpnpService upnpService;
    protected RegistryMaintainer registryMaintainer;
    protected final Set<RemoteGENASubscription> pendingSubscriptionsLock = new HashSet();
    protected Object lock = new Object();

    public RegistryImpl() {
    }

    /**
     * Starts background maintenance immediately.
     */
    public RegistryImpl(UpnpService upnpService) {
        log.trace("Creating Registry: " + getClass().getName());

        this.upnpService = upnpService;

        log.trace("Starting registry background maintenance...");
        synchronized (lock) {
            registryMaintainer = createRegistryMaintainer();
            if (registryMaintainer != null) {
                getConfiguration().getRegistryMaintainerExecutor("upnp-registry").execute(registryMaintainer);
            }
        }
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public UpnpServiceConfiguration getConfiguration() {
        return getUpnpService().getConfiguration();
    }

    public ProtocolFactory getProtocolFactory() {
        return getUpnpService().getProtocolFactory();
    }

    protected RegistryMaintainer createRegistryMaintainer() {
        return new RegistryMaintainer(
                this,
                getConfiguration().getRegistryMaintenanceIntervalMillis()
        );
    }

    // #################################################################################################

    protected final Set<RegistryListener> registryListeners = new CopyOnWriteArraySet();
    protected final Set<RegistryItem<URI, Resource>> resourceItems = Collections
            .newSetFromMap(new ConcurrentHashMap<RegistryItem<URI, Resource>, Boolean>());
    protected final List<Runnable> pendingExecutions = new LinkedList();

    // in the methods that acquire both locks at the same time always acquire remoteItemsLock first
    protected final ReentrantReadWriteLock remoteItemsLock = new ReentrantReadWriteLock(true);
    protected final ReentrantReadWriteLock localItemsLock = new ReentrantReadWriteLock(true);
    protected final RemoteItems remoteItems = new RemoteItems(this);
    protected final LocalItems localItems = new LocalItems(this);

    // #################################################################################################

    public void addListener(RegistryListener listener) {
        registryListeners.add(listener);
    }

    public void removeListener(RegistryListener listener) {
        registryListeners.remove(listener);
    }

    public Collection<RegistryListener> getListeners() {
        return Collections.unmodifiableCollection(registryListeners);
    }

    public boolean notifyDiscoveryStart(final RemoteDevice device) {
        // Exit if we have it already, this is atomic inside this method, finally
        if (getRemoteDevice(device.getIdentity().getUdn(), true) != null) {
            log.trace("Not notifying listeners, already registered: " + device);
            return false;
        }

        for (final RegistryListener listener : getListeners()) {
            getConfiguration().getRegistryListenerExecutor("upnp-registry").execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceDiscoveryStarted(RegistryImpl.this, device);
                        }
                    }
            );
        }

        return true;
    }

    public void notifyDiscoveryFailure(final RemoteDevice device, final Exception ex) {
        for (final RegistryListener listener : getListeners()) {
            getConfiguration().getRegistryListenerExecutor("upnp-registry").execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceDiscoveryFailed(RegistryImpl.this, device, ex);
                        }
                    }
            );
        }
    }

    // #################################################################################################

    public void addDevice(LocalDevice localDevice) {
        remoteItemsLock.readLock().lock();
        try {
            localItemsLock.writeLock().lock();
            try{
                localItems.add(localDevice);
            } finally {
                localItemsLock.writeLock().unlock();
            }
        } finally {
            remoteItemsLock.readLock().unlock();
        }
    }

    public void addDevice(LocalDevice localDevice, DiscoveryOptions options) {
        remoteItemsLock.readLock().lock();
        try {
            localItemsLock.writeLock().lock();
            try {
                localItems.add(localDevice, options);
            } finally {
                localItemsLock.writeLock().unlock();
            }
        } finally {
            remoteItemsLock.readLock().unlock();
        }
    }

    public void setDiscoveryOptions(UDN udn, DiscoveryOptions options) {
        localItemsLock.writeLock().lock();
        try {
            localItems.setDiscoveryOptions(udn, options);
        } finally {
            localItemsLock.writeLock().unlock();
        }
    }

    public DiscoveryOptions getDiscoveryOptions(UDN udn) {
        localItemsLock.readLock().lock();
        try {
            return localItems.getDiscoveryOptions(udn);
        } finally {
            localItemsLock.readLock().unlock();
        }
    }

    public void addDevice(RemoteDevice remoteDevice) {
        remoteItemsLock.writeLock().lock();
        try {
            localItemsLock.readLock().lock();
            try {
                remoteItems.add(remoteDevice);
            } finally {
                localItemsLock.readLock().unlock();
            }
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    public boolean update(RemoteDeviceIdentity rdIdentity) {
        remoteItemsLock.writeLock().lock();
        try {
            localItemsLock.readLock().lock();
            try {
                return remoteItems.update(rdIdentity);
            } finally {
                localItemsLock.readLock().unlock();
            }
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    public boolean removeDevice(LocalDevice localDevice) {
        localItemsLock.writeLock().lock();
        try {
            return localItems.remove(localDevice);
        } finally {
            localItemsLock.writeLock().unlock();
        }
    }

    public boolean removeDevice(RemoteDevice remoteDevice) {
        remoteItemsLock.writeLock().lock();
        try {
            return remoteItems.remove(remoteDevice);
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    public void removeAllLocalDevices() {
        localItemsLock.writeLock().lock();
        try {
            localItems.removeAll();
        } finally {
            localItemsLock.writeLock().unlock();
        }
    }

    public void removeAllRemoteDevices() {
        remoteItemsLock.writeLock().lock();
        try {
            remoteItems.removeAll();
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    public boolean removeDevice(UDN udn) {
        Device device = getDevice(udn, true);
        if (device != null && device instanceof LocalDevice)
            return removeDevice((LocalDevice) device);
        if (device != null && device instanceof RemoteDevice)
            return removeDevice((RemoteDevice) device);
        return false;
    }

    public Device getDevice(UDN udn, boolean rootOnly) {
        Device device;

        if ((device = getLocalDevice(udn, rootOnly)) != null) return device;
        if ((device = getRemoteDevice(udn, rootOnly)) != null) return device;

        return null;
    }

    public LocalDevice getLocalDevice(UDN udn, boolean rootOnly) {
        localItemsLock.readLock().lock();
        try {
            return localItems.get(udn, rootOnly);
        } finally {
            localItemsLock.readLock().unlock();
        }
    }

    public RemoteDevice getRemoteDevice(UDN udn, boolean rootOnly) {
        remoteItemsLock.readLock().lock();
        try {
            return remoteItems.get(udn, rootOnly);
        } finally {
            remoteItemsLock.readLock().unlock();
        }
    }

    public Collection<LocalDevice> getLocalDevices() {
        localItemsLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(localItems.get());
        } finally {
            localItemsLock.readLock().unlock();
        }
    }

    public Collection<RemoteDevice> getRemoteDevices() {
        remoteItemsLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(remoteItems.get());
        } finally {
            remoteItemsLock.readLock().unlock();
        }
    }

    public Collection<Device> getDevices() {
        Set<Device> all = new HashSet<>();

        remoteItemsLock.readLock().lock();
        try {
            all.addAll(remoteItems.get());
        } finally {
            remoteItemsLock.readLock().unlock();
        }

        localItemsLock.readLock().lock();
        try {
            all.addAll(localItems.get());
        } finally {
            localItemsLock.readLock().unlock();
        }

        return Collections.unmodifiableCollection(all);
    }

    public Collection<Device> getDevices(DeviceType deviceType) {
        Collection<Device> devices = new HashSet<>();

        remoteItemsLock.readLock().lock();
        try {
            devices.addAll(remoteItems.get(deviceType));
        } finally {
            remoteItemsLock.readLock().unlock();
        }

        localItemsLock.readLock().lock();
        try {
            devices.addAll(localItems.get(deviceType));
        } finally {
            localItemsLock.readLock().unlock();
        }

        return Collections.unmodifiableCollection(devices);
    }

    public Collection<Device> getDevices(ServiceType serviceType) {
        Collection<Device> devices = new HashSet<>();

        remoteItemsLock.readLock().lock();
        try {
            devices.addAll(remoteItems.get(serviceType));
        } finally {
            remoteItemsLock.readLock().unlock();
        }

        localItemsLock.readLock().lock();
        try {
            devices.addAll(localItems.get(serviceType));
        } finally {
            localItemsLock.readLock().unlock();
        }

        return Collections.unmodifiableCollection(devices);
    }

    public Service getService(ServiceReference serviceReference) {
        Device device;
        if ((device = getDevice(serviceReference.getUdn(), false)) != null) {
            return device.findService(serviceReference.getServiceId());
        }
        return null;
    }

    // #################################################################################################

    public Resource getResource(URI pathQuery) throws IllegalArgumentException {
        if (pathQuery.isAbsolute()) {
            throw new IllegalArgumentException("Resource URI can not be absolute, only path and query:" + pathQuery);
        }

        // Note: Uses field access on resourceItems for performance reasons

		for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
        	Resource resource = resourceItem.getItem();
        	if (resource.matches(pathQuery)) {
                return resource;
            }
        }

        // TODO: UPNP VIOLATION: Fuppes on my ReadyNAS thinks it's a cool idea to add a slash at the end of the callback URI...
        // It also cuts off any query parameters in the callback URL - nice!
        if (pathQuery.getPath().endsWith("/")) {
            URI pathQueryWithoutSlash = URI.create(pathQuery.toString().substring(0, pathQuery.toString().length() - 1));

 			for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            	Resource resource = resourceItem.getItem();
            	if (resource.matches(pathQueryWithoutSlash)) {
                    return resource;
                }
            }
        }

        return null;
    }

    public <T extends Resource> T getResource(Class<T> resourceType, URI pathQuery) throws IllegalArgumentException {
        Resource resource = getResource(pathQuery);
        if (resource != null && resourceType.isAssignableFrom(resource.getClass())) {
            return (T) resource;
        }
        return null;
    }

    public Collection<Resource> getResources() {
        Collection<Resource> s = new HashSet(resourceItems.size());

        for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            s.add(resourceItem.getItem());
        }
        return s;
    }

    public <T extends Resource> Collection<T> getResources(Class<T> resourceType) {
        Collection<T> s = new HashSet(resourceItems.size());
        for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            if (resourceType.isAssignableFrom(resourceItem.getItem().getClass()))
                s.add((T) resourceItem.getItem());
        }
        return s;
    }

    public void addResource(Resource resource) {
        addResource(resource, ExpirationDetails.UNLIMITED_AGE);
    }

    public void addResource(Resource resource, int maxAgeSeconds) {
        RegistryItem resourceItem = new RegistryItem(resource.getPathQuery(), resource, maxAgeSeconds);

        resourceItems.remove(resourceItem);
        resourceItems.add(resourceItem);
    }

    public boolean removeResource(Resource resource) {
        return resourceItems.remove(new RegistryItem(resource.getPathQuery()));
    }

    // #################################################################################################

    public void addLocalSubscription(LocalGENASubscription subscription) {
        localItemsLock.writeLock().lock();
        try {
            localItems.addSubscription(subscription);
        } finally {
            localItemsLock.writeLock().unlock();
        }
    }

    public LocalGENASubscription getLocalSubscription(String subscriptionId) {
        localItemsLock.readLock().lock();
        try {
            return localItems.getSubscription(subscriptionId);
        } finally {
            localItemsLock.readLock().unlock();
        }
    }

    public boolean updateLocalSubscription(LocalGENASubscription subscription) {
        localItemsLock.writeLock().lock();
        try {
            return localItems.updateSubscription(subscription);
        } finally {
            localItemsLock.writeLock().unlock();
        }
    }

    public boolean removeLocalSubscription(LocalGENASubscription subscription) {
        localItemsLock.writeLock().lock();
        try {
            return localItems.removeSubscription(subscription);
        } finally {
            localItemsLock.writeLock().unlock();
        }
    }

    public void addRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItemsLock.writeLock().lock();
        try {
            remoteItems.addSubscription(subscription);
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    public RemoteGENASubscription getRemoteSubscription(String subscriptionId) {
        remoteItemsLock.readLock().lock();
        try {
            return remoteItems.getSubscription(subscriptionId);
        } finally {
            remoteItemsLock.readLock().unlock();
        }
    }

    public void updateRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItemsLock.writeLock().lock();
        try {
            remoteItems.updateSubscription(subscription);
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    public void removeRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItemsLock.writeLock().lock();
        try {
            remoteItems.removeSubscription(subscription);
        } finally {
            remoteItemsLock.writeLock().unlock();
        }
    }

    /* ############################################################################################################ */

    public void advertiseLocalDevices() {
        localItemsLock.readLock().lock();
        try {
            localItems.advertiseLocalDevices();
        } finally {
            localItemsLock.readLock().unlock();
        }
    }

    /* ############################################################################################################ */

    // When you call this, make sure you have the Router lock before this lock is obtained!
    public void shutdown() {
        log.trace("Shutting down registry...");

        synchronized(lock) {
            if (registryMaintainer != null)
                registryMaintainer.stop();
        }

        // Final cleanup run to flush out pending executions which might
        // not have been caught by the maintainer before it stopped
        synchronized(pendingExecutions) {
            log.trace("Executing final pending operations on shutdown: {}", pendingExecutions.size());
            runPendingExecutions(false);
        }

        for (RegistryListener listener : registryListeners) {
            listener.beforeShutdown(this);
        }

        for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            resourceItem.getItem().shutdown();
        }

        remoteItemsLock.writeLock().lock();
        try {
            remoteItems.shutdown();
        } finally {
            remoteItemsLock.writeLock().unlock();
        }

        localItemsLock.writeLock().lock();
        try {
            localItems.shutdown();
        } finally {
            localItemsLock.writeLock().unlock();
        }

        for (RegistryListener listener : registryListeners) {
            listener.afterShutdown();
        }
    }

    public void pause() {
        synchronized(lock) {
            if (registryMaintainer != null) {
                log.trace("Pausing registry maintenance");
                runPendingExecutions(true);
                registryMaintainer.stop();
                registryMaintainer = null;
            }
        }
    }

    public void resume() {
        synchronized(lock) {
            if (registryMaintainer == null) {
                log.trace("Resuming registry maintenance");
                remoteItemsLock.writeLock().lock();
                try {
                    localItemsLock.readLock().lock();
                    try {
                        remoteItems.resume();
                    } finally {
                        localItemsLock.readLock().unlock();
                    }
                } finally {
                    remoteItemsLock.writeLock().unlock();
                }

                registryMaintainer = createRegistryMaintainer();
                if (registryMaintainer != null) {
                    getConfiguration().getRegistryMaintainerExecutor("upnp-registry").execute(registryMaintainer);
                }
            }
        }
    }

    public boolean isPaused() {
        synchronized(lock) {
            return registryMaintainer == null;
        }
    }

    /* ############################################################################################################ */

    void maintain() {

        log.trace("Maintaining registry...");

        // Remove expired resources
        Iterator<RegistryItem<URI, Resource>> it = resourceItems.iterator();
        while (it.hasNext()) {
            RegistryItem<URI, Resource> item = it.next();
            if (item.getExpirationDetails().hasExpired()) {
                log.trace("Removing expired resource: " + item);
                it.remove();
            }
        }
        
        // Let each resource do its own maintenance
        synchronized(pendingExecutions) {
            for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                resourceItem.getItem().maintain(
                        pendingExecutions,
                        resourceItem.getExpirationDetails()
                );
            }
        }

        // These add all their operations to the pendingExecutions queue
        remoteItemsLock.writeLock().lock();
        try {
            remoteItems.maintain();
        } finally {
            remoteItemsLock.writeLock().unlock();
        }

        localItemsLock.writeLock().lock();
        try {
            localItems.maintain();
        } finally {
            localItemsLock.writeLock().unlock();
        }

        // We now run the queue asynchronously so the maintenance thread can continue its loop undisturbed
        runPendingExecutions(true);
    }

    void executeAsyncProtocol(Runnable runnable) {
        synchronized (pendingExecutions) {
            pendingExecutions.add(runnable);
        }
    }

    void runPendingExecutions(boolean async) {
        synchronized (pendingExecutions) {
            log.trace("Executing pending operations: {}", pendingExecutions.size());
            for (Runnable pendingExecution : pendingExecutions) {
                if (async)
                    getConfiguration().getAsyncProtocolExecutor("upnp-registry").execute(pendingExecution);
                else
                    pendingExecution.run();
            }
            if (pendingExecutions.size() > 0) {
                pendingExecutions.clear();
            }
        }
    }

    /* ############################################################################################################ */

    public void printDebugLog() {
        if (log.isTraceEnabled()) {
            log.trace("====================================    REMOTE   ================================================");

            remoteItemsLock.readLock().lock();
            try {
                for (RemoteDevice remoteDevice : remoteItems.get()) {
                    log.trace(remoteDevice.toString());
                }
            } finally {
                remoteItemsLock.readLock().unlock();
            }

            log.trace("====================================    LOCAL    ================================================");

            localItemsLock.readLock().lock();
            try {
                for (LocalDevice localDevice : localItems.get()) {
                    log.trace(localDevice.toString());
                }
            } finally {
                localItemsLock.readLock().unlock();
            }

            log.trace("====================================  RESOURCES  ================================================");

            for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                log.trace(resourceItem.toString());
            }

            log.trace("=================================================================================================");

        }

    }
    
 	@Override
	public void registerPendingRemoteSubscription(RemoteGENASubscription subscription) {
		synchronized (pendingSubscriptionsLock) {
            pendingSubscriptionsLock.add(subscription);
        }
	}
	
	@Override
	public void unregisterPendingRemoteSubscription(RemoteGENASubscription subscription) {
        synchronized (pendingSubscriptionsLock) {
            if(pendingSubscriptionsLock.remove(subscription)) {
                pendingSubscriptionsLock.notifyAll();
            }
        }
	}

    @Override
    public RemoteGENASubscription getWaitRemoteSubscription(String subscriptionId) {
        synchronized (pendingSubscriptionsLock) {
            do {
                RemoteGENASubscription subscription = getRemoteSubscription(subscriptionId);
                if (subscription != null) {
                    return subscription;
                }
                if (!pendingSubscriptionsLock.isEmpty()) {
                    try {
                        log.trace("Subscription not found, waiting for pending subscription procedure to terminate." );
                        pendingSubscriptionsLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            } while (!pendingSubscriptionsLock.isEmpty());
        }
        return null;
    }

}
