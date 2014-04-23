package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransactionHandler.java [org.js4ms.jsdk:rest]
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



public interface TransactionHandler {

    /**
     * 
     * @param request - The {@link Request} message sent from the client to the server to initiate the transaction.
     * @param response - The {@link Response} message that will be sent from the server back to the client.
     * @return 
     * <li><code>true</code>
     * to indicate that the handler has changed the response status code and transaction handling should be terminated.
     * <li><code>false</code>
     * to indicate that the handler has not changed the response status code and transaction handling may continue. 
     * @throws RequestException
     * @throws IOException
     */
    boolean handleTransaction(Request request, Response response) throws IOException;
}
