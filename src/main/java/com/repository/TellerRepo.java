/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queue.QueueProducer;
import com.service.CorebankingService;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class TellerRepo {

    

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    CorebankingService corebanking;

  @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TellerRepo.class);
    @Autowired
    QueueProducer queProducer;
     /*
    GET PAYMENTS MODULES PERMISSIONS
     */
    public List<Map<String, Object>> getUserPaymentsModulesPerRole(String roleId) {
        try {
            return this.jdbcTemplate.queryForList("SELECT a.name,a.image_url,a.module_dashboard_url FROM payment_modules a INNER JOIN payment_module_permission b on b.module_id=a.id INNER join payment_permission_role c on c.payment_permission_id=b.permission_id where c.role_id=? GROUP by a.id", roleId);
        } catch (Exception ex) {
            LOGGER.info(null, ex);
        }
        return null;
    }
}
