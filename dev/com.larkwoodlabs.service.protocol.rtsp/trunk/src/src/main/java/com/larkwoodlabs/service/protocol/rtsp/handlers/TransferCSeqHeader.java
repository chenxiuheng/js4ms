package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.StatusCodes;
import com.larkwoodlabs.service.protocol.restful.entity.StringEntity;
import com.larkwoodlabs.service.protocol.restful.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.restful.message.MessageHeader;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.Response;
import com.larkwoodlabs.service.protocol.rtsp.RtspMessageHeaders;

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
            response.setHeader((MessageHeader)request.getHeader(RtspMessageHeaders.CSEQ).clone());
        }
        return false;
    }

}
