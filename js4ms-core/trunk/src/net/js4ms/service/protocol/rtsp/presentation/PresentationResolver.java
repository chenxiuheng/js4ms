package net.js4ms.service.protocol.rtsp.presentation;

import net.js4ms.service.protocol.rest.RequestException;
import net.js4ms.service.protocol.rest.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
