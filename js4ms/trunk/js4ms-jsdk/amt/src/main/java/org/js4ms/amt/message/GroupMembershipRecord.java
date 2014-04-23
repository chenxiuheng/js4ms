package org.js4ms.amt.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * GroupMembershipRecord.java [org.js4ms.jsdk:amt]
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


import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Describes the current reception state, or a change in reception state
 * for a single multicast group address. This class is used to provide a
 * protocol-independent representation of an IGMPv3 or MLDv2 group record.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class GroupMembershipRecord {

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
     * Constructs an instance for the specified group.
     * 
     * @param group
     *            A multicast group address.
     * @param recordType
     *            An enumerator describing the state or state change for the group.
     * @param sourceSet
     *            - The sources to list in the record - set is stored by reference, not
     *            copied.
     */
    public GroupMembershipRecord(InetAddress group, Type recordType, HashSet<InetAddress> sourceSet) {
        this.group = group;
        this.recordType = recordType;
        this.sourceSet = sourceSet;
    }

    /**
     * Gets the multicast group address.
     * 
     * @return An InetAddress (IPv4 or IPv6) object containing the group address.
     */
    public InetAddress getGroup() {
        return this.group;
    }

    /**
     * Gets the group record type.
     * 
     * @return A Type enumerator describing the record type.
     */
    public Type getRecordType() {
        return this.recordType;
    }

    /**
     * Gets the HashSet containing the source addresses that describe the current
     * include/exclude state or are used to describe a state change.
     * 
     * @return The HashSet originally provided when the record was constructed.
     */
    public HashSet<InetAddress> getSources() {
        return this.sourceSet;
    }

    /**
     * Gets an Iterator for the source address HashSet.
     * 
     * @return An Iterator that can be used to iterator over the set of source addresses.
     */
    public Iterator<InetAddress> getSourceIterator() {
        return this.sourceSet.iterator();
    }

}
