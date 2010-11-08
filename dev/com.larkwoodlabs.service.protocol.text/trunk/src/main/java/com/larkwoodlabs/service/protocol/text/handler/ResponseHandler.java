package com.larkwoodlabs.service.protocol.text.handler;

import com.larkwoodlabs.service.protocol.text.message.Response;

public interface ResponseHandler {
    void handleResponse(Response response);
}
