package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageHeader.java [org.js4ms.jsdk:rest]
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

public interface MessageHeader extends Cloneable {

    public interface Factory {
        public String getHeaderName();
        public MessageHeader construct(final String value);
    }

    public Object clone();

    public String getName();

    public String getValue();

    public void setValue(final String value) throws IllegalArgumentException;

    public void appendHeader(final MessageHeader header) throws IllegalArgumentException;

    public String toString();

    /**
     * Writes the header to the specified OutputStream.
     * @param outstream - The destination OutputStream
     */
    public void writeTo(final OutputStream outstream) throws IOException;
}
