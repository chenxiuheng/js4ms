<%--
  ServiceLauncher-JWS-Applet deployLauncher.jsp

  Copyright © 2011 Cisco Systems, Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

--%>
<%@page contentType="text/javascript" %>
<% response.setHeader("Last-Modified", config.getInitParameter("LastModified")); %>
<%
String requestUrl = request.getRequestURL().toString();
String codebaseUrl = requestUrl.substring(0,requestUrl.lastIndexOf('/')+1);
String jnlpUrl = codebaseUrl + config.getInitParameter("JnlpName");
%>
function deployLauncher(id,appletParameters) {
	
    var attributes = {
            codebase: '<%= codebaseUrl %>',
            code:'<%= config.getInitParameter("MainClass") %>',
            archive:'<%= config.getInitParameter("JarName") %>',
            id:id,
            name:id,
            classloader_cache:false,
            MAYSCRIPT: "true",
            width:1,
            height:1} ; 

    var parameters = {
            jnlp_href: '<%= jnlpUrl %>',
            cache_archive_ex: '<%= config.getInitParameter("JarName") %>;<%= config.getInitParameter("CacheVersion") %>',
            MAYSCRIPT: "true"};

    for (parameter in appletParameters) {
    	parameters[parameter] = appletParameters[parameter];
    }

    deployJava.runApplet(attributes, parameters, '1.6');
}