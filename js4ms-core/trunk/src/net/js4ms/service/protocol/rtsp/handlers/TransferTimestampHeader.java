package net.js4ms.service.protocol.rtsp.handlers;

import java.io.IOException;

import net.js4ms.service.protocol.rest.handler.TransactionHandler;
import net.js4ms.service.protocol.rest.message.MessageHeader;
import net.js4ms.service.protocol.rest.message.Request;
import net.js4ms.service.protocol.rest.message.Response;
import net.js4ms.service.protocol.rtsp.RtspMessageHeaders;


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
