package org.js4ms.service.protocol.rest.server.session;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.logging.java.Log;




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
