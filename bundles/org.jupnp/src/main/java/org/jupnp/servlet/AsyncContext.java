package org.jupnp.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * The {@link AsyncContext} is a replacement for the original {@code javax.servlet.AsyncContext}
 * so that asynchronous processing can also be used by Servlet 2.5 containers.
 * <p>
 * This class can be removed if Servlet 2.5 support should be given up again.
 * 
 * @author Michael Grammling - Initial Contribution
 */
public interface AsyncContext {

    ServletRequest getRequest();

    ServletResponse getResponse();

    void setTimeout(long timeout) throws IllegalStateException;

    long getTimeout();

    void complete();

    void addListener(AsyncListener listener) throws IllegalStateException;

}
