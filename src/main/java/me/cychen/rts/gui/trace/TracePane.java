package me.cychen.rts.gui.trace;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import me.cychen.rts.event.Event;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.event.TaskInstantEvent;
import me.cychen.rts.gui.GuiConfig;
import me.cychen.rts.gui.TaskSetGui;
import me.cychen.rts.gui.TimeLine;
import me.cychen.rts.gui.event.EventPane;
import me.cychen.rts.gui.event.InstantEventPane;
import me.cychen.rts.gui.event.IntervalEventPane;

import java.util.HashMap;

/**
 * Created by jjs on 2/13/17.
 */
public class TracePane extends Pane {
    private double baselineY = (GuiConfig.TRACE_PANE_HEIGHT+ GuiConfig.TRACE_PANE_CONTENT_HEIGHT)/2;
    private double contentReferenceY = (GuiConfig.TRACE_PANE_HEIGHT- GuiConfig.TRACE_PANE_CONTENT_HEIGHT)/2;
    //private double currentOffsetX = GuiConfig.TRACE_BEGIN_OFFSET_X;
    //private double currentEndTimestamp = 0;

    private TimeLine globalTimeLine;
    private BaseLinePane baseLine;// = new BaseLinePane(currentOffsetX, baselineY);

    private TaskSetGui globalTaskSet;

    public SchedulerIntervalEvent lastEvent = null;

    private HashMap<Event, EventPane> eventShapes = new HashMap<>();
    //private ArrayList events = new ArrayList();

    public TracePane(TimeLine inTimeLine, TaskSetGui inTaskSet) {
        super();

        globalTimeLine = inTimeLine;
        globalTaskSet = inTaskSet;

        setPrefHeight(GuiConfig.TRACE_PANE_HEIGHT);
        setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        baseLine = new BaseLinePane(globalTimeLine, GuiConfig.TRACE_BEGIN_OFFSET_X, baselineY);
        getChildren().add(baseLine);
    }

    public void setGlobalTaskSet(TaskSetGui globalTaskSet) {
        this.globalTaskSet = globalTaskSet;
    }

    public void addSchedulerIntervalEvent(SchedulerIntervalEvent inEvent) {
        IntervalEventPane newIntervalEventPane = new IntervalEventPane(globalTaskSet, inEvent, GuiConfig.TRACE_BEGIN_OFFSET_X, contentReferenceY);

        eventShapes.put(inEvent, newIntervalEventPane);
        lastEvent = inEvent;

        getChildren().add(newIntervalEventPane);
        //currentOffsetX = inEvent.getScaledEndTimestamp();
        //currentEndTimestamp = currentEndTimestamp>inEvent.getOrgEndTimestamp()?currentEndTimestamp:inEvent.getOrgEndTimestamp();

        //globalTimeLine.pushEndTimestamp(inEvent.getScaledEndTimestamp());
        //baseLine.setEndTimestamp(currentEndTimestamp);
        //setWidth(globalTimeLine.getCurrentEndTimestamp());

        updateBaseLineGraph();
    }

    public void addTaskInstantEvent(TaskInstantEvent inEvent) {
        InstantEventPane newInstantEventPane = new InstantEventPane(globalTaskSet, inEvent, GuiConfig.TRACE_BEGIN_OFFSET_X, contentReferenceY);
        eventShapes.put(inEvent, newInstantEventPane);
        getChildren().add(newInstantEventPane);

        updateBaseLineGraph();
    }

    public void updateEventGraph(Event inEvent) {
        eventShapes.get(inEvent).updateGraph();
        //updateBaseLineGraph();
    }
//
//    public void updateLatestEventGraph() {
//        updateEventGraph(lastEvent);
//    }

    public void updateBaseLineGraph() {
        baseLine.updateGraph();
    }

//    public Boolean extendLatestActiveEvent(long inExtraLength) {
//        if (lastEvent.eventCompleted == false) {
//            lastEvent.extendEnd(inExtraLength);
//            //globalTimeLine.pushEndTimestamp(lastEvent.getScaledEndTimestamp());
//            return true;
//        }
//        return false; // to indicate that nothing is updated.
//    }
//
    public Boolean pushLastActiveEvent() {
        if (lastEvent == null) {
            return false;
        }
        if (lastEvent.eventCompleted == false) {
            lastEvent.setOrgEndTimestamp((long)globalTimeLine.getCurrentEndTimestamp());
            updateEventGraph(lastEvent);
            return true;
        }
        return false;
    }

    public Boolean closeLastActiveEvent(long inTimeStamp) {
        if (lastEvent == null) {
            return false;
        }
        if (lastEvent.eventCompleted == false) {
            lastEvent.setOrgEndTimestamp(inTimeStamp);
            lastEvent.eventCompleted = true;
            updateEventGraph(lastEvent);
            return true;
        }
        return false;
    }

    public void updateAllGraph() {
        updateBaseLineGraph();
        for (Event currentEvent : eventShapes.keySet()) {
            eventShapes.get(currentEvent).updateGraph();
        }
    }

    public void clear() {
        lastEvent = null;
        eventShapes.clear();
        getChildren().clear();
    }
}
