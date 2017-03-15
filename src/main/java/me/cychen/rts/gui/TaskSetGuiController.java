package me.cychen.rts.gui;

import javafx.scene.paint.Color;
import me.cychen.rts.framework.Task;
import me.cychen.rts.framework.TaskSet;
import me.cychen.util.ProgMsg;
import me.cychen.util.gui.ColorList;

import java.util.HashMap;

/**
 * Created by jjs on 2/13/17.
 */
public class TaskSetGuiController extends TaskSet{
    HashMap<Task, Color> taskColorMap = new HashMap<>();
    ColorList colorList = new ColorList();

    @Override
    public Boolean addTask(Integer inTaskId, String inTitle, int inType, long inPeriod, long inDeadline, long inExecTime, int inPriority) {
        if (super.addTask(inTaskId, inTitle, inType, inPeriod, inDeadline, inExecTime, inPriority)) {
            taskColorMap.put(getTaskById(inTaskId), colorList.getNextColor());
            //ProgMsg.putLine("Task added.");
            return true;
        } else {
            return false;
        }
    }

    public Color getColorByTask(Task inTask) {
        return taskColorMap.get(inTask);
    }
}
