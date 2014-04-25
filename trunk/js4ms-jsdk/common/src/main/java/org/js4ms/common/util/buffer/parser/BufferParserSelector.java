package org.js4ms.common.util.buffer.parser;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * BufferParserSelector.java [org.js4ms.jsdk:common]
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

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.Field;




public class BufferParserSelector<T> extends BufferParserMap<T> {

    private Field<Object> keyField;
    
    public BufferParserSelector(final Field<Object> keyField) {
        this.keyField = keyField;
    }

    protected Object getKeyField(final ByteBuffer buffer) {
        return this.keyField != null ? this.keyField.get(buffer) : null;
    }

    public T parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
        return parse(buffer, getKeyField(buffer));
    }

}
