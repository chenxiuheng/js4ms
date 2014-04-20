package org.js4ms.rtsp.presentation;

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
