package net.js4ms.util.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncResult<V> implements Future<V> {

    enum State {
        Waiting,
        Set,
        Cancelled,
        Failed
    }

    private final Object lock = new Object();
    private Thread producer;
    private State state;
    private V value;
    private Throwable throwable;

    public AsyncResult(final Thread producer) {
        this.producer = producer;
        this.state = State.Waiting;
        this.value = null;
        this.throwable = null;
    }

    public AsyncResult() {
        this.producer = null;
        this.state = State.Waiting;
        this.value = null;
        this.throwable = null;
    }

    public void reset(boolean ifNotCancelled) {
        synchronized (this.lock) {
            if (ifNotCancelled && this.state == State.Cancelled) {
                return;
            }
            this.producer = Thread.currentThread();
            this.state = State.Waiting;
            this.value = null;
            this.throwable = null;
        }
    }

    public void set(final V value) {
        synchronized (this.lock) {
            this.value = value;
            this.state = State.Set;
            this.lock.notifyAll();
        }
    }

    public void fail(final Throwable throwable) {
        synchronized (this.lock) {
            this.state = State.Failed;
            this.throwable = throwable;
            this.lock.notifyAll();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this.lock) {
            if (this.state == State.Waiting) {
                this.state = State.Cancelled;
                if (mayInterruptIfRunning) {
                    if (this.producer != null) {
                        this.producer.interrupt();
                    }
                }
                this.lock.notifyAll();
                return true;
            }
            else {
                return false;
            }
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException, CancellationException {

        synchronized (this.lock) {

            while (this.state == State.Waiting) {
                this.lock.wait();
            }

            if (this.state == State.Cancelled) {
                throw new CancellationException();
            }
            else if (this.state == State.Failed) {
                throw new ExecutionException(this.throwable);
            }

            return this.value;
        }
    }

    @Override
    public V get(final long timeout, final TimeUnit unit) throws InterruptedException,
                                                                 ExecutionException,
                                                                 CancellationException,
                                                                 TimeoutException {
        synchronized (this.lock) {

            if (this.state == State.Waiting) {
                this.lock.wait(unit.convert(timeout,TimeUnit.MILLISECONDS));
                if (this.state == State.Waiting) {
                    throw new TimeoutException();
                }
            }

            if (this.state == State.Cancelled) {
                throw new CancellationException();
            }
            else if (this.state == State.Failed) {
                throw new ExecutionException(this.throwable);
            }

            return this.value;
        }
    }

    @Override
    public boolean isCancelled() {
        synchronized (this.lock) {
            return this.state == State.Cancelled;
        }
    }

    @Override
    public boolean isDone() {
        synchronized (this.lock) {
            return this.state != State.Waiting;
        }
    }

}
