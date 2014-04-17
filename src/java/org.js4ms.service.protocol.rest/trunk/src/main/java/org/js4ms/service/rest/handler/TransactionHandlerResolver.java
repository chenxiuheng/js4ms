package org.js4ms.service.rest.handler;

import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.message.Request;

public interface TransactionHandlerResolver {
    TransactionHandler getHandler(Request request) throws RequestException;
}
