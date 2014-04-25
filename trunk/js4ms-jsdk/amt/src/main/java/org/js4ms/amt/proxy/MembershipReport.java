package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MembershipReport.java [org.js4ms.jsdk:amt]
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


import java.util.HashSet;
import java.util.Iterator;

import org.js4ms.amt.message.GroupMembershipRecord;

/**
 * A protocol-independent representation of an IGMPv3 or MLDv2 report message.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class MembershipReport {

    private final HashSet<GroupMembershipRecord> records = new HashSet<GroupMembershipRecord>();

    /**
     * Default constructor.
     */
    public MembershipReport() {
    }

    /**
     * Adds a group record to the report.
     * 
     * @param record
     *            A GroupMembershipRecord that describes the reception state
     *            for a single multicast group address.
     */
    public void addRecord(final GroupMembershipRecord record) {
        this.records.add(record);
    }

    /**
     * Gets a HashSet containing the group membership records.
     * @return A reference to the internal group record HashSet.
     */
    public HashSet<GroupMembershipRecord> getRecords() {
        return this.records;
    }

    /**
     * Gets an Iterator that may be used to iterate over the set of
     * group records contained in the report.
     * @return An Iterator for the internal HashSet.
     */
    public Iterator<GroupMembershipRecord> getRecordIterator() {
        return this.records.iterator();
    }

}
