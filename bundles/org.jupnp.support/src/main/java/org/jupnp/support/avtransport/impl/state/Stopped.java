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

package org.jupnp.support.avtransport.impl.state;

import java.net.URI;

import org.jupnp.support.avtransport.lastchange.AVTransportVariable;
import org.jupnp.support.model.AVTransport;
import org.jupnp.support.model.SeekMode;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class Stopped<T extends AVTransport> extends AbstractState<T> {

    private final Logger logger = LoggerFactory.getLogger(Stopped.class);

    public Stopped(T transport) {
        super(transport);
    }

    public void onEntry() {
        logger.debug("Setting transport state to STOPPED");
        getTransport().setTransportInfo(
                new TransportInfo(TransportState.STOPPED, getTransport().getTransportInfo().getCurrentTransportStatus(),
                        getTransport().getTransportInfo().getCurrentSpeed()));
        getTransport().getLastChange().setEventedValue(getTransport().getInstanceId(),
                new AVTransportVariable.TransportState(TransportState.STOPPED),
                new AVTransportVariable.CurrentTransportActions(getCurrentTransportActions()));
    }

    public abstract Class<? extends AbstractState<?>> setTransportURI(URI uri, String metaData);

    public abstract Class<? extends AbstractState<?>> stop();

    public abstract Class<? extends AbstractState<?>> play(String speed);

    public abstract Class<? extends AbstractState<?>> next();

    public abstract Class<? extends AbstractState<?>> previous();

    public abstract Class<? extends AbstractState<?>> seek(SeekMode unit, String target);

    public TransportAction[] getCurrentTransportActions() {
        return new TransportAction[] { TransportAction.Stop, TransportAction.Play, TransportAction.Next,
                TransportAction.Previous, TransportAction.Seek };
    }
}
