package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ResponseHandlerList.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.message.Response;




public class ResponseHandlerList implements ResponseHandler {

    private final LinkedList<ResponseHandler> handlers = new LinkedList<ResponseHandler>();

    public ResponseHandlerList() {
    }

    public void addHandler(ResponseHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(ResponseHandler handler) {
        this.handlers.remove(handler);
    }

    public void removeHandler(Class<?> handlerClass) {
        Iterator<ResponseHandler> iter = this.handlers.iterator();
        while (iter.hasNext()) {
            if (iter.next().getClass().equals(handlerClass)) {
                iter.remove();
            }
        }
    }

    @Override
    public void handleResponse(Response response) throws IOException {
        for (ResponseHandler handler : this.handlers) {
            handler.handleResponse(response);
        }
    }

}
