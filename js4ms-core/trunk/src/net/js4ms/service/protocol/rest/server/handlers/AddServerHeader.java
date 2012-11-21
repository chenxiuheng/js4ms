package net.js4ms.service.protocol.rest.server.handlers;

import net.js4ms.service.protocol.rest.MessageHeaders;
import net.js4ms.service.protocol.rest.handler.ResponseHandler;
import net.js4ms.service.protocol.rest.headers.SimpleMessageHeader;
import net.js4ms.service.protocol.rest.message.Response;

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
