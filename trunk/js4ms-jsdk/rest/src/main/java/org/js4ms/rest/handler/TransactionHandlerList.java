package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransactionHandlerList.java [org.js4ms.jsdk:rest]
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
import java.util.Iterator;
import java.util.LinkedList;

import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;



public class TransactionHandlerList implements TransactionHandler {

    private final LinkedList<TransactionHandler> handlers = new LinkedList<TransactionHandler>();

    public TransactionHandlerList() {
    }

    public void addHandler(TransactionHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(TransactionHandler handler) {
        this.handlers.remove(handler);
    }

    public void removeHandler(Class<?> handlerClass) {
        Iterator<TransactionHandler> iter = this.handlers.iterator();
        while (iter.hasNext()) {
            if (iter.next().getClass().equals(handlerClass)) {
                iter.remove();
            }
        }
    }

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        for (TransactionHandler handler : this.handlers) {
            if (handler.handleTransaction(request, response)) return true;
        }
        return false;
    }

}
