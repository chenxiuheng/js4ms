package com.larkwoodlabs.service.protocol.restful.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;

public interface RequestHandler {
    void handleRequest(Request request) throws RequestException, IOException;
}
