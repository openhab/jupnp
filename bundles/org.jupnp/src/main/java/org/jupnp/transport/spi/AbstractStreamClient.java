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

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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
 * 
 * @author Christian Bauer
 */
public abstract class AbstractStreamClient<C extends StreamClientConfiguration, REQUEST> implements StreamClient<C> {

    private final Logger log = LoggerFactory.getLogger(StreamClient.class);

    private static final int FAILED_REQUESTS_MAX_SIZE = 100;
    private Map<URI, Long> failedRequests = new ConcurrentHashMap<URI, Long>();

    @Override
    public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) throws InterruptedException {
        log.trace("Preparing HTTP request: " + requestMessage);

        // We want to track how long it takes
        long start = System.nanoTime();

        final Long previeousFailureTime = failedRequests.get(requestMessage.getUri());
        if (getConfiguration().getRetryAfterSeconds() > 0 && previeousFailureTime != null) {
            if (start - previeousFailureTime < TimeUnit.SECONDS
                    .toNanos(getConfiguration().getRetryAfterSeconds())) {
                log.debug("Will not attempt request because it failed in the last {} seconds: {}",
                        getConfiguration().getRetryAfterSeconds(), requestMessage);
                return null;
            } else {
                failedRequests.remove(requestMessage.getUri());
            }
        }

        REQUEST request = createRequest(requestMessage);
        if (request == null)
            return null;

        Callable<StreamResponseMessage> callable = createCallable(requestMessage, request);
        RequestWrapper requestWrapper = new RequestWrapper(callable);

        // Execute the request on a new thread
        Future<StreamResponseMessage> future = getConfiguration().getRequestExecutorService().submit(requestWrapper);

        // Wait on the current thread for completion
        try {
            log.trace("Waiting {} seconds for HTTP request to complete: {}", getConfiguration().getTimeoutSeconds(), requestMessage);
            StreamResponseMessage response = future.get(getConfiguration().getTimeoutSeconds(), TimeUnit.SECONDS);

            // Log a warning if it took too long
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.trace("Got HTTP response in {} ms: {}", elapsed, requestMessage);
            if (getConfiguration().getLogWarningSeconds() > 0
                    && elapsed > TimeUnit.SECONDS.toMillis(getConfiguration().getLogWarningSeconds())) {
                log.warn("HTTP request took a long time ({} ms): {}", elapsed, requestMessage);
            }

            return response;

        } catch (InterruptedException ex) {
            log.trace("Interruption, aborting request: " + requestMessage);
            abort(request);
            throw new InterruptedException("HTTP request interrupted and aborted");

        } catch (TimeoutException ex) {

            log.info("Timeout of {} seconds while waiting for HTTP request to complete, aborting: {}",
                    getConfiguration().getTimeoutSeconds(), requestMessage);
            abort(request);

            handleRequestTimeout(requestMessage, requestWrapper);
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

            handleRequestFailure(requestMessage);
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

    private void handleRequestFailure(StreamRequestMessage requestMessage) {
        if (getConfiguration().getRetryAfterSeconds() <= 0) {
            return;
        }

        final long currentTime = System.nanoTime();
        failedRequests.put(requestMessage.getUri(), currentTime);

        if (failedRequests.size() > FAILED_REQUESTS_MAX_SIZE) {
            cleanOldFailedRequests(currentTime);
        }
    }

    private void handleRequestTimeout(StreamRequestMessage requestMessage, RequestWrapper requestWrapper) {
        if (getConfiguration().getRetryAfterSeconds() <= 0) {
            return;
        }

        final long currentTime = System.nanoTime();
        if (requestWrapper.startTime != null && currentTime - requestWrapper.startTime > TimeUnit.SECONDS
                .toNanos(getConfiguration().getTimeoutSeconds())) {
            failedRequests.put(requestMessage.getUri(), currentTime);
        }

        cleanOldFailedRequests(currentTime);
    }

    private void cleanOldFailedRequests(long currentTime) {
        if (failedRequests.size() <= FAILED_REQUESTS_MAX_SIZE) {
            return;
        }

        Iterator<Map.Entry<URI, Long>> it = failedRequests.entrySet().iterator();
        while (it.hasNext()) {
            Long elapsedTime = currentTime - it.next().getValue();
            if (elapsedTime > TimeUnit.SECONDS.toNanos(getConfiguration().getRetryAfterSeconds())) {
                it.remove();
            }
        }
    }

    // Wrap the Callables to track if execution started or if it timed out while waiting in the executor queue 
    private static class RequestWrapper implements Callable<StreamResponseMessage> {

        Callable<StreamResponseMessage> task;
        Long startTime = null;

        public RequestWrapper(Callable<StreamResponseMessage> task) {
            this.task = task;
        }

        @Override
        public StreamResponseMessage call() throws Exception {
            startTime = System.nanoTime();
            return task.call();
        }

    }

}
