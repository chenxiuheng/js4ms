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

/*
* Conditions Of Use 
* 
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
* 
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
* 
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*  
* .
* 
*/
package com.larkwoodlabs.net.streaming.sdp;
import gov.nist.core.ParserCore;
import gov.nist.core.Token;
import gov.nist.javax.sdp.parser.Lexer;

import java.net.UnknownHostException;
import java.text.ParseException;

/**
 * Parser for the SDP source-filter attribute.<br/>
 * The source-filter attribute has the following syntax:
 * 
 * <pre>a=source-filter: &lt;filter-mode> &lt;filter-spec></pre>
 * 
 * The &ltfilter-mode> is either "incl" or "excl" (for inclusion or exclusion,
 * respectively). The &lt;filter-spec> has four sub-components:
 * 
 * <pre>&lt;nettype> &lt;address-types> &lt;dest-address> &lt;src-list></pre>
 * 
 * A &lt;filter-mode> of "incl" means that an incoming packet is accepted only if
 * its source address is in the set specified by &lt;src-list>. A &lt;filter-mode> of
 * "excl" means that an incoming packet is rejected if its source address is in
 * the set specified by &lt;src-list>.
 * <p>
 * The first sub-field, &lt;nettype>, indicates the network type, since SDP is
 * protocol independent. This document is most relevant to the value "IN", which
 * designates the Internet Protocol.
 * <p>
 * The second sub-field, &lt;address-types>, identifies the address family, and for
 * the purpose of this document may be either &lt;addrtype> value "IP4" or "IP6".
 * Alternately, when &lt;dest-address> is an FQDN, the value MAY be "*" to apply to
 * both address types, since either address type can be returned from a DNS
 * lookup.
 * <p>
 * The third sub-field, &lt;dest-address>, is the destination address, which MUST
 * correspond to one or more of the session's "connection- address" field
 * values. It may be either a unicast or multicast address, an FQDN, or the "*"
 * wildcard to match any/all of the session's "connection-address" values.
 * <p>
 * The fourth sub-field, &lt;src-list>, is the list of source hosts/interfaces in
 * the source-filter, and consists of one or more unicast addresses or FQDNs,
 * separated by space characters.
 */
public class SourceFilterAttributeParser extends ParserCore {

    /** Creates new MediaFieldParser */
    public SourceFilterAttributeParser(String attributeValue) {
        lexer = new Lexer("charLexer", attributeValue);
    }

    public SourceFilterSpecification sourceFilterSpecification() throws ParseException, UnknownHostException {
        SourceFilterSpecification filterSpec = new SourceFilterSpecification();

        Token mode = lexer.getNextToken();
        filterSpec.setMode(mode.getTokenValue());

        this.lexer.SPorHT();

        lexer.match(Lexer.ID);
        Token nettype = lexer.getNextToken();
        filterSpec.setNetType(nettype.getTokenValue());

        this.lexer.SPorHT();

        lexer.match(Lexer.ID);
        Token addrtype = lexer.getNextToken();
        filterSpec.setAddrType(addrtype.getTokenValue());

        this.lexer.SPorHT();

        lexer.match(Lexer.ID);
        Token destinationAddress = lexer.getNextToken();
        filterSpec.setDestinationAddress(destinationAddress.getTokenValue());

        // The source address list
        StringBuffer sourceAddress = new StringBuffer();
        while (lexer.hasMoreChars()) {
            char next = lexer.lookAhead(0);
            lexer.consume(1);
            if (next == '\n' || next == '\r' || next == ' ') {
                if (sourceAddress.length() > 0) {
                    filterSpec.addSourceAddress(sourceAddress.toString());
                    if (next == ' ') {
                        sourceAddress = new StringBuffer();
                    }
                }
            } else {
                lexer.consume(1);
                sourceAddress.append(next);
            }
        }

        return filterSpec;
    }

}
