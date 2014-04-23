package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPMultiByteHeaderOption.java [org.js4ms.jsdk:ip]
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
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.ByteField;




/**
 * Base class for multibyte IP Header Options.
 * The option is comprised of an option type, length, and data bytes.
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
 *  |     Type      |     Length    |          Data...              
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
 * </pre>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPMultiByteHeaderOption
                extends IPHeaderOption {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPHeaderOption.ParserType {

        @Override
        public IPHeaderOption parse(final ByteBuffer buffer) throws ParseException {
            return new IPMultiByteHeaderOption(buffer);
        }

        @Override
        public Object getKey() {
            return null; // Any option
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final ByteField OptionLength = new ByteField(1);

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param option
     * @param optionLength
     */
    protected IPMultiByteHeaderOption(final byte option, final int optionLength) {
        super(optionLength, option);
        setOptionLength(optionLength);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPMultiByteHeaderOption.IPMultiByteHeaderOption", option, optionLength));
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     */
    public IPMultiByteHeaderOption(final ByteBuffer buffer) {
        super(consume(buffer, OptionLength.get(buffer)));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPMultiByteHeaderOption.IPMultiByteHeaderOption", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": length=" + getOptionLength()));
    }

    /**
     * @return
     */
    public final int getOptionLength() {
        return OptionLength.get(getBufferInternal());
    }

    /**
     * @param length
     */
    public final void setOptionLength(final int length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPMultiByteHeaderOption.setOptionLength", length));
        }

        OptionLength.set(getBufferInternal(), (byte) length);
    }

}
