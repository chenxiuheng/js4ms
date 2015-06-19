Describes setup required to successfully checkout, use, build and deploy js4ms components.

# Eclipse IDE #

Download the Eclipse IDE for Java SE or Java EE from http://www.eclipse.org.

## Ant build support ##

Several projects require global changes to the Ant preferences maintained in Eclipse. Ant preferences are accessed through the Eclipse Preferences menu option.

### Proguard Code Compression and Obfuscation ###

  * Download the Proguard distribution from http://proguard.sourceforge.net.
  * Open the Ant preferences and add a global class path entry pointing to the proguard.jar file in the Proguard lib directory.

### Tomcat Build Deployment ###

  * Download and install Apache Tomcat from http://tomcat.apache.org.
  * Install the Tomcat plugin for Eclipse. Point the Tomcat plugin to the Tomcat runtime (the top-level directory of the Tomcat installation).
  * Open the Ant preferences and add a global class path entry pointing to the catalina-ant.jar file in the Tomcat lib directory.

### Google App Engine Deployment ###

  * Install the Google App Engine plugin for Eclipse.
  * Add an **`appengine.sdk`** property to Ant properties. Provide the App Engine SDK location as the value of the property. If the Eclipse distribution includes the Ant plugin, the SDK location should point into the plugin bundle, e.g.:
> > `${eclipse_home}/plugins/com.google.appengine.eclipse.sdkbundle_1.7.3/appengine-java-sdk-1.7.3`