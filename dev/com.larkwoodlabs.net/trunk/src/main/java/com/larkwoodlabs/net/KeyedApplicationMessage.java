/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: KeyedApplicationMessage.java (com.larkwoodlabs.net)
 * 
 * Copyright � 2010-2012 Cisco Systems, Inc.
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

package com.larkwoodlabs.net;

import com.larkwoodlabs.util.buffer.fields.Field;
import com.larkwoodlabs.util.buffer.parser.BufferParserSelector;
import com.larkwoodlabs.util.buffer.parser.KeyedBufferParser;

/**
 * Interface implemented by typed-message classes.
 * Classes that implement this interface support a key-based lookup
 * of a matching message parser.
 * 
 * @param <KeyType>
 *            The object type used to map message types to parsers.
 * @author Greg Bumgardner (gbumgard)
 */
public interface KeyedApplicationMessage<KeyType>
                extends ApplicationMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * Base interface for individual application message parsers.
     */
    public static interface ParserType
                    extends KeyedBufferParser<KeyedApplicationMessage<?>> {

    }

    /**
     * Base class for parsers that parse a family of application messages.
     * Typically used in base message classes associated with a single application
     * protocol.
     */
    public static class Parser
                    extends BufferParserSelector<KeyedApplicationMessage<?>>
                    implements ApplicationMessage.Parser {

        public Parser(Field<Object> keyField) {
            super(keyField);
        }

    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Returns the message key value.
     */
    public KeyType getType();

}
