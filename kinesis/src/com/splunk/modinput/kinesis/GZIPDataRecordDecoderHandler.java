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
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		return new String(output);
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
