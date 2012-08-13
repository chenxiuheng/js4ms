/*
 * Copyright © 2009-2010 Larkwood Labs Software.
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
