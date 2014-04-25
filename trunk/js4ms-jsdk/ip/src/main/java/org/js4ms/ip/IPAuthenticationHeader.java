package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPAuthenticationHeader.java [org.js4ms.jsdk:ip]
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.parser.MissingParserException;




/**
 * An IP authentication header.
 * See <a href="http://tools.ietf.org/html/rfc2402">[RFC-2402]</a>
 * <p>
 * The protocol header (IPv4, IPv6, or Extension) immediately preceding the AH header will
 * contain the value 51 in its Protocol (IPv4) or Next Header (IPv6, Extension) field
 * [STD-2].
 * <h3>Header Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  | Next Header   |  Payload Len  |          RESERVED             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                 Security Parameters Index (SPI)               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                    Sequence Number Field                      |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  ~                Authentication Data (variable)                 ~
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * The following subsections define the fields that comprise the AH format. All the fields
 * described here are mandatory, i.e., they are always present in the AH format and are
 * included in the Integrity Check Value (ICV) computation (see Sections 2.6 and 3.3.3).
 * <p>
 * <dl>
 * <dt><u>Next Header</u>
 * <dt>
 * <p>
 * <dd>The Next Header is an 8-bit field that identifies the type of the next payload
 * after the Authentication Header. The value of this field is chosen from the set of IP
 * Protocol Numbers defined in the most recent "Assigned Numbers" [STD-2] RFC from the
 * Internet Assigned Numbers Authority (IANA).</dd>
 * <p>
 * <dt><u>Payload Length</u>
 * <dt>
 * <p>
 * <dd>This 8-bit field specifies the length of AH in 32-bit words (4-byte units), minus
 * "2". (All IPv6 extension headers, as per RFC 1883, encode the "Hdr Ext Len" field by
 * first subtracting 1 (64-bit word) from the header length (measured in 64-bit words). AH
 * is an IPv6 extension header. However, since its length is measured in 32-bit words, the
 * "Payload Length" is calculated by subtracting 2 (32 bit words).) In the "standard" case
 * of a 96-bit authentication value plus the 3 32-bit word fixed portion, this length
 * field will be "4". A "null" authentication algorithm may be used only for debugging
 * purposes. Its use would result in a "1" value for this field for IPv4 or a "2" for
 * IPv6, as there would be no corresponding Authentication Data field (see Section
 * 3.3.3.2.1 on "Authentication Data Padding").</dd>
 * <p>
 * <dt><u>Reserved</u>
 * <dt>
 * <p>
 * <dd>This 16-bit field is reserved for future use. It MUST be set to "zero." (Note that
 * the value is included in the Authentication Data calculation, but is otherwise ignored
 * by the recipient.)</dd>
 * <p>
 * <dt><u>Security Parameters Index (SPI)</u>
 * <dt>
 * <p>
 * <dd>The SPI is an arbitrary 32-bit value that, in combination with the destination IP
 * address and security protocol (AH), uniquely identifies the Security Association for
 * this datagram. The set of SPI values in the range 1 through 255 are reserved by the
 * Internet Assigned Numbers Authority (IANA) for future use; a reserved SPI value will
 * not normally be assigned by IANA unless the use of the assigned SPI value is specified
 * in an RFC. It is ordinarily selected by the destination system upon establishment of an
 * SA (see the Security Architecture document for more details).
 * <p>
 * The SPI value of zero (0) is reserved for local, implementation- specific use and MUST
 * NOT be sent on the wire. For example, a key management implementation MAY use the zero
 * SPI value to mean "No Security Association Exists" during the period when the IPsec
 * implementation has requested that its key management entity establish a new SA, but the
 * SA has not yet been established.</dd>
 * <p>
 * <dt><u>Sequence Number</u>
 * <dt>
 * <p>
 * <dd>This unsigned 32-bit field contains a monotonically increasing counter value
 * (sequence number). It is mandatory and is always present even if the receiver does not
 * elect to enable the anti-replay service for a specific SA. Processing of the Sequence
 * Number field is at the discretion of the receiver, i.e., the sender MUST always
 * transmit this field, but the receiver need not act upon it (see the discussion of
 * Sequence Number Verification in the "Inbound Packet Processing" section below).
 * <p>
 * The sender's counter and the receiver's counter are initialized to 0 when an SA is
 * established. (The first packet sent using a given SA will have a Sequence Number of 1;
 * see Section 3.3.2 for more details on how the Sequence Number is generated.) If
 * anti-replay is enabled (the default), the transmitted Sequence Number must never be
 * allowed to cycle. Thus, the sender's counter and the receiver's counter MUST be reset
 * (by establishing a new SA and thus a new key) prior to the transmission of the 2^32nd
 * packet on an SA.</dd>
 * <p>
 * <dt><u>Authentication Data</u>
 * <dt>
 * <p>
 * <dd>This is a variable-length field that contains the Integrity Check Value (ICV) for
 * this packet. The field must be an integral multiple of 32 bits in length. The details
 * of the ICV computation are described in Section 3.3.2 below. This field may include
 * explicit padding. This padding is included to ensure that the length of the AH header
 * is an integral multiple of 32 bits (IPv4) or 64 bits (IPv6). All implementations MUST
 * support such padding. Details of how to compute the required padding length are
 * provided below. The authentication algorithm specification MUST specify the length of
 * the ICV and the comparison rules and processing steps for validation.</dd>
 * <p>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IPAuthenticationHeader
                extends IPExtensionHeader {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPMessage.ParserType {

        @Override
        public IPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IPAuthenticationHeader(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Protocol number for Authentication headers. */
    public static final byte IP_PROTOCOL_NUMBER = 51;

    /*-- Member Variables ---------------------------------------------------*/

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param buffer
     * @throws ParseException
     */
    public IPAuthenticationHeader(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, ((HeaderLength.get(buffer) + 2) * 4)), IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPAuthenticationHeader.IPAuthenticationHeader", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger,level);
        logState(logger,level);
    }

    private void logState(final Logger logger, final Level level) {
    }

    @Override
    public final int getHeaderLength() {
        return (HeaderLength.get(getBufferInternal()) + 2) * 4;
    }
}
