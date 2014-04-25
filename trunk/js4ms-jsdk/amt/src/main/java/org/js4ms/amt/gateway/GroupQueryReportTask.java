package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * GroupQueryReportTask.java [org.js4ms.jsdk:amt]
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
import java.util.Timer;

import org.js4ms.common.util.task.ReschedulableTask;


final class GroupQueryReportTask
                extends ReschedulableTask {

    private final InterfaceMembershipManager interfaceMembershipManager;

    private final InetAddress groupAddress;

    private HashSet<InetAddress> querySourceSet;

    /**
     * Constructs a response task for group and source-specific query.
     */
    GroupQueryReportTask(final Timer taskTimer,
                         final InterfaceMembershipManager interfaceMembershipManager,
                         final InetAddress groupAddress,
                         final HashSet<InetAddress> querySourceSet) {
        super(taskTimer);
        this.interfaceMembershipManager = interfaceMembershipManager;
        this.groupAddress = groupAddress;
        this.querySourceSet = new HashSet<InetAddress>(querySourceSet);
    }

    void updateQuerySourceSet(final HashSet<InetAddress> sourceSetAdditions) {
        if (this.querySourceSet == null) {
            this.querySourceSet = new HashSet<InetAddress>(sourceSetAdditions);
        }
        else {
            // Clear the source list if we receive a group or group-specific query with an
            // empty source list
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
