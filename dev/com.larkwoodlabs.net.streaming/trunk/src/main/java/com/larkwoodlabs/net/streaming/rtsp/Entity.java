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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.LoggableBase;

/**
 * An RTSP or HTTP request/response entity whose content is retrieved from an InputStream.
 *
 * @author Gregory Bumgardner
 */
public class Entity extends LoggableBase {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(Server.class.getName());

    private static final int BUFFER_SIZE = 2048;

    
    /*-- Member Variables ----------------------------------------------------*/

    protected InputStream content;
    protected String contentType;
    protected String contentEncoding;
    protected int contentLength;
    protected boolean isConsumed;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Protected constructor.
     */
    protected Entity() {
        this(null,-1,null,null);
    }

    /**
     * 
     * @param content
     */
    public Entity(final InputStream content) {
        this(content, -1);
    }

    /**
     * Constructs an HTTP entity.
     * @param content - The content of the entity.
     * @param contentLength - The content length of the entity.
     */
    public Entity(final InputStream content, final int contentLength) {
        this(content, contentLength, null, null);
    }

    /**
     * Constructs an HTTP entity.
     * @param content - The content of the entity.
     * @param contentLength - The content length of the entity.
     * @param contentType - The content type of the entity.
     */
    public Entity(final InputStream content,
                  final int contentLength,
                  final String contentType) {
        this(content, contentLength, contentType, null);
    }

    /**
     * Constructs an HTTP entity.
     * @param content - The content of the entity.
     * @param contentType - The content type of the entity.
     * @param contentEncoding - The content encoding of the entity.
     */
    public Entity(final InputStream content,
                  final String contentType,
                  final String contentEncoding) {
        this(content, -1, contentType, contentEncoding);
    }

    /**
     * Constructs an HTTP entity.
     * @param content - The content of the entity.
     * @param contentLength - The content length of the entity.
     * @param contentType - The content type of the entity.
     * @param contentEncoding - The content encoding of the entity.
     */
    public Entity(final InputStream content,
                  final int contentLength,
                  final String contentType,
                  final String contentEncoding) {
        this.content = content;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.isConsumed = false;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }

    private void logState(Logger logger) {
        logger.info(ObjectId + " : content-length = " + this.contentLength);
        if (this.contentType != null) {
            logger.info(ObjectId + " : content-type = " + this.contentType);
        }
        if (this.contentEncoding != null) {
            logger.info(ObjectId + " : content-encoding = " + this.contentEncoding);
        }
    }

    /**
     * Returns the content length of the entity.
     * Used to set the value of the Content-Length HTTP header.
     * Returns -1 if no length was specified.
     */
    public int getContentLength() {
        return this.contentLength;
    }

    /**
     * Returns the content type of the entity.
     * Used to set the Content-Type HTTP header.
     * Returns <code>null</code> if no content type was specified.
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the content encoding of the entity.
     * Used to set the Content-Encoding HTTP header.
     * Returns <code>null</code> if no content encoding was specified.
     */
    public String getContentEncoding() {
        return this.contentEncoding;
    }

    /**
     * Indicates whether the entity has been consumed (the InputStream has reached EOF).
     */
    public boolean isConsumed() {
        return this.isConsumed;
    }

    /**
     * Indicates whether the entity is currently streaming (the InputStream has not reached EOF).
     */
    public boolean isStreaming() {
        return !this.isConsumed;
    }
    

    /**
     * Writes this entity to the specified OutputStream.
     * @param outstream - The output stream that will receive the entity.
     * @throws IOException
     */
    public void writeTo(final OutputStream outstream) throws IOException {
        InputStream instream = this.content;
        byte[] buffer = new byte[BUFFER_SIZE];
        int count;
        if (this.contentLength < 0) {
            // consume until EOF
            while ((count = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, count);
            }
        } else {
            // consume no more than length
            long remaining = this.contentLength;
            while (remaining > 0) {
                count = instream.read(buffer, 0, (int)Math.min(BUFFER_SIZE, remaining));
                if (count == -1) {
                    break;
                }
                outstream.write(buffer, 0, count);
                remaining -= count;
            }
            outstream.flush();
        }
        this.isConsumed = true;
    }

    /**
     * Consumes (reads) the remaining entity content.
     * @throws IOException
     */
    public void consumeContent() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (this.content.read(buffer) != -1) {
        }
    }
}
