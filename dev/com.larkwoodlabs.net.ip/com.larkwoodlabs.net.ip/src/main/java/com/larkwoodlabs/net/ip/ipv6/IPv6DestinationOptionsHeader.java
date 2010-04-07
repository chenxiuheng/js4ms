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

package com.larkwoodlabs.net.ip.ipv6;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

public final class IPv6DestinationOptionsHeader extends IPv6OptionsHeader {

    public static class Parser extends IPv6OptionsHeader.Parser {

        @Override
        public IPv6OptionsHeader constructHeader(ByteBuffer buffer) throws ParseException {
            return new IPv6DestinationOptionsHeader(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }
    }

    public static final byte IP_PROTOCOL_NUMBER = 60;

    public IPv6DestinationOptionsHeader() {
        super(IP_PROTOCOL_NUMBER);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this));
            log();
        }
    }

    public IPv6DestinationOptionsHeader(ByteBuffer buffer) throws ParseException {
        super(buffer, IP_PROTOCOL_NUMBER);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this, buffer));
            log();
        }
    }
}
