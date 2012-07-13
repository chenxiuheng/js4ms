package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.restful.message.MessageHeader;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.Response;
import com.larkwoodlabs.service.protocol.rtsp.RtspMessageHeaders;

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
