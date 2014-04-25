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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 
<% response.setHeader("Last-Modified", config.getInitParameter("LastModified")); %>
<jnlp spec="1.0+"
      href="<c:out value='${pageContext.request.requestURL}'/><c:if test="${fn:length(pageContext.request.queryString) > 0}">?<c:out value='${pageContext.request.queryString}'/></c:if>"
      codebase="${pageContext.request.contextPath}">
  <information>
    <title><%= config.getInitParameter("title") %></title>
    <description><%= config.getInitParameter("description") %></description>
    <vendor><%= config.getInitParameter("vendor") %></vendor>
    <homepage href="<%= config.getInitParameter("homePage") %>"/>
  </information>
  <security>
    <all-permissions/>
  </security>
  <resources>
    <j2se version="1.7.0+" href="http://java.sun.com/products/autodl/j2se" />
    <jar href="<%= config.getInitParameter("jarName") %>" version="<%= config.getInitParameter("jarVersion") %>" main="true"/>
    <property name="jnlp.versionEnabled" value="true"/>
  </resources>
  <application-desc main-class="<%= config.getInitParameter("mainClass") %>">
    <argument><c:forEach var="pageParameter" items="${param}">-<c:out value='${pageParameter.key}'/>=<c:out value='${pageParameter.value}'/> </c:forEach></argument>
  </application-desc>
</jnlp>
