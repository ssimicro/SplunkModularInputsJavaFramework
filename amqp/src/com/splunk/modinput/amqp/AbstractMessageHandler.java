package com.splunk.modinput.amqp;

import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.amqp.AMQPModularInput.MessageReceiver;

public abstract class AbstractMessageHandler {

	public abstract Stream handleMessage(byte[] messageContents,
			Envelope envelope, AMQP.BasicProperties messageProperties,
			MessageReceiver context) throws Exception;

	public abstract void setParams(Map<String, String> params);

	protected SplunkLogEvent buildCommonEventMessagePart(Envelope envelope,
			AMQP.BasicProperties messageProperties, MessageReceiver context)
			throws Exception {

		SplunkLogEvent event = new SplunkLogEvent("amqp_msg_received",
				messageProperties != null ? messageProperties.getMessageId()
						: "", true, true);

		event.addPair("msg_queue", context.queueName);
		event.addPair("msg_exchange", context.exchangeName);

		if (context.indexMessageEnvelope && envelope != null) {
			// JMS Message Header fields
			event.addPair("msg_envelope_delivery_tag",
					envelope.getDeliveryTag());
			event.addPair("msg_envelope_exchange", envelope.getExchange());
			event.addPair("msg_envelope_routing_key", envelope.getRoutingKey());
			event.addPair("msg_envelope_redelivery_flag",
					envelope.isRedeliver());

		}

		if (context.indexMessagePropertys && messageProperties != null) {
			event.addPair("msg_property_timestamp",
					messageProperties.getTimestamp());
			event.addPair("msg_property_appid", messageProperties.getAppId());
			event.addPair("msg_property_bodysize",
					messageProperties.getBodySize());
			event.addPair("msg_property_classid",
					messageProperties.getClassId());
			event.addPair("msg_property_classname",
					messageProperties.getClassName());
			event.addPair("msg_property_clusterid",
					messageProperties.getClusterId());
			event.addPair("msg_property_contentencoding",
					messageProperties.getContentEncoding());
			event.addPair("msg_property_correlationid",
					messageProperties.getCorrelationId());
			event.addPair("msg_property_expiration",
					messageProperties.getExpiration());
			event.addPair("msg_property_replyto",
					messageProperties.getReplyTo());
			event.addPair("msg_property_type", messageProperties.getType());
			event.addPair("msg_property_userid", messageProperties.getUserId());
			event.addPair("msg_property_deliverymode",
					messageProperties.getDeliveryMode());
			event.addPair("msg_property_priority",
					messageProperties.getPriority());

			Map<String, Object> headers = messageProperties.getHeaders();
			if (headers != null) {
				for (String name : headers.keySet()) {
					event.addPair("msg_property_header_" + name,
							headers.get(name));
				}
			}
		}

		return event;

	}

	protected String getMessageBody(byte[] messageContents) {

		return new String(messageContents);

	}

	protected String stripNewlines(String input) {

		if (input == null) {
			return "";
		}
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (Character.isWhitespace(chars[i])) {
				chars[i] = ' ';
			}
		}

		return new String(chars);
	}

}
