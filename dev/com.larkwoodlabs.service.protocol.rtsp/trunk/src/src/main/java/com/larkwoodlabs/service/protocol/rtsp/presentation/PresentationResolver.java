package com.larkwoodlabs.service.protocol.rtsp.presentation;

import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.message.Request;

public interface PresentationResolver {
    Presentation getPresentation(Request request) throws RequestException;
}
