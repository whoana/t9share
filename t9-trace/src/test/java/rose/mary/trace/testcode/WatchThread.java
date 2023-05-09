package rose.mary.trace.testcode;

public class WatchThread implements Runnable {

    DeadLockThreadTest another;

    Thread thread = new Thread(this, "watcher");

    boolean isRunning = false;


    public WatchThread(DeadLockThreadTest another){
        this.another = another;
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
                System.out.println("I'm watcher!, I will stop by anotherThread.");
                another.stopWatcher();//deadlock
                isRunning = false;
            } catch (InterruptedException e) {
                isRunning = false;
                break;
            }
        }
    }

}
