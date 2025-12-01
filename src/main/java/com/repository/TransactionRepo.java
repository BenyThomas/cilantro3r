/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.CashMovementRequestObj;
import com.DTO.GeneralJsonResponse;
import com.DTO.GetmnoBalancesResponse;
import com.DTO.Teller.FundTransferReq;
import com.DTO.Teller.VikobaUpdateRequest;
import com.DTO.psssf.LoanVerificationReq;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.helper.DateUtil;
import com.service.CorebankingService;
import com.service.HttpClientService;
import com.service.TransferService;
import com.service.XMLParserService;
import com.service.XapiWebService;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import philae.api.PostGLToGLTransfer;
import philae.api.UsRole;
import philae.api.XaResponse;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class TransactionRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcReconTemplate;

    @Autowired
    @Qualifier("gwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    @Qualifier("mkobadb")
    JdbcTemplate jdbcMkobaTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    HttpSession httpSession;

    @Autowired
    @Qualifier("cbsConnection")
    HikariDataSource coreBankingDatasource;

    @Autowired
    @Qualifier("mkobaConnection")
    HikariDataSource mkobaDatasource;

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;

    @Autowired
    private Environment activeEnv;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    @Qualifier("gwAirtelVikobaDBConnection")
    JdbcTemplate jdbcAirtelVikobaTemplate;

    @Autowired
    TransferService transferService;

    @Autowired
    SYSENV sysenv;

    @Autowired
    @Qualifier("gwConnection")
    HikariDataSource gwDatasource;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TransactionRepo.class);

    @Qualifier("gwBrinjalDbConnection")
    @Autowired
    private JdbcTemplate gwBrinjalDbConnection;

    public String getGwSearchTransactions(String input, String searchon) {
        //check condition.....
        List<Map<String, Object>> findAll;
        if (searchon.equalsIgnoreCase("mkoba")) {
            String mainSql = "select transid txid,msisdn,receipt txReceipt,transate txdate,transamount txamount,sourceaccount txsourceAccount,destinationaccount txdestinationAccount,transstatus txstatus,transstatus txstatusdesc from vg_group_transaction where msisdn like '%" + input + "%' OR receipt like '%" + input + "%' OR transid like '%" + input + "%' OR sourceaccount like '%" + input + "%' OR destinationaccount like '%" + input + "%'  ";
            mainSql += " UNION " +
                    "select transid txid,msisdn,receipt txReceipt,transate txdate,transamount txamount,sourceaccount txsourceAccount,destinationaccount txdestinationAccount,transstatus txstatus,transstatus txstatusdesc from vg_group_transaction_archive where msisdn like '%" + input + "%' OR receipt like '%" + input + "%' OR transid like '%" + input + "%' OR sourceaccount like '%" + input + "%' OR destinationaccount like '%" + input + "%' ";
            mainSql += " UNION " +
                    "select ThirdPartyReference as txid,'0' as msisdn,receipt as txReceipt, '' as  txdate,'N/A' txamount,'N/A' AS txsourceAccount,'N/A' AS txdestinationAccount,'0' AS txstatus,'success in queue' as txstatusdesc from tp_deposit_retry where ThirdPartyReference like '%" + input + "%' OR receipt like '%" + input + "%'";
            findAll = this.jdbcMkobaTemplate.queryForList(mainSql);
        } else if (searchon.equalsIgnoreCase("rubikon")) {
            //cbstransactiosn(txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?";

            String mainSql = "select txnid as txid  from cbstransactiosn  where txnid like '%" + input + "%' OR txnid like '%" + input + "%' OR txid like '" + input + "' OR sourceaccount like '%" + input + "%' OR destinationaccount like '%" + input + "%' order by txndate desc limit 1000";
            findAll = this.jdbcReconTemplate.queryForList(mainSql);

        } else if (searchon.equalsIgnoreCase("gateway")) {
            String mainSql = "select * from tp_transaction  where msisdn like '%" + input + "%' OR txReceipt like '%" + input + "%' OR txid like '%" + input + "%' OR txsourceAccount like '%" + input + "%' OR txdestinationAccount like '%" + input + "%' order by txdate desc limit 1000";
            findAll = this.jdbcTemplate.queryForList(mainSql);

        } else {
            //default is gateway
            String mainSql = "select * from tp_transaction  where msisdn like '%" + input + "%' OR txReceipt like '%" + input + "%' OR txid like '%$input%' OR txsourceAccount like '%" + input + "%' OR txdestinationAccount like '%" + input + "%' order by txdate desc limit 1000";
            findAll = this.jdbcTemplate.queryForList(mainSql);

        }

        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
            //LOGGER.info("RequestBody");
        } catch (JsonProcessingException ex) {
            LOGGER.info("RequestBody-Search Transaction exception: {}", ex);
        }
        String json = jsonString;
        return json;
    }

    public List<Map<String, Object>> getLookupRegistrationOnGW(String msisdn) {
        return this.jdbcTemplate.queryForList("Select a.msisdn,a.account,a.accountName,a.status,a.custtype,b.custstatus,b.imsi from tp_subscriber_account a join tp_subscriber b  on a.msisdn=b.msisdn  where a.msisdn=?", msisdn);
    }

    public List<Map<String, Object>> getLookupRegistrationOnRubikon(String msisdn) {
        return this.jdbcRUBIKONTemplate.queryForList("SELECT a.REC_ST status,b.REC_ST status2,a.ACCESS_CD msisdn,a.QUIZ_CD imsi, b.SHORT_NAME accessName,c.ACCT_NO acctNo, c.ACCT_NM acctName,c.REC_ST acctStatus FROM CUSTOMER_CHANNEL_USER a,CUST_CHANNEL_ACCOUNT b,ACCOUNT c\n"
                + "WHERE ACCESS_CD =? AND a.CUST_ID =b.CUST_ID AND b.CHANNEL_ID =9 AND c.ACCT_ID =b.ACCT_ID AND a.CUST_ID =c.CUST_ID ", msisdn);
    }

    public List<Map<String, Object>> getLast10TransactionsGW(String msisdn) {
        return this.jdbcTemplate.queryForList("select txid txnid,txdate txndate,txReceipt reference, txsourceAccount sourceAcct,txdestinationAccount destinationAcct,txdestinationType description, txdestinationName serviceName, txstatusdesc,txamount amount from ers.tp_transaction tt where msisdn LIKE '%" + msisdn + "%' order by txndate desc limit 10");
    }

    public String getAccountDetails(String accountNo) {
        List<Map<String, Object>> findAll;
        String mainSql = "select c.cust_no,c.cust_nm, a.acct_no,\n"
                + "(select AD.ADDR_LINE_1 from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id )  ADDR_LINE_1,\n"
                + "(select AD.ADDR_LINE_2 from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id )  ADDR_LINE_2,\n"
                + "(select AD.ADDR_LINE_3 from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id ) ADDR_LINE_3,\n"
                + "(select AD.ADDR_LINE_4 from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id ) ADDR_LINE_4, cu.crncy_cd, DAS.CLEARED_BAL\n"
                + " from account a, customer c, currency cu, deposit_account_summary das\n"
                + "where a.cust_id=c.cust_id\n"
                + "and CU.CRNCY_ID=A.CRNCY_ID\n"
                + "and das.acct_no=a.acct_no\n"
                + "and (a.acct_no='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')"; //"select * from tp_transaction  where msisdn like '%" + input + "%' OR txReceipt like '%" + input + "%' OR txid like '%$input%' OR txsourceAccount like '%" + input + "%' OR txdestinationAccount like '%" + input + "%' order by txdate desc limit 1000";
        findAll = this.jdbcRUBIKONTemplate.queryForList(mainSql);
        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
            //LOGGER.info("RequestBody");
        } catch (JsonProcessingException ex) {
            LOGGER.info("EXCEPTION ON GETTING ACCOUNT DETAILS: ", ex);
        }
        String json = jsonString;
        return json;
    }

    //log the TISS TRANSACTIONS
    @Async("dbPoolExecutor")
    @Transactional
    public Integer insertOutgoingTransaction(String txnid, String txndate, String senderAccount, String beneficiaryAccount, String amount, String senderName, String beneficiaryName, String description, String status, String cbs_status, String app_status, String beneficiaryBIC, String SenderBIC, String supporting_doc) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT  INTO rtgs_eft_payments(txnid, txndate, senderAccount, beneficiaryAccount, amount, senderName, beneficiaryName, description, status, cbs_status, app_status, beneficiaryBIC, SenderBIC,supporting_doc)",
                    txnid, txndate, senderAccount, beneficiaryAccount, amount, senderName, beneficiaryName, description, status, cbs_status, app_status, beneficiaryBIC, SenderBIC, supporting_doc);
        } catch (DataAccessException e) {
            LOGGER.info(e.getMessage());
            result = -1;
        }
        return result;
    }

    //sync transactions not in rubikon
    public String syncTxnsNotInRubikon(String txnid, String sourceAcct, String destinationAcct, String txn_type, String ttype) {
        String sourceAccount = "-1";
        String docode = "-1";
        String destinationAccount = "-1";
        int result = -1;
//            String sqlAcctStatus = "SELECT AC.ACCT_NO,AC.ACCT_NM,CUR.CRNCY_CD_ISO AS CURRENCY_CODE,AC.REC_ST,ASR.REF_DESC ACCOUNT AC JOINpartnerSummaryCURRENCY CUR ON AC.CRNCY_ID = CUR.CRNCY_ID JOIN CCOUNT_STATUS_REF ASR ON AC.REC_ST =ASR.REF_KEY WHERE (REPLACE(AC.OLD_ACCT_NO,'-','') = ? OR AC.ACCT_NO=?) OR (REPLACE(AC.OLD_ACCT_NO,'-','') = ? OR AC.ACCT_NO=?) AND ROWNUM = 1";
//            List<Map<String, Object>> getData = this.jdbcRUBIKONTemplate.queryForList(sqlAcctStatus, sourceAcct.replace("-", ""), destinationAcct.replace("-", ""),sourceAcct.replace("-", ""), destinationAcct.replace("-", ""));
//            if (!getData.isEmpty()) {

        String query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
                + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO contra_account,\n"
                + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
                + "gah.STMNT_BAL postBalance\n"
                + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE (gah.ACCT_NO in (" + sourceAcct + ") or gah.ACCT_NO in (" + destinationAcct + ")) and gah.TRAN_DESC not like '%B2C Charge%' and gah.tran_ref_txt in (" + txnid + ")";
        //LOGGER.info(query);
        if (txn_type.equals("LUKU")) {
            query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
                    + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO contra_account,\n"
                    + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
                    + "gah.STMNT_BAL postBalance\n"
                    + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.ACCT_NO in ('180208000016')  and gah.TRAN_DESC not like '%B2C Charge%' and gah.tran_ref_txt in (" + txnid + ")";

        }

//            if (txn_type.equals("MKOBA2WALLET")) {
//                query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
//                        + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO contra_account,\n"
//                        + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
//                        + "gah.STMNT_BAL postBalance\n"
//                        + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.CONTRA_ACCT_NO in ('1-060-00-1102-1102114')  and gah.TRAN_DESC not like '%B2C Charge%' and gah.tran_ref_txt in (" + txnid + ")";
//            }

//            if (txn_type.equals("WALLET2MKOBA")) {
//                query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
//                        + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO contra_account,\n"
//                        + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
//                        + "gah.STMNT_BAL postBalance\n"
//                        + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE gah.CONTRA_ACCT_NO in ('1-060-00-1102-1102113')  and gah.TRAN_DESC not like '%B2C Charge%' and gah.tran_ref_txt in (" + txnid + ")";
//            }

        Connection conn = null;
        try {
            conn = coreBankingDatasource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();
            String txnStatus = "Failed";
            LOGGER.info(query);
            while (rs.next()) {
                String dr_cr_ind = rs.getString("status");

                if (dr_cr_ind.equals("DR")) {
                    dr_cr_ind = "CR";
                }

                if (dr_cr_ind.equals("CR")) {
                    dr_cr_ind = "DR";
                }

                if (checkIfCBSTransFound(rs.getString("txnid"), ttype, dr_cr_ind)) {
                    LOGGER.info(" CBSTrans Found in txind: {}, {}, {}", rs.getString("txnid"), ttype, dr_cr_ind);
                    continue;
                }

                if (ttype.contains("B2C") || ttype.contains("UTILITY") || txn_type.contains("EZYPESA")) {
                    //GATEWAY TRANSACTION
                    query = "select txsourceAccount sourceAcct,txdestinationAccount destinationAcct,txtype as docode from tp_transaction where txid=?";
                    sourceAccount = getAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
                    destinationAccount = getDestinationAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
                    LOGGER.info("in B2C OR UTILITY destinationAccount: {} = {}", destinationAccount, query);
                    if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "success";
                    }
                    if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    if ("A".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Success";
                    }
                    if ("R".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    if (txn_type.equals("LUKU")) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "success";
                        }
                    }
                }
                if (ttype.contains("C2B")) {
                    LOGGER.info("in C2B destinationAccount: {} = {}", destinationAccount, query);

                    if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "success";
                    }
                    if ("A".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Success";
                    }
                    if ("R".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    query = "select txsourceAccount sourceAcct,txdestinationAccount destinationAcct,txtype as docode from tp_transaction where txid=?";
                    sourceAccount = getAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
                    destinationAccount = getDestinationAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
//                    gwDatasource.close();
                    LOGGER.info("in C2B destinationAccount: {} sourceAccount: {} = {}", sourceAccount, destinationAccount, query);

                }
                if (ttype.contains("MKOBA")) {
                    //MKOBA TRANSACTION
                    query = "select sourceaccount sourceAcct,destinationaccount destinationAcct,transtype as docode from vg_group_transaction where transid=?";
                    sourceAccount = getAcct(query, rs.getString("txnid"), mkobaDatasource.getConnection());
                    destinationAccount = getDestinationAcct(query, rs.getString("txnid"), mkobaDatasource.getConnection());
                    docode = getDocode(query, rs.getString("txnid"), mkobaDatasource.getConnection());
//                    mkobaDatasource.close();
                    if (txn_type.equalsIgnoreCase("MKOBA2WALLET")) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "success";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("A".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("R".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                    }
                    if (txn_type.equalsIgnoreCase("WALLET2MKOBA")) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "success";
                        }
                        if ("A".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("R".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                    }
                }
                if (!ttype.contains("B2C") && !ttype.contains("C2B") && !ttype.contains("UTILITY") && !ttype.contains("MKOBA")) {
                    //neither MKOBA nor GATEWAY
                    sourceAccount = rs.getString("accountNo");
                    destinationAccount = rs.getString("contra_account");
                }
                String sql = "insert  into cbstransactiosn(txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?";
                //check if the record exists
                String existsSql = "SELECT * FROM cbstransactiosn where txnid=? or thirdparty_reference=?";
                LOGGER.info(existsSql.replace("?","'{}'"),rs.getString("txnid"),rs.getString("txnid"));
                List<Map<String,Object>> recordExists = jdbcReconTemplate.queryForList(existsSql, new String[]{rs.getString("txnid"),rs.getString("txnid")});
                LOGGER.info("Sync final response if record exists... {}", recordExists);
                if(recordExists.isEmpty()) {
                    LOGGER.info("No matching record exists in cbstransactiosn or txnid... {}",rs.getString("txnid"));
                    result = jdbcReconTemplate.update("insert  into cbstransactiosn(txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind,docode) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?,docode=?",
                            rs.getString("txnid"), txn_type, ttype, rs.getString("txndate"), sourceAccount, destinationAccount, rs.getString("amount"), rs.getString("description"), sourceAccount, "TZS", txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), docode, txnStatus, rs.getString("postBalance"), docode);
                }else{
                    result = -2;
                }
                //LOGGER.info("insert into cbstransactiosn {} = {}", sql, result);
                // LOGGER.info(sql.replace("?", "'{}'"), rs.getString("txnid"), txn_type, ttype, rs.getString("txndate"), sourceAccount, destinationAccount, rs.getString("amount"), rs.getString("description"), sourceAccount, "TZS", txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), txnStatus, rs.getString("postBalance"));
                //  LOGGER.info("After sync to Core banking recon Table: " + result);
            }
            //}
            String message = "Error occured";
            if (result != -1) {
                message = "successfully downloaded missing transaction!!!!!!";
            } else if (result == -2) {
                message = "Transaction exists, cannot be synced more than once";
            }

            return message;
        } catch (Exception ex) {
            Logger.getLogger(TransactionRepo.class.getName()).log(Level.SEVERE, "Error", ex);
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //get destination/source account from gateway

    public String getAcct(String query, String txnid, Connection con) {
        String sourceAccount = "-1";
        Map<String, String> data = new HashMap<>();
        try {
            //String qry = "select * from tp_transaction where txid=?";
            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1, txnid);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                sourceAccount = rs.getString("sourceAcct");
                System.out.println("QUERY:" + query);
                System.out.println("FROM GATEWAY: [sourceAcct= " + sourceAccount + "]");
            }
            con.close();
        } catch (SQLException ex) {
            LOGGER.info("SQL GATEWAY EXCEPTION: {}", ex);
        }
        return sourceAccount;
    }
    //get destination/source account from gateway

    public String getDestinationAcct(String query, String txnid, Connection con) {
        String destinationAccount = "-1";
        try {
            //String qry = "select * from tp_transaction where txid=?";
            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1, txnid);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                destinationAccount = rs.getString("destinationAcct");
            }
            con.close();

        } catch (SQLException ex) {
            LOGGER.info("SQL GATEWAY EXCEPTION: {}", ex);
        }
        return destinationAccount;
    }

    public String getDocode(String query, String txnid, Connection con) {
        String destinationAccount = "-1";
        try {
            //String qry = "select * from tp_transaction where txid=?";
            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1, txnid);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                destinationAccount = rs.getString("docode");
            }
            con.close();

        } catch (SQLException ex) {
            LOGGER.info("SQL GATEWAY EXCEPTION: {}", ex);
        }
        return destinationAccount;
    }

    public int syncMkobaAcctOpeningTxns(String txnids) {
        List<String> accts = getMkobaNewOpenedAcct(txnids);
        int result1 = -1;
        for (String acct : accts) {
            try {
                result1 = this.jdbcReconTemplate.update("update cbstransactiosn set txn_status='Opening group account Transaction with inititial deposit' where txnid=?", acct);
                LOGGER.info("syncMkobaAcctOpeningTxns-tid:{}, update:{}", acct, result1);
            } catch (DataAccessException e) {
                LOGGER.error("Rollbacked... {}", e.getMessage());
            }
        }

        return result1;

    }

    /*
     *  get accounts that completed the number of signatories and account opened with initial balance
     */
    public int syncMkobaAccountsWithNoSignatories(String txnids) {
        List<String> accts = getMkobaAcctWithNoSignatories(txnids);
        int result1 = -1;
        // jdbcReconTemplate.execute("LOCK TABLES thirdpartytxns WRITE;");
        for (String acct : accts) {
            try {
                result1 = this.jdbcReconTemplate.update("update thirdpartytxns set mnoTxns_status='Incomplete signatories; Account is not opened on Core Banking' where txnid=?", acct);
                LOGGER.info("syncMkobaAccountsWithNoSignatories-tid:{}, update:{}", acct, result1);
            } catch (DataAccessException e) {
                LOGGER.error("Rollbacked... {}", e.getMessage());
            }
        }
        //jdbcReconTemplate.execute("UNLOCK TABLES;");
        return result1;

    }

    /*
     *  get accounts that completed the number of signatories and account opened with initial balance
     */
    public List<String> getMkobaNewOpenedAcct(String txnids) {
        String query = "select transid  txnid from vg_group_transaction where transid in (" + txnids + ") and transtype in (1144,1142,1143)";
        List<String> data = jdbcMkobaTemplate.query(query, new RowMapper<String>() {
            public String mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                return rs.getString(1);
            }
        });
        LOGGER.info("{}\n{}", query, txnids);
        return data;
    }

    /*
     *  get accounts that has incompleted the number of signatories and account is not opened on rubikon
     */
    public List<String> getMkobaAcctWithNoSignatories(String txnids) {
        String query = "select transid  txnid from vg_group_transaction where transid in (" + txnids + ") and destinationaccount like '11122%'";
        LOGGER.info("getMkobaAcctWithNoSignatories: {}", query);
        List<String> data = jdbcMkobaTemplate.query(query, new RowMapper<String>() {
            public String mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                return rs.getString(1);
            }
        });
        return data;
    }

    //get GATEWAY TRANSACTION TYPES (B2C AND C2B)
    public List<Map<String, Object>> gawayTxnsDocodes() {
        return this.jdbcTemplate.queryForList("SELECT * FROM txns_types where ttype in ('B2C','C2B','UTILITY')");

    }
//get account movements setup

    public List<Map<String, Object>> getCashMovementAccts() {
        String mainSql = "select a.code as code,a.cbs_account as sourceAcct,a.gl_name as sourceAcctName,(select b.cbs_account from txns_types b where b.code=a.code and b.ttype='C2B') as destinationAcct,(select b.gl_name from txns_types b where b.code=a.code and b.ttype='C2B') as destinationAcctName from txns_types a WHERE ttype='B2C'\n"
                + "union all\n"
                + "select a.ttype as code, a.cbs_account as sourceAcct,a.gl_name as sourceAcctName,(select b.cbs_account from txns_types b where b.ttype=a.ttype and b.CODE='WALLET2MKOBA') destinationAcct,(select b.gl_name from txns_types b where b.ttype=a.ttype and b.CODE='WALLET2MKOBA') destinationAcctName from txns_types a WHERE a.code='MKOBA2WALLET'";
        return this.jdbcReconTemplate.queryForList(mainSql);
    }

    //initiate cashmovement transactions
//    @Async("dbPoolExecutor")
//    @Transactional
    public Integer saveInitiateCashmovement(String sourceAcct,String sourceAcctName, String destinationAcct,String destinationAcctName, String amount, String reference, String code, String indicator, String status,String cbsStatus, String comments, String initiated_by) {
        Integer result = -1;
        try {
            result = jdbcReconTemplate.update("INSERT INTO mobile_transfers(sourceAcct, destinationAcct, amount, reference, code, message_type, supportingDocument, status,cbs_status, comments, initiated_by,txn_type,sa_name,da_name) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    sourceAcct, destinationAcct, amount, reference, code, indicator,reference, status,cbsStatus, comments, initiated_by, "001",sourceAcctName,destinationAcctName);
        } catch (DataAccessException e) {
            LOGGER.info(e.getMessage());
            result = -1;
        }
        return result;
    }
    //initiate gepg remittance

    @Async("dbPoolExecutor")
    @Transactional
    public Integer saveinitiatedGePGRemittance(String sourceAcct, String destinationAcct, String amount, String reference, String code, String indicator, byte[] supportingDocument, String status, String comments, String initiated_by) {
        Integer result = -1;
        try {
            result = jdbcReconTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, code, indicator, supportingDocument, status, comments, initiated_by,txn_type) values(?,?,?,?,?,?,?,?,?,?,?)",
                    sourceAcct, destinationAcct, amount, reference, code, indicator, supportingDocument, status, comments, initiated_by, "001");
        } catch (DataAccessException e) {
            LOGGER.info(e.getMessage());
            result = -1;
        }
        return result;
    }

    //get TRANSFER TYPES:
    public List<Map<String, Object>> getRTGSTransferTypes(String roleId) {
        return this.jdbcReconTemplate.queryForList("select a.* from transfer_type a inner join transfer_type_role b on b.transfer_type_id=a.id where b.role_id=?", roleId);
    }
    //get initiated cash movement transaction

    public String getGepgMNORemittanceTxnsAjax(String ttype, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where status='I' and txn_type=? and code<>'IB' and code is not null";
            totalRecords = jdbcReconTemplate.queryForObject(mainSql, new Object[]{ttype}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = "WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND b.txn_type=? and b.status='I'";
                totalRecordwithFilter = jdbcReconTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b" + searchQuery, new Object[]{searchValue, ttype}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from transfers b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcReconTemplate.queryForList(mainSql, new Object[]{searchValue, ttype});

            } else {
                mainSql = "select * from  transfers b where b.txn_type=? and b.status='I'  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                //LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                results = this.jdbcReconTemplate.queryForList(mainSql, new Object[]{ttype});
            }
            //Java objects to JSON string - compact-print - salamu - Pomoja.

            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    GET ONLINE INITIATED PAYMENTS
     */
    public String getOnlineRemittanceAjax(String ttype, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where status in('P','BC') and cbs_status in ('C','BO','P') and txn_type=? and code='IB'";
            totalRecords = jdbcReconTemplate.queryForObject(mainSql, new Object[]{ttype}, Integer.class);
            String searchQuery = "";

            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = "WHERE concat(sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND  status in('P','BC')and cbs_status in ('C','BO','P') and txn_type=? and code='IB'";
                totalRecordwithFilter = jdbcReconTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b" + searchQuery, new Object[]{searchValue, ttype}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from transfers b" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcReconTemplate.queryForList(mainSql, new Object[]{searchValue, ttype});

            } else {
                mainSql = "select * from  transfers b where  status in('P','BC') and cbs_status in ('C','BO','P') and txn_type=? and code='IB' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                //LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                results = this.jdbcReconTemplate.queryForList(mainSql, new Object[]{ttype});
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
    GET Branch INITIATED PAYMENTS and posted to Transfer awaiting GLS
     */
    public String getBranchRemittanceAjax(String ttype, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where status='P' and cbs_status in ('C') and txn_type=? and code<>'IB'";
            totalRecords = jdbcReconTemplate.queryForObject(mainSql, new Object[]{ttype}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = "WHERE  concat(sourceAcct,' ',destinationAcct,' ',amount,' ',reference,' ',status,' ',comments,' ',initiated_by) LIKE ? AND status='P' and cbs_status in ('C') and txn_type=?  and code<>'IB'";
                totalRecordwithFilter = jdbcReconTemplate.queryForObject("SELECT COUNT(b.id) FROM transfers b" + searchQuery, new Object[]{searchValue, ttype}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from transfers b" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcReconTemplate.queryForList(mainSql, new Object[]{searchValue, ttype});
            } else {
                mainSql = "select b.*,(select name from  branches where code=b.branch_no) branch from  transfers b where status='P' and cbs_status in ('C') and txn_type=?  and code<>'IB' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                // LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                results = this.jdbcReconTemplate.queryForList(mainSql, new Object[]{ttype});
            }
            //Java objects to JSON string - compact-print - salamu - Pomoja.

            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException | DataAccessException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }
//get all gepg accounts

    public String getGepgAccountsAjax(String ttype, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from ega_partners where status =? and acct_no not in ('170242000001','130410000002') AND LEN(bot_acct)>=2";
            totalRecords = jdbcPartnersTemplate.queryForObject(mainSql, new Object[]{"1"}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(acct_no,' ',partner_code,' ',partner_name,' ',bot_acct) LIKE ? and acct_no not in ('170242000001','130410000002') AND LEN(bot_acct)>=2";
                totalRecordwithFilter = jdbcPartnersTemplate.queryForObject("SELECT COUNT(b.acct_no) FROM ega_partners b " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from ega_partners b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                results = this.jdbcPartnersTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select * from  ega_partners b where status=? and acct_no not in ('170242000001','130410000002') AND LEN(bot_acct)>=2 ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY";
                // LOGGER.info(mainSql.replace("?", "'" + ttype + "'"));
                results = this.jdbcPartnersTemplate.queryForList(mainSql, new Object[]{"1"});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }
//get all gepg accounts balance redy for remittance

    public String getGepgAccountBalanceAJax(String accts, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT COUNT(v.ACCT_NO) FROM DEPOSIT_ACCOUNT_SUMMARY v,CURRENCY c,ACCOUNT a  WHERE v.CLEARED_BAL >0 AND a.CRNCY_ID =c.CRNCY_ID and a.ACCT_NO =v.ACCT_NO  AND v.ACCT_NO IN  (" + accts + ") AND v.REC_ST ='A'";
            totalRecords = jdbcRUBIKONTemplate.queryForObject(mainSql, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(sourceAcct,' ',currency,' ',accountName,' ',amount) LIKE ?  AND v.CLEARED_BAL >0 AND a.CRNCY_ID =c.CRNCY_ID and a.ACCT_NO =v.ACCT_NO  AND v.ACCT_NO IN  (" + accts + ") AND v.REC_ST ='A'";
                totalRecordwithFilter = jdbcRUBIKONTemplate.queryForObject("SELECT COUNT(v.ACCT_NO) FROM DEPOSIT_ACCOUNT_SUMMARY v,CURRENCY c,ACCOUNT a " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {

                mainSql = "SELECT v.ACCT_NO sourceAcct,c.CRNCY_CD currency,a.ACCT_NM accountName,v.CLEARED_BAL amount FROM DEPOSIT_ACCOUNT_SUMMARY v,CURRENCY c,ACCOUNT a " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + "  FETCH NEXT " + rowPerPage + " ROWS ONLY";
                results = this.jdbcRUBIKONTemplate.queryForList(mainSql);

            } else {
                mainSql = "  SELECT v.ACCT_NO sourceAcct,c.CRNCY_CD currency,a.ACCT_NM accountName,v.CLEARED_BAL amount FROM DEPOSIT_ACCOUNT_SUMMARY v,CURRENCY c,ACCOUNT a  WHERE v.CLEARED_BAL >0 AND a.CRNCY_ID =c.CRNCY_ID and a.ACCT_NO =v.ACCT_NO  AND v.ACCT_NO IN  (" + accts + ") AND v.REC_ST ='A'";
                results = this.jdbcRUBIKONTemplate.queryForList(mainSql);
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        System.out.println("ACCOUNT BALANCES: " + json);
        return json;
    }

    public String getGepgAccountBalanceAJaxEOY(String accts, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(a.ACCT_NO) FROM DEPOSIT_ACCOUNT_SUMMARY db, ACCOUNT a, CURRENCY c WHERE a.ACCT_NO = db.ACCT_NO AND a.CRNCY_ID = c.CRNCY_ID AND db.CLEARED_BAL >0 AND (a.ACCT_NO IN ( " + accts + ") OR a.OLD_ACCT_NO IN ( " + accts + " )) ";

            totalRecords = jdbcRUBIKONTemplate.queryForObject(mainSql, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(sourceAcct,' ',currency,' ',accountName,' ',amount) LIKE ? and  a.acct_no = db.acct_no AND a.CRNCY_ID =c.CRNCY_ID AND db.CLEARED_BAL >0 AND (a.ACCT_NO IN ( " + accts + ") OR a.OLD_ACCT_NO IN ( " + accts + " ))";
                totalRecordwithFilter = jdbcRUBIKONTemplate.queryForObject("SELECT COUNT(a.ACCT_NO) FROM DP_ACCT_DAILY_BAL_HIST db, ACCOUNT a,CURRENCY c " + searchQuery, new Object[]{searchValue, DateUtil.todayRubikonFormat()}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.ACCT_NO sourceAcct,c.CRNCY_CD currency,a.ACCT_NM accountName,db.CLEARED_BAL amount from  DP_ACCT_DAILY_BAL_HIST db, ACCOUNT a,CURRENCY c " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + "  FETCH NEXT " + rowPerPage + " ROWS ONLY";
                results = this.jdbcRUBIKONTemplate.queryForList(mainSql);

            } else {
                mainSql = "SELECT a.ACCT_NO sourceAcct, c.CRNCY_CD currency, a.ACCT_NM accountName, db.CLEARED_BAL amount FROM DEPOSIT_ACCOUNT_SUMMARY db, ACCOUNT a, CURRENCY c WHERE a.ACCT_NO = db.ACCT_NO AND a.CRNCY_ID = c.CRNCY_ID AND db.CLEARED_BAL >0 AND (a.ACCT_NO IN ( " + accts + ") OR a.OLD_ACCT_NO IN ( " + accts + " )) ORDER BY " + columnName + " " + columnSortOrder + "  FETCH NEXT " + rowPerPage + " ROWS ONLY";
                //LOGGER.info(mainSql.replace("?", "'" + accts + "'"));
                results = this.jdbcRUBIKONTemplate.queryForList(mainSql);
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        System.out.println("ACCOUNT BALANCES: " + json);
        return json;
    }

    //download supporting document
    public byte[] getSupportingDocument(String ref, String id) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcReconTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    //get BOT account
    public List<Map<String, Object>> getBOTAccountGePG(String collectionAcct, String currency) {
        String mainSql = "select top 1 * from ega_partners where acct_no=? and currency=? ";
        return this.jdbcPartnersTemplate.queryForList(mainSql, new Object[]{collectionAcct, currency});
    }

    /*
    INITIATE GEPG REMITTANCE TO BOT
     */
    public int[] insertGepgRemittance(List<FundTransferReq> list) {
        int result1[] = {-1};
        try {
            result1 = this.jdbcReconTemplate.batchUpdate(
                    "INSERT  INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, sender_phone, sender_address, sender_name, reference, status, comments,purpose, initiated_by, branch_no,cbs_status,beneficiaryBIC,swift_message,code)"
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, list.get(i).getTxnType());
                    ps.setString(2, list.get(i).getSourceAcct());
                    ps.setString(3, list.get(i).getDestinationAcct());
                    ps.setBigDecimal(4, list.get(i).getAmount());
                    ps.setString(5, list.get(i).getCurrency());
                    ps.setString(6, list.get(i).getBeneficiaryName());
                    ps.setString(7, "0");
                    ps.setString(8, "-1");
                    ps.setString(9, list.get(i).getSenderName());
                    ps.setString(10, list.get(i).getReference());
                    ps.setString(11, list.get(i).getStatus());
                    ps.setString(12, list.get(i).getDescription());
                    ps.setString(13, list.get(i).getDescription());
                    ps.setString(14, list.get(i).getInitiatedBy());
                    ps.setString(15, list.get(i).getBranchNo());
                    ps.setString(16, "I");
                    ps.setString(17, list.get(i).getReceiverBIC());
                    ps.setString(18, list.get(i).getSwiftMessage());
                    ps.setString(19, "GePG");
                }

                @Override
                public int getBatchSize() {
                    return list.size();
                }

            });
            LOGGER.info("Transaction Logged successfully{}", result1);
        } catch (DataAccessException e) {
            result1[0] = -1;
            LOGGER.error("Rollbacked... {}", e.getMessage());
            return result1;
        }
        return result1;

    }

    //get the banks list
    public List<Map<String, Object>> getBanksList() {
        return this.jdbcReconTemplate.queryForList("SELECT * FROM banks");
    }

    public String syncTxnsSearchNotInRubikon(String txnid, String sourceAcct, String destinationAcct, String txn_type, String ttype) {
        String message = "Error occured: failed to insert into the database";
        try {
            String sourceAccount = "-1";
            String destinationAccount = "-1";
            int result = -1;

            String query = "SELECT gah.TRAN_REF_TXT accountNo,gah.TXN_AMT amount,gah.ROW_TS txndate, \n"
                    + "gah.TRAN_REF_TXT txnid,gah.TRAN_DESC description,gah.ACCT_NO contra_account,\n"
                    + "gah.ACCT_CRNCY_ID currency,gah.ORIGIN_BU_ID branchID,gah.DR_CR_IND status,gah.ACCT_HIST_ID lastPTID,\n"
                    + "gah.STMNT_BAL postBalance\n"
                    + "FROM DEPOSIT_ACCOUNT_HISTORY  gah WHERE (gah.ACCT_NO in (" + sourceAcct + ") or gah.ACCT_NO in (" + destinationAcct + ")) and gah.TRAN_DESC not like '%B2C Charge%' and gah.tran_ref_txt in (" + txnid + ")";
            LOGGER.info("ttype and txn_type are {} : {}", ttype, txn_type);
            System.out.println("********************");
            LOGGER.info(query);
            System.out.println("********************");
            PreparedStatement preparedStatement = coreBankingDatasource.getConnection().prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();
            String txnStatus = "Failed";
            LOGGER.info("ttype and txn_type are {} : {} and the rs ... {}", ttype, txn_type, rs);

            while (rs.next()) {
                String dr_cr_ind = rs.getString("status");

                if (dr_cr_ind.equals("DR")) {
                    dr_cr_ind = "CR";
                }

                if (dr_cr_ind.equals("CR")) {
                    dr_cr_ind = "DR";
                }
                if (checkIfCBSTransFound(rs.getString("txnid"), ttype, dr_cr_ind)) {
                    LOGGER.info("Search CBSTrans Found in txind: {}, {}, {}", rs.getString("txnid"), ttype, dr_cr_ind);
                    continue;
                }

                if (ttype.contains("B2C") || ttype.contains("UTILITY")) {
                    //GATEWAY TRANSACTION
                    query = "select txsourceAccount sourceAcct,txdestinationAccount destinationAcct,txtype as docode,txdate as txndate from tp_transaction where txid=?";
                    sourceAccount = getAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
                    destinationAccount = getDestinationAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
                    //LOGGER.info("in B2C OR UTILITY destinationAccount: {} = {}", destinationAccount, query);
                    if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "success";
                    }
                    if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    if ("A".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Success";
                    }
                    if ("R".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    LOGGER.info("ttype and txn_type are in B2C {} : {} and the rs ... {}", ttype, txn_type, rs);
                }
                if (ttype.contains("C2B")) {
                    // LOGGER.info("in C2B destinationAccount: {} = {}", destinationAccount, query);

                    if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "success";
                    }
                    if ("A".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Success";
                    }
                    if ("R".equalsIgnoreCase(rs.getString("status"))) {
                        txnStatus = "Reversed";
                    }
                    query = "select txsourceAccount sourceAcct,txdestinationAccount destinationAcct,txtype as docode,txdate as txndate from tp_transaction where txid=?";
                    sourceAccount = getAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
                    destinationAccount = getDestinationAcct(query, rs.getString("txnid"), gwDatasource.getConnection());
//                    gwDatasource.close();
                    // LOGGER.info("in C2B destinationAccount: {} sourceAccount: {} = {}", sourceAccount, destinationAccount, query);

                }
                if (ttype.contains("MKOBA")) {
                    //MKOBA TRANSACTION
                    query = "select sourceaccount sourceAcct,destinationaccount destinationAcct,transtype as docode,transate  as txndate from vg_group_transaction where transid=?";
                    sourceAccount = getAcct(query, rs.getString("txnid"), mkobaDatasource.getConnection());
                    destinationAccount = getDestinationAcct(query, rs.getString("txnid"), mkobaDatasource.getConnection());
//                    mkobaDatasource.close();
                    if (txn_type.equalsIgnoreCase("MKOBA2WALLET")) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "success";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("A".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("R".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                    }
                    if (txn_type.equalsIgnoreCase("WALLET2MKOBA")) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "success";
                        }
                        if ("A".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("R".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                    }
                }
                if (!ttype.contains("B2C") && !ttype.contains("C2B") && !ttype.contains("UTILITY") && !ttype.contains("MKOBA")) {
                    //neither MKOBA nor GATEWAY
                    sourceAccount = rs.getString("accountNo");
                    destinationAccount = rs.getString("contra_account");
                }
                String sql = "insert  into cbstransactiosn(txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?,txn_type=?,ttype=?";
                if (!txn_type.equals("LUKU")) {
                    result = jdbcReconTemplate.update("insert  into cbstransactiosn(txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?, txn_type=?,ttype=?",
                            rs.getString("txnid"), txn_type, ttype, rs.getString("txndate"), sourceAccount, destinationAccount, rs.getString("amount"), rs.getString("description"), sourceAccount, "TZS", txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), txnStatus, rs.getString("postBalance"), txn_type, ttype);
                    // LOGGER.info("insert into cbstransactiosn {} = {}", sql, result);
                    // LOGGER.info(sql.replace("?", "'{}'"), rs.getString("txnid"), txn_type, ttype, rs.getString("txndate"), sourceAccount, destinationAccount, rs.getString("amount"), rs.getString("description"), sourceAccount, "TZS", txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), txnStatus, rs.getString("postBalance"));
                    //  LOGGER.info("After sync to Core banking recon Table: " + result);
                }
            }
            //}
            if (result != -1) {
                message = "successfully downloaded missing transaction!!!!!!";
            }

            return message;
        } catch (SQLException ex) {
            LOGGER.info("SQLEXCEPTION ON SYNC RUBIKON TRANSACTIONS: {} ", ex);
        }
        return "failed";
    }

    public boolean checkIfCBSTransFound(String txnid, String ttype, String dr_cr_ind) {
        boolean is_found = false;
        int result = this.jdbcReconTemplate.queryForObject("select count(txnid) from cbstransactiosn  where  ttype=? and txnid =? and dr_cr_ind =?", new Object[]{ttype, txnid, dr_cr_ind}, Integer.class);
        if (result > 0) {
            is_found = true;
        }
        return is_found;
    }

    public String fireMobileMovementWFAjax(String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from mobile_transfers  where status='I' and txn_type='001'";
            totalRecords = jdbcReconTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',purpose,' ',initiated_by,' ',status) LIKE ? and status='I' and txn_type='001'";
                totalRecordwithFilter = jdbcReconTemplate.queryForObject("SELECT count(id) FROM mobile_transfers " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  mobile_transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcReconTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select * from mobile_transfers t where  status='I' and txn_type='001' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info("sql ...{}", mainSql);
                results = jdbcReconTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    //download supporting document
    public byte[] firePrevCMSupportingDocument(String ref, String id) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcReconTemplate.queryForObject("select * from fund_movement_documents where txnReference=? limit 1", new Object[]{ref}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            return result;
        }
        return result;
    }

    public String fireAuthMobileMovemntToPV(String reference, UsRole role, String username, String branchNo) {
        String finalResponse = "{\"result\":\"99\",\"message\":\"Failed to approve cash movement transaction \"}";
        int update1 = 0;
        try{
            update1 = this.jdbcReconTemplate.update("Update mobile_transfers set status='P', cbs_status='C', branch_approved_by=?, branch_approved_dt=?, branch_no=? where reference=?", username, DateUtil.now(),branchNo,reference);
            if(update1 == 1){
                //get details and send email.
                CashMovementRequestObj details = cashMovementRequestTxnObj(reference);
                LOGGER.info("Going to send email for reference.. {}",reference);
                transferService.sendEmailAlertToPV(details);
                finalResponse = "{\"result\":\"0\",\"message\":\"Transaction approved successfully to PV \"}";
            }
        }catch (DataAccessException dae){
            LOGGER.info("Updating to PV error ... {}", dae);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return finalResponse;
    }

    public String fireGetCMTxnsForPVWFAjax(String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from mobile_transfers  where status='P' and cbs_status='C' and txn_type='001'";
            totalRecords = jdbcReconTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',purpose,' ',initiated_by,' ',status) LIKE ? and status='P' and cbs_status='C' and txn_type='001'";
                totalRecordwithFilter = jdbcReconTemplate.queryForObject("SELECT count(id) FROM mobile_transfers " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  mobile_transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcReconTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select * from mobile_transfers t where status='P' and cbs_status='C' and txn_type='001' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info("sql ...{}", mainSql);
                results = jdbcReconTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public CashMovementRequestObj cashMovementRequestTxnObj(String reference) {
        CashMovementRequestObj cashMovementRequestObj=null;
        try{
            String sql = "SELECT * FROM mobile_transfers WHERE reference=?";
            return jdbcReconTemplate.queryForObject(sql, new Object[]{reference},
                    (ResultSet rs,int rowNum)->{
                    CashMovementRequestObj cmo = new CashMovementRequestObj();
                        cmo.setComments(rs.getString("comments"));
                        cmo.setMessageType(rs.getString("message_type"));
                        cmo.setCode(rs.getString("code"));
                        cmo.setTxnType(rs.getString("txn_type"));
                        cmo.setAmount((rs.getBigDecimal("amount")));
                        cmo.setReference(rs.getString("reference"));
                        cmo.setSourceAcc(rs.getString("sourceAcct"));
                        cmo.setSourceAccName(rs.getString("sa_name"));
                        cmo.setDestinationAcc(rs.getString("destinationAcct"));
                        cmo.setDestinationAccName(rs.getString("da_name"));
                        cmo.setIniatedBy(rs.getString("initiated_by"));
                        cmo.setIniatedDate(rs.getString("create_dt"));
                        cmo.setApprovedBy(rs.getString("branch_approved_by"));
                        cmo.setApprovedDate(rs.getString("branch_approved_dt"));
                        return cmo;
                    });
        }catch (DataAccessException dae){
            LOGGER.info("Data access exception on getting cashmovement object ... {}", dae);
            return null;
        }
    }


    public void updateCashMovementTxn(CashMovementRequestObj obj,String username) {
        try{
            jdbcReconTemplate.update("update mobile_transfers set status='C',message='SUCCESSULLY POSTED', hq_approved_by=?,hq_approved_dt=? where reference=?",username,DateUtil.now(),obj.getReference());
        }catch (DataAccessException d){
            LOGGER.info("Exception on updating mobile transaction..{}", d);
        }
    }

    public List<Map<String, Object>> fireMobileMovementReportAjax(String txnStatus, String sourceAcct,String destinationAcct,String code, String fromDate, String toDate) {
       String mainSql;
        List<Map<String, Object>> results=null;

            switch (txnStatus) {
                case "C":
                    //SUCCESS
                    mainSql = "select * from mobile_transfers where status=? and cbs_status=? and sourceAcct=? and destinationAcct=? and create_dt>=? and create_dt<=?";
                    results = jdbcReconTemplate.queryForList(mainSql, new Object[]{txnStatus,txnStatus,sourceAcct,destinationAcct,fromDate, toDate});
                    break;
                default:
                    mainSql = "select * from mobile_transfers where status=? and cbs_status='C' and sourceAcct=? and destinationAcct=? and create_dt>=? and create_dt<=?";
                    results = jdbcReconTemplate.queryForList(mainSql, new Object[]{txnStatus,sourceAcct,destinationAcct,fromDate, toDate});
            }
        return results;
    }

    public void saveSupportingDoc(String reference, MultipartFile file) {
        try {
            int result = jdbcReconTemplate.update("INSERT INTO fund_movement_documents (txnReference,supportingDoc,file_name,file_size) VALUES (?,?,?,?)",
                    reference, file.getBytes(), file.getOriginalFilename(), file.getSize());
            LOGGER.info("INSERTING FUND MOVEMENT FILE :{} FILE NAME: {} SIZE: {}", result, file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            LOGGER.info("ERROR ON INSERTING FUND MOVEMENT SUPPORTING DOCUMENT: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>>  getCMSupportingDocument(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcReconTemplate.queryForList("SELECT * FROM  fund_movement_documents  where txnReference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING CM SUPPORTING DOCUMENT: {}", e.getMessage());
        }
        return result;
    }

    //download supporting document
    public byte[] getCMSupportingDocument(String ref, String id) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcReconTemplate.queryForObject("select supportingDoc from fund_movement_documents where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked FM... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public String discardRemittanceTransaction(String reference) {
        String result = "{\"result\":\"99\",\"message\":\" Failed to update \"}";

        try{
            if(1== jdbcReconTemplate.update("update transfers set status ='F' where reference=?",reference)){
                result = "{\"result\":\"0\",\"message\":\"Transaction removed from workflow \"}";
                return result;
            }
        }catch (DataAccessException dae){
            LOGGER.info("Removing gepg remittance failure... {}", dae);
            result = "{\"result\":\"403\",\"message\":\"Exception error occured. Failed to remove transaction on workflow\"}";

        }
        return result;
    }

    public List<Map<String,Object>> getMuseStatementsCBSAjax(String accountNo, String fromDate, String toDate) {
        String query = "SELECT * FROM DEPOSIT_ACCOUNT_HISTORY dah WHERE ACCT_NO =? AND TRUNC(TRAN_DT) >=TO_DATE(?,'YYYY-MM-DD') AND  TRUNC(TRAN_DT) <=TO_DATE(?,'YYYY-MM-DD')";
//        LOGGER.info(query.replace("?","'{}'"),accountNo,fromDate,toDate);
        return jdbcRUBIKONTemplate.queryForList(query, accountNo, fromDate, toDate);
    }

    public List<Map<String,Object>> getMultipleWalletTransactionsAjax(String fromDate, String toDate) {
        String query = "SELECT * FROM transactions x WHERE channel ='WALLET' and date(create_date) >=? AND  date(create_date) <=? ORDER BY x.id DESC";
       LOGGER.info(query.replace("?","'{}'"),fromDate,toDate);
        return gwBrinjalDbConnection.queryForList(query, fromDate, toDate);
    }



    public Integer saveInitiateVikobaFundmovement(String mnoReceipt, String mno, String amount, String status, String cbsStatus, String initiatedBy, String updateControl) {
        Integer result = -1;
        String code = "VIKOBA";
        String transactionID = "-1";
//        String txnType = "1111";
        String comments = "Initiated Vikoba, waiting approval";
        String messageType = null;
        if(updateControl.equalsIgnoreCase("updateCollectionAccount")){
            messageType ="C2B";
            transactionID="ucb"+DateUtil.now("yyyMMddHHmmss");
        }else{
            messageType ="B2C";
            transactionID="udb"+DateUtil.now("yyyMMddHHmmss");
        }

        try {
            String sql = "INSERT INTO mobile_transfers(amount, reference,txid, code, message_type, supportingDocument, status,cbs_status, comments, initiated_by,txn_type) values(?,?,?,?,?,?,?,?,?,?,?)";
            result = jdbcReconTemplate.update(sql,amount.replace(",",""), transactionID,mnoReceipt, code, messageType,transactionID, status,cbsStatus, comments, initiatedBy, mno);
        } catch (DataAccessException e) {
            LOGGER.info(e.getMessage());
            result = -1;
        }
        return  result;
    }

    public String fireVikobaFundMovementWFAjax(String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from mobile_transfers  where status='I' and code='VIKOBA'";
            totalRecords = jdbcReconTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',destinationAcct,' ',amount,' ',purpose,' ',initiated_by,' ',status) LIKE ? and status='I' and code='VIKOBA'";
                totalRecordwithFilter = jdbcReconTemplate.queryForObject("SELECT count(id) FROM mobile_transfers " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  mobile_transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcReconTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select * from mobile_transfers t where  status='I' and code='VIKOBA' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info("sql ...{}", mainSql);
                results = jdbcReconTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String fireAuthVikobaTxn(VikobaUpdateRequest request, UsRole role, String username, String branchNo) {
        String result = "{\"result\":\"-1\",\"message\":\"General Failure: \"}";


        String payload = "<updateCollectionAccount>\n" +
                        "<transactionID>"+request.getTransactionID()+"</transactionID>\n" +
                            "<mno>"+request.getMno()+"</mno>\n" +
                            "<mnoReceipt>"+request.getMnoReceipt()+"</mnoReceipt>\n" +
                        "<amount>"+request.getAmount()+"</amount>\n" +
                        "</updateCollectionAccount>";

            if(request.getMessageType().equalsIgnoreCase("B2C")){
                payload = "<updateDisbursementAccount>\n" +
                        "<transactionID>"+request.getTransactionID()+"</transactionID>\n" +
                            "<mno>"+request.getMno()+"</mno>\n" +
                                "<mnoReceipt>"+request.getMnoReceipt()+"</mnoReceipt>\n" +
                            "<amount>"+request.getAmount()+"</amount>\n" +
                        "</updateDisbursementAccount>";
            }
            String url = sysenv.VIKOBA_OPERATIONS_URL;

        String response = HttpClientService.sendXMLRequest(payload,url);
        LOGGER.info("checking ... {}", response);
        String responseCode = XMLParserService.getDomTagText("responseCode",response);
        String responseMessage = XMLParserService.getDomTagText("responseMessage",response);
        LOGGER.info("responseCode.... {} and responseMessage ... {}", responseCode,responseMessage);
        if(responseCode.equalsIgnoreCase("0")){
            if(1==updateVikobaFundMovementTxn(request,responseMessage,username)){
                result = "{\"result\":\"0\",\"message\":\""+responseMessage+": \"}";
            }
        }
        return result;
    }

    public VikobaUpdateRequest getVikobaUpdateRequest(String reference) {
        VikobaUpdateRequest vikobaUpdateRequest=null;
        try{
            String sql = "SELECT * FROM mobile_transfers  WHERE reference=?";
//            LOGGER.info(sql.replace("?", "'{}'"),reference);
            return jdbcReconTemplate.queryForObject(sql, new Object[]{reference},
                    (ResultSet rs, int rowNum)->{
                        VikobaUpdateRequest vur = new VikobaUpdateRequest();
                        vur.setTransactionID(rs.getString("reference"));
                        vur.setAmount(rs.getString("amount"));
                        vur.setMessageType(rs.getString("message_type"));
                        vur.setMno(rs.getString("txn_type"));
                        vur.setMnoReceipt(rs.getString("txid"));
                        return vur;
                    });
        }catch (DataAccessException dae){
            LOGGER.info("Data access exception on getting Vikoba Request details object ... {}", dae);
            return null;
        }
    }

    public Integer updateVikobaFundMovementTxn(VikobaUpdateRequest request,String responseMessage,String username) {
        Integer updateRes = 0;
        try{
           updateRes = jdbcReconTemplate.update("update mobile_transfers set status='C',message=?, hq_approved_by=?,hq_approved_dt=? where reference=?",responseMessage,username,DateUtil.now(),request.getTransactionID());
        }catch (DataAccessException d){
            LOGGER.info("Exception on updating mobile transaction..{}", d);
        }
        return updateRes;
    }

    public GeneralJsonResponse getVikobaBalancePerMnoAjax(Map<String,String> customeQuery) {
        GeneralJsonResponse response = new GeneralJsonResponse();
        if(customeQuery.get("balanceControl").equalsIgnoreCase("SPECIFIC_DATE_BALANCE")){
            String query = "select a.openingBalance as collectionOB,a.clocingBalance as collectionCB, a.transactionDay as balanceDate, (select b.openingBalance\n" +
                    "from vg_group_disbursement_previous_day_balances b where b.transactionDay =?) as disbursementOB,\n" +
                    "(select b.closingBalance from vg_group_disbursement_previous_day_balances b where b.transactionDay =?) as disbursementCB\n" +
                    "from vg_group_collection_previous_day_balances a where a.transactionDay =?";
            List<Map<String,Object>> airtelVikobaOCB = jdbcAirtelVikobaTemplate.queryForList(query,new Object[]{ customeQuery.get("balanceDate"),customeQuery.get("balanceDate"),customeQuery.get("balanceDate")});
            response.setStatus("0");
            response.setResult(airtelVikobaOCB);
            return response;
        }else{
            String request = "<getmnoBalances>\n" +
                    "<transactionID>"+"mb"+DateUtil.now("yyyMMddHHmmss")+"</transactionID>\n" +
                    "<mno>"+customeQuery.get("mno")+"</mno>\n" +
                    "</getmnoBalances>";
            String url = sysenv.VIKOBA_OPERATIONS_URL;
            String result = HttpClientService.sendXMLRequest(request,url);
            XmlMapper xmlMapper = new XmlMapper();
            try {
                GetmnoBalancesResponse responseBalance = xmlMapper.readValue(result,GetmnoBalancesResponse.class);
                response.setStatus(responseBalance.getResponseCode()+"");
                response.setResult(responseBalance);
                return response;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    public String CheckTxnBasedOnMnoAndRetry(String mno, String reference, String ttype) {
        List<Map<String, Object>> finalResp = null;
        String sql = "-1";
        String request = "-1";

        if(ttype.equalsIgnoreCase("WALLET2VIKOBA")){
            request = "<DepositRetryRequest>\n"+
                    "<ThirdPartyReference>"+reference+"</ThirdPartyReference>\n" +
                    "<ResultCode>0</ResultCode>\n"+
                    "<Receipt>"+reference+"</Receipt>\n"+
                    "</DepositRetryRequest>";
        }else if(ttype.equalsIgnoreCase("VIKOBA2WALLET")){
            request="<getTransactionStatus>\n" +
                    "<transactionID>"+reference+"</transactionID>\n" +
                    "<mno>"+mno+"</mno>\n" +
                    "\t</getTransactionStatus>";
        }

        String approverId = httpSession.getAttribute("username").toString();
        String url = sysenv.VIKOBA_OPERATIONS_URL;
        String response = "-1";
        String responseCode ="-1";
        switch (mno){
            case("AIRTELMONEY"):
                sql = "SELECT * FROM  from vg_group_transaction WHERE transid =? and transstatus='0'";
                finalResp = jdbcAirtelVikobaTemplate.queryForList(sql,reference);
                LOGGER.info("check if transaction to be retried is success.... {}", finalResp);
                if(finalResp.size()<1){
                    LOGGER.info("going to retry the ...{} transaction reference... {} as ... {}, retried by..{}", mno, reference, ttype,approverId);
                  response=  HttpClientService.sendXMLRequest(request,url);
                  LOGGER.info("retry to ... {},with reference..{} response is ...{}", mno, reference,response);
                   responseCode = XMLParserService.getDomTagText("responseCode", response);
                   response = "{\"result\":\""+responseCode+"\",\"aaData\":\""+finalResp+" \"}";
                }
                break;
        }
        return response;
    }

    public List<Map<String,Object>> getMusePartners() {
        String query = "SELECT * FROM muse_partners";
        return jdbcPartnersTemplate.queryForList(query);
    }


    public void uploadFilesReconProcess(MultipartFile is) {

    }
}
