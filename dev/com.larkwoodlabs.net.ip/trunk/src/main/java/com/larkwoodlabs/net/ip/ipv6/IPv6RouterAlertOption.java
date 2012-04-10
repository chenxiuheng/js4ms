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

package com.larkwoodlabs.net.ip.ipv6;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPHeaderOption;
import com.larkwoodlabs.net.ip.IPMultiByteHeaderOption;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.logging.Logging;


/**
 * IPv6 Router Alert Option (Option 20) The router alert option is 4 bytes
 * comprised of an option type, length, and alert value.
 * 
 * <pre>
 *  00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |          Type         |         Length        |              Router Alert Value               |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * </pre>
 * 
 * The Type field has the following fixed value: Copy=1, Class=0, Option=20
 * 
 * <pre>
 * +-+-+-+-+-+-+-+-+
 * |0|0 0|0 0 1 0 1| 0x05
 * +-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * Length field is 4. Router Alert Value describes action router should take.
 * Router shall examine packet. 1-65535 Reserved.
 * MLD uses a value of zero.
 * 
 * @author Gregory Bumgardner
 */
public final class IPv6RouterAlertOption extends IPMultiByteHeaderOption {

    /*-- Inner Classes ---------------------------------------------------*/

    /**
     * 
     * 
     *
     * @author gbumgard
     */
    public static class Parser implements IPHeaderOption.ParserType {

        @Override
        public IPHeaderOption parse(final ByteBuffer buffer) throws ParseException {
            return new IPv6RouterAlertOption(buffer);
        }

        @Override
        public Object getKey() {
            return IPv6RouterAlertOption.OPTION_CODE;
        }
    }


    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final ShortField RouterAlertValue = new ShortField(2); 

    /**
     * The router alert option field has the following fixed value: Copy=0, Class=0, Option=5
     * 
     * <pre>
     * +-+-+-+-+-+-+-+-+
     * |0|0 0|0 0 1 0 1| 0x05
     * +-+-+-+-+-+-+-+-+
     * </pre>
     */
    public static final byte OPTION_VALUE = (byte)0x05;
    /** */
    public static final byte OPTION_CODE = (byte)0x05;
    /** */
    public static final byte OPTION_LENGTH = 4;


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public IPv6RouterAlertOption() {
        this((short)0);
        if (logger.isLoggable(Level.FINER)) logger.finer(Logging.entering(ObjectId, "IPv6RouterAlertOption.IPv6RouterAlertOption"));
    }

    /**
     * 
     * @param routerAlertValue
     */
    public IPv6RouterAlertOption(final short routerAlertValue) {
        super(OPTION_VALUE,OPTION_LENGTH);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RouterAlertOption.IPv6RouterAlertOption", routerAlertValue));
        }
        
        setRouterAlertValue(routerAlertValue);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     */
    public IPv6RouterAlertOption(final ByteBuffer buffer) {
        super(buffer);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this, "IPv6RouterAlertOption.IPv6RouterAlertOption", buffer));
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
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId+" : router-alert-value="+getRouterAlertValue());
    }

    /**
     * 
     * @return
     */
    public short getRouterAlertValue() {
        return RouterAlertValue.get(getBufferInternal());
    }

    /**
     * 
     * @param routerAlertValue
     */
    public void setRouterAlertValue(final short routerAlertValue) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this, "IPv6RouterAlertOption.setRouterAlertValue", routerAlertValue));
        }
        
        RouterAlertValue.set(getBufferInternal(), routerAlertValue);
    }
}
