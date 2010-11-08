package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rtsp.RtspMessageHeaders;
import com.larkwoodlabs.service.protocol.text.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;

public class TransferTimestampHeader implements TransactionHandler {

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        // Transfer Timestamp header from request to response
        if (request.containsHeader(RtspMessageHeaders.TIMESTAMP)) {
            response.setHeader(new Header(request.getHeader(RtspMessageHeaders.TIMESTAMP)));
        }
        return false;
    }

}
