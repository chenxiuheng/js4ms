package com.larkwoodlabs.service.protocol.rest.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rest.RequestException;
import com.larkwoodlabs.service.protocol.rest.message.Request;

public interface RequestHandler {
    void handleRequest(Request request) throws RequestException, IOException;
}
