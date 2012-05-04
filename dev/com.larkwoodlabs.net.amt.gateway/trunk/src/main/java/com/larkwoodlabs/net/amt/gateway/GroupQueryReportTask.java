/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: GroupQueryReportTask.java (com.larkwoodlabs.net.amt.gateway)
 * 
 * Copyright © 2009-2012 Cisco Systems, Inc.
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

package com.larkwoodlabs.net.amt.gateway;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Timer;

import com.larkwoodlabs.util.task.ReschedulableTask;


public final class GroupQueryReportTask extends ReschedulableTask {

    final InterfaceMembershipManager interfaceMembershipManager;

    final InetAddress groupAddress;
    HashSet<InetAddress> querySourceSet;
  
    /**
     * Constructs a response task for group and source-specific query.
     */
    public GroupQueryReportTask(final Timer taskTimer,
                                final InterfaceMembershipManager interfaceMembershipManager,
                                final InetAddress groupAddress,
                                final HashSet<InetAddress> querySourceSet) {
        super(taskTimer);
        this.interfaceMembershipManager = interfaceMembershipManager;
        this.groupAddress = groupAddress;
        this.querySourceSet = new HashSet<InetAddress>(querySourceSet);
    }
    
    public void updateQuerySourceSet(final HashSet<InetAddress> sourceSetAdditions) {
        if (this.querySourceSet == null) {
            this.querySourceSet = new HashSet<InetAddress>(sourceSetAdditions);
        }
        else {
            // Clear the source list if we receive a group or group-specific query with an empty source list 
            if (sourceSetAdditions.isEmpty()) {
                this.querySourceSet.clear();
            }
            else {
                this.querySourceSet.addAll(sourceSetAdditions);
            }
        }
    }

    @Override
    public void run() {
        this.interfaceMembershipManager.sendGroupQueryResponse(this.groupAddress, this.querySourceSet);
        this.querySourceSet.clear();
    }

}
