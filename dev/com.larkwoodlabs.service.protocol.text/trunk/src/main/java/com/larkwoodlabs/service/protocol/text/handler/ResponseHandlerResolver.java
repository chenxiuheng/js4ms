package com.larkwoodlabs.service.protocol.text.handler;

import com.larkwoodlabs.service.protocol.text.message.Response;

public interface ResponseHandlerResolver {
    ResponseHandler getHandler(Response response);
}
