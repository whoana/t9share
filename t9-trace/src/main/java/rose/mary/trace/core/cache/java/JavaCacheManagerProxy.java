package rose.mary.trace.core.cache.java;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
 
import rose.mary.trace.core.cache.CacheManagerProxy;
import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.config.CacheManagerConfig;
import rose.mary.trace.core.data.cache.CacheConfig;
 
import rose.mary.trace.core.data.common.InterfaceInfo;
import rose.mary.trace.core.data.common.State;
import rose.mary.trace.core.data.common.StateEvent;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.core.data.common.Unmatch;
import rose.mary.trace.core.exception.SystemError;


public class JavaCacheManagerProxy extends CacheManagerProxy{

    public JavaCacheManagerProxy(CacheManagerConfig config) {
        super(config);
    }

    @Override
    public void initialize() throws Exception {
        
        // ----------------------------------------------------------------
		// distributeCache setting
		// ----------------------------------------------------------------
		List<CacheConfig> distributeCacheConfigs = config.getDistributeCacheConfigs();
		for (CacheConfig distributeCacheConfig : distributeCacheConfigs) {
			CacheProxy<String, Trace> cache = new JavaCacheProxy<String, Trace>(distributeCacheConfig.getName(), new ConcurrentHashMap<String, Trace>());
			distributeCaches.add(cache);
		}

        // ----------------------------------------------------------------
		// mergeCache setting
		// ----------------------------------------------------------------
		CacheConfig mergeCacheConfig = config.getMergeCacheConfig();
        mergeCache = new JavaCacheProxy<String, Trace>(mergeCacheConfig.getName(), new ConcurrentHashMap<String, Trace>());


		// ----------------------------------------------------------------
		// backupCache setting
		// ----------------------------------------------------------------
		CacheConfig backupCacheConfig = config.getBackupCacheConfig();
        backupCache = new JavaCacheProxy<String, Trace>(backupCacheConfig.getName(), new ConcurrentHashMap<String, Trace>());

        // ----------------------------------------------------------------
		// botCacheConfigs setting
		// ----------------------------------------------------------------
		List<CacheConfig> botCacheConfigs = config.getBotCacheConfigs();
        for (CacheConfig botCacheConfig : botCacheConfigs) {
			JavaCacheProxy<String, StateEvent> cache = new JavaCacheProxy<String, StateEvent>(botCacheConfig.getName(), new ConcurrentHashMap<String, StateEvent>());
			botCaches.add(cache);
		}

		// ----------------------------------------------------------------
		// cloneCacheConfigs  setting
		// ----------------------------------------------------------------
		List<CacheConfig> cloneCacheConfigs = config.getCloneCacheConfigs();
        for (CacheConfig cloneCacheConfig : cloneCacheConfigs) {
			JavaCacheProxy<String, State> cache = new JavaCacheProxy<String, State>(cloneCacheConfig.getName(), new ConcurrentHashMap<String, State>());
			cloneCaches.add(cache);
		}

        // ----------------------------------------------------------------
		// finCacheConfig setting
		// ----------------------------------------------------------------
		CacheConfig finCacheConfig = config.getFinCacheConfig();
		finCache = new JavaCacheProxy<String, State>(finCacheConfig.getName(), new ConcurrentHashMap<String, State>());

		CacheConfig routingCacheConfig = config.getRoutingCacheConfig();
		routingCache = new JavaCacheProxy<String, Integer>(routingCacheConfig.getName(), new ConcurrentHashMap<String, Integer>());

		// ----------------------------------------------------------------
		// errorCache01 setting
		// ----------------------------------------------------------------
		CacheConfig errorCache01Config = config.getErrorCache01Config();
		errorCache01 = new JavaCacheProxy<String, Trace>(errorCache01Config.getName(), new ConcurrentHashMap<String, Trace>());

		// ----------------------------------------------------------------
		// errorCache02 setting
		// ----------------------------------------------------------------
		CacheConfig errorCache02Config = config.getErrorCache02Config();
		errorCache02 = new JavaCacheProxy<String, State>(errorCache02Config.getName(), new ConcurrentHashMap<String, State>());

		// ----------------------------------------------------------------
		// testCache setting
		// ----------------------------------------------------------------
		CacheConfig testCacheConfig = config.getTestCacheConfig();
		if (testCacheConfig != null)
			testCache = new JavaCacheProxy<String, Trace>(testCacheConfig.getName(), new ConcurrentHashMap<String, Trace>());

		// ----------------------------------------------------------------
		// interfaceCache setting
		// ----------------------------------------------------------------
		CacheConfig interfaceCacheConfig = config.getInterfaceCacheConfig();
		if (interfaceCacheConfig != null)
			interfaceCache = new JavaCacheProxy<String, InterfaceInfo>(interfaceCacheConfig.getName(), new ConcurrentHashMap<String, InterfaceInfo>());

		// ----------------------------------------------------------------
		// unmatchCache setting
		// ----------------------------------------------------------------
		CacheConfig unmatchCacheConfig = config.getUnmatchCacheConfig();
		if (unmatchCacheConfig != null)
			unmatchCache = new JavaCacheProxy<String, Unmatch>(unmatchCacheConfig.getName(), new ConcurrentHashMap<String, Unmatch>());

		// ----------------------------------------------------------------
		// systemErrorCache setting
		// ----------------------------------------------------------------
		CacheConfig systemErrorCacheConfig = config.getSystemErrorCacheConfig();
		if (systemErrorCacheConfig != null)
			systemErrorCache = new JavaCacheProxy<String, SystemError>(systemErrorCacheConfig.getName(), new ConcurrentHashMap<String, SystemError>());
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }
    
}
