/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.boter;

import java.util.Collection;
import java.util.List;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.config.BoterManagerConfig;
import rose.mary.trace.core.data.common.Bot;
import rose.mary.trace.core.data.common.InterfaceInfo;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.StateEvent;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.database.FromDatabase;
import rose.mary.trace.core.envs.Variables;
import rose.mary.trace.core.exception.ExceptionHandler;
import rose.mary.trace.core.helper.checker.StateCheckerMap;
import rose.mary.trace.core.monitor.ThroughputMonitor;
import rose.mary.trace.core.util.IntCounter;

/**
 * <pre>
 * The Boter creates and updates {@link Bot Bot}.
 * </pre>
 * 
 * @author whoana
 * @date Sep 18, 2019
 */
public class Boter implements Runnable {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	final static int EXISTS_CHECK_CACHE = 0;
	final static int EXISTS_CHECK_DB = 2;

	final static String NOT_MATCH_NM = "unregistered";

	protected final static int DEFAULT_COMMIT_COUNT = 1000;

	private boolean isShutdown = true;

	private Thread thread = null;

	private ExceptionHandler exceptionHandler;

	private CacheProxy<String, Trace> mergeCache;

	private CacheProxy<String, State> errorCache;

	private CacheProxy<String, Trace> backupCache;

	private CacheProxy<String, State> finCache;

	// private int existsCheckType = EXISTS_CHECK_CACHE; // 0:CacheProxy<String,
	// State> stateCache, 1 : select from database

	List<CacheProxy<String, StateEvent>> botCaches;

	CacheProxy<String, InterfaceInfo> interfaceCache;

	CacheProxy<String, Integer> routingCache;

	private ThroughputMonitor tpm;

	private long delayForNoMessage = 10;

	private long exceptionDelay = 5000;

	private int maxRoutingCacheSize = 10000;

	IntCounter counter;

	BoterManagerConfig config;

	String name;

	FromDatabase fromDatabase;

	public Boter(
			BoterManagerConfig config,
			CacheProxy<String, Trace> mergeCache,
			CacheProxy<String, Trace> backupCache,
			List<CacheProxy<String, StateEvent>> botCaches,
			CacheProxy<String, State> finCache,
			CacheProxy<String, InterfaceInfo> interfaceCache,
			CacheProxy<String, State> errorCache,
			CacheProxy<String, Integer> routingCache,
			ThroughputMonitor tpm,
			ExceptionHandler exceptionHandler) {
		this.name = config.getName();
		this.config = config;
		this.mergeCache = mergeCache;
		this.backupCache = backupCache;
		this.botCaches = botCaches;
		this.finCache = finCache;
		this.interfaceCache = interfaceCache;
		this.errorCache = errorCache;
		this.routingCache = routingCache;
		this.tpm = tpm;
		this.exceptionHandler = exceptionHandler;
		this.delayForNoMessage = config.getDelayForNoMessage();
		this.maxRoutingCacheSize = config.getMaxRoutingCacheSize();
		counter = new IntCounter(0, botCaches.size() - 1, 1);
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
		/*
		 * if (Variables.startStopAsap) {
		 * stopAsap();
		 * } else {
		 * stopGracefully();
		 * }
		 */
		isShutdown = true;
		if (thread != null) {
			thread.interrupt();
		}
	}

	public void run() {
		if (Variables.startStopAsap) {
			runAsap();
		} else {
			runGracefully();
		}
	}

	public void stopGracefully() {
		isShutdown = true;
		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				logger.error("", e);
			}
		}
	}

	public void stopAsap() {
		isShutdown = true;
		if (thread != null) {
			thread.interrupt();
		}
	}

	/**
	 * @deprecated since 202209
	 */
	public void runAsap() {

		logger.info(Util.join("start boter:[" + name + "]"));

		while (true) {

			try {

				if (thread.isInterrupted()) {
					break;
				}

				Collection<Trace> values = mergeCache.values();
				if (values == null || values.size() == 0) {
					try {
						Thread.sleep(delayForNoMessage);
						continue;
					} catch (java.lang.InterruptedException ie) {
						isShutdown = true;
						break;
					}
				}

				for (Trace trace : values) {
					if (trace == null)
						continue;
					String key = trace.getId();
					try {
						String botId = Util.join(trace.getIntegrationId(), "@", trace.getDate(), "@",
								trace.getOriginHostId());
						State state = finCache.get(botId);
						boolean first = false;

						// final cache 에서 이전 트레킹 상태가 존재하지 않으면 최초 상태를 생성한다.
						if (state == null) {
							long currentDate = System.currentTimeMillis();
							state = new State();
							state.setCreateDate(currentDate);
							state.setBotId(botId);
							first = true;
						}

						// ----------------------------------------------------------------
						// StateCheckHandler 는 Trace 정보를 이용하여 State 정보를 업데이트한다.
						// 기존 MTE 포멧을 이용하여 State 값을 만들기 위해 아래 인터페이스 구현제를 사용한다.
						// rose.mary.trace.apps.handler.OldStateCheckHandler
						// checkAndSet 내에서 처리되는 주요 내용은
						// 트레킹 단계별 상태값, 에러 정보 업데이트 들이다.
						// 더 자세한 내용을 아래 인터페이스 구현체를 참고
						// rose.mary.trace.apps.handler.OldStateCheckHandler
						// ----------------------------------------------------------------

						// CheckHandler 는 Trace 를 통해 전달하지 않도록 수정한다. 20220801.
						// TODO: Boter 생성 시점에 구체적인 CheckHandler 지정하여 생성되도록 소스 확장 변경 필요 .
						// trace.getStateCheckHandler().checkAndSet(first, trace, state);
						StateCheckerMap.map.get(trace.getStateCheckHandlerId()).checkAndSet(first, trace, state);

						// ----------------------------------------------------------------
						// 현재 상태 단계가 BRKR, REPL 단계이면 트레킹 상태값의 디비적재를 처리하지 않도록
						// skip 값이 false 일때만 finalCache 에 상태값을 등록하고
						// 동시에 디비로드를 위한 bot cache 로 라우팅한다.
						// ----------------------------------------------------------------
						if (!state.skip()) {
							finCache.put(botId, state);
							// --------------------------------------------------------------
							// 트레킹 상태값을 디비로 로드하기위해 botCache 에 상택값 적재
							// botCache 내의 state 값들은 BotLoader 에 듸해 데이터베이스에 적재된다.
							// --------------------------------------------------------------
							routeToBotCache(botId, state);
						}

					} catch (Exception e) {
						if (exceptionHandler != null) {
							exceptionHandler.handle("", e);
						} else {
							logger.error("", e);
						}
					} finally {
						// mergeCache에서 빼내야 다음걸 처리한다.
						mergeCache.remove(key);
					}

				}

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

		isShutdown = true;
		logger.info(Util.join("stop boter:[" + name + "]"));

	}

	public void runGracefully() {

		logger.info(Util.join("start boter:[" + name + "]"));

		while (Thread.currentThread() == thread && !isShutdown) {

			try {
				Collection<Trace> values = mergeCache.values();
				int processCnt = values == null ? 0 : values.size();
				if (processCnt == 0) {
					try {
						Thread.sleep(delayForNoMessage);
						continue;
					} catch (java.lang.InterruptedException ie) {
						isShutdown = true;
						break;
					}
				}

				for (Trace trace : values) {
					if (trace == null)
						continue;
					String key = trace.getId();
					try {

						String botId = Util.join(
											trace.getIntegrationId(), 
											"@", 
											trace.getDate(), 
											"@",
											trace.getOriginHostId());

						State state = finCache.get(botId);
						
						//첫 발생건은 무조건 DB. 검색을 하게 되므로 다른 방식을 검토해보자
						// if (state == null && fromDatabase != null) {
						// 	state = fromDatabase.getState(trace.getIntegrationId(), trace.getDate(),
						// 			trace.getOriginHostId());
						// }
						// if(state == null) {
						// 	if(historyCache.get(botId) != null) {
						// 		state = fromDatabase.getState(trace.getIntegrationId(), trace.getDate(),trace.getOriginHostId());
						// 	}
						// }

						boolean first = false;
						if (state == null) {
							long currentDate = System.currentTimeMillis();
							state = new State();
							state.setCreateDate(currentDate);
							state.setBotId(botId);
							first = true;
							//historyCache.put(botId, System.currentTimeMillis() - currentDate);
						}

						// ----------------------------------------------------------------
						// StateCheckHandler 는 Trace 정보를 이용하여 State 정보를 업데이트한다.
						// 기존 MTE 포멧을 이용하여 State 값을 만들기 위해 아래 인터페이스 구현제를 사용한다.
						// rose.mary.trace.apps.handler.OldStateCheckHandler
						// checkAndSet 내에서 처리되는 주요 내용은
						// 트레킹 단계별 상태값, 에러 정보 업데이트 들이다.
						// 더 자세한 내용을 아래 인터페이스 구현체를 참고
						// rose.mary.trace.apps.handler.OldStateCheckHandler
						// ----------------------------------------------------------------

						// CheckHandler 는 Trace 를 통해 전달하지 않도록 수정한다. 20220801.
						// TODO: Boter 생성 시점에 구체적인 CheckHandler 지정하여 생성되도록 소스 확장 변경 필요 .
						// trace.getStateCheckHandler().checkAndSet(first, trace, state);
						StateCheckerMap.map.get(trace.getStateCheckHandlerId()).checkAndSet(first, trace, state);

						if (!state.skip()) {
							state.setLoaded(false); // 새로운 TRACE 발생되어 디비에 반영 전이므로 loaded 를 false 로 초기화해준다.
													// 디비로드후 삭제대상이 되도록 loaded 를 true 로 변경하는 시점은 botLoader 가 디비 반영후 상태를
													// 변경한다.
							if (Variables.debugLineByLine)
								logger.debug(name + "-BTLBLD0100");
							finCache.put(botId, state);
							if (Variables.debugLineByLine)
								logger.debug(name + "-BTLBLD0101");
							// -----------------------------------------------
							// date 2022.08.25
							// -----------------------------------------------
							// copy reference 에서 copy value 로 변경
							// 변경 사유 : 대량 데이터 발생시 db 배치 작업 전에 상태값이 변경되면 copy reference 상황에서는 최종 상태만 유지하게되므로
							// 동일 상태값이 insert 및 update 됨
							// Trace 가 들어온 순서데로 상태값을 DB 기록하기 위해 copy value 모델로 로직 변경한다.
							// routeToBotCache(botId, state);

							// StateEvent 모델로 변경하므로 copy reference 로 변경
							// routeToBotCache(botId, SerializationUtils.clone(state));
							routeToBotCache(botId, state);
							if (Variables.debugLineByLine)
								logger.debug(name + "-BTLBLD0102");

						}
						// 20220830
						// main block 내로 옮긴다.
						// 사유 : F.C, B.C 로 옮기지 못했는데 삭제되면 메시지가 유실되므로 finally 에서 try 구분 내로 이동함.
						mergeCache.remove(key);
						if (Variables.debugLineByLine)
							logger.debug(name + "-BTLBLD0103");

					} catch (Exception e) {
						if (exceptionHandler != null) {
							exceptionHandler.handle("", e);
						} else {
							logger.error("", e);
						}
					} finally {
						// mergeCache에서 빼내야 다음걸 처리한다.
						// 20220830
						// main block 내로 옮긴다.
						// 사유 : F.C, B.C 로 옮기지 못했는데 삭제되면 메시지가 유실되므로 finally 에서 try 구분 내로 이동함.
						// mergeCache.remove(key);
					}

				}
				if (tpm != null && processCnt > 0) {
					tpm.count(processCnt);
				}
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

		isShutdown = true;
		logger.info(Util.join("stop boter:[" + name + "]"));

	}

	public FromDatabase getFromDatabase() {
		return fromDatabase;
	}

	public void setFromDatabase(FromDatabase fromDatabase) {
		this.fromDatabase = fromDatabase;
	}

	private void routeToBotCache(String botId, State state) throws Exception {
		Integer index = getBotCacheIndex(botId);

		CacheProxy<String, StateEvent> botCache = botCaches.get(index);

		// 2022.08.23 dup 에러가 발생됨. merge 문을 사용하였음에도 발생.
		// 동일 배치처리 SQL 블럭에 동일 건이 포함되면 merge 문에서도 에러가 발생되지 않나 싶다.
		// 키값으로 uniqId 대신에 state.getBotId() 를 사용하는 것은 어떨까?
		// String uniqId = state.getBotId();
		String uniqId = UUID.randomUUID().toString();
		StateEvent se = new StateEvent();
		se.setId(uniqId);
		se.setBotId(state.getBotId());
		botCache.put(uniqId, se);

		// logger.debug("put botCache[" + index + "][" + state.getBotId() + ", status:"
		// + state.getStatus() + ", created:"
		// + state.getCreateDate() + "]:" + Util.toJSONString(state));
	}

	private Integer getBotCacheIndex(String botId) throws Exception {
		Integer index = routingCache.get(botId);
		if (index == null) {
			index = counter.getAndIncrease();
			routingCache.put(botId, index);
		}
		// if(routingCache.size() >= maxRoutingCacheSize) routingCache.clear();
		return index;
	}

	public ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	public void setExceptionHandler(ExceptionHandler loaderExceptionHandler) {
		this.exceptionHandler = loaderExceptionHandler;
	}

}
