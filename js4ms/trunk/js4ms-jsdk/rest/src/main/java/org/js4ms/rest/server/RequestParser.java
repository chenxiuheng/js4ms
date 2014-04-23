package org.js4ms.rest.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RequestParser.java [org.js4ms.jsdk:rest]
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
import java.util.LinkedHashMap;

import org.js4ms.common.exception.ParseException;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.handler.RequestHandler;
import org.js4ms.rest.message.Message;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.MessageHeaderParser;
import org.js4ms.rest.message.MessageParser;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.RequestLine;
import org.js4ms.rest.message.StartLine;
import org.js4ms.server.Connection;




public class RequestParser extends MessageParser {


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    final RequestHandler handler;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * @param headerParser - A message header parser to use during message parsing.
     * @param connection
     * @param handler
     */
    public RequestParser(final MessageHeaderParser headerParser, final RequestHandler handler) {
        super(headerParser);
        this.handler = handler;
    }

    @Override
    protected StartLine doParseStartLine(final String line) throws ParseException {
        return RequestLine.parse(line);
    }

    @Override
    protected Message doConstructMessage(Connection connection,
                                         StartLine startLine,
                                         LinkedHashMap<String, MessageHeader> headers,
                                         Entity entity) {
        return new Request(connection, (RequestLine)startLine, headers, entity);
    }

    @Override
    protected void doHandleMessage(Message message) throws RequestException, IOException {
        this.handler.handleRequest((Request)message);
    }

}
