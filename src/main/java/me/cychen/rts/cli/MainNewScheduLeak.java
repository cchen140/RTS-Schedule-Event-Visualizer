package me.cychen.rts.cli;

import me.cychen.rts.event.EventContainer;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.NewScheduLeak;
import me.cychen.rts.scheduleak.test.SingleNewScheduLeakTest;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.rts.simulator.TaskSetContainer;
import me.cychen.rts.simulator.TaskSetGenerator;
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by cy on 5/3/2017.
 */
public class MainNewScheduLeak {
    private static final int OBSERVER_PRI = 1;
    private static final int VICTIM_PRI = 2;

    private static long SIM_DURATION = 500000;

    private static final Logger loggerConsole = LogManager.getLogger("console");

    public static void main(String[] args) {


        // Generate a task set.
        TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

        //taskSetGenerator.setMaxPeriod(100);
        //taskSetGenerator.setMinPeriod(50);

        //taskSetGenerator.setMaxExecTime(20);
        //taskSetGenerator.setMinExecTime(5);

        taskSetGenerator.setMaxUtil(0.6);
        taskSetGenerator.setMinUtil(0.5);

        taskSetGenerator.setNonHarmonicOnly(true);

        taskSetGenerator.setMaxHyperPeriod(2*3*5*7*11*13*15);
        taskSetGenerator.setGenerateFromHpDivisors(true);

        /* Optimal attack condition experiment. */
        taskSetGenerator.setNeedGenObserverTask(true);
        taskSetGenerator.setObserverTaskPriority(OBSERVER_PRI);
        taskSetGenerator.setVictimTaskPriority(VICTIM_PRI);

        taskSetGenerator.setMaxObservationRatio(999);
        taskSetGenerator.setMinObservationRatio(1);

        TaskSetContainer taskSets = taskSetGenerator.generate(15, 1);

        TaskSet taskSet = taskSets.getTaskSets().get(0);
        long hyperPeriod = taskSet.calHyperPeriod();

        // victim and observer task
        Task observer = taskSet.getOneTaskByPriority(OBSERVER_PRI);
        Task victim = taskSet.getOneTaskByPriority(VICTIM_PRI);

        double gcd = Umath.gcd(victim.getPeriod(), observer.getPeriod());
        double lcm = Umath.lcm(victim.getPeriod(), observer.getPeriod());
        double observationRatio = observer.getExecTime() / gcd;


        SingleNewScheduLeakTest singleNewScheduLeakTest = new SingleNewScheduLeakTest(taskSet, observer, victim);
        singleNewScheduLeakTest.run();

    }


}
