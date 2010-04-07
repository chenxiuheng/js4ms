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

public class MissingParserException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -9036300799970422741L;

    public MissingParserException() {
        super();
    }

    public MissingParserException(String message) {
        super(message);
    }

    public MissingParserException(String message, Throwable throwable) {
        super(message,throwable);
    }

    public MissingParserException(Throwable throwable) {
        super(throwable);
    }
}
