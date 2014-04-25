package org.js4ms.common.util.task;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ReschedulableTask.java [org.js4ms.jsdk:common]
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


import java.util.Timer;
import java.util.TimerTask;

public abstract class ReschedulableTask implements Runnable {

    class Task extends TimerTask {

        ReschedulableTask task;

        Task(ReschedulableTask task) {
            this.task = task;
        }
        
        @Override
        public void run() {
            this.task.execute();
        }
    }

    Timer timer;
    TimerTask task = null;
    
    long period = 0;
    long nextTime = 0;
    boolean isPeriodic = false;
    boolean isScheduled = false;
    
    protected ReschedulableTask(Timer timer) {
        this.timer = timer;
    }
    
    /**
     * Schedule or reschedule task for execution after the specified delay.
     * @param delay
     */
    public synchronized void schedule(long delay) {
        init(delay, 0);
        this.isPeriodic = false;
        this.timer.schedule(this.task, delay);
    }

    /**
     * Schedule or reschedule the task for execution after 
     * the specified delay and repeated after the specified period.
     */
    public synchronized void schedule(long delay, long period) {
        init(delay, period);
        this.isPeriodic = true;
        this.timer.schedule(this.task, delay, period);
    }
    
    /**
     * Schedule or reschedule the task for execution after the specified
     * delay and repeated at a fixed rate as defined by the specified period.
     */
    public synchronized void scheduleAtFixedRate(long delay, long period) {
        init(delay, period);
        this.isPeriodic = true;
        this.timer.scheduleAtFixedRate(this.task, delay, period);
    }

    /**
     * Cancels task. A task may cancel itself.
     */
    public synchronized void cancel() {
        if (this.task != null) {
            this.task.cancel();
            this.isScheduled = false;
        }
    }

    /**
     * Indicates whether the task has executed at least once since last scheduled.
     */
    public boolean isScheduled() {
        return this.isScheduled;
    }

    /**
     * Returns the estimated time remaining until next execution of the task.
     * Will return MAX_VALUE if the task is not scheduled or has completed
     * its most recently scheduled execution.
     * @return
     */
    public synchronized long getTimeRemaining() {
        return this.isScheduled ? this.nextTime - System.currentTimeMillis() : Long.MAX_VALUE;
    }
    
    private void init(long delay, long period) {
        if (task != null) {
            this.task.cancel();
        }
        this.period = 0;
        this.nextTime = System.currentTimeMillis() + delay;
        this.task = new Task(this);
        this.isScheduled = true;
    }

    private synchronized void execute() {
        this.nextTime = System.currentTimeMillis() + this.period;
        this.isScheduled = this.isPeriodic;
        run();
    }
    
}
