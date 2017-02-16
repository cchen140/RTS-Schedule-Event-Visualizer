package me.cychen.rts.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.framework.Task;
import me.cychen.rts.gui.trace.TraceController;
import me.cychen.util.ProgMsg;
import me.cychen.util.connect.SerialConnection;

public class MainSceneController {
    @FXML
    public TextFlow textFlowProgMsg;
    public BorderPane leftControlPane;
    public Button leftControlPaneFoldingButton;
    public VBox vboxTraceContent;
    public VBox vboxTraceHeads;
    public TextField textFieldSerialPortName;
    public Button btnStartStop;

    /* local variables */
    Boolean stopSerialConnectionThread = false;
    Boolean isSerialConnectionThreadStopped = true;
    SerialConnection serialConnection;
    TimeLine globalTimeLine = new TimeLine();
    TraceController traceController = new TraceController(globalTimeLine);

    @FXML
    protected void initialize() {
        ProgMsg.setTargetDoc(textFlowProgMsg);
        //test.getChildren().add(new Button("Click me!"));

        //leftControlPaneFoldingButton.setOnMouseClicked();

        ProgMsg.putLine("Hello");

        vboxTraceContent.getChildren().add(traceController.getTracePane());
        //vboxTraceContent.getChildren().add(new TracePane());

        /* Periodically update traces. */
        //Platform.runLater(new TraceUpdaterThread());
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(1000),
                ae -> traceUpdater()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        //timeline.stop();

        //AnimationTimer
    }

    @FXML
    private void toggleFoldingLeftControlPane() {
        //ProgMsg.putLine(String.valueOf(leftControlPane.getPrefWidth()));
        if (leftControlPane.getPrefWidth() == 0) {
            leftControlPaneFoldingButton.setText("<");
            leftControlPane.setPrefWidth(200);
            leftControlPane.setVisible(true);
        } else {
            leftControlPaneFoldingButton.setText(">");
            leftControlPane.setPrefWidth(0);
            leftControlPane.setVisible(false);
        }
    }

    @FXML
    private void startStopSerialPortReceiver() {
        if (btnStartStop.getText().toLowerCase() == "start") {
            stopSerialPortReceiver();

            // The button should be changed by the thread when it is really terminated.
            //btnStartStop.setText("Stop");
        } else {
            if (startSerialPortReceiver()) {
                btnStartStop.setText("Start");
            }
        }
    }

    private Boolean startSerialPortReceiver() {
        /* If the serial object is not empty (activated before), then try to close it. */
        if (serialConnection!=null) {
            serialConnection.close();
        }

        try {
            serialConnection = new SerialConnection(textFieldSerialPortName.getText());
            new Thread(new SerialConnectionThread()).start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void stopSerialPortReceiver() {
        stopSerialConnectionThread = true;
        // Note that the
    }

    private class SerialConnectionThread implements Runnable {

        @Override
        public void run() {
            isSerialConnectionThreadStopped = false;
            while (stopSerialConnectionThread == false) {
                ProgMsg.putLine(serialConnection.read());
            }
            serialConnection.close();
            isSerialConnectionThreadStopped = true;
            btnStartStop.setText("Stop");
        }
    }

    int temp = 20;
    // This function will be called periodically when real-time monitoring is activated.
    public void traceUpdater() {
        traceController.getTracePane().addSchedulerIntervalEvent(new SchedulerIntervalEvent(temp, temp+20, new Task(1, "test", 0, 10, 10, 10, 1), "hello"));
        temp += 50;
    }
}
