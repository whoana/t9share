package rose.mary.trace.testcode; 
 
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.Trace;

public class LoaderThread implements Runnable {
    protected final static int DEFAULT_COMMIT_COUNT = 1000;

    private Map<String, Trace> loadItems = new HashMap<String, Trace>(); 
    private long commitLapse = System.currentTimeMillis();
	private long maxCommitWait = 1000; 
    private long delayForNoMessage = 1000;
    private int commitCount = DEFAULT_COMMIT_COUNT;
    private long exceptionDelay = 5000;
    Thread thread;
    
    boolean isRunning = false; 

    CacheProxy<String, Trace> cache1;
    CacheProxy<String, Trace> cache2;
    CacheProxy<String, Trace> errorCache;
    

    public LoaderThread(CacheProxy<String, Trace> cache1, CacheProxy<String, Trace> cache2, CacheProxy<String, Trace> errorCache ){
        this.cache1 = cache1;
        this.cache2 = cache2;
        this.errorCache = errorCache; 
    }

    public void start(){
        isRunning = true;
        thread = new Thread(this, "loader");
        thread.start();
    }

    public void stop(){
        isRunning = false;
        
		if(thread != null) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    }
	
    @Override
    public void run() {
         
        while(Thread.currentThread().equals(thread) && isRunning){ 

                try {
                    if(loadItems.size() > 0 && (System.currentTimeMillis() - commitLapse >= maxCommitWait)) {
                        commit(); 
                    }
                     
                    Collection<Trace> values = cache1.values();
                    if(values == null || values.size() == 0)  {
                        try {
                            Thread.sleep(delayForNoMessage); 
                            continue;
                        }catch(java.lang.InterruptedException ie) {
                            isRunning = false;
                            break;
                        }
                    }
                     
                     
                    for (Trace trace : values) {
                        String key = trace.getId(); 
                        
                        
                        if(cache2.containsKey(key)) {
                            //delete the trace loaded already.
                            cache1.remove(key);
                        }
                        
                        loadItems.put(key, trace);
                         
                        if(loadItems.size() > 0 && (loadItems.size() % commitCount == 0 )) {
                            
                            try {
                                commit();
                                break;
                            } catch (Exception e) {
                                e.printStackTrace();
                                 
                                try {
                                    Thread.sleep(exceptionDelay);
                                } catch (InterruptedException e1) { 
                                    isRunning = false;
                                    return;
                                }
                                
                                break;
                            } 
                        }
                    }
                    
                     
                
                  
                } catch (Exception e) { 
                     
                      
                    e.printStackTrace();
                    try {
                        Thread.sleep(exceptionDelay);
                    } catch (InterruptedException e1) { 
                        isRunning = false;
                        break;
                    }
                    
                }
            }
         
         
            try {
                commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
    
            isRunning = false; 
            System.out.println("stop loader");
        
    }

    private void commit() throws Exception {

        try { 
			Collection<Trace> collection = loadItems.values();
			traceLoadServiceLoad(collection);
            cache2.put(loadItems);								 
			cache1.removeAll(loadItems.keySet());			
			loadItems.clear();			
		}catch(Exception e) { 
			if(errorCache != null) errorCache.put(loadItems);		
			cache1.removeAll(loadItems.keySet());
			loadItems.clear();
			throw e;		 
		}finally {
			commitLapse = System.currentTimeMillis();	
			//if(tm != null) tm.commit();
		}


    }

    int totalLoadCount = 0;
    private void traceLoadServiceLoad(Collection<Trace> collection) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int loadCount = collection.size();
        totalLoadCount = totalLoadCount + loadCount;
        System.out.println("load traces:" + loadCount + "(total:" + totalLoadCount + ")");
        
    }

 

}
