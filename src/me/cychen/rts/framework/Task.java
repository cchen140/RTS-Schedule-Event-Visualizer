package me.cychen.rts.framework;

/**
 * Created by CY on 2/13/17.
 */
public class Task {
    public static int TASK_TYPE_UNKNOWN = 0;
    public static int TASK_TYPE_SYS = 1;
    public static int TASK_TYPE_APP = 2;
    public static int TASK_TYPE_IDLE = 3;
    public static int TASK_TYPE_HACK = 4;

    protected int id = 0;
    private int taskType = TASK_TYPE_UNKNOWN;

    private String title = "";

    protected long period = 0;

    protected long execTime = 0;
    protected long execTimeError = 500000;  // The error should be positive.

    protected int priority = 0;
    protected long deadline = 0;

    public Task(){}

    public Task(int inTaskId, String inTitle, int inType, long inPeriod, long inDeadline, long inExecTime, int inPriority)
    {
        title = inTitle;
        id = inTaskId;
        taskType = inType;
        period = inPeriod;
        deadline = inDeadline;
        execTime = inExecTime;
        priority = inPriority;
    }

    //public Task(Task inTask) {
    //    cloneSettings(inTask);
    //}

    public int getId()
    {
        return id;
    }

    public long getExecTime() {
        return execTime;
    }

    public long getPeriod() {
        return period;
    }

    public String getTitle()
    {
        return title;
    }

    public int getTaskType()
    {
        return taskType;
    }

    public long getDeadline() { return deadline; }

    public int getPriority() { return priority; }

    public void setPriority(int inPriority)
    {
        priority = inPriority;
    }

    public void setExecTimeError(long execTimeError) {
        this.execTimeError = execTimeError;
    }

    public long getExecTimeError() {
        return execTimeError;
    }
}
