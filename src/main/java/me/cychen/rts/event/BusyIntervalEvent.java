package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by CY on 5/21/2015.
 */
public class BusyIntervalEvent extends IntervalEvent {
    //private long beginTimeStamp = 0;
    //private long endTimeStamp = 0;
    //TaskReleaseEventContainer compositionGroundTruth;
    //TaskReleaseEventContainer compositionInference = new TaskReleaseEventContainer();
    //TaskArrivalEventContainer arrivalInference = new TaskArrivalEventContainer();
    //ArrayList schedulingInference = new ArrayList<>();

    // There may have multiple inferences, so two-layer array is used here.
    private ArrayList<ArrayList<Task>> composition = new ArrayList<>();

    ArrayList<SchedulerIntervalEvent> schedulerIntervalEvents = new ArrayList<>();

    public BusyIntervalEvent(long inBeginTimeStamp, long inEndTimeStamp)
    {
        super(inBeginTimeStamp, inEndTimeStamp);
    }

    public ArrayList<SchedulerIntervalEvent> getSchedulerIntervalEvents() {
        return schedulerIntervalEvents;
    }

    //    public void setCompositionGroundTruth(TaskReleaseEventContainer inGroundTruth)
//    {
//        compositionGroundTruth = inGroundTruth;
//    }

    public void setComposition(ArrayList<ArrayList<Task>> inComposition)
    {
        composition = inComposition;
    }

//    public TaskReleaseEventContainer getCompositionGroundTruth()
//    {
//        return  compositionGroundTruth;
//    }

    public ArrayList<ArrayList<Task>> getComposition()
    {
        return composition;
    }

    public Boolean containsComposition(Task inTask)
    {
        // If any inferred compositions contain inTask, then return true.
        for (ArrayList<Task> thisComposition : composition)
        {
            if (thisComposition.contains(inTask) == true)
                return true;
        }
        return false;
    }

    /* Get the first element in the composition array.
     * This method is used when there is only one inference in each busy interval.
     */
    public ArrayList<Task> getFirstComposition()
    {
        if (composition.size() == 0)
        {
            composition.add(new ArrayList<Task>());
        }

        return composition.get(0);
    }

}