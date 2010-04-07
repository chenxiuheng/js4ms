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


public class BoundException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -5587197699181393667L;

    protected final Object object;
    protected final Throwable throwable;
    
    public BoundException(final Object object, final Throwable throwable) {
        this.object = object;
        this.throwable = throwable;
    }
    
    public Object getObject() {
        return this.object;
    }
    
    public Throwable getThrowable() {
        return this.throwable;
    }
    
    public void rethrow() throws Throwable {
        throw this.throwable;
    }
}
