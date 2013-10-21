package com.dtdsoftware.splunk.formatter;


import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <pre>
 * 
 * Custom formatter implementation that outputs the mbean canonical name as
 * split up tokens
 * </pre>
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class TokenizedMBeanNameFormatter extends SplunkFormatter implements
		Formatter {

	public TokenizedMBeanNameFormatter() {

		this.outputPrefix = new StringBuffer();
	}

	@Override
	public void setParameters(Map<String, String> parameters) {

		setCommonSplunkParameters(parameters);

	}

	@Override
	public String format(String mBean, Map<String, String> attributes,
			long timestamp) {

		// not using the timestamp, using the SPLUNK index time insteade

		// append the common prefix
		StringBuffer output = new StringBuffer();
		prependDate(timestamp, output);
		output.append(outputPrefix);

		SortedMap<String, String> mbeanNameParts = tokenizeMBeanCanonicalName(mBean);

		Set<String> mBeanNameKeys = mbeanNameParts.keySet();

		for (String key : mBeanNameKeys) {

			output.append(buildPair(key, mbeanNameParts.get(key)));
		}

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

	/**
	 * Take a canonical mbean name ie: "domain:key=value, key2=value2" , and
	 * split out the parts into individual fields.
	 * 
	 * @param mBean
	 *            the canonical mbean name
	 * @return sorted map of the name parts
	 */
	private SortedMap<String, String> tokenizeMBeanCanonicalName(String mBean) {

		SortedMap<String, String> result = new TreeMap<String, String>();

		String[] parts = mBean.split(":");
		if (parts == null || parts.length != 2) {
			return result;
		}
		// the mbean domain
		result.put("mbean_domain", parts[0]);

		// the mbean properties
		String[] properties = parts[1].split(",");
		if (properties == null) {
			return result;
		}
		for (String prop : properties) {
			String[] property = prop.split("=");
			if (property == null || property.length != 2) {
				continue;
			}
			result.put("mbean_property_" + property[0], property[1]);
		}

		return result;
	}

	@Override
	public void setMetaData(Map<String, String> metaData) {

		setCommonSplunkMetaData(metaData);

	}

}
