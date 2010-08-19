function deployRelay(onready, onerror, onfailover) {
	
	var anchor = document.createElement("a");
    anchor.href = "RtspMulticastRelayApplet.jnlp";
    var codebase = anchor.href.substring(0,anchor.href.lastIndexOf('/')+1);
    var jnlp_abs_href = anchor.href;
    
    var attributes = {
            codebase: codebase,
            code:'com.larkwoodlabs.RtspMulticastRelayApplet.class',
            archive:'RtspMulticastRelayApplet.jar, jain-sdp-1.0.100.jar',
            id:'relay',
            name:'relay',
            classloader_cache:false,
            MAYSCRIPT: "true",
            width:1,
            height:1} ; 

    var parameters = {
            jnlp_href: jnlp_abs_href,
            cache_archive_ex: "RtspMulticastRelayApplet.jar;@CACHE_VERSION@",
            MAYSCRIPT: "true"};
    
    if (typeof onready == "string") {
    	parameters["onready"] = onready;
    }

    if (typeof onerror == "string") {
    	parameters["onerror"] = onerror;
    }

    if (typeof onfailover == "string") {
    	parameters["onfailover"] = onfailover;
    }

    deployJava.runApplet(attributes, parameters, '1.6');
}