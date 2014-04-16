package org.js4ms.service.protocol.rest.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.message.Request;




/**
 * Examples:
 * <pre>
 * *
 * /*
 * /foo/*
 * /foo/bar
 * </pre>
 * 
 * @author gbumgard
 */
public final class TransactionUriPathResolver implements TransactionHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(TransactionUriPathResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    /**
     * 
     */
    final HashMap<String,TransactionHandlerResolver> resolvers = new HashMap<String, TransactionHandlerResolver>();

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public TransactionUriPathResolver() {
    }

    /**
     * Registers a handler for the specified URI pattern.
     * @param pattern - A String containing an absolute URI or portion thereof.
     * @param handler
     */
    public void put(final String pattern, final TransactionHandler handler) {
        put(pattern, new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                return handler;
            }
        });
    }

    /**
     * Registers a handler resolver for the specified URI pattern.
     * @param pattern - A String containing an absolute URI or portion thereof.
     * @param resolver
     */
    public void put(final String pattern, final TransactionHandlerResolver resolver) {
        this.resolvers.put(pattern, resolver);
    }

    public void remove(final String pattern) {
        this.resolvers.remove(pattern);
    }

    @Override
    public TransactionHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        String path = request.getRequestLine().getUri().getPath();

        String bestMatchPattern = "";
        TransactionHandlerResolver bestMatchResolver = null;
        for (Map.Entry<String,TransactionHandlerResolver> entry : this.resolvers.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.equals(path)) {
                bestMatchPattern = pattern;
                bestMatchResolver = entry.getValue();
                break;
            }
            else {
                if (pattern.equals("*") ||
                    (pattern.startsWith("*") && path.endsWith(pattern.substring(1))) ||
                    (pattern.endsWith("*") && path.startsWith(pattern.substring(0,pattern.length()-1)))) {
                    if (pattern.length() > bestMatchPattern.length()) { 
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(log.msg("found candidate handler resolver for URI path '"+path+"' using pattern '"+pattern+"'"));
                        }
                        bestMatchPattern = pattern;
                        bestMatchResolver = entry.getValue();
                    }
                }
            }
        }
        if (bestMatchResolver != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("found handler resolver for URI path '"+path+"' using pattern '"+bestMatchPattern+"'"));
            }
            return bestMatchResolver.getHandler(request);
        }

        return null;
    }

}
