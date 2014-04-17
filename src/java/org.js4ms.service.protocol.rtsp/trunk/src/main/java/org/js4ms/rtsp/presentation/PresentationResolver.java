package org.js4ms.rtsp.presentation;

import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
