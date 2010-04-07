/*
 * Copyright © 2009-2010 Larkwood Labs Software.
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

package com.larkwoodlabs.net.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.util.logging.LoggableBase;
import com.larkwoodlabs.util.logging.Logging;

public final class IPDatagram extends LoggableBase {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(IPDatagram.class.getName());

    /**
     * Static instance used to interrupt receivers.
     */
    public static final IPDatagram FINAL = new IPDatagram();


    /*-- Member Variables ---------------------------------------------------*/

    InetAddress sourceInetAddress;
    InetAddress destinationInetAddress;
    
    IPMessage payload;


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    private IPDatagram() {
        
    }

    /**
     * 
     * @param sourceInetAddress
     * @param destinationInetAddress
     * @param payload
     */
    public IPDatagram(InetAddress sourceInetAddress, InetAddress destinationInetAddress, IPMessage payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "IPDatagram.IPDatagram",
                                        Logging.address(sourceInetAddress),
                                        Logging.address(destinationInetAddress),
                                        payload));
        }

        Precondition.checkAddresses(sourceInetAddress, destinationInetAddress);

        this.sourceInetAddress = sourceInetAddress;
        this.destinationInetAddress = destinationInetAddress;
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param sourceAddress
     * @param destinationAddress
     * @param payload
     */
    public IPDatagram(byte[] sourceAddress, byte[] destinationAddress, IPMessage payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "IPDatagram.IPDatagram",
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
            logState(logger);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs state variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : source="+Logging.address(getSourceAddress()));
        logger.info(ObjectId + " : destination="+Logging.address(getDestinationAddress()));
        logger.info(ObjectId + " ----> payload");
        this.payload.log(logger);
        logger.info(ObjectId + " <---- payload");
    }

    /**
     * 
     * @param buffer
     */
    public void writeTo(ByteBuffer buffer) {
        this.payload.writeTo(buffer);
    }

    /**
     * 
     * @return
     */
    public InetAddress getSourceInetAddress() {
        return this.sourceInetAddress;
    }

    /**
     * 
     * @return
     */
    public byte[] getSourceAddress() {
        return this.sourceInetAddress.getAddress();
    }

    /**
     * 
     * @return
     */
    public InetAddress getDestinationInetAddress() {
        return this.destinationInetAddress;
    }

    /**
     * 
     * @return
     */
    public byte[] getDestinationAddress() {
        return this.destinationInetAddress.getAddress();
    }

    /**
     * 
     * @param sourceInetAddress
     * @param destinationInetAddress
     */
    public void setAddresses(InetAddress sourceInetAddress, InetAddress destinationInetAddress) {
        Precondition.checkAddresses(sourceInetAddress, destinationInetAddress);
        this.sourceInetAddress = sourceInetAddress;
        this.destinationInetAddress = destinationInetAddress;
    }

    /**
     * 
     * @param sourceAddress
     * @param destinationAddress
     */
    public void setAddresses(byte[] sourceAddress, byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPDatagram.setAddresses", Logging.address(sourceAddress), Logging.address(destinationAddress)));
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
     * 
     * @return
     */
    public IPMessage getPayload() {
        return this.payload;
    }

    /**
     * 
     * @param payload
     */
    public void setPayload(IPMessage payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPDatagram.setPayload", payload));
        }

        this.payload = payload;
    }
}
