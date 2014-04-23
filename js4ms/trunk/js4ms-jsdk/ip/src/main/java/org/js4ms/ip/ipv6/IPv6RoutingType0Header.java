package org.js4ms.ip.ipv6;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6RoutingType0Header.java [org.js4ms.jsdk:ip]
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
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.Precondition;




/**
 * Represents an IPv6 Routing Header with routing type equal to zero.
 *
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IPv6RoutingType0Header extends IPv6RoutingHeader {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser implements IPv6RoutingHeader.ParserType {

        @Override
        public IPv6RoutingHeader parse(final ByteBuffer buffer) throws ParseException {
            return new IPv6RoutingType0Header(buffer);
        }

        @Override
        public Object getKey() {
            return ROUTING_TYPE;
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final byte ROUTING_TYPE = 0;


    /*-- Member Variables ---------------------------------------------------*/

    /** */
    final private Vector<byte[]> addresses = new Vector<byte[]>();
  

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv6RoutingType0Header(final ByteBuffer buffer) throws ParseException {
        super(buffer);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingType0Header.IPv6RoutingType0Header", buffer));
        }
        
        int count = getNumberOfAddresses();
        for(int i=0; i<count; i++) {
            byte[] address = new byte[4];
            buffer.get(address);
            this.addresses.add(address);
        }

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingType0Header.writeTo", buffer));
        }
        
        //Precondition.checkReference(buffer);
        //Precondition.checkBounds(buffer.length, offset, getTotalLength());
        super.writeTo(buffer);
        Iterator<byte[]> iter = this.addresses.iterator();
        while (iter.hasNext()) {
            buffer.put(iter.next());
        }
    }

    /**
     * 
     * @return
     */
    public int getNumberOfAddresses() {
        return HeaderLength.get(getBufferInternal()) / 2;
    }
    
    /**
     * 
     * @param address
     * @throws UnknownHostException
     */
    public void addAddress(final InetAddress address) throws UnknownHostException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingType0Header.addAddress", Logging.address(address)));
        }
        
        Precondition.checkReference(address);
        addAddress(address.getAddress());
    }

    /**
     * 
     * @param address
     * @return
     */
    public int addAddress(final byte[] address) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingType0Header.addAddress", Logging.address(address)));
        }
        
        //Precondition.checkIPv6Address(address);
        int index = this.addresses.size();
        this.addresses.add(address.clone());
        HeaderLength.set(getBufferInternal(), (byte)(this.addresses.size() * 2));
        return index;
    }

    /**
     * 
     * @param index
     * @return
     */
    public byte[] getAddress(final int index) {
        return this.addresses.get(index);
    }

    /**
     * 
     * @param index
     */
    public void removeAddress(final int index) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingType0Header.removeAddress", index));
        }
        
        this.addresses.remove(index);
    }

    /**
     * 
     * @return
     */
    public byte[] getNextAddress() {
        return getAddress(getNumberOfAddresses() - getSegmentsLeft());
    }

    /**
     * 
     * @param address
     */
    public void setLastAddress(final byte[] address) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingType0Header.setLastAddress", Logging.address(address)));
        }
        
        //Precondition.checkIPv6Address(address);
        this.addresses.set(getNumberOfAddresses() - getSegmentsLeft(), address);
    }
    
}
