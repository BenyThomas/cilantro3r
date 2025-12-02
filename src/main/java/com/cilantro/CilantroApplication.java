package com.cilantro;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import jakarta.annotation.PostConstruct;


@SpringBootApplication
@ComponentScan({"com.*"})
public class CilantroApplication implements CommandLineRunner {

    @PostConstruct
    public void init() {}

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CilantroApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {}

}
