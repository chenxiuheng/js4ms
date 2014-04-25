package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ByteField.java [org.js4ms.jsdk:common]
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

public final class ByteField extends ByteAlignedField<Byte> {

    public ByteField(final int byteOffset) {
        super(byteOffset);
    }

    @Override
    public Byte get(final InputStream is) throws IOException {
        is.mark(this.offset+1);
        is.skip(this.offset);
        int b = (byte)is.read();
        is.reset();
        if (b == -1) throw new java.io.EOFException();
        return (byte)b;
    }

    @Override
    public Byte get(final ByteBuffer buffer) {
        return buffer.get(this.offset);
    }

    @Override
    public void set(final ByteBuffer buffer, final Byte value) {
        buffer.put(this.offset, value);
    }

}
