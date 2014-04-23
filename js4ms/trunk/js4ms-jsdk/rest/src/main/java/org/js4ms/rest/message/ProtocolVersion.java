package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ProtocolVersion.java [org.js4ms.jsdk:rest]
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


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.js4ms.common.exception.ParseException;




/**
 * A protocol version identifier.
 * Provides methods for parsing and serializing a protocol version
 * string that appears in request and response messages.
 *
 * @author Gregory Bumgardner
 */
public final class ProtocolVersion {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * Regular expression used to parse a protocol version string.
     */
    public static final Pattern pattern = Pattern.compile("([A-Z]+)/([0-9])\\.([0-9])");
    
    /*-- Member Variables ----------------------------------------------------*/

    private ProtocolName protocol;
    private int majorVersion;
    private int minorVersion;
    private String string = null;
    private byte[] bytes = null;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Parses a protocol version specification string to produce a {@link ProtocolVersion} instance.
     * @param bytes - A UTF-8 encoded string containing a protocol version specification (e.g. "HTTP/1.0").
     * @throws ParseException If the protocol version string is malformed.
     */
    public static ProtocolVersion parse(final byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    /**
     * Parses a protocol version specification string to produce a {@link ProtocolVersion} instance.
     * @param string - A string containing a protocol version specification (e.g. "HTTP/1.0").
     * @throws ParseException If the protocol version string is malformed.
     */
    public static ProtocolVersion parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid protocol version");
        }

        try {
            return new ProtocolVersion(new ProtocolName(matcher.group(1)),
                                       Integer.parseInt(matcher.group(2)),
                                       Integer.parseInt(matcher.group(3)));
        }
        catch (Exception e) {
            // Should not get here
            throw new ParseException(e);
        }
    }

    /**
     * Indicates whether the specified string is formatted as a protocol version field.
     * @param string
     * @return
     */
    public static boolean matches(final String string) {
        return pattern.matcher(string).matches();
    }

    /**
     * Constructs a version object for the specified protocol type and version.
     * @param protocol - The message {@link ProtocolName}.
     * @param majorVersion - The major version of the protocol.
     * @param minorVersion - The minor version of the protocol.
     */
    public ProtocolVersion(final String protocol, final int majorVersion, final int minorVersion) {
        this.protocol = new ProtocolName(protocol);
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    /**
     * Constructs a version object for the specified protocol type and version.
     * @param protocol - The message {@link ProtocolName}.
     * @param majorVersion - The major version of the protocol.
     * @param minorVersion - The minor version of the protocol.
     */
    public ProtocolVersion(final ProtocolName protocol, final int majorVersion, final int minorVersion) {
        this.protocol = protocol;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    /**
     * Returns the {@link ProtocolName} component of the version.
     */
    public ProtocolName getProtocolName() {
        return this.protocol;
    }

    /**
     * Returns the major version number.
     */
    public int getMajorVersion() {
        return this.majorVersion;
    }
    
    /**
     * Returns the major version number.
     */
    public int getMinorVersion() {
        return this.minorVersion;
    }

    /**
     * Returns the protocol version specification string for this object (e.g. "RTSP/1.0").
     */
    @Override
    public String toString() {
        if (this.string == null) {
            this.string = this.protocol.getName() + "/" + this.majorVersion + "." + this.minorVersion;
        }
        return this.string;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object instanceof ProtocolVersion) {
            ProtocolVersion other = (ProtocolVersion)object;
            return this.protocol.equals(other.protocol) &&
                   this.majorVersion == other.majorVersion &&
                   this.minorVersion == other.minorVersion;
        }
        return false;
    }
    
    /**
     * Writes the protocol version specification string for this object to the specified
     * OutputStream as a UTF-8 encoded string of bytes.
     * @param outstream - The destination OutputStream.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(final OutputStream os) throws IOException {
        if (this.bytes == null) {
            try {
                this.bytes = toString().getBytes("UTF8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        os.write(this.bytes);
    }
}
