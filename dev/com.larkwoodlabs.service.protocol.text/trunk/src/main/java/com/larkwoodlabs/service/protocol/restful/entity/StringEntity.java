/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

package com.larkwoodlabs.service.protocol.restful.entity;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;



/**
 * An RTSP or HTTP request/response entity whose content is retrieved from a String.
 *
 * @author Gregory Bumgardner
 */
public class StringEntity extends RawEntity {

    public static final String US_ASCII =   "US-ASCII";
    public static final String ISO_8859_1 = "ISO-8859-1";
    public static final String UTF_8 =      "UTF-8";
    public static final String UTF_16BE =   "UTF-16BE";
    public static final String UTF_16LE =   "UTF-16LE";
    public static final String UTF_16 =     "UTF-16";

    private final StringBuffer source;
    private Charset encoder;
    private boolean isPrepareRequired = true;

    /**
     * Constructs an entity with an empty string buffer.
     */
    

    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value of <code>text/plain</code>
     * and <code>charset</code> of ISO-8859-1.
     * @param content - The entity content.
     */
    public StringEntity(final String content) {
        this(new StringBuffer(content), "text/plain", ISO_8859_1, true);
    }

    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value of <code>text/plain</code>
     * and <code>charset</code> of ISO-8859-1.
     * @param content - The entity content.
     */
    public StringEntity(final StringBuffer content) {
        this(content, "text/plain", ISO_8859_1, true);
    }

    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value set to the specified content type
     * and <code>charset</code> of ISO-8859-1.
     * @param content - The entity content.
     */
    public StringEntity(final String content, final String contentType) {
        this(new StringBuffer(content), contentType, ISO_8859_1, true);
    }

    /**
     * Constructs an entity from the specified String that will produce a
     * <code>Content Type</code> header value set to the specified content type
     * and <code>charset</code> of ISO-8859-1.
     * @param content - The entity content.
     */
    public StringEntity(final StringBuffer content, final String contentType) {
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
    public StringEntity(final String content,
                        final String contentType,
                        final String characterSet,
                        final boolean addCharset) throws IllegalArgumentException {
        this(new StringBuffer(content), contentType, characterSet, addCharset);
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
    public StringEntity(final StringBuffer content,
                        final String contentType,
                        final String characterSet,
                        final boolean addCharset) throws IllegalArgumentException {
        super();
        this.source = content;
        this.encoder = Charset.forName(characterSet);
        if (addCharset) {
            this.contentType = contentType + "; charset=" + encoder.name();
        }
        else {
            this.contentType = contentType;
        }
        this.isPrepareRequired = true;
        prepareContentStream();
    }

    
    public void append(final String content) {
        this.isPrepareRequired = true;
        this.source.append(content);
    }

    public void append(final StringBuffer content) {
        this.isPrepareRequired = true;
        this.source.append(content);
    }

    @Override
    public int getContentLength() {
        prepareContentStream();
        return this.contentLength;
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    private void logState(Logger logger) {
        logger.info(log.msg(": ----> Content"));
        logger.info(log.msg("\n"+this.source.toString()));
        logger.info(log.msg(": <---- Content "));
    }

    void prepareContentStream() {
        if (this.isPrepareRequired) {
            ByteBuffer buffer = encoder.encode(this.source.toString());
            this.content = new ByteArrayInputStream(buffer.array(), buffer.arrayOffset(), buffer.limit());
            this.contentLength = buffer.limit();
            this.isPrepareRequired = false;
        }
    }
}
