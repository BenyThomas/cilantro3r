/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.GeneralJsonResponse;
import com.DTO.recon.AdjustedRecon;
import com.entities.CbsRecords;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.reports.ReconReportsRepo;
import com.service.JasperService;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author MELLEJI
 */
@Repository
public class ReportRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcTemplateCbs;

    @Autowired
    ObjectMapper jacksonMapper;
    @Autowired
    Recon_M reconRepo;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportRepo.class);

    @Autowired
    JasperService jasperService;

    public String downloadGatewayTxns(JdbcTemplate jdbcTemplateGw, String query, String txn_type, String ttype) {
        System.out.println("TXN_TYPE:" + txn_type + " TTYPE: " + ttype);
        String msisdn;
        String txndate;
        String txnid;
        String txnReceipt;
        String sourceAccount;
        String destinationAccount;
        String acct_no;
        String description;
        double amount = 0.0D;
        String gateway_status;
        String gateway_second_status;
        String completed_date_time;
        String gateway_final_txstatus;
        String txstatusdesc;
        String docode;
        double charge;
        String status;
        double totalamt;
        System.out.println("QUERY:------ " + query);
        List<Map<String, Object>> resSet = jdbcTemplateGw.queryForList(query);
        for (Map<String, Object> rs : resSet) {
            docode = String.valueOf(rs.get("txtype"));
            msisdn = String.valueOf(rs.get("msisdn"));
            txndate = String.valueOf(rs.get("txdate"));
            txnid = String.valueOf(rs.get("txid"));
            txnReceipt = String.valueOf(rs.get("txReceipt"));
            sourceAccount = String.valueOf(rs.get("txsourceAccount"));
            destinationAccount = String.valueOf(rs.get("txdestinationAccount"));
            acct_no = String.valueOf(rs.get("account"));
            amount = Double.valueOf(String.valueOf(rs.get("txamount")));
            totalamt = amount + Double.valueOf(String.valueOf(rs.get("charge")));
            gateway_status = String.valueOf(rs.get("txstatus"));
            gateway_second_status = String.valueOf(rs.get("second_txstatus"));
            completed_date_time = String.valueOf(rs.get("completed_date_time"));
            gateway_final_txstatus = String.valueOf(rs.get("final_txstatus"));
            txstatusdesc = String.valueOf(rs.get("txstatusdesc"));
            charge = Double.valueOf(rs.get("charge").toString());
            saveSessionState(msisdn, txndate, txnid, txnReceipt, sourceAccount, destinationAccount, Double.valueOf(amount), Double.valueOf(totalamt), gateway_status, gateway_second_status, completed_date_time, gateway_final_txstatus, txstatusdesc, Double.valueOf(charge), txn_type, ttype, acct_no, docode);
        }
        return "Gateway-Txns";
    }

    public int processThirdPartyTransactions(String amountI, String amountPF, String txn_type, String ttype, String txnid, String txndate, String sourceAccount, String receiptNo, String amount, String charge, String currency, String mnoTxns_status, String destinationAcct, String status, String postBalance, String terminal, String description, String pan, String identifier, String acct_no, String file_name) {
        Integer result = 0;
        try {
            System.out.println("*****************************INSERTING THIRD PARTY TRANSACTIONS TO RECON DATABASE [reference= " + txnid + " txndate=" + txndate + "]***************************");
            result = jdbcTemplate.update("insert  into thirdpartytxns(txn_type,ttype,txnid,txndate,sourceAccount,receiptNo,amount,charge,currency,mnoTxns_status,txdestinationaccount,status,post_balance,terminal,description,pan,identifier,acct_no,file_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, currency, mnoTxns_status, destinationAcct, status, postBalance, terminal, description, pan, identifier, acct_no, file_name);
            result = jdbcTemplate.update("insert  into thirdpartytxns(txn_type,ttype,txnid,txndate,sourceAccount,receiptNo,amount,charge,currency,mnoTxns_status,txdestinationaccount,status,post_balance,terminal,description,pan,identifier,acct_no,file_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "PROCESSINGF", "SONGESHAP", txnid, txndate, sourceAccount, receiptNo, amountPF, charge, currency, mnoTxns_status, destinationAcct, status, postBalance, terminal, description, pan, identifier, acct_no, file_name);
            result = jdbcTemplate.update("insert  into thirdpartytxns(txn_type,ttype,txnid,txndate,sourceAccount,receiptNo,amount,charge,currency,mnoTxns_status,txdestinationaccount,status,post_balance,terminal,description,pan,identifier,acct_no,file_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "INTEREST", "SONGESHAP", txnid, txndate, sourceAccount, receiptNo, amountI, charge, currency, mnoTxns_status, destinationAcct, status, postBalance, terminal, description, pan, identifier, acct_no, file_name);

        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        return result;
    }

    @Transactional
    public int downloadTanescoTxnsFromCBS(String balance, String txnid, String txndate, String sourceAccount, String destinationAcct, String amount, String terminal, String status, String pan) {
        System.out.println("---------------------Downloading TANESCO SACCOS TRANSACTIONS FROM CBS --------------------------");
        Integer result = 0;
        try {
            result = jdbcTemplate.update("INSERT ignore INTO `cbstransactiosn`(`post_balance`,`txnid`, `txn_type`, `ttype`, `txndate`, `sourceaccount`, `destinationaccount`, `amount`, `terminal`, `txn_status`, `pan`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    balance, txnid, "ATM_POS", "SETTLEMENT", txndate, sourceAccount, destinationAcct, amount, terminal, status, pan);
        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        return result;
    }

    @Transactional
    public int processCBSTxns(String amountI, String amountPF, String balance, String txnid, String txndate, String sourceAccount, String destinationAcct, String amount, String terminal, String status, String pan, String txn_type, String ttype) {
        System.out.println("---------------------downloading principal to recon--------------------------");
        Integer result = 0;
        try {
            result = jdbcTemplate.update("INSERT  INTO `cbstransactiosn`(`post_balance`,`txnid`, `txn_type`, `ttype`, `txndate`, `sourceaccount`, `destinationaccount`, `amount`, `terminal`, `txn_status`, `pan`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    balance, txnid, txn_type, ttype, txndate, sourceAccount, destinationAcct, amount, terminal, status, pan);
            result = jdbcTemplate.update("INSERT  INTO `cbstransactiosn`(`post_balance`,`txnid`, `txn_type`, `ttype`, `txndate`, `sourceaccount`, `destinationaccount`, `amount`, `terminal`, `txn_status`, `pan`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    balance, txnid, "INTEREST", "SONGESHAP", txndate, sourceAccount, destinationAcct, amountI, terminal, status, pan);
            result = jdbcTemplate.update("INSERT  INTO `cbstransactiosn`(`post_balance`,`txnid`, `txn_type`, `ttype`, `txndate`, `sourceaccount`, `destinationaccount`, `amount`, `terminal`, `txn_status`, `pan`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    balance, txnid, "PROCESSINGF", "SONGESHAP", txndate, sourceAccount, destinationAcct, amountPF, terminal, status, pan);

        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        System.out.println("RESULT[PRINCIPAL]: " + result);
        return result;
    }

    @Transactional
    public int saveSessionState(String msisdn, String txndate, String txnid, String txnReceipt, String sourceAccount, String destinationAccount, Double amount, Double totalamt, String gateway_status, String gateway_second_status, String completed_date_time, String gateway_final_txstatus, String txstatusdesc, Double charge, String txn_type, String ttype, String acct_no, String docode) {
        Integer result = 0;
        try {
            result = jdbcTemplate.update("insert ignore into gatewaytxns(msisdn,txndate,txnid,txnReceipt,txn_type,ttype,sourceAccount,destinationAccount,amount,totalamt,gateway_status,gateway_second_status,completed_date_time,gateway_final_txstatus,txstatusdesc,charge,acct_no,docode,status,) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    msisdn, txndate, txnid, txnReceipt, txn_type, ttype, sourceAccount, destinationAccount, amount, totalamt, gateway_status, gateway_second_status, completed_date_time, gateway_final_txstatus, txstatusdesc, charge, acct_no, docode, gateway_final_txstatus);
            System.out.println("TXN DATE:" + txndate + "TxnId: " + txnid + "TXN_TYPE: " + ttype + " sourceAccount: " + sourceAccount + " destinationAccount:" + destinationAccount + "Amount:" + totalamt + " Status:" + txstatusdesc);
        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        return result;
    }

    @Transactional
    public List<CbsRecords> downloadRubikonTransactions(String query, String ttype, String txn_type) {
        System.out.println("QUERY:" + query);
        System.out.println("TTYPE:" + ttype);
        System.out.println("TXN_TYPE:" + txn_type);
        List<CbsRecords> cbsRecord = this.jdbcTemplateCbs.query(query,
                (ResultSet rs, int rowNum) -> {
                    CbsRecords cbsRecords = new CbsRecords();
                    System.out.println("");
                    return cbsRecords;
                });
        System.out.println(txn_type + "CBS RECORDS:" + cbsRecord.toString());
        return cbsRecord;
    }

    public String getThirdPartyTransactionsReport(String txnType, String fromDate, String toDate) {
        List<Map<String, Object>> findAll;
        String mainSql = "SELECT txn_type,ttype,txnid,DATE_FORMAT(txndate, \"%Y-%m-%d %H:%i:%S\") txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,pan FROM thirdpartytxns WHERE ttype=? and date(txndate)>= ? and  date(txndate)<=? order by txndate desc";
        findAll = this.jdbcTemplate.queryForList(mainSql, txnType, fromDate, toDate);
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.info("RequestBody: ", ex);
        }
        String json = jsonString;
        return json;
    }

    public String getCbsTransactionsReport(String txnType, String fromDate, String toDate) {
        List<Map<String, Object>> findAll;
        String mainSql = "SELECT txnid,txn_type,ttype,txndate as txndate,sourceaccount,destinationaccount,amount,description,contraaccount,dr_cr_ind,branch,currency,txn_status,prevoius_balance,post_balance FROM cbstransactiosn WHERE ttype=? and date(txndate)>= ? and  date(txndate)<=?";
        findAll = this.jdbcTemplate.queryForList(mainSql, txnType, fromDate, toDate);
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.info("RequestBody: ", ex);
        }
        String json = jsonString;
        return json;
    }

    public List<Map<String, Object>> getPosIncomeTransactionReportAjax(String txnType, String period, String month, String year) {
        List<Map<String, Object>> findAll = null;
        String mainSql;
        if (period.equalsIgnoreCase("daily")) {
            mainSql = "SELECT SUM(charge*0.25) AS y, DAY(txndate)  AS label FROM thirdpartytxns WHERE MONTH(txndate)=? AND YEAR(txndate) = ? and identifier='POS' and  mnoTxns_status like '%Success%'  AND ttype=? GROUP BY  DAY(txndate) ORDER BY DAY(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, month, year, txnType);
        } else if (period.equalsIgnoreCase("monthly")) {
            System.out.println("MONTHY:" + year);
            mainSql = "SELECT SUM(charge*0.25) AS y,CASE MONTH(txndate)  WHEN 1 THEN 'January' WHEN 2 THEN 'february' WHEN 3 THEN 'March' WHEN 4 THEN 'March' WHEN 5 THEN 'May' WHEN 6 THEN 'June' WHEN 7 THEN 'July' WHEN 8 THEN 'August' WHEN 9 THEN 'September' WHEN 10 THEN 'October' WHEN 11 THEN 'November' WHEN 12 THEN 'December' end  AS label FROM thirdpartytxns WHERE YEAR(txndate) = ? and identifier='POS' AND mnoTxns_status like '%Success%' AND ttype=? GROUP BY  MONTH(txndate) ORDER BY MONTH(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, year, txnType);
        }

        return findAll;
    }

    public List<Map<String, Object>> getATMIncomeTransactionReportAjax(String txnType, String period, String month, String year) {
        List<Map<String, Object>> findAll = null;
        String mainSql;
        if (period.equalsIgnoreCase("daily")) {
            mainSql = "SELECT count(*)*120 AS y, DAY(txndate)  AS label FROM thirdpartytxns WHERE MONTH(txndate)=? AND YEAR(txndate) = ? and identifier='ATM' and  mnoTxns_status like '%Success%' AND ttype=? GROUP BY  DAY(txndate) ORDER BY DAY(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, month, year, txnType);
        } else if (period.equalsIgnoreCase("monthly")) {
            System.out.println("MONTHY:" + year);
            mainSql = "SELECT count(*)*120 AS y,CASE MONTH(txndate)  WHEN 1 THEN 'January' WHEN 2 THEN 'february' WHEN 3 THEN 'March' WHEN 4 THEN 'March' WHEN 5 THEN 'May' WHEN 6 THEN 'June' WHEN 7 THEN 'July' WHEN 8 THEN 'August' WHEN 9 THEN 'September' WHEN 10 THEN 'October' WHEN 11 THEN 'November' WHEN 12 THEN 'December' end  AS label FROM thirdpartytxns WHERE YEAR(txndate) = ? and mnoTxns_status like '%Success%' and identifier='ATM'  AND ttype=? GROUP BY  MONTH(txndate) ORDER BY MONTH(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, year, txnType);
        }

        return findAll;
    }

    public List<Map<String, Object>> getPosExceptionTransactionReportAjax(String txnType, String period, String month, String year) {
        List<Map<String, Object>> findAll = null;
        String mainSql;
        if (period.equalsIgnoreCase("daily")) {
            mainSql = "SELECT SUM(amount) AS y, DAY(txndate)  AS label FROM thirdpartytxns WHERE MONTH(txndate)=? AND YEAR(txndate) = ? and txn_type='POS' and  mnoTxns_status not  like '%Success%'  AND ttype=? GROUP BY  DAY(txndate) ORDER BY DAY(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, month, year, txnType);
        } else if (period.equalsIgnoreCase("monthly")) {
            System.out.println("MONTHY:" + year);
            mainSql = "SELECT SUM(amount) AS y,CASE MONTH(txndate)  WHEN 1 THEN 'January' WHEN 2 THEN 'february' WHEN 3 THEN 'March' WHEN 4 THEN 'March' WHEN 5 THEN 'May' WHEN 6 THEN 'June' WHEN 7 THEN 'July' WHEN 8 THEN 'August' WHEN 9 THEN 'September' WHEN 10 THEN 'October' WHEN 11 THEN 'November' WHEN 12 THEN 'December' end  AS label FROM thirdpartytxns WHERE YEAR(txndate) = ? and txn_type='POS' AND mnoTxns_status not like '%Success%' AND ttype=? GROUP BY  MONTH(txndate) ORDER BY MONTH(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, year, txnType);
        }

        return findAll;
    }

    public List<Map<String, Object>> getATMExceptionTransactionReportAjax(String txnType, String period, String month, String year) {
        List<Map<String, Object>> findAll = null;
        String mainSql;
        if (period.equalsIgnoreCase("daily")) {
            mainSql = "SELECT sum(amount) AS y, DAY(txndate)  AS label FROM thirdpartytxns WHERE MONTH(txndate)=? AND YEAR(txndate) = ? and identifier='ATM' and  mnoTxns_status not like '%Success%' AND ttype=? GROUP BY  DAY(txndate) ORDER BY DAY(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, month, year, txnType);
        } else if (period.equalsIgnoreCase("monthly")) {
            System.out.println("MONTHY:" + year);
            mainSql = "SELECT sum(amount) AS y,CASE MONTH(txndate)  WHEN 1 THEN 'January' WHEN 2 THEN 'february' WHEN 3 THEN 'March' WHEN 4 THEN 'April' WHEN 5 THEN 'May' WHEN 6 THEN 'June' WHEN 7 THEN 'July' WHEN 8 THEN 'August' WHEN 9 THEN 'September' WHEN 10 THEN 'October' WHEN 11 THEN 'November' WHEN 12 THEN 'December' end  AS label FROM thirdpartytxns WHERE YEAR(txndate) = ? and mnoTxns_status not like '%Success%' and identifier='ATM'  AND ttype=? GROUP BY  MONTH(txndate) ORDER BY MONTH(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, year, txnType);
        }

        return findAll;
    }

    public List<Map<String, Object>> getTransactionTrendReportAjax(String txnType, String period, String month, String year) {
        List<Map<String, Object>> findAll = null;
        String mainSql;
        if (period.equalsIgnoreCase("daily")) {
            mainSql = "SELECT SUM(amount) AS y, DAY(txndate)  AS label FROM thirdpartytxns WHERE MONTH(txndate)=? AND YEAR(txndate) = ? AND ttype=? GROUP BY  DAY(txndate) ORDER BY DAY(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, month, year, txnType);
        } else if (period.equalsIgnoreCase("monthly")) {
            mainSql = "SELECT SUM(amount) AS y,CASE MONTH(txndate)  WHEN 1 THEN 'January' WHEN 2 THEN 'february' WHEN 3 THEN 'March' WHEN 4 THEN 'April' WHEN 5 THEN 'May' WHEN 6 THEN 'June' WHEN 7 THEN 'July' WHEN 8 THEN 'August' WHEN 9 THEN 'September' WHEN 10 THEN 'October' WHEN 11 THEN 'November' WHEN 12 THEN 'December' end  AS label FROM thirdpartytxns WHERE YEAR(txndate) = ? AND ttype=? GROUP BY  MONTH(txndate) ORDER BY MONTH(txndate) ASC";
            findAll = this.jdbcTemplate.queryForList(mainSql, year, txnType);
        }

        return findAll;
    }

    public List<Map<String, Object>> getReconciliationReportAjax(String txnType, String fromDate, String toDate) {
        List<Map<String, Object>> findAll = null;
        String mainSql;
        mainSql = "SELECT * FROM `recon_tracker` WHERE txn_type=? and recondt BETWEEN ? and ? ";
        findAll = this.jdbcTemplate.queryForList(mainSql, txnType, fromDate, toDate);
        return findAll;
    }

    public String getReconCategoriesBasedOnReconType(String reconType) {
        String jsonString = null;
        List<Map<String, Object>> findAll = null;
        try {
            String mainSql;
            mainSql = "SELECT name,code,cbs_account account FROM txns_types WHERE ttype=? ";
            findAll = this.jdbcTemplate.queryForList(mainSql, reconType);
            jsonString = this.jacksonMapper.writeValueAsString(findAll);

        } catch (Exception e) {

        }
        return jsonString;
    }

    public String getAuditTrailReportAjax(String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql = "select count(a.id) from audit_logs a INNER join roles b on a.role_id=b.id where log_date BETWEEN ? and ? ";

        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
        String searchQuery = "";
        System.out.println("HERE MEN");
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = " WHERE concat(log_date,' ',username,' ',ip_address,' ',comments) LIKE ? AND log_date between ? and ?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }

        if (!searchQuery.equals("")) {
            mainSql = "select a.*,b.name as roleName from audit_logs a inner join roles b on a.role_id=b.id " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate + " 00:00:00", toDate + " 23:59:59"});

        } else {
            mainSql = "select a.*,b.name as roleName from audit_logs a INNER join roles b on a.role_id=b.id where log_date BETWEEN ? and ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate + " 00:00:00", toDate + " 23:59:59"});
        }
        //Java objects RESPONSE
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    GET RTGS/EFT TRANSFER TYPES
     */
    //get gateway txns
    public List<Map<String, Object>> getRTGSTransferTypes(String roleId) {
        return this.jdbcTemplate.queryForList("select a.* from transfer_type a inner join transfer_type_role b on b.transfer_type_id=a.id where b.role_id=?", roleId);
    }

    /*
    GET Branch  PAYMENTS REPORTS
     */
    public String getRTGSRemittanceReportAjax(String direction, String branchNo, String ttype, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        fromDate = fromDate + " 00:00:00";
        toDate = toDate + " 23:59:59";
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
       // direction = "outgoing";
        //get report for HQ/IBD USER 
        if (branchNo.equalsIgnoreCase("060")) {
            try {
                //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
                mainSql = "select count(*) from transfers where status='C' and cbs_status='C' and txn_type=? and  create_dt>=? AND create_dt<=?  ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, fromDate, toDate}, Integer.class);
                String searchQuery = "";
                if (!searchValue.equals("")) {
                    searchValue = "%" + searchValue + "%";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = " WHERE  concat(currency,' ',hq_approved_by,' ',beneficiaryBIC,' ',beneficiaryName,' ',purpose,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND status='C' and cbs_status='C' and txn_type=? AND create_dt>=? AND create_dt<=? ";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b" + searchQuery, new Object[]{searchValue, ttype, fromDate, toDate}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.info(mainSql.replace("?", "'{}'"), searchValue, ttype, fromDate, toDate);

                    results = this.jdbcTemplate.queryForList(mainSql, searchValue, ttype, fromDate, toDate);
//                    LOGGER.info("Datatable Response: {}", results);

                } else {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from  transfers b where status='C' and cbs_status='C' and txn_type=? AND create_dt>=? AND create_dt<=?   ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                    this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, fromDate, toDate});
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, fromDate, toDate});
                }
                //Java objects to JSON string - compact-print - salamu - Pomoja.

                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (Exception ex) {
                ex.printStackTrace();
                LOGGER.error("RequestBody: ", ex);
            }
        } else {
            //GET A REPORT FOR BRANCH
            try {
                //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
                mainSql = "select count(*) from transfers where status='C' and cbs_status='C' and txn_type=? and  create_dt>=? AND create_dt<=?  and branch_no=? AND direction=? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, fromDate, toDate, branchNo, direction}, Integer.class);
                String searchQuery = "";
                if (!searchValue.equals("")) {
                    searchValue = "%" + searchValue + "%";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = " WHERE  concat(currency,' ',hq_approved_by,' ',beneficiaryBIC,' ',beneficiaryName,' ',purpose,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND status='C' and cbs_status='C' and txn_type=? AND create_dt>=? AND create_dt<=?  and branch_no=? AND direction=? ";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b " + searchQuery, new Object[]{searchValue, ttype, fromDate, toDate, branchNo, direction}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.info(mainSql.replace("?", "'{}'"), searchValue, ttype, fromDate, toDate);
                    results = this.jdbcTemplate.queryForList(mainSql, searchValue, ttype, fromDate, toDate, direction);
//                    LOGGER.info("Datatable Response: {}", results);

                } else {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from  transfers b where status='C' and cbs_status='C' and txn_type=? AND create_dt>=? AND create_dt<=?  and branch_no=? AND direction=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, fromDate, toDate, branchNo, direction});
                }
                //Java objects to JSON string - compact-print - salamu - Pomoja.
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (Exception ex) {
                ex.printStackTrace();
                LOGGER.error("RequestBody: ", ex);
            }
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    EFT TRANSACTION  PAYMENTS REPORTS
     */
    public String getEftPaymentsReport(String branchNo, String ttype, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        fromDate = fromDate + " 00:00:00";
        toDate = toDate + " 23:59:59";
        List<Map<String, Object>> results = null;
        if (ttype.equals("ALL")) {
            ttype = "'INCOMING','OUTGOING'";
        } else {
            ttype = "'" + ttype + "'";
        }
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER 
        if (branchNo.equalsIgnoreCase("060")) {
            try {
                //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
                mainSql = "select count(*) from transfers where txn_type='005' and direction in (" + ttype + ")  and  create_dt>=? AND create_dt<=? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                String searchQuery = "";
                if (!searchValue.equals("")) {
                    searchValue = "%" + searchValue + "%";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = " WHERE  concat(currency,' ',hq_approved_by,' ',beneficiaryBIC,' ',beneficiaryName,' ',purpose,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND  txn_type='005' AND direction IN (" + ttype + ")  AND create_dt>=? AND create_dt<=?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b" + searchQuery, new Object[]{searchValue, fromDate, toDate}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    LOGGER.info(mainSql.replace("?", "'{}'"), searchValue, ttype, fromDate, toDate);

                    results = this.jdbcTemplate.queryForList(mainSql, searchValue, fromDate, toDate);
                    LOGGER.info("Datatable Response: {}", results);

                } else {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from  transfers b where txn_type='005' and direction IN (" + ttype + ")   AND create_dt>=? AND create_dt<=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }
                //Java objects to JSON string - compact-print - salamu - Pomoja.

                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (Exception ex) {
                ex.printStackTrace();
                LOGGER.error("RequestBody: ", ex);
            }
        } else {
            //GET A REPORT FOR BRANCH
            try {
                //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
                mainSql = "select count(*) from transfers where  txn_type='005' and direction IN (" + ttype + ")  and  create_dt>=? AND create_dt<=?  and branch_no=?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, branchNo}, Integer.class);
                String searchQuery = "";
                if (!searchValue.equals("")) {
                    searchValue = "%" + searchValue + "%";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = " WHERE  concat(currency,' ',hq_approved_by,' ',beneficiaryBIC,' ',beneficiaryName,' ',purpose,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND  txn_type='005' AND direction IN (" + ttype + ")  AND create_dt>=? AND create_dt<=?  and branch_no=?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b" + searchQuery, new Object[]{searchValue, fromDate, toDate, branchNo}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.info(mainSql.replace("?", "'{}'"), searchValue, ttype, fromDate, toDate);
                    results = this.jdbcTemplate.queryForList(mainSql, searchValue, fromDate, toDate);
//                    LOGGER.info("Datatable Response: {}", results);

                } else {
                    mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from  transfers b where txn_type='005' and direction IN (" + ttype + ")   AND create_dt>=? AND create_dt<=?  and branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, branchNo});
                }
                //Java objects to JSON string - compact-print - salamu - Pomoja.
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (Exception ex) {
                ex.printStackTrace();
                LOGGER.error("RequestBody: ", ex);
            }
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    GET SWIFT MESSAGE SUPPORTING DOCUMENT
     */
    public List<Map<String, Object>> getSwiftMessageSupportingDocs(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM transfer_document where txnReference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING SUPPORTING DOCUMENT: {}", e.getMessage());
        }
        return result;
    }

    /*
    GET SWIFT TRANSACTIONS WITH REFERENCE
     */
    public List<Map<String, Object>> getSwiftMessage(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfers where reference=?", reference);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON TRANSFER TABLE: {}", ex.getMessage());
            return null;
        }
    }

    //RECONCILIATION REPORT
    public String getReconSummaryReport(String exporterFileType, HttpServletResponse response, String destName, String txnType, String ttype, String fromDate, String toDate, String printedBy, String reportType) {

        String account = txnType.split("==")[1];
        String TxnType = txnType.split("==")[0];
        List<AdjustedRecon> cuRecon = new ArrayList<>();
        try {
            String reportFileTemplate = "/iReports/recon/adjusted-mobile-reconciliation-report-tcb2.jasper";
            switch (reportType) {
                case "RR":
                    AdjustedRecon data = new AdjustedRecon();
                    data.setCashBookOpeningBalance("11021000");
                    cuRecon.add(data);
                    Map<String, Object> parameters = new HashMap<>();
                    List<Map<String, Object>> bl = reconRepo.getCBSTxnsVolumeOpeningBLClosingBL(txnType, ttype, fromDate + " 00:00:00", toDate + " 23:59:59");
                    List<Map<String, Object>> creditsNotInCBS = getThirdPartyCreditsNotInCBS(txnType, ttype, fromDate, toDate);
                    List<Map<String, Object>> creditsNotInThirdparty = getCBSCreditsNotInThirdparty(txnType, ttype, fromDate, toDate);
                    List<Map<String, Object>> debitsNotInThirdparty = getCBSdebitsNotInThirdparty(txnType, ttype, fromDate, toDate);
                    parameters.put("CASHBOOK_OPENING_BALANCE", new BigDecimal(bl.get(0).get("closingBalance").toString()));
                    parameters.put("CREDIT_COUNTS_NOT_IN_BANK_LEDGER", creditsNotInCBS.get(0).get("txnCount") + "");
                    parameters.put("CREDITS_NOT_IN_TCB", new BigDecimal(creditsNotInCBS.get(0).get("txnVolume").toString()));
                    parameters.put("CREDIT_COUNTS_NOT_IN_THIRDPARTY", creditsNotInThirdparty.get(0).get("txnCount") + "");
                    parameters.put("CREDITS_NOT_THIRDPARTY", creditsNotInThirdparty.get(0).get("txnVolume").toString());
                    parameters.put("DEBITS_COUNTS_IN_THIRDPARTY_NOT_IN_CBS", "0");
                    parameters.put("DEBITS_COUNTS_NOT_IN_BANK_LEDGER", "0");
                    parameters.put("DEBITS_COUNT_NOT_IN_THIRDPARTY", debitsNotInThirdparty.get(0).get("txnCount") + "");
                    parameters.put("DEBITS_AMOUNT_IN_CBS_NOT_IN_THIRDPARTY", new BigDecimal(debitsNotInThirdparty.get(0).get("txnVolume").toString()));
                    parameters.put("ADJUSTED_BANK_LEDGER_CLOSING_BALANCE", new BigDecimal("1000000.00"));
                    parameters.put("BANK_STATEMENT_CLOSING_BALANCE", new BigDecimal("1000000.00"));
                    parameters.put("UNCREDITED_CHEQUES", new BigDecimal("0.00"));
                    parameters.put("UNPRESENTED_CHEQUES", "0.0");
                    parameters.put("RECON_DATE", toDate);
                    parameters.put("ADJUSTED_THIRDPARTY_CLOSING_BALANCE", new BigDecimal("1000000.00"));
                    parameters.put("PRINTED_BY", "MELLEJI MOLLEL");
                    parameters.put("PRINTED_DATE", "2021-11-23 12:23:21");
                    parameters.put("BANK_ACCOUNT", account);
                    parameters.put("ACCOUNT_NAME", TxnType);
                    JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, new JRBeanCollectionDataSource(cuRecon));
                    return jasperService.exportFileOption(print, exporterFileType, response, destName);
                case "CTPNIC":
                    break;
                case "CBNITP":
                    break;
                case "DICNITP":
                    break;
                case "DITPNC":
                    break;
            }

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ReconReportsRepo.class.getName()).log(Level.SEVERE, null, ex);
            return null;

        } catch (Exception ex) {
            LOGGER.info(null, ex);
            java.util.logging.Logger.getLogger(ReconReportsRepo.class.getName()).log(Level.SEVERE, null, ex);

            return null;

        }
        return null;

    }

    public List<Map<String, Object>> getThirdPartyCreditsNotInCBS(String txnType, String ttype, String fromDate, String reconDate) {
        String sql = "select\n"
                + "ifnull(count(*),0) txnCount,ifnull(sum(amount),0) txnVolume\n"
                + "from\n"
                + "	thirdpartytxns\n"
                + "where\n"
                + "	ttype =?\n"
                + "	and txndate >= ? \n"
                + "	and txndate <=? \n"
                + "	and txn_type =?\n"
                + "	and mnoTxns_status IN ('Refund~ Money returned to TPB Disbursement account','Reversed','Reversed to customer account','SUCCESSFUL','Success','Successfully Reversed','successfully','Cash Movement Between Accounts')\n"
                + "	and trim(txnid) not in (\n"
                + "	select\n"
                + "		trim(txnid)\n"
                + "	from\n"
                + "		cbstransactiosn\n"
                + "	where\n"
                + "	ttype =?\n"
                + "	and txn_type =?\n"
                + "	and txndate >= ? \n"
                + "	and txndate <=?)";

        List<Map<String, Object>> result = this.jdbcTemplate.queryForList(sql, ttype, fromDate + " 00:00:00", reconDate + " 23:59:59", txnType, ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59");
        return result;

    }

    public List<Map<String, Object>> getCBSCreditsNotInThirdparty(String txnType, String ttype, String fromDate, String reconDate) {
        String sql = "select\n"
                + "	count(b.txnid) as txnCount,\n"
                + "	IFNULL(SUM(cast(b.amount as decimal(18))), 0) as txnVolume\n"
                + "from\n"
                + "	cbstransactiosn b\n"
                + "where\n"
                + "	trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(a.txnid)\n"
                + "	from\n"
                + "		thirdpartytxns a\n"
                + "	where\n"
                + "     a.ttype = ?\n"
                + "	and a.txn_type = ?\n"
                + "	and a.txndate >= ?\n"
                + "	and a.txndate <= ?)\n"
                + "	and b.ttype = ?\n"
                + "	and b.txn_type = ?\n"
                + "	and b.txndate >= ?\n"
                + "	and b.txndate <= ?\n"
                + "	and b.dr_cr_ind ='CR'\n"
                + "	and trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(c.txnid)\n"
                + "	from\n"
                + "		cbstransactiosn c\n"
                + "	where\n"
                + "		 c.ttype = ?\n"
                + "		and c.txn_type = ?\n"
                + "		and c.txndate >= ?\n"
                + "		and c.txndate <= ?\n"
                + "		and c.dr_cr_ind ='DR')";

        List<Map<String, Object>> result = this.jdbcTemplate.queryForList(sql, ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59", ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59", ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59");
        return result;
    }

    public List<Map<String, Object>> getCBSdebitsNotInThirdparty(String txnType, String ttype, String fromDate, String reconDate) {
        String sql = "	\n"
                + "select\n"
                + "	count(b.txnid) as txnCount,\n"
                + "	IFNULL(SUM(cast(b.amount as decimal(18))), 0) as txnVolume\n"
                + "from\n"
                + "	cbstransactiosn b\n"
                + "where\n"
                + "	trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(a.txnid)\n"
                + "	from\n"
                + "		thirdpartytxns a\n"
                + "	where\n"
                + "	a.ttype = ?\n"
                + "	and a.txn_type = ?\n"
                + "	and a.txndate >= ?\n"
                + "	and a.txndate <= ?)\n"
                + "	and b.ttype = ?\n"
                + "	and b.txn_type = ?\n"
                + "	and b.txndate >= ?\n"
                + "	and b.txndate <= ?\n"
                + "	and b.dr_cr_ind ='DR'\n"
                + "	and trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(c.txnid)\n"
                + "	from\n"
                + "		cbstransactiosn c\n"
                + "	where\n"
                + "		 c.ttype = ?\n"
                + "		and c.txn_type = ?\n"
                + "		and c.txndate >= ?\n"
                + "		and c.txndate <= ?\n"
                + "		and c.dr_cr_ind ='CR')";

        List<Map<String, Object>> result = this.jdbcTemplate.queryForList(sql, ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59", ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59", ttype, txnType, fromDate + " 00:00:00", reconDate + " 23:59:59");
        return result;
    }




    public int insertChargeTransaction(String reference,String debitAccount,String  currency,BigDecimal charge,int no_of_page, String txn_type, BigDecimal txn_type_amount, String initiated_by, String income_legder, String branch_code) {
        int res = -1;
        String   sql="INSERT INTO charge_transactions (reference, debit_account, currency, charge, no_of_page, txn_type, txn_type_amount, initiated_by, income_legder, branch_code,response_msg) VALUES (?,?,?,?,?,?,?,?,?,?,'Now posting to cbs')";
        res = this.jdbcTemplate.update(sql,reference,debitAccount,currency,charge,no_of_page,txn_type, txn_type_amount,  initiated_by,  income_legder, branch_code);
        try {
        } catch (DataAccessException ex) {
            LOGGER.error("insertChargeTransaction: ", ex);
        }

        return res;
    }

    public int updateChargeTransaction(String reference,String status ,String cbsStatus,String responseCode,String responseMsg){
        int res = -1;
        String   sql="UPDATE charge_transactions SET status =?, cbs_status = ?,response_code=?,response_msg=? WHERE reference=?";
        res = this.jdbcTemplate.update(sql,status,cbsStatus,responseCode,responseMsg,reference);
        try {
        } catch (DataAccessException ex) {
            LOGGER.error("insertChargeTransaction: ", ex);
        }

        return res;
    }

    public List<Map<String,Object>> getOnlineTransactions(Map<String, String> customeQuery) {
        List<Map<String,Object>> response = null;
        return response;
    }
}
