/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.config.LoaderManagerConfig;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.monitor.ThroughputMonitor;
import rose.mary.trace.database.service.TraceService;

import rose.mary.trace.loader.RouteHandler;

import rose.mary.trace.loader.TraceRouter;

/**
 * <pre>
 * rose.mary.trace.manager
 * TraceLoaderManager.java
 * </pre>
 * 
 * @author whoana
 * @date Aug 28, 2019
 */
public class TraceRouterManager {

	Logger logger = LoggerFactory.getLogger("rose.mary.trace.SystemLogger");

	LoaderManagerConfig config = null;

	TraceService traceService;

	List<TraceRouter> routers = new ArrayList<TraceRouter>();

	int threadCount = 1;

	int commitCount = 1000;

	int delayForNoMessage = 100;

	ThroughputMonitor tpm;

	CacheManager cacheManager;

	boolean loadError = true;

	boolean loadContents = true;

	RouteHandler routeHandler;
	
	public TraceRouterManager(LoaderManagerConfig config, TraceService traceService, CacheManager cacheManager,
			ThroughputMonitor tpm,
			RouteHandler routeHandler) {
		this.config = config;
		this.traceService = traceService;
		this.threadCount = config.getThreadCount();
		this.commitCount = config.getCommitCount();
		this.delayForNoMessage = config.getDelayForNoMessage();
		this.tpm = tpm;
		this.cacheManager = cacheManager;
		this.loadError = config.isLoadError();
		this.loadContents = config.isLoadContents();
		this.routeHandler = routeHandler;

	}

	public void ready() throws Exception {
	}

	public void start() throws Exception {
		try {

			List<CacheProxy<String, Trace>> distributeCaches = cacheManager.getDistributeCaches();
			CacheProxy<String, Trace> errorCache = cacheManager.getErrorCache01();

			int idx = 0;
			stop();
			routers = new ArrayList<TraceRouter>();
			logger.info("Routers starting");

			threadCount = config.getThreadCount();
			logger.info("Router thread count per cache:" + threadCount);

			for (CacheProxy<String, Trace> distributeCache : distributeCaches) {

				for (int i = 0; i < threadCount; i++) {

					String name = Util.join(config.getName(), idx);
					TraceRouter router = new TraceRouter(
							name,
							commitCount,
							delayForNoMessage,
							loadError,
							loadContents,
							distributeCache,
							errorCache,
							traceService,
							tpm,
							null,
							routeHandler);

					routers.add(router);
					router.start();
					idx++;
				}
			}
			logger.info("Routers started");
		} catch (Exception e) {
			stop();
			throw e;
		}
	}

	/**
	 * 
	 */
	public void stop() {

		if (routers != null && routers.size() > 0) {
			logger.info("Loaders stopping");

			// int idx = 0;
			// java.util.ConcurrentModificationException 방지를 위해 변경함.
			// for (Loader loader : loaders) {
			// loader.stop();
			// //loaders.remove(loader);
			// logger.info("Loader(" + (idx ++) + ") stop....");
			// }

			Iterator<TraceRouter> iterator = routers.iterator();
			while (iterator.hasNext()) {
				TraceRouter router = iterator.next();
				router.stop();
				iterator.remove();
			}

			logger.info("Routers stopped");
		}
	}

}
