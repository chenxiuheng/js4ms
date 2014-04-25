package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransactionDispatcher.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;



public class TransactionDispatcher implements TransactionHandler {

    final TransactionHandlerResolver resolver;

    public TransactionDispatcher(final TransactionHandlerResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        TransactionHandler handler;
        try {
            handler = this.resolver.getHandler(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }
        if (handler != null) {
            return handler.handleTransaction(request, response);
        }
        return false;
    }

}
