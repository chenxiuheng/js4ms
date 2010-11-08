package com.larkwoodlabs.service.protocol.text.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.message.Request;

public interface RequestHandler {
    void handleRequest(Request request) throws RequestException, IOException;
}
