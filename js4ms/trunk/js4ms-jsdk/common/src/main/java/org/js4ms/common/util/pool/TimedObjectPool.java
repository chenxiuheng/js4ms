package org.js4ms.common.util.pool;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TimedObjectPool.java [org.js4ms.jsdk:common]
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


import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Logging;
import org.js4ms.common.util.task.TimerService;




/**
 * @param <T>
 * @author Gregory Bumgardner
 */
public class TimedObjectPool<T>
                extends TimerTask
                implements ObjectPool<T> {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(TimedObjectPool.class.getName());

    /*-- Inner Classes ---------------------------------------------------*/

    public static class Factory<ObjectType>
                    implements ObjectPoolFactory<ObjectType> {

        private TimerService timerService;

        private int minInactive;

        private int maxInactive;

        private int maxActive;

        private long maxAge;

        public Factory(TimerService timerService, long maxAge) {
            this(timerService, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, maxAge);
        }

        public Factory(TimerService timerService, int minInactive, int maxInactive, int maxActive, long maxAge) {
            this.timerService = timerService;
            this.minInactive = minInactive;
            this.maxInactive = maxInactive;
            this.maxActive = maxActive;
            this.maxAge = maxAge;
        }

        @Override
        public ObjectPool<ObjectType> makePool(PooledObjectFactory<ObjectType> objectFactory) {
            TimedObjectPool<ObjectType> pool = new TimedObjectPool<ObjectType>(objectFactory,
                                                                               this.minInactive,
                                                                               this.maxInactive,
                                                                               this.maxActive,
                                                                               this.maxAge);
            this.timerService.add(pool);
            return pool;
        }

    }

    static class Holder<T> {

        private T object;

        private long deactivationTimeNanos;

        Holder(T object) {
            this.object = object;
            this.deactivationTimeNanos = System.nanoTime();
        }

        public T getObject() {
            return this.object;
        }

        void deactivate() {
            this.deactivationTimeNanos = System.nanoTime();
        }

        void destroy() {
            this.object = null;
        }

        boolean isExpired(long maxAge) {
            return this.object == null || getAge() > maxAge;
        }

        long getAge() {
            return (System.nanoTime() - this.deactivationTimeNanos) / 100000;
        }
    }

    public static class HolderFactory<T>
                    extends PooledObjectFactory<Holder<T>> {

        public static final Logger logger = Logger.getLogger(HolderFactory.class.getName());

        private PooledObjectFactory<T> factory;

        private HashMap<T, Holder<T>> holders = new HashMap<T, Holder<T>>();

        public HolderFactory(PooledObjectFactory<T> factory) {
            this.factory = factory;
        }

        @Override
        public void activate(Holder<T> holder) throws Exception {
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, holder));
            this.factory.activate(holder.getObject());
        }

        public synchronized Holder<T> create(T object) {
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
            Holder<T> holder = new Holder<T>(object);
            this.holders.put(holder.getObject(), holder);
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.exit(this, holder));
            return holder;
        }

        @Override
        public synchronized Holder<T> create() {
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
            Holder<T> holder = new Holder<T>(this.factory.create());
            this.holders.put(holder.getObject(), holder);
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.exit(this, holder));
            return holder;
        }

        @Override
        public void deactivate(Holder<T> holder) throws Exception {
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, holder));
            this.factory.deactivate(holder.getObject());
            holder.deactivate();
        }

        @Override
        public synchronized void destroy(Holder<T> holder) throws Exception {
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, holder));
            this.holders.remove(holder.getObject());
            this.factory.destroy(holder.getObject());
            holder.destroy();
        }

        @Override
        public void validate(Holder<T> holder) throws Exception {
            if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, holder));
            this.factory.validate(holder.getObject());
        }

        public Holder<T> find(T object) {
            return this.holders.get(object);
        }

        public PooledObjectFactory<T> getFactory() {
            return this.factory;
        }
    }

    static class ExpirationTest<T>
                    implements Condition<Holder<T>> {

        private long maxAge;

        public ExpirationTest(long maxAge) {
            this.maxAge = maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }

        public long getMaxAge() {
            return this.maxAge;
        }

        @Override
        public boolean test(Holder<T> object) {
            return object.isExpired(this.maxAge);
        }

    }

    /*-- Member Variables ---------------------------------------------------*/

    private HolderFactory<T> factory;

    private GenericObjectPool<Holder<T>> pool;

    private ExpirationTest<T> expirationTest;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an object pool that reaps "expired" inactive objects.
     * The pool uses the <code>factory</code> to construct objects.
     * The pool retains a minimum of zero inactive objects.
     * 
     * @param factory
     *            - an object factory
     * @param maxAge
     *            - age at which inactive objects will be reaped from the object pool
     */
    public TimedObjectPool(PooledObjectFactory<T> factory, long maxAge) {
        this(factory, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, maxAge);
    }

    /**
     * Constructs an object pool with that limits the number of active objects and number
     * and age of inactive objects.
     * The pool uses the <code>factory</code> to construct up to <code>maxActive</code>
     * objects.
     * The pool retains up to <code>maxInactive</code> objects and a minimum of
     * <code>minInactive</code> inactive objects.
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
     * @param maxAge
     *            - age at which inactive objects will be reaped from the object pool
     */
    public TimedObjectPool(PooledObjectFactory<T> factory, int minInactive, int maxInactive, int maxActive, long maxAge) {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, factory, minInactive, maxInactive, maxActive, maxAge));
        this.factory = new HolderFactory<T>(factory);
        this.pool = new GenericObjectPool<Holder<T>>(this.factory, minInactive, maxInactive, maxActive);
        this.expirationTest = new ExpirationTest<T>(maxAge);
    }

    public void setMaxAge(long milliseconds) {
        this.expirationTest.setMaxAge(milliseconds);
    }

    public long getMaxAge() {
        return this.expirationTest.getMaxAge();
    }

    @Override
    public T acquire() throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
        T object = this.pool.acquire().getObject();
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.exit(this, object));
        return object;
    }

    @Override
    public T acquire(long milliseconds) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, milliseconds));
        Holder<T> holder = this.pool.acquire(milliseconds);
        T object = null;
        if (holder != null) {
            object = holder.getObject();
        }
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.exit(this, object));
        return object;
    }

    @Override
    public void release(T object) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        Holder<T> holder = this.factory.find(object);
        if (holder != null) {
            this.pool.release(holder);
        }
    }

    @Override
    public boolean add(T object) {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        return this.pool.add(this.factory.create(object));
    }

    @Override
    public boolean remove(T object) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        Holder<T> holder = this.factory.find(object);
        boolean result = false;
        if (holder != null) {
            result = this.pool.remove(holder);
        }
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.exit(this, result));
        return result;
    }

    @Override
    public void activate(T object) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        this.factory.getFactory().activate(object);
    }

    @Override
    public void validate(T object) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        this.factory.getFactory().validate(object);
    }

    @Override
    public void deactivate(T object) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        this.factory.getFactory().deactivate(object);
    }

    @Override
    public void destroy(T object) throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this, object));
        remove(object);
        this.factory.getFactory().destroy(object);
    }

    @Override
    public PooledObjectFactory<T> getFactory() {
        return this.factory.getFactory();
    }

    public int getInactiveCount() {
        return this.pool.getInactiveCount();
    }

    public int getInactiveCapacity() {
        return this.pool.getInactiveCapacity();
    }

    public int getActiveCount() {
        return this.pool.getActiveCount();
    }

    public int getActiveCapacity() {
        return this.pool.getActiveCapacity();
    }

    public void reap() throws Exception {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
        this.pool.reap(this.expirationTest);
    }

    @Override
    public void run() {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
        try {
            reap();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
