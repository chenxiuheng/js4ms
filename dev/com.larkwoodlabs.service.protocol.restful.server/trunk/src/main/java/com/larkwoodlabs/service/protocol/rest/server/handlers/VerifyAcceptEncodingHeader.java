package com.larkwoodlabs.service.protocol.rest.server.handlers;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.rest.MessageHeaders;
import com.larkwoodlabs.service.protocol.rest.StatusCodes;
import com.larkwoodlabs.service.protocol.rest.entity.CodecManager;
import com.larkwoodlabs.service.protocol.rest.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.rest.message.Request;
import com.larkwoodlabs.service.protocol.rest.message.Response;

public class VerifyAcceptEncodingHeader implements TransactionHandler {

    public VerifyAcceptEncodingHeader() {
        
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        if (request.containsHeader(MessageHeaders.ACCEPT_ENCODING)) {
            String value = request.getHeader(MessageHeaders.ACCEPT_ENCODING).getValue();
            // wildcard is always acceptable
            if (value.equals("*")) return false;
            String[] encodings = value.split(",[ ]*");
            for (String encoding : encodings) {
                float q = 1;
                String[] params = encoding.split(";");
                String encodingName = params[0];
                if (params.length > 1) {
                    String[] qExpression = params[1].split("=");
                    if (qExpression[0].equals("q") && qExpression.length > 1) {
                        q = Float.parseFloat(qExpression[1]);
                    }
                }
                if (q > 0 && CodecManager.getManager().hasCodec(encodingName)) {
                    // We have a codec for the requested content encoding.
                    return false;
                }
            }
            
            response.setStatus(StatusCodes.NotAcceptable);
        }
        return false;
    }

}
