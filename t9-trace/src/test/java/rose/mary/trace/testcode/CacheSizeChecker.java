package rose.mary.trace.testcode;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.manager.CacheManager;
import rose.mary.trace.manager.ConfigurationManager;

public class CacheSizeChecker {
    public static void main(String[] args) {
        CacheManager cacheManager = null;
        try {
            
            ConfigurationManager configurationManager = new ConfigurationManager();
            configurationManager.prepare();
            cacheManager = new CacheManager(configurationManager.getCacheManagerConfig());
            cacheManager.prepare();    
            CacheProxy<String, Trace> dc = cacheManager.getDistributeCaches().get(0);
            CacheProxy<String, Trace> mc = cacheManager.getMergeCache();
            CacheProxy<String, Trace> ec = cacheManager.getErrorCache01();
            System.out.println("dc:" + dc.size());
            System.out.println("mc:" + mc.size());
            System.out.println("ec:" + ec.size());
    
        } catch (Exception e) {
    
            e.printStackTrace();
        }
      }
}
