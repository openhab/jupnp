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
package org.jupnp.model.message.header;

/**
 * @author Christian Bauer
 */
public class MXHeader extends UpnpHeader<Integer> {

    // 3 second seems like a good default to spread search responses (UDA says 120?!? wtf)
    public static final Integer DEFAULT_VALUE = 3;

    /**
     * Defaults to 3 seconds.
     */
    public MXHeader() {
        setValue(DEFAULT_VALUE);
    }

    public MXHeader(Integer delayInSeconds) {
        setValue(delayInSeconds);
    }

    public void setString(String s) throws InvalidHeaderException {
        Integer value;
        try {
            value = Integer.parseInt(s);
        } catch (Exception ex) {
            throw new InvalidHeaderException("Can't parse MX seconds integer from: " + s, ex);
        }

        // UDA 1.0, section 1.2.3: "If the MX header specifies a value greater than 120, the device
        // should assume that it contained the value 120 or less."
        if (value < 0 || value > 120) {
            setValue(DEFAULT_VALUE);
        } else {
            setValue(value);
        }
    }

    public String getString() {
        return getValue().toString();
    }
}
