package me.cychen.rts.scheduleak;

import java.util.HashMap;

/**
 * Created by cy on 3/28/2017.
 */
public class DistributionMap {
    HashMap<Long, Long> map = new HashMap<Long, Long>();

    public void touch(long inLocation) {
        if (map.get(inLocation) == null) {
            map.put(inLocation, (long)1);
        } else {
            map.put(inLocation, map.get(inLocation)+1);
        }
    }

    @Override
    public String toString() {
        String outputStr = "";
        for (Long index : map.keySet()) {
            outputStr += index + " = " + map.get(index) + "\r\n";
        }
        return outputStr;
    }
}
