package me.cychen.rts.scheduleak.test;

import me.cychen.rts.event.EventContainer;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.NewScheduLeak;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by cy on 5/3/2017.
 */
public class SingleNewScheduLeakTest {
    private static final Logger loggerConsole = LogManager.getLogger("console");

    TaskSet taskSet;
    Task observer, victim;

    /* Trace data */
    public NewScheduLeak scheduLeak = null;

    public SingleNewScheduLeakTest(TaskSet taskSet, Task observer, Task victim) {
        this.taskSet = taskSet;
        this.observer = observer;
        this.victim = victim;
    }

    public void run() {
        long hyperPeriod = taskSet.calHyperPeriod();

        // victim and observer task
        //Task observer = taskSet.getOneTaskByPriority(OBSERVER_PRI);
        //Task victim = taskSet.getOneTaskByPriority(VICTIM_PRI);

        double gcd = Umath.gcd(victim.getPeriod(), observer.getPeriod());
        double lcm = Umath.lcm(victim.getPeriod(), observer.getPeriod());
        double observationRatio = observer.getExecTime() / gcd;


        /*====== Sim schedule =====*/
        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskSet);

        // Pre-schedule
        long simDuration = hyperPeriod*2;
        QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(simDuration);//SIM_DURATION);

        // Run simulation.
        rmSimulator.simJobs(simJobContainer);
        EventContainer eventContainer = rmSimulator.getSimEventContainer();
        eventContainer.removeEventsBeforeButExcludeTimeStamp(hyperPeriod);

        /*===== Run ScheduLeak =====*/
        scheduLeak = new NewScheduLeak(observer, victim);
        scheduLeak.computeArrivalWindowFromObserverSchedulerEvents(eventContainer.getSchedulerEventsOfATask(observer));
        scheduLeak.computeArrivalInferencePrecision();  // The precision value is stored in a trace variable.

        /*===== Output results =====*/
//        loggerConsole.info(taskSet.toString());
//        loggerConsole.info("Observer Task: " + observer.toString());
//        loggerConsole.info("Victim Task: " + victim.toString());
//        loggerConsole.info("O(ov) = " + observer.getExecTime()/gcd);
//        loggerConsole.info("Sim time = " + (simDuration-hyperPeriod)/lcm);
//        loggerConsole.info("Answer Obtained at " + scheduLeak.inferenceSuccessTime/lcm);
//        loggerConsole.info(scheduLeak.getArrivalWindow().toString());
    }
}
