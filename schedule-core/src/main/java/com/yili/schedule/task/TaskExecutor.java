package com.yili.schedule.task;

import com.alibaba.fastjson.JSON;
import com.yili.schedule.config.Statistics;
import com.yili.schedule.config.TaskInfo;
import com.yili.schedule.config.TaskJob;
import com.yili.schedule.core.ScheduleSpringFactroy;
import com.yili.schedule.cron.CronExpression;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * job运行结构
 * Created by lancey on 16/2/28.
 */
public class TaskExecutor implements Runnable,Closeable{

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

    private TaskInfo taskInfo;
    private TaskJob taskJob;
    private volatile int startTime = 20;
    private volatile int nextTime = 20+20;
    private volatile int defaultSpanTime=-1;
    private volatile boolean stopFlag=false;
    private volatile boolean running=false;
    private Date startDate;
    private Date nextDate;

    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledExecutorService prevScheduledExecutorService;
    private ScheduleSpringFactroy factory;
    private CronExpression expression;

    /**
     *
     * @param factory
     * @param taskInfo
     * @param taskJob
     */
    public TaskExecutor(ScheduleSpringFactroy factory, TaskInfo taskInfo, TaskJob taskJob) {
        this.factory = factory;
        this.taskInfo = taskInfo;
        this.taskJob = taskJob;
        updateCron();
    }

    public  void init(){
        Date now = new Date();
        Date time = expression.getNextValidTimeAfter(now);
        resetTime(time,now);
    }

    private synchronized void resetTime(Date date,Date now){
        startDate = date;
        nextDate = expression.getNextValidTimeAfter(startDate);
        startTime = (int)((startDate.getTime()-now.getTime())/1000);
        nextTime = (int)((nextDate.getTime()-startDate.getTime())/1000);
        if(defaultSpanTime!=-1 && defaultSpanTime==nextTime){
            LOGGER.debug("nextTime is same span,don't need reset schedule.");
            return;
        }
        LOGGER.info("XXXXX startTime:{},nextTime:{}",startTime,nextTime);
        defaultSpanTime = nextTime;
        if(scheduledExecutorService!=null) {
            if(running) {
                prevScheduledExecutorService = scheduledExecutorService;
                prevScheduledExecutorService.shutdown();
            }else{
                scheduledExecutorService.shutdownNow();//不在运行就立即结束
            }
        }
        if(!stopFlag) {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            LOGGER.info("startTime:{},nextTime:{}", startTime, nextTime);
            scheduledExecutorService.scheduleAtFixedRate(this, startTime, nextTime, TimeUnit.SECONDS);
        }
    }

    /**
     * 出现调整时重新初始化定时器
     */
    public void reinit(){
        if(nextDate!=null) {
            resetTime(nextDate, new Date());
        }else{
            init();
        }
    }



    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        Date nowDate = new Date(startDate.getTime());
        Date next=new Date(nextDate.getTime());

        LOGGER.debug("thread running");
        if(stopFlag){
            return;
        }
        running=true;
        reinit();
        List<?> list = taskJob.selectList(taskInfo.getConfig(), taskInfo.getMaxLimit());
        if(CollectionUtils.isNotEmpty(list)){
            if(taskInfo.getMaxLimit()>0 && list.size()>taskInfo.getMaxLimit()){
                list = list.subList(0,taskInfo.getMaxLimit());
            }
        }
        Statistics stat = new Statistics();
        stat.setStartTime(nowDate);
        stat.setNextTime(next);
        stat.setFetchDataCount(list.size());
        int successCount = 0;
        if (CollectionUtils.isNotEmpty(list)) {
            ExecutorService executorService = Executors.newFixedThreadPool(getThreads(list.size()));
            List<Future<Boolean>> waitCompleteList = new ArrayList<Future<Boolean>>();
            for (final Object obj : list) {
                Future<Boolean> result = executorService.submit(new Callable<Boolean>() {
                    /**
                     * Computes a result, or throws an exception if unable to do so.
                     *
                     * @return computed result
                     * @throws Exception if unable to compute a result
                     */
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            return TaskExecutor.this.taskJob.execute(obj);
                        }catch(Exception ex){
                            return false;
                        }
                    }
                });

                if (!result.isDone()) {
                    waitCompleteList.add(result);
                } else {
                    try {
                        if (result.get()) {
                            successCount += 1;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
            while(!waitCompleteList.isEmpty()){
                for(Iterator<Future<Boolean>> it=waitCompleteList.iterator();;){
                    Future<Boolean> f = it.next();
                    if(f.isDone()){
                        try {
                            if (f.get()) {
                                successCount += 1;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        it.remove();
                    }
                    if(!it.hasNext()){
                        break;
                    }
                }
            }
            executorService.shutdown();
        }
        stat.setSuccessCount(successCount);

        factory.insertLog(taskInfo.getBeanName(), JSON.toJSONString(stat));
        running=false;
        LOGGER.debug("thread finish");
    }

    private int getThreads(int size){
        if(taskInfo.getThreads()<=1){
            return 1;
        }
        if(size<taskInfo.getThreads()){
            return size;
        }
        return taskInfo.getThreads();
    }

    public void updateCron(){
        try {
            expression = new CronExpression(taskInfo.getCronExpression());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(){
        stopFlag=true;
    }
}
