package org.js4ms.http.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * LoggingConfigurationHandler.java [org.js4ms.jsdk:http]
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Logging;
import org.js4ms.http.message.HttpStatusCode;
import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;




/**
 * An HTTP transaction handler that provides control over logger configuration
 * via query string parameters.
 * A client can send requests that set logger levels, load logging configuration
 * properties or reset the logging configuration. Logger settings are included in
 * the response for every successful configuration request.<p>
 * 
 * To list the current logger levels, send a request with a URI path that resolves to
 * a LoggingConfigurationHandler:
 * <pre>
 * http://host/path
 * </pre>
 * To create a logger or set an existing logger level, include a query string parameter
 * that assigns a logging level to a parameter that consists of the logger name followed
 * by a '.level' suffix, e.g.:
 * <pre>
 * http://host/path?org.js4ms.service.level=FINE
 * </pre>
 * To load logging configuration properties include a 'properties' parameter that is assigned
 * a URL for a logging properties file, e.g.:
 * <pre>
 * http://host/path?properties=http://host/logging.properties
 * </pre>
 * To reset the logging configuration to that last loaded by the logging framework,
 * include a 'reset' parameter in the query string:
 * <pre>
 * http://host/path?reset
 * </pre>
 * 
 * @see {@link LoggingOutputHandler} - generates log output stream in response to a request.
 * @author Greg Bumgardner (gbumgard)
 */
public class LoggingConfigurationHandler implements TransactionHandler {

    private HashMap<String,Logger> loggers = new HashMap<String, Logger>();

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        StringBuffer buffer = new StringBuffer();

        String query = request.getRequestLine().getUri().getQuery();
        if (query != null && query.length() > 0) {
            String parameters[] = query.split("[&;]");
            for (String parameter : parameters) {
                if (parameter.length() > 0) {
                    String pair[] = parameter.split("=");
                     if (pair[0].equalsIgnoreCase("properties")) {
                         if (pair.length == 2) {
                            try {
                                try {
                                    Logging.configureLogging(new URI(pair[1]));
                                    buffer.append("logging properties loaded from "+pair[1]+"\n\n");
                                }
                                catch (IOException e) {
                                    response.setStatus(HttpStatusCode.BadRequest);
                                    response.setEntity(new StringEntity("cannot load logging properties from "+pair[1]+" - "+e.getMessage()));
                                    return true;
                                }
                            }
                            catch (URISyntaxException e) {
                                response.setStatus(HttpStatusCode.BadRequest);
                                response.setEntity(new StringEntity("properties parameter value is invalid - "+e.getMessage()));
                                return true;
                            }
                         }
                         else {
                             response.setStatus(HttpStatusCode.BadRequest);
                             response.setEntity(new StringEntity("properties parameter value is missing"));
                             return true;
                         }
                     }
                     else if (pair[0].endsWith(".level")) {
                         String loggerName = pair[0].substring(0,pair[0].lastIndexOf('.'));
                         Level level = null;
                         if (pair.length == 2) {
                             try {
                                 level = Level.parse(pair[1]);
                             }
                             catch (IllegalArgumentException e) {
                                 response.setStatus(HttpStatusCode.BadRequest);
                                 response.setEntity(new StringEntity("logger level parameter value is invalid - "+e.getMessage()));
                                 return true;
                             }
                         }
                         Logger logger;
                         if (this.loggers.containsKey(loggerName)) {
                             logger = this.loggers.get(loggerName);
                         }
                         else {
                             logger = Logger.getLogger(loggerName);
                             this.loggers.put(loggerName,logger);
                         }
                         logger.setLevel(level);
                         buffer.append("logger '"+loggerName+"' level set to "+level+"\n\n");
                     }
                     else if (pair[0].equalsIgnoreCase("reset")) {
                         this.loggers.clear();
                         LogManager.getLogManager().readConfiguration();
                     }
                     else {
                         response.setStatus(HttpStatusCode.BadRequest);
                         response.setEntity(new StringEntity("query parameter '"+pair[0]+"' is not supported"));
                         return true;
                     }
                }
            }
        }

        buffer.append("Intantiated Loggers:\n");

        LinkedList<String> loggers = new LinkedList<String>();
        Enumeration<String> iter = LogManager.getLogManager().getLoggerNames();
        while (iter.hasMoreElements()) {
            String loggerName = iter.nextElement();
            loggers.add(loggerName);
        }

        Collections.sort(loggers);
        for (String loggerName : loggers) {
            Level level = Logger.getLogger(loggerName).getLevel();
            buffer.append(loggerName+"="+(level != null ? level.getName() : "inherited")+"\n");
        }

        response.setStatus(HttpStatusCode.OK);
        response.setEntity(new StringEntity(buffer.toString()));
        return true;
    }

}
