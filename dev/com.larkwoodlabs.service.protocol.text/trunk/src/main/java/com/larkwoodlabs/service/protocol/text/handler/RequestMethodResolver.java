package com.larkwoodlabs.service.protocol.text.handler;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.message.Method;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.util.logging.Log;

public final class RequestMethodResolver implements RequestHandlerResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(RequestMethodResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    private final HashMap<Method, RequestHandlerResolver> resolvers = new HashMap<Method,RequestHandlerResolver>();

    public RequestMethodResolver() {
        
    }

    public void put(final Method method, final RequestHandler handler) {
        this.resolvers.put(method, new RequestHandlerResolver() {

            @Override
            public RequestHandler getHandler(Request request) {
                return handler;
            }
            
        });
    }

    public void put(final Method method, final RequestHandlerResolver resolver) {
        this.resolvers.put(method, resolver);
    }

    public void remove(final Method method) {
        this.resolvers.remove(method);
    }

    @Override
    public RequestHandler getHandler(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getHandler", request));
        }

        Method method = request.getRequestLine().getMethod();
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("attempting to locate handler resolver for "+method.getName()+" method"));
        }

        RequestHandlerResolver resolver = this.resolvers.get(method);
        if (resolver != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("found handler resolver for "+method.getName()+" method"));
            }
            return resolver.getHandler(request);
        }
        return null;
    }

}
