package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPMessage.java [org.js4ms.jsdk:ip]
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


import java.nio.ByteBuffer;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.parser.BufferParserMap;
import org.js4ms.common.util.buffer.parser.KeyedBufferParser;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Loggable;




/**
 * Base interface for all IP protocol messages.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public interface IPMessage
                extends Loggable {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * Base interface for individual IP message parsers.
     */
    public static interface ParserType
                    extends KeyedBufferParser<IPMessage> {

        /**
         * @param buffer
         * @param sourceAddress
         * @param destinationAddress
         * @return
         * @throws MissingParserException
         * @throws ParseException
         */
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException;

    }

    /**
     * Base class for parsers that parse a family of IP messages.
     * Typically used in base message classes associated with a single IP protocol.
     */
    public static class Parser
                    extends BufferParserMap<IPMessage> {

        /**
         * @param buffer
         * @param protocolNumber
         * @param sourceAddress
         * @param destinationAddress
         * @return
         * @throws MissingParserException
         * @throws ParseException
         */
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte protocolNumber,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            ParserType parser = (ParserType) get(protocolNumber);
            if (parser == null) {
                // Check for default parser (null key)
                parser = (ParserType) get(null);
                if (parser == null) {
                    throw new MissingParserException();
                }
            }
            return parser.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * Protocol number used to indicate that no additional IP message
     * headers follow the current header.
     */
    public static final byte NO_NEXT_HEADER = 59;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Returns the IP protocol number of the message.
     * 
     * @return
     */
    public byte getProtocolNumber();

    /**
     * Sets the IP protocol number of the message.
     * Most subclasses override this method to do nothing because
     * the protocol number is defined by the message subclass.
     * 
     * @param protocolNumber
     */
    public void setProtocolNumber(byte protocolNumber);

    /**
     * Returns the protocol number value of the next IP message header.
     * If no IP message follows the current message, this method will
     * return a value of {@link #NO_NEXT_HEADER}.
     * This function is primarily used to return the "Next Header" field in extension
     * headers.
     * See {@link org.js4ms.ip.IPExtensionHeader#NextHeader
     * IPExtensionHeader.NextHeader}.
     * 
     * @return
     */
    public byte getNextProtocolNumber();

    /**
     * Returns the IP message that follows this message,
     * or <code>null</code> if no such message exists.
     * 
     * @return
     */
    public IPMessage getNextMessage();

    /**
     * Sets the IP message that will follow this message.
     * This method is primarily intended for use in extension headers.
     * 
     * @param header
     */
    public void setNextMessage(IPMessage header);

    /**
     * Removes the next IP message that follows this message.
     * This method is primarily intended for with extension headers.
     */
    public void removeNextMessage();

    /**
     * Returns the byte-length of the message header.
     * For fixed-size messages, this is often the same value as the returned from
     * {@link #getTotalLength()}.
     * 
     * @return
     */
    public int getHeaderLength();

    /**
     * Returns the total byte-length of the message including the message header.
     * 
     * @return
     */
    public int getTotalLength();

    /**
     * Writes the message to a byte buffer and advances the current position of that
     * buffer.
     * 
     * @param buffer
     *            - the ByteBuffer into which the message will be written.
     */
    public void writeTo(ByteBuffer buffer);

    /**
     * Updates the checksum of a message contained in the byte buffer;
     * 
     * @param buffer
     *            - a ByteBuffer containing the message.
     * @param sourceAddress
     *            An IPv4 (4-byte) or IPv6 (16-byte) address. Size must match that of the
     *            destination address.
     * @param destinationAddress
     *            An IPv4 (4-byte) or IPv6 (16-byte) address. Size must match that of the
     *            destination address.
     */
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress);

}
