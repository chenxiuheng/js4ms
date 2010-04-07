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

package com.larkwoodlabs.util.buffer.parser;

import java.nio.ByteBuffer;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.Field;

public class BufferParserSelector<T> extends BufferParserMap<T> {

    private Field<Object> keyField;
    
    public BufferParserSelector(Field<Object> keyField) {
        this.keyField = keyField;
    }

    protected Object getKeyField(ByteBuffer buffer) {
        return this.keyField != null ? this.keyField.get(buffer) : null;
    }

    public T parse(ByteBuffer buffer) throws ParseException, MissingParserException {
        return parse(buffer, getKeyField(buffer));
    }
    
}
