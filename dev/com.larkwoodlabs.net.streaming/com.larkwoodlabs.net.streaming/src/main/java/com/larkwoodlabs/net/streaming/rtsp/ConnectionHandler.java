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

package com.larkwoodlabs.net.streaming.rtsp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.io.FixedLengthInputStream;
import com.larkwoodlabs.util.logging.Logging;


/**
 * @author Gregory Bumgardner
 */
public abstract class ConnectionHandler 
                      implements Runnable {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(ConnectionHandler.class.getName());

    protected static final int MAX_MESSAGE_SIZE = 0xFFF;
    protected static final int MAX_LINE_SIZE = 0x3FF;

    /*-- Member Variables ----------------------------------------------------*/

    protected final Object lock = new Object();
    
    protected final Object monitor = new Object();
    protected boolean isPacketDeliveryEnabled = false;

    protected final String ObjectId = Logging.identify(this);

    protected Connection connection;
    
    protected HashMap<Integer,OutputChannel<ByteBuffer>> packetChannels;
    
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param context
     * @param connection
     */
    public ConnectionHandler(final Connection connection) {
        this.connection = connection;
    }
    
    public void close() throws IOException {
        this.connection.close();
    }

    public Connection getConnection() {
        return this.connection;
    }
    
    @Override
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.run"));
        }

        boolean badRequestResponseRequired = true;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Read one request, response or interleaved packet from the input stream
                    listen();
                    badRequestResponseRequired = true;
                }
                catch (RtspException e) {
                    if (e.getStatusCode() == StatusCode.BadRequest) {
                        if (badRequestResponseRequired) {
                            badRequestResponseRequired = false;
                            Response response = new Response();
                            e.setResponse(response);
                            sendResponse(response);
                        }
                    }
                    else if (e.getStatusCode() != StatusCode.BadResponse){
                        Response response = new Response();
                        e.setResponse(response);
                        sendResponse(response);
                    }
                }
            }
        }
        catch (EOFException e) {
            // Connection was closed by peer or the input was shutdown
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " connection handler exiting for " + e.getClass().getName() + ": " + e.getMessage());
            }
            return;
        }
        catch (SocketException e) {
            // The connection socket was closed by another thread while this thread was waiting on I/O.
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " connection handler exiting for " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
        catch (IOException e) {
            // IO exception occurred - most likely while attempting to send a message or data over a closed connection
            if (logger.isLoggable(Level.FINE)) {
                logger.warning(ObjectId + " connection handler aborted by " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " connection handler thread was interrupted");
            }
        }
        catch (Exception e) {
            // An unexpected exception occurred
            if (logger.isLoggable(Level.FINE)) {
                logger.warning(ObjectId + " connection handler aborted by " + e.getClass().getName() + ":" + e.getMessage());
            }
            e.printStackTrace();
        }

        try {
            this.connection.close();
        }
        catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId +
                            " connection handler cannot close connection - " +
                            e.getClass().getName() + ":" + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    protected void reconnect(final Connection connection) {
        synchronized (this.lock) {
            this.connection = connection;
        }
    }
    
    public void addPacketChannel(final int channelNumber, final OutputChannel<ByteBuffer> channel) {
        if (this.packetChannels == null) {
            this.packetChannels = new HashMap<Integer, OutputChannel<ByteBuffer>>();
        }
        this.packetChannels.put(channelNumber, channel);
    }

    public void removePacketChannel(final int channelNumber) {
        if (this.packetChannels != null) {
            this.packetChannels.remove(channelNumber);
        }
    }

    public void listen() throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.listen"));
        }
        
        InputStream inputStream = this.connection.getInputStream();
        
        // Get first character in message 
        // Throws SocketException if the socket is closed by 
        // another thread while waiting in this call
        int c = inputStream.read();

        if (c == -1) {
            // Peer stopped sending data or input was shutdown
            throw new EOFException("connection stream returned EOF");
        }

        if (c == '$') {
            // receiving an interleaved RTP/RTCP packet
            int channel = inputStream.read();
            if (channel != -1) {
                int msb = inputStream.read();
                if (msb != -1) {
                    int lsb = inputStream.read();
                    if (lsb != -1) {
                        int count = msb << 8 + lsb;
                        byte[] packet = new byte[count];
                        int actual = inputStream.read(packet);
                        if (actual == count) {
                            dispatchPacket(channel, ByteBuffer.wrap(packet));
                            return;
                        }
                    }
                }
            }
            // Peer stopped sending data or input was shutdown
            throw new EOFException("unexpected EOF occurred while reading interleaved packet");
        }
        else {

            final LinkedHashMap<String,Header> headers = new LinkedHashMap<String,Header>();

            String startLine = null;
            
            byte[] linebuf = new byte[MAX_LINE_SIZE];
            
            // Read bytes from the input stream to extract the start line and headers
            boolean isPrevCRLF = false;
            
            // Use status code to indicate message type
            StatusCode code = StatusCode.BadRequest;
 
            int i = 0;
            int j = 0;

            Header lastHeader = null;

            while (true) {
                if (c == -1) {
                    throw new EOFException("unexpected EOF occurred while reading RTSP message");
                }
                if (c == '\r') {
                    int lf = inputStream.read();
                    if (lf == -1) {
                        throw new EOFException("unexpected EOF occurred while reading RTSP message");
                    }
                    if (lf == '\n') {
                        if (isPrevCRLF) {
                            // We've hit the blank line at the end of the RTSP message
                            break;
                        }
                        isPrevCRLF = true;
                        String line = new String(linebuf, 0, j , "UTF8");
                        if (startLine == null) {
                            // First line is the message request or status line
                            startLine = line;
                            if (startLine.startsWith("HTTP") || startLine.startsWith("RTSP")) {
                                code = StatusCode.BadResponse;
                            }
                            else {
                                code = StatusCode.BadRequest;
                            }
                        }
                        else {
                            int firstChar = line.charAt(0);
                            if (firstChar == ' ' || firstChar == '\t') {
                                if (lastHeader == null) {
                                    throw RtspException.create(code, "RTSP message contains an invalid header", ObjectId, logger);
                                }
                                else {
                                    lastHeader.appendFragment(" "+line.trim());
                                }
                            }
                            try {
                                lastHeader = Header.parse(line);
                            }
                            catch (ParseException e) {
                                throw RtspException.create(code, "RTSP message contains an invalid header", e, ObjectId, logger);
                            }
                            if (headers.containsKey(lastHeader.getName())) {
                                Header header = headers.get(lastHeader.getName());
                                header.appendValue(lastHeader.getValue());
                                lastHeader = header;
                            }
                            else {
                                headers.put(lastHeader.getName().toLowerCase(), lastHeader);
                            }
                        }
                        // Reset linebuf index to start next line
                        j = 0;
                    }
                    else {
                        throw RtspException.create(code, "RTSP message contains an invalid line terminator", ObjectId, logger);
                    }
                }
                else {
                    if (i < MAX_MESSAGE_SIZE) {
                        if (j < MAX_LINE_SIZE) {
                            isPrevCRLF = false;
                            i++;
                            linebuf[j++] = (byte)c;
                        }
                        else {
                            throw RtspException.create(code,
                                                       "RTSP message contains line that exceeds maximum allowable length",
                                                       ObjectId, logger);
                        }
                    }
                    else {
                        throw RtspException.create(code,
                                                   "RTSP message exceeds maximum allowable length",
                                                   ObjectId, logger);
                    }
                }
                
                // Read next character from stream
                c = inputStream.read();
            }

            if (startLine == null || startLine.length() == 0) {
                throw RtspException.create(code,
                                           "RTSP message is empty",
                                           ObjectId, logger);
            }
    
            int contentLength = -1;
            String contentType = null;
            String contentEncoding = null;
            
            Header contentLengthHeader = headers.get(Header.CONTENT_LENGTH.toLowerCase());
            if (contentLengthHeader != null) {
                contentLength = Integer.parseInt(contentLengthHeader.getValue());
            }
    
            Header contentTypeHeader = headers.get(Header.CONTENT_TYPE.toLowerCase());
            if (contentTypeHeader != null) {
                contentType = contentTypeHeader.getValue();
            }
    
            Header contentEncodingHeader = headers.get(Header.CONTENT_ENCODING.toLowerCase());
            if (contentEncodingHeader != null) {
                contentEncoding = contentEncodingHeader.getValue();
            }
            
            Entity entity;
    
            if (contentLength > 0 && !contentType.contains(MimeType.application.x_rtsp_tunnelled)) {
                entity = new Entity(new FixedLengthInputStream(inputStream, contentLength), contentType, contentEncoding);
            }
            else {
                entity = null;
            }
            
                if (code == StatusCode.BadResponse) {
                    StatusLine statusLine;
                    try {
                        statusLine = StatusLine.parse(startLine);
                    }
                    catch (ParseException e) {
                        throw RtspException.create(code, e, ObjectId, logger);
                    }
                    Response response = new Response(this, statusLine, headers, entity);
                    dispatchResponse(response);
                }
                else {
                    RequestLine requestLine;
                    try {
                        requestLine = RequestLine.parse(startLine);
                    }
                    catch (ParseException e) {
                        throw RtspException.create(code, e, ObjectId, logger);
                    }
                    Request request = new Request(this, requestLine, headers, entity);
                    dispatchRequest(request);
                }
        }
    }
    
    private void dispatchRequest(Request request) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.dispatchRequest", request));
            request.log(logger);
        }

        handleRequest(request);
    }

    protected abstract void handleRequest(Request request) throws Exception;
    
    private void dispatchResponse(Response response) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.dispatchResponse", response));
            response.log(logger);
        }

        handleResponse(response);
    }
    
    protected abstract void handleResponse(final Response response) throws Exception;
    
    private void dispatchPacket(int channelNumber, ByteBuffer packet) throws RtspException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.dispatchPacket", channelNumber, packet));
        }

        handlePacket(channelNumber, packet);
    }

    protected void handlePacket(final int channelNumber, final ByteBuffer packet) throws RtspException, InterruptedException {
        if (this.packetChannels != null) {
            OutputChannel<ByteBuffer> channel = this.packetChannels.get(channelNumber);
            if (channel != null) {
                try {
                    channel.send(packet, Integer.MAX_VALUE);
                }
                catch (IOException e) {
                    throw RtspException.create(StatusCode.InternalServerError, e, ObjectId, logger);
                }
            }
        }
    }
    
    protected void sendRequest(final Request request) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.sendRequest", request));
            request.log(logger);
        }

        synchronized (this.lock) {
            request.writeTo(this.connection.getOutputStream());
        }
    }
    
    public void sendResponse(final Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.sendResponse", response));
            response.log(logger);
        }

        synchronized (this.lock) {
            response.writeTo(this.connection.getOutputStream());
        }
    }
    
    public void enablePacketDelivery(boolean isEnabled) {
        synchronized (this.monitor) {
            if (this.isPacketDeliveryEnabled != isEnabled) {
                this.isPacketDeliveryEnabled = isEnabled;
                if (isEnabled) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(ObjectId + " notifying threads that packet stream is enabled");
                    }
                    this.monitor.notifyAll();
                }
            }
        }
    }

    public void sendPacket(final int channel, final ByteBuffer packet) throws IOException, InterruptedException {

        /*
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.sendPacket", channel, packet));
        }
        */

        synchronized (this.monitor) {
            if (!this.isPacketDeliveryEnabled) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(ObjectId + " waiting for packet stream to be enable");
                }
                this.monitor.wait();
            }
        }
        
        OutputStream outputStream = this.connection.getOutputStream();

        synchronized (this.lock) {
            int count = packet.limit();
            outputStream.write('$');
            outputStream.write((byte)channel);
            outputStream.write((byte)((count >> 8) & 0xFF));
            outputStream.write((byte)(count & 0xFF));
            outputStream.write(packet.array(), packet.arrayOffset(), count);
        }
    }
}
