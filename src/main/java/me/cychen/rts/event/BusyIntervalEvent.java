package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by CY on 5/21/2015.
 */
public class BusyIntervalEvent extends IntervalEvent {
     ArrayList<SchedulerIntervalEvent> schedulerIntervalEvents = new ArrayList<>();

    public BusyIntervalEvent(long inBeginTimeStamp, long inEndTimeStamp)
    {
        super(inBeginTimeStamp, inEndTimeStamp);
    }

    public ArrayList<SchedulerIntervalEvent> getSchedulerIntervalEvents() {
        return schedulerIntervalEvents;
    }
}