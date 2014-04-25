package org.js4ms.rest.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AbstractSession.java [org.js4ms.jsdk:rest]
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;




public abstract class AbstractSession implements Session {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    protected final String identifier;
    protected final SessionManager sessionManager;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param identifier
     * @param manager
     */
    protected AbstractSession(final String identifier, final SessionManager sessionManager) {
        this.identifier = identifier;
        this.sessionManager = sessionManager;
        this.sessionManager.putSession(this);
    }

    /**
     * 
     */
    @Override
    public final String getIdentifier() {
        return this.identifier;
    }

    /**
     * 
     */
    @Override
    public void terminate() {

        if (getLogger().isLoggable(Level.FINER)) {
            getLogger().finer(log.entry("terminate"));
        }

        this.sessionManager.removeSession(identifier);
    }

    /**
     * 
     * @return
     */
    protected abstract Logger getLogger();

    /**
     * 
     * @param logger
     */
    public void log(final Logger logger) {
        logger.finer(log.msg("+ logging [" + getClass().getSimpleName() + "]"));
    }
}
