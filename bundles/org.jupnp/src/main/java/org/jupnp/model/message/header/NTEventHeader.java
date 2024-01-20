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

package org.jupnp.model.message.header;

import java.util.Locale;

/**
 * @author Christian Bauer
 */
public class NTEventHeader extends UpnpHeader<String> {

    public NTEventHeader() {
        setValue("upnp:event");
    }

    public void setString(String s) throws InvalidHeaderException {
        if (!s.toLowerCase(Locale.ENGLISH).equals(getValue())) {
            throw new InvalidHeaderException("Invalid event NT header value: " + s);
        }
    }

    public String getString() {
        return getValue();
    }
}
