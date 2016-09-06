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

This App is designed around the concept of a training model.
Every user of this App will want to voice interact with their Splunk instance differently , usually 
based on the domain of data they have indexed and the questions they want to ask that data by way of 
underlying Splunk searches.
So over time you will train up your Splunk instance to develop a conversational vocabulary.

There are 2 parts to training up your vocabulary :

1. Editing JSON files in the `SPLUNK_HOME/etc/apps/alexa/intents` directory
2. Updating your Alexa Skill definition in your developer account under the Interaction Model tab with utterances, slots and the intent schema.

**TODO - In Progress**

## Example walkthrough for setting up a new Intent

**TODO - In Progress**

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
