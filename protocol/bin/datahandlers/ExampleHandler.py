import vertx
from core.event_bus import EventBus

config = vertx.config()

stanza = config['stanza'];
event_bus_address = config['address'];

def msg_handler(message):

    #actual received data
    data = message.body;
    
    #pre-processing / transforming the received data
    output = "Ignoring received data and replacing with Python GOATS!!!!!!";

    #bundle up String output and send to Splunk
    print "<stream><event stanza=\""+stanza+"\"><data>"+output+"</data></event></stream>";
    
id = EventBus.register_handler(event_bus_address, handler=msg_handler)