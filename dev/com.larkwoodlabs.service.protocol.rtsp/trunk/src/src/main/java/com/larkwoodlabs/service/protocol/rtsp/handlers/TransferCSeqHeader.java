package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rtsp.RtspMessageHeaders;
import com.larkwoodlabs.service.protocol.text.StatusCodes;
import com.larkwoodlabs.service.protocol.text.entity.StringEntity;
import com.larkwoodlabs.service.protocol.text.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;

public class TransferCSeqHeader implements TransactionHandler {

    public TransferCSeqHeader() {
        
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        if (request.getProtocolVersion().getProtocolName().getName().equals("RTSP")) {
            if (!request.containsHeader(RtspMessageHeaders.CSEQ)) {
                response.setStatus(StatusCodes.BadRequest);
                response.setEntity(new StringEntity("missing CSEQ header"));
                return true;
            }
            response.setHeader(new Header(request.getHeader(RtspMessageHeaders.CSEQ)));
        }
        return false;
    }

}
