/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: IPHeaderOption.java (com.larkwoodlabs.net.ip)
 * 
 * Copyright © 2009-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.net.ip;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.net.ip.ipv4.IPv4RouterAlertOption;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.BooleanField;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.SelectorField;
import com.larkwoodlabs.util.buffer.parser.BufferParserSelector;
import com.larkwoodlabs.util.buffer.parser.KeyedBufferParser;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for IP header options.
 * <pre>
 * An IP datagram may carry one or more options in the header option field.
 * The otions provide for control functions needed or useful in some
 * situations but unnecessary for the most common communications.  The
 * options include provisions for timestamps, security, and special routing.
 * There can be several options present in the option field.
 * The options are variable in length and might not end on a 32-bit boundary.
 * The IP header must be padded to the next word boundary with octets of zeros.
 * The first of these is interpreted as the end-of-options option, and the
 * remainder as IP header padding.
 *
 *   0 
 * +---+---+---+---+---+---+---+---+
 * | C | Class |      Option       |
 * +---+---+---+---+---+---+---+---+
 *   7   6   5   4   3   2   1   0
 *
 * C - Copy flag. Indicates if the option is to be copied into all fragments.
 *   0 Do not copy.
 *   1 Copy.
 *
 * Class -
 *   0 Control.
 *   1 Reserved.
 *   2 Debugging and measurement.
 *   3 Reserved.
 *
 * Option - number that identifies the option
 * </pre>
 * @author Gregory Bumgardner
 */
public abstract class IPHeaderOption extends BufferBackedObject {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static interface ParserType extends KeyedBufferParser<IPHeaderOption> {

    }

    /**
     * 
     */
    public static class Parser extends BufferParserSelector<IPHeaderOption> {

        public Parser() {
            super(new SelectorField<Byte>(IPHeaderOption.OptionCode));
        }

    }


    /*-- Static Variables ---------------------------------------------------*/
    
    /** Logger used to generate IPHeaderOption log entries. */
    public static final Logger logger = Logger.getLogger(IPHeaderOption.class.getName());

    /** */
    public static final String CLASS_NAME = "  ["+IPHeaderOption.class.getName()+"]";

    /** */
    public static final int BASE_OPTION_LENGTH = 1;

    /** */
    public static final ByteField       Option = new ByteField(0); 
    /** */
    public static final ByteBitField    OptionCode = new ByteBitField(0,0,7); 
    /** */
    public static final ByteBitField    OptionNumber = new ByteBitField(0,0,5); 
    /** */
    public static final ByteBitField    OptionClass = new ByteBitField(0,5,2); 
    /** */
    public static final BooleanField    CopyFlag = new BooleanField(0,7); 


    /*-- Static Functions ---------------------------------------------------*/

    /**
     * 
     * @return
     */
    public static Parser constructParser() {
        IPHeaderOption.Parser parser = new IPHeaderOption.Parser();
        parser.add(new IPv4RouterAlertOption.Parser());
        parser.add(new IPNoOperationOption.Parser());
        parser.add(new IPEndOfListOption.Parser());
        parser.add(null, new IPMultiByteHeaderOption.Parser());
        return parser;
    }


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param option
     */
    public IPHeaderOption(final byte option) {
        this(BASE_OPTION_LENGTH,option);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.IPHeaderOption", option));
        }
    }

    /**
     * 
     * @param size
     * @param option
     */
    protected IPHeaderOption(final int size, final byte option) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.IPHeaderOption", size, option));
        }
        
        setOption(option);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param copyFlag
     * @param optionClass
     * @param optionNumber
     */
    public IPHeaderOption(final boolean copyFlag,
                          final int optionClass,
                          final int optionNumber) {
        this(BASE_OPTION_LENGTH, copyFlag, optionClass, optionNumber);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.IPHeaderOption", copyFlag, optionClass, optionNumber));
        }
    }

    /**
     * 
     * @param size
     * @param copyFlag
     * @param optionClass
     * @param optionNumber
     */
    protected IPHeaderOption(final int size,
                             final boolean copyFlag,
                             final int optionClass,
                             final int optionNumber) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.IPHeaderOption", size, copyFlag, optionClass, optionNumber));
        }
        
        setCopyFlag(copyFlag);
        setOptionClass(optionClass);
        setOptionNumber(optionNumber);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }
    
    /**
     * 
     * @param buffer
     */
    protected IPHeaderOption(final ByteBuffer buffer) {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.IPHeaderOption", buffer));
            logState(logger);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
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
        logger.info(ObjectId + " : copy-flag="+getCopyFlag());
        logger.info(ObjectId + " : option-class="+getOptionClass());
        logger.info(ObjectId + " : option-number="+getOptionNumber());
    }

    /**
     * 
     * @return
     */
    public final boolean getCopyFlag() {
        return CopyFlag.get(getBufferInternal());
    }

    /**
     * 
     * @param copyFlag
     */
    public final void setCopyFlag(final boolean copyFlag) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.setCopyFlag", copyFlag));
        }
        
        CopyFlag.set(getBufferInternal(), copyFlag);
    }

    /**
     * 
     * @return
     */
    public final int getOptionClass() {
        return OptionClass.get(getBufferInternal());
    }

    /**
     * 
     * @param optionClass
     */
    public final void setOptionClass(final int optionClass) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.setOptionClass", optionClass ));
        }
        
        OptionClass.set(getBufferInternal(), (byte)optionClass);
    }

    /**
     * 
     * @return
     */
    public final int getOptionCode() {
        return OptionCode.get(getBufferInternal());
    }

    /**
     * 
     * @param optionCode
     */
    public final void setOptionCode(final int optionCode) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.setOptionCode", optionCode));
        }
        
        OptionCode.set(getBufferInternal(), (byte)optionCode);
    }

    /**
     * 
     * @return
     */
    public final int getOptionNumber() {
        return OptionNumber.get(getBufferInternal());
    }

    /**
     * 
     * @param option
     */
    public final void setOptionNumber(final int option) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.setOptionNumber", option));
        }
        
        OptionNumber.set(getBufferInternal(),(byte)option);
    }

    /**
     * 
     * @return
     */
    public final byte getOption() {
        return Option.get(getBufferInternal());
    }
    
    /**
     * 
     * @param option
     */
    public final void setOption(final byte option) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPHeaderOption.setOption", option));
        }
        
        Option.set(getBufferInternal(),option);
    }

    /**
     * 
     * @return
     */
    public abstract int getOptionLength();

    /**
     * 
     * @return
     */
    public boolean isWordAlignmentRequired() {
        return false;
    }

}
