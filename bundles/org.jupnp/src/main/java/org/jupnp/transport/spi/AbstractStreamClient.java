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

package org.jupnp.transport.spi;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the timeout/callback processing and unifies exception handling.

 * @author Christian Bauer
 */
public abstract class AbstractStreamClient<C extends StreamClientConfiguration, REQUEST> implements StreamClient<C> {

    private final Logger log = LoggerFactory.getLogger(StreamClient.class);

    @Override
    public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) throws InterruptedException {
        log.trace("Preparing HTTP request: " + requestMessage);

        REQUEST request = createRequest(requestMessage);
        if (request == null)
            return null;

        Callable<StreamResponseMessage> callable = createCallable(requestMessage, request);

        // We want to track how long it takes
        long start = System.currentTimeMillis();

        // Execute the request on a new thread
        Future<StreamResponseMessage> future =
            getConfiguration().getRequestExecutorService().submit(callable);

        // Wait on the current thread for completion
        try {
            log.trace(
                "Waiting " + getConfiguration().getTimeoutSeconds()
                + " seconds for HTTP request to complete: " + requestMessage
            );
            StreamResponseMessage response =
                future.get(getConfiguration().getTimeoutSeconds(), TimeUnit.SECONDS);

            // Log a warning if it took too long
            long elapsed = System.currentTimeMillis() - start;
            log.trace("Got HTTP response in {} ms: {}", elapsed, requestMessage);
            if (getConfiguration().getLogWarningSeconds() > 0
                    && elapsed > getConfiguration().getLogWarningSeconds() * 1000) {
                log.warn("HTTP request took a long time (" + elapsed + "ms): " + requestMessage);
            }

            return response;

        } catch (InterruptedException ex) {
            log.trace("Interruption, aborting request: " + requestMessage);
            abort(request);
            throw new InterruptedException("HTTP request interrupted and aborted");

        } catch (TimeoutException ex) {

            log.info(
                "Timeout of " + getConfiguration().getTimeoutSeconds()
                + " seconds while waiting for HTTP request to complete, aborting: " + requestMessage
            );
            abort(request);
            return null;

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (!logExecutionException(cause)) {
                String message = "HTTP request failed: " + requestMessage;

                if (log.isDebugEnabled()) {
                    // if debug then the warning will additionally contain the stacktrace of the causing exception
                    log.warn(message, Exceptions.unwrap(cause));
                } else {
                    // compact logging
                    log.warn(message + " (" + Exceptions.unwrap(cause).getMessage() + ")");
                }
            }
            return null;
        } finally {
            onFinally(request);
        }
    }

    /**
     * Create a proprietary representation of this request, log warnings and
     * return <code>null</code> if creation fails.
     */
    abstract protected REQUEST createRequest(StreamRequestMessage requestMessage);

    /**
     * Create a callable procedure that will execute the request.
     */
    abstract protected Callable<StreamResponseMessage> createCallable(StreamRequestMessage requestMessage,
            REQUEST request);

    /**
     * Cancel and abort the request immediately, with the proprietary API.
     */
    abstract protected void abort(REQUEST request);

    /**
     * @return <code>true</code> if no more logging of this exception should be done.
     */
    abstract protected boolean logExecutionException(Throwable t);

    protected void onFinally(REQUEST request) {
        // Do nothing
    }

}
