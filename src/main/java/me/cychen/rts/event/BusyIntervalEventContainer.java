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
        //ArrayList<TaskInstantEvent> appEvents = inEventContainer.getTaskInstantEvents();

        // Reset the variable.
        busyIntervals.clear();

        Boolean busyIntervalFound = false;
        long beginTimeStamp = 0;
        long endTimeStamp = 0; // keep track on current end timestamp.
        ArrayList<SchedulerIntervalEvent> schedulerIntervalEventsInCurrentBI = new ArrayList<>();
        for (SchedulerIntervalEvent currentEvent: schedulerEvents)
        {
            if (busyIntervalFound == false)
            {
                if (currentEvent.getTask().getTaskType() == Task.TASK_TYPE_IDLE) {
                    continue;
                }
                else
                { // Start of a busy interval is found.
                    busyIntervalFound = true;
                    beginTimeStamp = currentEvent.getOrgBeginTimestamp();
                    schedulerIntervalEventsInCurrentBI.clear();
                    continue;
                }
            }

            if (currentEvent.getTask().getTaskType() == Task.TASK_TYPE_IDLE)
            { // This is the end of a busy interval.
                endTimeStamp = currentEvent.getOrgBeginTimestamp();
                //TaskReleaseEventContainer thisBusyIntervalGroundTruth = new TaskReleaseEventContainer();
                BusyIntervalEvent thisBusyInterval = new BusyIntervalEvent(beginTimeStamp, endTimeStamp);
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
            }
            else
            { // current task is not idle, thus it is still within a busy interval. Continue searching for the idle task.
                schedulerIntervalEventsInCurrentBI.add(currentEvent);
                endTimeStamp = currentEvent.getOrgEndTimestamp();
            }

        } // End of scheduler events iteration loop.

        if (busyIntervalFound == true) {
            // The last busy interval is not closed, so close it now.
            BusyIntervalEvent thisBusyInterval = new BusyIntervalEvent(beginTimeStamp, endTimeStamp);
            thisBusyInterval.getSchedulerIntervalEvents().addAll(schedulerIntervalEventsInCurrentBI);
            busyIntervals.add(thisBusyInterval);
            busyIntervalFound = false;
        }

        return true;
    }

//    public Boolean createBusyIntervalsFromIntervalEvents(ArrayList<IntervalEvent> inEvents)
//    {
//        // Reset the variable.
//        busyIntervals.clear();
//
//        for (IntervalEvent thisEvent : inEvents)
//        {
//            long thisBeginTimeStamp = thisEvent.getOrgBeginTimestamp();
//            long thisEndTimeStamp = thisEvent.getOrgEndTimestamp();
//            BusyIntervalEvent thisBusyInterval = new BusyIntervalEvent(thisBeginTimeStamp, thisEndTimeStamp);
//            busyIntervals.add(thisBusyInterval);
//        }
//        return true;
//    }

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

    public ArrayList<BusyIntervalEvent> findBusyIntervalsByTask(Task inTask)
    {
        ArrayList<BusyIntervalEvent> resultArrayList = new ArrayList<>();
        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
        {
            if (thisBusyInterval.containsComposition(inTask) == true)
            {
                resultArrayList.add(thisBusyInterval);
            }
        }

        return  resultArrayList;
    }

//    public ArrayList<Event> compositionInferencesToEvents() {
//        ArrayList<Event> resultEvents = new ArrayList<>();
//        for (BusyIntervalEvent thisBusyInterval : busyIntervals)
//        {
//            resultEvents.addAll(thisBusyInterval.compositionInferenceToEvents());
//        }
//        return resultEvents;
//    }
}
