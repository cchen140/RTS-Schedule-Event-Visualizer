package me.cychen.rts.gui.event;

import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.event.TaskInstantEvent;
import me.cychen.rts.gui.GuiConfig;
import me.cychen.rts.gui.TaskSetGui;
import me.cychen.util.ProgMsg;

/**
 * Created by cy on 3/15/2017.
 */
public class InstantEventPane extends EventPane {
    //private Rectangle box;
    private Text text;
    protected TaskInstantEvent event;
    private TaskSetGui globalTaskSet = null;

    public InstantEventPane(TaskSetGui inTaskSet, TaskInstantEvent inEvent, double inOffsetX, double contentReferenceY) {
        super();
        globalTaskSet = inTaskSet;
        offsetX = inOffsetX + inEvent.getScaledTimestamp();
        event = inEvent;

        setLayoutX(offsetX);

        //box = new Rectangle(0, contentReferenceY, inEvent.getScaledDuration(), GuiConfig.TRACE_PANE_CONTENT_HEIGHT);
        //box.setFill(globalTaskSet.getColorByTask(event.getTask()));
        //getChildren().add(box);

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
        //box.setWidth(event.getScaledDuration());
        //box.setHeight(GuiConfig.TRACE_PANE_CONTENT_HEIGHT);
        //box.setFill(globalTaskSet.getColorByTask(event.getTask()));
        //ProgMsg.putLine("" + event.getScaledDuration());
    }
}
