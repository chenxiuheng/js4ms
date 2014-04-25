package org.js4ms.rtsp.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransferSessionHeader.java [org.js4ms.jsdk:rtsp]
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

import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rtsp.message.RtspHeaderName;



public class TransferSessionHeader implements TransactionHandler {

    public TransferSessionHeader() {
        
    }

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        // Transfer Session header from request to response
        if (request.containsHeader(RtspHeaderName.SESSION)) {
            response.setHeader((MessageHeader)request.getHeader(RtspHeaderName.SESSION).clone());
        }
        return false;
    }

}
