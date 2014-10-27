package org.jupnp.servlet.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.jupnp.servlet.AsyncContext;
import org.jupnp.servlet.impl.AsyncContextImpl;


/**
 * The {@link AsyncHttpServletRequest} wraps an {@link HttpServletRequest} to a user defined
 * one which simulates the asynchronous processing feature of Servlet 3.0 containers.
 * <p>
 * Use the methods {@link #startAsynchronous()} and {@link #getAsynchronousContext()}
 * as replacement methods for using the asynchronous processing feature.
 * <p>
 * This class can be removed if Servlet 2.5 support should be given up again.
 * 
 * @author Michael Grammling
 */
public class AsyncHttpServletRequest extends HttpServletRequestWrapper
        implements HttpServletRequest {

    private AsyncContext asyncContext;
    private AsyncHttpServlet asyncHttpServlet;


    public AsyncHttpServletRequest(HttpServletRequest request, AsyncHttpServlet asyncHttpServlet) {
        super(request);
        this.asyncHttpServlet = asyncHttpServlet;
    }

    public AsyncContext startAsynchronous() {
        this.asyncContext = new AsyncContextImpl(
                super.getRequest(), this.asyncHttpServlet.getResponse());

        return this.asyncContext;
    }

    public boolean isAsyncStarted() {
        return (this.asyncContext != null);
    }

    public AsyncContext getAsynchronousContext() throws IllegalStateException {
        if (isAsyncStarted()) {
            return this.asyncContext;
        }
 
        throw new IllegalStateException("The request is not in asynchronous mode!");
    }

}
