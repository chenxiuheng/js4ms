package com.larkwoodlabs.service.protocol.text.server.handlers;

import com.larkwoodlabs.service.protocol.restful.MessageHeaders;
import com.larkwoodlabs.service.protocol.restful.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.restful.headers.SimpleMessageHeader;
import com.larkwoodlabs.service.protocol.restful.message.Response;

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
