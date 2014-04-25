package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPEncapsulatingSecurityPayload.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.BufferBackedObject;
import org.js4ms.common.util.buffer.parser.MissingParserException;




/**
 * An Encapsulating Security Payload packet.
 * See <a href="http://tools.ietf.org/html/rfc2406">[RFC-2406]</a>
 * <p>
 * The protocol header (IPv4, IPv6, or Extension) immediately preceding the ESP header
 * will contain the value 50 in its Protocol (IPv4) or Next Header (IPv6, Extension) field
 * [STD-2].
 * <h3>Header Format</h3> <blockquote>
 * 
 * <pre>
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ ----
 *   |               Security Parameters Index (SPI)                 | ^Auth.
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |Cov-
 *   |                      Sequence Number                          | |erage
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ | ----
 *   |                    Payload Data* (variable)                   | |   ^
 *   ~                                                               ~ |   |
 *   |                                                               | |Conf.
 *   +               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |Cov-
 *   |               |     Padding (0-255 bytes)                     | |erage*
 *   +-+-+-+-+-+-+-+-+               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |   |
 *   |                               |  Pad Length   | Next Header   | v   v
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ ------
 *   |                 Authentication Data (variable)                |
 *   ~                                                               ~
 *   |                                                               |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <blockquote> *If included in the Payload field, cryptographic synchronization data,
 * e.g., an Initialization Vector (IV, see Section 2.3), usually is not encrypted per se,
 * although it often is referred to as being part of the ciphertext. </blockquote>
 * <p>
 * In the following field descriptions "Optional" means that the field is omitted if the
 * option is not selected, i.e., it is present in neither the packet as transmitted nor as
 * formatted for computation of an Integrity Check Value (ICV, see Section 2.7). Whether
 * or not an option is selected is defined as part of Security Association (SA)
 * establishment. Thus the format of ESP packets for a given SA is fixed, for the duration
 * of the SA. In contrast, "mandatory" fields are always present in the ESP packet format,
 * for all SAs.
 * <dl>
 * <dt><u>Security Parameters Index</u></dt>
 * <p>
 * <dd>The SPI is an arbitrary 32-bit value that, in combination with the destination IP
 * address and security protocol (ESP), uniquely identifies the Security Association for
 * this datagram. The set of SPI values in the range 1 through 255 are reserved by the
 * Internet Assigned Numbers Authority (IANA) for future use; a reserved SPI value will
 * not normally be assigned by IANA unless the use of the assigned SPI value is specified
 * in an RFC. It is ordinarily selected by the destination system upon establishment of an
 * SA (see the Security Architecture document for more details). The SPI field is
 * mandatory.
 * <p>
 * The SPI value of zero (0) is reserved for local, implementation- specific use and MUST
 * NOT be sent on the wire. For example, a key management implementation MAY use the zero
 * SPI value to mean "No Security Association Exists" during the period when the IPsec
 * implementation has requested that its key management entity establish a new SA, but the
 * SA has not yet been established.</dd>
 * <p>
 * <dt><u>Sequence Number</u></dt>
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
 * see Section 3.3.3 for more details on how the Sequence Number is generated.) If
 * anti-replay is enabled (the default), the transmitted Sequence Number must never be
 * allowed to cycle. Thus, the sender's counter and the receiver's counter MUST be reset
 * (by establishing a new SA and thus a new key) prior to the transmission of the 2^32nd
 * packet on an SA.</dd>
 * <p>
 * <dt><u>Payload Data</u></dt>
 * <p>
 * <dd>Payload Data is a variable-length field containing data described by the Next
 * Header field. The Payload Data field is mandatory and is an integral number of bytes in
 * length. If the algorithm used to encrypt the payload requires cryptographic
 * synchronization data, e.g., an Initialization Vector (IV), then this data MAY be
 * carried explicitly in the Payload field. Any encryption algorithm that requires such
 * explicit, per-packet synchronization data MUST indicate the length, any structure for
 * such data, and the location of this data as part of an RFC specifying how the algorithm
 * is used with ESP. If such synchronization data is implicit, the algorithm for deriving
 * the data MUST be part of the RFC.
 * <p>
 * Note that with regard to ensuring the alignment of the (real) ciphertext in the
 * presence of an IV:
 * <p>
 * <ul>
 * <li>For some IV-based modes of operation, the receiver treats the IV as the start of
 * the ciphertext, feeding it into the algorithm directly. In these modes, alignment of
 * the start of the (real) ciphertext is not an issue at the receiver.
 * <li>In some cases, the receiver reads the IV in separately from the ciphertext. In
 * these cases, the algorithm specification MUST address how alignment of the (real)
 * ciphertext is to be achieved.
 * </ul>
 * </dd>
 * <dt><u>Padding (for Encryption)</u></dt>
 * <p>
 * <dd>Several factors require or motivate use of the Padding field.
 * <p>
 * <ul>
 * <li>If an encryption algorithm is employed that requires the plaintext to be a multiple
 * of some number of bytes, e.g., the block size of a block cipher, the Padding field is
 * used to fill the plaintext (consisting of the Payload Data, Pad Length and Next Header
 * fields, as well as the Padding) to the size required by the algorithm.
 * <li>Padding also may be required, irrespective of encryption algorithm requirements, to
 * ensure that the resulting ciphertext terminates on a 4-byte boundary. Specifically, the
 * Pad Length and Next Header fields must be right aligned within a 4-byte word, as
 * illustrated in the ESP packet format figure above, to ensure that the Authentication
 * Data field (if present) is aligned on a 4-byte boundary.
 * <li>Padding beyond that required for the algorithm or alignment reasons cited above,
 * may be used to conceal the actual length of the payload, in support of (partial)
 * traffic flow confidentiality. However, inclusion of such additional padding has adverse
 * bandwidth implications and thus its use should be undertaken with care.
 * </ul>
 * The sender MAY add 0-255 bytes of padding. Inclusion of the Padding field in an ESP
 * packet is optional, but all implementations MUST support generation and consumption of
 * padding.
 * <p>
 * <ul>
 * <li>For the purpose of ensuring that the bits to be encrypted are a multiple of the
 * algorithm's blocksize (first bullet above), the padding computation applies to the
 * Payload Data exclusive of the IV, the Pad Length, and Next Header fields.
 * <li>For the purposes of ensuring that the Authentication Data is aligned on a 4-byte
 * boundary (second bullet above), the padding computation applies to the Payload Data
 * inclusive of the IV, the Pad Length, and Next Header fields.
 * </ul>
 * If Padding bytes are needed but the encryption algorithm does not specify the padding
 * contents, then the following default processing MUST be used. The Padding bytes are
 * initialized with a series of (unsigned, 1-byte) integer values. The first padding byte
 * appended to the plaintext is numbered 1, with subsequent padding bytes making up a
 * monotonically increasing sequence: 1, 2, 3, ... When this padding scheme is employed,
 * the receiver SHOULD inspect the Padding field. (This scheme was selected because of its
 * relative simplicity, ease of implementation in hardware, and because it offers limited
 * protection against certain forms of "cut and paste" attacks in the absence of other
 * integrity measures, if the receiver checks the padding values upon decryption.)
 * <p>
 * Any encryption algorithm that requires Padding other than the default described above,
 * MUST define the Padding contents (e.g., zeros or random data) and any required receiver
 * processing of these Padding bytes in an RFC specifying how the algorithm is used with
 * ESP. In such circumstances, the content of the Padding field will be determined by the
 * encryption algorithm and mode selected and defined in the corresponding algorithm RFC.
 * The relevant algorithm RFC MAY specify that a receiver MUST inspect the Padding field
 * or that a receiver MUST inform senders of how the receiver will handle the Padding
 * field.</dd>
 * <p>
 * <dt><u>Pad Length</u></dt>
 * <p>
 * <dd>The Pad Length field indicates the number of pad bytes immediately preceding it.
 * The range of valid values is 0-255, where a value of zero indicates that no Padding
 * bytes are present. The Pad Length field is mandatory.</dd>
 * <p>
 * <dt><u>Next Header</u></dt>
 * <p>
 * <dd>The Next Header is an 8-bit field that identifies the type of data contained in the
 * Payload Data field, e.g., an extension header in IPv6 or an upper layer protocol
 * identifier. The value of this field is chosen from the set of IP Protocol Numbers
 * defined in the most recent "Assigned Numbers" [STD-2] RFC from the Internet Assigned
 * Numbers Authority (IANA). The Next Header field is mandatory.</dd>
 * <p>
 * <dt><u>Authentication Data</u></dt>
 * <p>
 * <dd>The Authentication Data is a variable-length field containing an Integrity Check
 * Value (ICV) computed over the ESP packet minus the Authentication Data. The length of
 * the field is specified by the authentication function selected. The Authentication Data
 * field is optional, and is included only if the authentication service has been selected
 * for the SA in question. The authentication algorithm specification MUST specify the
 * length of the ICV and the comparison rules and processing steps for validation.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPEncapsulatingSecurityPayload
                extends BufferBackedObject
                implements IPMessage {

    /*-- Inner Classes ---------------------------------------------------*/

    public static class Parser
                    implements IPMessage.ParserType {

        @Override
        public IPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IPEncapsulatingSecurityPayload(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer, final byte[] sourceAddress, final byte[] destinationAddress)
                                                                                                                           throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(IPEncapsulatingSecurityPayload.class.getName());

    public static final byte IP_PROTOCOL_NUMBER = 50;

    protected static final int BASE_HEADER_LENGTH = 8;

    /*-- Member Variables ---------------------------------------------------*/

    private ByteBuffer payload;

    /*-- Member Variables ---------------------------------------------------*/

    /**
     * @param buffer
     */
    public IPEncapsulatingSecurityPayload(final ByteBuffer buffer) {
        super(consume(buffer, BASE_HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPAuthenticationHeader.IPAuthenticationHeader", buffer));
        }

        this.payload = consume(buffer, buffer.remaining());

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger,level);
        logState(logger,level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level, this.log.msg(": payload array=" + this.payload.array() +
                                       " offset=" + this.payload.arrayOffset() +
                                       " limit=" + this.payload.limit()));
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPEncapsulatingSecurityPayload.writeTo", buffer));
        }

        super.writeTo(buffer);
        this.payload.rewind();
        buffer.put(this.payload);
        this.payload.rewind();
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {
        // NO-OP
    }

    public ByteBuffer getPayload() {
        return this.payload.slice();
    }

    @Override
    public int getHeaderLength() {
        // TODO: This only works with original unparsed content
        return BASE_HEADER_LENGTH + this.payload.limit();
    }

    @Override
    public int getTotalLength() {
        return getHeaderLength();
    }

    @Override
    public byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public void setProtocolNumber(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPEncapsulatingSecurityPayload.setProtocolNumber", protocolNumber));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public byte getNextProtocolNumber() {
        // TODO - could read this from decrypted packet?
        return IPMessage.NO_NEXT_HEADER;
    }

    @Override
    public IPMessage getNextMessage() {
        return null;
    }

    @Override
    public void setNextMessage(final IPMessage message) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPEncapsulatingSecurityPayload.setNextMessage", message));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNextMessage() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPEncapsulatingSecurityPayload.removeNextMessage"));
        }

        throw new UnsupportedOperationException();
    }

}
