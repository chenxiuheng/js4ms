package com.larkwoodlabs.service.protocol.rest.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rest.message.Response;

public interface ResponseHandler {
    void handleResponse(Response response) throws IOException;
}
