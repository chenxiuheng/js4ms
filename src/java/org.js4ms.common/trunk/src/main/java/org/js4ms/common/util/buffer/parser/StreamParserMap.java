/*
 * Copyright (C) 2009-2010 Larkwood Labs Software.
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

package org.js4ms.common.util.buffer.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.js4ms.common.exception.ParseException;



public class StreamParserMap<T> {

    final private HashMap<Object,StreamParser<T>> parsers = new HashMap<Object,StreamParser<T>>();
    
    public StreamParserMap() {
    }

    public void add(final KeyedStreamParser<T> parser) {
        add(parser.getKey(), parser);
    }

    public void add(final Object key, final StreamParser<T> parser) {
        this.parsers.put(key, parser);
    }

    public StreamParser<T> get(final Object key) {
        return this.parsers.get(key);
    }

    public boolean contains(final Object key) {
        return this.parsers.containsKey(key);
    }

    public void remove(final Object key) {
        this.parsers.remove(key);
    }

    public T parse(final InputStream is, final Object key) throws ParseException, MissingParserException, IOException {
        StreamParser<T> parser = this.parsers.get(key);
        if (parser == null) {
            // Check for default parser (null key)
            parser = this.parsers.get(null);
            if (parser == null) {
                throw new MissingParserException("unable to locate parser for key="+key);
            }
        }
        return parser.parse(is);
    }
}
