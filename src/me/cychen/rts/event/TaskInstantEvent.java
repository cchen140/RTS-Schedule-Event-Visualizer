package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

/**
 * Created by jjs on 2/13/17.
 */
public class TaskInstantEvent extends InstantEvent {
    private Task task = null;
    private int recordData = 0;

    public TaskInstantEvent(long inTimeStamp, Task inTask, int inData, String inNote)
    {
        orgTimestamp = inTimeStamp;
        scaledTimestamp = inTimeStamp;
        task = inTask;
        recordData = inData;
        note = inNote;
    }

    public int getTaskId(){
        return task.getId();
    }

    public Task getTask() { return task; }

}
