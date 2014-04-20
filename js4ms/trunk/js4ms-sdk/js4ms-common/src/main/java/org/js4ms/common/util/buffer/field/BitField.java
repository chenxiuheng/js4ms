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

/**
 * 
 * @author Gregory Bumgardner
 *
 * @param <Type>
 */
public abstract class BitField<Type> extends ByteAlignedField<Type> {

    protected final int shift;
    protected final long valueMask;
    protected final long erasureMask;

    protected BitField(final int byteOffset, final int bitOffset, final int bitWidth) {
        super(byteOffset);
        this.shift = bitOffset;
        this.valueMask = (1 << bitWidth)-1;
        this.erasureMask = ~(this.valueMask << bitOffset);
    }

    public final int getShift() {
        return this.shift;
    }

    public final long getValueMask() {
        return this.valueMask;
    }
    
    public final long getErasureMask() {
        return this.erasureMask;
    }
}
