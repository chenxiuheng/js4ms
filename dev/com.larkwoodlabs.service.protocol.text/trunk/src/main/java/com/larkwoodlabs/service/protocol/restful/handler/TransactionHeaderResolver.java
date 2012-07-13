package com.larkwoodlabs.service.protocol.restful.handler;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.util.logging.Log;

public class TransactionHeaderResolver implements TransactionHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(TransactionHeaderResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    /**
     * 
     */
    protected final HashMap<String,TransactionHandlerResolver> resolvers = new HashMap<String, TransactionHandlerResolver>();

    protected final String headerName;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * @param headerName - The name of the message header that this resolver will evaluate.
     */
    public TransactionHeaderResolver(final String headerName) {
        this.headerName = headerName;
    }

    /**
     * Registers a handler for requests that carry the target header and matching header value.
     * @param headerPattern - A regular expression used to evaluate the header value.
     * @param handler
     */
    public void put(final String headerPattern, final TransactionHandler handler) {
        put(headerPattern, new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                return handler;
            }
        });
    }

    /**
     * Registers a handler resolver for messages that carry the target header and matching header value.
     * @param headerPattern - A regular expression used to evaluate the header value.
     * @param resolver
     */
    public void put(final String headerPattern, final TransactionHandlerResolver resolver) {
        // Construct resolver that checks the header using a precompiled pattern
        final Pattern pattern = Pattern.compile(headerPattern);
        TransactionHandlerResolver patternResolver = new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                if (request.containsHeader(headerName)) {
                    if (pattern.matcher(request.getHeader(headerName).getValue()).matches()) {
                        return resolver.getHandler(request);
                    }
                }
                return null;
            }
        };

        this.resolvers.put(headerPattern, patternResolver);
    }

    /**
     * Unregisters a handler resolver for the specified header.
     * @param headerPattern - The regular expression used to register a resolver.
     */
    public void remove(final String headerPattern) {
        this.resolvers.remove(headerPattern);
    }

    @Override
    public TransactionHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        if (!request.containsHeader(this.headerName)) {
            return null;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("attempting to locate handler resolver for message with header '"+this.headerName+"'"));
        }

        String value = request.getHeader(this.headerName).getValue();

        for (TransactionHandlerResolver resolver : this.resolvers.values()) {
            TransactionHandler handler = resolver.getHandler(request);
            if (handler != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(log.msg("found handler resolver for header='"+this.headerName+"' and value='"+value+"'"));
                }
                return handler;
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("failed to locate handler resolver for header='"+this.headerName+"' and value='"+value+"'"));
        }

        return null;
    }

}
