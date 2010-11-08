package com.larkwoodlabs.service.protocol.text.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Codec {
    String getName();
    InputStream getInputStream(InputStream is) throws IOException;
    OutputStream getOutputStream(OutputStream os) throws IOException;
}
