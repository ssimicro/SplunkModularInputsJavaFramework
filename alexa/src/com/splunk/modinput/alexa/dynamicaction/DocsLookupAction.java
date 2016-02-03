package com.splunk.modinput.alexa.dynamicaction;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.splunk.modinput.alexa.DynamicAction;

/**
 * Looks up the description for a Splunk search command from the docs webpage
 * 
 * @author ddallimore
 *
 */
public class DocsLookupAction extends DynamicAction {

	@Override
	public String executeAction() {

		String response = "";
		try {

			Document doc = Jsoup.connect(getArg("base_url") + getSlot("searchcommandname")).get();
			Elements desc = doc.select(getArg("css_selector"));
			// strip any other html tags
			String dynamicResponse = desc.first().toString().replaceAll("<\\w+>|</\\w+>", "");
			response = replaceResponse(dynamicResponse);

		} catch (IOException e) {

		}
		return response;
	}

}
