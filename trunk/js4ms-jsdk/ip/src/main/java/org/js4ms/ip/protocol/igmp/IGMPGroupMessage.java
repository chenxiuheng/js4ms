package org.js4ms.ip.protocol.igmp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IGMPGroupMessage.java [org.js4ms.jsdk:ip]
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


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.logging.Logging;




/**
 * Base class for IGMP Messages that identify a group.
 * Handles interpretation of second word in some IGMP messages.
 * See [<a href="http://www.ietf.org/rfc/rfc2236.txt">RFC-2236</a>] and [<a
 * href="http://www.ietf.org/rfc/rfc3376.txt">RFC-3376</a>].
 * <p>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |   Type  | Max Resp Time |             Checksum                |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         Group Address                         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>A code that identifies the message type:
 * 
 * <pre>
 *    0x11 V2/V3 Membership Query       [RFC-2236 and RFC-3376]
 *    0x12 Version 1 Membership Report  [RFC-1112]
 *    0x16 Version 2 Membership Report  [RFC-2236]
 *    0x17 Version 2 Leave Group        [RFC-2236]
 *    0x22 Version 3 Membership Report  [RFC-3376]
 * </pre>
 * 
 * </dd>
 * <p>
 * <dt><u>Max Response Time (or Reserved)</u></dt>
 * <p>
 * <dd>The Max Resp Time field is meaningful only in Membership Query messages.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>The checksum is the 16-bit one's complement of the one's complement sum of the
 * whole IGMP message (the entire IP payload). For computing the checksum, the checksum
 * field is set to zero. When transmitting packets, the checksum MUST be computed and
 * inserted into this field. When receiving packets, the checksum MUST be verified before
 * processing a packet.</dd>
 * <p>
 * <dt><u>Group Address</u></dt>
 * <p>
 * <dd>The IP multicast group address.
 * <p>
 * See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])}.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public abstract class IGMPGroupMessage
                extends IGMPMessage {

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final ByteArrayField GroupAddress = new ByteArrayField(4, 4);

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param size
     * @param type
     * @param maximumResponseTime
     * @param groupAddress
     */
    protected IGMPGroupMessage(final int size,
                               final byte type,
                               final short maximumResponseTime,
                               final byte[] groupAddress) {
        super(size, type, maximumResponseTime);
        // Precondition.checkIPv4MulticastAddress(groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPGroupMessage.IGMPGroupMessage", size, type, maximumResponseTime,
                                        Logging.address(groupAddress)));
        }
        setGroupAddress(groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    protected IGMPGroupMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPGroupMessage.IGMPGroupMessage", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs private state.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": group-address=" + Logging.address(getGroupAddress())));
    }

    /**
     * A field whose interpretation depends on message type.
     * Typically this field identifies an IPv4 multicast group address.
     * The Group Address field is set to zero when sending a General Query,
     * and set to the IP multicast address being queried when sending a
     * Group-Specific Query or Group-and-Source-Specific Query.
     * 
     * @return
     */
    public final byte[] getGroupAddress() {
        return GroupAddress.get(getBufferInternal());
    }

    /**
     * @param groupAddress
     */
    public final void setGroupAddress(final InetAddress groupAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPGroupMessage.setGroupAddress", Logging.address(groupAddress)));
        }

        setGroupAddress(groupAddress == null ? null : groupAddress.getAddress());
    }

    /**
     * @param groupAddress
     */
    public final void setGroupAddress(final byte[] groupAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPGroupMessage.setGroupAddress", Logging.address(groupAddress)));
        }

        // Precondition.checkIPv6MulticastAddress(groupAddress);
        GroupAddress.set(getBufferInternal(), groupAddress);
    }

}
