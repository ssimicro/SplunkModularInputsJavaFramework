package com.splunk.modinput.jms;

import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Scheme;
import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;

public class JMSModularInput extends ModularInput {
		
	public static void main(String [] args){
		
		JMSModularInput instance = new JMSModularInput();
		instance.init(args);
		
	}

	@Override
	protected void doRun(Input input) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doValidate(Validation val) {
		
		try {
			//TODO actually do some proper validation
			System.exit(0);
		} catch (Exception e) {
			logger.error(e.getMessage());
			ValidationError error = new ValidationError("Validation Failed : "+e.getMessage());
			sendValidationError(error);
			System.exit(2);
		}
		
		
	}

	@Override
	protected Scheme getScheme() {
		
		Scheme scheme = new Scheme();
		scheme.setTitle("JMS Messagin");
		scheme.setDescription("Poll messages from queues and topics");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);
		
		Endpoint endpoint = new Endpoint();
		
		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("JMS queue or topic");
		arg.setDescription("Enter the name precisely in this format : topic/${mytopic} or queue/${myqueue}");	
		
		
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("jndi_initialcontext_factory");
		arg.setTitle("JNDI Initial Context Factory Name");
		arg.setDescription("Name of the initial context factory.If you are using a specific context factory implmentation, ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/JMSModularInput/bin/lib directory");		
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("jndi_provider_url");
		arg.setTitle("JNDI Provider URL");
		arg.setDescription("URL to the JNDI Server");		
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("jndi_user");
		arg.setTitle("JNDI username");
		arg.setDescription("JNDI Username to authenticate with");		
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("jndi_pass");
		arg.setTitle("JNDI password");
		arg.setDescription("JNDI Password  to authenticate with");		
		endpoint.addArg(arg);
		
		arg = new Arg();
		arg.setName("jms_connection_factory_name");
		arg.setTitle("JMS Connection Factory Name");
		arg.setDescription("Name of the JMS Connection Factory.If you are using a specific message provider implmentation, ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/JMSModularInput/bin/lib directory");		
		endpoint.addArg(arg);
		
		scheme.setEndpoint(endpoint);
		
		return scheme;
	}
	
	

}
