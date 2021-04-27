package com.liu.community;

import com.liu.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TransactionTests {
    @Autowired
    private AlphaService alphaService;

    @Autowired
    private Scheduler scheduler;

    @Test
    public void dele(){
        try {
            boolean result = scheduler.deleteJob(new JobKey("alphaJob","alphaJobGroup"));
            System.out.println(result);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void save1(){
        Object obj = alphaService.save1();
        System.out.println(obj);
    }

    @Test
    public void save2(){
        Object obj = alphaService.save2();
        System.out.println(obj);
    }
}
