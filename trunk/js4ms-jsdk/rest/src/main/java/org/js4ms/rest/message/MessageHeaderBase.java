package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageHeaderBase.java [org.js4ms.jsdk:rest]
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
import java.io.OutputStream;


public abstract class MessageHeaderBase implements MessageHeader, Cloneable {

    private final String name;
    
    protected MessageHeaderBase(final String name) {
        this.name = name;
    }

    protected MessageHeaderBase(final MessageHeader header) {
        this.name = header.getName();
    }

    public abstract Object clone();

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return getName() + ": " + getValue();
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(toString().getBytes("UTF8"));
        outstream.write('\r');
        outstream.write('\n');
    }
}
