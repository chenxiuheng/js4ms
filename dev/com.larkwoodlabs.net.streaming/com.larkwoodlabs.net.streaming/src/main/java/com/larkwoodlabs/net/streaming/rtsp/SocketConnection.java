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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;


public class SocketConnection extends Connection {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(SocketConnection.class.getName());

    final Socket socket;
    
    public SocketConnection(final Socket socket) throws IOException {
        super(socket.getInetAddress(), new BufferedInputStream(socket.getInputStream()), new BufferedOutputStream(socket.getOutputStream()));
        this.socket = socket;
        this.socket.setSoTimeout(0);
    }
    
    @Override
    public void shutdownInput() throws IOException {
        if (!this.socket.isInputShutdown()) {
            this.socket.shutdownInput();
        }
    }

    @Override
    public void shutdownOutput() throws IOException {
        if (!this.socket.isOutputShutdown()) {
            this.socket.shutdownOutput();
        }
    }

    @Override
    public void close() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "SocketConnection.close"));
        }

        socket.close();
    }

}
