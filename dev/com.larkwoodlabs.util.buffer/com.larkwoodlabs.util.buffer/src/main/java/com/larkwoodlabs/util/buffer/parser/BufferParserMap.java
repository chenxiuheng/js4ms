/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

package com.larkwoodlabs.util.buffer.parser;

import java.nio.ByteBuffer;
import java.util.HashMap;

import com.larkwoodlabs.common.exceptions.ParseException;

public class BufferParserMap<T> {

    private HashMap<Object,BufferParser<T>> parsers = new HashMap<Object,BufferParser<T>>();
    
    public BufferParserMap() {
    }

    public void add(KeyedBufferParser<T> parser) {
        add(parser.getKey(), parser);
    }

    public void add(Object key, BufferParser<T> parser) {
        this.parsers.put(key, parser);
    }

    public BufferParser<T> get(Object key) {
        return this.parsers.get(key);
    }

    public boolean contains(Object key) {
        return this.parsers.containsKey(key);
    }

    public void remove(Object key) {
        this.parsers.remove(key);
    }

    public T parse(ByteBuffer buffer, Object key) throws ParseException, MissingParserException {
        BufferParser<T> parser = this.parsers.get(key);
        if (parser == null) {
            // Check for default parser (null key)
            parser = this.parsers.get(null);
            if (parser == null) {
                throw new MissingParserException("unable to locate parser for key="+key);
            }
        }
        return parser.parse(buffer);
    }
}
