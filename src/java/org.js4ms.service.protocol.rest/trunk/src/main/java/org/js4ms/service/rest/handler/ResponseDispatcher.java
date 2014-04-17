package org.js4ms.service.rest.handler;

import java.io.IOException;

import org.js4ms.service.protocol.rest.message.Response;



public class ResponseDispatcher implements ResponseHandler {

    final ResponseHandlerResolver resolver;

    public ResponseDispatcher(final ResponseHandlerResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handleResponse(Response response) throws IOException {
        ResponseHandler handler = this.resolver.getHandler(response);
        if (handler != null) {
            handler.handleResponse(response);
        }
    }

}
