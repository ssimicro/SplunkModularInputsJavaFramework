package com.splunk.modinput.jmx;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import java.util.List;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.xerces.parsers.SAXParser;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.dtdsoftware.splunk.config.Attribute;
import com.dtdsoftware.splunk.config.Formatter;
import com.dtdsoftware.splunk.config.JMXPoller;
import com.dtdsoftware.splunk.config.JMXServer;
import com.dtdsoftware.splunk.config.MBean;
import com.dtdsoftware.splunk.config.Operation;
import com.dtdsoftware.splunk.formatter.FormatterUtils;
import com.splunk.modinput.Arg;
import com.splunk.modinput.Endpoint;
import com.splunk.modinput.Input;
import com.splunk.modinput.Item;
import com.splunk.modinput.ModularInput;
import com.splunk.modinput.Param;
import com.splunk.modinput.Scheme;
import com.splunk.modinput.SplunkLogEvent;
import com.splunk.modinput.Stream;
import com.splunk.modinput.StreamEvent;

import com.splunk.modinput.Stanza;

import com.splunk.modinput.Validation;
import com.splunk.modinput.ValidationError;
import com.splunk.modinput.Scheme.StreamingMode;

import com.sun.tools.attach.VirtualMachine;

public class JMXModularInput extends ModularInput {

	public static void main(String[] args) {

		JMXModularInput instance = new JMXModularInput();
		instance.init(args);

	}

	private void streamEvent(String text, String stanzaName) {

		Stream stream = new Stream();
		StreamEvent event = new StreamEvent();
		event.setData(text);
		event.setStanza(stanzaName);
		ArrayList<StreamEvent> list = new ArrayList<StreamEvent>();
		list.add(event);
		stream.setEvents(list);
		marshallObjectToXML(stream);

	}

	@Override
	protected void doRun(Input input) throws Exception {

		if (input != null) {

			for (Stanza stanza : input.getStanzas()) {

				String name = stanza.getName();
				String configFile = "config.xml";// default
				int frequency = 60; // default seconds

				if (name != null) {

					List<Param> params = stanza.getParams();
					for (Param param : params) {
						String value = param.getValue();
						if (value == null || value.length() == 0) {
							continue;
						}
						if (param.getName().equals("config_file")) {

							configFile = value;

						} else if (param.getName().equals("config_file_dir")) {

							// override default config file directory
							System.setProperty("confighome",
									System.getProperty("splunkhome")
											+ File.separator + value
											+ File.separator);

						} else if (param.getName().equals("polling_frequency")) {

							try {
								frequency = Integer.parseInt(value);
							} catch (Exception e) {
								// if this fails, the default value will get
								// used
							}

						}
						else if (param.getName().equals("additional_jvm_properties")) {
							setJVMSystemProperties(param.getValue());
						}

					}
					new JMXExecutionThread(configFile, frequency, name).start();

				}

				else {
					logger.error("Invalid stanza name : " + name);
					System.exit(2);
				}

			}
		} else {
			logger.error("Input is null");
			System.exit(2);
		}

	}

	class JMXExecutionThread extends Thread {

		String configFile;// default
		int frequency; // default seconds
		String stanzaName;

		JMXExecutionThread(String configFile, int frequency, String stanzaName) {

			this.configFile = configFile;
			this.frequency = frequency;
			this.stanzaName = stanzaName;
		}

		public void run() {

			JMXMBeanPoller poller = new JMXMBeanPoller(stanzaName, configFile);
			while (!isDisabled(stanzaName)) {

				// reload the file if it has changed
				if (poller.configFileHasChanged()
						|| poller.hasDynamicPIDSettings())
					poller.loadConfigFile();

				poller.execute();

				try {
					Thread.sleep(frequency * 1000);
				} catch (InterruptedException e) {

				}
			}

		}
	}

	@Override
	protected void doValidate(Validation val) {

		try {

			if (val != null) {

				List<Item> items = val.getItems();
				for (Item item : items) {
					List<Param> params = item.getParams();

					for (Param param : params) {
						String value = param.getValue();
						if (value == null || value.length() == 0) {
							continue;
						}

						if (param.getName().equals("config_file")) {

							File configFile = getConfigFile(value);
							if (!configFile.exists()) {
								throw new Exception(
										"Config file "
												+ value
												+ " does not exist.Ensure that this file is placed in the jmx_ta/bin/config directory.");
							}
							if (configFile.isDirectory()) {
								throw new Exception(
										value
												+ " is a directory, you must pass the config file NAME.");
							}

							xsdValidation(configFile);

						}
						if (param.getName().equals("polling_frequency")) {
							int freq = Integer.parseInt(value);
							if (freq < 1) {
								throw new Exception(
										value
												+ " is invalid.Must be a positive integer");
							}

						}
					}
				}
			}
			System.exit(0);
		} catch (Exception e) {
			logger.error(e.getMessage());
			ValidationError error = new ValidationError("Validation Failed : "
					+ e.getMessage());
			sendValidationError(error);
			System.exit(2);
		}

	}

	private File getConfigFile(String value) {

		return new File(System.getProperty("confighome") + value);
	}

	@Override
	protected Scheme getScheme() {

		Scheme scheme = new Scheme();
		scheme.setTitle("JMX (Java Management Extensions)");
		scheme.setDescription("Monitor Java Virtual Machines via their exposed JMX MBean attributes, operations and notifications");
		scheme.setUse_external_validation(true);
		scheme.setUse_single_instance(true);
		scheme.setStreaming_mode(StreamingMode.XML);

		Endpoint endpoint = new Endpoint();

		Arg arg = new Arg();
		arg.setName("name");
		arg.setTitle("JMX Input Name");
		arg.setDescription("Name of the JMX input");
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("config_file");
		arg.setTitle("JMX Config File");
		arg.setDescription("Name of the config file.Defaults to config.xml");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("config_file_dir");
		arg.setTitle("JMX Config File Directory");
		arg.setDescription("Alternative location for the config files relative to SPLUNK_HOME ie: etc/apps/foobar");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("polling_frequency");
		arg.setTitle("Polling Frequency");
		arg.setDescription("How frequently to execute the polling in seconds.Defaults to 60");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);

		arg = new Arg();
		arg.setName("additional_jvm_propertys");
		arg.setTitle("Additional JVM Propertys");
		arg.setDescription("");
		arg.setRequired_on_create(false);
		endpoint.addArg(arg);
		
		scheme.setEndpoint(endpoint);

		return scheme;
	}

	class JMXMBeanPoller {

		private JMXPoller config;
		private Formatter formatter;
		boolean registerNotifications = true;
		String stanzaName;
		String configFileName;
		//default for the first run
		long configFileModifiedTime = 0;
		//if config file uses pidfile or pidcommand
		boolean hasDynamicPIDSettings = false;

		public JMXMBeanPoller(String stanzaName, String configFile) {

			this.configFileName = configFile;
			this.stanzaName = stanzaName;
		}

		/**
		 * Determine if config file has been modified
		 * @return
		 */
		public boolean configFileHasChanged() {
			File file = getConfigFile(this.configFileName);
			long currentModTime = file.lastModified();

			if (currentModTime > this.configFileModifiedTime) {
				this.configFileModifiedTime = currentModTime;
				return true;
			} else
				return false;

		}

		public void loadConfigFile() {
			try {
				// parse XML config into POJOs

				File file = getConfigFile(configFileName);
				this.config = loadConfig(file);
				config.normalizeClusters();
				this.hasDynamicPIDSettings = determineHasDynamicPIDSettings(config);
				this.formatter = config.getFormatter();
				if (formatter == null) {
					formatter = new Formatter();// default
				}

			} catch (Exception e) {

				logger.error("Error executing JMX stanza " + stanzaName + " : "
						+ e.getMessage());
			}
		}

		public boolean hasDynamicPIDSettings() {
			return hasDynamicPIDSettings;

		}

		private boolean determineHasDynamicPIDSettings(JMXPoller config) {

			if (this.config != null) {
				List<JMXServer> servers = this.config.getServers();
				if (servers != null) {

					for (JMXServer server : servers) {
						if ((server.getPidCommand() != null && server
								.getPidCommand().length() > 0)
								|| (server.getPidFile() != null && server
										.getPidFile().length() > 0)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		public void execute() {

			try {

				if (this.config != null) {
					// get list of JMX Servers and process in their own thread.
					List<JMXServer> servers = this.config.normalizeMultiPIDs();
					if (servers != null) {

						for (JMXServer server : servers) {
							new ProcessServerThread(server,
									this.formatter.getFormatterInstance(),
									this.registerNotifications, stanzaName)
									.start();
						}
						// we only want to register a notification listener on
						// the
						// first iteration
						this.registerNotifications = false;
					} else {
						logger.error("No JMX servers have been specified, stanza : "
								+ stanzaName);
					}
				} else {
					logger.error("The root config object(JMXPoller) failed to initialize, stanza : "
							+ stanzaName);
				}
			} catch (Exception e) {

				logger.error("Error executing JMX stanza " + stanzaName + " : "
						+ e.getMessage());

			}
		}

		/**
		 * Parse the config XML into Java POJOs and validate against XSD
		 * 
		 * @param configFileName
		 * @return The configuration POJO root
		 * @throws Exception
		 */
		private JMXPoller loadConfig(File file) throws Exception {

			if (file.isDirectory()) {
				throw new Exception(
						file.getName()
								+ " is a directory, you must pass the file NAME to this program and this file must adhere to the config.xsd schema");
			} else if (!file.exists()) {
				throw new Exception("The config file " + file.getName()
						+ " does not exist");
			}

			xsdValidation(file);

			// use CASTOR to parse XML into Java POJOs
			Mapping mapping = new Mapping();
			URL mappingURL = JMXPoller.class.getResource("/mapping.xml");
			mapping.loadMapping(mappingURL);
			Unmarshaller unmar = new Unmarshaller(mapping);

			FileReader fr = new FileReader(file);
			InputSource inputSource = new InputSource(fr);

			JMXPoller poller = (JMXPoller) unmar.unmarshal(inputSource);

			return poller;

		}

	}

	class ProcessServerThread extends Thread {

		private MBeanServerConnection serverConnection;
		private JMXConnector jmxc;

		private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

		private JMXServer serverConfig;

		private boolean directJVMAttach = false;

		// formatter
		private com.dtdsoftware.splunk.formatter.Formatter formatter;

		private boolean registerNotificationListeners;

		private String stanzaName;

		/**
		 * Thread to run each JMX Server connection in
		 * 
		 * @param serverConfig
		 *            config POJO for this JMX Server
		 * @param formatter
		 *            formatter impl
		 * @param transport
		 *            transport impl
		 */
		public ProcessServerThread(JMXServer serverConfig,
				com.dtdsoftware.splunk.formatter.Formatter formatter,
				boolean registerNotificationListeners, String stanzaName) {

			this.serverConfig = serverConfig;
			this.formatter = formatter;
			this.registerNotificationListeners = registerNotificationListeners;
			this.stanzaName = stanzaName;
			// set up the formatter
			Map<String, String> meta = new HashMap<String, String>();

			if (serverConfig.getProcessID() > 0) {
				this.directJVMAttach = true;
				meta.put(
						com.dtdsoftware.splunk.formatter.Formatter.META_PROCESS_ID,
						String.valueOf(serverConfig.getProcessID()));
			}

			meta.put(com.dtdsoftware.splunk.formatter.Formatter.META_HOST,
					this.serverConfig.getHost());
			meta.put(
					com.dtdsoftware.splunk.formatter.Formatter.META_JVM_DESCRIPTION,
					this.serverConfig.getJvmDescription());

			formatter.setMetaData(meta);

		}

		@Override
		public void run() {
			boolean notificationsRegistered = false;
			try {

				// establish connection to JMX Server
				connect();

				// get list of MBeans to Query
				List<MBean> mbeans = serverConfig.getMbeans();
				if (mbeans != null) {
					for (MBean bean : mbeans) {

						// the list of queried MBeans found on the server
						// if no values are specified for domain and properties
						// attributes , the value will default to the * wildcard
						Set<ObjectInstance> foundBeans = serverConnection
								.queryMBeans(
										new ObjectName(
												(bean.getDomain().length() == 0 ? "*"
														: bean.getDomain())
														+ ":"
														+ (bean.getPropertiesList()
																.length() == 0 ? "*"
																: bean.getPropertiesList())),
										null);

						for (ObjectInstance oi : foundBeans) {
							ObjectName on = oi.getObjectName();
							// the mbean specific part of the SPLUNK output
							// String
							String mBeanName = on.getCanonicalName();

							try {
								com.dtdsoftware.splunk.config.Notification notification = bean
										.getNotification();
								if (registerNotificationListeners
										&& notification != null) {

									NotificationFilter filter = null;
									String filterClass = notification
											.getFilterImplementationClass();
									if (filterClass != null
											&& filterClass.length() > 0) {
										filter = (NotificationFilter) Class
												.forName(filterClass)
												.newInstance();
									}
									SplunkNotificationListener listener = new SplunkNotificationListener(
											mBeanName, stanzaName);
									serverConnection.addNotificationListener(
											on, listener, filter, null);
									notificationsRegistered = true;

								}
							} catch (Exception e1) {
								logger.error("Error registering notification listener for JMX stanza "
										+ stanzaName + " : " + e1.getMessage());
							}
							Map<String, String> mBeanAttributes = new HashMap<String, String>();

							// execute operations
							if (bean.getOperations() != null) {

								for (Operation operation : bean.getOperations()) {
									try {
										Object result = serverConnection
												.invoke(on,
														operation.getName(),
														operation
																.getParametersArray(),
														operation
																.getSignatureArray());
										String outputname = operation
												.getOutputname();
										if (outputname != null
												&& !outputname.isEmpty())
											// mBeanAttributes.put(operation
											// .getOutputname(),
											// resolveObjectToString(result));
											extractAttributeValue(result,
													mBeanAttributes,
													operation.getOutputname());
									} catch (Exception e) {

										logger.error("Error executing JMX stanza "
												+ stanzaName
												+ " : "
												+ e.getMessage());
									}
								}
							}
							// extract all attributes
							if (bean.isDumpAllAttributes()) {
								MBeanAttributeInfo[] attributes = serverConnection
										.getMBeanInfo(on).getAttributes();
								for (MBeanAttributeInfo attribute : attributes) {
									try {
										if (attribute.isReadable()) {
											Object attributeValue = serverConnection
													.getAttribute(on,
															attribute.getName());
											extractAttributeValue(
													attributeValue,
													mBeanAttributes,
													attribute.getName());
										}
									} catch (Exception e) {

										logger.error("Error executing JMX stanza "
												+ stanzaName
												+ " : "
												+ e.getMessage());
									}

								}

							}
							// extract attributes
							else if (bean.getAttributes() != null) {

								// look up the attribute for the MBean
								for (Attribute singular : bean.getAttributes()) {
									List<String> tokens = singular.getTokens();
									Object attributeValue = null;

									// if the attribute pattern is multi level,
									// loop
									// through the levels until the value is
									// found
									for (String token : tokens) {

										// get root attribute object the first
										// time
										if (attributeValue == null)
											try {

												attributeValue = serverConnection
														.getAttribute(on, token);
											} catch (Exception e) {

												logger.error("Error executing JMX stanza "
														+ stanzaName
														+ " : "
														+ e.getMessage());
											}
										else if (attributeValue instanceof CompositeData) {
											try {

												attributeValue = ((CompositeData) attributeValue)
														.get(token);
											} catch (Exception e) {

												logger.error("Error executing JMX stanza "
														+ stanzaName
														+ " : "
														+ e.getMessage());
											}
										} else if (attributeValue instanceof TabularData) {
											try {

												Object[] key = { token };

												attributeValue = ((TabularData) attributeValue)
														.get(key);

											} catch (Exception e) {

												logger.error("Error executing JMX stanza "
														+ stanzaName
														+ " : "
														+ e.getMessage());
											}
										} else {
										}
									}

									mBeanAttributes
											.put(singular.getOutputname(),
													resolveObjectToString(attributeValue));

								}

							}

							String payload = formatter
									.format(mBeanName, mBeanAttributes,
											System.currentTimeMillis());

							streamEvent(payload, stanzaName);
						}

					}

				}

			} catch (Exception e) {

				logger.error(serverConfig + ",stanza=" + stanzaName
						+ ",systemErrorMessage=\"" + e.getMessage() + "\"");
			} finally {
				// need to keep this server connection open if we registered
				// notification listeners
				if (jmxc != null && !notificationsRegistered) {
					try {
						jmxc.close();
					} catch (Exception e) {
						logger.error("Error executing JMX stanza " + stanzaName
								+ " : " + e.getMessage());
					}
				}

			}

		}

		/**
		 * Extract MBean attributes and if necessary, deeply inspect and resolve
		 * composite and tabular data.
		 * 
		 * @param attributeValue
		 *            the attribute object
		 * @param mBeanAttributes
		 *            the map used to hold attribute values before being handed
		 *            off to the formatter
		 * @param attributeName
		 *            the attribute name
		 */
		private void extractAttributeValue(Object attributeValue,
				Map<String, String> mBeanAttributes, String attributeName) {

			if (attributeValue instanceof String[]) {
				try {
					mBeanAttributes.put(attributeName,
							resolveObjectToString(attributeValue));
				} catch (Exception e) {

					logger.error("Error executing JMX stanza " + stanzaName
							+ " : " + e.getMessage());
				}
			} else if (attributeValue instanceof Object[]) {
				try {
					int index = 0;
					for (Object obj : (Object[]) attributeValue) {
						index++;
						extractAttributeValue(obj, mBeanAttributes,
								attributeName + "_" + index);
					}
				} catch (Exception e) {

					logger.error("Error executing JMX stanza " + stanzaName
							+ " : " + e.getMessage());
				}
			} else if (attributeValue instanceof Collection) {
				try {
					int index = 0;
					for (Object obj : (Collection) attributeValue) {
						index++;
						extractAttributeValue(obj, mBeanAttributes,
								attributeName + "_" + index);
					}
				} catch (Exception e) {

					logger.error("Error executing JMX stanza " + stanzaName
							+ " : " + e.getMessage());
				}
			} else if (attributeValue instanceof CompositeData) {
				try {
					CompositeData cds = ((CompositeData) attributeValue);
					CompositeType ct = cds.getCompositeType();

					Set<String> keys = ct.keySet();

					for (String key : keys) {
						extractAttributeValue(cds.get(key), mBeanAttributes,
								attributeName + "_" + key);
					}

				} catch (Exception e) {

					logger.error("Error executing JMX stanza " + stanzaName
							+ " : " + e.getMessage());
				}
			} else if (attributeValue instanceof TabularData) {
				try {
					TabularData tds = ((TabularData) attributeValue);
					Set keys = tds.keySet();
					for (Object key : keys) {

						Object keyName = ((List) key).get(0);
						Object[] keyArray = { keyName };
						extractAttributeValue(tds.get(keyArray),
								mBeanAttributes, attributeName + "_" + keyName);
					}

				} catch (Exception e) {
					logger.error("Error executing JMX stanza " + stanzaName
							+ " : " + e.getMessage());
				}
			} else {

				try {
					mBeanAttributes.put(attributeName,
							resolveObjectToString(attributeValue));
				} catch (Exception e) {

					logger.error("Error executing JMX stanza " + stanzaName
							+ " : " + e.getMessage());
				}
			}

		}

		/**
		 * Resolve an Object to a String representation. Arrays, Lists, Sets and
		 * Maps will be recursively deep resolved
		 * 
		 * @param obj
		 * @return
		 */
		private String resolveObjectToString(Object obj) {

			StringBuffer sb = new StringBuffer();
			if (obj != null) {

				// convert an array to a List view
				if (obj instanceof Object[]) {
					sb.append(Arrays.toString((Object[]) obj));
				} else if (obj instanceof int[]) {
					sb.append(Arrays.toString((int[]) obj));
				} else if (obj instanceof long[]) {
					sb.append(Arrays.toString((long[]) obj));
				} else if (obj instanceof float[]) {
					sb.append(Arrays.toString((float[]) obj));
				} else if (obj instanceof double[]) {
					sb.append(Arrays.toString((double[]) obj));
				} else if (obj instanceof char[]) {
					sb.append(Arrays.toString((char[]) obj));
				} else if (obj instanceof boolean[]) {
					sb.append(Arrays.toString((boolean[]) obj));
				} else if (obj instanceof byte[]) {
					sb.append(Arrays.toString((byte[]) obj));
				} else if (obj instanceof short[]) {
					sb.append(Arrays.toString((short[]) obj));
				}

				else if (obj instanceof Map) {
					sb.append("[");
					Map map = (Map) obj;
					Set keys = map.keySet();
					int totalEntrys = keys.size();
					int index = 0;
					for (Object key : keys) {
						index++;
						Object value = map.get(key);
						sb.append(resolveObjectToString(key));
						sb.append("=");
						sb.append(resolveObjectToString(value));
						if (index < totalEntrys)
							sb.append(",");
					}
					sb.append("]");
				} else if (obj instanceof List) {
					sb.append("[");
					List list = (List) obj;
					int totalEntrys = list.size();
					int index = 0;
					for (Object item : list) {
						index++;
						sb.append(resolveObjectToString(item));
						if (index < totalEntrys)
							sb.append(",");
					}
					sb.append("]");
				} else if (obj instanceof Set) {
					sb.append("[");
					Set set = (Set) obj;
					int totalEntrys = set.size();
					int index = 0;
					for (Object item : set) {
						index++;
						sb.append(resolveObjectToString(item));
						if (index < totalEntrys)
							sb.append(",");
					}
					sb.append("]");
				} else {
					sb.append(obj.toString());
				}
			}
			return sb.toString();

		}

		/**
		 * Connect to the JMX Server
		 * 
		 * @throws Exception
		 */
		private void connect() throws Exception {

			// get the JMX URL
			JMXServiceURL url = getJMXServiceURL();

			// only send user/pass credentials if they have been set
			if (serverConfig.getJmxuser().length() > 0
					&& serverConfig.getJmxpass().length() > 0
					&& !this.directJVMAttach) {
				Map<String, String[]> env = new HashMap<String, String[]>();
				String[] creds = { serverConfig.getJmxuser(),
						serverConfig.getJmxpass() };
				env.put(JMXConnector.CREDENTIALS, creds);

				jmxc = JMXConnectorFactory.connect(url, env);

			} else {
				jmxc = JMXConnectorFactory.connect(url);
			}

			serverConnection = jmxc.getMBeanServerConnection();

		}

		/**
		 * Get a JMX URL
		 * 
		 * @return the URL
		 * @throws Exception
		 */
		private JMXServiceURL getJMXServiceURL() throws Exception {
			JMXServiceURL url = null;

			// connect to local process
			if (serverConfig.getProcessID() > 0) {

				url = getURLForPid(serverConfig.getProcessID());
			}
			// connect to a remote process
			else {
				String rawURL = serverConfig.getJmxServiceURL();
				String protocol = serverConfig.getProtocol();
				// use raw URL
				if (rawURL != null && rawURL.length() > 0) {
					url = new JMXServiceURL(rawURL);
				}
				// construct URL for MX4J connectors
				else if (protocol.startsWith("soap")
						|| protocol.startsWith("hessian")
						|| protocol.startsWith("burlap")
						|| protocol.equalsIgnoreCase("local")) {

					String lookupPath = serverConfig.getLookupPath();
					if (lookupPath == null || lookupPath.length() == 0) {
						if (protocol.startsWith("soap")) {
							lookupPath = "/jmxconnector";

						} else {
							lookupPath = "/" + protocol;
						}
					}
					url = new JMXServiceURL(serverConfig.getProtocol(),
							serverConfig.getHost(), serverConfig.getJmxport(),
							lookupPath);
				}
				// use remote encoded stub for JSR160 iiop and rmi
				else if (serverConfig.getStubSource().equalsIgnoreCase("ior")
						|| serverConfig.getStubSource()
								.equalsIgnoreCase("stub")) {
					url = new JMXServiceURL(protocol, "", 0, "/"
							+ serverConfig.getStubSource() + "/"
							+ serverConfig.getEncodedStub());
				}
				// use jndi lookup for JSR160 iiop and rmi stub
				else if (serverConfig.getStubSource().equalsIgnoreCase("jndi")) {

					String lookupPath = serverConfig.getLookupPath();
					if (lookupPath == null || lookupPath.length() == 0) {
						lookupPath = "/jmxrmi";
					}

					String urlPath = "/" + serverConfig.getStubSource() + "/"
							+ protocol + "://" + serverConfig.getHost() + ":"
							+ serverConfig.getJmxport() + lookupPath;

					url = new JMXServiceURL(protocol, "", 0, urlPath);
				}

			}

			return url;

		}

		/**
		 * Get a JMX URL for a process ID
		 * 
		 * @param pid
		 * @return the URL
		 * @throws Exception
		 */
		private JMXServiceURL getURLForPid(int pid) throws Exception {

			// attach to the target application
			final VirtualMachine vm = VirtualMachine
					.attach(String.valueOf(pid));

			// get the connector address
			String connectorAddress = vm.getAgentProperties().getProperty(
					CONNECTOR_ADDRESS);

			// no connector address, so we start the JMX agent
			if (connectorAddress == null) {
				String agent = vm.getSystemProperties()
						.getProperty("java.home")
						+ File.separator
						+ "lib"
						+ File.separator + "management-agent.jar";
				vm.loadAgent(agent);

				// agent is started, get the connector address
				connectorAddress = vm.getAgentProperties().getProperty(
						CONNECTOR_ADDRESS);
				assert connectorAddress != null;
			}
			return new JMXServiceURL(connectorAddress);
		}
	}

	class SplunkNotificationListener implements NotificationListener {

		private String mBeanName;
		private String stanzaName;

		public SplunkNotificationListener(String mBeanName, String stanzaName) {
			this.mBeanName = mBeanName;
			this.stanzaName = stanzaName;
		}

		@Override
		public void handleNotification(Notification notification,
				Object handback) {

			try {
				if (notification != null) {
					SplunkLogEvent event = new SplunkLogEvent(
							"jmx-notification", "", true, false);

					SortedMap<String, String> mbeanNameParts = FormatterUtils
							.tokenizeMBeanCanonicalName(mBeanName);

					Set<String> mBeanNameKeys = mbeanNameParts.keySet();

					for (String key : mBeanNameKeys) {

						event.addPair(key, mbeanNameParts.get(key));

					}
					event.addPair("type", notification.getType());
					event.addPair("message", notification.getMessage());
					event.addPair("seqNum", notification.getSequenceNumber());
					event.addPair("timestamp", notification.getTimeStamp());
					event.addPair("userData", notification.getUserData()
							.toString());
					event.addPair("source", notification.getSource().toString());
					event.addPair("class", notification.getClass()
							.getCanonicalName());
					streamEvent(event.toString(), stanzaName);
				}
			} catch (Exception e) {
				logger.error("Error executing JMX stanza " + stanzaName + " : "
						+ e.getMessage());
			}

		}

	}

	private void xsdValidation(File file) throws Exception {

		FileReader fr = new FileReader(file);
		InputSource inputSource = new InputSource(fr);
		SchemaValidator validator = new SchemaValidator();
		validator.validateSchema(inputSource);

	}

	class SchemaValidator {

		/**
		 * Validate XML source
		 * 
		 * @param xml
		 * @throws Exception
		 *             if validation fails
		 */
		public void validateSchema(InputSource xml) throws Exception {

			SAXParser parser = new SAXParser();
			try {

				parser.setFeature("http://xml.org/sax/features/validation",
						true);
				parser.setFeature(
						"http://apache.org/xml/features/validation/schema",
						true);
				parser.setFeature(
						"http://apache.org/xml/features/validation/schema-full-checking",
						true);

				// config.xsd is on the classpath in jmxpoller.jar
				URL schemaUrl = SchemaValidator.class
						.getResource("/config.xsd");

				parser.setProperty(
						"http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
						schemaUrl.toString());
				Validator handler = new Validator();
				parser.setErrorHandler(handler);

				parser.parse(xml);

				if (handler.validationError == true)
					throw new Exception("XML has a validation error:"
							+ handler.validationError + ""
							+ handler.saxParseException.getMessage());
				else
					return;
			} catch (Exception e) {
				throw new Exception(e.getMessage());
			}

		}

		/**
		 * Validation handler
		 * 
		 */
		private class Validator extends DefaultHandler {
			public boolean validationError = false;

			public SAXParseException saxParseException = null;

			public void error(SAXParseException exception) throws SAXException {
				validationError = true;
				saxParseException = exception;
			}

			public void fatalError(SAXParseException exception)
					throws SAXException {
				validationError = true;
				saxParseException = exception;
			}

			public void warning(SAXParseException exception)
					throws SAXException {
			}
		}

	}

}
