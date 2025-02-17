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
package org.jupnp.support.model.dlna;

/**
 * DLNA.ORG_OP: operations parameter (string)
 *
 * <pre>
 *     "00" (or "0") neither time seek range nor range supported
 *     "01" range supported
 *     "10" time seek range supported
 *     "11" both time seek range and range supported
 * </pre>
 *
 * @author Mario Franco
 */
public enum DLNAOperations {

    NONE(0x00),
    RANGE(0x01),
    TIMESEEK(0x10);

    private final int code;

    DLNAOperations(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static DLNAOperations valueOf(int code) {
        for (DLNAOperations errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return null;
    }
}
