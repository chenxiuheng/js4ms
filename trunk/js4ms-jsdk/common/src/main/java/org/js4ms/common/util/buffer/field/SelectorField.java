package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * SelectorField.java [org.js4ms.jsdk:common]
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
import java.nio.ByteBuffer;

public final class SelectorField<T> implements Field<Object> {

    private final Field<T> field;

    public SelectorField(final Field<T> field) {
        this.field = field;
    }

    @Override
    public Object get(final InputStream is) throws IOException {
        return (Object)field.get(is);
    }

    @Override
    public Object get(final ByteBuffer buffer) {
        return (Object)field.get(buffer);
    }

    @Override
    public void set(final ByteBuffer buffer, final Object value) {
        // Ignored
    }
}
