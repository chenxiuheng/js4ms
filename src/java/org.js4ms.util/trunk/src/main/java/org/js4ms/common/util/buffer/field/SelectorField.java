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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class SelectorField<T> implements Field<Object> {

    private final Field<T> field;

    public SelectorField(final Field<T> field) {
        this.field = field;
    }

    @Override
    public Object get(final InputStream is) throws IOException {
        return (Object)field.get(is);
    }

    @Override
    public Object get(final ByteBuffer buffer) {
        return (Object)field.get(buffer);
    }

    @Override
    public void set(final ByteBuffer buffer, final Object value) {
        // Ignored
    }
}
