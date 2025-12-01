/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config;

import com.helper.MaiString;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

/**
 * @author melleji.mollel
 */
@Component
public class AppConfiguration implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "databaseProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String DATASOURCE_USERNAME = environment.getProperty("spring.datasource.username");
        String DATASOURCE_PASSWORD = environment.getProperty("spring.datasource.password");
        String DATASOURCE_URL = environment.getProperty("spring.datasource.url");
        String DATASOURCE_CLASSNAME = environment.getProperty("spring.datasource.driverClassName");
        String APPLICATION_PROFILE = environment.getProperty("spring.profiles.active");
        System.out.println("DATASOURCE_USERNAME=" + DATASOURCE_USERNAME);
        System.out.println("DATASOURCE_PASSWORD=" + DATASOURCE_PASSWORD);
        System.out.println("DATASOURCE_URL=" + DATASOURCE_URL);
        System.out.println("DATASOURCE_CLASSNAME=" + DATASOURCE_CLASSNAME);
        System.out.println("APPLICATION_PROFILE=" + APPLICATION_PROFILE);
        String QUERY = "SELECT NAME, DEV_VALUE AS VALUE FROM system_configuration ORDER BY NAME ASC";
        int LOOP = 1;
        if (DATASOURCE_URL != null && DATASOURCE_CLASSNAME != null) {
            if (APPLICATION_PROFILE == null) {
                System.out.println("APPLICATION_PROFILE: Default");
                QUERY = "SELECT NAME, DEV_VALUE AS VALUE FROM system_configuration ORDER BY NAME ASC";
            } else if (APPLICATION_PROFILE.equalsIgnoreCase("uat") ||
                    APPLICATION_PROFILE.equalsIgnoreCase("dev") ||
                    APPLICATION_PROFILE.equalsIgnoreCase("melleji") ||
                    APPLICATION_PROFILE.equalsIgnoreCase("mike") ||
                    APPLICATION_PROFILE.equalsIgnoreCase("kajilo") ||
                    APPLICATION_PROFILE.equalsIgnoreCase("arthur")) {
                System.out.println("APPLICATION_PROFILE: Development/melleji/mike/uat/kajilo");
                QUERY = "SELECT NAME, DEV_VALUE AS VALUE FROM system_configuration ORDER BY NAME ASC";
            } else if (APPLICATION_PROFILE.equalsIgnoreCase("prod") ) {
                System.out.println("APPLICATION_PROFILE: Production");
                QUERY = "SELECT NAME, PROD_VALUE AS VALUE FROM system_configuration ORDER BY NAME ASC";
            } else if (APPLICATION_PROFILE.equalsIgnoreCase("dr")) {
                System.out.println("APPLICATION_PROFILE: DR Site");
                QUERY = "SELECT NAME, DR_VALUE AS VALUE FROM system_configuration ORDER BY NAME ASC";
            }
//            System.out.println("DATABASE USERNAME:" + DATASOURCE_USERNAME + "\n DATABASE PASS:" + DATASOURCE_PASSWORD + "\n DATASOURCE_URL:" + DATASOURCE_URL + "\nDATASOURCE_CLASSNAME:" + DATASOURCE_CLASSNAME + "\nAPPLICATION_PROFILE:" + APPLICATION_PROFILE);
            //  LOGGER.info("====# Getting configuration from the database #====");
            Map<String, Object> propertySource = new HashMap<>();
            try {
                // Build manually datasource to ServiceConfig

                Class.forName(DATASOURCE_CLASSNAME);
                try ( // Fetch all properties
                    Connection conn = DriverManager.getConnection(DATASOURCE_URL, DATASOURCE_USERNAME, DATASOURCE_PASSWORD);
                    PreparedStatement preparedStatement = conn.prepareStatement(QUERY)) {
                    ResultSet rs = preparedStatement.executeQuery();

                    // Populate all properties into the property source
                    while (rs.next()) {
                        String propName = rs.getString("NAME");
                        propertySource.put(propName, rs.getString("VALUE"));
                        try {
                            System.out.println("[" + LOOP + "]: " + propName + " = " + MaiString.maskString(rs.getString("VALUE"), 2, 15, '_'));
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                        LOOP++;
                    }
                }
                // Create a custom property source with the highest precedence and add it to Spring Environment
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, propertySource));
                System.out.println("###### FINISHED GETTING APPLICATION SETTINGS ##########");
            } catch (ClassNotFoundException | SQLException e) {
                throw new RuntimeException("Error fetching configuration properties from db: [" + e.getMessage() + "]");
            }

        }
    }

}
