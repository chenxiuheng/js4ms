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

package com.larkwoodlabs.common.exceptions;

import java.util.Iterator;
import java.util.LinkedList;

public class MultiException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 4050769134360179803L;

    LinkedList<Throwable> throwables;
    
    public MultiException() {
    }
    
    public void add(Throwable t) {
        if (this.throwables == null) {
            this.throwables = new LinkedList<Throwable>();
        }
        this.throwables.add(t);
    }
    
    public Iterator<Throwable> iterator() {
        return this.throwables.iterator();
    }
    
    public void rethrow() throws MultiException {
        if (!this.throwables.isEmpty()) {
            throw this;
        }
    }
}
