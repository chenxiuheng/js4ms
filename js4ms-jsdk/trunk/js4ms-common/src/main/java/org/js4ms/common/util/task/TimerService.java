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

package org.js4ms.common.util.task;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Logging;




/**
 * 
 *
 * @author Gregory Bumgardner
 */
public class TimerService {

    public static final Logger logger = Logger.getLogger(TimerService.class.getName());

    static class TimerServiceTask extends TimerTask {

        private TimerService service;
        
        public TimerServiceTask(TimerService service) {
            this.service = service;
        }

        @Override
        public void run() {
            this.service.run();
        }
        
    }

    private static TimerService instance = null;
    
    private long interval = 10000; // Default to 10 secs

    Timer timer = null;

    private HashSet<TimerTask> tasks = new HashSet<TimerTask>();

    public static TimerService instance() {
        synchronized(TimerService.class) {
            if (TimerService.instance == null) {
                TimerService.instance = new TimerService();
            }
        }
        return TimerService.instance;
    }
    
    public TimerService() {
    }

    public TimerService(long milliseconds) {
        this.interval = milliseconds;
    }
    
    public synchronized void setInterval(long milliseconds) {
        this.interval = milliseconds;
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = new Timer("TimerService",true);
            this.timer.schedule(new TimerServiceTask(this), 0, this.interval);
        }
    }
    
    public synchronized void start() {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
        if (this.timer == null) {
            this.timer = new Timer("TimerService", true);
            this.timer.schedule(new TimerServiceTask(this), 0, this.interval);
        }
    }

    public synchronized void stop() {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
    }

    public synchronized void add(TimerTask task) {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this,task));
        this.tasks.add(task);
        start();
    }

    public synchronized void remove(TimerTask task) {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this,task));
        this.tasks.remove(task);
        if (this.tasks.isEmpty()) {
            stop();
        }
    }

    public synchronized void clear() {
        stop();
        this.tasks.clear();
    }

    public void run() {
        if (logger.isLoggable(Level.FINE)) logger.fine(Logging.entry(this));
        Iterator<TimerTask> iter = this.tasks.iterator();
        while (iter.hasNext()) {
            iter.next().run();
        }
    }
}
