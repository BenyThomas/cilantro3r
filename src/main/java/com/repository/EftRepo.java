/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.EFT.EftBulkPaymentReq;
import com.DTO.EFT.EftBulkPaymentForm;
import com.DTO.EFT.EftIncommingBulkPaymentsReq;
import com.DTO.EFT.EftOutgoingBulkPaymentsReq;
import com.DTO.EFT.EftPacs00400102Req;
import com.DTO.EFT.EftPacs004002TxnsInfoReq;
import com.DTO.EFT.EftPacs00800102OutgoingReq;
import com.DTO.EFT.EftPacs00800102Req;
import com.DTO.GeneralJsonResponse;
import com.DTO.ReplayIncomingTransactionReq;
import com.DTO.IBANK.BatchPaymentReq;
import com.DTO.IBANK.PaymentReq;
import com.DTO.RemittanceToQueue;
import com.DTO.Reports.TransferTransactions;
import com.DTO.batch.BatchPayemntReq;
import com.DTO.batch.BatchTxnEntries;
import com.DTO.batch.BatchTxnsEntry;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.prowidesoftware.swift.io.parser.MxParser;
import com.prowidesoftware.swift.model.mx.MxPacs00200103;
import com.prowidesoftware.swift.model.mx.MxPacs00400102;
import com.prowidesoftware.swift.model.mx.MxPacs00800102;
import com.prowidesoftware.swift.model.mx.MxPain00100103;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentification4Choice;
import com.prowidesoftware.swift.model.mx.dic.ActiveCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.ActiveOrHistoricCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.AmountType3Choice;
import com.prowidesoftware.swift.model.mx.dic.BranchAndFinancialInstitutionIdentification4;
import com.prowidesoftware.swift.model.mx.dic.CashAccount16;
import com.prowidesoftware.swift.model.mx.dic.ChargeBearerType1Code;
import com.prowidesoftware.swift.model.mx.dic.ClearingSystemIdentification3Choice;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransactionInformation10;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransactionInformation11;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV03;
import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerCreditTransferV02;
import com.prowidesoftware.swift.model.mx.dic.FinancialInstitutionIdentification7;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader32;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader33;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader38;
import com.prowidesoftware.swift.model.mx.dic.OrganisationIdentification4;
import com.prowidesoftware.swift.model.mx.dic.OriginalGroupInformation3;
import com.prowidesoftware.swift.model.mx.dic.OriginalTransactionReference13;
import com.prowidesoftware.swift.model.mx.dic.Party6Choice;
import com.prowidesoftware.swift.model.mx.dic.PartyIdentification32;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification1;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification3;
import com.prowidesoftware.swift.model.mx.dic.PaymentInstructionInformation3;
import com.prowidesoftware.swift.model.mx.dic.PaymentMethod3Code;
import com.prowidesoftware.swift.model.mx.dic.PaymentReturnV02;
import com.prowidesoftware.swift.model.mx.dic.PaymentTransactionInformation27;
import com.prowidesoftware.swift.model.mx.dic.PaymentTypeInformation21;
import com.prowidesoftware.swift.model.mx.dic.PaymentTypeInformation22;
import com.prowidesoftware.swift.model.mx.dic.Purpose2Choice;
import com.prowidesoftware.swift.model.mx.dic.RemittanceInformation5;
import com.prowidesoftware.swift.model.mx.dic.ReturnReason5Choice;
import com.prowidesoftware.swift.model.mx.dic.ReturnReasonInformation9;
import com.prowidesoftware.swift.model.mx.dic.ServiceLevel8Choice;
import com.prowidesoftware.swift.model.mx.dic.SettlementInformation13;
import com.prowidesoftware.swift.model.mx.dic.SettlementMethod1Code;
import com.prowidesoftware.swift.model.mx.dic.StructuredRemittanceInformation7;
import com.queue.QueueProducer;
import com.service.CorebankingService;
import com.service.FullNameAnagramService;
import com.service.HttpClientService;
import com.service.XMLParserService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.istack.NotNull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import philae.ach.TaTransfer;
import philae.api.PostDepositToGLTransfer;
import philae.api.TxRequest;
import philae.api.UsRole;
import philae.api.XaResponse;

/**
 * @author melleji.mollel
 */
@Repository
public class EftRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    QueueProducer queProducer;

    @Autowired
    SYSENV systemVariable;

    //    @Autowired
//    ISO20022Service iso20022Service;
    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;

    @Autowired
    BanksRepo banksRepo;
    @Autowired
    FullNameAnagramService fullNameAnagramService;
    @Autowired
    CorebankingService coreBankingService;

    @Autowired
    SignRequest sign;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EftRepo.class);
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    /*
    INSERT SUPPORTING DOCUMENT
     */
    public Integer saveSupportingDocuments(String reference, MultipartFile file) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT INTO transfer_document(txnReference,supportingDoc,file_name,file_size) VALUES (?,?,?,?)",
                    reference, file.getBytes(), file.getOriginalFilename(), file.getSize());
            LOGGER.info("INSERTING FILE :{} FILE NAME: {} SIZE: {}", result, file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            LOGGER.info("ERROR ON INSERTING SUPPORTING DOCUMENT: {}", e.getMessage());
            result = -1;
        }
        return result;
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
     *INITIATE BULK EFT PAYMENTS - OUTGOING
     */
    public int insertEftBulkPayments(String senderAccount, List<EftBulkPaymentReq> eftBulkPayments, EftBulkPaymentForm eftBulkPaymentForm, String batchReference, String initiatedBy, String branchCode, int totalTxnsCount, MultipartFile fileDoc, BigDecimal totalAMt) {
        int result = -1;
        try {
            /*
            INSERT BATCH DETAILS FIRST
             */
            int res1 = insertBatchDeatils(senderAccount, batchReference, totalTxnsCount, eftBulkPaymentForm.getMandate(), totalAMt, eftBulkPayments.get(0).getPaymentPurpose(), initiatedBy, branchCode, "005");
            int res2 = saveSupportingDocuments(batchReference, fileDoc);
            if (res1 == 1 && res2 == 1) {
                int[] result1 = this.jdbcTemplate.batchUpdate(
                        "INSERT  INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, sender_phone, sender_address, sender_name, reference, status, comments,purpose, initiated_by, branch_no,cbs_status,beneficiaryBIC,swift_message,code,batch_reference,direction)"
                                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                ps.setString(1, "005");
                                ps.setString(2, senderAccount);
                                ps.setString(3, eftBulkPayments.get(i).getBeneficiaryAccount());
                                ps.setBigDecimal(4, new BigDecimal(eftBulkPayments.get(i).getAmount()));
                                ps.setString(5, "TZS");
                                ps.setString(6, eftBulkPayments.get(i).getBeneficiaryName());
                                ps.setString(7, "0");
                                ps.setString(8, "-1");
                                ps.setString(9, eftBulkPaymentForm.getSenderName());
                                ps.setString(10, batchReference + "_" + i);
                                ps.setString(11, "I");
                                ps.setString(12, eftBulkPayments.get(i).getPaymentPurpose());
                                ps.setString(13, eftBulkPayments.get(i).getPaymentPurpose());
                                ps.setString(14, initiatedBy);
                                ps.setString(15, branchCode);
                                ps.setString(16, "I");
                                ps.setString(17, eftBulkPayments.get(i).getBeneficiaryBic());
                                ps.setString(18, "");
                                ps.setString(19, "EFT");
                                ps.setString(20, batchReference);
                                ps.setString(21, "OUTGOING");
                            }

                            @Override
                            public int getBatchSize() {
                                return eftBulkPayments.size();
                            }

                        });

                LOGGER.info("Transaction Logged successfully{}", result1);
                result = 0;
            }
        } catch (DataAccessException e) {
            result = -1;
            LOGGER.error("Rollbacked... {}", e.getMessage());
            return result;
        }
        return result;

    }

    /*
    INSERT BATCH TRANSACTIONS
     */
    public Integer insertBatchDeatils(String senderAccount, String reference, int noOfTxns, String mandate, BigDecimal tamt, String narration, String initiatedBy, String branchNo, String txnType) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT INTO transfer_eft_batches(direction,sourceAccount,batch_reference,number_of_txns,debit_mandate,total_amount,narration,initiated_by,branch_no,status,txn_type) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    "OUTGOING", senderAccount, reference, noOfTxns, mandate, tamt, narration, initiatedBy, branchNo, "I", txnType);
            LOGGER.info("INSERTING EFT BATCH DETAILS BATCH-REFERENCE :{} NUMBER OF TRANSACTIONS: {}", reference, noOfTxns);
        } catch (Exception e) {
            LOGGER.info("ERROR ON INSERTING SUPPORTING DOCUMENT: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    /*
     *GET EFT BULK TRANSACTIONS ON  workflow in branch level
     */
    public String getEftBulkPaymentsOnWorkFlowAjax(String date, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfer_eft_batches a where create_dt>=? and a.status = 'I'  and a.txn_type = '005' and branch_no=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{date, branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(sourceAcct,' ',sender_name,' ',batch_reference,' ',totalAmount,' ',noOfTxns,' ',debit_mandate) LIKE ? and create_dt>=? and status='I' and txn_type='005' and branch_no=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_eft_batches  " + searchQuery, new Object[]{searchValue, date, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.narration purpose, a.initiated_by,(select b.sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select b.sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name,a.batch_reference,a.total_amount totalAmount,a.number_of_txns noOfTxns,a.debit_mandate debit_mandate,(select b.status from transfers b where b.batch_reference=a.batch_reference limit 1) as status, (select b.cbs_status from transfers b where b.batch_reference=a.batch_reference limit 1) as cbs_status from transfer_eft_batches a where a.status='I' and a.txn_type='005' and a.create_dt>=?  and branch_no=? GROUP by a.batch_reference  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, date, branchNo});

            } else {
                mainSql = "select a.narration purpose,a.initiated_by,(select b.sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select b.sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name,a.batch_reference,a.total_amount totalAmount,a.number_of_txns noOfTxns,a.debit_mandate debit_mandate,(select b.status from transfers b where b.batch_reference=a.batch_reference limit 1) as status, (select b.cbs_status from transfers b where b.batch_reference=a.batch_reference limit 1) as cbs_status from transfer_eft_batches a where a.status='I' and a.txn_type='005' and  a.create_dt>=? and branch_no=? GROUP by a.batch_reference  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                // LOGGER.info(mainSql.replace("?", "'" + branchNo + "'"));
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{date, branchNo});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
//        System.out.println("ACCOUNT BALANCES: " + json);
        return json;
    }

    /*
    GET EFT BATCH ENTRIES
     */
    public List<Map<String, Object>> getEftBatchEntries(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT a.*,(SELECT name from banks where swift_code=a.beneficiaryBIC limit 1) benBank,(select debit_mandate  from transfer_eft_batches where batch_reference=?) mandate FROM transfers a where batch_reference=? and batch_reference2 is null", reference, reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH ENTRIES : {}", e.getMessage());
        }
        return result;
    }

    public int discardEftBatch(String reference) {
        int result = 0;
        try {
            result = this.jdbcTemplate.update("UPDATE  transfer_eft_batches set status='F' where batch_reference=? ", reference);
        } catch (Exception e) {
            LOGGER.info("FAILED TO REMOVE BATCH ON WORKFLOW : {}", e.getMessage());
        }
        return result;
    }

    /*
    GET EFT BATCH ENTRIES count
     */
    public int getEftBatchEntriesCount(String reference) {
        int result = -1;
        try {
            String mainSql = "SELECT count(*) FROM transfers a where batch_reference=?";
            result = jdbcTemplate.queryForObject(mainSql, new Object[]{reference}, Integer.class);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH ENTRIES : {}", e.getMessage());
        }
        return result;
    }

    /*
    GET EFT BATCH
     */
    public List<Map<String, Object>> getEftBatch(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT a.*,(select beneficiaryBIC from transfers b where b.batch_reference=a.batch_reference limit 1) beneficiaryBic,(select name from branches b where b.code=a.branch_no limit 1) branchName,(select sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name  FROM transfer_eft_batches a  where batch_reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    /*
     PROCESS EFT BATCH REMITTANCE FROM CUSTOMER ACCOUNT TO EFT AWAITING GL
     */
    public String processEftFrmAcctToEFTAwaiting(String reference, philae.ach.UsRole role, int batchCount) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";

        try {
            //get the BATCH DETAILS FROM THE QUEUE
            int confirmCount = 0;
            List<Map<String, Object>> txn = getEftBatchEntries(reference);
            if (txn.get(0).get("mandate").toString().equalsIgnoreCase("D102")) {
                //check mandate
                List<PaymentReq> paymenttxns = new ArrayList<>();
                for (int i = 0; i < txn.size(); i++) {
//                if (txn.get(i).get("mandate").equals("D102")) {
                    RemittanceToQueue remToQueue = new RemittanceToQueue();
                    remToQueue.setUsAchRole(role);
                    remToQueue.setBnUser(null);
                    remToQueue.setReferences(txn.get(i).get("reference").toString());
                    queProducer.sendToQueueEftFrmcustAcctToBrnchEFTLedger(remToQueue);
                    confirmCount++;
                }
            } else if (txn.get(0).get("mandate").toString().equalsIgnoreCase("D101")) {
                //GENERATE BATCH TRANSACTIONS FOR SINGLE DEBIT
                BatchPaymentReq req = generateBatchForSingleDebit(reference);
                result = processSingleDebitTransferFromAccount(req, role);//process single debit
//                queProducer.sendToQueueBatchTransactionFromBranch(generateBatchForSingleDebit(reference));
//                jdbcTemplate.update("update transfer_eft_batches set status='P',approved_by=?,approved_dt=?  where  batch_reference=?", role.getUserName(), DateUtil.now(), reference);
//                result = "{\"result\":\"0\",\"message\":\"Transactions Are being processed. Please preview the processed batch button to monitor the transactionsfrom there!!! \"}";
            }
            if (confirmCount == batchCount) {
                result = "{\"result\":\"0\",\"message\":\"Transactions Are being processed. Please preview the processed batch button to monitor the transactionsfrom there!!! \"}";
            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = "{\"result\":\"101\",\"message\":\"Exception error occured. Please contact IT for Support!!!!!\"}";
        }
        return result;
    }

    //   PROCESS SINGLE DEBIT ENTRIES
    public String processSingleDebitTransferFromAccount(BatchPaymentReq req, philae.ach.UsRole role) {
        String result = "{\"result\":\"99\",\"message\":\"Error Occured During debiting customer Account\"}";
//get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postDepositToGLTransfer";
        TxRequest transferReq = new TxRequest();
//        identifier = "api:postGLToDepositTransfer";
        transferReq.setReference(req.getBatchReference());
        transferReq.setAmount(req.getTotalAmt());
        transferReq.setNarration(req.getPurpose() + " No of Txns: " + req.getNoOfTxns());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(req.getSourceAccount());
        transferReq.setCreditAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(coreBankingService.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING BATCH TRANSACTION FROM CUSTOMER ACCT TO BRANCH LEDGER: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", req.getBatchReference());
            result = "{\"result\":\"999\",\"message\":\"Error Occured During debiting customer Account\"}";

        }

        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("update transfer_eft_batches set charge=?,status='P',approved_by=?,approved_dt=?  where  batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), role.getUserName(), DateUtil.now(), req.getBatchReference());
            jdbcTemplate.update("UPDATE  transfers SET status='P', cbs_status = 'C',branch_approved_by=?,branch_approved_dt =?,message='Transaction batch posted successfully from customer account to Transfer Awaiting, HQ pending approval',comments='Transaction batch posted successfully from customer account to Transfer Awaiting, HQ pending approval' where  batch_reference=?", role.getUserName(), DateUtil.now(), req.getBatchReference());
            result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + "\"}";

            if (!systemVariable.WAIVED_ACCOUNTS_LISTS.contains(req.getSourceAccount())) {//if account is not waived
                queProducer.sendToQueueProcessBatchTxnChargeFromBranch(req);
            }

        } else {
            result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + "\"}";
            LOGGER.info("<-------------------------------FAILED TO PROCESS SINGLE DEBIT: BATCH REFERENCE: {} ------------------------------------->", req.getBatchReference());
        }
        return result;

    }

    public BatchPaymentReq generateBatchForSingleDebit(String reference) {
        List<Map<String, Object>> txn = getEftBatchEntries(reference);
        List<Map<String, Object>> batchDetails = getEftBatch(reference);
        BatchPaymentReq batchReq = new BatchPaymentReq();
        if (batchDetails != null && txn != null) {
            batchReq.setBatchReference(batchDetails.get(0).get("batch_reference").toString());
            batchReq.setBatchType("005");
            batchReq.setBranchCode(batchDetails.get(0).get("branch_no").toString());
            batchReq.setCurrency("TZS");
            batchReq.setMandate("D101");
            batchReq.setSourceAccount(batchDetails.get(0).get("sourceAccount").toString());
            batchReq.setPurpose(batchDetails.get(0).get("narration").toString());
            batchReq.setTotalAmt(new BigDecimal(batchDetails.get(0).get("total_amount").toString()));
            batchReq.setNoOfTxns(batchDetails.get(0).get("number_of_txns").toString());
            List<PaymentReq> paymentRequest = new ArrayList<>();
            for (int i = 0; i < txn.size(); i++) {
                PaymentReq paymentReq = new PaymentReq();
                paymentReq.setAmount(new BigDecimal(txn.get(i).get("amount").toString()));
                paymentReq.setBatchReference(txn.get(i).get("batch_reference").toString());
                paymentReq.setBeneficiaryAccount(txn.get(i).get("destinationAcct").toString());
                paymentReq.setBeneficiaryBIC(txn.get(i).get("beneficiaryBIC").toString());
                paymentReq.setBeneficiaryContact("0");
                paymentReq.setBeneficiaryName(txn.get(i).get("beneficiaryName").toString());
                paymentReq.setBoundary("LOCAL");
                paymentReq.setChargeCategory("-1");
                //paymentReq.setCurrency(txn.get(i).get("currency").toString());
                paymentReq.setCustomerBranch(txn.get(i).get("branch_no").toString());
                paymentReq.setDescription(txn.get(i).get("purpose").toString());
                paymentReq.setInitiatorId(txn.get(i).get("initiated_by").toString());
                paymentReq.setIntermediaryBank("-1");
                paymentReq.setReference(txn.get(i).get("reference").toString());
                paymentReq.setSenderAccount(txn.get(i).get("sourceAcct").toString());
                paymentReq.setSenderAddress(txn.get(i).get("sender_address").toString());
                paymentReq.setSenderName(txn.get(i).get("sender_name").toString());
                paymentReq.setSenderPhone(txn.get(i).get("sender_phone").toString());
                paymentReq.setSpecialRateToken("-1");
                paymentReq.setType("005");
                paymentRequest.add(paymentReq);//add the payload to list array
            }
            batchReq.setPaymentRequest(paymentRequest);
        }
        return batchReq;

    }

    /*
     *GET EFT BULK TRANSACTIONS ON  workflow in HQ level
     */
    public String getEftBulkPaymentsOnHQWorkFlowAjax(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "select count(*) from transfer_eft_batches a where a.status = 'P'  and a.txn_type = '005'";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(sourceAcct,' ',sender_name,' ',batch_reference,' ',totalAmount,' ',noOfTxns,' ',debit_mandate) LIKE ? and status='I' and txn_type='005'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_eft_batches  " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.narration purpose, a.initiated_by,(select b.sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select b.sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name,a.batch_reference,a.total_amount totalAmount,a.number_of_txns noOfTxns,a.debit_mandate debit_mandate,(select b.status from transfers b where b.batch_reference=a.batch_reference limit 1) as status, (select b.cbs_status from transfers b where b.batch_reference=a.batch_reference limit 1) as cbs_status from transfer_eft_batches a where a.status='P' and a.txn_type='005' GROUP by a.batch_reference  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select a.narration purpose,a.initiated_by,(select b.sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select b.sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name,a.batch_reference,a.total_amount totalAmount,a.number_of_txns noOfTxns,a.debit_mandate debit_mandate,(select b.status from transfers b where b.batch_reference=a.batch_reference limit 1) as status, (select b.cbs_status from transfers b where b.batch_reference=a.batch_reference limit 1) as cbs_status from transfer_eft_batches a where a.status='P' and a.txn_type='005'  GROUP by a.batch_reference  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
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
    HQ PROCESS EFT BATCH TRANSACTION FROM BRANCH AWAITING GL TO HQ EFT GL
     */
    public String processEftFrmBrnchEFTLedgerToHqEFTLedger(String reference, UsRole role, int batchCount) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";

        try {
            //get the BATCH DETAILS FROM THE QUEUE
            int confirmCount = 0;
            List<Map<String, Object>> txn = getEftBatchEntries(reference);
            for (int i = 0; i < txn.size(); i++) {
                RemittanceToQueue remToQueue = new RemittanceToQueue();
                remToQueue.setUsRole(role);
                remToQueue.setBnUser(null);
                remToQueue.setReferences(txn.get(i).get("reference").toString());
                LOGGER.info("sendToQueueEftFrmBrnchLedgeToHqLedger: REMITTANCE OBJECT REQUEST: {} ", remToQueue);
                queProducer.sendToQueueEftFrmBrnchLedgeToHqLedger(remToQueue);
                confirmCount++;
            }
            if (confirmCount == batchCount) {
                result = "{\"result\":\"0\",\"message\":\"Transactions Are being processed. Please preview the processed batch button to monitor the transactionsfrom there!!! \"}";

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = "{\"result\":\"101\",\"message\":\"Exception error occured. Please contact IT for Support!!!!!\"}";
        }
        return result;
    }

    public String fireHqDiscardEftBatchTxn(String reference, UsRole role, int batchCount) {
        String result = "{\"result\":\"99\",\"message\":\" Failed to update \"}";

        try {
            //get the BATCH DETAILS FROM THE QUEUE
            if (1 == discardEftBatch(reference)) {
                result = "{\"result\":\"0\",\"message\":\"Transaction removed from workflow \"}";
                return result;
            }
        } catch (Exception ex) {
            LOGGER.error("EFT removing batch on workflow failure {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = "{\"result\":\"101\",\"message\":\"Exception error occured. Failed to remove transaction on workflow\"}";
        }
        return result;
    }

    /*
     * INSERT INCOMMING TRANSACTIONS FROM  m2
     */
    public Integer insertIncomingEFTBulkTransactions(EftPacs00800102Req req, String fileName, boolean isNotAllowedToProcess) {
        String responseCode = "-1";
        String cbsStatus = "F";
        String status = "S";
        String message = "Transaction are being processed";

        if (isNotAllowedToProcess == true) {
            responseCode = "999";
            cbsStatus = "F";
            status = "S";
            message = "Signature can not be verified. Proceed with you have confirmed on BOT side";
        }
        Integer result = -1;

        String code = "EFT";
        if (req.getCdtTrfTxInf().getEndToEndId().startsWith("P001P") || req.getCdtTrfTxInf().getEndToEndId().startsWith("N001P") || req.getCdtTrfTxInf().getEndToEndId().startsWith("PEN001P")) {
            code = "HPENSION";
        }
        if (req.getCdtTrfTxInf().getSenderName().contains("THE NSSF BOARD OF TRUSTEE") || req.getCdtTrfTxInf().getSenderName().equalsIgnoreCase("THE NSSF BOARD OF TRUSTEE")) {
            code = "NSSF";
        }

        if (req.getCdtTrfTxInf().getEndToEndId().startsWith("P004P")) {
            code = "HSALARIES";
        }
        try {
//            if (isGePGaccount(req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getCurrency()) == 0) {
//                status = "S";
//                message = "its GePG account. Please post transaction using control number";
//                result = 2;
//                responseCode = "2";
//            }
            //NAMEQUERY
//            String accountNameQuery = null;
//            accountNameQuery = coreBankingService.accountNameQuery(req.getCdtTrfTxInf().getBeneficiaryAcct()).toUpperCase();//get account Name
////            double distance =  100.00;
//            if (distance < 78) {
//                responseCode = "777";
//                cbsStatus = "F";
//                status = "S";
//                message = "Account name doesnt match by: " + distance + "% rubikon name: " + accountNameQuery;
//                result = 99;
//            }
            String reference = req.getCdtTrfTxInf().getTxId() + "_" + req.getCdtTrfTxInf().getEndToEndId().substring(0, 3);
            if (req.getCdtTrfTxInf().getTxId().length() >= 32) {
                reference = req.getCdtTrfTxInf().getTxId();
            }

            LOGGER.info("insertIncomingEFTBulkTransactions reference: {} ", reference);
            String mainSql = "INSERT  INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, sender_phone, sender_address, sender_name, reference, status, comments,purpose, initiated_by, branch_no,cbs_status,beneficiaryBIC,swift_message,code,batch_reference,direction,senderBIC,value_date,originallMsgNmId,txid,instrId,message,response_code,OrgnlTxId )"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE value_date=?";
            //ON DUPLICATE KEY UPDATE
            result = jdbcTemplate.update(mainSql,
                    "005", req.getCdtTrfTxInf().getSenderAccount(), req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getAmount(), req.getCdtTrfTxInf().getCurrency(), req.getCdtTrfTxInf().getBeneficiaryName().replaceAll("\uFFFD", "\""), "0", "", req.getCdtTrfTxInf().getSenderName().replaceAll("\uFFFD", "\""), reference, status, req.getCdtTrfTxInf().getPurpose(), req.getCdtTrfTxInf().getPurpose(), "SYSTEM", "", cbsStatus, req.getCdtTrfTxInf().getBeneficiaryBIC(), "", code, req.getMsgId(), "INCOMING", req.getCdtTrfTxInf().getSenderBIC(), req.getIntrBkSttlmDt().toString(), req.getOriginalMsgNmId(), req.getCdtTrfTxInf().getEndToEndId(),
                    req.getCdtTrfTxInf().getInstrId(), message, responseCode,req.getCdtTrfTxInf().getTxId(), req.getIntrBkSttlmDt().toString());

            LOGGER.info(mainSql.replace("?", "'{}'"),
                    "005", req.getCdtTrfTxInf().getSenderAccount(), req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getAmount(), req.getCdtTrfTxInf().getCurrency(), req.getCdtTrfTxInf().getBeneficiaryName().replaceAll("\uFFFD", "\""), "0", "", req.getCdtTrfTxInf().getSenderName().replaceAll("\uFFFD", "\""), reference, status, req.getCdtTrfTxInf().getPurpose(), req.getCdtTrfTxInf().getPurpose(), "SYSTEM", "", cbsStatus, req.getCdtTrfTxInf().getBeneficiaryBIC(), "", code, req.getMsgId(), "INCOMING", req.getCdtTrfTxInf().getSenderBIC(), req.getIntrBkSttlmDt().toString(), req.getOriginalMsgNmId(), req.getCdtTrfTxInf().getEndToEndId(),
                    req.getCdtTrfTxInf().getInstrId(), message, responseCode,req.getCdtTrfTxInf().getTxId(), req.getIntrBkSttlmDt().toString());

            LOGGER.info("INSERTING INCOMING EFT TXN: BATCH REFERENCE :{} EndToEndId: {} amount: {} sender Bank: {} sender Account: {} Beneficiary account: {},Beneficiary BIC: {}", req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount(), req.getCdtTrfTxInf().getSenderBIC(), req.getCdtTrfTxInf().getSenderAccount(), req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getBeneficiaryBIC());
//            if (distance < 78 || (isGePGaccount(req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getCurrency()) == 0)) {
//                result = 99;
//            }
        } catch (Exception e) {
            LOGGER.info("ERROR ON INSERTING EFT INCOMING TRANSACTION: {},BATCH REFERENCE:{},SOURCE FILE: {}", e.getMessage(), req.getMsgId(), fileName);
            result = -1;
        }
        return result;
    }


    /*
     * INSERT INCOMMING TRANSACTIONS FROM  m2
     */
    public EftPacs00800102Req processInsertIncomingReversalEFTBulkTransactions(MxPacs00400102 mx,String fileName) {
        EftPacs00800102Req pacs008 = new EftPacs00800102Req();
        pacs008.setMsgId(mx.getPmtRtr().getGrpHdr().getMsgId());
        pacs008.setCreDtTm(mx.getPmtRtr().getGrpHdr().getCreDtTm());
        pacs008.setNbOfTxs(mx.getPmtRtr().getGrpHdr().getNbOfTxs());
        pacs008.setTtlIntrBkSttlmAmt(mx.getPmtRtr().getGrpHdr().getTtlRtrdIntrBkSttlmAmt().getValue());
        pacs008.setTtlIntrBkSttlmCcy(mx.getPmtRtr().getGrpHdr().getTtlRtrdIntrBkSttlmAmt().getCcy());
        pacs008.setIntrBkSttlmDt(mx.getPmtRtr().getGrpHdr().getIntrBkSttlmDt());
        pacs008.setSttlmMtd(mx.getPmtRtr().getGrpHdr().getSttlmInf().getSttlmMtd().name());
        pacs008.setInstdAgt(mx.getPmtRtr().getGrpHdr().getInstdAgt().getFinInstnId().getBIC());
        pacs008.setOriginalMsgNmId("pacs.004.001.02");
        int numberOfTxns = Integer.parseInt(pacs008.getNbOfTxs());
        int count = 1;
        insertEFTBacthTxnIncoming(pacs008, fileName);
        for (PaymentTransactionInformation27 ctti : mx.getPmtRtr().getTxInf()) {
            EftIncommingBulkPaymentsReq cti = new EftIncommingBulkPaymentsReq();
            cti.setInstrId(ctti.getRtrId());
            cti.setEndToEndId(ctti.getOrgnlInstrId() );
            cti.setTxId(ctti.getRtrId());
            cti.setSvcLvlCd(ctti.getRtrRsnInf().get(0).getRsn().getCd());
            cti.setAmount(ctti.getOrgnlIntrBkSttlmAmt().getValue());
            cti.setCurrency(ctti.getOrgnlIntrBkSttlmAmt().getCcy());
            cti.setChrgBr("0.0");
            cti.setSenderName(ctti.getOrgnlTxRef().getDbtr().getNm());
            cti.setSenderBICorBEI(ctti.getOrgnlTxRef().getDbtrAgt().getFinInstnId().getBIC());
            cti.setSenderAccount(ctti.getOrgnlTxRef().getDbtrAcct().getId().getIBAN());
            cti.setSenderBIC(ctti.getOrgnlTxRef().getDbtrAgt().getFinInstnId().getBIC());
            cti.setBeneficiaryBICorBEI(ctti.getOrgnlTxRef().getCdtrAgt().getFinInstnId().getBIC());
            cti.setBeneficiaryAcct(ctti.getOrgnlTxRef().getCdtrAcct().getId().getIBAN());
            cti.setBeneficiaryBIC(ctti.getOrgnlTxRef().getCdtrAgt().getFinInstnId().getBIC());
            cti.setBeneficiaryName(ctti.getOrgnlTxRef().getCdtr().getNm());
            cti.setPurpose("Return transaction from other bank - "+ctti.getRtrRsnInf().get(0).getRsn().getCd());
            pacs008.setCdtTrfTxInf(cti);
            insertIncomingReversalEFTBulkTransactions(pacs008, fileName,ctti.getRtrRsnInf().get(0).getRsn().getCd());
        }
        return  pacs008;
    }


    public Integer insertIncomingReversalEFTBulkTransactions(EftPacs00800102Req pacs008, String fileName,String responseCode) {
        int result =-1;
        try {
            String reference = pacs008.getCdtTrfTxInf().getTxId() + "_" + pacs008.getCdtTrfTxInf().getEndToEndId().substring(0, 3);
            if (pacs008.getCdtTrfTxInf().getTxId().length() >= 32) {
                reference = pacs008.getCdtTrfTxInf().getTxId();
            }
            LOGGER.info("insertIncomingReversalEFTBulkTransactions reference: {} ", reference);
            String mainSql = "INSERT  INTO transfers(create_dt,modified_dt, returned_dt,message_type,txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, sender_phone, sender_address, sender_name, reference, status, comments,purpose, initiated_by, branch_no,cbs_status,beneficiaryBIC,swift_message,code,batch_reference,direction,senderBIC,value_date,originallMsgNmId,txid,instrId,message,response_code,OrgnlTxId )"
                    + " VALUES(NOW(),NOW(), NOW(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE value_date=?";
            //ON DUPLICATE KEY UPDATE
            result = jdbcTemplate.update(mainSql,pacs008.getOriginalMsgNmId(),
                    "005", pacs008.getCdtTrfTxInf().getSenderAccount(), pacs008.getCdtTrfTxInf().getBeneficiaryAcct(), pacs008.getCdtTrfTxInf().getAmount(), pacs008.getCdtTrfTxInf().getCurrency(), pacs008.getCdtTrfTxInf().getBeneficiaryName().replaceAll("\uFFFD", "\""), "0", "", pacs008.getCdtTrfTxInf().getSenderName().replaceAll("\uFFFD", "\""), reference, "S", pacs008.getCdtTrfTxInf().getPurpose(), pacs008.getCdtTrfTxInf().getPurpose(), "SYSTEM", "", "C", pacs008.getCdtTrfTxInf().getBeneficiaryBIC(), "", "EFT-RETURNED", pacs008.getMsgId(), "INCOMING", pacs008.getCdtTrfTxInf().getSenderBIC(), pacs008.getIntrBkSttlmDt().toString(), pacs008.getOriginalMsgNmId(), pacs008.getCdtTrfTxInf().getEndToEndId(),
                    pacs008.getCdtTrfTxInf().getInstrId(), "Returned", responseCode,pacs008.getCdtTrfTxInf().getTxId(), pacs008.getIntrBkSttlmDt().toString());

            LOGGER.info("Insert Result:{} INSERTING INCOMING EFT TXN: BATCH REFERENCE :{} EndToEndId: {} amount: {} sender Bank: {} sender Account: {} Beneficiary account: {},Beneficiary BIC: {}",result, pacs008.getMsgId(), pacs008.getCdtTrfTxInf().getEndToEndId(), pacs008.getCdtTrfTxInf().getAmount(), pacs008.getCdtTrfTxInf().getSenderBIC(), pacs008.getCdtTrfTxInf().getSenderAccount(), pacs008.getCdtTrfTxInf().getBeneficiaryAcct(), pacs008.getCdtTrfTxInf().getBeneficiaryBIC());
        } catch (Exception e) {
            LOGGER.info("ERROR ON INSERTING EFT INCOMING TRANSACTION: {},BATCH REFERENCE:{},SOURCE FILE: {}", e.getMessage(), pacs008.getMsgId(), fileName,e);
        }
        return result;
    }


    /*
     *INSERT BATCH INCOMMING TRANSACTIONS FROM EFT
     */
    public Integer insertEFTBacthTxnIncoming(EftPacs00800102Req req, String fileName) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT  INTO transfer_eft_batches(txn_type, batch_reference, total_amount, number_of_txns, branch_no,narration,status,direction,source_file_name,initiated_by)"
                            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                    "005", req.getMsgId(), req.getTtlIntrBkSttlmAmt(), req.getNbOfTxs(), "060", " ", "R", "INCOMING", fileName, "SYSTEM");
            LOGGER.info(">>>>>>BATCH<<<<<<< INSERTING INCOMING EFT TXN: BATCH REFERENCE :{} BATCH TOTAL AMOUNT: {} NUMBER OF TRANSACTIONS: {} ", req.getMsgId(), req.getTtlIntrBkSttlmAmt(), req.getNbOfTxs());
        } catch (DataAccessException e) {
            LOGGER.info(">>>>>BATCH<<<<< ERROR ON INSERTING EFT INCOMING BATCH TRANSACTION: {},BATCH REFERENCE:{},SOURCE FILE: {}", e.getMessage(), req.getMsgId(), fileName);
            result = -1;
        }
        return result;
    }


    /*
    GET EFT dashboard
     */
    public List<Map<String, Object>> getEFTModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    /*
    GET INWARD BATCH TRANSACTION SUMMARY
     */
    public String getInwardEFTAjax(String fromDate, String todate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "select count(*) from (SELECT senderBIC FROM transfers where create_dt>=? and create_dt<=? and txn_type='005'  group by senderBIC ) as derivedTable";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, todate}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(status,' ',senderBic) LIKE ?  and  txn_type='005' and create_dt>=? and create_dt<=? AND direction='INCOMING'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("select count(*) from (SELECT senderBIC FROM transfers  " + searchQuery + " GROUP by senderBIC ) as derivedTable", new Object[]{searchValue, fromDate, todate}, Integer.class);
                mainSql = "select\n"
                        + "	count(*) totalCount,\n"
                        + "	sum(amount) totalAmount,\n"
                        + "	senderBIC,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		name\n"
                        + "	from\n"
                        + "		banks\n"
                        + "	where\n"
                        + "		swift_code = a.senderBIC LIMIT 1) bankName ,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'C' AND direction='INCOMING') as totalSuccess,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'QI' AND direction='INCOMING') as totalInQueue,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'F'"
                        + "             and direction='INCOMING') as totalFailed,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'C'"
                        + "             AND direction='INCOMING'), 0) as totalSuccessAmt,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'QI'"
                        + "             AND direction='INCOMING'), 0) as totalAmtInQueue,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'F' AND direction='INCOMING'), 0) as totalFailedAmt\n"
                        + "from\n"
                        + "	transfers a \n"
                        + " " + searchQuery + " GROUP by senderBIC ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, todate});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "select\n"
                        + "	count(*) totalCount,\n"
                        + "	sum(amount) totalAmount,\n"
                        + "	senderBIC,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		name\n"
                        + "	from\n"
                        + "		banks\n"
                        + "	where\n"
                        + "		swift_code = a.senderBIC LIMIT 1) bankName ,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'QI' AND direction='INCOMING') as totalInQueue,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'QI'"
                        + "             AND direction='INCOMING'), 0) as totalAmtInQueue,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'C' AND direction='INCOMING') as totalSuccess,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'F' AND direction='INCOMING') as totalFailed,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'C' AND direction='INCOMING'), 0) as totalSuccessAmt,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type = '005'\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'S'\n"
                        + "		and cbs_status = 'F' AND direction='INCOMING'), 0) as totalFailedAmt\n"
                        + "from\n"
                        + "	transfers a\n"
                        + "where\n"
                        + "  txn_type='005' and create_dt>=? and create_dt<=? AND direction='INCOMING'  GROUP by senderBIC ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, todate});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String getInwardEFTSuccessPerBankAjax(String fromDate, String toDate, String senderBic, String txnstatus, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(senderBIC) FROM transfers where create_dt>=? and create_dt<=? and txn_type='005'  and senderBIC=? and cbs_status=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, senderBic, txnstatus,}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(status,' ',senderBic) LIKE ?  and  txn_type='005' and create_dt>=? and create_dt<=?  and senderBIC=? and cbs_status=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(senderBIC) FROM transfers  " + searchQuery, new Object[]{searchValue, senderBic, txnstatus}, Integer.class);
                mainSql = "SELECT * FROM transfers  " + searchQuery + " GROUP by senderBIC ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, senderBic});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT * FROM transfers a where create_dt>=?  and create_dt<=? and txn_type='005' and senderBIC=? and cbs_status=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, senderBic, txnstatus});
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
     * CHECK IF THE ACCOUNT IS GEPG ACCOUNT
     */
    public int isGePGaccount(String accountNo, String currency) {
        int result = 404;
        if (accountNo == null) {
            return 404;
        }

        if (accountNo.contains("CCA0240000032") || accountNo.contains("024-0000032") || accountNo.contains("0240000032")
                || accountNo.contains("4600000812802")
                || accountNo.contains("4600000812801")
                || accountNo.contains("4900000494802")
                || accountNo.contains("170227000078")
                || accountNo.contains("4600000811001")) {
            result = 0;
        } else {
            try {
                String query = "SELECT COUNT(acct_no) FROM transfers WHERE acct_no=? and currency=?";
                int count = this.jdbcPartnersTemplate.queryForObject(query, new Object[]{accountNo, currency}, Integer.class);
                if (count > 0) {
                    result = 0;
                }
            } catch (DataAccessException e) {
                result = -1;
                LOGGER.error("It is not gepg account: Rollbacked... {}", e.getMessage());
                return result;
            }
        }
        return result;
    }

    /*
     * GET EFT MESSAGE
     */
    public List<Map<String, Object>> getEftMessage(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.*,(select sum(amount) from transfers where reference IN (" + reference + ")) totalAmount,(select count(*) from transfers where reference IN (" + reference + ")) noOfTxns from transfers a where reference IN (" + reference + ")");

        } catch (DataAccessException ex) {

        }
        return result;

    }

    /*
     * RETURN THE TRANSACTION TO BOT
     */
    public String generateReturnMessageToTach(String returnReason, String references, String username, String reasonDescription) {
        String result = "{\"result\":\"101\",\"message\":\"An Error Occured During Processing. Please try again: \"}";
        try {
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            String formattedDate = df.format(Calendar.getInstance().getTime());
            List<Map<String, Object>> eftMsgs = getEftMessage(references);
            LOGGER.info("TRANSACTION DETAILS:{}", eftMsgs);
            if (eftMsgs != null && !eftMsgs.isEmpty()) {
                EftPacs00400102Req req = new EftPacs00400102Req();
                String batchReference = "005E" + formattedDate;
                req.setSettlementMthd("CLRG");
                req.setClearingSystem("ACH");
                req.setCreateDateTime(DateUtil.getXMLGregorianCalendarNow());
                req.setInstructingAgent(systemVariable.SENDER_BIC);
                req.setInterBankSettlmntDate(DateUtil.getXMLGregorianCalendarNow());
                req.setMessageId(batchReference);
                req.setNbOfTxs(eftMsgs.get(0).get("noOfTxns").toString());
                req.setTotalReturnedIntrBnkSettlmntAmt(new BigDecimal(eftMsgs.get(0).get("totalAmount").toString()));
                List<EftPacs004002TxnsInfoReq> txnInfo = new ArrayList<>();
                for (int i = 0; i < eftMsgs.size(); i++) {
                    String txnReference = "005E" + formattedDate + "_" + i;
                    EftPacs004002TxnsInfoReq txinfo = new EftPacs004002TxnsInfoReq();
                    txinfo.setBeneficiaryAcct(eftMsgs.get(i).get("destinationAcct").toString());
                    txinfo.setBeneficiaryBIC(systemVariable.SENDER_BIC);
                    txinfo.setBeneficiaryBICorBEI(systemVariable.SENDER_BIC);
                    txinfo.setBeneficiaryName(eftMsgs.get(i).get("beneficiaryName").toString());
                    txinfo.setChargeBearer("SLEV");
                    txinfo.setOriginalEndToEndId(eftMsgs.get(i).get("txid").toString());
                    txinfo.setOriginalInstructionId(eftMsgs.get(i).get("instrId").toString());
                    txinfo.setOriginalInterBankSettmntAmt(new BigDecimal(eftMsgs.get(i).get("amount").toString()));
                    txinfo.setOriginalInterBankSettmntCcy(eftMsgs.get(i).get("currency").toString());
                    txinfo.setOriginalMsgId(eftMsgs.get(i).get("batch_reference").toString());
                    txinfo.setOriginalMsgNmId(eftMsgs.get(i).get("originallMsgNmId").toString());
                    txinfo.setOriginalTxid(eftMsgs.get(i).get("OrgnlTxId").toString());
                    txinfo.setReturnReasonInfomation(returnReason);//HARDCODED
                    txinfo.setReturnTxnId(txnReference);
                    txinfo.setReturnedInterBankSettmntAmt(new BigDecimal(eftMsgs.get(i).get("amount").toString()));
                    txinfo.setReturnedInterBankSettmntCcy(eftMsgs.get(i).get("currency").toString());
                    txinfo.setSenderAccount(eftMsgs.get(i).get("sourceAcct").toString());
                    txinfo.setSenderBIC(eftMsgs.get(i).get("senderBIC").toString());
                    txinfo.setSenderBICorBEI(eftMsgs.get(i).get("senderBIC").toString());
                    txinfo.setSenderName(eftMsgs.get(i).get("sender_name").toString());
                    txinfo.setTxnDate(eftMsgs.get(i).get("value_date").toString());
                    txnInfo.add(txinfo);
                    //insert the transaction into the table with status=p and cbs_status=P set the transaction to queue and debit BOT GL (GL2GL TRANSFER)
                    updateEFTIncomingToOutwardRejection(batchReference, eftMsgs.get(i).get("reference").toString(), reasonDescription + " : RETURN CODE:" + returnReason);
                    //insert rejection as outgoing for tracking
                    //txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, purpose, direction, originallMsgNmId, initiated_by, branch_approved_by, hq_approved_by, value_date, branch_approved_dt, hq_approved_dt, branch_no, cbs_status);
                    String status = "R";
                    String cbsStatus = "I";
                    if (eftMsgs.get(i).get("code").equals("HPENSION")) {
                        status = "HP";
                        cbsStatus = "HP";
                    }
                    saveEFTRejectionTransaction("005", eftMsgs.get(i).get("sourceAcct").toString(), eftMsgs.get(i).get("destinationAcct").toString(), eftMsgs.get(i).get("amount").toString(), eftMsgs.get(i).get("currency").toString(), eftMsgs.get(i).get("beneficiaryName").toString(), eftMsgs.get(i).get("beneficiaryBIC").toString(), "0", eftMsgs.get(i).get("senderBIC").toString(), "0", "0", eftMsgs.get(i).get("sender_name").toString(), txnReference, txnReference, txnReference, batchReference, batchReference, eftMsgs.get(i).get("code").toString(), status, eftMsgs.get(i).get("purpose").toString(), "OUTGOING", batchReference, username, username, username, DateUtil.now(), DateUtil.now(), DateUtil.now(), "060", cbsStatus);
                    //produce to queue debit (eft credit) credit (eft operation) if code = hazina pension
                }
                req.setTxInfo(txnInfo);
                String fileNameReturned = eftMsgs.get(0).get("senderBIC").toString() + DateUtil.now("yyyyMMddHms") + ".i";//file name to be returned
                //insert the batch as outgoing on eft batch tables
                insertEFTBacthOutgoingRejection(req, fileNameReturned, "060");
                result = "{\"result\":\"0\",\"message\":\"Transaction Submitted successfully. You Will be notificed once accepted\"}";
                String rawXmlsigned = generateMxPacs00400102SignedReq(req, eftMsgs.get(0).get("senderBIC").toString());
                queProducer.sendToQueueEftFrmCBSToTACH(fileNameReturned + "^" + rawXmlsigned);
            }
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            result = "{\"result\":\"99\",\"message\":\"Cannot contact BOT. Please contact System administrator for further support: \"}";

        }
        return result;
    }

    /*
    Process inward-eft to core banking
     */
    public void processEFTInwardToCorebankingIso20022() {
        //get entries on the queue
        List<Map<String, Object>> result = null;
        int noOfTxns = 0;
        BigDecimal totalAmt = BigDecimal.ZERO;
        String batchRef = SYSENV.generateTransactionReference(5) + "-" + DateUtil.now("yyyyMMddHHmmssSS");
        try {
            result = this.jdbcTemplate.queryForList("select * from transfers where cbs_status='AQ' AND  direction='INCOMING' AND beneficiaryBic='TAPBTZTZ' and  code NOT IN ('HPENSION','NSSF','TANESCO_SACCOSS') LIMIT 2000");
            List<BatchTxnEntries> btxns = new ArrayList();
            if (result != null) {
                for (Map<String, Object> txn : result) {
                    BatchTxnEntries btxn = new BatchTxnEntries();
                    totalAmt = totalAmt.add(new BigDecimal(txn.get("amount") + ""));
                    btxn.setAmount(new BigDecimal(txn.get("amount") + ""));
                    btxn.setBatch(batchRef);
                    btxn.setBuId("-5");
                    btxn.setCode("?");
                    btxn.setCrAcct(formatAccountNo(txn.get("destinationAcct") + ""));
                    btxn.setCurrency(txn.get("currency") + "");
                    if (txn.get("code").equals("HPENSION") || txn.get("code").equals("NSSF")) {
                        btxn.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    } else {
                        btxn.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    }

                    btxn.setModule("EFT-INCOMING");
                    btxn.setNarration(txn.get("purpose") + " B/O " + txn.get("sender_name"));
                    btxn.setReverse("N");
                    btxn.setTxnRef(txn.get("reference") + "");
                    btxn.setScheme("?");
                    //update the entry after being pulled
                    int updateResult = updateEFTIncomingToCorebanking(btxn);
                    LOGGER.info("update result for txn reference... {} is ... {}", btxn.getTxnRef(), updateResult);
                    if (1 == updateResult) {
                        btxns.add(btxn);
                        noOfTxns++;
                    }
                }
                if (noOfTxns > 0) {
                    BatchPayemntReq bpayments = new BatchPayemntReq();
                    bpayments.setCallbackUrl(systemVariable.EFT_BATCH_CALLBACK_URL);
                    bpayments.setItemCount(String.valueOf(noOfTxns));
                    bpayments.setReference(batchRef);
                    bpayments.setSerialize("false");
                    bpayments.setTotalAmount(totalAmt.toString());
                    BatchTxnsEntry bEntries = new BatchTxnsEntry();
                    bEntries.setTxn(btxns);
                    bpayments.setTxns(bEntries);
                    //insert the batch into eft-batches table
                    logEFTIncomingBatchToCorebanking(bpayments);
                    String requestToCore = XMLParserService.jaxbGenericObjToXML(bpayments, true, true);
                    LOGGER.info("EFT-INWARD BATCH REQ TO CORE:{}", requestToCore);
                    String response = coreBankingService.processRequestToCore(requestToCore, "api:processBatch");
                    LOGGER.info("EFT-INWARD BATCH RESP FROM CORE:{}", response);
                }
            }
        } catch (Exception e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING INWARD BATCH:{}", e.getMessage());
            LOGGER.info(null, e);
        }
    }

    public void processLoanRepayment() {
        //get entries on the queue
        List<Map<String, Object>> result = null;
        int noOfTxns = 0;
        BigDecimal totalAmt = BigDecimal.ZERO;
        String batchRef = SYSENV.generateTransactionReference(5) + "-" + DateUtil.now("yyyyMMddHHmmssSS");
        try {
            result = this.jdbcTemplate.queryForList("select * from transfers where cbs_status='AQ' AND  direction='INCOMING' AND beneficiaryBic='TAPBTZTZ' and code IN ('HPENSION','NSSF') LIMIT 500");

            List<BatchTxnEntries> btxns = new ArrayList();
            if (result != null) {
                for (Map<String, Object> txn : result) {

                    BatchTxnEntries btxn = new BatchTxnEntries();
                    totalAmt = totalAmt.add(new BigDecimal(txn.get("amount") + ""));
                    btxn.setAmount(new BigDecimal(txn.get("amount") + ""));
                    btxn.setBatch(batchRef);
                    btxn.setBuId("-5");
                    btxn.setCode("?");
                    btxn.setCrAcct(formatAccountNo(txn.get("destinationAcct") + ""));
                    btxn.setCurrency(txn.get("currency") + "");
                    if (txn.get("code").equals("HPENSION")) {
                        btxn.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    } else {
                        btxn.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    }

                    btxn.setModule("EFT-INCOMING");
                    btxn.setNarration(txn.get("purpose") + " B/O " + txn.get("sender_name"));
                    btxn.setReverse("N");
                    btxn.setTxnRef(txn.get("reference") + "");
                    btxn.setScheme("?");
                    //update the entry after being pulled
                    int updateResult = updateEFTIncomingToCorebanking(btxn);
                    LOGGER.info("update result for txn reference... {} is ... {} code... {}", btxn.getTxnRef(), updateResult, txn.get("code"));
                    if (1 == updateResult) {
                        btxns.add(btxn);
                        noOfTxns++;
                    }
                }
                if (noOfTxns > 0) {
                    BatchPayemntReq bpayments = new BatchPayemntReq();
                    bpayments.setCallbackUrl(systemVariable.EFT_BATCH_CALLBACK_URL);
                    bpayments.setItemCount(String.valueOf(noOfTxns));
                    bpayments.setReference(batchRef);
                    bpayments.setSerialize("false");
                    bpayments.setTotalAmount(totalAmt.toString());
                    BatchTxnsEntry bEntries = new BatchTxnsEntry();
                    bEntries.setTxn(btxns);
                    bpayments.setTxns(bEntries);
                    //insert the batch into eft-batches table
                    logEFTIncomingBatchToCorebanking(bpayments);
                    String requestToCore = XMLParserService.jaxbGenericObjToXML(bpayments, true, true);
                    LOGGER.info("EFT-INWARD BATCH REQ TO CORE:{}", requestToCore);
//                    String response = coreBankingService.processRequestToCore(requestToCore, "api:processBatch");
                    String response = HttpClientService.sendLoanRepaymentXMLRequest(requestToCore, systemVariable.BRINJAL_LOAN_REPAYMENT_URL);
                    LOGGER.info("EFT LOAN REPAYMENT RESP FROM CORE:{}, ... batchRef ...{}", response, batchRef);
                }
            }
        } catch (Exception e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING INWARD BATCH:{}", e.getMessage());
            LOGGER.info(null, e);
        }
    }
    public void processTanescoSaccosInwardEft() {
        //get entries on the queue
        List<Map<String, Object>> result = null;
        int noOfTxns = 0;
        BigDecimal totalAmt = BigDecimal.ZERO;
        String batchRef = SYSENV.generateTransactionReference(5) + "-" + DateUtil.now("yyyyMMddHHmmssSS");
        try {
            result = this.jdbcTemplate.queryForList("select * from transfers where cbs_status='AQ' AND  direction='INCOMING' AND beneficiaryBic='TAPBTZTZ' and code IN ('TANESCO_SACCOSS') LIMIT 500");

            List<BatchTxnEntries> btxns = new ArrayList();
            List<BatchTxnEntries> btxns2 = new ArrayList();
            if (result != null) {
                for (Map<String, Object> txn : result) {

                    BatchTxnEntries btxn = new BatchTxnEntries();
                    BatchTxnEntries btxn2 = new BatchTxnEntries();
                    totalAmt = totalAmt.add(new BigDecimal(txn.get("amount") + ""));
                    btxn.setAmount(new BigDecimal(txn.get("amount") + ""));
                    btxn2.setAmount(new BigDecimal(txn.get("amount") + ""));
                    btxn.setBatch(batchRef);
                    btxn2.setBatch(batchRef);
                    btxn.setBuId("-5");
                    btxn2.setBuId("-5");
                    btxn.setCode("?");
                    btxn2.setCode("?");
                    btxn.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    btxn2.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    btxn.setCrAcct(formatAccountNo(txn.get("destinationAcct") + ""));
                    btxn2.setCrAcct(formatAccountNo(txn.get("destinationAcct") + ""));
                    btxn.setCurrency(txn.get("currency") + "");
                    btxn2.setCurrency(txn.get("currency") + "");
                    if (txn.get("code").equals("TANESCO_SACCOSS")) {
                        btxn.setCrAcct("180207000051");
                        btxn2.setCrAcct("180207000051");
                    }

                    btxn.setModule("EFT-INCOMING");
                    btxn2.setModule("EFT-INCOMING");
                    btxn.setNarration(txn.get("destinationAcct")+" "+txn.get("purpose") + " B/O " + txn.get("sender_name"));
                    btxn2.setNarration(txn.get("destinationAcct")+" "+ txn.get("purpose") + " B/O " + txn.get("sender_name"));
                    btxn.setReverse("N");
                    btxn2.setReverse("N");
                    btxn.setTxnRef(txn.get("reference") + "");
                    btxn2.setTxnRef(txn.get("reference") + "");
                    btxn.setScheme("?");
                    btxn2.setScheme("?");
                    btxn2.setDestinationAccount(formatAccountNo(txn.get("destinationAcct") +""));
                    btxn2.setSenderAccount(formatAccountNo(txn.get("sourceAcct") +""));
                    btxn2.setSenderName(txn.get("sender_name")+"");
                    btxn2.setSenderBic(txn.get("sender_name")+"");
                    btxn2.setBeneficiaryName(txn.get("beneficiaryName")+"");
                    btxn2.setEndToEndReference(txn.get("txid")+"");
                    btxn2.setBatchReference(txn.get("batch_reference")+"");
                    //update the entry after being pulled
                    int updateResult = updateEFTIncomingToCorebanking(btxn);
                    LOGGER.info("update result for txn reference... {} is ... {} code... {}", btxn.getTxnRef(), updateResult, txn.get("code"));
                    if (1 == updateResult) {
                        btxns.add(btxn);
                        btxns2.add(btxn2);
                        noOfTxns++;
                    }
                }
                if (noOfTxns > 0) {
                    BatchPayemntReq bpayments = new BatchPayemntReq();
                    BatchPayemntReq bpayments2 = new BatchPayemntReq();
                    bpayments.setCallbackUrl(systemVariable.EFT_BATCH_CALLBACK_URL);
                    bpayments2.setCallbackUrl(systemVariable.EFT_BATCH_CALLBACK_URL);
                    bpayments.setItemCount(String.valueOf(noOfTxns));
                    bpayments2.setItemCount(String.valueOf(noOfTxns));
                    bpayments.setReference(batchRef);
                    bpayments2.setReference(batchRef);
                    bpayments.setSerialize("false");
                    bpayments2.setSerialize("false");
                    bpayments.setTotalAmount(totalAmt.toString());
                    bpayments2.setTotalAmount(totalAmt.toString());
                    BatchTxnsEntry bEntries = new BatchTxnsEntry();
                    BatchTxnsEntry bEntries2 = new BatchTxnsEntry();
                    bEntries.setTxn(btxns);
                    bEntries2.setTxn(btxns2);
                    bpayments.setTxns(bEntries);
                    bpayments2.setTxns(bEntries2);
                    //insert the batch into eft-batches table
                    logEFTIncomingBatchToCorebanking(bpayments);
                    String requestToCore = XMLParserService.jaxbGenericObjToXML(bpayments, true, true);
                    LOGGER.info("EFT-INWARD BATCH REQ TO CORE:{}", requestToCore);
//                    String requestToCore = XMLParserService.jaxbGenericObjToXML(bpayments, true, true);
                    LOGGER.info("EFT-INWARD BATCH REQ TO CORE:{}", requestToCore);
                    String response = coreBankingService.processRequestToCore(requestToCore, "api:processBatch");
//                    String response = coreBankingService.processRequestToCore(requestToCore, "api:processBatch");
//                    String response = HttpClientService.sendLoanRepaymentXMLRequest(requestToCore, systemVariable.BRINJAL_LOAN_REPAYMENT_URL);
                    //todo process to tanesco saccoss the same batch for processing via a proxy url on brinjal
                    String response2 = HttpClientService.sendLoanRepaymentXMLRequest(bpayments2.toString(), systemVariable.BRINJAL_TANESCO_SACCOSS_PROXY);
                    LOGGER.info("EFT MEANT FOR TANESCO SACCOSS FROM CORE:{}, ... batchRef ...{}", response, batchRef);
                    LOGGER.info("EFT MEANT FOR TANESCO SACCOSS FROM PROXY:{}, ... batchRef ...{}", response2, batchRef);
                }
            }
        } catch (Exception e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING INWARD BATCH:{}", e.getMessage());
            LOGGER.info(null, e);
        }
    }
    public void processEFTReturnsFromCreditGLToOpLedger() {
        //get entries on the queue
        List<Map<String, Object>> result = null;
        int noOfTxns = 0;
        BigDecimal totalAmt = BigDecimal.ZERO;
        String batchRef = SYSENV.generateTransactionReference(5) + "-" + DateUtil.now("yyyyMMddHHmmssSS");
        try {
            result = this.jdbcTemplate.queryForList("select * from transfers where cbs_status='HP' AND direction='OUTGOING' LIMIT 2000");
            List<BatchTxnEntries> btxns = new ArrayList<>();
            if (result != null) {
                for (Map<String, Object> txn : result) {

                    BatchTxnEntries btxn = new BatchTxnEntries();
                    totalAmt = totalAmt.add(new BigDecimal(txn.get("amount") + ""));
                    btxn.setAmount(new BigDecimal(txn.get("amount") + ""));
                    btxn.setBatch(batchRef);
                    btxn.setBuId("-5");
                    btxn.setCode("?");
                    btxn.setCrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    btxn.setCurrency(txn.get("currency") + "");
                    btxn.setDrAcct(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                    btxn.setModule("EFT-HP-RETURN");
                    btxn.setNarration(txn.get("purpose") + " B/O " + txn.get("sender_name"));
                    btxn.setReverse("N");
                    btxn.setTxnRef(txn.get("reference") + "");
                    btxn.setScheme("?");
                    //update the entry after being pulled
                    int updateResult = updateEFTReturn(btxn);
                    LOGGER.info("update result for txn reference... {} is ... {}", btxn.getTxnRef(), updateResult);
                    if (1 == updateResult) {
                        btxns.add(btxn);
                        noOfTxns++;
                    }
                }
                if (noOfTxns > 0) {
                    BatchPayemntReq bpayments = new BatchPayemntReq();
                    bpayments.setCallbackUrl(systemVariable.EFT_BATCH_CALLBACK_URL);
                    bpayments.setItemCount(String.valueOf(noOfTxns));
                    bpayments.setReference(batchRef);
                    bpayments.setSerialize("false");
                    bpayments.setTotalAmount(totalAmt.toString());
                    BatchTxnsEntry bEntries = new BatchTxnsEntry();
                    bEntries.setTxn(btxns);
                    bpayments.setTxns(bEntries);
                    //insert the batch into eft-batches table
                    logEFTOutgoingBatchToCoreBanking(bpayments);
                    String requestToCore = XMLParserService.jaxbGenericObjToXML(bpayments, true, true);
                    LOGGER.info("EFT-HP-RETURN BATCH REQ TO CORE:{}", requestToCore);
                    String response = coreBankingService.processRequestToCore(requestToCore, "api:processBatch");
                    LOGGER.info("EFT-HP-RETURN BATCH RESP FROM CORE:{}", response);
                }
            }
        } catch (Exception e) {
            LOGGER.info("AN ERROR OCCURRED DURING PROCESSING INWARD BATCH:{}", e.getMessage());
            LOGGER.info(null, e);
        }
    }

    public String validateEftInwardBatch(String batchRef) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select * from transfers where cbs_status not in ('C','AQ','QI') AND batch_reference=?", batchRef);
            if (result != null) {
                //get the MESSAGE DETAILS FROM THE QUEUE
                //CHECK ACCOUNT NAME IF IT MATCHES
                //NAME QUERY
//        String accountNameQuery;
//        accountNameQuery = corebanking.accountNameQuery(req.getCdtTrfTxInf().getBeneficiaryAcct()).toUpperCase();//get account Name
                double distance = 53L; //StringUtils.getJaroWinklerDistance(req.getCdtTrfTxInf().getBeneficiaryName().toUpperCase(), accountNameQuery.toUpperCase()) * 100;
                String message1 = "No saving account";
                String responseCode = "53";
                boolean isTANESCO = false;
                String cbsName = "--NiLL--";
                for (Map<String, Object> txn : result) {
                    String sql = "SELECT c.CUST_NM, AC.ACCT_NO,AC.ACCT_NM, CUR.CRNCY_CD_ISO AS CURRENCY_CODE, AC.REC_ST,ASR.REF_DESC FROM  ACCOUNT AC JOIN CURRENCY CUR ON AC.CRNCY_ID = CUR.CRNCY_ID JOIN ACCOUNT_STATUS_REF ASR ON  AC.REC_ST = ASR.REF_KEY JOIN CUSTOMER c ON c.CUST_ID=AC.CUST_ID WHERE (REPLACE(AC.OLD_ACCT_NO, '-', '') = ?   OR AC.ACCT_NO = ?)   AND ROWNUM = 1";
                    LOGGER.info(sql.replace("?", "'{}'"), formatAccountNo(txn.get("destinationAcct").toString()), formatAccountNo(txn.get("destinationAcct").toString()));
                    List<Map<String, Object>> getData = this.jdbcRUBIKONTemplate.queryForList(sql, formatAccountNo(txn.get("destinationAcct").toString()), formatAccountNo(txn.get("destinationAcct").toString()));
                    if (!getData.isEmpty()) {
//                        distance = StringUtils.getJaroWinklerDistance(txn.get("beneficiaryName").toString().toUpperCase().trim(), String.valueOf(getData.get(0).get("ACCT_NM")).toUpperCase().trim()) * 100;
                        List<Map<String, Object>> tanescoVerify = this.jdbcTemplate.queryForList(sql, formatAccountNo(txn.get("destinationAcct").toString()));

                        // Calculate similarity without considering case sensitivity
//                        distance = fullNameAnagramService.getSimilarityPercentage(txn.get("beneficiaryName").toString().toLowerCase().trim(), String.valueOf(getData.get(0).get("ACCT_NM")).toLowerCase().trim());
                        distance = namesMatch(txn.get("beneficiaryName").toString().toLowerCase().trim(), String.valueOf(getData.get(0).get("ACCT_NM")).toLowerCase().trim());
                        cbsName = String.valueOf(getData.get(0).get("CUST_NM")).toUpperCase().trim();
                        message1 = "Account exist:" + getData.get(0).get("ACCT_NO") + " ACCOUNT NAME:" + getData.get(0).get("ACCT_NM");
                        LOGGER.info("Calculate similarity without considering case sensitivity for s1.. {} and s2...{} is {}", txn.get("beneficiaryName").toString().toLowerCase(), String.valueOf(getData.get(0).get("ACCT_NM")).toLowerCase(), distance);
                        if (String.valueOf(getData.get(0).get("REC_ST")).equalsIgnoreCase("A") || String.valueOf(getData.get(0).get("REC_ST")).equalsIgnoreCase("D")) {
                            //update entry and assign it to queue
                            message1 = "success";
                            responseCode = "0";
                        } else {

                            responseCode = "999";
                            message1 = String.valueOf(getData.get(0).get("REF_DESC"));
                        }
                    } else {
                        //todo check tanesco saccoss accounts
                        sql = "select * from saccoss_customers_mapping where accountNo=?";
                        List<Map<String, Object>> tanescoVerify = this.jdbcTemplate.queryForList(sql, formatAccountNo(txn.get("destinationAcct").toString()));
                        if (!tanescoVerify.isEmpty()) {
                            JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
//                            distance = jaroWinklerSimilarity.apply(txn.get("beneficiaryName").toString().toLowerCase().trim(), String.valueOf(tanescoVerify.get(0).get("accountName")).toLowerCase().trim()) * 100;
                            distance = namesMatch(txn.get("beneficiaryName").toString().toLowerCase().trim(), String.valueOf(tanescoVerify.get(0).get("accountName")).toLowerCase().trim()) * 100;
                            cbsName = String.valueOf(tanescoVerify.get(0).get("accountName")).toUpperCase().trim();
                            LOGGER.info("\n*******************************ITS A TANESCO SACCOSS BENEFICIARY *************************** \nACCOUNT NO:{}\nTRANSFER NAME:{} \nVALIDATED NAME:{},\nreference:{}\n*******************************ITS A TANESCO SACCOSS BENEFICIARY ***************************",txn.get("destinationAcct") ,txn.get("beneficiaryName"),tanescoVerify.get(0).get("accountName"),txn.get("reference"));
                            isTANESCO = true;
                            message1 = "success";
                            responseCode = "0";
                        }
                    }
                    if (formatAccountNo(txn.get("destinationAcct").toString()).startsWith("999") || formatAccountNo(txn.get("destinationAcct").toString()).length() == 6) {
                        message1 = "its Meant For Cash Collection (CMS) account. Please post transaction using control number";
                        responseCode = "999";
                        LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A GEPG ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get("batch_reference"), txn.get("txid"), txn.get("amount"));
                        jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), txn.get("txid"), txn.get("batch_reference"));
                    } else if (distance < 78) {
                        responseCode = "777";
                        message1 = "Account name doesn't match by: " + String.format("%.2f", distance) + "% rubikon name: " + cbsName.toUpperCase() + " : Beneficiary name Meant:" + txn.get("beneficiaryName").toString().toUpperCase();
                        LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE BENEFICIARY NAME DOES NoTie MATCH CORE BANKING NAME: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get("batch_reference"), txn.get("txid"), txn.get("amount"));
                        jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), txn.get("txid"), txn.get("batch_reference"));
                    } else if (isGePGaccount(txn.get("destinationAcct").toString(), txn.get("currency").toString()) == 0) {
                        message1 = "its GePG account. Please post transaction using control number";
                        responseCode = "2";
                        LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A GEPG ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get("batch_reference"), txn.get("reference"), txn.get("amount"));
                        jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), txn.get("txid"), txn.get("batch_reference"));

                    } else if (isCMSAccount(txn.get("destinationAcct").toString()) == 0) {
                        message1 = "its CMS account. Please post transaction using control number";
                        responseCode = "2";
                        LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A CMS ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get("batch_reference"), txn.get("reference"), txn.get("amount"));
                        jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), txn.get("txid"), txn.get("batch_reference"));

                    } else {
                        //process single transactions to core banking ....
                        /*
            new approach update cbs status as AQ awaiting processing
            when processing change status to Q meaning its placed on queue
                         */
                        message1 = "Transaction validated and queued ready for processing";
                        responseCode = "766";
                        LOGGER.info("TRANSACTION QUEUED FOR PROCESSING: RESULT:{} , RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get("batch_reference"), txn.get("reference"), txn.get("amount"));
                        if (isTANESCO) {
                            //transaction meant for TANESCO SACCOSS SET CODE TO TANESCO_SCCOSS AND STATUS AQ WITH RESPONSE CODE 222
                            jdbcTemplate.update("update transfers set status='S',code='TANESCO_SACCOSS',cbs_status='AQ',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), txn.get("txid"), txn.get("batch_reference"));
                        } else {
                            jdbcTemplate.update("update transfers set status='S',cbs_status='AQ',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), txn.get("txid"), txn.get("batch_reference"));
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        return "ok";
    }

    public int isCMSAccount(String acct) {
        int result = 404;
        if (acct.equalsIgnoreCase("170227000078")) {//AIRTEL TRUST ACCOUNT
            result = 0;
        }
        String query = "SELECT COUNT(partner_name) FROM tpb_partners WHERE block_incom_stp = 'Y' AND (acct_no = ?  or  new_acct_no=?)";
        LOGGER.debug("SELECT COUNT(partner_name) FROM tpb_partners WHERE block_incom_stp = 'Y' AND (acct_no = '{}'  or  new_acct_no='{}')", acct, acct);
        try {
            int count = this.jdbcPartnersTemplate.queryForObject(query, new Object[]{acct, acct}, Integer.class);
            if (count > 0) {
                result = 0;
            }
        } catch (DataAccessException e) {
            LOGGER.error("isCMSAccount exception", e);
            result = -1;
            return result;
        }

        return result;

    }

    /*
    process batch callback
     */
    /*process callback from core banking*/
    public HashMap<String, String> processCorebankingCallback(HashMap<String, String> data) {
        try {
            LOGGER.info("CALLBACK FROM CORE INWARD-BATCH PAYMENTS :{}", data);
            //check if payment is successfully
            if ((data.get("result").equalsIgnoreCase("00") || data.get("result").equalsIgnoreCase("26")) && data.get("reverse").equalsIgnoreCase("N")) {
//update transfers table
                jdbcTemplate.update(" UPDATE  transfers set  status='S',response_code =?,cbs_status='C',comments=?,message=? WHERE reference=?",
                        "0", "settled", "settled", data.get("txnRef"));

            }else {
                jdbcTemplate.update(" UPDATE  transfers set  status='S',response_code=?,cbs_status='F',comments=?,message=? WHERE reference=?",
                        data.get("result"), "failed to settled, Reason:" + data.get("result"), "failed to settled, Reason:" + data.get("result"), data.get("txnRef"));
            }
        } catch (Exception e) {
            LOGGER.info("processCorebankingCallback: jsonException{}", e.getMessage());
            LOGGER.info(null, e);

        }
        LOGGER.info("processCorebankingCallback: req:{} resp:{}", data, data);
        return data;
    }

    public String formatAccountNo(String unformattedAcct) {
        String account = unformattedAcct;
        if (account.startsWith("00000")) {
            account = unformattedAcct.substring(4, unformattedAcct.length());
//            String account2 = account;
//            if (account.startsWith("00") && account.length() == 12) {
//                account = unformattedAcct.substring(1, account2.length());
//            }
        }
        if (account.startsWith("000")) {
            account = unformattedAcct.substring(3, unformattedAcct.length());
        }
        if (account.length() == 13) {
            if (account.startsWith("0")) {
                account = unformattedAcct.substring(1, unformattedAcct.length());
            }
            if (account.startsWith("00")) {
                account = unformattedAcct.substring(2, unformattedAcct.length());
            }
        }
        return account;
    }

    /*
     *GET TRANSACTIONS READY FOR EFT SESSIONS PER BANK
     */
    public List<Map<String, Object>> getEftBatchTransactionPerBank(String benBIC) {

        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select\n"
                    + "	a.*,\n"
                    + "	(\n"
                    + "	select\n"
                    + "		count(*)\n"
                    + "	from\n"
                    + "		transfers b\n"
                    + "	where\n"
                    + "		b.status = 'C'\n"
                    + "		and b.cbs_status = 'C'\n"
                    + "		and b.txn_type = '005'\n"
                    + "		and b.beneficiaryBIC =? and direction='OUTGOING') as noOfTxns,\n"
                    + "	(\n"
                    + "	select\n"
                    + "		SUM(amount)\n"
                    + "	from\n"
                    + "		transfers b\n"
                    + "	where\n"
                    + "		b.status = 'C'\n"
                    + "		and b.cbs_status = 'C'\n"
                    + "		and b.txn_type = '005'\n"
                    + "		and beneficiaryBIC = ? and direction='OUTGOING') as totalAmount\n"
                    + "from\n"
                    + "	transfers a\n"
                    + "where\n"
                    + "	status = 'C'\n"
                    + "	and cbs_status = 'C'\n"
                    + "	and txn_type = '005'\n"
                    + "	and beneficiaryBIC =? and direction='OUTGOING'", benBIC, benBIC, benBIC);
        } catch (Exception ex) {
            ex.printStackTrace();
            result = null;
        }
        return result;
    }

    public String replayEFTIncomingTOCBS(String optionType, String branchNo, UsRole role, List<String> txnid) {
        String result = "{\"result\":\"0\",\"message\":\"Transaction is being Processed please confirm on RUBIKON\"}";

        taskExecutor.execute(() -> {
            for (String reference : txnid) {
                String identifier = "GL2GL";
                String destinationAccount = "-1";
                switch (optionType) {
                    case "1":
                        //PROCESS TO EFT HQ
                        destinationAccount = systemVariable.EFT_HQ_TRANSFER_AWAITING;
                        break;
                    case "2":
                        //PROCESS TO TANESCO SACCOSS COLLECTION ACCOUNT
                        destinationAccount = systemVariable.TANESCO_SACCOSS_COLLECTION;
                        identifier = "GL2ACCT";
                        break;
                    case "3":
                        //PROCESS TO BRANCH TRANSFER AWAITING
                        destinationAccount = systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", branchNo);
                        break;
                    case "4":
                        //PROCESS TO INSURANCE COMMISSION GL
                        destinationAccount = systemVariable.INSURANCE_COMMISSION_GL_ACCOUNT;
                        break;
                    case "5":
                        //PROCESS TO INSURANCE COMMISSION GL
                        destinationAccount = "-1";
                        identifier = "replay";
                        break;
                    case "6":
                        //PROCESS TO AIRTEL_DGS_COLLECTION_GL
                        destinationAccount = systemVariable.AIRTEL_DGS_COLLECTION_GL;
                        break;
                    case "7":
                        //PROCESS TO AIRTEL_DGS_COLLECTION_GL
                        destinationAccount = systemVariable.TIGO_DGS_COLLECTION_GL;
                        break;
                    case "8":
                        //PROCESS TO AIRTEL_DGS_COLLECTION_GL
                        destinationAccount = systemVariable.HALOTEL_DGS_COLLECTION_GL;
                        break;
                    case "9":
                        //PROCESS TO AIRTEL_DGS_COLLECTION_GL
                        destinationAccount = systemVariable.MKOBA_DGS_COLLECTION_GL;
                        break;
                    default:
                        //No case defined
                        destinationAccount = "000";
                }
                ReplayIncomingTransactionReq replayReq = new ReplayIncomingTransactionReq();
                replayReq.setDestinationAcct(destinationAccount);
                replayReq.setReference(reference);
                replayReq.setIdentifier(identifier);
                replayReq.setUsRole(role);
                System.out.println("REPLAY TRANSACTION TO DESTINATION ACCOUNT:" + destinationAccount);

                LOGGER.info("The replay to eft transaction request: {}", replayReq);
                queProducer.sendToQueueEftReplayToCoreBanking(replayReq);
            }
        });
        return result;
    }

    public EftPacs00800102OutgoingReq generateOutgoingIso20022() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String formattedDate = df.format(Calendar.getInstance().getTime());

        EftPacs00800102OutgoingReq outgoingReq = new EftPacs00800102OutgoingReq();
        List<Map<String, Object>> banks = banksRepo.getLocalBanksList();
        if (banks != null) {
            for (int j = 0; j < banks.size(); j++) {
                String bankSwiftCode = "-1";
                if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
                    bankSwiftCode = banks.get(j).get("swift_code").toString();
                } else {
                    bankSwiftCode = banks.get(j).get("swift_code_test").toString();
                }
                List<Map<String, Object>> txns = getEftBatchTransactionPerBank(bankSwiftCode);
                if (txns != null && txns.size() > 0) {
                    String references = "'0'";
                    try {
                        outgoingReq.setMsgId("005E" + formattedDate + bankSwiftCode.substring(0, 2));
                        outgoingReq.setNbOfTxs(String.valueOf(txns.get(0).get("noOfTxns").toString()));
                        outgoingReq.setTtlIntrBkSttlmAmt(new BigDecimal(txns.get(0).get("totalAmount").toString()));
                        //check time and process the transactions on the ahead date

                        String dayNames[] = new DateFormatSymbols().getWeekdays();
                        Calendar date2 = Calendar.getInstance();
                        String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];
                        LOGGER.info("DAY OF THE WEEK: {}", todayName);
//                        LOGGER.info("GET THE DAY AHEAD: {}", outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNextDay(1,8,0,0)));
                        int currentHour = date2.get(Calendar.HOUR_OF_DAY);
                        //
                        outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNow());
                        outgoingReq.setIntrBkSttlmDt(DateUtil.getXMLGregorianCalendarNow());
                        //check exceptions for formatting date on the required dates
                        if (currentHour > 17) {
                            outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNextDay(1, 8, 0, 0));
                            outgoingReq.setIntrBkSttlmDt(DateUtil.getXMLGregorianCalendarNextDay(1, 8, 0, 0));
                        }
                        //CHECK CUTT-OFF TIME
                        if (todayName.equalsIgnoreCase("saturday")) {
                            if (currentHour > 12) {
                                //set the transaction to setlle on monday
                                outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNextDay(2, 8, 0, 0));
                                outgoingReq.setIntrBkSttlmDt(DateUtil.getXMLGregorianCalendarNextDay(2, 8, 0, 0));
                            }
                        }
                        if (todayName.equalsIgnoreCase("sunday")) {
                            ///  if (currentHour > 12) {
                            //set the transaction to setlle on monday
                            outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNextDay(1, 8, 0, 0));
                            outgoingReq.setIntrBkSttlmDt(DateUtil.getXMLGregorianCalendarNextDay(1, 8, 0, 0));
                            //}
                        }

                        outgoingReq.setSttlmMtd("ACH");
                        outgoingReq.setTtlIntrBkSttlmCcy("TZS");
                        List<EftOutgoingBulkPaymentsReq> transactions = new ArrayList();
                        for (int i = 0; i < txns.size(); i++) {
                            EftOutgoingBulkPaymentsReq custTxns = new EftOutgoingBulkPaymentsReq();
                            custTxns.setTxId(txns.get(i).get("reference").toString());
                            custTxns.setAmount(new BigDecimal(txns.get(i).get("amount").toString()));
                            custTxns.setSenderName(txns.get(i).get("sender_name").toString());
                            custTxns.setSenderAccount(txns.get(i).get("sourceAcct").toString());
                            custTxns.setBeneficiaryBIC(txns.get(i).get("beneficiaryBIC").toString());
                            custTxns.setBeneficiaryName(txns.get(i).get("beneficiaryName").toString());
                            custTxns.setBeneficiaryAcct(txns.get(i).get("destinationAcct").toString());
                            custTxns.setPurpose(txns.get(i).get("purpose").toString());
                            references += ",'" + txns.get(i).get("reference").toString() + "'";
                            transactions.add(custTxns);
                            //insert the transaction as outgoing
                        }
                        outgoingReq.setCdtTrfTxInf(transactions);
                        String fileName = bankSwiftCode + DateUtil.now("yyyyMMddHms") + ".i";
                        //INSERT THE OUTGOING BATCH TO EFT BATCHES
                        insertEFTBacthOutgoing(outgoingReq, fileName, "060");
                        //generate xml with signature
                        String rawXmlsigned = generateMxPacs00800102RawXml(outgoingReq, bankSwiftCode);
                        //UPDATE THE TRANSACTIONS ON TRANSFER TABLE AND SET OUTGOING BATCH REFERENCE ON BATCHREFERENCE2
                        updateEFTTxnsWithOutgoingBatchReference(outgoingReq.getMsgId(), references);
                        //render the xml to queue broker
                        queProducer.sendToQueueEftFrmCBSToTACH(fileName + "^" + rawXmlsigned);
                    } catch (IOException ex) {
                        LOGGER.info(null, ex);
                        LOGGER.info("AN ERROR OCCURED DURING GENERATING OUTGOING MESSAGE: {}", ex.getMessage());
                    }

                }
            }

        }
        return null;
    }

    //    public String testGenerateOutgoingIso20022() throws IOException {
//        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
//        String formattedDate = df.format(Calendar.getInstance().getTime());
//
//        EftPacs00800102OutgoingReq outgoingReq = new EftPacs00800102OutgoingReq();
//
//        outgoingReq.setMsgId("005E" + "NLCBTZT0".substring(0, 2) + System.currentTimeMillis());
//        outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNow());
//        outgoingReq.setNbOfTxs("1");
//        outgoingReq.setTtlIntrBkSttlmAmt(new BigDecimal("1200"));
//        outgoingReq.setIntrBkSttlmDt(DateUtil.getXMLGregorianCalendarNow());
//        outgoingReq.setSttlmMtd("ACH");
//        outgoingReq.setTtlIntrBkSttlmCcy("TZS");
//        List<EftOutgoingBulkPaymentsReq> transactions = new ArrayList();
//        EftOutgoingBulkPaymentsReq custTxns = new EftOutgoingBulkPaymentsReq();
//        custTxns.setTxId("005TTTT" + System.currentTimeMillis());
//        custTxns.setAmount(new BigDecimal("1200"));
//        custTxns.setSenderName("MICHAEL KAZIMOTO DANIEL");
//        custTxns.setSenderAccount("110210111118");
//        custTxns.setBeneficiaryBIC("NLCBTZT0");
//        custTxns.setBeneficiaryName("Katamba Leonardo");
//        custTxns.setBeneficiaryAcct("001jjj00019919");
//        custTxns.setPurpose("TEST SIGNATURE");
//        transactions.add(custTxns);
//
//        outgoingReq.setCdtTrfTxInf(transactions);
//        String fileName = "NLCBTZT0" + DateUtil.now("yyyyMMddHms") + ".i";
//        //generate xml with signature
//        String rawXmlsigned = iso20022Service.testGenerateMxPacs00800102RawXml(outgoingReq);
//
//        return fileName + "^" + rawXmlsigned;
//    }
    public EftPacs00800102OutgoingReq generateOutgoingIso20022Pain00100103() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String formattedDate = df.format(Calendar.getInstance().getTime());

        EftPacs00800102OutgoingReq outgoingReq = new EftPacs00800102OutgoingReq();
        List<Map<String, Object>> banks = banksRepo.getLocalBanksList();
        if (banks != null) {
            for (int j = 0; j < banks.size(); j++) {
                String bankSwiftCode = "-1";
                if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
                    bankSwiftCode = banks.get(j).get("swift_code").toString();
                } else {
                    bankSwiftCode = banks.get(j).get("swift_code_test").toString();
                }
                List<Map<String, Object>> txns = getEftBatchTransactionPerBank(bankSwiftCode);
                if (txns != null && txns.size() > 0) {
                    outgoingReq.setMsgId("005E" + formattedDate + bankSwiftCode.substring(0, 2));
                    outgoingReq.setCreDtTm(DateUtil.getXMLGregorianCalendarNow());
                    outgoingReq.setNbOfTxs(String.valueOf(txns.get(0).get("noOfTxns").toString()));
                    outgoingReq.setTtlIntrBkSttlmAmt(new BigDecimal(txns.get(0).get("totalAmount").toString()));
                    outgoingReq.setIntrBkSttlmDt(DateUtil.getXMLGregorianCalendarNow());
                    outgoingReq.setSttlmMtd("ACH");
                    outgoingReq.setTtlIntrBkSttlmCcy("TZS");
                    List<EftOutgoingBulkPaymentsReq> transactions = new ArrayList();
                    for (int i = 0; i < txns.size(); i++) {
                        EftOutgoingBulkPaymentsReq custTxns = new EftOutgoingBulkPaymentsReq();
                        custTxns.setTxId(txns.get(i).get("reference").toString());
                        custTxns.setAmount(new BigDecimal(txns.get(i).get("amount").toString()));
                        custTxns.setSenderName(txns.get(i).get("sender_name").toString());
                        custTxns.setSenderAccount(txns.get(i).get("sourceAcct").toString());
                        custTxns.setBeneficiaryBIC(txns.get(i).get("beneficiaryBIC").toString());
                        custTxns.setBeneficiaryName(txns.get(i).get("beneficiaryName").toString());
                        custTxns.setBeneficiaryAcct(txns.get(i).get("destinationAcct").toString());
                        custTxns.setPurpose(txns.get(i).get("purpose").toString());
                        transactions.add(custTxns);
                        //UPDATE THE TRANSACTIONS ON TRANSFER TABLE AND SET OUTGOING BATCH REFERENCE ON BATCHREFERENCE2
                        updateEFTTxnsWithOutgoingBatchReference(outgoingReq.getMsgId(), txns.get(i).get("reference").toString());
                        //insert the transaction as outgoing
                    }
                    outgoingReq.setCdtTrfTxInf(transactions);
                    String fileName = bankSwiftCode + DateUtil.now("yyyyMMddHms") + ".i";
                    //INSERT THE OUTGOING BATCH TO EFT BATCHES
                    insertEFTBacthOutgoing(outgoingReq, fileName, "060");
                    //generate xml with signature
                    String rawXmlsigned = generatePacs00100103Message(outgoingReq);
//                        String raw2Xmlsigned = iso20022Service.generateMxPacs00800102CITIRawXml(outgoingReq, banks.get(j).get("swift_code").toString());

//render the xml to queue broker
//                        queProducer.sendToQueueEftFrmCBSToTACH(fileName + "^" + rawXmlsigned);
                }
            }

        }
        return null;
    }

    /*
     * Update outgoing transaction with outgoing batch reference
     */
    public Integer updateEFTTxnsWithOutgoingBatchReference(String batchReference, String reference) {
        Integer res = -1;
        try {
            res = jdbcTemplate.update(" UPDATE  transfers set batch_reference2=?,comments='Transaction File Generated and Submitted to TACH', status='S' WHERE reference IN (" + reference + ")",
                    batchReference);
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer insertEFTBacthOutgoing(EftPacs00800102OutgoingReq req, String fileName, String branchNo) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT  INTO transfer_eft_batches(txn_type, batch_reference, total_amount, number_of_txns, branch_no,narration,status,direction,source_file_name,initiated_by)"
                            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                    "005", req.getMsgId(), req.getTtlIntrBkSttlmAmt(), req.getNbOfTxs(), branchNo, " ", "S", "OUTGOING", fileName, "SYSTEM");
            LOGGER.info(">>>>>>REJECTION BATCH<<<<<<< INSERTING OUTGOING EFT BATCH REJECTION TXN: BATCH REFERENCE :{} BATCH TOTAL AMOUNT: {} NUMBER OF TRANSACTIONS: {} ", req.getMsgId(), req.getTtlIntrBkSttlmAmt(), req.getNbOfTxs());
        } catch (Exception e) {
            LOGGER.info(">>>>>BATCH<<<<< ERROR ON INSERTING EFT REJECTION OUTGOING BATCH TRANSACTION: {},BATCH REFERENCE:{},SOURCE FILE: {}", e.getMessage(), req.getMsgId(), fileName);
            result = -1;
        }
        return result;
    }

    public Integer updateEFTIncomingToOutwardRejection(String batchReference, String reference, String returnMessage) {
        Integer res = -1;
        try {

            res = jdbcTemplate.update(" UPDATE  transfers set batch_reference2=?, status='S',cbs_status='R',modified_dt=now(),returned_dt=now(),message=? WHERE reference=?",
                    batchReference, returnMessage, reference);
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer updateEFTIncomingToCorebanking(BatchTxnEntries txn) {
        Integer res = -1;
        try {
            String sql = "UPDATE  transfers set batch_reference2=?, status='S',cbs_status='QI',message=? WHERE reference=?";
            LOGGER.info(sql.replace("?", "'{}'"), txn.getBatch(), "Transaction assigned to the queue for processing", txn.getTxnRef());
            res = jdbcTemplate.update(sql,
                    txn.getBatch(), "Transaction assigned to the queue for processing", txn.getTxnRef());
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer updateEFTReturn(BatchTxnEntries txn) {
        Integer res = -1;
        try {
            String sql = "UPDATE transfers set batch_reference2=?,cbs_status='QI',message=? WHERE txid=?";
            LOGGER.info(sql.replace("?", "'{}'"), txn.getBatch(), "Transaction assigned to the queue for processing", txn.getTxnRef());
            res = jdbcTemplate.update(sql,
                    txn.getBatch(), "Transaction assigned to the queue for processing", txn.getTxnRef());
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
    INSERT INTO BATCH TRANSACTIONS FOR PROCESSING INCOMING EFT TRANSACTIONS
     */
    public Integer logEFTIncomingBatchToCorebanking(BatchPayemntReq txn) {
        Integer res = -1;
        try {

            res = jdbcTemplate.update(" INSERT INTO transfer_eft_batches (txn_type,batch_reference,total_amount,number_of_txns,branch_no,narration,status,direction,source_file_name,initiated_by,create_dt,approved_by,approved_dt,authorized_by,authorized_dt)\n"
                            + "	VALUES ('005',?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    txn.getReference(), txn.getTotalAmount(), txn.getItemCount(), "060", "assigned entries for processing to core banking", "Q", "INCOMING", "--Nill--", "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer logEFTOutgoingBatchToCoreBanking(BatchPayemntReq txn) {
        Integer res = -1;
        try {
            res = jdbcTemplate.update(" INSERT INTO transfer_eft_batches (txn_type,batch_reference,total_amount,number_of_txns,branch_no,narration,status,direction,source_file_name,initiated_by,create_dt,approved_by,approved_dt,authorized_by,authorized_dt)\n"
                            + "	VALUES ('005',?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    txn.getReference(), txn.getTotalAmount(), txn.getItemCount(), "060", "assigned entries for processing to core banking", "Q", "OUTGOING", "--Nill--", "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveEFTRejectionTransaction(String txn_type, String sourceAcct, String destinationAcct, String amount, String currency, String beneficiaryName, String beneficiaryBIC, String beneficiary_contact, String senderBIC, String sender_phone, String sender_address, String sender_name, String reference, String txid, String instrId, String batch_reference, String batch_reference2, String code, String status, String purpose, String direction, String originallMsgNmId, String initiated_by, String branch_approved_by, String hq_approved_by, String value_date, String branch_approved_dt, String hq_approved_dt, String branch_no, String cbs_status) {
        Integer res = -1;
        try {
            res = jdbcTemplate.update("INSERT INTO transfers( txn_type,sourceAcct,destinationAcct,amount,currency,beneficiaryName,beneficiaryBIC,beneficiary_contact,senderBIC,sender_phone,sender_address,sender_name,reference,txid,instrId,batch_reference,batch_reference2,code,status,purpose,direction,originallMsgNmId,initiated_by,branch_approved_by,hq_approved_by,value_date,branch_approved_dt,hq_approved_dt,branch_no,cbs_status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, purpose, direction, originallMsgNmId, initiated_by, branch_approved_by, hq_approved_by, value_date, branch_approved_dt, hq_approved_dt, branch_no, cbs_status);
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer insertEFTBacthOutgoingRejection(EftPacs00400102Req req, String fileName, String branchNo) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT  INTO transfer_eft_batches(txn_type, batch_reference, total_amount, number_of_txns, branch_no,narration,status,direction,source_file_name,initiated_by)"
                            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                    "005", req.getMessageId(), req.getTotalReturnedIntrBnkSettlmntAmt(), req.getNbOfTxs(), branchNo, " ", "S", "OUTGOING", fileName, "SYSTEM");
            LOGGER.info(">>>>>>BATCH<<<<<<< INSERTING OUTGOING EFT TXN: BATCH REFERENCE :{} BATCH TOTAL AMOUNT: {} NUMBER OF TRANSACTIONS: {} ", req.getMessageId(), req.getTotalReturnedIntrBnkSettlmntAmt(), req.getNbOfTxs());
        } catch (Exception e) {
            LOGGER.info(">>>>>BATCH<<<<< ERROR ON INSERTING EFT OUTGOING BATCH TRANSACTION: {},BATCH REFERENCE:{},SOURCE FILE: {}", e.getMessage(), req.getMessageId(), fileName);
            result = -1;
        }
        return result;
    }

    public Integer updateEftOutwardBatchOnResponseFrmBOT(String batchReference, String reference, String status, String reason) {
        Integer res = -1;
        try {
            String mainSql = "UPDATE  transfers set batch_reference2=?, status='S',cbs_status='R' WHERE reference=?";
            if (status.equalsIgnoreCase("ACCP")) {
                mainSql = "UPDATE  transfers set status='ACCP' WHERE batch_reference=?";
                res = jdbcTemplate.update(mainSql, batchReference, reference);
            }
            if (status.equalsIgnoreCase("PART")) {
                mainSql = "UPDATE  transfers set batch_reference2=?, status='F' WHERE batch_reference=?";
            }

        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public String eftErrorCodes(String code) {
        String result = "Failed ";
        if (code.contains("AC01")) {
            result = "Incorrect account Number";
        }
        if (code.contains("AC04")) {
            result = "Closed Account Number";
        }
        if (code.contains("AC06")) {
            result = "Blocked Account Number";
        }
        if (code.contains("AG01")) {
            result = "Transaction Forbidden";
        }
        if (code.contains("AG02")) {
            result = "Invalid Bank operational Code";
        }
        if (code.contains("AM01")) {
            result = "Zero amount";
        }
        if (code.contains("AM02")) {
            result = "Amount not allowed";
        }
        if (code.contains("AM03")) {
            result = "Currency not allowed";
        }
        if (code.contains("AM04")) {
            result = "Insufficient Funds";
        }
        if (code.contains("AM05")) {
            result = "Duplication(This Message appears to have been duplicated)";
        }
        if (code.contains("AM06")) {
            result = "Specified transaction amount is less than agreed minimum";
        }
        if (code.contains("AM07")) {
            result = "Amount specified in message has been blocked by regulatory authorities";
        }
        if (code.contains("AM09")) {
            result = "Amount received is not the amount agreed or expected";
        }
        if (code.contains("AM10")) {
            result = "Sum of instructed amounts does not equal the control sum";
        }
        if (code.contains("BE01")) {
            result = "Identification of end Customer is not consistent with associated account number";
        }
        if (code.contains("BE04")) {
            result = "Missing creditor address";
        }
        if (code.contains("BE05")) {
            result = "Unrecognised initiating party";
        }
        if (code.contains("BE06")) {
            result = "Unknown end customer";
        }
        if (code.contains("BE07")) {
            result = "Missing debtor Address";
        }
        if (code.contains("DT01")) {
            result = "invalid date";
        }
        if (code.contains("ED01")) {
            result = "Corresponding bank not possible";
        }
        if (code.contains("ED03")) {
            result = "Balance of payments complementary info is requested";
        }
        if (code.contains("ED05")) {
            result = "Settlement of the transaction failed";
        }
        if (code.contains("MD01")) {
            result = "No mandate";
        }
        if (code.contains("MD02")) {
            result = "Missing mandatory information in mandate";
        }
        if (code.contains("MD03")) {
            result = "Invalid file format for other reason than grouping indicator";
        }
        if (code.contains("MD04")) {
            result = "File format incorrect in terms of grouping indicator";
        }
        if (code.contains("MD05")) {
            result = "Collection Not Due";
        }
        if (code.contains("MD06")) {
            result = "Return of funds requested by end customer";
        }
        if (code.contains("NARR")) {
            result = "Reason is provided as narrative information in the additional reason information";
        }
        if (code.contains("AG01")) {
            result = "Transaction Forbidden";
        }
        if (code.contains("MD07")) {
            result = "Customer Deceased";
        }
        if (code.contains("MS02")) {
            result = "By Order of the Beneficiary";
        }
        if (code.contains("DC01")) {
            result = "Beneficiary name Incomplete/irregular";
        }
        if (code.contains("DC02")) {
            result = "Payer Name incomplete/Irregular";
        }
        if (code.contains("DC03")) {
            result = "Missing Payer Account";
        }
        if (code.contains("DC04")) {
            result = "Missing beneficiary reference/control number";
        }
        if (code.contains("DC05")) {
            result = "Incorrect beneficiary reference/control number";
        }
        if (code.contains("ACSC")) {
            result = "Accepted settlement completed";
        }
        if (code.contains("ACSP")) {
            result = "Accepted settlementIn process ";
        }
        if (code.contains("ACTC")) {
            result = "Authentication and syntactical and semantical validation are successful";
        }
        if (code.contains("PDNG")) {
            result = "Pending";
        }
        if (code.contains("RJCT")) {
            result = "Rejected";
        }
        if (code.contains("RJCT")) {
            result = "Rejected";
        }
        if (code.contains("DU04")) {
            result = "Duplicate end to end id";
        }
        if (code.contains("FF01")) {
            result = "Invalid File Format/Invalid settlement date";
        }
        if (code.contains("FF02")) {
            result = "Syntax error";
        }
        if (code.contains("FF03")) {
            result = "Invalid payment type information";
        }
        if (code.contains("FF04")) {
            result = "Invalid service level code";
        }
        if (code.contains("FF05")) {
            result = "Invalid local instrument code";
        }
        if (code.contains("FF06")) {
            result = "Invalid category purpose code";
        }
        if (code.contains("FF06")) {
            result = "Invalid category purpose code";
        }
        if (code.contains("FF07")) {
            result = "Invalid purpose";
        }
        if (code.contains("FF08")) {
            result = "Invalid end to end id";
        }
        if (code.contains("FF09")) {
            result = "Invalid Cheque Number";
        }
        if (code.contains("FF10")) {
            result = "Bank System Processing Error";
        }
        return result;

    }

    public Map<String, BigDecimal> getChargeSpliting(BigDecimal chargeAMount) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal exerciseDuty = chargeAMount.multiply(new BigDecimal("0.10"));
        BigDecimal valueAddedTax = (chargeAMount.add(exerciseDuty)).multiply(new BigDecimal("0.18"));
        BigDecimal incomeAmount = chargeAMount.subtract(exerciseDuty.add(valueAddedTax));
        result.put("exerciseDuty", exerciseDuty);
        result.put("VAT", valueAddedTax);
        result.put("income", incomeAmount);
        return result;

    }

    public String getOutwardEFTBatches(String txnType, String txnStatus, String amount, String reference, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        System.out.println("FROM DATE:" + fromDate + " to date:" + toDate);
        List<Map<String, Object>> results = null;
        String mainSql = "";
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            String searchQuery = "";
            if (!txnType.equals("null")) {
                switch (txnType) {
                    case "OTB"://outward transactions batches
                        mainSql = "select count(*) from transfer_eft_batches  where  txn_type = '005' and create_dt>=? and create_dt <=?";
                        searchValue = "'%" + searchValue + "%'";
                        searchQuery = " WHERE  concat(sourceAccount,' ',senderName,' ',batch_reference,' ',total_amount ,' ',number_of_txns,' ',debit_mandate ) LIKE ? and  txn_type='005' and create_dt>=? and create_dt <=?";
                        break;
                    case "OSPB"://outward summary per bank
                        mainSql = "select count(*) from transfers  where  txn_type = '005' and create_dt>=? and create_dt <=? and direction='OUTGOING' group by beneficiaryBIC";
                        searchValue = "'%" + searchValue + "%'";
                        searchQuery = " WHERE  concat(beneficiaryBIC,' ',amount) LIKE ? and  txn_type='005' and create_dt>=? and create_dt <=? group by beneficiaryBIC";
                        break;
                    case "ITB"://outward summary per bank
                        mainSql = "select count(*) from transfers  where  txn_type = '005' and create_dt>=? and create_dt <=? and direction='INCOMING' group by beneficiaryBIC";
                        searchValue = "'%" + searchValue + "%'";
                        searchQuery = " WHERE  concat(beneficiaryBIC,' ',amount) LIKE ? and  txn_type='005' and create_dt>=? and create_dt <=? AND direction='INCOMING' group by beneficiaryBIC";
                        break;
                    case "OSPBR"://outward summary per bank
                        mainSql = "select count(*) from transfers  where  txn_type = '005' and create_dt>=? and create_dt <=? and direction='INCOMING' group by beneficiaryBIC";
                        searchValue = "'%" + searchValue + "%'";
                        searchQuery = " WHERE  concat(beneficiaryBIC,' ',amount) LIKE ? and  txn_type='005' and create_dt>=? and create_dt <=? AND direction='INCOMING' group by beneficiaryBIC";
                        break;
                }
            }
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate}, Integer.class);

            if (!searchValue.equals("")) {
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_eft_batches  " + searchQuery, new Object[]{searchValue, fromDate, toDate}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "elect * from transfer_eft_batches " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate});

            } else {
                mainSql = "select * from transfer_eft_batches   where txn_type = '005' and create_dt>=? and create_dt <=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public List<TransferTransactions> getEftOutwardTranstion(String txnDate, String txnType) {

        try {
            List<TransferTransactions> transactions
                    = this.jdbcTemplate.query("select * from txns_types where ttype=? AND isAllowed=1", new Object[]{txnType}, (ResultSet rs, int rowNum) -> {
                //rowspan++;
                TransferTransactions transaction = new TransferTransactions();
                transaction.setAmount(rs.getString("amount"));
                transaction.setBatchReference("batch_reference");
                transaction.setBeneficiaryBIC("beneficiaryBIC");
                transaction.setBeneficiaryName("beneficiaryName");
                transaction.setBranchNo("");
                return transaction;
            });
            return transactions;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String reprocessChargeSpliting(String batchReference) {
        BatchPaymentReq req = generateBatchForSingleDebit(batchReference);
        queProducer.sendToQueueChargeSpilitingINCOME(req);
        queProducer.sendToQueueChargeSpilitingExereciseDuty(req);
        queProducer.sendToQueueChargeSpilitingValueAddedTax(req);
        return "received Successfully";
    }

    public List<String> getBanksForEft() {
        try {
            List<String> data = jdbcTemplate.queryForList("select swift_code from banks where identifier='LOCAL'", String.class);
            if (data != null) {
                data.add(systemVariable.SENDER_BIC);
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String generateMxPacs00800102RawXml(EftPacs00800102OutgoingReq req, String bankCode) throws IOException {

        /*
         * Initialize the MX object
         */
        String content = null;
        MxPacs00800102 mx = new MxPacs00800102();
        mx.setFIToFICstmrCdtTrf(new FIToFICustomerCreditTransferV02().setGrpHdr(new GroupHeader33()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setMsgId(req.getMsgId());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setCreDtTm(req.getCreDtTm());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setTtlIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy(req.getTtlIntrBkSttlmCcy()).setValue(req.getTtlIntrBkSttlmAmt()));
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setIntrBkSttlmDt(req.getCreDtTm());
        /*
         * Settlement Information
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setSttlmInf(new SettlementInformation13());
        mx.getFIToFICstmrCdtTrf().getGrpHdr().getSttlmInf().setSttlmMtd(SettlementMethod1Code.CLRG).setClrSys(new ClearingSystemIdentification3Choice().setPrtry("ACH"));

        /*
         * Instructing Agent
         */
        mx.getFIToFICstmrCdtTrf().getGrpHdr().setInstgAgt(
                (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                        (new FinancialInstitutionIdentification7()).setBIC(systemVariable.SENDER_BIC)));

        /*
         * Transaction Identification
         */
 /*
LOOP THE BANK TRANSACTIONS AND GET ALL CUSTOMERS
         */
        for (int i = 0; i < req.getCdtTrfTxInf().size(); i++) {
            /*
             * Payment Transaction Information
             */
            CreditTransferTransactionInformation11 cti = new CreditTransferTransactionInformation11();

            cti.setPmtId(new PaymentIdentification3());
            cti.getPmtId().setInstrId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setEndToEndId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.getPmtId().setTxId(req.getCdtTrfTxInf().get(i).getTxId());
            cti.setPmtTpInf(new PaymentTypeInformation21().setSvcLvl(new ServiceLevel8Choice().setCd("SEPA")));//setSvcLvl(new ServiceLevel8Choice());

            /*
             * Transaction Amount
             */
            ActiveCurrencyAndAmount amount = new ActiveCurrencyAndAmount();
            amount.setCcy("TZS");
            amount.setValue(req.getCdtTrfTxInf().get(i).getAmount());
            cti.setIntrBkSttlmAmt(amount);

            /*
             * Transaction Value Date
             */
            //cti.setIntrBkSttlmDt(getXMLGregorianCalendarNow());
            /*
             * Transaction Charges
             */
            cti.setChrgBr(ChargeBearerType1Code.SLEV);//ACCP//RJCK//.........

            /*
             * Orderer Name & Address
             */
            cti.setDbtr(new PartyIdentification32());
            cti.getDbtr().setNm(req.getCdtTrfTxInf().get(i).getSenderName());
//                    cti.getDbtr().setPstlAdr((new PostalAddress6()).addAdrLine("310 Field Road, NY"));
            cti.getDbtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(systemVariable.SENDER_BIC)));
            /*
             * Orderer Account
             */
            cti.setDbtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount()))));
            /*
             * Order Financial Institution
             */
            cti.setDbtrAgt(
                    (new BranchAndFinancialInstitutionIdentification4()).setFinInstnId(
                            (new FinancialInstitutionIdentification7()).setBIC(systemVariable.SENDER_BIC)));

            /*
             * Beneficiary Institution
             */
            cti.setCdtrAgt((new BranchAndFinancialInstitutionIdentification4()).setFinInstnId((new FinancialInstitutionIdentification7()).setBIC(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));

            /*
             * Beneficiary Name & Address
             */
            cti.setCdtr(new PartyIdentification32());
            cti.getCdtr().setNm(req.getCdtTrfTxInf().get(i).getBeneficiaryName());
            cti.getCdtr().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getCdtTrfTxInf().get(i).getBeneficiaryBIC())));
            cti.setCdtrAcct(
                    (new CashAccount16()).setId(
                            (new AccountIdentification4Choice()).setIBAN(req.getCdtTrfTxInf().get(i).getBeneficiaryAcct())));
            cti.setRmtInf(new RemittanceInformation5().addUstrd(req.getCdtTrfTxInf().get(i).getPurpose()));
            mx.getFIToFICstmrCdtTrf().addCdtTrfTxInf(cti);

        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";

        return signEftRequest(content, bankCode);
    }

    public String signEftRequest(String rawXML, String bankCode) {
        String rawXmlsigned = null;
        try {

            String signature = sign.CreateSignature(rawXML, systemVariable.PRIVATE_EFT_TACH_KEYPASS,
                    systemVariable.PRIVATE_EFT_TACH_KEY_ALIAS, systemVariable.PRIVATE_EFT_TACH_KEY_FILE_PATH);

            rawXmlsigned = rawXML + "|" + signature;
//            queProducer.sendToQueueEftFrmCBSToTACH(bankCode + DateUtil.now("yyyyMMddHms") + ".i^" + rawXmlsigned);
//            FileUtils.writeStringToFile(new File("C:\\Users\\HP\\Desktop\\BOT\\NEWCERT\\" + bankCode + DateUtil.now("yyyyMMddHms") + ".i"), rawXmlsigned, StandardCharsets.UTF_8);
            LOGGER.info("\nEFT SIGNED REQUEST FOR :{}\n{}", bankCode, rawXmlsigned);
        } catch (Exception ex) {
            LOGGER.error("Generating digital signature...{}", ex);
        }
        return rawXmlsigned;
    }

    public void processPacs00800201Incoming(String eftMessageContent, String fileName, boolean isNotAllowedToProcess) {
        /*
         * its MT103 WITH SINGLE OR MULTIPLE ENTRIES [PACS.008.001.02]
         */
        EftPacs00800102Req pacs008 = new EftPacs00800102Req();
        MxPacs00800102 mx = MxPacs00800102.parse(eftMessageContent);
        pacs008.setMsgId(mx.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId());
        pacs008.setCreDtTm(mx.getFIToFICstmrCdtTrf().getGrpHdr().getCreDtTm());
        pacs008.setNbOfTxs(mx.getFIToFICstmrCdtTrf().getGrpHdr().getNbOfTxs());
        pacs008.setTtlIntrBkSttlmAmt(mx.getFIToFICstmrCdtTrf().getGrpHdr().getTtlIntrBkSttlmAmt().getValue());
        pacs008.setTtlIntrBkSttlmCcy(mx.getFIToFICstmrCdtTrf().getGrpHdr().getTtlIntrBkSttlmAmt().getCcy());
        pacs008.setIntrBkSttlmDt(mx.getFIToFICstmrCdtTrf().getGrpHdr().getIntrBkSttlmDt());
        pacs008.setSttlmMtd(mx.getFIToFICstmrCdtTrf().getGrpHdr().getSttlmInf().getSttlmMtd().name());
        pacs008.setInstdAgt(mx.getFIToFICstmrCdtTrf().getGrpHdr().getInstdAgt().getFinInstnId().getBIC());
        pacs008.setOriginalMsgNmId("pacs.008.001.02");
        int numberOfTxns = Integer.parseInt(pacs008.getNbOfTxs());
        int count = 1;
//insert batch transaction transactions
        insertEFTBacthTxnIncoming(pacs008, fileName);
        for (CreditTransferTransactionInformation11 ctti : mx.getFIToFICstmrCdtTrf().getCdtTrfTxInf()) {
            EftIncommingBulkPaymentsReq cti = new EftIncommingBulkPaymentsReq();
            cti.setInstrId(ctti.getPmtId().getInstrId());
            cti.setEndToEndId(ctti.getPmtId().getEndToEndId());
            cti.setTxId(ctti.getPmtId().getTxId());
            cti.setSvcLvlCd(ctti.getPmtTpInf().getSvcLvl().getCd());
            cti.setAmount(ctti.getIntrBkSttlmAmt().getValue());
            cti.setCurrency(ctti.getIntrBkSttlmAmt().getCcy());
            cti.setChrgBr(ctti.getChrgBr().name());
            cti.setSenderName(ctti.getDbtr().getNm());
//            if (ctti.getDbtr().getId().getOrgId().getBICOrBEI()== null) {
            cti.setSenderBICorBEI(ctti.getDbtrAgt().getFinInstnId().getBIC());
//            } else {
//                cti.setSenderBICorBEI(ctti.getDbtr().getId().getOrgId().getBICOrBEI());

//            }
            cti.setSenderAccount(ctti.getDbtrAcct().getId().getIBAN());
            cti.setSenderBIC(ctti.getDbtrAgt().getFinInstnId().getBIC());
            cti.setBeneficiaryBICorBEI(ctti.getCdtrAgt().getFinInstnId().getBIC());
//                    cti.setBeneficiaryAcct(ctti.getCdtrAcct().getId().getIBAN());
            //  if (ctti.getDbtrAgt().getFinInstnId().getBIC().contains("CORUTZTZ") || ctti.getDbtr().getId().getOrgId().getBICOrBEI().contains("CORUTZTZ")) {
            cti.setBeneficiaryAcct(ctti.getCdtrAcct().getId().getIBAN());
            //}
            cti.setBeneficiaryBIC(ctti.getCdtrAgt().getFinInstnId().getBIC());
            cti.setBeneficiaryName(ctti.getCdtr().getNm());
            if (ctti.getRmtInf() != null) {
                if (ctti.getRmtInf().getUstrd() != null) {
                    cti.setPurpose(ctti.getRmtInf().getUstrd().get(0));
                } else {
                    cti.setPurpose("transfer");
                }
            } else {
                cti.setPurpose("transfer");
            }
            pacs008.setCdtTrfTxInf(cti);
            //insert the transaction AND ADD TO QUEUE THE TRANSACIONS
            int res = insertIncomingEFTBulkTransactions(pacs008, fileName, isNotAllowedToProcess);

            if (res == 1 && (isNotAllowedToProcess == false) && ctti.getCdtrAgt().getFinInstnId().getBIC().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
                //send a transaction to QUEUE
                //check if the transaction if PENSION TRANSACTION
                //if (!ctti.getPmtId().getEndToEndId().startsWith("P001P")) {//process only if its not pension ELSE DO NOTHING
                queProducer.sendToQueueEftIncomingToCBS(pacs008);
                //}
                //validate PENSION PAYROLL DATA
            } else if (res == 2) {
                //log the transaction on the exception table
                LOGGER.info("The received is gepg transactions: reference:{},destinationAccount:{}, amount:{}", cti.getEndToEndId(), cti.getBeneficiaryAcct(), cti.getAmount());
            } else if (res == -1) {
                //TRANSACTION WAS NOT LOGGED ATTEMPT TO RE-LOG AGAIN
                insertIncomingEFTBulkTransactions(pacs008, fileName, isNotAllowedToProcess);
            }
            count++;
        }
        if (count == numberOfTxns) {
            //update the batch of transactions as completed
        }
//            pacs008.setCdtTrfTxInf(cdtTrfTxInf);
        LOGGER.info("PACS00800102 MESSAGE: {}", pacs008.toString());
    }

    public void processPacs00200103Incoming(String message, String fileName) {
        LOGGER.info("FILE RECEIVED FROM TACH: {}", message.trim());
        MxPacs00200103 mx = MxPacs00200103.parse(message);
        String status = "----";
        String batchRef = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getOrgnlMsgId();
        String OriginalMessageType = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getOrgnlMsgNmId();
        String ackString = batchRef + "^" + status + "^" + OriginalMessageType + "^" + FilenameUtils.getExtension(fileName) + "^" + message;
        queProducer.sendToQueueOutwardAcknowledgementByTACH(ackString);
    }

    public void processPacs00400102(String message, String fileName) {
        String allowedReverseCodes = "AC01,AC04,AC06,AG01,AG02,DC01,DC02,DC03,DC04,DC05,BE01,BE04,BE05,BE06,BE07,RJCT";
        //overide and getting values from the database.
        allowedReverseCodes = systemVariable.BOT_EFT_REVERSAL_CODES;
        List<String> convertedReverseList = Arrays.asList(StringUtils.splitPreserveAllTokens(allowedReverseCodes, ","));


        if ((FilenameUtils.getExtension(fileName)).equalsIgnoreCase("S")) {
            MxPacs00400102 mx = MxPacs00400102.parse(message);
            processInsertIncomingReversalEFTBulkTransactions(mx,fileName);
            for (PaymentTransactionInformation27 ptx : mx.getPmtRtr().getTxInf()) {
                String responseCode = ptx.getRtrRsnInf().get(0).getRsn().getCd();
                String txnReference = ptx.getOrgnlEndToEndId();
                String reversalReason = eftErrorCodes(responseCode);//GET REVERSAL MESSAGE
                String reversalString = txnReference + "^" + reversalReason + "^" + responseCode;
                if (convertedReverseList.contains(responseCode)) {
                    LOGGER.info("it can be reversed allowed Reverse code: {}, coming reverse code: {}", allowedReverseCodes, responseCode);
                    queProducer.sendToQueueOutwardReversal(reversalString);
                } else {
                    LOGGER.info("Could not be reversed allowed Reverse code: {}, coming reverse code: {}", allowedReverseCodes, responseCode);
                }
            }

        }
    }

    public String generatePacs00100103Message(EftPacs00800102OutgoingReq req) {
        String content = null;
        MxPain00100103 mx = new MxPain00100103();
        mx.setCstmrCdtTrfInitn(new CustomerCreditTransferInitiationV03().setGrpHdr(new GroupHeader32()));
        mx.getCstmrCdtTrfInitn().getGrpHdr().setMsgId(req.getMsgId());
        mx.getCstmrCdtTrfInitn().getGrpHdr().setCreDtTm(req.getCreDtTm());
        mx.getCstmrCdtTrfInitn().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getCstmrCdtTrfInitn().getGrpHdr().setInitgPty(new PartyIdentification32().setNm("TPB BANK PLC"));
        for (int i = 0; i < req.getCdtTrfTxInf().size(); i++) {
            PaymentInstructionInformation3 pi = new PaymentInstructionInformation3();
            pi.setChrgBr(ChargeBearerType1Code.SLEV);
            pi.setChrgsAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount())));
            pi.setPmtInfId(req.getCdtTrfTxInf().get(i).getEndToEndId());
            pi.setPmtMtd(PaymentMethod3Code.TRF);
            pi.setDbtr(new PartyIdentification32().setNm(req.getCdtTrfTxInf().get(i).getSenderName()));
            pi.setDbtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ"))));
            pi.setDbtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getSenderAccount())));
            pi.setDbtrAgt(new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC("CITITZTZ")));
            pi.addCdtTrfTxInf(new CreditTransferTransactionInformation10().setAmt(new AmountType3Choice().setInstdAmt(new ActiveOrHistoricCurrencyAndAmount().setCcy(req.getCdtTrfTxInf().get(i).getCurrency()).setValue(req.getCdtTrfTxInf().get(i).getAmount())))
                    .setCdtr(new PartyIdentification32().setNm(req.getCdtTrfTxInf().get(i).getBeneficiaryName()))
                    .setCdtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getCdtTrfTxInf().get(i).getBeneficiaryAcct())))
                    .setChrgBr(ChargeBearerType1Code.SLEV)
                    .setPurp(new Purpose2Choice().setCd(req.getCdtTrfTxInf().get(i).getPurpose()))
                    .setPmtId(new PaymentIdentification1().setEndToEndId(req.getCdtTrfTxInf().get(i).getEndToEndId()))
                    .setInstrForDbtrAgt(req.getCdtTrfTxInf().get(i).getPurpose())
                    .setUltmtCdtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ"))))
                    .setUltmtDbtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI("CITITZTZ"))))
                    .setRmtInf(new RemittanceInformation5().addStrd(new StructuredRemittanceInformation7().addAddtlRmtInf(req.getCdtTrfTxInf().get(i).getPurpose()))));
            mx.getCstmrCdtTrfInitn().addPmtInf(pi);
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        LOGGER.info("pain.001.001.03{}", content);
        return content;
    }

    public String generateMxPacs00400102SignedReq(EftPacs00400102Req req, String bankBIC) {
        String content = null;
        MxPacs00400102 mx = new MxPacs00400102();
        mx.setPmtRtr(new PaymentReturnV02().setGrpHdr(new GroupHeader38()));
        mx.getPmtRtr().getGrpHdr().setMsgId(req.getMessageId());
        mx.getPmtRtr().getGrpHdr().setCreDtTm(req.getCreateDateTime());
        mx.getPmtRtr().getGrpHdr().setNbOfTxs(req.getNbOfTxs());
        mx.getPmtRtr().getGrpHdr().setTtlRtrdIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy("TZS").setValue(req.getTotalReturnedIntrBnkSettlmntAmt()));
        mx.getPmtRtr().getGrpHdr().setIntrBkSttlmDt(req.getInterBankSettlmntDate());
        mx.getPmtRtr().getGrpHdr().setSttlmInf(new SettlementInformation13().setSttlmMtd(SettlementMethod1Code.CLRG)
                .setClrSys(new ClearingSystemIdentification3Choice().setPrtry(req.getClearingSystem())));
        mx.getPmtRtr().getGrpHdr().setInstgAgt(new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC(systemVariable.SENDER_BIC)));
        for (int i = 0; i < req.getTxInfo().size(); i++) {
            PaymentTransactionInformation27 txinfo = new PaymentTransactionInformation27();
            txinfo.setRtrId(req.getTxInfo().get(i).getReturnTxnId());
            txinfo.setOrgnlGrpInf(new OriginalGroupInformation3().setOrgnlMsgId(req.getTxInfo().get(i).getOriginalMsgId())
                    .setOrgnlMsgNmId(req.getTxInfo().get(i).getOriginalMsgNmId()));
            txinfo.setOrgnlInstrId(req.getTxInfo().get(i).getOriginalInstructionId());
            txinfo.setOrgnlEndToEndId(req.getTxInfo().get(i).getOriginalEndToEndId());
            txinfo.setOrgnlTxId(req.getTxInfo().get(i).getOriginalTxid());
            txinfo.setOrgnlIntrBkSttlmAmt(new ActiveOrHistoricCurrencyAndAmount().setCcy(req.getTxInfo().get(i).getOriginalInterBankSettmntCcy()).setValue(req.getTxInfo().get(i).getOriginalInterBankSettmntAmt()));
            txinfo.setRtrdIntrBkSttlmAmt(new ActiveCurrencyAndAmount().setCcy(req.getTxInfo().get(i).getReturnedInterBankSettmntCcy()).setValue(req.getTxInfo().get(i).getReturnedInterBankSettmntAmt()));
            txinfo.setChrgBr(ChargeBearerType1Code.SLEV);
            txinfo.getRtrRsnInf().add(0, new ReturnReasonInformation9().setOrgtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(systemVariable.PRIVATE_EFT_TACH_KEY_ALIAS)))).setRsn(new ReturnReason5Choice().setCd(req.getTxInfo().get(i).getReturnReasonInfomation())));//get(0).setOrgtr(new PartyIdentification32().setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(systemVariables.SENDER_BIC))));
            txinfo.setOrgnlTxRef(new OriginalTransactionReference13().setIntrBkSttlmDt(DateUtil.convertDateToXmlGregorian(req.getTxInfo().get(i).getTxnDate(), "yyyy-MM-dd"))
                    .setPmtTpInf(new PaymentTypeInformation22().setSvcLvl(new ServiceLevel8Choice().setCd("SEPA")))
                    .setDbtr(new PartyIdentification32().setNm(req.getTxInfo().get(i).getSenderName())
                            .setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getTxInfo().get(i).getSenderBIC()))))
                    .setDbtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getTxInfo().get(i).getSenderAccount())))
                    .setDbtrAgt(new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC(req.getTxInfo().get(i).getSenderBIC())))
                    .setCdtrAgt((new BranchAndFinancialInstitutionIdentification4().setFinInstnId(new FinancialInstitutionIdentification7().setBIC(req.getTxInfo().get(i).getBeneficiaryBIC()))
                            .setFinInstnId(new FinancialInstitutionIdentification7().setBIC(req.getTxInfo().get(i).getBeneficiaryBIC())))
                    )
                    .setCdtr(new PartyIdentification32().setNm(req.getTxInfo().get(i).getBeneficiaryName())
                            .setId(new Party6Choice().setOrgId(new OrganisationIdentification4().setBICOrBEI(req.getTxInfo().get(i).getBeneficiaryBIC()))))
                    .setCdtrAcct(new CashAccount16().setId(new AccountIdentification4Choice().setIBAN(req.getTxInfo().get(i).getBeneficiaryAcct()))));
            mx.getPmtRtr().addTxInf(txinfo);
        }
        content = mx.message().replace("Doc:", "").replace(":Doc", "").replace(" >", ">").replace("encoding=\"UTF-8\"", "encoding=\"UTF-8\"?").replace(" ?>", ">").trim() + "\r";
        System.out.println("--------------------raw message from------------------------");
        System.out.println(content);
        System.out.println("--------------------end row message------------------------");

//        LOGGER.info("RAW XML MESSAGE: \n" + content);
//        return content;
        return signEftRequest(content, bankBIC);

    }

    public void parseIso20022Message(String Message) throws Exception {
        String lastLineOfMessage = Message.split("\\^")[1].substring(Message.split("\\^")[1].lastIndexOf("\n"));//get signature content
        if (lastLineOfMessage.contains("Document")) {
            lastLineOfMessage = lastLineOfMessage.split("\\</Document>")[1];
        }
        LOGGER.info("parseIso20022Message {}", Message);
        String eftMessageSinged = Message.split("\\^")[1];
        String fileName = Message.split("\\^")[0];
        String eftMessageContent = eftMessageSinged.split("\\|")[0];
        String signature = eftMessageSinged.split("\\|")[1];
        boolean isEftMessageValid = sign.verifySignature(signature, eftMessageContent, systemVariable.PUBLIC_EFT_TACH_KEYPASS, systemVariable.PUBLIC_EFT_TACH_KEY_ALIAS, systemVariable.PUBLIC_EFT_TACH_KEY_FILE_PATH);
        if(systemVariable.ACTIVE_PROFILE.equals("dev") || systemVariable.ACTIVE_PROFILE.equals("uat")) {
            isEftMessageValid = true;
        }
        LOGGER.info("IS MESSAGE VALID? {}", isEftMessageValid);
        if (isEftMessageValid) {
            MxParser swiftMessage = new MxParser(eftMessageContent);
            LOGGER.info("Message Type: {}", swiftMessage.analyzeMessage().getDocumentNamespace());
            if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")) {
                LOGGER.info("processPacs00800201Incoming");
                //Incoming transaction from other bank
                processPacs00800201Incoming(eftMessageContent, fileName, false);
            } else if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.002.001.03")) {
                LOGGER.info("processPacs00200103Incoming");
                //transaction acknowledgement from bot after sending transaction to BOT
                processPacs00200103Incoming(eftMessageContent, fileName);
            } else if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.004.001.02")) {
                LOGGER.info("processPacs00400102");
                //returned transaction for reversal if response code met
                processPacs00400102(eftMessageContent, fileName);
            }
        } else {
            if (systemVariable.ACTIVE_PROFILE.equals("uat")) {
                MxParser swiftMessage = new MxParser(eftMessageContent);
                //test pacs on uat environment
                if (swiftMessage.analyzeMessage().getDocumentNamespace().equals("urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")) {
                    LOGGER.info("processPacs00800201Incoming");
                    processPacs00800201Incoming(eftMessageContent, fileName, false);
                }
            } else {
                processPacs00800201Incoming(eftMessageContent, fileName, true);
                System.out.println("RECEIVED FILE: " + fileName + " signature cannot be verified!!!!!!");
            }
        }
    }

    public List<Map<String, Object>> getEftBanks() {
        List<Map<String, Object>> data = null;
        data = jdbcTemplate.queryForList("SELECT * FROM banks WHERE identifier='LOCAL' AND fsp_category='BANK'");
        return data;
    }

    public String queryFailedToReachTachAjax(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        int totalRecordwithFilter = 0;

        int totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfers a WHERE a.status='CF' and a.txn_type='005' and direction='OUTGOING'", Integer.class);
        String searchQuery = "";
        if (!searchValue.equals("")) {
            searchValue = "%" + searchValue + "%";
            searchQuery = "WHERE status='CF' AND concat(sourceAcct,' ',destinationAcct,' ',senderBIC,' ',beneficiaryBIC) LIKE ?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(a.*) FROM transfers a " + searchQuery, new Object[]{searchValue}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }

        List<Map<String, Object>> findAll;
        String mainSql = "select * FROM transfers a WHERE  a.status='CF' and a.txn_type='005' and direction='OUTGOING' ";
        if (!searchQuery.equals("")) {
            mainSql = "select *  from transfers " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " OFFSET " + start + " ROWS FETCH FIRST " + rowPerPage + " ROWS ONLY ";
            findAll = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
        } else {
            findAll = this.jdbcTemplate.queryForList(mainSql);
        }

        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.error("FAILED TACH  REQ BODY: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public String pushTransactionToTachAjax(Map<String, String> customQuery) {
        String resp = "{\"responseCode\":\"99\",\"message\":\"General Failure\"}";
        int updateResult = 0;
        try {
            String sql = "Update transfers set status='C' where reference=? and sourceAcct=? and destinationAcct=?";
            updateResult = jdbcTemplate.update(sql, customQuery.get("reference"), customQuery.get("sourceAcct"), customQuery.get("destinationAcct"));
        } catch (DataAccessException e) {
            LOGGER.info("Exception found... {}", e.getMessage(), e);
        }

        if (updateResult == 1) {
            resp = "{\"responseCode\":\"0\",\"message\":\"Transaction Pushed To Tach\"}";
        }
        return resp;
    }

    public String cancelTransactionToTachAjax(Map<String, String> customQuery) {
        String resp = "{\"responseCode\":\"99\",\"message\":\"General Failure\"}";
        int updateResult = 0;
        try {
            String sql = "Update transfers set status='CAC', comments='Settled', response_code='00', cbs_status='C' where reference=? and sourceAcct=? and destinationAcct=?";
            updateResult = jdbcTemplate.update(sql, customQuery.get("reference"), customQuery.get("sourceAcct"), customQuery.get("destinationAcct"));
        } catch (DataAccessException e) {
            LOGGER.info("Exception found... {}", e.getMessage(), e);
        }

        if (updateResult == 1) {
            resp = "{\"responseCode\":\"0\",\"message\":\"Transaction Removed From WF\"}";
        }
        return resp;
    }

    public String cancelTransactionHQWFAjax(Map<String, String> customQuery) {
        String resp = "{\"responseCode\":\"99\",\"message\":\"General Failure\"}";
        int updateResult = 0;
        try {
            String sql = "Update transfer_eft_batches set status='C' where batch_reference=?";
            updateResult = jdbcTemplate.update(sql, customQuery.get("batch_reference"));
        } catch (DataAccessException e) {
            LOGGER.info("Exception found in hq eft batch... {}", e.getMessage(), e);
        }

        if (updateResult == 1) {
            resp = "{\"responseCode\":\"0\",\"message\":\"Transaction Removed From WF\"}";
        }
        return resp;
    }

    public List<Map<String, Object>> getBranches() {
        List<Map<String, Object>> data = null;
        data = jdbcTemplate.queryForList("SELECT * FROM branches");
        return data;
    }

    public GeneralJsonResponse getEftReconReportAjax(Map<String, String> customQuery) {
        GeneralJsonResponse response = new GeneralJsonResponse();
        List<Map<String, Object>> outputData = null;
        String reportType = customQuery.get("reportType");
        String fromDate = customQuery.get("fromDate");
        String toDate = customQuery.get("toDate");
        String bankCode = customQuery.get("bankName");
        String branchCode = customQuery.get("branchCode");
        String sql = "-1";
        String direction = "INCOMING";
        String queryUsing = "senderBIC";
        if (reportType.equalsIgnoreCase("OUTWARD_SUMMARY_PER_BANK")) {
            direction = "OUTGOING";
            queryUsing = "beneficiaryBIC";
        }

        switch (reportType) {
            case "INWARD_SUMMARY_PER_BANK":
            case "OUTWARD_SUMMARY_PER_BANK":
                if (bankCode.equalsIgnoreCase("ALL")) {
                    sql = "SELECT * FROM transfers WHERE date(create_dt)>=? and date(create_dt)<=? AND direction=? AND txn_type='005'";
                    outputData = jdbcTemplate.queryForList(sql, fromDate, toDate, direction);
                } else {
                    sql = "SELECT * FROM transfers WHERE date(create_dt)>=? and date(create_dt)<=? AND direction=? AND txn_type='005' and " + queryUsing + "=? ";
                    outputData = jdbcTemplate.queryForList(sql, fromDate, toDate, direction, bankCode);
                }
                break;
            case "OUTWARD_SUMMARY_PER_BRANCH":
                if (branchCode.equalsIgnoreCase("ALL")) {
                    sql = "SELECT t.*,(SELECT name FROM branches b WHERE b.code=t.branch_no) as branchName FROM transfers t WHERE initiated_by <>'SYSTEM' AND date(create_dt)>=? and date(create_dt)<=? AND direction='OUTGOING' AND txn_type='005'";
                    outputData = jdbcTemplate.queryForList(sql, fromDate, toDate);
                } else {
                    sql = "SELECT  t.sourceAcct,t.destinationAcct, t.amount, t.currency, t.beneficiaryName, t.beneficiaryBIC, t.senderBIC, t.sender_name, t.reference, t.batch_reference, t.status, t.comments, t.message ,t.direction,t.initiated_by, t.create_dt,t.branch_approved_by, t.branch_approved_dt, t.hq_approved_by, t.hq_approved_dt ,(SELECT name FROM branches b WHERE b.code=t.branch_no) as branchName FROM transfers t WHERE initiated_by <>'SYSTEM' AND date(create_dt)>=? and date(create_dt)<=? AND direction='OUTGOING' AND txn_type='005' AND branch_no=?";
                    outputData = jdbcTemplate.queryForList(sql, fromDate, toDate, branchCode);
                }
                break;
            case "HAZINA_PENSION_TRANS_LISTING":
                sql = "SELECT * FROM transfers t WHERE code='HPENSION' AND date(create_dt)>=? and date(create_dt)<=? ";
                outputData = jdbcTemplate.queryForList(sql, fromDate, toDate);
                break;
            case "RETURNED_TXNS":
                sql = "SELECT * FROM transfers  WHERE  cbs_status='RS' AND txn_type='005' AND direction='INCOMING' AND date(create_dt)>=? and date(create_dt)<=? ";
                outputData = jdbcTemplate.queryForList(sql, fromDate, toDate);
                break;
            case "REJECTED_TXNS":
                sql = "SELECT * FROM transfers  WHERE  status<>'CAC' AND txn_type='005' AND direction='OUTGOING' AND date(create_dt)>=? and date(create_dt)<=? ";
                outputData = jdbcTemplate.queryForList(sql, fromDate, toDate);
                break;
            case "SETTLED_COMPLETED_TXNS":
                sql = "SELECT * FROM transfers  WHERE  status='CAC' AND txn_type='005' AND direction='OUTGOING' AND date(create_dt)>=? and date(create_dt)<=? ";
                outputData = jdbcTemplate.queryForList(sql, fromDate, toDate);
                break;
            case "INCOMING_FAILED_TO_CREDIT_CUSTOMERS":
                sql = "SELECT * FROM transfers  WHERE  cbs_status<>'C' AND txn_type='005' AND direction='INCOMING' AND date(create_dt)>=? and date(create_dt)<=? ";
                outputData = jdbcTemplate.queryForList(sql, fromDate, toDate);
                break;
        }
        response.setStatus("SUCCESS");
        response.setResult(outputData);
        return response;
    }

    public void updateBatchEntriesAssignedToQueue(List<BatchTxnEntries> req) {
        LOGGER.info("***********************BATCH UPDATE********************");

        String sql = "UPDATE  transfers set batch_reference2=?, status='S',cbs_status='QI',message='Transaction assigned to the queue for processing-batch update' WHERE txid=?";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NotNull PreparedStatement ps, int i) throws SQLException {
                LOGGER.info(sql.replace("?", "'{}'"), req.get(i).getBatch(), req.get(i).getTxnRef());
                ps.setString(1, req.get(i).getBatch());
                ps.setString(2, req.get(i).getTxnRef());
                ps.executeBatch();
            }
//    int[] affectedRecords = ps.executeBatch();

            @Override
            public int getBatchSize() {
                return req.size();
            }
        });
    }

    public double namesMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return 0.00;
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        String normalized1 = normalize(name1);
        String normalized2 = normalize(name2);
        return similarity.apply(normalized1, normalized2);
    }

    private String normalize(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z\\s]", "");
    }
}
