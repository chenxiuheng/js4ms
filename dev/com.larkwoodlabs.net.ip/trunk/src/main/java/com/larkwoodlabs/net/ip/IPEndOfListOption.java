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

package com.larkwoodlabs.net.ip;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * IP End of Options list Option (Option 0).
 * Used to indicate that no more options follow.
 * 
 * The Type field has the following value:
 * Copy=0, Class=0, Option=0
 * 
 * <pre>
 * +-+-+-+-+-+-+-+-+
 * |0|0 0|0 0 0 0 0|
 * +-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author Gregory Bumgardner
 */
public class IPEndOfListOption extends IPSingleByteHeaderOption {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements IPHeaderOption.ParserType {

        @Override
        public IPHeaderOption parse(final ByteBuffer buffer) throws ParseException {
            return new IPEndOfListOption(buffer);
        }

        @Override
        public Object getKey() {
            return IPEndOfListOption.OPTION_CODE;
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final int OPTION_CLASS = 0;
    public static final int OPTION_NUMBER = 0;
    public static final byte OPTION_CODE = OPTION_CLASS | OPTION_NUMBER;


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public IPEndOfListOption() {
        super(OPTION_CODE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPEndOfListOption.IPEndOfListOption"));
        }
    }

    /**
     * 
     * @param segment
     */
    public IPEndOfListOption(final ByteBuffer segment) {
        super(segment);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPEndOfListOption.IPEndOfListOption", segment));
        }
    }

}
