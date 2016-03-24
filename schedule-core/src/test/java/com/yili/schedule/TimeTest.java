package com.yili.schedule;

import com.yili.schedule.cron.CronExpression;

import java.util.Date;

/**
 * Created by lancey on 16/2/27.
 */
public class TimeTest {

//    @Test
    public static void main(String[] args)throws Exception{
        CronExpression e = new CronExpression("0 0/2 * * * ?");
        Date date = e.getNextValidTimeAfter(new Date());
        Date date2 = e.getNextValidTimeAfter(date);

        System.out.println(date+"\n"+date2);

    }
}
