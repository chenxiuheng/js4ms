package org.js4ms.service.protocol.rest.message;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.js4ms.common.exception.ParseException;
import org.js4ms.service.protocol.rest.headers.SimpleMessageHeader;




/**
 * Maintains a map of {@link MessageHeaderFactory} objects and uses those factories
 * to construct {@link MessageHeader} objects from a raw message header record or
 * a specified name and value.
 *
 * @author Greg Bumgardner (gbumgard)
 */
public class MessageHeaderParser {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Pattern pattern = Pattern.compile("([[0-9][a-z][A-Z][-_]]+):[ ]*(.*)");

    /*-- Member Variables ----------------------------------------------------*/

    private final HashMap<String,MessageHeaderFactory> factories = new HashMap<String,MessageHeaderFactory>();


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Default constructor.
     */
    public MessageHeaderParser() {
    }

    /**
     * Adds the specified type-specific message header factory to the factory map.
     * @param factory - A message header factory.
     */
    public void register(final MessageHeaderFactory factory) {
        this.factories.put(factory.getHeaderName(), factory);
    }

    /**
     * Removes the named message header factory from the factory map.
     * @param headerName - A message header name.
     */
    public void unregister(final String headerName) {
        this.factories.remove(headerName);
    }

    /**
     * Constructs a {@link MessageHeader} instance from a message header record.
     * 
     * @param string - A string containing a single message header record.
     * @throws ParseException
     */
    public MessageHeader parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid header");
        }
        return construct(matcher.group(1),matcher.group(2).trim());
    }

    /**
     * Constructs a concrete {@link MessageHeader} object from a given name and value.
     * This methods uses the specified name to lookup a message header factory, and if one exists,
     * uses that factory to construct a MessageHeader object from the specified value.
     * If no factory has been registered for the specified name, this method will return
     * a {@link SimpleMessageHeader} constructed using the name and value arguments.
     * 
     * @param name - The header name used to lookup a factory.
     * @param value - The header value.
     * @throws IllegalArgumentException
     *         If the specified value cannot be used to construct a message header of the type identified by the name. 
     * @return A MessageHeader object.
     */
    public MessageHeader construct(final String name, final String value) throws IllegalArgumentException {
        MessageHeader header = null;
        if (this.factories.containsKey(name)) {
            this.factories.get(name).construct(value);
        }
        else {
            return new SimpleMessageHeader(name, value);
        }
        return header;
    }
}
