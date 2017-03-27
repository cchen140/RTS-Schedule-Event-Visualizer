package me.cychen.rts.scheduleak.restricted;

import me.cychen.rts.scheduleak.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by dogs0 on 9/27/2016.
 * This class does not support shifting or adjustment on begin and end times.
 */
public class WeightedInterval extends Interval {
    HashMap<Long, Integer> weightMap = new HashMap<>();

    public WeightedInterval(long inBegin, long inEnd) {
        super(inBegin, inEnd);
        resetWeightMap();
    }

    //public WeightedInterval(Interval inInterval) {
    //    super(inInterval);
    //}

    public void resetWeightMap() {
        weightMap.clear();
        for (long i=getBegin(); i<=getEnd(); i++) {
            weightMap.put(i, 0);
        }
    }

    public void applyWeight(ArrivalSegment inArrivalSegment) {
        Interval intersection = this.intersect(inArrivalSegment);

        if (intersection == null) {
            return;
        }

        for (long i=intersection.getBegin(); i<=intersection.getEnd(); i++) {
            //int increasedWeight = (weightMap.get(i)==null) ? 1 : weightMap.get(i)+1;
            int increasedWeight = ((weightMap.get(i)==null) ? 0 : weightMap.get(i))
                    + ((inArrivalSegment.getSegmentType()==ArrivalSegment.ONE_ARRIVAL_SEGMENT) ? 2 : 1);
            weightMap.put(i, increasedWeight);
        }
    }

    public ArrayList<Interval> getMostWeightedIntervals() {
        ArrayList<Interval> resultIntervals = new ArrayList<>();

        int highestWeight = Collections.max(weightMap.values());;
        Boolean isBuildingAInterval = false;
        long currentIntervalBegin = 0;
        for (long i=getBegin(); i<=getEnd(); i++) {
            int currentWeight = weightMap.get(i)==null ? 0 : weightMap.get(i);

            if (currentWeight == highestWeight) {
                if (isBuildingAInterval) {
                    continue;
                } else {
                    currentIntervalBegin = i;
                    isBuildingAInterval = true;
                }
            } else {
                if (isBuildingAInterval) {
                    Interval newInterval = new Interval(currentIntervalBegin, i-1);
                    resultIntervals.add(newInterval);
                    isBuildingAInterval = false;
                }
            }
        }

        // Check if the last arrival interval has not yet closed.
        if (isBuildingAInterval) {
            Interval newInterval = new Interval(currentIntervalBegin, getEnd());
            resultIntervals.add(newInterval);
            //isBuildingAInterval = false;
        }

        if (resultIntervals.size() == 0) {
            //ProgMsg.debugErrPutline("It should never happen.");
        }

        return resultIntervals;
    }


}