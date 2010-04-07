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

import java.net.InetSocketAddress;

import com.larkwoodlabs.channels.DuplexChannel;

/**
 * A {@link DuplexChannel} used to send and receive {@link UdpDatagram} objects.
 * Classes that implement this interface typically provide some form of UDP transport,
 * using regular sockets or some form of tunneling protocol.
 *
 * @author Gregory Bumgardner
 */
public interface UdpEndpoint extends DuplexChannel<UdpDatagram> {

    InetSocketAddress getLocalSocketAddress();
    InetSocketAddress getRemoteSocketAddress();

}
