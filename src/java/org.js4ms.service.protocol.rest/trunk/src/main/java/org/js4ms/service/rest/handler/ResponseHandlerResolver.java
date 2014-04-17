package org.js4ms.service.rest.handler;

import org.js4ms.service.rest.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}
