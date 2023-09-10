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

package org.jupnp.osgi.test.integration;

import java.io.File;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.osgi.framework.Bundle;
import org.osgi.service.upnp.UPnPDevice;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UnsignedVariableInteger;
import org.jupnp.osgi.test.util.BundleUtil;
import org.jupnp.osgi.test.util.DataUtil;

public class BaseIntegration {
	private UpnpService upnpService;

	public void setup() throws Exception {
		upnpService = new UpnpServiceImpl(new JettyUpnpServiceConfiguration());
		upnpService.getControlPoint().search(new STAllHeader());
		Thread.sleep(5000);
	}

	public void tearDown() {
		upnpService.shutdown();
	}

	public UpnpService getUpnpService() {
		return upnpService;
	}

	public void dumpEnvironment() {
		for (Object key : System.getenv().keySet()) {
			System.out.printf("%s: %s\n", key, System.getenv().get(key));
		}

		System.out.printf("current directory: %s\n", new File(".").getAbsolutePath());
	}

	public void dumpBundles(Bundle[] bundles) {
		System.out.println("This is running inside Felix.");
		for (Bundle bundle : bundles) {
			System.out.printf("%2d|%-12s| %s\n", bundle.getBundleId(), BundleUtil.getBundleState(bundle), bundle.getSymbolicName());
		}
	}

	public void dumpRegistry() {
		System.out.printf("*** UPnP Devices ***\n");
		for (Device device : upnpService.getRegistry().getDevices()) {
			System.out.printf("%s: %s\n", device.getIdentity().getUdn(), device.getType().getType());
		}
	}

	public void dumpUPnPDevice(UPnPDevice device) {
		System.out.printf("%s\n", device);
		for (Object key : Collections.list(device.getDescriptions(null).keys())) {
			System.out.printf("\t %s: %s\n", key, device.getDescriptions(null).get(key));
		}
	}

	public Device getDevice(DeviceType type) {
		Collection<Device> devices = upnpService.getRegistry().getDevices(type);

		return devices.iterator().hasNext() ? devices.iterator().next() : null;
	}

	public Device getDevice(ServiceType type) {
		Collection<Device> devices = upnpService.getRegistry().getDevices(type);

		return devices.iterator().hasNext() ? devices.iterator().next() : null;
	}

	public Service getService(Device device, ServiceType type) {
		for (Service service : device.getServices()) {
			if (service.getServiceType().equals(type)) {
				return service;
			}
		}

		return null;
	}

	public Service getService(ServiceType type) {
		Device device = getDevice(type);
		for (Service service : device.getServices()) {
			if (service.getServiceType().equals(type)) {
				return service;
			}
		}

		return null;
	}

	public Action getAction(Service service, String name) {
		return service.getAction(name);
	}

	public String bytesToString(byte[] bytes) {
		String string = new String();

		for (int i = 0; i < bytes.length; i++) {
			string += String.format("0x%x ", bytes[i]);
		}

		return string;
	}

	public byte[] toBytes(Byte[] Bytes) {
		byte[] bytes = new byte[Bytes.length];
		for (int i = 0; i < Bytes.length; i++) {
			bytes[i] = Bytes[i].byteValue();
		}

		return bytes;
	}

	public String valueToString(Object value) {
		String string;

		if (value == null) {
			string = "[null]";
		}
		else if (value instanceof byte[]) {
			string = bytesToString((byte[]) value);
		}
		else if (value instanceof Byte[]) {
			string = bytesToString(toBytes((Byte[]) value));
		}
		else {
			string = value.toString();
		}

		return string;
	}

	public boolean validate(String name, String type, Object value, Object desired) {
		boolean matches;

		System.out.printf("=========================================\n");
		System.out.printf("data type: %s\n", type);
		System.out.printf("    value: %s (%s)\n", valueToString(value), value.getClass().getName());
		System.out.printf("  desired: %s (%s)\n", valueToString(desired), desired.getClass().getName());

		if (value instanceof UnsignedVariableInteger) {
			value = Integer.valueOf(((UnsignedVariableInteger) value).getValue().intValue());
		}
		else if (value instanceof Calendar) {
			if (type.equals("time") || type.equals("time.tz")) {
				Calendar calendar = (Calendar) value;
				Date date = calendar.getTime();
				long time = date.getTime() + calendar.getTimeZone().getOffset(date.getTime());
				value = Long.valueOf(time);
			}
			else {
				value = ((Calendar) value).getTime();
			}
		}
		else if (value instanceof Byte[]) {
			if (type.equals("bin.base64")) {
				value = Base64.getDecoder().decode(toBytes((Byte[]) value));
			}
			else {
				value = toBytes((Byte[]) value);
			}
		}

		if (value instanceof byte[]) {
			matches = DataUtil.compareBytes((byte[]) value, (byte[]) desired);
		}
		else {
			matches = value.equals(desired);

			if (!matches) {
				matches = value.toString().equals(desired.toString());
			}
		}

		System.out.printf("  matches: %s\n", matches ? "TRUE" : "FALSE");

		return matches;
	}
}
