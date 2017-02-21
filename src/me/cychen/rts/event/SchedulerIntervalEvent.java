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

        orgEndTimestamp = orgBeginTimestamp;
        scaledEndTimestamp = orgEndTimestamp;

        task = inTask;
        note = inNote;

        // Since the end time is not specified, it is an incomplete event.
        eventCompleted = false;
    }

    public SchedulerIntervalEvent(long inBeginTimeStamp, long inEndTimeStamp, Task inTask, String inNote)
    {
        this(inBeginTimeStamp, inTask, inNote);
        orgEndTimestamp = inEndTimeStamp;
        scaledEndTimestamp = inEndTimeStamp;

        eventCompleted = true;
    }

    public Task getTask() { return task; }

    @Override
    public String toString() {
        return orgBeginTimestamp + "-" + orgEndTimestamp + ", " + task.getId() + ", " + note;
    }
}
