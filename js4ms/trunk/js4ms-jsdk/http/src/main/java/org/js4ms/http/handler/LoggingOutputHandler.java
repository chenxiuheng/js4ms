package org.js4ms.http.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * LoggingOutputHandler.java [org.js4ms.jsdk:http]
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
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

import org.js4ms.common.util.logging.JsonLogFormatter;
import org.js4ms.common.util.logging.LogFormatter;
import org.js4ms.http.message.HttpHeaderName;
import org.js4ms.http.message.HttpStatusCode;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.header.SimpleMessageHeader;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;




/**
 * An HTTP transaction handler that publishes log records to an open connection.
 * Used to stream log records to a client in response to an HTTP request.
 * 
 * Parameters in the request URI query string are used to control the  output format for the log records.<p>
 * The 'output' query string parameter is used to specify the output format.
 * The choices are 'text', 'xml', 'json', 'jsonp'.
 * The default format is 'text'.
 * If the output format is 'jsonp', then the URI query string must also include
 * a 'callback' parameter that gives the name of a function to use in the
 * the JSONP callback.
 * 
 * @see {@link LoggingConfigurationHandler} - provides logging configuration control via HTTP requests.
 * @author Greg Bumgardner (gbumgard)
 */
public class LoggingOutputHandler implements TransactionHandler {

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {

        String outputType = null;
        String jsonpCallback = null;

        String query = request.getRequestLine().getUri().getQuery();
        if (query != null && query.length() > 0) {
            String parameters[] = query.split("[&;]");
            for (String parameter : parameters) {
                if (parameter.length() > 0) {
                    String pair[] = parameter.split("=");
                     if (pair[0].equalsIgnoreCase("output")) {
                         if (pair.length == 2) {
                             outputType = pair[1];
                         }
                         else {
                             response.setStatus(HttpStatusCode.BadRequest);
                             response.setEntity(new StringEntity("output parameter value is missing"));
                             return true;
                         }
                     }
                     else if (pair[0].equalsIgnoreCase("callback")) {
                         if (pair.length == 2) {
                             jsonpCallback= pair[1];
                         }
                         else {
                             response.setStatus(HttpStatusCode.BadRequest);
                             response.setEntity(new StringEntity("callback parameter value is missing"));
                             return true;
                         }
                     }
                }
            }
        }

        if (outputType == null) {
            outputType = "text";
        }

        Formatter formatter;
        
        if (outputType.equals("text")) {
            formatter = new LogFormatter();
            response.setHeader(new SimpleMessageHeader(Entity.CONTENT_TYPE,"text/plain"));
        }
        else if (outputType.equals("xml")) {
            formatter = new XMLFormatter();
            response.setHeader(new SimpleMessageHeader(Entity.CONTENT_TYPE,"text/xml"));
        }
        else if (outputType.equals("json")) {
            formatter = new JsonLogFormatter();
            response.setHeader(new SimpleMessageHeader(Entity.CONTENT_TYPE,"application/json"));
        }
        else if (outputType.equals("jsonp")) {
            if (jsonpCallback == null) {
                response.setStatus(HttpStatusCode.BadRequest);
                response.setEntity(new StringEntity("callback parameter value is missing"));
                return true;
            }
            formatter = new JsonLogFormatter(jsonpCallback);
            response.setHeader(new SimpleMessageHeader(Entity.CONTENT_TYPE,"application/javascript"));
        }
        else {
            response.setStatus(HttpStatusCode.BadRequest);
            response.setEntity(new StringEntity("output parameter value is invalid"));
            return true;
        }

        response.setStatus(HttpStatusCode.OK);
        response.setHeader(new SimpleMessageHeader(Entity.CONTENT_LENGTH,String.valueOf(Long.MAX_VALUE)));
        response.setHeader(new SimpleMessageHeader(HttpHeaderName.CONNECTION,"close"));
        response.send();

        final StreamHandler handler = new StreamHandler(response.getConnection().getOutputStream(), formatter) {

            @Override
            public void close() {
                // Do nothing here as we don't want the stream handler closing or flushing the connection output stream.
            }

            @Override
            public void publish(final LogRecord record) {
                super.publish(record);
                // Flush after every record so log messages are sent when they are generated.
                flush();
            }
        };

        handler.setLevel(Level.ALL);

        handler.setErrorManager(new ErrorManager() {
            @Override
            public void error(String msg, Exception ex, int code) {
                Logger.getLogger("").removeHandler(handler);
            }
        });

        Logger.getLogger("").addHandler(handler);

        // Generate a log message so the client will have something to work with.
        Logger.getLogger("").info("Log started");
        return true;
    }

}
