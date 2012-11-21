package net.js4ms.service.protocol.rest.handler;

import net.js4ms.service.protocol.rest.RequestException;
import net.js4ms.service.protocol.rest.message.Request;

public interface RequestHandlerResolver {
    RequestHandler getHandler(Request request) throws RequestException;
}
