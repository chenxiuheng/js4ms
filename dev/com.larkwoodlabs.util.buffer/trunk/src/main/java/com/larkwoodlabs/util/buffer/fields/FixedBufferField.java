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

public class FixedBufferField extends ArrayField<ByteBuffer> {

    public FixedBufferField(final int offset, final int size) {
        super(offset, size);
    }

    @Override
    public final ByteBuffer get(final ByteBuffer buffer) {
        int position = buffer.position();
        buffer.position(this.offset);
        ByteBuffer result = buffer.slice();
        result.limit(Math.min(this.size,result.limit()));
        buffer.position(position);
        return result.slice();
    }

    @Override
    public final void set(final ByteBuffer buffer, final ByteBuffer value) {
        int position = buffer.position();
        buffer.position(this.offset);
        ByteBuffer source = value.slice();
        source.limit(Math.min(this.size,source.limit()));
        buffer.put(source);
        buffer.position(position);
    }

}
