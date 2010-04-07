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
import java.util.logging.Level;

import com.larkwoodlabs.io.Base64InputStream;
import com.larkwoodlabs.util.logging.Logging;

public final class ServerTunnelConnection extends Connection {
    

    /*-- Member Variables ----------------------------------------------------*/

    private final Server server;
    
    private final String sessionCookie;
    
    private final Connection inputConnection;
    
    private final Connection outputConnection;
    
    
    /**
     * 
     * @param sessionCookie
     * @param inputStream
     * @param outputStream
     */
    public ServerTunnelConnection(Server server,
                                  final String sessionCookie,
                                  final Connection inputConnection,
                                  final Connection outputConnection) {
        super(inputConnection.getRemoteAddress(), new Base64InputStream(inputConnection.getInputStream()), outputConnection.getOutputStream());

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerTunnelConnection.ServerTunnelConnection", server, sessionCookie, inputConnection, outputConnection));
        }

        this.server = server;
        this.sessionCookie = sessionCookie;
        this.inputConnection = inputConnection;
        this.outputConnection = outputConnection;
    }
    
    @Override
    public void shutdownInput() throws IOException {
        this.inputConnection.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        this.outputConnection.shutdownOutput();
    }

    @Override
    public void close() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerTunnelConnection.close"));
        }

        this.inputConnection.close();
        this.outputConnection.close();
        
        this.server.removeOutputConnection(this.sessionCookie);
    }

}
