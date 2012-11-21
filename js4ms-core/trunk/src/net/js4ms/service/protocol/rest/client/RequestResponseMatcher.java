package net.js4ms.service.protocol.rest.client;

import net.js4ms.service.protocol.rest.message.Request;
import net.js4ms.service.protocol.rest.message.Response;

public interface RequestResponseMatcher {
    public boolean isResponseTo(final Request request, final Response response);
}
