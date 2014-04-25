package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IntegerField.java [org.js4ms.jsdk:common]
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

public final class IntegerField extends ByteAlignedField<Integer> {

    private final static int SIZE = (Integer.SIZE >> 3);

    public IntegerField(final int byteOffset) {
        super(byteOffset);
    }

    @Override
    public Integer get(final InputStream is) throws IOException {
        is.mark(this.offset+SIZE);
        is.skip(this.offset);
        byte bytes[] = new byte[SIZE];
        int count = is.read(bytes);
        if (count != SIZE) throw new EOFException();
        long result = 0;
        for (int i = 0; i < SIZE; i++)
        {
           result = (result << 8) | (bytes[i] & 0xff);
        }
        is.reset();
        return (int)result;
    }

    @Override
    public Integer get(final ByteBuffer buffer) {
        return buffer.getInt(this.offset);
    }

    @Override
    public void set(final ByteBuffer buffer, final Integer value) {
        buffer.putInt(this.offset, value);
    }

}
