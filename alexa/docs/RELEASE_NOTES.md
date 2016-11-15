0.7
----
* Can have time range defaults now if no time slot is passed in with the intent. These can be global  or per search intent.
* Can override the default mod input user (splunk-system-user) , and auth with your own user
* If you specify your own auth user , password can be encrypted
* Can specify the search namespace( aka app context) , global or per intent, saves having to ask App owners to make everything global.
* Multiple search result rows now supported , you don't have to do anything , they are magically handled.

0.6
-----
* Hardwired the SSL port to be 443 as currently the Alexa Cloud service does not support using other ports
* Added support for mapping generating search commands, ie: those starting with a pipe such as "| metadata" , "| tstats" etc..

0.5
-----
Initial beta release
