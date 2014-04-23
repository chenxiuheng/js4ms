package org.js4ms.common.util.task;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * FutureTaskWithCallback.java [org.js4ms.jsdk:common]
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


import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;



public class FutureTaskWithCallback<V> extends FutureTask<V> {

    private AsyncCallback<V> callback;
    private Executor callbackExecutor;

    public FutureTaskWithCallback(Callable<V> callable, AsyncCallback<V> callback) {
        super(callable);
        this.callback = callback;
        this.callbackExecutor = null;
    }
    
    public FutureTaskWithCallback(Callable<V> callable, AsyncCallback<V> callback, Executor callbackExecutor) {
        super(callable);
        this.callback = callback;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    protected void done() {
        if (!isCancelled() && this.callback != null) {
            if (this.callbackExecutor != null) {
                final Future<V> future = this;
                final AsyncCallback<V> callback = this.callback;
                this.callbackExecutor.execute(new Runnable() {
                    public void run() {
                        callback.invoke(future);
                    }
                });
            }
            else {
                this.callback.invoke(this);
            }
        }
    }

}
