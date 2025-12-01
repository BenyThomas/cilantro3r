/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.security;

import com.config.SYSENV;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author MELLEJI
 */
@Configuration
@EnableWebSecurity
public class webSecurity implements WebMvcConfigurer {

    @Autowired
    SYSENV systemVariables;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        WebMvcConfigurer.super.addArgumentResolvers(resolvers); //To change body of generated methods, choose Tools | Templates.
    }
    private final long MAX_AGE_SECS = 3600;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*").exposedHeaders("Freeflow")
                //.allowedOrigins("*").allowedHeaders("*").exposedHeaders("Freeflow")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .maxAge(MAX_AGE_SECS);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if ("prod".equalsIgnoreCase(systemVariables.ACTIVE_PROFILE)) {
            registry.addResourceHandler("/resources/**")
                    .addResourceLocations("classpath:/static/resources/")
                    .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).mustRevalidate().cachePrivate());
        } else {
            registry.addResourceHandler("/CILANTRO/resources/**")
                    .addResourceLocations("classpath:/static/resources/");
                    
        }
    }
   @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
