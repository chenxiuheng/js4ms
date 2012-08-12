package com.larkwoodlabs.net.messaging;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.larkwoodlabs.common.exceptions.ParseException;

/**
 * A message header consisting of a name and value.
 * This class is used to parse and serialize message headers.
 * 
 * @author Gregory Bumgardner
 */
public class Header {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Pattern pattern = Pattern.compile("([[0-9][a-z][A-Z][-_]]+):[ ]*(.*)");

    public static final SimpleDateFormat DATE_FORMAT_RFC_1123;
    
    static {
        DATE_FORMAT_RFC_1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        DATE_FORMAT_RFC_1123.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    /*-- Static Functions ----------------------------------------------------*/

    /**
     * Constructs a Header instance from the specified string.
     * @param string - A string containing a single HTTP header record.
     * @throws ParseException
     */
    public static final Header parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid header");
        }
        return new Header(matcher.group(1),matcher.group(2).trim());
    }


    /*-- Member Variables ----------------------------------------------------*/

    protected final String name;
    protected String value;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a header with the specified name and an empty value.
     * @param name - The header name.
     */
    public Header(final String name) {
        this.name = name;
        this.value = "";
    }

    /**
     * Constructs a header with the specfied name and value.
     * @param name - The header name.
     * @param value - The header value.
     */
    public Header(final String name, final String value) {
        this.name = name;
        this.value = value;
    }
    
    /**
     * Returns the header name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the header value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Sets the header value.
     * @param value - The header value.
     */
    public void setValue(final String value) {
        this.value = value;
    }
    
    /**
     * Appends a string to the current header value.
     * @param value - The string to appeand to the header value.
     */
    public void appendFragment(final String value) {
        this.value += value;
    }

    /**
     * Sets or appends a value to the current header.
     * Multiple values are comma-separated.
     * @param value - The value to set or append.
     */
    public void appendValue(final String value) {
        if (this.value.length() > 0) {
            this.value += "," + value;
        }
        else {
            this.value = value;
        }
    }

    /**
     * Returns a string representation of the header.
     * The value returned has the following format:
     * <pre>
     *  "&lt;header-name&gt;: &lt;header-value&gt;"
     * </pre>
     */
    @Override
    public String toString() {
        return this.name + ": " + this.value;
    }

    /**
     * Writes the header to the specified OutputStream.
     * The header name and value is encoded as UTF-8 and serialized as follows:
     * <pre>
     *  &lt;header-name&gt; + ": "  + &lt;header-value&gt; + CRLF
     * </pre>
     * @param outstream - The destination OutputStream
     */
    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(this.name.getBytes("UTF8"));
        outstream.write(':');
        outstream.write(' ');
        outstream.write(this.value.getBytes("UTF8"));
        outstream.write('\r');
        outstream.write('\n');
    }

}
