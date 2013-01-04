/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.fourthline.cling.transport.impl.apache;

import org.fourthline.cling.model.ServerClientTokens;
import org.fourthline.cling.transport.spi.StreamClientConfiguration;

/**
 * Settings for the Apache HTTP Components implementation.
 *
 * @author Christian Bauer
 */
public class StreamClientConfigurationImpl implements StreamClientConfiguration {

    private int maxTotalConnections = 1024;
    private int maxTotalPerRoute = 100;
    private int connectionTimeoutSeconds = 20; // WMP can be very slow to connect
    private int dataReadTimeoutSeconds = 60; // WMP can be very slow sending the initial data after connection
    private String contentCharset = "UTF-8"; // UDA spec says it's always UTF-8 entity content

    /**
     * Defaults to 1024.
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    /**
     * Defaults to 100.
     */
    public int getMaxTotalPerRoute() {
        return maxTotalPerRoute;
    }

    public void setMaxTotalPerRoute(int maxTotalPerRoute) {
        this.maxTotalPerRoute = maxTotalPerRoute;
    }

    /**
     * Defaults to 20 seconds.
     */
    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }
    /**
     * Defaults to 60 seconds.
     */
    public int getDataReadTimeoutSeconds() {
        return dataReadTimeoutSeconds;
    }

    public void setDataReadTimeoutSeconds(int dataReadTimeoutSeconds) {
        this.dataReadTimeoutSeconds = dataReadTimeoutSeconds;
    }

    /**
     * @return Character set of textual content, defaults to "UTF-8".
     */
    public String getContentCharset() {
        return contentCharset;
    }

    public void setContentCharset(String contentCharset) {
        this.contentCharset = contentCharset;
    }

    /**
     * Defaults to string value of {@link ServerClientTokens}.
     */
    public String getUserAgentValue(int majorVersion, int minorVersion) {
        return new ServerClientTokens(majorVersion, minorVersion).toString();
    }

    /**
     * If -1, the default value of HttpClient will be used (8192 in httpclient 4.1)
     * <p>
     * This will also avoid OOM on the HTC Thunderbolt where default size is 2MB (!):
     * http://stackoverflow.com/questions/5358014/android-httpclient-oom-on-4g-lte-htc-thunderbolt
     * </p>
     */
    public int getSocketBufferSize() {
    	return -1; 
    }

	public boolean getStaleCheckingEnabled() {
		return false;
	}

    /**
     * If -1, the default value of HttpClient will be used (3 in httpclient 4.1)
     */
	public int getRequestRetryCount() {
		// The default that is used by DefaultHttpClient if unspecified
		return -1;
	}

}
