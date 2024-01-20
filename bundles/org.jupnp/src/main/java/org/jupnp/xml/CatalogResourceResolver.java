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

package org.jupnp.xml;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Another namespace-URI-to-whatever (namespace, context, resolver, map) magic thingy.
 * <p>
 * Of course it's just a map, like so many others in the JAXP hell. But this time someone
 * really went all out on the API fugliness. The person who designed this probably got promoted
 * and is now designing the signaling system for the airport you are about to land on.
 * Feeling better?
 * </p>
 *
 * @author Christian Bauer
 */
public class CatalogResourceResolver implements LSResourceResolver {

    private Logger log = LoggerFactory.getLogger(CatalogResourceResolver.class);

    private final Map<URI, URL> catalog;

    public CatalogResourceResolver(Map<URI, URL> catalog) {
        this.catalog = catalog;
    }

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        log.trace("Trying to resolve system identifier URI in catalog: {}", systemId);
        URL systemURL;
        if ((systemURL = catalog.get(URI.create(systemId))) != null) {
            log.trace("Loading catalog resource: {}", systemURL);
            try {
                Input i = new Input(systemURL.openStream());
                i.setBaseURI(baseURI);
                i.setSystemId(systemId);
                i.setPublicId(publicId);
                return i;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        log.info(
                "System identifier not found in catalog, continuing with default resolution (this most likely means remote HTTP request!): {}",
                systemId
        );
        return null;
    }

    // WTF...
    private static final class Input implements LSInput {

        InputStream in;

        public Input(InputStream in) {
            this.in = in;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public InputStream getByteStream() {
            return in;
        }

        public void setByteStream(InputStream byteStream) {
        }

        public String getStringData() {
            return null;
        }

        public void setStringData(String stringData) {
        }

        public String getSystemId() {
            return null;
        }

        public void setSystemId(String systemId) {
        }

        public String getPublicId() {
            return null;
        }

        public void setPublicId(String publicId) {
        }

        public String getBaseURI() {
            return null;
        }

        public void setBaseURI(String baseURI) {
        }

        public String getEncoding() {
            return null;
        }

        public void setEncoding(String encoding) {
        }

        public boolean getCertifiedText() {
            return false;
        }

        public void setCertifiedText(boolean certifiedText) {
        }
    }
}
