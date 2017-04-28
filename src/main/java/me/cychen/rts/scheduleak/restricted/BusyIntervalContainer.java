package me.cychen.rts.scheduleak.restricted;

import me.cychen.rts.event.*;
import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by CY on 5/26/2015.
 */
@Deprecated
public class BusyIntervalContainer {
    ArrayList<BusyInterval> busyIntervals = new ArrayList<BusyInterval>();

    public BusyIntervalContainer() {}

    public BusyIntervalContainer(BusyIntervalEventContainer inNewFormatBiContainer) {
        for (BusyIntervalEvent thisNewBiEvent : inNewFormatBiContainer.getBusyIntervals()) {
            busyIntervals.add(new BusyInterval(thisNewBiEvent.getOrgBeginTimestamp(), thisNewBiEvent.getOrgEndTimestamp()));
        }
    }

    public BusyIntervalContainer(ArrayList<BusyInterval> inBusyIntervals) {
        busyIntervals.addAll( inBusyIntervals );
    }

    public Boolean createBusyIntervalsFromEvents(EventContainer inEventContainer)
    {
        ArrayList<SchedulerIntervalEvent> schedulerEvents = inEventContainer.getSchedulerEvents();
        ArrayList<TaskInstantEvent> appEvents = inEventContainer.getTaskInstantEvents();
        int idleTaskId = 0;

        // Reset the variable.
        busyIntervals.clear();

        // Find IDLE task ID
        for (Object currentObject : inEventContainer.getTaskSet().getTasksAsArray())
        {
            Task currentTask = (Task) currentObject;
            if (currentTask.getTitle().equalsIgnoreCase("IDLE")){
                idleTaskId = currentTask.getId();
                break;
            }
        }

        Boolean busyIntervalFound = false;
        long beginTimeStamp = 0;
        for (SchedulerIntervalEvent currentEvent: schedulerEvents)
        {
            if (busyIntervalFound == false)
            {
                if (currentEvent.getTask().getId() == idleTaskId) {
                    continue;
                }
                else
                { // Start of a busy interval is found.
                    busyIntervalFound = true;
                    beginTimeStamp = currentEvent.getOrgBeginTimestamp();
                    continue;
                }
            }

            if (currentEvent.getTask().getId() == idleTaskId)
            { // This is the end of a busy interval.
                long endTimeStamp = currentEvent.getOrgBeginTimestamp();
                StartTimeEventContainer thisBusyIntervalGroundTruth = new StartTimeEventContainer();
                BusyInterval thisBusyInterval = new BusyInterval(beginTimeStamp, endTimeStamp);

                /* Search for the composition of this busy interval. (ground truth) */
                for (TaskInstantEvent currentAppEvent : appEvents)
                {
                    if ( (currentAppEvent.getOrgTimestamp() >= beginTimeStamp)
                            && (currentAppEvent.getOrgTimestamp() <= endTimeStamp))
                    { // This app event is within the busy interval.
                        if (currentAppEvent.getNote().equalsIgnoreCase("BEGIN"))
                            thisBusyIntervalGroundTruth.add( currentAppEvent );
                    }
                }
                thisBusyInterval.setStartTimesGroundTruth(thisBusyIntervalGroundTruth);
                busyIntervals.add(thisBusyInterval);

                // Reset flag to search next busy interval.
                busyIntervalFound = false;
            }
            else
            { // current task is not idle, thus it is still within a busy interval. Continue searching for the idle task.

            }

        } // End of scheduler events iteration loop.
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
            BusyInterval thisBusyInterval = new BusyInterval(thisBeginTimeStamp, thisEndTimeStamp);
            busyIntervals.add(thisBusyInterval);
        }
        return true;
    }

    public ArrayList<BusyInterval> getBusyIntervals()
    {
        return busyIntervals;
    }

    public BusyInterval findBusyIntervalByTimeStamp(int inTimeStamp)
    {
        for (BusyInterval thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.contains(inTimeStamp) == true)
            {
                return thisBusyInterval;
            }
        }

        // If the program reaches here, that means no interval contains the input time stamp.
        return null;
    }

    public ArrayList<BusyInterval> findBusyIntervalsBeforeTimeStamp(int inTimeStamp)
    {
        ArrayList<BusyInterval> resultBis = new ArrayList<>();
        for (BusyInterval thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.getBeginTimeStampNs() <= inTimeStamp)
            {
                resultBis.add(thisBusyInterval);
            }
        }
        return resultBis;
    }

    public ArrayList<BusyInterval> findBusyIntervalsByTask(Task inTask)
    {
        ArrayList<BusyInterval> resultArrayList = new ArrayList<>();
        for (BusyInterval thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.containsTaskCheckedByNkValues(inTask) == true)
            {
                resultArrayList.add(thisBusyInterval);
            }
        }

        return  resultArrayList;
    }

    public ArrayList<Event> compositionInferencesToEvents() {
        ArrayList<Event> resultEvents = new ArrayList<>();
        for (BusyInterval thisBusyInterval : busyIntervals)
        {
            resultEvents.addAll(thisBusyInterval.compositionInferenceToEvents());
        }
        return resultEvents;
    }

    public long getEndTime()
    {
        long endTime = 0;
        for (BusyInterval thisBusyInterval : busyIntervals) {
            if (thisBusyInterval.getEndTimeStampNs() > endTime) {
                endTime = thisBusyInterval.getEndTimeStampNs();
            }
        }
        return endTime;
    }

    public long getBeginTime()
    {
        long beginTime = 0;
        Boolean firstLoop = true;
        for (BusyInterval thisBusyInterval : busyIntervals) {
            if (firstLoop == true) {
                beginTime = thisBusyInterval.getBeginTimeStampNs();
                firstLoop = false;
            }

            beginTime = thisBusyInterval.getBeginTimeStampNs() < beginTime ? thisBusyInterval.getBeginTimeStampNs() : beginTime;
        }
        return beginTime;
    }

    public void removeBusyIntervalsBeforeTimeStamp(int inTimeStamp) {
        ArrayList<BusyInterval> biBeforeTimeStamp;
        biBeforeTimeStamp = findBusyIntervalsBeforeTimeStamp(inTimeStamp);
        for (BusyInterval thisBi : biBeforeTimeStamp) {
            busyIntervals.remove(thisBi);
        }
    }

    public void removeBusyIntervalsBeforeButExcludeTimeStamp(int inTimeStamp) {
        ArrayList<BusyInterval> biBeforeTimeStamp;
        biBeforeTimeStamp = findBusyIntervalsBeforeTimeStamp(inTimeStamp);
        for (BusyInterval thisBi : biBeforeTimeStamp) {
            if (thisBi.contains(inTimeStamp)) {
                continue;
            }
            busyIntervals.remove(thisBi);
        }
    }

    public void removeTheLastBusyInterval() {
        long lastBeginTime = 0;
        BusyInterval lastBi = null;
        for (BusyInterval thisBi : busyIntervals) {
            if (lastBeginTime < thisBi.getBeginTimeStampNs()) {
                lastBeginTime = thisBi.getBeginTimeStampNs();
                lastBi = thisBi;
            }
        }
        busyIntervals.remove(lastBi);
    }

    public ArrayList<TaskInstantEvent> getStartTimeEventsGTByTask(Task inTask) {
        ArrayList resultEvents = new ArrayList();
        for (BusyInterval bi : busyIntervals) {
            bi.startTimesGroundTruth.sortTaskReleaseEventsByTime();
            resultEvents.addAll(bi.startTimesGroundTruth.getEventsOfTask(inTask));
        }
        return resultEvents;
    }

    public ArrayList<TaskInstantEvent> getStartTimeEventsInfByTask(Task inTask) {
        ArrayList resultEvents = new ArrayList();
        for (BusyInterval bi : busyIntervals) {
            bi.startTimesInference.sortTaskReleaseEventsByTime();
            resultEvents.addAll(bi.startTimesInference.getEventsOfTask(inTask));
        }
        return resultEvents;
    }

    public int size() {
        return busyIntervals.size();
    }
}