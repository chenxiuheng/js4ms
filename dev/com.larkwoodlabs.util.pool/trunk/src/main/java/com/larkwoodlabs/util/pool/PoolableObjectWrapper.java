/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.util.pool;

public abstract class PoolableObjectWrapper<ObjectType> implements PoolableObject {

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
