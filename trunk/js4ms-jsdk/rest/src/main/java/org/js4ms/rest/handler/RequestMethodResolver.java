package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RequestMethodResolver.java [org.js4ms.jsdk:rest]
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

import org.js4ms.common.util.logging.Log;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Method;
import org.js4ms.rest.message.Request;




public final class RequestMethodResolver implements RequestHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(RequestMethodResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    private final HashMap<Method, RequestHandlerResolver> resolvers = new HashMap<Method,RequestHandlerResolver>();

    public RequestMethodResolver() {
        
    }

    public void put(final Method method, final RequestHandler handler) {
        this.resolvers.put(method, new RequestHandlerResolver() {

            @Override
            public RequestHandler getHandler(Request request) {
                return handler;
            }
            
        });
    }

    public void put(final Method method, final RequestHandlerResolver resolver) {
        this.resolvers.put(method, resolver);
    }

    public void remove(final Method method) {
        this.resolvers.remove(method);
    }

    @Override
    public RequestHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        Method method = request.getRequestLine().getMethod();
        
        RequestHandlerResolver resolver = this.resolvers.get(method);
        if (resolver != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("found handler resolver for "+method.getName()+" method"));
            }
            return resolver.getHandler(request);
        }
        return null;
    }

}
