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

package org.jupnp.transport.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.servlet.AsyncContext;
import org.jupnp.servlet.AsyncEvent;
import org.jupnp.servlet.AsyncListener;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.Exceptions;
import org.jupnp.util.io.IO;

/**
 * Implementation based on Servlet 3.0 API.
 * <p>
 * Concrete implementations must provide a connection wrapper, as this wrapper most likely has
 * to access proprietary APIs to implement connection checking.
 * </p>
 *
 * @author Christian Bauer
 * @author Michael Grammling - Refactored so that it can also work with Servlet 2.5
 */
public abstract class AsyncServletUpnpStream extends UpnpStream implements AsyncListener {

    final private static Logger log = Logger.getLogger(UpnpStream.class.getName());

    final protected AsyncContext asyncContext;
    final protected HttpServletRequest request;

    protected StreamResponseMessage responseMessage;

    public AsyncServletUpnpStream(ProtocolFactory protocolFactory,
                                  AsyncContext asyncContext,
                                  HttpServletRequest request) {
        super(protocolFactory);
        this.asyncContext = asyncContext;
        this.request = request;
        asyncContext.addListener(this);
    }

    protected HttpServletRequest getRequest() {
        return request;
    }

    protected HttpServletResponse getResponse() {
        ServletResponse response;
        if ((response = asyncContext.getResponse()) == null) {
            throw new IllegalStateException(
                "Couldn't get response from asynchronous context, already timed out"
            );
        }
        return (HttpServletResponse) response;
    }

    protected void complete() {
        try {
            asyncContext.complete();
        } catch (IllegalStateException ex) {
            // If Jetty's connection, for whatever reason, is in an illegal state, this will be thrown
            // and we can "probably" ignore it. The request is complete, no matter how it ended.
            log.info("Error calling servlet container's AsyncContext#complete() method: " + ex);
        }
    }

    @Override
    public void run() {
        try {
            StreamRequestMessage requestMessage = readRequestMessage();
            if (log.isLoggable(Level.FINER))
                log.finer("Processing new request message: " + requestMessage);

            responseMessage = process(requestMessage);

            if (responseMessage != null) {
                if (log.isLoggable(Level.FINER))
                    log.finer("Preparing HTTP response message: " + responseMessage);
                writeResponseMessage(responseMessage);
            } else {
                // If it's null, it's 404
                if (log.isLoggable(Level.FINER))
                    log.finer("Sending HTTP response status: " + HttpURLConnection.HTTP_NOT_FOUND);
                getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Throwable t) {
            log.info("Exception occurred during UPnP stream processing: " + t);
            if (log.isLoggable(Level.FINER)) {
                log.log(Level.FINER, "Cause: " + Exceptions.unwrap(t), Exceptions.unwrap(t));
            }
            if (!getResponse().isCommitted()) {
                log.finer("Response hasn't been committed, returning INTERNAL SERVER ERROR to client");
                getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                log.info("Could not return INTERNAL SERVER ERROR to client, response was already committed");
            }
            responseException(t);
        } finally {
            complete();
        }
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        // Call not needed, it will only be called on request.startAsync() which
        // then immediately removes the listener.
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        if (log.isLoggable(Level.FINER))
            log.finer("Completed asynchronous processing of HTTP request: " + event.getSuppliedRequest());
        responseSent(responseMessage);
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        if (log.isLoggable(Level.FINER))
            log.finer("Asynchronous processing of HTTP request timed out: " + event.getSuppliedRequest());
        responseException(new Exception("Asynchronous request timed out"));
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        if (log.isLoggable(Level.FINER))
            log.finer("Asynchronous processing of HTTP request error: " + event.getThrowable());
        responseException(event.getThrowable());
    }

    protected StreamRequestMessage readRequestMessage() throws IOException {
        // Extract what we need from the HTTP httpRequest
        String requestMethod = getRequest().getMethod();
        String requestURI = getRequest().getRequestURI();

        if (log.isLoggable(Level.FINER))
            log.finer("Processing HTTP request: " + requestMethod + " " + requestURI);

        StreamRequestMessage requestMessage;
        try {
            requestMessage =
                new StreamRequestMessage(
                    UpnpRequest.Method.getByHttpName(requestMethod),
                    URI.create(requestURI)
                );
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid request URI: " + requestURI, ex);
        }

        if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
            throw new RuntimeException("Method not supported: " + requestMethod);
        }

        // Connection wrapper
        requestMessage.setConnection(createConnection());

        // Headers
        UpnpHeaders headers = new UpnpHeaders();
        Enumeration<String> headerNames = getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = getRequest().getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                headers.add(headerName, headerValue);
            }
        }
        requestMessage.setHeaders(headers);

        // Body
        byte[] bodyBytes;
        InputStream is = null;
        try {
            is = getRequest().getInputStream();
            bodyBytes = IO.readBytes(is);
        } finally {
            if (is != null)
                is.close();
        }
        if (log.isLoggable(Level.FINER))
            log.finer("Reading request body bytes: " + bodyBytes.length);

        if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {

            if (log.isLoggable(Level.FINER))
                log.finer("Request contains textual entity body, converting then setting string on message");
            requestMessage.setBodyCharacters(bodyBytes);

        } else if (bodyBytes.length > 0) {

            if (log.isLoggable(Level.FINER))
                log.finer("Request contains binary entity body, setting bytes on message");
            requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);

        } else {
            if (log.isLoggable(Level.FINER))
                log.finer("Request did not contain entity body");
        }

        return requestMessage;
    }

    protected void writeResponseMessage(StreamResponseMessage responseMessage) throws IOException {
        if (log.isLoggable(Level.FINER))
            log.finer("Sending HTTP response status: " + responseMessage.getOperation().getStatusCode());

        getResponse().setStatus(responseMessage.getOperation().getStatusCode());

        // Headers
        for (Map.Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                getResponse().addHeader(entry.getKey(), value);
            }
        }
        // The Date header is recommended in UDA
        getResponse().setDateHeader("Date", System.currentTimeMillis());

        // Body
        byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
        int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

        if (contentLength > 0) {
            getResponse().setContentLength(contentLength);
            log.finer("Response message has body, writing bytes to stream...");
            IO.writeBytes(getResponse().getOutputStream(), responseBodyBytes);
        }
    }

    abstract protected Connection createConnection();

}
