package me.cychen.rts.gui;

/**
 * Created by jjs on 2/14/17.
 */
public class TimeLine {
    private double currentEndTimestamp = 0;

    public double getCurrentEndTimestamp() {
        return currentEndTimestamp;
    }

    public void pushEndTimestamp(double inTime) {
        currentEndTimestamp = currentEndTimestamp>inTime?currentEndTimestamp:inTime;
    }
}
