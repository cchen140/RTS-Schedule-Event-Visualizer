package me.cychen.rts.scheduleak;

import java.util.ArrayList;

/**
 * Created by cy on 3/26/2017.
 */
public class IntermittentInterval {
    ArrayList<Interval> intervals = new ArrayList<>();

    public IntermittentInterval() {}

    public IntermittentInterval(ArrayList<Interval> inIntervals) {
        intervals.addAll(inIntervals);
    }

    public long getBegin() {
        long begin = 0;
        Boolean firstLoop = true;
        for (Interval thisInterval : intervals) {
            if (firstLoop) {
                firstLoop = false;
                begin = thisInterval.getBegin();
            } else {
                begin = thisInterval.getBegin() < begin ? thisInterval.getBegin() : begin;
            }
        }
        return begin;
    }

    public long getEnd() {
        long end = 0;
        Boolean firstLoop = true;
        for (Interval thisInterval : intervals) {
            if (firstLoop) {
                firstLoop = false;
                end = thisInterval.getEnd();
            } else {
                end = thisInterval.getEnd() > end ? thisInterval.getEnd() : end;
            }
        }
        return end;
    }

    public void shift(long inShift) {
        for (Interval thisInterval : intervals) {
            thisInterval.shift(inShift);
        }
    }

    public void union(Interval inInterval) {
        intervals = getUnion(inInterval).intervals;
    }

    public void union(IntermittentInterval interInterval) {
        intervals = getUnion(interInterval).intervals;
    }

    public IntermittentInterval getUnion(Interval inInterval) {

        // Check which intervals are intersected.
        ArrayList<Interval> intersectedIntervals = new ArrayList<>();
        for (Interval thisInterval : intervals) {
            if (thisInterval.intersect(inInterval) != null) {
                intersectedIntervals.add(thisInterval);
            }
        }

        // Create new getUnion interval if there is any intersection.
        if (intersectedIntervals.size() > 0) {
            Interval unionInterval = inInterval;
            for (Interval thisInterval : intersectedIntervals) {
                unionInterval = unionInterval.union(thisInterval).get(0);
            }

            // New object
            IntermittentInterval resultIntervals = new IntermittentInterval(intervals);
            resultIntervals.intervals.removeAll(intersectedIntervals);
            resultIntervals.intervals.add(unionInterval);
            return resultIntervals;
        } else {
            IntermittentInterval resultIntervals = new IntermittentInterval(intervals);
            resultIntervals.intervals.add(inInterval);
            return resultIntervals;
        }
    }

    public IntermittentInterval getUnion(IntermittentInterval interInterval) {
        IntermittentInterval resultInterInterval = interInterval;
        for (Interval thisInterval : intervals) {
            resultInterInterval = resultInterInterval.getUnion(thisInterval);
        }
        return resultInterInterval;
    }

    public void intersect(Interval inInterval) {
        intervals = getIntersection(inInterval).intervals;
    }

    public void intersect(IntermittentInterval interInterval) {
        intervals = getIntersection(interInterval).intervals;
    }

    public IntermittentInterval getIntersection(Interval inInterval) {
        // Check which intervals are intersected.
        ArrayList<Interval> intersectedIntervals = new ArrayList<>();
        for (Interval thisInterval : intervals) {
            if (thisInterval.intersect(inInterval) != null) {
                intersectedIntervals.add(thisInterval);
            }
        }

        // Create new intersected interval if there is any intersection.
        if (intersectedIntervals.size() > 0) {
            IntermittentInterval resultIntervals = new IntermittentInterval(intervals);
            resultIntervals.intervals.removeAll(intersectedIntervals);

            for (Interval thisInterval : intersectedIntervals) {
                resultIntervals.intervals.add(thisInterval.intersect(inInterval));
            }
            return resultIntervals;
        } else {
            // return empty interval.
            return new IntermittentInterval();
        }
    }

    public IntermittentInterval getIntersection(IntermittentInterval interInterval) {
        IntermittentInterval resultInterInterval = new IntermittentInterval();
        for (Interval thisInterval : intervals) {
            resultInterInterval = resultInterInterval.getUnion(interInterval.getIntersection(thisInterval));
        }
        return resultInterInterval;
    }

    @Override
    public String toString() {
        return "IntermittentInterval{" +
                "interval size =" + intervals.size() +
                '}';
    }
}
