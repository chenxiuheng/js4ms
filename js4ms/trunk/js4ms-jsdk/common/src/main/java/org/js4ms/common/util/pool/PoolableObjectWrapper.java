package org.js4ms.common.util.pool;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * PoolableObjectWrapper.java [org.js4ms.jsdk:common]
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


public abstract class PoolableObjectWrapper<ObjectType>
                implements PoolableObject {

    protected ObjectType object;

    protected ObjectPool<ObjectType> pool;

    protected PoolableObjectWrapper(ObjectType object, ObjectPool<ObjectType> pool) {
        this.object = object;
        this.pool = pool;
    }

    public void release() throws Exception {
        if (this.pool != null) this.pool.release(this.object);
    }

    public ObjectType getObject() {
        return this.object;
    }

    public ObjectPool<ObjectType> getPool() {
        return this.pool;
    }

}
