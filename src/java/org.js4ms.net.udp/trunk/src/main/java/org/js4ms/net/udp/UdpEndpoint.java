/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: UdpEndpoint.java (org.js4ms.net.udp)
 * 
 * Copyright (C) 2009-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.js4ms.net.udp;

import java.net.InetSocketAddress;

import org.js4ms.channels.DuplexChannel;



/**
 * A {@link DuplexChannel} used to send and receive {@link UdpDatagram} objects.
 * Classes that implement this interface typically provide some form of UDP transport,
 * using regular sockets or some form of tunneling protocol.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public interface UdpEndpoint
                extends DuplexChannel<UdpDatagram> {

    InetSocketAddress getLocalSocketAddress();

    InetSocketAddress getRemoteSocketAddress();

}
