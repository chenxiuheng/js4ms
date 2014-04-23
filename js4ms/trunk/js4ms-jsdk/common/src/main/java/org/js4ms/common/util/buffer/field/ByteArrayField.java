package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ByteArrayField.java [org.js4ms.jsdk:common]
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

public final class ByteArrayField extends ArrayField<byte[]> {

    public ByteArrayField(final int offset, final int size) {
        super(offset, size);
    }

    @Override
    public byte[] get(final InputStream is) throws IOException {
        byte[] bytes = new byte[this.size];
        is.mark(this.offset+this.size);
        is.skip(this.offset);
        int count = is.read(bytes);
        is.reset();
        if (count != this.size) throw new EOFException();
        return bytes;
    }

    @Override
    public byte[] get(final ByteBuffer buffer) {
        byte[] bytes = new byte[this.size];
        int position = buffer.position();
        buffer.position(this.offset);
        buffer.get(bytes);
        buffer.position(position);
        return bytes;
    }

    @Override
    public void set(final ByteBuffer buffer, final byte[] value) {
        int position = buffer.position();
        buffer.position(this.offset);
        buffer.put(value, 0, this.size);
        buffer.position(position);
    }

}
