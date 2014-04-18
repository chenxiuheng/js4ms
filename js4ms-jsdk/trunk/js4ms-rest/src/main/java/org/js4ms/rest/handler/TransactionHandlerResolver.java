package org.js4ms.rest.handler;

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;

public interface TransactionHandlerResolver {
    TransactionHandler getHandler(Request request) throws RequestException;
}
