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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.data.SampleData;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.action.RemoteActionInvocation;
import org.jupnp.model.message.Connection;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.message.header.UserAgentHeader;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.types.UDADeviceType;

/**
 * Accessing remote client information
 * <p>
 * Theoretically, your service implementation should work with any client, as UPnP is
 * supposed to provide a compatibility layer. In practice, this never works as no
 * UPnP client and server is fully compatible with the specifications (except jUPnP, of
 * course).
 * </p>
 * <p>
 * If your action method has a last (or only parameter) of type <code>RemoteClientInfo</code>,
 * jUPnP will provide details about the control point calling your service:
 * </p>
 * <a class="citation" href="javacode://example.localservice.SwitchPowerWithClientInfo" style="include:CLIENT_INFO"/>
 * <p>
 * The <code>RemoteClientInfo</code> argument will only be available when this action method
 * is processing a remote client call, an <code>ActionInvocation</code> executed by the
 * local UPnP stack on a local service does not have remote client information and the
 * argument will be <code>null</code>.
 * </p>
 * <p>
 * A client's remote and local address might be <code>null</code> if the jUPnP
 * transport layer was not able to obtain the connection's address.
 * </p>
 * <p>
 * You can set extra response headers on the <code>RemoteClientInfo</code>, which will be
 * returned to the client with the response of your UPnP action. There is also a
 * <code>setResponseUserAgent()</code> method for your convenience.
 * </p>
 */
class RemoteClientInfoTest {

    static LocalDevice createTestDevice(Class serviceClass) throws Exception {

        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"), svc);
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] { { createTestDevice(SwitchPowerWithClientInfo.class) } };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        UpnpHeaders requestHeaders = new UpnpHeaders();
        requestHeaders.add(UpnpHeader.Type.USER_AGENT, new UserAgentHeader("foo/bar"));
        requestHeaders.add("X-MY-HEADER", "foo");

        RemoteClientInfo clientInfo = new RemoteClientInfo(new Connection() {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public InetAddress getRemoteAddress() {
                try {
                    return InetAddress.getByName("10.0.0.1");
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public InetAddress getLocalAddress() {
                try {
                    return InetAddress.getByName("10.0.0.2");
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, requestHeaders);

        ActionInvocation setTargetInvocation = new RemoteActionInvocation(svc.getAction("SetTarget"), clientInfo);

        setTargetInvocation.setInput("NewTargetValue", true);
        svc.getExecutor(setTargetInvocation.getAction()).execute(setTargetInvocation);
        assertNull(setTargetInvocation.getFailure());
        assertEquals(0, setTargetInvocation.getOutput().length);

        assertEquals("foobar", clientInfo.getExtraResponseHeaders().getFirstHeader("X-MY-HEADER"));
    }
}
