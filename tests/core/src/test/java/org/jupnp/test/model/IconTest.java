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

package org.jupnp.test.model;

import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.test.data.SampleData;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Christian Bauer
 */
class IconTest {

    @Test
    void validIcons() throws Exception {
        RemoteDevice rd = new RemoteDevice(
            SampleData.createRemoteDeviceIdentity(),
            new UDADeviceType("Foo", 1),
            new DeviceDetails("Foo"),
            new Icon[]{
                new Icon(null, 0, 0, 0, URI.create("foo")),
                new Icon("foo/bar", 0, 0, 0, URI.create("foo")),
                new Icon("foo/bar", 123, 456, 0, URI.create("foo"))
            },
            new RemoteService[0]
        );
        assertEquals(3, rd.findIcons().length);
    }

    @Test
    void invalidIcons() throws Exception {
        RemoteDevice rd = new RemoteDevice(
            SampleData.createRemoteDeviceIdentity(),
            new UDADeviceType("Foo", 1),
            new DeviceDetails("Foo"),
            new Icon[]{
                new Icon("image/png", 123, 123, 8, URI.create("urn:not_a_URL")),
            },
            new RemoteService[0]
        );
        assertEquals(0, rd.findIcons().length);
    }
}
