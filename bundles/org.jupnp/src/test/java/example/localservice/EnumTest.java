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
package example.localservice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.data.SampleData;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;

/**
 * Working with enums
 * <p>
 * Java <code>enum</code>'s are special, unfortunately: You can't instantiate
 * an enum value through reflection. So jUPnP can convert your enum value
 * into a string for transport in UPnP messages, but you have to convert
 * it back manually from a string. This is shown in the following
 * service example:
 * </p>
 * <a class="citation" href="javacode://example.localservice.MyServiceWithEnum" style="include: INC1"/>
 * <p>
 * jUPnP will automatically assume that the datatype is a UPnP string if the
 * field (or getter) or getter Java type is an enum. Furthermore, an
 * <code>&lt;allowedValueList&gt;</code> will be created in your service descriptor
 * XML, so control points know that this state variable has in fact a defined
 * set of possible values.
 * </p>
 */
class EnumTest {

    static LocalDevice createTestDevice(Class serviceClass) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new DeviceType("mydomain", "CustomDevice", 1),
                new DeviceDetails("A Custom Device"), svc);
    }

    static Object[][] getDevices() {

        try {
            return new LocalDevice[][] { { createTestDevice(MyServiceWithEnum.class) }, };
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            // Damn testng swallows exceptions in provider/factory methods
            throw new RuntimeException(ex);
        }
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void validateBinding(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        assertEquals(1, svc.getStateVariables().length);
        assertEquals(Datatype.Builtin.STRING, svc.getStateVariables()[0].getTypeDetails().getDatatype().getBuiltin());

        assertEquals(svc.getActions().length, 3); // Has 2 actions plus QueryStateVariableAction!

        assertEquals(1, svc.getAction("GetColor").getArguments().length);
        assertEquals("Out", svc.getAction("GetColor").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.OUT, svc.getAction("GetColor").getArguments()[0].getDirection());
        assertEquals("Color", svc.getAction("GetColor").getArguments()[0].getRelatedStateVariableName());

        assertEquals(1, svc.getAction("SetColor").getArguments().length);
        assertEquals("In", svc.getAction("SetColor").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.IN, svc.getAction("SetColor").getArguments()[0].getDirection());
        assertEquals("Color", svc.getAction("SetColor").getArguments()[0].getRelatedStateVariableName());
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        ActionInvocation setColor = new ActionInvocation(svc.getAction("SetColor"));
        setColor.setInput("In", MyServiceWithEnum.Color.Blue);
        svc.getExecutor(setColor.getAction()).execute(setColor);
        assertNull(setColor.getFailure());
        assertEquals(0, setColor.getOutput().length);

        ActionInvocation getColor = new ActionInvocation(svc.getAction("GetColor"));
        svc.getExecutor(getColor.getAction()).execute(getColor);
        assertNull(getColor.getFailure());
        assertEquals(1, getColor.getOutput().length);
        assertEquals(MyServiceWithEnum.Color.Blue.name(), getColor.getOutput()[0].toString());
    }
}
