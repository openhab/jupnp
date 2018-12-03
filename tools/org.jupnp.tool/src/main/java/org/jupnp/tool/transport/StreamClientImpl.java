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

package org.jupnp.tool.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.http.Headers;
import org.jupnp.util.Exceptions;
import org.jupnp.util.URIUtil;
import org.jupnp.util.io.IO;


/**
 * Default implementation based on the JDK's <code>HttpURLConnection</code>.
 * <p>
 * This class works around a serious design issue in the SUN JDK, so it will not work on any JDK that
 * doesn't offer the <code>sun.net.www.protocol.http.HttpURLConnection </code> implementation.
 * </p>
 * <p>
 * This implementation <em>DOES NOT WORK</em> on Android. Read the jUPnP manual for
 * alternatives for Android.
 * </p>
 * <p>
 * This implementation <em>DOES NOT</em> support jUPnP's server-side heartbeat for connection checking.
 * Any data returned by a server has to be "valid HTTP", checked in Sun's HttpClient with:
 * </p>
 * {@code ret = b[0] == 'H' && b[1] == 'T' && b[2] == 'T' && b[3] == 'P' && b[4] == '/' && b[5] == '1' && b[6] == '.';}
 * <p>
 * Hence, if you are using this client, don't call jUPnP's
 * {@link org.jupnp.model.profile.RemoteClientInfo#isRequestCancelled()} function on your
 * server to send a heartbeat to the client!
 * </p>
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - refactoring for jUPnP
 */
public class StreamClientImpl implements StreamClient {

    static final String HACK_STREAM_HANDLER_SYSTEM_PROPERTY = "hackStreamHandlerProperty";

    private final Logger log = LoggerFactory.getLogger(StreamClient.class.getName());

    protected final StreamClientConfigurationImpl configuration;

    public StreamClientImpl(StreamClientConfigurationImpl configuration) throws InitializationException {
        this.configuration = configuration;

        if (ModelUtil.ANDROID_EMULATOR || ModelUtil.ANDROID_RUNTIME) {
            /*
            See the fantastic PERMITTED_USER_METHODS here:

            https://android.googlesource.com/platform/libcore/+/android-4.0.1_r1.2/luni/src/main/java/java/net/HttpURLConnection.java

            We'd have to basically copy the whole Android code, and have a dependency on
            libcore.*, and do much more hacking to allow more HTTP methods. This is the same
            problem we are hacking below for the JDK but at least there we don't have a
            dependency issue for compiling jUPnP. These guys all suck, there is no list
            of "permitted" HTTP methods. HttpURLConnection and the whole stream handler
            factory stuff is the worst Java API ever created.
            */
            throw new InitializationException(
                "This client does not work on Android. The design of HttpURLConnection is broken, we "
                    + "can not add additional 'permitted' HTTP methods. Read the jUPnP manual."
            );
        }

        log.trace("Using persistent HTTP stream client connections: " + configuration.isUsePersistentConnections());
        System.setProperty("http.keepAlive", Boolean.toString(configuration.isUsePersistentConnections()));

        // Hack the environment to allow additional HTTP methods
        if (System.getProperty(HACK_STREAM_HANDLER_SYSTEM_PROPERTY) == null) {
            log.trace("Setting custom static URLStreamHandlerFactory to work around bad JDK defaults");
            try {
                // Use reflection to avoid dependency on sun.net package so this class at least
                // loads on Android, even if it doesn't work...
                URL.setURLStreamHandlerFactory(
                    (URLStreamHandlerFactory) Class.forName(
                        "org.jupnp.tool.transport.FixedSunURLStreamHandler"
                    ).newInstance()
                );
            } catch (Throwable t) {
                throw new InitializationException(
                    "Failed to set modified URLStreamHandlerFactory in this environment."
                        + " Can't use bundled default client based on HTTPURLConnection, see manual."
                );
            }
            System.setProperty(HACK_STREAM_HANDLER_SYSTEM_PROPERTY, "alreadyWorkedAroundTheEvilJDK");
        }
    }

    @Override
    public StreamClientConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) {

        final UpnpRequest requestOperation = requestMessage.getOperation();
        log.trace("Preparing HTTP request message with method '{}': ", requestOperation.getHttpMethodName());

        URL url = URIUtil.toURL(requestOperation.getURI());

        HttpURLConnection urlConnection = null;
        InputStream inputStream;
        try {

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod(requestOperation.getHttpMethodName());

            // Use the built-in expiration, we can't cancel HttpURLConnection
            urlConnection.setReadTimeout(configuration.getTimeoutSeconds() * 1000);
            urlConnection.setConnectTimeout(configuration.getTimeoutSeconds() * 1000);

            applyRequestProperties(urlConnection, requestMessage);
            applyRequestBody(urlConnection, requestMessage);

            log.trace("Sending HTTP request: {}",  requestMessage);
            inputStream = urlConnection.getInputStream();
            return createResponse(urlConnection, inputStream);

        } catch (ProtocolException ex) {
            log.warn("HTTP request failed: {}", requestMessage, Exceptions.unwrap(ex));
            return null;
        } catch (IOException ex) {

            if (urlConnection == null) {
                log.warn("HTTP request failed: {}", requestMessage, Exceptions.unwrap(ex));
                return null;
            }

            if (ex instanceof SocketTimeoutException) {
                log.info("Timeout of {} seconds while waiting for HTTP request to complete, aborting: {}",getConfiguration().getTimeoutSeconds(), requestMessage);
                return null;
            }

            log.trace("Exception occurred, trying to read the error stream: {}", Exceptions.unwrap(ex));
            try {
                inputStream = urlConnection.getErrorStream();
                return createResponse(urlConnection, inputStream);
            } catch (Exception errorEx) {
                log.trace("Could not read error stream: {}", errorEx);
                return null;
            }
        } catch (Exception ex) {
            log.warn("HTTP request failed: {}", requestMessage, Exceptions.unwrap(ex));
            return null;

        } finally {

            if (urlConnection != null) {
                // Release any idle persistent connection, or "indicate that we don't want to use this server for a while"
                urlConnection.disconnect();
            }
        }
    }

    @Override
    public void stop() {
        // NOOP
    }

    protected void applyRequestProperties(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) {

        urlConnection.setInstanceFollowRedirects(false); // Defaults to true but not needed here

        // HttpURLConnection always adds a "Host" header

        // HttpURLConnection always adds an "Accept" header (not needed but shouldn't hurt)

        // Add the default user agent if not already set on the message
        if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
            urlConnection.setRequestProperty(
                UpnpHeader.Type.USER_AGENT.getHttpName(),
                getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(), requestMessage.getUdaMinorVersion())
            );
        }

        // Other headers
        applyHeaders(urlConnection, requestMessage.getHeaders());
    }

    protected void applyHeaders(HttpURLConnection urlConnection, Headers headers) {
        log.trace("Writing headers on HttpURLConnection: {}", headers.size());
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String v : entry.getValue()) {
                String headerName = entry.getKey();
                log.trace("Setting header '{}': {}", headerName, v);
                urlConnection.setRequestProperty(headerName, v);
            }
        }
    }

    protected void applyRequestBody(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) throws IOException {

        if (requestMessage.hasBody()) {
            urlConnection.setDoOutput(true);
        } else {
            urlConnection.setDoOutput(false);
            return;
        }

        if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
            IO.writeUTF8(urlConnection.getOutputStream(), requestMessage.getBodyString());
        } else if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
            IO.writeBytes(urlConnection.getOutputStream(), requestMessage.getBodyBytes());
        }
        urlConnection.getOutputStream().flush();
    }

    protected StreamResponseMessage createResponse(HttpURLConnection urlConnection, InputStream inputStream) throws Exception {

        if (urlConnection.getResponseCode() == -1) {
            log.warn("Received an invalid HTTP response: {}", urlConnection.getURL());
            log.warn("Is your jUPnP-based server sending connection heartbeats with " +
                "RemoteClientInfo#isRequestCancelled? This client can't handle " +
                "heartbeats, read the manual.");
            return null;
        }

        // Status
        UpnpResponse responseOperation = new UpnpResponse(urlConnection.getResponseCode(), urlConnection.getResponseMessage());

        log.trace("Received response: {}", responseOperation);

        // Message
        StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

        // Headers
        responseMessage.setHeaders(new UpnpHeaders(urlConnection.getHeaderFields()));

        // Body
        byte[] bodyBytes = null;
        InputStream is = null;
        try {
            is = inputStream;
            if (inputStream != null) bodyBytes = IO.readBytes(is);
        } finally {
            if (is != null)
                is.close();
        }

        if (bodyBytes != null && bodyBytes.length > 0 && responseMessage.isContentTypeMissingOrText()) {
            log.trace("Response contains textual entity body, converting then setting string on message");
            responseMessage.setBodyCharacters(bodyBytes);
        } else if (bodyBytes != null && bodyBytes.length > 0) {
            log.trace("Response contains binary entity body, setting bytes on message");
            responseMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);
        } else {
            log.trace("Response did not contain entity body");
        }

        log.trace("Response message complete: {}", responseMessage);
        return responseMessage;
    }

}

