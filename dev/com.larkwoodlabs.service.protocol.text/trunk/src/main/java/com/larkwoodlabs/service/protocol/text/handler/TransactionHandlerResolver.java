package com.larkwoodlabs.service.protocol.text.handler;

import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.message.Request;

public interface TransactionHandlerResolver {
    TransactionHandler getHandler(Request request) throws RequestException;
}
