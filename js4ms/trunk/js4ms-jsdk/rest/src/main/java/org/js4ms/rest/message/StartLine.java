package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * StartLine.java [org.js4ms.jsdk:rest]
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



public abstract class StartLine {
    
    /*-- Member Variables ----------------------------------------------------*/

    protected ProtocolVersion protocolVersion;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a message start line for the specified protocol version.
     * @param protocolVersion - The protocol version for the request.
     */
    protected StartLine(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    /**
     * Returns the {@link ProtocolVersion} of this start line.
     * @return The current value of the {@link ProtocolVersion} property.
     */
    public final ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }
    
    /**
     * Sets the {@link ProtocolVersion} of this start line.
     * @param protocolVersion - The new protocol version.
     */
    public final void setProtocolVersion(final ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public abstract void writeTo(final OutputStream os) throws IOException;

}
