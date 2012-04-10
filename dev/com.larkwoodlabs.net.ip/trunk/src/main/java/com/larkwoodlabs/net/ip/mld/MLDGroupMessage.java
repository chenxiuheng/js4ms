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

package com.larkwoodlabs.net.ip.mld;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for Mulicast Listener Discovery Message classes
 * that use the multicast group address field.
 *
 * @author Gregory Bumgardner
 */
public abstract class MLDGroupMessage extends MLDMessage {

    /*-- Static Variables ---------------------------------------------------*/
    
    /**
     * 
     */
    public static final ByteArrayField GroupAddress = new ByteArrayField(4,16);


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param size
     * @param type
     * @param groupAddress
     */
    protected MLDGroupMessage(final int size, final byte type, final byte[] groupAddress) {
        super(size, type);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDGroupMessage.MLDGroupMessage", size, type, Logging.address(groupAddress)));
        }
        
        setGroupAddress(groupAddress);

        if (logger.isLoggable(Level.FINER)) {
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    protected MLDGroupMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDGroupMessage.MLDGroupMessage", buffer));
        }
    }

    /**
     * A field whose interpretation depends on message type.
     * Typically this field identifies an IPv6 multicast group address.
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
            logger.finer(Logging.entering(ObjectId, "MLDGroupMessage.setGroupAddress", Logging.address(groupAddress)));
        }
        
        setGroupAddress(groupAddress.getAddress());
    }

    /**
     * 
     * @param groupAddress
     */
    public final void setGroupAddress(final byte[] groupAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDGroupMessage.setGroupAddress", Logging.address(groupAddress)));
        }
        
        if (groupAddress.length != 16) {
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " invalid group address - MLD messages only allow use of IPv6 addresses");
            }
            
            throw new IllegalArgumentException("invalid group address - MLD messages only allow use of IPv6 addresses");
        }
        GroupAddress.set(getBufferInternal(),groupAddress);
    }
}
