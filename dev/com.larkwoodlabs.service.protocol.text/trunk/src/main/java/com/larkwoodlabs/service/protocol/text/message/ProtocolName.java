package com.larkwoodlabs.service.protocol.text.message;

/**
 * A message protocol identifier (e.g. HTTP, RTSP, etc.).
 *
 * @author Gregory Bumgardner
 */
public final class ProtocolName {
    
    String name;
    
    public ProtocolName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public boolean equals(Object object) {
        return (object instanceof ProtocolName) && this.name.equals(((ProtocolName)object).name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
