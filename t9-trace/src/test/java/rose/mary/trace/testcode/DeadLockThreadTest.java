package rose.mary.trace.testcode;

public class DeadLockThreadTest {
    
    WatchThread watcher = null;  
    
    public void setWatcher(WatchThread watcher){
        this.watcher = watcher;
    }

    public void stopWatcher(){
        watcher.stop();
    }

     public static void main(String[] args) {
        
         DeadLockThreadTest dtt = new DeadLockThreadTest();
         WatchThread watcher = new WatchThread(dtt);
         dtt.setWatcher(watcher);
 
 
        watcher.start();
     }

}
