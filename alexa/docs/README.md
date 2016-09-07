## Talk to Splunk with Amazon Alexa v0.5

## Overview

This is a Splunk App that enables your Splunk instance for interfacing with Amazon Alexa by way of a
custom Alexa skill, thereby provisioning a Natural Language interface for Splunk.

You can then use an Alexa device such as Echo,Tap or Dot to tell or ask Splunk anything you want.

* Get answers to questions based off Splunk Searches
* Ask for information , such as search command descriptions
* Return static responses and audio file snippets
* Developer extension hooks to plug in ANY custom voice driven requests and actions you want

The App also allows you to train your Splunk instance to the conversational vocabulary for your specific use case.

## Vision

The ultimate vision I foresee here is a future where you can completely do away with your keyboard, mouse , monitor & login prompt.

Even right now there are use cases where having to look at a monitor or operate an input device are simply counter productive, infeasible or unsafe , such as industrial operating environments.

You should be able to be transparently & dynamically authenticated based on your voice signature 
and then simply converse with your data like how you would talk to another person... asking questions or requesting to perform some action.This app is a step in the direction of this vision.

[Video of this app in action with an Echo device](https://www.youtube.com/watch?v=VonQytgcoms)

## Dependencies

* Internet accessible Splunk version 5+ instance
* Ability to open your firewall to incoming HTTPs requests , default port 443 , but configurable to any port. If you are opening a port < 1024 , you'll need to be running Splunk as a privileged user.
* Java Runtime version 8+ installed on your Splunk server
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX
* An Alexa device(Echo/Tap/Dot) and [free Amazon Developer account](http://developer.amazon.com)

## Setup

* Untar the release to your `$SPLUNK_HOME/etc/apps` directory
* Restart Splunk

## Generate Your Crypto Assets

Place your crypto assets and Java Keystore file (`java-keystore.jks`) in the `SPLUNK_HOME/etc/apps/alexa/crypto` directory.

[Follow the docs here for creating a certificate and private key](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/docs/testing-an-alexa-skill#create-a-private-key-and-self-signed-certificate-for-testing)

[Follow the docs here for using the certificate and private key to set up a Java KeyStore,ignore step 4](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/deploying-a-sample-skill-as-a-web-service#h3_keystore)

Note , make sure the keystore and the key have the **SAME** password. 

## Configuration

In SplunkWeb , browse to `Settings -> Data Inputs -> Alexa` and create an input stanza.
The fields are described in the web interface or you can read `SPLUNK_HOME/etc/apps/alexa/README/inputs.conf.spec`

Upon saving this stanza , an HTTPs web server will be spawned to start listening for incoming 
requests from the Amazon Alexa Cloud Service.

![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/setup.png)


## Firewall

You will need to open your firewall to your internet accessible Splunk instance to accept incoming 
requests for the HTTPs port you are using.


## Setting up your Splunk Alexa Skill

The means by which you interface your Alexa device(Echo/Tap/Dot) to Splunk is by registering a custom Alexa Skill with the AWS Alexa Cloud Service.

This App is a web service based implementation of a custom Alexa Skill you can register.

As we want this custom skill to be private and secure to your own usage , you are going to be 
registering the skill under your own free Developer account.This is in essence 100% functionally equivalent to
hosting a private Alexa skill(not currently an Alexa feature offering) rather than a publicly published Alexa skill.

Because this is not a publicly published skill , we don't need to worry about [implementing an authentication handler as detailed here](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/linking-an-alexa-user-with-a-user-in-your-system),
as this skill will be private to your own Alexa device.

![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/arch.png)

### Let's get started

1. [Sign up for your free Developer Account](http://developer.amazon.com)

2. [Register the Splunk skill](https://developer.amazon.com/edw/home.html#/skills/list)

### Skill Information Tab

**Application Id** :  this is generated for you , but can be then provided when you setup your Splunk App for more security
 
**Name** : anything you want ie: My Splunk Server
 
**Invocation name** : splunk   , this is then used when you talk to your Echo (" Alexa .... ask splunk .....") .Doesn't have to be "splunk" , you can use any name you want.
 
**Endpoint** : https://YOURHOST/alexa
 

![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/skill_1.png)

### Interaction model Tab
 
Samples are in the `SPLUNK_HOME/etc/apps/alexa/crypto/alexa_assets` directory from the Splunk App you installed.
Just copy paste them into the appropriate boxes below.
Whenever you add more slots/utterances/intents as you train up your Splunk instance , you will also have to 
update this interaction model tab.
 
![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/skill_2.png)


### SSL Certificate Tab
 
Select "I will upload a self-signed certificate in X.509 format” 
Copy paste your `certificate.pem` file contents(just open in a text editor) that you created in the Crypto instructions above.
 
![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/skill_3.png)

 
### Test Tab
 
Enable the skill. You should see "This skill is enabled for testing on this account."
 
 
![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/skill_4.png)
 
 
Test that it is all working using the service simulator.
 
A few things you can ask :
 
1. What is splunk
2. What is the XXXX  search command   (any command on docs)
3. What is the max cpu usage of server foo today
4. What is the average cpu usage of server foo yesterday
 
![alt text](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/docs/skill_5.png) 


## Training your Splunk instance

This App ships with a simple example vocabulary to get you started , but you are soon going to want to extend this by training up your own Splunk instance.
So this App is designed around the concept of a training model.

Every user of this App will want to voice interact with their Splunk instance differently , usually 
based on the domain of data they have indexed and the questions they want to ask that data by way of 
underlying Splunk searches.

So over time you will train up your Splunk instance to develop a rich conversational vocabulary.

There are 2 parts to training up your vocabulary :

1. Editing JSON files in the `SPLUNK_HOME/etc/apps/alexa/intents` directory.All JSON config files are monitored and dynamically reloaded every 10 secs if they have changed , so there is no need to restart the App or Splunk server when you make changes.

2. Updating your Splunk Alexa Skill definition in your developer account under the **Interaction Model** tab with [utterances, slots and your intent schema](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interaction-model-reference).

  * **Utterances** : the permutations of various things you will say to your Alexa device
  * **Slots** : Utterances can be tokenised with dynamic values called slots. Alexa has many built in slot types or you can provide your own custom slot types.
  * **Intents** : these are essentially identifier codes representing a set of potential utterances and will get sent to the Splunk skill(this App) along with any slot values that were used with the utterance when it was spoken to your Alexa device.The Splunk skill is then able to use this intent  to map to some underlying action to perform , such as executing a Splunk search, and then create a response to send back to Alexa to be spoken back to you.Pretty simple really.
  
For convenience you can keep a copy of your utterances,slots and intent schema in the `SPLUNK_HOME/etc/apps/alexa/alexa_assets` directory.

###Configuring mapping.json

This JSON file is the heart of the training model. It is where you define the mapping of the incoming intent to some action to perform.

The actions that you can perform are :

*  Execute a Search and return a response (text or SSML)
*  Execute a Saved Search and return a response (text or SSML)
*  Return a static text response
*  Return a SSML formatted response
*  Return an MP3 soundbite
*  Execute a dynamic action that you have coded and plugged in and return a response (text or SSML)

###Search actions

*  **intent** : the name of the incoming request intent to map this action to
*  **search** : the SPL search string to execute. This can also be tokenised with the name of a slot key passed in with the intent
  * `index=_internal | eval cpu=45  | head 1`
  * `index=_internal host=$servername$| eval cpu=45  | head 1`
  *  the fields in the search result can then be interpolated into the response. See more below on response formatting
*  **time_slot** : the name of the time slot key passed in with the intent. See more below on mapping of human speakable times to Splunk times.
*  **response** : the response to return back to Alexa. See more below on response formatting

###Saved Search actions

*  **intent** : the name of the incoming request intent to map this action to
*  **saved_search_name** : the name of the saved search
*  **saved_search_args** : any arguments you want to pass to the saved search.This can also be tokenised with the name of a slot key passed in with the intent
  *  `cpu=56`
  *  `server=$servername$,cpu=56`
  *  the fields in the search result can then be interpolated into the response. See more below on response formatting
*  **time_slot** : the name of the time slot key passed in with the intent. See more below on mapping of human speakable times to Splunk times.
*  **response** : the response to return back to Alexa. See more below on response formatting

###Static response actions

*  **intent** : the name of the incoming request intent to map this action to
*  **response** : the response to return back to Alexa. See more below on response formatting

###Create your own dynamic actions

You can easily extend the available set of built in actions by creating your own custom dynamic actions and plugging them in , all you need is some simple Java coding skills.

This App ships with an example dynamic action , [DocsLookupAction](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/src/com/splunk/modinput/alexa/dynamicaction/DocsLookupAction.java) , that responds to an incoming intent request 
to get information about a Splunk search command. The dynamic action simply makes an HTTP call out to the docs page , scrapes the search command description and returns this back to Alexa.

These are the steps for creating a new Dynamic Action :

1. Create a class that extends the [DynamicAction](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/src/com/splunk/modinput/alexa/DynamicAction.java) base class.
2. Implement the `executeAction()` method , use the [DocsLookupAction](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/blob/master/alexa/src/com/splunk/modinput/alexa/dynamicaction/DocsLookupAction.java) example as a guide.You can access slot values and custom arguments 
from within your code also.
3. Compile the class , add it to a jar file and place in the  `SPLUNK_HOME/etc/apps/alexa/dynamic_actions/lib` directory.Also place any other dependent jars for your action in this directory.
4. Update the `SPLUNK_HOME/etc/apps/alexa/dynamic_actions/dynamicactions.json` file to map the class name to some action name that you can refer to from `mapping.json`

  ```
  {
  "name": "foo_action",
  "class": "com.foo.FooAction"
  }
  ```

5. Add a mapping from an incoming intent request to this dynamic action in `mapping.json`
  *  **intent** : the name of the incoming request intent to map this action to
  *  **dynamic_action** : the action name in `dynamicactions.json` ie: `foo_action`
  *  **dynamic_action_args** : a key=value,key2=value2 ..... list of arguments that can be passed into the action and accessed by your code
  *  **response** : the response to return back to Alexa. See more below on response formatting


###Response formatting

The JSON `response` field in `mapping.json` can be in plain text or [SSML](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/speech-synthesis-markup-language-ssml-reference).

The response text or SSML can contain tokens to replace from the values of any slots that were passed in the request intent by wrapping the slot key in $ signs ie: `$timeperiod$` or `$servername$`  (except for static responses)

For searches and saved searches the response text or SSML can contain tokens to replace from the results of the searches. These are declared in the format `$resultfield_xxx$` , where `xxx` is the name of the field in the search result.

Dynamic action responses also have a special token `$dynamic_response` which is some dynamic text that the action returns ie: from the result of an HTTP lookup in the case of the example `DocsLookupAction`.This token can be used standalone or mixed in with plain text, SSML and slot tokens.

*  Static response : `Hello this is a lovely day`
*  Response with slot values : `Hello , I see you asked about server $servername$ $timeperiod$`
*  Response with slot values and search result fields : `The memory usage of server $servername$ $timeperiod$ is $resultfield_memoryusage$`  (where `memoryusage` would be the name of the search result field)
*  SSML response : `<speak> Hello there </speak>`
*  Dynamic action response : `My dynamic response is $dynamic_response$`

###Configuring timemappings.json

When you are communicating with your Alexa device you are going to use simple noun phrases to express time periods.So we need a way of mapping these human spoken times to Splunk time patterns.

This is accomplished with a combination of custom slots(TIME_PERIOD) and a mapping of the slot value(the noun phrase) in the `timemappings.json` mapping file.

To add more time noun phrases , you just update the TIME_PERIOD slots list for your Splunk Alexa skill in your Amazon developer console as shown above on the Interaction Model Tab screenshot.

To map these noun phrases you update the `timemappings.json` file in the Splunk App to map the time noun phrase to an earliest and latest Splunk time pattern.
```
{
"utterance": "yesterday",
"earliest": "-1d@d",
"latest": "@d"
}
```  
To use this time period slot , just refer to it in your Utterance definition for your Splunk Alexa skill
*  `MaxCPUIntent what is the maximum cpu usage of server {servername} {timeperiod}`   

Then in your search and saved search actions in `mapping.json` you can simply refer to the name of the time slot key in the `time_slot` field and it will be applied to your search
*  `"time_slot" : "timeperiod"`

###Soundbites

You can include MP3 soundbites in your response by embedding the URL of an MP3 file in an SSML formatted response.
*  `<speak>Splunk sounds like <audio src=\"https://www.myhost.com/soundbites/horse.mp3\"/></speak>`

This App is able to serve up these MP3 files over HTTPs for you also. All you need to do is to put your MP3 file in the `SPLUNK_HOME/etc/apps/alexa/soundbites` directory.

[Please refer to these additional guidelines for creating your MP3 soundbite](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/speech-synthesis-markup-language-ssml-reference#audio)


## Example walkthrough for setting up a new Intent

**TODO**

## Logging

Any errors can be searched for in SplunkWeb : `index=_internal error ExecProcessor alexa.py`

You can ignore any SLF4J errors 

## Troubleshooting

* Correct Splunk Version 5+ ?
* Correct Java Runtime version 8+ ?
* Supported OS ?
* HTTPs port was successfully opened ? `netstat -plnt` is a useful command to check with.
* Running Splunk as a privileged user if using a HTTPs port < 1024 ?
* Firewall is open for incoming traffic for the HTTPs port ?
* Correct path to Java keystore ?
* Correct name of Java keystore
* Correct Java keystore password ?
* Keystore and Key passwords are the same ?
* Have you looked in the logs for errors ? 
* Can you successfully test the skill from the Amazon developer console ?

## Support

This is a community supported App.For any issues please post your question to [answers.splunk.com](http://answers.splunk.com).The author will be notified with an email alert.

## Source Code

If you want the source code or are interested in collaborating , [then browse here to Github.](https://github.com/damiendallimore/SplunkModularInputsJavaFramework/tree/master/alexa) 
