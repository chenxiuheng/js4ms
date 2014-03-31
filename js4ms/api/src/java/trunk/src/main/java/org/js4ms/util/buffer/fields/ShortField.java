/*
 * Copyright (C) 2009-2010 Larkwood Labs Software.
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

package org.js4ms.util.buffer.fields;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ShortField extends ByteAlignedField<Short> {

    private final static int SIZE = (Short.SIZE >> 3);

    public ShortField(final int byteOffset) {
        super(byteOffset);
    }

    @Override
    public Short get(final InputStream is) throws IOException {
        is.mark(this.offset + SIZE);
        is.skip(this.offset);
        byte bytes[] = new byte[SIZE];
        int count = is.read(bytes);
        is.reset();
        if (count != SIZE) throw new EOFException();
        long result = 0;
        for (int i = 0; i < SIZE; i++)
        {
           result = (result << 8) | (bytes[i] & 0xff);
        }
        return (short)result;
    }

    @Override
    public Short get(final ByteBuffer buffer) {
        return buffer.getShort(this.offset);
    }

    @Override
    public void set(final ByteBuffer buffer, final Short value) {
        buffer.putShort(this.offset, value);
    }

}
