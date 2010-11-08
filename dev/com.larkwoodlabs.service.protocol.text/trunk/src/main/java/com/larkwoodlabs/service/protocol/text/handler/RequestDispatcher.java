package com.larkwoodlabs.service.protocol.text.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.message.Request;

public class RequestDispatcher implements RequestHandler {

    final RequestHandlerResolver resolver;

    public RequestDispatcher(final RequestHandlerResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handleRequest(Request request) throws RequestException, IOException {
        RequestHandler handler = this.resolver.getHandler(request);
        if (handler != null) {
            handler.handleRequest(request);
        }
    }

}
