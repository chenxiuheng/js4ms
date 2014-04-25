package org.js4ms.rest.common;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageException.java [org.js4ms.jsdk:rest]
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

import org.js4ms.common.util.logging.Log;
import org.js4ms.rest.message.ProtocolVersion;



public class MessageException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3670722564024392131L;

    protected static final Log slog = new Log(MessageException.class);

    protected final ProtocolVersion protocolVersion;

    protected MessageException(final ProtocolVersion protocolVersion) {
        super();
        this.protocolVersion = protocolVersion;
    }

    public MessageException(final ProtocolVersion protocolVersion,
                            final String message) {
        super(message);
        this.protocolVersion = protocolVersion;
    }

    public MessageException(final ProtocolVersion protocolVersion,
                            final Throwable cause) {
        super(cause);
        this.protocolVersion = protocolVersion;
    }

    public MessageException(final ProtocolVersion protocolVersion,
                            final String message,
                            final Throwable cause) {
        super(message,cause);
        this.protocolVersion = protocolVersion;
    }

    /**
     * Returns the {@link ProtocolVersion} of the message that generated this exception.
     */
    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }


}
