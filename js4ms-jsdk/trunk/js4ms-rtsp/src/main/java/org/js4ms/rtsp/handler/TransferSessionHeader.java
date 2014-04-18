package org.js4ms.rtsp.handler;

import java.io.IOException;

import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rtsp.message.RtspHeaderName;



public class TransferSessionHeader implements TransactionHandler {

    public TransferSessionHeader() {
        
    }

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        // Transfer Session header from request to response
        if (request.containsHeader(RtspHeaderName.SESSION)) {
            response.setHeader((MessageHeader)request.getHeader(RtspHeaderName.SESSION).clone());
        }
        return false;
    }

}