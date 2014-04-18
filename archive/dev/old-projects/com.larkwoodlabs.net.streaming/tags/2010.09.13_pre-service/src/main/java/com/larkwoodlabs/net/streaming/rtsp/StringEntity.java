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

package com.larkwoodlabs.net.streaming.rtsp;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * An RTSP or HTTP request/response entity whose content is retrieved from a String.
 *
 * @author Gregory Bumgardner
 */
public class StringEntity extends Entity {

    public static final String US_ASCII =   "US-ASCII";
    public static final String ISO_8859_1 = "ISO-8859-1";
    public static final String UTF_8 =      "UTF-8";
    public static final String UTF_16BE =   "UTF-16BE";
    public static final String UTF_16LE =   "UTF-16LE";
    public static final String UTF_16 =     "UTF-16";

    private final String source;
    
    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value of <code>text/plain</code>
     * and <code>charset</code> of ISO-8859-1.
     * @param content - The entity content.
     */
    public StringEntity(String content) {
        this(content, MimeType.text.plain, ISO_8859_1, true);
    }

    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value set to the specified content type
     * and <code>charset</code> of ISO-8859-1.
     * @param content - The entity content.
     */
    public StringEntity(String content, String contentType) {
        this(content, contentType, ISO_8859_1, true);
    }

    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value set to the specified content type
     * and an optional <code>charset</code> parameter set to the specified
     * character set name.
     * The <code>charset</code> parameter is only added to the <code>Content Type</code>
     * header if the <code>addCharset</code> parameter is <code>true</code>.
     * 
     * @param content - The entity content.
     */
    public StringEntity(String content, String contentType, String characterSet, boolean addCharset) throws IllegalArgumentException {
        super();
        this.source = content;
        Charset encoder = Charset.forName(characterSet);
        ByteBuffer buffer = encoder.encode(content);
        this.content = new ByteArrayInputStream(buffer.array(), buffer.arrayOffset(), buffer.limit());
        this.contentLength = buffer.limit();
        if (addCharset) {
            this.contentType = contentType + "; charset=" + encoder.name();
        }
        else {
            this.contentType = contentType;
        }
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    private void logState(Logger logger) {
        logger.info(ObjectId + " : ----> Content");
        logger.info(this.source);
        logger.info(ObjectId + " : <---- Content ");
    }
}
