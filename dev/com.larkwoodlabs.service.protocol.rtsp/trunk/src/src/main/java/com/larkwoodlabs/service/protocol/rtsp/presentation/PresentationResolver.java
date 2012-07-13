package com.larkwoodlabs.service.protocol.rtsp.presentation;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
