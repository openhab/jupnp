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

package org.jupnp.support.messagebox;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.support.messagebox.model.Message;
import org.jupnp.util.MimeType;

/**
 * @author Christian Bauer
 */
public abstract class AddMessage extends ActionCallback {

    final protected MimeType mimeType = MimeType.valueOf("text/xml;charset=\"utf-8\"");

    public AddMessage(Service service, Message message) {
        super(new ActionInvocation(service.getAction("AddMessage")));

        getActionInvocation().setInput("MessageID", Integer.toString(message.getId()));
        getActionInvocation().setInput("MessageType", mimeType.toString());
        getActionInvocation().setInput("Message", message.toString());
    }

}
