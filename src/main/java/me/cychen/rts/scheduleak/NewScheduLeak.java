package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by cy on 5/3/2017.
 */
public class NewScheduLeak {
    IntermittentInterval arrivalWindow;
    Task observer, victim;
    long inferredArrivalTime = 0;
    long trueArrivalTime;

    /* NOT IN ALGO: */
    /* Trace Variables */
    public Boolean isArrivalTimeInferredCorrectly = false;
    public long inferenceSuccessTime = 0;
    public double inferencePrecision = 0;

    public NewScheduLeak(Task observer, Task victim) {
        this.observer = observer;
        this.victim = victim;
        trueArrivalTime = victim.getInitialOffset();
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
//    public Boolean isArrivalWindowCorrectWithoutVariation() {
//        if (arrivalWindow.intervals.size() != 1) {
//            return false;
//        }
//
//        if ( (arrivalWindow.getBegin()==victim.getInitialOffset()) && (arrivalWindow.intervals.get(0).getLength()==victim.getExecTime())) {
//            return true;
//        }
//
//        return false;
//    }

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
}
