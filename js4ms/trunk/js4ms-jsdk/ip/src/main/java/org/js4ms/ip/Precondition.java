package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Precondition.java [org.js4ms.jsdk:ip]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.net.InetAddress;

import org.js4ms.common.util.logging.Logging;


/**
 * Provides static methods that may be used to test various preconditions.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class Precondition {

    /**
     * @param object
     * @throws IllegalArgumentException
     */
    public static void checkReference(Object object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("object parameter must be non-null");
        }
    }

    /**
     * @param args
     * @throws IllegalArgumentException
     */
    public static void checkReferences(Object... args) throws IllegalArgumentException {
        for (Object arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("object parameter must be non-null");
            }
        }
    }

    /**
     * @param srcLength
     * @param newOffset
     * @param newLength
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static void checkBounds(int srcLength, int newOffset, int newLength) throws IllegalArgumentException, IndexOutOfBoundsException {
        if (newOffset < 0 || newLength < 0) {
            throw new IllegalArgumentException("illegal bounds specified - offset or length value is less than zero");
        }
        else if ((newOffset + newLength) > srcLength) {
            throw new IndexOutOfBoundsException("illegal bounds specified - span-offset=" + newOffset + " + span-length="
                                                + newLength + " > buffer-length=" + srcLength);
        }
    }

    /**
     * @param address
     */
    public static void checkAddress(byte[] address) {
        checkReference(address);
        if (address.length != 4 && address.length != 16) {
            throw new IllegalArgumentException("invalid address - the address length must be 4-bytes (IPv4) or 16-bytes (IPv6)");
        }
    }

    /**
     * @param addresses
     */
    public static void checkAddresses(InetAddress... addresses) {
        int length = 0;
        for (InetAddress inetAddress : addresses) {
            checkReference(inetAddress);
            byte[] address = inetAddress.getAddress();
            if (length == 0) {
                length = address.length;
            }
            else if (address.length != length) {
                throw new IllegalArgumentException("invalid address specified - all addresses must have the same length (must be IPv4 or IPv6)");
            }
        }
    }

    /**
     * @param addresses
     */
    public static void checkAddresses(byte[]... addresses) {
        int length = 0;
        for (byte[] address : addresses) {
            checkAddress(address);
            if (length == 0) {
                length = address.length;
            }
            else if (address.length != length) {
                throw new IllegalArgumentException("invalid address specified - all addresses must have the same length (must be IPv4 or IPv6)");
            }
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv4Address(byte[] address) {
        return (address != null && address.length == 4);
    }

    /**
     * @param address
     */
    public static void checkIPv4Address(byte[] address) {
        if (!isIPv4Address(address)) {
            throw new IllegalArgumentException("invalid address - the address length must be 4-bytes (IPv4)");
        }
    }

    /**
     * @param addresses
     */
    public static void checkIPv4Addresses(byte[]... addresses) {
        for (byte[] address : addresses) {
            checkIPv4Address(address);
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv4MulticastAddress(byte[] address) {
        return (isIPv4Address(address) && (address[0] & 0xFF) >= 224 && (address[0] & 0xFF) <= 239);
    }

    /**
     * @param address
     */
    public static void checkIPv4MulticastAddress(byte[] address) {
        if (!isIPv4MulticastAddress(address)) {
            throw new IllegalArgumentException("invalid IPv4 multicast address - " + Logging.address(address)
                                               + " is falls outsid the range 224.0.0.0 thru 239.255.255.255");
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv4ASMMulticastAddress(byte[] address) {
        return (isIPv4MulticastAddress(address) && !isIPv4SSMMulticastAddress(address));
    }

    /**
     * @param address
     */
    public static void checkIPv4ASMMulticastAddress(byte[] address) {
        if (!isIPv4ASMMulticastAddress(address)) {
            throw new IllegalArgumentException("invalid IPv4 ASM multicast address - " + Logging.address(address)
                                               + " falls outside of the ASM range");
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv4SSMMulticastAddress(byte[] address) {
        return (isIPv4Address(address) && (address[0] & 0xFF) == 232);
    }

    /**
     * @param address
     */
    public static void checkIPv4SSMMulticastAddress(byte[] address) {
        if (!isIPv4SSMMulticastAddress(address)) {
            throw new IllegalArgumentException("invalid IPv4 SSM multicast address - " + Logging.address(address)
                                               + " falls outside of the SSM range");
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv6Address(byte[] address) {
        return (address != null && address.length == 16);
    }

    /**
     * @param address
     */
    public static void checkIPv6Address(byte[] address) {
        if (!isIPv6Address(address)) {
            throw new IllegalArgumentException("invalid address - the address length must be 16-bytes (IPv6)");
        }
    }

    /**
     * @param addresses
     */
    public static void checkIPv6Addresses(byte[]... addresses) {
        for (byte[] address : addresses) {
            checkIPv6Address(address);
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv6MulticastAddress(byte[] address) {
        return (isIPv6Address(address) && address[0] == (byte)0xFF);
    }

    /**
     * @param address
     */
    public static void checkIPv6MulticastAddress(byte[] address) {
        if (!isIPv6MulticastAddress(address)) {
            throw new IllegalArgumentException("invalid IPv6 multicast address - the high order address byte must be 0xFF (255)");
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv6ASMMulticastAddress(byte[] address) {
        return (isIPv6MulticastAddress(address) && ((address[1] >> 4) & 0x0F) != 3);
    }

    /**
     * @param address
     */
    public static void checkIPv6ASMMulticastAddress(byte[] address) {
        if (!isIPv6ASMMulticastAddress(address)) {
            throw new IllegalArgumentException("invalid IPv6 ASM multicast address - the specified address falls outside the ASM multicast range");
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isIPv6SSMMulticastAddress(byte[] address) {
        return (isIPv6MulticastAddress(address) && ((address[1] >> 4) & 0x0F) == 3);
    }

    /**
     * @param address
     */
    public static void checkIPv6SSMMulticastAddress(byte[] address) {
        if (!isIPv6SSMMulticastAddress(address)) {
            throw new IllegalArgumentException("invalid IPv6 SSM multicast address - the specified address falls outside the SSM multicast range");
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isMulticastAddress(byte[] address) {
        return isIPv4MulticastAddress(address) || isIPv6MulticastAddress(address);
    }

    /**
     * @param address
     */
    public static void checkMulticastAddress(InetAddress address) {
        checkReference(address);
        checkMulticastAddress(address.getAddress());
    }

    /**
     * @param address
     */
    public static void checkMulticastAddress(byte[] address) {
        checkAddress(address);
        if (address.length == 4) {
            checkIPv4MulticastAddress(address);
        }
        else {
            checkIPv6MulticastAddress(address);
        }
    }

    /**
     * @param address
     */
    public static void checkASMMulticastAddress(InetAddress address) {
        checkReference(address);
        checkASMMulticastAddress(address.getAddress());
    }

    /**
     * @param address
     */
    public static void checkASMMulticastAddress(byte[] address) {
        checkAddress(address);
        if (address.length == 4) {
            checkIPv4ASMMulticastAddress(address);
        }
        else {
            checkIPv6ASMMulticastAddress(address);
        }
    }

    /**
     * @param address
     * @return
     */
    public static boolean isSSMMulticastAddress(InetAddress address) {
        return isSSMMulticastAddress(address.getAddress());
    }

    /**
     * @param address
     * @return
     */
    public static boolean isSSMMulticastAddress(byte[] address) {
        checkAddress(address);
        if (address.length == 4) {
            return isIPv4SSMMulticastAddress(address);
        }
        else {
            return isIPv6SSMMulticastAddress(address);
        }
    }

    /**
     * @param address
     */
    public static void checkSSMMulticastAddress(InetAddress address) {
        checkReference(address);
        checkSSMMulticastAddress(address.getAddress());
    }

    /**
     * @param address
     */
    public static void checkSSMMulticastAddress(byte[] address) {
        checkAddress(address);
        if (address.length == 4) {
            checkIPv4SSMMulticastAddress(address);
        }
        else {
            checkIPv6SSMMulticastAddress(address);
        }
    }

}
