package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InterfaceMembershipManager.java [org.js4ms.jsdk:amt]
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


import java.io.IOException;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.amt.message.GroupMembershipRecord;
import org.js4ms.amt.proxy.MembershipQuery;
import org.js4ms.amt.proxy.MembershipReport;
import org.js4ms.amt.proxy.SourceFilter;
import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.LoggableBase;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.OutputChannel;




final class InterfaceMembershipManager
                extends LoggableBase {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(InterfaceMembershipManager.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    private final Log log = new Log(this);

    /**
     * Contents describe the current multicast reception state of this interface.
     * This state is used to generate group records in membership reports sent in
     * response to membership queries.
     */
    private final HashMap<InetAddress, SourceFilter> interfaceReceptionState = new HashMap<InetAddress, SourceFilter>();

    private final HashMap<InetAddress, StateChangeReportTask> pendingStateChangeReports = new HashMap<InetAddress, StateChangeReportTask>();

    private final HashMap<InetAddress, GroupQueryReportTask> pendingGroupQueryReports = new HashMap<InetAddress, GroupQueryReportTask>();

    private GeneralQueryReportTimer pendingGeneralQueryReport = null;

    private OutputChannel<MembershipQuery> incomingQueryChannel;

    private OutputChannel<MembershipReport> outgoingReportChannel;

    private final Timer taskTimer;

    private int robustnessVariable = 2;

    private int unsolicitedReportIntervalMs = 125000; // Default query interval

    private boolean useRandomDelay = false;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param taskTimer
     */
    InterfaceMembershipManager(final Timer taskTimer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.InterfaceMembershipManager", taskTimer));
        }

        this.taskTimer = taskTimer;

        this.pendingGeneralQueryReport = new GeneralQueryReportTimer(taskTimer, this);

        this.incomingQueryChannel = new OutputChannel<MembershipQuery>() {

            @Override
            public void send(MembershipQuery message, int milliseconds) throws IOException, InterruptedException {
                handle(message);
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public final Logger getLogger() {
        return logger;
    }

    /**
     * @return
     */
    OutputChannel<MembershipQuery> getIncomingQueryChannel() {
        return this.incomingQueryChannel;
    }

    /**
     * @param outgoingReportChannel
     */
    void setOutgoingReportChannel(final OutputChannel<MembershipReport> outgoingReportChannel) {
        this.outgoingReportChannel = outgoingReportChannel;
    }

    /**
     * @param groupAddress
     * @throws IOException
     * @throws InterruptedException
     */
    void join(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.join",
                                        Logging.address(groupAddress)));
        }

        synchronized (this.interfaceReceptionState) {

            SourceFilter filter = this.interfaceReceptionState.get(groupAddress);

            if (filter == null) {
                filter = new SourceFilter(groupAddress);
                this.interfaceReceptionState.put(groupAddress, filter);
            }

            HashSet<InetAddress> oldSourceSet = new HashSet<InetAddress>(filter.getSourceSet());
            SourceFilter.Mode oldFilterMode = filter.getMode();

            filter.join();

            updateInterfaceGroupState(oldFilterMode, oldSourceSet, filter);
        }
    }

    /**
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     * @throws InterruptedException
     */
    void join(final InetAddress groupAddress,
              final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.join",
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        synchronized (this.interfaceReceptionState) {

            SourceFilter filter = this.interfaceReceptionState.get(groupAddress);

            if (filter == null) {
                filter = new SourceFilter(groupAddress);
                this.interfaceReceptionState.put(groupAddress, filter);
            }

            HashSet<InetAddress> oldSourceSet = new HashSet<InetAddress>(filter.getSourceSet());
            SourceFilter.Mode oldFilterMode = filter.getMode();

            filter.join(sourceAddress);

            updateInterfaceGroupState(oldFilterMode, oldSourceSet, filter);
        }
    }

    /**
     * @param groupAddress
     * @throws IOException
     */
    void leave(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.leave",
                                        Logging.address(groupAddress)));
        }

        synchronized (this.interfaceReceptionState) {

            SourceFilter filter = this.interfaceReceptionState.get(groupAddress);

            if (filter != null) {
                HashSet<InetAddress> oldSourceSet = filter.getSourceSet();
                SourceFilter.Mode oldFilterMode = filter.getMode();

                filter.leave();

                updateInterfaceGroupState(oldFilterMode, oldSourceSet, filter);
            }
        }
    }

    /**
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    void leave(final InetAddress groupAddress,
               final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.leave",
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        synchronized (this.interfaceReceptionState) {

            SourceFilter filter = this.interfaceReceptionState.get(groupAddress);

            if (filter != null) {
                HashSet<InetAddress> oldSourceSet = new HashSet<InetAddress>(filter.getSourceSet());
                SourceFilter.Mode oldFilterMode = filter.getMode();

                filter.leave(sourceAddress);

                updateInterfaceGroupState(oldFilterMode, oldSourceSet, filter);
            }
        }
    }

    /**
     * @throws IOException
     */
    final void leave() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.leave"));
        }

        synchronized (this.interfaceReceptionState) {

            HashSet<InetAddress> groupSet = new HashSet<InetAddress>(this.interfaceReceptionState.keySet());
            for (InetAddress groupAddress : groupSet) {
                leave(groupAddress);
            }
        }
    }

    /**
     * 
     */
    final void shutdown() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.shutdown"));
        }

        HashSet<InetAddress> groupSet = new HashSet<InetAddress>(this.interfaceReceptionState.keySet());
        for (InetAddress groupAddress : groupSet) {
            try {
                leave(groupAddress);
            }
            catch (IOException e) {
                logger.fine(this.log.msg("attempt to leave group=" +
                                         Logging.address(groupAddress) +
                                         " failed with exception - " +
                                         e.getClass().getName() + ":" + e.getMessage()));

                e.printStackTrace();
                // Continue on and try to leave the rest
            }
        }
    }

    /**
     * From <a href="http://www.rfc-editor.org/rfc/rfc3376.txt">[RFC-3376]</a>
     * 
     * <pre>
     *      The general rules for deriving the per-interface state from the
     *      per-socket state are as follows:  For each distinct (interface,
     *      multicast-address) pair that appears in any socket state, a per-
     *      interface record is created for that multicast address on that
     *      interface.  Considering all socket records containing the same
     *      (interface, multicast-address) pair,
     * 
     *    o if *any* such record has a filter mode of EXCLUDE, then the filter
     *      mode of the interface record is EXCLUDE, and the source list of the
     *      interface record is the intersection of the source lists of all
     *      socket records in EXCLUDE mode, minus those source addresses that
     *      appear in any socket record in INCLUDE mode.  For example, if the
     *      socket records for multicast address m on interface i are:
     * 
     *         from socket s1:  ( i, m, EXCLUDE, {a, b, c, d} )
     *         from socket s2:  ( i, m, EXCLUDE, {b, c, d, e} )
     *         from socket s3:  ( i, m, INCLUDE, {d, e, f} )
     * 
     *      then the corresponding interface record on interface i is:
     * 
     *                          ( m, EXCLUDE, {b, c} )
     * 
     *      If a fourth socket is added, such as:
     * 
     *         from socket s4:  ( i, m, EXCLUDE, {} )
     * 
     *      then the interface record becomes:
     * 
     *                          ( m, EXCLUDE, {} )
     * 
     *    o if *all* such records have a filter mode of INCLUDE, then the
     *      filter mode of the interface record is INCLUDE, and the source list
     *      of the interface record is the union of the source lists of all the
     *      socket records.  For example, if the socket records for multicast
     *      address m on interface i are:
     * 
     *         from socket s1:  ( i, m, INCLUDE, {a, b, c} )
     *         from socket s2:  ( i, m, INCLUDE, {b, c, d} )
     *         from socket s3:  ( i, m, INCLUDE, {e, f} )
     * 
     *      then the corresponding interface record on interface i is:
     * 
     *                          ( m, INCLUDE, {a, b, c, d, e, f} )
     * 
     *      An implementation MUST NOT use an EXCLUDE interface record to
     *      represent a group when all sockets for this group are in INCLUDE
     *      state.  If system resource limits are reached when an interface
     *      state source list is calculated, an error MUST be returned to the
     *      application which requested the operation.
     * </pre>
     */
    private void updateInterfaceGroupState(final SourceFilter.Mode oldFilterMode,
                                           final HashSet<InetAddress> oldSourceSet,
                                           final SourceFilter filter) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.updateInterfaceGroupState",
                                        oldFilterMode,
                                        oldSourceSet,
                                        filter));
        }

        // Block threads calling other methods from changing the interface reception state
        // until we have updated the state and generated a group state change report
        synchronized (this.interfaceReceptionState) {

            InetAddress groupAddress = filter.getGroupAddress();
            SourceFilter.Mode newFilterMode = filter.getMode();
            HashSet<InetAddress> newSourceSet = filter.getSourceSet();

            if (newFilterMode == SourceFilter.Mode.INCLUDE && newSourceSet.isEmpty()) {
                // The new group state does not include any sources - remove the filter
                this.interfaceReceptionState.remove(groupAddress);
                if (this.interfaceReceptionState.isEmpty()) {
                    this.pendingGeneralQueryReport.cancel();
                }
            }

            // Block access to the pending reports until the group state change report is
            // created/updated
            synchronized (this.pendingStateChangeReports) {

                // Look for pending group state change report
                StateChangeReportTask stateChangeReport = this.pendingStateChangeReports.get(groupAddress);

                if (stateChangeReport == null) {

                    // There is no pending report - create a new one
                    int retransmissionCount = this.robustnessVariable - 1;

                    if (newFilterMode != oldFilterMode) {
                        // Generate filter mode change report
                        stateChangeReport = new StateChangeReportTask(this.taskTimer,
                                                                      this,
                                                                      groupAddress,
                                                                      retransmissionCount,
                                                                      newFilterMode,
                                                                      newSourceSet);
                    }
                    else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(this.log.msg("generating groups state change report"));
                            logger.fine(this.log.msg("----> Old Source Set for " + Logging.address(groupAddress)));
                            for (InetAddress address : oldSourceSet) {
                                logger.fine(this.log.msg(" " + Logging.address(address)));
                            }
                            logger.fine(this.log.msg("<----"));
                            logger.fine(this.log.msg("----> New Source Set for " + Logging.address(groupAddress)));
                            for (InetAddress address : newSourceSet) {
                                logger.fine(this.log.msg(" " + Logging.address(address)));
                            }
                            logger.fine(this.log.msg("<----"));
                        }

                        // Generate source set change report
                        stateChangeReport = new StateChangeReportTask(this.taskTimer,
                                                                      this,
                                                                      groupAddress,
                                                                      retransmissionCount,
                                                                      newFilterMode,
                                                                      oldSourceSet,
                                                                      newSourceSet);
                    }

                    if (retransmissionCount > 1) {

                        // This is supposed to be random (0,interval) but can't do that
                        // with TimerTasks
                        long taskPeriod = this.unsolicitedReportIntervalMs;

                        // We delay one task period because we will call run()
                        // immediately.
                        stateChangeReport.schedule(taskPeriod, taskPeriod);

                        this.pendingStateChangeReports.put(groupAddress, stateChangeReport);
                    }
                }
                else {
                    // There is a pending report - update the group change sets and reset
                    // the retransmission count
                    stateChangeReport.updateSourceSet(newSourceSet);
                }

                // Send the first state change report immediately
                stateChangeReport.run();
            }
        }
    }

    /**
     * From <a href="http://www.rfc-editor.org/rfc/rfc3376.txt">[RFC-3376]</a>
     * 
     * <pre>
     * 5.2. Action on Reception of a Query
     * 
     *    When a system receives a Query, it does not respond immediately.
     *    Instead, it delays its response by a random amount of time, bounded
     *    by the Max Resp Time value derived from the Max Resp Code in the
     *    received Query message.  A system may receive a variety of Queries on
     *    different interfaces and of different kinds (e.g., General Queries,
     *    Group-Specific Queries, and Group-and-Source-Specific Queries), each
     *    of which may require its own delayed response.
     * 
     *    Before scheduling a response to a Query, the system must first
     *    consider previously scheduled pending responses and in many cases
     *    schedule a combined response.  Therefore, the system must be able to
     *    maintain the following state:
     * 
     *    o A timer per interface for scheduling responses to General Queries.
     * 
     *    o A per-group and interface timer for scheduling responses to Group-
     *      Specific and Group-and-Source-Specific Queries.
     * 
     *    o A per-group and interface list of sources to be reported in the
     *      response to a Group-and-Source-Specific Query.
     * 
     *    When a new Query with the Router-Alert option arrives on an
     *    interface, provided the system has state to report, a delay for a
     *    response is randomly selected in the range (0, [Max Resp Time]) where
     *    Max Resp Time is derived from Max Resp Code in the received Query
     *    message.  The following rules are then used to determine if a Report
     *    needs to be scheduled and the type of Report to schedule.  The rules
     *    are considered in order and only the first matching rule is applied.
     * 
     *    1. If there is a pending response to a previous General Query
     *       scheduled sooner than the selected delay, no additional response
     *       needs to be scheduled.
     * 
     *    2. If the received Query is a General Query, the interface timer is
     *       used to schedule a response to the General Query after the
     *       selected delay.  Any previously pending response to a General
     *       Query is canceled.
     * 
     *    3. If the received Query is a Group-Specific Query or a Group-and-
     *       Source-Specific Query and there is no pending response to a
     *       previous Query for this group, then the group timer is used to
     *       schedule a report.  If the received Query is a Group-and-Source-
     *       Specific Query, the list of queried sources is recorded to be used
     *       when generating a response.
     * 
     *    4. If there already is a pending response to a previous Query
     *       scheduled for this group, and either the new Query is a Group-
     *       Specific Query or the recorded source-list associated with the
     *       group is empty, then the group source-list is cleared and a single
     *       response is scheduled using the group timer.  The new response is
     *       scheduled to be sent at the earliest of the remaining time for the
     *       pending report and the selected delay.
     * 
     *    5. If the received Query is a Group-and-Source-Specific Query and
     *       there is a pending response for this group with a non-empty
     *       source-list, then the group source list is augmented to contain
     *       the list of sources in the new Query and a single response is
     *       scheduled using the group timer.  The new response is scheduled to
     *       be sent at the earliest of the remaining time for the pending
     *       report and the selected delay.
     * </pre>
     * 
     * @param queryMessage
     */
    void handle(final MembershipQuery queryMessage) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.handle", queryMessage));
        }

        this.unsolicitedReportIntervalMs = queryMessage.getQueryInterval();

        this.robustnessVariable = queryMessage.getRobustnessVariable();

        if (!this.useRandomDelay) {
            if (queryMessage.isGeneralQuery()) {
                sendGeneralQueryResponse();
            }
            else {
                sendGroupQueryResponse(queryMessage.getGroupAddress(), queryMessage.getSourceAddresses());
            }
        }
        else {

            // Response must be delayed by a random amount of time within
            // the range (0,maximum response delay) as specified in the query.
            long taskDelay = Math.round(Math.random() * queryMessage.getMaximumResponseDelay());

            if (queryMessage.isGeneralQuery()) {

                // Schedule the general query if none is pending, or reschedule pending
                // General Query

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(this.log.msg("rescheduling general query report delay=" + taskDelay + "ms"));
                }

                this.pendingGeneralQueryReport.schedule(taskDelay);
            }
            else {
                // Query is group-specific or group-source-specific

                // Check for pending general query response
                // No need to send a group query if a general query is already scheduled
                // for earlier delivery.
                if (this.pendingGeneralQueryReport.getTimeRemaining() > taskDelay) {

                    synchronized (this.pendingGroupQueryReports) {

                        // Check for pending group query response
                        GroupQueryReportTask response = this.pendingGroupQueryReports.get(queryMessage.getGroupAddress());
                        if (response == null) {
                            response = new GroupQueryReportTask(this.taskTimer,
                                                                this,
                                                                queryMessage.getGroupAddress(),
                                                                queryMessage.getSourceAddresses());
                            this.pendingGroupQueryReports.put(queryMessage.getGroupAddress(), response);
                            response.schedule(taskDelay);
                        }
                        else {
                            // There may be a pending response
                            // Get the time remaining, if any, for the existing response
                            // (will be MAX_VALUE if not scheduled)
                            long timeRemaining = response.getTimeRemaining();

                            // Set delay to earliest of the two
                            taskDelay = timeRemaining < taskDelay ? timeRemaining : taskDelay;

                            // Cancel the response so we can update the source set and
                            // reschedule with a new session token
                            response.cancel();
                            response.updateQuerySourceSet(queryMessage.getSourceAddresses());
                            response.schedule(taskDelay);
                        }
                    }
                }
            }
        }
    }

    /**
     * @param groupAddress
     * @param type
     * @param sourceSet
     */
    void sendGroupMembershipReport(final InetAddress groupAddress,
                                   final GroupMembershipRecord.Type type,
                                   final HashSet<InetAddress> sourceSet) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.sendGroupMembershipReport",
                                        Logging.address(groupAddress),
                                        type,
                                        sourceSet));
        }

        MembershipReport report = new MembershipReport();
        report.addRecord(new GroupMembershipRecord(groupAddress, type, sourceSet));

        try {
            this.outgoingReportChannel.send(report, Integer.MAX_VALUE);
        }
        catch (PortUnreachableException e) {
            // TODO:
        }
        catch (IOException e) {
            // TODO
            throw new Error(e);
        }
        catch (InterruptedException e) {
        }
    }

    /**
     * @param groupAddress
     * @param mode
     * @param sourceSet
     * @param transmissionsRemaining
     */
    void sendGroupFilterModeChangeReport(final InetAddress groupAddress,
                                         final SourceFilter.Mode mode,
                                         final HashSet<InetAddress> sourceSet,
                                         final int transmissionsRemaining) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.sendGroupFilterModeChangeReport",
                                        Logging.address(groupAddress),
                                        mode,
                                        sourceSet,
                                        transmissionsRemaining));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("sending membership report for group filter mode change"));
        }

        GroupMembershipRecord.Type type = (mode == SourceFilter.Mode.INCLUDE ?
                        GroupMembershipRecord.Type.CHANGE_TO_INCLUDE_MODE :
                        GroupMembershipRecord.Type.CHANGE_TO_EXCLUDE_MODE);

        sendGroupMembershipReport(groupAddress, type, sourceSet);

        if (transmissionsRemaining == 0) {
            synchronized (this.pendingStateChangeReports) {
                this.pendingStateChangeReports.remove(groupAddress);
            }
        }
    }

    /**
     * @param groupAddress
     * @param allowNewSources
     * @param blockOldSources
     * @param transmissionsRemaining
     */
    void sendGroupSourceSetChangeReport(final InetAddress groupAddress,
                                        final HashSet<InetAddress> allowNewSources,
                                        final HashSet<InetAddress> blockOldSources,
                                        final int transmissionsRemaining) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.sendGroupStateChangeReport",
                                        Logging.address(groupAddress),
                                        allowNewSources,
                                        blockOldSources,
                                        transmissionsRemaining));
        }

        if (!allowNewSources.isEmpty() || !blockOldSources.isEmpty()) {

            MembershipReport report = new MembershipReport();

            if (!allowNewSources.isEmpty()) {
                report.addRecord(new GroupMembershipRecord(groupAddress,
                                                           GroupMembershipRecord.Type.ALLOW_NEW_SOURCES,
                                                           allowNewSources));
            }

            if (!blockOldSources.isEmpty()) {
                report.addRecord(new GroupMembershipRecord(groupAddress,
                                                           GroupMembershipRecord.Type.BLOCK_OLD_SOURCES,
                                                           blockOldSources));
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("sending membership report for group source set change"));
            }

            try {
                this.outgoingReportChannel.send(report, Integer.MAX_VALUE);
            }
            catch (PortUnreachableException e) {
                // TODO
            }
            catch (IOException e) {
                // TODO
                throw new Error(e);
            }
            catch (InterruptedException e) {
            }
        }

        if (transmissionsRemaining == 0) {
            synchronized (this.pendingStateChangeReports) {
                this.pendingStateChangeReports.remove(groupAddress);
            }
        }
    }

    /**
     * 
     */
    void sendGeneralQueryResponse() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.sendGeneralQueryResponse"));
        }

        MembershipReport report = new MembershipReport();

        for (SourceFilter filter : this.interfaceReceptionState.values()) {

            GroupMembershipRecord.Type type = (filter.getMode() == SourceFilter.Mode.INCLUDE ?
                            GroupMembershipRecord.Type.MODE_IS_INCLUDE :
                            GroupMembershipRecord.Type.MODE_IS_EXCLUDE);

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(this.log.msg("adding record type=" + type.name() + " group="
                                          + Logging.address(filter.getGroupAddress()) + " source-count="
                                          + filter.getSourceSet().size()));
            }

            report.addRecord(new GroupMembershipRecord(filter.getGroupAddress(), type, filter.getSourceSet()));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("sending membership report for general query"));
        }

        try {
            this.outgoingReportChannel.send(report, Integer.MAX_VALUE);
        }
        catch (PortUnreachableException e) {
            // TODO
        }
        catch (IOException e) {
            // TODO
            throw new Error(e);
        }
        catch (InterruptedException e) {
        }

    }

    /**
     * From <a href="http://www.rfc-editor.org/rfc/rfc3376.txt">[RFC-3376]</a>
     * 
     * <pre>
     *  If the expired timer is a group timer and the list of recorded
     *  sources for that group is non-empty (i.e., it is a pending
     *  response to a Group-and-Source-Specific Query), then if and only
     *  if the interface has reception state for that group address, the
     *  contents of the responding Current-State Record is determined from
     *  the interface state and the pending response record, as specified
     *  in the following table:
     * 
     *                          set of sources in the
     *       interface state   pending response record   Current-State Record
     *       ---------------   -----------------------   --------------------
     *        INCLUDE (A)                B                   IS_IN (A*B)
     *        EXCLUDE (A)                B                   IS_IN (B-A)
     * 
     *  If the resulting Current-State Record has an empty set of source
     *  addresses, then no response is sent.
     * </pre>
     * 
     * @param groupAddress
     * @param sourceSet
     */
    void sendGroupQueryResponse(final InetAddress groupAddress,
                                final HashSet<InetAddress> querySourceSet) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("InterfaceMembershipManager.sendGroupMembershipReport",
                                        Logging.address(groupAddress),
                                        querySourceSet));
        }

        HashSet<InetAddress> responseSourceSet;

        synchronized (this.interfaceReceptionState) {

            SourceFilter filter = this.interfaceReceptionState.get(groupAddress);

            // Don't send a response if there is no filter - there is no reception state
            // for group
            if (filter != null) {
                if (querySourceSet == null || querySourceSet.isEmpty()) {

                    // Group-specific query - report the filter mode and source set for
                    // the group
                    responseSourceSet = filter.getSourceSet();

                    GroupMembershipRecord.Type type = (filter.getMode() == SourceFilter.Mode.INCLUDE ?
                                    GroupMembershipRecord.Type.MODE_IS_INCLUDE :
                                    GroupMembershipRecord.Type.MODE_IS_EXCLUDE);

                    sendGroupMembershipReport(groupAddress, type, responseSourceSet);
                }
                else {
                    responseSourceSet = new HashSet<InetAddress>(querySourceSet);
                    if (filter.getMode() == SourceFilter.Mode.INCLUDE) {
                        // (A*B)
                        responseSourceSet.retainAll(filter.getSourceSet());
                    }
                    else {
                        // (B-A)
                        responseSourceSet.removeAll(filter.getSourceSet());
                    }

                    // Only send a report if there are sources in the response
                    if (!responseSourceSet.isEmpty()) {

                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(this.log.msg("sending membership report in response to group-specific query"));
                        }

                        // Source query responses are sent with MODE_IS_INCLUDE
                        sendGroupMembershipReport(groupAddress, GroupMembershipRecord.Type.MODE_IS_INCLUDE, responseSourceSet);
                    }
                }
            }
        }
    }
}
