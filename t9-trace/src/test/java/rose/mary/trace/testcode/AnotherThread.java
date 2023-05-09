package rose.mary.trace.testcode;

public class AnotherThread implements Runnable {
    Thread thread = new Thread(this, "another");

    boolean isRunning = false;
    
    WatchThread watcher;

    public AnotherThread(WatchThread watcher){
        this.watcher = watcher;
    }
    
    public void stop() {
        isRunning = false;
        thread.interrupt();
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        isRunning = true;
        while (Thread.currentThread() == thread && isRunning) {
            try {
                Thread.sleep(1000);
                System.out.println("I'm anotherThread. ");
                Thread.sleep(1000);
                System.out.println("I will kill watcher");
                if(watcher != null) watcher.stop(); 
            } catch (InterruptedException e) {
                isRunning = false;
                break;
            }
        }

    }
}
