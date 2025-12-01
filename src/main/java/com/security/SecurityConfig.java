/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.security;

import java.util.Arrays;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.LazyCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 *
 * @author MELLEJI
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private userAuthentication authProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**")
                .addFilterBefore(customAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/login", "/assets/**", "/upload/**", "/", "/api/**",
                        "/batchProcessingAsyncUrl/**", "/actuator/**", "/error").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling().authenticationEntryPoint(delegatingEntryPoint())
                .and()
                .addFilterAfter(customAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .formLogin().disable();

        http.sessionManagement().sessionFixation().none().sessionAuthenticationErrorUrl("/login?session").invalidSessionUrl("/login?session").maximumSessions(1).expiredUrl("/login?session");
        http.headers().frameOptions().sameOrigin();
        http.cors().disable();
        http.csrf().ignoringAntMatchers("/api/**");

    }

    @Bean
    public UsernamePasswordAuthenticationFilter customAuthFilter() throws Exception {
        CustomUsernamePasswordAuthenticationFilter authenticationFilter = new CustomUsernamePasswordAuthenticationFilter();
        authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login", "POST"));
        authenticationFilter.setUsernameParameter("username");
        authenticationFilter.setPasswordParameter("password");
        authenticationFilter.setAuthenticationManager(authenticationManagerBean());
        authenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
        authenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
        authenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
        return authenticationFilter;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler("/dashboard");
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new LazyCsrfTokenRepository(new HttpSessionCsrfTokenRepository());
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler("/login?error=1");
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new CompositeSessionAuthenticationStrategy(Arrays.asList(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository())
        ));
    }

   

    @Bean
    public AuthenticationEntryPoint delegatingEntryPoint() {
        final LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> map = new LinkedHashMap();
        //for root return to login
        map.put(new AntPathRequestMatcher("/"), new LoginUrlAuthenticationEntryPoint("/login?"));
        //any other return to 403
        map.put(new AntPathRequestMatcher("/error"), new Http403ForbiddenEntryPoint());
        // map.put(new AntPathRequestMatcher("/setup/**"), new Http403ForbiddenEntryPoint());
        final DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(map);

        //default redirect to login
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login?error"));
        return entryPoint;

    }

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/**");
//                .addResourceLocations("file:///C:\\Users\\HP\\Documents\\PROJECTS\\cilantro\\upload\\");
//                .setCacheControl(CacheControl.maxAge(12, TimeUnit.SECONDS))
//                .resourceChain(false)
//                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));

    }

}
