function deployLauncher(id,appletParameters) {
	
	var anchor = document.createElement("a");
    anchor.href = "@JNLP_PATH@";
    var codebase = anchor.href.substring(0,anchor.href.lastIndexOf('/')+1);
    var jnlp_abs_href = anchor.href;

    var attributes = {
            codebase: codebase,
            code:'@MAIN_CLASS@',
            archive:'@JAR_NAME@',
            id:id,
            name:id,
            classloader_cache:false,
            MAYSCRIPT: "true",
            width:1,
            height:1} ; 

    var parameters = {
            jnlp_href: jnlp_abs_href,
            cache_archive_ex: "@JAR_NAME@;@CACHE_VERSION@",
            MAYSCRIPT: "true"};

    for (parameter in appletParameters) {
    	parameters[parameter] = appletParameters[parameter];
    }

    deployJava.runApplet(attributes, parameters, '1.6');
}