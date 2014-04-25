package org.js4ms.rest.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AbstractTimedSession.java [org.js4ms.jsdk:rest]
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
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;



/**
 * A session that self-terminates if it receives no messages within a specified time period.
 *
 *
 * @author gbumgard
 */
public abstract class AbstractTimedSession extends AbstractSession {

    protected final SessionTimer timer;

    final int sessionTimeout;

    protected AbstractTimedSession(final String identifier,
                                   final SessionManager sessionManager,
                                   final Timer sessionTimer,
                                   int sessionTimeout) {
        super(identifier, sessionManager);
        this.timer = new SessionTimer(sessionTimer, this);
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public final boolean handleTransaction(final Request request,
                                           final Response response) throws IOException {
        // Restart session timer each time a request is received
        this.timer.schedule(this.sessionTimeout);
        return doHandleTransaction(request, response);
    }

    public abstract boolean doHandleTransaction(Request request, Response response) throws IOException;

    @Override
    public void terminate() {

        if (getLogger().isLoggable(Level.FINER)) {
            getLogger().finer(log.entry("terminate"));
        }

        this.timer.cancel();
        super.terminate();
    }

    public void log(final Logger logger) {
        logger.finer(log.msg("session timeout="+sessionTimeout));
    }
}
