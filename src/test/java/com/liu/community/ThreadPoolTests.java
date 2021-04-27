package com.liu.community;

import com.liu.community.service.AlphaService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
public class ThreadPoolTests {
    /**
     * 1.jdk普通线程池 定时任务线程池
     *      ExecutorService：ScheduledExecutorService
     *          executorService.submit(Runnable)
     *          scheduledExecutorService.scheduleAtFixedRate(Runnable task, 10000初始化延迟, 1000周期, TimeUnit.MILLISECONDS)
     * 2.spring 普通线程池 定时任务线程池
     *      @EnableScheduling 开启spring定时任务
     *      ThreadPoolTaskExecutor：ThreadPoolTaskScheduler
     *          threadPoolTaskExecutor.submit(Runnable)
     *          threadPoolTaskScheduler.scheduleAtFixedRate(task,Date开始时间,1000周期)
     * 3.spring 普通线程池 定时任务线程池(简化)
     *      @EnableAsync 开启spring普通线程池异步处理方法
     *      @EnableScheduling 开启spring定时任务
     *
     *      @Async 该方法用spring普通线程池
     *      @Scheduled(initialDelay = 10000,fixedDelay = 1000)
     *      该方法用spring定时任务线程池，且在应用启动时自动调用
     */
    /*
    一 : jdk
     */
    //jdk普通线程池  分配5个线程体 可执行任务时不断复用
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    //jdk可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    private void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //1.jdk普通线程池
    @Test
    public void testExecutorService(){
        Runnable task = () -> {
            log.debug("Hello ExecutorService");
        };

        for (int i=0;i<10;i++) {
            executorService.submit(task);
        }
        sleep(10000);
    }

    //2.jdk定时任务线程池
    @Test
    public void testScheduledExecutorService(){
        Runnable task = () -> {
            log.debug("Hello ScheduledExecutorService");
        };

        //1.任务(线程任务) 2.第一次延迟多少秒执行(初始化延迟) 3.执行间隔周期(间隔周期) 4.时间单元
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);

        sleep(30000);
    }
    /*
    二 : spring : 普通线程池和定时任务线程池
        注意: 定时任务线程池需要标注@EnableScheduling开启
     */
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    //1.spring普通线程池
    @Test
    public void testThreadPoolTaskExecutor(){
        Runnable task = () -> {
            log.debug("Hello ThreadPoolTaskExecutor");
        };

        for (int i=0;i<10;i++) {
            taskExecutor.submit(task);
        }
        sleep(10000);
    }

    //2.spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler(){
        Runnable task = () -> {
            log.debug("Hello ThreadPoolTaskScheduler");
        };

        //1.任务(线程任务) 2.执行时间 3.执行间隔周期(间隔周期)
        Date startTime = new Date(System.currentTimeMillis() + 10000);
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000);

        sleep(30000);
    }
    /*
    spring普通线程池(简化)和定时任务线程池(简化)
        @EnableAsync    开启普通线程池异步处理方法
        @EnableScheduling  开启定时任务线程池
        @Async:     表明用线程池异步处理该方法
        @Scheduled: 表明用定时任务线程池处理该方法
                    而且在程序启动时就开启执行，不需要额外调用
     */
    @Autowired
    private AlphaService alphaService;
    //1.普通任务方法
//    @Async
//    public void execute1(){
//        log.debug("execute1");
//    }
//    //2.定时任务方法
//    @Scheduled(initialDelay = 10000,fixedDelay = 1000)
//    public void execute2(){
//        log.debug("execute2");
//    }
    //1.spring普通线程池(简化)
    @Test
    public void testThreadPoolTaskExecutorSimple(){
        for (int i=0;i<10;i++) {
            alphaService.execute1();
        }
        sleep(10000);
    }

    //2.spring定时任务线程池(简化)
    @Test
    public void testThreadPoolTaskSchedulerSimple(){
        sleep(30000);
    }





}
