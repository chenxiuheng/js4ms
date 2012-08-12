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

package com.larkwoodlabs.net.streaming.sdp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

public class SourceFilterSpecification {

    public static final String MODE_INCL = "incl";
    public static final String MODE_EXCL = "excl";
    public static final String NETTYPE_IN = "IN";

    public static final String ADDRTYPE_IP4 = "IP4";
    public static final String ADDRTYPE_IP6 = "IP6";
    public static final String ADDRTYPE_WILD = "*";
    
    private String mode = MODE_INCL;
    private String nettype = NETTYPE_IN;
    private String addrtype = ADDRTYPE_IP4;
    private InetAddress destination;
    private Vector<InetAddress> sources;

    public SourceFilterSpecification() {
    }
    
    public void setMode(String mode) {
        if (mode != MODE_INCL && mode != MODE_EXCL ) {
            throw new IllegalArgumentException(mode + " is not a valid source filter mode");
        }
        this.mode = mode;
    }
    
    public String getMode() {
        return this.mode;
    }

    public void setNetType(String nettype) {
        if (nettype != NETTYPE_IN) {
            throw new IllegalArgumentException(nettype + " is not a valid network type");
        }
        this.nettype = nettype;
    }
    
    public String getNetType() {
        return this.nettype;
    }

    public void setAddrType(String addrtype) {
        if (addrtype != ADDRTYPE_IP4 &&
            addrtype != ADDRTYPE_IP6 &&
            addrtype != ADDRTYPE_WILD) {
            throw new IllegalArgumentException(addrtype + " is not a valid address type");
        }
        this.addrtype = addrtype;
    }
    
    public String getAddrType() {
        return this.addrtype;
    }

    public void setDestinationAddress(String address) throws UnknownHostException {

        InetAddress destination = null;
        if (address == "*") {
            if (this.addrtype == ADDRTYPE_IP6) {
                destination = InetAddress.getByName("0:0:0:0:0:0:0:0");
            }
            else {
                // Default to IP4 wildcard address for IP4 and wildcard
                destination = InetAddress.getByName("0.0.0.0");
            }
        }
        else {
            destination = InetAddress.getByName(address);
            byte[] ip = destination.getAddress();
            if (ip.length == 4 && this.addrtype == ADDRTYPE_IP6 ||
                ip.length == 6 && this.addrtype == ADDRTYPE_IP4 ) {
                throw new IllegalArgumentException("destination address does not match address type");
            }
        }

        this.destination = destination;
    }
    
    public InetAddress getDestinationAddress() {
        return this.destination;
    }

    public void addSourceAddress(String address) throws UnknownHostException {
        InetAddress source = InetAddress.getByName(address);
        byte[] ip = source.getAddress();
        if (ip.length == 4 && this.addrtype == ADDRTYPE_IP6 ||
            ip.length == 6 && this.addrtype == ADDRTYPE_IP4 ) {
            throw new IllegalArgumentException("source address does not match address type");
        }
        this.sources.add(source);
    }
    
    public Vector<InetAddress> getSourceAddresses() {
        return this.sources;
    }
}