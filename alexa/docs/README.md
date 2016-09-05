## Talk to Splunk with Amazon Alexa v0.5

## Overview

This is a Splunk App that enables your Splunk instance for interfacing with Amazon Alexa, thereby 
provisioning a Natural Language interface for Splunk.
You can then use an Alexa device such as Echo,Tap or Dot to tell or ask Splunk anything you want. 

## Dependencies

* Internet accessible Splunk 5 + instance
* Ability to open your firewall to incoming HTTPs requests , default port 443 , but configurable to any port. If you are opening a port < 1024 , you'll need to be running Splunk as a privileged user.
* Java Runtime Version 8 + installed on your Splunk server
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX
* An Alexa device(Echo/Tap/Dot) and free Amazon Developer account(http://developer.amazon.com)

## Setup

* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk

## Crypto Assets

Place your crypto assets and Java Keystore file (java-keystore.jks) in the SPLUNK_HOME/etc/apps/alexa/crypto directory.

Follow the docs here for creating a certificate and private key : 

* https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/docs/testing-an-alexa-skill#create-a-private-key-and-self-signed-certificate-for-testing

Follow the docs here for using the certificate and private key to set up a Java KeyStore :

* https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/deploying-a-sample-skill-as-a-web-service#setting-up-an-ssl.2ftls-java-keystore

## Configuration

In SplunkWeb , browse to Settings -> Data Inputs -> Alexa and create an input stanza.

Upon saving this stanza , an HTTPs web server will be spawned to start listening for incoming 
requests from the Amazon Alexa Cloud Service.

## Firewall

You will need to open your firewall to your internet accessible Splunk instance to accept incoming 
requests for the HTTPs port you are using.


## Setting up your Splunk Alexa Skill

Please refer to ****this blog**** for comprehensive documentation on setting up your Splunk Alexa Skill.


## Training your Splunk instance

This App is designed around the concept of a training model.
Every user of this App will want to voice interact with their Splunk instance differently , usually 
based on the type/domain of data they have indexed and the questions they want to ask that data by way of 
underlying Splunk searches.
So over time you will train up your Splunk instance to develop a conversational vocabulary.

Please refer to ****this blog**** for comprehensive documentation on training your Splunk instance.

## Logging

Any errors can be searched for in SplunkWeb : index=_internal error ExecProcessor alexa.py

## Troubleshooting

* Correct Java Runtime version ?
* HTTPs port was successfully opened ?
* Running Splunk as a privileged user if using a HTTPs port < 1024 ?
* Firewall is open for incoming traffic for the HTTPs port ?
* Correct path to Java keystore ?
* Correct name of Java keystore
* Correct Java keystore password ?

## Contact

Damien Dallimore , ddallimore@splunk.com
