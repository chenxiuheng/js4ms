package org.js4ms.rest.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * VerifyAcceptEncodingHeader.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.entity.CodecManager;
import org.js4ms.rest.message.HeaderName;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rest.message.StatusCode;



public class VerifyAcceptEncodingHeader implements TransactionHandler {

    public VerifyAcceptEncodingHeader() {
        
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        if (request.containsHeader(HeaderName.ACCEPT_ENCODING)) {
            String value = request.getHeader(HeaderName.ACCEPT_ENCODING).getValue();
            // wildcard is always acceptable
            if (value.equals("*")) return false;
            String[] encodings = value.split(",[ ]*");
            for (String encoding : encodings) {
                float q = 1;
                String[] params = encoding.split(";");
                String encodingName = params[0];
                if (params.length > 1) {
                    String[] qExpression = params[1].split("=");
                    if (qExpression[0].equals("q") && qExpression.length > 1) {
                        q = Float.parseFloat(qExpression[1]);
                    }
                }
                if (q > 0 && CodecManager.getManager().hasCodec(encodingName)) {
                    // We have a codec for the requested content encoding.
                    return false;
                }
            }
            
            response.setStatus(StatusCode.NotAcceptable);
        }
        return false;
    }

}
