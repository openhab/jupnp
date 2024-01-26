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
package org.jupnp.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Christian Bauer
 */
public class IO {

    public static String readLines(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream is null");
        }

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(is));

        StringBuilder input = new StringBuilder();
        String inputLine;
        while ((inputLine = inputReader.readLine()) != null) {
            input.append(inputLine).append(System.lineSeparator());
        }

        return input.length() > 0 ? input.toString() : "";
    }
}
