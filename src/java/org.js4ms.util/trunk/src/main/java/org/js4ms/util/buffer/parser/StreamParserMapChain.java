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

package org.js4ms.util.buffer.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.js4ms.common.exception.ParseException;



public class StreamParserMapChain<T> {

    final LinkedList<StreamParser<T>> chain = new LinkedList<StreamParser<T>>();
    
    public void add(final StreamParser<T> parser) {
        this.chain.add(parser);
    }
    
    public void remove(final StreamParser<T> parser) {
        this.chain.remove(parser);
    }


    public T parse(final InputStream is) throws ParseException, MissingParserException, IOException {
        Iterator<StreamParser<T>> iter = this.chain.iterator();
        while (iter.hasNext()) {
            T object = iter.next().parse(is);
            if (object != null) return object;
        }
        return null;
    }

}
