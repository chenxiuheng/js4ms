package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * PacketAssembler.java [org.js4ms.jsdk:amt]
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
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.LoggableBase;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.Precondition;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.ipv6.IPv6Packet;




/**
 * An {@link OutputChannel} that reassembles fragmented IP datagrams.
 * <p>
 * This channel accepts {@link IPPacket} objects that carry complete or fragmented
 * datagrams. If a packet carries a complete datagram, the defragmenter will immediately
 * send the packet to the downstream output channel. If the packet carries a datagram
 * fragment, the fragmenter will attempt to combine that packet with other incoming
 * datagram fragments to reconstruct the original datagram. Once the datagram is complete,
 * the defragmenter will send a new packet carrying the completed datagram to the
 * downstream channel.
 * <p>
 * The defragmenter imposes a 60 second time limit on for reassembly of a datagram. If a
 * particular datagram cannot be reassembled within timeout period, the partial
 * reconstruction of that datagram is flushed from the internal cache. (Note: The
 * reassembly process will restart if additional fragments for the datagram arrive after
 * the time-limit.)
 * <p>
 * The defragmenter can be attached to a second output channel that can be used to receive
 * a timeout notification. The defragmenter will send the packet containing fragment zero
 * of the datagram if that packet is available and the destination address is not a
 * multicast address. No timeout is reported for a multicast packets
 * <p>
 * The defragmenter must allocate a 64K buffer to reassemble each fragmented datagram. If
 * a large number of fragmented datagrams arrive within a 60s time window, the
 * defragmenter will consume a significant amount of memory. To avoid excessive memory
 * use, the defragmenter can be constructed with a limit on the number of datagrams that
 * can undergo simultaneous reassembly. If the limit is reached, the defragmenter places
 * the new datagram fragments into a queue for future reassembly. This queue is limited by
 * the total size of the queued packets. The defragmenter will discard packets once the
 * total size of the packets in the queue reaches 64K.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
final class PacketAssembler
                extends LoggableBase
                implements OutputChannel<IPPacket> {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     */
    final static class ReassemblyBuffer {

        /*-- Static Variables ---------------------------------------------------*/

        /**
         * 
         */
        public static final Logger logger = Logger.getLogger(PacketAssembler.ReassemblyBuffer.class.getName());

        private static final int EOL = 0xFFFF;

        /*-- Member Variables ---------------------------------------------------*/

        private final Log log = new Log(this);

        private ByteBuffer identifier;

        // byte[] buffer = new byte[65515];
        private byte[] buffer = new byte[1500];

        private int firstHole = 0;

        private int length = 0;

        private long timestamp;

        private long maxAge;

        private IPPacket fragmentZeroPacket;

        /**
         * @param identifier
         * @param maxAge
         */
        ReassemblyBuffer(final ByteBuffer identifier, final long maxAge) {
            this.identifier = identifier;
            setHole(this.firstHole, 0xFFFF, EOL, EOL);
            this.timestamp = System.currentTimeMillis();
            this.maxAge = maxAge;
        }

        /**
         * @return
         */
        ByteBuffer getIdentifier() {
            return identifier;
        }

        /**
         * @param currentTimeMs
         * @return
         */
        boolean isExpired(final long currentTimeMs) {
            if ((currentTimeMs - timestamp) > maxAge) {
                return true;
            }
            return false;
        }

        /**
         * @param currentTimeMs
         * @return
         */
        long getAge(final long currentTimeMs) {
            return currentTimeMs - timestamp;
        }

        /**
         * @param packet
         * @return
         * @throws ParseException
         */
        boolean addFragment(final IPPacket packet) throws ParseException {

            // Already done?
            if (isComplete()) return true;

            // Does this packet carry the header(s) we should save?
            if (packet.getFragmentOffset() == 0) {
                // Use this packet to construct the completed packet
                this.fragmentZeroPacket = packet;
            }

            if (addFragment(packet.getFragment(), packet.getFragmentOffset(), packet.isMoreFragments())) {
                this.fragmentZeroPacket.setReassembledPayload(ByteBuffer.wrap(buffer, 0, this.length));
                return true;
            }

            return false;
        }

        /**
         * @return
         */
        IPPacket getFragmentZeroPacket() {
            return this.fragmentZeroPacket;
        }

        /**
         * @return
         */
        IPPacket getCompletedPacket() {
            if (isComplete()) {
                return this.fragmentZeroPacket;
            }
            return null;
        }

        /**
         * @return
         */
        boolean isComplete() {
            return this.firstHole == EOL;
        }

        /**
         * @param hole
         * @param holeLast
         * @param prevHole
         * @param nextHole
         */
        void setHole(final int hole,
                     final int holeLast,
                     final int prevHole,
                     final int nextHole) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(this.log.msg("new hole.first=" + hole + " hole.last=" + holeLast + " hole.prev=" + prevHole
                                          + " hole.next=" + nextHole));
            }
            int byteOffset = hole * 8;
            setOffsetValue(byteOffset, holeLast);
            setOffsetValue(byteOffset + 2, prevHole);
            setOffsetValue(byteOffset + 4, nextHole);
        }

        /**
         * @param byteOffset
         * @param offset
         */
        void setOffsetValue(final int byteOffset, final int offset) {
            buffer[byteOffset] = (byte) ((offset >> 8) & 0xFF);
            buffer[byteOffset + 1] = (byte) (offset & 0xFF);
        }

        /**
         * @param byteOffset
         * @return
         */
        int getOffsetValue(final int byteOffset) {
            return ((buffer[byteOffset] & 0xFF) << 8) | (buffer[byteOffset + 1] & 0xFF);
        }

        /**
         * @param holeFirst
         * @return
         */
        int getHoleLast(final int holeFirst) {
            return getOffsetValue(holeFirst * 8);
        }

        /**
         * @param holeFirst
         * @param holeLast
         */
        void setHoleLast(final int holeFirst, final int holeLast) {
            setOffsetValue(holeFirst * 8, holeLast);
        }

        /**
         * @param holeFirst
         * @return
         */
        int getPrevHole(final int holeFirst) {
            return getOffsetValue(holeFirst * 8 + 2);
        }

        /**
         * @param holeFirst
         * @param prevHole
         */
        void setPrevHole(final int holeFirst, final int prevHole) {
            setOffsetValue(holeFirst * 8 + 4, prevHole);
        }

        /**
         * @param holeFirst
         * @return
         */
        int getNextHole(final int holeFirst) {
            return getOffsetValue(holeFirst * 8 + 4);
        }

        /**
         * @param holeFirst
         * @param nextHole
         */
        void setNextHole(final int holeFirst, final int nextHole) {
            setOffsetValue(holeFirst * 8 + 6, nextHole);
        }

        /**
         * Algorithm described in <a
         * href="http://tools.ietf.org/html/rfc815">[RFC-851]</a>
         * <p>
         * This will remove all of the hole descriptors that will be overwritten by the
         * fragment. 1. Select the next hole descriptor from the hole descriptor list. If
         * there are no more entries, go to step eight.
         * <p>
         * 2. If fragment.first is greater than hole.last, go to step one.
         * <p>
         * 3. If fragment.last is less than hole.first, go to step one.
         * <p>
         * <blockquote> (If either step two or step three is true, then the newly arrived
         * fragment does not overlap with the hole in any way, so we need pay no further
         * attention to this hole. We return to the beginning of the algorithm where we
         * select the next hole for examination.) </blockquote> 4. Delete the current
         * entry from the hole descriptor list. <blockquote> (Since neither step two nor
         * step three was true, the newly arrived fragment does interact with this hole in
         * some way. Therefore, the current descriptor will no longer be valid. We will
         * destroy it, and in the next two steps we will determine whether or not it is
         * necessary to create any new hole descriptors.) </blockquote> 5. If
         * fragment.first is greater than hole.first, then create a new hole descriptor
         * "new_hole" with new_hole.first equal to hole.first, and new_hole.last equal to
         * fragment.first minus one. <blockquote> (If the test in step five is true, then
         * the first part of the original hole is not filled by this fragment. We create a
         * new descriptor for this smaller hole.) </blockquote> 6. If fragment.last is
         * less than hole.last and fragment.more fragments is true, then create a new hole
         * descriptor "new_hole", with new_hole.first equal to fragment.last plus one and
         * new_hole.last equal to hole.last. <blockquote> (This test is the mirror of step
         * five with one additional feature. Initially, we did not know how long the
         * reassembled datagram would be, and therefore we created a hole reaching from
         * zero to infinity. Eventually, we will receive the last fragment of the
         * datagram. At this point, that hole descriptor which reaches from the last octet
         * of the buffer to infinity can be discarded. The fragment which contains the
         * last fragment indicates this fact by a flag in the internet header called
         * "more fragments". The test of this bit in this statement prevents us from
         * creating a descriptor for the unneeded hole which describes the space from the
         * end of the datagram to infinity.) </blockquote> 7. Go to step one.
         * <p>
         * 8. If the hole descriptor list is now empty, the datagram is now complete. Pass
         * it on to the higher level protocol processor for further handling. Otherwise,
         * return.
         * 
         * @param buffer
         *            - The IP datagram fragment.
         * @param fragmentOffset
         *            - The fragment offset.
         * @param isMoreFragments
         *            - Indicates this is the end fragment.
         * @return Indicates whether the datagram is complete.
         */
        boolean addFragment(final ByteBuffer buffer, final int fragmentOffset, final boolean isMoreFragments) {

            int fragmentFirst = fragmentOffset;
            int fragmentLast = fragmentFirst + ((buffer.limit() + 7) / 8) - 1;

            // Remove all of the hole descriptors that will be overritten by this fragment
            int hole = this.firstHole;

            while (hole != EOL) {

                int holeLast = getHoleLast(hole);
                int nextHole = getNextHole(hole);
                if (fragmentFirst > holeLast) {
                    // Fragment falls after this hole - check next hole
                    hole = nextHole;
                    continue;
                }

                if (fragmentLast < hole) {
                    // Fragment falls in front of this hole - check next hole
                    hole = nextHole;
                    continue;
                }

                int prevHole = getPrevHole(hole);

                if (fragmentFirst > hole) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(this.log.msg("hole.first=" + hole + " hole.last=" + holeLast + " frag.first=" + fragmentFirst
                                                  + " frag.last=" + fragmentLast + " <-- fragment overlaps end of hole"));
                    }
                    setHoleLast(hole, fragmentLast - 1);
                }
                else if (fragmentLast < holeLast && isMoreFragments) {
                    logger.finer(this.log.msg("hole.first=" + hole + " hole.last=" + holeLast + " frag.first=" + fragmentFirst
                                              + " frag.last=" + fragmentLast + " <-- fragment overlaps start of hole"));
                    int newHole = fragmentLast + 1;
                    if (prevHole == EOL) {
                        this.firstHole = newHole;
                    }
                    else {
                        setNextHole(prevHole, newHole);
                    }
                    if (nextHole != EOL) {
                        setPrevHole(nextHole, newHole);
                    }
                    setHole(fragmentLast + 1, holeLast, prevHole, nextHole);
                }
                else {
                    logger.finer(this.log.msg("hole.first=" + hole + " hole.last=" + holeLast + " frag.first=" + fragmentFirst
                                              + " frag.last=" + fragmentLast + " <-- fragment overlaps entire hole - delete hole"));
                    if (prevHole == EOL) {
                        this.firstHole = nextHole;
                    }
                    else {
                        setNextHole(prevHole, nextHole);
                    }
                    if (nextHole != EOL) {
                        setPrevHole(nextHole, prevHole);
                    }
                }
                hole = nextHole;
            }

            // Now write the fragment into the buffer
            buffer.position(0);
            buffer.get(this.buffer, fragmentOffset * 8, buffer.limit());
            buffer.position(0);

            int limit = buffer.limit() + fragmentOffset * 8;
            if (limit > this.length) {
                this.length = limit > this.buffer.length ? this.buffer.length : limit;
            }

            // If firstHole is zero, there are no more holes to fill.
            return this.firstHole == EOL;
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(PacketAssembler.class.getName());

    public static final int MAX_CUMMULATIVE_PENDING_PACKET_SIZE = 65536;

    public static final int REASSEMBLY_TIMEOUT = 500; // 60000; // Milliseconds

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * Constructs an IPv6 buffer identifier from values contained in an IPv6
     * packet header. A <code>ByteBuffer</code> provides the <code>hashCode()</code> and
     * <code>equals()</code> methods that are
     * required to locate a {@link ReassemblyBuffer} in the {@link #cache} HashMap.
     * 
     * @param packet
     *            - The IPv6 packet that provides the values used to construct
     *            the identifier.
     */
    static ByteBuffer constructBufferIdentifier(final IPv6Packet packet) {
        return constructBufferIdentifier(packet.getSourceAddress(), packet.getDestinationAddress(), packet.getFragmentIdentifier());
    }

    /**
     * Constructs an {@link ReassemblyBuffer} identifier from values contained
     * in an IPv6 packet header. A <code>ByteBuffer</code> provides the
     * <code>hashCode()</code> and <code>equals()</code> methods that are
     * required to locate a {@link ReassemblyBuffer} in the {@link #cache} HashMap.
     * 
     * @param sourceAddress
     *            - The source address contained in the IPv6 header (16 bytes).
     * @param destinationAddress
     *            - The destination address contained in the IPv6 header (16
     *            bytes).
     * @param identification
     *            - The identification value contained in the IPv6 header (4
     *            bytes).
     */
    static ByteBuffer constructBufferIdentifier(final byte[] sourceAddress,
                                                final byte[] destinationAddress,
                                                final int identification) {
        int size = sourceAddress.length + destinationAddress.length + 4;
        ByteBuffer identifier = ByteBuffer.allocate(size);
        // Components added in order most likely to produce earliest indication
        // of inequality. TODO
        identifier.putInt(identification);
        identifier.put(sourceAddress);
        identifier.put(destinationAddress);
        identifier.rewind();
        return identifier;
    }

    /**
     * Constructs a {@link ReassemblyBuffer} identifier from values contained in
     * an IPv4 packet header. A <code>ByteBuffer</code> provides the
     * <code>hashCode()</code> and <code>equals()</code> methods that are
     * required to locate a {@link ReassemblyBuffer} in the {@link #cache} HashMap.
     * 
     * @param packet
     *            - The IPv5 packet that provides the values used to construct
     *            the identifier.
     */
    static ByteBuffer constructBufferIdentifier(final IPv4Packet packet) {
        return constructBufferIdentifier(packet.getSourceAddress(), packet.getDestinationAddress(), (short) packet
                        .getFragmentIdentifier(), packet.getProtocol());
    }

    /**
     * Constructs an IPv4 buffer identifier from values contained in an IPv6
     * packet header. A <code>ByteBuffer</code> provides the <code>hashCode()</code> and
     * <code>equals()</code> methods that are
     * required to locate a {@link ReassemblyBuffer} in the {@link #cache} HashMap.
     * 
     * @param sourceAddress
     *            - The source address contained in the IPv4 header (4 bytes).
     * @param destinationAddress
     *            - The destination address contained in the IPv4 header (4
     *            bytes).
     * @param identification
     *            - The identification value contained in the IPv4 header (2
     *            bytes).
     * @param protocol
     *            - The protocol value contained in the IPv4 header (1 byte).
     */
    static ByteBuffer constructBufferIdentifier(final byte[] sourceAddress,
                                                final byte[] destinationAddress, short identification,
                                                final byte protocol) {
        int size = sourceAddress.length + destinationAddress.length + 5;
        ByteBuffer identifier = ByteBuffer.allocate(size);
        // Components added in order most likely to produce earliest indication
        // of inequality. TODO
        identifier.putShort(identification);
        identifier.put(sourceAddress);
        identifier.put(destinationAddress);
        identifier.put(protocol);
        identifier.rewind();
        return identifier;
    }

    /*-- Member Variables ---------------------------------------------------*/

    private final Object lock = new Object();

    private final Log log = new Log(this);

    private final OutputChannel<IPPacket> outputChannel;

    private final OutputChannel<IPPacket> timeoutChannel;

    private final HashMap<ByteBuffer, ReassemblyBuffer> cache = new HashMap<ByteBuffer, ReassemblyBuffer>();

    private final LinkedList<IPPacket> pendingQueue = new LinkedList<IPPacket>();

    private final int maxCacheSize;

    private int cummulativePendingPacketSize = 0;

    private final Timer taskTimer;

    private TimerTask reaperTask = null;

    private enum Result {
        Started,
        Added,
        Completed,
        Denied
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an output channel that reassembles fragmented IP datagrams.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param timeoutChannel
     *            - The optional output channel used to receive timeout notifications.
     *            May be <code>null</code>
     * @param maxCacheSize
     *            - Maximum number of datagrams that can be reassembled at the same time.
     *            A value of zero is used to indicate that there should be no limit.
     * @param taskTimer
     *            - Externally constructed Timer used to execute the task used to check
     *            timeouts.
     *            The assembler will construct its own timer if this value is
     *            <code>null</code>.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final OutputChannel<IPPacket> timeoutChannel,
                    final int maxCacheSize,
                    final Timer taskTimer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "PacketAssembler.Defragmenter",
                                        outputChannel,
                                        timeoutChannel,
                                        maxCacheSize,
                                        taskTimer));
        }

        if (outputChannel == null) {
            throw new IllegalArgumentException("output channel must not be null");
        }

        this.outputChannel = outputChannel;
        this.timeoutChannel = timeoutChannel;
        this.maxCacheSize = maxCacheSize >= 0 ? maxCacheSize : 0;
        this.taskTimer = (taskTimer != null ? taskTimer : new Timer(PacketAssembler.class.getName()));
    }

    /**
     * Constructs an assembler output channel that imposes no limit on cache size
     * and constructs its own Timer thread.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel) {
        this(outputChannel, null, 0, null);
    }

    /**
     * Constructs an assembler output channel that constructs its own Timer thread.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param maxCacheSize
     *            - Maximum number of datagrams that can be reassembled at the same time.
     *            A value of zero is used to indicate that there should be no limit.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final int maxCacheSize) {
        this(outputChannel, null, maxCacheSize, null);
    }

    /**
     * Constructs an assembler output channel that constructs its own Timer thread.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param maxCacheSize
     *            - Maximum number of datagrams that can be reassembled at the same time.
     *            A value of zero is used to indicate that there should be no limit.
     * @param taskTimer
     *            - Externally constructed Timer used to execute the task used to check
     *            timeouts.
     *            The assembler will construct its own timer if this value is
     *            <code>null</code>.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final int maxCacheSize,
                    final Timer taskTimer) {
        this(outputChannel, null, maxCacheSize, taskTimer);
    }

    /**
     * Constructs an assembler output channel that imposes no limit on cache size and
     * constructs its own Timer thread.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param timeoutChannel
     *            - The optional output channel used to receive timeout notifications.
     *            May be <code>null</code>
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final OutputChannel<IPPacket> timeoutChannel) {
        this(outputChannel, timeoutChannel, 0, null);
    }

    /**
     * Constructs an assembler output channel that constructs its own Timer thread.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param timeoutChannel
     *            - The optional output channel used to receive timeout notifications.
     *            May be <code>null</code>
     * @param maxCacheSize
     *            - Maximum number of datagrams that can be reassembled at the same time.
     *            A value of zero is used to indicate that there should be no limit.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final OutputChannel<IPPacket> timeoutChannel,
                    final int maxCacheSize) {
        this(outputChannel, timeoutChannel, 0, null);
    }

    /**
     * Constructs an assembler output channel that imposes no limit on cache size.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param taskTimer
     *            - Externally constructed Timer used to execute the task used to check
     *            timeouts.
     *            The defragmenter will construct its own timer if this value is
     *            <code>null</code>.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final Timer taskTimer) {
        this(outputChannel, null, 0, taskTimer);
    }

    /**
     * Constructs an assembler output channel that imposes no limit on cache size.
     * 
     * @param outputChannel
     *            - The output channel that will receive reassembled datagrams. Required.
     * @param timeoutChannel
     *            - The optional output channel used to receive timeout notifications.
     *            May be <code>null</code>
     * @param taskTimer
     *            - Externally constructed Timer used to execute the task used to check
     *            timeouts.
     *            The defragmenter will construct its own timer if this value is
     *            <code>null</code>.
     */
    PacketAssembler(final OutputChannel<IPPacket> outputChannel,
                    final OutputChannel<IPPacket> timeoutChannel,
                    final Timer taskTimer) {
        this(outputChannel, timeoutChannel, 0, taskTimer);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void send(final IPPacket packet, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("PacketAssembler.send", packet, milliseconds));
        }

        // Send it on if not fragmented
        if (!packet.isFragmented()) {

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(this.log.msg("forwarding unfragmented packet"));
            }

            this.outputChannel.send(packet, milliseconds);
            return;
        }

        synchronized (this.lock) {

            // Now see if we can do something with this packet
            Result result = processPacket(packet, milliseconds);
            if (result == Result.Denied) {

                // The packet appears to start a new datagram but the cache is full
                // so we can't start reassembly yet.
                // Attempt to add the packet to the pending queue.

                int packetSize = packet.getTotalLength();

                if (this.cummulativePendingPacketSize + packetSize > MAX_CUMMULATIVE_PENDING_PACKET_SIZE) {
                    throw new IOException("datagram reassembly cache size limit reached");
                }

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(this.log.msg("queueing packet for future reassembly"));
                }

                this.cummulativePendingPacketSize += packetSize;
                this.pendingQueue.add(packet);
            }
            else if (result == Result.Started || result == Result.Completed) {
                processPendingPackets(milliseconds);
            }
        }
    }

    /**
     * @param milliseconds
     * @throws InterruptedIOException
     * @throws IOException
     * @throws InterruptedException
     */
    private void processPendingPackets(final int milliseconds) throws InterruptedIOException, IOException, InterruptedException {
        Iterator<IPPacket> iter = this.pendingQueue.iterator();
        boolean isCheckAll = false;
        while (iter.hasNext()) {
            IPPacket pendingPacket = iter.next();
            int packetSize = pendingPacket.getTotalLength();
            Result result = processPacket(pendingPacket, milliseconds);
            if (result != Result.Denied) {
                iter.remove();
                this.cummulativePendingPacketSize -= packetSize;
            }
            if (!isCheckAll) {
                if (result == Result.Started || result == Result.Completed) {
                    // We have room for a new entry, or a new entry was started
                    // Check the rest to see if they can be processed
                    isCheckAll = true;
                }
                else {
                    // Nothing has changed for packets remaining in the queue
                    break;
                }
            }
        }
    }

    /**
     * @param packet
     * @param milliseconds
     * @return
     * @throws InterruptedIOException
     * @throws IOException
     * @throws InterruptedException
     */
    private Result processPacket(final IPPacket packet, final int milliseconds) throws InterruptedIOException, IOException, InterruptedException {

        Result result = Result.Denied;

        // Construct the identifier we used to lookup reassembly buffers in the cache
        ByteBuffer identifier;
        byte version = packet.getVersion();
        if (version == 4) {
            identifier = constructBufferIdentifier((IPv4Packet) packet);
        }
        else if (version == 6) {
            identifier = constructBufferIdentifier((IPv6Packet) packet);
        }
        else {
            throw new IllegalArgumentException("unrecognized IP packet type");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.msg("searching reassembly buffer for source=" + Logging.address(packet.getSourceAddress()) +
                                      " destination=" + Logging.address(packet.getDestinationAddress()) +
                                      " identification=" + packet.getFragmentIdentifier() +
                                      " fragment-offset=" + packet.getFragmentOffset() +
                                      " identifier-hashCode=" + identifier.hashCode()));
        }

        ReassemblyBuffer reassemblyBuffer = this.cache.get(identifier);
        if (reassemblyBuffer == null) {

            if (this.maxCacheSize != 0 && this.cache.size() >= this.maxCacheSize) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(this.log.msg("reassembly cache is full cache-size=" + this.cache.size() + " queue-size="
                                              + this.pendingQueue.size()));
                }
                // Indicate that the packet can't be processed yet
                return result;
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(this.log.msg("creating new reassembly buffer"));
            }
            reassemblyBuffer = new ReassemblyBuffer(identifier, REASSEMBLY_TIMEOUT);
            this.cache.put(identifier, reassemblyBuffer);
            result = Result.Started;
            if (this.reaperTask == null) {
                this.reaperTask = new TimerTask() {

                    @Override
                    public void run() {
                        try {
                            reap(false);
                        }
                        catch (Exception e) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.fine(PacketAssembler.this.log.msg("exception thrown in packet reassembly timer task - " +
                                                                         e.getClass().getName() + ":" + e.getMessage()));
                            }
                        }
                    }
                };
                // Run once a second starting once second from now
                this.taskTimer.schedule(this.reaperTask, 1000, 1000);
            }
        }
        else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(this.log.msg("found existing reassembly buffer"));
            }
        }

        try {
            if (reassemblyBuffer.addFragment(packet)) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(this.log.msg("reassembly complete"));
                }
                this.outputChannel.send(reassemblyBuffer.getCompletedPacket(), milliseconds);
                this.cache.remove(reassemblyBuffer.getIdentifier());
                result = Result.Completed;
            }
            else if (result == Result.Denied) {
                result = Result.Added;
            }
        }
        catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.msg("reassembly cache-size=" + this.cache.size() + " queue-size=" + this.pendingQueue.size()));
        }

        // Indicate the end result
        return result;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        reap(true);
        this.outputChannel.close();
    }

    /**
     * @param reapAll
     * @throws InterruptedIOException
     * @throws IOException
     * @throws InterruptedException
     */
    private void reap(final boolean reapAll) throws InterruptedIOException, IOException, InterruptedException {

        synchronized (this.lock) {

            long currentTimeMillis = System.currentTimeMillis();

            if (reapAll) {
                this.pendingQueue.clear();
                this.cache.clear();
            }
            else {
                // Iterate over cache and check age of each entry
                // Copy key set to avoid concurrent modification errors
                HashSet<ByteBuffer> identifierSet = new HashSet<ByteBuffer>(this.cache.keySet());

                Iterator<ByteBuffer> iter = identifierSet.iterator();
                while (iter.hasNext()) {

                    // Get the next reassembly buffer
                    // The buffer will no longer exist if flushed by the
                    // processPendingPackets() call below.
                    ReassemblyBuffer buffer = this.cache.get(iter.next());
                    if (buffer != null && buffer.isExpired(currentTimeMillis)) {

                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(this.log.msg("reassembly timeout exceeded buffer-identifier="
                                                      + Logging.identify(buffer.getIdentifier()) +
                                                      " identifier-hashCode=" + buffer.getIdentifier().hashCode() +
                                                      " age=" + buffer.getAge(currentTimeMillis) + "ms"));
                        }

                        if (this.timeoutChannel != null) {
                            IPPacket packet = buffer.getFragmentZeroPacket();
                            if (packet != null) {
                                if (!Precondition.isMulticastAddress(packet.getDestinationAddress())) {
                                    this.timeoutChannel.send(packet, Integer.MAX_VALUE);
                                }
                            }
                        }

                        // Remove the reassembly buffer from the cache now that we're done
                        // with it.
                        iter.remove();

                    }
                }

                // If the cache size was limited, there may be packets pending
                // Now that the cache size is reduced, we can process these to create a
                // new entries
                if (this.maxCacheSize > 0 && this.cache.size() < this.maxCacheSize) {
                    processPendingPackets(Integer.MAX_VALUE);
                }

                if (this.cache.isEmpty()) {
                    this.reaperTask.cancel();
                    this.reaperTask = null;
                }
            }
        }
    }

}
