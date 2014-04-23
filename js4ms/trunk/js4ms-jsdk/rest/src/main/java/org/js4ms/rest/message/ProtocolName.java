package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ProtocolName.java [org.js4ms.jsdk:rest]
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
 * A message protocol identifier (e.g. HTTP, RTSP, etc.).
 *
 * @author Gregory Bumgardner
 */
public final class ProtocolName {
    
    String name;
    
    public ProtocolName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public boolean equals(Object object) {
        return (object instanceof ProtocolName) && this.name.equals(((ProtocolName)object).name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
