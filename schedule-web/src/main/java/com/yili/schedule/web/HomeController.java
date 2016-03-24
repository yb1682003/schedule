package com.yili.schedule.web;

import com.yili.schedule.config.ZookeeperProfile;
import com.yili.schedule.zk.ZkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

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
        map.put("jobs", ZkUtils.getJobList(zookeeperProfile));
        return "index";
    }

    @RequestMapping("/export.do")
    public String export(ModelMap map){
        map.put("jobs",ZkUtils.getJobListString(zookeeperProfile));
        return "task_export";
    }
}
