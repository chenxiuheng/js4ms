package org.js4ms.service.protocol.rest;

import org.js4ms.service.protocol.rest.message.ProtocolVersion;
import org.js4ms.util.logging.Log;



public class MessageException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3670722564024392131L;

    protected static final Log slog = new Log(MessageException.class);

    protected final ProtocolVersion protocolVersion;

    protected MessageException(final ProtocolVersion protocolVersion) {
        super();
        this.protocolVersion = protocolVersion;
    }

    public MessageException(final ProtocolVersion protocolVersion,
                            final String message) {
        super(message);
        this.protocolVersion = protocolVersion;
    }

    public MessageException(final ProtocolVersion protocolVersion,
                            final Throwable cause) {
        super(cause);
        this.protocolVersion = protocolVersion;
    }

    public MessageException(final ProtocolVersion protocolVersion,
                            final String message,
                            final Throwable cause) {
        super(message,cause);
        this.protocolVersion = protocolVersion;
    }

    /**
     * Returns the {@link ProtocolVersion} of the message that generated this exception.
     */
    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }


}
