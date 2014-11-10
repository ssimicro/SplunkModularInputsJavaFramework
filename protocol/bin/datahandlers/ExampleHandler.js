var eb = require('vertx/event_bus');
var container = require('vertx/container');
var console = require('vertx/console');

var config = container.config;

var stanza = config.stanza;
var eventBusAddress = config.address;

var myHandler = function(message) {
	//actual received data
	var data = message;
	
	//pre-processing / transforming the received data
	var output = "Ignoring received data and replacing with Javascript GOATS!!!!!!";

	//bundle up String output and send to Splunk
	var stream = "<stream><event stanza=\""+stanza+"\"><data>"+output+"</data></event></stream>";
	
	console.log(stream);
	
}

eb.registerHandler(eventBusAddress, myHandler);