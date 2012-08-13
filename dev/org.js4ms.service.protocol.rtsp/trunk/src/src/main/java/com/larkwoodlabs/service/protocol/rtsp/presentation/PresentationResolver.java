package com.larkwoodlabs.service.protocol.rtsp.presentation;

import com.larkwoodlabs.service.protocol.rest.RequestException;
import com.larkwoodlabs.service.protocol.rest.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
