package org.js4ms.rest.handler;

import java.io.IOException;

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;



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
