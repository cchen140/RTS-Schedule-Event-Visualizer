package me.cychen.rts.gui.event;

import javafx.scene.layout.Pane;
import me.cychen.rts.event.Event;

/**
 * Created by jjs on 2/20/17.
 */
public abstract class EventPane extends Pane {
    protected Event event;
    protected double offsetX = 0;

    public EventPane() {
        super();
    }

    abstract public void updateGraph();
}
