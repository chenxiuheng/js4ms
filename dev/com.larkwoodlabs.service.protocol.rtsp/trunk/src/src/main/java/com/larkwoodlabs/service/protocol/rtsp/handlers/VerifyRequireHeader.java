package com.larkwoodlabs.service.protocol.rtsp.handlers;

import java.io.IOException;
import java.util.HashSet;

import com.larkwoodlabs.service.protocol.text.MessageHeaders;
import com.larkwoodlabs.service.protocol.text.StatusCodes;
import com.larkwoodlabs.service.protocol.text.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;

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
            Header header = request.getHeader(MessageHeaders.REQUIRE);
            if (this.features == null || this.features.isEmpty()) {
                response.setStatus(StatusCodes.OptionNotSupported);
                response.setHeader(new Header(MessageHeaders.UNSUPPORTED, header.getValue()));
                return true;
            }
            else {
                // If multiple Require headers were present in the message, they
                // will have been concatenated into a single comma-delimited list by the parser.
                // Split the header value into individual feature names and check each.
                header = new Header(MessageHeaders.UNSUPPORTED);
                String[] names = header.getValue().split(",[ ]*");
                for (String name : names) {
                    if (!this.features.contains(name)) {
                        header.appendValue(name);
                    }
                }
                if (header.getValue().length() != 0) {
                    response.setStatus(StatusCodes.OptionNotSupported);
                    response.setHeader(header);
                    return true;
                }
            }
        }
        return false;
    }

}
