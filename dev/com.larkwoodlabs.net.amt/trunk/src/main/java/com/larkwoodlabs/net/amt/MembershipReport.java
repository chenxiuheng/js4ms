/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: MembershipReport.java (com.larkwoodlabs.net.amt)
 * 
 * Copyright © 2010-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.net.amt;

import java.util.HashSet;
import java.util.Iterator;


public final class MembershipReport {

    private final HashSet<GroupMembershipRecord> records = new HashSet<GroupMembershipRecord>();
    
    public MembershipReport() {
    }
    
    public void addRecord(final GroupMembershipRecord record) {
        this.records.add(record);
    }
    
    public HashSet<GroupMembershipRecord> getRecords() {
        return this.records;
    }

    public Iterator<GroupMembershipRecord> getRecordIterator() {
        return this.records.iterator();
    }

}
