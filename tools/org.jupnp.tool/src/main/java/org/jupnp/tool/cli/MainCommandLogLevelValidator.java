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
public class MainCommandLogLevelValidator implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException {
        if (name.equals("--loglevel")) {
            if ((value.equalsIgnoreCase("TRACE")) || (value.equalsIgnoreCase("DEBUG"))
                    || (value.equalsIgnoreCase("INFO")) || (value.equalsIgnoreCase("WARN"))
                    || (value.equalsIgnoreCase("ERROR")) || (value.equalsIgnoreCase("OFF"))) {
            } else {
                throw new ParameterException(
                        "Parameter " + name + " must be {OFF|ERROR|WARN|INFO|DEBUG|TRACE} (found " + value + ")");
            }
        }
    }
}
