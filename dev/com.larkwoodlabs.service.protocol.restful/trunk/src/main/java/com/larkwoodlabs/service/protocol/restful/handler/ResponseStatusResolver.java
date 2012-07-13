package com.larkwoodlabs.service.protocol.restful.handler;

import java.util.HashMap;

import com.larkwoodlabs.service.protocol.restful.message.Response;

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
