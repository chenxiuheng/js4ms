/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

package com.larkwoodlabs.net.ip;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.logging.Logging;


/**
 * A multibyte IP Header Option.
 * The option is comprised of an option type, length, and data bytes.
 * 
 * <pre>
 *  00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+~
 * |          Type         |         Length        |         Data ...      |~
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+~
 * </pre>
 *
 * @author Gregory Bumgardner
 */
public class IPMultiByteHeaderOption extends IPHeaderOption {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements IPHeaderOption.ParserType {

        @Override
        public IPHeaderOption parse(ByteBuffer buffer) throws ParseException {
            return new IPMultiByteHeaderOption(buffer);
        }

        @Override
        public Object getKey() {
            return null; // Any option
        }
    }


    /*-- Static Variables ---------------------------------------------------*/

    public static final ByteField OptionLength = new ByteField(1); 


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param option
     * @param optionLength
     */
    protected IPMultiByteHeaderOption(byte option, int optionLength) {
        super(optionLength,option);
        setOptionLength(optionLength);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPMultiByteHeaderOption.IPMultiByteHeaderOption", option, optionLength));
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     */
    public IPMultiByteHeaderOption(ByteBuffer buffer) {
        super(consume(buffer,OptionLength.get(buffer)));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,"IPMultiByteHeaderOption.IPMultiByteHeaderOption", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : length="+getOptionLength());
    }

    /**
     * 
     */
    public final int getOptionLength() {
        return OptionLength.get(getBufferInternal());
    }

    /**
     * 
     * @param length
     */
    public final void setOptionLength(int length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPMultiByteHeaderOption.setOptionLength", length));
        }
        
        OptionLength.set(getBufferInternal(), (byte)length);
    }

}
