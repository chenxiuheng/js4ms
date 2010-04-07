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

public final class VariableByteArrayField<LengthType> extends ByteAlignedField<byte[]> {
    
    final SelectorField<LengthType> lengthField;
    
    public VariableByteArrayField(final int offset, final Field<LengthType> lengthField) {
        super(offset);
        this.lengthField = new SelectorField<LengthType>(lengthField);
    }
    
    public int getSize(final ByteBuffer buffer) {
        return (Integer)this.lengthField.get(buffer);
    }

    public byte[] get(final ByteBuffer buffer) {
        byte[] bytes = new byte[getSize(buffer)];
        buffer.position(this.offset);
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public void set(final ByteBuffer buffer, final byte[] value) {
        buffer.position(this.offset);
        buffer.put(value, 0, Math.min(value.length, getSize(buffer)));
    }
    

}
