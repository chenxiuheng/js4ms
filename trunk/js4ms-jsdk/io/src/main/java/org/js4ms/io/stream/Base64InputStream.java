package org.js4ms.io.stream;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Base64InputStream.java [org.js4ms.jsdk:io]
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
 * An input stream that converts a stream of bytes read from another InputStream
 * into a base-64 representation.
 * @author Gregory Bumgardner (gbumgard)
 */
public class Base64InputStream
                extends InputStream {

    private static final byte[] DECODE_TABLE = {
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, 62, -1, 63, 52, 53, 54,
                    55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4,
                    5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                    24, 25, -1, -1, -1, -1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34,
                    35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
    };

    final InputStream in;

    private int modulus;

    private int register;

    private int count;

    private boolean isEof;

    /**
     * @param in
     */
    public Base64InputStream(final InputStream in) {
        this.in = in;
        this.modulus = 0;
        this.register = 0;
        this.count = 0;
        this.isEof = false;
    }

    @Override
    public int read() throws IOException {
        if (this.isEof) {
            return -1;
        }
        if (this.modulus == 0 && this.count > 0) {
            this.count--;
            this.register = this.register << 8;
            return ((this.register >> 16) & 0xFF);
        }
        else {
            while (true) {
                int c = in.read();
                if (c != -1 && c != '=') {
                    if (c < DECODE_TABLE.length) {
                        int result = DECODE_TABLE[c];
                        if (result >= 0) {
                            this.modulus = (++this.modulus) % 4;
                            this.register = (this.register << 6) + result;
                            if (this.modulus == 0) {
                                // Third byte in register is next decoded byte
                                this.count = 2;
                                return ((this.register >> 16) & 0xFF);
                            }
                        }
                    }
                }
                else {
                    if (this.modulus != 0) {
                        switch (this.modulus) {
                            case 3:
                                this.register = this.register << 6;
                                this.count = 1;
                                break;
                            case 2:
                                this.register = this.register << 12;
                                this.count = 0;
                                break;
                            case 1:
                                this.modulus = 0;
                                this.count = 0;
                                if (c == -1) {
                                    // Reached EOF without completing last byte
                                    this.isEof = true;
                                    return -1;
                                }
                                else {
                                    // Received an '=' with only one base64 character
                                    // received since last byte was completed.
                                    // Skip it and continue on and wait for a new
                                    // character
                                    continue;
                                }
                        }
                        this.modulus = 0;
                        return ((this.register >> 16) & 0xFF);
                    }
                    else if (c == -1) {
                        this.isEof = true;
                        return -1;
                    }
                }
            }
        }
    }

}
