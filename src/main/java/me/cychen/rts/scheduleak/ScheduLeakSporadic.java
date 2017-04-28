package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.framework.Job;
import me.cychen.rts.framework.Task;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;

import java.util.ArrayList;

/**
 * Created by cy on 3/26/2017.
 */
public class ScheduLeakSporadic {

    /* logging information */
    TaskArrivalWindow taskArrivalWindow;
    Boolean smallestArrivalWindowMatched = false;
    long processedBiCount = 0;
    long processedPeriodCount = 0;

    public TaskArrivalWindow getTaskArrivalWindow() {
        return taskArrivalWindow;
    }

    public Boolean getSmallestArrivalWindowMatched() {
        return smallestArrivalWindowMatched;
    }

    public long getProcessedBiCount() {
        return processedBiCount;
    }

    public long getProcessedPeriodCount() {
        return processedPeriodCount;
    }

    public DistributionMap computeExecutionDistributionByTaskPeriod(BusyIntervalEventContainer inBiContainer, Task inTask) {
        long beginTimeStampe = inBiContainer.getBeginTime();
        long endTimeStamp = inBiContainer.getEndTime();

        DistributionMap executionMap = new DistributionMap();
        Interval baseTaskPeriodInterval = new Interval(0, inTask.getPeriod());

        // Compute the offset factor for the first busy interval
        long initialPeriodFactor = beginTimeStampe/inTask.getPeriod();
        for (long periodFactor=initialPeriodFactor; periodFactor*inTask.getPeriod()<=endTimeStamp; periodFactor++) {
            long thisPeriodBeginTime = periodFactor*inTask.getPeriod();
            for (BusyIntervalEvent thisBi : inBiContainer.findBusyIntervalsBetweenTimeStamp(thisPeriodBeginTime, thisPeriodBeginTime+inTask.getPeriod())) {
                Interval thisInterval = new Interval(thisBi.getOrgBeginTimestamp(), thisBi.getOrgEndTimestamp());
                thisInterval.shift(-thisPeriodBeginTime);

                // Trim the shifted busy interval and update the execution distribution.
                executionMap.touchInterval(baseTaskPeriodInterval.intersect(thisInterval));
            }
        }

        return executionMap;
    }

    /* This function processes through every period within given busy intervals. */
    public TaskArrivalWindow computeArrivalWindowOfTaskByIntersection(BusyIntervalEventContainer inBiContainer, Task inTask) {
        TaskArrivalWindow taskArrivalWindow = new TaskArrivalWindow(inTask);

        // Make the initial arrival window as the length of the task's period.
        taskArrivalWindow.union(new Interval(0, inTask.getPeriod()));

        updateTaskArrivalWindowOfTask(taskArrivalWindow, inBiContainer);

        taskArrivalWindow.shiftToNearZero();
        return taskArrivalWindow;
    }

    /* The process stops when there is one arrival window left and that it matches the given interval. */
    public Boolean computeArrivalWindowOfTaskByIntersection(BusyIntervalEventContainer inBiContainer, Task inTask, Interval stopIfMatchedInterval, long timeLimit) {
        TaskArrivalWindow taskArrivalWindow = new TaskArrivalWindow(inTask);

        // Make the initial arrival window as the length of the task's period.
        taskArrivalWindow.union(new Interval(0, inTask.getPeriod()));

        smallestArrivalWindowMatched = false;
        processedBiCount = 0;

        taskArrivalWindow.shiftToClosestPoint(inBiContainer.getBeginTime());

        while ( (taskArrivalWindow.getBegin()<inBiContainer.getEndTime()) && (taskArrivalWindow.getEnd()<timeLimit)) {
            ArrayList<BusyIntervalEvent> bisInThisPeriod = inBiContainer.findBusyIntervalsBetweenTimeStamp(taskArrivalWindow.getBegin(), taskArrivalWindow.getEnd());
            if (bisInThisPeriod.size() == 0) {
                throw new AssertionError();
            }
            IntermittentInterval biIntervalsInThisPeriod = new IntermittentInterval();
            for (BusyIntervalEvent thisBi : bisInThisPeriod) {
                biIntervalsInThisPeriod.intervals.add(new Interval(thisBi.getOrgBeginTimestamp(), thisBi.getOrgEndTimestamp()));
            }

            if (taskArrivalWindow.getIntersection(biIntervalsInThisPeriod).intervals.size()==0) {
                throw new AssertionError();
            }
            taskArrivalWindow.intersect(biIntervalsInThisPeriod);
            taskArrivalWindow.shift(inTask.getPeriod());

            if (taskArrivalWindow.hasMatchedArrivalWindow(stopIfMatchedInterval) && taskArrivalWindow.intervals.size()==1) {
                smallestArrivalWindowMatched = true;
                processedPeriodCount = taskArrivalWindow.getBegin()/inTask.getPeriod();
                break;
            }
        }

        taskArrivalWindow.shiftToNearZero();
        this.taskArrivalWindow = taskArrivalWindow;
        return smallestArrivalWindowMatched;
    }

    /* Provided busy intervals have to be continuous. */
    public TaskArrivalWindow updateTaskArrivalWindowOfTask(TaskArrivalWindow inTaskArrivalWindow, BusyIntervalEventContainer inBiContainer) {
        Task task = inTaskArrivalWindow.task;

        inTaskArrivalWindow.shiftToClosestPoint(inBiContainer.getBeginTime());

        while (inTaskArrivalWindow.getBegin() < inBiContainer.getEndTime()) {
            ArrayList<BusyIntervalEvent> bisInThisPeriod = inBiContainer.findBusyIntervalsBetweenTimeStamp(inTaskArrivalWindow.getBegin(), inTaskArrivalWindow.getEnd());
            IntermittentInterval biIntervalsInThisPeriod = new IntermittentInterval();
            for (BusyIntervalEvent thisBi : bisInThisPeriod) {
                biIntervalsInThisPeriod.intervals.add(new Interval(thisBi.getOrgBeginTimestamp(), thisBi.getOrgEndTimestamp()));
            }

            inTaskArrivalWindow.intersect(biIntervalsInThisPeriod);
            inTaskArrivalWindow.shift(task.getPeriod());
        }
        inTaskArrivalWindow.shiftToNearZero();
        return inTaskArrivalWindow;
    }

    public long findTaskSmallestJobExecutionTime(QuickFPSchedulerJobContainer inJobContainer, Task inTask) {
        long smallestTaskExecutionTime = 0;
        Boolean firstLoop = true;
        for (Job thisJob : inJobContainer.getTaskJobs(inTask)) {
            if (firstLoop) {
                smallestTaskExecutionTime = thisJob.remainingExecTime;
                firstLoop = false;
                continue;
            }
            smallestTaskExecutionTime = thisJob.remainingExecTime<smallestTaskExecutionTime ? thisJob.remainingExecTime : smallestTaskExecutionTime;
        }
        return smallestTaskExecutionTime;
    }

}
