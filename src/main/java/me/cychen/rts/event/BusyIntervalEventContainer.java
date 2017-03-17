package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by cy on 3/16/2017.
 */
public class BusyIntervalEventContainer {
    ArrayList<BusyIntervalEvent> busyIntervals = new ArrayList<BusyIntervalEvent>();

    public Boolean createBusyIntervalsFromEvents(EventContainer inEventContainer)
    {
        ArrayList<SchedulerIntervalEvent> schedulerEvents = inEventContainer.getSchedulerEvents();
        ArrayList<TaskInstantEvent> appEvents = inEventContainer.getTaskInstantEvents();
        int idleTaskId = 0;

        // Reset the variable.
        busyIntervals.clear();

        // Find IDLE task ID
        idleTaskId = inEventContainer.getTaskSet().getIdleTask().getId();


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
                int endTimeStamp = currentEvent.getOrgBeginTimestampNs();
                TaskReleaseEventContainer thisBusyIntervalGroundTruth = new TaskReleaseEventContainer();
                BusyInterval thisBusyInterval = new BusyInterval(beginTimeStamp, endTimeStamp);

                /* Search for the composition of this busy interval. (ground truth) */
                for (AppEvent currentAppEvent : appEvents)
                {
                    if ( (currentAppEvent.getOrgBeginTimestampNs() >= beginTimeStamp)
                            && (currentAppEvent.getOrgBeginTimestampNs() <= endTimeStamp))
                    { // This app event is within the busy interval.
                        if (currentAppEvent.getNote().equalsIgnoreCase("BEGIN"))
                            thisBusyIntervalGroundTruth.add( currentAppEvent );
                    }
                }
                thisBusyInterval.setCompositionGroundTruth(thisBusyIntervalGroundTruth);
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

    public Boolean createBusyIntervalsFromIntervalEvents(ArrayList<IntervalEvent> inEvents)
    {
        // Reset the variable.
        busyIntervals.clear();

        for (IntervalEvent thisEvent : inEvents)
        {
            int thisBeginTimeStamp = thisEvent.getOrgBeginTimestampNs();
            int thisEndTimeStamp = thisEvent.getOrgEndTimestampNs();
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

    public ArrayList<BusyInterval> findBusyIntervalsByTask(Task inTask)
    {
        ArrayList<BusyInterval> resultArrayList = new ArrayList<>();
        for (BusyInterval thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.containsComposition(inTask) == true)
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
}
