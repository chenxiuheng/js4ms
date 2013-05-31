package org.js4ms.service.protocol.rest.handler;

import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.message.Request;

public interface RequestHandlerResolver {
    RequestHandler getHandler(Request request) throws RequestException;
}
