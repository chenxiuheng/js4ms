package com.larkwoodlabs.service.protocol.rest.handler;

import com.larkwoodlabs.service.protocol.rest.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}
