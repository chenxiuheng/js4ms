package org.js4ms.service.protocol.rest.handler;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.message.ProtocolName;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.util.logging.java.Log;




public class TransactionProtocolResolver implements TransactionHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(TransactionProtocolResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    private final HashMap<String, TransactionHandlerResolver> resolvers = new HashMap<String,TransactionHandlerResolver>();


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public TransactionProtocolResolver() {
    }

    public void put(final ProtocolName protocolName, final TransactionHandler handler) {
        put(protocolName.getName(), handler);
    }

    public void put(final ProtocolName protocolName, final TransactionHandlerResolver resolver) {
        put(protocolName.getName(), resolver);
    }

    public void put(final String protocolName, final TransactionHandler handler) {
        put(protocolName, new TransactionHandlerResolver() {

            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                return handler;
            }
            
        });
    }

    public void put(final String protocolName, final TransactionHandlerResolver resolver) {
        this.resolvers.put(protocolName, resolver);
    }

    public void remove(final ProtocolName protocolName) {
        remove(protocolName.getName());
    }

    public void remove(final String protocolName) {
        this.resolvers.remove(protocolName);
    }

    @Override
    public TransactionHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        String protocolName = request.getRequestLine().getProtocolVersion().getProtocolName().getName();
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("attempting to locate handler resolver for "+protocolName+" protocol"));
        }

        TransactionHandlerResolver resolver = this.resolvers.get(protocolName);
        if (resolver != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("found handler resolver for "+protocolName+" protocol"));
            }
            return resolver.getHandler(request);
        }
        return null;
    }

}
