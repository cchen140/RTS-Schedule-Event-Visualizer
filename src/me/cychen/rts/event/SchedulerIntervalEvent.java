package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

/**
 * Created by jjs on 2/13/17.
 */
public class SchedulerIntervalEvent extends IntervalEvent {
    private Task task = null;

    public SchedulerIntervalEvent(long inTimeStamp, Task inTask, String inNote)
    {
        orgBeginTimestamp = inTimeStamp;
        scaledBeginTimestamp = inTimeStamp;
        task = inTask;
        note = inNote;
    }

    public SchedulerIntervalEvent(long inBeginTimeStamp, long inEndTimeStamp, Task inTask, String inNote)
    {
        this(inBeginTimeStamp, inTask, inNote);
        orgEndTimestamp = inEndTimeStamp;
        scaledEndTimestamp = inEndTimeStamp;
    }

    public Task getTask() { return task; }

}
