/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.loader;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pep.per.mint.common.util.Util;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.Trace;

import rose.mary.trace.core.exception.ExceptionHandler;
import rose.mary.trace.core.monitor.ThroughputMonitor;
import rose.mary.trace.database.service.TraceService;

/**
 * <pre>
 * Trace 디비 로딩 후 라우팅 처리 버전 (스레드 경합 최소화를 위한 작업)
 * 1.캐시에 쌓인 Trace 를 디비에 로딩 
 * 2.botId 기준 라우팅, 앞서 작성했던 StateHandler.handleState 참고하여 finCache 에 최종 상태를 등록하고 botCache 에 이벤트를 라우팅 한다. 
 * TraceRouter.java
 * </pre>
 * 
 * @author whoana
 * @date Aug 26, 2019
 */
public class TraceRouter implements Runnable {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final static int DEFAULT_COMMIT_COUNT = 1000;

	private boolean isShutdown = true;

	private TraceService traceLoadService;

	private Thread thread = null;

	private ExceptionHandler exceptionHandler;

	private CacheProxy<String, Trace> distributeCache;

	private CacheProxy<String, Trace> errorCache;

	private ThroughputMonitor tpm;

	private int commitCount = DEFAULT_COMMIT_COUNT;

	private long delayForNoMessage = 1000;

	private long exceptionDelay = 5000;

	private boolean loadError = true;

	private boolean loadContents = true;

	private Map<String, Trace> loadItems = new LinkedHashMap<String, Trace>();

	private long commitLapse = System.currentTimeMillis();

	private long maxCommitWait = 1000;

	private RouteHandler routeHandler;

	String name;

	/**
	 * 
	 * @param name
	 * @param commitCount
	 * @param delayForNoMessage
	 * @param loadError
	 * @param loadContents
	 * @param distributeCache
	 * @param errorCache
	 * @param traceLoadService
	 * @param tpm
	 * @param exceptionHandler
	 */
	public TraceRouter(
			String name,
			int commitCount,
			long delayForNoMessage,
			boolean loadError,
			boolean loadContents,
			CacheProxy<String, Trace> distributeCache,
			CacheProxy<String, Trace> errorCache,
			TraceService traceLoadService,
			ThroughputMonitor tpm,
			ExceptionHandler exceptionHandler,
			RouteHandler routeHandler) {
		this.name = name;
		this.commitCount = commitCount;
		this.distributeCache = distributeCache;
		this.traceLoadService = traceLoadService;
		this.tpm = tpm;
		this.exceptionHandler = exceptionHandler;
		this.delayForNoMessage = delayForNoMessage;
		this.loadError = loadError;
		this.loadContents = loadContents;
		this.errorCache = errorCache;
		this.routeHandler = routeHandler;
	}

	/**
	 * <pre>
	 * 	Database 관련 예외 발생시 에러큐로 빼고 채널을 종료 시킬 방안을 생각해 보자
	 * </pre>
	 * 
	 * @throws Exception
	 */
	public void commit() throws Exception {
		try {
			Collection<Trace> collection = loadItems.values();
			traceLoadService.load(collection, loadError, loadContents);

			if (tpm != null)
				tpm.count(loadItems.size());

			distributeCache.removeAll(loadItems.keySet());

		} catch (Exception e) {
			// ----------------------------------------------------
			// 20220905
			// 예외 발생시 에러캐시로 옮기고 D.C데이터 삭제하는 부분에 대해서는
			// 수정이 필요한지 고민해볼 부분이 있따.
			// 에러큐로 빼지 않고 그대로 놔두고 시스템 종료, 문제해결, 재기동 후
			// 에러큐에 넣지 안아도 D.C 에 있는 것은 재처리 되므로....
			//
			// 20221110
			// 에러발생된건이 재처리 될수 없는 건이라면, 이를테면 데이터 길이 오류등
			// 테이블에 입력될 수 없는 겅우라면
			// 로그 확인 후 캐시를 지우고 재기동하는 것이 합리적인 처리 정책인건지 고민해 볼것
			// ----------------------------------------------------
			if (errorCache != null) {
				errorCache.put(loadItems);
			}
			distributeCache.removeAll(loadItems.keySet());
			// ----------------------------------------------------

			// 20221111
			// errorCache를 이용하는 것으로 일단 수정하자.
			// logger.error("Loader commit Exception", e);
			// throw e;
		} finally {
			loadItems.clear();
			commitLapse = System.currentTimeMillis();
		}
	}

	public void rollback() {

	}

	public void setThroughputMonitor(ThroughputMonitor tpm) {
		this.tpm = tpm;
	}

	public ThroughputMonitor getThroughputMonitor() {
		return tpm;
	}

	public void start() throws Exception {
		if (thread != null)
			stop();
		thread = new Thread(this, name);
		isShutdown = false;
		thread.start();
	}

	public void stop() {
		stopGracefully();
	}

	public void run() {
		runGracefully();
	}

	public void stopGracefully() {
		isShutdown = true;
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				logger.error("", e);
			}
		}
	}

	public void runGracefully() {

		logger.info(Util.join("start TraceRouter:[", name, "]"));

		while (Thread.currentThread() == thread && !isShutdown) {

			try {

				Set<String> keys = null;
				if (distributeCache.isAccessable()) {
					keys = distributeCache.keys();
				}
				if (keys == null || keys.size() == 0) {
					try {
						Thread.sleep(delayForNoMessage);
						continue;
					} catch (java.lang.InterruptedException ie) {
						isShutdown = true;
						break;
					}
				}

				String regDate = Util.getFormatedDate("yyyyMMddHHmmssSSS");

				for (String key : keys) {
					Trace trace = distributeCache.get(key);

					trace.setRegDate(regDate);

					loadItems.put(key, trace);

					// routeHandler.handleStateByClone(trace);
					routeHandler.handleState(trace);

					if (loadItems.size() >= commitCount) {
						break;
					}
 
				}
				commit();

			} catch (Exception e) {

				if (exceptionHandler != null) {
					exceptionHandler.handle("", e);
				} else {
					logger.error("", e);
				}

				try {
					Thread.sleep(exceptionDelay);
				} catch (InterruptedException e1) {
					isShutdown = true;
					break;
				}

			}
		}

		try {
			commit();
		} catch (Exception e) {
			if (exceptionHandler != null) {
				exceptionHandler.handle("", e);
			} else {
				logger.error("", e);
			}
		}

		isShutdown = true;
		logger.info(Util.join("stop TraceRouter:[", name, "]"));
	}

	/*
	 * public void runGracefully() {
	 * 
	 * logger.info(Util.join("start TraceRouter:[", name, "]"));
	 * 
	 * while (Thread.currentThread() == thread && !isShutdown) {
	 * 
	 * try {
	 * if (loadItems.size() > 0 && (System.currentTimeMillis() - commitLapse >=
	 * maxCommitWait)) {
	 * 
	 * commit();
	 * }
	 * 
	 * Collection<Trace> values = distributeCache.values();
	 * if (values == null || values.size() == 0) {
	 * try {
	 * Thread.sleep(delayForNoMessage);
	 * continue;
	 * } catch (java.lang.InterruptedException ie) {
	 * isShutdown = true;
	 * break;
	 * }
	 * }
	 * 
	 * String regDate = Util.getFormatedDate("yyyyMMddHHmmssSSS");
	 * for (Trace trace : values) {
	 * String key = trace.getId();
	 * trace.setRegDate(regDate);
	 * 
	 * loadItems.put(key, trace);
	 * 
	 * routeHandler.handleStateByClone(trace);
	 * 
	 * if (loadItems.size() > 0 && (loadItems.size() % commitCount == 0)) {
	 * 
	 * try {
	 * commit();
	 * break;
	 * } catch (Exception e) {
	 * 
	 * if (exceptionHandler != null) {
	 * exceptionHandler.handle("", e);
	 * } else {
	 * logger.error("", e);
	 * }
	 * 
	 * try {
	 * Thread.sleep(exceptionDelay);
	 * } catch (InterruptedException e1) {
	 * isShutdown = true;
	 * return;
	 * }
	 * 
	 * break;
	 * }
	 * }
	 * }
	 * 
	 * } catch (Exception e) {
	 * 
	 * if (exceptionHandler != null) {
	 * exceptionHandler.handle("", e);
	 * } else {
	 * logger.error("", e);
	 * }
	 * 
	 * try {
	 * Thread.sleep(exceptionDelay);
	 * } catch (InterruptedException e1) {
	 * isShutdown = true;
	 * break;
	 * }
	 * 
	 * }
	 * }
	 * 
	 * try {
	 * commit();
	 * } catch (Exception e) {
	 * if (exceptionHandler != null) {
	 * exceptionHandler.handle("", e);
	 * } else {
	 * logger.error("", e);
	 * }
	 * }
	 * 
	 * isShutdown = true;
	 * logger.info(Util.join("stop TraceRouter:[", name, "]"));
	 * }
	 */
	public ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	public void setExceptionHandler(ExceptionHandler loaderExceptionHandler) {
		this.exceptionHandler = loaderExceptionHandler;
	}

	/**
	 * @return the commitCount
	 */
	public int getCommitCount() {
		return commitCount;
	}

	/**
	 * @param commitCount the commitCount to set
	 */
	public void setCommitCount(int commitCount) {
		this.commitCount = commitCount;
	}

}
