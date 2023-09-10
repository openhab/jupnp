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

package org.jupnp.test.transport;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.mock.MockProtocolFactory;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.protocol.ProtocolCreationException;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.transport.spi.UpnpStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;

public abstract class StreamServerClientTest {

    final private static Logger log = LoggerFactory.getLogger(StreamServerClientTest.class);

    public static final String TEST_HOST = "localhost";
    public static final int TEST_PORT = 8081;

    public static UpnpServiceConfiguration configuration = new MockUpnpServiceConfiguration(false, true);

    public static MockProtocolFactory protocolFactory = new MockProtocolFactory() {
        @Override
        public ReceivingSync createReceivingSync(StreamRequestMessage requestMessage) throws ProtocolCreationException {
            String path = requestMessage.getUri().getPath();
            if (path.endsWith(OKEmptyResponse.PATH)) {
                lastExecutedServerProtocol = new OKEmptyResponse(requestMessage);
            } else if (path.endsWith(OKBodyResponse.PATH)) {
                lastExecutedServerProtocol = new OKBodyResponse(requestMessage);
            } else if (path.endsWith(NoResponse.PATH)) {
                lastExecutedServerProtocol = new NoResponse(requestMessage);
            } else if (path.endsWith(DelayedResponse.PATH)) {
                lastExecutedServerProtocol = new DelayedResponse(requestMessage);
            } else if (path.endsWith(TooLongResponse.PATH)) {
                lastExecutedServerProtocol = new TooLongResponse(requestMessage);
            } else if (path.endsWith(CheckAliveResponse.PATH)) {
                lastExecutedServerProtocol = new CheckAliveResponse(requestMessage);
            } else if (path.endsWith(CheckAliveLongResponse.PATH)) {
                lastExecutedServerProtocol = new CheckAliveLongResponse(requestMessage);
            } else {
                throw new ProtocolCreationException("Invalid test path: " + path);
            }
            return lastExecutedServerProtocol;
        }
    };

    public static MockRouter router = new MockRouter(configuration, protocolFactory) {
        @Override
        public void received(UpnpStream stream) {
            stream.run();
        }
    };

    public static StreamServer server;
    public static StreamClient client;
    public static long clientTimeoutMillis;
    public static TestProtocol lastExecutedServerProtocol;

    static void start(Function<Integer, StreamServer> createServer,
                      Function<UpnpServiceConfiguration, StreamClient> createClient) throws Exception {
        server = createServer.apply(TEST_PORT);
        server.init(InetAddress.getByName(TEST_HOST), router);
        configuration.getStreamServerExecutorService().execute(server);

        client = createClient.apply(configuration);
        clientTimeoutMillis = 1000L * client.getConfiguration().getTimeoutSeconds();
        Thread.sleep(1000);
    }

    @AfterAll
    static void stop() throws Exception {
        server.stop();
        client.stop();
        Thread.sleep(1000);
    }

    @BeforeEach
    void clearLastProtocol() {
        lastExecutedServerProtocol = null;
    }

    @Test
    void basic() throws Exception {
        StreamResponseMessage responseMessage;

        responseMessage = client.sendRequest(createRequestMessage(OKEmptyResponse.PATH));
        assertNotNull(responseMessage, "responseMessage");
        assertNotNull(responseMessage.getOperation(), "responseMessage.getOperation()");
        assertEquals(responseMessage.getOperation().getStatusCode(), 200);
        assertFalse(responseMessage.hasBody());
        assertTrue(lastExecutedServerProtocol.isComplete);

        lastExecutedServerProtocol = null;
        responseMessage = client.sendRequest(createRequestMessage(OKBodyResponse.PATH));
        assertNotNull(responseMessage, "responseMessage");
        assertNotNull(responseMessage.getOperation(), "responseMessage.getOperation()");
        assertEquals(responseMessage.getOperation().getStatusCode(), 200);
        assertTrue(responseMessage.hasBody());
        assertEquals(responseMessage.getBodyString(), "foo");
        assertTrue(lastExecutedServerProtocol.isComplete);

        lastExecutedServerProtocol = null;
        responseMessage = client.sendRequest(createRequestMessage(NoResponse.PATH));
        assertNotNull(responseMessage, "responseMessage");
        assertNotNull(responseMessage.getOperation(), "responseMessage.getOperation()");
        assertEquals(responseMessage.getOperation().getStatusCode(), 404);
        assertFalse(responseMessage.hasBody());
        assertFalse(lastExecutedServerProtocol.isComplete);
    }

    @Test
    void cancelled() throws Exception {
        final AtomicBoolean interrupted = new AtomicBoolean(false);

        final Thread requestThread = new Thread(() -> {
            try {
                client.sendRequest(createRequestMessage(DelayedResponse.PATH));
            } catch (InterruptedException ex) {
                log.info("Request thread interrupted as expected");
                interrupted.set(true);
            }
        });

        requestThread.start();

        // Cancel the request after 250ms
        Thread.sleep(250);
        requestThread.interrupt();

        Thread.sleep(DelayedResponse.SLEEP_MS + 500);

        assertTrue(interrupted.get());
        // The server doesn't check if the connection is still alive, so it will complete
        assertTrue(lastExecutedServerProtocol.isComplete);
    }

    @Test
    void expired() throws Exception {
        StreamResponseMessage responseMessage = client.sendRequest(createRequestMessage(TooLongResponse.PATH));
        assertNull(responseMessage);
        assertFalse(lastExecutedServerProtocol.isComplete);
        // The client expires the HTTP connection but the server doesn't check if
        // it's alive, so the server will complete the request after a while
        Thread.sleep(TooLongResponse.SLEEP_MS + 1000);
        assertTrue(lastExecutedServerProtocol.isComplete);
    }

    @Test
    void checkAlive() throws Exception {
        StreamResponseMessage responseMessage = client.sendRequest(createRequestMessage(CheckAliveResponse.PATH));
        assertEquals(responseMessage.getOperation().getStatusCode(), 200);
        assertFalse(responseMessage.hasBody());
        assertTrue(lastExecutedServerProtocol.isComplete);
    }

    @Test
    void checkAliveExpired() throws Exception {
        StreamResponseMessage responseMessage = client.sendRequest(createRequestMessage(CheckAliveLongResponse.PATH));
        assertNull(responseMessage);
        CheckAliveLongResponse.requestCancelled = true;
        // The client expires the HTTP connection and the server checks if the
        // connection is still alive, it will abort the request
        Thread.sleep(3000);
        assertFalse(lastExecutedServerProtocol.isComplete);
    }

    @Test
    void checkAliveCancelled() throws Exception {
        final AtomicBoolean interrupted = new AtomicBoolean(false);

        final Thread requestThread = new Thread(() -> {
            try {
                client.sendRequest(createRequestMessage(CheckAliveResponse.PATH));
            } catch (InterruptedException ex) {
                log.info("Request thread interrupted as expected");
                interrupted.set(true);
                CheckAliveResponse.requestCancelled = true;
            }
        });

        requestThread.start();

        // Cancel the request after 1 second
        Thread.sleep(1000);
        requestThread.interrupt();

        Thread.sleep(3000);

        assertTrue(interrupted.get());
        assertFalse(lastExecutedServerProtocol.isComplete);
    }

    protected StreamRequestMessage createRequestMessage(String path) {
        return new StreamRequestMessage(
            UpnpRequest.Method.GET,
            URI.create("http://" + TEST_HOST + ":" + TEST_PORT + path)
        );
    }

    public static abstract class TestProtocol extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {
        volatile public boolean isComplete;

        public TestProtocol(StreamRequestMessage inputMessage) {
            super(null, inputMessage);
        }
    }

    public static class OKEmptyResponse extends TestProtocol {

        public static final String PATH = "/ok";

        public OKEmptyResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            isComplete = true;
            return new StreamResponseMessage(UpnpResponse.Status.OK);
        }
    }

    public static class OKBodyResponse extends TestProtocol{

        public static final String PATH = "/okbody";

        public OKBodyResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            isComplete = true;
            return new StreamResponseMessage("foo");
        }
    }

    public static class NoResponse extends TestProtocol {

        public static final String PATH = "/noresponse";

        public NoResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            return null;
        }
    }

    public static class DelayedResponse extends TestProtocol {

        public static final String PATH = "/delayed";
        public static final long SLEEP_MS = clientTimeoutMillis + 1000;

        public DelayedResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            try {
                log.info("Sleeping for " + SLEEP_MS + "ms before completion...");
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException ex) {
                fail("Interrupted while sleeping in DelayedResponse", ex);
            }
            isComplete = true;
            return new StreamResponseMessage(UpnpResponse.Status.OK);
        }
    }

    public static class TooLongResponse extends TestProtocol {

        public static final String PATH = "/toolong";
        public static final long SLEEP_MS = clientTimeoutMillis + 1000;

        public TooLongResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            try {
                log.info("Sleeping for " + SLEEP_MS + " before completion...");
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException ex) {
                fail("Interrupted while sleeping in TooLongResponse", ex);
            }
            isComplete = true;
            return new StreamResponseMessage(UpnpResponse.Status.OK);
        }
    }

    public static class CheckAliveResponse extends TestProtocol {

        public static final String PATH = "/checkalive";
        public static boolean requestCancelled = false;

        public CheckAliveResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            requestCancelled = false;
            // Return OK response after 2 seconds, check if client connection every 500ms
            for (int i = 0; i < 4; i++) {
                try {
                    log.info("Sleeping for 500ms before checking connection...");
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    fail("Interrupted while sleeping in CheckAliveResponse", ex);
                    return null;
                }
                if (requestCancelled) {
                    log.info("Request got cancelled");
                    return null;
                }
            }
            isComplete = true;
            return new StreamResponseMessage(UpnpResponse.Status.OK);
        }
    }

    public static class CheckAliveLongResponse extends TestProtocol {

        public static final String PATH = "/checkalivelong";
        public static boolean requestCancelled = false;

        public CheckAliveLongResponse(StreamRequestMessage inputMessage) {
            super(inputMessage);
        }

        @Override
        protected StreamResponseMessage executeSync() {
            requestCancelled = false;
            // Return OK response after 5 seconds, check if client connection every 500ms
            for (int i = 0; i < 10; i++) {
                try {
                    log.info("Sleeping for 500ms before checking connection...");
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    fail("Interrupted while sleeping in CheckAliveLongResponse", ex);
                    return null;
                }
                if (requestCancelled) {
                    log.info("Request got cancelled");
                    return null;
                }
            }
            isComplete = true;
            return new StreamResponseMessage(UpnpResponse.Status.OK);
        }
    }

}
