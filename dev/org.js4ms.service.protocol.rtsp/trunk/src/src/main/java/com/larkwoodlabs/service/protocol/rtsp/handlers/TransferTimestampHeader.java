package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rest.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.rest.message.MessageHeader;
import com.larkwoodlabs.service.protocol.rest.message.Request;
import com.larkwoodlabs.service.protocol.rest.message.Response;
import com.larkwoodlabs.service.protocol.rtsp.RtspMessageHeaders;

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
