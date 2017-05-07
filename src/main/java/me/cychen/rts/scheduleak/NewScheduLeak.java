package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.util.Umath;

import java.util.ArrayList;

/**
 * Created by cy on 5/3/2017.
 */
public class NewScheduLeak {
    IntermittentInterval arrivalWindow;
    Task observer, victim;
    long inferredArrivalTime = 0;


    /* NOT IN ALGO: */
    /* Trace Variables */
    TaskSet taskSet;
    long trueArrivalTime;
    public Boolean isArrivalTimeInferredCorrectly = false;
    public Boolean isArrivalWindowCleared = false;
    public long inferenceSuccessTime = 0;
    public long arrivalWindowClearedTime = 0;
    public double inferencePrecision = 0;

    public NewScheduLeak(Task observer, Task victim, TaskSet inTaskSet) {
        taskSet = inTaskSet;    // for analysis only.
        this.observer = observer;
        this.victim = victim;
        trueArrivalTime = victim.getInitialOffset();    // for analysis only.
        arrivalWindow = new IntermittentInterval(new Interval(0, victim.getPeriod()));   // Initialize the inference window with the victim's period.
    }

    protected void minusArrivalWindow(Interval inInterval) {
        Interval shiftedInterval = new Interval(inInterval);
        long shiftValue = inInterval.getBegin()/victim.getPeriod();
        shiftValue = shiftValue * victim.getPeriod();
        shiftedInterval.shift(-shiftValue);
        arrivalWindow.minus(shiftedInterval);

        /* If the length of the given interval is greater than one period: */
        while (shiftedInterval.getEnd() > victim.getPeriod()) {
            shiftedInterval.shift(-victim.getPeriod());
            arrivalWindow.minus(shiftedInterval);
        }
    }

    public void computeArrivalWindowFromObserverSchedulerEvents(ArrayList<SchedulerIntervalEvent> inSchedulerEvents) {
        long lastTimestamp = 0;
        long firstTimestamp = 0;
        Boolean firstLoop = true;
        for (SchedulerIntervalEvent thisEvent : inSchedulerEvents) {
            if (thisEvent.getTask() != observer) {
                continue;
            }

            if (firstLoop == true) {
                firstLoop = false;
                firstTimestamp = thisEvent.getOrgBeginTimestamp();
            }

            if (thisEvent.getOrgBeginTimestamp() < lastTimestamp) {
                throw new AssertionError(thisEvent.getOrgBeginTimestamp() + " should be greater than " + lastTimestamp);
            } else {
                lastTimestamp = thisEvent.getOrgBeginTimestamp();
            }

            Interval thisEventInterval = new Interval(thisEvent.getOrgBeginTimestamp(), thisEvent.getOrgEndTimestamp());
            minusArrivalWindow(thisEventInterval);

            /* NOT IN ALGO: */
            /* Check with ground truth */
            if (isArrivalTimeInferredCorrectly == false) {
                if (inferArrivalTime() == trueArrivalTime) {
                    isArrivalTimeInferredCorrectly = true;
                    inferenceSuccessTime = thisEvent.getOrgEndTimestamp() - firstTimestamp;
                }
            }
            if (isArrivalWindowCleared == false) {
                if (isArrivalWindowCleared() == true) {
                    isArrivalWindowCleared = true;
                    arrivalWindowClearedTime = thisEvent.getOrgEndTimestamp() - firstTimestamp;
                }
            }
        }

        inferredArrivalTime = inferArrivalTime();
    }

    public IntermittentInterval getArrivalWindow() {
        return arrivalWindow;
    }

    public long inferArrivalTime() {
        if (arrivalWindow.intervals.size() == 0) {
            return 0;
        } else {

            /* Check if the 2 left intervals are actually continuous. */
            if (arrivalWindow.intervals.size() == 2) {
                if ( (arrivalWindow.getEnd()==victim.getPeriod()) && (arrivalWindow.getBegin()==0)) {
                    if (arrivalWindow.intervals.get(0).getBegin() != 0) {
                        return arrivalWindow.intervals.get(0).getBegin();
                    } else {
                        return arrivalWindow.intervals.get(1).getBegin();
                    }
                }
            }

            return arrivalWindow.getLongestInterval().getBegin();
        }
    }

//    public Boolean isArrivalWindowCorrectRough() {
//        if (arrivalWindow.intervals.size() != 1) {
//            return false;
//        }
//
//        if (arrivalWindow.getBegin() == victim.getInitialOffset()) {
//            return true;
//        }
//
//        return false;
//    }
//
    public Boolean isArrivalWindowCleared() {
        if (arrivalWindow.intervals.size() != 1) {
            return false;
        }

        if ( (arrivalWindow.getBegin()==victim.getInitialOffset()) && (arrivalWindow.intervals.get(0).getLength()==victim.getExecTime())) {
            return true;
        }

        return false;
    }

    public double computeArrivalInferencePrecision() {
        long pv = victim.getPeriod();
        long trueArrivalTime = victim.getInitialOffset();
        long delta = Math.abs(trueArrivalTime - inferredArrivalTime);
        if (delta > (pv/2.0)) {
            inferencePrecision =  1- ((pv - delta)/(pv/2.0));
        } else {
            inferencePrecision =  1- (delta/(pv/2.0));
        }
        return inferencePrecision;
    }

    public double computeProbabilityOfAnyTaskBeingObservedAtASlot() {
        long po = observer.getPeriod();
        long pv = victim.getPeriod();
        long lcmPoPv = Umath.lcm(po, pv);
        long gcdPoPv = Umath.gcd(po, pv);

        double antiOohp = 1;    // antiOohp is the probability of no any task being observed at a slot in a lcmPoPv period
        for (Task thisTask : taskSet.getHigherPriorityTasks(observer.getPriority())) {
            if (thisTask == victim) {
                continue;
            }

            long ph = thisTask.getPeriod();
            long eh = thisTask.getExecTime();
            long lcmPoPh = Umath.lcm(po, ph);
            long lcmPoPvPh = Umath.lcm(lcmPoPh, pv);
            long gcdPoPh = Umath.gcd(po, ph);
            double observationRatio = (double) observer.getExecTime()/(double)gcdPoPv;

            long hWcrt = taskSet.calc_WCRT(thisTask);
//            double Ooh = (double)hWcrt/(double)(lcmPoPh); // ooh is the probability of the task h bing observed at a slot in a lcmPoPv period.
//            if (Ooh > 1.0) {
//                antiOohp = Ooh;
//            }
            double Ooh = (double)lcmPoPv/(double)(lcmPoPvPh);

//            if (observationRatio>1.0) {
//                Ooh = Ooh/observationRatio;
//            }

            antiOohp *= (1.0-Ooh);
        }
        return 1.0-antiOohp;
    }

    public double computeConfidenceLevelOverLcmPoPvTime(long inLcmPoPvTime) {
        double Oohp = computeProbabilityOfAnyTaskBeingObservedAtASlot();

        if (Oohp >= 1) {
            // This means that some h tasks can appear at the same frequency as the victim task, so it's impossible to solve the problem.
            return 0;
        }

        double antiConfidence = 1;
        for (long t=0; t<inLcmPoPvTime; t++) {
            antiConfidence *= Oohp;
        }
        double confidenceLevel = 1- antiConfidence;
        return confidenceLevel;
    }

    public long computeLcmPoPvTimesByConfidenceLevel(double inConfidence) {
        if (inConfidence<=0 || inConfidence>=1) {
            return 0;
        }

        double Oohp = computeProbabilityOfAnyTaskBeingObservedAtASlot();
        if (Oohp >= 1) {
            // This means that some h tasks can appear at the same frequency as the victim task, so it's impossible to solve the problem.
            return 0;   // Note that only this case it returns 0, otherwise this function will return the number >= 1.
        }

        double currentConfidence = 0;
        double lastAntiConfidence = 1;
        long time = 0;
        while (currentConfidence < inConfidence) {
            time++;
            lastAntiConfidence *= Oohp;
            currentConfidence = 1.0 - lastAntiConfidence;
        }
        return time;
    }
}
