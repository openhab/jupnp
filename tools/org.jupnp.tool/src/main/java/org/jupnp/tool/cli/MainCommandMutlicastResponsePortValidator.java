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
package org.jupnp.tool.cli;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class MainCommandMutlicastResponsePortValidator implements IParameterValidator {

    private static final String ERROR_MSG = "Parameter --multicastResponsePort ";

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (name.equals("--multicastResponsePort")) {
            try {
                int port = Integer.parseInt(value);
                if ((port < 0) || (port > 65535)) {
                    throw new ParameterException(ERROR_MSG + "must be between 0..65535");
                }
            } catch (NumberFormatException ex) {
                // must be valid port number
                throw new ParameterException(ERROR_MSG + " is not a valid number)");
            }
        }
    }
}
