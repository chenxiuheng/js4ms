The JS4MS project provides a Java SDK and various executables that may be used to create multicast applications and services primarily through the use of RTSP and Automatic Multicast Tunneling (AMT) protocols.

<blockquote>
<font color='darkred'>
UPDATE: The RFC approval process for <a href='http://tools.ietf.org/id/draft-ietf-mboned-auto-multicast.html'>AMT Internet-Draft</a> is complete. The new standard is <a href='https://tools.ietf.org/html/rfc7450'>RFC-7450</a>.<br>
</font>
</blockquote>

<blockquote>
<font color='#555'>
Visitors,<br>
<br>
I have again taken a break from working on anything AMT-related, though I did return to work on the JS4MS project code for a brief period last year. I began a transition to Maven for builds and dependency management, but continued to use Eclipse as the primary development environment. To simplify the "Mavenizing" of the project I refactored and reorganized the codebase. I made good progress but there is still more to do.<br>
<br>
I am now maintaining the following project artifacts:<br>
<ul><li><a href='https://js4ms.googlecode.com/svn/site/0.0.1-SNAPSHOT/index.html'>Project Documentation</a>.<br>
</li><li><a href='https://js4ms.googlecode.com/svn/trunk'>Java Source Code</a>.<br>
</li><li><a href='https://js4ms.googlecode.com/svn/site/0.0.1-SNAPSHOT/apidocs/index.html'>API Javadocs</a>.</li></ul>

The primary interface for gaining access to AMT functionality is provided by the  <a href='http://js4ms.googlecode.com/svn/site/apidocs/org/js4ms/amt/gateway/AmtIPInterface.html'>AmtIPInterface</a> class. A singleton <a href='http://js4ms.googlecode.com/svn/site/apidocs/org/js4ms/amt/gateway/AmtIPInterfaceManager.html'>AmtIPInterfaceManager</a> instance must be used to construct instances of the AmtIPInterface class . The <a href='http://js4ms.googlecode.com/svn/site/apidocs/org/js4ms/amt/gateway/AmtUDPInterface.html'>AmtUDPInterface</a> class may be used to extract UDP datagrams from the IP packets generated by the AmtIPInterface These classes are contained in the <a href='http://js4ms.googlecode.com/svn/site/apidocs/org/js4ms/amt/gateway/package-summary.html'>org.js4ms.amt.gateway</a> package. The IP and UDP interface classes are designed to be used with object interfaces defined in the <a href='http://js4ms.googlecode.com/svn/site/apidocs/org/js4ms/io/channel/package-summary.html'>org.js4ms.io.channel</a> package.<br>
<br>
The components listed above are one of many that are used to implement an RTSP multicast reflector (server) that uses AMT to receive multicast media streams. I am currently working to clean up the project that provides a Java Web Start (JWS) version(s) of the reflector. The JWS application jars must be served from a servlet container to properly support JWS versioning and updates. I will post a .WAR archive in the downloads section in the next day or two. I have been using my own Tomcat server for deployment, but plan to deploy the application to Google App Engine at <a href='http://js4ms-rtsp-reflector.appspot.com'>http://js4ms-rtsp-reflector.appspot.com</a>.<br>
<br>
-g.b.<br>
<br>
April 26, 2014<br>
<br>
Greg Bumgardner<br>
gbumgard@gmail.com<br>
</font>
</blockquote>