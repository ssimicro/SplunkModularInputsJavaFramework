package com.splunk.modinput.alexa;

import java.io.InputStream;
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
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
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

		IntentMapping mapping = AlexaSessionManager.getIntentMappings().get("intentName");

		if (mapping == null) {
			throw new SpeechletException("Invalid Intent");
		}
		if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelpResponse();
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
		String speechText = "Welcome to Splunk";

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

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(response);

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private String executeSearch(String search, String response, Map<String, Slot> slots) {

		Set<String> slotKeys = slots.keySet();

		for (String key : slotKeys) {

			String value = slots.get(key).getValue();
			if (!key.equalsIgnoreCase("timeperiod")) {
				search.replaceAll("\\$" + key + "\\$", value);
				response.replaceAll("\\$" + key + "\\$", value);
			} else {

				search.replaceAll("\\$timeperiod\\$", AlexaSessionManager.getTimeMappings().get(value));
				response.replaceAll("\\$timeperiod\\$", value);
			}

		}

		// execute search
		HashMap<String, String> outputKeyVal = performSearch("search " + search + "| head 1");
		for (String key : outputKeyVal.keySet())
			response.replaceAll("\\$resultfield_" + key + "\\$", outputKeyVal.get(key));

		return response;
	}

	private HashMap<String, String> performSearch(String search) {

		Service splunkService = AlexaSessionManager.getService();
		JobArgs jobargs = new JobArgs();

		jobargs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
		Job job = splunkService.search(search, jobargs);
		while (!job.isReady()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

			}
		}

		InputStream resultsNormalSearch = job.getResults();

		ResultsReaderXml resultsReaderNormalSearch;

		try {
			resultsReaderNormalSearch = new ResultsReaderXml(resultsNormalSearch);
			HashMap<String, String> event;
			while ((event = resultsReaderNormalSearch.getNextEvent()) != null) {
				return event;
			}

		} catch (Exception e) {
			logger.error("Error performing search : "+search+" , because "+e.getMessage());
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