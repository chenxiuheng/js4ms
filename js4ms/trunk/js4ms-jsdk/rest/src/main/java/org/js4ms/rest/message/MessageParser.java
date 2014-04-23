package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageParser.java [org.js4ms.jsdk:rest]
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.logging.Log;
import org.js4ms.rest.common.MessageException;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.RawEntity;
import org.js4ms.server.Connection;




public abstract class MessageParser {

    /*-- Static Constants ----------------------------------------------------*/

    /**
     * 
     */
    protected static final int DEFAULT_MAX_NUMBER_OF_HEADERS = 32;
    protected static final int DEFAULT_MAX_LINE_SIZE = 8192;

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(MessageParser.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    protected int maxNumberOfHeaders = DEFAULT_MAX_NUMBER_OF_HEADERS;
    protected int maxLineSize = DEFAULT_MAX_LINE_SIZE;

    protected final MessageHeaderParser headerParser;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param connection
     */
    protected MessageParser(final MessageHeaderParser headerParser) {
        this.headerParser = headerParser;
    }

    public void parse(final Connection connection) throws IOException, MessageException, ParseException {
        doParse(connection);
    }

    /**
     * Reads and parses a single message from the specified connection and connection input stream.
     * The input stream argument may be the same one exposed by the connection, or may be an input stream
     * that wraps the connection input stream.
     * This method delegates parsing of message start line to derived classes via {@link #doParseStartLine}
     * and delegates message handling to derived classes via abstract method {@link #doHandleMessage()}. 
     * @throws MessageException - The downstream message handler rejected the message.
     * @throws ParseException - The parser could not parse the incoming byte stream into a message.
     *                          The caller should abort message processing and close the connection.
     * @throws IOException - A read operation failed because the connection was closed or unexpected IO error occurred.
     */
    protected void parseMessage(final Connection connection, final InputStream inputStream) throws IOException, ParseException, MessageException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("parseMessage", connection, inputStream));
        }

        // Get first character in next message 
        // Throws SocketException if the socket is closed by 
        // another thread while waiting in this call
        int c = inputStream.read();

        if (c == -1) {
            // Peer stopped sending data or input was shutdown
            throw new EOFException("connection stream returned EOF");
        }

        final LinkedHashMap<String, MessageHeader> headers = new LinkedHashMap<String, MessageHeader>();

        byte[] linebuf = new byte[this.maxLineSize];

        // Read bytes from the input stream to extract the start line and headers
        boolean isPrevCRLF = false;

        StartLine startLine = null;

        int j = 0;

        StringBuffer lastHeaderRecord = new StringBuffer();

        while (true) {
            if (c == -1) {
                throw new EOFException("unexpected EOF occurred while reading message");
            }
            if (c == '\r') {

                int lf = inputStream.read();
                if (lf == -1) {
                    throw new EOFException("unexpected EOF occurred while reading message");
                }

                if (lf == '\n') {
                    if (isPrevCRLF) {
                        // We've hit the blank line at the end of the message
                        if (lastHeaderRecord.length() > 0) {
                            MessageHeader header = this.headerParser.parse(lastHeaderRecord.toString());
                            if (headers.containsKey(header.getName())) {
                                headers.get(header.getName()).appendHeader(header);
                            }
                            else {
                                headers.put(header.getName().toLowerCase(), header);
                            }
                        }
                        break;
                    }
                    isPrevCRLF = true;
                    String line = new String(linebuf, 0, j, "UTF8");

                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(log.msg(line));
                    }

                    if (startLine == null) {
                        // An exception will cause next parse attempt to being at the next line.
                        // TODO: Should we flush the contents of the stream since what ever is 
                        // there is not likely a valid message?
                        try {
                            startLine = doParseStartLine(line);
                        }
                        catch (ParseException e) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(log.msg("'"+line+"' is not a valid message start line; "+e.getMessage()));
                            }
                            throw e;
                        }
                    }
                    else {
                        // Are we parsing a continuation of an earlier header record or starting a new one?
                        int firstChar = line.charAt(0);
                        if (firstChar == ' ' || firstChar == '\t') {
                            if (lastHeaderRecord.length() == 0) {
                                throw new MessageException(startLine.getProtocolVersion(),
                                                           "message contains an invalid header");
                            }
                            else {
                                lastHeaderRecord.append(" " + line.trim());
                            }
                        }
                        else {
                            // We're starting a new record - construct a header from the previous record now
                            if (lastHeaderRecord.length() > 0) {
                                try {
                                    if (headers.size() > maxNumberOfHeaders) {
                                        throw new MessageException(startLine.getProtocolVersion(),
                                                                  "message contains too many headers");
                                    }
                                    MessageHeader header = this.headerParser.parse(lastHeaderRecord.toString());
                                    if (headers.containsKey(header.getName())) {
                                        headers.get(header.getName()).appendHeader(header);
                                    }
                                    else {
                                        headers.put(header.getName().toLowerCase(), header);
                                    }
                                }
                                catch (ParseException e) {
                                    throw new MessageException(startLine.getProtocolVersion(),
                                                              "message contains an invalid header");
                                }
                            }
                            /// Start the new record
                            lastHeaderRecord.setLength(0);
                            lastHeaderRecord.append(line.trim());
                        }
                    }
                    // Reset linebuf index to start next line
                    j = 0;
                }
                else {
                    throw new MessageException(startLine.getProtocolVersion(),
                                               "message contains an invalid line terminator");
                }
            }
            else {
                if (j < this.maxLineSize) {
                    isPrevCRLF = false;
                    linebuf[j++] = (byte) c;
                }
                else {
                    throw new MessageException(startLine.getProtocolVersion(),
                                               "message contains line that exceeds maximum allowable length");
                }
            }

            // Read next character from stream
            c = inputStream.read();
        }

        Message message = doConstructMessage(connection, startLine, headers, null);

        if (message.containsHeader(Entity.CONTENT_LENGTH)) {
            if (!message.getHeader(Entity.CONTENT_LENGTH).getValue().equals("0")) {
                message.setEntity(new RawEntity(inputStream, message));
            }
        }

        doHandleMessage(message);

    }

    /**
     * Reads and parses a single message unit from the connection.
     * Overridden in derived classes to allow for type-specific parser method selection.
     * @param connection 
     * @throws ParseException
     * @throws MessageException
     * @throws IOException 
     */
    protected void doParse(final Connection connection)  throws IOException, ParseException, MessageException {
        parseMessage(connection, connection.getInputStream());
    }

    /**
     * Parses the start line of a message.
     * Overridden in derived classes to provide type-specific parsing.
     * @param startLine
     * @return
     * @throws ParseException - Indicates a non-recoverable error occurred while parsing the start line.
     */
    protected abstract StartLine doParseStartLine(final String startLine) throws ParseException;


    protected abstract Message doConstructMessage(final Connection connection,
                                                  final StartLine startLine,
                                                  final LinkedHashMap<String,MessageHeader> headers,
                                                  final Entity entity);

    /**
     * Sink for messages generated by the {@link parse()} method.
     * Overridden in derived classes to provide type-specific dispatching. 
     * @param message
     */
    protected abstract void doHandleMessage(final Message message) throws MessageException, IOException;


}
