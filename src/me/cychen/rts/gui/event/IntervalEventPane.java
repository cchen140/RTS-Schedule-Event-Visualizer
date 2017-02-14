package me.cychen.rts.gui.event;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.gui.ProgConfig;

/**
 * Created by jjs on 2/14/17.
 */
public class IntervalEventPane extends Pane {
    private Rectangle box;

    public IntervalEventPane(SchedulerIntervalEvent inEvent, double contentReferenceY) {
        super();
        box = new Rectangle(inEvent.getScaledBeginTimestamp(), contentReferenceY, inEvent.getScaledDuration(), ProgConfig.TRACE_PANE_CONTENT_HEIGHT);
        getChildren().add(box);
    }
}
