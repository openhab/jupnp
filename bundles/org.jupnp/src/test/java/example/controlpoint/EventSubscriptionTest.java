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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.BooleanDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.Reflections;

import example.binarylight.BinaryLightSampleData;
import example.binarylight.SwitchPower;

/**
 * Receiving events from services
 * <p>
 * The UPnP specification defines a general event notification architecture (GENA) which is based
 * on a publish/subscribe paradigm. Your control point subscribes with a service in order to receive
 * events. When the service state changes, an event message will be delivered to the callback
 * of your control point. Subscriptions are periodically refreshed until you unsubscribe from
 * the service. If you do not unsubscribe and if a refresh of the subscription fails, maybe
 * because the control point was turned off without proper shutdown, the subscription will
 * timeout on the publishing service's side.
 * </p>
 * <p>
 * This is an example subscription on a service that sends events for a state variable named
 * <code>Status</code> (e.g. the previously shown <a href="#section.SwitchPower">SwitchPower</a>
 * service). The subscription's refresh and timeout period is 600 seconds:
 * </p>
 * <a class="citation" href="javacode://this#subscriptionLifecycle" style="include: SUBSCRIBE; exclude: EXC1, EXC2,
 * EXC3, EXC4, EXC5;"/>
 * <p>
 * The <code>SubscriptionCallback</code> offers the methods <code>failed()</code>,
 * <code>established()</code>, and <code>ended()</code> which are called during a subscription's lifecycle.
 * When a subscription ends you will be notified with a <code>CancelReason</code> whenever the termination
 * of the subscription was irregular. See the Javadoc of these methods for more details.
 * </p>
 * <p>
 * Every event message from the service will be passed to the <code>eventReceived()</code> method,
 * and every message will carry a sequence number. You can access the changed state variable values
 * in this method, note that only state variables which changed are included in the event messages.
 * A special event message called the "initial event" will be send by the service once, when you
 * subscribe. This message contains values for <em>all</em> evented state variables of the service;
 * you'll receive an initial snapshot of the state of the service at subscription time.
 * </p>
 * <p>
 * Whenever the receiving UPnP stack detects an event message that is out of sequence, e.g. because
 * some messages were lost during transport, the <code>eventsMissed()</code> method will be called
 * before you receive the event. You then decide if missing events is important for the correct
 * behavior of your application, or if you can silently ignore it and continue processing events
 * with non-consecutive sequence numbers.
 * </p>
 * <p>
 * You can optionally override the <code>invalidMessage()</code> method and react to message parsing
 * errors, if your subscription is with a remote service. Most of the time all you can do here is
 * log or report an error to developers, so they can work around the broken remote service (UPnP
 * interoperability is frequently very poor).
 * </p>
 * <p>
 * You end a subscription regularly by calling <code>callback.end()</code>, which will unsubscribe
 * your control point from the service.
 * </p>
 */
public class EventSubscriptionTest {

    @Test
    void subscriptionLifecycle() throws Exception {

        MockUpnpService upnpService = createMockUpnpService();

        final List<Boolean> testAssertions = new ArrayList<>();

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPower.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = device.getServices()[0];

        SubscriptionCallback callback = new SubscriptionCallback(service, 600) { // DOC: SUBSCRIBE

            @Override
            public void established(GENASubscription sub) {
                System.out.println("Established: " + sub.getSubscriptionId());
                testAssertions.add(true); // DOC: EXC2
            }

            @Override
            protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
                    String defaultMsg) {
                System.err.println(defaultMsg);
                testAssertions.add(false); // DOC: EXC1
            }

            @Override
            public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
                assertNull(reason);
                assertNotNull(sub); // DOC: EXC3
                assertNull(response);
                testAssertions.add(true); // DOC: EXC3
            }

            @Override
            public void eventReceived(GENASubscription sub) {

                System.out.println("Event: " + sub.getCurrentSequence().getValue());

                Map<String, StateVariableValue> values = sub.getCurrentValues();
                StateVariableValue status = values.get("Status");

                assertEquals(BooleanDatatype.class, status.getDatatype().getClass());
                assertEquals(Datatype.Builtin.BOOLEAN, status.getDatatype().getBuiltin());

                System.out.println("Status is: " + status);

                if (sub.getCurrentSequence().getValue() == 0) { // DOC: EXC4
                    assertEquals("0", sub.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else if (sub.getCurrentSequence().getValue() == 1) {
                    assertEquals("1", sub.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                } // DOC: EXC4
            }

            @Override
            public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                System.out.println("Missed events: " + numberOfMissedEvents);
                testAssertions.add(false); // DOC: EXC5
            }

            @Override
            protected void invalidMessage(RemoteGENASubscription sub, UnsupportedDataException e) {
                // Log/send an error report?
            }
        };

        upnpService.getControlPoint().execute(callback); // DOC: SUBSCRIBE

        // Modify the state of the service and trigger event
        Object serviceImpl = service.getManager().getImplementation();
        Reflections.set(Reflections.getField(serviceImpl.getClass(), "status"), serviceImpl, true);
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);

        assertEquals(2L, callback.getSubscription().getCurrentSequence().getValue()); // It's the NEXT sequence!
        assertTrue(callback.getSubscription().getSubscriptionId().startsWith("uuid:"));

        // Actually, the local subscription we are testing here has an "unlimited" duration
        assertEquals(Integer.MAX_VALUE, callback.getSubscription().getActualDurationSeconds());

        callback.end();

        assertEquals(4, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    protected MockUpnpService createMockUpnpService() {
        MockUpnpService mock = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { createSubscribeResponseMessage(),
                                createUnsubscribeResponseMessage() };
                    }
                };
            }
        };
        mock.startup();
        return mock;
    }

    protected StreamResponseMessage createSubscribeResponseMessage() {
        StreamResponseMessage msg = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        msg.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader("uuid:1234"));
        msg.getHeaders().add(UpnpHeader.Type.TIMEOUT, new TimeoutHeader(180));
        return msg;
    }

    protected StreamResponseMessage createUnsubscribeResponseMessage() {
        return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
    }
}
