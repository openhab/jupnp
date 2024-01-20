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

package example.localservice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.data.SampleData;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;

/**
 * Exclusive list of string values
 * <p>
 * If you have a static list of legal string values, set it directly on the annotation
 * of your state variable's field:
 * </p>
 * <a class="citation" href="javacode://example.localservice.MyServiceWithAllowedValues" style="include: VAR"/>
 * <p>
 * Alternatively, if your allowed values have to be determined dynamically when
 * your service is being bound, you can implement a class with the
 * <code>org.jupnp.binding.AllowedValueProvider</code> interface:
 * </p>
 * <a class="citation" href="javacode://example.localservice.MyServiceWithAllowedValueProvider" style="include:
 * PROVIDER"/>
 * <p>
 * Then, instead of specifying a static list of string values in your state variable declaration,
 * name the provider class:
 * </p>
 * <a class="citation" id="MyServiceWithAllowedValueProvider-VAR" href=
 * "javacode://example.localservice.MyServiceWithAllowedValueProvider" style="include: VAR"/>
 * <p>
 * Note that this provider will only be queried when your annotations are being processed,
 * once when your service is bound in jUPnP.
 * </p>
 */
class AllowedValueTest {

    public static LocalDevice createTestDevice(Class serviceClass) throws Exception {

        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new DeviceType("mydomain", "CustomDevice", 1),
                new DeviceDetails("A Custom Device"), svc);
    }

    public static Object[][] getDevices() {
        try {
            return new LocalDevice[][] { { createTestDevice(MyServiceWithAllowedValues.class) },
                    { createTestDevice(MyServiceWithAllowedValueProvider.class) }, };
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
        assertEquals(3, svc.getStateVariables()[0].getTypeDetails().getAllowedValues().length);
        assertEquals("Foo", svc.getStateVariables()[0].getTypeDetails().getAllowedValues()[0]);
        assertEquals("Bar", svc.getStateVariables()[0].getTypeDetails().getAllowedValues()[1]);
        assertEquals("Baz", svc.getStateVariables()[0].getTypeDetails().getAllowedValues()[2]);
    }
}
