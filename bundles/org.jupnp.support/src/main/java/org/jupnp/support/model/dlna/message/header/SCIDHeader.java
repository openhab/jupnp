/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.support.model.dlna.message.header;

import org.jupnp.model.message.header.InvalidHeaderException;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class SCIDHeader extends DLNAHeader<String> {

    public SCIDHeader() {
        setValue("");
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty()) {
            setValue(s);
            return;
        }
        throw new InvalidHeaderException("Invalid SCID header value: " + s);
    }

    @Override
    public String getString() {
        return getValue();
    }
}
