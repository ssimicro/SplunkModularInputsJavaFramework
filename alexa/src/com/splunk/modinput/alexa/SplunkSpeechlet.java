package com.splunk.modinput.alexa;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.ResultsReaderXml;
import com.splunk.Service;

public class SplunkSpeechlet implements Speechlet {
	protected static Logger logger = Logger.getLogger(SplunkSpeechlet.class);

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		// any initialization logic goes here
	}

	@Override
	public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) throws SpeechletException {
		return getWelcomeResponse();
	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		IntentMapping mapping = AlexaSessionManager.getIntentMappings().get(intentName);

		if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelpResponse();
		} else if (mapping == null) {
			throw new SpeechletException("Invalid Intent");
		} else {
			return getIntentResponse(mapping, intent);
		}

	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		// any cleanup logic goes here
	}

	/**
	 * Creates and returns a {@code SpeechletResponse} with a welcome message.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getWelcomeResponse() {
		String speechText = "Welcome to Splunk, ask me something";

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Splunk");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}

	/**
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getIntentResponse(IntentMapping mapping, Intent intent) {

		String search = mapping.getSearch();
		String response = mapping.getResponse();

		if (search != null && search.length() > 0) {
			response = executeSearch(search, response, intent.getSlots());
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Splunk");
		card.setContent(response);

		OutputSpeech speech;
		// ssml
		if (response.startsWith("<speak>")) {
			speech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) speech).setSsml(response);
		} else {
			speech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) speech).setText(response);
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private String executeSearch(String search, String response, Map<String, Slot> slots) {

		logger.error("executing searching");
		logger.error(search);
		logger.error(response);
		Set<String> slotKeys = slots.keySet();

		for (String key : slotKeys) {

			String value = slots.get(key).getValue();

			logger.error("slot key : " + key);

			logger.error("slot value : " + value);
			if (!key.equalsIgnoreCase("timeperiod")) {
				search = search.replaceAll("\\$" + key + "\\$", value);
				response = response.replaceAll("\\$" + key + "\\$", value);
				logger.error("1 : " + search);
				logger.error("1 : " + response);
			} else {

				search = search.replaceAll("\\$timeperiod\\$", AlexaSessionManager.getTimeMappings().get(value));
				response = response.replaceAll("\\$timeperiod\\$", value);
				logger.error("2 : " + search);
				logger.error("2 : " + response);
			}

		}

		// execute search
		HashMap<String, String> outputKeyVal = performSearch("search " + search + " | head 1");

		if (outputKeyVal == null) {
			response = "I'm sorry , I couldn't find any results";
		} else {
			for (String key : outputKeyVal.keySet()) {
				logger.error("event key : " + key);
				response = response.replaceAll("\\$resultfield_" + key + "\\$", outputKeyVal.get(key));
				logger.error("3 : " + response);
			}
		}
		return response;
	}

	private HashMap<String, String> performSearch(String search) {

		logger.error("performing searching");

		try {
			Service splunkService = AlexaSessionManager.getService();
			JobArgs jobargs = new JobArgs();

			jobargs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
			logger.error("dispatch search : "+search);
			Job job = splunkService.search(search, jobargs);
			
			while (!job.isReady()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {

				}
			}
			logger.error("search done");

			InputStream resultsNormalSearch = job.getResults();
			
			ResultsReaderXml resultsReaderNormalSearch;

			resultsReaderNormalSearch = new ResultsReaderXml(resultsNormalSearch);
			HashMap<String, String> event;
			logger.error("event loop");
			while ((event = resultsReaderNormalSearch.getNextEvent()) != null) {
				logger.error("returning event");
				return event;
			}

		} catch (Exception e) {
			logger.error("Error performing search : " + search + " , because " + e.getMessage());
		}
		return null;
	}

	/**
	 * Creates a {@code SpeechletResponse} for the help intent.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getHelpResponse() {
		String speechText = "You can ask Splunk anything you want";

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Splunk");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}
}