package com.larkwoodlabs.service.protocol.text.handler;

import com.larkwoodlabs.service.protocol.text.message.Response;

public class ResponseDispatcher implements ResponseHandler {

    final ResponseHandlerResolver resolver;

    public ResponseDispatcher(final ResponseHandlerResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handleResponse(Response response) {
        ResponseHandler handler = this.resolver.getHandler(response);
        if (handler != null) {
            handler.handleResponse(response);
        }
    }

}
