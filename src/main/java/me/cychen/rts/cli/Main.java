package me.cychen.rts.cli;

import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.rts.simulator.TaskSetContainer;
import me.cychen.rts.simulator.TaskSetGenerator;
import me.cychen.rts.util.ExcelLogHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by jjs on 2/13/17.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger("Main");

    public static void main(String[] args) {
        logger.info("Test starts.");

        // Generate a task set.
        TaskSetGenerator taskSetGenerator = new TaskSetGenerator();
        taskSetGenerator.setMaxUtil(0.5);
        taskSetGenerator.setMinUtil(0.4);
        TaskSetContainer taskSets = taskSetGenerator.generate(5, 1);
        TaskSet taskSet = taskSets.getTaskSets().get(0);
        logger.info(taskSet.toString());

        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskSet);

        // Run simulation.
        rmSimulator.runSim(1000);
        EventContainer eventContainer = rmSimulator.getSimEventContainer();

        BusyIntervalEventContainer biEvents = new BusyIntervalEventContainer();
        biEvents.createBusyIntervalsFromEvents(eventContainer);

        ExcelLogHandler excelLogHandler = new ExcelLogHandler();
        excelLogHandler.genRowSchedulerIntervalEvents(eventContainer);
        excelLogHandler.genRowBusyIntervals(biEvents);
        excelLogHandler.saveAndClose(null);

        logger.info(eventContainer.getAllEvents());
    }
}
