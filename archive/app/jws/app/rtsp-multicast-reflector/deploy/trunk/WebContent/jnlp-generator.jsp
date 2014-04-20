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
<jnlp spec="1.0+" href="<c:out value='${pageContext.request.requestURL}'/>?<c:out value='${pageContext.request.queryString}'/>">
  <information>
    <title><%= config.getInitParameter("title") %></title>
    <vendor><%= config.getInitParameter("vendor") %></vendor>
    <description><%= config.getInitParameter("description") %></description>
  </information>
  <security>
    <all-permissions/>
  </security>
  <resources>
    <j2se version="1.6.0_10+" href="http://java.sun.com/products/autodl/j2se" />
    <jar href="<%= config.getInitParameter("jarName") %>" version="<%= config.getInitParameter("jarVersion") %>" main="true"/>
    <property name="jnlp.versionEnabled" value="true"/>
    <c:forEach var="pageParameter" items="${param}">
	<property name="<c:out value='${pageParameter.key}'/>" value="<c:out value='${pageParameter.value}'/>" />
    </c:forEach>
  </resources>
  <application-desc main-class="<%= config.getInitParameter("mainClass") %>" />
</jnlp>
