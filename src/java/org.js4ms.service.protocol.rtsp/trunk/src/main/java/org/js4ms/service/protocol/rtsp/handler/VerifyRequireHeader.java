package org.js4ms.service.protocol.rtsp.handler;

import java.io.IOException;
import java.util.HashSet;

import org.js4ms.service.protocol.rest.MessageHeaders;
import org.js4ms.service.protocol.rest.StatusCodes;
import org.js4ms.service.protocol.rest.handler.TransactionHandler;
import org.js4ms.service.protocol.rest.header.SimpleMessageHeader;
import org.js4ms.service.protocol.rest.message.MessageHeader;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;



public class VerifyRequireHeader implements TransactionHandler {

    private HashSet<String> features = null;

    public VerifyRequireHeader() {
        
    }

    public void addFeature(final String name) {
        if (this.features == null) {
            this.features = new HashSet<String>();
        }
        this.features.add(name);
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        if (request.containsHeader(MessageHeaders.REQUIRE)) {
            // Indicate that no special features are supported
            MessageHeader header = request.getHeader(MessageHeaders.REQUIRE);
            if (this.features == null || this.features.isEmpty()) {
                response.setStatus(StatusCodes.OptionNotSupported);
                response.setHeader(new SimpleMessageHeader(MessageHeaders.UNSUPPORTED, header.getValue()));
                return true;
            }
            else {
                // If multiple Require headers were present in the message, they
                // will have been concatenated into a single comma-delimited list by the parser.
                // Split the header value into individual feature names and check each.
                StringBuffer headerValue = new StringBuffer();
                String[] names = header.getValue().split(",[ ]*");
                for (String name : names) {
                    if (!this.features.contains(name)) {
                        if (headerValue.length() > 0) {
                            headerValue.append(",");
                        }
                        headerValue.append(name);
                    }
                }
                if (headerValue.length() != 0) {
                    response.setStatus(StatusCodes.OptionNotSupported);
                    response.setHeader(new SimpleMessageHeader(MessageHeaders.UNSUPPORTED,headerValue.toString()));
                    return true;
                }
            }
        }
        return false;
    }

}
