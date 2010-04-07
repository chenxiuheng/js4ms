/*
 * Copyright � 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.net.ip.ipv6;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.util.logging.Logging;

/**
 * 
 *
 * @author Gregory Bumgardner
 */
public final class IPv6RoutingType0Header extends IPv6RoutingHeader {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements IPv6RoutingHeader.ParserType {

        @Override
        public IPv6RoutingHeader parse(ByteBuffer buffer) throws ParseException {
            return new IPv6RoutingType0Header(buffer);
        }

        @Override
        public Object getKey() {
            return ROUTING_TYPE;
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte ROUTING_TYPE = 0;


    /*-- Member Variables ---------------------------------------------------*/

    private Vector<byte[]> addresses = new Vector<byte[]>();
  

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv6RoutingType0Header(ByteBuffer buffer) throws ParseException {
        super(buffer);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingType0Header.IPv6RoutingType0Header", buffer));
        }
        
        int count = getNumberOfAddresses();
        for(int i=0; i<count; i++) {
            byte[] address = new byte[4];
            buffer.get(address);
            this.addresses.add(address);
        }

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }
    
    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }

    private void logState(Logger logger) {
        
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingType0Header.writeTo", buffer));
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
    public void addAddress(InetAddress address) throws UnknownHostException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingType0Header.addAddress", Logging.address(address)));
        }
        
        Precondition.checkReference(address);
        addAddress(address.getAddress());
    }

    /**
     * 
     * @param address
     * @return
     */
    public int addAddress(byte[] address) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingType0Header.addAddress", Logging.address(address)));
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
    public byte[] getAddress(int index) {
        return this.addresses.get(index);
    }
    
    /**
     * 
     * @param index
     */
    public void removeAddress(int index) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingType0Header.removeAddress", index));
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
    public void setLastAddress(byte[] address) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingType0Header.setLastAddress", Logging.address(address)));
        }
        
        //Precondition.checkIPv6Address(address);
        this.addresses.set(getNumberOfAddresses() - getSegmentsLeft(), address);
    }
    
}