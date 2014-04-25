package org.js4ms.ip.protocol.mld;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MLDMessage.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv6.IPv6HopByHopOptionsHeader;
import org.js4ms.ip.ipv6.IPv6Packet;
import org.js4ms.ip.ipv6.IPv6RouterAlertOption;
import org.js4ms.ip.protocol.icmp.ICMPv6Message;




/**
 * Represents a Multicast Listener Discovery (MLD) message as described in
 * [<a href="http://tools.ietf.org/html/rfc2710">RFC-2710</a>] and [<a
 * href="http://tools.ietf.org/html/rfc3810">RFC-3810</a>].
 * <p>
 * MLD is a sub-protocol of ICMPv6, that is, MLD message types are a subset of the set of
 * ICMPv6 messages, and MLD messages are identified in IPv6 packets by a preceding Next
 * Header value of 58. All MLD messages described in this document are sent with a
 * link-local IPv6 Source Address, an IPv6 Hop Limit of 1, and an IPv6 Router Alert option
 * [RTR-ALERT] in a Hop-by-Hop Options header. (The Router Alert option is necessary to
 * cause routers to examine MLD messages sent to multicast addresses in which the routers
 * themselves have no interest.)
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |     Type      |     Code      |          Checksum             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |     Maximum Response Delay    |          Reserved             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                       Multicast Address                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>There are four types of MLD messages:
 * <ul>
 * <li>Multicast Listener Query (Type = decimal 130)
 * <li>Multicast Listener Report (Type = decimal 131)
 * <li>Multicast Listener Done (Type = decimal 132)
 * <li>Multicast V2 Listener Report (Type = decimal 143)
 * </ul>
 * These two subtypes of listener queries. They are differentiated by the contents of the
 * Multicast Address field:
 * <ul>
 * <li>A General Query, used to learn which multicast addresses have listeners on an
 * attached link.
 * <li>A Multicast-Address-Specific Query, used to learn if a particular multicast address
 * has any listeners on an attached link.
 * </ul>
 * In the rest of this document, the above messages types are referred to simply as
 * &quot;Query&quot;, &quot;Report&quot;, and &quot;Done&quot;.
 * <p>
 * See {@link #getType()}.</dd>
 * <p>
 * <dt><u>Code</u></dt>
 * <p>
 * <dd>Initialized to zero by the sender; ignored by receivers.
 * <p>
 * See {@link #getCode()}, {@link #setCode(byte)}.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>The standard ICMPv6 checksum, covering the entire MLD message plus a
 * &quot;pseudo-header&quot; of IPv6 header fields [ICMPv6,IPv6].
 * <p>
 * See {@link #getChecksum()}, {@link #setChecksum(short)},
 * {@link #calculateChecksum(ByteBuffer, int, byte[], byte[])} and
 * {@link #verifyChecksum(byte[], byte[], int)}.</dd>
 * <p>
 * <dt><u>Maximum Response Delay</u></dt>
 * <p>
 * <dd>The Maximum Response Delay field is meaningful only in Query messages, and
 * specifies the maximum allowed delay before sending a responding Report, in units of
 * milliseconds. In all other messages, it is set to zero by the sender and ignored by
 * receivers.
 * <p>
 * Varying this value allows the routers to tune the &quot;leave latency&quot; (the time
 * between the moment the last node on a link ceases listening to a particular multicast
 * address and moment the routing protocol is notified that there are no longer any
 * listeners for that address), as discussed in section 7.8. It also allows tuning of the
 * burstiness of MLD traffic on a link, as discussed in section 7.3.
 * <p>
 * See {@link #getMaximumResponseDelay()} and {@link #setMaximumResponseDelay(short)}.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Initialized to zero by the sender; ignored by receivers.</dd>
 * <p>
 * <dt><u>Multicast Address</u></dt>
 * <p>
 * <dd>In a Query message, the Multicast Address field is set to zero when sending a
 * General Query, and set to a specific IPv6 multicast address when sending a
 * Multicast-Address-Specific Query.
 * <p>
 * In a Report or Done message, the Multicast Address field holds a specific IPv6
 * multicast address to which the message sender is listening or is ceasing to listen,
 * respectively.</dd>
 * <p>
 * <dt><u>Other fields</u></dt>
 * <p>
 * <dd>The length of a received MLD message is computed by taking the IPv6 Payload Length
 * value and subtracting the length of any IPv6 extension headers present between the IPv6
 * header and the MLD message. If that length is greater than 24 octets, that indicates
 * that there are other fields present beyond the fields described above, perhaps
 * belonging to a future backwards-compatible version of MLD. An implementation of the
 * version of MLD specified in this document MUST NOT send an MLD message longer than 24
 * octets and MUST ignore anything past the first 24 octets of a received MLD message. In
 * all cases, the MLD checksum MUST be computed over the entire MLD message, not just the
 * first 24 octets.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public abstract class MLDMessage
                extends ICMPv6Message {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static interface ParserType
                    extends ICMPv6Message.ParserType {

    }

    /**
     * 
     */
    public static final class Parser
                    extends ICMPv6Message.Parser {

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate MLDMessage log entries. */
    public static final Logger logger = Logger.getLogger(MLDMessage.class.getName());

    /** */
    public static final short MLD_ROUTER_ALERT_VALUE = 0;

    /** */
    public static final ShortField MaximumResponseDelay = new ShortField(4);

    /** */
    public static final ShortField Reserved = new ShortField(6);

    /**
     * General-Query multicast address.
     * 
     * <pre>
     * 0:0:0:0:0:0:0:0
     * </pre>
     */
    public static final byte[] IPv6GeneralQueryGroupAddress = {
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    /**
     * All-Nodes multicast address.
     * 
     * <pre>
     * FF02:0:0:0:0:0:0:1
     * </pre>
     */
    public static final byte[] IPv6QueryDestinationAddress = {
                    (byte) 0xFF, (byte) 0x02, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };

    /**
     * All-MLDv2-Routers multicast address.
     * 
     * <pre>
     * FF02:0:0:0:0:0:0:16
     * </pre>
     */
    public static final byte[] IPv6ReportDestinationAddress = {
                    (byte) 0xFF, (byte) 0x02, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x16
    };

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static MLDMessage.Parser getMLDMessageParser() {
        MLDMessage.Parser parser = new MLDMessage.Parser();
        parser.add(new MLDv1QueryMessage.Parser());
        parser.add(new MLDv1ReportMessage.Parser());
        parser.add(new MLDv2ReportMessage.Parser());
        parser.add(new MLDv1DoneMessage.Parser());
        return parser;
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(getMLDMessageParser());
        return parser;
    }

    /**
     * @return
     */
    public static IPv6Packet.Parser getIPv6PacketParser() {
        IPv6Packet.Parser parser = new IPv6Packet.Parser();
        parser.setProtocolParser(getIPMessageParser());
        return parser;
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        IPPacket.BufferParser parser = new IPPacket.BufferParser();
        parser.add(getIPv6PacketParser());
        return parser;
    }

    /**
     * @param messageParser
     * @return
     */
    public static MLDMessage.Parser getMLDMessageParser(final MLDMessage.ParserType messageParser) {
        MLDMessage.Parser parser = new MLDMessage.Parser();
        parser.add(messageParser);
        return parser;
    }

    /**
     * @param messageParser
     * @return
     */
    public static IPMessage.Parser getIPMessageParser(final MLDMessage.ParserType messageParser) {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(getMLDMessageParser(messageParser));
        return parser;
    }

    /**
     * @param messageParser
     * @return
     */
    public static IPv6Packet.Parser getIPv6PacketParser(final MLDMessage.ParserType messageParser) {
        IPv6Packet.Parser parser = new IPv6Packet.Parser();
        parser.setProtocolParser(getIPMessageParser(messageParser));
        return parser;
    }

    /**
     * @param messageParser
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser(final MLDMessage.ParserType messageParser) {
        IPPacket.BufferParser parser = new IPPacket.BufferParser();
        parser.add(getIPv6PacketParser(messageParser));
        return parser;
    }

    /**
     * Calculates the MLD message checksum for an MLD packet contained in a buffer.
     * 
     * @param buffer
     *            - the buffer containing the MLD message.
     * @param messageLength
     *            - the length of the IGMP message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     * @return
     */
    public static short calculateChecksum(final ByteBuffer buffer,
                                          final int messageLength,
                                          final byte[] sourceAddress,
                                          final byte[] destinationAddress) {
        return IPPacket.calculateChecksum(buffer, Checksum, sourceAddress, destinationAddress, IP_PROTOCOL_NUMBER, messageLength);
    }

    /**
     * @param sourceAddress
     * @param destinationAddress
     * @param message
     * @return
     */
    public static IPv6Packet constructIPv6Packet(final byte[] sourceAddress,
                                                 final byte[] destinationAddress,
                                                 final MLDMessage message) {
        IPv6HopByHopOptionsHeader optionsHeader = new IPv6HopByHopOptionsHeader();
        optionsHeader.addOption(new IPv6RouterAlertOption(MLD_ROUTER_ALERT_VALUE));
        IPv6Packet header = new IPv6Packet((byte) 0, // Priority
                                           0, // Flow Label (no flow)
                                           (byte) 1, // Hop Limit (stops at router)
                                           sourceAddress, // Host address
                                           destinationAddress, // Query or Report Address
                                           optionsHeader); // First IP Protocol header -
                                                           // MLD message added later
        header.addProtocolMessage(message);
        return header;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param size
     * @param type
     */
    protected MLDMessage(final int size, final byte type) {
        super(size);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDMessage.MLDMessage", size, type));
        }

        setType(type);
        setCode((byte) 0);
        setChecksum((short) 0);
        MLDMessage.Reserved.set(getBufferInternal(), (short) 0);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    protected MLDMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDMessage.MLDMessage", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": message-length=" + getTotalLength()));
    }

    /**
     * @return
     */
    public short getMaximumResponseDelay() {
        return MLDMessage.MaximumResponseDelay.get(getBufferInternal());
    }

    /**
     * @param milliseconds
     */
    public void setMaximumResponseDelay(final short milliseconds) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDMessage.setMaximumResponseDelay", milliseconds));
        }

        MLDMessage.MaximumResponseDelay.set(getBufferInternal(), milliseconds);
    }
}
