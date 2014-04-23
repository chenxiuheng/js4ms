package org.js4ms.common.util.pool;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * GenericObjectPool.java [org.js4ms.jsdk:common]
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


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.LoggableBase;



public class GenericObjectPool<T>
                extends LoggableBase
                implements ObjectPool<T> {

    public static class Factory<ObjectType>
                    implements ObjectPoolFactory<ObjectType> {

        private int minInactive;

        private int maxInactive;

        private int maxActive;

        public Factory() {
            this(0, GenericObjectPool.DEFAULT_MAX_INACTIVE, GenericObjectPool.DEFAULT_MAX_ACTIVE);
        }

        public Factory(int minInactive, int maxInactive, int maxActive) {
            this.minInactive = minInactive;
            this.maxInactive = maxInactive;
            this.maxActive = maxActive;
        }

        @Override
        public ObjectPool<ObjectType> makePool(PooledObjectFactory<ObjectType> objectFactory) {
            return new GenericObjectPool<ObjectType>(objectFactory, this.minInactive, this.maxInactive, this.maxActive);
        }

    }

    private PooledObjectFactory<T> factory;

    public static final Logger logger = Logger.getLogger(GenericObjectPool.class.getName());

    public static final int DEFAULT_MAX_INACTIVE = 64;

    public static final int DEFAULT_MAX_ACTIVE = 64;

    ArrayBlockingQueue<T> inactiveObjects;

    ArrayBlockingQueue<T> activeObjects;

    int minInactive;

    /**
     * Constructs an object with an <code>Integer.MAX_VALUE</code> limit on the number of
     * active or inactive objects.
     * The pool uses <code>factory</code> to construct objects.
     * The pool retains a minimum of zero inactive objects.
     * 
     * @param factory
     *            - an object factory
     */
    public GenericObjectPool(PooledObjectFactory<T> factory) {
        this(factory, 0, DEFAULT_MAX_INACTIVE, DEFAULT_MAX_ACTIVE);
    }

    /**
     * Constructs an object pool with that limits the number of inactive and active
     * objects.
     * The pool uses <code>factory</code> to construct up to <code>maxActive</code>
     * objects.
     * The pool retains up to <code>maxInactive</code> objects.
     * The acquire method blocks until an object is released if there are already
     * <code>maxActive</code> number of active objects.
     * The release method destroys an object if there are already a
     * <code>maxInactive</code> number of inactive objects.
     * 
     * @param factory
     *            - an object factory
     * @param minInactive
     *            - minimum number of inactive objects that will be retained by the pool
     *            by the {@link #reap(Condition)} method.
     * @param maxInactive
     *            - maximum number of inactive objects that will be retained by the pool.
     * @param maxActive
     *            - maximum number of active objects that may be acquired from the pool.
     */
    public GenericObjectPool(PooledObjectFactory<T> factory, int minInactive, int maxInactive, int maxActive) {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.GenericObjectPool", factory,
                                                                        minInactive, maxInactive, maxActive));
        this.factory = factory;
        this.minInactive = Math.max(minInactive, 0);
        this.inactiveObjects = new ArrayBlockingQueue<T>(maxInactive);
        this.activeObjects = new ArrayBlockingQueue<T>(maxActive);
    }

    public synchronized T acquire() throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.acquire"));
        // Try to get an object from the inactive queue
        T object = this.inactiveObjects.poll();
        if (object == null) {
            // Can we create a new object and activate it?
            if (this.activeObjects.remainingCapacity() > 0) {
                if (logger.isLoggable(Level.FINE)) logger.fine(this.log.msg("constructed new object: " + object));
                object = this.factory.create();
            }
            else {
                // Wait for an object to be released
                if (logger.isLoggable(Level.FINE)) logger.fine(this.log.msg("waiting for release: active=" + getActiveCount()
                                                                            + " inactive=" + getInactiveCount()));
                object = this.inactiveObjects.take();
                this.factory.validate(object);
            }
        }

        try {
            this.activeObjects.put(object);
            this.factory.activate(object);
            if (logger.isLoggable(Level.FINE)) {
                if (this.activeObjects.contains(object)) {
                    logger.fine(this.log.msg("added object " + object + " to active queue: active=" + getActiveCount()
                                             + " inactive=" + getInactiveCount()));
                }
                else {
                    logger.warning(this.log.msg("object " + object + " does not appear to have been added to the active queue!"));
                }
            }
        }
        catch (Exception e) {
            if (!this.inactiveObjects.offer(object)) {
                this.factory.destroy(object);
            }
            throw e;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("" + "active=" + getActiveCount() + " inactive=" + getInactiveCount()));
        }
        return object;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public synchronized T acquire(long milliseconds) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.acquire", milliseconds));
        // Try to get an object from the inactive queue
        T object = this.inactiveObjects.poll();
        if (object == null) {
            if (this.activeObjects.remainingCapacity() > 0) {
                object = this.factory.create();
                if (logger.isLoggable(Level.FINE)) logger.fine(this.log.msg("constructed new object: " + object));
            }
            else {
                if (logger.isLoggable(Level.FINE)) logger.fine(this.log.msg("waiting for release: active=" + getActiveCount()
                                                                            + " inactive=" + getInactiveCount()));
                long startTime = System.currentTimeMillis();
                object = this.inactiveObjects.poll(milliseconds, TimeUnit.MILLISECONDS);
                if (object != null) {
                    this.factory.validate(object);
                    // Calculate time allowed to add object to active queue
                    milliseconds = Math.max(milliseconds - (System.currentTimeMillis() - startTime), 0);
                }
            }
        }

        if (object != null) {
            try {
                // Try to add the object to the active queue
                if (!this.activeObjects.offer(object, milliseconds, TimeUnit.MILLISECONDS)) {
                    if (logger.isLoggable(Level.FINE)) logger.fine(this.log.msg("unable to add object " + object
                                                                                + " to active queue: active=" + getActiveCount()
                                                                                + " inactive="
                                                                                + getInactiveCount()));
                    if (!this.inactiveObjects.offer(object)) {
                        this.factory.destroy(object);
                    }
                    object = null;
                }
                else {
                    this.factory.activate(object);
                    if (logger.isLoggable(Level.FINE)) {
                        if (this.activeObjects.contains(object)) {
                            logger.fine(this.log.msg("added object " + object + " to active queue: active=" + getActiveCount()
                                                     + " inactive=" + getInactiveCount()));
                        }
                        else {
                            logger.warning(this.log.msg("object " + object
                                                        + " does not appear to have been added to the active queue!"));
                        }
                    }
                }
            }
            catch (Exception e) {
                if (!this.inactiveObjects.offer(object)) {
                    this.factory.destroy(object);
                }
                throw e;
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("active=" + getActiveCount() + " inactive=" + getInactiveCount()));
        }
        return object;
    }

    public synchronized void release(T object) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.release", object));
        if (this.activeObjects.remove(object)) {
            if (logger.isLoggable(Level.FINE)) logger.fine(this.log.msg("removed object " + object + " from active queue: active="
                                                                        + getActiveCount() + " inactive=" + getInactiveCount()));
            this.factory.deactivate(object);
            if (!this.inactiveObjects.offer(object)) {
                this.factory.destroy(object);
            }
        }
        else {
            logger.warning(this.log.msg("illegal attempt to release inactive object " + object + " from active queue: active="
                                        + getActiveCount() + " inactive=" + getInactiveCount()));
            throw new IllegalArgumentException("illegal attempt to release inactive object");
        }
        if (logger.isLoggable(Level.FINE)) logger
                        .fine(this.log.msg("active=" + getActiveCount() + " inactive=" + getInactiveCount()));
    }

    public synchronized boolean add(T object) {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.add", object));
        boolean result = false;
        if (this.inactiveObjects.offer(object)) {
            result = true;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("active=" + getActiveCount() + " inactive=" + getInactiveCount()));
        }
        return result;
    }

    public synchronized boolean remove(T object) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.remove", object));
        boolean result = false;
        if (this.activeObjects.remove(object)) {
            this.factory.deactivate(object);
            this.factory.destroy(object);
            result = true;
        }
        else if (this.inactiveObjects.remove(object)) {
            this.factory.destroy(object);
            result = true;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("active=" + getActiveCount() + " inactive=" + getInactiveCount()));
        }
        return result;
    }

    public synchronized void reap(Condition<T> condition) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.reap", condition));
        while (this.inactiveObjects.size() > this.minInactive && condition.test(this.inactiveObjects.peek())) {
            T object = this.inactiveObjects.poll();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("reaping object " + object));
                logger.fine(this.log.msg("active=" + getActiveCount() + " inactive=" + getInactiveCount()));
            }
            this.factory.destroy(object);
        }
    }

    public PooledObjectFactory<T> getFactory() {
        return this.factory;
    }

    public void activate(T object) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.activate", object));
        this.factory.activate(object);
    }

    public void validate(T object) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.validate", object));
        this.factory.validate(object);
    }

    public void deactivate(T object) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.deactivate", object));
        this.factory.deactivate(object);
    }

    public void destroy(T object) throws Exception {
        if (logger.isLoggable(Level.FINER)) logger.finer(this.log.entry("GenericObjectPool.destroy", object));
        remove(object);
        this.factory.destroy(object);
    }

    public int getInactiveCount() {
        return this.inactiveObjects.size();
    }

    public int getInactiveCapacity() {
        return this.inactiveObjects.remainingCapacity();
    }

    public int getActiveCount() {
        return this.activeObjects.size();
    }

    public int getActiveCapacity() {
        return this.activeObjects.remainingCapacity();
    }

}
