/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 * @author MELLEJI
 */
@Configuration
public class DB extends HikariConfig {

    @Autowired
    private Environment env;

    //CILANTRO DB CONFIG TRANSFERED TO DBConfig

//    @Primary
//    @Bean(name = "amgwConnection")
//    public HikariDataSource amgwDBPool() throws SQLException {
//        HikariDataSource dataSourceAmgw = new HikariDataSource();
//        dataSourceAmgw.setDriverClassName(env.getProperty("spring.datasource.driverClassName"));
//        dataSourceAmgw.setJdbcUrl(env.getProperty("spring.datasource.url"));
//        dataSourceAmgw.setUsername(env.getProperty("spring.datasource.username"));
//        dataSourceAmgw.setPassword(env.getProperty("spring.datasource.password"));
//        dataSourceAmgw.setMaxLifetime(60000L);
//        dataSourceAmgw.setConnectionTimeout(30000L);
//        dataSourceAmgw.setMaximumPoolSize(3500);
//        dataSourceAmgw.setMinimumIdle(100);
//        dataSourceAmgw.setIdleTimeout(10000L);
//        dataSourceAmgw.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
//        dataSourceAmgw.setPoolName("cilantro - localDatabase");
//        dataSourceAmgw.setLogWriter(new PrintWriter(System.out));
//        return dataSourceAmgw;
//    }
//

    //    @Bean(name = "txManagerMaster")
//    public PlatformTransactionManager txManager() throws SQLException {
//        return new DataSourceTransactionManager(amgwDBPool()); // (2)
//    }

    //    @Primary
//    @Bean(name = "amgwdb")
//    public JdbcTemplate amgwdb(@Qualifier("amgwConnection") HikariDataSource ds) {
//        return new JdbcTemplate(ds);
//    }

    @Bean(name = "cbsConnection")
    public HikariDataSource dbCbsConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.tpblive.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.tpblive.url"));
        dataSource.setUsername(env.getProperty("spring.tpblive.username"));
        dataSource.setPassword(env.getProperty("spring.tpblive.password"));
        dataSource.setSchema(env.getProperty("spring.tpblive.schema"));
        dataSource.setMaximumPoolSize(150);
        dataSource.setMinimumIdle(10);
        dataSource.setIdleTimeout(30000L);
        dataSource.setPoolName("Hikari - CoreBanking");
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "partnersDB")
    public HikariDataSource partnersDB() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.partners.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.partners.url"));
        dataSource.setUsername(env.getProperty("spring.partners.username"));
        dataSource.setPassword(env.getProperty("spring.partners.password"));
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(10);
        dataSource.setIdleTimeout(30000L);
        dataSource.setPoolName("Hikari - PARTNERS");
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "tpbonlineDB")
    public HikariDataSource tpbonlineDB() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.tpbonline.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.tpbonline.url"));
        dataSource.setUsername(env.getProperty("spring.tpbonline.username"));
        dataSource.setPassword(env.getProperty("spring.tpbonline.password"));
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(10);
        dataSource.setIdleTimeout(30000L);
        dataSource.setPoolName("Hikari - TCB ONLINE DB");
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "gwkyc")
    public HikariDataSource gwkyc() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.kyc.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.kyc.url"));
        dataSource.setUsername(env.getProperty("spring.kyc.username"));
        dataSource.setPassword(env.getProperty("spring.kyc.password"));
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(10);
        dataSource.setIdleTimeout(70000L);
        dataSource.setPoolName("Hikari - KYC DATABASE");
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "gwConnection")
    public HikariDataSource gwCbsConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.gw.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.gw.url"));
        dataSource.setUsername(env.getProperty("spring.gw.username"));
        dataSource.setPassword(env.getProperty("spring.gw.password"));
        dataSource.setMaximumPoolSize(500);
        dataSource.setMinimumIdle(20);
        dataSource.setIdleTimeout(30000L);
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setPoolName("Hikari - gateway");
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "gwAirtelVikobaConnection")
    public HikariDataSource gwAirtelVikobaConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.airtel-vikoba.datasource.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.airtel-vikoba.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.airtel-vikoba.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.airtel-vikoba.datasource.password"));
//        dataSource.setSchema(env.getProperty("spring.airtel-vikoba.schema"));
        dataSource.setMaximumPoolSize(500);
        dataSource.setMinimumIdle(20);
        dataSource.setIdleTimeout(30000L);
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setPoolName("Hikari - gateway airtel vikoba");
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "brinjalConnection")
    public HikariDataSource brinjalConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.brinjal.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.brinjal.url"));
        dataSource.setUsername(env.getProperty("spring.brinjal.username"));
        dataSource.setPassword(env.getProperty("spring.brinjal.password"));
        dataSource.setMaximumPoolSize(500);
        dataSource.setMinimumIdle(20);
        dataSource.setIdleTimeout(30000L);
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setPoolName("Hikari - gateway");
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Bean(name = "channelManagerDataConnection")
    public HikariDataSource channelManagerDataConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.cm.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.cm.url"));
        dataSource.setUsername(env.getProperty("spring.cm.username"));
        dataSource.setPassword(env.getProperty("spring.cm.password"));
        dataSource.setMaximumPoolSize(500);
        dataSource.setMinimumIdle(20);
        dataSource.setIdleTimeout(30000L);
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setPoolName("Hikari - Data for channel manager");
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }



    @Bean(name = "gwReconConnection")
    public HikariDataSource gwReconConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.gwrecon.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.gwrecon.url"));
        dataSource.setUsername(env.getProperty("spring.gwrecon.username"));
        dataSource.setPassword(env.getProperty("spring.gwrecon.password"));
        dataSource.setMaximumPoolSize(90);
        dataSource.setMinimumIdle(19);
        dataSource.setIdleTimeout(30000L);
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setPoolName("Hikari - gwRecon");
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Primary
    @Bean(name = "gwdb")
    public JdbcTemplate gwdb(@Qualifier("gwConnection") HikariDataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "mkobaConnection")
    public HikariDataSource mkobaCbsConnection() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.mkoba.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.mkoba.url"));
        dataSource.setUsername(env.getProperty("spring.mkoba.username"));
        dataSource.setPassword(env.getProperty("spring.mkoba.password"));
        dataSource.setMaximumPoolSize(200);
        dataSource.setMinimumIdle(20);
        dataSource.setIdleTimeout(30000L);
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(120));
        dataSource.setPoolName("Hikari - mkobaDB");
        dataSource.setLogWriter(new PrintWriter(System.out));
        return dataSource;
    }

    @Primary
    @Bean(name = "mkobadb")
    public JdbcTemplate mkobadb(@Qualifier("mkobaConnection") HikariDataSource ds) {
        return new JdbcTemplate(ds);
    }



    @Bean(name = "jdbcCbsLive")
    public JdbcTemplate cbsConnection(@Qualifier("cbsConnection") HikariDataSource ds2) {
        return new JdbcTemplate(ds2);
    }

    @Bean(name = "gwKycDbConnection")
    public JdbcTemplate gwkycDbConnection(@Qualifier("gwkyc") HikariDataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "partners")
    public JdbcTemplate partnersConnection(@Qualifier("partnersDB") HikariDataSource ds2) {
        return new JdbcTemplate(ds2);
    }

    @Bean(name = "tpbonline")
    public JdbcTemplate tpbOnlineConnection(@Qualifier("tpbonlineDB") HikariDataSource ds2) {
        return new JdbcTemplate(ds2);
    }

    @Bean(name = "gwBrinjalDbConnection")
    public JdbcTemplate gwBrinjalDbConnection(@Qualifier("brinjalConnection") HikariDataSource ds2) {
        return new JdbcTemplate(ds2);
    }

    @Bean(name = "cmDataDbConnection")
    public JdbcTemplate cmDataDbConnection(@Qualifier("channelManagerDataConnection") HikariDataSource ds2) {
        return new JdbcTemplate(ds2);
    }
    @Bean(name = "gwAirtelVikobaDBConnection")
    public JdbcTemplate gwAirtelVikobaJdbcTemplate(@Qualifier("gwAirtelVikobaConnection") HikariDataSource ds2) {
        return new JdbcTemplate(ds2);
    }
}
