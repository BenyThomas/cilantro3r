package com.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Configuration
@PropertySource({ "classpath:application.properties" })
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {"com.repository"},
        entityManagerFactoryRef = "cilantroEntityManagerFactory",
        transactionManagerRef = "txManagerMaster")
public class DBConfig {

    @Autowired
    private Environment env;

    @Bean(name = "cilantroEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean userEntityManager() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(userDataSource());
        em.setPackagesToScan(new String[] { "com.models" });

        HibernateJpaVendorAdapter vendorAdapter
                = new HibernateJpaVendorAdapter ();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        properties.put("hibernate.dialect", env.getProperty("spring.jpa.properties.hibernate.dialect"));
        properties.put("show-sql", env.getProperty("spring.jpa.show-sql"));
        properties.put("hibernate.format_sql", env.getProperty("spring.jpa.properties.hibernate.format_sql"));
        properties.put("open-in-view", env.getProperty("spring.jpa.open-in-view"));
        properties.put("hibernate.show_sql", env.getProperty("spring.jpa.properties.hibernate.show_sql"));
        properties.put("hibernate.use_sql_comments", env.getProperty("spring.jpa.properties.hibernate.use_sql_comments"));
        properties.put("hibernate.generate_statistics", env.getProperty("spring.jpa.properties.hibernate.generate_statistics"));
        em.setJpaPropertyMap(properties);
        return em;
    }

    @Primary
    @Bean(name = {"amgwConnection", "dataSource"})
    public DataSource userDataSource() {

        HikariDataSource dataSource
                = new HikariDataSource();
        dataSource.setDriverClassName(
                env.getProperty("spring.datasource.driverClassName"));
        dataSource.setJdbcUrl(env.getProperty("spring.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));
        dataSource.setSchema(env.getProperty("spring.datasource.schema"));
        dataSource.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
        dataSource.setMaximumPoolSize(4000);
        dataSource.setMinimumIdle(100);
        dataSource.setIdleTimeout(TimeUnit.SECONDS.toMillis(60));
        dataSource.setMaxLifetime(TimeUnit.SECONDS.toMillis(120));
        dataSource.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(60));
        dataSource.setValidationTimeout(TimeUnit.MINUTES.toMillis(1));
        dataSource.setPoolName("HikariPool - cilantro");
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "1000");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return dataSource;
    }

    @Bean(name = {"txManagerMaster", "transactionManager"})
    public PlatformTransactionManager platformTransactionManager() {

        JpaTransactionManager transactionManager
                = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(
                userEntityManager().getObject());
        return transactionManager;
    }

    @Primary
    @Bean(name = "amgwdb")
    public JdbcTemplate jdbcTemplateOne() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(userDataSource());
        return jdbcTemplate;

    }

}
