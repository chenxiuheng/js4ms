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
import java.util.Iterator;
import java.util.LinkedList;

import com.larkwoodlabs.common.exceptions.ParseException;

public class BufferParserMapChain<T> {

    LinkedList<BufferParser<T>> chain = new LinkedList<BufferParser<T>>();
    
    public void add(BufferParser<T> parser) {
        this.chain.add(parser);
    }
    
    public void remove(BufferParser<T> parser) {
        this.chain.remove(parser);
    }

    public T parse(ByteBuffer buffer) throws ParseException, MissingParserException {
        Iterator<BufferParser<T>> iter = this.chain.iterator();
        while (iter.hasNext()) {
            T object = iter.next().parse(buffer);
            if (object != null) return object;
        }
        return null;
    }

}
