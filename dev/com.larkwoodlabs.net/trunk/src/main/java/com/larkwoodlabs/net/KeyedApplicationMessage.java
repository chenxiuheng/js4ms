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

package com.larkwoodlabs.net;

import com.larkwoodlabs.util.buffer.fields.Field;
import com.larkwoodlabs.util.buffer.parser.BufferParserSelector;
import com.larkwoodlabs.util.buffer.parser.KeyedBufferParser;

/**
 * 
 * @param <KeyType>
 *
 * @author Gregory Bumgardner
 */
public interface KeyedApplicationMessage<KeyType> extends ApplicationMessage {
    
    /*-- Inner Classes ------------------------------------------------------*/
    
    /**
     * Base interface for individual application message parsers.
     */
    public static interface ParserType extends KeyedBufferParser<KeyedApplicationMessage<?>> {
        
    }
    
    /**
     * Base class for parsers that parse a family of application messages.
     * Typically used in base message classes associated with a single application protocol.
     */
    public static class Parser extends BufferParserSelector<KeyedApplicationMessage<?>> implements ApplicationMessage.Parser {

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
