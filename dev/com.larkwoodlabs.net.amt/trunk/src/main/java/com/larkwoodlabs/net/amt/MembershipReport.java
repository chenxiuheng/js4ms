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

import java.util.HashSet;
import java.util.Iterator;


final class MembershipReport {

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
