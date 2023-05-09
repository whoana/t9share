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
 * NoWaitingCleaner
 * 완료된 건들만 삭제 처리한다.
 * </pre>
 * 
 * @author whoana
 * @since 2020.03.30
 */
public class NoWaitingCleaner implements Cleaner {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	private CacheProxy<String, Trace> backupCache;

	private CacheProxy<String, State> finCache;

	private CacheProxy<String, Integer> routingCache;

	int waitForCleaning = 60 * 60 * 1000; // 60분

	public NoWaitingCleaner(int waitForCleaning, CacheProxy<String, Trace> backupCache,
			CacheProxy<String, State> finCache, CacheProxy<String, Integer> routingCache) {
		this.backupCache = backupCache;
		this.finCache = finCache;
		this.routingCache = routingCache;
		this.waitForCleaning = waitForCleaning;
	}

	@Override
	public int clean(long currentTime, State state) throws Exception {
		long elapsed = currentTime - state.getCreateDate();
		int res = 0;
		if (elapsed >= waitForCleaning) {
			finCache.remove(state.getBotId());
			routingCache.remove(state.getBotId());
			res = 1;
		} else {
			if (state.isFinish() && state.isLoaded()) { // 완료건 중 DB 로딩된 건은 바로 삭제
				finCache.remove(state.getBotId());
				routingCache.remove(state.getBotId());
				res = 1;
			}
		}
		return res;

	}

}
