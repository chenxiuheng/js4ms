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

package com.larkwoodlabs.net.udp;

import java.io.IOException;
import java.net.InetAddress;

import com.larkwoodlabs.channels.MessageInput;

public interface MulticastEndpoint extends MessageInput<UdpDatagram> {

    void join(InetAddress groupAddress) throws IOException;
    void join(InetAddress groupAddress, InetAddress sourceAddress) throws IOException;
    void join(InetAddress groupAddress, int port) throws IOException;
    void join(InetAddress groupAddress, InetAddress sourceAddress, int port) throws IOException;

    void leave(InetAddress groupAddress) throws IOException;
    void leave(InetAddress groupAddress, InetAddress sourceAddress) throws IOException;
    void leave(InetAddress groupAddress, int port) throws IOException;
    void leave(InetAddress groupAddress, InetAddress sourceAddress, int port) throws IOException;
    void leave() throws IOException;
}
