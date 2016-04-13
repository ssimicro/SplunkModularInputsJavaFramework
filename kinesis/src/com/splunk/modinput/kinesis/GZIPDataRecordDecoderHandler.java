package com.splunk.modinput.kinesis;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

import com.splunk.modinput.kinesis.KinesisModularInput.MessageReceiver;

public class GZIPDataRecordDecoderHandler extends AbstractMessageHandler {

	@Override
	public void handleMessage(String record, byte[] rawBytes, String seqNumber, String partitionKey,
			MessageReceiver context) throws Exception {

		String decodedData = decompress(rawBytes);

		String text = stripNewlines(decodedData);

		transportMessage(text, String.valueOf(System.currentTimeMillis()), "");

	}

	public String decompress(byte[] data) throws Exception {
	        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
	        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
	        String outStr = "";
	        String line;
	        while ((line=bf.readLine())!=null) {
	          outStr += line;
	        }
	        return outStr;
	}

	public static List<String> chunkData(String text, int size) {

		List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	@Override
	public void setParams(Map<String, String> params) {
		// Do nothing , params not used

	}

}
