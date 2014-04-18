/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: StateChangeReportTask.java (org.js4ms.net.amt.gateway)
 * 
 * Copyright (C) 2009-2012 Cisco Systems, Inc.
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

package org.js4ms.amt.gateway;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.common.util.task.ReschedulableTask;
import org.js4ms.net.ip.multicast.service.proxy.SourceFilter;



/**
 * @author Greg Bumgardner (gbumgard)
 */
final class StateChangeReportTask
                extends ReschedulableTask {

    private final InterfaceMembershipManager interfaceMembershipManager;

    private final InetAddress groupAddress;

    private final int retransmissionCount;

    private int modeChangeTransmissionsRemaining;

    private int sourceChangeTransmissionsRemaining;

    private SourceFilter.Mode mode;

    private HashSet<InetAddress> sourceSet;

    private HashSet<InetAddress> allowNewSources;

    private HashSet<InetAddress> blockOldSources;

    /**
     * @param taskTimer
     * @param interfaceMembershipManager
     * @param groupAddress
     * @param retransmissionCount
     * @param currentMode
     * @param currentSourceSet
     */
    StateChangeReportTask(final Timer taskTimer,
                          final InterfaceMembershipManager interfaceMembershipManager,
                          final InetAddress groupAddress,
                          final int retransmissionCount,
                          final SourceFilter.Mode currentMode,
                          final HashSet<InetAddress> currentSourceSet) {
        super(taskTimer);
        this.interfaceMembershipManager = interfaceMembershipManager;
        this.groupAddress = groupAddress;
        this.retransmissionCount = retransmissionCount;
        this.mode = currentMode;
        this.sourceSet = new HashSet<InetAddress>(currentSourceSet);
        this.modeChangeTransmissionsRemaining = retransmissionCount;
        this.sourceChangeTransmissionsRemaining = 0;
    }

    /**
     * 
     * @param taskTimer
     * @param interfaceMembershipManager
     * @param groupAddress
     * @param retransmissionCount
     * @param currentMode
     * @param currentSourceSet
     * @param newSourceSet
     */
    StateChangeReportTask(final Timer taskTimer,
                                 final InterfaceMembershipManager interfaceMembershipManager,
                                 final InetAddress groupAddress,
                                 final int retransmissionCount,
                                 final SourceFilter.Mode currentMode,
                                 final HashSet<InetAddress> currentSourceSet,
                                 final HashSet<InetAddress> newSourceSet) {
        this(taskTimer, interfaceMembershipManager, groupAddress, retransmissionCount, currentMode, currentSourceSet);
        this.sourceChangeTransmissionsRemaining = retransmissionCount;
        this.modeChangeTransmissionsRemaining = 0;
        updateSourceSet(newSourceSet);
    }

    /**
     * 
     * @param newSourceSet
     */
    synchronized void updateSourceSet(final HashSet<InetAddress> newSourceSet) {

        /*
         * Old State New State State-Change Record Sent
         * --------- --------- ------------------------
         * INCLUDE (A) INCLUDE (B) ALLOW (B-A), BLOCK (A-B)
         * EXCLUDE (A) EXCLUDE (B) ALLOW (A-B), BLOCK (B-A)
         */

        if (this.mode == SourceFilter.Mode.INCLUDE) {
            // ALLOW_NEW_SOURCES set is generated by subtracting the old set from the new
            // set
            this.allowNewSources = new HashSet<InetAddress>(newSourceSet);
            this.allowNewSources.removeAll(this.sourceSet);

            // BLOCK_OLD_SOURCES set is generated by subtracting the new set from the old
            // set
            this.blockOldSources = new HashSet<InetAddress>(this.sourceSet);
            this.blockOldSources.removeAll(newSourceSet);
        }
        else {
            // ALLOW_NEW_SOURCES set is generated by subtracting the new set from the old
            // set
            this.allowNewSources = new HashSet<InetAddress>(this.sourceSet);
            this.allowNewSources.removeAll(newSourceSet);

            // BLOCK_OLD_SOURCES set is generated by subtracting the old set from the new
            // set
            this.blockOldSources = new HashSet<InetAddress>(newSourceSet);
            this.blockOldSources.removeAll(this.sourceSet);
        }

        Logger logger = InterfaceMembershipManager.logger;
        Log log = new Log(this.interfaceMembershipManager);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("generating groups state change report for filter mode=" + this.mode.toString()));
            logger.fine(log.msg(" ----> ALLOW_NEW_SOURCES"));
            for (InetAddress address : this.allowNewSources) {
                logger.fine(log.msg("  " + Logging.address(address)));
            }
            logger.fine(log.msg(" <---- ALLOW_NEW_SOURCES"));
            logger.fine(log.msg(" ----> BLOCK_OLD_SOURCES"));
            for (InetAddress address : this.blockOldSources) {
                logger.fine(log.msg("  " + Logging.address(address)));
            }
            logger.fine(log.msg(" <---- BLOCK_OLD_SOURCES"));
        }

        this.sourceChangeTransmissionsRemaining = this.retransmissionCount;
    }

    /**
     * 
     * @param newMode
     * @param newSourceSet
     */
    synchronized void updateFilterMode(final SourceFilter.Mode newMode, final HashSet<InetAddress> newSourceSet) {
        if (this.mode != newMode) {
            this.sourceSet = newSourceSet;
            this.allowNewSources.clear();
            this.blockOldSources.clear();
            this.modeChangeTransmissionsRemaining = this.retransmissionCount;
            this.sourceChangeTransmissionsRemaining = 0;
        }
    }

    @Override
    public synchronized void run() {
        if (this.modeChangeTransmissionsRemaining > 0) {
            this.modeChangeTransmissionsRemaining--;
            this.interfaceMembershipManager
                            .sendGroupFilterModeChangeReport(this.groupAddress,
                                                             this.mode,
                                                             this.sourceSet,
                                                             this.modeChangeTransmissionsRemaining
                                                                             + this.sourceChangeTransmissionsRemaining);
        }
        else if (this.sourceChangeTransmissionsRemaining > 0) {
            this.sourceChangeTransmissionsRemaining--;
            this.interfaceMembershipManager.sendGroupSourceSetChangeReport(this.groupAddress,
                                                                           this.allowNewSources,
                                                                           this.blockOldSources,
                                                                           this.sourceChangeTransmissionsRemaining);
        }
        else {
            cancel();
        }
    }

}