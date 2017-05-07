package me.cychen.rts.cli;

import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.test.SingleNewScheduLeakTest;
import me.cychen.rts.simulator.TaskSetContainer;
import me.cychen.rts.simulator.TaskSetGenerator;
import me.cychen.util.Umath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;

/**
 * Created by cy on 5/3/2017.
 */
public class MainNewScheduLeakMassiveTest {
    //private static long SIM_DURATION = 500000;
    private static long NUM_OF_TEST_PER_CONDITION = 100;

    private static final Logger loggerConsole = LogManager.getLogger("console");
    private static final Logger loggerExp_by_taskset = LogManager.getLogger("exp_by_taskset");
    private static final Logger loggerExp_by_util = LogManager.getLogger("exp_by_util");
    private static final Logger loggerExp_by_exp_by_numOfTasksPerTaskset = LogManager.getLogger("exp_by_numOfTasksPerTaskset");

    public static void main(String[] args) {

        /* Title row */
        loggerExp_by_taskset.trace("#util \t O \t precision \t successTimeByLcm \t successTimeByHpRatio");
        loggerExp_by_util.trace("#util \t averagePrecision \t successRate \t averageSuccessTimeByLcm \t averageSuccessTimeByHpRatio");
        loggerExp_by_exp_by_numOfTasksPerTaskset.trace("#numOfTasks \t averagePrecision \t successRate");

        for (int victimPriorityMode=1; victimPriorityMode<=3 ; victimPriorityMode++) {
            loggerConsole.info("##### victim priority mode = " + victimPriorityMode + " #####");

            loggerExp_by_taskset.trace("\r\n\r\n#victimMode={}", victimPriorityMode);
            loggerExp_by_util.trace("\r\n\r\n#victimMode={}", victimPriorityMode);
            loggerExp_by_exp_by_numOfTasksPerTaskset.trace("\r\n\r\n#victimMode={}", victimPriorityMode);

            //int victimPriorityMode=1;
            int observerPriority = 1;
            int victimPriority = 0; // to be computed later.

            /* Number of tasks loop*/
            for (int numOfTasks = 5; numOfTasks <= 15; numOfTasks += 2) {
                loggerConsole.info("##### num of tasks = " + numOfTasks + " #####");

                loggerExp_by_util.trace("\r\n\r\n#{} tasks per taskset", numOfTasks);

                /* Utilization loop */
                double cumulativePrecisionForNumOfTasks = 0;
                int cumulativeSuccessCount = 0;
                for (double util = 0.001; util < 1; util += 0.1) {
                    loggerConsole.info("##### util = " + doubleToString(util) + " #####");


                    /* Test per condition */
                    double cumulativeInferencePrecision = 0;
                    double cumulativeSuccessTimeByLcm = 0;
                    double cumulativeSuccessTimeByHpRatio = 0;
                    int successCount = 0;
                    for (int testId = 1; testId <= NUM_OF_TEST_PER_CONDITION; testId++) {
                        loggerConsole.info("#" + testId + " begins:");

                        // Generate a task set.
                        TaskSetGenerator taskSetGenerator = new TaskSetGenerator();

                        //taskSetGenerator.setMaxPeriod(100);
                        //taskSetGenerator.setMinPeriod(50);

                        //taskSetGenerator.setMaxExecTime(20);
                        //taskSetGenerator.setMinExecTime(5);

                        taskSetGenerator.setMaxUtil(util + 0.1);
                        taskSetGenerator.setMinUtil(util);

                        taskSetGenerator.setNonHarmonicOnly(true);

                        //taskSetGenerator.setMaxHyperPeriod(2 * 3 * 5 * 7 * 11 * 13 * 15);
                        //taskSetGenerator.setGenerateFromHpDivisors(true);

                        /* Optimal attack condition experiment. */
                        taskSetGenerator.setNeedGenObserverTask(true);
                        taskSetGenerator.setObserverTaskPriority(observerPriority);
                        switch (victimPriorityMode) {
                            case 1:
                                victimPriority = 2;
                                break;
                            case 2:
                                victimPriority = (int) Math.ceil(numOfTasks / 2);
                                break;
                            case 3:
                                victimPriority = numOfTasks;
                                break;
                            default:
                                throw new AssertionError("Victim task's priority is not given.");
                        }

                        taskSetGenerator.setVictimTaskPriority(victimPriority);


                        taskSetGenerator.setMaxObservationRatio(2); // Warning!!!!!!!!
                        taskSetGenerator.setMinObservationRatio(1);

                        TaskSetContainer taskSets = taskSetGenerator.generate(numOfTasks, 1);

                        TaskSet taskSet = taskSets.getTaskSets().get(0);
                        long hyperPeriod = taskSet.calHyperPeriod();

                        // victim and observer task
                        Task observer = taskSet.getOneTaskByPriority(observerPriority);
                        Task victim = taskSet.getOneTaskByPriority(victimPriority);

                        double gcd = Umath.gcd(victim.getPeriod(), observer.getPeriod());
                        double lcm = Umath.lcm(victim.getPeriod(), observer.getPeriod());
                        double observationRatio = observer.getExecTime() / gcd;

                        loggerConsole.info("\t Util = " + doubleToString(taskSet.getUtilization()));
                        loggerConsole.info("\t O(o,v) = " + observationRatio);

                        SingleNewScheduLeakTest singleNewScheduLeakTest = new SingleNewScheduLeakTest(taskSet, observer, victim);
                        singleNewScheduLeakTest.desiredConfidentLevel = 0.95;
                        singleNewScheduLeakTest.run(0);

                        double inferencePrecision = singleNewScheduLeakTest.scheduLeak.inferencePrecision;
                        double successTimeByLcm = singleNewScheduLeakTest.scheduLeak.inferenceSuccessTime/lcm;
                        double successTimeByHpRatio = singleNewScheduLeakTest.scheduLeak.inferenceSuccessTime/(double)hyperPeriod;

                        //loggerExp_by_taskset.trace("util \t O \t precision \t successTimeByLcm \t successTimeByHpRatio");
                        loggerExp_by_taskset.trace("\r\n{}\t{}\t{}\t{}\t{}", doubleToString(util), doubleToString(observationRatio), doubleToString(inferencePrecision), doubleToString(successTimeByLcm), doubleToString(successTimeByHpRatio));

                        /* Success count and cumulative time */
                        if (singleNewScheduLeakTest.scheduLeak.inferencePrecision == 1.0) {
                            successCount++;
                            cumulativeSuccessTimeByLcm += singleNewScheduLeakTest.scheduLeak.inferenceSuccessTime/lcm;
                            cumulativeSuccessTimeByHpRatio += successTimeByHpRatio;
                        }

                        cumulativeInferencePrecision += singleNewScheduLeakTest.scheduLeak.inferencePrecision;
                    }/* Test per condition */

                    double averagePrecisionForUtil = cumulativeInferencePrecision/(double)NUM_OF_TEST_PER_CONDITION;
                    double averageSuccessTimeByLcm = successCount==0 ? 0 : cumulativeSuccessTimeByLcm/(double)successCount;
                    double averageSuccessTimeByHpRatio = successCount==0 ? 0 : cumulativeSuccessTimeByHpRatio/(double)successCount;
                    double successRate = successCount/(double)NUM_OF_TEST_PER_CONDITION;

                    //loggerExp_by_util.trace("util \t averagePrecision \t successRate \t averageSuccessTimeByLcm \t averageSuccessTimeByHpRatio");
                    loggerExp_by_util.trace("\r\n{}\t{}\t{}\t{}\t{}", doubleToString(util), doubleToString(averagePrecisionForUtil), doubleToString(successRate), doubleToString(averageSuccessTimeByLcm), doubleToString(averageSuccessTimeByHpRatio));

                    cumulativeSuccessCount += successCount;
                    cumulativePrecisionForNumOfTasks += averagePrecisionForUtil;
                }/* Utilization loop */

                double averagePrecisionForeNumOfTasks = cumulativePrecisionForNumOfTasks/10.0;
                double successRate = cumulativeSuccessCount/(NUM_OF_TEST_PER_CONDITION*10.0);

                //loggerExp_by_exp_by_numOfTasksPerTaskset.trace("numOfTasks \t averagePrecision \t successRate");
                loggerExp_by_exp_by_numOfTasksPerTaskset.trace("\r\n{}\t{}\t{}", numOfTasks, doubleToString(averagePrecisionForeNumOfTasks), doubleToString(successRate));
            }/* Number of tasks loop*/

        }

    }

    public static String doubleToString(double util) {
        return (new DecimalFormat("##.##").format(util));
    }

}
