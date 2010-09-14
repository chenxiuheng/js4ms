package com.larkwoodlabs.net.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Entity-header fields define optional metainformation about the entity-body
 * or, if no body is present, about the resource identified by the request.
 * <pre>
 * entity-header       =    Allow
 *                          |    Content-Base
 *                          |    Content-Encoding
 *                          |    Content-Language
 *                          |    Content-Length
 *                          |    Content-Location
 *                          |    Content-Type
 *                          |    Expires
 *                          |    Last-Modified
 *                          |    extension-header
 *      extension-header    =    message-header
 * </pre>
 * 
 * The extension-header mechanism allows additional entity-header fields to be
 * defined without changing the protocol, but these fields cannot be assumed to
 * be recognizable by the recipient. Unrecognized header fields SHOULD be
 * ignored by the recipient and forwarded by proxies.
 * 
 * @author Gregory Bumgardner
 */
public class Entity {
    
    /*-- Member Variables ----------------------------------------------------*/

    protected InputStream content;


    /*-- Member Functions ----------------------------------------------------*/

    protected Entity(final InputStream content) {
        this.content = content;
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        
        outstream.write('\r');
        outstream.write('\n');
    }

}
