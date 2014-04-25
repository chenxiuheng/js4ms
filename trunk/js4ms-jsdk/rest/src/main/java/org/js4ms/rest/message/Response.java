package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Response.java [org.js4ms.jsdk:rest]
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


import java.util.LinkedHashMap;

import org.js4ms.rest.entity.Entity;
import org.js4ms.server.Connection;



public class Response extends Message {

    /**
     * @param connection - The connection on which the message is to be sent.
     * @param statusLine
     */
    public Response(Connection connection,
                    StatusLine statusLine) {
        super(connection, statusLine);
    }

    /**
     * Constructs a response message with the specified status line, headers, and entity.
     * @param connection - The connection on which the message is to be sent.
     * @param statusLine - A representation of the first line in the response message
     *                     (consisting of the protocol version, status code and reason phrase).
     * @param headers - A collection of response message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Response(final Connection connection,
                    final StatusLine statusLine,
                    final LinkedHashMap<String,MessageHeader> messageHeaders,
                    final Entity entity) {
        super(connection, statusLine, messageHeaders, entity);
    }

    /**
     * Returns a representation of the first line, or message header, for this response.
     */
    public StatusLine getStatusLine() {
        return (StatusLine)this.startLine;
    }

    /**
     * Sets the first line, or message header, for this response.
     */
    public void setStatusLine(StatusLine statusLine) {
        this.startLine = statusLine;
    }

    /**
     * Returns status code of by this response.
     */
    public Status getStatus() {
        return getStatusLine().getStatus();
    }

    /**
     * Sets the status code of this response.
     */
    public void setStatus(Status status) {
        getStatusLine().setStatus(status);
    }
}
