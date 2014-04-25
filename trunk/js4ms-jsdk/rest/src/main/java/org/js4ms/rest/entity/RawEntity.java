package org.js4ms.rest.entity;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RawEntity.java [org.js4ms.jsdk:rest]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.rest.common.MessageException;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.header.DateHeader;
import org.js4ms.rest.header.SimpleMessageHeader;
import org.js4ms.rest.message.Message;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.StatusCode;
import org.js4ms.rest.util.DateUtil;





/**
 * Entity-header fields define optional metainformation about the entity-body
 * or, if no body is present, about the resource identified by the request.
 * <pre>
 * entity-header       =    Allow
 *                          |    Content-Base
 *                          |    Content-Encoding
 *                          |    Content-Disposition
 *                          |    Content-Language
 *                          |    Content-Length
 *                          |    Content-Location
 *                          |    Content-Type
 *                          |    Expires
 *                          |    Last-Modified
 *                          |    extension-header
 *      extension-header    =    message-header
 * </pre>
 * 
 * The extension-header mechanism allows additional entity-header fields to be
 * defined without changing the protocol, but these fields cannot be assumed to
 * be recognizable by the recipient. Unrecognized header fields SHOULD be
 * ignored by the recipient and forwarded by proxies.
 * 
 * @author Gregory Bumgardner
 */
public class RawEntity implements Entity {
    
    /*-- Static Constants ----------------------------------------------------*/

    /**
     * 
     */
    private static final int BUFFER_SIZE = 2048;

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    protected InputStream content;

    protected int contentLength;
    protected String contentType;
    protected String contentLanguage;
    protected String contentEncoding;
    protected String contentBase;
    protected String contentLocation;
    protected String contentDisposition;
    protected Date expires;
    protected Date lastModified;

    protected boolean isConsumed;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public RawEntity() {
        this(null);
    }

    /**
     * 
     * @param content
     */
    public RawEntity(final InputStream content) {
        this(content, -1, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructs a message entity.
     * @param content - The content of the entity.
     * @param contentLength - The content length of the entity.
     * @param contentType - The content type of the entity.
     * @param contentEncoding - The content encoding of the raw entity.
     * @param contentBase
     * @param contentLocation
     * @param contentDisposition
     * @param expires
     * @param lastModified
     */
    protected RawEntity(final InputStream content,
                        final int contentLength,
                        final String contentType,
                        final String contentLanguage,
                        final String contentEncoding,
                        final String contentBase,
                        final String contentLocation,
                        final String contentDisposition,
                        final Date expires,
                        final Date lastModified) {
        
        this.content = content;

        this.contentLength = contentLength;
        this.contentType = contentType;
        this.contentLanguage = contentLanguage;
        this.contentEncoding = contentEncoding;
        this.contentBase = contentBase;
        this.contentLocation = contentLocation;
        this.contentDisposition = contentDisposition;
        this.expires = expires;
        this.lastModified = lastModified;

        this.isConsumed = false;
    }

    /**
     * Constructs a message entity.
     * @param content - The content of the entity.
     * @param contentLength - The content length of the entity.
     * @param contentType - The content type of the entity.
     * @throws RequestException 
     */
    public RawEntity(final InputStream content,
                     final Message message) throws MessageException {
        
        this.content = content;

        if (message.containsHeader(Entity.CONTENT_LENGTH)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_LENGTH);
            this.contentLength = header != null ? Integer.parseInt(header.getValue()) : -1;
        }

        if (message.containsHeader(Entity.CONTENT_TYPE)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_TYPE);
            this.contentType = header != null ? header.getValue() : null;
        }
        else if (this.contentLength > 0) {
            throw RequestException.create(null, StatusCode.BadRequest, "message with non-zero Content-Length is missing Content-Type header");
        }

        if (message.containsHeader(Entity.CONTENT_LANGUAGE)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_LANGUAGE);
            this.contentLanguage = header != null ? header.getValue() : null;
        }

        if (message.containsHeader(Entity.CONTENT_ENCODING)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_ENCODING);
            this.contentEncoding = header != null ? header.getValue() : null;
        }

        if (message.containsHeader(Entity.CONTENT_BASE)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_BASE);
            this.contentBase = header != null ? header.getValue() : null;
        }

        if (message.containsHeader(Entity.CONTENT_LOCATION)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_LOCATION);
            this.contentLocation = header != null ? header.getValue() : null;
        }

        if (message.containsHeader(Entity.CONTENT_DISPOSITION)) {
            MessageHeader header = message.removeHeader(Entity.CONTENT_DISPOSITION);
            this.contentDisposition = header != null ? header.getValue() : null;
        }

        if (message.containsHeader(Entity.EXPIRES)) {
            MessageHeader header = message.removeHeader(Entity.EXPIRES);
            try {
                this.expires = header != null ? DateUtil.toDate(header.getValue()) : null;
            }
            catch (ParseException e) {
                throw RequestException.create(message.getProtocolVersion(),
                                              StatusCode.BadRequest,
                                              "Invalid Expires header value",
                                              e);
            }
        }

        if (message.containsHeader(Entity.LAST_MODIFIED)) {
            MessageHeader header = message.removeHeader(Entity.LAST_MODIFIED);
            try {
                this.expires = header != null ? DateUtil.toDate(header.getValue()) : null;
            }
            catch (ParseException e) {
                throw RequestException.create(message.getProtocolVersion(),
                                              StatusCode.BadRequest,
                                              "Invalid Last-Modified header value",
                                              e);
            }
        }

        this.isConsumed = false;
    }

    public void log(final Logger logger, final Level level) {
        logger.log(level,log.msg("+ logging [" + getClass().getSimpleName() + "]"));
        logState(logger,level);
    }

    private void logState(final Logger logger, final Level level) {
        logger.log(level,log.msg(Entity.CONTENT_LENGTH+": " + this.contentLength));
        logger.log(level,log.msg(Entity.CONTENT_TYPE+": " + this.contentType));
        if (this.contentEncoding != null) {
            logger.log(level,log.msg(Entity.CONTENT_ENCODING+": " + this.contentEncoding));
        }
        if (this.contentLanguage != null) {
            logger.log(level,log.msg(Entity.CONTENT_LANGUAGE+": " + this.contentLanguage));
        }
        if (this.contentBase != null) {
            logger.log(level,log.msg(Entity.CONTENT_BASE+": " + this.contentBase));
        }
        if (this.contentLocation != null) {
            logger.log(level,log.msg(Entity.CONTENT_LOCATION+": " + this.contentLocation));
        }
        if (this.contentDisposition != null) {
            logger.log(level,log.msg(Entity.CONTENT_DISPOSITION+": " + this.contentDisposition));
        }
        if (this.expires != null) {
            logger.log(level,log.msg(Entity.EXPIRES+": " + DateUtil.toString(this.expires)));
        }
        if (this.lastModified != null) {
            logger.log(level,log.msg(Entity.LAST_MODIFIED+": " + DateUtil.toString(this.lastModified)));
        }
    }

    @Override
    public InputStream getContent(final Codec codec) throws IOException {
        prepareContent();
        if (this.contentEncoding == null || codec.getName().equals(this.contentEncoding)) {
            // No transcoding required
            return this.content;
        }
        else {
            // Transcode from raw encoding to desired encoding
            Codec decoder = CodecManager.getManager().getCodec(this.contentEncoding);
            return codec.getInputStream(decoder.getInputStream(this.content));
        }
    }

    /**
     * Returns the content length of the entity.
     * Used to set the value of the Content-Length message header.
     * Returns -1 if no length was specified.
     */
    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    /**
     * Returns the content type of the entity.
     * Used to get the Content-Type message header.
     * Returns <code>null</code> if no content type was specified.
     */
    @Override
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Sets the content type of the entity.
     * Used to set the Content-Type message header.
     * Set to <code>null</code> to disable Content-Type header generation.
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentLanguage() {
        return this.contentLanguage;
    }

    /**
     * Sets the content language of the entity.
     * Used to set the Content-Language message header.
     * Set to <code>null</code> to disable Content-Language header generation.
     */
    public void setContentLanguage(final String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    /**
     * Returns the content encoding of the entity.
     * Used to get the Content-Encoding message header.
     * Returns <code>null</code> if no content encoding was specified.
     */
    @Override
    public String getContentEncoding() {
        return this.contentEncoding;
    }

    /**
     * Sets the content encoding of the entity.
     * Used to set the Content-Encoding message header.
     * The content encoding is also used to select a {@link Codec} to use for decoding/encoding.
     * Set to <code>null</code> to disable Content-Encoder header generation.
     */
    public void setContentEncoding(final String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    @Override
    public String getContentBase() {
        return this.contentBase;
    }

    /**
     * Sets the content base of the entity.
     * Used to set the Content-Base message header.
     * Set to <code>null</code> to disable Content-Base header generation.
     */
    public void setContentBase(final String contentBase) {
        this.contentBase = contentBase;
    }

    @Override
    public String getContentLocation() {
        return this.contentLocation;
    }

    /**
     * Sets the content location of the entity.
     * Used to set the Content-Location message header.
     * Set to <code>null</code> to disable Content-Location header generation.
     */
    public void setContentLocation(final String contentLocation) {
        this.contentLocation = contentLocation;
    }

    @Override
    public String getContentDisposition() {
        return this.contentDisposition;
    }

    /**
     * Sets the content disposition of the entity.
     * Used to set the Content-Disposition message header.
     * Set to <code>null</code> to disable Content-Disposition header generation.
     */
    public void setContentDisposition(final String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    @Override
    public Date getExpires() {
        return this.expires;
    }

    /**
     * Sets the expiration date of the entity.
     * Used to set the Expires message header.
     * Set to <code>null</code> to disable Expires header generation.
     */
    public void setExpires(final Date expires) {
        this.expires = expires;
    }

    @Override
    public Date getLastModified() {
        return this.lastModified;
    }

    /**
     * Sets the last-modified date of the entity.
     * Used to set the Last-Modified message header.
     * Set to <code>null</code> to disable Last-Modified header generation.
     */
    @Override
    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Indicates whether the entity has been consumed (the InputStream has reached EOF).
     */
    public boolean isConsumed() {
        return this.isConsumed;
    }

    /**
     * Called indicate that the entity content should be treated as if it has been consumed.
     * Typically used when an entity is created by a request that initiates an unbounded stream (e.g. tunnel). 
     */
    @Override
    public void ignoreContent() {
        this.isConsumed = true;
    }

    /**
     * Indicates whether the entity is currently streaming (the InputStream has not reached EOF).
     */
    public boolean isStreaming() {
        return !this.isConsumed;
    }

    /**
     * 
     */
    @Override
    public boolean isEntityHeader(final MessageHeader header) {
        String name = header.getName();
        return name.equalsIgnoreCase(Entity.CONTENT_LENGTH)
               || name.equalsIgnoreCase(Entity.CONTENT_TYPE)
               || name.equalsIgnoreCase(Entity.CONTENT_ENCODING)
               || name.equalsIgnoreCase(Entity.CONTENT_LANGUAGE)
               || name.equalsIgnoreCase(Entity.CONTENT_BASE)
               || name.equalsIgnoreCase(Entity.CONTENT_LOCATION)
               || name.equalsIgnoreCase(Entity.CONTENT_DISPOSITION)
               || name.equalsIgnoreCase(Entity.EXPIRES)
               || name.equalsIgnoreCase(Entity.LAST_MODIFIED);
    }
    

    /**
     * Writes this entity to the specified OutputStream.
     * @param outstream - The output stream that will receive the entity.
     * @throws IOException
     */
    @Override
    public void writeTo(final OutputStream outstream, final Codec codec) throws IOException {

        prepareContent();

        if (this.contentEncoding != null) {
            if (codec != null) {
                if (!codec.getName().equals("identity")) {
                    new SimpleMessageHeader(Entity.CONTENT_ENCODING, codec.getName()).writeTo(outstream);
                }
            }
            else {
                if (!this.contentEncoding.equals("identity")) {
                    new SimpleMessageHeader(Entity.CONTENT_ENCODING, this.contentEncoding).writeTo(outstream);
                }
            }
        }

        if (this.contentLanguage != null) {
            new SimpleMessageHeader(Entity.CONTENT_LANGUAGE, this.contentLanguage).writeTo(outstream);
        }

        if (this.contentBase != null) {
            new SimpleMessageHeader(Entity.CONTENT_BASE, this.contentBase).writeTo(outstream);
        }

        if (this.contentLocation != null) {
            new SimpleMessageHeader(Entity.CONTENT_LOCATION, this.contentLocation).writeTo(outstream);
        }

        if (this.contentDisposition != null) {
            new SimpleMessageHeader(Entity.CONTENT_DISPOSITION, this.contentDisposition).writeTo(outstream);
        }

        if (this.expires != null) {
            new DateHeader(Entity.EXPIRES, this.expires).writeTo(outstream);
        }

        if (this.lastModified != null) {
            new DateHeader(Entity.LAST_MODIFIED, this.lastModified).writeTo(outstream);
        }

        if (this.contentLength >= 0) {
            new SimpleMessageHeader(Entity.CONTENT_TYPE, this.contentType).writeTo(outstream);
            new SimpleMessageHeader(Entity.CONTENT_LENGTH, String.valueOf(this.contentLength)).writeTo(outstream);
        }

        outstream.write('\r');
        outstream.write('\n');

        InputStream instream;
        if (codec == null || codec.getName().equals(this.contentEncoding)) {
            // No transcoding required
            instream = this.content;
        }
        else {
            // Transcoding required
            instream = getContent(codec);
        }

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

    protected void prepareContent() {
        
    }

    /**
     * Consumes (reads) the remaining entity content.
     * @throws IOException
     */
    @Override
    public void consumeContent() throws IOException {
        // TODO: Handle streaming entity
        if (!this.isConsumed && this.contentLength > 0) {
            for (int i=0; i < this.contentLength; i++) {
                this.content.read();
            }
        }
    }


}
