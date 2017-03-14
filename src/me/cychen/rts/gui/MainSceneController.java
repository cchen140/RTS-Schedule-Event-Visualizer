package me.cychen.rts.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import me.cychen.rts.event.SchedulerIntervalEvent;
import me.cychen.rts.event.TaskInstantEvent;
import me.cychen.rts.framework.Task;
import me.cychen.rts.gui.trace.TracePane;
import me.cychen.rts.util.V11LogParser;
import me.cychen.util.ProgMsg;
import me.cychen.util.connect.SerialConnection;

import java.util.ArrayList;

public class MainSceneController {
    @FXML
    public ScrollPane scrollPaneProgMsg;
    public TextFlow textFlowProgMsg;
    public BorderPane leftControlPane;
    public Button leftControlPaneFoldingButton;
    public VBox vboxTraceContent;
    public VBox vboxTraceHeads;
    //public TextField textFieldSerialPortName;
    public ChoiceBox choiceBoxSerialPortList;
    public Button btnStartStop;
    public ScrollPane scrollPaneTraceContent;

    /* local variables */
    Boolean stopSerialConnectionThread = false;
    Boolean isSerialConnectionThreadStopped = true;
    SerialConnection serialConnection;
    TimeLine globalTimeLine = new TimeLine();

    TaskSetGuiController globalTaskSet = new TaskSetGuiController();
    me.cychen.rts.util.V11LogParser serialLogStringParser = new V11LogParser();

    TracePane serialInputTrace = new TracePane(globalTimeLine, globalTaskSet);

    Timeline mainTimeline;

    @FXML
    protected void initialize() {
        ProgMsg.setTargetDoc(textFlowProgMsg);
        scrollPaneProgMsg.setVvalue(1.0);   // Always scroll to the end.
        //test.getChildren().add(new Button("Click me!"));

        //leftControlPaneFoldingButton.setOnMouseClicked();

        serialLogStringParser.setTaskSet(globalTaskSet);

        /* Initialize the serial port list. */
        ArrayList<String> serialPortList = SerialConnection.getSerialPorts();
        if (serialPortList.size() > 0) {
            choiceBoxSerialPortList.setItems(FXCollections.observableArrayList(serialPortList));
        } else {
            /* If no ports are found, then disable the start/stop button. */
            choiceBoxSerialPortList.setItems(FXCollections.observableArrayList("No port found."));
            btnStartStop.setDisable(true);
            choiceBoxSerialPortList.setDisable(true);
        }
        // Select the first one by default.
        choiceBoxSerialPortList.getSelectionModel().selectFirst();

        vboxTraceContent.getChildren().add(serialInputTrace);
        //serialInputTrace.addSchedulerIntervalEvent(new SchedulerIntervalEvent(5, new Task(1, "test", 0, 10, 10, 10, 1), "hello"));

        /* Periodically update traces. */
        //Platform.runLater(new TraceUpdaterThread());
        mainTimeline = new Timeline(new KeyFrame(
                Duration.millis(100),
                ae -> globalTimeLineUpdater()));
        mainTimeline.setCycleCount(Animation.INDEFINITE);
        //timeline.play();
        //timeline.stop();

        Timeline timeline2 = new Timeline(new KeyFrame(
                Duration.millis(10),
                ae -> createNewEvent()));
        timeline2.setCycleCount(Animation.INDEFINITE);
        //timeline2.play();

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
        if (btnStartStop.getText().equalsIgnoreCase("stop")) {
            stopSerialPortReceiver();

            // The button should be changed by the thread when it is really terminated.
            //btnStartStop.setText("Stop");
        } else {
            if (startSerialPortReceiver()) {
                btnStartStop.setText("Stop");
            }
        }
    }

    private Boolean startSerialPortReceiver() {
        /* If the serial object is not empty (activated before), then try to close it. */
        if (serialConnection!=null) {
            serialConnection.close();
        }

        try {
            String selectedSerialPortName = choiceBoxSerialPortList.getSelectionModel().getSelectedItem().toString();
            ProgMsg.putLine(selectedSerialPortName);
            serialConnection = new SerialConnection(selectedSerialPortName);
            globalTimeLine.resetBeginTime();
            mainTimeline.play();
            //new Thread(new SerialConnectionThread()).start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void stopSerialPortReceiver() {
        //stopSerialConnectionThread = true;
        mainTimeline.stop();
        serialConnection.close();
        //isSerialConnectionThreadStopped = true;
        btnStartStop.setText("Start");
    }

    private class SerialConnectionThread implements Runnable {

        @Override
        public void run() {
            String receivedString;
            isSerialConnectionThreadStopped = false;
            while (stopSerialConnectionThread == false) {
                //receivedString = serialConnection.read();
                //if (receivedString != null) {
                //    ProgMsg.debugPutline(receivedString);
                //}
                ;
            }
            serialConnection.close();
            isSerialConnectionThreadStopped = true;
            btnStartStop.setText("Stop");
        }
    }

    private Boolean isFirstSerialStringProcessed = false;
    private long beginLogTimestamp = 0;
    public void globalTimeLineUpdater() {
        globalTimeLine.updateEndTimestamp();

        if (serialConnection != null) {
            while (serialConnection.isStringReady()) {
                TaskInstantEvent thisEvent = serialLogStringParser.parseSerialLogString(serialConnection.readNextString());
                if (thisEvent == null) {
                    continue;
                }

                if (isFirstSerialStringProcessed == false) {
                    // There is no previous event that needs to be handled.
                    beginLogTimestamp = (long) (thisEvent.getOrgTimestamp() - globalTimeLine.getCurrentEndTimestamp());
                    //ProgMsg.putLine(thisEvent.getOrgTimestamp() + " - " + globalTimeLine.getBeginTimestamp() + " = " + beginLogTimestamp);
                    serialInputTrace.addSchedulerIntervalEvent(new SchedulerIntervalEvent(adjustLogTimestamp(thisEvent.getOrgTimestamp()), thisEvent.getTask(), thisEvent.getNote()));
                    isFirstSerialStringProcessed = true;
                } else {
                    serialInputTrace.closeLastActiveEvent(adjustLogTimestamp(thisEvent.getOrgTimestamp()));
                    ProgMsg.putLine(globalTimeLine.getCurrentEndTimestamp() + ": " + thisEvent.getOrgTimestamp() + ": " + serialInputTrace.lastEvent.toString());
                    serialInputTrace.addSchedulerIntervalEvent(new SchedulerIntervalEvent(adjustLogTimestamp(thisEvent.getOrgTimestamp()), thisEvent.getTask(), thisEvent.getNote()));
                }
            }
        }

        serialInputTrace.pushLastActiveEvent();
        serialInputTrace.updateBaseLineGraph();
        scrollPaneTraceContent.setHvalue(1.0);
    }

    public long adjustLogTimestamp(long inLogTimestamp) {
        return (inLogTimestamp - beginLogTimestamp);
    }

    // This function will be called periodically when real-time monitoring is activated.
    public void traceUpdater() {
        //serialInputTrace.extendLatestActiveEvent(1);
        //serialInputTrace.updateLatestEventGraph();
    }

    public void createNewEvent() {
        int choice = (int) ((Math.random()*3)%3);
        if (choice == 0) {
            //ProgMsg.putLine("Hello0");
            serialInputTrace.lastEvent.eventCompleted = true;
            serialInputTrace.addSchedulerIntervalEvent(new SchedulerIntervalEvent((long)globalTimeLine.getCurrentEndTimestamp(), new Task(1, "test", 0, 10, 10, 10, 1), "hello"));
        } else if (choice == 1) {
            //ProgMsg.putLine("Hello1");
            serialInputTrace.lastEvent.eventCompleted = true;
        } else {
            //ProgMsg.putLine("Hello2");
            ;
        }

    }
}
