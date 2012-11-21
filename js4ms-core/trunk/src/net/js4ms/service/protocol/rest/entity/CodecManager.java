package net.js4ms.service.protocol.rest.entity;

import java.util.HashMap;

public class CodecManager {

    private static CodecManager manager;
    
    static {
        manager = new CodecManager();
        manager.addCodec("*", IdentityCodec.getCodec());
        manager.addCodec(IdentityCodec.getCodec());
        manager.addCodec(GZIPCodec.getCodec());
    }

    public static CodecManager getManager() {
        return manager;
    }

    private final HashMap<String,Codec> codecs = new HashMap<String,Codec>();

    public CodecManager() {
        
    }

    public boolean hasCodec(final String name) {
        return this.codecs.containsKey(name);
    }

    public Codec getCodec(final String name) {
        return this.codecs.get(name);
    }

    public void addCodec(final String key, final Codec codec) {
        this.codecs.put(key, codec);
    }

    public void addCodec(final Codec codec) {
        this.codecs.put(codec.getName(), codec);
    }
}
