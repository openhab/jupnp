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

package org.jupnp.support.avtransport.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class Play extends ActionCallback {

    private final Logger logger = LoggerFactory.getLogger(Play.class);

    public Play(Service<?, ?> service) {
        this(new UnsignedIntegerFourBytes(0), service, "1");
    }

    public Play(Service<?, ?> service, String speed) {
        this(new UnsignedIntegerFourBytes(0), service, speed);
    }

    public Play(UnsignedIntegerFourBytes instanceId, Service<?, ?> service) {
        this(instanceId, service, "1");
    }

    public Play(UnsignedIntegerFourBytes instanceId, Service<?, ?> service, String speed) {
        super(new ActionInvocation<>(service.getAction("Play")));
        getActionInvocation().setInput("InstanceID", instanceId);
        getActionInvocation().setInput("Speed", speed);
    }

    @Override
    public void success(ActionInvocation invocation) {
        logger.debug("Execution successful");
    }
}
