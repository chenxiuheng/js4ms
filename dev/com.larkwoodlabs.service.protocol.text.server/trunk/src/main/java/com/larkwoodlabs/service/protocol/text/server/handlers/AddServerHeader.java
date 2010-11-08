package com.larkwoodlabs.service.protocol.text.server.handlers;

import com.larkwoodlabs.service.protocol.text.MessageHeaders;
import com.larkwoodlabs.service.protocol.text.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Response;

public class AddServerHeader implements ResponseHandler {

    private final String serverName;

    public AddServerHeader(final String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void handleResponse(Response response) {
        response.setHeader(new Header(MessageHeaders.SERVER,this.serverName));
    }

}
