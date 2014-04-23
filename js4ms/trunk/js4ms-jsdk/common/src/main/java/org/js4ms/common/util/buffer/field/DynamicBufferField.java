package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * DynamicBufferField.java [org.js4ms.jsdk:common]
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

public abstract class DynamicBufferField implements Field<ByteBuffer> {

    protected DynamicBufferField() {
    }

    public abstract int getOffset(final ByteBuffer buffer);
    
    public abstract int getSize(final ByteBuffer buffer);

    @Override
    public final ByteBuffer get(final ByteBuffer buffer) {
        int position = buffer.position();
        buffer.position(getOffset(buffer));
        ByteBuffer result = buffer.slice();
        result.limit(Math.min(getSize(buffer),result.limit()));
        buffer.position(position);
        return result.slice();
    }

    @Override
    public final void set(final ByteBuffer buffer, final ByteBuffer value) {
        int position = buffer.position();
        buffer.position(getOffset(buffer));
        ByteBuffer source = value.slice();
        source.limit(Math.min(getSize(buffer),source.limit()));
        buffer.put(source);
        buffer.position(position);
    }
    
}
