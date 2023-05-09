/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rose.mary.trace.manager.BotErrorHandlerManager;
import rose.mary.trace.manager.BotLoaderManager;
import rose.mary.trace.manager.BoterManager;
import rose.mary.trace.manager.ChannelManager;
import rose.mary.trace.manager.CloneLoaderManager;
import rose.mary.trace.manager.ConfigurationManager;
import rose.mary.trace.manager.DirectLoaderManager;
import rose.mary.trace.manager.FinisherManager;
import rose.mary.trace.manager.LoaderManager;
import rose.mary.trace.manager.MonitorManager;
import rose.mary.trace.manager.SystemErrorTestManager;
import rose.mary.trace.manager.TesterManager;
import rose.mary.trace.manager.TraceErrorHandlerManager;
import rose.mary.trace.manager.TraceRouterManager;
import rose.mary.trace.manager.UnmatchHandlerManager;
import rose.mary.trace.manager.CacheManager;

/**
 * <pre>
 * The TraceServer controls apps.
 * </pre>
 * 
 * @author whoana
 * @since Aug 23, 2019
 */
public class TraceServer {

	Logger logger = LoggerFactory.getLogger(getClass());

	public final static int STATE_INIT = 0;
	public final static int STATE_STOP = 1;
	public final static int STATE_START = 2;
	public final static int STATE_SHUTDOWN = 3;

	private int state = STATE_INIT;

	private String name;

	// DatabasePolicyHandlerManager databasePolicyHandlerManager;

	ChannelManager channelManager;
	LoaderManager loaderManager;
	BoterManager boterManager;
	BotLoaderManager botLoaderManager;
	FinisherManager finisherManager;
	TraceErrorHandlerManager traceErrorHandlerManager;
	BotErrorHandlerManager botErrorHandlerManager;
	MonitorManager monitorManager;
	UnmatchHandlerManager unmatchHandlerManager;
	// DatabasePolicyHandlerManager databasePolicyHandlerManager;
	TesterManager testerManager;
	ConfigurationManager configurationManager;
	SystemErrorTestManager systemErrorTestManager;

	CacheManager cacheManager;

	DirectLoaderManager directLoaderManager;

	final static String TYPE_FIRST = "A";
	final static String TYPE_DIRECT = "B";
	final static String TYPE_ROUTER = "C";

	String type = TYPE_DIRECT;
	TraceRouterManager traceRouterManager;

	/**
	 * 
	 * @param name
	 */
	public TraceServer(
			String name,
			ChannelManager channelManager,
			LoaderManager loaderManager,
			BoterManager boterManager,
			BotLoaderManager botLoaderManager,
			FinisherManager finisherManager,
			TraceErrorHandlerManager traceErrorHandlerManager,
			BotErrorHandlerManager botErrorHandlerManager,
			MonitorManager monitorManager,
			UnmatchHandlerManager unmatchHandlerManager,
			// DatabasePolicyHandlerManager databasePolicyHandlerManager,
			TesterManager testerManager,
			ConfigurationManager configurationManager,
			SystemErrorTestManager systemErrorTestManager,
			CacheManager cacheManager) {
		this.name = name;
		this.channelManager = channelManager;
		this.loaderManager = loaderManager;
		this.boterManager = boterManager;
		this.botLoaderManager = botLoaderManager;
		this.finisherManager = finisherManager;
		this.traceErrorHandlerManager = traceErrorHandlerManager;
		this.botErrorHandlerManager = botErrorHandlerManager;
		this.monitorManager = monitorManager;
		this.unmatchHandlerManager = unmatchHandlerManager;
		// this.databasePolicyHandlerManager = databasePolicyHandlerManager;
		this.testerManager = testerManager;
		this.configurationManager = configurationManager;
		this.systemErrorTestManager = systemErrorTestManager;
		this.cacheManager = cacheManager;

	}

	boolean direct = true;

	public void setDirectLoaderManager(DirectLoaderManager directLoaderManager) {
		this.directLoaderManager = directLoaderManager;
	}

	public void setDirect(boolean direct) {
		this.direct = direct;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public TraceRouterManager getTraceRouterManager() {
		return traceRouterManager;
	}

	public void setTraceRouterManager(TraceRouterManager traceRouterManager) {
		this.traceRouterManager = traceRouterManager;
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void ready() throws Exception {
		if (TYPE_FIRST.equals(type)) {
			loaderManager.ready();
			boterManager.ready();
			botLoaderManager.ready();
			finisherManager.ready();
			botErrorHandlerManager.ready();
			traceErrorHandlerManager.ready();
			unmatchHandlerManager.ready();
			// databasePolicyHandlerManager.ready();
		} else if (TYPE_DIRECT.equals(type)) {
			finisherManager.ready();
			unmatchHandlerManager.ready();
			// databasePolicyHandlerManager.ready();
		} else if (TYPE_ROUTER.equals(type)) {
			traceRouterManager.ready();
			botLoaderManager.ready();
			// cloneLoaderManager.ready();
			finisherManager.ready();
			botErrorHandlerManager.ready();
			traceErrorHandlerManager.ready();
			unmatchHandlerManager.ready();
		} else {
			throw new Exception(
					"UndefinedServerTypeException : input type[".concat(type).concat("], type value must be A, B, C"));
		}

		state = STATE_INIT;
	}

	/*
	 * public void ready() throws Exception {
	 * 
	 * loaderManager.ready();
	 * boterManager.ready();
	 * botLoaderManager.ready();
	 * finisherManager.ready();
	 * botErrorHandlerManager.ready();
	 * traceErrorHandlerManager.ready();
	 * unmatchHandlerManager.ready();
	 * // databasePolicyHandlerManager.ready();
	 * state = STATE_INIT;
	 * }
	 */
	/**
	 * 
	 * @throws Exception
	 */
	/*
	 * for backup
	 * public void start() throws Exception {
	 * 
	 * startBotLoader();
	 * startBoter();
	 * startLoader();
	 * startChannel();
	 * startFinisher();
	 * if (startTraceErrorHandler)
	 * startTraceErrorHandler();
	 * if (startBotErrorHandler)
	 * startBotErrorHandler();
	 * startUnmatchHandler();
	 * // startDatabasePolicyHandler();
	 * 
	 * if (configurationManager.getConfig().getSystemErrorTestManagerConfig() !=
	 * null
	 * && configurationManager.getConfig().getSystemErrorTestManagerConfig().
	 * isStartOnLoad()) {
	 * systemErrorTestManager.start();
	 * }
	 * 
	 * state = STATE_START;
	 * }
	 */
	public void start() throws Exception {

		if (TYPE_FIRST.equals(type)) {
			startBotLoader();
			startBoter();
			startLoader();
			startChannel();
			startFinisher();
			if (startTraceErrorHandler)
				startTraceErrorHandler();
			if (startBotErrorHandler)
				startBotErrorHandler();
			startUnmatchHandler();
			// startDatabasePolicyHandler();

			if (configurationManager.getConfig().getSystemErrorTestManagerConfig() != null
					&& configurationManager.getConfig().getSystemErrorTestManagerConfig().isStartOnLoad()) {
				systemErrorTestManager.start();
			}
		} else if (TYPE_DIRECT.equals(type)) {
			startDirectLoader();
			startFinisher();
			startUnmatchHandler();
		} else if (TYPE_ROUTER.equals(type)) {
			startBotLoader();
			// startCloneLoader();
			startTraceRouter();
			startChannel();
			startFinisher();
			if (startTraceErrorHandler)
				startTraceErrorHandler();
			if (startBotErrorHandler)
				startBotErrorHandler();
			startUnmatchHandler();

		} else {
			throw new Exception(
					"UndefinedServerTypeException : input type[".concat(type).concat("], type value must be A, B, C"));
		}

		state = STATE_START;
	}

	private void startTraceRouter() throws Exception {
		traceRouterManager.start();
	}

	private void startDirectLoader() throws Exception {
		directLoaderManager.startLoaders();
	}

	boolean startBotErrorHandler = true;
	boolean startTraceErrorHandler = true;

	long stopDelay = 1000;

	/**
	 * 
	 * @throws Exception
	 */
	public void stop() throws Exception {

		if (TYPE_FIRST.equals(type)) {
			stopChannel();
			Thread.sleep(stopDelay);
			stopLoader();
			Thread.sleep(stopDelay);
			stopBoter();
			Thread.sleep(stopDelay);
			stopBotLoader();
			Thread.sleep(stopDelay);
			stopFinisher();

			if (startTraceErrorHandler)
				stopTraceErrorHandler();
			if (startBotErrorHandler)
				stopBotErrorHandler();
			stopUnmatchHandler();

			// stopDatabasePolicyHandler();

			if (configurationManager.getConfig().getSystemErrorTestManagerConfig().isStartOnLoad()) {
				systemErrorTestManager.stop();
			}

		} else if (TYPE_DIRECT.equals(type)) {
			stopDirectLoader();
			Thread.sleep(stopDelay);
			stopFinisher();
			Thread.sleep(stopDelay);
			stopUnmatchHandler();
		} else if (TYPE_ROUTER.equals(type)) {
			stopChannel();
			Thread.sleep(stopDelay);
			stopTraceRouter();
			Thread.sleep(stopDelay);
			stopBotLoader();
			// stopCloneLoader();
			Thread.sleep(stopDelay);
			stopFinisher();

			if (startTraceErrorHandler)
				stopTraceErrorHandler();
			if (startBotErrorHandler)
				stopBotErrorHandler();
			stopUnmatchHandler();

			// stopDatabasePolicyHandler();

		} else {
			throw new Exception(
					"UndefinedServerTypeException : input type[".concat(type).concat("], type value must be A, B, C"));
		}

		state = STATE_STOP;
	}
	/*
	 * public void stop() throws Exception {
	 * stopChannel();
	 * Thread.sleep(stopDelay);
	 * stopLoader();
	 * Thread.sleep(stopDelay);
	 * stopBoter();
	 * Thread.sleep(stopDelay);
	 * stopBotLoader();
	 * Thread.sleep(stopDelay);
	 * stopFinisher();
	 * 
	 * if (startTraceErrorHandler)
	 * stopTraceErrorHandler();
	 * if (startBotErrorHandler)
	 * stopBotErrorHandler();
	 * stopUnmatchHandler();
	 * 
	 * // stopDatabasePolicyHandler();
	 * 
	 * if (configurationManager.getConfig().getSystemErrorTestManagerConfig().
	 * isStartOnLoad()) {
	 * systemErrorTestManager.stop();
	 * }
	 * 
	 * state = STATE_STOP;
	 * }
	 */

	private void stopDirectLoader() {
		directLoaderManager.stopLoaders();
	}

	private void stopTraceRouter() {
		traceRouterManager.stop();
	}

	private void startBotErrorHandler() throws Exception {
		botErrorHandlerManager.start();
	}

	private void stopBotErrorHandler() {
		botErrorHandlerManager.stop();
	}

	// public void startDatabasePolicyHandler() throws Exception {
	// databasePolicyHandlerManager.start();
	// }

	// public void stopDatabasePolicyHandler() {
	// databasePolicyHandlerManager.stop();
	// }

	/**
	 * 
	 * @throws Exception
	 */
	private void startUnmatchHandler() throws Exception {
		unmatchHandlerManager.start();
	}

	/**
	 * 
	 */
	private void stopUnmatchHandler() {
		unmatchHandlerManager.stop();
	}

	/**
	 * @throws Exception
	 * 
	 */
	public void startTraceErrorHandler() throws Exception {
		traceErrorHandlerManager.start();
	}

	/**
	 * 
	 */
	public void stopTraceErrorHandler() {
		traceErrorHandlerManager.stop();
	}

	/**
	 * @throws Exception
	 * 
	 */
	public void startFinisher() throws Exception {
		finisherManager.start();
	}

	/**
	 * 
	 */
	public void stopFinisher() {
		finisherManager.stop();
	}

	/**
	 * @throws Exception
	 * 
	 */
	public void startChannel() throws Exception {
		if (ChannelManager.STATE_CHANNEL_STARTING == channelManager.getState()
				|| ChannelManager.STATE_CHANNEL_STARTING == channelManager.getState()) {
			throw new Exception("ChannelManagerStateException(Now Channel manager is stopping or starting channels.)");
		}
		channelManager.startChannels();
		// channelManager.startChannelHealthCheck();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void stopChannel() throws Exception {
		// channelManager.stopChannelHealthCheck();
		if (ChannelManager.STATE_CHANNEL_STARTING == channelManager.getState()
				|| ChannelManager.STATE_CHANNEL_STARTING == channelManager.getState()) {
			throw new Exception("ChannelManagerStateException(Now Channel manager is stopping or starting channels.)");
		}
		channelManager.stopChannels();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void startLoader() throws Exception {
		loaderManager.start();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void stopLoader() throws Exception {
		loaderManager.stop();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void startBoter() throws Exception {
		boterManager.start();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void stopBoter() throws Exception {
		boterManager.stop();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void startBotLoader() throws Exception {
		botLoaderManager.start();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void stopBotLoader() throws Exception {
		botLoaderManager.stop();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void startCloneLoader() throws Exception {
		cloneLoaderManager.start();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void stopCloneLoader() throws Exception {
		cloneLoaderManager.stop();
	}

	public void startMonitor() throws Exception {
		monitorManager.startMonitors();
	}

	public void stopMonitor() throws Exception {
		monitorManager.stopMonitors();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	public boolean getChannelsStarted() {
		return channelManager.getChannelsStarted();
	}

	public void startGenerateMsgTester() throws Exception {
		testerManager.start();

	}

	public void stopGenerateMsgTester() {
		testerManager.stop();
	}

	CloneLoaderManager cloneLoaderManager;

	public void setCloneLoaderManager(CloneLoaderManager cloneLoaderManager) {
		this.cloneLoaderManager = cloneLoaderManager;
	}

}
