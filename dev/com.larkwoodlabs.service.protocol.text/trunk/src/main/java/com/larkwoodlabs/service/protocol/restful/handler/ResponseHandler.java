package com.larkwoodlabs.service.protocol.restful.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.message.Response;

public interface ResponseHandler {
    void handleResponse(Response response) throws IOException;
}
