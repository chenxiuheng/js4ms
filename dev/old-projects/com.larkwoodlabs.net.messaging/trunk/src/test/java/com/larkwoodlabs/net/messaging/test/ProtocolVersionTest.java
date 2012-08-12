package com.larkwoodlabs.net.messaging.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.messaging.Protocol;
import com.larkwoodlabs.net.messaging.ProtocolVersion;

public class ProtocolVersionTest {

    @Test
    public void testParseString() throws ParseException {
            ProtocolVersion version = ProtocolVersion.parse("TEST/1.2");
            assertTrue(version.getProtocol().getName().equals("TEST"));
            assertTrue(version.getMajorVersion() == 1);
            assertTrue(version.getMinorVersion() == 2);
            assertTrue(version.toString().equals("TEST/1.2"));
        try {
            ProtocolVersion.parse("");
            fail("expected exception for empty string");
        }
        catch(ParseException e) {
        }
        try {
            ProtocolVersion.parse("TEST");
            fail("expected exception for missing version numbers");
        }
        catch(ParseException e) {
        }
        try {
            ProtocolVersion.parse("TEST/");
            fail("expected exception for missing version numbers");
        }
        catch(ParseException e) {
        }
        try {
            ProtocolVersion.parse("TEST/1");
            fail("expected exception for missing minor version number");
        }
        catch(ParseException e) {
        }
        try {
            ProtocolVersion.parse("TEST/1.A");
            fail("expected exception for non-numeric version number");
        }
        catch(ParseException e) {
        }
        try {
            ProtocolVersion.parse("test/1.2");
            fail("expected exception for lower case protocol name");
        }
        catch(ParseException e) {
        }
    }

    @Test
    public void testProtocolVersion() {
        ProtocolVersion version = new ProtocolVersion(new Protocol("TEST"), 1, 2);
        assertTrue(version.getProtocol().getName().equals("TEST"));
        assertTrue(version.getMajorVersion() == 1);
        assertTrue(version.getMinorVersion() == 2);
        assertTrue(version.toString().equals("TEST/1.2"));
    }

    @Test
    public void testToString() {
        ProtocolVersion version = new ProtocolVersion(new Protocol("TEST"), 1, 2);
        assertTrue(version.toString().equals("TEST/1.2"));
    }

    @Test
    public void testEquals() {
        ProtocolVersion v1 = new ProtocolVersion(new Protocol("TEST"), 1, 2);
        ProtocolVersion v2 = new ProtocolVersion(new Protocol("TEST"), 1, 2);
        ProtocolVersion v3 = new ProtocolVersion(new Protocol("ABCD"), 1, 2);
        ProtocolVersion v4 = new ProtocolVersion(new Protocol("TEST"), 0, 2);
        ProtocolVersion v5 = new ProtocolVersion(new Protocol("TEST"), 1, 0);
        assertTrue(v1.equals(v2));
        assertFalse(v1.equals(v3));
        assertFalse(v1.equals(v4));
        assertFalse(v1.equals(v5));
    }

    @Test
    public void testWriteTo() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ProtocolVersion version = new ProtocolVersion(new Protocol("TEST"), 1, 2);
        try {
            version.writeTo(os);
            assertTrue(os.toString("UTF8").equals("TEST/1.2"));
        }
        catch (IOException e) {
            fail("unexpected exception");
            e.printStackTrace();
        }
        
    }

}
