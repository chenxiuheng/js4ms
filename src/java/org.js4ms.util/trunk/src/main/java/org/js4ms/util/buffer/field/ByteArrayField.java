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

package org.js4ms.util.buffer.field;

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
