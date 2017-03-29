package me.cychen.rts.event;

import me.cychen.rts.framework.Task;
import me.cychen.rts.scheduleak.Interval;

import java.util.ArrayList;

/**
 * Created by CY on 5/26/2015.
 */
public class BusyIntervalEventContainer {
    ArrayList<BusyIntervalEvent> busyIntervals = new ArrayList<>();

    public BusyIntervalEventContainer() {}

    public BusyIntervalEventContainer(ArrayList<BusyIntervalEvent> inBusyIntervals) {
        busyIntervals.addAll( inBusyIntervals );
    }

    public void add(BusyIntervalEvent inBi) {
        busyIntervals.add(inBi);
    }

    public Boolean createBusyIntervalsFromEvents(EventContainer inEventContainer) {
        ArrayList<SchedulerIntervalEvent> schedulerEvents = inEventContainer.getSchedulerEvents();
        //ArrayList<TaskInstantEvent> appEvents = inEventContainer.getTaskInstantEvents();

        // Reset the variable.
        busyIntervals.clear();

        Boolean busyIntervalFound = false;
        long beginTimeStamp = 0;
        long endTimeStamp = 0; // keep track on current end timestamp.
        ArrayList<SchedulerIntervalEvent> schedulerIntervalEventsInCurrentBI = new ArrayList<>();
        for (SchedulerIntervalEvent currentEvent : schedulerEvents) {
            if (busyIntervalFound == false) {
                if (currentEvent.getTask().getTaskType() == Task.TASK_TYPE_IDLE) {
                    continue;
                } else { // Start of a busy interval is found.
                    busyIntervalFound = true;
                    beginTimeStamp = currentEvent.getOrgBeginTimestamp();
                    endTimeStamp = currentEvent.getOrgEndTimestamp();
                    schedulerIntervalEventsInCurrentBI.clear();
                    schedulerIntervalEventsInCurrentBI.add(currentEvent);
                    continue;
                }
            }

            if (currentEvent.getTask().getTaskType() == Task.TASK_TYPE_IDLE) { // This is the end of a busy interval.
                endTimeStamp = currentEvent.getOrgBeginTimestamp();
                //TaskReleaseEventContainer thisBusyIntervalGroundTruth = new TaskReleaseEventContainer();
                BusyIntervalEvent thisBusyInterval = new BusyIntervalEvent(beginTimeStamp, endTimeStamp);
                if (beginTimeStamp > endTimeStamp) throw new AssertionError();

                thisBusyInterval.getSchedulerIntervalEvents().addAll(schedulerIntervalEventsInCurrentBI);

                /* Search for the composition of this busy interval. (ground truth) */
//                for (AppEvent currentAppEvent : appEvents)
//                {
//                    if ( (currentAppEvent.getOrgBeginTimestampNs() >= beginTimeStamp)
//                            && (currentAppEvent.getOrgBeginTimestampNs() <= endTimeStamp))
//                    { // This app event is within the busy interval.
//                        if (currentAppEvent.getNote().equalsIgnoreCase("BEGIN"))
//                            thisBusyIntervalGroundTruth.add( currentAppEvent );
//                    }
//                }
//                thisBusyInterval.setCompositionGroundTruth(thisBusyIntervalGroundTruth);
                busyIntervals.add(thisBusyInterval);

                // Reset flag to search next busy interval.
                busyIntervalFound = false;
            } else { // current task is not idle, thus it is still within a busy interval. Continue searching for the idle task.
                schedulerIntervalEventsInCurrentBI.add(currentEvent);
                endTimeStamp = currentEvent.getOrgEndTimestamp();
            }

        } // End of scheduler events iteration loop.

        if (busyIntervalFound == true) {
            // The last busy interval is not closed, so close it now.
            BusyIntervalEvent thisBusyInterval = new BusyIntervalEvent(beginTimeStamp, endTimeStamp);
            if (beginTimeStamp > endTimeStamp) throw new AssertionError();

            thisBusyInterval.getSchedulerIntervalEvents().addAll(schedulerIntervalEventsInCurrentBI);
            busyIntervals.add(thisBusyInterval);
            busyIntervalFound = false;
        }

        return true;
    }

    /* This is used to convert events from Zedboard log. */
    public Boolean createBusyIntervalsFromIntervalEvents(ArrayList<IntervalEvent> inEvents)
    {
        // Reset the variable.
        busyIntervals.clear();

        for (IntervalEvent thisEvent : inEvents)
        {
            long thisBeginTimeStamp = thisEvent.getOrgBeginTimestamp();
            long thisEndTimeStamp = thisEvent.getOrgEndTimestamp();
            BusyIntervalEvent thisBusyInterval = new BusyIntervalEvent(thisBeginTimeStamp, thisEndTimeStamp);
            busyIntervals.add(thisBusyInterval);
        }
        return true;
    }

    public ArrayList<BusyIntervalEvent> getBusyIntervals()
    {
        return busyIntervals;
    }

    public BusyIntervalEvent findBusyIntervalByTimeStamp(int inTimeStamp)
    {
        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.contains(inTimeStamp) == true)
            {
                return thisBusyInterval;
            }
        }

        // If the program reaches here, that means no interval contains the input time stamp.
        return null;
    }

    public ArrayList<BusyIntervalEvent> findBusyIntervalsBeforeTimeStamp(long inTimeStamp)
    {
        ArrayList<BusyIntervalEvent> resultBis = new ArrayList<>();
        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.getOrgBeginTimestamp() <= inTimeStamp)
            {
                resultBis.add(thisBusyInterval);
            }
        }
        return resultBis;
    }

    public ArrayList<BusyIntervalEvent> findBusyIntervalsBetweenTimeStamp(long inBegin, long inEnd) {
        ArrayList<BusyIntervalEvent> resultBis = new ArrayList<>();
        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
        {
            long thisBegin = thisBusyInterval.getOrgBeginTimestamp();
            long thisEnd = thisBusyInterval.getOrgEndTimestamp();

            Interval thisInterval = new Interval(thisBegin, thisEnd);
            Interval inInterval = new Interval(inBegin, inEnd);

            if (thisInterval.intersect(inInterval) != null) {
                resultBis.add(thisBusyInterval);
            }
        }
        return resultBis;
    }

//    public ArrayList<BusyIntervalEvent> findBusyIntervalsByTask(Task inTask)
//    {
//        ArrayList<BusyIntervalEvent> resultArrayList = new ArrayList<>();
//        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
//        {
//            if (thisBusyInterval.containsTaskCheckedByNkValues(inTask) == true)
//            {
//                resultArrayList.add(thisBusyInterval);
//            }
//        }
//
//        return  resultArrayList;
//    }
//
//    public ArrayList<Event> compositionInferencesToEvents() {
//        ArrayList<Event> resultEvents = new ArrayList<>();
//        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
//        {
//            resultEvents.addAll(thisBusyInterval.compositionInferenceToEvents());
//        }
//        return resultEvents;
//    }

    public long getEndTime()
    {
        long endTime = 0;
        for (BusyIntervalEvent thisBusyInterval : busyIntervals) {
            if (thisBusyInterval.getOrgEndTimestamp() > endTime) {
                endTime = thisBusyInterval.getOrgEndTimestamp();
            }
        }
        return endTime;
    }

    public long getBeginTime()
    {
        long beginTime = 0;
        Boolean firstLoop = true;
        for (BusyIntervalEvent thisBusyInterval : busyIntervals) {
            if (firstLoop == true) {
                beginTime = thisBusyInterval.getOrgBeginTimestamp();
                firstLoop = false;
            }

            beginTime = thisBusyInterval.getOrgBeginTimestamp() < beginTime ? thisBusyInterval.getOrgBeginTimestamp() : beginTime;
        }
        return beginTime;
    }

    public void removeBusyIntervalsBeforeTimeStamp(int inTimeStamp) {
        ArrayList<BusyIntervalEvent> biBeforeTimeStamp;
        biBeforeTimeStamp = findBusyIntervalsBeforeTimeStamp(inTimeStamp);
        for (BusyIntervalEvent thisBi : biBeforeTimeStamp) {
            busyIntervals.remove(thisBi);
        }
    }

    public void removeBusyIntervalsBeforeButExcludeTimeStamp(long inTimeStamp) {
        ArrayList<BusyIntervalEvent> biBeforeTimeStamp;
        biBeforeTimeStamp = findBusyIntervalsBeforeTimeStamp(inTimeStamp);
        for (BusyIntervalEvent thisBi : biBeforeTimeStamp) {
            if (thisBi.contains(inTimeStamp)) {
                continue;
            }
            busyIntervals.remove(thisBi);
        }
    }

    public void removeTheLastBusyInterval() {
        long lastBeginTime = 0;
        BusyIntervalEvent lastBi = null;
        for (BusyIntervalEvent thisBi : busyIntervals) {
            if (lastBeginTime < thisBi.getOrgBeginTimestamp()) {
                lastBeginTime = thisBi.getOrgBeginTimestamp();
                lastBi = thisBi;
            }
        }
        busyIntervals.remove(lastBi);
    }

//    public ArrayList<TaskInstantEvent> getStartTimeEventsGTByTask(Task inTask) {
//        ArrayList resultEvents = new ArrayList();
//        for (BusyIntervalEvent bi : busyIntervals) {
//            bi.startTimesGroundTruth.sortTaskReleaseEventsByTime();
//            resultEvents.addAll(bi.startTimesGroundTruth.getEventsOfTask(inTask));
//        }
//        return resultEvents;
//    }
//
//    public ArrayList<TaskInstantEvent> getStartTimeEventsInfByTask(Task inTask) {
//        ArrayList resultEvents = new ArrayList();
//        for (BusyIntervalEvent bi : busyIntervals) {
//            bi.startTimesInference.sortTaskReleaseEventsByTime();
//            resultEvents.addAll(bi.startTimesInference.getEventsOfTask(inTask));
//        }
//        return resultEvents;
//    }

    public int size() {
        return busyIntervals.size();
    }
}