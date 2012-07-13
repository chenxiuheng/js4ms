package com.larkwoodlabs.net.amt.gateway;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageKeyExtractor;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelMap;
import com.larkwoodlabs.channels.OutputChannelTee;
import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.net.amt.IPv4MembershipQueryTransform;
import com.larkwoodlabs.net.amt.IPv4MembershipReportTransform;
import com.larkwoodlabs.net.amt.IPv6MembershipQueryTransform;
import com.larkwoodlabs.net.amt.IPv6MembershipReportTransform;
import com.larkwoodlabs.net.amt.MembershipQuery;
import com.larkwoodlabs.net.amt.MembershipReport;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.igmp.IGMPMessage;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.net.ip.mld.MLDMessage;
import com.larkwoodlabs.util.logging.Log;
import com.larkwoodlabs.util.logging.Logging;

public class AmtIPInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtIPInterface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    private final AmtIPInterfaceManager manager;

    private final AmtPseudoInterface amtPseudoInterface;

    private final OutputChannelTee<IPPacket> dispatchChannel;

    private int referenceCount = 0;

    private final Timer taskTimer;

    private final InterfaceMembershipManager ipv4MembershipManager;

    private final InterfaceMembershipManager ipv6MembershipManager;

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    AmtIPInterface(final AmtIPInterfaceManager manager,
                   final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.AmtIPInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.manager = manager;
        this.amtPseudoInterface = AmtPseudoInterfaceManager.getInstance().getInterface(relayDiscoveryAddress);
        this.dispatchChannel = new OutputChannelTee<IPPacket>();

        this.taskTimer = new Timer("AMT IP Interface");

        // Create channel that sends packets over the pseudo-interface
        OutputChannel<IPPacket> reportChannel = new OutputChannel<IPPacket>() {

            @Override
            public void send(IPPacket packet, int milliseconds) throws IOException, InterruptedException {
                AmtIPInterface.this.amtPseudoInterface.send(packet);
            }

            @Override
            public void close() {
            }
        };

        // Create separate membership managers for IPv4/IGMP and IPv6/MLD group
        // subscriptions
        this.ipv4MembershipManager = new InterfaceMembershipManager(this.taskTimer);
        this.ipv6MembershipManager = new InterfaceMembershipManager(this.taskTimer);

        // Connect report channel of interface membership managers to pseudo-interface.
        this.ipv4MembershipManager.setOutgoingReportChannel(
                        new OutputChannelTransform<MembershipReport, IPPacket>(reportChannel,
                                                                               new IPv4MembershipReportTransform()));
        this.ipv6MembershipManager.setOutgoingReportChannel(
                        new OutputChannelTransform<MembershipReport, IPPacket>(reportChannel,
                                                                               new IPv6MembershipReportTransform()));

        // Create transforms for interface membership manager to query channels
        OutputChannelTransform<IPPacket, MembershipQuery> ipv4TransformChannel = new OutputChannelTransform<IPPacket, MembershipQuery>(
                                                                                                                                       this.ipv4MembershipManager
                                                                                                                                                       .getIncomingQueryChannel(),
                                                                                                                                       new IPv4MembershipQueryTransform());
        OutputChannelTransform<IPPacket, MembershipQuery> ipv6TransformChannel = new OutputChannelTransform<IPPacket, MembershipQuery>(
                                                                                                                                       this.ipv6MembershipManager
                                                                                                                                                       .getIncomingQueryChannel(),
                                                                                                                                       new IPv6MembershipQueryTransform());

        // Create an extractor to differentiate between IGMP, MLD and other message types.
        MessageKeyExtractor<IPPacket> protocolExtractor = new MessageKeyExtractor<IPPacket>() {

            @Override
            public Byte getKey(IPPacket packet) {
                if (packet.getVersion() == IPv4Packet.INTERNET_PROTOCOL_VERSION) {
                    IPMessage ipMessage = packet.getProtocolMessage(IGMPMessage.IP_PROTOCOL_NUMBER);
                    if (ipMessage == null) {
                        return null;
                    }
                    return IGMPMessage.IP_PROTOCOL_NUMBER;
                }
                else if (packet.getVersion() == IPv6Packet.INTERNET_PROTOCOL_VERSION) {
                    IPMessage ipMessage = packet.getProtocolMessage(MLDMessage.IP_PROTOCOL_NUMBER);
                    if (ipMessage == null) {
                        return null;
                    }
                    return MLDMessage.IP_PROTOCOL_NUMBER;
                }
                else {
                    return null;
                }
            }
        };

        // Create the output channel map that will be used to route packets to the
        // appropriate recipients.
        OutputChannelMap<IPPacket> outputChannelMap = new OutputChannelMap<IPPacket>(protocolExtractor);

        // Create channels that route IGMP or MLD messages to appropriate transform
        // channels.
        outputChannelMap.put(IGMPMessage.IP_PROTOCOL_NUMBER, ipv4TransformChannel);
        outputChannelMap.put(MLDMessage.IP_PROTOCOL_NUMBER, ipv6TransformChannel);

        // Add channel that will receive any packets containing something other than an
        // IGMP or MLD messages.
        outputChannelMap.put(null, this.dispatchChannel);

        // Connect the channel map to the pseudo-interface output channel
        this.amtPseudoInterface.addOutputChannel(outputChannelMap);

    }

    /**
     * Adds a packet destination channel.
     * 
     * @param destinationChannel
     */
    public final void addOutputChannel(final OutputChannel<IPPacket> destinationChannel) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.addOutputChannel", destinationChannel));
        }

        this.dispatchChannel.add(destinationChannel);
    }

    /**
     * Removes a packet destination channel.
     * 
     * @param destinationChannel
     */
    public final void removeOutputChannel(final OutputChannel<IPPacket> destinationChannel) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.removeOutputChannel", destinationChannel));
        }

        this.dispatchChannel.remove(destinationChannel);
    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * 
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.amtPseudoInterface.getRelayDiscoveryAddress();
    }

    /**
     * 
     */
    final synchronized void acquire() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.acquire"));
        }

        this.referenceCount++;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    final synchronized void release() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.release"));
        }

        if (--this.referenceCount == 0) {
            this.manager.closeInterface(this);
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {
        this.amtPseudoInterface.release();
    }

    /**
     * @param groupAddress
     * @throws IOException
     * @throws InterruptedException
     */
    public final void join(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.join", Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.join(groupAddress);
        }
        else {
            this.ipv6MembershipManager.join(groupAddress);
        }
    }

    /**
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     * @throws InterruptedException
     */
    public final void join(final InetAddress groupAddress,
                           final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.join",
                                   Logging.address(groupAddress),
                                   Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.join(groupAddress, sourceAddress);
        }
        else {
            this.ipv6MembershipManager.join(groupAddress, sourceAddress);
        }
    }

    /**
     * @param groupAddress
     * @throws IOException
     */
    public final void leave(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.leave", Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.leave(groupAddress);
        }
        else {
            this.ipv6MembershipManager.leave(groupAddress);
        }
    }

    /**
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public final void leave(final InetAddress groupAddress,
                            final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.leave",
                                   Logging.address(groupAddress),
                                   Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.leave(groupAddress, sourceAddress);
        }
        else {
            this.ipv6MembershipManager.leave(groupAddress, sourceAddress);
        }
    }

    /**
     * @throws IOException
     */
    public final void leave() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.leave"));
        }

        this.ipv4MembershipManager.leave();
        this.ipv6MembershipManager.leave();
    }
}