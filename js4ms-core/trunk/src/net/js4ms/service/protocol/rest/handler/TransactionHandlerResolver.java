package net.js4ms.service.protocol.rest.handler;

import net.js4ms.service.protocol.rest.RequestException;
import net.js4ms.service.protocol.rest.message.Request;

public interface TransactionHandlerResolver {
    TransactionHandler getHandler(Request request) throws RequestException;
}
