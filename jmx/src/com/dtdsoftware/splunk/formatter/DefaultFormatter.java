package com.dtdsoftware.splunk.formatter;

import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * Default formatter implementation
 * </pre>
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class DefaultFormatter extends SplunkFormatter implements Formatter {

	public DefaultFormatter() {

		this.outputPrefix = new StringBuffer();
	}

	@Override
	public void setParameters(Map<String, String> parameters) {

		setCommonSplunkParameters(parameters);

	}

	@Override
	public String format(String mBean, Map<String, String> attributes,
			long timestamp) {

		// not using the timestamp, using the SPLUNK index time instead

		// append the common prefix
		StringBuffer output = new StringBuffer();
		prependDate(timestamp, output);
		output.append(outputPrefix);

		// append the mbean name

		output.append(buildPair("mbean", mBean));

		// add mbean attributes
		Set<String> keys = attributes.keySet();
		for (String key : keys) {

			String value = attributes.get(key);
			value = FormatterUtils.stripNewlines(value);
			value= stripPatterns(value);
			output.append(buildPair(key, value));
		}

		String result = output.toString();
		return result.substring(0, result.length() - pairdelim.length());// just
																			// trim
																			// trailing
																			// pairdelim
																			// character(s)

	}

	

	@Override
	public void setMetaData(Map<String, String> metaData) {

		setCommonSplunkMetaData(metaData);

	}

}
