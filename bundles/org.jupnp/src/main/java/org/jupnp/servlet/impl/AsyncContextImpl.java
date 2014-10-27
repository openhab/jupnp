package org.jupnp.servlet.impl;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jupnp.servlet.AsyncContext;
import org.jupnp.servlet.AsyncEvent;
import org.jupnp.servlet.AsyncListener;


/**
 * The {@link AsyncContextImpl} is a concrete implementation of the {@link AsyncContext}.
 * <p>
 * This class simulates the asynchronous processing feature of Servlet 3.0 containers.
 * <p>
 * This class can be removed if Servlet 2.5 support should be given up again.
 * 
 * @author Michael Grammling
 */
public class AsyncContextImpl implements AsyncContext {

    private static final long DEFAULT_TIMEOUT = 60000;

    private final Object LOCK = new Object();

    private enum EventType {
        ON_COMPLETE,
        ON_ERROR,
        ON_START_ASYNC,
        ON_TIMEOUT
    }

    private ServletRequest request;
    private ServletResponse response;
    private long timeout;
    private Set<AsyncListener> listeners;

    private boolean started;
    private boolean complete;


    public AsyncContextImpl(ServletRequest request, ServletResponse response)
            throws IllegalArgumentException {

        if (request == null) {
            throw new IllegalArgumentException("The ServletRequest must not be null!");
        }

        if (response == null) {
            throw new IllegalArgumentException("The ServletResponse must not be null!");
        }

        this.request = request;
        this.response = response;

        setTimeout(-1);

        this.listeners = new HashSet<>();
    }

    public void setStarted() {
        synchronized (LOCK) {
            if ((!this.started) && (!this.complete)) {
                this.started = true;
                fireEvent(EventType.ON_START_ASYNC, new AsyncEvent(this));
            }
        }
    }

    private void assertNotStarted() throws IllegalStateException {
        if (this.started) {
            throw new IllegalArgumentException(
                "The timeout cannot be changed when the request is already dispatched.");
        }
    }

    @Override
    public ServletRequest getRequest() {
        return this.request;
    }

    @Override
    public ServletResponse getResponse() {
        return this.response;
    }

    @Override
    public synchronized void setTimeout(long timeout) throws IllegalStateException {
        assertNotStarted();
        this.timeout = (timeout < 0) ? DEFAULT_TIMEOUT : timeout;
    }

    @Override
    public long getTimeout() {
        return this.timeout;
    }

    @Override
    public void complete() {
        synchronized (LOCK) {
            if (!this.complete) {
                if (!this.started) {
                    setStarted();
                }

                fireEvent(EventType.ON_COMPLETE, new AsyncEvent(this));
                this.listeners.clear();

                this.complete = true;
                LOCK.notify();
            }
        }
    }

    public void waitForCompletion() {
        synchronized (LOCK) {
            if (this.started && (!this.complete)) {
                try {
                    LOCK.wait(this.timeout);
                    if (!this.complete) {
                        fireEvent(EventType.ON_TIMEOUT, new AsyncEvent(this));
                    }
                } catch (InterruptedException ie) {
                    // nothing to do
                } finally {
                    this.complete = true;
                }
            }
        }
    }

    @Override
    public synchronized void addListener(AsyncListener listener)  throws IllegalStateException {
        assertNotStarted();

        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    private synchronized void fireEvent(EventType type, AsyncEvent event) {
        for (AsyncListener listener : this.listeners) {
            try {
                switch (type) {
                    case ON_COMPLETE: listener.onComplete(event); break;
                    case ON_ERROR: listener.onError(event); break;
                    case ON_START_ASYNC: listener.onStartAsync(event); break;
                    case ON_TIMEOUT: listener.onTimeout(event); break;
                }
            } catch (Exception ex) {
                // nothing to do
            }
        }
    }

}
