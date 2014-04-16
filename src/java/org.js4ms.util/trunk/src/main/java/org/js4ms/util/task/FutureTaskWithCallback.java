/*
 * Copyright (C) 2009-2010 Larkwood Labs Software.
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

package org.js4ms.util.task;

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
