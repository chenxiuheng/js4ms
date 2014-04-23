package org.js4ms.common.exception;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MultiException.java [org.js4ms.jsdk:common]
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


import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Greg Bumgardner (gbumgard)
 */
public class MultiException
                extends Exception {

    private static final long serialVersionUID = 4050769134360179803L;

    private LinkedList<Throwable> throwables;

    /**
     * 
     */
    public MultiException() {
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
     * @throws MultiException
     */
    public void rethrow() throws MultiException {
        if (!this.throwables.isEmpty()) {
            throw this;
        }
    }
}
