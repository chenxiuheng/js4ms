/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.io;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 *
 * @author Gregory Bumgardner
 */
public final class FixedLengthInputStream extends InputStream {

    private final InputStream in;
    private final int length;
    private int count;

    /**
     * 
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
     * 
     * @return
     */
    public int remaining() {
        return this.length - this.count;
    }
}
