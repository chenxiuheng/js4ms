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

package com.larkwoodlabs.net;

import java.nio.ByteBuffer;

import com.larkwoodlabs.util.buffer.parser.BufferParser;
import com.larkwoodlabs.util.logging.Loggable;

/**
 * 
 * @author Gregory Bumgardner
 */
public interface ApplicationMessage extends Loggable {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static interface Parser extends BufferParser<ApplicationMessage> {
        
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Returns the byte-length of the message header.
     * For fixed-size messages, this is often the same value as the returned from {@link #getTotalLength()}. 
     */
    public int getHeaderLength();
    
    /**
     * Returns the total byte-length of the message including the message header.
     */
    public int getTotalLength();
    
    /**
     * Writes the message to a byte buffer.
     * @param buffer - a byte array.
     * @param offset - the offset within the array at which to write the message.
     * @throws java.lang.IndexOutOfBoundsException if the buffer is not of sufficient size to accommodate the message. 
     */
    public void writeTo(ByteBuffer buffer);
    
}
