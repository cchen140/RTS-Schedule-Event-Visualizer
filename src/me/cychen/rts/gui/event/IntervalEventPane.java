package me.cychen.rts.gui.event;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.gui.ProgConfig;
import me.cychen.rts.gui.TaskSetGuiController;
import me.cychen.util.ProgMsg;

/**
 * Created by jjs on 2/14/17.
 */
public class IntervalEventPane extends EventPane {
    private Rectangle box;
    protected SchedulerIntervalEvent event;
    private TaskSetGuiController globalTaskSet = null;

    public IntervalEventPane(TaskSetGuiController inTaskSet, SchedulerIntervalEvent inEvent, double inOffsetX, double contentReferenceY) {
        super();
        globalTaskSet = inTaskSet;
        offsetX = inOffsetX;
        event = inEvent;
        box = new Rectangle(inEvent.getScaledBeginTimestamp()+offsetX, contentReferenceY, inEvent.getScaledDuration(), ProgConfig.TRACE_PANE_CONTENT_HEIGHT);
        box.setFill(globalTaskSet.getColorByTask(event.getTask()));
        getChildren().add(box);

    }

    @Override
    public void updateGraph() {
        box.setX(event.getScaledBeginTimestamp()+offsetX);
        box.setWidth(event.getScaledDuration());
        box.setHeight(ProgConfig.TRACE_PANE_CONTENT_HEIGHT);
        box.setFill(globalTaskSet.getColorByTask(event.getTask()));
        ProgMsg.putLine("" + event.getScaledDuration());
    }
}
