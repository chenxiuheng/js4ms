package org.js4ms.rest.header;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ContentTypeHeader.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.MediaType;
import org.js4ms.rest.message.FormattedMessageHeader;
import org.js4ms.rest.message.MessageHeader;

public class ContentTypeHeader
                extends FormattedMessageHeader {

    private MediaType mediaType;

    public ContentTypeHeader(final MediaType mediaType) {
        super(Entity.CONTENT_TYPE);
        this.mediaType = mediaType;
    }

    public ContentTypeHeader(final ContentTypeHeader header) {
        super(header);
        this.mediaType = header.mediaType;
    }

    @Override
    public void appendHeader(MessageHeader header) throws IllegalArgumentException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void parse(String value) {
        this.mediaType = MediaType.parse(value);
    }

    @Override
    protected String format() {
        return this.mediaType.toString();
    }

    @Override
    public Object clone() {
        return new ContentTypeHeader(this);
    }

}
