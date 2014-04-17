package org.js4ms.service.protocol.rest.handler;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.service.protocol.rest.common.RequestException;
import org.js4ms.service.protocol.rest.message.ProtocolName;
import org.js4ms.service.protocol.rest.message.Request;




/**
 * A request handler resolver that uses the protocol name carried by a {@link Request}
 * to locate a handler for that protocol.
 *
 * @author gbumgard
 */
public final class RequestProtocolResolver implements RequestHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(RequestProtocolResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    private final HashMap<String, RequestHandlerResolver> resolvers = new HashMap<String,RequestHandlerResolver>();


    /*-- Member Functions  ----------------------------------------------------*/

    /**
     * 
     */
    public RequestProtocolResolver() {
        
    }

    /**
     * Registers a handler for the specified protocol name.
     * @param protocolName - A {@link ProtocolName}.
     * @param handler
     */
    public void put(final ProtocolName protocolName, final RequestHandler handler) {
        put(protocolName.getName(), handler);
    }

    /**
     * Registers a handler resolver for the specified protocol name.
     * @param protocolName - A {@link ProtocolName}.
     * @param resolver
     */
    public void put(final ProtocolName protocolName, final RequestHandlerResolver resolver) {
        put(protocolName.getName(), resolver);
    }

    /**
     * Registers a handler for the specified protocol name.
     * @param protocolName - A String containing a protocol name.
     * @param handler
     */
    public void put(final String protocolName, final RequestHandler handler) {
        this.resolvers.put(protocolName, new RequestHandlerResolver() {
            @Override
            public RequestHandler getHandler(Request request) {
                return handler;
            }
        });
    }

    /**
     * Registers a handler resolver for the specified protocol name.
     * @param protocolName - A String containing a protocol name.
     * @param resolver
     */
    public void put(final String protocolName, final RequestHandlerResolver resolver) {
        this.resolvers.put(protocolName, resolver);
    }

    public void remove(final ProtocolName protocolName) {
        remove(protocolName.getName());
    }

    public void remove(final String protocolName) {
        this.resolvers.remove(protocolName);
    }

    @Override
    public RequestHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        String protocolName = request.getRequestLine().getProtocolVersion().getProtocolName().getName();
        
        RequestHandlerResolver resolver = this.resolvers.get(protocolName);
        if (resolver != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("found handler resolver for "+protocolName+" protocol"));
            }
            return resolver.getHandler(request);
        }
        return null;
    }

}
