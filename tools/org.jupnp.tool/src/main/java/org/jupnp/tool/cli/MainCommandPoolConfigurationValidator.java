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

package org.jupnp.tool.cli;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class MainCommandPoolConfigurationValidator implements IParameterValidator {

	private final static String ERROR_MSG = "Paramer --pool must be of format "
			+ "'<mainPoolSize>,<asyncPoolSize>[,stats]'";

	public void validate(String name, String value) throws ParameterException {
		if (name.equals("--pool")) {
			// pool config is sth like "20,20,stats"
			StringTokenizer tokenizer = new StringTokenizer(value, ",");
			// must have 2..3 args
			if ((tokenizer.countTokens() < 2) || (tokenizer.countTokens() > 3)) {
				throw new ParameterException(ERROR_MSG + " (not 2 or 3 parameters)");
			} else {
				try {
					int mainPoolSize = Integer.parseInt(tokenizer.nextToken());
					int asyncPoolSize = Integer.parseInt(tokenizer.nextToken());

					// all >0
					if ((mainPoolSize <= 0) || (asyncPoolSize <= 0)) {
						throw new ParameterException(ERROR_MSG + " (all values must be greater than 0)");
					}
					// one token left?
					if (tokenizer.countTokens() == 1) {
						String option = tokenizer.nextToken();
						if (!CommandLineArgs.POOL_CONFIG_STATS_OPTION.equalsIgnoreCase(option)) {
							throw new ParameterException(ERROR_MSG + " (only "
									+ CommandLineArgs.POOL_CONFIG_STATS_OPTION + " allowed as last option)");
						}
					}
					// all fine otherwise
				} catch (NoSuchElementException ex) {
					// hmm, not enough tokens, should never happen
					throw new ParameterException(ERROR_MSG + " (not enough arguments)");
				} catch (NumberFormatException ex) {
					// must be valid numbers
					throw new ParameterException(ERROR_MSG + " (numbers wrong)");
				}
			}
		}
	}
}
