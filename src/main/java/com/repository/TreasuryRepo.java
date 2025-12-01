/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.treasury.SpecialRateDealNumber;
import com.config.SYSENV;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.queue.QueueProducer;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class TreasuryRepo {

    /*
    GET EFT dashboard 
     */
    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    QueueProducer queProducer;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EftRepo.class);

    public List<Map<String, Object>> getTreasuryModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    /*
     *GET SPECIAL RATES REQUESTS ON WORK-FLOW
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Africa/Nairobi")
    public String getRtgsSpecialRatesOnWorkFlowAjax(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM transfers a INNER JOIN transfer_special_rates b on a.reference=b.txnReference where status='SR' ";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(status,' ',senderBic) LIKE ? AND  status='SR'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(senderBIC) FROM transfers  " + searchQuery, new Object[]{searchValue}, Integer.class);
                mainSql = "SELECT * FROM transfers a INNER JOIN transfer_special_rates b on a.reference=b.txnReference   " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT * FROM transfers a INNER JOIN transfer_special_rates b on a.reference=b.txnReference WHERE status='SR' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    GET SWIFT TRANSACTIONS WITH REFERENCE
     */
    public List<Map<String, Object>> getSwiftMessage(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT swift_message FROM transfers where reference=?", reference);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING SWIFT MESSAGE: {}", ex.getMessage());
            return null;
        }
    }

    /*
    INSERT SPECIAL EXCHANGE RATE FROM TREASURY
     */
    public Integer saveApprovedSpecialRate(String approvedRate, String approvedBy, String reference, String remarks) {
        Integer res = -1;
        Integer res2 = -1;
        try {

            res = jdbcTemplate.update("UPDATE transfer_special_rates set approved_rate=?,approved_by=?,approved_dt=?,remarks=? where txnReference=?",
                    approvedRate, approvedBy, DateUtil.now(), remarks, reference);
            res = jdbcTemplate.update("UPDATE transfers set status='I' where reference=?",
                    reference);
            LOGGER.info("SPECIAL RATE: TXN REFERENCE:{},RATE PROVIDED:{}", reference, approvedRate);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public String getEchangeRateFrmCBS(String accountNo, String currency) {
        List<Map<String, Object>> result = null;
        String jsonString = null;
        String mainSql = "SELECT CRNCY_CD_ISO currency,VER.CRNCY_ID,CASE RATE_TY_ID WHEN 11 THEN 'buying_rate' WHEN 29 THEN  'selling_rate' ELSE 'unkown' END AS type,\n"
                + "  CASE \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=875 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=875),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=875),4)  \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=876 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=876),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=876),4)  \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=875),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=875 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=876),4) \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=876 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4) \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=875 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=876 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=876),4) \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=875),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  ELSE 1\n"
                + "  END AS debitRate,\n"
                + "    CASE \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=875 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=875),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=875),4)  \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=876 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=876),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=876),4)  \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4) \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=876 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4) \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=875 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=875),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=875 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=875),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=876 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=876),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4) \n"
                + "\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4)\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4)\n"
                + "  ELSE 1\n"
                + "  END AS creditRate,\n"
                + "  CASE \n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN 'SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN 'SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=875 THEN 'SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=876 THEN 'SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN 'CROSS-CURRENCY_BUYING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=841 THEN 'CROSS-CURRENCY_SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=875 THEN 'CROSS-CURRENCY_SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=876 THEN 'CROSS-CURRENCY_SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=841 THEN 'CROSS-CURRENCY_SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN 'CROSS-CURRENCY_SELLING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=875 THEN 'CROSS-CURRENCY_SELING(KES_TO_TZS_TO_EUR)'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=875 AND ver.CRNCY_ID=864 THEN 'CROSS-CURRENCY_SELING(KES_TO_TZS_TO_EUR)'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=876 AND ver.CRNCY_ID=864 THEN 'CROSS-CURRENCY_SELING'\n"
                + "  WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=876 THEN 'CROSS-CURRENCY_SELING'\n"
                + "  ELSE 'NO DEFINED'\n"
                + "  END AS fxType\n"
                + "  FROM V_EXCHANGE_RATE ver WHERE VER.RATE_TY_ID IN (11,29) AND CRNCY_CD_ISO =?";
        try {
            result = this.jdbcRUBIKONTemplate.queryForList(mainSql, currency);
            System.out.println("EXCHANGE RATE RESULTS: " + result);
            jsonString = this.jacksonMapper.writeValueAsString(result);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return jsonString;
    }

    /*
    INSERT SPECIAL EXCHANGE RATE 
     */
    public Integer createSpecialRateDealNo(SpecialRateDealNumber spRateForm, String reference, String initiatedBy, String dealNumber) {
        Integer res = -1;
        try {
            res = jdbcTemplate.update("INSERT INTO transfer_special_rates(txnReference, currency_conversion, rubikon_debit_rate, rubikon_credit_rate, approved_debit_rate, approved_credit_rate, fxtype, fxToken, senderAcct, senderName, partnerCode, currency, amount, fxstatus, status, valid_to, source, initiated_by, phone,email)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    reference, spRateForm.getCurrencyConversion(), spRateForm.getRequestedDebitRate(), spRateForm.getRubikonCreditRate(), spRateForm.getRequestedDebitRate(), spRateForm.getRequestedCreditRate(), spRateForm.getFxType(), dealNumber, spRateForm.getSenderAccount(), spRateForm.getAccountName(), "-1", spRateForm.getCurrency(), spRateForm.getAmount(), "Initiated", "I", DateUtil.now("yyyy-MM-dd") + " " + spRateForm.getValidsTo(), "TR", initiatedBy, spRateForm.getPhone(), spRateForm.getEmail());
            LOGGER.info("INITIATED SPECIAL RATE  REQUEST: TXN_REFERENCE{}, RUBIKON DEBIT RATE:{}, RUBIKON CREDIT RATE:{}, CURRENCY CONVERSION : {} GIVEN DEBIT  RATE: {} GIVEN CREDIT RATE: {} SOURCE: {} FX TYPE:{}", reference, spRateForm.getCurrencyConversion(), spRateForm.getRubikonDebitRate(), spRateForm.getRubikonCreditRate(), spRateForm.getRequestedDebitRate(), spRateForm.getRequestedCreditRate(), "TR", spRateForm.getFxType());
        } catch (DataAccessException e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "Africa/Nairobi")
    public String getPendingDealNumbers(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM transfer_special_rates WHERE status='I' ";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(txnReference,' ', currency_conversion,' ', rubikon_debit_rate,' ', rubikon_credit_rate,' ', approved_debit_rate,' ', approved_credit_rate,' ', fxtype,' ', fxToken,' ', senderAcct,' ', senderName,' ', phone,' ', email,' ', partnerCode,' ', currency,' ', amount,' ', fxstatus,' ', status,' ', valid_to,' ', source,' ', initiated_by,' ', approved_by,' ', approver_remarks,' ', initiated_dt,' ', approved_dt) LIKE ? AND  status='I'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(senderBIC) FROM transfers  " + searchQuery, new Object[]{searchValue}, Integer.class);
                mainSql = "SELECT * FROM transfer_special_rates  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT * FROM transfer_special_rates  WHERE status='I' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    GET DEAL NUMBER FOR APPROAL
     */
    public List<Map<String, Object>> getDealNumberForApproval(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfer_special_rates where txnReference=?", reference);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING SWIFT MESSAGE: {}", ex.getMessage());
            return null;
        }
    }

    public Integer approveDealNumber(String reference, String type, String approvedBy) {
        Integer res = -1;
        String status = "A";
        String fxStatus = "OPEN";
        if (type.equalsIgnoreCase("reject")) {
            status = "F";
            fxStatus = "REJECTED";
        }
        try {
            res = jdbcTemplate.update("UPDATE transfer_special_rates SET status=?,fxstatus=?,approved_by=?,approved_dt=? where txnReference=?",
                    status, fxStatus, approvedBy, DateUtil.now(), reference);
            //send SMS ON SUCCESS UPDATE
        } catch (DataAccessException e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }
}
