package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransactionMethodResolver.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Method;
import org.js4ms.rest.message.Request;



public class TransactionMethodResolver implements TransactionHandlerResolver {

    private final HashMap<Method, TransactionHandlerResolver> resolvers = new HashMap<Method,TransactionHandlerResolver>();

    public TransactionMethodResolver() {
        
    }

    public void put(final Method method, final TransactionHandler handler) {
        put(method, new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                return handler;
            }
        });
    }

    public void put(final Method method, final TransactionHandlerResolver resolver) {
        this.resolvers.put(method, resolver);
    }

    public void remove(final Method method) {
        this.resolvers.remove(method);
    }

    @Override
    public TransactionHandler getHandler(final Request request) throws RequestException {
        TransactionHandlerResolver resolver = this.resolvers.get(request.getRequestLine().getMethod());
        if (resolver != null) {
            return resolver.getHandler(request);
        }
        return null;
    }

}
