package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPDatagram.java [org.js4ms.jsdk:ip]
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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.LoggableBase;
import org.js4ms.common.util.logging.Logging;




/**
 * Represents an IP datagram containing a single IP message.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class IPDatagram
                extends LoggableBase {

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate IPDatagram log entries. */
    public static final Logger logger = Logger.getLogger(IPDatagram.class.getName());

    /**
     * Static instance used to interrupt receivers.
     */
    public static final IPDatagram FINAL = new IPDatagram();

    /*-- Member Variables ---------------------------------------------------*/

    /** */
    InetAddress sourceInetAddress;

    /** */
    InetAddress destinationInetAddress;

    /** */
    IPMessage payload;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    private IPDatagram() {

    }

    /**
     * @param sourceInetAddress
     * @param destinationInetAddress
     * @param payload
     */
    public IPDatagram(final InetAddress sourceInetAddress,
                      final InetAddress destinationInetAddress,
                      final IPMessage payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPDatagram.IPDatagram",
                                        Logging.address(sourceInetAddress),
                                        Logging.address(destinationInetAddress),
                                        payload));
        }

        Precondition.checkAddresses(sourceInetAddress, destinationInetAddress);

        this.sourceInetAddress = sourceInetAddress;
        this.destinationInetAddress = destinationInetAddress;

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * @param sourceAddress
     * @param destinationAddress
     * @param payload
     */
    public IPDatagram(final byte[] sourceAddress,
                      final byte[] destinationAddress,
                      final IPMessage payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPDatagram.IPDatagram",
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress),
                                        payload));
        }

        Precondition.checkAddresses(sourceAddress, destinationAddress);

        try {
            this.sourceInetAddress = InetAddress.getByAddress(sourceAddress);
            this.destinationInetAddress = InetAddress.getByAddress(destinationAddress);
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }

        this.payload = payload;

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(final Logger logger,final Level level) {
        super.log(logger,level);
        logState(logger,level);
    }

    /**
     * Logs state variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": source=" + Logging.address(getSourceAddress())));
        logger.log(level,this.log.msg(": destination=" + Logging.address(getDestinationAddress())));
        logger.log(level,this.log.msg("----> payload"));
        this.payload.log(logger,level);
        logger.log(level,this.log.msg("<---- payload"));
    }

    /**
     * @param buffer
     */
    public void writeTo(final ByteBuffer buffer) {
        this.payload.writeTo(buffer);
    }

    /**
     * @return
     */
    public InetAddress getSourceInetAddress() {
        return this.sourceInetAddress;
    }

    /**
     * @return
     */
    public byte[] getSourceAddress() {
        return this.sourceInetAddress.getAddress();
    }

    /**
     * @return
     */
    public InetAddress getDestinationInetAddress() {
        return this.destinationInetAddress;
    }

    /**
     * @return
     */
    public byte[] getDestinationAddress() {
        return this.destinationInetAddress.getAddress();
    }

    /**
     * @param sourceInetAddress
     * @param destinationInetAddress
     */
    public void setAddresses(final InetAddress sourceInetAddress,
                             final InetAddress destinationInetAddress) {
        Precondition.checkAddresses(sourceInetAddress, destinationInetAddress);
        this.sourceInetAddress = sourceInetAddress;
        this.destinationInetAddress = destinationInetAddress;
    }

    /**
     * @param sourceAddress
     * @param destinationAddress
     */
    public void setAddresses(final byte[] sourceAddress,
                             final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPDatagram.setAddresses", Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }

        Precondition.checkAddresses(sourceAddress, destinationAddress);

        try {
            this.sourceInetAddress = InetAddress.getByAddress(sourceAddress);
            this.destinationInetAddress = InetAddress.getByAddress(destinationAddress);
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    /**
     * @return
     */
    public IPMessage getPayload() {
        return this.payload;
    }

    /**
     * @param payload
     */
    public void setPayload(final IPMessage payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPDatagram.setPayload", payload));
        }

        this.payload = payload;
    }
}
