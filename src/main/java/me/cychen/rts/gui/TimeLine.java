package me.cychen.rts.gui;

import me.cychen.util.ProgMsg;

/**
 * Created by jjs on 2/14/17.
 */
public class TimeLine {
    private long beginTimestamp = 0;
    private double currentEndTimestamp = 0;

    public double getCurrentEndTimestamp() {
        return currentEndTimestamp;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public void pushEndTimestamp(double inTime) {
        currentEndTimestamp = currentEndTimestamp>inTime?currentEndTimestamp:inTime;
    }

    //public void extendEnd(double inExtraLength) {
    //    currentEndTimestamp += inExtraLength;
    //}

    public void updateEndTimestamp() {
        currentEndTimestamp = System.nanoTime()/1_000_000-beginTimestamp;
    }

    public void resetBeginTime() {
        beginTimestamp = System.nanoTime()/1_000_000;
        //ProgMsg.putLine("ResetBeginTime = " + beginTimestamp);
    }
}
