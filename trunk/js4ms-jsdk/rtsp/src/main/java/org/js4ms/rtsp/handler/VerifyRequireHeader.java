package org.js4ms.rtsp.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * VerifyRequireHeader.java [org.js4ms.jsdk:rtsp]
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
import java.util.HashSet;

import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.header.SimpleMessageHeader;
import org.js4ms.rest.message.HeaderName;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rest.message.StatusCode;



public class VerifyRequireHeader implements TransactionHandler {

    private HashSet<String> features = null;

    public VerifyRequireHeader() {
        
    }

    public void addFeature(final String name) {
        if (this.features == null) {
            this.features = new HashSet<String>();
        }
        this.features.add(name);
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        if (request.containsHeader(HeaderName.REQUIRE)) {
            // Indicate that no special features are supported
            MessageHeader header = request.getHeader(HeaderName.REQUIRE);
            if (this.features == null || this.features.isEmpty()) {
                response.setStatus(StatusCode.OptionNotSupported);
                response.setHeader(new SimpleMessageHeader(HeaderName.UNSUPPORTED, header.getValue()));
                return true;
            }
            else {
                // If multiple Require headers were present in the message, they
                // will have been concatenated into a single comma-delimited list by the parser.
                // Split the header value into individual feature names and check each.
                StringBuffer headerValue = new StringBuffer();
                String[] names = header.getValue().split(",[ ]*");
                for (String name : names) {
                    if (!this.features.contains(name)) {
                        if (headerValue.length() > 0) {
                            headerValue.append(",");
                        }
                        headerValue.append(name);
                    }
                }
                if (headerValue.length() != 0) {
                    response.setStatus(StatusCode.OptionNotSupported);
                    response.setHeader(new SimpleMessageHeader(HeaderName.UNSUPPORTED,headerValue.toString()));
                    return true;
                }
            }
        }
        return false;
    }

}
