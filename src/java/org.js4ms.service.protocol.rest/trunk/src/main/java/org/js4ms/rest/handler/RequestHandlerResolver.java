package org.js4ms.service.rest.handler;

import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.message.Request;

public interface RequestHandlerResolver {
    RequestHandler getHandler(Request request) throws RequestException;
}
