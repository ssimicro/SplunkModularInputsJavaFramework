import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.Vertx

import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Stream;
import com.splunk.modinput.protocol.handlerverticle.HandlerUtil;

def config = container.config

def stanza = config.stanza
def eventBusAddress = config.address

def eb = vertx.eventBus

def myHandler = { message ->
	//actual received data
	def data = message.body

    //pre-processing / transforming the received data
	def output = "Ignoring received data and replacing with Groovy GOATS!!!!!!";

	//bundle up String output and send to Splunk
	def stream = HandlerUtil.getStream(output, stanza);
	ModularInput.marshallObjectToXML(stream);
}

eb.registerHandler(eventBusAddress, myHandler)