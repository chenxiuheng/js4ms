package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Request.java [org.js4ms.jsdk:rest]
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


import java.net.URI;
import java.util.LinkedHashMap;

import org.js4ms.rest.entity.Entity;
import org.js4ms.server.Connection;



/**
 * A request message.
 *
 * @author Gregory Bumgardner
 */
public class Request extends Message {

    
    /**
     * Constructs a request message for the specified method and URI reference.
     * @param connection - The connection from which the message was received.
     * @param method - The request method.
     * @param uri - A resource or control URI.
     */
    public Request(final Connection connection,
                   final ProtocolVersion protocolVersion,
                   final Method method,
                   final URI uri) {
        this(connection, new RequestLine(method, uri, protocolVersion));
    }

    /**
     * Constructs a request message with the specified request line.
     * @param connection - The connection from which the message was received.
     * @param requestLine - The request line.
     */
    public Request(final Connection connection,
                   final RequestLine requestLine) {
        super(connection, requestLine);
    }

    /**
     * Protected constructor.
     * @param connection - The connection f which the message was received.
     * @param requestLine - A representation of the first line in the request message
     *                      (the message method, URI, and protocol version).
     * @param messageHeaders - A collection of request message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Request(final Connection connection,
                   final RequestLine requestLine,
                   final LinkedHashMap<String,MessageHeader> messageHeaders,
                   final Entity entity) {
        super(connection, requestLine, messageHeaders, entity);
    }

    /**
     * Returns a representation of the first line, or message header, for this request.
     */
    public RequestLine getRequestLine() {
        return (RequestLine)this.startLine;
    }

    /**
     * Sets the first line, or message header, for this request.
     */
    public void setRequestLine(RequestLine requestLine) {
        this.startLine = requestLine;
    }

}
