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

package org.jupnp.binding.xml;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Service;

/**
 * Implementation based on JAXP SAX.
 *
 * @author Christian Bauer
 */
public class RecoveringUDA10ServiceDescriptorBinderSAXImpl extends UDA10ServiceDescriptorBinderSAXImpl {

    private Logger log = Logger.getLogger(ServiceDescriptorBinder.class.getName());

    @Override
    public <S extends Service> S describe(S undescribedService, String descriptorXml)
            throws DescriptorBindingException, ValidationException {

        if (descriptorXml == null || descriptorXml.length() == 0) {
            throw new DescriptorBindingException("Null or empty descriptor");
        }

        try {
            log.fine("Reading service from XML descriptor");

            String fixedXml = fixBOM(descriptorXml);
            // fixedXml = fixWrongNamespaces(fixedXml);
            fixedXml = fixRetval(fixedXml);
            fixedXml = fixQuotes(fixedXml);
            return super.describe(undescribedService, fixedXml);
        } catch (DescriptorBindingException e) {
            log.warning(e.getMessage());
        }
        return null;
    }

    protected String fixBOM(String descriptorXml) {
        if (descriptorXml.contains("<scpd xmlns=\"urn:Belkin:service-1-0\">")) {
            descriptorXml = descriptorXml.trim().replaceFirst("^([\\W]+)<", "<");
            char myChar = descriptorXml.charAt(0);
            for (int i = 0; i < descriptorXml.length(); i++) {
                myChar = descriptorXml.charAt(i);
                if (myChar == 65279) {
                    log.warning("We found a BOM");
                }
            }
            String newXml;
            Matcher junkMatcher = (Pattern.compile("^([\\W]+)<")).matcher(descriptorXml.trim());

            newXml = junkMatcher.replaceFirst("<");
            return newXml.replaceAll("\0", " ");

        }
        return descriptorXml;
    }

    protected String fixWrongNamespaces(String descriptorXml) {
        if (descriptorXml.contains("<scpd xmlns=\"urn:Belkin:service-1-0\">")) {
            log.warning("Detected invalid scpd namespace 'urn:Belkin', replacing it with 'urn:schemas-upnp-org'");
            return descriptorXml.replaceAll("<scpd xmlns=\"urn:Belkin:service-1-0\">",
                    "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">");
        }
        return descriptorXml;
    }

    protected String fixRetval(String descriptorXml) {
        if (descriptorXml.contains("<retval")) {
            log.warning("Detected invalid service value 'retval', replacing it");
            descriptorXml = descriptorXml.replaceAll("<retval/>", " ");
            return descriptorXml.replaceAll("<retval />", " ");
        }
        return descriptorXml;
    }

    protected String fixQuotes(String descriptorXml) {
        if (descriptorXml.contains("Key\"")) {
            log.warning("Detected invalid quotes, replacing it");
            descriptorXml = descriptorXml.replaceAll("\"smartprivateKey\"", "smartprivateKey");
            return descriptorXml.replaceAll("\"pluginprivateKey\"", "pluginprivateKey");
        }
        return descriptorXml;
    }

}
