package org.js4ms.rest.header;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * SimpleMessageHeader.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.MessageHeaderBase;


public class SimpleMessageHeader extends MessageHeaderBase {

    private String value;

    public SimpleMessageHeader(final String name) {
        super(name);
    }

    public SimpleMessageHeader(final String name, final String value) {
        super(name);
        this.value = value;
    }

    public SimpleMessageHeader(final SimpleMessageHeader header) {
        super(header);
        this.value = header.value;
    }

    @Override
    public Object clone() {
        return new SimpleMessageHeader(this);
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public void appendHeader(final MessageHeader header) {
        if (this.value.length() > 0) {
            this.value += "," + header.getValue();
        }
        else {
            this.value = header.getValue();
        }
    }

}
