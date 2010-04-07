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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * 
 *
 *
 * @author Gregory Bumgardner
 */
public abstract class Connection  {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(Connection.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    protected final String ObjectId = Logging.identify(this);
    
    protected final InetAddress remoteAddress;
    protected final InputStream inputStream;
    protected final OutputStream outputStream;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param inputStream
     * @param outputStream
     */
    protected Connection(final InetAddress remoteAddress, final InputStream inputStream, final OutputStream outputStream) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Connection.Connection", inputStream, outputStream));
        }

        this.remoteAddress = remoteAddress;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }
    

    public InetAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    /**
     * 
     * @return
     */
    public final InputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * 
     * @return
     */
    public final OutputStream getOutputStream() {
        return this.outputStream;
    }

    /**
     * Disables the connection InputStream. Data received from connection
     * peer is silently discarded.
     * Used to provide non-abortive shutdown of connection.
     * The InputStream will return EOF (-1) if an attempt is made to read
     * from the stream after this method has been called.
     * @throws IOException
     */
    public abstract void shutdownInput() throws IOException;

    /**
     * Disables the connection OutputStream. The connection peer is
     * notified that no more data will be sent (FIN).
     * Used to provide non-abortive shutdown of connection;
     * after calling this method, call shutdownInput() or read the InputStream
     * until EOF and then call close().
     * The OutputStream will thrown an IOException if an attempt is made to 
     * write to the stream after this method has been called.
     * @throws IOException
     */
    public abstract void shutdownOutput() throws IOException;
    
    /**
     * Closes the connection.
     * The connection with the peer is aborted and any threads waiting
     * to read from the connection will throw a SocketException.
     * @throws IOException
     */
    public abstract void close() throws IOException;

}
