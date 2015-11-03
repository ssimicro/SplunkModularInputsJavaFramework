package com.splunk.modinput.transport;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;

public class STDOUTTransport implements Transport {

	protected static Logger logger = Logger.getLogger(STDOUTTransport.class);
	
	private String stanzaName = "";
	
	@Override
	public void init(Object obj) {
		// do nothing

	}
	
	public void setStanzaName(String name){
		this.stanzaName = name;
	}

	@Override
	public void transport(String text) {
		Stream stream = new Stream();
		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
		List<String> chunks = chunkData(text, 1024);

		for (int i = 0; i < chunks.size(); i++) {
			StreamEvent event = new StreamEvent();
			event.setUnbroken("1");
			event.setData(chunks.get(i));
			event.setStanza(stanzaName);
			// if we are seeing the last chunk, set the "done" element
			if (i == chunks.size() - 1)
				event.setDone(" ");
			list.add(event);
		}
		stream.setEvents(list);
		marshallObjectToXML(stream);
	}
	
	public static void marshallObjectToXML(Object obj) {
		try {
			JAXBContext context = JAXBContext.newInstance(obj.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			marshaller.marshal(obj, sw);
			String xml = sw.toString();
			logger.info("Data sent to Splunk:" + xml);
			logger.info("Size of data sent to Splunk:" + xml.length());
			System.out.println(xml.trim());

		} catch (Exception e) {
			logger.error("Error writing XML : " + e.getMessage());
		}
	}
	private List<String> chunkData(String text, int size) {

		List<String> ret = new ArrayList<String>((text.length() + size - 1)
				/ size);

		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	@Override
	public void transport(String message, String time,String host) {
		transport(message);
		
	}

}
