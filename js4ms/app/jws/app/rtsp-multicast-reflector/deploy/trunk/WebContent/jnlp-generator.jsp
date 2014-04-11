<%--
  jnlp-generator.jsp

  Copyright (C) 2011 Cisco Systems, Inc.

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
<%@ page contentType="application/x-java-jnlp-file" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% response.setHeader("Last-Modified", config.getInitParameter("LastModified")); %>
<%
String requestUrl = request.getRequestURL().toString();
String codebaseUrl = requestUrl.substring(0,requestUrl.lastIndexOf('/')+1);
//String href = codebaseUrl + config.getInitParameter("JnlpName");
%>
<jnlp spec="1.0+" href="<%= requestUrl %>" codebase="<%= codebaseUrl %>">
  <information>
    <title><%= config.getInitParameter("Title") %></title>
    <vendor><%= config.getInitParameter("Vendor") %></vendor>
    <description><%= config.getInitParameter("Description") %></description>
  </information>
  <security>
    <all-permissions/>
  </security>
  <resources>
    <j2se version="1.6.0_10+" href="http://java.sun.com/products/autodl/j2se" />
    <jar href="<%= config.getInitParameter("JarName") %>" version="<%= config.getInitParameter("JarVersion") %>" main="true"/>
    <property name="jnlp.versionEnabled" value="true"/>
    <c:forEach var="pageParameter" items="${param}">
	  <property name="<c:out value='${pageParameter.key}'/>" value="<c:out value='${pageParameter.value}'/>" />
    </c:forEach>
  </resources>
  <application-desc main-class="<%= config.getInitParameter("MainClass") %>" />
</jnlp>
