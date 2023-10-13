package common;

import java.io.IOException;
import java.util.logging.*;

// this file implements lamport clock funcionalities, which are to be used across all three entities.
// lamport clocks only need these three functions to work, the rest should be reflected in program behaviour

public class LamportClock {
    String className;
    String time;
    private int value = 0;

    public LamportClock(){

        // get the class name of the caller
        this.time = String.format("%04d", System.currentTimeMillis());

    }

    public synchronized void tick(){
        value++;
    }

    public synchronized void update(int newValue){
        if (newValue > value){
            value = newValue + 1;
        } else {
            value++;
        }
    }

    public int getValue(){
        return value;
    }

// this is the logging function to help me debugging. it generates a uniqe time stamp as well as class name.
// then writes down the event name with their lamport time stamps
public void log(String eventname) throws SecurityException, IOException {
    Logger logger = Logger.getLogger("lamportlogger");
    FileHandler handler = new FileHandler("logs/" + this.time +".log", true);
    logger.addHandler(handler);
    logger.setUseParentHandlers(false);
    SimpleFormatter formatter = new SimpleFormatter();
    handler.setFormatter(formatter);
    logger.info(eventname + " " + value);
}

}