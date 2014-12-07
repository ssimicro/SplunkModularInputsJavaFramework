import vertx
from core.event_bus import EventBus

config = vertx.config()

event_bus_address = config['address'];
output_address = config['output_address'];

def msg_handler(message):

    #actual received data
    data = message.body;
    
    #turn the raw bytes into some text
    text = data.tostring()
    
    #pass along to output handler
    EventBus.send(output_address,text);
    
id = EventBus.register_handler(event_bus_address, handler=msg_handler)