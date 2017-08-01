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

package org.jupnp.support.igd.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.support.model.PortMapping;

/**
 * @author Christian Bauer
 */
public abstract class PortMappingDelete extends ActionCallback {

    final protected PortMapping portMapping;

    public PortMappingDelete(Service service, PortMapping portMapping) {
        this(service, null, portMapping);
    }

    protected PortMappingDelete(Service service, ControlPoint controlPoint, PortMapping portMapping) {
        super(new ActionInvocation(service.getAction("DeletePortMapping")), controlPoint);

        this.portMapping = portMapping;

        getActionInvocation().setInput("NewExternalPort", portMapping.getExternalPort());
        getActionInvocation().setInput("NewProtocol", portMapping.getProtocol());
        if (portMapping.hasRemoteHost())
            getActionInvocation().setInput("NewRemoteHost", portMapping.getRemoteHost());

    }

}
