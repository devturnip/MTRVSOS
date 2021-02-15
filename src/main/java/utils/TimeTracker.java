package utils;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeTracker {
    private static TimeTracker timeTrackerInstance = new TimeTracker();
    private TimeTracker(){}
    private long started=0;
    private static Logger LOGGER = LoggerFactory.getLogger(TimeTracker.class);

    public static TimeTracker getTimeTrackerInstance() {return timeTrackerInstance;}

    public void startClock(int duration) {
        started = System.currentTimeMillis()/1000;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        long currentTime = System.currentTimeMillis() / 1000;
                        LOGGER.info("Duration ran: " + (currentTime-started) + " seconds.");
                        if (currentTime - started > duration) {
                            Platform.exit();
                            System.exit(0);
                        }
                        Thread.sleep(1000);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
