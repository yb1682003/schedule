package com.yili.schedule.zk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yili.schedule.config.TaskInfo;
import com.yili.schedule.config.TaskStatus;
import com.yili.schedule.config.ZookeeperProfile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lancey on 16/3/18.
 */
public class ZkUtils {

    private static final String task_path="/task_info";

    /**
     * 查询Job的任务列表
     * @param profile
     * @return
     */
    public static List<TaskInfo> getJobList(final ZookeeperProfile profile){
        String path = getTaskInfoPath(profile);
        List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
        List<String> jsons = getJobListString(profile);
        try {
            List<String> list = profile.createClient().getChildren().forPath(path);
            if(CollectionUtils.isNotEmpty(jsons)){
                for(String child:jsons){
                    TaskInfo taskInfo = getTaskInfo(child);
                    taskInfos.add(taskInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return taskInfos;
    }


    public static Map<String,Object> getTaskNodes(final ZookeeperProfile zookeeperProfile, String beanName){
        String path = zookeeperProfile.makePath("task",beanName);
        List<String> list = null;
        Map<String,Object> map = new HashedMap();
        try {
            list = zookeeperProfile.createClient().getChildren().forPath(path);
            List<String> nodes = new ArrayList<>();

            if(CollectionUtils.isNotEmpty(list)){

                for(String child:list){
                    if(child.equals("lock")){
                        String leader = getLeader(zookeeperProfile,ZKPaths.makePath(path,"lock"));
                        if(leader!=null){
                            map.put("master",leader);
                        }
                    }else{
                        nodes.add(child);
                    }
                }
            }
            map.put("nodes",nodes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private static String getLeader(ZookeeperProfile zookeeperProfile,String path){
        List<String> list = null;
        try {
            list = zookeeperProfile.createClient().getChildren().forPath(path);
            if(CollectionUtils.isNotEmpty(list)){
                String str = new String(zookeeperProfile.createClient().getData().forPath(ZKPaths.makePath(path,list.get(0))));
                return str;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<String> getJobListString(final ZookeeperProfile profile){
        String path = getTaskInfoPath(profile);
        List<String> taskInfos = new ArrayList<String>();
        try {
            List<String> list = profile.createClient().getChildren().forPath(path);
            if(CollectionUtils.isNotEmpty(list)){
                for(String child:list){
                    byte[] data = profile.createClient().getData().forPath(ZKPaths.makePath(path,child));
                    taskInfos.add(new String(data));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return taskInfos;
    }

    /**
     * 保存job任务到zk
     * @param profile
     * @param taskInfo
     */
    public static void saveTaskInfo(ZookeeperProfile profile,TaskInfo taskInfo){
        String root = profile.makePath(task_path);
        String taskPath = ZKPaths.makePath(root,taskInfo.getBeanName());
        if(hasExistsPath(profile.createClient(),taskPath)){
            TaskInfo old = getTaskInfo(profile,taskInfo.getBeanName());
//            old.setConcurrency(taskInfo.isConcurrency());
//            old.setConfig(taskInfo.getConfig());
//            old.setCronExpression(taskInfo.getCronExpression());
//            old.setMaxLimit(taskInfo.getMaxLimit());
//            old.setThreads(taskInfo.getThreads());
            TaskStatus status = old.getStatus();
            BeanUtils.copyProperties(taskInfo,old);
            old.setStatus(status);
            String str = JSONObject.toJSONString(old);
            try {
                profile.createClient().setData().forPath(taskPath,str.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            String str = JSONObject.toJSONString(taskInfo);
            try {
                profile.createClient().create().creatingParentsIfNeeded().forPath(taskPath, str.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateStatus(ZookeeperProfile profile,String beanName,String status){
        TaskInfo taskInfo = ZkUtils.getTaskInfo(profile,beanName);
        if(taskInfo.getStatus().name().equals(status)) {
            throw new IllegalArgumentException("当前的状态是一置");
        }

        taskInfo.setStatus(TaskStatus.valueOf(status));
        String str = JSONObject.toJSONString(taskInfo);
        try {
            String taskPath = profile.makePath(task_path,taskInfo.getBeanName());
            profile.createClient().setData().forPath(taskPath,str.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TaskInfo getTaskInfo(byte[] data) {
        String content = new String(data);
        return getTaskInfo(content);
    }

    private static TaskInfo getTaskInfo(String content) {
        return JSONObject.toJavaObject((JSON) JSONObject.parse(content), TaskInfo.class);
    }

    public static TaskInfo getTaskInfo(final ZookeeperProfile profile,String beanName){
        String path =ZKPaths.makePath(getTaskInfoPath(profile),beanName);
        if (!hasExistsPath(profile.createClient(), path)) return null;

        try {
            byte[] bytes = profile.createClient().getData().forPath(path);
            return getTaskInfo(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasExistsPath(CuratorFramework client, String path) {
        Stat stat = null;
        try {
            stat = client.checkExists().forPath(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(stat!=null){
            return true;
        }
        return false;
    }

    public static String getTaskInfoPath(ZookeeperProfile profile) {
        return profile.makePath(task_path);
    }
}
