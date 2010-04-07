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
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.LoggableBase;
import com.larkwoodlabs.util.logging.Logging;

public class IPinIPHeader extends LoggableBase implements IPMessage {

    public static class Parser implements IPMessage.ParserType {

        IPPacket.Parser ipParser = null;

        public Parser() {
            this(new IPPacket.Parser());
        }

        public Parser(IPPacket.Parser ipParser) {
            setIPHeaderParser(ipParser);
        }

        public void setIPHeaderParser(IPPacket.Parser ipParser) {
            //Precondition.checkReference(ipParser);
            this.ipParser = ipParser;
        }

        public IPPacket.Parser getIPHeaderParser() {
            return this.ipParser;
        }

        @Override
        public IPMessage parse(ByteBuffer buffer) throws ParseException, MissingParserException {
            IPPacket header = this.ipParser.parse(buffer);
            return new IPinIPHeader(header);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

    }

    public static final Logger logger = Logger.getLogger(IPinIPHeader.class.getName());

    public static final byte IP_PROTOCOL_NUMBER = 4;


    protected final String ObjectId = Logging.identify(this);

    private IPPacket encapsulatedHeader;
    
    public IPinIPHeader(IPPacket encapsulatedHeader) {
        //Precondition.checkReference(encapsulatedHeader);
        this.encapsulatedHeader = encapsulatedHeader;
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPinIPHeader.IPinIPHeader", encapsulatedHeader));
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

    private void logState(Logger logger) {
        logger.info(ObjectId + " : protocol="+getProtocolNumber());
        if (this.encapsulatedHeader != null) {
            logger.info(ObjectId + " ----> start encapsulated header");
            this.encapsulatedHeader.log(logger);
            logger.info(ObjectId + " <---- end encapsulated header");
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        // Does nothing in this class
    }

    @Override
    public byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public void setProtocolNumber(byte protocolNumber) {
        // Do nothing - protocol number is set in constructors
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
        // Does nothing in this class
    }

    @Override
    public int getHeaderLength() {
        return 0;
    }

    @Override
    public int getTotalLength() {
        return this.encapsulatedHeader.getHeaderLength() + this.encapsulatedHeader.getPayloadLength();
    }

    @Override
    public void writeTo(ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPinIPHeader.writeTo", buffer));
        }
        
        this.encapsulatedHeader.writeTo(buffer);
    }

    public IPPacket getEncapsulatedHeader() {
        return this.encapsulatedHeader;
    }

}
