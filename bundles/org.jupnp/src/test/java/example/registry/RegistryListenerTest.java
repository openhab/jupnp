/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
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
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package example.registry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.protocol.RetrieveRemoteDescriptors;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;

/**
 * Listening to registry changes
 * <p>
 * The <code>RegistryListener</code> is your primary API when discovering devices and services with your
 * control point. UPnP operates asynchronous, so advertisements (either <em>alive</em> or <em>byebye</em>)
 * of devices can occur at any time. Responses to your network search messages are also asynchronous.
 * </p>
 * <p>
 * This is the interface:
 * </p>
 * <a class="citation" href="javacode://example.registry.RegistryListenerTest.RegistryListener"/>
 * <p>
 * Typically you don't want to implement all of these methods. Some are only useful if you write
 * a service or a generic control point. Most of the time you want to be notified when a particular
 * device with a particular service appears on your network. So it is much easier to extend
 * the <code>DefaultRegistryListener</code>, which has empty implementations for all methods of
 * the interface, and only override the methods you need.
 * </p>
 * <a class="citation" href="javadoc://this#quickstartListener" style="read-title: false"/>
 * <a class="citation" href="javadoc://this#regularListener" style="read-title: false"/>
 */
class RegistryListenerTest {

    // Just for documentation inclusion!
    public interface RegistryListener {

        void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device);

        void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex);

        void remoteDeviceAdded(Registry registry, RemoteDevice device);

        void remoteDeviceUpdated(Registry registry, RemoteDevice device);

        void remoteDeviceRemoved(Registry registry, RemoteDevice device);

        void localDeviceAdded(Registry registry, LocalDevice device);

        void localDeviceRemoved(Registry registry, LocalDevice device);
    }

    /**
     * <p>
     * The <code>remoteDeviceDiscoveryStarted()</code> and <code>remoteDeviceDiscoveryFailed()</code>
     * methods are completely optional but useful on slow machines (such as Android handsets). jUPnP
     * will retrieve and initialize all device metadata for each UPnP device before it will announce
     * it on the <code>Registry</code>. UPnP metadata is split into several XML descriptors, so retrieval
     * via HTTP of these descriptors, parsing, and validating all metadata for a complex UPnP device
     * and service model can take several seconds. These two methods allow you to access the device
     * as soon as possible, after the first descriptor has been retrieved and parsed. At this time
     * the services metadata is however not available:
     * </p>
     * <a class="citation" href="javacode://example.registry.RegistryListenerTest.QuickstartRegistryListener" style=
     * "exclude: EXC1, EXC2;"/>
     * <p>
     * This is how you register and activate a listener:
     * </p>
     * <a class="citation" href="javacode://this" style="include: INC1"/>
     */
    @Test
    void quickstartListener() throws Exception {

        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        try {
                            String deviceDescriptorXML = getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice, new RemoteClientInfo(), getConfiguration().getNamespace());
                            String serviceOneXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[0]);
                            String serviceTwoXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[1]);
                            String serviceThreeXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[2]);
                            return new StreamResponseMessage[] {
                                    new StreamResponseMessage(deviceDescriptorXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceOneXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceTwoXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceThreeXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8) };
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }
        };
        upnpService.startup();

        QuickstartRegistryListener listener = new QuickstartRegistryListener(); // DOC: INC1
        upnpService.getRegistry().addListener(listener); // DOC: INC1

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertTrue(listener.valid);
    }

    @Test
    void failureQuickstartListener() throws Exception {
        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        String deviceDescriptorXML;
                        DeviceDescriptorBinder binder = getConfiguration().getDeviceDescriptorBinderUDA10();
                        try {
                            deviceDescriptorXML = binder.generate(hydratedDevice, new RemoteClientInfo(),
                                    getConfiguration().getNamespace());
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                        return new StreamResponseMessage[] { new StreamResponseMessage(deviceDescriptorXML,
                                ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8), null, null, null // Don't return any
                                                                                               // service descriptors,
                                                                                               // make it fail
                        };
                    }
                };
            }
        };
        upnpService.startup();

        FailureQuickstartRegistryListener listener = new FailureQuickstartRegistryListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertTrue(listener.valid);
    }

    public class QuickstartRegistryListener extends DefaultRegistryListener {
        public boolean valid = false; // DOC: EXC1

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {

            // You can already use the device here and you can see which services it will have
            assertEquals(3, device.findServices().length);

            // But you can't use the services
            for (RemoteService service : device.findServices()) {
                assertEquals(0, service.getActions().length);
                assertEquals(0, service.getStateVariables().length);
            }
            valid = true; // DOC: EXC2
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
            // You might want to drop the device, its services couldn't be hydrated
        }
    }

    public class FailureQuickstartRegistryListener extends DefaultRegistryListener {
        public boolean valid = false;

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
            valid = true;
        }
    }

    /**
     * <p>
     * Most of the time, on any device that is faster than a cellphone, your listeners will look
     * like this:
     * </p>
     * <a class="citation" href="javacode://example.registry.RegistryListenerTest.MyListener" style="exclude: EXC1,
     * EXC2, EXC3;"/>
     * <p>
     * The device metadata of the parameter to <code>remoteDeviceAdded()</code> is fully hydrated, all
     * of its services, actions, and state variables are available. You can continue with this metadata,
     * writing action invocations and event monitoring callbacks. You also might want to react accordingly
     * when the device disappears from the network.
     * </p>
     */
    @Test
    void regularListener() throws Exception {

        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        try {
                            String deviceDescriptorXML = getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice, new RemoteClientInfo(), getConfiguration().getNamespace());
                            String serviceOneXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[0]);
                            String serviceTwoXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[1]);
                            String serviceThreeXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[2]);
                            return new StreamResponseMessage[] {
                                    new StreamResponseMessage(deviceDescriptorXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceOneXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceTwoXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceThreeXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8) };
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }
        };
        upnpService.startup();

        MyListener listener = new MyListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        upnpService.getRegistry().removeAllRemoteDevices();

        assertTrue(listener.added);
        assertTrue(listener.removed);
    }

    @Test
    void ipAddressChangeOnRegisteredDevice() throws Exception {
        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        try {
                            String deviceDescriptorXML = getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice, new RemoteClientInfo(), getConfiguration().getNamespace());
                            String serviceOneXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[0]);
                            String serviceTwoXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[1]);
                            String serviceThreeXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[2]);
                            return new StreamResponseMessage[] {
                                    new StreamResponseMessage(deviceDescriptorXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceOneXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceTwoXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceThreeXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8) };
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }
        };
        upnpService.startup();

        MyListener listener = new MyListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertTrue(listener.added);
        assertFalse(listener.removed);

        listener.reset();

        upnpService.getRegistry().addDevice(new RemoteDevice(SampleData.createSecondRemoteDeviceIdentity(1800)));

        assertTrue(listener.added);
        assertTrue(listener.removed);
        assertEquals("127.0.0.2", listener.deviceAdded.getIdentity().getDescriptorURL().getHost());
        assertEquals("127.0.0.1", listener.deviceRemoved.getIdentity().getDescriptorURL().getHost());

        listener.reset();

        upnpService.getRegistry().removeAllRemoteDevices();
        assertFalse(listener.added);
        assertTrue(listener.removed);
    }

    public static class MyListener extends DefaultRegistryListener {
        public boolean added = false; // DOC: EXC1
        public boolean removed = false; // DOC: EXC1
        public RemoteDevice deviceAdded = null;
        public RemoteDevice deviceRemoved = null;

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            added = true; // DOC: EXC2
            deviceAdded = device;
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            // Stop using the service if this is the same device, it's gone now
            removed = true; // DOC: EXC3
            deviceRemoved = device;
        }

        void reset() {
            added = false;
            removed = false;
            deviceAdded = null;
            deviceRemoved = null;
        }
    }
}
