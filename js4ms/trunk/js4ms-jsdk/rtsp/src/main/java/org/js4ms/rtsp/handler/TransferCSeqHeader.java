package org.js4ms.rtsp.handler;

import java.io.IOException;

import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rest.message.StatusCode;
import org.js4ms.rtsp.message.RtspHeaderName;



public class TransferCSeqHeader implements TransactionHandler {

    public TransferCSeqHeader() {
        
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        if (request.getProtocolVersion().getProtocolName().getName().equals("RTSP")) {
            if (!request.containsHeader(RtspHeaderName.CSEQ)) {
                response.setStatus(StatusCode.BadRequest);
                response.setEntity(new StringEntity("missing CSEQ header"));
                return true;
            }
            response.setHeader((MessageHeader)request.getHeader(RtspHeaderName.CSEQ).clone());
        }
        return false;
    }

}