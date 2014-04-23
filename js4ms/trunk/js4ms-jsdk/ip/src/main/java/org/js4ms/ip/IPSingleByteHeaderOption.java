package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPSingleByteHeaderOption.java [org.js4ms.jsdk:ip]
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


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;

/**
 * Represents a simple IP Header Option that consists of a single byte.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPSingleByteHeaderOption
                extends IPHeaderOption {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final byte OPTION_LENGTH = 1;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param option
     */
    public IPSingleByteHeaderOption(byte option) {
        super(OPTION_LENGTH, option);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPSingleByteHeaderOption.IPSingleByteHeaderOption", option));
        }

    }

    /**
     * @param copyFlag
     * @param optionClass
     * @param optionNumber
     */
    public IPSingleByteHeaderOption(final boolean copyFlag, final int optionClass, final int optionNumber) {
        super(OPTION_LENGTH, copyFlag, optionClass, optionNumber);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPSingleByteHeaderOption.IPSingleByteHeaderOption", copyFlag, optionClass,
                                        optionNumber));
        }
    }

    /**
     * @param buffer
     */
    public IPSingleByteHeaderOption(final ByteBuffer buffer) {
        super(consume(buffer, OPTION_LENGTH));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPSingleByteHeaderOption.IPSingleByteHeaderOption", buffer));
        }
    }

    /**
     * @param buffer
     * @throws IOException
     */
    public IPSingleByteHeaderOption(final InputStream is) throws IOException {
        super(consume(is, OPTION_LENGTH));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPSingleByteHeaderOption.IPSingleByteHeaderOption", is));
        }
    }

    @Override
    public final int getOptionLength() {
        return OPTION_LENGTH;
    }

}
