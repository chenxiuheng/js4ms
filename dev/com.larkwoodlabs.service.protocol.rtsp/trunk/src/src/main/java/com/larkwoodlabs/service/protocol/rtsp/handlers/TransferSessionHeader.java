package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rtsp.RtspMessageHeaders;
import com.larkwoodlabs.service.protocol.text.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;

public class TransferSessionHeader implements TransactionHandler {

    public TransferSessionHeader() {
        
    }

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        // Transfer Session header from request to response
        if (request.containsHeader(RtspMessageHeaders.SESSION)) {
            response.setHeader(new Header(request.getHeader(RtspMessageHeaders.SESSION)));
        }
        return false;
    }

}
