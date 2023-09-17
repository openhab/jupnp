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

package example.controlpoint;

import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.header.DeviceTypeHeader;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.MANHeader;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.jupnp.model.message.header.UDADeviceTypeHeader;
import org.jupnp.model.message.header.UDAServiceTypeHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.async.SendingSearch;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Searching the network
 * <p>
 * When your control point joins the network it probably won't know any UPnP devices and services that
 * might be available. To learn about the present devices it can broadcast - actually with UDP multicast
 * datagrams - a search message which will be received by every device. Each receiver then inspects the
 * search message and decides if it should reply directly (with notification UDP datagrams) to the
 * sending control point.
 * </p>
 * <p>
 * Search messages carry a <em>search type</em> header and receivers consider this header when they
 * evaluate a potential response. The jUPnP <code>ControlPoint</code> API accepts a
 * <code>UpnpHeader</code> argument when creating outgoing search messages.
 * </p>
 * <a class="citation" href="javadoc://this#searchAll" style="read-title: false;"/>
 * <a class="citation" href="javadoc://this#searchUDN" style="read-title: false;"/>
 * <a class="citation" href="javadoc://this#searchDeviceType" style="read-title: false;"/>
 * <a class="citation" href="javadoc://this#searchServiceType" style="read-title: false;"/>
 */
class SearchExecuteTest {

    /**
     * <p>
     * Most of the time you'd like all devices to respond to your search, this is what the
     * dedicated <code>STAllHeader</code> is used for:
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH"/>
     * <p>
     * Notification messages will be received by your control point and you can listen to
     * the <code>Registry</code> and inspect the found devices and their services. (By the
     * way, if you call <code>search()</code> without any argument, that's the same.)
     * </p>
     */
    @Test
    void searchAll() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        upnpService.getControlPoint().search(       // DOC: SEARCH
                new STAllHeader()
        );                                          // DOC: SEARCH

        assertMessages(upnpService, new STAllHeader());
    }

    /**
     * <p>
     * On the other hand, when you already know the unique device name (UDN) of the device you
     * are searching for - maybe because your control point remembered it while it was turned off - you
     * can send a message which will trigger a response from only a particular device:
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH"/>
     * <p>
     * This is mostly useful to avoid network congestion when dozens of devices might <em>all</em>
     * respond to a search request. Your <code>Registry</code> listener code however still has to
     * inspect each newly found device, as registrations might occur independently from searches.
     * </p>
     */
    @Test
    void searchUDN() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        UDN udn = new UDN(UUID.randomUUID());
        upnpService.getControlPoint().search(       // DOC: SEARCH
                new UDNHeader(udn)
        );                                          // DOC: SEARCH

        assertMessages(upnpService, new UDNHeader(udn));
    }

    /**
     * <p>
     * You can also search by device or service type. This search request will trigger responses
     * from all devices of type "<code>urn:schemas-upnp-org:device:BinaryLight:1</code>":
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH_UDA"/>
     * <p>
     * If the desired device type is of a custom namespace, use this variation:
     * </p>
     * <a class="citation" id="javacode_dt_search_custom" href="javacode://this" style="include: SEARCH_CUSTOM"/>
     */
    @Test
    void searchDeviceType() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        UDADeviceType udaType = new UDADeviceType("BinaryLight");       // DOC: SEARCH_UDA
        upnpService.getControlPoint().search(
                new UDADeviceTypeHeader(udaType)
        );                                                              // DOC: SEARCH_UDA

        assertMessages(upnpService, new UDADeviceTypeHeader(udaType));

        upnpService.getRouter().getOutgoingDatagramMessages().clear();

        DeviceType type = new DeviceType("org-mydomain", "MyDeviceType", 1);    // DOC: SEARCH_CUSTOM
        upnpService.getControlPoint().search(
                new DeviceTypeHeader(type)
        );                                                                      // DOC: SEARCH_CUSTOM

        assertMessages(upnpService, new DeviceTypeHeader(type));
    }

    /**
     * <p>
     * Or, you can search for all devices which implement a particular service type:
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH_UDA"/>
     * <a class="citation" id="javacode_st_search_custom" href="javacode://this" style="include: SEARCH_CUSTOM"/>
     */
    @Test
    void searchServiceType() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        UDAServiceType udaType = new UDAServiceType("SwitchPower");      // DOC: SEARCH_UDA
        upnpService.getControlPoint().search(
                new UDAServiceTypeHeader(udaType)
        );                                                               // DOC: SEARCH_UDA

        assertMessages(upnpService, new UDAServiceTypeHeader(udaType));

        upnpService.getRouter().getOutgoingDatagramMessages().clear();

        ServiceType type = new ServiceType("org-mydomain", "MyServiceType", 1);    // DOC: SEARCH_CUSTOM
        upnpService.getControlPoint().search(
                new ServiceTypeHeader(type)
        );                                                                        // DOC: SEARCH_CUSTOM

        assertMessages(upnpService, new ServiceTypeHeader(type));
    }


    @Test
    void searchRoot() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        upnpService.getControlPoint().search(new RootDeviceHeader());
        assertMessages(upnpService, new RootDeviceHeader());
    }


    @Test
    void searchDefaults() {
        SendingSearch search = new SendingSearch(new MockUpnpService());
        assertEquals(new STAllHeader().getString(), search.getSearchTarget().getString());
    }

    @Test
    void searchInvalidST() {
        assertThrows(IllegalArgumentException.class, () ->
            new SendingSearch(new MockUpnpService(), new MXHeader()));
    }

    protected void assertMessages(MockUpnpService upnpService, UpnpHeader header) {
        assertEquals(3, upnpService.getRouter().getOutgoingDatagramMessages().size());
        for (UpnpMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            assertSearchMessage(msg, header);
        }
    }

    protected void assertSearchMessage(UpnpMessage msg, UpnpHeader searchTarget) {
        assertEquals(new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAN).getString());
        assertEquals(new MXHeader().getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.MX).getString());
        assertEquals(searchTarget.getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString());
        assertEquals(new HostHeader().getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST).getString());
    }
}
