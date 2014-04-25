package org.js4ms.ip.ipv4;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv4RouterAlertOption.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.ip.IPHeaderOption;
import org.js4ms.ip.IPMultiByteHeaderOption;




/**
 * Represents an IPv4 Router Alert Option. <h3>Option Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |      Type     |    Length     |      Router Alert Value       |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <dl>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>The Type field has the following fixed value: Copy=1, Class=0, Option=20:
 * 
 * <pre>
 * +-+-+-+-+-+-+-+-+
 * |1|0 0|1 0 1 0 0| 0x94
 * +-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <p>
 * See {@link #getCopyFlag()}, {@link #getOptionClass()}, {@link #getOptionNumber()}.</dd>
 * <p>
 * <dt><u>Length</u></dt>
 * <p>
 * <dd>Length field is always 4.</dd>
 * <p>
 * <dt><u>Router Alert Value</u></dt>
 * <p>
 * <dd>Describes action router should take.
 * 
 * <pre>
 *   0        Router shall examine packet.
 *   1-65535  Reserved.
 * </pre>
 * 
 * <p>
 * See {@link #getRouterAlertValue()}, {@link #setRouterAlertValue(short)}.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner
 */
public final class IPv4RouterAlertOption
                extends IPMultiByteHeaderOption {

    /*-- Inner Classes ---------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPHeaderOption.ParserType {

        @Override
        public IPHeaderOption parse(final ByteBuffer buffer) throws ParseException {
            return new IPv4RouterAlertOption(buffer);
        }

        @Override
        public Object getKey() {
            return IPv4RouterAlertOption.OPTION_CODE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final ShortField RouterAlertValue = new ShortField(2);

    /**
     * The router alert option field has the following fixed value: Copy=1,
     * Class=0, Option=20
     * 
     * <pre>
     * +-+-+-+-+-+-+-+-+
     * |1|0 0|1 0 1 0 0| 0x94
     * +-+-+-+-+-+-+-+-+
     * </pre>
     */
    public static final byte OPTION_VALUE = (byte) 0x94;

    /** */
    public static final byte OPTION_CODE = (byte) 0x14;

    /** */
    public static final byte OPTION_LENGTH = 4;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public IPv4RouterAlertOption() {
        this((short) 0);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4RouterAlertOption.IPv4RouterAlertOption"));
        }
    }

    /**
     * @param routerAlertValue
     */
    public IPv4RouterAlertOption(final short routerAlertValue) {
        super(OPTION_VALUE, OPTION_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4RouterAlertOption.IPv4RouterAlertOption", routerAlertValue));
        }

        setRouterAlertValue(routerAlertValue);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     */
    public IPv4RouterAlertOption(final ByteBuffer buffer) {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4RouterAlertOption.IPv4RouterAlertOption", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": router-alert-value=" + getRouterAlertValue()));
    }

    /**
     * @return
     */
    public short getRouterAlertValue() {
        return RouterAlertValue.get(getBufferInternal());
    }

    /**
     * @param routerAlertValue
     */
    public void setRouterAlertValue(final short routerAlertValue) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4RouterAlertOption.setRouterAlertValue", routerAlertValue));
        }

        RouterAlertValue.set(getBufferInternal(), routerAlertValue);
    }

}
