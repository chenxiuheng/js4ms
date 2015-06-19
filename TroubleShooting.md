<font color='red'><b>Draft Text - This page has not yet been formatted.</b></font>

{section}
{column:width=25%}
{info:title=Table Of Contents|icon=false}

---

{toc:minLevel=2|indent=15px}
{info}
{column}
{column}

h2. RTSP Reflector

To make it easier to debug the problems you all are experiencing, I need some additional information. If you encounter a failure, or successful stream playout, please reply to this e-mail and provide the following:
**Platform OS (whether native or VM).** Browser and version.
**Media player and version.** Stream source (SDP).
**Java JRE version.** Java browser plugin version (see instructions below).
**In the case of failure during Java Web Start launch, copy & paste the messages that appear in the tabs in the Java plug in error window.** In the case of failure during gateway operation, copy & paste the Java console log. Please note that some log messages will report errors, but do not necessarily indicate a failure. If you see an exception and stack trace appear in the console log, then that IS an error.
**If you suspect a problem in the RTSP negotiation or AMT protocol message exchange, you'll win bonus points if you attach a capture log from Wireshark (zipped please).**

Please let me know if there was something you had to do on your own to get it to work in your environment so we can document that too.

h3. Turn on Java Console in Windows

http://www.java.com/en/download/help/javaconsole.xml

h3. Procedure for determining browser plugin version

Firefox and Chrome**: Enter URL 1"about:plugins". Look for "Java Platform..." entry with    description "Next Generation Java Plugin"** **IE**:    Tools->Manage Addons->Extensions. Look for "Sun Microsystems..." entry    and Plug-In extension.
Safari**:    Help->Installed Plugins. Look for Java Plugin entries. Provide highest    version available. Verify that an entry for JNLP mime type exists    (probably under Cocoa plugin).**

h2. Wireshark

Wireshark \[http://www.wireshark.org/\] may be used to capture and dissect AMT messages sent between a gateway and relay. See [Dissector for AMT](Wireshark.md).
{column}
{section}