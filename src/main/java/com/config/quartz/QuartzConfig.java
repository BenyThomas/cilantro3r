/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config.quartz;

import com.zaxxer.hikari.HikariDataSource;
import java.text.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 *
 * @author melleji.mollel
 */
@Configuration
@EnableAutoConfiguration
public class QuartzConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource dataSource1;

    @Value("${spring.quartz.autoStart}")
    private String quartzAutoStart;

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(Trigger... triggers) throws ParseException {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setConfigLocation(new ClassPathResource("quartz.properties"));
        schedulerFactory.setOverwriteExistingJobs(true);
        schedulerFactory.setAutoStartup(Boolean.parseBoolean(quartzAutoStart));
        schedulerFactory.setDataSource(dataSource1);
//        schedulerFactory.setTransactionManager(platformTransactionManager);
        schedulerFactory.setJobFactory(springBeanJobFactory());
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
        //schedulerFactory.setSchedulerListeners(new GlobalSchedulerListener());
        // schedulerFactory.setG qlobalTriggerListeners(new GlobalTriggerListener());
        schedulerFactory.setStartupDelay(30);
        if (ArrayUtils.isNotEmpty(triggers)) {
            schedulerFactory.setTriggers(triggers);
        }

        return schedulerFactory;
    }

}
