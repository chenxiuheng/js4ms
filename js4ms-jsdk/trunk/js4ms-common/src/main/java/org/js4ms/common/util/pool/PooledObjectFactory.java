/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: PooledObjectFactory.java (org.js4ms.util.pool)
 * 
 * Copyright (C) 2009-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.js4ms.common.util.pool;

public abstract class PooledObjectFactory<T> {

    public abstract T create();

    public void destroy(T object) throws Exception {
    }

    public void validate(T object) throws Exception {
    }

    public void activate(T object) throws Exception {
    }

    public void deactivate(T object) throws Exception {
    }
}
