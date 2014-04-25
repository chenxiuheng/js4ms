package org.js4ms.common.exception;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * BoundException.java [org.js4ms.jsdk:common]
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


/**
 * @author Greg Bumgardner (gbumgard)
 */
public class BoundException
                extends Exception {

    private static final long serialVersionUID = -5587197699181393667L;

    protected final Object object;

    protected final Throwable throwable;

    /**
     * @param object
     * @param throwable
     */
    public BoundException(final Object object, final Throwable throwable) {
        this.object = object;
        this.throwable = throwable;
    }

    /**
     * @return
     */
    public Object getObject() {
        return this.object;
    }

    /**
     * @return
     */
    public Throwable getThrowable() {
        return this.throwable;
    }

    /**
     * @throws Throwable
     */
    public void rethrow() throws Throwable {
        throw this.throwable;
    }
}
