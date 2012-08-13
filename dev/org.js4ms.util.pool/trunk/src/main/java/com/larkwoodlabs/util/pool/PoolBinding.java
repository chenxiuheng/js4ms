/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: PoolBinding.java (com.larkwoodlabs.util.pool)
 * 
 * Copyright © 2009-2012 Cisco Systems, Inc.
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

package com.larkwoodlabs.util.pool;

public class PoolBinding<T> {

    T pooledObject;

    ObjectPool<T> pool;

    public PoolBinding(T pooledObject) {
        this(pooledObject, null);
    }

    public PoolBinding(T pooledObject, ObjectPool<T> pool) {
        this.pooledObject = pooledObject;
        this.pool = pool;
    }

    public T get() {
        return this.pooledObject;
    }

    public void release() throws Exception {
        if (this.pool != null) {
            this.pool.release(this.pooledObject);
        }
        this.pooledObject = null;
    }
}
