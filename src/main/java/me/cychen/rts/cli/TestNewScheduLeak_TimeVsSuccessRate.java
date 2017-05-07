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
 * Created by cy on 5/6/2017.
 */
public class TestNewScheduLeak_TimeVsSuccessRate {

    //private static long SIM_DURATION = 500000;
    private static long NUM_OF_TEST_PER_CONDITION = 5;

    private static final Logger loggerConsole = LogManager.getLogger("console");
    private static final Logger loggerExp_by_taskset = LogManager.getLogger("exp_by_taskset");
    private static final Logger loggerExp_by_util = LogManager.getLogger("exp_by_util");
    private static final Logger loggerExp_by_exp_by_numOfTasksPerTaskset = LogManager.getLogger("exp_by_numOfTasksPerTaskset");
    private static final Logger loggerExp_by_exp_by_timeVsSuccessRate = LogManager.getLogger("exp_by_timeVsSuccessRate");

    public static void main(String[] args) {

        /* Title row */
        loggerExp_by_taskset.trace("util \t O \t precision \t simTimeByLcm \t successTimeByLcm \t successTimeByHpRatio \t predictedTimeToSuccess \t confidenceLevel");
        loggerExp_by_util.trace("#util \t averagePrecision \t successRate \t averageSuccessTimeByLcm \t averageSuccessTimeByHpRatio");
        loggerExp_by_exp_by_numOfTasksPerTaskset.trace("#numOfTasks \t averagePrecision \t successRate");
        loggerExp_by_exp_by_timeVsSuccessRate.trace("#time \t AnalyticalSuccessRate \t trueSuccessRate \t averagePrecisionRatio");

        for (int simTimeByLcmPoPv=1; simTimeByLcmPoPv<=20; simTimeByLcmPoPv++) {
            loggerConsole.info("##### sim time = " + simTimeByLcmPoPv + " #####");

            loggerExp_by_taskset.trace("\r\n\r\n#simTime={}", simTimeByLcmPoPv);
            loggerExp_by_util.trace("\r\n\r\n#simTime={}", simTimeByLcmPoPv);
            loggerExp_by_exp_by_numOfTasksPerTaskset.trace("\r\n\r\n#simTime={}", simTimeByLcmPoPv);

            double cumulativeConfidenceLevel = 0;
            double cumulativeSuccessCountBySimTime = 0;
            double cumulativePrecisionBySimTime = 0;
            for (int victimPriorityMode = 1; victimPriorityMode <= 3; victimPriorityMode++) {
            //{
            //    int victimPriorityMode=1;
                loggerConsole.info("##### victim priority mode = " + victimPriorityMode + " #####");

                loggerExp_by_taskset.trace("\r\n\r\n#victimMode={}", victimPriorityMode);
                loggerExp_by_util.trace("\r\n\r\n#victimMode={}", victimPriorityMode);
                loggerExp_by_exp_by_numOfTasksPerTaskset.trace("\r\n\r\n#victimMode={}", victimPriorityMode);

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
                            singleNewScheduLeakTest.run(simTimeByLcmPoPv);

                            double inferencePrecision = singleNewScheduLeakTest.scheduLeak.inferencePrecision;
                            double successTimeByLcm = singleNewScheduLeakTest.scheduLeak.inferenceSuccessTime / lcm;
                            double successTimeByHpRatio = singleNewScheduLeakTest.scheduLeak.inferenceSuccessTime / (double) hyperPeriod;
                            double analyticalConfidenceLevel = singleNewScheduLeakTest.scheduLeak.computeConfidenceLevelOverLcmPoPvTime(simTimeByLcmPoPv);
                            long predictedTimeToSuccess = singleNewScheduLeakTest.scheduLeak.computeLcmPoPvTimesByConfidenceLevel(0.95);

                            //loggerExp_by_taskset.trace("util \t O \t precision \t simTimeByLcm \t successTimeByLcm \t successTimeByHpRatio \t predictedTimeToSuccess \t confidenceLevel");
                            loggerExp_by_taskset.trace("\r\n");
                            loggerExp_by_taskset.trace("{}\t", doubleToString(taskSet.getUtilization()));
                            loggerExp_by_taskset.trace("{}\t", doubleToString(observationRatio));
                            loggerExp_by_taskset.trace("{}\t", doubleToString(inferencePrecision));
                            loggerExp_by_taskset.trace("{}\t", doubleToString(singleNewScheduLeakTest.simDuration/lcm));
                            loggerExp_by_taskset.trace("{}\t", doubleToString(successTimeByLcm));
                            loggerExp_by_taskset.trace("{}\t", doubleToString(successTimeByHpRatio));
                            loggerExp_by_taskset.trace("{}\t", predictedTimeToSuccess);
                            loggerExp_by_taskset.trace("{}\t", analyticalConfidenceLevel);

                            /* Success count and cumulative time */
                            if (singleNewScheduLeakTest.scheduLeak.inferencePrecision == 1.0) {
                                successCount++;
                                cumulativeSuccessTimeByLcm += singleNewScheduLeakTest.scheduLeak.inferenceSuccessTime / lcm;
                                cumulativeSuccessTimeByHpRatio += successTimeByHpRatio;
                            }

                            cumulativeInferencePrecision += singleNewScheduLeakTest.scheduLeak.inferencePrecision;
                            cumulativeConfidenceLevel += analyticalConfidenceLevel;
                        }/* Test per condition */

                        double averagePrecisionForUtil = cumulativeInferencePrecision / (double) NUM_OF_TEST_PER_CONDITION;
                        double averageSuccessTimeByLcm = successCount == 0 ? 0 : cumulativeSuccessTimeByLcm / (double) successCount;
                        double averageSuccessTimeByHpRatio = successCount == 0 ? 0 : cumulativeSuccessTimeByHpRatio / (double) successCount;
                        double successRate = successCount / (double) NUM_OF_TEST_PER_CONDITION;

                        //loggerExp_by_util.trace("util \t averagePrecision \t successRate \t averageSuccessTimeByLcm \t averageSuccessTimeByHpRatio");
                        loggerExp_by_util.trace("\r\n");
                        loggerExp_by_util.trace("{}\t", doubleToString(util));
                        loggerExp_by_util.trace("{}\t", doubleToString(averagePrecisionForUtil));
                        loggerExp_by_util.trace("{}\t", doubleToString(successRate));
                        loggerExp_by_util.trace("{}\t", doubleToString(averageSuccessTimeByLcm));
                        loggerExp_by_util.trace("{}\t", doubleToString(averageSuccessTimeByHpRatio));

                        cumulativeSuccessCount += successCount;
                        cumulativePrecisionForNumOfTasks += averagePrecisionForUtil;
                    }/* Utilization loop */

                    double averagePrecisionForeNumOfTasks = cumulativePrecisionForNumOfTasks / 10.0;
                    double successRate = cumulativeSuccessCount / (NUM_OF_TEST_PER_CONDITION * 10.0);

                    //loggerExp_by_exp_by_numOfTasksPerTaskset.trace("numOfTasks \t averagePrecision \t successRate");
                    loggerExp_by_exp_by_numOfTasksPerTaskset.trace("\r\n{}\t{}\t{}", numOfTasks, doubleToString(averagePrecisionForeNumOfTasks), doubleToString(successRate));

                    cumulativeSuccessCountBySimTime+= successRate;
                    cumulativePrecisionBySimTime+= averagePrecisionForeNumOfTasks;
                }/* Number of tasks loop*/

            }

            double averageConfidenceLevel = cumulativeConfidenceLevel/(NUM_OF_TEST_PER_CONDITION*10.0*6.0*1.0);
            double averageSuccessRate = cumulativeSuccessCountBySimTime/(6.0*3.0);
            double averagePrecisionBySimTime = cumulativePrecisionBySimTime/(6.0*3.0);

            //loggerExp_by_exp_by_timeVsSuccessRate.trace("#time \t AnalyticalSuccessRate \t trueSuccessRate \t averagePrecisionRatio");
            loggerExp_by_exp_by_timeVsSuccessRate.trace("\r\n");
            loggerExp_by_exp_by_timeVsSuccessRate.trace("{}\t", simTimeByLcmPoPv);
            loggerExp_by_exp_by_timeVsSuccessRate.trace("{}\t", averageConfidenceLevel);
            loggerExp_by_exp_by_timeVsSuccessRate.trace("{}\t", averageSuccessRate);
            loggerExp_by_exp_by_timeVsSuccessRate.trace("{}\t", averagePrecisionBySimTime);
        }

    }

    public static String doubleToString(double util) {
        return (new DecimalFormat("##.##").format(util));
    }

}
