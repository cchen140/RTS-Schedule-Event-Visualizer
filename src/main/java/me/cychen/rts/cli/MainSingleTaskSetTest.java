package me.cychen.rts.cli;

import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.DistributionMap;
import me.cychen.rts.scheduleak.ScheduLeakSporadic;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.rts.simulator.TaskSetContainer;
import me.cychen.rts.simulator.TaskSetGenerator;
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by cy on 3/28/2017.
 */
public class MainSingleTaskSetTest {
    static long SIM_DURATION = 100000;

    private static final Logger logger = LogManager.getLogger("MainSingleTaskSetTest");
    private static final Logger loggerExp2 = LogManager.getLogger("exp2");

    public static void main(String[] args) {
        logger.info("Test starts.");

        // Generate a task set.
        TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

        //taskSetGenerator.setMaxPeriod(100);
        //taskSetGenerator.setMinPeriod(50);

        //taskSetGenerator.setMaxExecTime(20);
        //taskSetGenerator.setMinExecTime(5);

        taskSetGenerator.setMaxUtil(0.6);
        taskSetGenerator.setMinUtil(0.5);

        taskSetGenerator.setNonHarmonicOnly(true);

        //taskSetGenerator.setMaxHyperPeriod(70070);
        //taskSetGenerator.setGenerateFromHpDivisors(true);

        /* Optimal attack condition experiment. */
       taskSetGenerator.setNeedGenObserverTask(true);
        taskSetGenerator.setObserverTaskPriority(5);
        taskSetGenerator.setVictimTaskPriority(10);

        taskSetGenerator.setMaxObservationRatio(999);
        taskSetGenerator.setMinObservationRatio(1);

        TaskSetContainer taskSets = taskSetGenerator.generate(15, 1);

        TaskSet taskSet = taskSets.getTaskSets().get(0);

        // victim
        Task victimTask = taskSet.getOneTaskByPriority(10);

        // observer task
        Task observerTask = taskSet.getOneTaskByPriority(5);

        logger.info(taskSet.toString());
        long taskSetHyperPeriod = taskSet.calHyperPeriod();
        logger.info("Task Hyper-period: " + taskSetHyperPeriod);

        // Upper bound
        long observationUpperBound_1 = ScheduLeakSporadic.computeObservationUpperBound_1(taskSet, observerTask, victimTask);
        long observationUpperBound_2 = ScheduLeakSporadic.computeObservationUpperBound_2(taskSet, observerTask, victimTask);
        logger.info("Observation Upper bound1: " + observationUpperBound_1 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_1/victimTask.getPeriod());
        logger.info("Observation Upper bound2: " + observationUpperBound_2 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_2/victimTask.getPeriod());

        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskSet);

        // Pre-schedule
        QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(observationUpperBound_1);//SIM_DURATION);

        // New Sporadic ScheduLeak
        ScheduLeakSporadic scheduLeakSporadic = new ScheduLeakSporadic();
        long victimTaskSmallestExecutionTime = scheduLeakSporadic.findTaskSmallestJobExecutionTime(simJobContainer, victimTask);

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
        //logger.info("\r\n" + taskExecutionDistribution.toString());

        logger.info("Victim Task: " + victimTask.toString());
        logger.info("Observer Task: " + observerTask.toString());
        logger.info("O(ov) = " + observerTask.getExecTime()/(double) Umath.gcd(victimTask.getPeriod(), observerTask.getPeriod()));
        logger.info("Victim task's smallest C = " + victimTaskSmallestExecutionTime);
        logger.info("Observation Upper bound1: " + observationUpperBound_1 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_1/victimTask.getPeriod());
        logger.info("Observation Upper bound2: " + observationUpperBound_2 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_2/victimTask.getPeriod());
        logger.info("\r\nMost Weighted Intervals: " + taskExecutionDistribution.getMostWeightedIntervals().toString());
        logger.info("Most Weighted Value: " + taskExecutionDistribution.getMostWeightedValue());

        //ScheduLeakRestricted scheduLeakRestricted = new ScheduLeakRestricted(taskSet, new BusyIntervalContainer(biEvents));
        //EventContainer decomposedEvents = scheduLeakRestricted.runDecomposition();

        // Create Excel file
//        ExcelLogHandler excelLogHandler = new ExcelLogHandler();
//
//        excelLogHandler.genRowSchedulerIntervalEvents(eventContainer);
//        excelLogHandler.genRowBusyIntervals(biEvents);
//        excelLogHandler.genRowBusyIntervals(observedBiEvents);
//        //excelLogHandler.genRowSchedulerIntervalEvents(decomposedEvents);
//
//        // Output inferred arrival window
//        /*
//        EventContainer arrivalWindowEventContainer = new EventContainer();
//        for (SchedulerIntervalEvent thisEvent : arrivalWindow.getArrivalWindowEventByTime(0)) {
//            arrivalWindowEventContainer.add(thisEvent);
//        }
//        excelLogHandler.genRowSchedulerIntervalEvents(arrivalWindowEventContainer);
//        */
//
//        excelLogHandler.saveAndClose(null);

        logger.info("The number of observed BIs: " + observedBiEvents.size());

        logger.info(eventContainer.getAllEvents());

    }
}
