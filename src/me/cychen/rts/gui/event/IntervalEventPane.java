package me.cychen.rts.gui.event;

import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.gui.GuiConfig;
import me.cychen.rts.gui.TaskSetGuiController;
import me.cychen.util.ProgMsg;

/**
 * Created by jjs on 2/14/17.
 */
public class IntervalEventPane extends EventPane {
    private Rectangle box;
    private Text text;
    protected SchedulerIntervalEvent event;
    private TaskSetGuiController globalTaskSet = null;

    public IntervalEventPane(TaskSetGuiController inTaskSet, SchedulerIntervalEvent inEvent, double inOffsetX, double contentReferenceY) {
        super();
        globalTaskSet = inTaskSet;
        offsetX = inOffsetX + inEvent.getScaledBeginTimestamp();
        event = inEvent;

        setLayoutX(offsetX);

        box = new Rectangle(0, contentReferenceY, inEvent.getScaledDuration(), GuiConfig.TRACE_PANE_CONTENT_HEIGHT);
        box.setFill(globalTaskSet.getColorByTask(event.getTask()));
        getChildren().add(box);

        text = new Text(0, contentReferenceY-15, event.getNote());
        text.setStyle("-fx-font: 10 arial;");
        text.setTextAlignment(TextAlignment.LEFT);
        text.setRotate(-45);
        text.setFill(globalTaskSet.getColorByTask(event.getTask()));
        getChildren().add(text);

    }

    @Override
    public void updateGraph() {
        setLayoutX(offsetX);
        //box.setX(event.getScaledBeginTimestamp()+offsetX);
        box.setWidth(event.getScaledDuration());
        box.setHeight(GuiConfig.TRACE_PANE_CONTENT_HEIGHT);
        box.setFill(globalTaskSet.getColorByTask(event.getTask()));
        ProgMsg.putLine("" + event.getScaledDuration());
    }
}
