package me.cychen.rts.scheduleak.restricted;

import com.sun.xml.internal.bind.v2.model.annotation.Quick;
import me.cychen.rts.RtsConfig;
import me.cychen.rts.event.EventContainer;
import me.cychen.rts.event.TaskArrivalEventContainer;
import me.cychen.rts.event.TaskInstantEvent;
import me.cychen.rts.framework.Job;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.rts.scheduleak.Interval;
import me.cychen.rts.simulator.QuickFPSchedulerJobContainer;
import me.cychen.rts.simulator.QuickFixedPrioritySchedulerSimulator;
import me.cychen.util.ProgMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ScheduLeakRestricted {
    private TaskSet taskContainer;
    private BusyIntervalContainer orgBusyIntervalContainer;
    private BusyIntervalContainer processingBusyIntervalContainer;
    private HashMap<Task, ArrayList<Interval>> taskArrivalTimeWindows = new HashMap<Task, ArrayList<Interval>>();

    private HashMap<Long, ArrayList<ArrayList<Task>>> biDurationCompositionLookupTable = new HashMap();

    public ScheduLeakRestricted(TaskSet inTaskContainer, BusyIntervalContainer inBusyIntervalContainer)
    {
        taskContainer = inTaskContainer;
        orgBusyIntervalContainer = inBusyIntervalContainer;
        processingBusyIntervalContainer = new BusyIntervalContainer(orgBusyIntervalContainer.getBusyIntervals()); //inBusyIntervalContainer;
    }

    public Boolean runDecompositionStep1()
    {
        //ProgMsg.debugPutline("Initial busy interval count: %d", processingBusyIntervalContainer.size());

        // TODO: test for skipping the first hyper period.
        //processingBusyIntervalContainer.removeBusyIntervalsBeforeTimeStamp((int)taskContainer.calHyperPeriod()*2);
        //orgBusyIntervalContainer.removeBusyIntervalsBeforeTimeStamp((int)taskContainer.calHyperPeriod()*2);

        processingBusyIntervalContainer.removeBusyIntervalsBeforeButExcludeTimeStamp((int)taskContainer.calHyperPeriod()*2);
        orgBusyIntervalContainer.removeBusyIntervalsBeforeButExcludeTimeStamp((int)taskContainer.calHyperPeriod()*2);

        // Remove the last one since it may not be complete.
        //processingBusyIntervalContainer.removeTheLastBusyInterval();
        //orgBusyIntervalContainer.removeTheLastBusyInterval();

        //ProgMsg.debugPutline("Estimating N_k(tau_i) values for %d busy intervals.", processingBusyIntervalContainer.size());

        // Step one: finding n values for every task in every busy interval.
        ArrayList<BusyInterval> biToBeRemoved = new ArrayList<>();
        for (BusyInterval thisBusyInterval : processingBusyIntervalContainer.getBusyIntervals())
        {
            //thisBusyInterval.setComposition(calculateComposition(thisBusyInterval));

            // This is to calculate Nk values for each task in each busy interval.
            if (calculateAndSetNkValueOfBusyInterval(thisBusyInterval) == false) {
                // It returns false if no Nk inference has been reached.
                biToBeRemoved.add(thisBusyInterval);
            }
        }

        /* Remove those busy intervals that have no inferences. */
        //ProgMsg.sysPutLine("%d busy intervals have been removed due to incapable of inferring their Nk values.", biToBeRemoved.size());
        processingBusyIntervalContainer.busyIntervals.removeAll(biToBeRemoved);

        return true;
    }

    //public Boolean runDecompositionStep2() throws RuntimeException
    public void runDecompositionStep2() throws RuntimeException
    {
        // Step two: creating arrival time window for each task by processing the result from step one.

        //ArrayList<Trace> debugTraces = new ArrayList<>();

        int passCount = 0;
        while (true) {
            passCount++;

            //ProgMsg.debugPutline("Start calculating arrival windows: %d-th pass.", passCount);
            calculateArrivalTimeOfAllTasks();

            //debugTraces.addAll(buildTaskArrivalTimeWindowTracesForAllTasks());

            //ProgMsg.debugPutline("Removing ambiguous inference: %d-th pass.", passCount);
            Boolean isSomethingChanged = removeAmbiguousInferenceByArrivalTimeWindow();

            //debugTraces.add(buildCompositionTrace());

            if (isSomethingChanged == false) {

                /* We comment the following block out because it may remove correct arrival window with a wrong
                 * composition inference caused by variations. */
                /* Check if any task has unsolved arrival windows. */
//                Boolean needsToSolveAmbiguousArrivalWindows = false;
//                for (Task thisTask : taskContainer.getAppTasksAsArray()) {
//                    if (taskArrivalTimeWindows.get(thisTask).size() >= 2) {
//                        needsToSolveAmbiguousArrivalWindows = true;
//                        break;
//                    }
//                }
//                if (needsToSolveAmbiguousArrivalWindows == true) {
//                    ProgMsg.debugPutline("Try to solve ambiguous arrival windows.");
//                    if (removeAmbiguousArrivalWindowsByInferredCompositions()) {
//                        if (removeAmbiguousInferenceByArrivalTimeWindow()) {
//                            passCount+=1;
//                            ProgMsg.debugPutline("Some ambiguous arrival windows have been invalidated.");
//                            continue;
//                        }
//                    } else {
//                        ProgMsg.debugErrPutline("Unable to solve ambiguous arrival windows. Use the random selection measure.");
//
//                    }
//                }

                break;
            }
        }
        //ProgMsg.debugPutline("Total pass: %d pass.", passCount);

        /* Check whether there are multiple arrival time windows for a task. */
        String exceptionString = "";
        Boolean hasException = false;
        for (Task thisTask : taskContainer.getAppTasksAsArray()) {
            if (taskArrivalTimeWindows.get(thisTask) == null) {
                hasException = true;
                exceptionString += thisTask.getTitle() + " has no arrival window.\t";
            } else if (taskArrivalTimeWindows.get(thisTask).size() > 1) {
                hasException = true;
                exceptionString += thisTask.getTitle() + " has " + String.valueOf(taskArrivalTimeWindows.get(thisTask).size()) + " arrival windows.\t";
//                ProgMsg.errPutline("%s chooses random window from %d.", thisTask.getTitle(), taskArrivalTimeWindows.get(thisTask).size() );
                //Interval firstWindow = taskArrivalTimeWindows.get(thisTask).get(0);
                //taskArrivalTimeWindows.get(thisTask).clear();
                //taskArrivalTimeWindows.get(thisTask).add(firstWindow);
            }
        }
        if (hasException) {
            //ProgMsg.errPutline("%s has no arrival window.", exceptionString);
            //ProgMsg.errPutline("%s arrival time window not yet fixed.", exceptionString);
            //ProgMsg.errPutline("The first arrival window will be used for those ambiguous tasks.");
            //throw new RuntimeException(String.format("%s arrival time window not yet fixed.", exceptionString));

        } else {
            //ProgMsg.debugPutline("All windows have been solved and fixed!!");
        }

        //return debugTraces;
        //return true;
    }

    public void setZeroInitialOffsetArrivalWindows() {
        taskArrivalTimeWindows.clear();
        for ( Task thisTask : taskContainer.getAppTasksAsArray() ) {
            ArrayList<Interval> thisTaskArrivalWindow = new ArrayList<>();
            thisTaskArrivalWindow.add(new Interval(0,0));
            taskArrivalTimeWindows.put(thisTask, thisTaskArrivalWindow);
        }
    }

    public void setRandomInitialOffsetArrivalWindows() {
        taskArrivalTimeWindows.clear();
        for ( Task thisTask : taskContainer.getAppTasksAsArray() ) {
            int randomInitialOffset = (int) (Math.random() * thisTask.getWcet());
            ArrayList<Interval> thisTaskArrivalWindow = new ArrayList<>();
            thisTaskArrivalWindow.add(new Interval(randomInitialOffset, randomInitialOffset));
            taskArrivalTimeWindows.put(thisTask, thisTaskArrivalWindow);
        }
    }

    public EventContainer runDecompositionStep3()
    {
        //reconstructCompositionOfBusyIntervalByArrivalTimeWindows();
        return reconstructScheduleByArrivalWindows();
    }

    /* This is used as a comparison (always infer first instant as arrival time - method). */
    public Boolean runZeroDecomposition() throws RuntimeException
    {

        setZeroInitialOffsetArrivalWindows();

        // Step three: arrival time to scheduling.
        runDecompositionStep3();

        return true;
    }

    /* This is used as a comparison (random method). */
    public Boolean runRandomDecomposition() throws RuntimeException
    {

        setRandomInitialOffsetArrivalWindows();

        // Step three: arrival time to scheduling.
        runDecompositionStep3();

        return true;
    }

    /* Use this one to run the algorithm. */
    public EventContainer runDecomposition() throws RuntimeException
    {

        // Step one: finding n values for every task in every busy interval.
        runDecompositionStep1();

        // Step two: creating arrival time window for each task by processing the result from step one.
        runDecompositionStep2();

        // Step three: arrival time to scheduling.
        return runDecompositionStep3();

    }

//    public EventContainer runDecompositionWithErrors()
//    {
//        // Step one: finding n values for every task in every busy interval.
//        for (BusyInterval thisBusyInterval : processingBusyIntervalContainer.getBusyIntervals())
//        {
//            thisBusyInterval.setComposition(calculateCompositionWithErrors(thisBusyInterval));
//        }
//
//        // Step two: creating arrival time window for each task by processing the result from step one.
//        runDecompositionStep2();
//
//        // Step three: arrival time to scheduling.
//        return runDecompositionStep3();
//    }

    public Boolean calculateAndSetNkValueOfBusyInterval(BusyInterval inBusyInterval) {
        long duration = inBusyInterval.getIntervalNs();

        ArrayList<ArrayList<Task>> resultCompositions;
        if (biDurationCompositionLookupTable.get(duration) == null) {

            HashMap<Task, ArrayList<Integer>> nkOfTasks = new HashMap<Task, ArrayList<Integer>>();

            /* Calculate Nk of each task. */
            for (Object thisObject : taskContainer.getAppTasksAsArray()) {
                Task thisTask = (Task) thisObject;
                ArrayList<Integer> thisResult = new ArrayList<Integer>();

                long thisP = thisTask.getPeriod();
                long thisC = thisTask.getExecTime();
                long thisCLower = thisTask.getExecTimeLowerBound();
                //int thisCUpper = thisTask.getComputationTimeUpperBound();

                int numberOfCompletePeriods = (int) Math.floor(duration / thisP);
                long subIntervalNs = duration - numberOfCompletePeriods * thisP;

                //if (subIntervalNs < thisC) {// This task can only have occurred 0 time in this sub-interval.
                if (subIntervalNs < thisCLower) {// This task can only have occurred 0 time in this sub-interval.
                    thisResult.add(numberOfCompletePeriods + 0);
                    //} else if (subIntervalNs < (thisP - thisC)) {// This task can have occurred 0 or 1 time in this sub-interval.
                } else if (subIntervalNs < (thisP - thisCLower)) {// This task can have occurred 0 or 1 time in this sub-interval.
                    thisResult.add(numberOfCompletePeriods + 0);
                    thisResult.add(numberOfCompletePeriods + 1);
                } else // if (subIntervalNs < thisP)
                {// This task can only have occurred 1 times in this sub-interval.
                    thisResult.add(numberOfCompletePeriods + 1);
                }

                nkOfTasks.put(thisTask, thisResult);
            }

            // Find Ns match this interval.
            //ArrayList<ArrayList<Task>> resultCompositions;// = new ArrayList<HashMap<Integer, Integer>>();//HashMap<Integer, Integer>();

            resultCompositions = findMatchingCompositions(nkOfTasks, duration, null);
            biDurationCompositionLookupTable.put(duration, resultCompositions);
        } else {
            resultCompositions = (ArrayList)biDurationCompositionLookupTable.get(duration).clone();
        }


        if (resultCompositions.size() != 0) {
            inBusyInterval.setComposition(resultCompositions);

            /* Update Nk values. */
            inBusyInterval.updateNkValuesFromCompositions(taskContainer);

            return true;
        } else {
            // We can't get any inference for this busy interval.
            return false;
        }

    }

    public ArrayList<ArrayList<Task>> calculateCompositionWithErrors(BusyInterval inBusyInterval)
    {
        long intervalNs = inBusyInterval.getIntervalNs();
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

    private ArrayList<ArrayList<Task>> findMatchingCompositions(HashMap<Task, ArrayList<Integer>> inNOfTasks, long inTargetInterval, HashMap<Task, Integer> inProcessingNOfTasks)
    {
        ArrayList<ArrayList<Task>> resultCompositions = new ArrayList<ArrayList<Task>>();

        if (inNOfTasks.isEmpty())
        {
            /* Compute the interval from current compositions. */
            //int compositeInterval = 0;
            int compositeIntervalLowerBound = 0;
            int compositeIntervalUpperBound = 0;

            for (Task thisTask : inProcessingNOfTasks.keySet())
            {
                //compositeInterval += thisTask.getComputationTimeNs() * inProcessingNOfTasks.get(thisTask);
                compositeIntervalLowerBound += thisTask.getExecTimeLowerBound() * inProcessingNOfTasks.get(thisTask);
                compositeIntervalUpperBound += thisTask.getExecTimeUpperBound() * inProcessingNOfTasks.get(thisTask);
            }
//            System.out.format("End of recursive calls, %d\r\n", compositeInterval);

            /* Check whether current composite interval equals target interval or not. */
            //if (compositeInterval == inTargetInterval)
            if ((compositeIntervalLowerBound <= inTargetInterval) && (inTargetInterval <= compositeIntervalUpperBound)) // Including 20% error
            {
//                System.out.println("Matched!!");
//                System.out.println(inProcessingNOfTasks);

                ArrayList thisResultComposition = new ArrayList<Task>();
                for (Task thisTask : inProcessingNOfTasks.keySet())
                {
                    for (int loop=0; loop<inProcessingNOfTasks.get(thisTask); loop++)
                    {
                        thisResultComposition.add(thisTask);
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
        Task thisTask = inNOfTasks.keySet().iterator().next();
        ArrayList<Integer> nOfThisTask = inNOfTasks.get(thisTask);
        HashMap<Task, ArrayList<Integer>> restNOfTasks = new HashMap<Task, ArrayList<Integer>>(inNOfTasks);
        restNOfTasks.remove(thisTask);

        if (inProcessingNOfTasks == null)
        { // For the first time the program gets here, inProcessingNOfTasks has to be initialized.
            inProcessingNOfTasks = new HashMap<Task, Integer>();
        }

        // Iterate every possible n value of current task and pass the value down recursively.
        for (Integer thisN: nOfThisTask)
        {
            inProcessingNOfTasks.put(thisTask, thisN);
            resultCompositions.addAll(findMatchingCompositions(restNOfTasks, inTargetInterval, inProcessingNOfTasks));
            inProcessingNOfTasks.remove(thisTask);
        }
        return resultCompositions;
    }

    private ArrayList<ArrayList<Task>> findMatchingCompositionsWithErrors(HashMap<Integer, ArrayList<Integer>> inNOfTasks, long inTargetInterval, HashMap<Integer, Integer> inProcessingNOfTasks)
    {
        ArrayList<ArrayList<Task>> resultCompositions = new ArrayList<ArrayList<Task>>();
        if (inNOfTasks.isEmpty())
        {
            /* Compute the interval from current compositions. */
            long compositeInterval = 0;
            long accumulatedCErrors = 0;
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

//    private ArrayList<ArrayList<Task>> getWhateverCompositions(HashMap<Integer, ArrayList<Integer>> inNOfTasks, int inTargetInterval, HashMap<Integer, Integer> inProcessingNOfTasks)
//    {
//        ArrayList<ArrayList<Task>> resultCompositions = new ArrayList<ArrayList<Task>>();
//
//        if (inNOfTasks.isEmpty())
//        {
//            /* Compute the interval from current compositions. */
////            int compositeInterval = 0;
////            for (int thisTaskId : inProcessingNOfTasks.keySet())
////            {
////                compositeInterval += taskContainer.getTaskById(thisTaskId).getComputationTimeNs() * inProcessingNOfTasks.get(thisTaskId);
////            }
////            System.out.format("End of recursive calls, %d\r\n", compositeInterval);
//
//            /* Check whether current composite interval equals target interval or not. */
//            //if (compositeInterval == inTargetInterval)
//            if (true)
//            {
////                System.out.println("Matched!!");
////                System.out.println(inProcessingNOfTasks);
//
//                ArrayList thisResultComposition = new ArrayList<Task>();
//                for (int thisTaskId : inProcessingNOfTasks.keySet())
//                {
//                    for (int loop=0; loop<inProcessingNOfTasks.get(thisTaskId); loop++)
//                    {
//                        thisResultComposition.add(taskContainer.getTaskById(thisTaskId));
//                    }
//                }
//                resultCompositions.add(thisResultComposition);
//                return resultCompositions;
//            }
//            else
//            {
//                /* Because addAll() doesn't accept null pointer, thus returning empty arrayList instead. */
//                //return null;
//                return resultCompositions;
//            }
//        }
//
//        /* Select an unsorted task to process and create a list which contains rest of n values of unsorted tasks. */
//        int thisTaskId = inNOfTasks.keySet().iterator().next();
//        ArrayList<Integer> nOfThisTask = inNOfTasks.get(thisTaskId);
//        HashMap<Integer, ArrayList<Integer>> restNOfTasks = new HashMap<Integer, ArrayList<Integer>>(inNOfTasks);
//        restNOfTasks.remove(thisTaskId);
//
//        if (inProcessingNOfTasks == null)
//        { // For the first time the program gets here, inProcessingNOfTasks has to be initialized.
//            inProcessingNOfTasks = new HashMap<Integer, Integer>();
//        }
//
//        // Iterate every possible n value of current task and pass the value down recursively.
//        for (Integer thisN: nOfThisTask)
//        {
//            inProcessingNOfTasks.put(thisTaskId, thisN);
//            resultCompositions.addAll(findMatchingCompositions(restNOfTasks, inTargetInterval, inProcessingNOfTasks));
//            inProcessingNOfTasks.remove(thisTaskId);
//        }
//        return resultCompositions;
//    }

    public Boolean areEqualWithinError(long inNum01, long inNum02, long inErrorRange)
    {
        return Math.abs(inNum01-inNum02)<=inErrorRange ? true : false;
    }

    public Boolean calculateArrivalTimeOfAllTasks() throws RuntimeException
    {
        for (Task thisTask : taskContainer.getAppTasksAsArray()){
            ArrivalSegmentsContainer thisArrivalSegmentsContainer = new ArrivalSegmentsContainer(thisTask, new BusyIntervalContainer(processingBusyIntervalContainer.findBusyIntervalsByTask(thisTask)));
            if (thisArrivalSegmentsContainer.calculateFinalArrivalTimeWindow() == true) {
                taskArrivalTimeWindows.put(thisTask, thisArrivalSegmentsContainer.getFinalArrivalTimeWindow());
            } else {
                // Oh no... not good.
                // The program will never reach here since the previous function will throw the exception.
                //ProgMsg.debugErrPutline("Oh no. %s has 0 window", thisTask.getTitle());
            }
        }
        return true;
    }



    public Interval calculateArrivalTimeWindowOfTask(Task inTask, Interval inFirstWindow) throws RuntimeException
    {
        ArrayList<BusyInterval> thisTaskBusyIntervals;

        /* Find the busy intervals that contain inTask. */
        thisTaskBusyIntervals = processingBusyIntervalContainer.findBusyIntervalsByTask(inTask);

        Interval firstWindow = null;

        if (inFirstWindow != null) {
            firstWindow = inFirstWindow;
        }
        else {
            BusyInterval shortestBusyIntervalContainingTask = findShortestBusyIntervalContainingTask(inTask, thisTaskBusyIntervals);
            if (shortestBusyIntervalContainingTask == null) {
                // Unable to find the initial busy interval for calculating the arrival time window.
                ProgMsg.errPutline("Unable to find the initial busy interval for calculating the arrival time window for %s.", inTask.getTitle());
                //throw new RuntimeException(String.format("Unable to find the initial busy interval for calculating the arrival time window for task '%s'.", inTask.getTitle()));
                return null;
            } else {
                firstWindow = calculateArrivalTimeWindowOfTaskInABusyInterval(shortestBusyIntervalContainingTask, inTask);
            }
        }
        // Move the window to around zero point.
        firstWindow.shift(-(firstWindow.getEnd() / inTask.getPeriod()) * inTask.getPeriod());
        //ProgMsg.debugPutline("first window of %s, %d:%d ms", inTask.getTitle(), (int)(firstWindow.getBegin()*ProgConfig.TIMESTAMP_UNIT_TO_MS_MULTIPLIER), (int)(firstWindow.getEnd()*ProgConfig.TIMESTAMP_UNIT_TO_MS_MULTIPLIER));



        Boolean anyoneHasTwoIntersectionsInTheEnd = false;
        Interval lastWindow = new Interval(firstWindow);

        anyoneHasTwoIntersectionsInTheEnd = false;
        lastWindow.setBegin(firstWindow.getBegin());
        lastWindow.setEnd(firstWindow.getEnd());

        for (BusyInterval thisBusyInterval : thisTaskBusyIntervals) {

            // Check whether this busy interval has been solved for inTask.
            if (thisBusyInterval.getIsArrivalTimeWindowParsedAndFixed(inTask) != null) {
                if (thisBusyInterval.getIsArrivalTimeWindowParsedAndFixed(inTask) == true) {
                    // This busy interval has been parsed and the window can not contribute more, thus skip.
                    continue;
                }
            }


                /* Get current arrival window and check whether the window is valid or not. */
            Interval thisWindow = calculateArrivalTimeWindowOfTaskInABusyInterval(thisBusyInterval, inTask);
            if (thisWindow == null) {
                    /* This could happen if it contains 0 or 1 arrival. */
                continue;
            }

            Long smallestShiftPeriodValue = findSmallestPeriodShiftValueWithIntersection(firstWindow, thisWindow, inTask.getPeriod());

            if (smallestShiftPeriodValue == null) {// No intersection.
                ProgMsg.errPutline("No intersection! Should not ever happen!!");
                ProgMsg.errPutline("\t- %d:%d ms", (int) (thisWindow.getBegin() * RtsConfig.TIMESTAMP_UNIT_TO_MS_MULTIPLIER), (int) (thisWindow.getEnd() * RtsConfig.TIMESTAMP_UNIT_TO_MS_MULTIPLIER));
                continue;
            } else {// Has intersection.
                Interval shiftedThisWindow = new Interval(thisWindow);
                shiftedThisWindow.shift(smallestShiftPeriodValue * inTask.getPeriod());

                // Shift one more -period to see if there is another intersection
                shiftedThisWindow.shift(-inTask.getPeriod());
                if (firstWindow.intersect(shiftedThisWindow) != null) {// Has two intersections, thus skip intersecting this window.
                    //ProgMsg.debugPutline("Two windows have multiple intersections!! Skip intersecting this window for now.");
                    anyoneHasTwoIntersectionsInTheEnd = true;

                    // Mark this busy interval as not solved.
                    thisBusyInterval.setIsArrivalTimeWindowParsedAndFixed(inTask, false);
                    continue;
                }
                // No intersection with moving -1 period.

                // Now testing the intersection with moving +1 period.
                // Shift one more +period to see if there is another intersection
                shiftedThisWindow.shift(2 * inTask.getPeriod());
                if (firstWindow.intersect(shiftedThisWindow) != null) {// Has two intersections, thus skip intersecting this window.
                    //ProgMsg.debugPutline("Two windows have multiple intersections!! Skip intersecting this window for now.");
                    anyoneHasTwoIntersectionsInTheEnd = true;

                    // Mark this busy interval as not solved.
                    thisBusyInterval.setIsArrivalTimeWindowParsedAndFixed(inTask, false);
                    continue;
                }

                // In the end, it has only one intersection, so apply the intersection to firstWindow.
                thisWindow.shift(smallestShiftPeriodValue * inTask.getPeriod());
                firstWindow = firstWindow.intersect(thisWindow);
            }

        } // End of for loop.

        // TODO: Check this variable to see whether we have two intersections for a pair of window in the end.
        if (anyoneHasTwoIntersectionsInTheEnd == true) {
//            ProgMsg.errPutline("Still got someone having two intersections in the last parse for %s", inTask.getTitle());
        } else {
//            ProgMsg.debugPutline("2-intersection test pass for %s", inTask.getTitle());
        }

        assert firstWindow!=null : "No arrival window is found in all busy intervals for this task";
        if (firstWindow == null) {
            ProgMsg.debugPutline("No arrival window is found in all busy intervals for %s task", inTask.getTitle());
        }

        // Move the window to around zero point.
        firstWindow.shift(-(firstWindow.getBegin() / inTask.getPeriod()) * inTask.getPeriod());

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

    public Interval calculateArrivalTimeWindowOfTaskInABusyInterval(BusyInterval inBusyInterval, Task inTask)
    {
        // First, check whether this busy interval contains inTask or not.
        if (inBusyInterval.containsTaskCheckedByNkValues(inTask) == false)
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
                        // TODO: watch this if too many busy intervals are skipped for the inference of arrival window for a task.
                        //ProgMsg.debugPutline("One of the inference contains 0 of %s.", inTask.getTitle());
                        return null;
                    }
                    else
                    {
                        // Has ambiguity but it's doable. (different inferences contain different number of inTask but not zero.)
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
        long resultBeginTime = inBusyInterval.getBeginTimeStampNs();
        long resultEndTime = Math.min(inBusyInterval.getEndTimeStampNs() - inTask.getExecTime(),
                inBusyInterval.getBeginTimeStampNs() + inTask.getPeriod() - inTask.getExecTime());
        Interval resultArrivalTimeWindow = new Interval(resultBeginTime, resultEndTime);

        /* If it has no ambiguity and has more than one inTask, then narrow the window with the one got from last period in this busy interval. */
        if (hasAmbiguousInferenceForThisTask==false)
        {
            // No ambiguity means only one inference is available, thus just get the first inference from busy interval.
            int numOfInTask = Collections.frequency(inBusyInterval.getFirstComposition(), inTask);
            if (numOfInTask > 1)
            {
                // Calculate the window of the last period in this busy interval.
                long lastPeriodBeginTime = inBusyInterval.getBeginTimeStampNs() + inTask.getPeriod()*(numOfInTask-1);
                long lastPeriodEndTime = inBusyInterval.getEndTimeStampNs();
                Interval lastPeriodArrivalTimeWindow = new Interval(lastPeriodBeginTime, lastPeriodEndTime-inTask.getExecTime());

                // Shift the window of last period to the first window
                lastPeriodArrivalTimeWindow.shift( -(inTask.getPeriod()*(numOfInTask - 1)) );

                // Get intersection of two.
                resultArrivalTimeWindow = resultArrivalTimeWindow.intersect(lastPeriodArrivalTimeWindow);

                assert (resultArrivalTimeWindow!=null) : "Got an empty Arrival Time Window from the intersection.";
                if (resultArrivalTimeWindow == null)
                { // It should not ever happen.
                    ProgMsg.debugPutline("Got an empty Arrival Time Window from the intersection.");
                }
            }

            // Mark this busy interval as solved for inTask.
            inBusyInterval.setIsArrivalTimeWindowParsedAndFixed(inTask, true);

        } else { // (hasAmbiguousInferenceForThisTask == true)
            // It means that for sure we know there is inTask in this busy interval,
            // but the number of inTask in this busy interval remain unknown since different inferences have inconsistent guess.
            // ProgMsg.debugPutline("Has ambiguity to be solved.");

            // resultArrivalTimeWindow will be the arrival time window then.
        }

        /** End **/

        return resultArrivalTimeWindow;
    }

//    public ArrayList<Trace> buildTaskArrivalTimeWindowTrace(Task inTask)
//    {
//        if (taskArrivalTimeWindows.get(inTask) == null)
//        {
//            ProgMsg.debugPutline("%s task has null arrival time window! Can't process.", inTask.getTitle());
//            return null;
//        }
//
//        ArrayList<Trace> resultTraces = new ArrayList<>();
//
//        for (Interval taskArrivalTimeWindow : taskArrivalTimeWindows.get(inTask)) {
//            //Interval taskArrivalTimeWindow = taskArrivalTimeWindows.get(inTask);
//            long windowLength = taskArrivalTimeWindow.getLength();
//
//            ArrayList<IntervalEvent> intervalEvents = new ArrayList<>();
//            int thisWindowBeginTime = taskArrivalTimeWindow.getBegin(); // Initialize window time with the first window.
//            int taskPeriod = inTask.getPeriodNs();
//            int endTime = orgBusyIntervalContainer.getEndTime();
//
//            while ((thisWindowBeginTime + windowLength) <= endTime) {
//                IntervalEvent thisIntervalEvent = new IntervalEvent(thisWindowBeginTime, thisWindowBeginTime + windowLength);
//                thisIntervalEvent.setColor(inTask.getTaskColor());
//                thisIntervalEvent.enableTexture();
//                intervalEvents.add(thisIntervalEvent);
//
//                thisWindowBeginTime += taskPeriod;
//            }
//            resultTraces.add(new Trace(inTask.getTitle() + " Arr. Window", inTask, intervalEvents, new TimeLine(), Trace.TRACE_TYPE_OTHER));
//        }
//
//        return resultTraces;
//    }
//
//    public ArrayList<Trace> buildTaskArrivalTimeWindowTracesForAllTasks()
//    {
//        ArrayList<Trace> resultTraces = new ArrayList<Trace>();
//        for (Task thisTask : taskContainer.getAppTasksAsArray())
//        {
//            resultTraces.addAll(buildTaskArrivalTimeWindowTrace(thisTask));
//        }
//        return resultTraces;
//    }
//
//    public Trace buildCompositionTrace()
//    {
//        return new Trace("Step2 Inf.", processingBusyIntervalContainer.compositionInferencesToEvents(), new TimeLine());
//    }
//
//    public Trace buildSchedulingInferenceTrace()
//    {
//        ArrayList resultEvents = new ArrayList();
//        for ( BusyInterval thisBI : orgBusyIntervalContainer.getBusyIntervals() ) {
//            resultEvents.addAll( thisBI.schedulingInference );
//        }
//        return new Trace("Inf. Schedule", resultEvents, new TimeLine());
//    }
//
//    public ArrayList<Trace> buildResultTraces()
//    {
//        ArrayList<Trace> resultTraces = new ArrayList<>();
//
//        // Scheduling inference
//        resultTraces.add(buildSchedulingInferenceTrace());
//
//        // Composition trace (inference of N values)
//        //resultTraces.add(buildCompositionTrace());
//
//        // Arrival time window traces
//        resultTraces.addAll(buildTaskArrivalTimeWindowTracesForAllTasks());
//
//        return resultTraces;
//    }

    /* It will skip the busy intervals that have ambiguity for the given task. */
    public BusyInterval findShortestBusyIntervalContainingTask(Task inTask, ArrayList<BusyInterval> inBusyIntervals)
    {
        BusyInterval shortestBusyInterval = null;
        Boolean firstLoop = true;
        for (BusyInterval thisBusyInterval : inBusyIntervals)
        {
            /* Check whether this busy interval is ambiguous for inTask. */
            if (thisBusyInterval.getComposition().size() > 1)
            { // This busy interval has more than one inferences.

                Boolean hasValueZero = false;
                for (ArrayList<Task> thisInference : thisBusyInterval.getComposition())
                {
                    int thisNumOfInTask = 0;

                    /* Calculate the number of inTask contained in this inference. */
                    thisNumOfInTask = Collections.frequency(thisInference, inTask);

                    if (thisNumOfInTask == 0) {
                        hasValueZero = true;
                        break;
                    }
                }

                if (hasValueZero == true)
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

            if (thisBusyInterval.getIntervalNs() < shortestBusyInterval.getIntervalNs())
                shortestBusyInterval = thisBusyInterval;

        }

        if (shortestBusyInterval == null) {
            //ProgMsg.debugPutline("shortestBusyInterval is null.");
        }

        return shortestBusyInterval;
    }

    /**
     * Use the existed arrival time window of each task to eliminate invalid inferences in each busy interval.
     * @return true for one or more ambiguous inferences have been removed in this pass, false for nothing has been changed.
     */
    public Boolean removeAmbiguousInferenceByArrivalTimeWindow()
    {
        Boolean isSomethingChanged = false;
        for (BusyInterval thisBusyInterval : processingBusyIntervalContainer.getBusyIntervals())
        {

            if (thisBusyInterval.getComposition().size() == 1) {

                // If this busy interval has no ambiguous inference, then it's best for removing ambiguity of the windows.
                // TODO:
                continue;
            }

            int orgNumOfInferences = thisBusyInterval.getComposition().size();
            ArrayList<ArrayList<Task>> potentialInferences = new ArrayList<>();
            potentialInferences.addAll(thisBusyInterval.getComposition());

            for (Task thisTask : taskContainer.getAppTasksAsArray()) {
                int numOfThisTaskByWindow = calculateNumOfGivenTaskInBusyIntervalByArrivalTimeWindow(thisBusyInterval, thisTask);
                if (numOfThisTaskByWindow == -1) {
                    // Skip this one for now since arrival window for this task is not yet ready but is still possible to be the answer.
                    continue;
                }

                Boolean prettySureOfNumOfThisTaskByWindow = false;
                if (taskArrivalTimeWindows.get(thisTask).size() == 1) {
                    prettySureOfNumOfThisTaskByWindow = true;
                }

                ArrayList<ArrayList<Task>> compositionsToBeRemoved = new ArrayList<>();
                for (ArrayList<Task> thisInference : potentialInferences) {
                    int numOfThisTaskByInference = Collections.frequency(thisInference, thisTask);
                    // TODO: !!Bug!! if the arrival window is too big, then it's likely to infer incorrect number. ###FIXED!!!?



                    // If a task has only one window, then when it checks a busy interval, if within this period it only overlaps this one, then we are sure about the Nki value.
                    if (prettySureOfNumOfThisTaskByWindow == true) {
                        if (numOfThisTaskByInference != numOfThisTaskByWindow) {
                            compositionsToBeRemoved.add(thisInference);
                        }
                    } else {
                        if (numOfThisTaskByInference > numOfThisTaskByWindow) {
                            compositionsToBeRemoved.add(thisInference);
                        }
                    }

                }
                potentialInferences.removeAll(compositionsToBeRemoved);

            }

//            ArrayList<ArrayList<Task>> potentialInferences = new ArrayList<>();
//            for (ArrayList<Task> thisInference : thisBusyInterval.getComposition())
//            {
//                /* Check if the number of thisTask is consistent with thisInference.
//                *  If not, then thisInference may not be true answer.
//                *  ### We are removing those who are not the answer for sure and leave ambiguous ones!! ###
//                */
//
//                // Iterate by tasks
//                Boolean thisIsMismatchForSure = false;
//                for (Task thisTask : taskContainer.getAppTasksAsArray())
//                {
//                    /* Calculate the number of thisTask contained in this inference. */
//                    int numOfThisTaskByInference = Collections.frequency(thisInference, thisTask);
//                    int numOfThisTaskByWindow = calculateNumOfGivenTaskInBusyIntervalByArrivalTimeWindow(thisBusyInterval, thisTask);
//
//                    if (numOfThisTaskByWindow == -1) {
//                        // Skip this one for now since it's still possible to be the answer.
//                        continue;
//                    }
//
//                    // TODO: !!Bug!! if the arrival window is too big, then it's likely to infer incorrect number. ###FIXED!!!?
//                    if (numOfThisTaskByInference != numOfThisTaskByWindow) {
//                        thisIsMismatchForSure = true;
//                        break;
//                    }
//                }
//
//                if (thisIsMismatchForSure == false) {
//                    // This is not mismatch and still is likely to be an answer.
//                    potentialInferences.add(thisInference);
//                }
//
//            }

            if ((potentialInferences.size()>0) && (potentialInferences.size()!=orgNumOfInferences)) {
                // Note that we have skipped the busy intervals without ambiguity in the beginning of the for loop,
                // thus anything reaches here means something has been removed.
                isSomethingChanged = true;
                thisBusyInterval.getComposition().clear();
                thisBusyInterval.getComposition().addAll(potentialInferences);

                /* Update Nk values */
                thisBusyInterval.updateNkValuesFromCompositions(taskContainer);
            } else {
                //ProgMsg.debugPutline("No matched inference is found for bi " + String.valueOf((double)thisBusyInterval.getBeginTimeStampNs()*ProgConfig.TIMESTAMP_UNIT_TO_MS_MULTIPLIER) + "ms: %d", thisBusyInterval.getComposition().size());
            }

            if (thisBusyInterval.getComposition().size() == 0) {
                // This will never happen.
                //ProgMsg.debugErrPutline("One of busy interval's inferences become 0! It should never happen!");
            }

        }

        if (isSomethingChanged == true) {
            return true;
        } else {
            return false;
        }
    }

    private Boolean removeAmbiguousArrivalWindowsByInferredCompositions() {
        Boolean isSomethingImproved = false;

        for (Task thisTask : taskContainer.getAppTasksAsArray()) {
            if (taskArrivalTimeWindows.get(thisTask).size() >= 2) {

                // Go through every busy interval that contains thisTask.
                for (BusyInterval thisBI : processingBusyIntervalContainer.findBusyIntervalsByTask(thisTask)) {
                    // Does this busy interval have certain Nk number?
                    if (thisBI.getNkValuesOfTask(thisTask).size() == 1) {
                        // Ok, it is possible to identify the correct arrival window from this BI.
                        int thisNk = thisBI.getNkValuesOfTask(thisTask).get(0);

                        // Check every arrival window to see which one matches the certain Nk number.
                        ArrayList<Interval> arrivalWindowsToBeRemoved = new ArrayList<>();
                        for (Interval thisWindow : taskArrivalTimeWindows.get(thisTask)) {
                            int thisAwToNk = calculateNumOfGivenTaskInBusyIntervalByGivenArrivalWindow(thisBI, thisTask, thisWindow);
                            if (thisAwToNk != thisNk) {
                                arrivalWindowsToBeRemoved.add(thisWindow);
                                isSomethingImproved = true;
                                //ProgMsg.debugPutline("(%s) An ambiguous arrival window has been invalidated.", thisTask.getTitle());
                            }
                        }
                        taskArrivalTimeWindows.get(thisTask).removeAll(arrivalWindowsToBeRemoved);

                        if (taskArrivalTimeWindows.get(thisTask).size() == 1) {
                            // Oh yeah! This task has been solved, so skip the rest of busy intervals.
                            break;
                        }

                        if (taskArrivalTimeWindows.get(thisTask).size() == 0) {
                            //ProgMsg.debugErrPutline("It should never happen!!");
                            break;
                        }
                    }
                }

            }
        }

        return isSomethingImproved;
    }

    public int calculateNumOfGivenTaskInBusyIntervalByArrivalTimeWindow(BusyInterval inBusyInterval, Task inTask)
    {
        if (taskArrivalTimeWindows.get(inTask) == null) {
            // Arrival time window for inTask is empty.
            return -1;
        }

        int firstArrivalTimeCount = 0;
        Boolean firstLoop = true;
        /* If there are multiple arrival windows, return the value if all windows leads to the same count.
         * Otherwise return -1. */
        for (Interval thisWindow : taskArrivalTimeWindows.get(inTask)) {

            if (firstLoop == true) {
                firstArrivalTimeCount = calculateNumOfGivenTaskInBusyIntervalByGivenArrivalWindow(inBusyInterval, inTask, thisWindow);
                firstLoop = false;
            } else {
                int thisArrivalTimeCount = calculateNumOfGivenTaskInBusyIntervalByGivenArrivalWindow(inBusyInterval, inTask, thisWindow);

                if (firstArrivalTimeCount != thisArrivalTimeCount) {
                    // two windows lead to different result, thus return -1 as unsolvable.
                    return -1;
                } else {
                    // ok keep going.
                }
            }
        }

        return firstArrivalTimeCount;
    }

    public int calculateNumOfGivenTaskInBusyIntervalByGivenArrivalWindow(BusyInterval inBusyInterval, Task inTask, Interval inWindow)
    {

        Interval thisTaskArrivalWindow = new Interval(inWindow);
        Interval intervalBusyInterval = new Interval(inBusyInterval.getBeginTimeStampNs(), inBusyInterval.getEndTimeStampNs());

        Long shiftValue = findSmallestPeriodShiftValueWithIntersection(intervalBusyInterval, thisTaskArrivalWindow, inTask.getPeriod());
        if (shiftValue == null)
            return 0;

        thisTaskArrivalWindow.shift(shiftValue * inTask.getPeriod());

        // Check shifting direction
        int shiftingPositiveNegativeFactor = 1;
        if (shiftValue >= 0) {
            shiftingPositiveNegativeFactor = 1;
        } else {
            shiftingPositiveNegativeFactor = -1;
        }

        int countIntersectedTaskPeriod = 0;
        while (true) {
            if (thisTaskArrivalWindow.intersect(intervalBusyInterval) != null) {
                thisTaskArrivalWindow.shift(shiftingPositiveNegativeFactor * inTask.getPeriod());
                countIntersectedTaskPeriod++;
            } else {
                break;
            }
        }

        return countIntersectedTaskPeriod;
    }

    public EventContainer reconstructScheduleByArrivalWindows() {
        QuickFPSchedulerJobContainer schedulerJobContainer = new QuickFPSchedulerJobContainer();
        for ( BusyInterval  thisBusyInterval : orgBusyIntervalContainer.getBusyIntervals() ) {
            // Here we assume that the number of possible inferences is reduced to one, thus process the only one inference.
            ArrayList<Task> thisInference = thisBusyInterval.getFirstComposition();

            /* Start constructing arrival time sequence of all tasks in this busy interval. */
            for ( Task thisTask : taskContainer.getAppTasksAsArray() ) {

                //int numOfThisTask = Collections.frequency(thisInference, thisTask);
                Interval thisArrivalWindow = findClosestArrivalTimeOfTask( thisBusyInterval.getBeginTimeStampNs(), thisTask );

                while (thisArrivalWindow.getBegin() < thisBusyInterval.getEndTimeStampNs()) {
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
        // TODO: we didn't consider multiple arrival time window condition here. Just get first one.
        Interval taskWindow = new Interval( taskArrivalTimeWindows.get( inTask ).get(0) );

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



//    private Boolean computeInferenceDeviationSingleBusyIntervalByTask(BusyInterval bi, Task task) {
//        bi.getStartTimesInference().sortTaskReleaseEventsByTime();
//        bi.getStartTimesGroundTruth().sortTaskReleaseEventsByTime();
//
//        ArrayList<AppEvent> taskStartEventsGT = bi.getStartTimesGroundTruth().getEventsOfTask(task);
//        ArrayList<AppEvent> taskStartEventsIF = bi.getStartTimesInference().getEventsOfTask(task);
//
//        if (taskStartEventsGT.size() != taskStartEventsIF.size()) {
//            return false;
//        }
//
//        int countOfElements = taskStartEventsGT.size();
//        for (int i = 0; i < countOfElements; i++) {
//            AppEvent startGT = taskStartEventsGT.get(i);
//            AppEvent startIF = taskStartEventsIF.get(i);
//
//            startIF.deviation = startGT.getOrgBeginTimestampNs() - startIF.getOrgBeginTimestampNs();
//        }
//        return true;
//    }
//
//    private double recomputeInferencePrecisionRatioMultipleSingleBusyInterval(BusyInterval bi) {
//        double resultDeviationMultiple = 1;
//
//        bi.getStartTimesInference().sortTaskReleaseEventsByTime();
//        bi.getStartTimesGroundTruth().sortTaskReleaseEventsByTime();
//
//        for (Task thisTask : taskContainer.getAppTasksAsArray()) {
//            ArrayList<TaskInstantEvent> taskStartEventsGT = bi.getStartTimesGroundTruth().getEventsOfTask(thisTask);
//            ArrayList<TaskInstantEvent> taskStartEventsIF = bi.getStartTimesInference().getEventsOfTask(thisTask);
//            resultDeviationMultiple *= recomputeStartEventsPrecisionRatioMultiple(taskStartEventsGT, taskStartEventsIF);
//        }
//
//        return resultDeviationMultiple;
//    }

//    private double recomputeStartEventsPrecisionRatioMultiple(ArrayList<TaskInstantEvent> gt, ArrayList<TaskInstantEvent> inf) {
//
//        if (gt.size() != inf.size()) {
//            //ProgMsg.debugErrPutline("Ground truth and inference have inconsistent number of starting events.");
//            return -1;
//        }
//
//        double resultDeviationMultiple = 1;
//
//        int countOfElements = gt.size();
//        for (int i = 0; i < countOfElements; i++) {
//            TaskInstantEvent startGT = gt.get(i);
//            TaskInstantEvent startIF = inf.get(i);
//            int taskP = startIF.getTask().getPeriod();
//
//            startIF.deviation = startGT.getOrgBeginTimestampNs() - startIF.getOrgBeginTimestampNs();
//            startIF.precisionRatio = 1.0 - ((double)Math.abs(startIF.deviation) / (double)taskP);
//            resultDeviationMultiple *= startIF.precisionRatio;
//
//            if (startIF.deviation != 0) {
//                ProgMsg.debugPutline("%s:" + Double.toString(startIF.precisionRatio), startIF.getTask().getTitle());
//            }
//        }
//        return resultDeviationMultiple;
//    }

    // TODO: The range of the deviation between ground truth and inference has to be specified further.
    private Boolean verifySchedulingInferenceSingleBusyInterval(BusyInterval bi) {
        if ((bi.getStartTimesGroundTruth() == null) || (bi.getStartTimesInference()==null))
            return false;

        if (bi.getStartTimesGroundTruth().size() != bi.getStartTimesInference().size())
            return false;

        bi.getStartTimesInference().sortTaskReleaseEventsByTime();
        bi.getStartTimesGroundTruth().sortTaskReleaseEventsByTime();

        int countOfElements = bi.getStartTimesGroundTruth().size();
        for (int i=0; i<countOfElements; i++) {
            TaskInstantEvent startTimeGroundTruth = bi.getStartTimesGroundTruth().get(i);
            TaskInstantEvent startTimeInference = bi.getStartTimesInference().get(i);

            if (false == areEqualWithinError(startTimeGroundTruth.getOrgTimestamp(), startTimeInference.getOrgTimestamp(), 0))
                return false;
        }

        return true;
    }

    public Boolean verifySchedulingInference() {
        Boolean overallResult = true;
        for (BusyInterval bi : orgBusyIntervalContainer.getBusyIntervals()) {
            Boolean verificationResult = verifySchedulingInferenceSingleBusyInterval(bi);

            if (verificationResult == false) {
                //ProgMsg.debugPutline("Busy interval verification failed in busy interval at " + String.valueOf((double)bi.getBeginTimeStampNs() * (double)ProgConfig.TIMESTAMP_UNIT_TO_MS_MULTIPLIER) + " ms");
                overallResult = false;
            }
        }

        //ProgMsg.debugPutline("Overall verification done: " + overallResult.toString());
        return overallResult;
    }

//    public double computeInferencePrecisionRatioGeometricMean()  throws RuntimeException {
//        double overallPrecisionRatio = 1;
//        double precisionRatioMultiple = 1;
//        for (BusyInterval bi : processingBusyIntervalContainer.getBusyIntervals()) {
//            precisionRatioMultiple *= recomputeInferencePrecisionRatioMultipleSingleBusyInterval(bi);
//        }
//
//        overallPrecisionRatio = Math.pow(precisionRatioMultiple, 1/(double)getAllStartEventsCount());
//        //ProgMsg.debugPutline("Overall verification done: " + overallResult.toString());
//
//        return overallPrecisionRatio;
//    }

    public double computeInferencePrecisionRatioGeometricMeanByTaskStandardDeviation()  throws RuntimeException {
        //double sdRatioMultiple = 1.0;
        double sdRatioSum = 0.0;
        int numOfLegitimateTask = 0;
        for (Task thisTask : taskContainer.getAppTasksAsArray()) {
            double sumOfSquare = 0;
            ArrayList<TaskInstantEvent> taskStartEventsGT = orgBusyIntervalContainer.getStartTimeEventsGTByTask(thisTask);
            ArrayList<TaskInstantEvent> taskStartEventsIF = orgBusyIntervalContainer.getStartTimeEventsInfByTask(thisTask);

            //if (taskStartEventsGT.size() != taskStartEventsIF.size()) {
            //    ProgMsg.errPutline("%s Event counts are mismatched: %d : %d", thisTask.getTitle(), taskStartEventsGT.size(), taskStartEventsIF.size());
            //throw new RuntimeException(String.format("%s Event counts are mismatched: %d : %d", thisTask.getTitle(), taskStartEventsGT.size(), taskStartEventsIF.size()));
            //}

            if (taskStartEventsGT.size() > 0) {
                numOfLegitimateTask++;
            } else {
                //ProgMsg.debugPutline("%s has no GT event. (IF:%d)", thisTask.getTitle(), taskStartEventsIF.size());
                continue;
            }

            for (int i=0; i<taskStartEventsGT.size(); i++) {
                //taskStartEventsIF.get(i).deviation = taskStartEventsGT.get(i).getOrgBeginTimestampNs() - taskStartEventsIF.get(i).getOrgBeginTimestampNs();
                //sumOfSquare += Math.pow(taskStartEventsIF.get(i).deviation, 2);
                sumOfSquare += Math.pow(computeDeviationOfAnAppEvent(taskStartEventsGT.get(i), thisTask.getPeriod(), taskStartEventsIF), 2);
            }

            double standardDeviation = Math.pow(sumOfSquare / (double) taskStartEventsGT.size(), 0.5);
            double sdRatio = 1.0 - (standardDeviation/(double)(thisTask.getPeriod()));
            //ProgMsg.debugPutline("%s SD, ratio = %s, %s", thisTask.getTitle(), Double.toString(standardDeviation), Double.toString(sdRatio));
            //sdRatioMultiple = sdRatioMultiple * sdRatio;
            sdRatioSum += sdRatio;
        }

        double arithmeticMean = 0;
        if (numOfLegitimateTask == 0) {
            arithmeticMean = -0.01;
        } else {
            //double geometricMean = Math.pow(sdRatioMultiple, 1.0/(double)(taskContainer.getAppTasksAsArray().size()));
            arithmeticMean = sdRatioSum / (double) (numOfLegitimateTask);
            //ProgMsg.debugPutline("Overall ratio = %s", Double.toString(arithmeticMean));
        }

        //return geometricMean;
        return arithmeticMean;
    }

    private long computeDeviationOfAnAppEvent(TaskInstantEvent inSourceEvent, long inPeriod, ArrayList<TaskInstantEvent> inTargetEvents) {
        long sourceEventTime = inSourceEvent.getOrgTimestamp();

        /* Find the closest target event within +/- one period. */
        long closestEventTime = -1;
        Boolean hasMatched = false;
        for (TaskInstantEvent thisEvent: inTargetEvents) {
            long thisEventTime = thisEvent.getOrgTimestamp();
            if ( (sourceEventTime-(0.5*inPeriod) <= thisEventTime) && (thisEventTime <= sourceEventTime+(0.5*inPeriod)) ) {
                if (hasMatched == false) {
                    closestEventTime = thisEventTime;
                    hasMatched = true;
                } else {
                    if (Math.abs(thisEventTime - sourceEventTime) < Math.abs(closestEventTime - sourceEventTime)) {
                        closestEventTime = thisEventTime;
                    }
                }
            }
        }

        if (hasMatched == false) {
            // No inference within +/- one period, thus the deviation is one period by definition.
            return inPeriod;
        } else {
            return (closestEventTime - sourceEventTime);    // negative or positive doesn't matter.
        }
    }

    public double computeMeanPrecisionRatioFromEventContainer(BusyIntervalContainer inBiContainer) {
        orgBusyIntervalContainer = inBiContainer;

        orgBusyIntervalContainer.removeBusyIntervalsBeforeButExcludeTimeStamp((int)taskContainer.calHyperPeriod()*2);

        runDecompositionStep3();
        return computeInferencePrecisionRatioGeometricMeanByTaskStandardDeviation();
    }

}