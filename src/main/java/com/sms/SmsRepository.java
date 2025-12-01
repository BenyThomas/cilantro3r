/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms;

import java.sql.ResultSet;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 * @author samichael
 */
@Repository
public class SmsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsRepository.class);

    @Autowired
    @Qualifier("tpbonline")
    JdbcTemplate jdbcTPBONLINETemplate;

    public SmsBody getSmsBody(int procId, String lang, String channel) {
        try {
            String sqlQuery = null;
            if (lang.equalsIgnoreCase(SmsConstants.LANG_EN_CD)) {
                if (channel.equals(SmsConstants.SMS_CHANNEL_CD)) {
                    sqlQuery = "SELECT ID, PROC_ID, PROC_DESC, MSG_EN AS MSG, PROC_OWNER, REC_ST, REF FROM TPB_SMS_MAPPING WHERE PROC_ID = ? ";
                } else if (channel.equals(SmsConstants.BPARTY_SMS_CHANNEL_CD)) {
                    sqlQuery = "SELECT ID, PROC_ID, PROC_DESC, BPARTY_MSG_EN AS MSG, PROC_OWNER, REC_ST, REF FROM TPB_SMS_MAPPING WHERE PROC_ID = ? ";
                } else {
                    sqlQuery = "SELECT ID, PROC_ID, PROC_DESC, EMAIL_EN AS MSG, PROC_OWNER, REC_ST, REF FROM TPB_SMS_MAPPING WHERE PROC_ID = ? ";
                }
            } else if (lang.equalsIgnoreCase(SmsConstants.LANG_SW_CD)) {
                if (channel.equals(SmsConstants.SMS_CHANNEL_CD)) {
                    sqlQuery = "SELECT ID, PROC_ID, PROC_DESC, MSG_SW AS MSG, PROC_OWNER, REC_ST, REF FROM TPB_SMS_MAPPING WHERE PROC_ID = ? ";
                } else if (channel.equals(SmsConstants.BPARTY_SMS_CHANNEL_CD)) {
                    sqlQuery = "SELECT ID, PROC_ID, PROC_DESC, BPARTY_MSG_SW AS MSG, PROC_OWNER, REC_ST, REF FROM TPB_SMS_MAPPING WHERE PROC_ID = ? ";
                } else {
                    sqlQuery = "SELECT ID, PROC_ID, PROC_DESC, EMAIL_SW AS MSG, PROC_OWNER, REC_ST, REF FROM TPB_SMS_MAPPING WHERE PROC_ID = ? ";
                }
            }
            LOGGER.debug("getSmsBody:query-> {}", sqlQuery);
            SmsBody sms
                    = jdbcTPBONLINETemplate.queryForObject(sqlQuery, new Object[]{procId},
                            (ResultSet rs, int rowNum) -> {
                                SmsBody row = new SmsBody();
                                row.setProcId(rs.getString("PROC_ID"));
                                row.setMsg(rs.getString("MSG"));
                                row.setLang(lang);
                                return row;
                            });
            return sms;
        } catch (DataAccessException e) {
            LOGGER.info("getSmsBody:DataAccessException", e);
            return null;
        }
    }
}
