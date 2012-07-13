package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.restful.message.MessageHeader;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.Response;
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
