package org.js4ms.service.rest.handler;

import java.util.HashMap;

import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.message.Method;
import org.js4ms.service.rest.message.Request;



public class TransactionMethodResolver implements TransactionHandlerResolver {

    private final HashMap<Method, TransactionHandlerResolver> resolvers = new HashMap<Method,TransactionHandlerResolver>();

    public TransactionMethodResolver() {
        
    }

    public void put(final Method method, final TransactionHandler handler) {
        put(method, new TransactionHandlerResolver() {
            @Override
            public TransactionHandler getHandler(Request request) throws RequestException {
                return handler;
            }
        });
    }

    public void put(final Method method, final TransactionHandlerResolver resolver) {
        this.resolvers.put(method, resolver);
    }

    public void remove(final Method method) {
        this.resolvers.remove(method);
    }

    @Override
    public TransactionHandler getHandler(final Request request) throws RequestException {
        TransactionHandlerResolver resolver = this.resolvers.get(request.getRequestLine().getMethod());
        if (resolver != null) {
            return resolver.getHandler(request);
        }
        return null;
    }

}
