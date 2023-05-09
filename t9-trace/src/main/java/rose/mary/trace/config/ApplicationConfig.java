/**
 * Copyright 2018 t9.whoami.com Inc. All Rights Reserved.
 */
package rose.mary.trace.config;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.config.OldStateCheckHandlerConfig;
import rose.mary.trace.core.data.common.RuntimeInfo;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.monitor.SystemResourceMonitor;
import rose.mary.trace.core.monitor.ThroughputMonitor;

import rose.mary.trace.core.helper.checker.OldStateCheckHandler;
import rose.mary.trace.core.helper.checker.StateChecker;
import rose.mary.trace.core.helper.checker.StateCheckerMap;
import rose.mary.trace.database.service.BotService;
import rose.mary.trace.database.service.InterfaceService;
import rose.mary.trace.database.service.StateService;
import rose.mary.trace.database.service.SystemService;
import rose.mary.trace.database.service.TraceService;
import rose.mary.trace.loader.RouteHandler;
import rose.mary.trace.loader.StateHandler;
import rose.mary.trace.manager.BotErrorHandlerManager;
import rose.mary.trace.manager.BotLoaderManager;
import rose.mary.trace.manager.BoterManager;
import rose.mary.trace.manager.CacheManager;
import rose.mary.trace.manager.ChannelManager;
import rose.mary.trace.manager.CloneLoaderManager;
import rose.mary.trace.manager.ConfigurationManager;
import rose.mary.trace.manager.DirectLoaderManager;
import rose.mary.trace.manager.PolicyHandlerManager;
import rose.mary.trace.manager.RecoveryManager;
import rose.mary.trace.manager.FinisherManager;
import rose.mary.trace.manager.InterfaceCacheManager;
import rose.mary.trace.manager.LoaderManager;
import rose.mary.trace.manager.MonitorManager;
import rose.mary.trace.manager.ServerManager;
import rose.mary.trace.manager.SystemErrorTestManager;
import rose.mary.trace.manager.TesterManager;
import rose.mary.trace.manager.TraceErrorHandlerManager;
import rose.mary.trace.manager.TraceRouterManager;
import rose.mary.trace.manager.UnmatchHandlerManager;
import rose.mary.trace.server.TraceServer;
import rose.mary.trace.service.GenerateTraceMsgService;
import rose.mary.trace.system.SystemErrorDetector;
import rose.mary.trace.system.SystemUtil;

import javax.sql.DataSource;

/**
 * <pre>
 * ApplicationConfig
 * </pre>
 * 
 * @author whoana
 * @date Aug 1, 2019
 */
@Configuration
@EnableAsync
public class ApplicationConfig {

	Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

	@Autowired
	ApplicationContext applicationContext;

	@Autowired
	ConfigurationManager configurationManager;

	// @Autowired
	// TraceService traceService;

	// @Autowired
	// SystemService systemService;

	@Bean
	public RuntimeInfo runtimeInfo() {
		RuntimeInfo runtimeInfo = new RuntimeInfo();
		runtimeInfo.setBootKey(UUID.randomUUID().toString());
		runtimeInfo.setStartedTime(System.currentTimeMillis());
		return runtimeInfo;
	}

	int schedulerPoolSize = 10;
	String schedulerPrefix = "scheduler";

	@Bean(name = "taskScheduler")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(schedulerPoolSize);
		scheduler.setThreadNamePrefix(schedulerPrefix);
		scheduler.initialize();
		return scheduler;
	}

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		return executor;
	}

	@Bean(initMethod = "prepare")
	public ConfigurationManager configurationManager() throws Exception {
		ConfigurationManager manager = new ConfigurationManager();
		return manager;
	}

	@Bean("tpm1")
	public ThroughputMonitor channelThroughputMonitor() throws Exception {
		ThroughputMonitor monitor = new ThroughputMonitor(1000);
		return monitor;
	}

	@Bean("tpm2")
	public ThroughputMonitor loaderThroughputMonitor() throws Exception {
		ThroughputMonitor monitor = new ThroughputMonitor(1000);
		return monitor;
	}

	@Bean("tpm3")
	public ThroughputMonitor boterThroughputMonitor() throws Exception {
		ThroughputMonitor monitor = new ThroughputMonitor(1000);
		return monitor;
	}

	@Bean("tpm4")
	public ThroughputMonitor botLoaderThroughputMonitor() throws Exception {
		ThroughputMonitor monitor = new ThroughputMonitor(1000);
		return monitor;
	}

	@Bean
	public SystemResourceMonitor systemResourceMonitor() throws Exception {
		SystemResourceMonitor monitor = new SystemResourceMonitor(1000);
		return monitor;
	}

	@Bean
	public MonitorManager monitorManager(
			@Autowired SystemResourceMonitor srm,
			@Autowired @Qualifier("tpm1") ThroughputMonitor tpm1,
			@Autowired @Qualifier("tpm2") ThroughputMonitor tpm2,
			@Autowired @Qualifier("tpm3") ThroughputMonitor tpm3,
			@Autowired @Qualifier("tpm4") ThroughputMonitor tpm4) throws Exception {
		MonitorManager manager = new MonitorManager(srm, tpm1, tpm2, tpm3, tpm4);
		return manager;
	}

	@Bean(initMethod = "prepare")
	public CacheManager cacheManager(
		@Autowired ConfigurationManager configurationManager
	) throws Exception {
		CacheManager manager = new CacheManager(configurationManager.getCacheManagerConfig());
		return manager;
	}

	 

	@Bean(initMethod = "prepare")
	public InterfaceCacheManager interfaceCacheManager(@Autowired ConfigurationManager configurationManager,
			@Autowired ThreadPoolTaskScheduler taskScheduler, @Autowired InterfaceService service,
			@Autowired CacheManager cacheManager) throws Exception {
		InterfaceCacheManager manager = new InterfaceCacheManager(
				configurationManager.getInterfaceCacheManagerConfig(),
				taskScheduler, service, cacheManager);

		OldStateCheckHandlerConfig oschc = configurationManager.getChannelManagerConfig()
				.getOldStateCheckHandlerConfig();
		StateChecker sc = new OldStateCheckHandler(oschc);
		StateCheckerMap.map.put("rose.mary.trace.core.helper.checker.OldStateCheckHandler", sc);

		return manager;
	}

	@Bean
	public StateHandler getStateHandler(
		@Autowired CacheManager cacheManager
	) {
		StateHandler handler = new StateHandler(cacheManager.getFinCache());
		return handler;
	}

	/**
	 * <pre>
	 *  on testing ...
	 * 	direct loader 
	 * </pre>
	 * @param configurationManager
	 * @param cacheManager
	 * @param tpm1
	 * @param traceService
	 * @param botService
	 * @return
	 * @throws Exception
	 */
	@Bean 
	public DirectLoaderManager directLoaderManager(
		@Autowired ConfigurationManager configurationManager,
		@Autowired CacheManager cacheManager, 
		@Autowired @Qualifier("tpm1") ThroughputMonitor tpm1,
		@Autowired TraceService traceService,
		@Autowired BotService botService,
		@Autowired StateHandler stateHandler				
	) throws Exception {
		DirectLoaderManager directLoaderManager = new DirectLoaderManager(
			configurationManager.getChannelManagerConfig(), 
			cacheManager, 
			tpm1, 
			traceService, 
			botService, 
			configurationManager.getLoaderManagerConfig(),
			stateHandler
		);
		
		return directLoaderManager;
	}


	@Bean
	public RouteHandler getRouteHandler(
		@Autowired CacheManager cacheManager
	) {
		RouteHandler handler = new RouteHandler(
			cacheManager.getFinCache(),
			cacheManager.getBotCaches(),
			cacheManager.getCloneCaches(),
			cacheManager.getRoutingCache()
		);
		return handler;
	}

	@Bean
	public TraceRouterManager traceRouterManager(
		@Autowired ConfigurationManager configurationManager,
		@Autowired TraceService traceService, 
		@Autowired CacheManager cacheManager,
		@Autowired @Qualifier("tpm2") ThroughputMonitor tpm2,
		@Autowired RouteHandler routeHandler
	) throws Exception {
		return new TraceRouterManager(
			configurationManager.getLoaderManagerConfig(), 
			traceService, 
			cacheManager, 
			tpm2,
				routeHandler
		);
	}

	@Bean
	public ChannelManager channelManager(@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager, @Autowired @Qualifier("tpm1") ThroughputMonitor tpm1,
			@Autowired ThreadPoolTaskExecutor taskExecutor)
			throws Exception {
		return new ChannelManager(configurationManager.getChannelManagerConfig(), cacheManager, tpm1, taskExecutor);
	}

	@Bean
	public LoaderManager loaderManager(@Autowired ConfigurationManager configurationManager,
			@Autowired TraceService traceService, @Autowired CacheManager cacheManager,
			@Autowired @Qualifier("tpm2") ThroughputMonitor tpm2) throws Exception {
		return new LoaderManager(configurationManager.getLoaderManagerConfig(), traceService, cacheManager, tpm2);
	}

	@Bean
	public BoterManager boterManager(@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager, @Autowired @Qualifier("tpm3") ThroughputMonitor tpm3,
			@Autowired StateService stateService)
			throws Exception {
		return new BoterManager(configurationManager.getBoterManagerConfig(), cacheManager, tpm3, stateService);
	}

	@Bean
	public BotLoaderManager botLoaderManager(@Autowired ConfigurationManager configurationManager,
			@Autowired BotService botService, @Autowired CacheManager cacheManager,
			@Autowired @Qualifier("tpm4") ThroughputMonitor tpm4, @Autowired StateService stateService)
			throws Exception {
		return new BotLoaderManager(configurationManager.getBotLoaderManagerConfig(), botService, cacheManager, tpm4,
				stateService);
	}

	@Bean
	public CloneLoaderManager cloneLoaderManager(@Autowired ConfigurationManager configurationManager,
			@Autowired BotService botService, @Autowired CacheManager cacheManager,
			@Autowired @Qualifier("tpm4") ThroughputMonitor tpm4, @Autowired StateService stateService)
			throws Exception {
		return new CloneLoaderManager(configurationManager.getBotLoaderManagerConfig(), botService, cacheManager, tpm4,
				stateService);
	}

	@Bean
	public FinisherManager finisherManager(@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager) throws Exception {
		return new FinisherManager(configurationManager.getFinisherManagerConfig(), cacheManager);
	}

	@Bean
	public TraceErrorHandlerManager traceErrorHandlerManager(@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager, @Autowired TraceService traceService,
			@Autowired MessageSource messageSource) throws Exception {
		return new TraceErrorHandlerManager(configurationManager.getTraceErrorHandlerManagerConfig(), cacheManager,
				traceService, messageSource);
	}

	@Bean
	public BotErrorHandlerManager botErrorHandlerManager(@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager, @Autowired BotService botService,
			@Autowired MessageSource messageSource) throws Exception {
		return new BotErrorHandlerManager(configurationManager.getBotErrorHandlerManagerConfig(), cacheManager,
				botService, messageSource);
	}

	@Bean
	public UnmatchHandlerManager unmatchHandlerManager(@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager, @Autowired BotService botService,
			@Autowired MessageSource messageSource) throws Exception {
		return new UnmatchHandlerManager(configurationManager.getUnmatchHandlerManagerConfig(), cacheManager,
				botService, messageSource);
	}

	@Bean
	public SystemErrorTestManager systemErrorTesterManager(@Autowired ConfigurationManager configurationManager,
			@Autowired SystemService systemService) throws Exception {
		return new SystemErrorTestManager(configurationManager.getConfig(), systemService);
	}

	@Bean
	public PolicyHandlerManager databasePolicyHandlerManager(
			@Autowired ConfigurationManager configurationManager, @Autowired CacheManager cacheManager,
			@Autowired ServerManager serverManager, @Autowired ChannelManager channelManager,
			@Autowired SystemService systemService, @Autowired MessageSource messageSource) throws Exception {

		PolicyHandlerManager dpm = new PolicyHandlerManager(
				configurationManager.getPolicyConfig(), systemService,
				serverManager, channelManager, messageSource, cacheManager);
		dpm.ready();
		return dpm;
	}

	/*
	 * @Bean
	 * public ServerManager serverManager(
	 * 
	 * @Autowired TraceServer server, @Autowired @Qualifier("datasource01")
	 * DataSource datasource01
	 * ) throws Exception {
	 * ServerManager manager = new ServerManager(server, datasource01);
	 * return manager;
	 * }
	 */

	@Bean
	public ServerManager serverManager(
			@Autowired @Qualifier("datasource01") DataSource datasource01,
			@Autowired ChannelManager channelManager,
			@Autowired LoaderManager loaderManager,
			@Autowired BoterManager boterManager,
			@Autowired BotLoaderManager botLoaderManager,
			@Autowired CloneLoaderManager cloneLoaderManager,
			@Autowired FinisherManager finisherManager,
			@Autowired TraceErrorHandlerManager traceErrorHandlerManager,
			@Autowired BotErrorHandlerManager botErrorHandlerManager,
			@Autowired MonitorManager monitorManager,
			@Autowired UnmatchHandlerManager unmatchHandlerManager,
			@Autowired TesterManager testerManager,
			@Autowired ConfigurationManager configurationManager,
			@Autowired SystemErrorTestManager systemErrorTestManager,
			@Autowired CacheManager cacheManager,
			@Autowired ApplicationContext applicationContext,
			@Autowired DirectLoaderManager directLoaderManager,
			@Autowired BotService botService,
			@Autowired TraceRouterManager traceRouterManager
		) throws Exception {
		String name = configurationManager.getServerManagerConfig().getName();
		TraceServer server = new TraceServer(
				name,
				channelManager,
				loaderManager,
				boterManager,
				botLoaderManager,
				finisherManager,
				traceErrorHandlerManager,
				botErrorHandlerManager,
				monitorManager,
				unmatchHandlerManager,
				testerManager,
				configurationManager,
				systemErrorTestManager,
				cacheManager);
		server.ready();
		 
		server.setDirectLoaderManager(directLoaderManager);		 
		server.setTraceRouterManager(traceRouterManager);
		String type = configurationManager.getConfig().getType();
		server.setType(type);


		server.setCloneLoaderManager(cloneLoaderManager);

		ServerManager manager = new ServerManager(server, datasource01, applicationContext, cacheManager);
		

		// 	미완료 트레킹 서머리 캐시 로딩 
		if(configurationManager.getConfig().isLoadNotFinishedState()){ 
			logger.info("get not finised state list ");
			int dur = configurationManager.getConfig().getLoadNotFinishedStateDurationMin();
			String toDate = Util.getFormatedDate(Util.DEFAULT_DATE_FORMAT_MI);
			String fromDate = SystemUtil.getFormatedDate(Util.DEFAULT_DATE_FORMAT_MI, toDate, Calendar.MINUTE, dur);			  
			Map<String, State> states = botService.getNotFinishedStates(fromDate, toDate);		
			cacheManager.getFinCache().put(states);
		
		}
	
		return manager;
	}

	@Bean
	public TesterManager testManager(@Autowired ConfigurationManager cm, @Autowired GenerateTraceMsgService gtms) {
		TesterManager testerManager = new TesterManager(cm, gtms);
		return testerManager;
	}

	@Bean
	public LocaleResolver localeResolver() {
		SessionLocaleResolver localeResolver = new SessionLocaleResolver();
		// localeResolver.setDefaultLocale(Locale.KOREA);
		return localeResolver;
	}

	@Bean(initMethod = "prepare")
	public SystemErrorDetector systemErrorDetector() {
		return new SystemErrorDetector();
	}

	@Bean
	public RecoveryManager recoveryManager(
			@Autowired ConfigurationManager configurationManager,
			@Autowired CacheManager cacheManager,
			@Autowired ServerManager serverManager,
			@Autowired PolicyHandlerManager policyHandlerManager) throws Exception {

		long recoveryTaskDelay = 10000;

		RecoveryManager recoveryManager = new RecoveryManager(
				configurationManager,
				policyHandlerManager,
				cacheManager,
				serverManager,
				recoveryTaskDelay);

		return recoveryManager;
	}

}
