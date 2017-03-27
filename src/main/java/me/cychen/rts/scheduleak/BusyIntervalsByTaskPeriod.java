package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by cy on 3/26/2017.
 */
public class BusyIntervalsByTaskPeriod {
    protected ArrayList<IntermittentInterval> busyIntervalsByTaskPeriod = new ArrayList<>();
    protected BusyIntervalEventContainer  biContainer;
    protected Task task;

    public BusyIntervalsByTaskPeriod(BusyIntervalEventContainer inBiContainer, Task inTask) {
        biContainer = inBiContainer;
        task = inTask;
    }

    protected Boolean buildPeriodIntervals() {

        return true;
    }

    public Boolean computeIntersection() {

        return true;
    }

}
