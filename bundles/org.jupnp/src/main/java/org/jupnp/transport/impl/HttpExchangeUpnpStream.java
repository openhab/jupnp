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
package org.jupnp.transport.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Locale;

import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.Exceptions;
import org.jupnp.util.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

/**
 * Default implementation based on the JDK 6.0 built-in HTTP Server.
 * <p>
 * Instantiated by a <code>com.sun.net.httpserver.HttpHandler</code>.
 * </p>
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - refactoring for JUPnP
 */
@SuppressWarnings("restriction")
public abstract class HttpExchangeUpnpStream extends UpnpStream {

    private final Logger log = LoggerFactory.getLogger(HttpExchangeUpnpStream.class.getName());

    private HttpExchange httpExchange;

    public HttpExchangeUpnpStream(ProtocolFactory protocolFactory, HttpExchange httpExchange) {
        super(protocolFactory);
        this.httpExchange = httpExchange;
    }

    public HttpExchange getHttpExchange() {
        return httpExchange;
    }

    @Override
    public void run() {

        try {
            log.trace("Processing HTTP request: {} {}", getHttpExchange().getRequestMethod(),
                    getHttpExchange().getRequestURI());

            // Status
            StreamRequestMessage requestMessage = new StreamRequestMessage(
                    UpnpRequest.Method.getByHttpName(getHttpExchange().getRequestMethod()),
                    getHttpExchange().getRequestURI());

            if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
                log.trace("Method not supported by UPnP stack: {}", getHttpExchange().getRequestMethod());
                throw new RuntimeException("Method not supported: " + getHttpExchange().getRequestMethod());
            }

            // Protocol
            requestMessage.getOperation().setHttpMinorVersion(
                    getHttpExchange().getProtocol().toUpperCase(Locale.ROOT).equals("HTTP/1.1") ? 1 : 0);

            log.trace("Created new request message: {}", requestMessage);

            // Connection wrapper
            requestMessage.setConnection(createConnection());

            // Headers
            requestMessage.setHeaders(new UpnpHeaders(getHttpExchange().getRequestHeaders()));

            // Body
            byte[] bodyBytes;
            InputStream is = null;
            try {
                is = getHttpExchange().getRequestBody();
                bodyBytes = IO.readBytes(is);
            } finally {
                if (is != null)
                    is.close();
            }

            log.trace("Reading request body bytes: {}", bodyBytes.length);

            if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {
                log.trace("Request contains textual entity body, converting then setting string on message");
                requestMessage.setBodyCharacters(bodyBytes);
            } else if (bodyBytes.length > 0) {
                log.trace("Request contains binary entity body, setting bytes on message");
                requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);
            } else {
                log.trace("Request did not contain entity body");
            }

            // Process it
            StreamResponseMessage responseMessage = process(requestMessage);

            // Return the response
            if (responseMessage != null) {
                log.trace("Preparing HTTP response message: {}", responseMessage);

                // Headers
                getHttpExchange().getResponseHeaders().putAll(responseMessage.getHeaders());

                // Body
                byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
                int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

                log.trace("Sending HTTP response message: {} with content length: {}", responseMessage, contentLength);
                getHttpExchange().sendResponseHeaders(responseMessage.getOperation().getStatusCode(), contentLength);

                if (contentLength > 0) {
                    log.trace("Response message has body, writing bytes to stream...");
                    OutputStream os = null;
                    try {
                        os = getHttpExchange().getResponseBody();
                        IO.writeBytes(os, responseBodyBytes);
                        os.flush();
                    } finally {
                        if (os != null)
                            os.close();
                    }
                }

            } else {
                // If it's null, it's 404, everything else needs a proper httpResponse
                log.trace("Sending HTTP response status: {}", HttpURLConnection.HTTP_NOT_FOUND);
                getHttpExchange().sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            }

            responseSent(responseMessage);

        } catch (Exception ex) {

            // You definitely want to catch all Exceptions here, otherwise the server will
            // simply close the socket and you get an "unexpected end of file" on the client.
            // The same is true if you just rethrow an IOException - it is a mystery why it
            // is declared then on the HttpHandler interface if it isn't handled in any
            // way... so we always do error handling here.

            // TODO: We should only send an error if the problem was on our side
            // You don't have to catch Throwable unless, like we do here in unit tests,
            // you might run into Errors as well (assertions).
            log.trace("Exception occured during UPnP stream processing: {}", ex);
            if (log.isTraceEnabled()) {
                log.trace("Cause: {}", Exceptions.unwrap(ex), Exceptions.unwrap(ex));
            }
            try {
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            } catch (IOException ioe) {
                log.warn("Couldn't send error response: {}", ioe.getMessage(), ioe);
            }

            responseException(ex);
        }
    }

    protected abstract Connection createConnection();
}
