package me.cychen.rts.cli;

import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.DistributionMap;
import me.cychen.rts.scheduleak.Interval;
import me.cychen.rts.scheduleak.ScheduLeakSporadic;
import me.cychen.rts.scheduleak.TaskArrivalWindow;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.rts.simulator.TaskSetContainer;
import me.cychen.rts.simulator.TaskSetGenerator;
import me.cychen.rts.util.ExcelLogHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by cy on 3/28/2017.
 */
public class MainSingleTaskSetTest {
    static long SIM_DURATION = 100000;

    private static final Logger logger = LogManager.getLogger("MainSingleTaskSetTest");

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
        TaskSetContainer taskSets = taskSetGenerator.generate(15, 1);

        TaskSet taskSet = taskSets.getTaskSets().get(0);

        // victim
        Task victimTask = taskSet.getOneTaskByPriority(6);
        logger.info("Victim Task: " + victimTask.toString());

        // observer task
        Task observerTask = taskSet.getOneTaskByPriority(1 );
        logger.info("Observer Task: " + observerTask.toString());

        logger.info(taskSet.toString());
        long taskSetHyperPeriod = taskSet.calHyperPeriod();
        logger.info("Task Hyper-period: " + taskSetHyperPeriod);

        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskSet);

        // Pre-schedule
        QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(SIM_DURATION);

        // New Sporadic ScheduLeak
        ScheduLeakSporadic scheduLeakSporadic = new ScheduLeakSporadic();
        long victimTaskSmallestExecutionTime = scheduLeakSporadic.findTaskSmallestJobExecutionTime(simJobContainer, victimTask);
        logger.info("Victim task's smallest C = " + victimTaskSmallestExecutionTime);

        // Run simulation.
        rmSimulator.simJobs(simJobContainer);
        EventContainer eventContainer = rmSimulator.getSimEventContainer();

        // Build busy intervals for ScheduLeak
        BusyIntervalEventContainer biEvents = new BusyIntervalEventContainer();
        biEvents.createBusyIntervalsFromEvents(eventContainer);
        biEvents.removeBusyIntervalsBeforeButExcludeTimeStamp(victimTask.getInitialOffset());

        // Get only observable busy intervals
        BusyIntervalEventContainer observedBiEvents =
                new BusyIntervalEventContainer( biEvents.getObservableBusyIntervalsByTask(observerTask) );


        /* Run ScheduLeak */
        /* intersection
        TaskArrivalWindow arrivalWindow;
        if (scheduLeakSporadic.computeArrivalWindowOfTaskByIntersection(biEvents, victimTask, new Interval(victimTask.getInitialOffset(), victimTask.getInitialOffset() + victimTaskSmallestExecutionTime), SIM_DURATION)) {
            logger.info("Arrival window matched! " + scheduLeakSporadic.getProcessedPeriodCount());
        }
        arrivalWindow = scheduLeakSporadic.getTaskArrivalWindow();
        logger.info(arrivalWindow.toString());
        */

        /* distribution map */
        DistributionMap taskExecutionDistribution = scheduLeakSporadic.computeExecutionDistributionByTaskPeriod(observedBiEvents, victimTask);
        logger.info("\r\n" + taskExecutionDistribution.toString());

        //ScheduLeakRestricted scheduLeakRestricted = new ScheduLeakRestricted(taskSet, new BusyIntervalContainer(biEvents));
        //EventContainer decomposedEvents = scheduLeakRestricted.runDecomposition();

        // Create Excel file
        ExcelLogHandler excelLogHandler = new ExcelLogHandler();

        excelLogHandler.genRowSchedulerIntervalEvents(eventContainer);
        excelLogHandler.genRowBusyIntervals(biEvents);
        excelLogHandler.genRowBusyIntervals(observedBiEvents);
        //excelLogHandler.genRowSchedulerIntervalEvents(decomposedEvents);

        // Output inferred arrival window
        /*
        EventContainer arrivalWindowEventContainer = new EventContainer();
        for (SchedulerIntervalEvent thisEvent : arrivalWindow.getArrivalWindowEventByTime(0)) {
            arrivalWindowEventContainer.add(thisEvent);
        }
        excelLogHandler.genRowSchedulerIntervalEvents(arrivalWindowEventContainer);
        */

        excelLogHandler.saveAndClose(null);

        logger.info("The number of observed BIs: " + observedBiEvents.size());

        logger.info(eventContainer.getAllEvents());

    }
}
