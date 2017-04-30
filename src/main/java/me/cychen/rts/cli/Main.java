package me.cychen.rts.cli;

import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.*;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.rts.simulator.TaskSetContainer;
import me.cychen.rts.simulator.TaskSetGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by jjs on 2/13/17.
 */
public class Main {
    static long SIM_DURATION = 10000;

    private static final Logger logger = LogManager.getLogger("Main");
    private static final Logger loggerExp2 = LogManager.getLogger("exp2");

    public static void main(String[] args) {
        logger.info("Test starts.");
        loggerExp2.trace("\n");

        // Generate a task set.
        TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

        //taskSetGenerator.setMaxPeriod(100);
        //taskSetGenerator.setMinPeriod(50);

        //taskSetGenerator.setMaxExecTime(20);
        //taskSetGenerator.setMinExecTime(5);
for (double u = 0.1; u<1; u+=0.1) {
    taskSetGenerator.setMaxUtil(u + 0.09);
    taskSetGenerator.setMinUtil(u);

    taskSetGenerator.setNonHarmonicOnly(true);

    //taskSetGenerator.setMaxHyperPeriod(70070);
    //taskSetGenerator.setGenerateFromHpDivisors(true);

        /* Optimal attack condition experiment. */
    taskSetGenerator.setNeedGenObserverTask(true);
    //taskSetGenerator.setNeedGenBadObserverTask(true);
    taskSetGenerator.setObserverTaskPriority(5);
    taskSetGenerator.setVictimTaskPriority(10);

    TaskSetContainer taskSets = taskSetGenerator.generate(15, 10);

    DistributionMap matchedPeriodDistribution = new DistributionMap();

    long successfulInferenceCount = 0;
    long successfulVictimHighestPriorityCount = 0;
    for (TaskSet thisTaskSet : taskSets.getTaskSets()) {
        //logger.info(thisTaskSet.toString());

        // victim and observer task
        Task victimTask = thisTaskSet.getOneTaskByPriority(10);
        Task observerTask = thisTaskSet.getOneTaskByPriority(5);

        // Upper bound
        long observationUpperBound_1 = ScheduLeakSporadic.computeObservationUpperBound_1(thisTaskSet, observerTask, victimTask);
        //long observationUpperBound_2 = ScheduLeakSporadic.computeObservationUpperBound_2(thisTaskSet, observerTask, victimTask);
        //logger.info("Observation Upper bound1: " + observationUpperBound_1 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_1/victimTask.getPeriod());
        //logger.info("Observation Upper bound2: " + observationUpperBound_2 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_2/victimTask.getPeriod());

        if (observationUpperBound_1 > 1000000) {
            continue;
        } else if (observationUpperBound_1 <= 0) {
            continue;
        }

        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(thisTaskSet);

        // Pre-schedule
        QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(observationUpperBound_1);

        // New Sporadic ScheduLeak
        ScheduLeakSporadic scheduLeakSporadic = new ScheduLeakSporadic();
        //long victimTaskSmallestExecutionTime = scheduLeakSporadic.findTaskSmallestJobExecutionTime(simJobContainer, victimTask);
        //logger.info("Victim task's smallest C = " + victimTaskSmallestExecutionTime);

        // Run simulation.
        rmSimulator.simJobs(simJobContainer);
        EventContainer eventContainer = rmSimulator.getSimEventContainer();

        // Build busy intervals for ScheduLeak
        BusyIntervalEventContainer biEvents = new BusyIntervalEventContainer();
        biEvents.createBusyIntervalsFromEvents(eventContainer);
        biEvents.removeBusyIntervalsBeforeButExcludeTimeStamp(victimTask.getInitialOffset());
        //biEvents.removeTheLastBusyInterval();

        // Get only observable busy intervals
        BusyIntervalEventContainer observedBiEvents =
                new BusyIntervalEventContainer(biEvents.getObservableBusyIntervalsByTask(observerTask));

            /* distribution map */
        DistributionMap taskExecutionDistribution = scheduLeakSporadic.computeExecutionDistributionByTaskPeriod(observedBiEvents, victimTask);

            /* Run ScheduLeak */
//            TaskArrivalWindow arrivalWindow;
//            if (scheduLeakSporadic.computeArrivalWindowOfTaskByIntersection(biEvents, victimTask, new Interval(victimTask.getInitialOffset(), victimTask.getInitialOffset()+victimTaskSmallestExecutionTime), SIM_DURATION)) {
//                //logger.info("Arrival window matched! " + scheduLeakSporadic.getProcessedPeriodCount());
//                successfulInferenceCount++;
//
//                matchedPeriodDistribution.touch(scheduLeakSporadic.getProcessedPeriodCount());
//
//                if (victimTask.getPriority() == 1) {
//                    successfulVictimHighestPriorityCount++;
//                }
//            }
        //arrivalWindow = scheduLeakSporadic.getTaskArrivalWindow();
        //logger.info(arrivalWindow.toString());

        loggerExp2.trace("\n" + thisTaskSet.getUtilization() + "\t" + observationUpperBound_1 + "\t" + scheduLeakSporadic.foundPeriodFactor + "\t" + scheduLeakSporadic.foundPeriodFactor / (double) observationUpperBound_1);

    }
}
//        logger.info("Successful Inference: " + successfulInferenceCount);
//        logger.info(" - Highest Priority Victim: " + successfulVictimHighestPriorityCount);
//        logger.info("Distribution: \r\n" + matchedPeriodDistribution.toString());
        logger.info("Finished.");

    }
}
