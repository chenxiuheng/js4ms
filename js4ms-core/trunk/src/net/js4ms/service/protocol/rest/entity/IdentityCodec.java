package net.js4ms.service.protocol.rest.entity;

import java.io.InputStream;
import java.io.OutputStream;

public final class IdentityCodec implements Codec {

    private final static IdentityCodec codec;

    static {
        codec = new IdentityCodec();
    }

    public static IdentityCodec getCodec() {
        return codec;
    }

    private IdentityCodec() {}

    @Override
    public String getName() {
        return "identity";
    }

    @Override
    public InputStream getInputStream(final InputStream is) {
        return is;
    }

    @Override
    public OutputStream getOutputStream(final OutputStream os) {
        return os;
    }

}
