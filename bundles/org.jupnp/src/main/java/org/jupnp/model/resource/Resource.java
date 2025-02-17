/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.model.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.jupnp.model.ExpirationDetails;

/**
 * An addressable object, stored, managed, and accessible through the {@link org.jupnp.registry.Registry}.
 *
 * @param <M> The type of the model object.
 *
 * @author Christian Bauer
 */
public class Resource<M> {

    private URI pathQuery;
    private M model;

    /**
     * @param pathQuery The path and (optional) query URI parts of this resource.
     * @param model The model object.
     */
    public Resource(URI pathQuery, M model) {
        try {
            this.pathQuery = new URI(null, null, pathQuery.getPath(), pathQuery.getQuery(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.model = model;
        if (model == null) {
            throw new IllegalArgumentException("Model instance must not be null");
        }
    }

    public URI getPathQuery() {
        return pathQuery;
    }

    public M getModel() {
        return model;
    }

    /**
     * @param pathQuery A relative URI.
     * @return <code>true</code> if the given URI path and query matches the resource's path and query.
     */
    public boolean matches(URI pathQuery) {
        return pathQuery.equals(getPathQuery());
    }

    /**
     * Called periodically by the registry to maintain the resource.
     * <p>
     * NOOP by default.
     * </p>
     *
     * @param pendingExecutions Add <code>Runnable</code>'s to this collection if maintenance code has to run in the
     *            background.
     * @param expirationDetails The details of this resource's expiration, e.g. when it will expire.
     */
    public void maintain(List<Runnable> pendingExecutions, ExpirationDetails expirationDetails) {
        // Do nothing
    }

    /**
     * Called by the registry when it stops, in the shutdown thread.
     * <p>
     * NOOP by default.
     * </p>
     */
    public void shutdown() {
        // Do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Resource resource = (Resource) o;

        if (!getPathQuery().equals(resource.getPathQuery())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getPathQuery().hashCode();
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") URI: " + getPathQuery();
    }
}
