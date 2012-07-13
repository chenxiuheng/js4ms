package com.larkwoodlabs.service.protocol.text.server.handlers;

import com.larkwoodlabs.service.protocol.rest.MessageHeaders;
import com.larkwoodlabs.service.protocol.rest.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.rest.headers.SimpleMessageHeader;
import com.larkwoodlabs.service.protocol.rest.message.Response;

public class AddServerHeader implements ResponseHandler {

    private final String serverName;

    public AddServerHeader(final String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void handleResponse(Response response) {
        response.setHeader(new SimpleMessageHeader(MessageHeaders.SERVER,this.serverName));
    }

}
