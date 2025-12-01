/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import java.util.List;
import java.util.Map;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 *
 * @author MELLEJI
 */
@Repository
public class SftpRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    //save to database transactions from sftp file [mnos]
    @Autowired
    @Qualifier("mkobadb")
    JdbcTemplate jdbcMKOBATemplate;
    @Autowired
    @Qualifier("gwdb")
    JdbcTemplate jdbcGWTemplate;

    @Autowired
    @Qualifier("txManagerMaster")
    PlatformTransactionManager txManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpRepo.class);

    @Async("dbPoolExecutor")
    @Transactional
    public Integer saveMNOSftpTransactions(String txn_type, String ttype, String txnid, String txdate, String receiptNo, String amount, String charge, String description, String currency, String mnoTxns_status, String status, String sourceAccount, String destinationaccount, String post_balance, String previous_balance, String shortcode, String file_name) {
//        if (getGWTxnidSourceAcctDestinatinAcct(receiptNo).size() >= 0) {
//            txnid = getGWTxnidSourceAcctDestinatinAcct(receiptNo).get(0).get("txnid").toString();
//            sourceAccount = getGWTxnidSourceAcctDestinatinAcct(receiptNo).get(0).get("sourceAcct").toString();
//            destinationaccount = getGWTxnidSourceAcctDestinatinAcct(receiptNo).get(0).get("destinationAcct").toString();
//        }
        Integer result = -1;
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        TransactionStatus status1 = txManager.getTransaction(def);
//
//        LOGGER.info("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate,receiptNo, amount,charge,description,currency,mnoTxns_status,status,sourceAccount,txdestinationaccount,post_balance, previous_balance, identifier,file_name) VALUES('{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}') ON DUPLICATE KEY UPDATE post_balance='{}',previous_balance='{}',amount='{}',description='{}'",
//                txn_type, ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name, post_balance, previous_balance, amount, description);
//
//        result = jdbcTemplate.update("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate,receiptNo, amount,charge,description,currency,mnoTxns_status,status,sourceAccount,txdestinationaccount,post_balance, previous_balance, identifier,file_name) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE post_balance=?,previous_balance=?,amount=?,description=?",
//                txn_type, ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name, post_balance, previous_balance, amount, description);

        LOGGER.info("UPDATE thirdpartytxns set description='{}',post_balance='{}',previous_balance='{}' where receiptNo='{}'",
                description, post_balance, previous_balance, receiptNo);
        result = jdbcTemplate.update("UPDATE thirdpartytxns set description=?,post_balance=?,previous_balance=? where receiptNo=?",
                description, post_balance, previous_balance, receiptNo);
        System.out.println("TRANSACTION INSERTED: " + result);
        System.out.println("TXN_TYPE: " + txn_type + " TTYPE: " + ttype + " TXNID: " + txnid + " CHARGE:" + charge);
        txManager.commit(status1);

        return result;
    }

    @Async("dbPoolExecutor")
    @Transactional
    public Integer saveMNOSftpTransactionsChages(String txn_type, String ttype, String txnid, String txdate, String receiptNo, String amount, String charge, String description, String currency, String mnoTxns_status, String status, String sourceAccount, String destinationaccount, String post_balance, String previous_balance, String shortcode, String file_name) {
        Integer result = -1;
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        TransactionStatus status1 = txManager.getTransaction(def);
//        if (getGWTxnidSourceAcctDestinatinAcct(receiptNo).size() >= 0) {
//            txnid = getGWTxnidSourceAcctDestinatinAcct(receiptNo).get(0).get("txnid").toString();
//            sourceAccount = getGWTxnidSourceAcctDestinatinAcct(receiptNo).get(0).get("sourceAcct").toString();
//            destinationaccount = getGWTxnidSourceAcctDestinatinAcct(receiptNo).get(0).get("destinationAcct").toString();
//        }
//        LOGGER.info("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate,receiptNo, amount,charge,description,currency,mnoTxns_status,status,sourceAccount,txdestinationaccount,post_balance, previous_balance, identifier,file_name) VALUES('{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}','{}') ON DUPLICATE KEY UPDATE post_balance='{}',previous_balance='{}',charge='{}'",
//                txn_type, ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name, post_balance, previous_balance, charge);

//        result = jdbcTemplate.update("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate,receiptNo, amount,charge,description,currency,mnoTxns_status,status,sourceAccount,txdestinationaccount,post_balance, previous_balance, identifier,file_name) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE post_balance=?,previous_balance=?,charge=?",
//                txn_type, ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name, post_balance, previous_balance, charge);
        LOGGER.info("UPDATE thirdpartytxns set description='{}',post_balance='{}',previous_balance='{}',charge='{}' where receiptNo='{}'",
                description, post_balance, previous_balance, charge, receiptNo);
        result = jdbcTemplate.update("UPDATE thirdpartytxns set description=?,post_balance=?,previous_balance=?,charge=? where receiptNo=?",
                description, post_balance, previous_balance, charge, receiptNo);
        System.out.println("CHARGE INSERTED: " + result);
        System.out.println("TXN_TYPE: " + txn_type + " TTYPE: " + ttype + " TXNID: " + txnid + " CHARGE:" + charge);
        txManager.commit(status1);
        return result;
    }

//get gateway txns
    public List<Map<String, Object>> getGWTxnidSourceAcctDestinatinAcct(String receiptNo) {
        return this.jdbcGWTemplate.queryForList("select txid txnid,txsourceAccount sourceAcct,txdestinationAccount destinationAcct from tp_transaction where txReceipt=?", receiptNo);
    }

    //get gateway txns
    public List<Map<String, Object>> getMKOBATxnidSourceAcctDestinatinAcct(String receiptNo) {
        return this.jdbcMKOBATemplate.queryForList("select transid txnid,sourceaccount sourceAcct,destinationaccount destinationAcct from vg_group_transaction where receipt=?", receiptNo);
    }
}
