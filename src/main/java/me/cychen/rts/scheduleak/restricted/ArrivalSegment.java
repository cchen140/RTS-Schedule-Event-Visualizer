package me.cychen.rts.scheduleak.restricted;

import me.cychen.rts.scheduleak.Interval;

import java.util.ArrayList;

/**
 * Created by CY on 10/31/2015.
 */
@Deprecated
public class ArrivalSegment extends Interval {
    public static final int ZERO_ARRIVAL_SEGMENT = 0;
    public static final int ONE_ARRIVAL_SEGMENT = 1;
    public static final int ZERO_ONE_ARRIVAL_SEGMENT = 2;

    private int segmentType = ZERO_ARRIVAL_SEGMENT;

    public ArrivalSegment(ArrivalSegment inSource) {
        super(inSource);
        segmentType = inSource.segmentType;
    }

    public ArrivalSegment(long inBegin, long inEnd, int inSegmentType) {
        super(inBegin, inEnd);
        segmentType = inSegmentType;
    }

    public int getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(int segmentType) {
        this.segmentType = segmentType;
    }

//    // Integer type for the returned value is used because "null" will be returned if no intersection is found.
//    public static Integer findSmallestPeriodShiftValueWithIntersection(Interval shiftingInterval, Interval fixedInterval, int inPeriod)
//    {
//        int periodShiftValue = (fixedInterval.getBegin()-shiftingInterval.getBegin()) / inPeriod;
//
//        Interval newInstanceShiftingInterval = new Interval(shiftingInterval);
//        newInstanceShiftingInterval.shift(periodShiftValue*inPeriod);
//
//        /* Check whether the intersection exists. */
//        if (fixedInterval.intersect(newInstanceShiftingInterval) != null)
//        {// Has intersection.
//            return periodShiftValue;
//        }
//
//
//        /* Shift one more to see if they have intersection. */
//        if (fixedInterval.getBegin() >= shiftingInterval.getBegin()) {
//            periodShiftValue++;
//            newInstanceShiftingInterval.shift(inPeriod);
//        } else {
//            periodShiftValue--;
//            newInstanceShiftingInterval.shift(-inPeriod);
//        }
//
//        if (fixedInterval.intersect(newInstanceShiftingInterval) != null) {
//            // Has intersection.
//            return periodShiftValue;
//        } else {
//            return null;
//        }
//    }

    public ArrayList<Interval> getIntersectionWithPeriodShift(Interval fixedInterval, long inPeriod) {
        ArrayList<Interval> resultIntersections = new ArrayList<>();

        long periodShiftValue = (fixedInterval.getBegin()-this.getBegin()) / inPeriod;

        Interval instanceOfMe = new Interval(this);
        instanceOfMe.shift(periodShiftValue * inPeriod);

        /* Note that the intersection could be null here. */
        Interval result = fixedInterval.intersect(instanceOfMe);
        if (result != null)
            resultIntersections.add( result );

        /* Shift one more to see if they have intersection. */
        if (fixedInterval.getBegin() >= instanceOfMe.getBegin()) {
            periodShiftValue++;
            instanceOfMe.shift(inPeriod);
        } else {
            periodShiftValue--;
            instanceOfMe.shift(-inPeriod);
        }

        /* Note that the intersection could be null here. */
        result = fixedInterval.intersect(instanceOfMe);
        if (result != null)
            resultIntersections.add( result );

        return resultIntersections;
    }

}