package com.yili.schedule.web;

import com.yili.schedule.config.TaskInfo;

import java.util.List;

/**
 *
 * Created by yangbin on 17/1/18.
 */
public class TaskDetailInfo {

    private TaskInfo taskInfo;
    private List<String> nodes;
    private String master;

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }
}
