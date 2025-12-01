/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 *
 * @author samichael
 */
@Configuration
public class MailConfig {

    @Autowired
    private Environment env;

    @Bean
    public JavaMailSender tpbMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(env.getProperty("spring.mail.host"));
        mailSender.setPort(Integer.valueOf(env.getProperty("spring.mail.port")));
        mailSender.setUsername(env.getProperty("spring.mail.username"));
        mailSender.setPassword(env.getProperty("spring.mail.password"));
        mailSender.setDefaultEncoding(env.getProperty("spring.mail.default-encoding"));
        Properties props = new Properties();
        props.put("mail.transport.protocol", env.getProperty("spring.mail.protocol"));
        props.put("mail.smtp.auth", env.getProperty("spring.mail.properties.mail.smtp.auth"));
        props.put("mail.smtp.starttls.enable", env.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        props.put("mail.smtp.ssl.trust", env.getProperty("spring.mail.properties.mail.smtp.ssl.trust"));
        props.put("mail.smtp.starttls.required", env.getProperty("spring.mail.properties.mail.smtp.starttls.required"));
        props.put("mail.smtp.connectiontimeout", env.getProperty("spring.mail.properties.mail.smtp.connectiontimeout"));
        props.put("mail.smtp.timeout", env.getProperty("spring.mail.properties.mail.smtp.timeout"));
        props.put("mail.smtp.writetimeout", env.getProperty("spring.mail.properties.mail.smtp.writetimeout"));
        props.put("mail.debug", env.getProperty("spring.mail.properties.mail.debug"));
        props.put("mail.properties.mail.smtp.ssl.enable", env.getProperty("spring.mail.properties.mail.smtp.ssl.enable"));
        mailSender.setJavaMailProperties(props);
        return mailSender;
    }

}
