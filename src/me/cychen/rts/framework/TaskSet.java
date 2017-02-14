package me.cychen.rts.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by jjs on 2/13/17.
 */
public class TaskSet {
    //private ArrayList<Task> tasks = new ArrayList<Task>();
    public HashMap<Integer, Task> tasks = new HashMap<Integer, Task>();
    //private ArrayList<Color> colorList = new ArrayList<Color>();

    public TaskSet()
    {
    }

    public Boolean addTask(Integer inTaskId, String inTitle, int inType, long inPeriod, long inDeadline, long inExecTime, int inPriority)
    {
        int newTaskId = 0;

        if (inTaskId == null) {
            newTaskId = getLargestTaskId() + 1;
        } else {
            newTaskId = inTaskId;
        }

        if (tasks.containsKey(newTaskId))
        {
            return false;
        }

        tasks.put(newTaskId,
                new Task(newTaskId, inTitle, inType, inPeriod, inDeadline, inExecTime, inPriority));

        return true;
    }

    //public Boolean addTask(int taskId, String taskTitle, int taskType, int taskPeriod, int taskComputationTime, int taskPriority)
    //{
    //    // When adding this task, assign deadline as equals period.
    //    return addTask(taskId, taskTitle, taskType, taskPeriod, taskPeriod, taskComputationTime, taskPriority);
    //}

    //public void addTask(Task inTask) {
    //
    //}

    public Task addBlankTask() {
        int newTaskId = getLargestTaskId() + 1;

        addTask(newTaskId, "Task" + newTaskId, Task.TASK_TYPE_APP, 10000000, 10000000, 100000, 0);
        return getTaskById(newTaskId);
    }

    public Boolean removeTask(Task inTask) {
        if (inTask == null)
            return false;

        int thisTaskId = inTask.getId();
        if (getTaskById(thisTaskId) == null) {
            return false;
        }
        else {
            tasks.remove(thisTaskId);
            return true;
        }
    }

    public Task getTaskById(int searchId)
    {
        return tasks.get(searchId);
    }

    public ArrayList<Task> getTasksAsArray()
    {
        ArrayList<Task> resultTaskList = new ArrayList<Task>();
        ArrayList<Integer> taskIdList = new ArrayList<Integer>(tasks.keySet());
        Collections.sort(taskIdList);
        for (int thisTaskId : taskIdList)
        {
            resultTaskList.add(tasks.get(thisTaskId));
        }

//        return resultTaskList.toArray();
        return resultTaskList;
    }

    public ArrayList<Task> getAppTaskAsArraySortedByComputationTime()
    {
        // This method will return a new task array.
        return SortTasksByComputationTime(getAppTasksAsArray());
    }

    public ArrayList<Task> getAppTaskAsArraySortedByPeriod() {
        // This method will return a new task array.
        return SortTasksByPeriod(getAppTasksAsArray());
    }

    private ArrayList<Task> SortTasksByComputationTime(ArrayList<Task> inTaskArray)
    {
        if (inTaskArray.size() <= 1)
        { // If only one task is left in the array, then just return it.
            return new ArrayList<Task>(inTaskArray);
        }

        /* Find the task that has largest computation time. */
        Task LargestComputationTimeTask = null;
        Boolean firstLoop = true;
        for (Task thisTask : inTaskArray)
        {
            if (firstLoop == true)
            {
                LargestComputationTimeTask = thisTask;
                firstLoop = false;
                continue;
            }
            else
            {
                if (thisTask.getExecTime() > LargestComputationTimeTask.getExecTime())
                {
                    LargestComputationTimeTask = thisTask;
                }
            }
        }

        // Clone the input task array and pass it into next layer of recursive function (with largest task removed).
        ArrayList processingTaskArray = new ArrayList<Task>(inTaskArray);
        processingTaskArray.remove(LargestComputationTimeTask);

        // Get the rest of tasks sorted in the array.
        ArrayList<Task> resultTaskArray = SortTasksByComputationTime(processingTaskArray);

        // Add the largest computation time task in the array so that it is in ascending order.
        resultTaskArray.add(LargestComputationTimeTask);
        return resultTaskArray;

    }

    private ArrayList<Task> SortTasksByPeriod(ArrayList<Task> inTaskArray)
    {
        if (inTaskArray.size() <= 1)
        { // If only one task is left in the array, then just return it.
            return new ArrayList<Task>(inTaskArray);
        }

        /* Find the task that has largest period. */
        Task LargestPeriodTask = null;
        Boolean firstLoop = true;
        for (Task thisTask : inTaskArray)
        {
            if (firstLoop == true)
            {
                LargestPeriodTask = thisTask;
                firstLoop = false;
                continue;
            }
            else
            {
                if (thisTask.getPeriod() > LargestPeriodTask.getPeriod())
                {
                    LargestPeriodTask = thisTask;
                }
            }
        }

        // Clone the input task array and pass it into next layer of recursive function (with largest task removed).
        ArrayList processingTaskArray = new ArrayList<Task>(inTaskArray);
        processingTaskArray.remove(LargestPeriodTask);

        // Get the rest of tasks sorted in the array.
        ArrayList<Task> resultTaskArray = SortTasksByPeriod(processingTaskArray);

        // Add the largest period task in the array so that it is in ascending order.
        resultTaskArray.add(LargestPeriodTask);
        return resultTaskArray;

    }

    public ArrayList<Task> getAppTasksAsArray()
    {
        ArrayList<Task> appTasks = new ArrayList<Task>();
        for (Task thisTask: tasks.values())
        {
            if (thisTask.getTaskType() == Task.TASK_TYPE_APP)
            {
                appTasks.add(thisTask);
            }
        }
        return appTasks;
    }

    public void clear()
    {
        tasks.clear();
    }

    public int size() { return tasks.size(); }


    public Task getTaskByName( String inName ) {
        for (Task thisTask : getTasksAsArray()) {
            if ( thisTask.getTitle().equalsIgnoreCase(inName) == true ) {
                return thisTask;
            }
        }

        // No task has been found.
        return null;
    }

    public void removeIdleTask() {
        Task idleTask = getTaskByName("IDLE");
        if (idleTask != null) {
            removeTask(idleTask);
        }
    }

    //public void clearSimData() {
    //    for (Task thisTask : getTasksAsArray()) {
    //        thisTask.clearSimData();
    //    }
    //}

    public int getLargestTaskId() {
        int largestId = 0;
        /* Search for the largest ID number. */
        for (int thisId : tasks.keySet()) {
            largestId = (largestId>thisId) ? largestId : thisId;
        }
        return largestId;
    }
}
