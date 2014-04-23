package org.js4ms.rest.entity;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * CodecManager.java [org.js4ms.jsdk:rest]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
