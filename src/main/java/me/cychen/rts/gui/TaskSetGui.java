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
public class TaskSetGui extends TaskSet{
    HashMap<Task, Color> taskColorMap = new HashMap<>();
    ColorList colorList = new ColorList();

    public TaskSetGui() {
        super();
    }

    public TaskSetGui(TaskSet inTaskSet) {
        tasks.clear();
        tasks.putAll(inTaskSet.tasks);
    }

    public void importTaskSet(TaskSet inTaskSet) {
        tasks.clear();
        tasks.putAll(inTaskSet.tasks);
    }

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

    public void applyColors() {
        colorList.resetColorIndex(0);
        for (Task thisTask : tasks.values()) {
            if (thisTask.getTaskType() == Task.TASK_TYPE_IDLE) {
                taskColorMap.put(thisTask, GuiConfig.IDLE_TASK_COLOR);
            } else {
                taskColorMap.put(thisTask, colorList.getNextColor());
            }
        }
    }
}
