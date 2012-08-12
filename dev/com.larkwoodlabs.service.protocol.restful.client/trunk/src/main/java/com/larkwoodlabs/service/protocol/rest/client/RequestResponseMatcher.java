package com.larkwoodlabs.service.protocol.rest.client;

import com.larkwoodlabs.service.protocol.rest.message.Request;
import com.larkwoodlabs.service.protocol.rest.message.Response;

public interface RequestResponseMatcher {
    public boolean isResponseTo(final Request request, final Response response);
}
