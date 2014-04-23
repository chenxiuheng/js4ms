package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransactionHeaderResolver.java [org.js4ms.jsdk:rest]
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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.js4ms.common.util.logging.Log;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;




public class TransactionHeaderResolver implements TransactionHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(TransactionHeaderResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    /**
     * 
     */
    protected final HashMap<String,TransactionHandlerResolver> resolvers = new HashMap<String, TransactionHandlerResolver>();

    protected final String headerName;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * @param headerName - The name of the message header that this resolver will evaluate.
     */
    public TransactionHeaderResolver(final String headerName) {
        this.headerName = headerName;
    }

    /**
     * Registers a handler for requests that carry the target header and matching header value.
     * @param headerPattern - A regular expression used to evaluate the header value.
     * @param handler
     */
    public void put(final String headerPattern, final TransactionHandler handler) {
        put(headerPattern, new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                return handler;
            }
        });
    }

    /**
     * Registers a handler resolver for messages that carry the target header and matching header value.
     * @param headerPattern - A regular expression used to evaluate the header value.
     * @param resolver
     */
    public void put(final String headerPattern, final TransactionHandlerResolver resolver) {
        // Construct resolver that checks the header using a precompiled pattern
        final Pattern pattern = Pattern.compile(headerPattern);
        TransactionHandlerResolver patternResolver = new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                if (request.containsHeader(headerName)) {
                    if (pattern.matcher(request.getHeader(headerName).getValue()).matches()) {
                        return resolver.getHandler(request);
                    }
                }
                return null;
            }
        };

        this.resolvers.put(headerPattern, patternResolver);
    }

    /**
     * Unregisters a handler resolver for the specified header.
     * @param headerPattern - The regular expression used to register a resolver.
     */
    public void remove(final String headerPattern) {
        this.resolvers.remove(headerPattern);
    }

    @Override
    public TransactionHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        if (!request.containsHeader(this.headerName)) {
            return null;
        }

        String value = request.getHeader(this.headerName).getValue();

        for (TransactionHandlerResolver resolver : this.resolvers.values()) {
            TransactionHandler handler = resolver.getHandler(request);
            if (handler != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(log.msg("found handler resolver for header='"+this.headerName+"' and value='"+value+"'"));
                }
                return handler;
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("failed to locate handler resolver for header='"+this.headerName+"' and value='"+value+"'"));
        }

        return null;
    }

}
