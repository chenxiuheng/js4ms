package org.js4ms.rest.entity;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IdentityCodec.java [org.js4ms.jsdk:rest]
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
