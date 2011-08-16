package com.larkwoodlabs.service.protocol.text.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.text.message.Response;

public interface ResponseHandler {
    void handleResponse(Response response) throws IOException;
}
