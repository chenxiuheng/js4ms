package org.js4ms.rest.client;

import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;

public interface RequestResponseMatcher {
    public boolean isResponseTo(final Request request, final Response response);
}
