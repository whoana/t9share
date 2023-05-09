package rose.mary.trace.testcode;

import java.util.UUID;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.Trace;
import rose.mary.trace.manager.CacheManager;
import rose.mary.trace.manager.ConfigurationManager;

public class KillTest {

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


        //put data 
        int num = 100000;
        for(int i = 0 ; i < num ; i ++){
            Trace trace = new Trace();
            trace.setId(UUID.randomUUID().toString());
            trace.setStatus("00");
            dc.put(trace.getId(), trace); 
        }
        System.out.println("channel put msg :" + num);

        System.out.println("loader starting....");
        LoaderThread loader = new LoaderThread(dc, mc, ec);
        loader.start();

        Thread.sleep(5000);

        loader.stop();
        
        // System.out.println("channing starting....");
        // ChannelThread channel = new ChannelThread(dc, 1000, 10000);
        // channel.start();


    } catch (Exception e) {

        e.printStackTrace();
    }
  }
}
