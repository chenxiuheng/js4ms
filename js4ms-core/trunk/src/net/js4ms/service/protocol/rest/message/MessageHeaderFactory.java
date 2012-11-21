package net.js4ms.service.protocol.rest.message;


/**
 * Interface exposed by objects that construct
 * {@link MessageHeader} objects given a name and value.
 *
 * @author Greg Bumgardner (gbumgard)
 */
public interface MessageHeaderFactory {

    /**
     * Returns the header name associated with the message header type constructed by this factory.
     * @return The message header name.
     */
    public String getHeaderName();

    /**
     * Constructs a {@link MessageHeader} object.
     * @param value - The header value.
     * @throws IllegalArgumentException
     *         If the specified value cannot be used to construct a message header of the type identified by the name. 
     * @return A new MessageHeader object.
     */
    public MessageHeader construct(final String value) throws IllegalArgumentException;

}
