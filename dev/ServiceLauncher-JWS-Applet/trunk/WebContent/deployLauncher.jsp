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