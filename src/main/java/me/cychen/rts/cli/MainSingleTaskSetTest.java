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
import me.cychen.rts.util.ExcelLogHandler;
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by cy on 3/28/2017.
 */
public class MainSingleTaskSetTest {
    static long SIM_DURATION = 100000;

    private static final int VICTIM_PRI = 2;
    private static final int OBSERVER_PRI = 1;

    private static final Logger loggerConsole = LogManager.getLogger("console");
    private static final Logger loggerExp2 = LogManager.getLogger("exp2");

    public static void main(String[] args) {
        loggerConsole.info("Test starts.");

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
        taskSetGenerator.setObserverTaskPriority(OBSERVER_PRI);
        taskSetGenerator.setVictimTaskPriority(VICTIM_PRI);

        taskSetGenerator.setMaxObservationRatio(999);
        taskSetGenerator.setMinObservationRatio(1);

        TaskSetContainer taskSets = taskSetGenerator.generate(15, 1);

        TaskSet taskSet = taskSets.getTaskSets().get(0);

        // victim and observer task
        Task victimTask = taskSet.getOneTaskByPriority(VICTIM_PRI);
        Task observerTask = taskSet.getOneTaskByPriority(OBSERVER_PRI);

        double gcd = Umath.gcd(victimTask.getPeriod(), observerTask.getPeriod());
        double lcm = Umath.lcm(victimTask.getPeriod(), observerTask.getPeriod());
        double observationRatio = observerTask.getExecTime() / gcd;


        loggerConsole.info(taskSet.toString());
        long taskSetHyperPeriod = taskSet.calHyperPeriod();
        loggerConsole.info("Task Hyper-period: " + taskSetHyperPeriod);

        // Upper bound
        long observationUpperBound_1 = ScheduLeakSporadic.computeObservationUpperBound_1(taskSet, observerTask, victimTask);
        long observationUpperBound_2 = ScheduLeakSporadic.computeObservationUpperBound_2(taskSet, observerTask, victimTask);
        loggerConsole.info("Observation Upper bound1: " + observationUpperBound_1 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_1/victimTask.getPeriod());
        loggerConsole.info("Observation Upper bound2: " + observationUpperBound_2 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_2/victimTask.getPeriod());

        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskSet);

        // Pre-schedule
        QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(SIM_DURATION);

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
                null;
        try {
            observedBiEvents = new BusyIntervalEventContainer( biEvents.getObservableBusyIntervalsByTask(observerTask) );
        } catch (Exception e) {
            e.printStackTrace();
        }


        /* Run ScheduLeak */
        /* intersection
        TaskArrivalWindow arrivalWindow;
        if (scheduLeakSporadic.computeArrivalWindowOfTaskByIntersection(biEvents, victimTask, new Interval(victimTask.getInitialOffset(), victimTask.getInitialOffset() + victimTaskSmallestExecutionTime), SIM_DURATION)) {
            loggerConsole.info("Arrival window matched! " + scheduLeakSporadic.getProcessedPeriodCount());
        }
        arrivalWindow = scheduLeakSporadic.getTaskArrivalWindow();
        loggerConsole.info(arrivalWindow.toString());
        */

        /* distribution map */
        DistributionMap taskExecutionDistribution = scheduLeakSporadic.computeExecutionDistributionByTaskPeriod(observedBiEvents, victimTask);
        //loggerConsole.info("\r\n" + taskExecutionDistribution.toString());

        loggerConsole.info("Victim Task: " + victimTask.toString());
        loggerConsole.info("Observer Task: " + observerTask.toString());
        loggerConsole.info("O(ov) = " + observerTask.getExecTime()/(double) Umath.gcd(victimTask.getPeriod(), observerTask.getPeriod()));
        loggerConsole.info("Victim task's smallest C = " + victimTaskSmallestExecutionTime);
        loggerConsole.info("Observation Upper bound1: " + observationUpperBound_1 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_1/victimTask.getPeriod());
        loggerConsole.info("Observation Upper bound2: " + observationUpperBound_2 + "/" + victimTask.getPeriod() + " = " + observationUpperBound_2/victimTask.getPeriod());
        loggerConsole.info("\r\nMost Weighted Intervals: " + taskExecutionDistribution.getMostWeightedIntervals().toString());
        loggerConsole.info("Most Weighted Value: " + taskExecutionDistribution.getMostWeightedValue());

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

        loggerConsole.info("The number of observed BIs: " + observedBiEvents.size());

        loggerConsole.info(eventContainer.getAllEvents());

    }
}
