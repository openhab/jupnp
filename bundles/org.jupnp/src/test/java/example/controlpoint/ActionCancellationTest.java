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
package example.controlpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Future;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionCancelledException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.UDAServiceId;

import example.binarylight.BinaryLightSampleData;

/**
 * Cancelling an action invocation
 * <p>
 * You call actions of services with the <code>ControlPoint#execute(myCallback)</code> method. So far you probably
 * haven't considered the optional return value of this method, a <code>Future</code> which can be used to cancel
 * the invocation:
 * </p>
 * <p/>
 * <a class="citation"
 * href="javacode://this#invokeActions(LocalDevice)"
 * style="include: EXECUTE_CANCEL;"/>
 * <p/>
 * <p>
 * Here we are calling the <code>SetTarget</code> action of a <em>SwitchPower:1</em> service, and after waiting a
 * (short) time period, we cancel the request. What happens now depends on the invocation and what service you are
 * calling. If it's a local service, and no network access is needed, the thread calling the local service (method)
 * will simply be interrupted. If you are calling a remote service, jUPnP will abort the HTTP request to the server.
 * </p>
 * <p>
 * Most likely you want to handle this explicit cancellation of an action call in your action invocation callback, so
 * you can present the result to your user. Override the <code>failure()</code> method to handle the interruption:
 * </p>
 * <p/>
 * <a class="citation" href="javacode://this#invokeActions(LocalDevice)"
 * id="ActionCancellationTest_invokeActions2"
 * style="include: CALLBACK; exclude: TEST;"/>
 * <p>
 * A special exception type is provided if the action call was indeed cancelled.
 * </p>
 * <p>
 * Several important issues have to be considered when you try to cancel action calls to remote services:
 * </p>
 * <p>
 * There is no guarantee that the server will actually stop processing your request. When the client closes the
 * connection, the server doesn't get notified. The server will complete the action call and only fail when trying to
 * return the response to the client on the closed connection. jUPnP's server transports offer a special heartbeat
 * feature for checking client connections, we'll discuss this feature later in this chapter. Other UPnP servers will
 * most likely not detect a dropped client connection immediately.
 * </p>
 * <p>
 * Not all HTTP client transports in jUPnP support interruption of requests:
 * </p>
 * <table class="infotable fullwidth" border="1">
 * <thead>
 * <tr>
 * <th>Transport</th>
 * <th class="thirdwidth">Supports Interruption?</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td class="nowrap">
 * <code>org.jupnp.transport.impl.StreamClientImpl (default)</code>
 * </td>
 * <td>NO</td>
 * </tr>
 * <tr>
 * <td class="nowrap">
 * <code>org.jupnp.transport.impl.apache.StreamClientImpl</code>
 * </td>
 * <td>YES</td>
 * </tr>
 * <tr>
 * <td class="nowrap">
 * <code>org.jupnp.transport.impl.jetty.StreamClientImpl (default on Android)</code>
 * </td>
 * <td>YES</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * Transports which do not support cancellation won't produce an error when you abort an action invocation, they
 * silently ignore the interruption and continue waiting for the server to respond.
 * </p>
 */
class ActionCancellationTest {

    static LocalService bindService(Class<?> clazz) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(clazz);
        svc.setManager(new DefaultServiceManager(svc, clazz));
        return svc;
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] {
                { BinaryLightSampleData.createDevice(bindService(SwitchPowerWithInterruption.class)) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) throws Exception {
        final boolean[] tests = new boolean[1];

        MockUpnpService upnpService = new MockUpnpService(false, false, true);
        upnpService.startup();
        LocalService service = device.findService(new UDAServiceId("SwitchPower"));
        Action action = service.getAction("SetTarget");

        ActionInvocation setTargetInvocation = new ActionInvocation(action);
        setTargetInvocation.setInput("NewTargetValue", true);

        // DOC:CALLBACK
        ActionCallback setTargetCallback = new ActionCallback(setTargetInvocation) {

            @Override
            public void success(ActionInvocation invocation) {
                // Will not be called if invocation has been cancelled
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                if (invocation.getFailure() instanceof ActionCancelledException) {
                    // Handle the cancellation here...
                    tests[0] = true; // DOC:TEST
                }
            }
        };
        // DOC:CALLBACK

        // DOC:EXECUTE_CANCEL
        Future future = upnpService.getControlPoint().execute(setTargetCallback);
        Thread.sleep(500); // DOC:WAIT_FOR_THREAD
        future.cancel(true);
        // DOC:EXECUTE_CANCEL

        Thread.sleep(500);
        for (boolean test : tests) {
            assertTrue(test);
        }
    }
}
