package rose.mary.trace.testcode;

import java.util.UUID;

import rose.mary.trace.core.cache.CacheProxy;
import rose.mary.trace.core.data.common.Trace;

public class ChannelThread implements Runnable {
    
    Thread thread;
    
    boolean isRunning = false;

    long delay = 1000;

    CacheProxy<String, Trace> cache;

    int lengthOfMsg = 100000;

    public ChannelThread(CacheProxy<String, Trace> cache, long delay, int lengthOfMsg){
        this.cache = cache;
        this.delay = delay;
        this.lengthOfMsg = lengthOfMsg;
    }

    public void start(){
        isRunning = true;
        thread = new Thread(this, "channel");
        thread.start();
    }

    public void stop(){
        isRunning = false;
        thread.interrupt();
    }

    @Override
    public void run() {
        int currentLength = 0;
        while(Thread.currentThread().equals(thread) && isRunning){
            
            if(currentLength >= lengthOfMsg) {
                try {
                    System.out.println("length of trace msg from channel reached max:");
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    if(!isRunning) {
                        System.out.println("channel die...");
                        break;
                    }
                }
            }

            try {
                Trace msg = getMessage();
                cache.put(msg.getId(), msg);
                currentLength ++;
            } catch (Exception e) {
                e.printStackTrace();
            } 

            

        }
        System.out.println("channel stop...");
        
    }


    private Trace getMessage() throws InterruptedException {
        Trace trace = new Trace();
        trace.setId(UUID.randomUUID().toString());
        trace.setStatus("00");
        Thread.sleep(1);
        return trace;
    }


}
