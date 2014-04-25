package org.js4ms.io.stream;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * FixedLengthInputStream.java [org.js4ms.jsdk:io]
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

/**
 * An input stream that is used to limit the number of bytes that may be
 * read from another InputStream.
 * 
 * @author Gregory Bumgardner
 */
public final class FixedLengthInputStream
                extends InputStream {

    private final InputStream in;

    private final int length;

    private int count;

    /**
     * @param in
     * @param length
     */
    public FixedLengthInputStream(final InputStream in, final int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("invalid length specified");
        }
        this.in = in;
        this.length = length;
        this.count = 0;
    }

    @Override
    public int read(final byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        int remaining = this.length - this.count;
        int actual = in.read(buffer, offset, length > remaining ? remaining : length);
        if (actual != -1) {
            this.count += actual;
        }
        return actual;
    }

    @Override
    public int read() throws IOException {
        if (this.count < this.length) {
            int actual = in.read();
            if (actual != -1) {
                this.count++;
            }
            return actual;
        }
        else {
            return -1;
        }
    }

    /**
     * @return
     */
    public int remaining() {
        return this.length - this.count;
    }
}
