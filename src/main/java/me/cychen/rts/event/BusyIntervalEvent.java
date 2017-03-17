package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by CY on 5/21/2015.
 */
public class BusyIntervalEvent extends IntervalEvent {
    private int beginTimeStampNs = 0;
    private int endTimeStampNs = 0;
    TaskReleaseEventContainer compositionGroundTruth;
    //TaskReleaseEventContainer compositionInference = new TaskReleaseEventContainer();
    //TaskArrivalEventContainer arrivalInference = new TaskArrivalEventContainer();
    ArrayList schedulingInference = new ArrayList<>();

    // There may have multiple inferences, so two-layer array is used here.
    private ArrayList<ArrayList<Task>> composition = new ArrayList<>();

    public BusyIntervalEvent(int inBeginTimeStamp, int inEndTimeStamp)
    {
        beginTimeStampNs = inBeginTimeStamp;
        endTimeStampNs = inEndTimeStamp;
    }

    public void setCompositionGroundTruth(TaskReleaseEventContainer inGroundTruth)
    {
        compositionGroundTruth = inGroundTruth;
    }

    public void setComposition(ArrayList<ArrayList<Task>> inComposition)
    {
        composition = inComposition;
    }

    public int getIntervalNs()
    {
        return (endTimeStampNs - beginTimeStampNs);
    }

    public int getBeginTimeStampNs()
    {
        return beginTimeStampNs;
    }

    public TaskReleaseEventContainer getCompositionGroundTruth()
    {
        return  compositionGroundTruth;
    }

    public ArrayList<ArrayList<Task>> getComposition()
    {
        return composition;
    }

    public int getEndTimeStampNs() {
        return endTimeStampNs;
    }

    public Boolean contains(int inTimeStamp)
    {
        if ((beginTimeStampNs <= inTimeStamp)
                && (endTimeStampNs >= inTimeStamp))
        {
            return true;
        }
        else
        {
            return false;
        }
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