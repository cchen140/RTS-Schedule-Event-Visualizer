package me.cychen.rts.gui.trace;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.gui.ProgConfig;
import me.cychen.rts.gui.TimeLine;
import me.cychen.rts.gui.event.IntervalEventPane;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jjs on 2/13/17.
 */
public class TracePane extends Pane {
    private double baselineY = (ProgConfig.TRACE_PANE_HEIGHT+ProgConfig.TRACE_PANE_CONTENT_HEIGHT)/2;
    private double contentReferenceY = (ProgConfig.TRACE_PANE_HEIGHT-ProgConfig.TRACE_PANE_CONTENT_HEIGHT)/2;
    private double currentOffsetX = ProgConfig.TRACE_BEGIN_OFFSET_X;
    //private double currentEndTimestamp = 0;

    private TimeLine globalTimeLine;
    private BaseLinePane baseLine;// = new BaseLinePane(currentOffsetX, baselineY);

    private HashMap<Object, Object> eventShapes = new HashMap<>();
    private ArrayList events = new ArrayList();

    public TracePane(TimeLine inTimeLine) {
        super();

        globalTimeLine = inTimeLine;

        setPrefHeight(ProgConfig.TRACE_PANE_HEIGHT);
        setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        baseLine = new BaseLinePane(globalTimeLine, currentOffsetX, baselineY);
        getChildren().add(baseLine);
    }

    public void addSchedulerIntervalEvent(SchedulerIntervalEvent inEvent) {
        IntervalEventPane newIntervalEventPane = new IntervalEventPane(inEvent, contentReferenceY);
        //new Rectangle(inEvent.getScaledBeginTimestamp(), contentReferenceY, inEvent.getScaledDuration(), ProgConfig.TRACE_PANE_CONTENT_HEIGHT);
        //events.add(inEvent);
        eventShapes.put(inEvent, newIntervalEventPane);

        getChildren().add(newIntervalEventPane);
        currentOffsetX = inEvent.getScaledEndTimestamp();
        //currentEndTimestamp = currentEndTimestamp>inEvent.getOrgEndTimestamp()?currentEndTimestamp:inEvent.getOrgEndTimestamp();

        globalTimeLine.pushEndTimestamp(inEvent.getScaledEndTimestamp());
        //baseLine.setEndTimestamp(currentEndTimestamp);
        setWidth(globalTimeLine.getCurrentEndTimestamp());
        //return currentEndTimestamp;
        updateGraph();
    }

    public void updateGraph() {
        baseLine.updateGraph();
//        for (Object currentObj : events)
//        {
//            if (SchedulerIntervalEvent.class.isInstance(currentObj)) {
//                ((SchedulerIntervalEvent)currentObj)
//            }
//        }
    }
}
