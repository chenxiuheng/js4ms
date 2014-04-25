package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageHeaderFactory.java [org.js4ms.jsdk:rest]
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


/**
 * Interface exposed by objects that construct
 * {@link MessageHeader} objects given a name and value.
 *
 * @author Greg Bumgardner (gbumgard)
 */
public interface MessageHeaderFactory {

    /**
     * Returns the header name associated with the message header type constructed by this factory.
     * @return The message header name.
     */
    public String getHeaderName();

    /**
     * Constructs a {@link MessageHeader} object.
     * @param value - The header value.
     * @throws IllegalArgumentException
     *         If the specified value cannot be used to construct a message header of the type identified by the name. 
     * @return A new MessageHeader object.
     */
    public MessageHeader construct(final String value) throws IllegalArgumentException;

}
