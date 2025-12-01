package com.cilantro;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;


@SpringBootApplication
@ComponentScan({"com.*"})
public class CilantroApplication extends SpringBootServletInitializer implements CommandLineRunner {

    @PostConstruct
    public void init() {}

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CilantroApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {}

    //For WAR deployment.
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CilantroApplication.class);
    }

}
