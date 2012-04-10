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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.util.logging.Logging;


/**
 * A simple IP Header Option that consists of a single byte.
 * @author Gregory Bumgardner
 */
public class IPSingleByteHeaderOption extends IPHeaderOption {

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte OPTION_LENGTH = 1;


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param option
     */
    public IPSingleByteHeaderOption(byte option) {
        super(OPTION_LENGTH, option);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPSingleByteHeaderOption.IPSingleByteHeaderOption", option));
        }
        
    }

    /**
     * 
     * @param copyFlag
     * @param optionClass
     * @param optionNumber
     */
    public IPSingleByteHeaderOption(final boolean copyFlag, final int optionClass, final int optionNumber) {
        super(OPTION_LENGTH, copyFlag, optionClass, optionNumber);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPSingleByteHeaderOption.IPSingleByteHeaderOption", copyFlag, optionClass, optionNumber));
        }
    }

    /**
     * 
     * @param buffer
     */
    public IPSingleByteHeaderOption(final ByteBuffer buffer) {
        super(consume(buffer, OPTION_LENGTH));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPSingleByteHeaderOption.IPSingleByteHeaderOption", buffer));
        }
    }

    /**
     * 
     * @param buffer
     * @throws IOException 
     */
    public IPSingleByteHeaderOption(final InputStream is) throws IOException {
        super(consume(is, OPTION_LENGTH));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPSingleByteHeaderOption.IPSingleByteHeaderOption", is));
        }
    }


    @Override
    public final int getOptionLength() {
        return OPTION_LENGTH;
    }

}
