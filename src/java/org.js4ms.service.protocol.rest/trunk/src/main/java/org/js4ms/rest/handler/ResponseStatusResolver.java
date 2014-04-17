package org.js4ms.service.rest.handler;

import java.util.HashMap;

import org.js4ms.service.rest.message.Response;



public class ResponseStatusResolver implements ResponseHandlerResolver {

    protected final HashMap<Integer,ResponseHandler> handlers = new HashMap<Integer,ResponseHandler>();

    public ResponseStatusResolver() {
        
    }

    public void put(int statusCode, final ResponseHandler handler) {
        handlers.put(statusCode, handler);
    }

    @Override
    public ResponseHandler getHandler(Response response) {
        return handlers.get(response.getStatus().getCode());
    }

}
