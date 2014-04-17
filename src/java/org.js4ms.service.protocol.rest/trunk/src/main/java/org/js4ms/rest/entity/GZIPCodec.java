package org.js4ms.service.rest.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPCodec implements Codec {

    private final static GZIPCodec codec;

    static {
        codec = new GZIPCodec();
    }

    public static GZIPCodec getCodec() {
        return codec;
    }

    private GZIPCodec() {}

    @Override
    public String getName() {
        return "gzip";
    }

    @Override
    public InputStream getInputStream(InputStream is) throws IOException {
        return new GZIPInputStream(is);
    }

    @Override
    public OutputStream getOutputStream(OutputStream os) throws IOException {
        return new GZIPOutputStream(os);
    }

}
