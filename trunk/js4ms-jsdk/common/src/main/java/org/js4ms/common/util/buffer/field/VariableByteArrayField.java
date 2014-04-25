package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * VariableByteArrayField.java [org.js4ms.jsdk:common]
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


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class VariableByteArrayField<LengthType> extends ByteAlignedField<byte[]> {
    
    final SelectorField<LengthType> lengthField;
    
    public VariableByteArrayField(final int offset, final Field<LengthType> lengthField) {
        super(offset);
        this.lengthField = new SelectorField<LengthType>(lengthField);
    }
    
    public int getSize(final InputStream is) throws IOException {
        return (Integer)this.lengthField.get(is);
    }

    public int getSize(final ByteBuffer buffer) {
        return (Integer)this.lengthField.get(buffer);
    }

    public byte[] get(final InputStream is) throws IOException {
        int size = getSize(is);
        byte[] bytes = new byte[getSize(is)];
        is.mark(this.offset+size);
        is.skip(this.offset);
        int count = is.read(bytes);
        is.reset();
        if (count != size) throw new EOFException();
        return bytes;
    }

    public byte[] get(final ByteBuffer buffer) {
        byte[] bytes = new byte[getSize(buffer)];
        buffer.position(this.offset);
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public void set(final ByteBuffer buffer, final byte[] value) {
        buffer.position(this.offset);
        buffer.put(value, 0, Math.min(value.length, getSize(buffer)));
    }
    

}
