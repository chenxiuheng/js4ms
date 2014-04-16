package org.js4ms.service.protocol.rest.handler.header;

import org.js4ms.service.protocol.rest.MessageHeaders;
import org.js4ms.service.protocol.rest.handler.ResponseHandler;
import org.js4ms.service.protocol.rest.header.SimpleMessageHeader;
import org.js4ms.service.protocol.rest.message.Response;

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
