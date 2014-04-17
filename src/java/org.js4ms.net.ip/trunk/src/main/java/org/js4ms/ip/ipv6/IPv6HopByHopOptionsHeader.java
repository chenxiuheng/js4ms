/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: IPv6HopByHopOptionsHeader.java (org.js4ms.net.ip.ipv6)
 * 
 * Copyright (C) 2009-2012 Cisco Systems, Inc.
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

package org.js4ms.ip.ipv6;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;




/**
 * Represents an IPv6 Hop-By-Hop Options header.
 * 
 *
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IPv6HopByHopOptionsHeader extends IPv6OptionsHeader {

    /**
     * 
     */
    public static class Parser extends IPv6OptionsHeader.Parser {

        @Override
        public IPv6OptionsHeader constructHeader(final ByteBuffer buffer) throws ParseException {
            return new IPv6HopByHopOptionsHeader(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }
    }

    /** Protocol number for IPv6 Hop-by-Hop Options headers. */
    public static final byte IP_PROTOCOL_NUMBER = 0;

    /**
     * 
     */
    public IPv6HopByHopOptionsHeader() {
        super(IP_PROTOCOL_NUMBER);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this));
            log(Level.FINER);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv6HopByHopOptionsHeader(final ByteBuffer buffer) throws ParseException {
        super(buffer, IP_PROTOCOL_NUMBER);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this, buffer));
            log(Level.FINER);
        }
    }

}
