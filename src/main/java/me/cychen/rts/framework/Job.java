package me.cychen.rts.framework;

/**
 * Created by CY on 2/13/17.
 */
public class Job {
    public long seqNo;
    public Task task;
    public long remainingExecTime;
    public long releaseTime;
    //public long responseTime;
    public boolean hasStarted;

    public Job(){}

    public Job( Task inTask, long inReleaseTime, long inRemainingExecTime )
    {
        task = inTask;
        releaseTime = inReleaseTime;
        remainingExecTime = inRemainingExecTime;
        hasStarted = false;
    }
}
