package org.js4ms.rest.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * SessionManager.java [org.js4ms.jsdk:rest]
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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * Manages a collection of active sessions.
 *
 * @author gbumgard
 */
public class SessionManager {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(SessionManager.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    /**
     * 
     */
    protected final HashMap<String,Session> sessions = new HashMap<String,Session>();

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    public SessionManager() {
        
    }

    /**
     * 
     * @param session
     */
    public void putSession(final Session session) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("putSession", session));
        }

        this.sessions.put(session.getIdentifier(), session);
    }

    /**
     * 
     * @param identifier
     * @return
     */
    public Session getSession(final String identifier) {
        return this.sessions.get(identifier);
    }

    /**
     * 
     * @param identifier
     */
    public void removeSession(final String identifier) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("removeSession", identifier));
        }

        this.sessions.remove(identifier);
    }

    /**
     * 
     */
    public void terminateSessions() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("terminateSessions"));
        }

        synchronized (this.sessions) {
            for (Session session : this.sessions.values()) {
                session.terminate();
            }

            sessions.clear();
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.exit("terminateSessions"));
        }

    }
}
