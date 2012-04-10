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

package com.larkwoodlabs.net.ip.igmp;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for IGMP Messages that identify a group.
 * Handles interpretation of second word in some IGMP messages.
 * [See <a href="http://www.ietf.org/rfc/rfc2236.txt">RFC-2236</a> and <a
 * href="http://www.ietf.org/rfc/rfc3376.txt">RFC-3376</a>].
 * 
 * <pre>
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |   Type  | Max Resp Time |             Checksum                |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         Group Address                         |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * Type
 * 
 *    0x11 V2/V3 Membership Query       [RFC-2236 and RFC-3376]
 *    0x12 Version 1 Membership Report  [RFC-1112]
 *    0x16 Version 2 Membership Report  [RFC-2236]
 *    0x17 Version 2 Leave Group        [RFC-2236]
 *    0x22 Version 3 Membership Report  [RFC-3376]
 * 
 * Max Response Time (or Reserved)
 * 
 *    The Max Resp Time field is meaningful only in Membership Query messages.
 * 
 * Checksum
 * 
 *    The checksum is the 16-bit one's complement of the one's complement
 *    sum of the whole IGMP message (the entire IP payload).  For computing
 *    the checksum, the checksum field is set to zero.  When transmitting
 *    packets, the checksum MUST be computed and inserted into this field.
 *    When receiving packets, the checksum MUST be verified before
 *    processing a packet.
 * 
 * Group Address
 * 
 *    The IP multicast group address.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
public abstract class IGMPGroupMessage extends IGMPMessage {


    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final ByteArrayField GroupAddress = new ByteArrayField(4,4);


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
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
        //Precondition.checkIPv4MulticastAddress(groupAddress);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPGroupMessage.IGMPGroupMessage", size, type, maximumResponseTime, Logging.address(groupAddress)));
        }
        setGroupAddress(groupAddress);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    protected IGMPGroupMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPGroupMessage.IGMPGroupMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(final Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs private state.
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " : group-address="+Logging.address(getGroupAddress()));
    }

    /**
     * A field whose interpretation depends on message type.
     * Typically this field identifies an IPv4 multicast group address.
     * 
     * The Group Address field is set to zero when sending a General Query,
     * and set to the IP multicast address being queried when sending a
     * Group-Specific Query or Group-and-Source-Specific Query.
     * @return
     */
    public final byte[] getGroupAddress() {
        return GroupAddress.get(getBufferInternal());
    }

    /**
     * 
     * @param groupAddress
     */
    public final void setGroupAddress(final InetAddress groupAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPGroupMessage.setGroupAddress", Logging.address(groupAddress)));
        }
        
        setGroupAddress(groupAddress == null ? null : groupAddress.getAddress());
    }

    /**
     * 
     * @param groupAddress
     */
    public final void setGroupAddress(final byte[] groupAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPGroupMessage.setGroupAddress", Logging.address(groupAddress)));
        }
        
        //Precondition.checkIPv6MulticastAddress(groupAddress);
        GroupAddress.set(getBufferInternal(),groupAddress);
    }

}
