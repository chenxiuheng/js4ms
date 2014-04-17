package org.js4ms.service.rtsp.handler;

import java.io.IOException;

import org.js4ms.service.rest.handler.TransactionHandler;
import org.js4ms.service.rest.message.MessageHeader;
import org.js4ms.service.rest.message.Request;
import org.js4ms.service.rest.message.Response;
import org.js4ms.service.rtsp.message.RtspHeaderName;



public class TransferTimestampHeader implements TransactionHandler {

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        // Transfer Timestamp header from request to response
        if (request.containsHeader(RtspHeaderName.TIMESTAMP)) {
            response.setHeader((MessageHeader)request.getHeader(RtspHeaderName.TIMESTAMP).clone());
        }
        return false;
    }

}
