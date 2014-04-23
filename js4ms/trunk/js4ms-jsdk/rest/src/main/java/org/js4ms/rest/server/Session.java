package org.js4ms.rest.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Session.java [org.js4ms.jsdk:rest]
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

import org.js4ms.rest.handler.TransactionHandler;

/**
 * An interface exposed by objects that maintain state information over a
 * sequence of client-server transactions.
 * 
 *
 * @author gbumgard
 */
public interface Session extends TransactionHandler {
    
    /**
     * An identifier that can be used to lookup an ongoing session.
     * @return
     */
    String getIdentifier();
    
    /**
     * Terminates the session. This method might be used to indicate
     * that a transaction sequence representing a session has reached its
     * normal end-point, or may also be used to abort an ongoing session.
     */
    void terminate();
}
