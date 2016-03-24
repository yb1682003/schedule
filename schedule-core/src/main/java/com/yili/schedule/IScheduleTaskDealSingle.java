package com.yili.schedule;

import java.util.List;

/**
 * Created by lancey on 15/11/19.
 */
public interface IScheduleTaskDealSingle<T> {

    /**
     * 读取数据列表
     * @param taskDefineList 参数条件，配置中心提供数据
     * @param rows 数据行数，由配置中心提供数据
     * @return
     */
    List<T> selectList(List<TaskCondition> taskDefineList, int rows);

    /**
     * 执行任务
     * @param obj
     * @return
     */
    boolean execute(T obj);


    /**
     * 异常抛出时做异常处理
     * @param obj
     */
    void raiseException(T obj);
}
