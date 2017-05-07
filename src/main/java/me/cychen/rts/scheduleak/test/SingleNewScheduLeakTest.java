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

    public long simDuration;
    public double desiredConfidentLevel = 0;

    /* Trace data */
    public NewScheduLeak scheduLeak = null;

    public SingleNewScheduLeakTest(TaskSet taskSet, Task observer, Task victim) {
        this.taskSet = taskSet;
        this.observer = observer;
        this.victim = victim;
    }

    public void run(long inSimTimeByLcmPoPv) {
        long hyperPeriod = taskSet.calHyperPeriod();
        simDuration = hyperPeriod*2;

        // victim and observer task
        //Task observer = taskSet.getOneTaskByPriority(OBSERVER_PRI);
        //Task victim = taskSet.getOneTaskByPriority(VICTIM_PRI);

        double gcdPoPv = Umath.gcd(victim.getPeriod(), observer.getPeriod());
        long lcmPoPv = Umath.lcm(victim.getPeriod(), observer.getPeriod());
        double observationRatio = observer.getExecTime() / gcdPoPv;


        // Initialize ScheduLeak first to obtain
        scheduLeak = new NewScheduLeak(observer, victim, taskSet);
        long simTimeByLcmPoPv = 0;
        if (desiredConfidentLevel > 0) {
            simTimeByLcmPoPv = scheduLeak.computeLcmPoPvTimesByConfidenceLevel(desiredConfidentLevel);

            if (simTimeByLcmPoPv == 0) {
                // It means that in the worst case there is no way to solve the problem.
                // We will still give it a shot.
                simDuration = 20 * lcmPoPv;
            } else {
                simDuration = simTimeByLcmPoPv * lcmPoPv;
            }
        }

        // If sim time is given, then use that instead.
        if (inSimTimeByLcmPoPv > 0) {
            simDuration = inSimTimeByLcmPoPv * lcmPoPv;
        }

        /*====== Sim schedule =====*/
        // New and configure a RM scheduling simulator.
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskSet);

        // Pre-schedule
        //long simDuration = hyperPeriod*2;

        QuickFPSchedulerJobContainer simJobContainer = rmSimulator.preSchedule(simDuration);//SIM_DURATION);

        // Run simulation.
        rmSimulator.simJobs(simJobContainer);
        EventContainer eventContainer = rmSimulator.getSimEventContainer();
        if (desiredConfidentLevel == 0) {
            eventContainer.removeEventsBeforeButExcludeTimeStamp(hyperPeriod);
        }

        /*===== Run ScheduLeak =====*/
        scheduLeak.computeArrivalWindowFromObserverSchedulerEvents(eventContainer.getSchedulerEventsOfATask(observer));
        scheduLeak.computeArrivalInferencePrecision();  // The precision value is stored in a trace variable.


        /*===== Output results =====*/
//        loggerConsole.info(taskSet.toString());
//        loggerConsole.info("Observer Task: " + observer.toString());
//        loggerConsole.info("Victim Task: " + victim.toString());
//        loggerConsole.info("O(ov) = " + observer.getExecTime()/gcdPoPv);
//        loggerConsole.info("Sim time = " + (simDuration/lcmPoPv));//(simDuration-hyperPeriod)/lcmPoPv);
//        loggerConsole.info("Inference Ratio = {}", scheduLeak.inferencePrecision);
//        loggerConsole.info("Answer Obtained at " + scheduLeak.inferenceSuccessTime/lcmPoPv);
//        loggerConsole.info(scheduLeak.getArrivalWindow().toString());
//        loggerConsole.info("time to {}: {}", desiredConfidentLevel, simTimeByLcmPoPv);
//
//        for (long i=0; i<10; i++) {
//            System.out.println(scheduLeak.computeConfidenceLevelOverLcmPoPvTime(i));
//        }
    }
}
