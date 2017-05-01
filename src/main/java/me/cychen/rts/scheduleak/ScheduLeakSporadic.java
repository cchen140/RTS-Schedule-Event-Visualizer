package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.framework.Job;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by cy on 3/26/2017.
 */
public class ScheduLeakSporadic {
    private static final Logger logger = LogManager.getLogger("ScheduLeakSporadic");
    private static final Logger expLogger = LogManager.getLogger("exp1");

    /* logging information */
    TaskArrivalWindow taskArrivalWindow;
    Boolean smallestArrivalWindowMatched = false;
    long processedBiCount = 0;
    long processedPeriodCount = 0;
    public Boolean hasFoundArrival = false;
    public long foundPeriodFactor = 0;
    public long arrivalColumnCount = 0;

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

    public DistributionMap computeExecutionDistributionByTaskPeriod(BusyIntervalEventContainer inBiContainer, Task inVictim) {
        long beginTimeStampe = inBiContainer.getBeginTime();
        long endTimeStamp = inBiContainer.getEndTime();

        DistributionMap executionMap = new DistributionMap();
        Interval baseTaskPeriodInterval = new Interval(0, inVictim.getPeriod());


        // Compute the offset factor for the first busy interval
        long initialPeriodFactor = beginTimeStampe/inVictim.getPeriod();
        for (long periodFactor=initialPeriodFactor; periodFactor*inVictim.getPeriod()<=endTimeStamp; periodFactor++) {
            expLogger.trace("\n" + periodFactor + "\t");

            long thisPeriodBeginTime = periodFactor*inVictim.getPeriod();
            for (BusyIntervalEvent thisBi : inBiContainer.findBusyIntervalsBetweenTimeStamp(thisPeriodBeginTime, thisPeriodBeginTime+inVictim.getPeriod())) {
                Interval thisInterval = new Interval(thisBi.getOrgBeginTimestamp(), thisBi.getOrgEndTimestamp());
                thisInterval.shift(-thisPeriodBeginTime);

                // Trim the shifted busy interval and update the execution distribution.
                executionMap.touchInterval(baseTaskPeriodInterval.intersect(thisInterval));
            }

            /* Check if we have found the answer. */
            ArrayList<Interval> arrivalInferences = executionMap.getMostWeightedIntervals();
            if (arrivalInferences.size()==1) {
                if (arrivalInferences.get(0).getBegin() == inVictim.getInitialOffset()) {
                    //logger.info("Arrival is found: " + periodFactor + "x" + inVictim.getPeriod() + " = " +periodFactor*inVictim.getPeriod());

                    if (hasFoundArrival == false) {
                        foundPeriodFactor = periodFactor;
                        arrivalColumnCount = executionMap.getMostWeightedValue();
                        hasFoundArrival = true;
                    }
                } else {
                    if (hasFoundArrival == true) {
                        hasFoundArrival = false;
                    }
                }
            } else {
                if (hasFoundArrival == true) {
                    hasFoundArrival = false;
                }
            }

            long trueArrivalCount = executionMap.getValue(inVictim.getInitialOffset());
            long mostCount = executionMap.getMostWeightedValue();
            mostCount = mostCount==0 ? 1 : mostCount;
            expLogger.trace(arrivalInferences.size() + "\t" + trueArrivalCount + "\t" + mostCount + "\t" + (trueArrivalCount/(double)mostCount) + "\t");

            /* arrival inference precision */
            //ArrayList<Interval> possibleArrivalIntervals = executionMap.getMostWeightedIntervals();

            if (arrivalInferences.size()==0) {
                expLogger.trace(0.0 + "\t");
            } else {
                expLogger.trace(computeArrivalInferencePrecision(inVictim, arrivalInferences.get(0).getBegin()) + "\t");
            }

        }
        if (hasFoundArrival == true) {
            logger.info("Arrival is found: " + foundPeriodFactor + "x" + inVictim.getPeriod() + " = " + foundPeriodFactor * inVictim.getPeriod());
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

    public static long computeObservationUpperBound_1(TaskSet taskSet, Task observer, Task victim) {
        ArrayList<Task> hpTasks = taskSet.getHigherPriorityTasks(observer.getPriority());
        long po = observer.getPeriod();
        long pv = victim.getPeriod();
        double dividend = hpTasks.size()-1;
        double divisor1 = (1.0/(double) Umath.lcm(po,pv));
        double divisor2 = 0;
        for (Task thisTask : hpTasks) {
            if (thisTask==victim) {
                continue;
            }
            long ph = thisTask.getPeriod();
            divisor2 += Umath.gcd(po, ph)*thisTask.getExecTime()/(double)ph;
        }
        divisor2 = divisor2/(double)(po*pv);
        return (long)Math.ceil(dividend/(divisor1-divisor2));
    }

    public static long computeObservationUpperBound_2(TaskSet taskSet, Task observer, Task victim) {
        ArrayList<Task> hpTasks = taskSet.getHigherPriorityTasks(observer.getPriority());
        long po = observer.getPeriod();
        long pv = victim.getPeriod();
        double dividend = hpTasks.size()-1;
        double divisor1 = (1.0/(double) Umath.lcm(po,pv));
        double divisor2 = 0;
        for (Task thisTask : hpTasks) {
            if (thisTask==victim) {
                continue;
            }
            long ph = thisTask.getPeriod();
            long eh = thisTask.getExecTime();
            long theta_v_oh = Math.min(eh, Umath.lcm(po, ph)%pv);
            long countOfHEveryHP = (long)Math.ceil((double)eh/(double)theta_v_oh);
            long lcm_ovh = Umath.lcm(Umath.lcm(po, ph), pv);
            divisor2 += countOfHEveryHP/lcm_ovh;
        }
        return (long)Math.ceil(dividend / (divisor1-divisor2));

    }

    public double computeArrivalInferencePrecision(Task victim, long inInfer) {
        long gtArrival = victim.getInitialOffset();
        long pv = victim.getPeriod();
        long diff = Math.abs(gtArrival - inInfer);
        return diff>=(pv/2.0) ? 1.0-(pv-diff)/(pv/2.0) : 1.0-diff/(pv/2.0);
    }

}
