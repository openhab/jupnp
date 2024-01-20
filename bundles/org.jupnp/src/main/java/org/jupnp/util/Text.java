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

package org.jupnp.util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Text {

    public static String ltrim(String s) {
        return s.replaceAll("(?s)^\\s+", "");
    }

    public static String rtrim(String s) {
        return s.replaceAll("(?s)\\s+$", "");
    }

    public static String displayFilesize(long fileSizeInBytes) {
        // TODO: Yeah, that could be done smarter..
        if (fileSizeInBytes >= 1073741824) {
            return new BigDecimal(fileSizeInBytes / 1024 / 1024 / 1024) + " GiB";
        } else if (fileSizeInBytes >= 1048576) {
            return new BigDecimal(fileSizeInBytes / 1024 / 1024) + " MiB";
        } else if (fileSizeInBytes >= 1024) {
            return new BigDecimal(fileSizeInBytes / 1024) + " KiB";
        } else {
            return new BigDecimal(fileSizeInBytes) + " bytes";
        }
    }

    public static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ssz";

    public static Calendar fromISO8601String(TimeZone targetTimeZone, String s) {
        DateFormat format = new SimpleDateFormat(ISO8601_PATTERN);
        format.setTimeZone(targetTimeZone);
        try {
            Calendar cal = new GregorianCalendar();
            cal.setTime(format.parse(s));
            return cal;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toISO8601String(TimeZone targetTimeZone, Date datetime) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(datetime);
        return toISO8601String(targetTimeZone, cal);
    }

    public static String toISO8601String(TimeZone targetTimeZone, long unixTime) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(unixTime);
        return toISO8601String(targetTimeZone, cal);
    }

    public static String toISO8601String(TimeZone targetTimeZone, Calendar cal) {
        DateFormat format = new SimpleDateFormat(ISO8601_PATTERN);
        format.setTimeZone(targetTimeZone);
        try {
            return format.format(cal.getTime());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
