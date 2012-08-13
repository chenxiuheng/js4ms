package com.larkwoodlabs.service.protocol.rest.server.session;

import java.io.IOException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.service.protocol.rest.message.Request;
import com.larkwoodlabs.service.protocol.rest.message.Response;

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