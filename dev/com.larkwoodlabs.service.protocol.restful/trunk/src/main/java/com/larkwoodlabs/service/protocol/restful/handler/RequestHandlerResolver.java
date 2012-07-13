package com.larkwoodlabs.service.protocol.restful.handler;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;

public interface RequestHandlerResolver {
    RequestHandler getHandler(Request request) throws RequestException;
}
