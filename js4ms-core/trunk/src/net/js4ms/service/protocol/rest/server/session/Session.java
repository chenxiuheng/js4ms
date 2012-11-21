package net.js4ms.service.protocol.rest.server.session;

import net.js4ms.service.protocol.rest.handler.TransactionHandler;

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
