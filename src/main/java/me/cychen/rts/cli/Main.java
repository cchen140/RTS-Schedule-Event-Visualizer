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
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by jjs on 2/13/17.
 */
public class Main {
    static long SIM_DURATION = 10000;

    private static final int VICTIM_PRI = 2;
    private static final int OBSERVER_PRI = 1;
    //private static final int NUM_OF_TASKS = 15;
    private static final int NUM_OF_TASK_SETS = 10;

    private static final Logger logger = LogManager.getLogger("Main");
    private static final Logger loggerExp_by_taskset = LogManager.getLogger("exp_by_taskset");
    private static final Logger loggerExp_by_util = LogManager.getLogger("exp_by_util");
    private static final Logger loggerExp_by_exp_by_numOfTasksPerTaskset = LogManager.getLogger("exp_by_numOfTasksPerTaskset");


    public static void main(String[] args) {
        int errorCount = 0;


        logger.info("Test starts.");
        loggerExp_by_taskset.trace("\n");

        // Generate a task set.
        //TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

        //taskSetGenerator.setMaxPeriod(100);
        //taskSetGenerator.setMinPeriod(50);

        //taskSetGenerator.setMaxExecTime(20);
        //taskSetGenerator.setMinExecTime(5);
        for (int numOfTasks = 5; numOfTasks<=15; numOfTasks+=2) {
            double processedLcmPvPo_numOfTasksPerTaskset = 0;

            loggerExp_by_util.trace("\n numOfTasks: " + numOfTasks);

            for (double u = 0.0; u <= 0.91; u += 0.1) {
                TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

                taskSetGenerator.setMaxUtil(u + 0.09);
                taskSetGenerator.setMinUtil(u);

                taskSetGenerator.setNonHarmonicOnly(true);

                //taskSetGenerator.setMaxHyperPeriod(500000);  // 2 3 5 7 11 13
                //taskSetGenerator.setGenerateFromHpDivisors(true);

        /* Optimal attack condition experiment. */
                taskSetGenerator.setNeedGenObserverTask(true);
                taskSetGenerator.setMaxObservationRatio(999);
                taskSetGenerator.setMinObservationRatio(1);

                //taskSetGenerator.setNeedGenBadObserverTask(true);
                taskSetGenerator.setObserverTaskPriority(OBSERVER_PRI);
                taskSetGenerator.setVictimTaskPriority(VICTIM_PRI);

                //TaskSetContainer taskSets = taskSetGenerator.generate(NUM_OF_TASKS, NUM_OF_TASK_SETS);

                //DistributionMap matchedPeriodDistribution = new DistributionMap();

                long successfulInferenceCount = 0;
                long successfulVictimHighestPriorityCount = 0;
                double processedLcmPvPo = 0;
                int taskSetCount = NUM_OF_TASK_SETS;
                int failureCount = 0;
                while (taskSetCount > 0) {
                    //logger.info(thisTaskSet.toString());
                    TaskSet thisTaskSet = taskSetGenerator.generate(numOfTasks, 1).getTaskSets().get(0);

                    long hyperPeriod = thisTaskSet.calHyperPeriod();

                    // victim and observer task
                    Task victimTask = thisTaskSet.getOneTaskByPriority(VICTIM_PRI);
                    Task observerTask = thisTaskSet.getOneTaskByPriority(OBSERVER_PRI);

                    double gcd = Umath.gcd(victimTask.getPeriod(), observerTask.getPeriod());
                    double lcm = Umath.lcm(victimTask.getPeriod(), observerTask.getPeriod());
                    double observationRatio = observerTask.getExecTime() / gcd;

                    // Upper bound
                    long observationUpperBound_1 = ScheduLeakSporadic.computeObservationUpperBound_1(thisTaskSet, observerTask, victimTask);
                    //long observationUpperBound_2 = ScheduLeakSporadic.computeObservationUpperBound_2(thisTaskSet, observerTask, victimTask);
                    //logger.info("Observation Upper bound1: " + observationUpperBound_1 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_1/victimTask.getPeriod());
                    //logger.info("Observation Upper bound2: " + observationUpperBound_2 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_2/victimTask.getPeriod());

//                    if (observationUpperBound_1 > 1000000) {
//                        continue;
//                    } else if (observationUpperBound_1 <= 0) {
//                        errorCount++;
//                        //logger.error("Negative upper bound!");
//                        continue;
//                    }

                    // New and configure a RM scheduling simulator.
                    QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
                    rmSimulator.setTaskSet(thisTaskSet);

                    // Pre-schedule
                    QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule((long)lcm*10); //################################# warning!!!

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
                    //biEvents.removeBusyIntervalsBeforeButExcludeTimeStamp(hyperPeriod);
                    //biEvents.removeTheLastBusyInterval();

                    // Get only observable busy intervals
                    BusyIntervalEventContainer observedBiEvents =
                            null;
                    try {
                        observedBiEvents = new BusyIntervalEventContainer(biEvents.getObservableBusyIntervalsByTask(observerTask));
                    } catch (Exception e) {
                        //e.printStackTrace();
                        continue;
                    }

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

//        if (scheduLeakSporadic.foundPeriodFactor == 0) {
//            continue;
//        }
                    if (scheduLeakSporadic.hasFoundArrival == false) {
                        failureCount++;
                        taskSetCount--;
                        continue;
                    }


                    long foundTimeTick = scheduLeakSporadic.foundPeriodFactor*victimTask.getPeriod();

                    //loggerExp_by_taskset.trace("\n" + thisTaskSet.getUtilization() + "\t" + scheduLeakSporadic.foundPeriodFactor + "\t" + victimTask.getPeriod() + "\t" + scheduLeakSporadic.foundPeriodFactor*victimTask.getPeriod() + "\t" + observationUpperBound_1 + "\t" + (scheduLeakSporadic.foundPeriodFactor*victimTask.getPeriod()) / (double) observationUpperBound_1);
                    loggerExp_by_taskset.trace("\n" + thisTaskSet.getUtilization() + "\t" + scheduLeakSporadic.foundPeriodFactor + "\t" + victimTask.getPeriod() + "\t" + observerTask.getPeriod() + "\t" + observationUpperBound_1 + "\t" + foundTimeTick/lcm + "\t" + observationUpperBound_1 / lcm + "\t" + (foundTimeTick / (double) observationUpperBound_1));
                    loggerExp_by_taskset.trace("\t" + scheduLeakSporadic.arrivalColumnCount);
                    loggerExp_by_taskset.trace("\t" + observationRatio);
                    loggerExp_by_taskset.trace("\t" + hyperPeriod + "\t" + hyperPeriod/lcm);

                    processedLcmPvPo += ((scheduLeakSporadic.foundPeriodFactor * victimTask.getPeriod()) / (double) lcm);
                    taskSetCount--;
                }

                processedLcmPvPo = processedLcmPvPo/(NUM_OF_TASK_SETS - failureCount);
                processedLcmPvPo_numOfTasksPerTaskset += processedLcmPvPo;

                loggerExp_by_util.trace("\n" + u + "\t" + processedLcmPvPo);
                loggerExp_by_util.trace("\t" + ((double) failureCount / NUM_OF_TASK_SETS));
            }

            processedLcmPvPo_numOfTasksPerTaskset = processedLcmPvPo_numOfTasksPerTaskset/10.0; // 10 utilization groups.

            loggerExp_by_exp_by_numOfTasksPerTaskset.trace("\n" + numOfTasks + "\t" + processedLcmPvPo_numOfTasksPerTaskset);
        }
//        logger.info("Successful Inference: " + successfulInferenceCount);
//        logger.info(" - Highest Priority Victim: " + successfulVictimHighestPriorityCount);
//        logger.info("Distribution: \r\n" + matchedPeriodDistribution.toString());
        logger.info("Finished.");
        logger.error("Negative upper bound: " + errorCount);

    }
}
