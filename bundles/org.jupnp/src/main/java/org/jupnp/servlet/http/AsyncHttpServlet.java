package org.jupnp.servlet.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jupnp.servlet.AsyncContext;
import org.jupnp.servlet.impl.AsyncContextImpl;


/**
 * The {@link AsyncHttpServlet} is a proxy for an {@link HttpServlet} of Servlet 2.5 containers.
 * It can be used if asynchronous processing (supported by Servlet 3.0 containers) should be used
 * in a Servlet 2.5 container. Although the asynchronous behaviour is the same as for Servlet 3.0
 * containers, this proxy will <i>not</i> return its response to the servlet container as long as
 * the {@link AsyncContext#complete()} method is called and therefore it blocks the thread for the
 * request.
 * <p>
 * This proxy takes only effect if the method
 * {@link #service(AsyncHttpServletRequest, HttpServletResponse)} is overridden.
 * <p>
 * This class can be removed if Servlet 2.5 support should be given up again.
 * 
 * @author Michael Grammling
 */
public abstract class AsyncHttpServlet extends HttpServlet {

    private static final long serialVersionUID = 5234053730076572677L;

    private HttpServletResponse response;


    @Override
    protected final void service(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        this.response = response;

        AsyncHttpServletRequest asyncRequest = new AsyncHttpServletRequest(request, this);

        // now execute the code (usually this call returns fast)
        service(asyncRequest, response);

        // wait for completion
        if (asyncRequest.isAsyncStarted()) {
            AsyncContextImpl asyncContextImpl =
                    (AsyncContextImpl) asyncRequest.getAsynchronousContext();

            asyncContextImpl.setStarted();
            asyncContextImpl.waitForCompletion();
        }

        this.response = null;
    }

    protected void service(AsyncHttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
    }

    protected HttpServletResponse getResponse() {
        return this.response;
    }

}
