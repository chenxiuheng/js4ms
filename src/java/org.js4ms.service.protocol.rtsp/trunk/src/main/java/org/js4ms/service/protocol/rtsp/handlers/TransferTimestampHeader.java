package org.js4ms.service.protocol.rtsp.handlers;

import java.io.IOException;

import org.js4ms.service.protocol.rest.handler.TransactionHandler;
import org.js4ms.service.protocol.rest.message.MessageHeader;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;
import org.js4ms.service.protocol.rtsp.RtspMessageHeaders;



public class TransferTimestampHeader implements TransactionHandler {

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        // Transfer Timestamp header from request to response
        if (request.containsHeader(RtspMessageHeaders.TIMESTAMP)) {
            response.setHeader((MessageHeader)request.getHeader(RtspMessageHeaders.TIMESTAMP).clone());
        }
        return false;
    }

}
