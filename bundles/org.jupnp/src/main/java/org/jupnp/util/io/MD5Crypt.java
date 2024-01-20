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

/*
   Created: 3 November 1999
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2004/07/12 13:35:20 $
   Java Port By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Original C Version:
   ----------------------------------------------------------------------------
   "THE BEER-WARE LICENSE" (Revision 42):
   <phk@login.dknet.dk> wrote this file.  As long as you retain this notice you
   can do whatever you want with this stuff. If we meet some day, and you think
   this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
   ----------------------------------------------------------------------------
 */
package org.jupnp.util.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Crypt {

    // Character set allowed for the salt string
    static private final String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    // Character set of the encrypted password: A-Za-z0-9./
    static private final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Function to return a string from the set: A-Za-z0-9./
     *
     * @param size Length of the string
     * @param v value to be converted
     * @return A string of size (size) from the set A-Za-z0-9./
     */
    static private final String to64(long v, int size) {
        StringBuffer result = new StringBuffer();

        while (--size >= 0) {
            result.append(itoa64.charAt((int) (v & 0x3f)));
            v >>>= 6;
        }

        return result.toString();
    }

    static private final void clearbits(byte bits[]) {
        for (int i = 0; i < bits.length; i++) {
            bits[i] = 0;
        }
    }

    /**
     * convert an encoded unsigned byte value
     * into a int with the unsigned value.
     */
    static private final int bytes2u(byte inp) {
        return (int) inp & 0xff;
    }

    /**
     * LINUX/BSD MD5Crypt function
     *
     * @param password Password to be encrypted
     * @return The encrypted password as an MD5 hash
     */
    static public final String crypt(String password) {
        StringBuffer salt = new StringBuffer();
        java.util.Random rnd = new java.util.Random();

        // build a random 8 chars salt
        while (salt.length() < 8) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.substring(index, index + 1));
        }

        // crypt
        return crypt(password, salt.toString(), "$1$");
    }

    /**
     * LINUX/BSD MD5Crypt function
     *
     * @param salt Random string used to initialize the MD5 engine
     * @param password Password to be encrypted
     * @return The encrypted password as an MD5 hash
     */
    static public final String crypt(String password, String salt) {
        return crypt(password, salt, "$1$");
    }

    /**
     * Linux/BSD MD5Crypt function
     *
     * @param magic $1$ for Linux/BSB, $apr1$ for Apache crypt
     * @param salt 8 byte permutation string
     * @param password user password
     * @return The encrypted password as an MD5 hash
     * @throws java.lang.Exception
     */
    static public final String crypt(String password, String salt, String magic) {

        byte finalState[];
        long l;

        /**
         * Two MD5 hashes are used
         */
        MessageDigest ctx, ctx1;

        try {
            ctx = MessageDigest.getInstance("md5");
            ctx1 = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
            return null;
        }

        /* Refine the Salt first */
        /* If it starts with the magic string, then skip that */

        if (salt.startsWith(magic)) {
            salt = salt.substring(magic.length());
        }

        /* It stops at the first '$', max 8 chars */

        if (salt.indexOf('$') != -1) {
            salt = salt.substring(0, salt.indexOf('$'));
        }

        if (salt.length() > 8) {
            salt = salt.substring(0, 8);
        }

        /**
         * Transformation set #1:
         * The password first, since that is what is most unknown
         * Magic string
         * Raw salt
         */
        ctx.update(password.getBytes());
        ctx.update(magic.getBytes());
        ctx.update(salt.getBytes());

        /* Then just as many characters of the MD5(pw,salt,pw) */

        ctx1.update(password.getBytes());
        ctx1.update(salt.getBytes());
        ctx1.update(password.getBytes());
        finalState = ctx1.digest(); // ctx1.Final();

        for (int pl = password.length(); pl > 0; pl -= 16) {
            ctx.update(finalState, 0, pl > 16 ? 16 : pl);
        }

        /**
         * the original code claimed that finalState was being cleared
         * to keep dangerous bits out of memory,
         * but doing this is also required in order to get the right output.
         */

        clearbits(finalState);

        /* Then something really weird... */

        for (int i = password.length(); i != 0; i >>>= 1) {
            if ((i & 1) != 0) {
                ctx.update(finalState, 0, 1);
            } else {
                ctx.update(password.getBytes(), 0, 1);
            }
        }

        finalState = ctx.digest();

        /**
         * and now, just to make sure things don't run too fast
         * On a 60 Mhz Pentium this takes 34 msec, so you would
         * need 30 seconds to build a 1000 entry dictionary...
         * (The above timings from the C version)
         */

        for (int i = 0; i < 1000; i++) {
            try {
                ctx1 = MessageDigest.getInstance("md5");
            } catch (NoSuchAlgorithmException e0) {
                return null;
            }

            if ((i & 1) != 0) {
                ctx1.update(password.getBytes());
            } else {
                ctx1.update(finalState, 0, 16);
            }

            if ((i % 3) != 0) {
                ctx1.update(salt.getBytes());
            }

            if ((i % 7) != 0) {
                ctx1.update(password.getBytes());
            }

            if ((i & 1) != 0) {
                ctx1.update(finalState, 0, 16);
            } else {
                ctx1.update(password.getBytes());
            }

            finalState = ctx1.digest(); // Final();
        }

        /* Now make the output string */

        StringBuffer result = new StringBuffer();

        result.append(magic);
        result.append(salt);
        result.append("$");

        /**
         * Build a 22 byte output string from the set: A-Za-z0-9./
         */
        l = (bytes2u(finalState[0]) << 16) | (bytes2u(finalState[6]) << 8) | bytes2u(finalState[12]);
        result.append(to64(l, 4));

        l = (bytes2u(finalState[1]) << 16) | (bytes2u(finalState[7]) << 8) | bytes2u(finalState[13]);
        result.append(to64(l, 4));

        l = (bytes2u(finalState[2]) << 16) | (bytes2u(finalState[8]) << 8) | bytes2u(finalState[14]);
        result.append(to64(l, 4));

        l = (bytes2u(finalState[3]) << 16) | (bytes2u(finalState[9]) << 8) | bytes2u(finalState[15]);
        result.append(to64(l, 4));

        l = (bytes2u(finalState[4]) << 16) | (bytes2u(finalState[10]) << 8) | bytes2u(finalState[5]);
        result.append(to64(l, 4));

        l = bytes2u(finalState[11]);
        result.append(to64(l, 2));

        /* Don't leave anything around in vm they could use. */
        clearbits(finalState);

        return result.toString();
    }

    public static boolean isEqual(String clear, String encrypted) {
        return isEqual(clear.toCharArray(), encrypted);
    }

    public static boolean isEqual(char[] clear, String encrypted) {
        String[] split = encrypted.split("\\$");
        if (split.length != 4)
            return false;
        char[] a = encrypted.toCharArray();
        char[] b = crypt(new String(clear), split[2], "$" + split[1] + "$").toCharArray();
        boolean result = false;
        if ((a == null) || (b == null))
            return (a == b);
        if (a.length == b.length) {
            boolean equals = true;
            for (int i = 0; (i < a.length) && equals; i++)
                equals = (a[i] == b[i]);
            result = equals;
        }
        return result;
    }
}
