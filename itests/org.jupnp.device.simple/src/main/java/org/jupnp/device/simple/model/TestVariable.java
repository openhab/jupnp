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
package org.jupnp.device.simple.model;

import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVariable extends Observable {
    private static Logger log = LoggerFactory.getLogger(TestVariable.class);
    private Object value;

    public TestVariable(Object value) {
        setValue(value);
    }

    public void setValue(Object value) {
        log.trace("ENTRY {}.{}: {}", this.getClass().getName(), "setValue", value);
        if (this.value == null || !this.value.equals(value)) {
            log.trace("old: {} new: {}", this.value, value);

            this.value = value;
            setChanged();
            notifyObservers(this);
        }
    }

    public Object getValue() {
        return value;
    }
}
