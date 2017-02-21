package me.cychen.rts.gui.trace;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import me.cychen.rts.gui.TimeLine;

/**
 * Created by jjs on 2/14/17.
 */
public class BaseLinePane extends Pane {
    Line baseLine = new Line(0, 0, 0, 0);

    private double offsetX = 0;
    private TimeLine globalTimeLine;
    //private double endTimestamp = 0;

    public BaseLinePane(TimeLine inTimeLine, double inLayoutX, double inLayoutY) {
        globalTimeLine = inTimeLine;
        setLayoutX(inLayoutX);
        setLayoutY(inLayoutY);

        baseLine.setEndX(globalTimeLine.getCurrentEndTimestamp());
        baseLine.setStroke(Color.BLUE);
        baseLine.setStrokeWidth(1);
        getChildren().add(baseLine);
    }

    public void updateGraph() {
        baseLine.setEndX(globalTimeLine.getCurrentEndTimestamp());
    }
}
