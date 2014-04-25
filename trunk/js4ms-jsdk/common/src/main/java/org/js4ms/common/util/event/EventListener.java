package org.js4ms.common.util.event;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * EventListener.java [org.js4ms.jsdk:common]
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


/**
 * Interface that must be implemented by event listeners that can be attached to an {@link Event}
 * @param <EventRecordType> The type of event object that will be passed to {@link #onEvent(EventRecordType)}
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public interface EventListener<EventRecordType> {

    /**
     * @param record An object that describes the event.
     * @return A Boolean value where:
     * <li><code>true</code> indicates that the handler should be detached from the event.
     * <li><code>false</code> indicates that the handler should remain attached to the event.
     */
    public boolean onEvent(final EventRecordType record);

}
