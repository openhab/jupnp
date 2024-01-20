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

package org.jupnp.util;

/**
 * @author Christian Bauer
 */
public class ByteArray {

    public static byte[] toPrimitive(Byte[] array) {
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = array[i];
        }
        return bytes;
    }

    public static Byte[] toWrapper(byte[] array) {
        Byte[] wrappers = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            wrappers[i] = array[i];
        }
        return wrappers;
    }
}
