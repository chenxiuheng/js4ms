package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPEndOfListOption.java [org.js4ms.jsdk:ip]
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

import org.js4ms.common.exception.ParseException;



/**
 * Represents an IP End of Options list Option (Option 0).
 * Used to indicate that no more options follow.
 * The Type field has the following value:
 * Copy=0, Class=0, Option=0
 * 
 * <pre>
 * +-+-+-+-+-+-+-+-+
 * |0|0 0|0 0 0 0 0|
 * +-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPEndOfListOption
                extends IPSingleByteHeaderOption {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser
                    implements IPHeaderOption.ParserType {

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
            logger.finer(this.log.entry("IPEndOfListOption.IPEndOfListOption"));
        }
    }

    /**
     * @param segment
     */
    public IPEndOfListOption(final ByteBuffer segment) {
        super(segment);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPEndOfListOption.IPEndOfListOption", segment));
        }
    }

}
