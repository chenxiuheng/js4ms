package com.larkwoodlabs.service.protocol.rest.handler;

import com.larkwoodlabs.service.protocol.rest.RequestException;
import com.larkwoodlabs.service.protocol.rest.message.Request;

public interface RequestHandlerResolver {
    RequestHandler getHandler(Request request) throws RequestException;
}
