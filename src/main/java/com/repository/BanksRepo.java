/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.IBANK.Banks;
import com.config.SYSENV;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class BanksRepo {

    @Value("${spring.profiles.active}")
    private String envPath;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpRepo.class);
    @Autowired
    SYSENV systemVariable;

    /*
     *Get local banks list
     */
    public List<Map<String, Object>> getLocalBanksList() {
        try {
            String mainSql = "SELECT * from banks where identifier='LOCAL' and (swift_code<>? OR swift_code_test<>?)";
            return this.jdbcTemplate.queryForList(mainSql, systemVariable.SENDER_BIC, systemVariable.SENDER_BIC);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON OBTAINING CONNECTION TO cilantrodb: {}", ex.getMessage());
            return null;
        }

    }

    public boolean isLocalBank(String bic) {
        System.out.println("********new bic: " + bic);
        try {
            String sql;

//            if (!envPath.equals("prod")) {
//                sql = "SELECT COUNT(*) FROM banks WHERE identifier = 'LOCAL' AND swift_code_test = ?";
//            } else {
//                sql = "SELECT COUNT(*) FROM banks WHERE identifier = 'LOCAL' AND swift_code = ?";
//            }

            sql = "SELECT COUNT(*) FROM banks WHERE identifier = 'LOCAL' AND swift_code = ?";

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, bic);

            System.out.println("==========count data" +count);
            return count != null && count > 0;

        } catch (Exception e) {
            // Log the exception to know why it failed
            LOGGER.error("Error in isLocalBank========: {}", e.getMessage());
            return false;
        }
    }

}
