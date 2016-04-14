package com.splunk.modinput.kinesis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.splunk.modinput.kinesis.KinesisModularInput.MessageReceiver;

public class GZIPDataRecordDecoderHandler extends AbstractMessageHandler {

	@Override
	public void handleMessage(ByteBuffer rawBytes, String seqNumber, String partitionKey, MessageReceiver context)
			throws Exception {

		String decodedData = decompress(rawBytes.array());

		String text = stripNewlines(decodedData);

		transportMessage(text, String.valueOf(System.currentTimeMillis()), "");

	}

	public String decompress(byte[] data) throws Exception {
		GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
		BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
		StringBuffer outStr =new StringBuffer();
		String line;
		while ((line = bf.readLine()) != null) {
			outStr.append(line);
		}
		bf.close();
		return outStr.toString();
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
