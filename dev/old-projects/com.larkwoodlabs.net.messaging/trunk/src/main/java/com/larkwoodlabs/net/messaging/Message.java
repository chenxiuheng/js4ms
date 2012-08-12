package com.larkwoodlabs.net.messaging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;


public abstract class Message {

    /*-- Member Variables ----------------------------------------------------*/

    final LinkedHashMap<String,Header> headers;

    protected Entity entity = null;

    protected StartLine startLine = null;
    
    /*-- Member Functions ----------------------------------------------------*/

    protected Message(StartLine startLine) {
        this.startLine = startLine;
        this.headers = new LinkedHashMap<String,Header>();
    }

    protected Message(StartLine startLine,
                      LinkedHashMap<String,Header> messageHeaders,
                      Entity entity) {
        this.startLine = startLine;
        if (messageHeaders == null) {
            this.headers = new LinkedHashMap<String,Header>();
        }
        else {
            this.headers = messageHeaders;
        }
        this.entity = entity;
    }
    

    public void writeTo(final OutputStream outstream) throws IOException {
        
        this.startLine.writeTo(outstream);
        
        for (Map.Entry<String, Header> entry : this.headers.entrySet()) {
            entry.getValue().writeTo(outstream);
        }
        
        if (this.entity != null) {
            this.entity.writeTo(outstream);
        }
        else {
            outstream.write('\r');
            outstream.write('\n');
        }
    }
}
