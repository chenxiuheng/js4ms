package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ChannelMembershipManager.java [org.js4ms.jsdk:amt]
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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.BoundException;
import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.LoggableBase;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.MultiIOException;
import org.js4ms.io.channel.MessageKeyExtractor;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelMap;
import org.js4ms.io.channel.OutputChannelTee;
import org.js4ms.io.net.UdpDatagram;
import org.js4ms.ip.Precondition;




final class ChannelMembershipManager
                extends LoggableBase {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(ChannelMembershipManager.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    private final Log log = new Log(this);

    private final AmtIPInterface ipInterface;

    private final MessageKeyExtractor<UdpDatagram> groupExtractor;

    private final MessageKeyExtractor<UdpDatagram> sourceExtractor;

    private final MessageKeyExtractor<UdpDatagram> portExtractor;

    /**
     * Base channel selector used to track reception state for each group, source, and
     * port.
     * The hierarchy of selectors and channels differs for ASM and SSM multicast groups.
     * <p>
     * ASM example:
     * 
     * <pre>
     * Group Channel Map-+->Port Channel Map-+->Channel Tee--->Output Channel
     *                   |                   |
     *                   |                   +->Channel Tee-+->Output Channel
     *                   |                                  |
     *                   |                                  +->Output Channel
     *                   +->Port Channel Map--->Channel Tee--->Output Channel
     * </pre>
     * <p>
     * SSM example:
     * 
     * <pre>
     * Group Channel Map-+->Source Channel Map-+->Port Channel Map--->Channel Tee--->Output Channel
     *                   |                     |
     *                   |                     +->Port Channel Map--->Channel Tee-+->Output Channel
     *                   |                                                        |
     *                   |                                                        +->Output Channel
     *                   +->Source Channel Map--->Port Channel Map-+->Channel Tee--->Output Channel
     *                                                             |
     *                                                             +->Channel Tee--->Output Channel
     * </pre>
     */
    private final OutputChannelMap<UdpDatagram> groupMap;

    /**
     * Channel that receives UdpDatagrams for dispatch to application-side output
     * channels.
     */
    private final OutputChannel<UdpDatagram> dispatchChannel;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param ipInterface
     */
    ChannelMembershipManager(final AmtIPInterface ipInterface) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.ChannelMembershipManager", ipInterface));
        }

        this.ipInterface = ipInterface;

        this.groupExtractor = new MessageKeyExtractor<UdpDatagram>() {

            @Override
            public InetAddress getKey(UdpDatagram message) {
                return message.getDestinationInetAddress();
            }
        };

        this.sourceExtractor = new MessageKeyExtractor<UdpDatagram>() {

            @Override
            public InetAddress getKey(UdpDatagram message) {
                return message.getSourceInetAddress();
            }
        };

        this.portExtractor = new MessageKeyExtractor<UdpDatagram>() {

            @Override
            public Integer getKey(UdpDatagram message) {
                return message.getDestinationPort();
            }
        };

        final ChannelMembershipManager manager = this;

        this.dispatchChannel = new OutputChannel<UdpDatagram>() {

            @Override
            public void send(UdpDatagram message, int milliseconds) throws IOException, InterruptedException {
                manager.send(message, milliseconds);
            }

            @Override
            public void close() {
            }
        };

        this.groupMap = new OutputChannelMap<UdpDatagram>(this.groupExtractor);

    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * @return
     */
    OutputChannel<UdpDatagram> getDispatchChannel() {
        return this.dispatchChannel;
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    void join(final OutputChannel<UdpDatagram> pushChannel,
              final InetAddress groupAddress,
              final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.join", pushChannel, Logging.address(groupAddress),
                                        port));
        }

        Precondition.checkASMMulticastAddress(groupAddress);

        synchronized (this.groupMap) {
            OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
            if (portMap == null) {
                portMap = new OutputChannelMap<UdpDatagram>(this.portExtractor);
                this.groupMap.put(groupAddress, portMap);
                OutputChannelTee<UdpDatagram> tee = new OutputChannelTee<UdpDatagram>();
                portMap.put(port, tee);
                tee.add(pushChannel);
                this.ipInterface.join(groupAddress);
            }
            else {
                OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                if (tee == null) {
                    tee = new OutputChannelTee<UdpDatagram>();
                    portMap.put(port, tee);
                }
                tee.add(pushChannel);
            }
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    void join(final OutputChannel<UdpDatagram> pushChannel,
              final InetAddress groupAddress,
              final InetAddress sourceAddress,
              final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.join",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress),
                                        port));
        }

        Precondition.checkMulticastAddress(groupAddress);
        Precondition.checkAddresses(groupAddress, sourceAddress);

        synchronized (this.groupMap) {
            OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
            if (sourceMap == null) {
                sourceMap = new OutputChannelMap<UdpDatagram>(this.sourceExtractor);
                this.groupMap.put(groupAddress, sourceMap);
                OutputChannelMap<UdpDatagram> portMap = new OutputChannelMap<UdpDatagram>(this.portExtractor);
                sourceMap.put(sourceAddress, portMap);
                OutputChannelTee<UdpDatagram> tee = new OutputChannelTee<UdpDatagram>();
                tee.add(pushChannel);
                portMap.put(port, tee);
                this.ipInterface.join(groupAddress, sourceAddress);
            }
            else {
                OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) sourceMap.get(sourceAddress);
                if (portMap == null) {
                    portMap = new OutputChannelMap<UdpDatagram>(this.portExtractor);
                    sourceMap.put(sourceAddress, portMap);
                    OutputChannelTee<UdpDatagram> tee = new OutputChannelTee<UdpDatagram>();
                    portMap.put(port, tee);
                    tee.add(pushChannel);
                }
                else {
                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                    if (tee == null) {
                        tee = new OutputChannelTee<UdpDatagram>();
                        portMap.put(port, tee);
                    }
                    tee.add(pushChannel);
                }
            }
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    void leave(final OutputChannel<UdpDatagram> pushChannel,
               final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.leave", pushChannel, Logging.address(groupAddress)));
        }

        Precondition.checkMulticastAddress(groupAddress);

        synchronized (this.groupMap) {
            // Get the source or port selector for this group
            OutputChannelMap<UdpDatagram> entryMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
            if (entryMap != null) {
                // Look for the channel under all entries
                Iterator<Object> entryIter = entryMap.getKeys().iterator();
                while (entryIter.hasNext()) {
                    Object entry = entryIter.next();
                    if (entry instanceof InetAddress) {
                        InetAddress sourceAddress = (InetAddress) entry;
                        // Get the port selector for the source
                        OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) entryMap.get(sourceAddress);
                        if (portMap != null) {
                            // Look for the channel under all of the port entries
                            Iterator<Object> portIter = portMap.getKeys().iterator();
                            while (portIter.hasNext()) {
                                int port = (Integer) portIter.next();
                                // Get the splitter for this port
                                OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                                // Remove the channel (even though it may not be there).
                                tee.remove(pushChannel);
                                if (tee.isEmpty()) {
                                    // No more channels associated with this port - remove
                                    // the port entry
                                    portIter.remove();
                                    if (portMap.isEmpty()) {
                                        // No more ports associated with this source -
                                        // remove the source entry
                                        entryIter.remove();
                                        if (entryMap.isEmpty()) {
                                            // No more sources associated with this group
                                            // - remove the group entry
                                            this.groupMap.remove(groupAddress);
                                        }
                                        // No channels are left in this source group -
                                        // update the interface reception state
                                        this.ipInterface.leave(groupAddress, sourceAddress);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        int port = (Integer) entry;
                        // Get the splitter for this port
                        OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) entryMap.get(port);
                        // Remove the channel (even though it may not be there).
                        tee.remove(pushChannel);
                        if (tee.isEmpty()) {
                            // No more channels associated with this port - remove the
                            // port entry
                            entryIter.remove();
                            if (entryMap.isEmpty()) {
                                // No more ports associated with this group - remove the
                                // group entry
                                this.groupMap.remove(groupAddress);
                                // No channels are left in this group - update the
                                // interface reception state
                                this.ipInterface.leave(groupAddress);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    void leave(final OutputChannel<UdpDatagram> pushChannel,
               final InetAddress groupAddress,
               final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.leave", pushChannel, Logging.address(groupAddress),
                                        port));
        }

        Precondition.checkMulticastAddress(groupAddress);

        synchronized (this.groupMap) {
            OutputChannelMap<UdpDatagram> entryMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
            if (entryMap != null) {
                // Look for the channel under all entries
                Iterator<Object> entryIter = entryMap.getKeys().iterator();
                while (entryIter.hasNext()) {
                    Object entry = entryIter.next();
                    if (entry instanceof InetAddress) {
                        // Entry map is a source map
                        InetAddress sourceAddress = (InetAddress) entry;
                        // Get the port selector for the source
                        OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) entryMap.get(sourceAddress);
                        if (portMap != null) {
                            // Get the splitter for this port
                            OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                            // Remove the channel (even though it may not be there).
                            tee.remove(pushChannel);
                            if (tee.isEmpty()) {
                                // No more channels associated with this port - remove the
                                // port entry
                                portMap.remove(port);
                                if (portMap.isEmpty()) {
                                    // No more ports associated with this source - remove
                                    // the source entry
                                    entryIter.remove();
                                    if (entryMap.isEmpty()) {
                                        // No more sources associated with this group -
                                        // remove the group entry
                                        this.groupMap.remove(groupAddress);
                                    }
                                    // No channels are left in this source group - update
                                    // the interface reception state
                                    this.ipInterface.leave(groupAddress, sourceAddress);
                                }
                            }
                        }
                    }
                    else {
                        // Get the splitter for this port
                        OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) entryMap.get(port);
                        // Remove the channel (even though it may not be there).
                        tee.remove(pushChannel);
                        if (tee.isEmpty()) {
                            // No more channels associated with this port - remove the
                            // port entry
                            entryMap.remove(port);
                            if (entryMap.isEmpty()) {
                                // No more ports associated with this group - remove the
                                // group entry
                                this.groupMap.remove(groupAddress);
                                // No channels are left in this group - update the
                                // interface reception state
                                this.ipInterface.leave(groupAddress);
                            }
                        }
                        // The entry map was a port map not a source map
                        // so no need to continue iteration
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param channel
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    void leave(final OutputChannel<UdpDatagram> channel,
               final InetAddress groupAddress,
               final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.leave", channel, Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        Precondition.checkMulticastAddress(groupAddress);
        Precondition.checkAddresses(groupAddress, sourceAddress);

        synchronized (this.groupMap) {
            // Get the source selector for the group
            OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
            if (sourceMap != null) {
                // Get the port selector for the source
                OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) sourceMap.get(sourceAddress);
                if (portMap != null) {
                    // Look for the channel under all of the port entries
                    Iterator<Object> portIter = portMap.getKeys().iterator();
                    while (portIter.hasNext()) {
                        int port = (Integer) portIter.next();
                        // Get the splitter for this port
                        OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                        // Remove the channel (even though it may not be there).
                        tee.remove(channel);
                        if (tee.isEmpty()) {
                            // No more channels associated with this port - remove the
                            // port entry
                            portIter.remove();
                            if (portMap.isEmpty()) {
                                // No more ports associated with this source - remove the
                                // source entry
                                sourceMap.remove(sourceAddress);
                                if (sourceMap.isEmpty()) {
                                    // No more sources associated with this group - remove
                                    // the group entry
                                    this.groupMap.remove(groupAddress);
                                }
                                // No channels are left in this source group - update the
                                // interface reception state
                                this.ipInterface.leave(groupAddress, sourceAddress);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    void leave(final OutputChannel<UdpDatagram> pushChannel,
               final InetAddress groupAddress,
               final InetAddress sourceAddress,
               final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.leave",
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress),
                                        port));
        }

        Precondition.checkMulticastAddress(groupAddress);
        Precondition.checkAddresses(groupAddress, sourceAddress);

        synchronized (this.groupMap) {
            // Get the source selector for the group
            OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
            if (sourceMap != null) {
                // Get the port selector for the source
                OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) sourceMap.get(sourceAddress);
                if (portMap != null) {
                    // Get the splitter for the port
                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                    // Remove the channel from the splitter
                    tee.remove(pushChannel);
                    if (tee.isEmpty()) {
                        // No more channels associated with this port - remove the port
                        // entry
                        portMap.remove(port);
                        if (portMap.isEmpty()) {
                            // No more ports associated with this source - remove the
                            // source entry
                            sourceMap.remove(sourceAddress);
                            if (sourceMap.isEmpty()) {
                                // No more sources associated with this group - remove the
                                // group entry
                                this.groupMap.remove(groupAddress);
                            }
                            // No channels are left in this source group - update the
                            // interface reception state
                            this.ipInterface.leave(groupAddress, sourceAddress);
                        }
                    }
                }
            }
        }
    }

    /**
     * @param pushChannel
     * @throws IOException
     */
    void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.leave", pushChannel));
        }

        synchronized (this.groupMap) {
            // Look for the channel under all group entries
            Iterator<Object> groupIter = this.groupMap.getKeys().iterator();
            while (groupIter.hasNext()) {
                InetAddress groupAddress = (InetAddress) groupIter.next();
                // Get the source or port selector for this group
                OutputChannelMap<UdpDatagram> entryMap = (OutputChannelMap<UdpDatagram>) this.groupMap.get(groupAddress);
                if (entryMap != null) {
                    // Look for the channel under all entries
                    Iterator<Object> entryIter = entryMap.getKeys().iterator();
                    while (entryIter.hasNext()) {
                        Object entry = entryIter.next();
                        if (entry instanceof InetAddress) {
                            InetAddress sourceAddress = (InetAddress) entry;
                            // Get the port selector for the source
                            OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>) entryMap.get(sourceAddress);
                            if (portMap != null) {
                                // Look for the channel under all of the port entries
                                Iterator<Object> portIter = portMap.getKeys().iterator();
                                while (portIter.hasNext()) {
                                    int port = (Integer) portIter.next();
                                    // Get the splitter for this port
                                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) portMap.get(port);
                                    // Remove the channel (even though it may not be
                                    // there).
                                    tee.remove(pushChannel);
                                    if (tee.isEmpty()) {
                                        // No more channels associated with this port -
                                        // remove the port entry
                                        portIter.remove();
                                        if (portMap.isEmpty()) {
                                            // No more ports associated with this source -
                                            // remove the source entry
                                            entryIter.remove();
                                            if (entryMap.isEmpty()) {
                                                // No more sources associated with this
                                                // group - remove the group entry
                                                groupIter.remove();
                                            }
                                            // No channels are left in this source group -
                                            // update the interface reception state
                                            this.ipInterface.leave(groupAddress, sourceAddress);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            int port = (Integer) entry;
                            // Get the splitter for this port
                            OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>) entryMap.get(port);
                            // Remove the channel (even though it may not be there).
                            tee.remove(pushChannel);
                            if (tee.isEmpty()) {
                                // No more channels associated with this port - remove the
                                // port entry
                                entryIter.remove();
                                if (entryMap.isEmpty()) {
                                    // No more ports associated with this group - remove
                                    // the group entry
                                    groupIter.remove();
                                    // No channels are left in this group - update the
                                    // interface reception state
                                    this.ipInterface.leave(groupAddress);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @throws InterruptedException
     */
    void shutdown() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ChannelMembershipManager.shutdown"));
        }

        synchronized (this.groupMap) {
            try {
                this.groupMap.close();
            }
            catch (IOException e) {
                logger.fine(this.log.msg("attempt to close channel group map failed failed with exception - " +
                                         e.getClass().getName() + ":" + e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    /**
     * @param message
     * @param milliseconds
     * @throws InterruptedException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void send(final UdpDatagram message, final int milliseconds) throws InterruptedException, IOException {
        synchronized (this.groupMap) {
            try {
                this.groupMap.send(message, milliseconds);
            }
            catch (IOException e) {
                if (e instanceof MultiIOException) {
                    MultiIOException me = (MultiIOException) e;
                    Iterator<Throwable> iter = me.iterator();
                    while (iter.hasNext()) {
                        Throwable t = iter.next();
                        if (t instanceof BoundException) {
                            BoundException be = (BoundException) t;
                            Object o = be.getObject();
                            if (o instanceof OutputChannel<?>) {
                                if (logger.isLoggable(Level.FINE)) {
                                    Throwable te = be.getThrowable();
                                    logger.fine(this.log.msg("removing channel " + Logging.identify(o) + " due to exception - " +
                                                             te.getClass().getName() + ": " + te.getMessage()));
                                }
                                OutputChannel<UdpDatagram> channel = (OutputChannel<UdpDatagram>) o;
                                leave(channel);
                                return;
                            }
                        }
                    }
                }
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(this.log.msg("closing all multicast channels due to unhandled exception - " +
                                e.getClass().getName() + ": " + e.getMessage()));
                }
                shutdown();
            }
        }
    }

}
