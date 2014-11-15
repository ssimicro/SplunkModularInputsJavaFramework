import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.Vertx

def config = container.config

def eventBusAddress = config.address
def outputAddress = config.output_address

def eb = vertx.eventBus

def myHandler = { message ->
	//actual received data
	def data = message.body

    //pre-processing / transforming the received data
	def output = "Ignoring received data and replacing with Groovy GOATS!!!!!!";

	//pass along to output handler
	eb.send(outputAddress,output)
}

eb.registerHandler(eventBusAddress, myHandler)