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

    public static void main(String[] args) {
        logger.info("Test starts.");

        // Generate a task set.
        TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

        //taskSetGenerator.setMaxPeriod(100);
        //taskSetGenerator.setMinPeriod(50);

        //taskSetGenerator.setMaxExecTime(20);
        //taskSetGenerator.setMinExecTime(5);

        taskSetGenerator.setMaxUtil(0.9);
        taskSetGenerator.setMinUtil(0.1);
        TaskSetContainer taskSets = taskSetGenerator.generate(15, 10000);

        DistributionMap matchedPeriodDistribution = new DistributionMap();

        long successfulInferenceCount = 0;
        long successfulVictimHighestPriorityCount = 0;
        for (TaskSet thisTaskSet : taskSets.getTaskSets()) {
            //logger.info(thisTaskSet.toString());

            // New and configure a RM scheduling simulator.
            QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
            rmSimulator.setTaskSet(thisTaskSet);

            Task victimTask = thisTaskSet.getTaskById(5);
            //Task victimTask = thisTaskSet.getHighestPriorityTask();

            // Pre-schedule
            QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(SIM_DURATION);

            // New Sporadic ScheduLeak
            ScheduLeakSporadic scheduLeakSporadic = new ScheduLeakSporadic();
            long victimTaskSmallestExecutionTime = scheduLeakSporadic.findTaskSmallestJobExecutionTime(simJobContainer, victimTask);
            //logger.info("Victim task's smallest C = " + victimTaskSmallestExecutionTime);

            // Run simulation.
            rmSimulator.simJobs(simJobContainer);
            EventContainer eventContainer = rmSimulator.getSimEventContainer();

            // Build busy intervals for ScheduLeak
            BusyIntervalEventContainer biEvents = new BusyIntervalEventContainer();
            biEvents.createBusyIntervalsFromEvents(eventContainer);
            biEvents.removeBusyIntervalsBeforeButExcludeTimeStamp(victimTask.getInitialOffset());
            //biEvents.removeTheLastBusyInterval();

            /* Run ScheduLeak */
            TaskArrivalWindow arrivalWindow;
            if (scheduLeakSporadic.computeArrivalWindowOfTaskByIntersection(biEvents, victimTask, new Interval(victimTask.getInitialOffset(), victimTask.getInitialOffset()+victimTaskSmallestExecutionTime), SIM_DURATION)) {
                //logger.info("Arrival window matched! " + scheduLeakSporadic.getProcessedPeriodCount());
                successfulInferenceCount++;

                matchedPeriodDistribution.touch(scheduLeakSporadic.getProcessedPeriodCount());

                if (victimTask.getPriority() == 1) {
                    successfulVictimHighestPriorityCount++;
                }
            }
            //arrivalWindow = scheduLeakSporadic.getTaskArrivalWindow();
            //logger.info(arrivalWindow.toString());

        }

        logger.info("Successful Inference: " + successfulInferenceCount);
        logger.info(" - Highest Priority Victim: " + successfulVictimHighestPriorityCount);
        logger.info("Distribution: \r\n" + matchedPeriodDistribution.toString());


    }
}
