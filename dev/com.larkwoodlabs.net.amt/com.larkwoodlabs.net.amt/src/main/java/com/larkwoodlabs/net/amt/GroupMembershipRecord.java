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

package com.larkwoodlabs.net.amt;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;

final class GroupMembershipRecord {

    /**
     * Enumeration of group record types.
     */
    public enum Type {

        /**
         * MODE_IS_INCLUDE - indicates that the interface has a filter mode of
         * INCLUDE for the specified multicast address. The Source Address [i]
         * fields in this Group Record contain the interface's source list for
         * the specified multicast address, if it is non-empty.
         */
        MODE_IS_INCLUDE(1),

        /**
         * MODE_IS_EXCLUDE - indicates that the interface has a filter mode of
         * EXCLUDE for the specified multicast address. The Source Address [i]
         * fields in this Group Record contain the interface's source list for
         * the specified multicast address, if it is non-empty.
         */
        MODE_IS_EXCLUDE(2),

        /**
         * CHANGE_TO_INCLUDE_MODE - indicates that the interface changed to
         * INCLUDE filter mode for the specified address. The Source Address [i]
         * fields this Group Record contain the interface's new list for the
         * specified multicast address, if it is non-empty.
         */
        CHANGE_TO_INCLUDE_MODE(3),

        /**
         * CHANGE_TO_EXCLUDE_MODE - indicates that the interface has changed to
         * EXCLUDE filter mode for the specified multicast address. The Source
         * Address [i] fields in this Group Record contain the interface's new
         * source list for the specified multicast address, if it is non-empty.
         */
        CHANGE_TO_EXCLUDE_MODE(4),

        /**
         * ALLOW_NEW_SOURCES - indicates that the Source Address [i] fields in
         * this Group Record contain a list of the additional sources that the
         * system wishes to hear from, for packets sent to the specified
         * multicast address. If the change was to an INCLUDE source list, these
         * are the addresses that were added to the list; if the change was to
         * an EXCLUDE source list, these are the addresses that were deleted
         * from the list.
         */
        ALLOW_NEW_SOURCES(5),

        /**
         * BLOCK_OLD_SOURCES - indicates that the Source Address [i] fields in
         * this Group Record contain a list of the sources that the system no
         * longer wishes to hear from, for packets sent to the specified
         * multicast address. If the change was to an INCLUDE source list, these
         * are the addresses that were deleted from the list; if the change was
         * to an EXCLUDE source list, these are the addresses that were added to
         * the list.
         */
        BLOCK_OLD_SOURCES(6);
        
        int value;
        Type(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return this.value;
        }
    }

    InetAddress group;
    Type recordType;
    HashSet<InetAddress> sourceSet;
    
    /**
     * 
     * @param group
     * @param recordType
     * @param sourceSet - The sources to list in the record - set is stored by reference, not copied.
     */
    public GroupMembershipRecord(InetAddress group, Type recordType, HashSet<InetAddress> sourceSet) {
        this.group = group;
        this.recordType = recordType;
        this.sourceSet = sourceSet;
    }
    
    public InetAddress getGroup() {
        return this.group;
    }

    public Type getRecordType() {
        return this.recordType;
    }

    public HashSet<InetAddress> getSources() {
        return this.sourceSet;
    }

    public Iterator<InetAddress> getSourceIterator() {
        return this.sourceSet.iterator();
    }
    
}
