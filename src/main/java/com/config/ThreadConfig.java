/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 *
 * @author MELLEJI
 */
@Configuration
public class ThreadConfig {

    @Bean(name = "dbPoolExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        //or use TaskExecutor
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(80);
        executor.setMaxPoolSize(300);
        executor.setQueueCapacity(200);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(40);
        executor.setThreadNamePrefix("reconDw-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }



     @Bean(name = "threadPoolExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor2() {
        //or use TaskExecutor
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(20);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("threadPoolExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }





    @Bean(name = "pensionExecutor")
    public ThreadPoolTaskExecutor threadPensionTaskExecutor() {
        //or use TaskExecutor
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(200);
        executor.setMaxPoolSize(500);
        executor.setQueueCapacity(2000);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(40);
        executor.setThreadNamePrefix("pensionthread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
