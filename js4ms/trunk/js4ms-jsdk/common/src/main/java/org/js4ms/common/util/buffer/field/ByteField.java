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

package org.js4ms.common.util.buffer.field;

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