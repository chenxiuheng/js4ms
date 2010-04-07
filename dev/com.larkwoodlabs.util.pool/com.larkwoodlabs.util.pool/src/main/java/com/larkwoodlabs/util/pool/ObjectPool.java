/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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


/**
 *
 * @param <T>
 *
 * @author Gregory Bumgardner
 */
public interface ObjectPool<T> {

    /**
     * 
     * @return
     * @throws Exception
     */
    public T acquire() throws Exception;
    
    /**
     * 
     * @param milliseconds
     * @return
     * @throws Exception
     */
    public T acquire(long milliseconds) throws Exception;

    /**
     * 
     * @param object
     * @throws Exception
     */
    public void release(T object) throws Exception;
    
    /**
     * 
     * @param object
     * @return
     */
    public boolean add(T object);

    /**
     * 
     * @param object
     * @return
     * @throws Exception
     */
    public boolean remove(T object) throws Exception;

    /**
     * 
     * @param object
     * @throws Exception
     */
    public void activate(T object) throws Exception;

    /**
     * 
     * @param object
     * @throws Exception
     */
    public void validate(T object) throws Exception;

    /**
     * 
     * @param object
     * @throws Exception
     */
    public void deactivate(T object) throws Exception;

    /**
     * 
     * @param object
     * @throws Exception
     */
    public void destroy(T object) throws Exception;

    /**
     * 
     * @return
     */
    public PooledObjectFactory<T> getFactory();

}
