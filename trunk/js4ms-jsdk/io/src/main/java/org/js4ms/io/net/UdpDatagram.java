package org.js4ms.io.net;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * UdpDatagram.java [org.js4ms.jsdk:io]
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


import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.LoggableBase;
import org.js4ms.common.util.logging.Logging;


/**
 * A representation of a UDP datagram.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class UdpDatagram
                extends LoggableBase {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(UdpDatagram.class.getName());

    public static final byte[] INADDR_ANY = {
                    0, 0, 0, 0
    };

    public static final byte[] IN6ADDR_ANY = {
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    public static final short ETHEREAL_PORT = 0;

    /**
     * Static instance used to interrupt receivers.
     */
    public static final UdpDatagram FINAL = new UdpDatagram();

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @param destinationAddress
     * @return
     */
    // TODO move this somewhere else
    public static byte[] getLocalSourceAddress(final byte[] destinationAddress) {
        // Precondition.checkAddress(destinationAddress);
        try {
            InetAddress localHostAddress = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHostAddress);
            if (networkInterface != null) {
                Enumeration<InetAddress> iter = networkInterface.getInetAddresses();
                while (iter.hasMoreElements()) {
                    localHostAddress = iter.nextElement();
                    byte[] sourceAddress = localHostAddress.getAddress();
                    if (sourceAddress.length == destinationAddress.length) {
                        return sourceAddress;
                    }
                }
            }
            if (destinationAddress.length == 4) {
                return INADDR_ANY;
            }
            else {
                return IN6ADDR_ANY;
            }
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * @param object
     * @throws IllegalArgumentException
     */
    public static void checkReference(Object object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("address parameter must be non-null");
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

    /*-- Member Variables ---------------------------------------------------*/

    InetSocketAddress sourceSocketAddress = null;

    byte[] sourceAddress = null;

    InetSocketAddress destinationSocketAddress = null;

    byte[] destinationAddress = null;

    int sourcePort;

    int destinationPort;

    ByteBuffer payload;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     *
     */
    protected UdpDatagram() {

    }

    /**
     * @param destinationSocketAddress
     * @param payload
     */
    public UdpDatagram(final InetSocketAddress destinationSocketAddress, final ByteBuffer payload) {
        this(destinationSocketAddress == null ? null : destinationSocketAddress.getAddress().getAddress(), destinationSocketAddress
                        .getPort(), payload);

        this.destinationSocketAddress = destinationSocketAddress;

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry(
                                       "UdpDatagram.UdpDatagram",
                                       Logging.address(destinationSocketAddress),
                                       payload));
        }

    }

    /**
     * @param destinationAddress
     * @param destinationPort
     * @param payload
     */
    public UdpDatagram(final InetAddress destinationAddress, final int destinationPort, final ByteBuffer payload) {
        this(destinationAddress == null ? null : destinationAddress.getAddress(), destinationPort, payload);

        this.destinationSocketAddress = new InetSocketAddress(destinationAddress, destinationPort);

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry(
                                       "UdpDatagram.UdpDatagram",
                                       Logging.address(destinationAddress),
                                       destinationPort,
                                       payload));
        }

    }

    /**
     * @param destinationAddress
     * @param destinationPort
     * @param payload
     */
    public UdpDatagram(final byte[] destinationAddress, final int destinationPort, final ByteBuffer payload) {
        this(destinationAddress.length == 4 ? INADDR_ANY : IN6ADDR_ANY,
             ETHEREAL_PORT,
             destinationAddress,
             destinationPort,
             payload);

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry("UdpDatagram.UdpDatagram", Logging.address(destinationAddress), destinationPort,
                                       payload));
        }
    }

    /**
     * @param sourceSocketAddress
     * @param destinationSocketAddress
     * @param payload
     */
    public UdpDatagram(final InetSocketAddress sourceSocketAddress,
                       final InetSocketAddress destinationSocketAddress,
                       final ByteBuffer payload) {
        this(sourceSocketAddress == null ? null : sourceSocketAddress.getAddress().getAddress(),
             sourceSocketAddress.getPort(),
             destinationSocketAddress.getAddress().getAddress(),
             destinationSocketAddress.getPort(),
             payload);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "UdpDatagram.UdpDatagram",
                                        Logging.address(sourceSocketAddress),
                                        Logging.address(destinationSocketAddress),
                                        payload));
        }

        this.destinationSocketAddress = destinationSocketAddress;
        this.sourceSocketAddress = sourceSocketAddress;

    }

    /**
     * @param sourceInetAddress
     * @param sourcePort
     * @param destinationInetAddress
     * @param destinationPort
     * @param payload
     */
    public UdpDatagram(final InetAddress sourceInetAddress,
                       final int sourcePort,
                       final InetAddress destinationInetAddress,
                       final int destinationPort,
                       final ByteBuffer payload) {
        this(sourceInetAddress == null ? null : sourceInetAddress.getAddress(),
             sourcePort,
             destinationInetAddress.getAddress(),
             destinationPort,
             payload);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "UdpDatagram.UdpDatagram",
                                        Logging.address(sourceAddress),
                                        sourcePort,
                                        Logging.address(destinationAddress),
                                        destinationPort,
                                        payload));
        }

        this.destinationSocketAddress = new InetSocketAddress(destinationInetAddress, destinationPort);
        this.sourceSocketAddress = new InetSocketAddress(sourceInetAddress, sourcePort);

    }

    /**
     * @param sourceAddress
     * @param sourcePort
     * @param destinationAddress
     * @param destinationPort
     * @param payload
     */
    public UdpDatagram(final byte[] sourceAddress,
                       final int sourcePort,
                       final byte[] destinationAddress,
                       final int destinationPort,
                       final ByteBuffer payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "UdpDatagram.UdpDatagram",
                                        Logging.address(sourceAddress),
                                        destinationPort,
                                        Logging.address(destinationAddress),
                                        destinationPort,
                                        payload));
        }

        checkAddresses(sourceAddress, destinationAddress);

        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;

        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;

        this.payload = payload.slice();

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": source=" + Logging.address(getSourceAddress()) + ":" + getSourcePort()));
        logger.log(level,this.log.msg(": destination=" + Logging.address(getDestinationAddress()) + ":" + getDestinationPort()));
        logger.log(level,this.log.msg(": payload buffer=" + this.payload.array() +
                                 " offset=" + this.payload.arrayOffset() +
                                 " limit=" + this.payload.limit()));
    }

    /**
     * @param buffer
     */
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpDatagram.writeTo", buffer));
        }

        this.payload.rewind();
        buffer.put(this.payload);
        this.payload.rewind();
    }

    /**
     * Constructs a DatagramPacket bound the UdpPacket payload buffer.
     */
    public DatagramPacket constructDatagramPacket() {
        return new DatagramPacket(this.payload.array(),
                                  this.payload.arrayOffset(),
                                  this.payload.limit(),
                                  getDestinationInetAddress(),
                                  getDestinationPort());
    }

    /**
     * @return
     */
    public byte[] getSourceAddress() {
        return this.sourceAddress.clone();
    }

    /**
     * @return
     */
    public InetAddress getSourceInetAddress() {
        if (this.sourceSocketAddress == null) {
            try {
                this.sourceSocketAddress = new InetSocketAddress(InetAddress.getByAddress(this.sourceAddress), this.sourcePort);
            }
            catch (UnknownHostException e) {
                // Only thrown for invalid address length
                throw new Error(e);
            }
        }
        return this.sourceSocketAddress.getAddress();
    }

    /**
     * @return
     */
    public InetSocketAddress getSourceSocketAddress() {
        if (this.sourceSocketAddress == null) {
            try {
                this.sourceSocketAddress = new InetSocketAddress(InetAddress.getByAddress(this.sourceAddress), this.sourcePort);
            }
            catch (UnknownHostException e) {
                // Only thrown for invalid address length
                throw new Error(e);
            }
        }
        return this.sourceSocketAddress;
    }

    /**
     * @param sourceInetAddress
     * @param sourcePort
     */
    public void setSourceAddress(final InetAddress sourceInetAddress, final int sourcePort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpDatagram.setSourceAddress", Logging.address(sourceAddress), sourcePort));
        }

        this.sourceSocketAddress = new InetSocketAddress(sourceInetAddress, sourcePort);
        this.sourceAddress = sourceInetAddress.getAddress();
        this.sourcePort = sourcePort;
    }

    /**
     * @param sourceAddress
     * @param sourcePort
     */
    public void setSourceAddress(final byte[] sourceAddress, final int sourcePort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpDatagram.setSourceAddress", Logging.address(sourceAddress), sourcePort));
        }

        if (this.sourceSocketAddress != null) {
            try {
                this.sourceSocketAddress = new InetSocketAddress(InetAddress.getByAddress(sourceAddress), sourcePort);
            }
            catch (UnknownHostException e) {
                throw new Error(e);
            }
        }
        this.sourceAddress = sourceAddress.clone();
        this.sourcePort = sourcePort;
    }

    /**
     * @return
     */
    public int getSourcePort() {
        return this.sourcePort;
    }

    /**
     * @return
     */
    public byte[] getDestinationAddress() {
        return this.destinationAddress.clone();
    }

    /**
     * @return
     */
    public InetAddress getDestinationInetAddress() {
        if (this.destinationSocketAddress == null) {
            try {
                this.destinationSocketAddress = new InetSocketAddress(InetAddress.getByAddress(this.destinationAddress),
                                                                      this.destinationPort);
            }
            catch (UnknownHostException e) {
                throw new Error(e);
            }
        }
        return this.destinationSocketAddress.getAddress();
    }

    /**
     * @return
     */
    public InetSocketAddress getDestinationSocketAddress() {
        if (this.destinationSocketAddress == null) {
            try {
                this.destinationSocketAddress = new InetSocketAddress(InetAddress.getByAddress(this.destinationAddress),
                                                                      this.destinationPort);
            }
            catch (UnknownHostException e) {
                throw new Error(e);
            }
        }
        return this.destinationSocketAddress;
    }

    /**
     * @param destinationAddress
     * @param destinationPort
     */
    public void setDestinationAddress(final InetSocketAddress destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpDatagram.setDestinationAddress", Logging.address(destinationAddress)));
        }

        this.destinationSocketAddress = destinationAddress;
        this.destinationAddress = this.destinationSocketAddress.getAddress().getAddress();
        this.destinationPort = destinationAddress.getPort();
    }

    /**
     * @param destinationInetAddress
     * @param destinationPort
     */
    public void setDestinationAddress(final InetAddress destinationInetAddress, final int destinationPort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpDatagram.setDestinationAddress", Logging.address(destinationAddress),
                                        destinationPort));
        }

        this.destinationSocketAddress = new InetSocketAddress(destinationInetAddress, destinationPort);
        this.destinationAddress = this.destinationSocketAddress.getAddress().getAddress();
        this.destinationPort = destinationPort;
    }

    /**
     * @param destinationAddress
     * @param destinationPort
     */
    public void setDestinationAddress(final byte[] destinationAddress, final int destinationPort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpDatagram.setDestinationAddress", Logging.address(destinationAddress)));
        }

        if (this.destinationSocketAddress != null) {
            try {
                this.destinationSocketAddress = new InetSocketAddress(InetAddress.getByAddress(destinationAddress), destinationPort);
            }
            catch (UnknownHostException e) {
                throw new Error(e);
            }
        }
        this.destinationAddress = destinationAddress.clone();
        this.destinationPort = destinationPort;
    }

    /**
     * @return
     */
    public int getDestinationPort() {
        return this.destinationPort;
    }

    /**
     * @param sourceAddress
     * @param sourcePort
     * @param destinationAddress
     * @param destinationPort
     */
    public void setAddresses(final byte[] sourceAddress,
                             final int sourcePort,
                             final byte[] destinationAddress,
                             final int destinationPort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "UdpDatagram.setAddresses",
                                        Logging.address(sourceAddress),
                                        sourcePort,
                                        Logging.address(destinationAddress),
                                        destinationPort));
        }

        checkAddresses(sourceAddress, destinationAddress);
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
    }

    /**
     * Returns the current size of the datagram payload.
     */
    public int getPayloadLength() {
        return this.payload.limit();
    }

    /**
     * @return
     */
    public ByteBuffer getPayload() {
        return this.payload.duplicate();
    }

    /**
     * Returns a byte array containing a copy of the current packet payload,
     * whether it be a byte array or ApplicationMessage.
     */
    public ByteBuffer copyPayload() {
        byte[] buffer = new byte[this.payload.remaining()];
        this.payload.get(buffer);
        this.payload.flip();
        return ByteBuffer.wrap(buffer);
    }

}
