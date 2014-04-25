package org.js4ms.common.util.buffer.field;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * BitField.java [org.js4ms.jsdk:common]
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
