package org.js4ms.service.protocol.rest.handler;

import org.js4ms.service.protocol.rest.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}