package com.larkwoodlabs.service.protocol.restful.handler;

import com.larkwoodlabs.service.protocol.restful.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}
