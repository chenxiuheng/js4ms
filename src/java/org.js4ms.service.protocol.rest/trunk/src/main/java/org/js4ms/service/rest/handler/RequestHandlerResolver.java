package org.js4ms.service.rest.handler;

import org.js4ms.service.protocol.rest.common.RequestException;
import org.js4ms.service.protocol.rest.message.Request;

public interface RequestHandlerResolver {
    RequestHandler getHandler(Request request) throws RequestException;
}
