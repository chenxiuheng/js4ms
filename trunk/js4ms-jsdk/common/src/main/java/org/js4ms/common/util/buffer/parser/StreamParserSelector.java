package org.js4ms.common.util.buffer.parser;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * StreamParserSelector.java [org.js4ms.jsdk:common]
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


import java.io.IOException;
import java.io.InputStream;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.Field;




public class StreamParserSelector<T> extends StreamParserMap<T> {

    private Field<Object> keyField;
    
    public StreamParserSelector(final Field<Object> keyField) {
        this.keyField = keyField;
    }

    protected Object getKeyField(final InputStream is) throws IOException {
        return this.keyField != null ? this.keyField.get(is) : null;
    }

    public T parse(final InputStream is) throws ParseException, MissingParserException, IOException {
        return parse(is, getKeyField(is));
    }

}
