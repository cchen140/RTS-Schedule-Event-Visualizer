package me.cychen.rts.event;

import me.cychen.rts.framework.Task;

import java.util.ArrayList;

/**
 * Created by CY on 7/29/2015.
 */
public class TaskReleaseEventContainer {
    ArrayList<TaskInstantEvent> taskReleaseEvents = new ArrayList<>();

    public TaskReleaseEventContainer() {}

    public TaskReleaseEventContainer( TaskReleaseEventContainer inContainer )
    {
        // replicate the event array.
        taskReleaseEvents.addAll( inContainer.taskReleaseEvents );
    }

    public void add( TaskInstantEvent inEvent )
    {
        taskReleaseEvents.add( inEvent );
        sortTaskReleaseEventsByTime();
    }

    public TaskInstantEvent add(long releaseTime, Task inTask)
    {
        TaskInstantEvent thisEvent = new TaskInstantEvent( releaseTime, inTask, 0, "" );

        this.add(thisEvent);

        return thisEvent;
    }

    public void addAll( ArrayList<TaskInstantEvent> inEvents )
    {
        taskReleaseEvents.addAll(inEvents);
    }

    public void sortTaskReleaseEventsByTime()
    {
        ArrayList<TaskInstantEvent> sortedEvents = new ArrayList<>();
        for ( TaskInstantEvent thisEvent : taskReleaseEvents ) {

            Boolean firstLoop = true;
            for ( TaskInstantEvent thisSortedEvent : sortedEvents ) {
                if ( firstLoop == true ) {
                    firstLoop = false;
                    sortedEvents.add( thisEvent );
                    continue;
                }

                // If the time is smaller (earlier), then insert to that
                if ( thisEvent.getOrgTimestamp() < thisSortedEvent.getOrgTimestamp() ) {
                    sortedEvents.add( sortedEvents.indexOf( thisSortedEvent ), thisEvent );
                    break; // Found place, so insert the event and break the loop to process next event.
                }
            }
        }
    }

    public int size()
    {
        return taskReleaseEvents.size();
    }

    public ArrayList<Task> getTasksOfEvents()
    {
        ArrayList<Task> resultTasks = new ArrayList<>();
        for ( TaskInstantEvent thisEvent : taskReleaseEvents ) {
            resultTasks.add(thisEvent.getTask());
        }
        return resultTasks;
    }

    public TaskInstantEvent get(int index)
    {
        return taskReleaseEvents.get( index );
    }

    public void clear()
    {
        taskReleaseEvents.clear();
    }


    /* Find the first event after the designated time stamp. */
    public TaskInstantEvent getNextEvent( long inTimeStamp )
    {
        for ( TaskInstantEvent thisEvent : taskReleaseEvents ) {
            if ( thisEvent.getOrgTimestamp() >= inTimeStamp )
                return thisEvent;
        }

        // If no event is after the designated time, then return null.
        return null;
    }

    public ArrayList<TaskInstantEvent> getEvents()
    {
        return taskReleaseEvents;
    }

}