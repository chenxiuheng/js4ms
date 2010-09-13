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

package com.larkwoodlabs.net.streaming.rtsp;

/**
 * An enumeration of RTSP methods.
 */
public enum Method {

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-32">RFC-2236, Section 10.3</a>].*/ 
    ANNOUNCE,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-31">RFC-2236, Section 10.2</a>].*/ 
    DESCRIBE,

    /**
     * See [<a href="http://tools.ietf.org/html/rfc2068#page-50">RFC-2068, Section 9.3</a>].
     * Used to establish server->client HTTP tunnel for TCP transport.
     * See <a href="http://developer.apple.com/quicktime/icefloe/dispatch028.html">Tunnelling RTSP and RTP through HTTP<a>.
     */ 
    GET,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-37">RFC-2236, Section 10.8</a>].*/ 
    GET_PARAMETER,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-30">RFC-2236, Section 10.1</a>].*/ 
    OPTIONS,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-36">RFC-2236, Section 10.6</a>].*/ 
    PAUSE,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-34">RFC-2236, Section 10.5</a>].*/ 
    PLAY,

    /**
     * See [<a href="http://tools.ietf.org/html/rfc2068#page-51">RFC-2068, Section 9.5</a>].
     * Used to establish client->server HTTP tunnel for TCP transport.
     * See <a href="http://developer.apple.com/quicktime/icefloe/dispatch028.html">Tunnelling RTSP and RTP through HTTP<a>.
     */ 
    POST,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-39">RFC-2236, Section 10.11</a>].*/ 
    RECORD,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-39">RFC-2236, Section 10.10</a>].*/ 
    REDIRECT,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-33">RFC-2236, Section 10.4</a>].*/ 
    SETUP,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-38">RFC-2236, Section 10.9</a>].*/ 
    SET_PARAMETER,

    /** See [<a href="http://tools.ietf.org/html/rfc2326#page-37">RFC-2236, Section 10.7</a>].*/ 
    TEARDOWN;
}
