package com.larkwoodlabs.service.protocol.text.client;

import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;

public interface RequestResponseMatcher {
    public boolean isResponseTo(final Request request, final Response response);
}
