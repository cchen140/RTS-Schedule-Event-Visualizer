package me.cychen.rts.scheduleak.restricted;

import me.cychen.rts.framework.Task;
import me.cychen.rts.scheduleak.Interval;

import java.util.ArrayList;

/**
 * Created by CY on 10/31/2015.
 */
public class ArrivalSegmentsContainer {
    /* inputs from constructor */
    private BusyIntervalContainer biContainer;
    private Task task;

    private ArrayList<ArrivalSegment> arrivalSegments = new ArrayList<>();
    private ArrayList<Interval> finalArrivalTimeWindows;

    public ArrivalSegmentsContainer(Task inTask, BusyIntervalContainer inBiContainer){
        biContainer = inBiContainer;
        task = inTask;
    }

    public Boolean calculateFinalArrivalTimeWindow() throws RuntimeException {

        convertBusyIntervalsToArrivalSegments();

        ArrayList<Interval> arrivalWindows = computeArrivalWindowsFromArrivalSegmentsByWeight();   // This generates task's arrival windows and puts into arrivalIntersections.

        /* Move the window to around zero point. */
        for (int i=0; i<arrivalWindows.size(); i++) {
            arrivalWindows.get(i).shift(-(arrivalWindows.get(i).getBegin() / task.getPeriod()) * task.getPeriod());
        }
        finalArrivalTimeWindows = arrivalWindows;

        if (finalArrivalTimeWindows.size() > 0) {
            return true;
        } else {
            // The program will not reach here since createArrivalIntersectionsByPeriod() will throw the exception ]
            // in the case when a task's arrival window becomes null.
            throw new RuntimeException(String.format("%s has 0 arrival window. It shouldn't happen.", task.getTitle()));
            //return false;
        }
    }

    private Boolean convertBusyIntervalsToArrivalSegments() {
        if (biContainer == null)
            return false;

        // Reset related variables.
        arrivalSegments.clear();

        /* Partitions busy intervals into arrival segments (1-seg and 0-1-seg) */
        for (BusyInterval thisBi : biContainer.getBusyIntervals()) {
            arrivalSegments.addAll(getArrivalSegmentsInBusyIntervalForTask_Improved(thisBi));
            //arrivalSegments.addAll(getArrivalSegmentsInBusyIntervalForTask(thisBi));
        }

        return true;
    }

    private ArrayList<ArrivalSegment> getArrivalSegmentsInBusyIntervalForTask_Improved(BusyInterval inBusyInterval)
    {
        ArrayList<ArrivalSegment> resultArrivalSegments = new ArrayList<>();

        long taskP = task.getPeriod();
        //int taskC = task.getComputationTimeNs();
        long thisCLower = task.getExecTimeLowerBound();
        //int thisCUpper = task.getComputationTimeUpperBound();
        long biDuration = inBusyInterval.getIntervalNs();

        if (inBusyInterval.getNkValuesOfTask(task).size() == 1) {
            int thisNkValue = inBusyInterval.getMinNkValueOfTask(task);
            if ( (int)(Math.ceil((double)biDuration/(double)taskP)) == thisNkValue) {
                for (int i=0; i<thisNkValue; i++) {
                    /* Create the arrival segment for every period in this busy interval. */
                    long resultBeginTime = inBusyInterval.getBeginTimeStampNs() + taskP*i;
                    //int resultEndTime = inBusyInterval.getEndTimeStampNs() - taskP*(thisNkValue - (i+1)) - taskC;
                    long resultEndTime = inBusyInterval.getEndTimeStampNs() - taskP*(thisNkValue - (i+1)) - thisCLower;

                    /* If the end time is earlier than the begin time, then drop this segment.
                     * This could happen due to too large negative deviation in this execution time. */
                    if (resultBeginTime<=resultEndTime) {
                        resultArrivalSegments.add(new ArrivalSegment(resultBeginTime, resultEndTime, ArrivalSegment.ONE_ARRIVAL_SEGMENT));
                    } else {
                        //ProgMsg.errPutline("We have dropped a segment due to negative window length.");
                    }
                }
            } else {
                for (int i=0; i<thisNkValue; i++) {
                    /* Create the arrival segment for every period in this busy interval. */
                    long resultBeginTime = inBusyInterval.getEndTimeStampNs() - taskP*(thisNkValue+1-(i+1));
                    //int resultEndTime = inBusyInterval.getBeginTimeStampNs() + taskP*(i+1) - taskC;
                    long resultEndTime = inBusyInterval.getBeginTimeStampNs() + taskP*(i+1) - thisCLower;

                    /* If the end time is earlier than the begin time, then drop this segment.
                     * This could happen due to too large negative deviation in this execution time. */
                    if (resultBeginTime<=resultEndTime) {
                        resultArrivalSegments.add(new ArrivalSegment(resultBeginTime, resultEndTime, ArrivalSegment.ONE_ARRIVAL_SEGMENT));
                    } else {
                        //ProgMsg.errPutline("We have dropped a segment due to negative window length.");
                    }
                }
            }
        } else {
            /* Deal with the certain part first. */
            int thisNkValue = inBusyInterval.getMinNkValueOfTask(task);
            for (int i=0; i<thisNkValue; i++) {
                /* Create the arrival segment for every period in this busy interval. */
                long resultBeginTime = inBusyInterval.getBeginTimeStampNs() + taskP*i;
                //int resultEndTime = inBusyInterval.getBeginTimeStampNs() + taskP*(i+1) - taskC;
                long resultEndTime = inBusyInterval.getBeginTimeStampNs() + taskP*(i+1) - thisCLower;

                /* If the end time is earlier than the begin time, then drop this segment.
                 * This could happen due to too large negative deviation in this execution time. */
                if (resultBeginTime<=resultEndTime) {
                    resultArrivalSegments.add(new ArrivalSegment(resultBeginTime, resultEndTime, ArrivalSegment.ONE_ARRIVAL_SEGMENT));
                } else {
                    //ProgMsg.errPutline("We have dropped a segment due to negative window length.");
                }
            }

            /* Create the arrival segment (0-1-segment) for the extra, uncertain one. */
            long nthPeriod = inBusyInterval.getMaxNkValueOfTask(task);
            long resultBeginTime = inBusyInterval.getBeginTimeStampNs() + taskP*(nthPeriod-1); // Note that nthPeriod is always >=1
            //int resultEndTime = inBusyInterval.getEndTimeStampNs() - taskC;
            long resultEndTime = inBusyInterval.getEndTimeStampNs() - thisCLower;

             /* If the end time is earlier than the begin time, then drop this segment.
              * This could happen due to too large negative deviation in this execution time. */
            if (resultBeginTime<=resultEndTime) {
                resultArrivalSegments.add(new ArrivalSegment(resultBeginTime, resultEndTime, ArrivalSegment.ZERO_ONE_ARRIVAL_SEGMENT));
            } else {
                //ProgMsg.errPutline("We have dropped a segment due to negative window length.");
            }
        }

        return resultArrivalSegments;
    }

    private ArrivalSegment getFirstOneArrivalSegment() {
        for (ArrivalSegment thisArrivalSegment : arrivalSegments) {
            if (thisArrivalSegment.getSegmentType() == ArrivalSegment.ONE_ARRIVAL_SEGMENT) {
                return thisArrivalSegment;
            }
        }
        return null;
    }

    private ArrayList<Interval> computeArrivalWindowsFromArrivalSegmentsByWeight() {
        long beginTimeStamp = findEarliestArrivalSegmentBeginTime();
        long endTimeStamp = findLeastArrivalSegmentEndTime();

        long taskP = task.getPeriod();

        // Make base interval as whole period.
        WeightedInterval baseWeightedInterval = new WeightedInterval(beginTimeStamp, beginTimeStamp+taskP);

        for (int i=0; (beginTimeStamp+taskP*(i+1)) < endTimeStamp; i++) {// i+1 is to skip the last period since it may not be complete
            ArrayList<ArrivalSegment> thisPeriodSegments = findArrivalSegmentsBetweenTimes(beginTimeStamp + taskP * i, beginTimeStamp + taskP * (i + 1) - 1);

            for (ArrivalSegment thisSegment : thisPeriodSegments) {

                ArrivalSegment clonedSegment = new ArrivalSegment(thisSegment);
                clonedSegment.shift(-(taskP * i));
                baseWeightedInterval.applyWeight(clonedSegment);
            }
        }

        ArrayList<Interval> rawResultWindows = baseWeightedInterval.getMostWeightedIntervals();
        ArrayList<Interval> mergedResultWindows = new ArrayList<>();

        /* Combine arrival intersections if they are actually continuous. */
        if (rawResultWindows.size() >= 2) {

            ArrayList<Interval> mergedRawResultWindows = new ArrayList<>();
            for (Interval firstLoopWindow : rawResultWindows) {

                if (mergedRawResultWindows.contains(firstLoopWindow)) {
                    // If this window has been merged in previous loops, then skip.
                    continue;
                }

                Boolean shouldStartComputing = false;
                Interval mergedInterval = null;
                for (Interval secondLoopWindow : rawResultWindows) {
                    if (firstLoopWindow == secondLoopWindow) {
                        shouldStartComputing = true;
                        continue;
                    }

                    if (shouldStartComputing == false) {
                        continue;
                    }

                    Long shiftValue = findSmallestPeriodShiftValueWithIntersection(firstLoopWindow, secondLoopWindow, taskP);
                    if (shiftValue != null) {
                        Interval tempFirstLoopWindow = new Interval(firstLoopWindow);
                        tempFirstLoopWindow.shift(shiftValue * taskP);
                        ArrayList<Interval> thisIntersections = tempFirstLoopWindow.union(secondLoopWindow);

                        if (thisIntersections.size() == 1) {
                            mergedInterval = thisIntersections.get(0);
                            mergedRawResultWindows.add(firstLoopWindow);
                            mergedRawResultWindows.add(secondLoopWindow);   // this is to avoid duplicate handles.
                            break;
                        }
                    }
                }
                if (mergedInterval != null) {
                    mergedResultWindows.add(mergedInterval);
                } else {
                    mergedResultWindows.add(firstLoopWindow);
                }
            }
        } else {
            mergedResultWindows.addAll(rawResultWindows);
        }

        return mergedResultWindows;
    }

    private long findEarliestArrivalSegmentBeginTime() {
        long earliestTimeStamp = 0;
        Boolean firstLoop = true;
        for (ArrivalSegment thisSegment : arrivalSegments) {
            if (firstLoop == true) {
                earliestTimeStamp = thisSegment.getBegin();
                firstLoop = false;
            }
            earliestTimeStamp = thisSegment.getBegin() < earliestTimeStamp ? thisSegment.getBegin() : earliestTimeStamp;
        }
        return earliestTimeStamp;
    }

    private long findLeastArrivalSegmentEndTime() {
        long leastEndTime = 0;
        for (ArrivalSegment thisSegment : arrivalSegments) {
            leastEndTime = thisSegment.getEnd() > leastEndTime ? thisSegment.getEnd() : leastEndTime;
        }
        return leastEndTime;
    }

    private ArrayList<ArrivalSegment> findArrivalSegmentsBetweenTimes(long begin, long end) {
        Interval range = new Interval(begin, end);
        ArrayList<ArrivalSegment> resultSegments = new ArrayList<>();
        for (ArrivalSegment thisSegment : arrivalSegments) {
            if (range.intersect(thisSegment) != null) {
                resultSegments.add(thisSegment);
            }
        }
        return resultSegments;
    }

    public ArrayList<Interval> getFinalArrivalTimeWindow() {
        return finalArrivalTimeWindows;
    }

    // Integer type for the returned value is used because "null" will be returned if no intersection is found.
    public Long findSmallestPeriodShiftValueWithIntersection(Interval shiftingInterval, Interval fixedInterval, long inPeriod)
    {
        long periodShiftValue = (fixedInterval.getBegin()-shiftingInterval.getBegin()) / inPeriod;

        Interval newInstanceShiftingInterval = new Interval(shiftingInterval);
        newInstanceShiftingInterval.shift(periodShiftValue*inPeriod);

        /* Check whether the intersection exists. */
        if (fixedInterval.intersect(newInstanceShiftingInterval) != null)
        {// Has intersection.
            return periodShiftValue;
        }


        /* Shift one more to see if they have intersection. */
        if (fixedInterval.getBegin() >= shiftingInterval.getBegin()) {
            periodShiftValue++;
            newInstanceShiftingInterval.shift(inPeriod);
        } else {
            periodShiftValue--;
            newInstanceShiftingInterval.shift(-inPeriod);
        }

        if (fixedInterval.intersect(newInstanceShiftingInterval) != null) {
            // Has intersection.
            return periodShiftValue;
        } else {
            return null;
        }
    }
}