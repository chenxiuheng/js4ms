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

package com.larkwoodlabs.util.buffer.fields;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class BooleanField extends ByteAlignedField<Boolean> {

    private final int mask;

    protected BooleanField(final int bitOffset) {
        this(bitOffset/8, bitOffset%8);
    }

    public BooleanField(final int byteOffset, final int bitOffset) {
        super(byteOffset);
        this.mask = 0x1 << bitOffset;
    }
    
    public int getMask() {
        return this.mask;
    }

    @Override
    public Boolean get(final InputStream is) throws IOException {
        is.mark(this.offset+1);
        is.skip(this.offset);
        int b = (byte)is.read();
        is.reset();
        if (b == -1) throw new java.io.EOFException();
        return ((byte)b & this.mask) != 0;
    }

    @Override
    public Boolean get(final ByteBuffer buffer) {
        return (buffer.get(this.offset) & this.mask) != 0;
    }

    @Override
    public void set(final ByteBuffer buffer, final Boolean value) {
        buffer.put(this.offset, (byte)((buffer.get(this.offset) & ~this.mask) | (value ? this.mask : 0)));
    }
}
