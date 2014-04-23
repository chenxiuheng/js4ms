package org.js4ms.common.util.buffer.parser;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * BufferParserMap.java [org.js4ms.jsdk:common]
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
import java.util.HashMap;

import org.js4ms.common.exception.ParseException;



public class BufferParserMap<T> {

    final private HashMap<Object,BufferParser<T>> parsers = new HashMap<Object,BufferParser<T>>();
    
    public BufferParserMap() {
    }

    public void add(final KeyedBufferParser<T> parser) {
        add(parser.getKey(), parser);
    }

    public void add(final Object key, final BufferParser<T> parser) {
        this.parsers.put(key, parser);
    }

    public BufferParser<T> get(final Object key) {
        return this.parsers.get(key);
    }

    public boolean contains(final Object key) {
        return this.parsers.containsKey(key);
    }

    public void remove(final Object key) {
        this.parsers.remove(key);
    }

    public T parse(final ByteBuffer buffer, final Object key) throws ParseException, MissingParserException {
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
