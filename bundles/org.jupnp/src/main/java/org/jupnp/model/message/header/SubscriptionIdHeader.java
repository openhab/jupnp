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

/**
 * @author Christian Bauer
 */
public class SubscriptionIdHeader extends UpnpHeader<String> {

    public static final String PREFIX = "uuid:";

    public SubscriptionIdHeader() {
    }

    public SubscriptionIdHeader(String value) {
        setValue(value);
    }

    public void setString(String s) throws InvalidHeaderException {
        if (!s.startsWith(PREFIX)) {
            throw new InvalidHeaderException(
                    "Invalid subscription ID header value, must start with '" + PREFIX + "': " + s);
        }
        setValue(s);
    }

    public String getString() {
        return getValue();
    }
}
