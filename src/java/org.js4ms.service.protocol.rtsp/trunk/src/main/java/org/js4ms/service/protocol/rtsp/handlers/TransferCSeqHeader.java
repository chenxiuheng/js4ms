package org.js4ms.service.protocol.rtsp.handlers;

import java.io.IOException;

import org.js4ms.service.protocol.rest.StatusCodes;
import org.js4ms.service.protocol.rest.entity.StringEntity;
import org.js4ms.service.protocol.rest.handler.TransactionHandler;
import org.js4ms.service.protocol.rest.message.MessageHeader;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;
import org.js4ms.service.protocol.rtsp.RtspMessageHeaders;



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
