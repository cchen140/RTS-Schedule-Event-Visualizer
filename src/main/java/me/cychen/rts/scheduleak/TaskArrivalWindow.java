package me.cychen.rts.scheduleak;

import me.cychen.rts.framework.Task;

/**
 * Created by cy on 3/27/2017.
 */
public class TaskArrivalWindow extends IntermittentInterval {
    Task task;

    public TaskArrivalWindow(Task inTask) {
        super();
        task = inTask;
    }

    public void shiftToNearZero() {
        shiftToClosestPoint(0);
    }

    // This will find the first arrival time after the reference point.
    public void shiftToClosestPoint( long referenceTimePoint)
    {
        long difference = referenceTimePoint - getBegin();
        long shiftFactor = difference / task.getPeriod();
        if ( difference % task.getPeriod() == 0 ) {
            // shiftFactor remains unchanged.
        } else if ( difference > 0 ) {
            // referencePoint is bigger
            shiftFactor++;
        } else {
            // reference Point is smaller
            // shiftFactor is negative and will remain the unchanged.
        }

        shift( shiftFactor * task.getPeriod() );
    }
}
