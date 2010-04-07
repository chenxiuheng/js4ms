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

import java.nio.ByteBuffer;

public final class ShortBitField extends BitField<Short> {

    public ShortBitField(final int byteOffset, final int bitOffset, final int bitWidth) {
        super(byteOffset, bitOffset, bitWidth);
        if ((bitOffset+bitWidth) > 16) {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }

    @Override
    public Short get(final ByteBuffer buffer) {
        return (short)((buffer.getShort(this.offset) >> this.shift) & this.valueMask);
    }

    @Override
    public void set(final ByteBuffer buffer, final Short value) {
        buffer.putShort(this.offset,(short)((buffer.getShort(this.offset) & this.erasureMask) | ((value & this.valueMask) << this.offset)));
    }
}
