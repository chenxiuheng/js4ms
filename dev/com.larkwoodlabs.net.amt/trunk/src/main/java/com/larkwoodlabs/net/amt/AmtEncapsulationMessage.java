/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtEncapsulationMessage.java (com.larkwoodlabs.net.amt)
 * 
 * Copyright � 2010-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * 
 * @see AmtMembershipQueryMessage
 * @see AmtMembershipUpdateMessage
 * @see AmtMulticastDataMessage
 * @author Gregory Bumgardner (gbumgard)
 */
abstract class AmtEncapsulationMessage
                extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * A parser for AMT messages that encapsulate an IP packet.
     * 
     */
    public static abstract class Parser
                    implements AmtMessage.ParserType {

        IPPacket.BufferParser ipParser = null;

        /**
         * @param ipParser
         */
        protected Parser(final IPPacket.BufferParser ipParser) {
            setIPPacketParser(ipParser);
        }

        /**
         * @param ipParser
         */
        public void setIPPacketParser(final IPPacket.BufferParser ipParser) {
            this.ipParser = ipParser;
        }

        /**
         * @return
         */
        public IPPacket.BufferParser getIPPacketParser() {
            return this.ipParser;
        }

        /**
         * @param buffer
         * @return
         * @throws ParseException
         */
        protected abstract AmtEncapsulationMessage constructMessage(final ByteBuffer buffer) throws ParseException;

        @Override
        public AmtMessage parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
            // Precondition.checkReference(buffer);
            AmtEncapsulationMessage message = constructMessage(buffer);
            if (this.ipParser != null) {
                message.verifyPacketChecksum(this.ipParser);
                message.parsePacket(this.ipParser);
            }
            return message;
        }

    }

    /*-- Member Variables ---------------------------------------------------*/

    protected ByteBuffer unparsedPacket = null;

    protected IPPacket packet = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance with the specified message size, message type, and
     * encapsulated IP packet.
     * @param size The number of bytes that appear in front of the encapsulated packet.
     * @param type The message type.
     * @param packet The IP packet to be encapsulated in the message.
     */
    protected AmtEncapsulationMessage(final int size, final byte type, final IPPacket packet) {
        super(size, type);

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
     * Constructs an instance from the contents of a ByteBuffer.
     * @param buffer The ByteBuffer containing the message.
     * @param baseMessageLength The number of bytes that appear in front of the encapsulated packet.
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
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     *            The logger that will be used to generate the log messages.
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " ----> start encapsulated IP packet");
        if (this.packet != null) {
            this.packet.log(logger);
        }
        logger.info(ObjectId + " <---- end encapsulated IP packet");
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.writeTo", buffer));
        }
        super.writeTo(buffer);
        if (this.packet != null) {
            this.packet.writeTo(buffer);
            // No need to write the checksum here - the packet writeTo() method handles it
        }
    }

    @Override
    public final int getTotalLength() {
        return getBufferInternal().limit() + (getPacket() != null ? getPacket().getTotalLength() : 0);
    }

    /**
     * Gets the encapsulated IP packet.
     * 
     * @return An IPPacket representation of the encapsulated packet.
     */
    public final IPPacket getPacket() {
        return this.packet;
    }

    /**
     * Sets the encapsulated IP packet.
     * 
     * @param packet
     *            An IPPacket representation of the encapsulated packet.
     */
    public final void setPacket(final IPPacket packet) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.setPacket", packet));
        }

        this.packet = packet;
    }

    /**
     * @param parser
     * @throws MissingParserException
     * @throws ParseException
     */
    public final void verifyPacketChecksum(final IPPacket.BufferParser parser) throws MissingParserException, ParseException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.verifyPacketChecksum", parser));
        }

        if (this.unparsedPacket != null) {
            // Check the IP packet checksum
            parser.verifyChecksum(this.unparsedPacket);
        }
    }

    /**
     * @param parser
     * @return
     * @throws ParseException
     * @throws MissingParserException
     */
    public final IPPacket parsePacket(final IPPacket.BufferParser parser) throws ParseException, MissingParserException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtEncapsulationMessage.parsePacket", parser));
        }

        IPPacket packet = null;
        if (this.unparsedPacket != null) {
            // Parse the IP packet containing an IPv4 or IPv6 packet
            // No need to check the checksum first - the packet constructor handles it
            // (only required in IPv4 anyway)
            packet = parser.parse(this.unparsedPacket);
            this.unparsedPacket.rewind();
        }
        setPacket(packet);
        return packet;
    }

}
