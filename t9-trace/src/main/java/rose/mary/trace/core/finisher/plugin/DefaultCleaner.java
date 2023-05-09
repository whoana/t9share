/**
 * Copyright 2020 t9.whoami.com All Rights Reserved.
 */
package rose.mary.trace.core.finisher.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.Trace;

/**
 * <pre>
 * DefaultCleaner
 * 설정된 시간 보다 오래된 건들 중 완료된 건들만 삭제 처리한다.
 * </pre>
 * 
 * @author whoana
 * @since 2020.03.30
 */
public class DefaultCleaner implements Cleaner {

	Logger logger = LoggerFactory.getLogger(getClass());

	private CacheProxy<String, Trace> backupCache;

	private CacheProxy<String, State> finCache;

	private CacheProxy<String, Integer> routingCache;

	int waitForCleaning = 60 * 60 * 1000; // 60분

	int waitForFinishedCleaning = 60 * 10 * 1000;// 10분

	

	public DefaultCleaner(
			int waitForCleaning,
			int waitForFinishedCleaning,
			CacheProxy<String, Trace> backupCache,
			CacheProxy<String, State> finCache,
			CacheProxy<String, Integer> routingCache) {
		this.waitForCleaning = waitForCleaning;
		this.waitForFinishedCleaning = waitForFinishedCleaning;
		this.backupCache = backupCache;
		this.finCache = finCache;
		this.routingCache = routingCache;
		
	}

	@Override
	public int clean(long currentTime, State state) throws Exception {
		// ----------------------------------------------------------
		// 완료건 중에 삭제시간이 도래된 것만 삭제
		// 완료되지 않은 건들 중 청소시간을 넘긴 것들 삭제 처리
		// ----------------------------------------------------------
		long elapsed = currentTime - state.getCreateDate();
		boolean check1 = state.isFinish() && state.isLoaded() && elapsed >= waitForFinishedCleaning;
		boolean check2 = (elapsed >= waitForCleaning);
		if (check1 || check2) {
			String botId = state.getBotId();
			logger.debug("delete botId[" + botId + "]check1:" + check1 + ", check2:" + check2);
			finCache.remove(state.getBotId()); // 예외 발생됨,  ISPN000299: Unable to acquire lock after 10 seconds for key ONLINE_473@20221114040332974145@HOST_SEND and requestor CommandInvocation:<local>:1603169. Lock is held by CommandInvocation:<local>:1603114
			routingCache.remove(state.getBotId());
			return 1;
		} else {
			return 0;
		}
	}

}
