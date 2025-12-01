/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.CummulativeReconciliationObject;
import com.DTO.recon.BankStReconDataReq;
import com.DTO.recon.LedgerReconDataReq;
import com.DTO.recon.Requests.GeneralReconConfig;
import com.config.SYSENV;
import com.entities.ReconDashboard;
import com.entities.ReconExceptions;
import com.entities.ReconSummaryReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.TxnsCBSDownloader;
import com.service.HttpClientService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.service.JasperService;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author MELLEJI
 */
@Repository
public class Recon_M {

    String transRefs = "";

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    ObjectMapper jacksonMapper;
    @Autowired
    HttpSession httpSession;
    String reconDate2;
    @Autowired
    @Qualifier("gwdb")
    JdbcTemplate jdbcGWTemplate;
    @Autowired
    @Qualifier("mkobadb")
    JdbcTemplate jdbcMKOBATemplate;
    @Autowired
    @Qualifier("txManagerMaster")
    PlatformTransactionManager txManager;

    @Autowired
    @Qualifier("dbPoolExecutor")
    ThreadPoolTaskExecutor exec;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;
    @Autowired
    SYSENV systemVariable;

    @Autowired
    JasperService jasperService;

    @Autowired
    @Qualifier("gwAirtelVikobaDBConnection")
    JdbcTemplate jdbcAirtelVikobaTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpRepo.class);

    public List<Map<String, Object>> getReconTxntypes(String roleId, String userId) {
        return this.jdbcTemplate.queryForList("select * from recon_types a INNER JOIN recon_types_roles b on b.recon_type_id=a.id INNER JOIN user_roles c on c.role_id=b.role_id INNER JOIN users d on d.id=c.user_id where c.role_id=? and d.id=?", roleId, userId);
    }

    public int rowspan = 0;

    public List<ReconDashboard> getReconDashboard(String txnDate, String txnType) {

        try {
            List<ReconDashboard> reconDatas
                    = this.jdbcTemplate.query("select * from txns_types where ttype=? AND isAllowed=1", new Object[]{txnType}, new RowMapper<ReconDashboard>() {
                @Override
                public ReconDashboard mapRow(ResultSet rs, int rowNum) throws SQLException {
                    rowspan++;
                    ReconDashboard reconData = new ReconDashboard();
//                            System.out.println("Getting txns,opening and closing balance of [" + rs.getString("code") + "]");
                    //assign the volumes
                    reconData.setTxn_type(rs.getString("code"));
                    reconData.setGetCoreBanking(getCBSTxnsVolumeOpeningBLClosingBL(rs.getString("code"), txnType, txnDate + " 00:00:00", txnDate + " 23:59:59"));
                    reconData.setGetThirdParty(getThirdPartyTxnsVolumeOpeningBLClosingBL(rs.getString("code"), txnType, txnDate + " 00:00:00", txnDate + " 23:59:59"));
//                            System.out.println("OBTAINED TXNS,OPENING AND CLOSING BALANCES DETAILS: " + reconData.toString());

                    return reconData;
                }
            });
            return reconDatas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<ReconSummaryReport> initiateRecon(String txnDate, String ttype, String username) {
        try {
//            System.out.println("====================INITIATE/UPDATE RECONCILIATION===============");
            List<ReconSummaryReport> summaryReport;
            summaryReport = this.jdbcTemplate.query("select * from txns_types where ttype=? order by id asc", new Object[]{ttype}, (ResultSet rs, int rowNum) -> {
                String description;
                String cbstxnsCount;
                String cbstxnsVolume;
                String cbstxnsCharge;
                String thirdPartytxnsCount;
                String thirdPartytxnsVolume;
                String thirdpartyCharge;
                String difftxnCount;
                Double difftxnVolume;
                Double diffCharge;
                String exceptionType = "";
                String exceptionTxnsCount = "0";
                String exceptionTxnsVolume = "0.00";
                String first_status = "L";
                String second_status = "-1";
                String initiated_by = username;
                List<Map<String, Object>> cbsTxns = getCBSTxns(rs.getString("code"), ttype, txnDate + " 00:00:00", txnDate + " 23:59:59");
                List<Map<String, Object>> thirdPartTxns = getThirdPartyTxns(rs.getString("code"), ttype, txnDate);
                //get opening and closing blance of txn_type
//                /description = "OPENING BL " + rs.getString("name");
                //List<Map<String, Object>> cbsOpenigClosing = getCBSTxnsVolumeOpeningBLClosingBL(rs.getString("code"), ttype, txnDate);
                //List<Map<String, Object>> thirdOpenigClosing = getThirdPartyTxnsVolumeOpeningBLClosingBL(rs.getString("code"), ttype, txnDate);
                ///initiateReconciliation(ttype, description, "0", String.valueOf(cbsOpenigClosing.get(0).get("openingBalance")), "0", String.valueOf(thirdOpenigClosing.get(0).get("openingBalance")), "0", Double.valueOf(String.valueOf(thirdOpenigClosing.get(0).get("openingBalance"))) - Double.valueOf(String.valueOf(cbsOpenigClosing.get(0).get("openingBalance"))), first_status, second_status, initiated_by, txnDate, "0", "0", 0.0);

                ReconSummaryReport summaryPerMno = new ReconSummaryReport();
                summaryPerMno.setTxnType(rs.getString("name"));
                Double lukuExCount = 0.0;
                Double lukuExVolume = 0.0;

                description = rs.getString("name");

                cbstxnsCount = String.valueOf(cbsTxns.get(0).get("txnCount"));
                cbstxnsVolume = String.valueOf(cbsTxns.get(0).get("txnVolume"));
                cbstxnsCharge = String.valueOf(cbsTxns.get(0).get("charge"));

                thirdPartytxnsCount = String.valueOf(thirdPartTxns.get(0).get("txnCount"));
                thirdPartytxnsVolume = String.valueOf(thirdPartTxns.get(0).get("txnVolume"));
                thirdpartyCharge = String.valueOf(thirdPartTxns.get(0).get("charge"));
                LOGGER.info("initiateRecon: {} - {}", rs.getString("code"), ttype);
               /*if (rs.getString("code") != null && rs.getString("code").equals("LUKU")) {
                    List<Map<String, Object>> cbsSuspensTxns = getCBSSuspensTxns("LUKUEX", ttype, txnDate + " 00:00:00", txnDate + " 23:59:59");

                    lukuExCount = Double.valueOf(cbsSuspensTxns.get(0).get("txnCount") + "");
                    lukuExVolume = Double.valueOf(cbsSuspensTxns.get(0).get("txnVolume") + "");

                    cbstxnsCount = String.valueOf(Double.valueOf(cbstxnsCount) + lukuExCount);
                    cbstxnsVolume = String.valueOf(Double.valueOf(cbstxnsVolume) + lukuExVolume);
                }*/
                difftxnCount = String.valueOf(Double.valueOf(cbstxnsCount) - Double.valueOf(thirdPartytxnsCount));
                difftxnVolume = Double.valueOf(cbstxnsVolume) - Double.valueOf(thirdPartytxnsVolume);
                diffCharge = Double.valueOf(cbstxnsCharge) - Double.valueOf(thirdpartyCharge);

//                System.out.println("Description: " + description + " cbstxnsCount:" + cbstxnsCount + " cbstxnsVolume:" + cbstxnsVolume + " thirdPartytxnsCount:" + thirdPartytxnsCount + " thirdPartytxnsVolume:" + thirdPartytxnsVolume + " difftxnCount:" + difftxnCount + " difftxnVolume:" + difftxnVolume + " exceptionType:" + exceptionType + " exceptionTxnsCount:" + exceptionTxnsCount + " exceptionTxnsVolume:" + exceptionTxnsVolume);
                /*
                save the reconciliation data for reports tracking
                 */
                if (rs.getString("code") != null && !rs.getString("code").equals("LUKUEX")) {

                    initiateReconciliation(ttype, description, cbstxnsCount, cbstxnsVolume, thirdPartytxnsCount, thirdPartytxnsVolume, difftxnCount, difftxnVolume, first_status, second_status, initiated_by, txnDate, cbstxnsCharge, thirdpartyCharge, diffCharge);
                }
                return summaryPerMno;
            });
            return summaryReport;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<ReconExceptions> initiateReconExcetpions(String txnDate, String ttype, String username) {
        try {
//            System.out.println("====================RECON EXCEPTIONS INSERT/UPDATE===============");
            List<ReconExceptions> reconExceptions
                    = this.jdbcTemplate.query("select * from exceptions order by id asc", (ResultSet rs, int rowNum) -> {
                String exceptionType = "";
                String exceptionTxnsCount = "0";
                String exceptionTxnsVolume = "0.00";
                String initiated_by = username;
                ReconExceptions reconException = new ReconExceptions();

                List<Map<String, Object>> exception = getReconExceptions(rs.getString("query"), ttype, txnDate + " 00:00:00", txnDate + " 23:59:59");
                reconException.setExceptions(exception);

                exceptionType = rs.getString("name");
                System.out.println("EXCEPTION NAME: " + exceptionType);
                exceptionTxnsCount = String.valueOf(exception.get(0).get("txnCount"));
                exceptionTxnsVolume = String.valueOf(exception.get(0).get("txnVolume"));
                System.out.println("RESULTS STRING:" + exception.toString());
                //insert reconciliation data
                saveReconciliationException(ttype, exceptionType, exceptionTxnsCount, exceptionTxnsVolume, txnDate, initiated_by);
                return reconException;
            });
            return reconExceptions;
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    //get recon exceptions
    public List<Map<String, Object>> getReconExceptions(String query, String ttype, String txndate, String txndate2) {
//        LOGGER.info("EXCEPTION QUERY:" + query.replace("?", "'{}'"), ttype, txndate, txndate2, ttype, txndate, txndate2);
        query = query.replace("{ttype}", ttype);
        query = query.replace("{txndate}", txndate);
        query = query.replace("{txndate2}", txndate2);
//        LOGGER.info(query);
        if (query.contains("LUKUEX")) {
            return this.jdbcTemplate.queryForList(query);
        } else {
            return this.jdbcTemplate.queryForList(query, ttype, txndate, txndate2, ttype, txndate, txndate2);
        }
    }

    /*
     *get cbs transactions per mno on cbs
     */
    public List<Map<String, Object>> getCBSTxns(String txnType, String ttype, String txnDate, String txnDate2) {
        //System.out.println("GETTING CBS TRANSACTIONS FOR [" + txnType + "]")
        // LOGGER.info("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume,IFNULL(SUM(Cast(charge as decimal(18))),0)  as charge FROM cbstransactiosn WHERE txndate>=? and txndate<=? AND ttype=? and txn_type=? and txn_status='success' and txnid not in (select txnid from cbstransactiosn where ttype=? and txn_type=? and txndate>=? and txn_status not like '%Success%') ".replace("?", "'{}'"), txnDate, txnDate2, ttype, txnType, ttype, txnType, txnDate);
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume,IFNULL(SUM(Cast(charge as decimal(18))),0)  as charge FROM cbstransactiosn WHERE txndate>=? and txndate<=? AND ttype=? and txn_type=? and txn_status='success' and txnid not in (select txnid from cbstransactiosn where ttype=? and txn_type=? and txndate>=? and txn_status not like '%Success%') ", txnDate, txnDate2, ttype, txnType, ttype, txnType, txnDate);
    }

    public List<Map<String, Object>> getCBSSuspensTxns(String txnType, String ttype, String txnDate, String txnDate2) {
        if (txnType.equals("LUKUEX")) {
            LOGGER.info("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume,IFNULL(SUM(Cast(charge as decimal(18))),0)  as charge FROM suspe_cbstransactiosn WHERE txndate>=? and txndate<=? AND ttype=? and txn_type=? and txn_status='success'".replace("?", "'{}'"), txnDate, txnDate2, ttype, txnType);
        }
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume,IFNULL(SUM(Cast(charge as decimal(18))),0)  as charge FROM suspe_cbstransactiosn WHERE txndate>=? and txndate<=? AND ttype=? and txn_type=? and txn_status='success'", txnDate, txnDate2, ttype, txnType);
    }

    /*
     *get cbs transactions(atm+POS) per mno on cbs
     */
    public List<Map<String, Object>> getTPBTotalTransactions(String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume FROM thirdpartytxns WHERE DATE(txndate)=? AND ttype=?", txnDate, ttype);
    }

    public List<Map<String, Object>> getCBSTotalTransactions(String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume FROM cbstransactiosn WHERE DATE(txndate)=? AND ttype=? and txnid not in (select txnid from cbstransactiosn where ttype=? and date(txndate)=? and txn_status not like '%success%')", txnDate, ttype, ttype, txnDate);
    }

    /*
     *get cbs transactions INCOME (atm+POS) per mno on cbs
     */
    public List<Map<String, Object>> getTPBtransactionsIncome(String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT COUNT(*)+(SELECT count(*) FROM `thirdpartytxns` WHERE ttype='SETTLEMENT' and txn_type='ATM' AND date(txndate)='" + txnDate + "') txnCount,sum(charge*0.25)+(SELECT count(*)*120 FROM `thirdpartytxns` WHERE ttype='SETTLEMENT' and txn_type='ATM' AND date(txndate)='" + txnDate + "') as txnVolume FROM `thirdpartytxns` WHERE ttype='SETTLEMENT' and txn_type='POS' AND date(txndate)='" + txnDate + "'");
    }

    public List<Map<String, Object>> getCBStransactionsIncome(String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT COUNT(*)+(SELECT count(*) FROM `thirdpartytxns` WHERE ttype='SETTLEMENT' and txn_type='ATM' AND date(txndate)='" + txnDate + "') txnCount,sum(charge*0.25)+(SELECT count(*)*120 FROM `thirdpartytxns` WHERE ttype='SETTLEMENT' and txn_type='ATM' AND date(txndate)='" + txnDate + "') as txnVolume FROM `thirdpartytxns` WHERE ttype='SETTLEMENT' and txn_type='POS' AND date(txndate)='" + txnDate + "'");
    }

    /*
     *get mno transactions received on the file
     */
    public List<Map<String, Object>> getThirdPartyTxns(String txnType, String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume,IFNULL(SUM(Cast(charge as decimal(18))),0)  as charge  FROM thirdpartytxns WHERE txndate>='" + txnDate + " 00:00:00' and txndate<='" + txnDate + " 23:59:59' AND ttype=? and txn_type=? and mnoTxns_status like '%success%' and txnid not in (select txnid from thirdpartytxns where ttype='" + ttype + "' and txn_type='" + txnType + "' and txndate>='" + txnDate + " 00:00:00'  and mnoTxns_status not like '%success%')", ttype, txnType);
    }

    /*
     *get cbs Success and mno failed
     */
    public List<Map<String, Object>> getCbsSuccessMNOFailed(String ttype, String txnDate) {
//        System.out.println("GETTING CBS SUCCESS THIRD PARTY FAILED TRANSACTIONS FOR [" + ttype + "]");

        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume FROM cbstransactiosn WHERE DATE(txndate)='" + txnDate + "' AND ttype='" + ttype + "'  and txnid not in(select txnid from cbstransactiosn where ttype=? and date(txndate)=? and txn_status not like '%success%') and  txnid  in (SELECT txnid FROM thirdpartytxns WHERE DATE(txndate)=? AND ttype=?  AND mnoTxns_status NOT LIKE '%success%')", ttype, txnDate, txnDate, ttype);
    }

    /*
     *get MNO Success and CBS failed
     */
    public List<Map<String, Object>> getMNOSuccessCbsFailed(String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume FROM thirdpartytxns WHERE DATE(txndate)='" + txnDate + "' AND ttype='" + ttype + "' and mnoTxns_status like '%success%' and txnid not in (SELECT txnid FROM cbstransactiosn WHERE DATE(txndate)=? AND ttype=?)", txnDate, ttype);
    }

    /*
     *get MNO Success and CBS failed
     */
    public List<Map<String, Object>> getNotInMNO(String ttype, String txnDate) {

        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume FROM cbstransactiosn WHERE DATE(txndate)='" + txnDate + "' AND ttype='" + ttype + "'  and txnid not in (SELECT txnid FROM thirdpartytxns WHERE DATE(txndate)=? AND ttype=?)", txnDate, ttype);
    }

    /*
     *get MNO Success and CBS failed
     */
    public List<Map<String, Object>> getNotInCBS(String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18))),0)  as txnVolume FROM thirdpartytxns WHERE DATE(txndate)='" + txnDate + "' AND ttype='" + ttype + "' and mnoTxns_status like '%success%' and txnid not in (SELECT txnid FROM cbstransactiosn WHERE DATE(txndate)=? AND ttype=?)", txnDate, ttype);
    }

    /*
     *get transactions exceptions
     */
    public List<Map<String, Object>> getExceptionTxns(String txnType, String ttype, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT txn_type,IFNULL(SUM(Cast(post_balance as decimal(18,2))),0) post_balance FROM cbstransactiosn WHERE DATE(txndate)=? AND ttype=? group by txn_type order by txn_type,txndate DESC", txnDate, txnType);
    }

    /*
    get trnsactions dashbord opening balance,transactions volume,closing balance and their differences
     */
    public List<Map<String, Object>> getCBSOpeningBL(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT txn_type,IFNULL(SUM(Cast(post_balance as decimal(18,2))),0) post_balance FROM cbstransactiosn WHERE DATE(txndate)=? AND ttype=? order by txndate DESC limit 1", txnDate, txnType);
    }

    public List<Map<String, Object>> getOpeningThirdPartyBL(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT txn_type,IFNULL(SUM(Cast(post_balance as decimal(18,2))),0) post_balance FROM thirdpartytxns WHERE DATE(txndate)=? AND ttype=? order by txndate DESC limit 1", txnDate, txnType);
    }

    public List<Map<String, Object>> getCBSTxnsVolume(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18,2))),0)  as txnVolume,txn_type FROM `cbstransactiosn` WHERE DATE(txndate)=? AND ttype=?", txnDate, txnType);
    }

    public List<Map<String, Object>> getTxnsThirdPartyVolume(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT count(*) as txnCount,IFNULL(SUM(Cast(amount as decimal(18,2))),0)  as txnVolume,txn_type FROM thirdpartytxns WHERE DATE(txndate)=? AND ttype=? ", txnDate, txnType);
    }

    public List<Map<String, Object>> getCBSClosingBL(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT COALESCE(post_balance,0) post_balance FROM cbstransactiosn WHERE DATE(txndate)=? AND ttype=? order by txndate DESC limit 1", txnType, txnDate);
    }

    public List<Map<String, Object>> getClosingThirdPartyBL(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("SELECT txn_type,post_balance FROM thirdpartytxns WHERE DATE(txndate)=DATE_SUB(?, INTERVAL 1 DAY) AND ttype=? order by txndate DESC limit 1", txnDate, txnType);
    }

    public List<Map<String, Object>> getTxnsDiffVolume(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("select a.name,a.url from permissions a INNER JOIN module_permission b on a.id=b.permission_id INNER join module_roles c on c.module_id=b.module_id where b.module_id=? and a.id in (select bb.permission_id from permission_role  bb  INNER join  user_roles d ON d.role_id=bb.role_id  INNER JOIN users aa on aa.id=d.user_id where aa.id=?)", txnType, txnDate);
    }

    public List<Map<String, Object>> getTxnsExcepVolume(String txnType, String txnDate) {
        return this.jdbcTemplate.queryForList("select a.name,a.url from permissions a INNER JOIN module_permission b on a.id=b.permission_id INNER join module_roles c on c.module_id=b.module_id where b.module_id=? and a.id in (select bb.permission_id from permission_role  bb  INNER join  user_roles d ON d.role_id=bb.role_id  INNER JOIN users aa on aa.id=d.user_id where aa.id=?)", txnType, txnDate);
    }
    //get transaction confirmation using TXID OR REFERENCE from cbs

    public List<Map<String, Object>> getConfirmationCBS(String txnid) {
        return this.jdbcTemplate.queryForList("select * from cbstransactiosn where txnid=?", txnid);
    }

    public List<Map<String, Object>> getConfirmationOnSuspeCBS(String txnid) {
        return this.jdbcTemplate.queryForList("select * from suspe_cbstransactiosn where txnid=?", txnid);
    }

    //get MSISDN from gateway Table
    public List<Map<String, Object>> getMsisdnGW(String txnid) {
        return this.jdbcGWTemplate.queryForList("select * from tp_transaction where txid=?", txnid);
    }

    //confirm bulk transactions on cbs
    public List<Map<String, Object>> getConfirmCBSBulk(String txnid) {
        return this.jdbcTemplate.queryForList("select * from cbstransactiosn where txnid IN (" + txnid + ")");
    }

    //confirm bulk transactions on third party
    public List<Map<String, Object>> getConfirmThirdPartyBulk(String txnid) {
        return this.jdbcTemplate.queryForList("select txn_type, ttype, txnid,DATE_FORMAT(txndate, '%Y-%m-%d %H:%i:%S') as txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, terminal, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name, pan, identifier from thirdpartytxns where  txnid IN (" + txnid + ")");
    }
    //get transaction confirmation using TXID OR REFERENCE from thirdparty

    public List<Map<String, Object>> getConfirmationThirdParty(String txnid) {
        return this.jdbcTemplate.queryForList("select * from thirdpartytxns where txnid=?", txnid);
    }

    //get reconciliation dashboard TABS
    public List<Map<String, Object>> getReconDashboardTabs(String txnType) {
        String sql = "select b.name as name,a.url as url, a.ajax_url as ajaxUrl,a.maxdays_allowed_operation as maxdays_allowed_operation,b.exception_type as exception_type from report_setup a INNER JOIN report_setup_txntype b on b.report_setup_id=a.id INNER JOIN recon_types c on c.id=b.txntype_id where c.code=? order by a.id asc";
        //LOGGER.info(sql.replace("?","'{}'"),txnType);
        return this.jdbcTemplate.queryForList(sql, txnType);
    }

    public List<Map<String, Object>> getTxnsType(String txnType) {
        return this.jdbcTemplate.queryForList("SELECT * FROM txns_types where ttype=? AND isAllowed=1", txnType);
    }

    //get cbs transactions in a datatable format(ajax)
//    public String getCbsTxnsList(String txnType, String ttype, String txndate) {
//        List<Map<String, Object>> findAll;
//        String mainSql = "";
//        if (txnType.equalsIgnoreCase("all")) {
//            mainSql = "SELECT txnid,txn_type,ttype,txndate as txndate,sourceaccount,destinationaccount,amount,description,contraaccount,dr_cr_ind,branch,currency,txn_status,prevoius_balance,post_balance FROM `cbstransactiosn` WHERE  ttype=? and date(txndate)=? and txn_status like '%success%'";
//            findAll = this.jdbcTemplate.queryForList(mainSql, ttype, txndate);
//        } else {
//            mainSql = "SELECT txnid,txn_type,ttype,txndate as txndate,sourceaccount,destinationaccount,amount,description,contraaccount,dr_cr_ind,branch,currency,txn_status,prevoius_balance,post_balance FROM `cbstransactiosn` WHERE txn_type=? and ttype=? and date(txndate)=? and txn_status like '%success%'";
//            findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
//
//        }
//        //Java objects to JSON string - compact-print - salamu - Pomoja.
//        String jsonString = null;
//        try {
//            jsonString = this.jacksonMapper.writeValueAsString(findAll);
//            //LOGGER.info("RequestBody");
//        } catch (JsonProcessingException ex) {
//            LOGGER.info("RequestBody: ", ex);
//        }
//        String json = jsonString;
//        return json;
//    }
    /*
    Core banking Transactions
     */
    public String getCoreBankingTxnsAjax(String txnType, String ttype, String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder, String exceptionType) {
        List<Map<String, Object>> results = null;
        String mainSql;
        String mnoMainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String json = null;
        List<Map<String, Object>> cbsDB = null;
        List<Map<String, Object>> mnoArrayDB = null;
        String jsonString = null;
        if (!ttype.equals("AIRTEL-VIKOBA")) {
            if (exceptionType.equals("333")) {
                //THIS IS NOT IN THIRDPART
                List<String> cbsArray = new ArrayList<>();
                List<String> mnoArray = new ArrayList<>();
                List<String> finalRerefernceArray = new ArrayList<>();


                if (txnType.equalsIgnoreCase("all")) {
                    mainSql = "select b.txnid\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%')";
                    cbsDB = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});


                    mnoMainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    mnoArrayDB = this.jdbcTemplate.queryForList(mnoMainSql, ttype, txndate);

                    for (Map<String, Object> map : cbsDB) {
                        cbsArray.add(map.get("txnid") + "");
                    }

                    LOGGER.info("cbsArrayt: {}\ncbsArrayDuplicateRemoved: {}", cbsArray);


                    for (Map<String, Object> map : mnoArrayDB) {
                        mnoArray.add(map.get("txnid") + "");
                    }
                    LOGGER.info("mnoArray: {}", mnoArray);

                    for (String reference : cbsArray) {
                        if (!mnoArray.contains(reference.trim())) {
                            finalRerefernceArray.add(reference);
                        }
                    }

                    LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
                    String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));

                    mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and  b.txnid in (" + finalRerefernces + ")";
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});
                    //LOGGER.info(mainSql, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType);

                    totalRecordwithFilter = 0;
                    totalRecords = 0;
                    json = null;
                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

                } else {
                    mainSql = "select b.txnid\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                    cbsDB = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});


                    mnoMainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    mnoArrayDB = this.jdbcTemplate.queryForList(mnoMainSql, txnType, ttype, txndate);


                    for (Map<String, Object> map : cbsDB) {
                        cbsArray.add(map.get("txnid") + "");
                    }

                    LOGGER.info("cbsArrayt: {}\ncbsArrayDuplicateRemoved: {}", cbsArray);


                    for (Map<String, Object> map : mnoArrayDB) {
                        mnoArray.add(map.get("txnid") + "");
                    }
                    LOGGER.info("mnoArray: {}", mnoArray);

                    for (String reference : cbsArray) {
                        if (!mnoArray.contains(reference.trim())) {
                            finalRerefernceArray.add(reference);
                        }
                    }
                    LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
                    String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));

                    mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=? and  b.txnid in (" + finalRerefernces + ")";
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
                    //LOGGER.info(mainSql, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType);

                    totalRecordwithFilter = 0;
                    totalRecords = 0;
                    json = null;
                    //Java objects to JSON string - compact-print - salamu - Pomoja.
                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    //   jsonString = "{}";
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                    // LOGGER.info("json: {}", json);
                }
            } else {

                if (txnType.equalsIgnoreCase("all")) {
                    mainSql = "select count(b.txnid)\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%')";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"}, Integer.class);
                    String searchQuery = "";
                    if (!searchValue.equals("")) {
                        searchValue = "%" + searchValue + "%";
                        searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%')";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b " + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"}, Integer.class);
                    } else {
                        totalRecordwithFilter = totalRecords;
                    }
                    if (!searchQuery.equals("")) {
                        mainSql = "select * from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});

                    } else {
                        // mainSql = "select * from cbstransactiosn b where b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? AND a.mnoTxns_status LIKE '%Success%') and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%') and txn_type=?";
                        mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});
                    }

                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                } else {
                    mainSql = "select count(b.txnid)\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
                    String searchQuery = "";
                    if (!searchValue.equals("")) {
                        searchValue = "%" + searchValue + "%";
                        searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b " + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
                    } else {
                        totalRecordwithFilter = totalRecords;
                    }
                    if (!searchQuery.equals("")) {
                        mainSql = "select * from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

                    } else {
                        // mainSql = "select * from cbstransactiosn b where b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? AND a.mnoTxns_status LIKE '%Success%') and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%') and txn_type=?";
                        mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
                    }

                    //Java objects to JSON string - compact-print - salamu - Pomoja.
                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

                }
            }
        } else {
            if (exceptionType.equals("333")) {
                List<String> cbsArray = new ArrayList<>();
                List<String> mnoArray = new ArrayList<>();
                List<String> finalRerefernceArray = new ArrayList<>();


                if (txnType.equalsIgnoreCase("all")) {
                    mainSql = "select b.thirdparty_reference\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%')";
                    cbsDB = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});


                    mnoMainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    mnoArrayDB = this.jdbcTemplate.queryForList(mnoMainSql, ttype, txndate);

                    for (Map<String, Object> map : cbsDB) {
                        cbsArray.add(map.get("thirdparty_reference") + "");
                    }

                    LOGGER.info("cbsArrayt: {}\ncbsArrayDuplicateRemoved: {}", cbsArray);


                    for (Map<String, Object> map : mnoArrayDB) {
                        mnoArray.add(map.get("receiptNo") + "");
                    }
                    LOGGER.info("mnoArray: {}", mnoArray);

                    for (String reference : cbsArray) {
                        if (!mnoArray.contains(reference.trim())) {
                            finalRerefernceArray.add(reference);
                        }
                    }

                    LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
                    String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));

                    mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and  b.thirdparty_reference in (" + finalRerefernces + ")";
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});
                    //LOGGER.info(mainSql, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType);

                    totalRecordwithFilter = 0;
                    totalRecords = 0;
                    json = null;
                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

                } else {
                    mainSql = "select b.thirdparty_reference\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                    cbsDB = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});


                    mnoMainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    mnoArrayDB = this.jdbcTemplate.queryForList(mnoMainSql, txnType, ttype, txndate);


                    for (Map<String, Object> map : cbsDB) {
                        cbsArray.add(map.get("thirdparty_reference") + "");
                    }

                    LOGGER.info("cbsArrayt: {}\ncbsArrayDuplicateRemoved: {}", cbsArray);


                    for (Map<String, Object> map : mnoArrayDB) {
                        mnoArray.add(map.get("receiptNo") + "");
                    }
                    LOGGER.info("mnoArray: {}", mnoArray);

                    for (String reference : cbsArray) {
                        if (!mnoArray.contains(reference.trim())) {
                            finalRerefernceArray.add(reference);
                        }
                    }
                    LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
                    String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));

                    mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=? and  b.thirdparty_reference in (" + finalRerefernces + ")";
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
                    //LOGGER.info(mainSql, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType);

                    totalRecordwithFilter = 0;
                    totalRecords = 0;
                    json = null;
                    //Java objects to JSON string - compact-print - salamu - Pomoja.
                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    //   jsonString = "{}";
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                    // LOGGER.info("json: {}", json);
                }
            } else {

                if (txnType.equalsIgnoreCase("all")) {
                    mainSql = "select count(b.txnid)\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%')";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"}, Integer.class);
                    String searchQuery = "";
                    if (!searchValue.equals("")) {
                        searchValue = "%" + searchValue + "%";
                        searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%')";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b " + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"}, Integer.class);
                    } else {
                        totalRecordwithFilter = totalRecords;
                    }
                    if (!searchQuery.equals("")) {
                        mainSql = "select * from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});

                    } else {
                        // mainSql = "select * from cbstransactiosn b where b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? AND a.mnoTxns_status LIKE '%Success%') and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%') and txn_type=?";
                        mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00"});
                    }

                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                } else {
                    mainSql = "select count(b.thirdparty_reference)\n"
                            + "from cbstransactiosn b  where  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                            + "b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
                    String searchQuery = "";
                    if (!searchValue.equals("")) {
                        searchValue = "%" + searchValue + "%";
                        searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b " + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
                    } else {
                        totalRecordwithFilter = totalRecords;
                    }
                    if (!searchQuery.equals("")) {
                        mainSql = "select * from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

                    } else {
                        // mainSql = "select * from cbstransactiosn b where b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? AND a.mnoTxns_status LIKE '%Success%') and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%') and txn_type=?";
                        mainSql = "select * from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
                    }

                    //Java objects to JSON string - compact-print - salamu - Pomoja.
                    try {
                        jsonString = this.jacksonMapper.writeValueAsString(results);
                    } catch (JsonProcessingException ex) {
                        LOGGER.error("RequestBody: ", ex);
                    }
                    json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

                }
            }
        }
        return json;
    }

    //get cbs reversed transactions in a datatable format(ajax)
    public String getCbsReversedTxnsList(String txnType, String ttype, String txndate) {
        List<Map<String, Object>> findAll;
        String mainSql = "";
        if (txnType.equalsIgnoreCase("all")) {
            mainSql = "SELECT txnid,txn_type,ttype,txndate as txndate,sourceaccount,destinationaccount,amount,description,contraaccount,dr_cr_ind,branch,currency,txn_status,prevoius_balance,post_balance FROM `cbstransactiosn` WHERE ttype=? and date(txndate)=? and txn_status not like '%success%'";
            findAll = this.jdbcTemplate.queryForList(mainSql, ttype, txndate);
        } else {
            mainSql = "SELECT txnid,txn_type,ttype,txndate as txndate,sourceaccount,destinationaccount,amount,description,contraaccount,dr_cr_ind,branch,currency,txn_status,prevoius_balance,post_balance FROM `cbstransactiosn` WHERE txn_type=? and ttype=? and date(txndate)=? and  txn_status not like '%success%'";
            findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
            //LOGGER.info("RequestBody");
        } catch (JsonProcessingException ex) {
            LOGGER.info("RequestBody: ", ex);
        }
        String json = jsonString;
        return json;
    }
    //get third party  transactions in a datatable format(ajax)

    public String getThirdPartyTransactions(String txnType, String ttype, String txndate, String exceptionType) {
        List<Map<String, Object>> findAll = null;
        String mainSql = "";
        List<String> thirdPartArray = new ArrayList<>();
        List<String> cbsArray = new ArrayList<>();
        List<String> finalRerefernceArray = new ArrayList<>();
        if (ttype.equals("AIRTEL-VIKOBA")) {
//            mainSql = "select thirdparty_reference from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";

            if (exceptionType.equals("333")) {

                mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                List<Map<String, Object>> thirdPartDB = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
                for (Map<String, Object> map : thirdPartDB) {
                    thirdPartArray.add(map.get("receiptNo") + "");
                }
                LOGGER.info("thirdPartArray: {}", thirdPartArray);
                mainSql = "select thirdparty_reference from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.thirdparty_reference not in ( select c.thirdparty_reference from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                List<Map<String, Object>> cbsArrayDB = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

                for (Map<String, Object> map : cbsArrayDB) {
                    cbsArray.add(map.get("thirdparty_reference") + "");
                }
                LOGGER.info("cbsArray: {}", cbsArray);

                for (String reference : thirdPartArray) {
                    if (!cbsArray.contains(reference.trim())) {
                        finalRerefernceArray.add(reference);
                    }
                }
                LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
                String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));
                mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%' and receiptNo in (" + finalRerefernces + ")";
                LOGGER.info(mainSql.replace("?", "'{}'"), txnType, ttype, txndate);
//            if (finalRerefernces.equals("")) {
//                mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%' and txnid in (9999999999F999999XMKWJ)";
//            }
                findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
                LOGGER.info(mainSql.replace("?", "'{}'") + "; size:{}", txnType, ttype, txndate, findAll.size());

            } else {
                if (txnType.equalsIgnoreCase("all")) {
                    mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    findAll = this.jdbcTemplate.queryForList(mainSql, ttype, txndate);
                } else {
                    mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
                }
            }
            //Java objects to JSON string - compact-print - salamu - Pomoja.
            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(findAll);
                //LOGGER.info("RequestBody");
            } catch (JsonProcessingException ex) {
                LOGGER.info("RequestBody: ", ex);
            }
            String json = jsonString;
            return json;

        } else {
            if (exceptionType.equals("333")) {

                mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                List<Map<String, Object>> thirdPartDB = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
                for (Map<String, Object> map : thirdPartDB) {
                    thirdPartArray.add(map.get("txnid") + "");
                }
                LOGGER.info("thirdPartArray: {}", thirdPartArray);
                mainSql = "select txnid from  cbstransactiosn b where   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
                List<Map<String, Object>> cbsArrayDB = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

                for (Map<String, Object> map : cbsArrayDB) {
                    cbsArray.add(map.get("txnid") + "");
                }
                LOGGER.info("cbsArray: {}", cbsArray);

                for (String reference : thirdPartArray) {
                    if (!cbsArray.contains(reference.trim())) {
                        finalRerefernceArray.add(reference);
                    }
                }
                LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
                String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));
                mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%' and txnid in (" + finalRerefernces + ")";
                LOGGER.info(mainSql.replace("?", "'{}'"), txnType, ttype, txndate);
//            if (finalRerefernces.equals("")) {
//                mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%' and txnid in (9999999999F999999XMKWJ)";
//            }
                findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
                LOGGER.info(mainSql.replace("?", "'{}'") + "; size:{}", txnType, ttype, txndate, findAll.size());

            } else {
                if (txnType.equalsIgnoreCase("all")) {
                    mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    findAll = this.jdbcTemplate.queryForList(mainSql, ttype, txndate);
                } else {
                    mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%'";
                    findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
                }
            }
            //Java objects to JSON string - compact-print - salamu - Pomoja.
            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(findAll);
                //LOGGER.info("RequestBody");
            } catch (JsonProcessingException ex) {
                LOGGER.info("RequestBody: ", ex);
            }
            String json = jsonString;
            return json;

        }

    }
//get third party failed transactions

    public String getThirdPartyFailedTransactions(String txnType, String ttype, String txndate) {
        List<Map<String, Object>> findAll;
        String mainSql = "";
        if (txnType.equalsIgnoreCase("all")) {
            mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status not like '%success%'";
            findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
        } else {
            mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status not like '%success%'";
            findAll = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate);
        }
        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
            //LOGGER.info("RequestBody");
        } catch (JsonProcessingException ex) {
            LOGGER.info("RequestBody: ", ex);
        }
        String json = jsonString;
        return json;
    }

    //get not in CBS transactions BUT FOUND ON thirdparty
    public String getNotInCbsTransactions(String txnType, String ttype, String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder, String exceptionType) {
        String json = null;
        List<Map<String, Object>> results = null;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String mainSql = null;
        String searchQuery = null;
        LOGGER.info("ttype:{}, exceptionType:{}", txnType, exceptionType);
        if (txnType.equals("WALLET2MKOBA") && exceptionType.equals("0")) {
            mainSql = "SELECT COUNT(txnid) FROM thirdpartytxns WHERE txdestinationaccount like '1112%' and  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND  txdestinationaccount like '1112%' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txdestinationaccount like '1112%' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (txnType.equals("WALLET2MKOBA") && exceptionType.equals("1")) {
            mainSql = "SELECT COUNT(txnid) FROM thirdpartytxns WHERE txdestinationaccount like '1732%' and  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND  txdestinationaccount like '1732%' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid, CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txdestinationaccount like '1732%' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (txnType.equals("WALLET2MKOBA") && exceptionType.equals("2")) {
            mainSql = "SELECT COUNT(txnid) FROM thirdpartytxns WHERE txdestinationaccount ='' and  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND  txdestinationaccount = '' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txdestinationaccount = '' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (txnType.equals("WALLET2MKOBA") && exceptionType.equals("3")) {
            mainSql = "SELECT COUNT(txnid) FROM thirdpartytxns WHERE   txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (txnType.equals("WALLET2VIKOBA") && exceptionType.equals("1")) {
            //to be retried to cbs
            mainSql = "SELECT COUNT(receiptNo) FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (txnType.equals("WALLET2VIKOBA") && exceptionType.equals("3")) {
            mainSql = "SELECT COUNT(receiptNo) FROM thirdpartytxns WHERE   txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (((txnType.equals("WALLET2VIKOBA")) || (txnType.equals("VIKOBA2WALLET"))) && exceptionType.equals("99")) {
            mainSql = "SELECT COUNT(receiptNo) FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        }
        //vikoba after cbs migration
        else if (txnType.equals("AWALLET2VIKOBA") && exceptionType.equals("1")) {
            //to be retried to cbs
            mainSql = "SELECT COUNT(receiptNo) FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (txnType.equals("AWALLET2VIKOBA") && exceptionType.equals("3")) {
            //confirm date
            mainSql = "SELECT COUNT(receiptNo) FROM thirdpartytxns WHERE   txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        } else if (((txnType.equals("AWALLET2VIKOBA")) || (txnType.equals("AVIKOBA2WALLET"))) && exceptionType.equals("99")) {
            //not selected exception type
            mainSql = "SELECT COUNT(receiptNo) FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(receiptNo) not in (SELECT trim(thirdparty_reference) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59");
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        }
        //end vikoba after cbs migration
        else {
            mainSql = "SELECT COUNT(txnid) FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";

            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

                mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
            }

            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        }

        return json;
    }


    public String getNotInThirdPartyTransactions(String txnType, String ttype, String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        if (ttype.equalsIgnoreCase("AIRTEL-VIKOBA")) {
            mainSql = "select count(b.thirdparty_reference)\n"
                    + "from cbstransactiosn b  where trim(b.thirdparty_reference) not in (select trim(a.receiptNo) FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? )\n"
                    + "and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                    + " trim(b.thirdparty_reference) not in ( select trim(c.thirdparty_reference) from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND trim(b.thirdparty_reference) not in (select trim(a.receiptNo) FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and trim(b.thirdparty_reference) not in ( select trim(c.thirdparty_reference) from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.thirdparty_reference) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype, CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

            } else {
                mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype,CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from  cbstransactiosn b where trim(b.thirdparty_reference) not in (select trim(a.receiptNo) FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and trim(b.thirdparty_reference) not in ( select trim(c.thirdparty_reference) from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
            return json;
        } else {
            mainSql = "select count(b.txnid)\n"
                    + "from cbstransactiosn b  where trim(b.txnid) not in (select trim(a.txnid) FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? )\n"
                    + "and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and \n"
                    + " trim(b.txnid) not in ( select trim(c.txnid) from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND trim(b.txnid) not in (select trim(a.txnid) FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and trim(b.txnid) not in ( select trim(c.txnid) from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype, CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

            } else {
                mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype,CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from  cbstransactiosn b where trim(b.txnid) not in (select trim(a.txnid) FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and trim(b.txnid) not in ( select trim(c.txnid) from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txn_status not like '%success%') and txn_type=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
            return json;
        }
        //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);

    }

    public String getLukuExceptionsNotInSettlement(String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        mainSql = "select count(b.txnid) from suspe_cbstransactiosn b  where txn_status like '%success%' and txn_type='LUKUEX' and  b.txndate >= ? AND b.txndate <= ? and txnid not in (select t.txnid  from thirdpartytxns t where txn_type='LUKU' )";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND  txn_status like '%success%' and b.txndate >= ? AND b.txn_type='LUKUEX' and b.txndate <= ? and txnid not in (select t.txnid  from thirdpartytxns t where txn_type='LUKU' ) ";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM suspe_cbstransactiosn b " + searchQuery, new Object[]{searchValue, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype, CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from suspe_cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, txndate + " 00:00:00", txndate + " 23:59:59"});

        } else {
            mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype,CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from  suspe_cbstransactiosn b where   txn_status like '%success%' and  b.txndate >= ? AND b.txn_type='LUKUEX' and b.txndate <= ? and txnid not in (select t.txnid  from thirdpartytxns t where txn_type='LUKU' ) ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            LOGGER.info(mainSql.replace("?", "'{}'"), txndate + " 00:00:00", txndate + " 23:59:59");
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txndate + " 00:00:00", txndate + " 23:59:59"});
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String getGepgExceptionsNotInSettlement(String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        mainSql = "select count(b.txnid) from suspe_cbstransactiosn b  where txn_status like '%success%' and txn_type='GEPGEX' and  b.txndate >= ? AND b.txndate <= ?";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND  txn_status like '%success%' and b.txndate >= ? AND b.txn_type='GEPGEX' and b.txndate <= ?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM suspe_cbstransactiosn b " + searchQuery, new Object[]{searchValue, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype, CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from suspe_cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, txndate + " 00:00:00", txndate + " 23:59:59"});

        } else {
            mainSql = "select  id, txnid, thirdparty_reference, txn_type, ttype,CAST(txndate AS char) as txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch, docode, docode_desc from  suspe_cbstransactiosn b where   txn_status like '%success%' and  b.txndate >= ? AND b.txn_type='GEPGEX' and b.txndate <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            LOGGER.info(mainSql.replace("?", "'{}'"), txndate + " 00:00:00", txndate + " 23:59:59");
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txndate + " 00:00:00", txndate + " 23:59:59"});
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String getCbsSuccessThirdPartyFailed(String txnType, String ttype, String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        if (ttype.equalsIgnoreCase("AIRTEL-VIKOBA")) {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from (SELECT * FROM cbstransactiosn WHERE  ttype=? AND txndate>=? and txndate<=? AND txn_status like '%success%' and thirdparty_reference  in (SELECT receiptNo FROM thirdpartytxns WHERE ttype=? and txndate>=?  and txndate<=?  AND mnoTxns_status NOT LIKE '%success%')) combined where combined.thirdparty_reference not in (SELECT thirdparty_reference FROM cbstransactiosn WHERE  ttype=? AND txndate>=?  AND txn_status NOT LIKE '%SUCCESS%') and txn_type=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = "WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND ttype=? AND txndate>=? and txndate<=? AND txn_status like '%success%' and thirdparty_reference  in (SELECT receiptNo FROM thirdpartytxns WHERE ttype=? and txndate>=?  and txndate<=?  AND mnoTxns_status NOT LIKE '%success%')) combined where combined.thirdparty_reference not in (SELECT thirdparty_reference FROM cbstransactiosn WHERE  ttype=? AND txndate>=?  AND txn_status NOT LIKE '%SUCCESS%') and txn_type=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from cbstransactiosn " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

            } else {
                mainSql = "select * from (SELECT * FROM cbstransactiosn WHERE  ttype=? AND txndate>=? and txndate<=? AND txn_status like '%success%' and thirdparty_reference  in (SELECT receiptNo FROM thirdpartytxns WHERE ttype=? and txndate>=?  and txndate<=?  AND mnoTxns_status NOT LIKE '%success%')) combined where combined.thirdparty_reference not in (SELECT thirdparty_reference FROM cbstransactiosn WHERE  ttype=? AND txndate>=?  AND txn_status NOT LIKE '%SUCCESS%') and txn_type=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
            return json;
        } else {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from (SELECT * FROM cbstransactiosn WHERE  ttype=? AND txndate>=? and txndate<=? AND txn_status like '%success%' and txnid  in (SELECT txnid FROM thirdpartytxns WHERE ttype=? and txndate>=?  and txndate<=?  AND mnoTxns_status NOT LIKE '%success%')) combined where combined.txnid not in (SELECT txnid FROM cbstransactiosn WHERE  ttype=? AND txndate>=?  AND txn_status NOT LIKE '%SUCCESS%') and txn_type=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = "WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND ttype=? AND txndate>=? and txndate<=? AND txn_status like '%success%' and txnid  in (SELECT txnid FROM thirdpartytxns WHERE ttype=? and txndate>=?  and txndate<=?  AND mnoTxns_status NOT LIKE '%success%')) combined where combined.txnid not in (SELECT txnid FROM cbstransactiosn WHERE  ttype=? AND txndate>=?  AND txn_status NOT LIKE '%SUCCESS%') and txn_type=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from cbstransactiosn " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});

            } else {
                mainSql = "select * from (SELECT * FROM cbstransactiosn WHERE  ttype=? AND txndate>=? and txndate<=? AND txn_status like '%success%' and txnid  in (SELECT txnid FROM thirdpartytxns WHERE ttype=? and txndate>=?  and txndate<=?  AND mnoTxns_status NOT LIKE '%success%')) combined where combined.txnid not in (SELECT txnid FROM cbstransactiosn WHERE  ttype=? AND txndate>=?  AND txn_status NOT LIKE '%SUCCESS%') and txn_type=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txnType});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            String jsonString = null;
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
            return json;
        }

    }

    /*
     * get thirdparty success and cbs failed
     */
//    public String getThirdPartySuccessCBSFailed(String txnType, String ttype, String txndate) {
//        List<Map<String, Object>> findAll;
//        String mainSql = "SELECT * FROM thirdpartytxns WHERE ttype=? AND txn_type=? AND txndate>=?  and txndate<=? and mnoTxns_status like '%success%' and txnid  in (SELECT txnid FROM cbstransactiosn WHERE   ttype=? and txn_type=? and txndate>=? and txndate<=? AND txn_status NOT like '%success%')";
//        //"SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and date(txndate)=? and mnoTxns_status  like '%success%' and txnid not in (SELECT txnid from cbstransactiosn where txn_type=? and ttype=? and date(txndate)=?)";
//        findAll = this.jdbcTemplate.queryForList(mainSql, ttype, txnType, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txnType, txndate + " 00:00:00", txndate + " 23:59:59");
//        String jsonString = null;
//        try {
//            jsonString = this.jacksonMapper.writeValueAsString(findAll);
//        } catch (JsonProcessingException ex) {
//            LOGGER.info("RequestBody: ", ex);
//        }
//        String json = jsonString;
//        return json;
//    }
    //get getThirdPartySuccessCBSFailed
    public String getThirdPartySuccessCBSFailed(String txnType, String ttype, String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql = "SELECT COUNT(txnid) FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and txnid  in (SELECT txnid from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=? and txn_status NOT like '%success%')";
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        String searchQuery = "";
//        System.out.println("HERE MEN");
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',mnoTxns_status) LIKE ? AND txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and txnid  in (SELECT txnid from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=? and txn_status not like '%success%')";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }

        if (!searchQuery.equals("")) {
            mainSql = "select * from thirdpartytxns " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});

        } else {
            mainSql = "SELECT txn_type,ttype,txnid,txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and txnid  in (SELECT txnid from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=? and txn_status not like '%success%') ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
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

    //get transactions for confirmation
    public List<Map<String, Object>> getTransactionsForConfirmation(String txnids, String txndate, String ttype) {
        return this.jdbcTemplate.queryForList("select * from cbstransactiosn where txnid in (" + txnids + ") and Date(txndate)=? and ttype=?", txndate, ttype);
    }
//initializing the reconciliation process

    public CompletableFuture<Integer> initiateReconciliation(String ttype, String description, String cbstxnsCount, String cbstxnsVolume, String thirdPartytxnsCount, String thirdPartytxnsVolume, String difftxnCount, Double difftxnVolume, String first_status, String second_status, String initiated_by, String recondt, String cbsCharge, String thirpatyCharge, Double diffCharge) {
        Integer result = 0;
        try {
            result = jdbcTemplate.update("INSERT  INTO recon_tracker(txn_type,description, cbstxnsCount, cbstxnsVolume, thirdPartytxnsCount, thirdPartytxnsVolume, difftxnCount, difftxnVolume, first_status, second_status, initiated_by, recondt,cbsCharge,thirdpartyCharge,diffchargeVolume) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE  cbstxnsCount=?, cbstxnsVolume=?, thirdPartytxnsCount=?, thirdPartytxnsVolume=?, difftxnCount=?, difftxnVolume=?,first_status=?, second_status=?, initiated_by=?,cbsCharge=?,thirdpartyCharge=?,diffchargeVolume=?",
                    ttype, description, cbstxnsCount, cbstxnsVolume, thirdPartytxnsCount, thirdPartytxnsVolume, difftxnCount, difftxnVolume, first_status, second_status, initiated_by, recondt, cbsCharge, thirpatyCharge, diffCharge, cbstxnsCount, cbstxnsVolume, thirdPartytxnsCount, thirdPartytxnsVolume, difftxnCount, difftxnVolume, first_status, second_status, initiated_by, cbsCharge, thirpatyCharge, diffCharge);
        } catch (DataAccessException e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            result = 0;
        }
        return CompletableFuture.completedFuture(result);
    }

    //    public int[] updateBulkAmbiguousTxns(String query) {
//        int[] result = null;
//        try {
//            result = jdbcTemplate.batchUpdate(query);
//        } catch (DataAccessException e) {
//            e.printStackTrace();
//            LOGGER.info(e.getMessage());
//        }
//        return result;
//    }
    public int[][] updateBulkAmbiguousTxns(String qry, List<Map<String, Object>> dataObject) {

        int[][] batch = jdbcTemplate.batchUpdate(qry, dataObject, dataObject.size(), (PreparedStatement ps, Map<String, Object> bulkTxns) -> {
            ps.setString(1, bulkTxns.get("txndate").toString());
            ps.setString(2, bulkTxns.get("txnid").toString());
            LOGGER.info(qry + " {} {}", bulkTxns.get("txndate").toString(), bulkTxns.get("txnid").toString());
            ps.executeUpdate();
        });

        return batch;
    }

    //    public static Map<String, Object>[] getArrayData(List<Map<String, Object>> list) {
//        @SuppressWarnings("unchecked")
//        Map<String, Object>[] maps = new HashMap[list.size()];
//
//        Iterator<Map<String, Object>> iterator = list.iterator();
//        int i = 0;
//        while (iterator.hasNext()) {
//            Map<java.lang.String, java.lang.Object> map = (Map<java.lang.String, java.lang.Object>) iterator
//                    .next();
//            maps[i++] = map;
//        }
//
//        return maps;
//    }
    public CompletableFuture<Integer> saveReconciliationException(String ttype, String exceptionName, String exceptionTxnsCount, String exceptionTxnsVolume, String recon_dt, String initiated_by) {
        Integer result = 0;
        try {
            result = jdbcTemplate.update("INSERT  INTO recon_exception_tracker(txn_type,exception_name,exceptionTxnsCount,exceptionTxnsVolume,recon_dt,initiated_by) VALUES (?,?,?,?,?,?)  ON DUPLICATE KEY UPDATE  exceptionTxnsCount=?, exceptionTxnsVolume=?",
                    ttype, exceptionName, exceptionTxnsCount, exceptionTxnsVolume, recon_dt, initiated_by, exceptionTxnsCount, exceptionTxnsVolume);
        } catch (DataAccessException e) {
            LOGGER.info(e.getMessage());
            result = 0;
        }
        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<Integer> saveInitiatedRetryRefundTxns(String supportingDoc, String makerID, String txnid, String status, String reason, String ttype, String txn_type) {

        Integer result = 0;
        int resultss = 0;
        try {
            if (ttype.equalsIgnoreCase("MKOBA")) {
                //this hard coded was result of lack of support form melleji and his team.
                List<Map<String, Object>> getTxnTypeMK = this.jdbcMKOBATemplate.queryForList("select * from vg_group_transaction where transid =? limit 1", txnid);
                List<String> MKOBA2WALLET = Arrays.asList(StringUtils.splitPreserveAllTokens("122001,122002,115,12613,12614,122003,122004,126111,126112,126113,126114,12612", ","));
                List<String> WALLET2MKOBA = Arrays.asList(StringUtils.splitPreserveAllTokens("123,124,,1233,122,1222,101,102,111,121,1111,1211,12932,222,333,444,555,666,777,4444,4441,2222,2221,3333,3331,5555,5551,6661,6666,7777,7771", ","));
                if (!getTxnTypeMK.isEmpty()) {
                    String transStatus = getTxnTypeMK.get(0).get("transstatus") + "";
                    if (MKOBA2WALLET.contains(getTxnTypeMK.get(0).get("transtype") + "")) {
                        txn_type = "MKOBA2WALLET";
                    } else if (WALLET2MKOBA.contains(getTxnTypeMK.get(0).get("transtype") + "")) {
                        txn_type = "WALLET2MKOBA";
                    }
                    LOGGER.info("Transaction found in vg and txn_type:{} - {}", txn_type, getTxnTypeMK.get(0).get("transtype"));
                    List<Map<String, Object>> checkCBSTrans = this.jdbcTemplate.queryForList("select * from cbstransactiosn where  txnid =?", txnid);

                    if (txn_type.equalsIgnoreCase("WALLET2MKOBA")) {
                        if (!checkCBSTrans.isEmpty()) {
                            LOGGER.info("Transction is found cbs database, can not be initied for retry  txn_type  found:{} - {}, id:{}", txn_type, getTxnTypeMK.get(0).get("transtype"), txnid);
                        } else {
//                            result = jdbcMKOBATemplate.update("INSERT IGNORE  INTO tp_cbs_retry  select transid as sessionid,msisdn,'-1' as imsi, msisdn as account, destinationaccount as toaccount,\n"
//                                    + "                    200 as msgid,transamount as amount,570000 as processcode,(CASE WHEN destinationaccount = 173205000002  THEN 'MKBCHG'\n"
//                                    + "                        ELSE 'PAMOJAMPESA'\n"
//                                    + "                    END) as trans_type,\n"
//                                    + "                    '' as pin, '' as new_pin,'-1' as responsecode,0 as count,'-1' status,receipt,'0' as vgroup,\n"
//                                    + "                    '' AS reference,1 as lang_code,transate as transaction_date from vg_group_transaction where transid ='" + txnid + "'");
                            result = jdbcMKOBATemplate.update("INSERT IGNORE  INTO tp_cbs_retry  select transid as sessionid,msisdn,'-1' as imsi, msisdn as account, (CASE WHEN destinationaccount = '173205000002' THEN '173205000002' ELSE groupid END)  as toaccount,\n"
                                    + "                    200 as msgid,transamount as amount,570000 as processcode,transtype as trans_type,\n"
                                    + "                    '' as pin, '' as new_pin,'-1' as responsecode,0 as count,'-1' status,receipt,'0' as vgroup,\n"
                                    + "                    '' AS reference,1 as lang_code,transate as transaction_date from vg_group_transaction where transid ='" + txnid + "'");

                            result = jdbcMKOBATemplate.update("INSERT IGNORE  INTO tp_cbs_retry  select transid as sessionid,msisdn,'-1' as imsi, msisdn as account, (CASE WHEN destinationaccount = '173205000002' THEN '173205000002' ELSE groupid END)  as toaccount,\n"
                                    + "                    200 as msgid,transamount as amount,570000 as processcode,transtype as trans_type,\n"
                                    + "                    '' as pin, '' as new_pin,'-1' as responsecode,0 as count,'-1' status,receipt,'0' as vgroup,\n"
                                    + "                    '' AS reference,1 as lang_code,transate as transaction_date from vg_group_transaction_archive where transid ='" + txnid + "'");

                            result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                    supportingDoc, getTxnTypeMK.get(0).get("memberid"), txn_type, makerID, getTxnTypeMK.get(0).get("transid").toString(), status, reason);
                            LOGGER.info("Result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                    supportingDoc, getTxnTypeMK.get(0).get("memberid"), txn_type, makerID, getTxnTypeMK.get(0).get("transid").toString(), status, reason);
                        }
                    } else if (txn_type.equalsIgnoreCase("MKOBA2WALLET")) {
                        if (transStatus.equals("0")) {
                            LOGGER.info("Transid: {} can be not reserved it is either successfully or already reversed:{} - {} =status->{}", getTxnTypeMK.get(0).get("transid"), txn_type, getTxnTypeMK.get(0).get("transtype"), transStatus);

                        } else {
                            if (!checkCBSTrans.isEmpty()) {
                                String sourceaccount = getTxnTypeMK.get(0).get("sourceaccount") + "";
                                result = jdbcTemplate.update("update cbstransactiosn SET sourceAccount=? where txnid=?", sourceaccount, txnid);
                            }
                            result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                    supportingDoc, getTxnTypeMK.get(0).get("memberid"), txn_type, makerID, getTxnTypeMK.get(0).get("transid").toString(), status, reason);
                            LOGGER.info("Result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                    supportingDoc, getTxnTypeMK.get(0).get("memberid"), txn_type, makerID, getTxnTypeMK.get(0).get("transid").toString(), status, reason);
                        }
                    } else {
                        LOGGER.info("txn_type not found:{} - {}", txn_type, getTxnTypeMK.get(0).get("transtype"));

                    }
                } else {
                    List<Map<String, Object>> cbsTrans = this.jdbcTemplate.queryForList("select * from cbstransactiosn where  txnid =?", txnid);
                    String query = "-1";
                    if (!cbsTrans.isEmpty()) {
                        if ("1-060-00-1102-1102114".contains(cbsTrans.get(0).get("contraaccount") + "")) {
                            txn_type = "MKOBA2WALLET";
                            query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
                                    + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO,\n"
                                    + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
                                    + "gah.STMNT_BAL postBalance\n"
                                    + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.CONTRA_ACCT_NO in ('1-060-00-1102-1102114')  and gah.tran_ref_txt in ('" + txnid + "')";

                        } else if ("1-060-00-1102-1102113".contains(cbsTrans.get(0).get("contraaccount") + "")) {
                            txn_type = "WALLET2MKOBA";
                            query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
                                    + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO,\n"
                                    + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
                                    + "gah.STMNT_BAL postBalance\n"
                                    + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.CONTRA_ACCT_NO in ('1-060-00-1102-1102113') and gah.tran_ref_txt in ('" + txnid + "')";

                        }
                        String dest = "-1";

                        if (cbsTrans.get(0).get("sourceaccount").equals("-1") || cbsTrans.get(0).get("sourceaccount").equals("") || cbsTrans.get(0).get("sourceaccount") == null || cbsTrans.get(0).get("sourceaccount").equals(txnid)) {
                            List<Map<String, Object>> rubikonTransaction = this.jdbcRUBIKONTemplate.queryForList(query);
                            dest = rubikonTransaction.get(0).get("description") + "";
                            String[] destArray = dest.split(":");
                            LOGGER.info("Desc-{}, array size:", dest, destArray.length);
                            if (destArray.length >= 3) {
                                dest = destArray[2];
                            }
                            LOGGER.info("Desc-{}, array size:{}, final:->{}", dest, destArray.length, dest);
                            if (!rubikonTransaction.isEmpty()) {
                                result = jdbcTemplate.update("update cbstransactiosn SET sourceaccount=?, destinationaccount=?,terminal=?,thirdparty_reference=? where txnid=?", rubikonTransaction.get(0).get("ACCT_NO"), rubikonTransaction.get(0).get("description"), dest, txnid, txnid);
                            }
                        }
                        List<Map<String, Object>> checkMNOTrans = this.jdbcTemplate.queryForList("select * from thirdpartytxns where mnoTxns_status like '%success%' and txnid<>receiptNo and  txnid =?", txnid);
                        if (!checkMNOTrans.isEmpty()) {
                            LOGGER.info("Transction is found thirdpartytxns database, can not be initied for refund  txn_type  found:{}, id:{}", txn_type, txnid);
                        } else {
                            if (txn_type.equalsIgnoreCase("MKOBA2WALLET")) {
                                result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                        supportingDoc, dest, txn_type, makerID, cbsTrans.get(0).get("txnid").toString(), status, reason);
                                LOGGER.info("result: {}, INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'".replace("?", "'{}'"),
                                        result, supportingDoc, dest, txn_type, makerID, cbsTrans.get(0).get("txnid").toString(), status, reason);
                            }
                        }
                    } else {
                        resultss = 512;
                        LOGGER.info("Transaction was not found in cbstransaction table");
                    }

                }
            } else if (ttype.equalsIgnoreCase("AIRTEL-VIKOBA")) {
                if (txn_type.equalsIgnoreCase("WALLET2VIKOBA")) {
//                    List<Map<String, Object>> checkTxn = this.jdbcAirtelVikobaTemplate.queryForList("select * from vg_group_transaction WHERE transid =?", txnid);
                    List<Map<String, Object>> checkTxn = this.jdbcTemplate.queryForList("select * from cbstransactiosn where txnid =?", txnid);

                    if (checkTxn.isEmpty()) {
                        result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                supportingDoc, txnid, txn_type, makerID, txnid, status, reason);

                        LOGGER.info("result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'".replace("?", "'{}'"),
                                supportingDoc, txnid, txn_type, makerID, txnid, status, reason);
                    } else {
                        LOGGER.info("Transction is found at cbstransactiosn {},  id:{} hence can not be retried", ttype, txnid);

                    }
                } else {
                    List<Map<String, Object>> checkTxn = this.jdbcTemplate.queryForList("select * from cbstransactiosn where txnid =? and thirdparty_reference ='-1' and txn_status not like '%success%'", txnid);

                    if (checkTxn.isEmpty()) {
                        result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                supportingDoc, txnid, txn_type, makerID, txnid, status, reason);

                        LOGGER.info("result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'".replace("?", "'{}'"),
                                supportingDoc, txnid, txn_type, makerID, txnid, status, reason);
                    } else {
                        LOGGER.info("VIKOBA2WALLET Transction is found at cbstransactiosn and it is success {},  id:{} hence can not be retried", ttype, txnid);

                    }
                }

            } else if (ttype.equalsIgnoreCase("LUKUEX")) {
                List<Map<String, Object>> checkMNOTrans = this.jdbcTemplate.queryForList("select * from thirdpartytxns where txn_type = 'LUKU' and  txnid =?", txnid);
                LOGGER.info("checkMNOTrans Transction is found thirdpartytxns database, {}", checkMNOTrans);

                if (!checkMNOTrans.isEmpty()) {
                    LOGGER.info("Transction is found thirdpartytxns database, can not be initied for refund  txn_type  found:{}, id:{}", ttype, txnid);
                } else {
                    //List<Map<String, Object>> cbsTrans = this.jdbcTemplate.queryForList("select * from suspe_cbstransactiosn where txn_type = 'LUKUEX' and txnid =?", txnid);

                    result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                            supportingDoc, txnid, "LUKUEX", makerID, txnid, status, reason);

                    LOGGER.info("result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'".replace("?", "'{}'"),
                            supportingDoc, txnid, "LUKUEX", makerID, txnid, status, reason);

                }
            } else if (ttype.equalsIgnoreCase("UTILITY") && txn_type.equalsIgnoreCase("LUKU")) {
                List<Map<String, Object>> checkMNOTrans = this.jdbcTemplate.queryForList("select * from thirdpartytxns where txn_type = 'LUKU' and  txnid =?", txnid);
                List<Map<String, Object>> cbsTrans = this.jdbcTemplate.queryForList("select * from suspe_cbstransactiosn where txn_type = 'LUKUEX' and txnid =?", txnid);

                if (!checkMNOTrans.isEmpty()) {

                    result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                            supportingDoc, txnid, "LUKU", makerID, txnid, status, reason);

                    LOGGER.info("result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'".replace("?", "'{}'"),
                            supportingDoc, txnid, "LUKU", makerID, txnid, status, reason);
                } else {
                    LOGGER.info("Transction is empty at thirdpartytxns or suspe_cbstransactiosn found:{}, id:{}", ttype, txnid);

                }
            } else {
                //select the transaction from gateway and check get the ttype:
                List<Map<String, Object>> getTxnTypeGW = this.jdbcGWTemplate.queryForList("select * from tp_transaction where txid = '" + txnid + "' order by txdate desc limit 1");
                String txnIdentifier = "NA";
                LOGGER.info("select * from tp_transaction where txid like '" + txnid + "' order by txdate desc limit 1; {}", getTxnTypeGW);
                if (!getTxnTypeGW.isEmpty()) {
                    txnIdentifier = getTxnTypeGW.get(0).get("txtype").toString();
                } else {
                    LOGGER.info("Transaction not found in tp_transaction, I don't think you can retry it. {} I could help you BUT I'm not developer thread", txnid);

                    List<Map<String, Object>> cbsTrans = this.jdbcTemplate.queryForList("select * from cbstransactiosn where  txnid =?", txnid);
                    if (cbsTrans.get(0).get("ttype").equals("B2C")) {
                        result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                                supportingDoc, cbsTrans.get(0).get("sourceaccount"), "IB001", makerID, txnid, status, reason);
                    }
                }

                List<Map<String, Object>> getTtypeFromConfig = this.jdbcTemplate.queryForList("select * from txns_types where gw_docodes like '%" + txnIdentifier + "%'");
                LOGGER.info("select * from txns_types where gw_docodes like '%" + txnIdentifier + "%'; {}", getTtypeFromConfig);

                if (!getTtypeFromConfig.isEmpty()) {
                    //txn_type = getTtypeFromConfig.get(0).get("code").toString();
                    ttype = getTtypeFromConfig.get(0).get("ttype").toString();
//                  //check if the transactions is not in thirdpartytxns if not insert......
                    LOGGER.info("ttype:{}, code:{}", ttype, getTtypeFromConfig.get(0).get("code").toString());
                    //log the transaction on retry
                    //List<Map<String, Object>> gwRecord = getMsisdnGW(txnid);
                    result = jdbcTemplate.update("INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'",
                            supportingDoc, getTxnTypeGW.get(0).get("msisdn"), getTxnTypeGW.get(0).get("txtype"), makerID, getTxnTypeGW.get(0).get("txid").toString(), status, reason);

                    LOGGER.info("result:" + result + " INSERT  INTO retry (supportingDoc,msisdn,docode,maker_id,txnid,status,maker_comments) VALUES (?,?,?,?,?,?,?) on duplicate key update status='initiated'".replace("?", "'{}'"),
                            supportingDoc, getTxnTypeGW.get(0).get("msisdn"), getTxnTypeGW.get(0).get("txtype"), makerID, getTxnTypeGW.get(0).get("txid").toString(), status, reason);

                    if (ttype.equalsIgnoreCase("B2C") || ttype.equalsIgnoreCase("UTILITY")) {
                        result = jdbcGWTemplate.update("INSERT ignore  INTO `tp_b2c_refund` SELECT txid as transactionID ,msisdn,1 AS language, txsourceAccount AS sourceAccount, txdestinationAccount AS destAccount,txamount AS denomination,txid AS receipt,txtype AS trans_type ,-1 AS cbs_response, '-1' AS status, 0 AS processed,txdestinationType As description,txdate AS txdate,0 AS count,txdestinationName AS destination, null AS sec,-1 AS imsi, -1 AS lang_code, txid AS originatingId FROM tp_transaction WHERE txid =? ", txnid);
                        result = jdbcGWTemplate.update("update tp_b2c_refund set count=0 where  transactionID=?", txnid);
                    }
                    if (ttype.equalsIgnoreCase("C2B")) {
                        resultss = jdbcTemplate.update("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name,terminal) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update receiptNo=?",
                                getTtypeFromConfig.get(0).get("code").toString(), ttype, getTxnTypeGW.get(0).get("txid").toString(), getTxnTypeGW.get(0).get("txdate").toString(), getTxnTypeGW.get(0).get("txsourceAccount").toString(), getTxnTypeGW.get(0).get("txReceipt").toString(), getTxnTypeGW.get(0).get("txamount").toString(), "0", getTxnTypeGW.get(0).get("txstatusdesc").toString(), "TZS", getTxnTypeGW.get(0).get("txstatusdesc").toString(), getTxnTypeGW.get(0).get("txdestinationAccount").toString(), getTxnTypeGW.get(0).get("txdestinationAccount").toString(), status, "0.0", "0.0", "NULL", getTxnTypeGW.get(0).get("txdestinationAccount").toString(), getTxnTypeGW.get(0).get("txReceipt").toString());
                        LOGGER.info("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name,terminal) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update receiptNo=?".replace("?", "'{}'"),
                                getTtypeFromConfig.get(0).get("code").toString(), ttype, getTxnTypeGW.get(0).get("txid").toString(), getTxnTypeGW.get(0).get("txdate").toString(), getTxnTypeGW.get(0).get("txsourceAccount").toString(), getTxnTypeGW.get(0).get("txReceipt").toString(), getTxnTypeGW.get(0).get("txamount").toString(), "0", getTxnTypeGW.get(0).get("txstatusdesc").toString(), "TZS", getTxnTypeGW.get(0).get("txstatusdesc").toString(), getTxnTypeGW.get(0).get("txdestinationAccount").toString(), getTxnTypeGW.get(0).get("txdestinationAccount").toString(), status, "0.0", "0.0", "NULL", getTxnTypeGW.get(0).get("txdestinationAccount").toString(), getTxnTypeGW.get(0).get("txReceipt").toString());

                        result = jdbcGWTemplate.update("insert ignore  into tp_cbs_retry (sessionid,msisdn,imsi,toaccount,account,amount,trans_type,processcode,msgid,responsecode,receipt,log_date,reference,lang_code,count,status)select txid,msisdn,'',txdestinationAccount,txsourceAccount,txamount,txtype,570000,200,96,txReceipt,txdate,txdestinationAccount,1,0,0 from tp_transaction where txid=?", txnid);
                        result = jdbcGWTemplate.update("update tp_cbs_retry set count=0 where  sessionid=?", txnid);
                    }

                } else {
                    result = 96;
                    LOGGER.info("select * from txns_types where gw_docodes like '%" + txnIdentifier + "%'");
                }
            }

        } catch (DataAccessException | TransactionException e) {
            LOGGER.info("EXCEPTION DURING RETRY REFUND INITIATION: {} " + e);
            result = -1;
        }
        return CompletableFuture.completedFuture(result);
    }

    public String getInitiatedRefundRetryTxnsOnQueue(String txnType, String ttype, String txndate, boolean isB2W, boolean isW2B, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        if (ttype.equalsIgnoreCase("B2C") || (ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA")) && isB2W)) {
            LOGGER.info("ttype.equalsIgnoreCase(\"B2C\") || ttype.equalsIgnoreCase(\"UTILITY\") || (ttype.equalsIgnoreCase(\"MKOBA\") && isB2W)");
            LOGGER.info("select count(*)  from cbstransactiosn c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype ='{}' and c.txndate >='{}' and c.txndate <='{}' and txn_type='{}'", ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType);
            totalRecords = jdbcTemplate.queryForObject("select count(*)  from cbstransactiosn c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and txn_type=?", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
        } else if (ttype.equalsIgnoreCase("UTILITY") && !txnType.contains("LUKU") && isB2W) {
            LOGGER.info("ttype.equalsIgnoreCase(\"UTILITY\") && !txnType.contains(\"LUKU\")");
            LOGGER.info("select count(*)  from cbstransactiosn c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype ='{}' and c.txndate >='{}' and c.txndate <='{}' and txn_type='{}'", ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType);
            totalRecords = jdbcTemplate.queryForObject("select count(*)  from cbstransactiosn c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and txn_type=?", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
        } else if ((ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA")) && isW2B) || ttype.equalsIgnoreCase("C2B")) {
            LOGGER.info("(ttype.equalsIgnoreCase(\"MKOBA\") && isW2B) || ttype.equalsIgnoreCase(\"C2B\")");
            totalRecords = jdbcTemplate.queryForObject("select count(*)  from thirdpartytxns c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and txn_type=?", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
        } else if (txnType.equalsIgnoreCase("LUKU") && ttype.equalsIgnoreCase("UTILITY")) {
            LOGGER.info("txnType.equalsIgnoreCase(\"LUKU\") && ttype.equalsIgnoreCase(\"UTILITY\")");
            totalRecords = jdbcTemplate.queryForObject("select count(*)  from thirdpartytxns c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and txn_type=?", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
        } else if (txnType.equalsIgnoreCase("LUKUEX") && ttype.equalsIgnoreCase("UTILITY")) {
            totalRecords = jdbcTemplate.queryForObject("select count(*)  from suspe_cbstransactiosn c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and txn_type=?", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
        } else {
            totalRecords = jdbcTemplate.queryForObject("select count(*)  from cbstransactiosn c where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and txn_type=?", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
        }

        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            if (ttype.equalsIgnoreCase("B2C") || ((ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA"))) && isB2W)) {
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? AND ttype=? and txndate>=? and txndate<=? and txn_type=? and txnid  not in (select txnid from retry where status not like '%success%')";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
            } else if (ttype.equalsIgnoreCase("UTILITY") && !txnType.contains("LUKU") && isB2W) {
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? AND ttype=? and txndate>=? and txndate<=? and txn_type=? and txnid  not in (select txnid from retry where status not like '%success%')";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
            } else if (((ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA"))) && isW2B) || ttype.equalsIgnoreCase("C2B")) {
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? AND ttype=? and txndate>=? and txndate<=? and txn_type=? and txnid  not in (select txnid from retry where status not like '%success%')";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);

            } else if (((ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA"))) && isW2B) || ttype.equalsIgnoreCase("C2B")) {
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? AND ttype=? and txndate>=? and txndate<=? and txn_type=? and txnid  not in (select txnid from retry where status not like '%success%')";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM thirdpartytxns" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);

            } else if (txnType.equalsIgnoreCase("LUKU") && ttype.equalsIgnoreCase("UTILITY")) {
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? AND ttype=? and txndate>=? and txndate<=? and txn_type=? and txnid in (select txnid from retry where status not like '%success%')";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM suspe_cbstransactiosn " + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);

            } else {
                searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? AND ttype=? and txndate>=? and txndate<=? and txn_type=? and txnid  not in (select txnid from retry where status not like '%success%')";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn" + searchQuery, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType}, Integer.class);
            }
        } else {
            totalRecordwithFilter = totalRecords;
        }

        List<Map<String, Object>> findAll;
        String mainSql = "";//
        if ((ttype.equalsIgnoreCase("B2C") || (ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA"))) && isB2W)) {
            mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.destinationaccount destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
        } else if (ttype.equalsIgnoreCase("UTILITY") && !txnType.contains("LUKU") && isB2W) {
            mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.description destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
        } else if (((ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA"))) && isW2B) || ttype.equalsIgnoreCase("C2B")) {
            mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceAccount sourceaccount,c.txdestinationaccount destinationaccount,c.mnoTxns_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments  from thirdpartytxns c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
        } else if (txnType.equalsIgnoreCase("LUKU") && ttype.equalsIgnoreCase("UTILITY")) {
            mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceAccount sourceaccount,c.txdestinationaccount destinationaccount,c.mnoTxns_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments  from thirdpartytxns c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
        } else if (txnType.equalsIgnoreCase("LUKUEX") && ttype.equalsIgnoreCase("UTILITY")) {
            mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.destinationaccount destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from suspe_cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
        } else {
            mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.destinationaccount destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
        }

        if (!searchQuery.equals("")) {
            if ((ttype.equalsIgnoreCase("B2C") || (ttype.equalsIgnoreCase("MKOBA")) || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA")) && isB2W)) {
                mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.destinationaccount destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
                findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
            } else if (ttype.equalsIgnoreCase("UTILITY") && !txnType.contains("LUKU") && isB2W) {
                mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.description destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
                findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
            } else if (((ttype.equalsIgnoreCase("MKOBA") || (ttype.equalsIgnoreCase("AIRTEL-VIKOBA"))) && isW2B) || ttype.equalsIgnoreCase("C2B")) {
                mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceAccount sourceaccount,c.txdestinationaccount destinationaccount,c.mnoTxns_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments  from thirdpartytxns c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
                findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
            } else if (txnType.equalsIgnoreCase("LUKU") && ttype.equalsIgnoreCase("UTILITY")) {
                mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceAccount sourceaccount,c.txdestinationaccount destinationaccount,c.mnoTxns_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments  from thirdpartytxns c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
                findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
            } else if (txnType.equalsIgnoreCase("LUKUEX") && ttype.equalsIgnoreCase("UTILITY")) {
                mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.destinationaccount destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from suspe_cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
                findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
            } else {
                mainSql = "select c.txnid,c.txndate,c.txn_type,c.amount,c.sourceaccount sourceaccount,c.destinationaccount destinationaccount,c.txn_status txn_status,r.msisdn,r.docode,r.supportingDoc supportingDoc,r.maker_comments from cbstransactiosn c inner join retry r on r.txnid=c.txnid where c.txnid in (select txnid from retry r2 where r2.status not like '%success%') and c.ttype =? and c.txndate >=? and c.txndate <=? and  c.txn_type=?";
                findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
            }

        } else {
            findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType});
        }
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public List<Map<String, Object>> getReconciliationSummaryReport(String txnType, String txnDate) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT  * from recon_tracker where date(recondt)=? and txn_type=? order by id asc", txnDate, txnType);
        } catch (DataAccessException e) {
            LOGGER.error("Error occured during getting the RECON SUMMARY REPORT:{} ", e);

        }
        return result;
    }

    public List<Map<String, Object>> getReconExceptionSummaryReport(String txnType, String txnDate) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT  * from recon_exception_tracker where date(recon_dt)=? and txn_type=?", txnDate, txnType);
        } catch (DataAccessException e) {
            LOGGER.error("Error occured during getting the RECON EXCEPTION REPORT:{} ", e);

        }
        return result;
    }

    /*
     *get cbs transactions per mno on cbs
     */
    public List<Map<String, Object>> getTransactionTypes(String ttype) {
        return this.jdbcTemplate.queryForList("select * from txns_types where ttype=? AND isAllowed=1", ttype);
    }

    public List<Map<String, Object>> getCBSOpeningBalance(String code, String ttype, String txndate) {
        String sql = "SELECT IFNULL( (select COALESCE(post_balance,0) post_balance from cbstransactiosn where ttype=? AND txn_type=? and DATE(txndate)=? order by txndate DESC LIMIT 1) ,0) post_balance";
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("txn_type", code);
        map.put("txnVolume", jdbcTemplate.queryForObject(sql, new Object[]{ttype, code, txndate}, String.class));
        list.add(map);
        return list;

    }

    /*Get Opening,closing balances,and transactions volumes summary for recon date*/
    public List<Map<String, Object>> getCBSTxnsVolumeOpeningBLClosingBL(String code, String ttype, String txndate, String txndate2) {
//        String sql = "SELECT\n"
//                + "     COUNT(*) txnCount,\n"
//                + "     ifnull(sum(cbstransactiosn.amount),\n"
//                + "     0) AS txnvolume,\n"
//                + "     ifnull((SELECT\n"
//                + "         ifnull(cbstransactiosn.post_balance,0) + 0 AS closing \n"
//                + "     FROM\n"
//                + "         cbstransactiosn \n"
//                + "     WHERE\n"
//                + "             cbstransactiosn.txndate <= subdate(?, 1) \n"
//                + "       	AND cbstransactiosn.ttype =?\n"
//                + "     	AND cbstransactiosn.txn_type =?\n"
//                + "     ORDER BY\n"
//                + "         cbstransactiosn.txndate DESC LIMIT 1),\n"
//                + "     0) openingBalance,\n"
//                + "     ifnull((SELECT\n"
//                + "         cbstransactiosn.post_balance AS closing \n"
//                + "     FROM\n"
//                + "         cbstransactiosn \n"
//                + "     WHERE\n"
//                + "     cbstransactiosn.txndate <= ?\n"
//                + "     AND cbstransactiosn.ttype = ?\n"
//                + "     AND cbstransactiosn.txn_type = ?\n"
//                + "     ORDER BY\n"
//                + "         cbstransactiosn.txndate DESC LIMIT 1),\n"
//                + "     0) closingBalance \n"
//                + " FROM\n"
//                + "     cbstransactiosn \n"
//                + " WHERE\n"
//                + "     cbstransactiosn.txndate >= ? and cbstransactiosn.txndate <= ?\n"
//                + "     AND cbstransactiosn.ttype = ?\n"
//                + "     AND cbstransactiosn.txn_type =?\n"
//                + "     AND cbstransactiosn.txn_status LIKE '%success%' \n"
//                + "     and cbstransactiosn.txnid not in (select txnid from cbstransactiosn where ttype=? and txn_type =? and txndate >=? and txn_status not like '%success%')"
//                + " ORDER BY\n"
//                + "     cbstransactiosn.txndate ASC";
        String sql = "WITH transaction_data AS (\n" +
                "    SELECT\n" +
                "        txnid,\n" +
                "        amount,\n" +
                "        post_balance,\n" +
                "        txndate,\n" +
                "        ttype,\n" +
                "        txn_type,\n" +
                "        txn_status\n" +
                "    FROM\n" +
                "        cbstransactiosn\n" +
                "    WHERE\n" +
                "        txndate >= ? \n" +
                "        AND txndate <= ?\n" +
                "        AND ttype = ?\n" +
                "        AND txn_type = ?\n" +
                "        AND txn_status LIKE '%success%'\n" +
                "),\n" +
                "opening_closing_balance AS (\n" +
                "    SELECT\n" +
                "        txnid,\n" +
                "        post_balance,\n" +
                "        txndate\n" +
                "    FROM\n" +
                "        cbstransactiosn\n" +
                "    WHERE\n" +
                "        txndate <= ? \n" +
                "        AND ttype = ? \n" +
                "        AND txn_type = ? \n" +
                "    ORDER BY\n" +
                "        txndate DESC\n" +
                "    LIMIT 1\n" +
                ")\n" +
                "SELECT\n" +
                "    COUNT(*) AS txnCount,\n" +
                "    \n" +
                "    IFNULL(SUM(td.amount), 0) AS txnvolume,\n" +
                "    \n" +
                "    IFNULL(\n" +
                "        MAX(CASE \n" +
                "            WHEN td.txndate <= DATE_SUB(?, INTERVAL 1 DAY) THEN td.post_balance\n" +
                "            ELSE 0 \n" +
                "        END), \n" +
                "    0) AS openingBalance,\n" +
                "        IFNULL(MAX(CASE WHEN td.txndate <= ? THEN td.post_balance ELSE 0 END), 0) AS closingBalance\n" +
                "FROM\n" +
                "    transaction_data td\n" +
                "\n" +
                "LEFT JOIN opening_closing_balance ocb ON td.txnid = ocb.txnid\n" +
                "WHERE\n" +
                "    td.txnid NOT IN (\n" +
                "        SELECT txnid\n" +
                "        FROM cbstransactiosn\n" +
                "        WHERE\n" +
                "            ttype = ? \n" +
                "            AND txn_type = ? \n" +
                "            AND txndate >= ?\n" +
                "            AND txn_status NOT LIKE '%success%'\n" +
                "    )\n" +
                "ORDER BY\n" +
                "    td.txndate ASC;\n";

//        System.out.println("SQL QUERY: " + sql);
        LOGGER.info("=======================TRACEER  11111111111111111111111111 .==================");
//LOGGER.info(sql.replace("?","'{}'"),txndate2, ttype, code, txndate2, ttype, code, txndate, txndate2, ttype, code, ttype, code, txndate);
        // String sql = "SELECT COUNT(*) txnCount,ifnull(sum(amount),0) as txnvolume,ifnull((select post_balance +ifnull(amount,0)  as closing from cbstransactiosn where date(txndate)=subdate('"+txndate+"',1) and ttype='"+ttype+"' and txn_type='"+code+"' ORDER by txndate DESC limit 1),0)  as openingBalance,ifnull((select post_balance +ifnull(amount,0)  as closing from cbstransactiosn where date(txndate)=? and ttype=? and txn_type=? ORDER by txndate DESC limit 1),0) closingBalance from cbstransactiosn WHERE date(txndate)=? and ttype=? and txn_type=? and txn_status like '%success%' and txnid not in (select txnid from cbstransactiosn where date(txndate)=? and ttype=? and txn_type=? and txn_status not like '%success%') ORDER by txndate asc";
        List<Map<String, Object>> result = this.jdbcTemplate.queryForList(sql, txndate, txndate2, ttype, code, txndate2, ttype, code, txndate, txndate2, ttype, code, txndate);
//        List<Map<String, Object>> result = this.jdbcTemplate.queryForList(sql, txndate2, ttype, code, txndate2, ttype, code, txndate, txndate2, ttype, code, ttype, code, txndate);
        return result;//this.jdbcTemplate.queryForList(sql, txndate, ttype,txndate, ttype, code, code, txndate, ttype, code);
    }

    public List<Map<String, Object>> getThirdPartyTxnsVolumeOpeningBLClosingBL(String code, String ttype, String txndate, String txndate2) {
        String sql = "SELECT\n"
                + "        COUNT(*) txnCount,\n"
                + "        ifnull(sum(thirdpartytxns.amount),\n"
                + "        0) AS txnvolume,\n"
                + "        ifnull((SELECT\n"
                + "             ifnull(thirdpartytxns.post_balance,0) + 0 AS closing \n"
                + "        FROM\n"
                + "            thirdpartytxns \n"
                + "        WHERE\n"
                + "            thirdpartytxns.txndate <= subdate(?, 1)\n"
                + "            AND thirdpartytxns.ttype = ? \n"
                + "            AND thirdpartytxns.txn_type = ? \n"
                + "        ORDER BY\n"
                + "            thirdpartytxns.txndate DESC LIMIT 1),\n"
                + "        0) openingBalance,\n"
                + "        ifnull((SELECT\n"
                + "            thirdpartytxns.post_balance AS closing \n"
                + "        FROM\n"
                + "            thirdpartytxns \n"
                + "        WHERE\n"
                + "        thirdpartytxns.txndate <=?  \n"
                + "            AND thirdpartytxns.ttype = ? \n"
                + "            AND thirdpartytxns.txn_type = ? \n"
                + "        ORDER BY\n"
                + "            thirdpartytxns.txndate DESC LIMIT 1),\n"
                + "        0) closingBalance \n"
                + "    FROM\n"
                + "        thirdpartytxns \n"
                + "    WHERE\n"
                + "        thirdpartytxns.txndate >= ? and  thirdpartytxns.txndate <=? \n"
                + "        AND thirdpartytxns.ttype = ? \n"
                + "        AND thirdpartytxns.txn_type = ? \n"
                + "        AND thirdpartytxns.mnoTxns_status LIKE '%success%' \n"
                + "     and thirdpartytxns.txnid not in (select txnid from thirdpartytxns where ttype=? and txn_type =? and txndate >=? and mnoTxns_status not like '%Success%')"
                + "    ORDER BY\n"
                + "        thirdpartytxns.txndate ASC";
        if (code.equals("M-PESA") || code.equals("WALLET2MKOBA") || code.equals("MKOBA2WALLET")) {
            sql = "SELECT\n"
                    + "        COUNT(*) txnCount,\n"
                    + "        ifnull(sum(thirdpartytxns.amount),\n"
                    + "        0) AS txnvolume,\n"
                    + "        ifnull((SELECT\n"
                    + "             ifnull(mpesatransactionwithbalance.post_balance,0) + 0 AS closing \n"
                    + "        FROM\n"
                    + "            mpesatransactionwithbalance \n"
                    + "        WHERE\n"
                    + "            mpesatransactionwithbalance.txndate <= subdate(?, 1)\n"
                    + "            AND mpesatransactionwithbalance.ttype = ? \n"
                    + "            AND mpesatransactionwithbalance.txn_type = ? \n"
                    + "        ORDER BY\n"
                    + "            mpesatransactionwithbalance.txndate DESC LIMIT 1),\n"
                    + "        0) openingBalance,\n"
                    + "        ifnull((SELECT\n"
                    + "            mpesatransactionwithbalance.post_balance AS closing \n"
                    + "        FROM\n"
                    + "            mpesatransactionwithbalance \n"
                    + "        WHERE\n"
                    + "        thirdpartytxns.txndate <=?  \n"
                    + "            AND mpesatransactionwithbalance.ttype = ? \n"
                    + "            AND mpesatransactionwithbalance.txn_type = ? \n"
                    + "        ORDER BY\n"
                    + "            mpesatransactionwithbalance.txndate DESC LIMIT 1),\n"
                    + "        0) closingBalance \n"
                    + "    FROM\n"
                    + "        thirdpartytxns \n"
                    + "    WHERE\n"
                    + "        thirdpartytxns.txndate >= ? and  thirdpartytxns.txndate <=? \n"
                    + "        AND thirdpartytxns.ttype = ? \n"
                    + "        AND thirdpartytxns.txn_type = ? \n"
                    + "        AND thirdpartytxns.mnoTxns_status LIKE '%success%' \n"
                    + "     and thirdpartytxns.txnid not in (select txnid from thirdpartytxns where ttype=? and txn_type =? and txndate >=? and mnoTxns_status not like '%Success%')"
                    + "    ORDER BY\n"
                    + "        thirdpartytxns.txndate ASC";
        }

        LOGGER.info("=======================TRACEER  22222222222222222222222222222 .==================");
//        LOGGER.info(sql.replace("?","'{}'"),txndate2, ttype, code, txndate2, ttype, code, txndate, txndate2, ttype, code, ttype, code, txndate);
        List<Map<String, Object>> result = this.jdbcTemplate.queryForList(sql, txndate2, ttype, code, txndate2, ttype, code, txndate, txndate2, ttype, code, ttype, code, txndate);
        return result;
    }

    public List<Map<String, Object>> getCBSClosingBalance(String code, String ttype, String txndate) {
        String sql = "SELECT IFNULL( (select COALESCE(post_balance,0) post_balance from cbstransactiosn where ttype=? AND txn_type=? and DATE(txndate)=? order by txndate DESC LIMIT 1) ,0) post_balance";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("txn_type", code);
        map.put("txnVolume", jdbcTemplate.queryForObject(sql, new Object[]{ttype, code, txndate}, String.class));
        list.add(map);
        return list;
    }

    public List<Map<String, Object>> getCBSTransactionsVolume(String code, String ttype, String txndate) {
        String sql = "SELECT IFNULL( (select COALESCE(sum(amount),0) post_balance from cbstransactiosn where ttype=? AND txn_type=? and DATE(txndate)=? order by txndate DESC) ,0) post_balance";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("txn_type", code);
        map.put("txnVolume", jdbcTemplate.queryForObject(sql, new Object[]{ttype, code, txndate}, String.class));
        list.add(map);
        return list;
    }

    public List<Map<String, Object>> getThirdPartyOpeningBalance(String code, String ttype, String txndate) {
        String sql = "SELECT IFNULL( (select COALESCE(post_balance,0) post_balance from thirdpartytxns where ttype=? and DATE(txndate)=? order by txndate DESC LIMIT 1) ,0) post_balance";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("txn_type", code);
        map.put("txnVolume", jdbcTemplate.queryForObject(sql, new Object[]{ttype, txndate}, String.class));
        list.add(map);
        return list;
    }

    public List<Map<String, Object>> getThirdPartyTxnsVolume(String code, String ttype, String txndate) {
        String sql = "SELECT IFNULL( (select COALESCE(sum(amount),0) post_balance from thirdpartytxns where ttype=? AND txn_type=? and DATE(txndate)=? order by txndate DESC LIMIT 1) ,0) post_balance";
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("txn_type", code);
        map.put("txnVolume", jdbcTemplate.queryForObject(sql, new Object[]{ttype, code, txndate}, String.class));
        list.add(map);
        return list;
    }

    public List<Map<String, Object>> getThirdPartyClosingBalance(String code, String ttype, String txndate) {
        String sql = "SELECT IFNULL( (select COALESCE(post_balance,0) post_balance from thirdpartytxns where ttype=? AND txn_type=? and DATE(txndate)=? order by txndate DESC LIMIT 1) ,0) post_balance";
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("txn_type", code);
        map.put("txnVolume", jdbcTemplate.queryForObject(sql, new Object[]{ttype, code, txndate}, String.class));
        list.add(map);
        return list;
    }
    //get CUMMULATIVE RECONCILIATION

    public List<CummulativeReconciliationObject> getCummulativeReconciliation(String txndate, String ReconCode, String ReconType) {
        return null;
//        try {
//
//            List<Map<String, Object>> cashbookTxns = getCBSTxnsVolumeOpeningBLClosingBL(ReconCode, ReconType, txndate + " 00:00:00", txndate + " 23:59:59");
//            List<Map<String, Object>> bankbookTxns = getThirdPartyTxnsVolumeOpeningBLClosingBL(ReconCode, ReconType, txndate + " 00:00:00", txndate + " 23:59:59");
//            String cashbookRefundFosa = getRefundsToFOSA(ReconCode, ReconType, txndate + " 00:00:00", txndate + " 23:59:59");
//            String bankWithdrawsNotInFosa = getSumOfWithdrawsTransactionsNotInFosa(ReconCode, ReconType, txndate + " 23:59:59");
//            String bankDepositsNotInCashbook = getSumOfDepositsTransactionsNotInFosa(ReconCode, ReconType, txndate + " 00:00:00", txndate + " 23:59:59");
//            String cashbookTransactionCharges = getSumOfWithdrawsTransactionsChargesInCashbook(ReconCode, ReconType, txndate + " 00:00:00", txndate + " 23:59:59");
//            String sumOfWithdrawsTransactionsNotInBank = getSumOfWithdrawsTransactionsNotInBank(ReconCode, ReconType, txndate);
//            String bankTransactionCharges = getBankStatementTransactionCharges(ReconCode, ReconType, txndate + " 00:00:00", txndate + " 23:59:59");
//            String sumOfWithdrawsTransactionsChargesNotInBank = getSumOfWithdrawsTransactionsChargesNotInBank(ReconCode, ReconType, txndate + " 23:59:59");
//            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
//            Date reconDate = dateFormat.parse(txndate);
//
//            // Getting the previous day and formatting into 'YYYY-MM-DD'
//            long previousDayMilliSeconds = reconDate.getTime() - ONE_DAY_MILLI_SECONDS;
//            Date previousDate = new Date(previousDayMilliSeconds);
//            String reconDatePrevious = dateFormat.format(previousDate);
//            LOGGER.info("PREVIOUS DATE IN RECON:{}", reconDatePrevious);
//            String cashbookRefundFosaLessBack = getRefundsToFOSA2(ReconCode, ReconType, reconDatePrevious + " 00:00:00", reconDatePrevious + " 23:59:59");
//            String bankPreviousTransactionCharges = getPreviousBankTransactionCharges(ReconCode, ReconType, reconDatePrevious + " 00:00:00", reconDatePrevious + " 23:59:59");
//            List<CummulativeReconciliationObject> cummulativeRecon = this.jdbcTemplate.query("select * from txns_types where ttype=? and code=? order by id asc", new Object[]{ReconType, ReconCode}, (ResultSet rs, int rowNum) -> {
//                CummulativeReconciliationObject cuRecon = new CummulativeReconciliationObject();
//                cuRecon.setCashBookOpeningBalance(new BigDecimal(cashbookTxns.get(0).get("openingBalance").toString().replace("-", "")));
//                cuRecon.setCashBookClosingBalance(new BigDecimal(cashbookTxns.get(0).get("closingBalance").toString().replace("-", "")));
//                cuRecon.setCashBookCustomerWithraws(new BigDecimal(cashbookTxns.get(0).get("txnvolume").toString()));
//                cuRecon.setCashBookCustomerWithrawsCharges(new BigDecimal(cashbookTransactionCharges));
//                cuRecon.setCashBookRefundFosa(new BigDecimal(cashbookRefundFosa));
//                cuRecon.setBankClosingBalance(new BigDecimal(bankbookTxns.get(0).get("closingBalance").toString()));
//                cuRecon.setBankOpeningBalance(new BigDecimal(bankbookTxns.get(0).get("openingBalance").toString()));
//                cuRecon.setBankCashWithdrawsNotInFosa(new BigDecimal(bankWithdrawsNotInFosa));
//                cuRecon.setBankDepositsNotInCashbook(new BigDecimal(bankDepositsNotInCashbook));
//                cuRecon.setBankUncommissionedCustomerWithdraws(new BigDecimal(sumOfWithdrawsTransactionsNotInBank));
//                cuRecon.setBankUncreditedCheques(BigDecimal.ZERO);
//                cuRecon.setBankUncreditedDeposits(BigDecimal.ZERO);
//                cuRecon.setCashBookLedgerFee(BigDecimal.ZERO);
//                cuRecon.setBankTransactionCharges(new BigDecimal(bankTransactionCharges));
//                cuRecon.setBankUncommissionedCustomerWithdrawsCharges(new BigDecimal(sumOfWithdrawsTransactionsChargesNotInBank));
//                cuRecon.setBankLedgerFees(BigDecimal.ZERO);
//                cuRecon.setBankTransactionWithdrawsCharges(BigDecimal.ZERO);
//                cuRecon.setCashBookRefundFosaLessBack(new BigDecimal(cashbookRefundFosaLessBack));
//                cuRecon.setBankPreviousTransactionCharges(new BigDecimal(bankPreviousTransactionCharges));
//                return cuRecon;
//            });
//            return cummulativeRecon;
//        } catch (Exception e) {
//            LOGGER.info("EXCEPTION OCCURED DURING CREATING A CUMMULCATIVE RECON:{}", e.getMessage());
//            LOGGER.info(null, e);
//            return null;
//        }
    }
//GET RECON EXCEPTION REPORT

    public List<Map<String, Object>> getReconExceptionReportSetup() {
        return this.jdbcTemplate.queryForList(" SELECT * FROM recon_exception_reports_setup");
    }

    public String getReconExceptionReportsAjax(String exceptionType, String fromDate, String toDate, String ttype, String txndate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";

        //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
        switch (exceptionType) {
            case "NMT":
                //NOT IN MNO
                mainSql = "select count(b.txnid) from cbstransactiosn b  where b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txndate <= ? AND c.txn_status not like '%success%') and ttype=?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, fromDate, toDate, ttype, fromDate, toDate, ttype, fromDate, toDate, ttype}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = "WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txndate <= ? AND c.txn_status not like '%success%') and ttype=? ";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, fromDate, toDate, ttype, fromDate, toDate, ttype, fromDate, toDate, ttype}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select * from cbstransactiosn b" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, fromDate, toDate, ttype, fromDate, toDate, ttype, fromDate, ttype});

                } else {
                    mainSql = "select * from  cbstransactiosn b where b.txnid not in (select a.txnid FROM thirdpartytxns a WHERE a.ttype = ? AND a.txndate >= ?  AND a.txndate <= ? ) and  b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND b.txn_status like '%success%'  and b.txnid not in ( select c.txnid from cbstransactiosn c where c.ttype = ?  AND c.txndate >= ? AND c.txndate <= ? AND c.txn_status not like '%success%') and ttype=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, fromDate, toDate, ttype, fromDate, toDate, ttype, fromDate, toDate, ttype});
                }
                break;
            case "CMCBS":
                //NOT IN MNO
                mainSql = "select count(b.txnid) from cbstransactiosn b  where    b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND (b.txn_status like '%reversed%'  OR b.txn_status LIKE '%Cash Movement Between Accounts%' )";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    searchQuery = "WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount) LIKE ? and   b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND (b.txn_status like '%reversed%'  OR b.txn_status LIKE '%Cash Movement Between Accounts%')";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, fromDate, toDate}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select * from cbstransactiosn b" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, fromDate, toDate, ttype, fromDate, toDate, ttype, fromDate, ttype});

                } else {
                    mainSql = "select * from  cbstransactiosn b where b.ttype = ? AND b.txndate >= ? AND b.txndate <= ?  AND (b.txn_status like '%reversed%'  OR b.txn_status LIKE '%Cash Movement Between Accounts%' )  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, fromDate, toDate});
                }
                break;
            case "TLEOD":
                //Transaction Late EOD
                mainSql = "SELECT count(*) from cbstransactiosn where status2='LATEEOD' AND txndate2>=? and txndate2<=? and ttype=?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, ttype}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = "WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND status2='LATEEOD' and txndate2>=? and txndate2<=? and ttype=?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, fromDate, toDate, ttype}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select * from cbstransactiosn b" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, fromDate, toDate, ttype});

                } else {
                    mainSql = "select * from  cbstransactiosn b where  status2='LATEEOD'  AND txndate2>=? and txndate2<=? and ttype=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    LOGGER.info(mainSql);
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, ttype});
                }
                break;
            case "JTFF":
                //Journal Transaction FinFinancials
                break;
            case "NFFT":
                //Not in FinFinancial Transactions
                break;
            case "NBS":
                //Not in Bank Statement
                break;
            case "UF":
                //Uncomissioned Fosa
                break;

        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    //Cummullative reconciliations
    public String getLukuGwTransactionsAjax(String sourceType, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        switch (sourceType) {
            case "LUKU":
                //NOT IN MNO
                mainSql = "select count(b.txnid) from thirdpartytxns b  where b.txn_type = 'LUKU' AND DATE(b.txndate) >= ? AND DATE(b.txndate) <= ?";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "%" + searchValue + "%";
                    //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                    searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceAccount,' ',txdestinationaccount,' ',amount,' ') LIKE ? AND  b.txn_type = 'LUKU' AND DATE(b.txndate) >= ? AND DATE(b.txndate) <= ? ";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM thirdpartytxns b " + searchQuery, new Object[]{searchValue, fromDate, toDate}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select txnid AS txnid, sourceAccount AS msisdn, txdestinationaccount AS meter, txndate as txndate, mnoTxns_status as status, terminal as token, 'N/A' as units, amount as amount  from thirdpartytxns b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

                } else {
                    mainSql = "select txnid AS txnid, sourceAccount AS msisdn, txdestinationaccount AS meter, txndate as txndate, mnoTxns_status as status, terminal as token, 'N/A' as units, amount as amount from  thirdpartytxns b where  b.txn_type = 'LUKU' AND DATE(b.txndate) >= ? AND DATE(b.txndate) <= ?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }
                break;
            case "TCB":
                //gateway LUKU
                //SELECT reqtrxid, reqmsisdn, reqaccount, reqdate, reqlangcode, reqtranstype, reqstatus, restoken, resunits, restax, resfixed, resdebt, resreceipt, restimestamp, resstatus, reqamount, reqrequest, reqimsi, fixed2, fixed3, fixed4, callbackUrl
                mainSql = "select count(b.reqtrxid) from tp_LUKU_requests b  where  DATE(b.reqdate) >= ? AND DATE(b.reqdate) <= ?";
                totalRecords = jdbcGWTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "%" + searchValue + "%";
                    searchQuery = " WHERE  concat(reqtrxid,' ',reqmsisdn,' ',reqdate,' ',reqlangcode,' ',reqtranstype,' ',reqstatus,' ',resunits) LIKE ? and   DATE(b.reqdate) >= ? AND DATE(b.reqdate) <= ?";
                    totalRecordwithFilter = jdbcGWTemplate.queryForObject("SELECT COUNT(b.reqtrxid) FROM tp_LUKU_requests b " + searchQuery, new Object[]{searchValue, fromDate, toDate}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select reqtrxid AS txnid, reqmsisdn AS msisdn, reqaccount AS meter, reqdate as txndate, reqstatus as status, restoken as token, resunits as units, reqamount as amount  from tp_LUKU_requests b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcGWTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

                } else {
                    mainSql = "select reqtrxid AS txnid, reqmsisdn AS msisdn, reqaccount AS meter, reqdate as txndate, reqstatus as status, restoken as token, resunits as units, reqamount as amount from  tp_LUKU_requests b where  DATE(b.reqdate) >= ? AND DATE(b.reqdate) <= ?";
                    results = this.jdbcGWTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }
                //  LOGGER.info(mainSql.replace("?", "'{}'"), searchValue, fromDate, toDate);
                break;
            case "JTFF":
                //Journal Transaction FinFinancials
                break;
            case "NFFT":
                //Not in FinFinancial Transactions
                break;
            case "NBS":
                //Not in Bank Statement
                break;
            case "UF":
                //Uncomissioned Fosa
                break;

        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String getBulkTransactionsAjax(String ttype, String txnType, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        mainSql = "select count(b.txnid) from cbstransactiosn b  where b.ttype = ? AND b.txn_type = ? AND  date(b.txndate) >= ? AND date(b.txndate) <= ?  AND b.txn_status like '%success%'";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{ttype, txnType, fromDate, toDate}, Integer.class);
        LOGGER.info(mainSql.replace("?", "'{}'"), ttype, txnType, fromDate, toDate);
        if (!searchValue.equals("")) {
            searchValue = "'%" + searchValue + "%'";
            searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND b.ttype = ? AND b.txn_type = ? AND  date(b.txndate) >= ? AND date(b.txndate) <= ?  AND b.txn_status like '%success%";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, ttype, txnType, fromDate, toDate}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select * from cbstransactiosn b" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, ttype, txnType, fromDate, toDate});

        } else {
            mainSql = "select * from  cbstransactiosn b where b.ttype = ? AND b.txn_type = ? AND  date(b.txndate) >= ? AND date(b.txndate) <= ?  AND b.txn_status like '%success%'  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{ttype, txnType, fromDate, toDate});
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String procSendLukuSMS(String txid, String type) {
        try {
            String mainSql = "select reqtrxid, reqmsisdn, reqaccount AS meter, reqdate as txndate, restoken as token, resunits as units, reqamount as amount from  tp_LUKU_requests b where b.reqtrxid=?";
            List<Map<String, Object>> results = this.jdbcGWTemplate.queryForList(mainSql, new Object[]{txid});
            if (results.isEmpty()) {
                return "97";
            } else {
                String msgBody = "REF: " + results.get(0).get("reqtrxid");
                msgBody += "\nTOKEN:" + results.get(0).get("token");
                msgBody += "\nMETER NO: " + results.get(0).get("meter");
                msgBody += "\nAMOUNT:" + results.get(0).get("amount");

                String msgTo = results.get(0).get("reqmsisdn") + "";

                return sendSms(msgTo, msgBody);
            }
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            return "96";
        }
    }

    private String sendSms(String msgTo, String msgBody) {
        String response = "-1";
        String request = "<methodCall>"
                + "<methodName>TPB.SENDSMS</methodName>"
                + "<params>"
                + "<param><value><string>" + msgTo + "</string></value></param>"
                + "<param><value><string>" + msgBody + "</string></value></param>"
                + "<param><value><string>" + System.currentTimeMillis() + "</string></value></param>"
                + "</params>"
                + "</methodCall>";
        String smsResponse = HttpClientService.sendXMLRequest(request, this.systemVariable.SMSC_URL);
        LOGGER.info("REQUEST TO GATEWAY: {}", request);
        LOGGER.info("RAW RESPONSE FROM GATEWAY: {}", smsResponse);
        if (!smsResponse.equals("-1")) {
            return "0";
        } else {
            return "96";
        }

    }

    public String getSuspiciousGwTransactionsAjax(String amount, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
//SELECT id, txnid, thirdparty_reference, txn_type, ttype, txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch

        mainSql = "select count(b.txnid) from cbstransactiosn b  where  dr_cr_ind='CR' AND txn_status LIKE '%success%' and b.ttype = 'B2C' AND DATE(b.txndate) >= ? AND DATE(b.txndate) <= ? AND amount>=?";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, amount}, Integer.class);
        if (!searchValue.equals("")) {
            searchValue = "'%" + searchValue + "%'";
            searchQuery = " WHERE  concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceAccount,' ',destinationaccount,' ',amount,' ') LIKE ? AND  dr_cr_ind='CR' AND txn_status LIKE '%success%' and b.ttype = 'B2C' AND DATE(b.txndate) >= ? AND DATE(b.txndate) <= ? AND amount>=?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b " + searchQuery, new Object[]{searchValue, fromDate, toDate, amount}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select  txnid, thirdparty_reference, txn_type, ttype, txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch from cbstransactiosn b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, amount});

        } else {
            mainSql = "select  txnid, thirdparty_reference, txn_type, ttype, txndate, sourceaccount, destinationaccount, amount, charge, description, terminal, currency, txn_status, prevoius_balance, post_balance, pan, contraaccount, dr_cr_ind, branch from  cbstransactiosn b where dr_cr_ind='CR' AND txn_status LIKE '%success%' and  b.ttype = 'B2C' AND DATE(b.txndate) >= ? AND DATE(b.txndate) <= ?  AND amount>=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, amount});
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String getSwiftSTPReportAjax(String txnType, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        String json = null;
        String jsonString = null;
        //  System.out.println();
        switch (txnType) {
            case "0":
                mainSql = "select count(b.reference) from transfers b  where direction='INCOMING' AND message_type  in ('103',',202') AND cbs_status = 'C' AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    searchQuery = " WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',currency,' ',beneficiaryBIC,' ',senderBIC,' ',sender_name,' ',beneficiaryName,' ') LIKE ? AND direction='INCOMING' AND message_type  in ('103',',202') AND  b.cbs_status = 'C' AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.reference) FROM transfers b " + searchQuery, new Object[]{searchValue, fromDate, toDate, txnType}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select          message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message  from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

                } else {
                    mainSql = "select    message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message FROM transfers b where direction='INCOMING' AND message_type  in ('103',',202') AND cbs_status = 'C' AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }

                //Java objects to JSON string - compact-print - salamu - Pomoja.
                try {
                    jsonString = this.jacksonMapper.writeValueAsString(results);
                } catch (JsonProcessingException ex) {
                    LOGGER.error("RequestBody: ", ex);
                }
                json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                // System.out.println(mainSql);
                break;
            case "96":
                mainSql = "select count(b.reference) from transfers b  where direction='INCOMING' AND message_type in ('103',',202') AND cbs_status <> 'C' AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    searchQuery = " WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',currency,' ',beneficiaryBIC,' ',senderBIC,' ',sender_name,' ',beneficiaryName,' ') LIKE ? AND direction='INCOMING' AND message_type  in ('103',',202') AND  b.cbs_status <> 'C' AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.reference) FROM transfers b " + searchQuery, new Object[]{searchValue, fromDate, toDate, txnType}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select          message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message  from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

                } else {
                    mainSql = "select          message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message FROM transfers b where direction='INCOMING' AND message_type  in ('103',',202') AND cbs_status <> 'C' AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }

                //Java objects to JSON string - compact-print - salamu - Pomoja.
                try {
                    jsonString = this.jacksonMapper.writeValueAsString(results);
                } catch (JsonProcessingException ex) {
                    LOGGER.error("RequestBody: ", ex);
                }
                json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                //System.out.println(mainSql);

                break;

        }
        return json;
    }

    public String getSwiftOutReportAjax(String txnType, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        String json = null;
        String jsonString = null;
        //  System.out.println();
        switch (txnType) {
            case "0":
                mainSql = "select count(b.reference) from transfers b  where direction ='OUTGOING' and txn_type in ('001','004') and status ='C' and cbs_status ='C' and swift_status is not null AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    searchQuery = " WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',currency,' ',beneficiaryBIC,' ',senderBIC,' ',sender_name,' ',beneficiaryName,' ') LIKE ? AND direction ='OUTGOING' and txn_type in ('001','004') and status ='C' and cbs_status ='C' and swift_status is not null AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.reference) FROM transfers b " + searchQuery, new Object[]{searchValue, fromDate, toDate, txnType}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select         swift_status, message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message  from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

                } else {
                    mainSql = "select    swift_status,message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message FROM transfers b where direction ='OUTGOING' and txn_type in ('001','004') and status ='C' and cbs_status ='C' and swift_status is not null AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }

                //Java objects to JSON string - compact-print - salamu - Pomoja.
                try {
                    jsonString = this.jacksonMapper.writeValueAsString(results);
                } catch (JsonProcessingException ex) {
                    LOGGER.error("RequestBody: ", ex);
                }
                json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                // System.out.println(mainSql);
                break;
            case "96":
                mainSql = "select count(b.reference) from transfers b  where direction ='OUTGOING' and txn_type in ('001','004') and  cbs_status ='C' and swift_status is null AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
                if (!searchValue.equals("")) {
                    searchValue = "'%" + searchValue + "%'";
                    searchQuery = " WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',currency,' ',beneficiaryBIC,' ',senderBIC,' ',sender_name,' ',beneficiaryName,' ') LIKE ? AND direction ='OUTGOING' and txn_type in ('001','004') and cbs_status ='C' and swift_status is null AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ?";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.reference) FROM transfers b " + searchQuery, new Object[]{searchValue, fromDate, toDate, txnType}, Integer.class);
                } else {
                    totalRecordwithFilter = totalRecords;
                }
                if (!searchQuery.equals("")) {
                    mainSql = "select          swift_status,message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message  from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

                } else {
                    mainSql = "select    swift_status,message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message FROM transfers b where direction ='OUTGOING' and txn_type in ('001','004') and cbs_status ='C' and swift_status is null AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                }

                //Java objects to JSON string - compact-print - salamu - Pomoja.
                try {
                    jsonString = this.jacksonMapper.writeValueAsString(results);
                } catch (JsonProcessingException ex) {
                    LOGGER.error("RequestBody: ", ex);
                }
                json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
                //System.out.println(mainSql);

                break;

        }
        return json;
    }

    public String getSwiftReturnedReportAjax(String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        String json = null;
        String jsonString = null;

        mainSql = "select count(b.reference) from transfers b  where units  in ('RETURNED') AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);
        if (!searchValue.equals("")) {
            searchValue = "'%" + searchValue + "%'";
            searchQuery = " WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',currency,' ',beneficiaryBIC,' ',senderBIC,' ',sender_name,' ',beneficiaryName,' ') LIKE ? AND units  in  ('RETURNED') AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.reference) FROM transfers b " + searchQuery, new Object[]{searchValue, fromDate, toDate}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select          message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message  from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

        } else {
            mainSql = "select    message_type, txn_type, sourceAcct, destinationAcct, amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, supportingDocument, status, response_code, comments, purpose, direction, originallMsgNmId, initiated_by, returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt, modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message FROM transfers b where  units  in  ('RETURNED')  AND DATE(b.create_dt) >= ? AND DATE(b.create_dt) <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        // System.out.println(mainSql);

        return json;
    }

    public List<Map<String, Object>> getfireCardIssueAjax(String cardStatus, String fromDate, String toDate) {
        List<Map<String, Object>> results = null;
        String mainSql;
        String json = null;

        switch (cardStatus) {
            case "CP_AP":
//                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*, (select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c where c.PAN IS NOT NULL AND (c.approver2_dt IS NOT NULL OR c.received_from_printing_dt IS NOT NULL) AND DATE(c.create_dt) >= ? AND DATE(c.create_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});

                break;
            case "AP":
                // awaiting for printing
//                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b2 where b2.code=c.collecting_branch) as cb_name from card c where c.status='AP' AND DATE(c.approver2_dt) >= ? AND DATE(c.approver2_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "CD":
                //  CARD DISPARTCHED TO BRANCHES
                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b1 where b1.code=c.collecting_branch) as cb_name from card c where DATE(c.dispatched_dt) >= ? AND DATE(c.dispatched_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "CNI":
                //  CARD DISPARTCHED TO BRANCHES BUT NOT ISSUED
                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b1 where b1.code=c.collecting_branch) as cb_name from card c where c.status='CD' AND DATE(c.dispatched_dt) >= ? AND DATE(c.dispatched_dt) <= ?";
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "C":
                //  CARD ISSUED SUCCESSFULLY
                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c where c.status='C' AND DATE(c.issued_dt) >= ? AND DATE(c.issued_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "R":
                //  CARD ISSUED SUCCESSFULLY
                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c where c.status='R' AND DATE(c.create_dt) >= ? AND DATE(c.create_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "I":
                //  AWAIT BRANCH APPROVAL
//                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c where c.status='I' AND DATE(c.create_dt) >= ? AND DATE(c.create_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "P":
                // AWAIT HQ APPROVAL(APPROVED BY BRANCH)
//                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*, (select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c  where c.status='P' AND DATE(c.approver1_dt) >= ? AND DATE(c.approver1_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            case "RA":
                // AWAIT HQ APPROVAL(APPROVED BY BRANCH)
//                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*, (select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c  where c.status='RA' AND DATE(c.approver1_dt) >= ? AND DATE(c.approver1_dt) <= ?";
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            default:
                // RECEIVED FROM PRINTING CP
//                LOGGER.info("getfireCardIssueAjax with [{} {} {}]", fromDate, toDate, cardStatus);
                mainSql = "select c.*,(select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c where DATE(c.received_from_printing_dt) >= ? AND DATE(c.received_from_printing_dt) <= ?";

                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
        }

        return results;

    }

    public List<Map<String, Object>> getMKOBAAccountCBSTransation(String account, String dr_cr) {
        String query = "SELECT gah.TXN_AMT,gah.ROW_TS, \n"
                + "gah.TRAN_REF_TXT,gah.TRAN_DESC,gah.ACCT_NO,\n"
                + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID,gah.DR_CR_IND,gah.ACCT_HIST_ID,\n"
                + "gah.STMNT_BAL\n"
                + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.ACCT_NO = ? AND  DR_CR_IND=? ORDER BY ACCT_HIST_ID";

        return this.jdbcRUBIKONTemplate.queryForList(query, account, dr_cr);
    }

    public List<Map<String, Object>> getMKOBAAccountCBSTransationByRef(String account, String refer) {
        String query = "SELECT gah.TXN_AMT as amount,gah.ROW_TS as txndate, \n"
                + "gah.TRAN_REF_TXT as txid,gah.TRAN_DESC as msisdn,gah.ACCT_NO as sourceaccount,gah.ACCT_NO as groupid,\n"
                + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID,gah.DR_CR_IND,gah.ACCT_HIST_ID,\n"
                + "gah.STMNT_BAL\n"
                + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.ACCT_NO = ? AND  TRAN_REF_TXT in (" + refer + ") ORDER BY ACCT_HIST_ID";

        return this.jdbcRUBIKONTemplate.queryForList(query, account);
    }

    public List<Map<String, Object>> getMKOBAAccountVgTransationByRef(String account, String refer) {
        String query = "select transamount as AMOUNT,transate as TXNDATE, transid as TXID,memberid as MSISDN, destinationaccount as SOURCEACCOUNT ,GROUPID , 'TZS' AS CURRENCY,'-99' as ORIGIN_BU_ID,'CR' as DR_CR_IND, '1' AS ACCT_HIST_ID,\n"
                + "'0' as STMNT_BAL from vg_group_transaction where destinationaccount=?  and  receipt <> '-1' and transid in (" + refer + ") and transstatus='0'";
        query += " union select transamount as AMOUNT,transate as TXNDATE, transid as TXID,memberid as MSISDN, destinationaccount as SOURCEACCOUNT ,GROUPID , 'TZS' AS CURRENCY,'-99' as ORIGIN_BU_ID,'CR' as DR_CR_IND, '1' AS ACCT_HIST_ID,\n"
                + "'0' as STMNT_BAL from vg_group_transaction_archive where destinationaccount=?  and  receipt <> '-1' and transid in (" + refer + ") and transstatus='0'";

        return this.jdbcMKOBATemplate.queryForList(query, account, account);
    }

    public List<Map<String, Object>> getMKOBAAccountGatewayTransation(String account, String dr_cr) {
        String query = ("select * from vg_group_transaction where sourceaccount=?");
        if (dr_cr.equals("DR")) {
            query = ("select * from vg_group_transaction where sourceaccount=? and transstatus='0'");
            query += (" UNION select * from vg_group_transaction_archive where sourceaccount=? and transstatus='0'");
        } else {
            query = ("select * from vg_group_transaction where destinationaccount=?  and  receipt <> '-1'  and transstatus='0' and transtype not in (12716,2721,12722,12723,12724,12725,1275,1142,1143,1144)");
            query += (" UNION select * from vg_group_transaction_archive where destinationaccount=?  and  receipt <> '-1'  and transstatus='0' and transtype not in (12716,2721,12722,12723,12724,12725,1275,1142,1143,1144)");
        }

        return this.jdbcMKOBATemplate.queryForList(query, account, account);
    }

    public List<Map<String, Object>> getMKOBAAccountGatewayTransationByRef(String refer) {
        String query = ("select transid as txid,transamount as amount,sourceaccount,transate as txndate,memberid as msisdn,groupid from vg_group_transaction where transid in (" + refer + ");");

        return this.jdbcMKOBATemplate.queryForList(query);
    }

    public BigDecimal getMKOBAAccountGatewayBalance(String account) {
        BigDecimal result = this.jdbcMKOBATemplate.queryForObject("SELECT balance FROM vg_group_account WHERE account=?", new Object[]{account}, BigDecimal.class);

        return result;
    }

    public String getMKOBAAccountGateway(String account) {
        String result = this.jdbcMKOBATemplate.queryForObject("SELECT account FROM vg_group_account WHERE account=? or groupid=?", new Object[]{account, account}, String.class);
        return result;
    }

    public BigDecimal getMKOBAAccountCBSBalance(String account) {
        BigDecimal result = this.jdbcRUBIKONTemplate.queryForObject("select \n"
                + "LEDGER_BAL \n"
                + "from \n"
                + "deposit_account_summary where ACCT_NO=?", new Object[]{account}, BigDecimal.class);
        return result;
    }

    public String getfireMkobaSolutionAjax(String ttype, String account, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        List<String> cbsArray = new ArrayList<>();
        List<String> cbsArrayCR = new ArrayList<>();
        List<String> cbsArrayDuplicateRemoved = new ArrayList<>();
        List<String> mkobaArray = new ArrayList<>();
        List<String> finalRerefernceArray = new ArrayList<>();
        String json = null;

        LOGGER.info("Dr_Cr:{}, Account: {}", ttype, account);
        if (ttype.equals("DR")) {
            List<Map<String, Object>> cbsDB = getMKOBAAccountCBSTransation(account, "DR");
            List<Map<String, Object>> cbsDBCR = getMKOBAAccountCBSTransation(account, "CR");
            for (Map<String, Object> map : cbsDB) {
                cbsArray.add(map.get("TRAN_REF_TXT") + "");
            }

            for (Map<String, Object> map : cbsDBCR) {
                cbsArrayCR.add(map.get("TRAN_REF_TXT") + "");
            }
            for (String reference : cbsArray) {
                if (!cbsArrayCR.contains(reference.trim())) {
                    cbsArrayDuplicateRemoved.add(reference);
                }
            }
            LOGGER.info("cbsArrayt: {}\ncbsArrayDuplicateRemoved: {}", cbsArray, cbsArrayDuplicateRemoved);

            List<Map<String, Object>> mkobaDB = getMKOBAAccountGatewayTransation(account, ttype);
            for (Map<String, Object> map : mkobaDB) {
                mkobaArray.add(map.get("transid") + "");
            }
            LOGGER.info("mkobaArray: {}", mkobaArray);

            for (String reference : cbsArrayDuplicateRemoved) {
                if (!mkobaArray.contains(reference.trim())) {
                    finalRerefernceArray.add(reference);
                }
            }
            LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
            String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));

            results = getMKOBAAccountCBSTransationByRef(account, finalRerefernces);

            int totalRecordwithFilter = 0;
            int totalRecords = 0;
            String jsonString = null;
            //Java objects to JSON string - compact-print - salamu - Pomoja.
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            //   jsonString = "{}";
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
            LOGGER.info("json: {}", json);
        } else if (ttype.equals("CR")) {
            List<Map<String, Object>> cbsDB = getMKOBAAccountCBSTransation(account, "CR");
            List<Map<String, Object>> cbsDBCR = getMKOBAAccountCBSTransation(account, "DR");
            for (Map<String, Object> map : cbsDB) {
                cbsArray.add(map.get("TRAN_REF_TXT") + "");
            }

            for (Map<String, Object> map : cbsDBCR) {
                cbsArrayCR.add(map.get("TRAN_REF_TXT") + "");
            }
            for (String reference : cbsArray) {
                if (!cbsArrayCR.contains(reference.trim())) {
                    cbsArrayDuplicateRemoved.add(reference);
                }
            }
            LOGGER.info("cbsArrayt: {}\ncbsArrayDuplicateRemoved: {}", cbsArray, cbsArrayDuplicateRemoved);

            List<Map<String, Object>> mkobaDB = getMKOBAAccountGatewayTransation(account, ttype);
            for (Map<String, Object> map : mkobaDB) {
                mkobaArray.add(map.get("transid") + "");
            }
            LOGGER.info("mkobaArray: {}", mkobaArray);

            for (String reference : cbsArrayDuplicateRemoved) {
                if (!mkobaArray.contains(reference.trim())) {
                    finalRerefernceArray.add(reference);
                }
            }
            for (String reference : mkobaArray) {
                if (!cbsArrayDuplicateRemoved.contains(reference.trim())) {
                    finalRerefernceArray.add(reference);
                }
            }
            LOGGER.info("finalRerefernceArray: {}", finalRerefernceArray);
            String finalRerefernces = finalRerefernceArray.stream().collect(Collectors.joining("','", "'", "'"));

            results = getMKOBAAccountVgTransationByRef(account, finalRerefernces);

            int totalRecordwithFilter = 0;
            int totalRecords = 0;
            String jsonString = null;
            //Java objects to JSON string - compact-print - salamu - Pomoja.
            try {
                jsonString = this.jacksonMapper.writeValueAsString(results);
            } catch (JsonProcessingException ex) {
                LOGGER.error("RequestBody: ", ex);
            }
            //   jsonString = "{}";
            json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
            LOGGER.info("json: {}", json);

        } else {
            json = "{\"draw\":" + 0 + ",\"iTotalRecords\":\"" + 0 + "\",\"iTotalDisplayRecords\":\"" + 0 + "\",\"aaData\":[]}";

        }
        return json;
    }

    public BigDecimal getCBSOpeningBalance2(String accountNo, String balanceDate, String ttype) {
        try {
            if (ttype.equals("B2C")) {
                ttype = "+";
            } else {
                ttype = "-";

            }
            BigDecimal result = this.jdbcRUBIKONTemplate.queryForObject("SELECT STMNT_BAL" + ttype + "ACCT_AMT AS opening_balance FROM GL_ACCOUNT_HISTORY WHERE GL_ACCT_NO = ? \n"
                            + "  and  TRUNC(SYS_CREATE_TS)=to_date(?,'YYYY-MM-DD') order by ACCT_HIST_ID asc \n"
                            + "fetch first 1 rows only",
                    new Object[]{accountNo, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("Opening Balance first error: {}", e);

            if (e.getMessage().equals("Incorrect result size: expected 1, actual 0")) {
                try {
                    BigDecimal result = this.jdbcRUBIKONTemplate.queryForObject("SELECT ABS(STMNT_BAL) as closing FROM GL_ACCOUNT_HISTORY WHERE GL_ACCT_NO = ?\n"
                                    + "   order by ACCT_HIST_ID desc\n"
                                    + "fetch first 1 rows only",
                            new Object[]{accountNo}, BigDecimal.class);
                    return result;
                } catch (DataAccessException ed) {
                    LOGGER.info("Opening Balance second error: {}", ed);

                    return new BigDecimal("0.0");
                }
            } else {
                return new BigDecimal("0.0");
            }
        }
    }

    public BigDecimal getCBSClosingBalance2(String accountNo, String balanceDate, String ttype) {
        try {

            BigDecimal result = this.jdbcRUBIKONTemplate.queryForObject("SELECT STMNT_BAL as closing FROM GL_ACCOUNT_HISTORY WHERE GL_ACCT_NO = ?\n"
                            + "  and  TRUNC(SYS_CREATE_TS)=to_date(?,'YYYY-MM-DD') order by ACCT_HIST_ID desc\n"
                            + "fetch first 1 rows only",
                    new Object[]{accountNo, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("Closing Balance: {}", e);
            if (e.getMessage().equals("Incorrect result size: expected 1, actual 0")) {
                try {
                    BigDecimal result = this.jdbcRUBIKONTemplate.queryForObject("SELECT STMNT_BAL as closing FROM GL_ACCOUNT_HISTORY WHERE GL_ACCT_NO = ?\n"
                                    + "   order by ACCT_HIST_ID desc\n"
                                    + "fetch first 1 rows only",
                            new Object[]{accountNo}, BigDecimal.class);
                    return result;
                } catch (DataAccessException ed) {
                    LOGGER.info("Opening Balance second error: {}", ed);

                    return new BigDecimal("0.0");
                }
            } else {
                return new BigDecimal("0.0");
            }
        }
    }

    public BigDecimal getMPESAMKOBAClosingBalance(String txn_type, String ttype, String balanceDate) {
        try {

            BigDecimal result = this.jdbcTemplate.queryForObject("select post_balance  from mpesatransactionwithbalance  where txn_type =? and ttype =? and date(txndate)=? order by txndate desc limit 1;",
                    new Object[]{txn_type, ttype, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("Closing Balance: {}", e);
            return new BigDecimal("0.0");
        }
    }

    public BigDecimal getMPESAMKOBAOpeningBalance(String txn_type, String ttype, String balanceDate) {
        try {
            String alama = "+";
            if (ttype.equals("B2C") || txn_type.equals("MKOBA2WALLET")) {
                alama = "+";
            } else {
                alama = "-";

            }
            BigDecimal result = this.jdbcTemplate.queryForObject("select post_balance " + alama + " amount as bala  from mpesatransactionwithbalance  where txn_type =? and ttype =? and date(txndate)=? order by txndate asc limit 1;",
                    new Object[]{txn_type, ttype, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("Opening Balance: {}", e);
            return new BigDecimal("0.0");
        }
    }

    public BigDecimal getMNOOpeningBalance(String txn_type, String ttype, String balanceDate) {
        try {
            LOGGER.info("select previous_balance  as bala  from thirdpartytxns  where txn_type =? and ttype =? and date(file_txndate)=? and record_orig ='FILE' order by file_txndate asc limit 1;".replace("?", "'{}'"), txn_type, ttype, balanceDate);

            BigDecimal result = this.jdbcTemplate.queryForObject("select previous_balance  as bala  from thirdpartytxns  where txn_type =? and ttype =? and date(file_txndate)=? and record_orig ='FILE' order by file_txndate asc limit 1;",
                    new Object[]{txn_type, ttype, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("Opening Balance: {}", e);
            return new BigDecimal("0.0");
        }
    }

    public BigDecimal getMNOClosingBalance(String txn_type, String ttype, String balanceDate) {
        try {
            LOGGER.info("select post_balance as bala  from thirdpartytxns  where txn_type =? and ttype =? and date(file_txndate)=? and record_orig ='FILE' order by file_txndate desc limit 1;".replace("?", "'{}'"), txn_type, ttype, balanceDate);
            BigDecimal result = this.jdbcTemplate.queryForObject("select post_balance as bala  from thirdpartytxns  where txn_type =? and ttype =? and date(file_txndate)=? and record_orig ='FILE' order by file_txndate desc limit 1;",
                    new Object[]{txn_type, ttype, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("Closing Balance: {}", e);
            return new BigDecimal("0.0");
        }
    }

    public BigDecimal getTIGOClosingBalance(String txn_type, String ttype, String balanceDate) {
        try {
            LOGGER.info("select closing_balance from tigoaccountclosingbalance  where txn_type =? and ttype =? and date(txndate)=? order by txndate DESC".replace("?", "'{}'"), txn_type, ttype, balanceDate);
            BigDecimal result = this.jdbcTemplate.queryForObject("select closing_balance from tigoaccountclosingbalance  where txn_type =? and ttype =? and date(txndate)=? order by txndate DESC",
                    new Object[]{txn_type, ttype, balanceDate}, BigDecimal.class);
            return result;
        } catch (DataAccessException e) {
            LOGGER.info("TIGO Closing Balance: {}", e);
            return new BigDecimal("0.0");
        }
    }

    public List<Map<String, Object>> getAccountConfig() {
        try {
            List<Map<String, Object>> data = this.jdbcTemplate.queryForList("SELECT ID, account_group_id, a_account_no, b_account_no, account_name, row_ts, file_static_balance,txn_type,ttype FROM account_config order by ID asc");
            return data;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public int updateCBSBalanceForAccountHistory(String accountGroupId, String accountConfigId, String aAccountNo, String bAccountNo, String accountName, BigDecimal aAccountOpenBalance, BigDecimal aAccountCloseBalance, String balanceDate) {
        int result1 = 0;
        int result2 = 0;
        try {
            int count = this.jdbcTemplate.queryForObject("SELECT COUNT(account_config_id) FROM account_balance_history WHERE account_config_id = ? AND balance_date=?",
                    new Object[]{accountConfigId, balanceDate}, Integer.class);
            if (count < 1) {
                //insert
                result1 = this.jdbcTemplate.update("INSERT INTO account_balance_history ( account_group_id, account_config_id, a_account_no, b_account_no, account_name, a_account_open_balance, a_account_close_balance, balance_date) VALUES (?,?,?,?,?,?,?,?)", accountGroupId, accountConfigId, aAccountNo, bAccountNo, accountName, aAccountOpenBalance, aAccountCloseBalance, balanceDate);

            } else {
                //update
                result2 = this.jdbcTemplate.update("UPDATE account_balance_history SET a_account_open_balance=?, a_account_close_balance=? WHERE account_config_id=? AND balance_date=?", aAccountOpenBalance, aAccountCloseBalance, accountConfigId, balanceDate);

            }
        } catch (DataAccessException e) {
            result1 = -1;
            LOGGER.error("updateCBSBalanceForAccountHistory=>Rollbacked... {}", e);
            return result1;
        }
        return result1;
    }

    public int updateMNOBalanceForAccountHistory(String accountGroupId, String accountConfigId, String aAccountNo, String bAccountNo, String accountName, BigDecimal bAccountOpenBalance, BigDecimal bAccountCloseBalance, String balanceDate) {
        int result1 = 0;
        int result2 = 0;
        try {
            int count = this.jdbcTemplate.queryForObject("SELECT COUNT(account_config_id) FROM account_balance_history WHERE account_config_id = ? AND balance_date=?",
                    new Object[]{accountConfigId, balanceDate}, Integer.class);
            if (count < 1) {
                //insert
                result1 = this.jdbcTemplate.update("INSERT INTO account_balance_history ( account_group_id, account_config_id, a_account_no, b_account_no, account_name, b_account_open_balance, b_account_close_balance, balance_date) VALUES (?,?,?,?,?,?,?,?)", accountGroupId, accountConfigId, aAccountNo, bAccountNo, accountName, bAccountOpenBalance, bAccountCloseBalance, balanceDate);

            } else {
                //update
                result2 = this.jdbcTemplate.update("UPDATE account_balance_history SET b_account_open_balance=?, b_account_close_balance=? WHERE account_config_id=? AND balance_date=?", bAccountOpenBalance, bAccountCloseBalance, accountConfigId, balanceDate);

            }
        } catch (DataAccessException e) {
            result1 = -1;
            LOGGER.error("updateCBSBalanceForAccountHistory=>Rollbacked... {}", e);
            return result1;
        }
        return result1;
    }

    private List<Map<String, Object>> mnoToBeArchive(int days) {
        try {
            List<Map<String, Object>> data = this.jdbcTemplate.queryForList("SELECT  id  FROM thirdpartytxns WHERE txndate < date_ADD(NOW(), INTERVAL ? DAY) order by id asc LIMIT 5000", new Object[]{days});
            return data;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private List<Map<String, Object>> cbsToBeArchive(int days) {
        try {
            List<Map<String, Object>> data = this.jdbcTemplate.queryForList("SELECT  id  FROM cbstransactiosn WHERE txndate <  date_ADD(NOW(), INTERVAL ? DAY) order by id asc LIMIT 5000", new Object[]{days});
            return data;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public int mnoArchive(int days) throws InterruptedException {
        int result1 = 0;
        int result2 = 0;
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            List<Map<String, Object>> row = mnoToBeArchive(days);
            LOGGER.debug("Archive thirdpartytxns {} days Older transactions size: {}, Service is running at {}", days, row.size(), DateUtil.now());
            LOGGER.debug("SMSCArchiveArrayList: [ID]= {}", row);
            for (Map<String, Object> d : row) {
                Long id = Long.valueOf(d.get("id") + "");
                result1 = this.jdbcTemplate.update("insert IGNORE  into thirdpartytxns_ARCHIVE  SELECT *  FROM thirdpartytxns WHERE ID=?", (id));
                if (result1 == 1) {

                    result2 = this.jdbcTemplate.update("DELETE FROM thirdpartytxns WHERE ID=?", (id));
                }
                LOGGER.debug("smscArchive: [ID]= {},[INSERT]={},[DELETE]={}", id, result1, result2);
                if (false) {
                    Thread.sleep(200);
                }
            }
            txManager.commit(status);
        } catch (DataAccessException e) {
            txManager.rollback(status);
            result1 = -1;
            LOGGER.error("Rollbacked... {}", e.getMessage());
            return result1;
        }
        return result1;
    }

    public int cbsArchive(int days) throws InterruptedException {
        int result1 = 0;
        int result2 = 0;
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        TransactionStatus status = txManager.getTransaction(def);
        try {
            List<Map<String, Object>> row = cbsToBeArchive(days);
            LOGGER.debug("Archive cbstransactiosn {} days Older transactions size: {}, Service is running at {}", days, row.size(), DateUtil.now());
            LOGGER.debug("SMSCArchiveArrayList: [ID]= {}", row);
            for (Map<String, Object> d : row) {
                Long id = Long.valueOf(d.get("id") + "");
                result1 = this.jdbcTemplate.update("insert IGNORE  into cbstransactiosn_ARCHIVE  SELECT *  FROM cbstransactiosn WHERE ID=? ON DUPLICATE KEY UPDATE  txndate=?", id, d.get("txndate"));
                if (result1 == 1 || result1 == 2) {

                    result2 = this.jdbcTemplate.update("DELETE FROM cbstransactiosn WHERE ID=?", (id));
                }
                LOGGER.debug("smscArchive: [ID]= {},[INSERT]={},[DELETE]={}", id, result1, result2);
                if (false) {
                    Thread.sleep(200);
                }
            }
            txManager.commit(status);
        } catch (DataAccessException e) {
            txManager.rollback(status);
            result1 = -1;
            LOGGER.error("Rollbacked... {}", e.getMessage());
            return result1;
        }
        return result1;
    }

    public List<Map<String, Object>> getCustomerImages(String account) {
        try {
            List<Map<String, Object>> data = this.jdbcRUBIKONTemplate.queryForList("SELECT CUST_ID,BINARY_IMAGE,IMAGE_TY,IMAGE_TY_DESC FROM V_CUSTOMER_IMAGES vci WHERE CUST_ID IN (\n"
                    + "select CUST_ID FROM V_ACCOUNTs WHERE ACCT_NO =?\n"
                    + ")\n"
                    + " ", new Object[]{account});
            return data;
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("", e);
            return null;
        }
    }

    public List<Map<String, Object>> getReconGlAcctSettings() {
        return this.jdbcTemplate.queryForList("SELECT account_no,corresponding_gl_account FROM gl_account_recon_configs");
    }

    public List<LedgerReconDataReq> getGeneralCBSTransactionsForRecon(String reconDate, String glAcct, String txnType, String currency) {
        String sql = "SELECT STMNT_BAL, GL_ACCT_NO,TXN_AMT,SYS_CREATE_TS,DR_CR_IND,TRAN_REF_TXT,TRAN_DESC " +
                "FROM GL_ACCOUNT_HISTORY gah WHERE gah.GL_ACCT_NO=? AND TRUNC(SYS_CREATE_TS)=TO_DATE(?,'YYYY-MM-DD')" +
                "AND TXN_CRNCY_ID=? ORDER BY SYS_CREATE_TS DESC";
        try {
//            LOGGER.info(sql.replace("?", "'{}'"), glAcct, reconDate);
            List<LedgerReconDataReq> ltxns = jdbcRUBIKONTemplate.query(sql, new Object[]{glAcct, reconDate, currency},
                    (ResultSet rs, int rowNum) -> {
                        LedgerReconDataReq ltxn = new LedgerReconDataReq();
                        ltxn.setBenAccount(rs.getString("GL_ACCT_NO"));
                        ltxn.setNarration(rs.getString("TRAN_DESC"));
                        ltxn.setDrcrInd(rs.getString("DR_CR_IND"));
                        ltxn.setSourceAcct(rs.getString("GL_ACCT_NO"));
                        ltxn.setTxnType(txnType);
                        try {
                            ltxn.setTransDate(DateUtil.formatDate(rs.getString("SYS_CREATE_TS"), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss"));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (rowNum == 0) {
                            ltxn.setAmount(new BigDecimal(rs.getString("STMNT_BAL").replace("-", "")));
                            ltxn.setExceptionType("LEDGER_CLOSING_BALANCE");
                            ltxn.setStatus("VERIFIED");
                            ltxn.setReference(rs.getString("TRAN_REF_TXT") + "_12");
                            ltxn.setTxnType(txnType);
                            //insert here
                            insertCbsTxnIntoReconTracker(ltxn);
                        }
                        ltxn.setReference(rs.getString("TRAN_REF_TXT"));
                        ltxn.setStatus("PENDING");
                        ltxn.setAmount(rs.getBigDecimal("TXN_AMT"));

                        return ltxn;
                    });
            return ltxns;
        } catch (Exception dae) {
            LOGGER.info("Exception found: ... {}", dae.getMessage());
            return new ArrayList<>();
        }
    }

    public List<LedgerReconDataReq> getGeneralCBSTransactionsForRecon2(String reconDate, GeneralReconConfig config) {
        List<LedgerReconDataReq> list = new ArrayList<>();
        Connection connection;
        try {
            String userName = config.getDatasourceCBSUsername();
            String password = config.getDatasourceCBSPassword();
            String connectionUrl = config.getDatasourceCBSUrl();
            Class.forName(config.getDatasourceCBSDriver());
            connection = DriverManager.getConnection(connectionUrl, userName, password);

            String sql = config.getDatasourceCBSQuery();

            CallableStatement cstmt = connection.prepareCall(sql);
            cstmt.registerOutParameter(1, Types.REF_CURSOR);
            Calendar cal = Calendar.getInstance();
            cstmt.setDate(2, java.sql.Date.valueOf(reconDate));
            cstmt.setDate(3, java.sql.Date.valueOf(reconDate));
            cstmt.executeQuery();
            ResultSet cursor = cstmt.getObject(1, ResultSet.class);

            int i = 0;
            BigDecimal closingBalance = BigDecimal.ZERO;
            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            while (cursor.next()) {
                LedgerReconDataReq ltxn = new LedgerReconDataReq();

                totalCredit = cursor.getBigDecimal("TOTAL_CR_AMT");
                totalDebit = cursor.getBigDecimal("TOTAL_DR_AMT");
                ltxn = new LedgerReconDataReq();
                closingBalance = cursor.getBigDecimal("LEDGER_BAL").abs();
                ltxn.setBenAccount(cursor.getString("GL_ACCT_NO"));
                ltxn.setNarration(cursor.getString("TRAN_DESC"));
                ltxn.setDrcrInd(cursor.getString("DR_CR_IND"));
                ltxn.setSourceAcct(cursor.getString("GL_ACCT_NO"));
                ltxn.setTxnType(config.getTxnType());
                try {
                    ltxn.setTransDate(DateUtil.formatDate(cursor.getString("TRAN_DT"), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                ltxn.setReference(cursor.getString("TRAN_REF_TXT"));
                ltxn.setStatus("PENDING");
                if (ltxn.getDrcrInd() != null) {
                    if (ltxn.getDrcrInd().equalsIgnoreCase("DR")) {
                        ltxn.setAmount(cursor.getBigDecimal("DEBIT_AMT"));
                        totalDebit = totalDebit.add(ltxn.getAmount());
                    }

                    if (ltxn.getDrcrInd().equalsIgnoreCase("CR")) {
                        ltxn.setAmount(cursor.getBigDecimal("CREDIT_AMT"));
                        totalCredit = totalCredit.add(ltxn.getAmount());

                    }
                }
                list.add(ltxn);
                //insert into cbs transactions
                insertIntoCbsTransactions(ltxn);
                i++;
//                closingBalance = cursor.getBigDecimal("STMNT_BAL");
            }
            LedgerReconDataReq ltxn2 = new LedgerReconDataReq();

            //GET bank closing balance
            connection.close();
            //instantiate another connection
            connection = DriverManager.getConnection(connectionUrl, userName, password);
            String sqlBal = "select * from TPBLIVE.GL_DAILY_BALANCE_HISTORY a,TPBLIVE.GL_ACCOUNT b,TPBLIVE.GL_ACCOUNT_SUMMARY gas where B.GL_ACCT_NO=? AND gas.CRNCY_ID =? AND gas.GL_ACCT_SUMMARY_ID =a.GL_ACCT_SUMMARY_ID  and A.GL_ACCT_ID=B.GL_ACCT_ID and  TRUNC(a.BAL_DT)>= to_date(?, 'YYYY-MM-DD')  AND TRUNC(a.BAL_DT)<= to_date(?, 'YYYY-MM-DD')";
            PreparedStatement preparedStatement = connection.prepareStatement(sqlBal);
            preparedStatement.setString(1, config.getGlAcct());
            preparedStatement.setString(2, config.getCurrency());
            preparedStatement.setString(3, reconDate);
            preparedStatement.setString(4, reconDate);
            ResultSet rs2 = preparedStatement.executeQuery();
            while (rs2.next()) {
                closingBalance = rs2.getBigDecimal("LEDGER_BAL").abs();
            }
//            insert closing balance here
//            System.out.println("TOTAL CREDIT AMOUNT:" + totalCredit);
//            System.out.println("TOTAL DEBIT AMOUNT:" + totalDebit);
//            System.out.println("CLOSING BALANCE-FROM STATEMENT:" + closingBalance);
//            closingBalance = closingBalance.add(totalDebit).subtract(totalCredit);
//            System.out.println("CLOSING BALANCE-CALCULATED:" + closingBalance);
//            System.out.println("RECON DATE:" + ltxn2.getTransDate());
            ltxn2.setAmount(closingBalance);
            ltxn2.setTransDate(reconDate);
            ltxn2.setExceptionType("LEDGER_CLOSING_BALANCE");
            ltxn2.setNarration("closing balance as :" + reconDate);
            ltxn2.setTxnType(config.getTxnType());
            ltxn2.setStatus("VERIFIED");
            ltxn2.setDrcrInd("DR");
            ltxn2.setBenAccount(config.getGlAcct());
            ltxn2.setSourceAcct(config.getGlAcct());
            ltxn2.setBenAccount(config.getGlAcct());
            ltxn2.setReference(reconDate + "_clbl");
            ltxn2.setTxnType(config.getTxnType());
            //insert here
            insertCbsTxnIntoReconTracker(ltxn2);

            connection.close();

        } catch (Exception e) {
            LOGGER.info("Exception found: ... {}", e.getMessage());
            LOGGER.info(null, e);

            System.out.println("connection to GW: " + e.getMessage());
        }
        return list;
    }

    private void insertIntoCbsTransactions(LedgerReconDataReq ltxn) {
        String sql = "INSERT IGNORE INTO cbstransactiosn(txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//        LOGGER.info(sql.replace("?","'{}'"), ltxn.getReference(),ltxn.getTxnType(),ltxn.getTxnType(),ltxn.getTransDate(),ltxn.getSourceAcct(),ltxn.getBenAccount(),ltxn.getAmount(),ltxn.getNarration(),"terminal","TZS",ltxn.getStatus(),0.00,ltxn.getSourceAcct(),ltxn.getDrcrInd());
        try {
            jdbcTemplate.update(sql, ltxn.getReference(), ltxn.getTxnType(), ltxn.getTxnType(), ltxn.getTransDate(), ltxn.getSourceAcct(), ltxn.getBenAccount(), ltxn.getAmount(), ltxn.getNarration(), "terminal", "TZS", ltxn.getStatus(), 0.00, ltxn.getSourceAcct(), ltxn.getDrcrInd());
        } catch (DataAccessException ex) {
            LOGGER.info("exception in inserting into cbstxn... {}", ex);
        }
    }


    private void insertIntoThirdpartytxns(BankStReconDataReq ltxn) {
        String sql = "INSERT IGNORE INTO thirdpartytxns(txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, txdestinationaccount, acct_no, status, post_balance, previous_balance, identifier) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

//        LOGGER.info(sql.replace("?","'{}'"), ltxn.getTxnType(),ltxn.getTxnType(),ltxn.getReference(),ltxn.getTransDate(),ltxn.getSourceAcct(),ltxn.getReference(),ltxn.getAmount(),"0.00",ltxn.getNarration(),"TZS",ltxn.getStatus(),ltxn.getBenAccount(),ltxn.getSourceAcct(),ltxn.getStatus(),ltxn.getAmount(),ltxn.getAmount(),ltxn.getDrcrInd());
        try {
            jdbcTemplate.update(sql, new Object[]{ltxn.getTxnType(), ltxn.getTxnType(), ltxn.getReference(), ltxn.getTransDate(), ltxn.getSourceAcct(), ltxn.getReference(), ltxn.getAmount(), "0.00", ltxn.getNarration(), "TZS", ltxn.getStatus(), ltxn.getBenAccount(), ltxn.getSourceAcct(), ltxn.getStatus(), ltxn.getAmount(), ltxn.getAmount(), ltxn.getDrcrInd()});
        } catch (DataAccessException ex) {
            LOGGER.info("exception in inserting into thirdpartytxns... {}", ex);
        }
    }

    public List<BankStReconDataReq> getGeneralBankStatementTransactionsForRecon(String reconDate, GeneralReconConfig config) {
        String sql = config.getDatasourceQuery();
        sql = sql.replace("{TXN_TYPE}", "'" + config.getTxnType() + "'");
        sql = sql.replace("{TXN_DATE}", "'" + reconDate + "'");
        List<BankStReconDataReq> ltxns = new ArrayList<>();
        Connection connection;
        try {
            String userName = config.getDatasourceUsername();
            String password = config.getDatasourcePassword();
            String connectionUrl = config.getDatasourceUrl();
            Class.forName(config.getDatasourceDriver());
            connection = DriverManager.getConnection(connectionUrl, userName, password);

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            if (config.getTxnType().contains("BOT")) {
                preparedStatement.setString(1, reconDate);
                LOGGER.info(sql.replace("?", "'{}'"), reconDate);
            } else {
                preparedStatement.setString(1, config.getTxnType());
                preparedStatement.setString(2, config.getTxnType());
                preparedStatement.setString(3, reconDate);
                LOGGER.info(sql.replace("?", "'{}'"), config.getTxnType(), config.getTxnType(), reconDate);
            }

            ResultSet rs = preparedStatement.executeQuery();
            int i = 0;
            while (rs.next()) {
                BankStReconDataReq ltxn = new BankStReconDataReq();
                ltxn.setBenAccount(rs.getString("txdestinationaccount"));
                ltxn.setNarration(rs.getString("description"));
                ltxn.setDrcrInd(rs.getString("identifier"));
                ltxn.setTxnType(config.getTxnType());
                if (rs.getString("identifier").equalsIgnoreCase("IN") ||
                        rs.getString("identifier").equalsIgnoreCase("C")) {
                    ltxn.setDrcrInd("CR");
                    ltxn.setReference(rs.getString("receiptNo"));
                }
                if (rs.getString("identifier").equalsIgnoreCase("OUT") ||
                        rs.getString("identifier").equalsIgnoreCase("D")) {
                    ltxn.setDrcrInd("DR");
                    ltxn.setReference(rs.getString("txnid"));
                }
                ltxn.setSourceAcct(rs.getString("sourceAccount"));
                try {
                    ltxn.setTransDate(DateUtil.formatDate(rs.getString("txndate"), "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (i == 0 && !config.getTxnType().contains("BOT")) {
                    ltxn.setAmount(rs.getBigDecimal("closingBalance"));
                    ltxn.setStatus("VERIFIED");
                    ltxn.setExceptionType("BANK_CLOSING_BALANCE");
                    ltxn.setReference(ltxn.getReference());
                    ltxn.setTransDate(rs.getString("txndate"));
                    ltxn.setTxnType(config.getTxnType());
                    //insert
                    insertIntoReconTracker(ltxn);
                }
                ltxn.setStatus("PENDING");
                ltxn.setAmount(rs.getBigDecimal("amount"));
                if (config.getTxnType().contains("BOT")) {
                    ltxn.setTransDate(rs.getString("txndate"));
                }
                ltxns.add(ltxn);
//                LOGGER.info("BankStReconDataReq: {}", ltxn);
                insertIntoThirdpartytxns(ltxn);
                i++;
            }
//            LOGGER.info("BANK STATEMENT DTA.. {}", ltxns);
        } catch (Exception e) {
            LOGGER.info("Exception found: ... {}", e.getMessage());
            System.out.println("connection to GW: " + e.getMessage());
        }

        return ltxns;
    }

    public String getReconSummaryReport(String exporterFileType, HttpServletResponse response, String destName, String txnDate, String printedBy) {
//        LOGGER.info("RECON DATA: RECON DATE:{} TXN_TYPE:{} TTYPE:{}", txnDate, txnType, ttype);
        String reportFileTemplate;
        if (exporterFileType.equalsIgnoreCase("excel")) {
            reportFileTemplate = "/iReports/recon/general-bank-recon.jasper";
        } else {
            reportFileTemplate = "/iReports/recon/general-bank-recon-summary.jasper";
        }
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PRINTED_BY", printedBy);
            parameters.put("PRINTED_DATE", DateUtil.now());
            parameters.put("BANK_ACCOUNT", httpSession.getAttribute("glAcct"));
            parameters.put("TXN_TYPE", httpSession.getAttribute("txnType").toString());
            parameters.put("TTYPE", httpSession.getAttribute("txnType").toString());
            parameters.put("ACCOUNT_NAME", httpSession.getAttribute("txnType").toString());
            parameters.put("RECON_DATE", httpSession.getAttribute("txndate"));
            LOGGER.info("PARAMETERS FOR GENERAL RECON... {}", parameters);
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, jdbcTemplate.getDataSource().getConnection());
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (IOException ex) {
            LOGGER.info("Input Output Exception found in generating report.... {}", ex);
            return null;

        } catch (Exception ex) {
            LOGGER.info("Exception found in generating report2.... {}", ex);
            return null;

        }

    }

    public void insertIntoReconTracker(BankStReconDataReq txn) {
        String sql = "INSERT IGNORE INTO reconciliation_tracker (created_by, created_date, last_modified_by, last_modified_date, rec_status, account, amount, reconTtype, closedBy, closedDate, initiatedBy, initiatedDate, mirrorAccount, narration, reconDate, reconType, status, transDate, transReference, exceptionType) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            this.jdbcTemplate.update(sql, new Object[]{"SYSTEM", DateUtil.now(), "SYSTEM_USER", null, txn.getStatus(), txn.getSourceAcct(), txn.getAmount(), txn.getTxnType(), null, null, null, null, txn.getBenAccount(), txn.getNarration(), txn.getTransDate(), txn.getTxnType(), txn.getStatus(), txn.getTransDate(), txn.getReference(), txn.getExceptionType()});
        } catch (DataAccessException dae) {
//            LOGGER.info("Data access exception.. {}", dae);
        }
    }

    public void insertCbsTxnIntoReconTracker(LedgerReconDataReq txn) {
        String sql = "INSERT IGNORE INTO reconciliation_tracker (created_by, created_date, last_modified_by, last_modified_date, rec_status, account, amount, reconTtype, closedBy, closedDate, initiatedBy, initiatedDate, mirrorAccount, narration, reconDate, reconType, status, transDate, transReference, exceptionType) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            this.jdbcTemplate.update(sql, new Object[]{"SYSTEM", DateUtil.now(), "SYSTEM_USER", null, txn.getStatus(), txn.getSourceAcct(), txn.getAmount(), txn.getTxnType(), null, null, null, null, txn.getBenAccount(), txn.getNarration(), txn.getTransDate(), txn.getTxnType(), txn.getStatus(), txn.getTransDate(), txn.getReference(), txn.getExceptionType()});
        } catch (DataAccessException dae) {
//            LOGGER.info("CBS Data access exception.. {}", dae.getMessage());
        }
    }

    public String fireGetTipsTxnsReconReport(String exceptionType, String reconDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String txnType = httpSession.getAttribute("txnType").toString();
        LOGGER.info("REPORT FOR TXN TYPE... {}", txnType);
        mainSql = "select count(rt.transReference) from reconciliation_tracker rt  where (initiatedDate is null or date(initiatedDate) > ? or date(closedDate) > ?) and (status ='PENDING' OR status ='SUCCESS') and reconType=? and date(transDate) <= ? and exceptionType=? ";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{reconDate, reconDate, txnType, reconDate + " 23:59:59", exceptionType}, Integer.class);
        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = " WHERE concat(transReference,' ',transDate,' ',narration,' ',amount,' ',exceptionType,' ',account,' ',mirrorAccount,' ',reconType,' ') LIKE ? AND (initiatedDate is null or date(initiatedDate) > ? or date(closedDate) > ?) and (status ='PENDING' OR status ='SUCCESS') and rt.exceptionType=? and rt.reconType=? and date(transDate) <= ? ";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(rt.transReference) FROM reconciliation_tracker rt " + searchQuery, new Object[]{searchValue, reconDate, reconDate, exceptionType, txnType, reconDate + " 23:59:59"}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select id, reconType,transReference, transDate, account, mirrorAccount, narration, amount, exceptionType from reconciliation_tracker rt " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, reconDate, reconDate, exceptionType, txnType, reconDate + " 23:59:59"});

        } else {
            mainSql = "select id, reconType,transReference, transDate, account, mirrorAccount, narration, amount, exceptionType from reconciliation_tracker rt where (initiatedDate is null or date(initiatedDate) > ? or date(closedDate) > ?) and (status ='PENDING' OR status ='SUCCESS') and rt.reconType=? and rt.exceptionType=? and date(transDate) <= ? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            LOGGER.info(mainSql.replace("?", "'{}'"), reconDate, reconDate, txnType, exceptionType, reconDate + " 23:59:59");
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{reconDate, reconDate, txnType, exceptionType, reconDate + " 23:59:59"});
        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    //confirm bulk TIPS transactions
    public List<Map<String, Object>> fireConfirmBulkTipsTxns(String txnid, String txnType) {
        List<Map<String, Object>> finalRes = null;
        String query = "select * from thirdpartytxns where  (txn_type=? or ttype=?)  and (receiptNo IN (" + txnid + ") or txnid IN (" + txnid + ")) ";
        finalRes = this.jdbcTemplate.queryForList(query, txnType, txnType);
//        LOGGER.info("Transactions result on thirdpart side confirmations ...{}", finalRes);
        return finalRes;
    }

    //confirm bulk TIPS transactions on CBS
    public List<Map<String, Object>> fireConfirmBulkGeneralTxnsCBS(String txnid, String glAcct) {
        List<Map<String, Object>> finalRes = null;

        String query = "SELECT  amount, sourceaccount,amount, txndate AS transDate,dr_cr_ind,txnid AS transReference,description AS narration " +
                "FROM cbstransactiosn c WHERE c.sourceaccount=? AND txnid IN(" + txnid + ")";
        LOGGER.info(query.replace("?", "'{}'"), glAcct);
        try {
            finalRes = jdbcTemplate.queryForList(query, glAcct);
        } catch (DataAccessException e) {
            LOGGER.info("Cbs access error", e);
        }
//        LOGGER.info("Transactions result on CBS Side for confirmation ...{}", finalRes);

        return finalRes;
    }

    public String solveTIPSBulkAmbiguousTxns(String resolvedBy, String exceptionType, String ambiguousType, String inLists, String reconDate) {
        int res = 0;
        String finalResponse = "whoops";
        String sql = null;
        String reconType = httpSession.getAttribute("txnType").toString();
        String status = "whoops";
        if (ambiguousType.equals("1")) {
            status = "SUCCESS";
            sql = "UPDATE reconciliation_tracker SET status=?, initiatedBy=?, initiatedDate=?, closedDate = ? WHERE reconType=? AND transReference IN (" + inLists + ")";
//                LOGGER.info(sql.replace("?","'{}'"),status, resolvedBy, DateUtil.now(), reconType);
            res = this.jdbcTemplate.update(sql, status, resolvedBy, reconDate, reconDate, reconType);
        }
        if (res == 1) {
            finalResponse = "{\"responseCode\": 0, \"message\": \"Success\"}";
        } else {
            finalResponse = "{\"responseCode\": -1, \"message\": \"failed\"}";
        }
        return finalResponse;
    }

    public List<GeneralReconConfig> getReconConfigs() {
        try {
            String query = "select * from general_reconciliation_configs where display_status='A'";
            List<GeneralReconConfig> reconConfigs = this.jdbcTemplate.query(query, (ResultSet rs, int rowNum) -> {
                GeneralReconConfig config = new GeneralReconConfig();
                config.setGlAcct(rs.getString("glAcct"));
                config.setTxnType(rs.getString("txnType"));
                config.setTType(rs.getString("tType"));
                config.setCurrency(rs.getString("currency"));
                config.setDatasourceUrl(rs.getString("datasourceUrl"));
                config.setDatasourceDriver(rs.getString("datasourceDriver"));
                config.setDatasourceUsername(rs.getString("datasourceUsername"));
                config.setDatasourcePassword(rs.getString("datasourcePassword"));
                config.setDatasourceQuery(rs.getString("datasourceQuery"));
                config.setDatasourceCBSUrl(rs.getString("datasourceCBSUrl"));
                config.setDatasourceCBSDriver(rs.getString("datasourceCBSDriver"));
                config.setDatasourceCBSUsername(rs.getString("datasourceCBSUsername"));
                config.setDatasourceCBSPassword(rs.getString("datasourceCBSPassword"));
                config.setDatasourceCBSQuery(rs.getString("datasourceCBSQuery"));
                config.setThirdPartyAcct(rs.getString("third_party_account"));
                return config;
            });
            return reconConfigs;
        } catch (DataAccessException dae) {
            LOGGER.info("Data access exception for general recon configs... {}", dae);
            return null;
        }
    }

    public List<GeneralReconConfig> getReconConfigs(String roleId) {
        try {
            String query = "select * from general_reconciliation_configs grc inner join general_reconciliation_config_roles grcr on grc.id = grcr.grc_id where display_status='A' and grcr.role_id = ?";
            List<GeneralReconConfig> reconConfigs = this.jdbcTemplate.query(query, (ResultSet rs, int rowNum) -> {
                GeneralReconConfig config = new GeneralReconConfig();
                config.setGlAcct(rs.getString("glAcct"));
                config.setTxnType(rs.getString("txnType"));
                config.setTType(rs.getString("tType"));
                config.setDatasourceUrl(rs.getString("datasourceUrl"));
                config.setDatasourceDriver(rs.getString("datasourceDriver"));
                config.setDatasourceUsername(rs.getString("datasourceUsername"));
                config.setDatasourcePassword(rs.getString("datasourcePassword"));
                config.setDatasourceQuery(rs.getString("datasourceQuery"));
                config.setThirdPartyAcct(rs.getString("third_party_account"));
                return config;
            }, roleId);
            return reconConfigs;
        } catch (DataAccessException dae) {
            LOGGER.info("Data access exception for general recon configs... {}", dae);
            return null;
        }
    }

    public String getReconGl(String txnType) {
        return jdbcTemplate.queryForObject("SELECT glAcct from general_reconciliation_configs where txnType=?", new String[]{txnType}, String.class);
    }

    public List<Map<String, Object>> fireGeneralTxnReportAjax(String direction, String fromDate, String toDate) {
        String mainSql;
        List<Map<String, Object>> results = null;
        String txnType = httpSession.getAttribute("txnType").toString();
        String glAcct = httpSession.getAttribute("glAcct").toString();
        switch (direction) {
            case "STATEMENT":
                //search on thirdparttxns
                mainSql = "select sourceAccount as ACCOUNT , amount as AMOUNT,txndate as TXNDATE,identifier as IDENTIFIER, receiptNo as RECEIPTNO, currency as CURRENCY, txdestinationaccount as DESTINATION_ACCT from thirdpartytxns where txn_type=? or ttype=? and date(txndate)>=? and date(txndate)<=?";
                LOGGER.info(mainSql.replace("?", "'{}'"), txnType, txnType, fromDate, toDate);
                results = jdbcTemplate.queryForList(mainSql, new Object[]{txnType, txnType, fromDate, toDate});
                break;
            default:
                mainSql = "SELECT  GL_ACCT_NO AS ACCOUNT,TXN_AMT AS AMOUNT,SYS_CREATE_TS AS TXNDATE,DR_CR_IND AS IDENTIFIER,TRAN_REF_TXT AS RECEIPTNO, CONTRA_ACCT_NO AS  DESTINATION_ACCT " +
                        "FROM GL_ACCOUNT_HISTORY gah WHERE gah.GL_ACCT_NO=? AND TRUNC(SYS_CREATE_TS)>=TO_DATE(?,'YYYY-MM-DD') AND TRUNC(SYS_CREATE_TS)<=TO_DATE(?,'YYYY-MM-DD')";
                LOGGER.info(mainSql.replace("?", "'{}'"), glAcct, fromDate, toDate);
                results = jdbcRUBIKONTemplate.queryForList(mainSql, new Object[]{glAcct, fromDate, toDate});
        }
        return results;
    }

    public String fireDownloadCsbDataForRecontAjax(String mno, String txnType, String txnDate) {
        String queryWith = "code";
        String argument = mno;
        if (mno.equalsIgnoreCase("ALL")) {
            queryWith = "ttype";
            argument = txnType;
        } else {
            queryWith = "code";
            argument = mno;
        }
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList("SELECT * FROM txns_types WHERE " + queryWith + "=?", argument);
        LOGGER.info("List of map for sync in rubikon data .... {}", mapList);

        if (!mapList.isEmpty()) {
            for (Map<String, Object> map : mapList) {
                TxnsCBSDownloader downloadHelper = new TxnsCBSDownloader();
                downloadHelper.setTxnType(map.get("code").toString());
                downloadHelper.setTtype(map.get("ttype").toString());
                downloadHelper.setIsW2B(Boolean.parseBoolean(map.get("isW2B").toString()));
                downloadHelper.setIsB2W(Boolean.parseBoolean(map.get("isB2W").toString()));
                downloadHelper.setDburl(map.get("dburl").toString());
                downloadHelper.setDbusername(map.get("dbusername").toString());
                downloadHelper.setDbpassword(map.get("dbpassword").toString());
                downloadHelper.setDbDriverClassName(map.get("dbdriverClassName").toString());
                downloadHelper.setCbsQuery(map.get("queryD").toString());
                downloadHelper.setCbsAcct(map.get("cbs_account").toString());
                downloadHelper.setDbaccturl(map.get("dbaccturl").toString());
                downloadHelper.setDbacctusername(map.get("dbacctusername").toString());
                downloadHelper.setDbacctpassword(map.get("dbacctpassword").toString());
                downloadHelper.setDbacctDriverClassName(map.get("dbacctdriverClassName").toString());
                downloadHelper.setThirdPartyQuery(map.get("queryAcct").toString());
                downloadHelper.setLastPTID(map.get("lastPtid").toString());
                downloadHelper.setIsAllwed(Boolean.parseBoolean(map.get("isAllowed").toString()));
                downloadHelper.setJdbcTemplate(jdbcTemplate);
                exec.execute(downloadHelper);
            }
        }
        return "{\"result\":" + 200 + ",\"message\":\"Data downloaded. \"}";
    }

//    public List<Map<String, Object>> getNotInCbsTransactions(String txnType, String ttype, String txndate, String exceptionType) {
//        List<Map<String, Object>> results = null;
//        String mainSql = null;
//        LOGGER.info("ttype:{}, exceptionType:{}", txnType, exceptionType);
//            if(ttype.equalsIgnoreCase("WALLET2MKOBA") && exceptionType.equals("0")) {
//                LOGGER.info("Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txdestinationaccount like '1112%' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
//            }else if(ttype.equalsIgnoreCase("WALLET2MKOBA") && exceptionType.equals("1")) {
//                    LOGGER.info("Txn type... {} and exception type...{}", txnType, exceptionType);
//                    mainSql = "SELECT txn_type,ttype,txnid, CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txdestinationaccount like '1732%' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
//            }else if (ttype.equalsIgnoreCase("WALLET2MKOBA") && exceptionType.equals("2")){
//                LOGGER.info("Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txdestinationaccount = '' and txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
//            }else if(ttype.equalsIgnoreCase("WALLET2MKOBA") && exceptionType.equals("3")){
//                LOGGER.info("Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"});
//            }else if(ttype.equalsIgnoreCase("WALLET2VIKOBA") && (exceptionType.equals("0")||exceptionType.equals("2"))){
//                LOGGER.info("VIKOBA THINGS IN MIND Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
//            }else if(ttype.equalsIgnoreCase("WALLET2VIKOBA") && exceptionType.equals("1")){
//                LOGGER.info("VIKOBA THINGS IN MIND Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid, CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
//            }else if(ttype.equalsIgnoreCase("WALLET2VIKOBA") && exceptionType.equals("3")){
//                LOGGER.info("VIKOBA THINGS IN MIND Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE  txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59", txnType, ttype, txndate + " 23:30:00", txndate + " 23:59:59"});
//            }else{
//                LOGGER.info("Txn type... {} and exception type...{}", txnType, exceptionType);
//                mainSql = "SELECT txn_type,ttype,txnid,CAST(txndate AS char) as txndate,sourceAccount sourceaccount,receiptNo,amount,charge,description,currency,mnoTxns_status,terminal,txdestinationaccount destinationaccount,acct_no,status,post_balance,previous_balance,file_name,pan,docode,docode_desc FROM thirdpartytxns WHERE txn_type=? and ttype=? and txndate>=? and txndate<=? and mnoTxns_status  like '%success%' and trim(txnid) not in (SELECT trim(txnid) from cbstransactiosn where txn_type=? and ttype=? and txndate>=? and txndate<=?)";
//                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59", txnType, ttype, txndate + " 00:00:00", txndate + " 23:59:59"});
//            }
//
//        return results;
//    }

    public void insertVikobaTIPSB2CTxns() {
        String toDate = DateUtil.now("yyyy-MM-dd");
        String sql = "INSERT INTO thirdpartytxns (\n" +
                "    txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, description,\n" +
                "    currency, mnoTxns_status, txdestinationaccount, post_balance, previous_balance,\n" +
                "    identifier, log_date\n" +
                ") \n" +
                "SELECT \n" +
                "    'AVIKOBA2WALLET', 'AIRTELVIKOBA', txnid, txndate, sourceAccount, CONCAT(' ', receiptNo),\n" +
                "    amount, description, currency, mnoTxns_status, txdestinationaccount,\n" +
                "    post_balance, previous_balance, identifier, current_timestamp()\n" +
                "FROM \n" +
                "    thirdpartytxns \n" +
                "WHERE \n" +
                "    ttype = 'TIPS' \n" +
                "    AND identifier = 'OUT' \n" +
                "    AND txnid LIKE '%048-V3-%' \n" +
                "    AND txndate >= ?";
        try {
            LOGGER.info(sql.replace("?", "'{}'"), toDate);
            this.jdbcTemplate.update(sql, new Object[]{toDate});
        } catch (DataAccessException dae) {
//            LOGGER.info("CBS Data access exception.. {}", dae.getMessage());
        }
    }

    public void downloadFromTransfersToPensionerPayroll() {
        List<Map<String, Object>> configs = jdbcTemplate.queryForList("select * from pensioner_payroll_senders");
        for (int count = 0; count < configs.size(); count++) {
            String transQuery = "select txid, 'NSSF', beneficiaryName, beneficiaryName, '90' ,'TZS',amount, destinationAcct, 'TAPBTZTZ', 'TAPBTZTZ', txid, batch_reference,txid,concat('MAIN ', purpose) as sababu,'SYSTEM',create_dt,'A',response_code,message,comments, cbs_status,'SYSTEM',create_dt,'SYSTEM',create_dt,0,month(create_dt) as mwezi,year(create_dt) as mwaka,reference from transfers where pensioner_payroll='0' and sender_name =?";
            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(transQuery, configs.get(count).get("name"));
            if (transactions != null) {
                for (int i = 0; i < transactions.size(); i++) {
                    String sql = "insert ignore into pensioners_payroll (trackingNo, institution_id, name, cbs_name, percentage_match, currency, amount, account, channel_identifier, bankCode, pensioner_id, batchReference, bankReference, description, created_by, create_dt, status, responseCode, message, comments, cbs_status, verified_by, verified_dt, approved_by, approved_dt, od_loan_status, payroll_month, payroll_year) VALUE(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    jdbcTemplate.update(sql, transactions.get(i).get("txid"), "NSSF", transactions.get(i).get("beneficiaryName"), transactions.get(i).get("beneficiaryName"), "90", "TZS", transactions.get(i).get("amount"), removePrefix(transactions.get(i).get("destinationAcct")+""), "TAPBTZTZ", "TAPBTZTZ", transactions.get(i).get("txid"), transactions.get(i).get("batch_reference"), transactions.get(i).get("txid"), transactions.get(i).get("sababu"), "SYSTEM", transactions.get(i).get("create_dt"), "A", transactions.get(i).get("response_code"), transactions.get(i).get("message"), transactions.get(i).get("comments"), transactions.get(i).get("cbs_status"), "SYSTEM", transactions.get(i).get("create_dt"), "SYSTEM", transactions.get(i).get("create_dt"), "0", transactions.get(i).get("mwezi"), transactions.get(i).get("mwaka"));
                    jdbcTemplate.update("update transfers set pensioner_payroll= '1' where reference=?", new Object[]{transactions.get(i).get("reference")});
                }
            }
        }
    }

    public static String removePrefix(String accountNumber) {
        if (accountNumber != null && accountNumber.startsWith("000")) {
            return accountNumber.substring(3);
        }
        return accountNumber;
    }
}
