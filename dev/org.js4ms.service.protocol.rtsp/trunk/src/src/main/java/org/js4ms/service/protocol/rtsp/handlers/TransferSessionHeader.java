package org.js4ms.service.protocol.rtsp.handlers;

import java.io.IOException;

import org.js4ms.service.protocol.rest.handler.TransactionHandler;
import org.js4ms.service.protocol.rest.message.MessageHeader;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;
import org.js4ms.service.protocol.rtsp.RtspMessageHeaders;


public class TransferSessionHeader implements TransactionHandler {

    public TransferSessionHeader() {
        
    }

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        // Transfer Session header from request to response
        if (request.containsHeader(RtspMessageHeaders.SESSION)) {
            response.setHeader((MessageHeader)request.getHeader(RtspMessageHeaders.SESSION).clone());
        }
        return false;
    }

}
