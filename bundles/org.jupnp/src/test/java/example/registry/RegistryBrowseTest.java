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

package example.registry;

import org.junit.jupiter.api.Disabled;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.resource.DeviceDescriptorResource;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.RegistrationException;
import org.jupnp.registry.Registry;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.data.SampleDeviceRootLocal;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Browsing the Registry
 * <p>
 * Although you typically create a <code>RegistryListener</code> to be notified of discovered and
 * disappearing UPnP devices on your network, sometimes you have to browse the <code>Registry</code>
 * manually.
 * </p>
 * <a class="citation" href="javadoc://this#findDevice" style="read-title: false"/>
 * <a class="citation" href="javadoc://this#findDeviceByType" style="read-title: false"/>
 */
class RegistryBrowseTest {

    /**
     * <p>
     * The following call will return a device with the given unique device name, but
     * only a root device and not any embedded device. Set the second parameter of
     * <code>registry.getDevice()</code> to <code>false</code> if the device you are
     * looking for might be an embedded device.
     * </p>
     * <a class="citation" href="javacode://this" style="include: FIND_ROOT_UDN"/>
     * <p>
     * If you know that the device you need is a <code>LocalDevice</code> - or a
     * <code>RemoteDevice</code> - you can use the following operation:
     * </p>
     * <a class="citation" href="javacode://this" style="include: FIND_LOCAL_DEVICE" id="javacode_find_device_local"/>
     */
    @Test
    void findDevice() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice device = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(device);

        UDN udn = device.getIdentity().getUdn();

        Registry registry = upnpService.getRegistry();                          // DOC: FIND_ROOT_UDN
        Device foundDevice = registry.getDevice(udn, true);

        assertEquals(udn, foundDevice.getIdentity().getUdn());                  // DOC: FIND_ROOT_UDN

        LocalDevice localDevice = registry.getLocalDevice(udn, true);           // DOC: FIND_LOCAL_DEVICE
        assertEquals(udn, localDevice.getIdentity().getUdn());

        SampleDeviceRootLocal.assertLocalResourcesMatch(
                upnpService.getConfiguration().getNamespace().getResources(device)
        );
    }

    /**
     * <p>
     * Most of the time you need a device that is of a particular type or that implements
     * a particular service type, because this is what your control point can handle:
     * </p>
     * <a class="citation" href="javacode://this" style="include: FIND_DEV_TYPE"/>
     * <a class="citation" href="javacode://this" style="include: FIND_SERV_TYPE" id="javacode_find_serv_type"/>
     */
    @Test
    void findDeviceByType() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice device = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(device);

        Registry registry = upnpService.getRegistry();

        try {
            DeviceType deviceType = new UDADeviceType("MY-DEVICE-TYPE", 1);         // DOC: FIND_DEV_TYPE
            Collection<Device> devices = registry.getDevices(deviceType);           // DOC: FIND_DEV_TYPE
            assertEquals(1, devices.size());
        } finally {}

        try {
            ServiceType serviceType = new UDAServiceType("MY-SERVICE-TYPE-ONE", 1); // DOC: FIND_SERV_TYPE
            Collection<Device> devices = registry.getDevices(serviceType);          // DOC: FIND_SERV_TYPE
            assertEquals(1, devices.size());
        } finally {}
    }

    @Test
    void findLocalDevice() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice deviceOne = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(deviceOne);

        DeviceDescriptorResource resource =
                upnpService.getRegistry().getResource(
                        DeviceDescriptorResource.class,
                        SampleDeviceRoot.getDeviceDescriptorURI()
        );

        assertNotNull(resource);
    }

    @Test
    void findLocalDeviceInvalidRelativePath() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice deviceOne = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(deviceOne);

        assertThrows(IllegalArgumentException.class, () ->
                upnpService.getRegistry().getResource(
                        DeviceDescriptorResource.class,
                        URI.create("http://host/invalid/absolute/URI")
        ));
    }

    @Test
    @Disabled("TODO: For now just ignore duplicate devices because we need to test proxies")
    void registerDuplicateDevices() {
        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice deviceOne = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(deviceOne);

        LocalDevice deviceTwo = SampleData.createLocalDevice();
        assertThrows(RegistrationException.class, () ->
            upnpService.getRegistry().addDevice(deviceTwo));
    }

    @Test
    void cleanupRemoteDevice() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        RemoteDevice rd = SampleData.createRemoteDevice();

        upnpService.getRegistry().addDevice(rd);

        assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 1);

        Resource resource = upnpService.getRegistry().getResource(
                URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event/cb")
        );
        assertNotNull(resource);

        upnpService.getRegistry().removeDevice(rd);

        assertEquals(0, upnpService.getRegistry().getRemoteDevices().size());

        resource = upnpService.getRegistry().getResource(
                URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event/cb")
        );
        assertNull(resource);
    }

}
