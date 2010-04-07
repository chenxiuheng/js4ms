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

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

public class IPPayload extends BufferBackedObject implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements IPMessage.ParserType {

        @Override
        public IPPayload parse(ByteBuffer buffer) {
            return new IPPayload(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return null; // Any Protocol
        }

    }

    /*-- Member Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(IPPayload.class.getName());

    private byte protocolNumber;
 
    
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param buffer
     */
    public IPPayload(ByteBuffer buffer) {
        this((byte)0, buffer);
    }

    /**
     * 
     * @param protocolNumber
     * @param buffer
     */
    public IPPayload(byte protocolNumber, ByteBuffer buffer) {
        super(buffer);
        this.protocolNumber = protocolNumber;

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "IPPayload.IPPayload", protocolNumber, buffer));
            logState(logger);
        }
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : protocol="+getProtocolNumber());
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        // Does nothing in this class
    }

    @Override
    public byte getProtocolNumber() {
        return this.protocolNumber;
    }

    @Override
    public void setProtocolNumber(byte protocolNumber) {
        this.protocolNumber = protocolNumber;
    }
    
    @Override
    public byte getNextProtocolNumber() {
        return NO_NEXT_HEADER;
    }

    @Override
    public IPMessage getNextMessage() {
        return null;
    }

    @Override
    public void setNextMessage(IPMessage header) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNextMessage() {
        // Does nothing
    }

    @Override
    public int getHeaderLength() {
        return getBufferInternal().limit();
    }

    @Override
    public int getTotalLength() {
        return getBufferInternal().limit();
    }

}
