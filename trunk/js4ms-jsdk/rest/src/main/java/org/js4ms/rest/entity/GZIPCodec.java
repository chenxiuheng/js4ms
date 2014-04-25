package org.js4ms.rest.entity;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * GZIPCodec.java [org.js4ms.jsdk:rest]
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
