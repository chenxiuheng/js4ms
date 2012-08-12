package com.larkwoodlabs.net.messaging;

/**
 * A message protocol identifier (e.g. HTTP, RTSP, etc.).
 *
 * @author Gregory Bumgardner
 */
public final class Protocol {
    
    String name;
    
    public Protocol(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public boolean equals(Object object) {
        return (object instanceof Protocol) && this.name.equals(((Protocol)object).name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
