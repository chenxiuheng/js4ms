package org.js4ms.rest.entity;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Entity.java [org.js4ms.jsdk:rest]
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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.rest.message.MessageHeader;




public interface Entity {

    public static final String CONTENT_BASE = "Content-Base";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LANGUAGE = "Content-Language";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_LOCATION = "Content-Location";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String EXPIRES = "Expires";
    public static final String LAST_MODIFIED = "Last-Modified";

    InputStream getContent(final Codec codec) throws IOException;
    String getContentBase();
    String getContentDisposition();
    String getContentEncoding();
    String getContentLanguage();
    int getContentLength();
    String getContentLocation();
    String getContentType();

    Date getExpires();
    Date getLastModified();

    void setExpires(final Date date);
    void setLastModified(final Date date);

    boolean isEntityHeader(final MessageHeader header);

    /**
     * Writes this entity to the specified OutputStream.
     * @param outstream - The output stream that will receive the entity.
     * @throws IOException
     */
    public void writeTo(final OutputStream outstream, final Codec codec) throws IOException;

    public void consumeContent() throws IOException;

    public boolean isConsumed();

    public void ignoreContent();

    public void log(final Logger log, final Level level);
}
