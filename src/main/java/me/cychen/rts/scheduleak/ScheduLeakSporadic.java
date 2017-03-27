package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by cy on 3/26/2017.
 */
public class ScheduLeakSporadic {

    public IntermittentInterval computeArrivalWindowOfTask(BusyIntervalEventContainer inBiContainer, Task inTask) {
        TaskArrivalWindow taskArrivalWindow = new TaskArrivalWindow(inTask);

        // Make the initial arrival window as the length of the task's period.
        taskArrivalWindow.union(new Interval(0, inTask.getPeriod()));

        while (taskArrivalWindow.getBegin() < inBiContainer.getEndTime()) {
            ArrayList<BusyIntervalEvent> bisInThisPeriod = inBiContainer.findBusyIntervalsBetweenTimeStamp(taskArrivalWindow.getBegin(), taskArrivalWindow.getEnd());
            IntermittentInterval biIntervalsInThisPeriod = new IntermittentInterval();
            for (BusyIntervalEvent thisBi : bisInThisPeriod) {
                biIntervalsInThisPeriod.intervals.add(new Interval(thisBi.getOrgBeginTimestamp(), thisBi.getOrgEndTimestamp()));
            }

            taskArrivalWindow.intersect(biIntervalsInThisPeriod);
            taskArrivalWindow.shift(inTask.getPeriod());
        }
        taskArrivalWindow.shiftToNearZero();
        return taskArrivalWindow;
    }

}
