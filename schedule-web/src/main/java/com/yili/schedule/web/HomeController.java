package com.yili.schedule.web;

import com.yili.schedule.config.TaskInfo;
import com.yili.schedule.config.ZookeeperProfile;
import com.yili.schedule.cron.CronExpression;
import com.yili.schedule.zk.ZkUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 监控首页
 * Created by lancey on 16/3/8.
 */
@Controller
public class HomeController {

    @Autowired
    private ZookeeperProfile zookeeperProfile;

    @RequestMapping("/index.do")
    public String index(ModelMap map){
        List<TaskInfo> jobList = ZkUtils.getJobList(zookeeperProfile);
        List<TaskDetailInfo> detailInfoList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(jobList)){
            Date now = new Date();
            for(TaskInfo taskInfo:jobList){
                CronExpression cronExpression = null;
                try {
                    cronExpression = new CronExpression(taskInfo.getCronExpression());
                    taskInfo.setNextExecutorTime(DateFormatUtils.format(cronExpression.getNextValidTimeAfter(now),"yyyy-MM-dd HH:mm:ss"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                TaskDetailInfo taskDetailInfo = new TaskDetailInfo();
                taskDetailInfo.setTaskInfo(taskInfo);
                Map<String,Object> result = ZkUtils.getTaskNodes(zookeeperProfile,taskInfo.getBeanName());
                taskDetailInfo.setMaster((String)result.get("master"));
                taskDetailInfo.setNodes((List<String>)result.get("nodes"));
                detailInfoList.add(taskDetailInfo);
            }
        }
        map.put("jobs", detailInfoList);
        return "index";
    }

    @RequestMapping("/export.do")
    public String export(ModelMap map){
        map.put("jobs",ZkUtils.getJobListString(zookeeperProfile));
        return "task_export";
    }
}
