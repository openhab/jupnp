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

package org.jupnp.transport.impl.jetty;

import org.jupnp.transport.spi.AbstractStreamClientConfiguration;

import java.util.concurrent.ExecutorService;

/**
 * Settings for the Jetty 9.3.x implementation.
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - add option for buffer size
 */
public class StreamClientConfigurationImpl extends AbstractStreamClientConfiguration {

    public StreamClientConfigurationImpl(ExecutorService timeoutExecutorService) {
        super(timeoutExecutorService);
    }

    public StreamClientConfigurationImpl(ExecutorService timeoutExecutorService, int timeoutSeconds) {
        super(timeoutExecutorService, timeoutSeconds);
    }

    /**
     * @return By default <code>0</code>.
     */
    public int getRequestRetryCount() {
        return 0;
    }

    /**
     * Note: leaving this to the default value of {@code -1} will let the HTTP client use its defaults.
     *
     * @return By default <code>-1</code>, change to change buffer size)
     */
    public int getSocketBufferSize() {
        return -1;
    }

}
