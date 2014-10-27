package org.jupnp.servlet;

import java.io.IOException;
import java.util.EventListener;


/**
 * The {@link AsyncListener} is a replacement for the original {@code javax.servlet.AsyncListener}
 * so that asynchronous processing can also be used by Servlet 2.5 containers.
 * <p>
 * This class can be removed if Servlet 2.5 support should be given up again.
 * 
 * @author Michael Grammling - Initial Contribution
 */
public interface AsyncListener extends EventListener {

    void onComplete(AsyncEvent event) throws IOException;

    void onError(AsyncEvent event) throws IOException;

    void onStartAsync(AsyncEvent event) throws IOException;

    void onTimeout(AsyncEvent event) throws IOException;

}
