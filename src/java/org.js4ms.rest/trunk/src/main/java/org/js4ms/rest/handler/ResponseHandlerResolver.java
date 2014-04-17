package org.js4ms.rest.handler;

import org.js4ms.rest.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}
