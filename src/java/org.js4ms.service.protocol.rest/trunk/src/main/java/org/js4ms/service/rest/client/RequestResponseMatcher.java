package org.js4ms.service.rest.client;

import org.js4ms.service.rest.message.Request;
import org.js4ms.service.rest.message.Response;

public interface RequestResponseMatcher {
    public boolean isResponseTo(final Request request, final Response response);
}
