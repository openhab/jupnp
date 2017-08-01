/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.support.connectionmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.ServiceReference;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.model.types.csv.CSVUnsignedIntegerFourBytes;
import org.jupnp.support.model.ConnectionInfo;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.ProtocolInfos;

/**
 * Base for connection management, implements the connection ID "0" behavior.
 *
 * @author Christian Bauer
 * @author Alessio Gaeta
 */
@UpnpService(
        serviceId = @UpnpServiceId("ConnectionManager"),
        serviceType = @UpnpServiceType(value = "ConnectionManager", version = 1),
        stringConvertibleTypes = {ProtocolInfo.class, ProtocolInfos.class, ServiceReference.class}
)
@UpnpStateVariables({
        @UpnpStateVariable(name = "SourceProtocolInfo", datatype = "string"),
        @UpnpStateVariable(name = "SinkProtocolInfo", datatype = "string"),
        @UpnpStateVariable(name = "CurrentConnectionIDs", datatype = "string"),
        @UpnpStateVariable(name = "A_ARG_TYPE_ConnectionStatus", allowedValuesEnum = ConnectionInfo.Status.class, sendEvents = false),
        @UpnpStateVariable(name = "A_ARG_TYPE_ConnectionManager", datatype = "string", sendEvents = false),
        @UpnpStateVariable(name = "A_ARG_TYPE_Direction", allowedValuesEnum = ConnectionInfo.Direction.class, sendEvents = false),
        @UpnpStateVariable(name = "A_ARG_TYPE_ProtocolInfo", datatype = "string", sendEvents = false),
        @UpnpStateVariable(name = "A_ARG_TYPE_ConnectionID", datatype = "i4", sendEvents = false),
        @UpnpStateVariable(name = "A_ARG_TYPE_AVTransportID", datatype = "i4", sendEvents = false),
        @UpnpStateVariable(name = "A_ARG_TYPE_RcsID", datatype = "i4", sendEvents = false)
})
public class ConnectionManagerService {

    final private static Logger log = Logger.getLogger(ConnectionManagerService.class.getName());

    final protected PropertyChangeSupport propertyChangeSupport;
    final protected Map<Integer, ConnectionInfo> activeConnections = new ConcurrentHashMap();
    final protected ProtocolInfos sourceProtocolInfo;
    final protected ProtocolInfos sinkProtocolInfo;

    /**
     * Creates a default "active" connection with identifier "0".
     */
    public ConnectionManagerService() {
        this(new ConnectionInfo());
    }

    /**
     * Creates a default "active" connection with identifier "0".
     */
    public ConnectionManagerService(ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo) {
        this(sourceProtocolInfo, sinkProtocolInfo, new ConnectionInfo());
    }

    public ConnectionManagerService(ConnectionInfo... activeConnections) {
        this(null, new ProtocolInfos(), new ProtocolInfos(), activeConnections);
    }

    public ConnectionManagerService(ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo, ConnectionInfo... activeConnections) {
        this(null, sourceProtocolInfo, sinkProtocolInfo, activeConnections);
    }

    public ConnectionManagerService(PropertyChangeSupport propertyChangeSupport,
                                            ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo,
                                            ConnectionInfo... activeConnections) {
        this.propertyChangeSupport =
                propertyChangeSupport == null
                        ? new PropertyChangeSupport(this) : propertyChangeSupport;

        this.sourceProtocolInfo = sourceProtocolInfo;
        this.sinkProtocolInfo = sinkProtocolInfo;

        for (ConnectionInfo activeConnection : activeConnections) {
            this.activeConnections.put(activeConnection.getConnectionID(), activeConnection);
        }
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "RcsID", getterName = "getRcsID"),
            @UpnpOutputArgument(name = "AVTransportID", getterName = "getAvTransportID"),
            @UpnpOutputArgument(name = "ProtocolInfo", getterName = "getProtocolInfo"),
            @UpnpOutputArgument(name = "PeerConnectionManager", stateVariable = "A_ARG_TYPE_ConnectionManager", getterName = "getPeerConnectionManager"),
            @UpnpOutputArgument(name = "PeerConnectionID", stateVariable = "A_ARG_TYPE_ConnectionID", getterName = "getPeerConnectionID"),
            @UpnpOutputArgument(name = "Direction", getterName = "getDirection"),
            @UpnpOutputArgument(name = "Status", stateVariable = "A_ARG_TYPE_ConnectionStatus", getterName = "getConnectionStatus")
    })
    synchronized public ConnectionInfo getCurrentConnectionInfo(@UpnpInputArgument(name = "ConnectionID") int connectionId)
            throws ActionException {
        log.fine("Getting connection information of connection ID: " + connectionId);
        ConnectionInfo info;
        if ((info = activeConnections.get(connectionId)) == null) {
            throw new ConnectionManagerException(
                    ConnectionManagerErrorCode.INVALID_CONNECTION_REFERENCE,
                    "Non-active connection ID: " + connectionId
            );
        }
        return info;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "ConnectionIDs")
    })
    synchronized public CSV<UnsignedIntegerFourBytes> getCurrentConnectionIDs() {
        CSV<UnsignedIntegerFourBytes> csv = new CSVUnsignedIntegerFourBytes();
        for (Integer connectionID : activeConnections.keySet()) {
            csv.add(new UnsignedIntegerFourBytes(connectionID));
        }
        log.fine("Returning current connection IDs: " + csv.size());
        return csv;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "Source", stateVariable = "SourceProtocolInfo", getterName = "getSourceProtocolInfo"),
            @UpnpOutputArgument(name = "Sink", stateVariable = "SinkProtocolInfo", getterName = "getSinkProtocolInfo")
    })
    synchronized public void getProtocolInfo() throws ActionException {
        // NOOP
    }

    synchronized public ProtocolInfos getSourceProtocolInfo() {
        return sourceProtocolInfo;
    }

    synchronized public ProtocolInfos getSinkProtocolInfo() {
        return sinkProtocolInfo;
    }
}
