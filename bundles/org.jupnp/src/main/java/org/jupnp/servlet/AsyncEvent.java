package org.jupnp.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * The {@link AsyncEvent} is a replacement for the original {@code javax.servlet.AsyncEvent}
 * so that asynchronous processing can also be used by Servlet 2.5 containers.
 * <p>
 * This class can be removed if Servlet 2.5 support should be given up again.
 * 
 * @author Michael Grammling - Initial Contribution
 */
public class AsyncEvent {
 
    private AsyncContext asyncContext;
    private Throwable throwable;


    public AsyncEvent(AsyncContext asyncContext) {
        this(asyncContext, null);
    }

    public AsyncEvent(AsyncContext asyncContext, Throwable throwable)
            throws IllegalArgumentException {

        if (asyncContext == null) {
            throw new IllegalArgumentException("The AsyncContext must not be null!");
        }

        this.asyncContext = asyncContext;
        this.throwable = throwable;
    }

    public AsyncContext getAsyncContext() {
        return this.asyncContext;
    }

    public ServletRequest getSuppliedRequest() {
        return this.asyncContext.getRequest();
    }

    public ServletResponse getSuppliedResponse() {
        return this.asyncContext.getResponse();
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

}
