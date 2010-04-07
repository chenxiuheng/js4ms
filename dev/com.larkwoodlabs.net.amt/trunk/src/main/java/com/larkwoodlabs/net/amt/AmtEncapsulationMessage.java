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

package com.larkwoodlabs.net.amt;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for AMT message classes that carry an IP packet payload.<br/>
 * @see AmtMembershipQueryMessage
 * @see AmtMembershipUpdateMessage
 * @see AmtMulticastDataMessage
 * 
 * @author Gregory Bumgardner
 */
abstract class AmtEncapsulationMessage extends AmtMessage {


    /*-- Inner Classes ------------------------------------------------------*/

    public static abstract class Parser implements AmtMessage.ParserType {

        IPPacket.Parser ipParser = null;

        protected Parser(final IPPacket.Parser ipParser) {
            setIPPacketParser(ipParser);
        }

        public void setIPPacketParser(final IPPacket.Parser ipParser) {
            this.ipParser = ipParser;
        }

        public IPPacket.Parser getIPPacketParser() {
            return this.ipParser;
        }

        protected abstract AmtEncapsulationMessage constructMessage(final ByteBuffer buffer) throws ParseException;
        
        @Override
        public AmtMessage parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
            //Precondition.checkReference(buffer);
            AmtEncapsulationMessage message = constructMessage(buffer);
            if (this.ipParser != null) {
                message.verifyPacketChecksum(this.ipParser);
                message.parsePacket(this.ipParser);
            }
            return message;
        }

    }


    
    /*-- Member Variables ---------------------------------------------------*/

    ByteBuffer unparsedPacket = null;
    
    IPPacket packet = null;


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param size
     * @param type
     * @param packet
     */
    protected AmtEncapsulationMessage(final int size, final byte type, final IPPacket packet) {
        super(size,type);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.AmtEncapsulationMessage", size, type, packet));
        }
        
        setType(type);
        setPacket(packet);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @param baseMessageLength
     */
    protected AmtEncapsulationMessage(final ByteBuffer buffer, final int baseMessageLength) {
        super(consume(buffer, baseMessageLength));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.AmtEncapsulationMessage", buffer, baseMessageLength));
        }
        this.unparsedPacket = consume(buffer, buffer.remaining());
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    @Override
    public void log(final Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * 
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " ----> start encapsulated IP packet");
        if (this.packet != null) {
            this.packet.log(logger);
        }
        logger.info(ObjectId + " <---- end encapsulated IP packet");
    }

    @Override
    public final void writeTo(final ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.writeTo", buffer));
        }
        super.writeTo(buffer);
        if (this.packet != null) {
            this.packet.writeTo(buffer);
            // No need to write the checksum here - the packet writeTo() method handles it
        }
        else if (this.unparsedPacket != null) {
            buffer.put(this.unparsedPacket);
        }
    }

    @Override
    public final int getTotalLength() {
        return getBufferInternal().limit() + (getPacket() != null ? getPacket().getTotalLength() : 0);
    }

    /**
     * 
     * @return
     */
    public final IPPacket getPacket() {
        return this.packet;
    }
    
    /**
     * 
     * @param packet
     */
    public final void setPacket(final IPPacket packet) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.setPacket", packet));
        }
        
        this.packet = packet;
    }
    
    /**
     * 
     * @param parser
     * @throws MissingParserException
     * @throws ParseException
     */
    public final void verifyPacketChecksum(final IPPacket.Parser parser) throws MissingParserException, ParseException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.verifyPacketChecksum", parser));
        }
        
        if (this.unparsedPacket != null) {
            // Check the IP packet checksum
            parser.verifyChecksum(this.unparsedPacket);
        }
    }
    
    /**
     * 
     * @param parser
     * @return
     * @throws ParseException
     * @throws MissingParserException
     */
    public final IPPacket parsePacket(final IPPacket.Parser parser) throws ParseException, MissingParserException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.parsePacket", parser));
        }
        
        IPPacket packet = null;
        if (this.unparsedPacket != null) {
            // Parse the IP packet containing an IPv4 or IPv6 packet
            // No need to check the checksum first - the packet constructor handles it (only required in IPv4 anyway)
            packet = parser.parse(this.unparsedPacket);
            this.unparsedPacket.rewind();
        }
        setPacket(packet);
        return packet;
    }

}
