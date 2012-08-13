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

package org.js4ms.util.buffer.parser;

import java.io.IOException;
import java.io.InputStream;

import org.js4ms.common.exceptions.ParseException;
import org.js4ms.util.buffer.fields.Field;


public class StreamParserSelector<T> extends StreamParserMap<T> {

    private Field<Object> keyField;
    
    public StreamParserSelector(final Field<Object> keyField) {
        this.keyField = keyField;
    }

    protected Object getKeyField(final InputStream is) throws IOException {
        return this.keyField != null ? this.keyField.get(is) : null;
    }

    public T parse(final InputStream is) throws ParseException, MissingParserException, IOException {
        return parse(is, getKeyField(is));
    }

}
