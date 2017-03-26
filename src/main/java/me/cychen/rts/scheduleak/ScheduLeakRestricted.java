package me.cychen.rts.scheduleak;

import me.cychen.rts.event.BusyIntervalEvent;
import me.cychen.rts.event.BusyIntervalEventContainer;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.event.TaskArrivalEventContainer;
import me.cychen.rts.framework.Job;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ScheduLeakRestricted {
    private TaskSet taskContainer;
    private BusyIntervalEventContainer busyIntervalContainer;
    private HashMap<Task, Interval> taskArrivalTimeWindows = new HashMap<Task, Interval>();

    public ScheduLeakRestricted(TaskSet inTaskContainer, BusyIntervalEventContainer inBusyIntervalContainer)
    {
        taskContainer = inTaskContainer;
        busyIntervalContainer = inBusyIntervalContainer;
    }

    public Boolean runDecompositionStep1()
    {
        // Step one: finding n values for every task in every busy interval.
        for (BusyIntervalEvent thisBusyInterval : busyIntervalContainer.getBusyIntervals())
        {
            thisBusyInterval.setComposition(calculateComposition(thisBusyInterval));
        }
        return true;
    }

    public Boolean runDecompositionStep2()
    {
        // Step two: creating arrival time window for each task by processing the result from step one.
        calculateArrivalTimeOfAllTasks();

        removeAmbiguousInferenceByArrivalTimeWindow();

        calculateArrivalTimeOfAllTasks();

        return true;
    }

    public EventContainer runDecompositionStep3()
    {
        //reconstructCompositionOfBusyIntervalByArrivalTimeWindows();
        return reconstructScheduleByArrivalWindows();
        //return true;
    }

    public EventContainer runDecomposition()
    {

        // Step one: finding n values for every task in every busy interval.
        runDecompositionStep1();

        // Step two: creating arrival time window for each task by processing the result from step one.
        runDecompositionStep2();

        // Step three: arrival time to scheduling.
        return runDecompositionStep3();

        //return true;
    }

    public EventContainer runDecompositionWithErrors()
    {
        // Step one: finding n values for every task in every busy interval.
        for (BusyIntervalEvent thisBusyInterval : busyIntervalContainer.getBusyIntervals())
        {
            thisBusyInterval.setComposition(calculateCompositionWithErrors(thisBusyInterval));
        }

        // Step two: creating arrival time window for each task by processing the result from step one.
        runDecompositionStep2();

        // Step three: arrival time to scheduling.
        return runDecompositionStep3();

        //return true;
    }

    public ArrayList<ArrayList<Task>> calculateComposition(BusyIntervalEvent inBusyInterval)
    {
        long intervalNs = inBusyInterval.getDuration();
//        int matchingInterval = 0;

        /* Calculate N of each task. */
        HashMap<Integer, ArrayList<Integer>> nOfTasks = new HashMap<Integer, ArrayList<Integer>>();
        for (Object thisObject: taskContainer.getAppTasksAsArray())
        {
            Task thisTask = (Task) thisObject;
            ArrayList<Integer> thisResult = new ArrayList<Integer>();

            long thisP = thisTask.getPeriod();
            long thisC = thisTask.getExecTime();

            int numberOfCompletePeriods = (int) Math.floor(intervalNs / thisP);
            long subIntervalNs = intervalNs - numberOfCompletePeriods*thisP;

            if (subIntervalNs < thisC)
            {// This task can only have occurred 0 time in this sub-interval.
                thisResult.add(numberOfCompletePeriods + 0);
            }
            else if (subIntervalNs < (thisP-thisC))
            {// This task can have occurred 0 or 1 time in this sub-interval.
                thisResult.add(numberOfCompletePeriods + 0);
                thisResult.add(numberOfCompletePeriods + 1);
            }
            else // if (subIntervalNs < thisP)
            {// This task can only have occurred 1 times in this sub-interval.
                thisResult.add(numberOfCompletePeriods + 1);
            }

            nOfTasks.put(thisTask.getId(), thisResult);
//            matchingInterval += thisResult.get(0);
        }

        // Find Ns match this interval.
        ArrayList<ArrayList<Task>> resultCompositions;// = new ArrayList<HashMap<Integer, Integer>>();//HashMap<Integer, Integer>();
        resultCompositions = findMatchingCompositions(nOfTasks, intervalNs, null);
//        System.out.println(resultsNOfTasks);
        return resultCompositions;

    }

    public ArrayList<ArrayList<Task>> calculateCompositionWithErrors(BusyIntervalEvent inBusyInterval)
    {
        long intervalNs = inBusyInterval.getDuration();
//        int matchingInterval = 0;

        /* Calculate N of each task. */
        HashMap<Integer, ArrayList<Integer>> nOfTasks = new HashMap<Integer, ArrayList<Integer>>();
        for (Object thisObject: taskContainer.getAppTasksAsArray())
        {
            Task thisTask = (Task) thisObject;
            ArrayList<Integer> thisResult = new ArrayList<Integer>();

            long thisP = thisTask.getPeriod();
            long thisC = thisTask.getExecTime();
            long thisCError = thisTask.getExecTimeError();

            int numberOfCompletePeriods = (int) Math.floor(intervalNs / thisP);
            long subIntervalNs = intervalNs - numberOfCompletePeriods*thisP;

            if (subIntervalNs < thisC-thisCError)
            {// This task can only have occurred 0 time in this sub-interval.
                thisResult.add(numberOfCompletePeriods + 0);
            }
            else if (subIntervalNs < (thisP-thisC+thisCError))
            {// This task can have occurred 0 or 1 time in this sub-interval.
                thisResult.add(numberOfCompletePeriods + 0);
                thisResult.add(numberOfCompletePeriods + 1);
            }
            else // if (subIntervalNs < thisP)
            {// This task can only have occurred 1 times in this sub-interval.
                thisResult.add(numberOfCompletePeriods + 1);
            }

            nOfTasks.put(thisTask.getId(), thisResult);
//            matchingInterval += thisResult.get(0);
        }

        // Find Ns match this interval.
        ArrayList<ArrayList<Task>> resultCompositions;// = new ArrayList<HashMap<Integer, Integer>>();//HashMap<Integer, Integer>();
        resultCompositions = findMatchingCompositionsWithErrors(nOfTasks, intervalNs, null);
        return resultCompositions;

    }

    private ArrayList<ArrayList<Task>> findMatchingCompositions(HashMap<Integer, ArrayList<Integer>> inNOfTasks, long inTargetInterval, HashMap<Integer, Integer> inProcessingNOfTasks)
    {
        ArrayList<ArrayList<Task>> resultCompositions = new ArrayList<ArrayList<Task>>();

        if (inNOfTasks.isEmpty())
        {
            /* Compute the interval from current compositions. */
            int compositeInterval = 0;
            for (int thisTaskId : inProcessingNOfTasks.keySet())
            {
                compositeInterval += taskContainer.getTaskById(thisTaskId).getExecTime() * inProcessingNOfTasks.get(thisTaskId);
            }
//            System.out.format("End of recursive calls, %d\r\n", compositeInterval);

            /* Check whether current composite interval equals target interval or not. */
            if (compositeInterval == inTargetInterval)
            {
//                System.out.println("Matched!!");
//                System.out.println(inProcessingNOfTasks);

                ArrayList thisResultComposition = new ArrayList<Task>();
                for (int thisTaskId : inProcessingNOfTasks.keySet())
                {
                    for (int loop=0; loop<inProcessingNOfTasks.get(thisTaskId); loop++)
                    {
                        thisResultComposition.add(taskContainer.getTaskById(thisTaskId));
                    }
                }
                resultCompositions.add(thisResultComposition);
                return resultCompositions;
            }
            else
            {
                /* Because addAll() doesn't accept null pointer, thus returning empty arrayList instead. */
                //return null;
                return resultCompositions;
            }
        }

        /* Select an unsorted task to process and create a list which contains rest of n values of unsorted tasks. */
        int thisTaskId = inNOfTasks.keySet().iterator().next();
        ArrayList<Integer> nOfThisTask = inNOfTasks.get(thisTaskId);
        HashMap<Integer, ArrayList<Integer>> restNOfTasks = new HashMap<Integer, ArrayList<Integer>>(inNOfTasks);
        restNOfTasks.remove(thisTaskId);

        if (inProcessingNOfTasks == null)
        { // For the first time the program gets here, inProcessingNOfTasks has to be initialized.
            inProcessingNOfTasks = new HashMap<Integer, Integer>();
        }

        // Iterate every possible n value of current task and pass the value down recursively.
        for (Integer thisN: nOfThisTask)
        {
            inProcessingNOfTasks.put(thisTaskId, thisN);
            resultCompositions.addAll(findMatchingCompositions(restNOfTasks, inTargetInterval, inProcessingNOfTasks));
            inProcessingNOfTasks.remove(thisTaskId);
        }
        return resultCompositions;
    }

    private ArrayList<ArrayList<Task>> findMatchingCompositionsWithErrors(HashMap<Integer, ArrayList<Integer>> inNOfTasks, long inTargetInterval, HashMap<Integer, Integer> inProcessingNOfTasks)
    {
        ArrayList<ArrayList<Task>> resultCompositions = new ArrayList<ArrayList<Task>>();
        if (inNOfTasks.isEmpty())
        {
            /* Compute the interval from current compositions. */
            int compositeInterval = 0;
            int accumulatedCErrors = 0;
            for (int thisTaskId : inProcessingNOfTasks.keySet())
            {
                long thisC = taskContainer.getTaskById(thisTaskId).getExecTime();
                long thisCError = taskContainer.getTaskById(thisTaskId).getExecTimeError();
                compositeInterval += thisC * inProcessingNOfTasks.get(thisTaskId);
                accumulatedCErrors += thisCError * inProcessingNOfTasks.get(thisTaskId);
            }
            //ProgMsg.debugPutline("End of recursive calls, %d\r\n", compositeInterval);

            /* Check whether current composite interval equals target interval or not. */
            if (areEqualWithinError(compositeInterval, inTargetInterval, accumulatedCErrors) == true)
            {
                //ProgMsg.debugPutline("Matched!!");
                //ProgMsg.debugPutline(String.valueOf(inProcessingNOfTasks));

                ArrayList thisResultComposition = new ArrayList<Task>();
                for (int thisTaskId : inProcessingNOfTasks.keySet())
                {
                    for (int loop=0; loop<inProcessingNOfTasks.get(thisTaskId); loop++)
                    {
                        thisResultComposition.add(taskContainer.getTaskById(thisTaskId));
                    }
                }
                resultCompositions.add(thisResultComposition);
                return resultCompositions;
            }
            else
            {
                /* Because addAll() doesn't accept null pointer, thus returning empty arrayList instead. */
                //return null;
                return resultCompositions;
            }
        }

        /* Select an unsorted task to process and create a list which contains rest of n values of unsorted tasks. */
        int thisTaskId = inNOfTasks.keySet().iterator().next();
        ArrayList<Integer> nOfThisTask = inNOfTasks.get(thisTaskId);
        HashMap<Integer, ArrayList<Integer>> restNOfTasks = new HashMap<Integer, ArrayList<Integer>>(inNOfTasks);
        restNOfTasks.remove(thisTaskId);

        if (inProcessingNOfTasks == null)
        { // For the first time the program gets here, inProcessingNOfTasks has to be initialized.
            inProcessingNOfTasks = new HashMap<Integer, Integer>();
        }

        // Iterate every possible n value of current task and pass the value down recursively.
        for (Integer thisN: nOfThisTask)
        {
            inProcessingNOfTasks.put(thisTaskId, thisN);
            resultCompositions.addAll(findMatchingCompositionsWithErrors(restNOfTasks, inTargetInterval, inProcessingNOfTasks));
            inProcessingNOfTasks.remove(thisTaskId);
        }
        return resultCompositions;
    }

    public Boolean areEqualWithinError(long inNum01, long inNum02, long inErrorRange)
    {
        return Math.abs(inNum01-inNum02)<=inErrorRange ? true : false;
    }

    public Boolean calculateArrivalTimeOfAllTasks()
    {
        for (Task thisTask : taskContainer.getAppTasksAsArray()){
            Interval thisInterval = calculateArrivalTimeWindowOfTask(thisTask, taskArrivalTimeWindows.get(thisTask));
            taskArrivalTimeWindows.put(thisTask, thisInterval);
            //ProgMsg.debugPutline("%s, %d:%d", thisTask.getTitle(), thisInterval.getBegin(), thisInterval.getEnd());
        }

        // TODO: Should return false if any arrival time window is null?
        return true;
    }

    public Interval calculateArrivalTimeWindowOfTask(Task inTask, Interval inFirstWindow)
    {
        ArrayList<BusyIntervalEvent> thisTaskBusyIntervals;

        /* Find the busy intervals that contain inTask. */
        thisTaskBusyIntervals = busyIntervalContainer.findBusyIntervalsByTask(inTask);

        Interval firstWindow = null;
        Boolean firstLoop = true;

        /** TODO: Test for starting with the shortest busy interval **/
        if (inFirstWindow != null) {
            firstWindow = inFirstWindow;
        }
        else {
            firstWindow = calculateArrivalTimeWindowOfTaskInABusyInterval(findShortestBusyIntervalContainingTask(inTask), inTask);
        }
        // Move the window to around zero point.
        firstWindow.shift(-(firstWindow.getBegin() / inTask.getPeriod()) * inTask.getPeriod());
        firstLoop = false;
        /** Test ends **/


        for (int loop=0; loop<2; loop++) {
            for (BusyIntervalEvent thisBusyInterval : thisTaskBusyIntervals) {
                // The first busy interval should be the leftmost one (or the first valid one).
                if (firstLoop == true) {
                    firstWindow = calculateArrivalTimeWindowOfTaskInABusyInterval(thisBusyInterval, inTask);
                    // If this windows is invalid, then search for next valid window.
                    if (firstWindow != null) {
                        firstLoop = false;
                    }
                    continue;
                }


            /* Get current arrival window and check whether the window is valid or not. */
                Interval thisWindow = calculateArrivalTimeWindowOfTaskInABusyInterval(thisBusyInterval, inTask);
                if (thisWindow == null)
                    continue;

                Long smallestShiftPeriodValue = findSmallestPeriodShiftValueWithIntersection(firstWindow, thisWindow, inTask.getPeriod());

                if (smallestShiftPeriodValue == null) {// No intersection.
                    continue;
                } else {// Has intersection.
                    Interval shiftedThisWindow = new Interval(thisWindow);
                    shiftedThisWindow.shift(smallestShiftPeriodValue * inTask.getPeriod());

                    // Shift one more period to see if there is another intersection
                    shiftedThisWindow.shift(-inTask.getPeriod());

                    if (firstWindow.intersect(shiftedThisWindow) != null) {// Has two intersections, thus skip intersecting this window.
                        continue;
                    } else {// In the end, it has only one intersection, so apply the intersection to firstWindow.
                        thisWindow.shift(smallestShiftPeriodValue * inTask.getPeriod());
                        firstWindow = firstWindow.intersect(thisWindow);
                    }

                }

            } // End of for loop.
        }

        assert firstWindow!=null : "No arrival window is found in all busy intervals for this task";
        if (firstWindow == null) {
            //ProgMsg.debugPutline("No arrival window is found in all busy intervals for %s task", inTask.getTitle());
        }

        return firstWindow;
    }


    // Integer type for the returned value is used because "null" will be returned if no intersection is found.
    public Long findSmallestPeriodShiftValueWithIntersection(Interval fixedInterval, Interval shiftingInterval, long inPeriod)
    {

        long periodShiftValue = (fixedInterval.getBegin()-shiftingInterval.getBegin()) / inPeriod;

        Interval newInstanceShiftingInterval = new Interval(shiftingInterval);
        newInstanceShiftingInterval.shift(periodShiftValue*inPeriod);

        /* Check whether the intersection exists. */
        if (fixedInterval.intersect(newInstanceShiftingInterval) != null)
        {// Has intersection.
            return periodShiftValue;
        }


        /* Shift one more to see if they have intersection. */
        if (fixedInterval.getBegin() >= shiftingInterval.getBegin()) {
            periodShiftValue++;
            newInstanceShiftingInterval.shift(inPeriod);
        } else {
            periodShiftValue--;
            newInstanceShiftingInterval.shift(-inPeriod);
        }

        if (fixedInterval.intersect(newInstanceShiftingInterval) != null) {
            // Has intersection.
            return periodShiftValue;
        } else {
            return null;
        }
    }

    public Interval calculateArrivalTimeWindowOfTaskInABusyInterval(BusyIntervalEvent inBusyInterval, Task inTask)
    {
        // First, check whether this busy interval contains inTask or not.
        if (inBusyInterval.containsComposition(inTask) == false)
            return null;

        Boolean hasAmbiguousInferenceForThisTask = false;

        /* How many inTask are there in this busy interval? */
        /* Check whether the inference of the composition contains ambiguity for "inTask" or not. */
        if (inBusyInterval.getComposition().size() > 1)
        {// More than one possible answers.
            /* Check if*/
            Boolean firstLoop = true;
            int numOfInTask = 0;
            hasAmbiguousInferenceForThisTask = false;
            for (ArrayList<Task> thisInference : inBusyInterval.getComposition())
            {
                int thisNumOfInTask = 0;
                /* Calculate the number of inTask contained in this inference. */
                thisNumOfInTask = Collections.frequency(thisInference, inTask);

                if (firstLoop == true) {
                    firstLoop = false;
                    numOfInTask = thisNumOfInTask;
                    continue;
                }

                if (numOfInTask != thisNumOfInTask)
                {// Has ambiguity.

                    if (numOfInTask==0 || thisNumOfInTask==0)
                    {// One of the inference contains no inTask, thus it's not doable.
                        //ProgMsg.debugPutline("One of the inference contains 0 designated task.");
                        return null;
                    }
                    else
                    {
                        // Has ambiguity but it's doable.
                        hasAmbiguousInferenceForThisTask = true;
                    }
                }
                else
                {
                    // This inference of inTask is consistent with last one.
                    //continue; // Continue to check next.
                }
            }
        }

        /* Create the arrival window for the first computation period in this busy interval. */
        long resultBeginTime = inBusyInterval.getOrgBeginTimestamp();
        long resultEndTime = Math.min(inBusyInterval.getOrgEndTimestamp() - inTask.getExecTime(),
                inBusyInterval.getOrgBeginTimestamp() + inTask.getPeriod() - inTask.getExecTime());
        Interval resultArrivalTimeWindow = new Interval(resultBeginTime, resultEndTime);

        /* If it has no ambiguity and has more than one inTask, then narrow the window with the one got from last period in this busy interval. */
        if (hasAmbiguousInferenceForThisTask==false)
        {
            // No ambiguity means only one inference is available, thus just get the first inference from busy interval.
            int numOfInTask = Collections.frequency(inBusyInterval.getFirstComposition(), inTask);
            if (numOfInTask > 1)
            {
                // Calculate the window of the last period in this busy interval.
                long lastPeriodBeginTime = inBusyInterval.getOrgBeginTimestamp() + inTask.getPeriod()*(numOfInTask-1);
                long lastPeriodEndTime = inBusyInterval.getOrgEndTimestamp();
                Interval lastPeriodArrivalTimeWindow = new Interval(lastPeriodBeginTime, lastPeriodEndTime);

                // Shift the window of last period to the first window
                lastPeriodArrivalTimeWindow.shift( -(inTask.getPeriod()*(numOfInTask - 1)) );

                // Get intersection of two.
                resultArrivalTimeWindow = resultArrivalTimeWindow.intersect(lastPeriodArrivalTimeWindow);

                assert (resultArrivalTimeWindow!=null) : "Got an empty Arrival Time Window from the intersection.";
                if (resultArrivalTimeWindow == null)
                { // It should not ever happen.
                    //ProgMsg.debugPutline("Got an empty Arrival Time Window from the intersection.");
                }
            }
        }


        /** For early stage algorithm development only! It has to be removed after test. **/
        if (hasAmbiguousInferenceForThisTask == true)
        {
            /* Do nothing for now. */
            // TODO: Should mark this busy interval as unsolved?
            //ProgMsg.debugPutline("Has ambiguity to be solved.");
        }

        /** End **/

        return resultArrivalTimeWindow;
    }

//    public Trace buildTaskArrivalTimeWindowTrace(Task inTask)
//    {
//        if (taskArrivalTimeWindows.get(inTask) == null)
//        {
//            ProgMsg.debugPutline("%s task has null arrival time window! Can't process.", inTask.getTitle());
//            return null;
//        }
//
//        Interval thisTaskArrivalTimeWindow = taskArrivalTimeWindows.get(inTask);
//
//        ArrayList<IntervalEvent> intervalEvents = new ArrayList<>();
//        for (int loop = 0; loop<10; loop++)
//        {
//            IntervalEvent thisIntervalEvent = new IntervalEvent(thisTaskArrivalTimeWindow.getBegin()+loop*inTask.getPeriodNs(), thisTaskArrivalTimeWindow.getEnd()+loop*inTask.getPeriodNs());
//            thisIntervalEvent.setColor(inTask.getTaskColor());
//            thisIntervalEvent.enableTexture();
//            intervalEvents.add(thisIntervalEvent);
//        }
//
//        return new Trace(inTask.getTitle() + " Arrival Time", inTask, intervalEvents, new TimeLine(), Trace.TRACE_TYPE_OTHER);
//    }
//
//    public ArrayList<Trace> buildTaskArrivalTimeWindowTracesForAllTasks()
//    {
//        ArrayList<Trace> resultTraces = new ArrayList<Trace>();
//        for (Task thisTask : taskContainer.getAppTasksAsArray())
//        {
//            resultTraces.add(buildTaskArrivalTimeWindowTrace(thisTask));
//        }
//        return resultTraces;
//    }
//
//    public Trace buildCompositionTrace()
//    {
//        return new Trace("Step2 Inf.", busyIntervalContainer.compositionInferencesToEvents(), new TimeLine());
//    }
//
//    public Trace buildSchedulingInferenceTrace()
//    {
//        ArrayList resultEvents = new ArrayList();
//        for ( BusyInterval thisBI : busyIntervalContainer.getBusyIntervals() ) {
//            resultEvents.addAll( thisBI.schedulingInference );
//        }
//        return new Trace("Scheduling", resultEvents, new TimeLine());
//    }
//
//    public ArrayList<Trace> buildResultTraces()
//    {
//        ArrayList<Trace> resultTraces = new ArrayList<>();
//
//        // Composition trace
//        resultTraces.add(buildCompositionTrace());
//
//        // Arrival time window traces
//        resultTraces.addAll(buildTaskArrivalTimeWindowTracesForAllTasks());
//
//        // Scheduling inference
//        resultTraces.add(buildSchedulingInferenceTrace());
//
//        return resultTraces;
//    }

    /* It will skip the busy intervals that have ambiguity for the given task. */
    public BusyIntervalEvent findShortestBusyIntervalContainingTask(Task inTask)
    {
        BusyIntervalEvent shortestBusyInterval = null;
        Boolean firstLoop = true;
        for (BusyIntervalEvent thisBusyInterval : busyIntervalContainer.findBusyIntervalsByTask(inTask))
        {
            /* Check whether this busy interval is ambiguous for inTask. */
            if (thisBusyInterval.getComposition().size() > 1)
            { // This busy interval has more than one inferences.
                int numOfInTask = 0;
                Boolean subFirstLoop = true;
                Boolean isAmbiguous = false;
                for (ArrayList<Task> thisInference : thisBusyInterval.getComposition())
                {
                    int thisNumOfInTask = 0;

                    /* Calculate the number of inTask contained in this inference. */
                    thisNumOfInTask = Collections.frequency(thisInference, inTask);

                    if (subFirstLoop == true) {
                        subFirstLoop = false;
                        numOfInTask = thisNumOfInTask;
                        continue;
                    }

                    if (numOfInTask != thisNumOfInTask)
                    {// Has ambiguity.
                        isAmbiguous = true;
                    }
                    else
                    {
                        // This inference of inTask is consistent with last one.
                        //continue; // Continue to check next.
                    }
                }

                if (isAmbiguous == true)
                {// This busy interval is ambiguous for inTask, thus skip.
                    continue;
                }
            }

            if (firstLoop == true)
            {
                shortestBusyInterval = thisBusyInterval;
                firstLoop = false;
                continue;
            }

            if (thisBusyInterval.getDuration() < shortestBusyInterval.getDuration())
                shortestBusyInterval = thisBusyInterval;

        }

        if (shortestBusyInterval == null) {
            //ProgMsg.debugPutline("shortestBusyInterval is null.");
        }

        return shortestBusyInterval;
    }

    public Boolean removeAmbiguousInferenceByArrivalTimeWindow()
    {
        for (BusyIntervalEvent thisBusyInterval : busyIntervalContainer.getBusyIntervals())
        {
            // If this busy interval has no ambiguous inference, then continue to next.
            if (thisBusyInterval.getComposition().size() == 1)
                continue;

            for (ArrayList<Task> thisInference : thisBusyInterval.getComposition())
            {
                /* Check if the number of thisTask is consistent with thisInference.
                *  If not, then thisInference may not be true answer.
                */

                // Iterate by tasks
                Boolean thisIsMismatch = false;
                for (Task thisTask : taskContainer.getAppTasksAsArray())
                {
                    /* Calculate the number of thisTask contained in this inference. */
                    int numOfThisTaskByInference = Collections.frequency(thisInference, thisTask);
                    int numOfThisTaskByWindow = calculateNumOfGivenTaskInBusyIntervalByArrivalTimeWindow(thisBusyInterval, thisTask);

                    if (numOfThisTaskByInference != numOfThisTaskByWindow) {
                        thisIsMismatch = true;
                        break;
                    }
                }

                if (thisIsMismatch == true) {
                    //ProgMsg.debugPutline("A mismatch inference is removing: %d", thisBusyInterval.getComposition().size());
                    thisBusyInterval.getComposition().remove(thisInference);
                    //ProgMsg.debugPutline("A mismatch inference is removed: %d", thisBusyInterval.getComposition().size());

                    /** Temporarily apply the following code to solve null pointer problem when removing
                     * an inference from thisBusyInterval.getComposition().
                     * Since at most 2 inferences are existed in a busy interval, removing one means another
                     * one is potentially the true answer. **/
                    break;
                    /** End **/
                }

            }

            if (thisBusyInterval.getComposition().size() == 0) {
                //ProgMsg.debugPutline("One of busy interval's inferences become 0! Terminating!");
                return false;
            }

        }
        return true;
    }

    public int calculateNumOfGivenTaskInBusyIntervalByArrivalTimeWindow(BusyIntervalEvent inBusyInterval, Task inTask)
    {
        Interval thisTaskArrivalWindow = new Interval(taskArrivalTimeWindows.get(inTask));
        Interval intervalBusyInterval = new Interval(inBusyInterval.getOrgBeginTimestamp(), inBusyInterval.getOrgEndTimestamp());

        Long shiftValue = findSmallestPeriodShiftValueWithIntersection(intervalBusyInterval, thisTaskArrivalWindow, inTask.getPeriod());
        if (shiftValue == null)
            return 0;

        thisTaskArrivalWindow.shift(shiftValue * inTask.getPeriod());

        // Check shifting direction
        int shiftingPositiveNegativeFactor = 1;
        if (shiftValue >= 0) {
            shiftingPositiveNegativeFactor = 1;
        }
        else {
            shiftingPositiveNegativeFactor = -1;
        }

        int countIntersectedTaskPeriod = 0;
        while (true)
        {
            if (thisTaskArrivalWindow.intersect(intervalBusyInterval) != null)
            {
                thisTaskArrivalWindow.shift(shiftingPositiveNegativeFactor * inTask.getPeriod());
                countIntersectedTaskPeriod++;
            }
            else
            {
                break;
            }
        }

        return countIntersectedTaskPeriod;
    }

    public EventContainer reconstructScheduleByArrivalWindows() {
        QuickFPSchedulerJobContainer schedulerJobContainer = new QuickFPSchedulerJobContainer();
        for ( BusyIntervalEvent thisBusyInterval : busyIntervalContainer.getBusyIntervals() ) {
            // Here we assume that the number of possible inferences is reduced to one, thus process the only one inference.
            ArrayList<Task> thisInference = thisBusyInterval.getFirstComposition();

            /* Start constructing arrival time sequence of all tasks in this busy interval. */
            for ( Task thisTask : taskContainer.getAppTasksAsArray() ) {
                if ( thisBusyInterval.containsComposition(thisTask) == false ) {
                    continue;
                }

                int numOfThisTask = Collections.frequency(thisInference, thisTask);
                Interval thisArrivalWindow = findClosestArrivalTimeOfTask( thisBusyInterval.getOrgBeginTimestamp(), thisTask );

                for (int loop=0; loop<numOfThisTask; loop++) {
                    /* Note that after added element will be sorted by time. */
                    //thisArrivalInference.add( thisArrivalWindow.getBegin(), thisTask );
                    schedulerJobContainer.add(new Job(thisTask, thisArrivalWindow.getBegin(), thisTask.getExecTime()));
                    thisArrivalWindow.shift( thisTask.getPeriod() );
                }

            } /* Arrival time arrangement for this busy interval finished. */

            /* Reconstruct the busy interval according to the arrival time of each task in this busy interval. */
            //QuickFixedPrioritySchedulerSimulator.constructSchedulingOfBusyIntervalByArrivalWindow( thisBusyInterval );
        }
        QuickFixedPrioritySchedulerSimulator rmSimulator = new QuickFixedPrioritySchedulerSimulator();
        rmSimulator.setTaskSet(taskContainer);
        rmSimulator.simJobs(schedulerJobContainer);
        return rmSimulator.getSimEventContainer();
    }

//    public Boolean reconstructCompositionOfBusyIntervalByArrivalTimeWindows()
//    {
//        for ( BusyIntervalEvent thisBusyInterval : busyIntervalContainer.getBusyIntervals() ) {
//            // Here we assume that the number of possible inferences is reduced to one, thus process the only one inference.
//            ArrayList<Task> thisInference = thisBusyInterval.getFirstComposition();
//            TaskArrivalEventContainer thisArrivalInference = thisBusyInterval.arrivalInference;
//            thisArrivalInference.clear();
//
//            /* Start constructing arrival time sequence of all tasks in this busy interval. */
//            for ( Task thisTask : taskContainer.getAppTasksAsArray() ) {
//                if ( thisBusyInterval.containsComposition(thisTask) == false ) {
//                    continue;
//                }
//
//                int numOfThisTask = Collections.frequency(thisInference, thisTask);
//                Interval thisArrivalWindow = findClosestArrivalTimeOfTask( thisBusyInterval.getOrgBeginTimestamp(), thisTask );
//
//                for (int loop=0; loop<numOfThisTask; loop++) {
//                    /* Note that after added element will be sorted by time. */
//                    thisArrivalInference.add( thisArrivalWindow.getBegin(), thisTask );
//                    thisArrivalWindow.shift( thisTask.getPeriod() );
//                }
//
//            } /* Arrival time arrangement for this busy interval finished. */
//
//            /* Reconstruct the busy interval according to the arrival time of each task in this busy interval. */
//            QuickFixedPrioritySchedulerSimulator.constructSchedulingOfBusyIntervalByArrivalWindow( thisBusyInterval );
//
//        }
//
//        return true;
//
//    }

//    public Task findEarliestArrivalTimeTask( int referenceTimePoint, ArrayList<Task> inTasks )
//    {
//        Interval earliestArrivalWindow = null;
//        Task earliestArrivalTask = null;
//
//        Boolean firstLoop = true;
//        for ( Task thisTask : inTasks ) {
//            if ( firstLoop == true ) {
//                firstLoop = false;
//                earliestArrivalWindow = findClosestArrivalTimeOfTask( referenceTimePoint, thisTask );
//                earliestArrivalTask = thisTask;
//                continue;
//            }
//
//            Interval thisEarliestArrivalWindow;
//            thisEarliestArrivalWindow = findClosestArrivalTimeOfTask( referenceTimePoint, thisTask );
//
//            // Check whether this task has earliest arrival time and higher priority.
//            // Note that, in priority, a smaller number stands for a higher priority.
//            if ( ( thisEarliestArrivalWindow.getBegin() <= earliestArrivalWindow.getBegin() )
//                    && ( thisTask.getPriority() < earliestArrivalTask.getPriority() ) ) {
//                earliestArrivalWindow = thisEarliestArrivalWindow;
//                earliestArrivalTask = thisTask;
//            }
//        }
//
//        return earliestArrivalTask;
//
//    }


    // This will find the first arrival time after the reference point.
    public Interval findClosestArrivalTimeOfTask( long referenceTimePoint, Task inTask )
    {
        // Create a new Interval instance based on input value.
        Interval taskWindow = new Interval( taskArrivalTimeWindows.get( inTask ) );

        long difference = referenceTimePoint - taskWindow.getBegin();
        long shiftFactor = difference / inTask.getPeriod();
        if ( difference % inTask.getPeriod() == 0 ) {
            // shiftFactor remains unchanged.
        } else if ( difference > 0 ) {
            // referencePoint is bigger
            shiftFactor++;
        } else {
            // reference Point is smaller
            // shiftFactor is negative and will remain the unchanged.
        }

        taskWindow.shift( shiftFactor * inTask.getPeriod() );
        return taskWindow;
    }
}