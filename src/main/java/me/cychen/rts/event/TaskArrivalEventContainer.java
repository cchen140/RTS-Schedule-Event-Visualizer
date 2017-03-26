package me.cychen.rts.event;

import java.util.Collections;

/**
 * Created by CY on 7/29/2015.
 */
public class TaskArrivalEventContainer extends TaskReleaseEventContainer {
    public TaskArrivalEventContainer() {super();}

    public TaskArrivalEventContainer( TaskArrivalEventContainer inContainer )
    {
        super( inContainer );
    }

    @Override
    public void add(TaskInstantEvent inEvent) {
        super.add(inEvent);
        sortTaskReleaseEventsByTimePriority();
    }

    public void sortTaskReleaseEventsByTimePriority()
    {
        sortTaskReleaseEventsByTime();

        int beforeSwapping = 0;
        int afterSwapping = 0;
        do {
            int numOfEvents = taskReleaseEvents.size();
            beforeSwapping = taskReleaseEvents.hashCode();
            for (int loop=0; loop<(numOfEvents-1) ; loop++) {
                TaskInstantEvent thisEvent = taskReleaseEvents.get(loop);
                TaskInstantEvent nextEvent = taskReleaseEvents.get(loop+1);
                if ( thisEvent.getOrgTimestamp() == nextEvent.getOrgTimestamp() ) {
                    // This event and next event have the same arrival time, thus check priority in advance.
                    if ( nextEvent.getTask().getPriority() < thisEvent.getTask().getPriority() ) {
                        // Next event has higher priority, thus do swapping.
                        Collections.swap( taskReleaseEvents, loop, loop+1 );
                    }
                }
            }
            afterSwapping = taskReleaseEvents.hashCode();

            // If some elements are swapped, then the hash code would be different. Continue the process until nothing to swap.
        } while ( beforeSwapping != afterSwapping );
    }

    /* Find the first event after the designated time stamp. */
    public TaskInstantEvent getNextEvent( int inTimeStamp )
    {
        for ( TaskInstantEvent thisEvent : taskReleaseEvents ) {
            if ( thisEvent.getOrgTimestamp() >= inTimeStamp )
                return thisEvent;
        }

        // If no event is after the designated time, then return null.
        return null;
    }

    /* Pop the first event after the designated time stamp. */
    public TaskInstantEvent popNextEvent( int inTimeStamp )
    {
        for ( TaskInstantEvent thisEvent : taskReleaseEvents ) {
            if ( thisEvent.getOrgTimestamp() >= inTimeStamp ) {
                taskReleaseEvents.remove( thisEvent );
                return thisEvent;
            }

        }

        // If no event is after the designated time, then return null.
        return null;
    }
}