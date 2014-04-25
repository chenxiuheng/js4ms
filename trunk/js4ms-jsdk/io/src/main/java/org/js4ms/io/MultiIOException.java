package org.js4ms.io;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MultiIOException.java [org.js4ms.jsdk:io]
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
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Greg Bumgardner (gbumgard)
 */
public class MultiIOException
                extends IOException {

    private static final long serialVersionUID = 5271996285394748371L;

    private LinkedList<Throwable> throwables;

    /**
     * 
     */
    public MultiIOException() {
    }

    /**
     * @param t
     */
    public void add(Throwable t) {
        if (this.throwables == null) {
            this.throwables = new LinkedList<Throwable>();
        }
        this.throwables.add(t);
    }

    /**
     * @return
     */
    public Iterator<Throwable> iterator() {
        return this.throwables.iterator();
    }

    /**
     * @return
     */
    public boolean isThrowable() {
        return this.throwables != null && !this.throwables.isEmpty();
    }

    /**
     * @throws MultiIOException
     */
    public void rethrow() throws MultiIOException {
        if (isThrowable()) {
            throw this;
        }
    }
}
