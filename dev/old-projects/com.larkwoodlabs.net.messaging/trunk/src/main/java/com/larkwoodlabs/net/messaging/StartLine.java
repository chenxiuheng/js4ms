package com.larkwoodlabs.net.messaging;

import java.io.IOException;
import java.io.OutputStream;


public abstract class StartLine {
    
    /*-- Member Variables ----------------------------------------------------*/

    protected ProtocolVersion protocolVersion;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a message start line for the specified protocol version.
     * @param protocolVersion - The protocol version for the request.
     */
    protected StartLine(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    /**
     * Returns the {@link ProtocolVersion} of this start line.
     * @return The current value of the {@link ProtocolVersion} property.
     */
    public final ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }
    
    /**
     * Sets the {@link ProtocolVersion} of this start line.
     * @param protocolVersion - The new protocol version.
     */
    public final void setProtocolVersion(final ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public abstract void writeTo(final OutputStream os) throws IOException;

}
