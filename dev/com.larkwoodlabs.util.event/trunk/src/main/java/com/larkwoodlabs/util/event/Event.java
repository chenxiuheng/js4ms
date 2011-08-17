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

package com.larkwoodlabs.util.event;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * An <code>Event</code> provides an attachment point for event handlers and a means for delivering an event record to those handlers. 
 * @param <EventRecordType> The type of event object that will be passed to {@link #onEvent(EventType)}
 *
 * @author Gregory Bumgardner
 */
public class Event<EventRecordType> {

    LinkedList<EventListener<EventRecordType>> handlers;

    /**
     * Attaches a handler instance to the event.
     * @param handler An object that implements the {@link EventListener} interface.
     */
    public synchronized void attach(EventListener<EventRecordType> handler) {
        if (this.handlers == null) {
            this.handlers = new LinkedList<EventListener<EventRecordType>>();
        }
        this.handlers.add(handler);
    }

    /**
     * Detaches a handler instance from the event.
     * @param handler An object that implements the {@link EventListener} interface.
     */
    public synchronized void detach(EventListener<EventRecordType> handler) {
        if (this.handlers != null) {
            this.handlers.remove(handler);
        }
    }
    
    /**
     * Detaches all handler instances from the event.
     */
    public synchronized void detachAll() {
        if (this.handlers != null) {
            this.handlers.clear();
        }
    }

    public boolean isEmpty() {
        return this.handlers.isEmpty();
    }

    /**
     * Calls the {@link EventListener#onEvent(RecordType)} method on all attached event handlers passing the <code>event</code> object.
     * @param event An "event" object that will be passed to the attached event handlers.
     */
    public synchronized void invoke(EventRecordType record) {
        if (this.handlers != null) {
            Iterator<EventListener<EventRecordType>> iter = this.handlers.iterator();
            while (iter.hasNext()) {
                if (iter.next().onEvent(record)) {
                    iter.remove();
                }
            }
        }
    }
}
