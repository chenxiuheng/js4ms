package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * LongBitField.java [org.js4ms.jsdk:common]
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
import java.math.BigInteger;
import java.nio.ByteBuffer;

public final class LongBitField extends BitField<Long> {

    private final static int SIZE = (Long.SIZE >> 3);
    
    public LongBitField(final int byteOffset, final int bitOffset, final int bitWidth) {
        super(byteOffset, bitOffset, bitWidth);
        if ((bitOffset+bitWidth) > 32) {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }

    @Override
    public Long get(final InputStream is) throws IOException {
        is.mark(this.offset + SIZE);
        is.skip(this.offset);
        byte bytes[] = new byte[SIZE];
        int count = is.read(bytes);
        if (count != 0) throw new EOFException();
        BigInteger bigInt = new BigInteger(bytes);
        return (long)((bigInt.longValue() >> this.shift) & this.valueMask);
    }

    @Override
    public Long get(final ByteBuffer buffer) {
        return (long)((buffer.getLong(this.offset) >> this.shift) & this.valueMask);
    }

    @Override
    public void set(final ByteBuffer buffer, final Long value) {
        buffer.putLong(this.offset,(long)((buffer.getLong(this.offset) & this.erasureMask) | ((value & this.valueMask) << this.offset)));
    }

}
