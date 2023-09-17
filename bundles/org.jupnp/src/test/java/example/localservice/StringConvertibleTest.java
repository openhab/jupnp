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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;
import org.jupnp.data.SampleData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * String value converters
 * <p>
 * Consider the following service class with all state variables of
 * <code>string</code> UPnP datatype - but with a much more specific
 * Java type:
 * </p>
 * <a class="citation" href="javacode://example.localservice.MyServiceWithStringConvertibles" style="include: INC1"/>
 * <p>
 * The state variables are all of UPnP datatype <code>string</code> because
 * jUPnP knows that the Java type of the annotated field is "string convertible".
 * This is always the case for <code>java.net.URI</code> and <code>java.net.URL</code>.
 * </p>
 * <p>
 * Any other Java type you'd like to use for automatic string conversion has to be named
 * in the <code>@UpnpService</code> annotation on the class, like the
 * <code>MyStringConvertible</code>. Note that these types have to
 * have an appropriate <code>toString()</code> method and a single argument constructor
 * that accepts a <code>java.lang.String</code> ("from string" conversion).
 * </p>
 * <p>
 * The <code>List&lt;Integer></code> is the collection you'd use in your service
 * implementation to group several numbers. Let's assume that for UPnP communication
 * you need a comma-separated representation of the individual values in a string,
 * as is required by many of the UPnP A/V specifications. First, tell jUPnP that
 * the state variable really is a string datatype, it can't infer that
 * from the field type. Then, if an action has this output argument, instead of
 * manually creating the comma-separated string you pick the appropriate converter
 * from the classes in <code>org.jupnp.model.types.csv.*</code> and return
 * it from your action method. These are actually <code>java.util.List</code>
 * implementations, so you could use them <em>instead</em> of
 * <code>java.util.List</code> if you don't care about the dependency. Any action
 * input argument value can also be converted from a comma-separated string
 * representation to a list automatically - all you have to do is use the
 * CSV converter class as an input argument type.
 * </p>
 */
class StringConvertibleTest {

        static LocalDevice createTestDevice(Class serviceClass) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(
                SampleData.createLocalDeviceIdentity(),
                new DeviceType("mydomain", "CustomDevice", 1),
                new DeviceDetails("A Custom Device"),
                svc
        );
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][]{
                {createTestDevice(MyServiceWithStringConvertibles.class)},
        };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void validateBinding(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        assertEquals(4, svc.getStateVariables().length);
        for (StateVariable stateVariable : svc.getStateVariables()) {
            assertEquals(Datatype.Builtin.STRING, stateVariable.getTypeDetails().getDatatype().getBuiltin());
        }

        assertEquals(9, svc.getActions().length); // Has 8 actions plus QueryStateVariableAction!

        assertEquals(1, svc.getAction("SetMyURL").getArguments().length);
        assertEquals("In", svc.getAction("SetMyURL").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.IN, svc.getAction("SetMyURL").getArguments()[0].getDirection());
        assertEquals("MyURL", svc.getAction("SetMyURL").getArguments()[0].getRelatedStateVariableName());
        // The others are all the same...
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        ActionInvocation setMyURL = new ActionInvocation(svc.getAction("SetMyURL"));
        setMyURL.setInput("In", "http://foo/bar");
        svc.getExecutor(setMyURL.getAction()).execute(setMyURL);
        assertNull(setMyURL.getFailure());
        assertEquals(0, setMyURL.getOutput().length);

        ActionInvocation getMyURL = new ActionInvocation(svc.getAction("GetMyURL"));
        svc.getExecutor(getMyURL.getAction()).execute(getMyURL);
        assertNull(getMyURL.getFailure());
        assertEquals(1, getMyURL.getOutput().length);
        assertEquals("http://foo/bar", getMyURL.getOutput()[0].toString());

        ActionInvocation setMyURI = new ActionInvocation(svc.getAction("SetMyURI"));
        setMyURI.setInput("In", "http://foo/bar");
        svc.getExecutor(setMyURI.getAction()).execute(setMyURI);
        assertNull(setMyURI.getFailure());
        assertEquals(0, setMyURI.getOutput().length);

        ActionInvocation getMyURI = new ActionInvocation(svc.getAction("GetMyURI"));
        svc.getExecutor(getMyURI.getAction()).execute(getMyURI);
        assertNull(getMyURI.getFailure());
        assertEquals(1, getMyURI.getOutput().length);
        assertEquals("http://foo/bar", getMyURI.getOutput()[0].toString());

        ActionInvocation setMyNumbers = new ActionInvocation(svc.getAction("SetMyNumbers"));
        setMyNumbers.setInput("In", "1,2,3");
        svc.getExecutor(setMyNumbers.getAction()).execute(setMyNumbers);
        assertNull(setMyNumbers.getFailure());
        assertEquals(0, setMyNumbers.getOutput().length);

        ActionInvocation getMyNumbers = new ActionInvocation(svc.getAction("GetMyNumbers"));
        svc.getExecutor(getMyNumbers.getAction()).execute(getMyNumbers);
        assertNull(getMyNumbers.getFailure());
        assertEquals(1, getMyNumbers.getOutput().length);
        assertEquals("1,2,3", getMyNumbers.getOutput()[0].toString());

        ActionInvocation setMyStringConvertible = new ActionInvocation(svc.getAction("SetMyStringConvertible"));
        setMyStringConvertible.setInput("In", "foobar");
        svc.getExecutor(setMyStringConvertible.getAction()).execute(setMyStringConvertible);
        assertNull(setMyStringConvertible.getFailure());
        assertEquals(0, setMyStringConvertible.getOutput().length);

        ActionInvocation getMyStringConvertible = new ActionInvocation(svc.getAction("GetMyStringConvertible"));
        svc.getExecutor(getMyStringConvertible.getAction()).execute(getMyStringConvertible);
        assertNull(getMyStringConvertible.getFailure());
        assertEquals(1, getMyStringConvertible.getOutput().length);
        assertEquals("foobar", getMyStringConvertible.getOutput()[0].toString());
    }
}
