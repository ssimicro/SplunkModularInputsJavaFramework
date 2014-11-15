import vertx
from core.event_bus import EventBus

config = vertx.config()

event_bus_address = config['address'];
output_address = config['output_address'];

def msg_handler(message):

    #actual received data
    data = message.body;
    
    #pre-processing / transforming the received data
    output = "Ignoring received data and replacing with Python GOATS!!!!!!";

    #pass along to output handler
    EventBus.send(output_address,output);
    
id = EventBus.register_handler(event_bus_address, handler=msg_handler)