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

public final class LongBitField extends BitField<Long> {

    public LongBitField(final int byteOffset, final int bitOffset, final int bitWidth) {
        super(byteOffset, bitOffset, bitWidth);
        if ((bitOffset+bitWidth) > 32) {
            throw new java.lang.IndexOutOfBoundsException();
        }
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
