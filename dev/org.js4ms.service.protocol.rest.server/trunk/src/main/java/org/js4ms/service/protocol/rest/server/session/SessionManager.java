package org.js4ms.service.protocol.rest.server.session;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Log;

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
