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
		String dynamicAction = mapping.getDynamicAction();
		String dynamicActionArgs = mapping.getDynamicActionArgs();

		if (search != null && search.length() > 0) {
			response = executeSearch(search, response, intent.getSlots(), mapping.getTimeSlot());
		}
		if (dynamicAction != null && dynamicAction.length() > 0) {
			DynamicActionMapping dam = AlexaSessionManager.getDynamicActionMappings().get(dynamicAction);
			try {
				DynamicAction instance = (DynamicAction) (Class.forName(dam.getClassName()).newInstance());
				instance.setArgs(dynamicActionArgs);
				instance.setSlots(intent.getSlots());
				instance.setResponseTemplate(response);
				response = instance.executeAction();
			} catch (Exception e) {
			}
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Splunk");
		card.setContent(response);

		OutputSpeech speech;
		// ssml
		if (response.startsWith("<speak>")) {
			speech = new SsmlOutputSpeech();
			response = response.replaceAll("\\\\", "");
			((SsmlOutputSpeech) speech).setSsml(response);
		} else {
			speech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) speech).setText(response);
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private String executeSearch(String search, String response, Map<String, Slot> slots, String timeSlot) {

		String earliest = "";
		String latest = "";
		Set<String> slotKeys = slots.keySet();

		for (String key : slotKeys) {

			String value = slots.get(key).getValue();

			if (!key.equalsIgnoreCase(timeSlot)) {
				search = search.replaceAll("\\$" + key + "\\$", value);
				response = response.replaceAll("\\$" + key + "\\$", value);

			} else {
				TimeMapping tm = AlexaSessionManager.getTimeMappings().get(value);
				earliest = tm.getEarliest();
				latest = tm.getLatest();
				search = search.replaceAll("\\$" + timeSlot + "\\$",
						"earliest=" + tm.getEarliest() + " latest=" + tm.getLatest());
				response = response.replaceAll("\\$" + timeSlot + "\\$", value);

			}

		}

		// execute search
		HashMap<String, String> outputKeyVal = performSearch("search " + search + " | head 1", earliest, latest);

		if (outputKeyVal == null) {
			response = "I'm sorry , I couldn't find any results";
		} else {
			for (String key : outputKeyVal.keySet()) {

				response = response.replaceAll("\\$resultfield_" + key + "\\$", outputKeyVal.get(key));

			}
		}
		return response;
	}

	private HashMap<String, String> performSearch(String search, String earliestTime, String latestTime) {

		try {
			Service splunkService = AlexaSessionManager.getService();
			JobArgs jobargs = new JobArgs();

			jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);
			jobargs.setEarliestTime(earliestTime);
			jobargs.setLatestTime(latestTime);

			Job job = splunkService.getJobs().create(search, jobargs);

			InputStream resultsNormalSearch = job.getResults();

			ResultsReaderXml resultsReaderNormalSearch;

			resultsReaderNormalSearch = new ResultsReaderXml(resultsNormalSearch);
			HashMap<String, String> event;

			while ((event = resultsReaderNormalSearch.getNextEvent()) != null) {

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