package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPNoOperationOption.java [org.js4ms.jsdk:ip]
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
 * Represents an IP No-Operation Option (Option 1).
 * Typically used to adjust alignment of subsequent options.
 * The Type field has the following value:
 * Copy=1|0, Class=0, Option=1
 * 
 * <pre>
 * +-+-+-+-+-+-+-+-+
 * |?|0 0|0 0 0 0 1|
 * +-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IPNoOperationOption
                extends IPSingleByteHeaderOption {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPHeaderOption.ParserType {

        @Override
        public IPHeaderOption parse(final ByteBuffer buffer) throws ParseException {
            return new IPNoOperationOption(buffer);
        }

        @Override
        public Object getKey() {
            return IPNoOperationOption.OPTION_CODE;
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final int OPTION_CLASS = 0;

    /** */
    public static final int OPTION_NUMBER = 1;

    /** */
    public static final byte OPTION_CODE = OPTION_CLASS | OPTION_NUMBER;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public IPNoOperationOption() {
        super(OPTION_CODE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPNoOperationOption.IPNoOperationOption"));
        }
    }

    /**
     * @param copyFlag
     */
    public IPNoOperationOption(final boolean copyFlag) {
        super(copyFlag, OPTION_CLASS, OPTION_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPNoOperationOption.IPNoOperationOption", copyFlag));
        }
    }

    /**
     * @param buffer
     */
    public IPNoOperationOption(final ByteBuffer buffer) {
        super(buffer);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPNoOperationOption.IPNoOperationOption", buffer));
        }
    }

}
