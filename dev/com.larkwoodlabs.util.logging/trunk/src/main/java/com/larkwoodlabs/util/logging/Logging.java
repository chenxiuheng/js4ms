/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.util.logging;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;


public final class Logging {
    
    public static final int methodStackTraceLevel = determineStackTraceLevel();

    static int determineStackTraceLevel() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getMethodName().equals("determineStackTraceLevel")) {
                return i+1;
            }
        }
        return 0;
    }

    public static String entering(final String objectId, final String methodName) {
        return objectId + " entering " + methodName + "()";
    }

    public static String entering(final String objectId, final String methodName, final Object ...args) {
        return objectId + " entering " + methodName + Logging.args(args);
    }

    public static String exiting(final String objectId, final String methodName) {
        return objectId + " exiting " + methodName;
    }

    public static String exiting(final String objectId, final String methodName, final Object result) {
        return objectId + " exiting " + methodName + " returns " + result;
    }

    public static String entry(final Object object, final Object... args) {
        String info = Thread.currentThread().getStackTrace()[methodStackTraceLevel].getMethodName() + "(";
        for (int index = 0; index < args.length; index++) {
            info += args[index];
            if (index < args.length - 1) info += ", ";
        }
        info += ")";
        return info;
    }

    public static String exit(final Object object, final Object result) {
        return identify(object)+" "+Thread.currentThread().getStackTrace()[methodStackTraceLevel].getMethodName()+"->"+result;
    }

    public static String identify(final Object object) {
        String id = "00000000"+Integer.toHexString(object.hashCode());
        return "["+id.substring(id.length()-8)+"]";
    }

    public static String method() {
        return Thread.currentThread().getStackTrace()[methodStackTraceLevel].getMethodName();
    }
    
    public static String address(final SocketAddress address) {
        return Logging.address(((InetSocketAddress)address).getAddress().getAddress()) + ":" + ((InetSocketAddress)address).getPort();
    }

    public static String address(final InetSocketAddress address) {
        return Logging.address(address.getAddress()) + ":" + address.getPort();
    }

    public static String address(final InetAddress address) {
        return Logging.address(address.getAddress());
    }

    public static String address(final byte[] address) {
        String result = "";
        if (address == null) {
            return null;
        }
        if (address.length == 4) {
            // IPv4 Address
            for (int i = 0; i < address.length;) {
                result += address[i++] & 0xFF;
                if (i < address.length) result += ".";
            }
        }
        else {
            // IPv6 Address
            for (int i = 0; i < address.length;) {
                result += String.format("%02X", address[i++] & 0xFF);
                if (i < address.length) result += ":";
            }
        }
        return result;
    }

    public static String mac(final byte[] mac) {
        String result = "";
        for (int i = 0; i < mac.length; i++) {
            result += String.format("%02X", mac[i++]);
        }
        return result;
    }
    
    public static Object[] argArray(Object ... objects) {
        return objects;
    }
    
    public static String arg(final Object object) {
        if (object == null) return "null";
        Class<?> objectClass = object.getClass();
        if (objectClass.isPrimitive() || object instanceof String) {
            return object.toString();
        }
        else {
            String name = object.getClass().getSimpleName();
            if (name.length()==0) {
                name = ((Class<?>)object.getClass().getSuperclass()).getSimpleName();
            }
            
            return name+Logging.identify(object);
        }
    }

    public static String args(final Object ... objects) {
        int length = objects.length;
        String result = "(";
        if (length > 0) {
            result += Logging.arg(objects[0]);
            for (int i = 1; i < length; i++) {
                result += ", " + Logging.arg(objects[i]);
            }
        }
        return result + ")";
    }
}
