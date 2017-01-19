package com.yili.schedule.listener;

import com.yili.schedule.core.ScheduleSpringFactroy;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 选择为主的时候调用开启或关闭
 * Created by yangbin on 17/1/18.
 */
public class LatchSelectListener implements LeaderLatchListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatchSelectListener.class);
    private ScheduleSpringFactroy scheduleSpringFactroy;
    private String beanName;

    @Override
    public void isLeader() {
        scheduleSpringFactroy.selectLeadership(beanName);
    }

    @Override
    public void notLeader() {
        LOGGER.info("{} is notLeader",beanName);
    }

    public LatchSelectListener(ScheduleSpringFactroy scheduleSpringFactroy, String beanName) {
        this.scheduleSpringFactroy = scheduleSpringFactroy;
        this.beanName = beanName;
    }
}
