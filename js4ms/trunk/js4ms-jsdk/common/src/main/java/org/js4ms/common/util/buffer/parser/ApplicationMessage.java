package org.js4ms.common.util.buffer.parser;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ApplicationMessage.java [org.js4ms.jsdk:common]
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

import java.nio.ByteBuffer;

import org.js4ms.common.util.logging.Loggable;



/**
 * Interface implemented by various application-protocol classes.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public interface ApplicationMessage
                extends Loggable {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * Top-level parser interface for application message parsers.
     */
    public static interface Parser
                    extends BufferParser<ApplicationMessage> {

    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Returns the byte-length of the message header.
     * For fixed-size messages, this is often the same value as the returned from
     * {@link #getTotalLength()}.
     */
    public int getHeaderLength();

    /**
     * Returns the total byte-length of the message including the message header.
     */
    public int getTotalLength();

    /**
     * Writes the message to a byte buffer.
     * This method will advance the ByteBuffer position by an amount equal to
     * the total length of the message.
     * 
     * @param buffer
     *            The destination ByteBuffer.
     */
    public void writeTo(ByteBuffer buffer);

}
