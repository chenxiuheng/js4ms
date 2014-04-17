package org.js4ms.service.protocol.rtsp.presentation;

import org.js4ms.service.protocol.rest.common.RequestException;
import org.js4ms.service.protocol.rest.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
