package me.cychen.rts.scheduleak;

import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.framework.Task;

import java.util.ArrayList;

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

    public ArrayList<SchedulerIntervalEvent> getArrivalWindowEventByTime(long inTime) {
        ArrayList<SchedulerIntervalEvent> resultArrivalWindowEvents = new ArrayList<>();

        shiftToClosestPoint(inTime);
        for (Interval thisInterval : intervals) {
            resultArrivalWindowEvents.add(new SchedulerIntervalEvent(thisInterval.getBegin(), thisInterval.getEnd(), task, ""));
        }

        return resultArrivalWindowEvents;
    }

    public Boolean hasMatchedArrivalWindow(Interval inInterval) {
        long orgBegin = getBegin();
        Boolean isMatched = false;
        shiftToClosestPoint(inInterval.getBegin());
        isMatched = hasInterval(inInterval);

        // restore offset.
        shiftToClosestPoint(orgBegin);

        return isMatched;
    }
}
