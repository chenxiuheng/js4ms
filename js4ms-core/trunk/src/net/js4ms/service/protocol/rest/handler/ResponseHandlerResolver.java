package net.js4ms.service.protocol.rest.handler;

import net.js4ms.service.protocol.rest.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}
