var eb = require('vertx/event_bus');
var container = require('vertx/container');
var console = require('vertx/console');

var config = container.config;


var eventBusAddress = config.address;
var outputAddress = config.output_address;

var myHandler = function(message) {
	//actual received data
	var data = message;
	
	//pre-processing / transforming the received data
	var output = "Ignoring received data and replacing with Javascript GOATS!!!!!!";

	//pass along to output handler
	eb.send(outputAddress,output);
	
}

eb.registerHandler(eventBusAddress, myHandler);