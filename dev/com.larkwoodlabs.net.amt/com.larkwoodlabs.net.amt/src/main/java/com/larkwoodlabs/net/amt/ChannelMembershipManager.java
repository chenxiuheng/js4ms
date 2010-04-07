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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageKeyExtractor;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelMap;
import com.larkwoodlabs.channels.OutputChannelTee;
import com.larkwoodlabs.common.exceptions.BoundException;
import com.larkwoodlabs.common.exceptions.MultiIOException;
import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.LoggableBase;
import com.larkwoodlabs.util.logging.Logging;

final class ChannelMembershipManager
            extends LoggableBase {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(ChannelMembershipManager.class.getName());


    /*-- Member Variables ---------------------------------------------------*/

    /**
     * 
     */
    private final InterfaceMembershipManager interfaceManager;
    
    private final MessageKeyExtractor<UdpDatagram> groupExtractor;
    private final MessageKeyExtractor<UdpDatagram> sourceExtractor;
    private final MessageKeyExtractor<UdpDatagram> portExtractor;


    /**
     * Base channel selector used to track reception state for each group, source, and port.
     * The hierarchy of selectors and channels differs for ASM and SSM multicast groups.
     * <p>ASM example:
     * <pre>
     * Group Channel Map-+->Port Channel Map-+->Channel Tee--->Output Channel
     *                   |                   |
     *                   |                   +->Channel Tee-+->Output Channel
     *                   |                                  |
     *                   |                                  +->Output Channel
     *                   +->Port Channel Map--->Channel Tee--->Output Channel             
     * </pre>
     * <p>SSM example:
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
     * Channel that receives UdpDatagrams for dispatch to application-side output channels.
     */
    private final OutputChannel<UdpDatagram> dispatchChannel;

    
    /*-- Member Functions ---------------------------------------------------*/
  
    /**
     * 
     * @param interfaceManager
     */
    public ChannelMembershipManager(final InterfaceMembershipManager interfaceManager) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.ChannelMembershipManager", interfaceManager));
        }

        this.interfaceManager = interfaceManager;
        
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
            public void close(boolean isCloseAll) {
            }
        };

        this.groupMap = new OutputChannelMap<UdpDatagram>(this.groupExtractor);
        
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * 
     * @return
     */
    OutputChannel<UdpDatagram> getDispatchChannel() {
        return this.dispatchChannel;
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    public void join(final OutputChannel<UdpDatagram> pushChannel,
                     final InetAddress groupAddress,
                     final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.join", pushChannel, Logging.address(groupAddress), port));
        }
        
        Precondition.checkASMMulticastAddress(groupAddress);

        synchronized (this.groupMap) {
            OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
            if (portMap == null) {
                portMap = new OutputChannelMap<UdpDatagram>(this.portExtractor);
                this.groupMap.put(groupAddress, portMap);
                OutputChannelTee<UdpDatagram> tee = new OutputChannelTee<UdpDatagram>();
                portMap.put(port, tee);
                tee.add(pushChannel);
                this.interfaceManager.join(groupAddress);
            }
            else {
                OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                if (tee == null) {
                    tee = new OutputChannelTee<UdpDatagram>();
                    portMap.put(port, tee);
                }
                tee.add(pushChannel);
            }
        }
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    public void join(final OutputChannel<UdpDatagram> pushChannel,
                     final InetAddress groupAddress,
                     final InetAddress sourceAddress,
                     final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "ChannelMembershipManager.join",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }
        
        Precondition.checkSSMMulticastAddress(groupAddress);
        Precondition.checkAddresses(groupAddress, sourceAddress);

        synchronized (this.groupMap) {
            OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
            if (sourceMap == null) {
                sourceMap = new OutputChannelMap<UdpDatagram>(this.sourceExtractor);
                this.groupMap.put(groupAddress, sourceMap);
                OutputChannelMap<UdpDatagram> portMap = new OutputChannelMap<UdpDatagram>(this.portExtractor);
                sourceMap.put(sourceAddress, portMap);
                OutputChannelTee<UdpDatagram> tee = new OutputChannelTee<UdpDatagram>();
                tee.add(pushChannel);
                portMap.put(port, tee);
                this.interfaceManager.join(groupAddress, sourceAddress);
            }
            else {
                OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)sourceMap.get(sourceAddress);
                if (portMap == null) {
                    portMap = new OutputChannelMap<UdpDatagram>(this.portExtractor);
                    sourceMap.put(sourceAddress, portMap);
                    OutputChannelTee<UdpDatagram> tee = new OutputChannelTee<UdpDatagram>();
                    portMap.put(port, tee);
                    tee.add(pushChannel);
                }
                else {
                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
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
     * 
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.leave", pushChannel, Logging.address(groupAddress)));
        }
 
        Precondition.checkASMMulticastAddress(groupAddress);

        synchronized (this.groupMap) {
            OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
            if (portMap != null) {
                // Look for this channel 
                Iterator<Object> portIter = portMap.getKeys().iterator();
                while (portIter.hasNext()) {
                    int port = (Integer)portIter.next();
                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                    tee.remove(pushChannel);
                    if (tee.isEmpty()) {
                        // No more channels associated with this port - remove the port entry
                        portIter.remove();
                        if (portMap.isEmpty()) {
                            // No more ports associated with this group - remove the group entry
                            this.groupMap.remove(groupAddress);
                            // No channels are left in this group - update the interface reception state
                            this.interfaceManager.leave(groupAddress);
                        }
                    }
                }
            }
        }
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress,
                      final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.leave", pushChannel, Logging.address(groupAddress), port));
        }
        
        Precondition.checkASMMulticastAddress(groupAddress);

        synchronized (this.groupMap) {
            // Get the port selector for the group
            OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
            if (portMap != null) {
                // Get the splitter for the port
                OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                // Remove the channel from the splitter
                tee.remove(pushChannel);
                if (tee.isEmpty()) {
                    // No more channels associated with this port - remove the port entry
                    portMap.remove(port);
                    if (portMap.isEmpty()) {
                        // No more ports associated with this group - remove the group entry
                        this.groupMap.remove(groupAddress);
                        // No channels are left in this group - update the interface reception state
                        this.interfaceManager.leave(groupAddress);
                    }
                }
            }
        }
    }

    /**
     * 
     * @param channel
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> channel,
                      final InetAddress groupAddress,
                      final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.leave", channel, Logging.address(groupAddress), Logging.address(sourceAddress)));
        }
        
        Precondition.checkSSMMulticastAddress(groupAddress);
        Precondition.checkAddresses(groupAddress, sourceAddress);

        synchronized (this.groupMap) {
            // Get the source selector for the group
            OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
            if (sourceMap != null) {
                // Get the port selector for the source
                OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)sourceMap.get(sourceAddress);
                if (portMap != null) {
                    // Look for the channel under all of the port entries 
                    Iterator<Object> portIter = portMap.getKeys().iterator();
                    while (portIter.hasNext()) {
                        int port = (Integer)portIter.next();
                        // Get the splitter for this port
                        OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                        // Remove the channel (even though it may not be there).
                        tee.remove(channel);
                        if (tee.isEmpty()) {
                            // No more channels associated with this port - remove the port entry
                            portIter.remove();
                            if (portMap.isEmpty()) {
                                // No more ports associated with this source - remove the source entry
                                sourceMap.remove(sourceAddress);
                                if (sourceMap.isEmpty()) {
                                    // No more sources associated with this group - remove the group entry
                                    this.groupMap.remove(groupAddress);
                                }
                                // No channels are left in this source group - update the interface reception state
                                this.interfaceManager.leave(groupAddress, sourceAddress);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress,
                      final InetAddress sourceAddress,
                      final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "ChannelMembershipManager.leave",
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        Precondition.checkSSMMulticastAddress(groupAddress);
        Precondition.checkAddresses(groupAddress, sourceAddress);

        synchronized (this.groupMap) {
            // Get the source selector for the group
            OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
            if (sourceMap != null) {
                // Get the port selector for the source
                OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)sourceMap.get(sourceAddress);
                if (portMap != null) {
                    // Get the splitter for the port
                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                    // Remove the channel from the splitter
                    tee.remove(pushChannel);
                    if (tee.isEmpty()) {
                        // No more channels associated with this port - remove the port entry
                        portMap.remove(port);
                        if (portMap.isEmpty()) {
                            // No more ports associated with this source - remove the source entry
                            sourceMap.remove(sourceAddress);
                            if (sourceMap.isEmpty()) {
                                // No more sources associated with this group - remove the group entry
                                this.groupMap.remove(groupAddress);
                            }
                            // No channels are left in this source group - update the interface reception state
                            this.interfaceManager.leave(groupAddress, sourceAddress);
                        }
                    }
                }
            }
        }
    }

    /**
     * 
     * @param pushChannel
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.leave", pushChannel));
        }

        synchronized (this.groupMap) {
            // Look for the channel under all group entries
            Iterator<Object> groupIter = this.groupMap.getKeys().iterator();
            while (groupIter.hasNext()) {
                InetAddress groupAddress = (InetAddress)groupIter.next();
                if (Precondition.isSSMMulticastAddress(groupAddress)) {
                    // Get the source selector for this group
                    OutputChannelMap<UdpDatagram> sourceMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
                    if (sourceMap != null) {
                        // Look for the channel under all source entries
                        Iterator<Object> sourceIter = sourceMap.getKeys().iterator();
                        while (sourceIter.hasNext()) {
                            InetAddress sourceAddress = (InetAddress)sourceIter.next();
                            // Get the port selector for the source
                            OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)sourceMap.get(sourceAddress);
                            if (portMap != null) {
                                // Look for the channel under all of the port entries 
                                Iterator<Object> portIter = portMap.getKeys().iterator();
                                while (portIter.hasNext()) {
                                    int port = (Integer)portIter.next();
                                    // Get the splitter for this port
                                    OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                                    // Remove the channel (even though it may not be there).
                                    tee.remove(pushChannel);
                                    if (tee.isEmpty()) {
                                        // No more channels associated with this port - remove the port entry
                                        portIter.remove();
                                        if (portMap.isEmpty()) {
                                            // No more ports associated with this source - remove the source entry
                                            sourceIter.remove();
                                            if (sourceMap.isEmpty()) {
                                                // No more sources associated with this group - remove the group entry
                                                groupIter.remove();
                                            }
                                            // No channels are left in this source group - update the interface reception state
                                            this.interfaceManager.leave(groupAddress, sourceAddress);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    // Get the port selector for the group
                    OutputChannelMap<UdpDatagram> portMap = (OutputChannelMap<UdpDatagram>)this.groupMap.get(groupAddress);
                    if (portMap != null) {
                        // Look for the channel under all of the port entries 
                        Iterator<Object> portIter = portMap.getKeys().iterator();
                        while (portIter.hasNext()) {
                            int port = (Integer)portIter.next();
                            // Get the splitter for this port
                            OutputChannelTee<UdpDatagram> tee = (OutputChannelTee<UdpDatagram>)portMap.get(port);
                            // Remove the channel (even though it may not be there).
                            tee.remove(pushChannel);
                            if (tee.isEmpty()) {
                                // No more channels associated with this port - remove the port entry
                                portIter.remove();
                                if (portMap.isEmpty()) {
                                    // No more ports associated with this group - remove the group entry
                                    groupIter.remove();
                                    // No channels are left in this group - update the interface reception state
                                    this.interfaceManager.leave(groupAddress);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelMembershipManager.shutdown"));
        }

        synchronized (this.groupMap) {
            this.interfaceManager.shutdown();
            try {
                this.groupMap.close(true);
            }
            catch (IOException e) {
                logger.fine(ObjectId +
                            " attempt to close channel group map failed failed with exception - " +
                            e.getClass().getName() + ":" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    

    /**
     * 
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
                    MultiIOException me = (MultiIOException)e;
                    Iterator<Throwable> iter = me.iterator();
                    while (iter.hasNext()) {
                        Throwable t = iter.next();
                        if (t instanceof BoundException) {
                            BoundException be = (BoundException)t;
                            Object o = be.getObject();
                            if (o instanceof OutputChannel<?>){
                                if (logger.isLoggable(Level.FINE)) {
                                    Throwable te = be.getThrowable();
                                    logger.fine(ObjectId +
                                            " removing channel " + Logging.identify(o) + " due to exception - " +
                                            te.getClass().getName() + ": " + te.getMessage());
                                }
                                OutputChannel<UdpDatagram> channel = (OutputChannel<UdpDatagram>)o;
                                leave(channel);
                                return;
                            }
                        }
                    }
                }
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId +
                                " closing all multicast channels due to unhandled exception - " +
                                e.getClass().getName() + ": " + e.getMessage());
                }
                shutdown();
            }
        }
    }

}
