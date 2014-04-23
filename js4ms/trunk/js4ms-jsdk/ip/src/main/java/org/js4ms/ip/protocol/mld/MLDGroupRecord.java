package org.js4ms.ip.protocol.mld;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MLDGroupRecord.java [org.js4ms.jsdk:ip]
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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.BufferBackedObject;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.logging.Logging;




/**
 * A Multicast Address Record from a Multicast Listener Report Message as
 * described in [<a href="http://tools.ietf.org/html/rfc3810">RFC-3810</a>].
 * <p>
 * Each Multicast Address Record is a block of fields that contain information on the
 * sender listening to a single multicast address on the interface from which the Report
 * is sent.
 * <h3>Record Format</h3> <blockquote>
 * 
 * <pre>
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Record Type  |  Aux Data Len |     Number of Sources (N)     |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                     Multicast Address                         +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Source Address [1]                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Source Address [2]                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  .                               .                               .
 *  .                               .                               .
 *  .                               .                               .
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Source Address [N]                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                         Auxiliary Data                        .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Record Type</u></dt>
 * <p>
 * <dd>It specifies the type of the Multicast Address Record. See section 5.2.12 for a
 * detailed description of the different possible Record Types.
 * <p>
 * See {@link #getType()}, {@link #MODE_IS_INCLUDE}, {@link #MODE_IS_EXCLUDE},
 * {@link #CHANGE_TO_INCLUDE_MODE}, {@link #CHANGE_TO_EXCLUDE_MODE},
 * {@link #ALLOW_NEW_SOURCES} and {@link #BLOCK_OLD_SOURCES}.</dd>
 * <p>
 * <dt><u>Aux Data Len</u></dt>
 * <p>
 * <dd>The Aux Data Len field contains the length of the Auxiliary Data Field in this
 * Multicast Address Record, in units of 32-bit words. It may contain zero, to indicate
 * the absence of any auxiliary data.
 * <p>
 * See {@link #getAuxDataLength()}.</dd>
 * <p>
 * <dt><u>Number of Sources (N)</u></dt>
 * <p>
 * <dd>The Number of Sources (N) field specifies how many source addresses are present in
 * this Multicast Address Record.
 * <p>
 * See {@link #getNumberOfSources()}.</dd>
 * <p>
 * <dt><u>Multicast Address</u></dt>
 * <p>
 * <dd>The Multicast Address field contains the multicast address to which this Multicast
 * Address Record pertains.
 * <p>
 * See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])} and
 * {@link #setGroupAddress(InetAddress)}.</dd>
 * <p>
 * <dt><u>Source Address [i]</u></dt>
 * <p>
 * <dd>The Source Address [i] fields are a vector of n unicast addresses, where n is the
 * value in this record's Number of Sources (N) field.
 * <p>
 * See {@link #getSource(int)}, {@link #addSource(byte[])} and
 * {@link #addSource(InetAddress)}.</dd>
 * <p>
 * <dt><u>Auxiliary Data</u></dt>
 * <p>
 * <dd>The Auxiliary Data field, if present, contains additional information that pertain
 * to this Multicast Address Record. The protocol specified in this document, MLDv2, does
 * not define any auxiliary data. Therefore, implementations of MLDv2 MUST NOT include any
 * auxiliary data (i.e., MUST set the Aux Data Len field to zero) in any transmitted
 * Multicast Address Record, and MUST ignore any such data present in any received
 * Multicast Address Record. The semantics and the internal encoding of the Auxiliary Data
 * field are to be defined by any future version or extension of MLD that uses this field.
 * <p>
 * See {@link #getAuxData()}.</dd>
 * </dl>
 * </blockquote>
 * <h3>Multicast Address Record Types</h3> There are a number of different types of
 * Multicast Address Records that may be included in a Report message:
 * <ul>
 * <li>A "Current State Record" is sent by a node in response to a Query received on an
 * interface. It reports the current listening state of that interface, with respect to a
 * single multicast address. The Record Type of a Current State Record may be one of the
 * following two values:
 * <p>
 * <dl>
 * <dt>{@link #MODE_IS_INCLUDE} = 1
 * <dd>Indicates that the interface has a filter mode of INCLUDE for the specified
 * multicast address. The Source Address [i] fields in this Multicast Address Record
 * contain the interface's source list for the specified multicast address. A
 * MODE_IS_INCLUDE Record is never sent with an empty source list.
 * <dt>{@link #MODE_IS_EXCLUDE} = 2
 * <dd>Indicates that the interface has a filter mode of EXCLUDE for the specified
 * multicast address. The Source Address [i] fields in this Multicast Address Record
 * contain the interface's source list for the specified multicast address, if it is
 * non-empty.
 * </dl>
 * <li>A "Filter Mode Change Record" is sent by a node whenever a local invocation of
 * IPv6MulticastListen causes a change of the filter mode (i.e., a change from INCLUDE to
 * EXCLUDE, or from EXCLUDE to INCLUDE) of the interface-level state entry for a
 * particular multicast address, whether the source list changes at the same time or not.
 * The Record is included in a Report sent from the interface on which the change
 * occurred. The Record Type of a Filter Mode Change Record may be one of the following
 * two values:
 * <p>
 * <dl>
 * <dt>{@link #CHANGE_TO_INCLUDE_MODE} = 3
 * <dd>Indicates that the interface has changed to INCLUDE filter mode for the specified
 * multicast address. The Source Address [i] fields in this Multicast Address Record
 * contain the interface's new source list for the specified multicast address, if it is
 * non-empty.
 * <dt>{@link #CHANGE_TO_EXCLUDE_MODE} = 4
 * <dd>Indicates that the interface has changed to EXCLUDE filter mode for the specified
 * multicast address. The Source Address [i] fields in this Multicast Address Record
 * contain the interface's new source list for the specified multicast address, if it is
 * non-empty.
 * </dl>
 * <li>A "Source List Change Record" is sent by a node whenever a local invocation of
 * IPv6MulticastListen causes a change of source list that is *not* coincident with a
 * change of filter mode, of the interface-level state entry for a particular multicast
 * address. The Record is included in a Report sent from the interface on which the change
 * occurred. The Record Type of a Source List Change Record may be one of the following
 * two values:
 * <p>
 * <dl>
 * <dt>{@link #ALLOW_NEW_SOURCES} = 5
 * <dd>Indicates that the Source Address [i] fields in this Multicast Address Record
 * contain a list of the additional sources that the node wishes to listen to, for packets
 * sent to the specified multicast address. If the change was to an INCLUDE source list,
 * these are the addresses that were added to the list; if the change was to an EXCLUDE
 * source list, these are the addresses that were deleted from the list.
 * <dt>{@link #BLOCK_OLD_SOURCES} = 6
 * <dd>Indicates that the Source Address [i] fields in this Multicast Address Record
 * contain a list of the sources that the node no longer wishes to listen to, for packets
 * sent to the specified multicast address. If the change was to an INCLUDE source list,
 * these are the addresses that were deleted from the list; if the change was to an
 * EXCLUDE source list, these are the addresses that were added to the list.
 * </dl>
 * If a change of source list results in both allowing new sources and blocking old
 * sources, then two Multicast Address Records are sent for the same multicast address,
 * one of type ALLOW_NEW_SOURCES and one of type BLOCK_OLD_SOURCES.
 * </ul>
 * <p>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class MLDGroupRecord
                extends BufferBackedObject {

    /**
     * Record type used to indicate that the interface has a filter mode of
     * INCLUDE for a specified multicast address. The Source Address [i]
     * fields in the Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public final static byte MODE_IS_INCLUDE = 1;

    /**
     * Record type used to indicate that the interface has a filter mode of
     * EXCLUDE for a specified multicast address. The Source Address [i]
     * fields in the Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public final static byte MODE_IS_EXCLUDE = 2;

    /**
     * Record type used to indicate that the interface changed to
     * INCLUDE filter mode for a specified address. The Source Address [i]
     * fields the Group Record contain the interface's new list for the
     * specified multicast address, if it is non-empty.
     */
    public final static byte CHANGE_TO_INCLUDE_MODE = 3;

    /**
     * Record type used to indicate that the interface has changed to
     * EXCLUDE filter mode for the specified multicast address. The Source
     * Address [i] fields in the Group Record contain the interface's new
     * source list for the specified multicast address, if it is non-empty.
     */
    public final static byte CHANGE_TO_EXCLUDE_MODE = 4;

    /**
     * Record type used to indicate that the Source Address [i] fields in
     * the Group Record contain a list of the additional sources that the
     * system wishes to hear from, for packets sent to the specified
     * multicast address. If the change was to an INCLUDE source list, these
     * are the addresses that were added to the list; if the change was to
     * an EXCLUDE source list, these are the addresses that were deleted
     * from the list.
     */
    public final static byte ALLOW_NEW_SOURCES = 5;

    /**
     * Record type used to indicate that the Source Address [i] fields in
     * the Group Record contain a list of the sources that the system no
     * longer wishes to hear from, for packets sent to the specified
     * multicast address. If the change was to an INCLUDE source list, these
     * are the addresses that were deleted from the list; if the change was
     * to an EXCLUDE source list, these are the addresses that were added to
     * the list.
     */
    public final static byte BLOCK_OLD_SOURCES = 6;

    /** */
    public static final int BASE_RECORD_LENGTH = 8;

    /** */
    public static final ByteField RecordType = new ByteField(0);

    /** */
    public static final ByteField AuxDataLen = new ByteField(1);

    /** */
    public static final ShortField NumberOfSources = new ShortField(2);

    /** */
    public static final ByteArrayField GroupAddress = new ByteArrayField(4, 16);

    /**
     * @param type
     * @return
     */
    public static String getTypeName(final byte type) {
        switch (type) {
            case MODE_IS_INCLUDE:
                return "MODE_IS_INCLUDE";
            case MODE_IS_EXCLUDE:
                return "MODE_IS_EXCLUDE";
            case CHANGE_TO_INCLUDE_MODE:
                return "CHANGE_TO_INCLUDE_MODE";
            case CHANGE_TO_EXCLUDE_MODE:
                return "CHANGE_TO_EXCLUDE_MODE";
            case ALLOW_NEW_SOURCES:
                return "ALLOW_NEW_SOURCES";
            case BLOCK_OLD_SOURCES:
                return "BLOCK_OLD_SOURCES";
            default:
                return "UNRECOGINIZED TYPE!";
        }
    }

    /**
     * @param buffer
     * @return
     */
    public static short calculateGroupRecordSize(final ByteBuffer buffer) {
        return (short) (BASE_RECORD_LENGTH + NumberOfSources.get(buffer) * 16 + AuxDataLen.get(buffer) * 4);
    }

    /** */
    final private Vector<byte[]> sources = new Vector<byte[]>();

    /** */
    private ByteBuffer auxData;

    /**
     * @param type
     * @param groupAddress
     */
    public MLDGroupRecord(final byte type, final byte[] groupAddress) {
        this(type, groupAddress, null);

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger
                            .finer(this.log.entry("MLDGroupRecord.MLDGroupRecord", type, Logging.address(groupAddress)));
        }
    }

    /**
     * @param type
     * @param groupAddress
     * @param auxData
     */
    public MLDGroupRecord(final byte type, final byte[] groupAddress, final ByteBuffer auxData) {
        super(BASE_RECORD_LENGTH);

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.MLDGroupRecord", type,
                                                   Logging.address(groupAddress), auxData));
        }

        setType(type);
        setGroupAddress(groupAddress);
        setAuxData(auxData);

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            logState(MLDMessage.logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public MLDGroupRecord(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_RECORD_LENGTH));

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.MLDGroupRecord", buffer));
        }

        int count = getNumberOfSources();
        for (int i = 0; i < count; i++) {
            byte[] address = new byte[16];
            buffer.get(address);
            this.sources.add(address);
        }

        this.auxData = consume(buffer, getAuxDataLength() * 4);

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            logState(MLDMessage.logger, Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return MLDMessage.logger;
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": record-type=" + getType() + " " + getTypeName(getType())));
        logger.log(level,this.log.msg(": aux-data-length=" + getAuxDataLength()));
        logger.log(level,this.log.msg(": number-of-sources=" + getNumberOfSources()));
        logger.log(level,this.log.msg(": group-address=" + Logging.address(getGroupAddress())));
        logger.log(level,this.log.msg("----> start sources"));
        for (int i = 0; i < getNumberOfSources(); i++) {
            logger.log(level,this.log.msg(": source[" + i + "]=" + Logging.address(getSource(i))));
        }
        logger.log(level,this.log.msg("<---- end sources"));
        logger.log(level,this.log.msg(": aux-data=" + getAuxData()));
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.writeTo", buffer));
        }

        // Update fields
        setAuxDataLength(this.auxData != null ? this.auxData.limit() : 0);
        setNumberOfSources((short) this.sources.size());

        super.writeTo(buffer);
        Iterator<byte[]> iter = this.sources.iterator();
        while (iter.hasNext()) {
            buffer.put(iter.next());
        }

        if (this.auxData != null && this.auxData.limit() > 0) {
            buffer.put(this.auxData);
        }
    }

    /**
     * @return
     */
    public int getRecordLength() {
        return BASE_RECORD_LENGTH + getNumberOfSources() * 16 + getAuxDataLength();
    }

    /**
     * @return
     */
    public byte getType() {
        return RecordType.get(getBufferInternal());
    }

    /**
     * @param type
     */
    public void setType(final byte type) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.setType", type));
        }

        if (type == MODE_IS_INCLUDE ||
            type == MODE_IS_EXCLUDE ||
            type == CHANGE_TO_INCLUDE_MODE ||
            type == CHANGE_TO_EXCLUDE_MODE ||
            type == ALLOW_NEW_SOURCES ||
            type == BLOCK_OLD_SOURCES) {
            RecordType.set(getBufferInternal(), type);
        }
        else {
            throw new IllegalArgumentException("invalid group record type specified");
        }
    }

    /**
     * @return
     */
    public short getNumberOfSources() {
        return NumberOfSources.get(getBufferInternal());
    }

    /**
     * @param numberOfSources
     */
    protected void setNumberOfSources(final short numberOfSources) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.setNumberOfSources", numberOfSources));
        }

        NumberOfSources.set(getBufferInternal(), numberOfSources);
    }

    /**
     * Returns the number of auxiliary data words attached to the group record.
     * 
     * @return The data length as a number of 32-bit words.
     */
    public int getAuxDataLength() {
        return this.auxData != null ? (this.auxData.limit() + 3) / 4 : AuxDataLen.get(getBufferInternal());
    }

    /**
     * Sets the auxiliary data length field.
     * 
     * @param length
     *            - the data length specified as a number of 32-bit words.
     */
    protected void setAuxDataLength(final int length) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.setAuxDataLength", length));
        }

        AuxDataLen.set(getBufferInternal(), (byte) length);
    }

    /**
     * @return
     */
    public byte[] getGroupAddress() {
        return GroupAddress.get(getBufferInternal());
    }

    /**
     * @param groupAddress
     */
    public void setGroupAddress(final InetAddress groupAddress) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.setGroupAddress", Logging.address(groupAddress)));
        }

        setGroupAddress(groupAddress.getAddress());
    }

    /**
     * @param groupAddress
     */
    public void setGroupAddress(final byte[] groupAddress) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.setGroupAddress", Logging.address(groupAddress)));
        }

        // Precondition.checkIPv6MulticastAddress(groupAddress);
        GroupAddress.set(getBufferInternal(), groupAddress);
    }

    /**
     * @param sourceAddress
     * @throws UnknownHostException
     */
    public void addSource(final InetAddress sourceAddress) throws UnknownHostException {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.addSource", Logging.address(sourceAddress)));
        }

        // Precondition.checkReference(sourceAddress);
        addSource(sourceAddress.getAddress());
    }

    /**
     * @param sourceAddress
     * @return
     */
    public int addSource(final byte[] sourceAddress) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.addSource", Logging.address(sourceAddress)));
        }

        // Precondition.checkIPv6Address(sourceAddress);
        if (sourceAddress.length != 6) {

            if (MLDMessage.logger.isLoggable(Level.FINE)) {
                MLDMessage.logger.fine(this.log.msg("invalid source address - MLD messages only allow use of IPv6 addresses"));
            }

            throw new IllegalArgumentException("invalid source address - MLD messages only allow use of IPv6 addresses");
        }
        int index = this.sources.size();
        this.sources.add(sourceAddress.clone());
        setNumberOfSources((short) this.sources.size());
        return index;
    }

    /**
     * @param index
     * @return
     */
    public byte[] getSource(final int index) {
        return this.sources.get(index);
    }

    /**
     * @param index
     */
    public void removeSource(final int index) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.removeSource", index));
        }

        this.sources.remove(index);
    }

    /**
     * @return
     */
    public ByteBuffer getAuxData() {
        return this.auxData;
    }

    /**
     * @param auxData
     */
    public void setAuxData(final ByteBuffer auxData) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(this.log.entry("MLDGroupRecord.setAuxData", auxData));
        }

        this.auxData = auxData.slice();
        setAuxDataLength(this.auxData != null ? (this.auxData.limit() + 3) / 4 : 0);
    }

}
