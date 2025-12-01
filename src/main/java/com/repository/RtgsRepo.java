/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.GeneralJsonResponse;
import com.DTO.IBANK.BanksListResp;
import com.DTO.RemittanceToQueue;
import com.DTO.ResultObjectResp;
import com.DTO.Teller.FinanceMultipleGLMapping;
import com.DTO.Teller.RTGSTransferForm;
import com.DTO.Teller.RTGSTransferFormFinance;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.online.core.request.SupportDoc;
import com.queue.QueueProducer;
import com.service.CorebankingService;
import com.service.HttpClientService;
import com.service.SwiftService;
import com.service.XMLParserService;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import philae.ach.ProcessOutwardRtgsTransfer;
import philae.ach.TaResponse;
import philae.ach.TaTransfer;
import philae.api.PostGLToGLTransfer;
import philae.api.TxRequest;
import philae.api.UsRole;
import philae.api.XaResponse;

import jakarta.validation.constraints.Null;

/**
 * @author melleji.mollel
 */
@Repository
public class RtgsRepo {

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    QueueProducer queProducer;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;
    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;
    @Autowired
    SignRequest sign;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TellerRepo.class);
    @Autowired
    WebserviceRepo webserviceRepo;
    @Autowired
    private Environment env;

    @Autowired
    SwiftRepository swiftRepository;

    /*
    SAVE INITIATED RTGS TRANSFERS
     */
    public Integer saveinitiatedRTGSRemittance(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, MultipartFile[] files) {
        Integer res = -1;
        Integer res2 = -1;
        String initialStatus = "I";
        try {
            if (!rtgsTransferForm.getRequestingRate().isEmpty()) {
                initialStatus = "SR";
            }
            BigDecimal amt = new BigDecimal(rtgsTransferForm.getAmount());
            String complianceStatus = null;
            if (amt.compareTo(new BigDecimal(systemVariables.AMOUNT_THAT_REQUIRES_COMPLIANCE)) > 0) {
                complianceStatus = "P";
            }
            res = jdbcTemplate.update("INSERT INTO transfers(message_type,sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency, compliance,direction) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'outgoing')",
                    rtgsTransferForm.getMessageType(), rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, initialStatus, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, "P", rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC().split("==")[0], rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), complianceStatus);
            if (!rtgsTransferForm.getRequestingRate().isEmpty()) {
                res2 = saveSpecialRate(rtgsTransferForm, reference, initiatedBy);
            }
            if (res != -1) {
                taskExecutor.execute(() -> {
                    try {
                        String ttype = "TISS";
                        if (txnType.equalsIgnoreCase("001")) {
                            ttype = "TISS";
                        }
                        if (txnType.equalsIgnoreCase("004")) {
                            ttype = "TT";
                        }
                        if (txnType.equalsIgnoreCase("005")) {
                            ttype = "EFT";
                        }
                        String message = systemVariables.SMS_NOTIFICATION_FOR_TXNS_ON_WORKFLOW.replace("{TXN_TYPE}", ttype)
                                .replace("{AMOUNT} ", " " + rtgsTransferForm.getAmount() + " " + rtgsTransferForm.getCurrency())
                                .replace("{CUSTOMER_NAME} ", " " + rtgsTransferForm.getBeneficiaryName().toUpperCase())
                                .replace("{BENEFICIARY_BIC}", " " + rtgsTransferForm.getBeneficiaryBIC().split("==")[0])
                                .replace("{REFERENCE}", " " + reference);
                        corebanking.sendSmsToApprovers(branchCode, message, reference);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            LOGGER.info("INITIATION RESULT: {}", res);

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
    INSERT SPECIAL EXCHANGE RATE
     */
    public Integer saveSpecialRate(RTGSTransferForm rtgsTransferForm, String reference, String initiatedBy) {
        Integer res = -1;
        Integer res2 = -1;
        try {

            res = jdbcTemplate.update("INSERT IGNORE INTO transfer_special_rates(txnReference, currency_conversion, system_rate, requested_rate, initiated_by,source,fxtype) VALUES (?,?,?,?,?,?,?)",
                    reference, rtgsTransferForm.getCurrencyConversion(), rtgsTransferForm.getRubikonRate(), rtgsTransferForm.getRequestingRate(), initiatedBy, "BR", rtgsTransferForm.getFxType());
            LOGGER.info("INITIATED SPECIAL RATE REQUEST: TXN_REFERENCE{}, RUBIKON BUYING RATE:{}, CURRENCY CONVERSION : {} SYSTEM  RATE: {} SOURCE: {} FX TYPE:{}", reference, rtgsTransferForm.getCurrencyConversion(), rtgsTransferForm.getRubikonRate(), rtgsTransferForm.getRequestingRate(), "BR", rtgsTransferForm.getFxType());
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveInitiatedFinanceRTGSRemittance(RTGSTransferFormFinance rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, MultipartFile[] files, BigDecimal totalAmount, BigDecimal tax, boolean hasVAT) {
        int res = -1;
        int res2 = -1;
        BigDecimal vatAmount;
        BigDecimal principal;

        if (hasVAT) {
            BigDecimal vatRate = new BigDecimal("0.18");
            BigDecimal pRate = new BigDecimal("1.18");
            principal = totalAmount.divide(pRate, 2, RoundingMode.CEILING);
            vatAmount = principal.multiply(vatRate);
        } else {
            vatAmount = new BigDecimal(0);
            principal = totalAmount;
        }

        try {
            res = jdbcTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, "I", initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, "P", rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC().split("==")[0], rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency());
            LOGGER.info("FINANCE INITIATION RESULT: res {}", res);

            //INSERT INTO FINANCE TAX TABLE WITH DETAILS
            if (res == 1) {
                taskExecutor.execute(() -> {
                    try {
                        String ttype = "TISS";
                        if (txnType.equalsIgnoreCase("001")) {
                            ttype = "TISS";
                        }
                        if (txnType.equalsIgnoreCase("004")) {
                            ttype = "TT";
                        }
                        if (txnType.equalsIgnoreCase("005")) {
                            ttype = "EFT";
                        }
                        String message = systemVariables.SMS_NOTIFICATION_FOR_TXNS_ON_WORKFLOW.replace("{TXN_TYPE}", ttype)
                                .replace("{AMOUNT} ", rtgsTransferForm.getAmount() + " " + rtgsTransferForm.getCurrency())
                                .replace("{CUSTOMER_NAME} ", rtgsTransferForm.getBeneficiaryName().toUpperCase())
                                .replace("{BENEFICIARY_BIC}", rtgsTransferForm.getBeneficiaryBIC())
                                .replace("{REFERENCE}", reference);
                        corebanking.sendSmsToApprovers(branchCode, message, reference);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("ERROR ON RUNNING sftp: " + e.getMessage());
                    }
                });
                LOGGER.info("be4 FINANCE INITIATION RESULT: res2");
                res2 = jdbcTemplate.update("INSERT INTO transfer_with_tax_finance(txnReference, amount, taxable_amount, tax_amount, vat_amount, principal, tax_ledger, tax_rate) VALUES (?,?,?,?,?,?,?,?)",
                        reference, totalAmount, rtgsTransferForm.getTaxableAmount(), tax, vatAmount, principal, rtgsTransferForm.getTaxRate().split("==")[1], rtgsTransferForm.getTaxRate().split("==")[0]);
                LOGGER.info("after FINANCE INITIATION RESULT: res {}", res2);

            } else {
                //txManager.rollback(statuss);
                res = -1;
            }

            LOGGER.info("TAX DETAILS: INSERT INTO transfer_with_tax_finance(txnReference, amount, taxable_amount, tax_amount, tax_ledger, tax_rate) VALUES ('{}','{}','{}','{}','{}','{}')", reference, totalAmount, rtgsTransferForm.getTaxableAmount(), tax, rtgsTransferForm.getTaxRate().split("==")[1], rtgsTransferForm.getTaxRate().split("==")[0]);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("EXCEPTION ON PROCESSING FINANCE INITIATION TRANSACTION: {}", e.getMessage());
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
     *AMEND RTGS TRANSACTION ON THE WORK FLOW.
     */
    public Integer ammendRTGSTransaction(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, MultipartFile[] files, String cbsStatus) {
        Integer res = -1;
        Integer res2 = -1;
        String status = "I";
        if (cbsStatus.equalsIgnoreCase("C")) {
            //CHECK IF ITS AN AMMENDMENT FROM IBD. UPDATE BACK TO IBD WORKFLOW.
            status = "P";
        }
        try {
            res = jdbcTemplate.update("UPDATE transfers SET sourceAcct=?, destinationAcct=?, amount=?, reference=?, status=?, initiated_by=?,txn_type=?,purpose=?,sender_address=?,sender_phone=?,sender_name=?,swift_message=?,branch_no=?,cbs_status=?,beneficiary_contact=?,beneficiaryBIC=?,beneficiaryName=?,currency=? WHERE reference=?",
                    rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, status, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, cbsStatus, rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC().split("==")[0], rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), reference);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            LOGGER.info("ERROR ON INSERTING TRANSACTION ON TRANSFERS TABLE DURING AMMENDMEND: {}", e.getMessage());

//            txManager.rollback(statuss);
            res = -1;
        }
        return res;
    }

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
     *Update supporting document per transaction
     */
    public Integer updateSupportingDocPerTxnReference(String reference, MultipartFile file) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("UPDATE  transfer_document SET supportingDoc=?,file_name=?,file_size=? WHERE txnReference=?",
                    file.getBytes(), file.getOriginalFilename(), file.getSize(), reference);
            LOGGER.info("UPDATING SUPPORTING DOC FOR TXN:{} FILE :{} FILE NAME: {} SIZE: {}", reference, result, file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            LOGGER.info(null, e);
            LOGGER.info("ERROR ON UPDATING SUPPORTING DOCUMENT: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    /*
    GET Transactions on workflow in branch level
     */
    public String getRTGSTxnOnWorkFlowAjax(String date, String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        String status = "I";
        String APPLICATION_PROFILE = env.getProperty("spring.profiles.active");
        if (APPLICATION_PROFILE.equalsIgnoreCase("prod")) {
            if (roleId.equals("45")) {
                status = "PC";
            }
        } else {
            if (roleId.equals("43")) {
                status = "PC";
            }
        }
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where create_dt>=? and status=? and cbs_status='P' AND  code<>'IB' AND txn_type<>'0101' AND branch_no=?";
            LOGGER.info(mainSql.replace("?","'{}'"),date, status, branchNo);
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{date, status, branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',sender_name,' ',destinationAcct,' ',beneficiaryBIC,' ',beneficiaryName,' ',amount,' ',purpose,' ',initiated_by,' ',status,' ',cbs_status) LIKE ? and create_dt>=? and status=? and cbs_status='P' AND  code<>'IB' AND txn_type<>'0101' AND branch_no=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfers " + searchQuery, new Object[]{searchValue, date, status, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchNo});

            } else {
                mainSql = "select * from transfers where create_dt>=? and  status=? and cbs_status='P' AND  code<>'IB' AND txn_type<>'0101' AND branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.debug(mainSql.replace("?", "'" + branchNo + "'"));
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{date, status, branchNo});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    /*
    GET SWIFT TRANSACTIONS WITH REFERENCE
     */
    public List<Map<String, Object>> getSwiftMessage(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT a.*,IFNULL((select approved_rate   from transfer_special_rates where txnReference=a.reference),0) specialRate FROM transfers a where reference=? order by create_dt  desc", reference);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON TRANSFER TABLE: {}", ex.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> swiftMessageRtgsWorkaroundVPN() {
        try {
            LOGGER.info("SELECT a.*,IFNULL((select approved_rate   from transfer_special_rates where txnReference=a.reference),0) specialRate FROM transfers a where txn_type='{}' and create_dt>='2024-03-27' and direction='OUTGOING' order by create_dt  desc", "001");
            String sql="SELECT a.*,IFNULL((select approved_rate   from transfer_special_rates where txnReference=a.reference),0) specialRate FROM transfers a  where reference in ('STP1302409260602',\n" +
                    "'STP2202501180131',\n" +
                    "'602803244633703') and a.txn_type='001' and create_dt>='2024-03-27'  and direction='OUTGOING'  order by create_dt  desc";
            return this.jdbcTemplate.queryForList(sql);//"SELECT a.*,IFNULL((select approved_rate   from transfer_special_rates where txnReference=a.reference),0) specialRate FROM transfers a where txn_type=? and create_dt>='2024-09-24'  and direction='OUTGOING'  order by create_dt  desc", "001");
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON TRANSFER TABLE: {}", ex.getMessage());
            return null;
        }
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
    GET SWIFT MESSAGE SUPPORTING DOCUMENT
     */
    public List<Map<String, Object>> getReturnCodes() {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM response_codes");
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING RETURN CODES: {}", e.getMessage());
        }
        return result;
    }

    /*
    GET FINANCE TRANSACTION WITH TAX ASSOCIATED
     */
    public List<Map<String, Object>> getFinanceRTGSWithTax(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM transfer_with_tax_finance where txnReference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING FINANCE TXN WITH TAX: {}", e.getMessage());
        }
        return result;
    }

    /*
     Process RTGS Transactions
     */
    public String processRTGSRemittanceToCoreBanking(String reference, UsRole role, philae.ach.UsRole achRole) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        String apiIdentifier = "ach:processOutwardRtgsTransfer";
        try {
            //get the MESSAGE DETAILS FROM THE QUEUE

            List<Map<String, Object>> txn = getSwiftMessage(reference);
            LOGGER.info("getSwiftMessage:{}",txn);
            if (txn != null && !txn.isEmpty()) {
                //implement compliance
                String status = (String) txn.get(0).get("status");
                String txnType = (String) txn.get(0).get("txn_type");
                BigDecimal amount = new BigDecimal(txn.get(0).get("amount")+"");
                String complianceStatus = (String) txn.get(0).get("compliance");
                if (complianceStatus != null && complianceStatus.equals("P")) {
                    if (status.equals("I") && txnType.equals("001") && amount.compareTo(new BigDecimal(systemVariables.AMOUNT_THAT_REQUIRES_COMPLIANCE)) > 0) {
                        jdbcTemplate.update("update transfers set status='PC', comments='Transaction pending compliance by Compliance officer' where reference=?",
                                reference);
                        return "{\"result\":\"0\",\"message\":\"Transaction is pending compliance!\"}";
                    } else if (status.equals("PC")) {
                        jdbcTemplate.update("update transfers set compliance='C', status='I', comments='Transaction pending approval', compliance_officer=?, compliance_dt=? where reference=?",
                                role.getUserName(), DateUtil.now(), reference);
                        return "{\"result\":\"0\",\"message\":\"Transaction is pending approval!\"}";
                    }
                }

                String ledger = (String) txn.get(0).get("sourceAcct");
                LOGGER.info("SOURCE ACCOUNT: {}", txn.get(0).get("sourceAcct"));
                int checkIfLedger = StringUtils.countMatches(ledger, "-");
                if (checkIfLedger >= 4) {
                    /*
                     *SOURCE ACCOUNT IS GL-ACCOUNT
                     */
                    result = financeApproveFromGL2GL(reference, (philae.api.UsRole) role, false);
                } else {
                    //generate the request to RUBIKON
                    TaTransfer transferReq = new TaTransfer();
                    //check if its return payment then pick original reference from txid
                    if (txn.get(0).get("txid") != null) {
                        if (!txn.get(0).get("txid").toString().equalsIgnoreCase(txn.get(0).get("reference").toString())) {
                            transferReq.setReference((String) txn.get(0).get("txid"));
                            transferReq.setTxnRef((String) txn.get(0).get("txid"));
                        }
                    } else {
                        transferReq.setReference((String) txn.get(0).get("reference"));
                        transferReq.setTxnRef((String) txn.get(0).get("reference"));
                    }
                    //checking reference issues on transactions submitted after cut-off
                    if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
                        transferReq.setReference(reference);
                        transferReq.setTxnRef(reference);
                    }
                    transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(txn.get(0).get("create_dt").toString(), "yyyy-MM-dd HH:mm:ss"));
                    transferReq.setEmployeeId(role.getUserId());
                    transferReq.setSupervisorId(role.getUserId());
                    transferReq.setTransferType("RTGS");
                    transferReq.setCurrency((String) txn.get(0).get("currency"));
                    transferReq.setAmount(amount);
                    transferReq.setDebitFxRate(new BigDecimal(txn.get(0).get("specialRate").toString()));
                    transferReq.setCreditFxRate(new BigDecimal(txn.get(0).get("specialRate").toString()));
                    transferReq.setReceiverBank((String) txn.get(0).get("beneficiaryBIC"));
                    transferReq.setReceiverAccount((String) txn.get(0).get("destinationAcct"));
                    transferReq.setReceiverName((String) txn.get(0).get("beneficiaryName"));
                    transferReq.setSenderBank(systemVariables.SENDER_BIC);
                    transferReq.setSenderAccount((String) txn.get(0).get("sourceAcct"));
                    transferReq.setSenderName((String) txn.get(0).get("sender_name"));
                    transferReq.setDescription(txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName"));
                    transferReq.setTxnId(Long.parseLong(txn.get(0).get("id").toString()));
                    if (txnType.equals("002") || txnType.equals("001") || txnType.equals("003")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains((String) txn.get(0).get("sourceAcct"))) {//waived accounts
                            transferReq.setScheme("T99");
                        } else {
                            transferReq.setScheme("T01");
                        }

                        transferReq.setContraAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
                            transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);// BOTNostroAccount);
                        }
                    } else if (txnType.equals("005")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains((String) txn.get(0).get("sourceAcct"))) {//waived accounts
                            transferReq.setScheme("T99");
                        } else {
                            transferReq.setScheme("T01");
                        }
                        apiIdentifier = "ach:processOutwardEftTransfer";
                        //process eft transactions
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);// BOTNostroAccount);

                    } else if (txnType.equals("004")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains((String) txn.get(0).get("sourceAcct"))) {//waived accounts
                            transferReq.setScheme("T99");
                        } else {
                            transferReq.setScheme("T02");
                        }
                        transferReq.setContraAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
                            switch ((String) txn.get(0).get("currency")) {
                                case "USD":
                                case "GBP":
                                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                                    break;
                                case "KES":
                                case "UGX":
                                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);// BOTNostroAccount);
                                    break;
                                case "EUR":
                                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);// BHF NOSTRO ACCOUNT;
                                    break;
                                case "ZAR":
                                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                                    break;
                                default:
                                    transferReq.setContraAccount("0-000-00-0000-0000000");
                                    break;
                            }
                        }
                    } else {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains((String) txn.get(0).get("sourceAcct"))) {//waived accounts
                            transferReq.setScheme("T99");
                        } else {
                            transferReq.setScheme("T01");
                        }
                        transferReq.setContraAccount("");
                    }
                    transferReq.setReversal(Boolean.FALSE);
                    transferReq.setUserRole(achRole);
                    ProcessOutwardRtgsTransfer postOutwardReq = new ProcessOutwardRtgsTransfer();
                    postOutwardReq.setTransfer(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, apiIdentifier), TaResponse.class);
                    if (cbsResponse == null) {
                        LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                        //do not update the transaction status
                        return result;
                    }
                    if (cbsResponse.getResult() == 0) {
                        if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
                            jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                            LOGGER.info("RTGS SUCCESS SUBMITED AFTER CUT-OFF AND APPROVED BY BACK-OFFICER: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                        } else {
                            jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                        }
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been submitted to IBD Workflow. \"}";
                        LOGGER.info("RTGS SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                    } else {
                        if (cbsResponse.getResult() == 26) {
                            if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
                                jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                                LOGGER.info("RTGS SUCCESS SUBMITED AFTER CUT-OFF AND APPROVED BY BACK-OFFICER: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                            } else {
                                jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                            }
//                            jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), role.getUserName(), DateUtil.now(), reference);
                        } else {
                            jdbcTemplate.update("update transfers set comments=?,branch_approved_by=?,branch_approved_dt=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), role.getUserName(), DateUtil.now(), reference);
                        }
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                        //failed on posting CBS
                        LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                        //update the transaction status
                    }

                }
            } else {
                result = "{\"result\":\"101\",\"message\":\"An Error occurred During processing Please Try Again!!!!!!!: \"}";

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }

    /*
     Process RTGS Transactions
     */
    public String processReverseRTGSRemittanceToCoreBanking(String reference, UsRole role, philae.ach.UsRole achRole) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";

        try {
            //get the MESSAGE DETAILS FROM THE QUEUE

            List<Map<String, Object>> txn = getSwiftMessage(reference);
            if (txn != null && txn.get(0).get("cbs_status").equals("C")) {
                String ledger = (String) txn.get(0).get("sourceAcct");
                int checkIfLedger = StringUtils.countMatches(ledger, "-");
                if (checkIfLedger >= 4) {
                    /*
                     *SOURCE ACCOUNT IS GL-ACCOUNT
                     */
                    result = financeApproveFromGL2GL(reference, (philae.api.UsRole) role, true);
                } else {
                    //generate the request to RUBIKON
                    TaTransfer transferReq = new TaTransfer();
                    transferReq.setReference((String) txn.get(0).get("reference"));
                    transferReq.setTxnRef((String) txn.get(0).get("reference"));
                    transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(txn.get(0).get("create_dt").toString(), "yyyy-MM-dd HH:mm:ss"));
                    transferReq.setEmployeeId(role.getUserId());
                    transferReq.setSupervisorId(role.getUserId());
                    transferReq.setTransferType("RTGS");
                    transferReq.setCurrency((String) txn.get(0).get("currency"));
                    transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                    transferReq.setExchangeRate(new BigDecimal(txn.get(0).get("specialRate").toString()));
                    transferReq.setReceiverBank((String) txn.get(0).get("beneficiaryBIC"));
                    transferReq.setReceiverAccount((String) txn.get(0).get("destinationAcct"));
                    transferReq.setReceiverName((String) txn.get(0).get("beneficiaryName"));
                    transferReq.setSenderBank(systemVariables.SENDER_BIC);
                    transferReq.setSenderAccount((String) txn.get(0).get("sourceAcct"));
                    transferReq.setSenderName((String) txn.get(0).get("sender_name"));
                    transferReq.setDescription((String) txn.get(0).get("purpose") + " B/O " + (String) txn.get(0).get("beneficiaryName"));
                    transferReq.setTxnId(Long.parseLong(txn.get(0).get("id").toString()));
                    if (((String) txn.get(0).get("txn_type")).equals("002") || ((String) txn.get(0).get("txn_type")).equals("001") || ((String) txn.get(0).get("txn_type")).equals("003")) {
                        transferReq.setScheme("T01");
                        transferReq.setContraAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    } else if (txn.get(0).get("txn_type").equals("004")) {
                        transferReq.setScheme("T02");
                        transferReq.setContraAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    } else {
                        transferReq.setScheme("T01");
                        transferReq.setContraAccount("");
                    }
                    transferReq.setReversal(Boolean.TRUE);
                    transferReq.setUserRole(achRole);
                    ProcessOutwardRtgsTransfer postOutwardReq = new ProcessOutwardRtgsTransfer();
                    postOutwardReq.setTransfer(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, "ach:processOutwardRtgsTransfer"), TaResponse.class);
                    if (cbsResponse == null) {
                        LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                        //do not update the transaction status
                        return result;
                    }
                    if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                        jdbcTemplate.update("update transfers set status='F',cbs_status='C',comments='Transaction reversed Successfylly',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been reversed Successfully please confirm in core Banking \"}";
                        LOGGER.info("RTGS SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                    } else if (cbsResponse.getResult() == 26) {
                        jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), role.getUserName(), DateUtil.now(), reference);
                    } else {
                        jdbcTemplate.update("update transfers set comments=?,branch_approved_by=?,branch_approved_dt=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), role.getUserName(), DateUtil.now(), reference);
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                        //failed on posting CBS
                        LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                        //update the transaction status
                    }

                }
            } else {
                jdbcTemplate.update("update transfers set status='F',comments='Transaction Cancelled by Chief Cashier',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                result = "{\"result\":\"0\",\"message\":\"Transaction Cancelled Successfully: \"}";

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }

    /*
     Process RTGS Transactions FINANCE TRANSACTION
     */
    public String financeApproveFromGL2GL(String reference, UsRole role, boolean condition) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";

        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            String sourceAccount = "-1";
            String destinationAccount = "-1";
            List<Map<String, Object>> txn = getSwiftMessage(reference);
            if (txn != null) {
                //generate the request to RUBIKON
                TxRequest transferReq = new TxRequest();
//                transferReq.setReference((String) txn.get(0).get("reference"));
                LOGGER.info(result);
                if (txn.get(0).get("txid") != null) {
                    if (!txn.get(0).get("txid").toString().equalsIgnoreCase(txn.get(0).get("reference").toString())) {
                        transferReq.setReference((String) txn.get(0).get("txid"));
                        destinationAccount = txn.get(0).get("destinationAcct").toString();
                        transferReq.setCreditAccount(txn.get(0).get("destinationAcct").toString());
                    } else {
                        transferReq.setReference((String) txn.get(0).get("reference"));
                    }
                } else {
                    transferReq.setReference((String) txn.get(0).get("reference"));
                }
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration(txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName"));
                transferReq.setCurrency((String) txn.get(0).get("currency"));
                transferReq.setDebitAccount(txn.get(0).get("sourceAcct").toString());
                sourceAccount = txn.get(0).get("sourceAcct").toString();
                if (txn.get(0).get("txn_type").equals("002") || txn.get(0).get("txn_type").equals("001") || ((String) txn.get(0).get("txn_type")).equals("003")) {
                    destinationAccount = systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString());
                    transferReq.setCreditAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                }
                if (txn.get(0).get("txn_type").equals("004")) {
                    destinationAccount = systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString());
                    transferReq.setCreditAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                }
                if (!destinationAccount.equalsIgnoreCase(sourceAccount)) {
//                    transferReq.setCreditAccount(destinationAccount);
                    transferReq.setUserRole(role);
                    PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
                    postOutwardReq.setRequest(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToGLTransfer"), XaResponse.class);
                    if (cbsResponse == null) {
                        LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                        //do not update the transaction status
                        return result;
                    }
                    switch (cbsResponse.getResult()) {
                        case 0:
                        case 26:
                            if (condition) {
                                jdbcTemplate.update("update transfers set status='F',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                                result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been submitted to SWIFT VERIFIER STAGE ON  Workflow. \"}";
                            } else {
                                result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been reversed successfully\"}";
                                jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                            }
                            LOGGER.info("FINANCE POSTED  SUCCESSFULLY FROM SOURCE GL TO SUSPENSE: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                            String messageRequest = (String) txn.get(0).get("swift_message");
                            break;
                        default:
                            jdbcTemplate.update("update transfers set comments=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), reference);
                            result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                            //failed on posting CBS
                            LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                            //update the transaction status
                            break;
                    }
                } else {
                    result = "{\"result\":\"90\",\"message\":\"Sender account is the same as Destination Account: SENDER ACCOUNT: " + sourceAccount + " DESTINATION ACCOUNT: " + destinationAccount + "\"}";

                }
            } else {
                result = "{\"result\":\"101\",\"message\":\"An Error occurred During processing Please Try Again!!!!!!!: \"}";

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }

    public String financeApproveRTGSFrmGLToSuspenseGL(String reference, UsRole role) {
        String result = "{\"result\":\"99\",\"message\":\"" + reference + "An Error occurred During processing-Timeout Please confirm on Rubikon:\"}";

        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            List<Map<String, Object>> txn = getSwiftMessage(reference);
            //get totalAmount with/without tax
            if (txn != null && !txn.isEmpty()) {
                if (txn.get(0).get("purpose").toString().contains("/ROC/")) {
                    //PROCESS GePG finance
                    TxRequest transferReq = new TxRequest();
                    transferReq.setReference((String) txn.get(0).get("reference"));
                    transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                    transferReq.setNarration(txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName"));
                    transferReq.setCurrency((String) txn.get(0).get("currency"));
                    if (txn.get(0).get("txn_type").equals("002") || txn.get(0).get("txn_type").equals("001") || txn.get(0).get("txn_type").equals("003")) {
                        transferReq.setDebitAccount(txn.get(0).get("sourceAcct").toString());
                        transferReq.setCreditAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    }
                    if (txn.get(0).get("txn_type").equals("004")) {
                        transferReq.setDebitAccount(txn.get(0).get("sourceAcct").toString());
                        transferReq.setCreditAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    }
                    transferReq.setUserRole(role);
                    PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
                    postOutwardReq.setRequest(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToGLTransfer"), XaResponse.class);
                    if (cbsResponse == null) {
                        LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                        //do not update the transaction status
                        return result;
                    }
                    switch (cbsResponse.getResult()) {
                        case 0:
                        case 26:
                            jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                            result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been submitted to SWIFT VERIFIER STAGE ON  Workflow. \"}";
                            LOGGER.info("FINANCE POSTED  SUCCESSFULLY FROM SOURCE GL TO SUSPENSE: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                            String messageRequest = (String) txn.get(0).get("swift_message");
                            /*
                            CREATE OBJECT FOR POSTING TAX TO SERVICE LEDGER
                             */
//                            RemittanceToQueue remittanceToQue = new RemittanceToQueue();
//                            remittanceToQue.setBnUser(null);
//                            remittanceToQue.setUsRole(role);
//                            remittanceToQue.setReferences(reference);
//                            queProducer.sendToQueueWithHodlingTaxPosting(remittanceToQue);
                            break;

                        default:
                            jdbcTemplate.update("update transfers set comments=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), reference);
                            result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                            //failed on posting CBS
                            LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                            //update the transaction status
                            break;
                    }
                } else {
                    List<Map<String, Object>> txn1 = getFinanceRTGSWithTax(reference);
                    //generate the request to RUBIKON
                    if (!txn1.isEmpty()) {
                        TxRequest transferReq = new TxRequest();
                        transferReq.setReference((String) txn.get(0).get("reference"));
                        String principalAmount = txn1.get(0).get("principal").toString();
                        String vatAmount = txn1.get(0).get("vat_amount").toString();
                        if (!principalAmount.equals("0.00")) {
                            transferReq.setAmount(new BigDecimal(principalAmount));
                            transferReq.setNarration("Principal for " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + " [Principal amt:" + principalAmount + ", VAT amt: " + vatAmount + "]");
                        } else {
                            transferReq.setAmount(new BigDecimal(txn1.get(0).get("amount").toString()));
                            transferReq.setNarration(txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName"));
                        }
                        transferReq.setCurrency((String) txn.get(0).get("currency"));
                        if (txn.get(0).get("txn_type").equals("002") || txn.get(0).get("txn_type").equals("001") || txn.get(0).get("txn_type").equals("003")) {
                            transferReq.setDebitAccount(txn.get(0).get("sourceAcct").toString());
                            transferReq.setCreditAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        }
                        if (txn.get(0).get("txn_type").equals("004")) {
                            transferReq.setDebitAccount(txn.get(0).get("sourceAcct").toString());
                            transferReq.setCreditAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        }
                        transferReq.setUserRole(role);
                        PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
                        postOutwardReq.setRequest(transferReq);
                        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                        //process the Request to CBS
                        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToGLTransfer"), XaResponse.class);
                        if (cbsResponse == null) {
                            LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                            //do not update the transaction status
                            return result;
                        }
                        switch (cbsResponse.getResult()) {
                            case 0:
                            case 26:
                                if (!vatAmount.equals("0.00")) {
                                    transferReq.setReference(transferReq.getReference() + "VAT");
                                    transferReq.setAmount(new BigDecimal(vatAmount));
                                    transferReq.setNarration("VAT for " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName"));
                                    transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_VAT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                                    postOutwardReq.setRequest(transferReq);
                                    outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                                    //process the Request to CBS
                                    XaResponse cbsResponseVAT = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToGLTransfer"), XaResponse.class);
                                    if (cbsResponseVAT == null) {
                                        LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : VAT trans reference {}", txn.get(0).get("reference"));
//                                    Reverse previous transaction
                                        transferReq.setReversal("true");
                                        postOutwardReq.setRequest(transferReq);
                                        outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                                        XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToGLTransfer"), XaResponse.class);
                                        return result;
                                    }
                                }
                                jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                                result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been submitted to SWIFT VERIFIER STAGE ON  Workflow. \"}";
                                LOGGER.info("FINANCE POSTED  SUCCESSFULLY FROM SOURCE GL TO SUSPENSE: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                                String messageRequest = (String) txn.get(0).get("swift_message");
                                /*
                            CREATE OBJECT FOR POSTING TAX TO SERVICE LEDGER
                                 */
                                RemittanceToQueue remittanceToQue = new RemittanceToQueue();
                                remittanceToQue.setBnUser(null);
                                remittanceToQue.setUsRole(role);
                                remittanceToQue.setReferences(reference);
                                queProducer.sendToQueueWithHodlingTaxPosting(remittanceToQue);
                                break;
                            default:
                                jdbcTemplate.update("update transfers set comments=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), reference);
                                result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                                //failed on posting CBS
                                LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                                //update the transaction status
                                break;
                        }
                    } else {
                        //transactions not exits
                        result = "{\"result\":\"789\",\"message\":\"" + reference + " Transaction not exist in the db:\"}";
                    }
                }
            } else {
                result = "{\"result\":\"101\",\"message\":\"An Error occurred During processing Please Try Again!!!!!!!: \"}";

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.info(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", result, role.getBranchCode(), role.getUserName());
        return result;
    }

    /*
     Process RTGS Transactions From TRANSFER AWAITING TO BOT ACCOUNT
     */
    public String processRTGSRemittanceFromTAToBOT(String reference, UsRole role, philae.ach.UsRole achRole) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        String apiIdentifier = "api:postGLToGLTransfer";
        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            List<Map<String, Object>> txn = getSwiftMessage(reference);
            if (txn != null) {
                String messageRequest = (String) txn.get(0).get("swift_message");
                //update message date to current date
                messageRequest = messageRequest.replace(StringUtils.substringBetween(messageRequest, ":32A:", txn.get(0).get("currency").toString()), DateUtil.now("yyMMdd"));
                //check if its transacfer after cutt-off/transfer that needs backoffice approvals
                if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
//                   public String processRTGSRemittanceToCoreBanking(String reference, UsRole role, philae.ach.UsRole achRole) {
                    result = processRTGSRemittanceToCoreBanking(reference, role, achRole);

                    //LOG RESUTS FROM CORE BANKING
                    LOGGER.info("RESULTS FROM POSTING[TRANSACTIONS ON CUT-OFF OR HIGH AMOUNTS]::::{}", result);
                    String result2 = StringUtils.substringBetween(result, "\"result\":\"", "\",\"message\"");
                    ResultObjectResp responseObject=jacksonMapper.readValue(result,ResultObjectResp.class);
                    //check response
                    if (responseObject.getResult().equalsIgnoreCase("0")||responseObject.getResult().equalsIgnoreCase("26")) {
                        if (txn.get(0).get("txn_type").equals("005")) {
                            messageRequest = "";
                        }
                        //update local db
                        jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success',hq_approved_by=?,hq_approved_dt=?,swift_message=? where  reference=?", role.getUserName(), DateUtil.now(), messageRequest, reference);
                        if (txn.get(0).get("code").toString().equalsIgnoreCase("IB")) {
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(reference + "^SUCCESS");
                        }
                        if (systemVariables.IS_TISS_VPN_ALLOWED && systemVariables.IS_TISS_VPN_ALLOWED_FOR_BRANCH && txn.get(0).get("txn_type").equals("001")) {
                            //SEND TRANSACTION TO BOT
                            //Create signature
                            String signedRequestXML = signTISSVPNRequest(messageRequest, systemVariables.SENDER_BIC);
                            String response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariables.BOT_TISS_VPN_URL, reference, systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, systemVariables.getSysConfiguration("BOT.tiss.daily.token", "prod"), systemVariables.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH,systemVariables.PRIVATE_TISS_VPN_KEYPASS);
                            if (response != null && !response.equalsIgnoreCase("-1")) {
                                //get message status and update transfers table with response to IBD approval
                                String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
                                if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                                    // insert into reports for
                                    swiftRepository.saveSwiftMessageInTransferAdvices(messageRequest,"BOT-VPN","OUTGOING");

                                    //the message is successfully on BOT ENDPOINT
                                    result = "{\"result\":\"0\",\"message\":\"" + result2 + ":Transaction is ACCEPTED By BOT successfully. Await settlement to receipient Bank. \"}";
                                } else {
                                    result = "{\"result\":\"96\",\"message\":\"Transaction is REJECTED By BOT \"}";
                                }
                            }
                        } else {
                            if (txn.get(0).get("txn_type").equals("004") || txn.get(0).get("txn_type").equals("001")) {
                                queProducer.sendToQueueRTGSToSwift(messageRequest + "^" + systemVariables.KPRINTER_URL + "^" + reference);
                            }
                        }
                    } else if (result2.equalsIgnoreCase("96") || result2.equalsIgnoreCase("53")) {
                        //do nothing retain the transactions on the workflow for approval again

                    } else {
                        jdbcTemplate.update("update transfers set status='F',cbs_status='F',comments='Failed," + result + "',hq_approved_by=?,hq_approved_dt=? where  reference=?", role.getUserName(), DateUtil.now(), reference);
                        if (txn.get(0).get("code").toString().equalsIgnoreCase("IB")) {
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(reference + "^FAILED");
                        }

                        if (txn.get(0).get("code").toString().equalsIgnoreCase("E-MKOPO")) {
                            queProducer.sendToQueueOutwardAcknowledgementToEmkoPo(reference + "^FAILED");
                        }
                    }
                }
                else if(txn.get(0).get("status").equals("P") && txn.get(0).get("cbs_status").equals("C"))
                {
                    //generate the request to RUBIKON
                    TxRequest transferReq = new TxRequest();
                    transferReq.setReference(" " + (String) txn.get(0).get("reference"));
                    transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                    transferReq.setNarration((String) txn.get(0).get("purpose") + " B/O " + (String) txn.get(0).get("beneficiaryName"));
                    transferReq.setCurrency((String) txn.get(0).get("currency"));
                    if (((String) txn.get(0).get("txn_type")).equals("002") || ((String) txn.get(0).get("txn_type")).equals("001") || ((String) txn.get(0).get("txn_type")).equals("003")) {
                        transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);// BOTNostroAccount);
                        //Transactions after cut-off set debit accounts
                        //BO-backoffice approval amount
                        //BC-AFTER CUT-OFF
//                        if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
////                        apiIdentifier = ""
//                            transferReq.setDebitAccount(txn.get(0).get("sourceAcct") + "");
//                        }
                    }
                    if(((String) txn.get(0).get("txn_type")).equals("005") && txn.get(0).get("cbs_status").equals("C")){
                        transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);// BOTNostroAccount);
                    }
                    if (txn.get(0).get("txn_type").equals("004")) {
                        transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
//                        if (txn.get(0).get("cbs_status").equals("BO") || txn.get(0).get("status").equals("BC")) {
//                            transferReq.setDebitAccount(txn.get(0).get("sourceAcct") + "");
//                        }
                        switch ((String) txn.get(0).get("currency")) {
                            case "USD":
                            case "GBP":
                                transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                                break;
                            case "KES":
                            case "UGX":
                                transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);// BOTNostroAccount);
                                break;
                            case "EUR":
                                transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);// BHF NOSTRO ACCOUNT;
                                break;
                            case "ZAR":
                                transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                                break;
                            default:
                                transferReq.setCreditAccount("0-000-00-0000-0000000");
                                break;
                        }
                    }
                    transferReq.setUserRole(role);
                    PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
                    postOutwardReq.setRequest(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, apiIdentifier), XaResponse.class
                    );
                    if (cbsResponse == null) {
                        LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                        //do not update the transaction status
                        return result;
                    }
                    if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                        if (txn.get(0).get("code").toString().equalsIgnoreCase("IB")) {
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(reference + "^SUCCESS");
                        }

                        if (txn.get(0).get("code").toString().equalsIgnoreCase("E-MKOPO")) {
                            queProducer.sendToQueueOutwardAcknowledgementToEmkoPo(reference + "^SUCCESS");
                        }
                        messageRequest = messageRequest.replace(StringUtils.substringBetween(messageRequest, ":32A:", txn.get(0).get("currency").toString()), DateUtil.now("yyMMdd"));
                        jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success',hq_approved_by=?,hq_approved_dt=?,swift_message=? where  reference=?", role.getUserName(), DateUtil.now(), messageRequest, reference);
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been submitted to SWIFT VERIFIER STAGE ON  Workflow. \"}";
                        LOGGER.info("RTGS SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());

                        //update message date to current date
                        //check gepg if allowed for TISS VPN
                        LOGGER.info("IS_GEPG_ALLOWED_THROUGH_TISS_VPN:{}",systemVariables.IS_GEPG_ALLOWED_THROUGH_TISS_VPN);
                        LOGGER.info("TXN TRANSACTION:{}",txn.get(0).get("txn_type"));
                        LOGGER.info("TXN CODE:{}",txn.get(0).get("code"));
                        if ((systemVariables.IS_GEPG_ALLOWED_THROUGH_TISS_VPN && txn.get(0).get("txn_type").equals("003") && txn.get(0).get("code")!=null && txn.get(0).get("code").equals("GePG"))
                                || (systemVariables.IS_TISS_VPN_ALLOWED && systemVariables.IS_TISS_VPN_ALLOWED_FOR_BRANCH && txn.get(0).get("txn_type").equals("001"))) {

                            //SEND TRANSACTION TO BOT
                            //Create signature
                            String signedRequestXML = signTISSVPNRequest(messageRequest, systemVariables.SENDER_BIC);
                            String response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariables.BOT_TISS_VPN_URL, reference, systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, systemVariables.getSysConfiguration("BOT.tiss.daily.token", "prod"), systemVariables.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH,systemVariables.PRIVATE_TISS_VPN_KEYPASS);
                            if (response != null && !response.equalsIgnoreCase("-1")) {
                                //get message status and update transfers table with response to IBD approval
                                String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
                                if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                                    // insert into reports for
                                    swiftRepository.saveSwiftMessageInTransferAdvices(messageRequest,"BOT-VPN","OUTGOING");

                                    //the message is successfully on BOT ENDPOINT
                                    result = "{\"result\":\"0\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction is ACCEPTED By BOT successfully. Await settlement to receipient Bank. \"}";
                                } else {
                                    result = "{\"result\":\"96\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction is REJECTED By BOT \"}";

                                }
                            }
                        } else {
                            queProducer.sendToQueueRTGSToSwift(messageRequest + "^" + systemVariables.KPRINTER_URL + "^" + reference);
                        }

                        //
                    } else {
                        jdbcTemplate.update("update transfers set comments=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), reference);
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                        //failed on posting CBS
                        LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                        //update the transaction status
                    }

                }else {
                    result = "{\"result\":\"53\",\"message\":\" An error occured during processing. cbs status and status are not allowed \"}";
                }
            } else {
                result = "{\"result\":\"101\",\"message\":\"An Error occurred During processing Please Try Again!!!!!!!: \"}";
            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }



    //download supporting document
    public byte[] getSupportingDocument(String ref, String id) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public String returnTxnForAmmendmend(String reference, String username, String comments, String returnReason) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        Integer res = -1;
        try {
            res = jdbcTemplate.update("UPDATE  transfers set status='A',returned_by=?,returned_dt=?,comments=? WHERE reference=?",
                    username, DateUtil.now(), comments + " | " + returnReason, reference);
            LOGGER.info("RETURNING THE TRANSACTION FOR AMMENDMEND :RESULT: {} ", result);
            if (res == 1) {
                queProducer.sendToQueueIbankOrMobAmmendimentCallback(reference + "^" + username + "^" + comments + "^" + returnReason);
            }
            result = "{\"result\":\"" + res + "\",\"message\":\"Successfully returned for Ammendmend:" + res + " \"}";
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
            result = "{\"result\":\"" + res + "\",\"message\":\"" + e.getMessage() + " \"}";
        }
        return result;
    }

    public String rejectRtgsTransaction(String reference, String username, String rejectReason) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        Integer res = -1;
        try {
            res = jdbcTemplate.update("UPDATE  transfers set status='F',returned_by=?,returned_dt=?,comments=? WHERE reference=?",
                    username, DateUtil.now(),rejectReason , reference);
            if (res == 1) {
                queProducer.sendToQueueIbankOrMobAmmendimentCallback(reference + "^" + username + "^" + rejectReason + "^" + rejectReason);
            }
            result = "{\"result\":\"" + res + "\",\"message\":\"Transaction successfully rejected:" + res + " \"}";
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
            result = "{\"result\":\"" + res + "\",\"message\":\"" + e.getMessage() + " \"}";
        }
        return result;
    }

    /*
    get BOT TOKEN
     */
 /*
    Returned transactions for ammendmend from chief cashier
     */
    public String getRTGSForAmmendmendAjax(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where status='A'  AND branch_no=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class
            );
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',sender_name,' ',destinationAcct,' ',beneficiaryBIC,' ',beneficiaryName,' ',amount,' ',purpose,' ',initiated_by,' ',status,' ',cbs_status) LIKE ? and status='A'  AND branch_no=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfers  " + searchQuery, new Object[]{searchValue, branchNo}, Integer.class
                );
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchNo});

            } else {
                mainSql = "select * from transfers where  status='A'  AND   branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.debug(mainSql.replace("?", "'" + branchNo + "'"));
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
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
    Returned transactions for ammendmend from IDB
     */
    public String getRTGSForAmmendmendFromIBDAjax(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where status='A' and cbs_status='C' AND  code<>'IB' and branch_no=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class
            );
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',sender_name,' ',destinationAcct,' ',beneficiaryBIC,' ',beneficiaryName,' ',amount,' ',purpose,' ',initiated_by,' ',status,' ',cbs_status) LIKE ? and status='A' and cbs_status='C' AND  code<>'IB' and branch_no=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfers  " + searchQuery, new Object[]{searchValue, branchNo}, Integer.class
                );
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchNo});

            } else {
                mainSql = "select * from transfers where  status='A' and cbs_status='C' AND  code<>'IB' and branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.debug(mainSql.replace("?", "'" + branchNo + "'"));
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
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
     *GET PENDING TRANSACTIONS AT IBD
     */
    public String getRTGSpendingAtIBDAjax(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfers where txn_type!='0101' and status='P' and cbs_status='C' AND  code<>'IB' and branch_no=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class
            );

//            LOGGER.info("==========query results", totalRecords);

            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE txn_type!='0101' and concat(reference,' ',sourceAcct,' ',sender_name,' ',destinationAcct,' ',beneficiaryBIC,' ',beneficiaryName,' ',amount,' ',purpose,' ',initiated_by,' ',status,' ',cbs_status) LIKE ? and status='P' and cbs_status='C' AND  code<>'IB' and branch_no=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfers  " + searchQuery, new Object[]{searchValue, branchNo}, Integer.class
                );
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchNo});

            } else {
                mainSql = "select * from transfers where txn_type!='0101' and status='P' and cbs_status='C' AND  code<>'IB' and branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.debug("======***Search query results",mainSql.replace("?", "'" + branchNo + "'"));
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";

        LOGGER.info("=======JSON RESPONSE", json);
        return json;
    }

    /*
    get the banks list
     */
    public List<Map<String, Object>> getBanksList() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM banks");
        } catch (Exception ex) {
            LOGGER.error("EXCEPTION: {}", ex);
            return null;
        }
    }

    /*
    get the banks list
     */
    public List<Map<String, Object>> getBanksListForTipsOnly() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM banks where identifier='LOCAL' and tips_bank_code<>'-1' and fsp_status='1'");
        } catch (Exception ex) {
            LOGGER.error("EXCEPTION: {}", ex);
            return null;
        }
    }

    /*
    get the banks list
     */
    public List<Map<String, Object>> getTaxCategoryList() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM taxes_categories");
        } catch (Exception ex) {
            LOGGER.error("EXCEPTION: {}", ex);
            return null;
        }
    }

    /*
    get the service provider list
     */
    public List<Map<String, Object>> getServiceProvidersLists() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM service_providers");
        } catch (Exception ex) {
            LOGGER.error("EXCEPTION: {}", ex);
            return null;
        }
    }

    /*
    get the service provider details
     */
    public String getServiceProvidersDetails(String id) {
        String jsonString = null;
        try {

            jsonString = this.jacksonMapper.writeValueAsString(this.jdbcTemplate.queryForList("SELECT * FROM service_providers where id=?", id));
            return jsonString;
        } catch (Exception ex) {
            LOGGER.info("Exception occured during getting the Service provider Details: {}", ex);
        }
        return jsonString;
    }

    public String getAccountDetails(String accountNo) {
        String jsonString = null;
        String json = null;
        try {
            List<Map<String, Object>> findAll;
            String mainSql = "select c.cust_no,c.cust_nm, a.acct_no,cu.CRNCY_CD_ISO currency, (SELECT BU_NM FROM BUSINESS_UNIT bu WHERE bu.BU_ID =a.MAIN_BRANCH_ID) branchName,\n"
                    + "' ' ACCT_DESC,REPLACE((select REPLACE(AD.ADDR_LINE_1,'None','')  from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','') ADDR_LINE_1,\n"
                    + "REPLACE((select REPLACE(AD.ADDR_LINE_2,'None','') from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','')  ADDR_LINE_2,\n"
                    + "REPLACE((select REPLACE(AD.ADDR_LINE_3,'None','') from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','') ADDR_LINE_3,\n"
                    + "REPLACE((select REPLACE(AD.ADDR_LINE_4,'None','') from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','') ADDR_LINE_4, cu.crncy_cd, DAS.CLEARED_BAL\n"
                    + " from account a, customer c, currency cu, deposit_account_summary das\n"
                    + "where a.cust_id=c.cust_id\n"
                    + "and CU.CRNCY_ID=A.CRNCY_ID\n"
                    + "and das.acct_no=a.acct_no\n"
                    + "and (a.acct_no='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')"; //"select * from tp_transaction  where msisdn like '%" + input + "%' OR txReceipt like '%" + input + "%' OR txid like '%$input%' OR txsourceAccount like '%" + input + "%' OR txdestinationAccount like '%" + input + "%' order by txdate desc limit 1000";
            LOGGER.info("getAccountDetails:{}", mainSql);
            findAll = this.jdbcRUBIKONTemplate.queryForList(mainSql);
            //Java objects to JSON string - compact-print - salamu - Pomoja.

            try {
                jsonString = this.jacksonMapper.writeValueAsString(findAll);
                //LOGGER.info("RequestBody");
            } catch (JsonProcessingException ex) {
                LOGGER.info("EXCEPTION ON GETTING ACCOUNT DETAILS: ", ex);
            }
            json = jsonString;
            return json;
        } catch (Exception e) {
            LOGGER.info("EXCEPTION ON GETTING ACCOUNT DETAILS: ", e);
            return json;
        }

    }

    public String getLedgerDetails(String accountNo) {
        String mainSql = "SELECT ' ' ADDR_LINE_1,' ' ADDR_LINE_2,' ' ADDR_LINE_3,' ' ADDR_LINE_4,' ' crncy_cd,' ' CLEARED_BAL, ga.ACCT_DESC ,(SELECT BU_NM FROM BUSINESS_UNIT bu WHERE bu.BU_NO =ga.BAL_CD) branchName FROM GL_ACCOUNT ga WHERE ga.GL_ACCT_NO =?";
        LOGGER.info(mainSql);
        List<Map<String, Object>> findAll = this.jdbcRUBIKONTemplate.queryForList(mainSql, new Object[]{accountNo});
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(findAll);
        } catch (JsonProcessingException ex) {
            LOGGER.info("EXCEPTION ON GETTING ACCOUNT DETAILS: ", (Throwable) ex);
        }
        String json = jsonString;
        return json;
    }

    public List<Map<String, Object>> getTransferCuttOff(String transferType) {
        Date date = new Date();
        String dayWeekText = new SimpleDateFormat("EEEE").format(date);
        List<Map<String, Object>> findAll = null;
        try {
            findAll = this.jdbcTemplate.queryForList("select * from transfer_calendar where transfer_type=? and day_of_week=?", new Object[]{transferType, dayWeekText.toLowerCase()});
        } catch (Exception ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSFER CUTT-OFF: ", (Throwable) ex);
        }
        return findAll;
    }

    /*
    GET RTGS dashboard
     */
    public List<Map<String, Object>> getRTGSModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=? order by a.id asc", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    /*
    GET
     */
    public String getEchangeRateFrmCBS(String accountNo, String currency) {
        List<Map<String, Object>> result = null;
        String jsonString = null;
//        String mainSql = "SELECT VER.CRNCY_ID,CASE RATE_TY_ID WHEN 11 THEN 'buying_rate' WHEN 29 THEN  'selling_rate' ELSE 'unkown' END AS type,\n"
//                + "   CASE \n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4)\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4)\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4)\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864)),4)\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841)),4)\n"
//                + "   ELSE 1\n"
//                + "   END AS rate,\n"
//                + "   EXCH_RATE rate2,\n"
//                + "   CASE \n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN 'SELLING'\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN 'SELLING'\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN 'CROSS-CURRENCY BUYING'\n"
//                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN 'CROSS-CURRENCY SELLING'\n"
//                + "   ELSE 'NO DEFINED'\n"
//                + "   END AS fxType\n"
//                + "   FROM V_EXCHANGE_RATE ver WHERE VER.RATE_TY_ID IN (11,29) AND CRNCY_CD_ISO =?\n"
//                + "   ";

        String mainSql = "SELECT VER.CRNCY_ID,CASE RATE_TY_ID WHEN 11 THEN 'buying_rate' WHEN 29 THEN  'selling_rate' ELSE 'unkown' END AS type,\n"
                + "   CASE \n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864)),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841)),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=863 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=863),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=852 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=863),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=841 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=841)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=863)),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=863 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=863)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=841)),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=863 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=863),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=864 THEN ROUND((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=863 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=863)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=864)),4)\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=843 AND ver.CRNCY_ID=864 THEN ROUND(((SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =29 AND CRNCY_ID=864)/(SELECT ver2.EXCH_RATE FROM V_EXCHANGE_RATE ver2 WHERE ver2.RATE_TY_ID =11 AND CRNCY_ID=863)),4)\n"
                + "\n"
                + "   ELSE 1\n"
                + "   END AS rate,\n"
                + "   EXCH_RATE rate2,\n"
                + "   CASE \n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=841 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=864 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN 'CROSS-CURRENCY BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN 'CROSS-CURRENCY SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=863 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=863 THEN 'CROSS-CURRENCY SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=841 THEN 'CROSS-CURRENCY BUYING'\n"
                + "\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=864 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=863 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=864 THEN 'CROSS-CURRENCY SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=863 THEN 'CROSS-CURRENCY BUYING'\n"
                + "   ELSE 'NO DEFINED'\n"
                + "   END AS fxType\n"
                + "FROM V_EXCHANGE_RATE ver WHERE VER.RATE_TY_ID IN (11,29) AND CRNCY_CD_ISO =? ";
        try {
            LOGGER.info(mainSql.replace("?", currency));
            result = this.jdbcRUBIKONTemplate.queryForList(mainSql, currency);
            System.out.println("EXCHANGE RATE RESULTS: " + result);
            jsonString = this.jacksonMapper.writeValueAsString(result);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return jsonString;
    }

    public BanksListResp getBankBinkList(String bankType, String searchValue) {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<bankListRequset>\n"
                + "<bankType>" + bankType + "</bankType>\n"
                + "<searchValue>" + searchValue + "</searchValue>\n"
                + "</bankListRequset>";
        String response = webserviceRepo.banksList(request);
        BanksListResp list = XMLParserService.jaxbXMLToObject(response, BanksListResp.class
        );
        return list;
    }

    public String getInwardrtgsSummaryPerBIC(String fromDate, String todate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "select count(*) from (SELECT senderBIC FROM transfers where create_dt>=? and create_dt<=? and txn_type IN ('001','004') group by senderBIC ) as derivedTable";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, todate}, Integer.class
            );
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(status,' ',senderBic) LIKE ?  and  txn_type IN ('001','004') and create_dt>=? and create_dt<=? AND direction='INCOMING' ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("select count(*) from (SELECT senderBIC FROM transfers  " + searchQuery + " GROUP by senderBIC ) as derivedTable", new Object[]{searchValue, fromDate, todate}, Integer.class
                );
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
                        + "		swift_code = a.senderBIC) bankName ,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
                        + "		and cbs_status = 'C' AND direction='INCOMING') as totalSuccess,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
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
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
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
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
                        + "		and cbs_status = 'F' AND direction='INCOMING'), 0) as totalFailedAmt\n"
                        + "from\n"
                        + "	transfers a\n"
                        + "where\n"
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
                        + "		swift_code = a.senderBIC) bankName ,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
                        + "		and cbs_status = 'C' AND direction='INCOMING') as totalSuccess,\n"
                        + "	(\n"
                        + "	select\n"
                        + "		count(*)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
                        + "		and cbs_status = 'F' AND direction='INCOMING') as totalFailed,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
                        + "		and cbs_status = 'C' AND direction='INCOMING'), 0) as totalSuccessAmt,\n"
                        + "	IFNULL((\n"
                        + "	select\n"
                        + "		sum(amount)\n"
                        + "	from\n"
                        + "		transfers b\n"
                        + "	where\n"
                        + "		b.create_dt >= '" + fromDate + "'\n"
                        + "		and b.create_dt <= '" + todate + "'\n"
                        + "		and b.txn_type IN ('001','004')\n"
                        + "		and b.senderBIC = a.senderBIC\n"
                        + "		and status = 'C'\n"
                        + "		and cbs_status = 'F' AND direction='INCOMING'), 0) as totalFailedAmt\n"
                        + "from\n"
                        + "	transfers a\n"
                        + "where\n"
                        + "   txn_type IN ('001','004') and create_dt>=? and create_dt<=? AND direction='INCOMING' GROUP by senderBIC ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
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

    public String getInwardRTGSSuccessPerBankAjax(String fromDate, String toDate, String senderBic, String txnstatus, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(senderBIC) FROM transfers where create_dt>=? and create_dt<=? and txn_type IN('001','004') and senderBIC=? and cbs_status=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, senderBic, txnstatus,}, Integer.class
            );
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(status,' ',senderBic) LIKE ?  and  txn_type IN('001','004') and create_dt>=? and create_dt<=?  and senderBIC=? and cbs_status=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(senderBIC) FROM transfers  " + searchQuery, new Object[]{searchValue, senderBic, txnstatus}, Integer.class
                );
                mainSql = "SELECT reference,message_type,sourceAcct,sender_name,destinationAcct,beneficiaryName,amount,currency,purpose,beneficiaryBIC,cbs_status,response_code,message,CAST(create_dt AS char) as create_dt FROM transfers  " + searchQuery + " GROUP by senderBIC ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, senderBic});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT reference,message_type,sourceAcct,sender_name,destinationAcct,beneficiaryName,amount,currency,purpose,cbs_status,response_code,message,beneficiaryBIC,CAST(create_dt AS char) as create_dt FROM transfers a where create_dt>=? and create_dt<=? and txn_type IN('001','004') and senderBIC=? and cbs_status=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
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

    public String getTRAccountNoUsingSpCode(String spCode) {
        String accountNo = "";
        String[] responseCodes = systemVariables.TRA_ACCOUNT_HIGH_VALUES_SPCODE_MAPPINGS.split("\\^");
        for (int i = 0; i < responseCodes.length; i++) {
            if (responseCodes[i].split("=")[0].equalsIgnoreCase(spCode)) {
                accountNo = responseCodes[i].split("=")[2];
            }
        }
        return accountNo;

    }

    public String getTRAaccountNameUsingSpCode(String spCode) {
        String accountNo = "";
        String[] responseCodes = systemVariables.TRA_ACCOUNT_HIGH_VALUES_SPCODE_MAPPINGS.split("\\^");
        for (int i = 0; i < responseCodes.length; i++) {
            if (responseCodes[i].split("=")[0].equalsIgnoreCase(spCode)) {
                accountNo = responseCodes[i].split("=")[1];
            }
        }
        return accountNo;

    }

    public String getTransactionCalender(String dayOfWeek, String transferType) {
        String result = null;
        try {
            result = this.jdbcTemplate.queryForObject("SELECT status from transfer_calendar where day_of_week =? and transfer_type =?", new Object[]{dayOfWeek.toLowerCase(), transferType}, (rs, rowNum) -> rs.getString(1));
//        this.jdbcTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            LOGGER.error("Error getting Transaction day of week callender", e);

        }
        return result;
    }

    public String signTISSVPNRequest(String rawXMLV, String bankCode) {
        String rawXML = rawXMLV.replace("&"," ");
        String rawXmlsigned = null;
        try {

            String signature = sign.CreateSignature(rawXML, systemVariables.PRIVATE_TISS_VPN_KEYPASS,
                    systemVariables.PRIVATE_TISS_VPN_KEY_ALIAS, systemVariables.PRIVATE_TISS_VPN_KEY_FILE_PATH);

            rawXmlsigned = rawXML + "|" + signature;
//            queProducer.sendToQueueEftFrmCBSToTACH(bankCode + DateUtil.now("yyyyMMddHms") + ".i^" + rawXmlsigned);
//            FileUtils.writeStringToFile(new File("C:\\Users\\HP\\Desktop\\BOT\\NEWCERT\\" + bankCode + DateUtil.now("yyyyMMddHms") + ".i"), rawXmlsigned, StandardCharsets.UTF_8);
            LOGGER.info("\nTISS VPN  SIGNED REQUEST :{}\n{}", bankCode, rawXmlsigned);
        } catch (Exception ex) {
            LOGGER.error("Generating digital signature...{}", ex);
        }
        return rawXmlsigned;
    }

    /*
     * GET TRANSFER ADVICES
     */
    public String getTransferAdvicesReportAjax(String messageType, String fromDate, String toDate, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        LOGGER.info("FROM DATE:{} TO DATE:{}", fromDate, toDate);
        try {
            if (messageType.equalsIgnoreCase("all")) {
                if ((fromDate != null || !fromDate.equals(""))) {
                    mainSql = "select count(*) from transfer_advices where transDate>=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate + " 00:00:00"}, Integer.class);
                } else if ((toDate != null || !toDate.equals(""))) {
                    mainSql = "select count(*) from transfer_advices where transDate<=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{toDate + " 23:59:59"}, Integer.class);
                } else if ((fromDate != null || !fromDate.equals("")) && (toDate != null || !toDate.equals(""))) {
                    mainSql = "select count(*) from transfer_advices where transDate>=? and transDate<=?";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
                } else {
                    mainSql = "select count(*) from transfer_advices";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
                }
            } else {
                if ((fromDate != null || !fromDate.equals(""))) {
                    mainSql = "select count(*) from transfer_advices where messageType=? and  transDate>=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{messageType, fromDate + " 00:00:00"}, Integer.class);
                } else if ((toDate != null || !toDate.equals(""))) {
                    mainSql = "select count(*) from transfer_advices where messageType=? and   transDate<=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{messageType, toDate + " 23:59:59"}, Integer.class);
                } else if ((fromDate != null || !fromDate.equals("")) && (toDate != null || !toDate.equals(""))) {
                    mainSql = "select count(*) from transfer_advices where messageType=? and  transDate>=? and transDate<=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{messageType, fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
                } else {
                    mainSql = "select count(*) from transfer_advices where messageType=? ";
                    totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{messageType}, Integer.class);
                }
            }

            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                if (messageType.equalsIgnoreCase("all")) {
                    LOGGER.info(jsonString);
                    if ((fromDate != null || !fromDate.equals(""))) {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and transDate>=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, fromDate + " 00:00:00"}, Integer.class);
                    } else if ((toDate != null || !toDate.equals(""))) {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and transDate<=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, toDate + " 23:59:59"}, Integer.class);
                    } else if ((fromDate != null || !fromDate.equals("")) && (toDate != null || !toDate.equals(""))) {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and transDate>=? and  transDate<=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
                    } else {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue}, Integer.class);
                    }
                } else {
                    if ((fromDate != null || !fromDate.equals(""))) {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and messageType=? and transDate>=? and transDate<=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, messageType, fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
                    } else if ((toDate != null || !toDate.equals(""))) {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and messageType=? and transDate>=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, messageType, toDate + " 23:59:59"}, Integer.class);
                    } else if ((fromDate != null || !fromDate.equals("")) && (toDate != null || !toDate.equals(""))) {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and messageType=? and transDate>=? and transDate<=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, messageType, fromDate + " 00:00:00", toDate + " 23:59:59"}, Integer.class);
                    } else {
                        searchQuery = " WHERE  concat(messageType,' ', senderBank,' ', receiverBank,' ', direction,' ', channel,' ', senderReference,' ', relatedReference,' ', transDate,' ', currency,' ', amount,' ', serviceCode,' ', status) LIKE ? and messageType=?";
                        totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_advices  " + searchQuery, new Object[]{searchValue, messageType}, Integer.class);
                    }
                }
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                if (messageType.equalsIgnoreCase("all")) {
                    if ((fromDate != null || !fromDate.equals(""))) {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate + " 00:00:00"});
                    } else if ((toDate != null || !toDate.equals(""))) {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, toDate + " 23:59:59"});

                    } else if ((fromDate != null || !fromDate.equals("")) && (toDate != null || !toDate.equals(""))) {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate + " 00:00:00", toDate + " 23:59:59"});

                    } else {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
                    }
                } else {
                    if ((fromDate != null || !fromDate.equals(""))) {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, messageType, fromDate + " 00:00:00"});
                    } else if ((toDate != null || !toDate.equals(""))) {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, messageType, toDate + " 23:59:59"});
                    } else if ((fromDate != null || !fromDate.equals("")) && (toDate != null || !toDate.equals(""))) {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, messageType, fromDate + " 00:00:00", toDate + " 23:59:59"});
                    } else {
                        mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from  transfer_advices  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, messageType});
                    }
                }

            } else {
                if (messageType.equalsIgnoreCase("all")) {
                    mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from transfer_advices where transDate>=? and transDate<=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate + " 00:00:00", toDate + " 23:59:59"});
                } else {
                    mainSql = "select id,messageType,senderBank,receiverBank,direction,channel,CAST(transDate AS char) as txndate,senderReference,relatedReference,currency,amount,serviceCode,status,valueDate from transfer_advices where  messageType=? and  transDate>=? and transDate<=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                    LOGGER.debug(mainSql.replace("?", "'" + branchNo + "'"));
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{messageType, fromDate + " 00:00:00", toDate + " 23:59:59"});
                }
            }
            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException | DataAccessException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
     *download supporting document
     */
    public byte[] getTransferAdviceCopy(String senderReference, String senderBank, String receiverBank) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcTemplate.queryForObject("select messageInPdf from transfer_advices where senderBank=? and senderReference=? and receiverBank=?", new Object[]{senderBank, senderReference, receiverBank}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public String getTransferIncomingDestAcctsExceptions() {
        List<Map<String, Object>> result = null;
        String response = "-1";
        try {
            result = this.jdbcTemplate.queryForList("SELECT account,accountName,accountType from transferincomingexceptions", new Object[]{});
            response = jacksonMapper.writeValueAsString(result);
        } catch (Exception e) {
            LOGGER.error("Exception On getting TransferIncomingExceptionsAccounts", e);
            LOGGER.info(null, e);

        }
        return response;
    }

    public String replayRTGSIncomingTOCBS(String references, String accounts, String optionType, String reason, String branchNo, UsRole role, philae.ach.UsRole achRole, List<String> txnids, String modDestAcct) {
        String result = "{\"result\":\"0\",\"message\":\"Transaction is being Processed please confirm on RUBIKON\"}";
        if (optionType.split("==")[1].equalsIgnoreCase("6") & (branchNo == null || branchNo.equalsIgnoreCase(""))) {
            result = "{\"result\":\"99\",\"message\":\"Branch Number is required, when replaying transaction to BRANCH TRANSFER AWAITING\"}";
        } else if (optionType.split("==")[1].equalsIgnoreCase("7") & (reason == null || reason.equalsIgnoreCase(""))) {
            result = "{\"result\":\"99\",\"message\":\"Return Reason is required and its lenght Must be less than 100 words\"}";

        } else {
            String responseString = "";
            int responseCode = -1;
            int i = 0;
            for (String reference : txnids) {
                LOGGER.info("TRANSACTION PROCESSED MANUALLY: BRANCH: {} REFERENCE:{} reason:{} REPLAYED BY:{}, modDestAcct: {}", branchNo, reference, reason, role.getUserName(), modDestAcct);
                //get the transaction using the reference number
                String destinationAccount = optionType.split("==")[0];
                String option = optionType.split("==")[1];
                //if its replayed to transfer awaiting then set ledger to it
                if (optionType.split("==")[1].equalsIgnoreCase("6")) {
                    destinationAccount = systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", branchNo);
                }
                if (option.equalsIgnoreCase("5")) {
                    //replay the transaction to core banking
                    int res = processRTGSReplayToCoreBanking(reference, role, achRole, modDestAcct);
                    switch (res) {
                        case 0:
                            responseCode = res;
                            responseString += " Transaction with Reference:" + reference + " successfully replayed to account";
                            break;
                        case 26:
                            responseCode = res;
                            responseString += " DUPLICATE: Transaction with Reference:" + reference + " successfully replayed to account";
                            break;
                        case 888:
                            responseCode = res;
                            responseString += "ITS GEPG ACCOUNT: Transaction with Reference:" + reference + " Cannot be replayed because destination is a GePG Account";
                            break;
                        case 889:
                            responseCode = res;
                            responseString += "ITS CMS ACCOUNT: Transaction with Reference:" + reference + " Cannot be replayed because destination is a CMS Account";
                            break;
                        default:
                            responseCode = res;
                            responseString += " Transaction with Reference:" + reference + " failed to be replayed to account: ERROR CODE: ";
                            break;
                    }//
                } else if (option.equalsIgnoreCase("7")) {
                    //return transaction to sender bank
                    DateFormat df = new SimpleDateFormat("MMddHHmmss");
                    String formattedDate = df.format(Calendar.getInstance().getTime());
                    String direction = "T";
                    String reference2 = "STP" + direction + i + formattedDate;
                    int res = processRTGSReturnToSendingBank(reference, reference2, reason, role);
                    if (res == 1) {
                        responseCode = 0;
                        responseString += " Transaction with Reference:" + reference + " Successfully Initiated to Supervisors workflow;";
                    } else {
                        responseCode = res;
                        responseString += " Transaction with Reference:" + reference + " Failed to be Initiated ERROR CODE:" + res;
                    }
                    //processRTGSReturnToSendingBank(String reference, String returnReason, UsRole role
                } else {
                    //replay to GL ACCOUNT:
                    int res = processRTGSRepalyToAnotherDestinationAcct(reference, role, destinationAccount);
                    switch (res) {
                        case 0:
                            responseCode = res;
                            responseString += " Transaction with Reference:" + reference + " successfully replayed to account";
                            break;
                        case 26:
                            responseString += " DUPLICATE: Transaction with Reference:" + reference + " successfully replayed to account: error code:" + res;
                            break;
                        case 889:
                            responseString += "ITS CMS ACCOUNT: Transaction with Reference:" + reference + " Cannot be replayed because destination is a CMS Account";
                            break;
                        default:
                            responseCode = res;
                            responseString += " Transaction with Reference:" + reference + " failed to be replayed to account: ERROR CODE: " + res;
                            break;
                    }
                }
                i++;
            }
            result = "{\"result\":\"" + responseCode + "\",\"message\":\"" + responseString + "\"}";
//            });
        }

        LOGGER.info(
                "RESPONSE TO THE USER: {}", result);
        return result;
    }

    public int processRTGSReplayToCoreBanking(String reference, UsRole role, philae.ach.UsRole achRole, String modDestAcct) {
        int result = 99;

        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            List<Map<String, Object>> txn = getSwiftMessage(reference);
            if (txn != null) {

                String message_type = txn.get(0).get("message_type") + "";
                if (message_type.equals("202")) {
                    List<Map<String, Object>> orgTxn = getSwiftMessage(txn.get(0).get("txid") + "");
                    String direction = orgTxn.get(0).get("direction") + "";

                    if (!orgTxn.isEmpty() && direction.equalsIgnoreCase("incoming")) {
                        LOGGER.info("TRANSACTION IS INCOMING AND IT CAN NOT BE REVERSED : trans Reference {} REASON: {}", reference, reference);
                        jdbcTemplate.update("UPDATE transfers set status='C',cbs_status='C',code='SWIFT',comments='Incoming transaction can not be reversed',message=? where txid=?", " Transaction is incoming Orignal Ref: " + reference, reference);
                        return 676;
                    }
                }

                String editedDestAcctMessage = "";
                String benAccount = (String) txn.get(0).get("destinationAcct");
                if (benAccount == null) {
                    benAccount = "";
                }
                String currency = (String) txn.get(0).get("currency");

                //overide destination account
                if (modDestAcct != null && !modDestAcct.equals("")) {
                    if (!benAccount.equals(modDestAcct)) {
                        editedDestAcctMessage = "DestAcctEdited " + benAccount + "=>" + modDestAcct;
                        benAccount = modDestAcct;
                    }
                }

                if (isGePGaccount(benAccount, currency) == 0) {
                    return 888;
                }
                if (isCMSAccount(benAccount) == 0) {
                    return 889;
                }

                //generate the request to RUBIKON
                TaTransfer transferReq = new TaTransfer();
                transferReq.setReference((String) txn.get(0).get("reference"));
                transferReq.setTxnRef((String) txn.get(0).get("reference"));
                transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(txn.get(0).get("create_dt").toString(), "yyyy-MM-dd HH:mm:ss"));
                transferReq.setValueDate(DateUtil.dateToGregorianCalendar(txn.get(0).get("value_date").toString(), "yyyy-MM-dd HH:mm:ss"));
                transferReq.setEmployeeId(role.getUserId());
                transferReq.setSupervisorId(role.getUserId());
                transferReq.setTransferType("RTGS");
                transferReq.setCurrency((String) txn.get(0).get("currency"));
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setDebitFxRate(new BigDecimal(txn.get(0).get("specialRate").toString()));
                transferReq.setCreditFxRate(new BigDecimal(txn.get(0).get("specialRate").toString()));
                transferReq.setReceiverBank((String) txn.get(0).get("beneficiaryBIC"));
                transferReq.setReceiverAccount(benAccount.replace("-", ""));
                transferReq.setReceiverName((String) txn.get(0).get("beneficiaryName"));
                transferReq.setSenderBank(systemVariables.SENDER_BIC);
                transferReq.setSenderAccount((String) txn.get(0).get("sourceAcct"));
                transferReq.setSenderName((String) txn.get(0).get("sender_name"));
                transferReq.setDescription((String) txn.get(0).get("purpose") + " B/O " + (String) txn.get(0).get("sender_name") + " Ref: " + txn.get(0).get("reference"));
                transferReq.setTxnId(Long.parseLong(txn.get(0).get("id").toString()));
                String chargeScheme = "T100";
                if (txn.get(0).get("txn_type").toString().equalsIgnoreCase("001")) {
                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                } else if (txn.get(0).get("txn_type").toString().equalsIgnoreCase("004")) {

                    if (txn.get(0).get("currency").toString().equalsIgnoreCase("USD") && txn.get(0).get("senderBIC").toString().contains("SCBL") || (txn.get(0).get("corresponding_bic") != null && txn.get(0).get("corresponding_bic").toString().contains("SCBLUS"))) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(benAccount)) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                    } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("USD") && txn.get(0).get("senderBIC").toString().contains("BHFB") || (txn.get(0).get("corresponding_bic") != null && txn.get(0).get("corresponding_bic").toString().contains("BHFB"))) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(benAccount)) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                    } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("EUR")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(benAccount)) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                    } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("ZAR")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(benAccount)) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                    } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("GBP")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(benAccount)) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                    } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("TZS")) {
                        chargeScheme = "T99";
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                    } else {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(benAccount)) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        //transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                        transferReq.setContraAccount("NOSTRO-ACCOUNT-NOT-SET-FOR-THIS-PAYMENTS");
                    }
                } else {
                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                }
                //CHAGE SCHEME

                transferReq.setScheme(chargeScheme);
                transferReq.setReversal(Boolean.FALSE);
                transferReq.setUserRole(achRole);
                ProcessOutwardRtgsTransfer postOutwardReq = new ProcessOutwardRtgsTransfer();
                postOutwardReq.setTransfer(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, "ach:postInwardTransfer"), TaResponse.class
                );
                if (cbsResponse == null) {
                    LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                    //do not update the transaction status
                    return result;
                }
                if (cbsResponse.getResult() == 0) {
                    jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments=?,hq_approved_by =?,hq_approved_dt=?,response_code=? where  reference=?", editedDestAcctMessage + " TRANSACTION REPLAYED BY: " + role.getUserName() + " DATE:" + DateUtil.now(), role.getUserName(), DateUtil.now(), cbsResponse.getResult(), reference);
                    result = 0;
                    LOGGER.info("RTGS REPLAY INCOMING: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                } else {
                    if (cbsResponse.getResult() == 26) {
                        jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments=?,hq_approved_by =?,hq_approved_dt=?,response_code=?  where  reference=?", editedDestAcctMessage + " TRANSACTION REPLAYED BY: " + role.getUserName() + " DATE:" + DateUtil.now() + " FOUND IT AS DUPLICATE ON CORE BANKING, IT WAS POSTED VIA STP", role.getUserName(), DateUtil.now(), cbsResponse.getResult(), reference);
                    } else {
                        jdbcTemplate.update("update transfers set comments=?,hq_approved_by =?,hq_approved_dt=?,response_code=? where  reference=?", editedDestAcctMessage + " TRANSACTION REPLAYED BY: " + role.getUserName() + " DATE:" + DateUtil.now(), role.getUserName(), DateUtil.now(), cbsResponse.getResult(), reference);
                    }
                    result = cbsResponse.getResult();
                    //failed on posting CBS
                    LOGGER.info("RTGS REPLAY INCOMING FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                    //update the transaction status
                }

            } else {
                result = 999;

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS REPLAY INCOMING EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = 9999;
        }
        return result;
    }

    public int processRTGSRepalyToAnotherDestinationAcct(String reference, UsRole role, String destinationAcct) {
        int result = 99;

        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            List<Map<String, Object>> txn = getSwiftMessage(reference);
            if (txn != null) {
                if (isGePGaccount(destinationAcct, txn.get(0).get("currency") + "") == 0) {
                    return 888;
                }
                if (isCMSAccount(destinationAcct) == 0) {
                    return 889;
                }
                //generate the request to RUBIKON
                String apiType = "api:postGLToGLTransfer";
                TxRequest transferReq = new TxRequest();
                transferReq.setReference((String) txn.get(0).get("reference"));
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration((String) txn.get(0).get("purpose") + " B/O " + (String) txn.get(0).get("beneficiaryName"));
                transferReq.setCurrency((String) txn.get(0).get("currency"));

                if (txn.get(0).get("currency").toString().equalsIgnoreCase("USD") && (txn.get(0).get("senderBIC").toString().contains("SCBL")||(txn.get(0).get("corresponding_bic")!=null&& txn.get(0).get("corresponding_bic").toString().contains("SCBL")))) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("USD") && (txn.get(0).get("senderBIC").toString().contains("BHFB")||(txn.get(0).get("corresponding_bic")!=null&& txn.get(0).get("corresponding_bic").toString().contains("BHFB")))) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("EUR")) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                } else if (txn.get(0).get("currency").toString().equalsIgnoreCase("ZAR")) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                } else {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                }
                transferReq.setCreditAccount(destinationAcct);// BOTNostroAccount);
                transferReq.setUserRole(role);
                PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                if (!destinationAcct.contains("-")) {
                    apiType = "api:postGLToDepositTransfer";
                }
                XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, apiType), XaResponse.class
                );
                if (cbsResponse == null) {
                    LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                    //do not update the transaction status
                    return result;
                }
                if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                    result = 0;
                    jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments=?,hq_approved_by=?,hq_approved_dt=?,response_code=? where  reference=?", "REPLAYED TO: " + destinationAcct + " INTENDED DESTINATION WAS: " + txn.get(0).get("destinationAcct"), role.getUserName(), DateUtil.now(), cbsResponse.getResult(), reference);
                    LOGGER.info("RTGS REPALY SUCCESS: transReference: {} Amount: {} BranchCode: {} REPLAYED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName());
                    //
                } else {
                    jdbcTemplate.update("update transfers set comments=? ,response_code=? where  reference=?", "FAILED TO REPLAY THE TRANSACTION TO CORE BANKING: ERROR MESSAGE->" + cbsResponse.getMessage() + " : " + cbsResponse.getResult(), cbsResponse.getResult(), reference);
                    //result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                    //failed on posting CBS
                    LOGGER.info("RTGS REPLAY FAILED: transReference: {} Amount: {} BranchCode: {} REPLAYED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), role.getBranchCode(), role.getUserName(), cbsResponse.getResult());
                    //update the transaction status
                    result = cbsResponse.getResult();
                }

            } else {
                result = 999;

            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS REPLAY EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }

    public int processRTGSReturnToSendingBank(String reference, String reference2, String returnReason, UsRole role) {
        int result = 99;
        RTGSTransferForm req = new RTGSTransferForm();
        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            List<Map<String, Object>> txn = getSwiftMessage(reference);

            if (txn != null) {

                String msgType = "LOCAL";
                String senderAccount = systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER;
                if (!txn.get(0).get("txn_type").equals("001")) {
                    msgType = "INTERNATIONAL";
                }
                String correspondentBank = systemVariables.BOT_SWIFT_CODE;//DEFAULT CORRESPONDEND BANK

                req.setAmount(txn.get(0).get("amount").toString());
                req.setBatchReference(reference);
                req.setBeneficiaryAccount((String) txn.get(0).get("sourceAcct"));
                req.setBeneficiaryBIC((String) txn.get(0).get("senderBIC"));
                req.setBeneficiaryContact((String) txn.get(0).get("sender_phone"));
                req.setBeneficiaryName((String) txn.get(0).get("sender_name"));
                req.setChargeDetails("T100");
                req.setComments("RETURNED REASON: " + returnReason);
                req.setCurrency((String) txn.get(0).get("currency"));
                req.setDescription("FAILED: " + returnReason);
                req.setMessage("FAILED TO Credit via STP because of: " + returnReason);
                req.setMessageType((String) txn.get(0).get("message_type"));
                req.setReference(reference2);
                req.setRelatedReference(reference);
                req.setSenderAccount("");
                req.setSenderAddress("TANZIA COMMERCIAL BANK");
                req.setTransactionType((String) txn.get(0).get("message_type"));
                req.setTransactionDate(DateUtil.now());
                //CHECKING CURRENCY
                if (txn.get(0).get("txn_type").toString().equalsIgnoreCase("004")) {
                    if (txn.get(0).get("currency").toString().equalsIgnoreCase("USD")) {
                        correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                        senderAccount = systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER;
                    }
                    if (txn.get(0).get("currency").toString().equalsIgnoreCase("EUR")) {
                        senderAccount = systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER;
                    }
                    if (txn.get(0).get("currency").toString().equalsIgnoreCase("ZAR")) {
                        correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                        senderAccount = systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER;

                    }
                    if (txn.get(0).get("currency").toString().equalsIgnoreCase("GBP")) {
                        correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                        senderAccount = systemVariables.GBP_CORRESPONDEND_BANK;

                    }
                }
                req.setBeneficiaryAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", "060"));
                req.setSenderName("TANZANIA COMMERCIAL BANK");
                //CREATE SWIFT MESSAGE
                String swiftMessage = SwiftService.createTellerMT202(req, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference2, correspondentBank);
                req.setSwiftMessage(swiftMessage);
                LOGGER.info("GENERATED RETURN SWIFT MESSAGE MT202:{} ", swiftMessage);
                req.setSenderAccount(senderAccount);
                result = jdbcTemplate.update("INSERT INTO transfers(swift_message,message_type,txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, create_dt, branch_no, cbs_status, message,callbackurl,units) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update message=?",
                        req.getSwiftMessage(), "202", txn.get(0).get("txn_type"), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), req.getSenderBic(), req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getRelatedReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "BR", "I", req.getResponseCode(), req.getDescription(), "OUTGOING", role.getUserName(), DateUtil.now(), "060", "P", req.getMessage(), "", "RETURNED", req.getMessage());
                if (result == 1 || result == 2) {
                    result = jdbcTemplate.update("UPDATE  transfers set cbs_status='RS',message=?,initiated_by=? where reference=?",
                            "Transaction returned" + returnReason, role.getUserName(), reference);
                }
            }
        } catch (Exception e) {
            LOGGER.info(null, e);
            result = 999;
        }
        return result;

    }

    public List<Map<String, Object>> getPendingSwiftTransaction() {
        try {

            return this.jdbcTemplate.queryForList("SELECT sourceAcct,amount,reference,txid,purpose FROM transfers a where direction ='OUTGOING' and txn_type in ('001','004') and status ='C' and cbs_status ='C' and date(create_dt)  >=  date(date_ADD(NOW(), INTERVAL -3 DAY)) and swift_status is null order by create_dt  asc");
        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON TRANSFER TABLE: {}", ex.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getSwiftQueueTransaction() {
        try {

            return this.jdbcTemplate.queryForList("SELECT id, reference, body, log_date,url FROM swift_msg_queue order by log_date asc limit 10");
        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION: {}", ex.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getDuePendingSwiftTransactionBystatus(String status, String cbsStatus, String dueTime) {
        try {
            LOGGER.info("SELECT  branch_approved_dt,message_type, txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName,beneficiaryBIC,  sender_name, reference, txid,  code, purpose,  create_dt,hq_approved_dt, branch_no FROM transfers a where direction ='OUTGOING' and txn_type in ('001','004') and status =? and cbs_status =? and (create_dt) >=  (date_ADD(NOW(), INTERVAL -" + dueTime + " DAY)) and create_dt <= date_ADD(NOW(), INTERVAL -60 minute) and swift_status is null order by create_dt  asc".replace("?", "'{}'"), status, cbsStatus);

            return this.jdbcTemplate.queryForList("SELECT branch_approved_dt, message_type, txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName,beneficiaryBIC,  sender_name, reference, txid,  code, purpose,  create_dt,hq_approved_dt, branch_no FROM transfers a where direction ='OUTGOING' and txn_type in ('001','004') and status =? and cbs_status =? and (create_dt) >=  (date_ADD(NOW(), INTERVAL -" + dueTime + " DAY)) and create_dt <= date_ADD(NOW(), INTERVAL -60 minute) and swift_status is null order by create_dt  asc", status, cbsStatus);
        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON Due Pending Swift Transaction By status: {}", ex.getMessage());
            return null;
        }
    }

    public int updateSwiftInfo(String reference, String amount, String rfc) {
        try {
            int result = -1;
            return result = jdbcTemplate.update("UPDATE  transfers set swift_status=? where reference=? and amount=?",
                    rfc, reference, amount);

        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON UPDATE updateSwiftInfo: {}", ex.getMessage());
            return -1;
        }
    }

    public int deleteSwiftQueueFile(String id) {
        try {
            int result = -1;
            return result = jdbcTemplate.update("delete from  swift_msg_queue where id=?",
                    id);

        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON UPDATE updateSwiftInfo: {}", ex.getMessage());
            return -1;
        }
    }

    public int isGePGaccount(String accountNo, String currency) {
        int result = 404;
        if (accountNo == null) {
            return 404;
        }

        if (accountNo.contains("CCA0240000032")
                || accountNo.contains("024-0000032")
                || accountNo.contains("0240000032")
                || accountNo.contains("4600000812802")
                || accountNo.contains("4600000812801")
                || accountNo.contains("4900000494802")
                || accountNo.contains("4600000811001")) {
            result = 0;
        } else {
            try {
                String query = "SELECT COUNT(acct_no) FROM ega_partners WHERE acct_no=? and currency=?";
                int count = this.jdbcPartnersTemplate.queryForObject(query, new Object[]{accountNo, currency}, Integer.class
                );
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

    public List<Map<String, Object>> getSwiftTransactionByChannel(String lastId) {
        try {
            return this.jdbcTemplate.queryForList("SELECT id,reference,txid FROM transfers a where id >? order by id  asc limit 500", lastId);
        } catch (DataAccessException ex) {
            LOGGER.info("getSwiftTransactionByChannel: {}", ex.getMessage());
            return null;
        }
    }

    public String getLastPT(String name) {
        try {
            return this.jdbcTemplate.queryForObject("SELECT lastPtid FROM last_ptid where name=?", new Object[]{name}, String.class);
        } catch (DataAccessException ex) {
            LOGGER.info("getSwiftTransactionByChannel: {}", ex);
            return null;
        }
    }

    public int updateLastPT(String name, String lastid) {
        try {
            int result = -1;
            return result = jdbcTemplate.update("UPDATE last_ptid SET lastPtid=? WHERE name=?", lastid, name
            );

        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON UPDATE updateSwiftInfo: {}", ex.getMessage());
            return -1;
        }
    }

    public int SaveFilesOnline(SupportDoc rfc) {
        try {
            int result = -1;
            return result = jdbcTemplate.update("INSERT INTO transfer_document (txnReference, supportingDoc, file_name, file_size) VALUES(?, ?, ?,?)",
                    rfc.getTxnId(), rfc.getFileBlob(), rfc.getFileName(), rfc.getFileSize());

        } catch (DataAccessException ex) {
            LOGGER.info("ERROR ON UPDATE updateSwiftInfo: {}", ex.getMessage());
            return -1;
        }
    }

    public List<SupportDoc> getGetFilesFromOnline(String txnId) {
        try {
            List<SupportDoc> docs
                    = this.jdbcPartnersTemplate.query("SELECT FILE_NAME, TXN_ID, REC_ST, FILE_SIZE, FILE_EXT, FILE_BLOB, ACS_TOKEN, ID, LOG_DATE FROM tpbonline.dbo.swift_inter_bank_documents WHERE TXN_ID = ?", new Object[]{txnId},
                            (ResultSet rs, int rowNum) -> {
                                SupportDoc row = new SupportDoc();
                                row.setId(rs.getString("ID"));
                                row.setAcsToken(rs.getString("ACS_TOKEN"));
                                row.setFileBlob(rs.getBytes("FILE_BLOB"));
                                row.setFileExt(rs.getString("FILE_EXT"));
                                row.setFileName(rs.getString("FILE_NAME"));
                                row.setFileSize(rs.getString("FILE_SIZE"));
                                row.setLogDate(rs.getString("LOG_DATE"));
                                row.setRecStatus(rs.getString("REC_ST"));
                                row.setTxnId(rs.getString("TXN_ID"));
                                return row;
                            });
            return docs;
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("getSupportDocs: {}", e);
            return new ArrayList<>();
        }
    }

    public int isCMSAccount(String acct) {
        int result = 404;
        if (acct.equalsIgnoreCase("170227000078")) {//AIRTEL TRUST ACCOUNT
            result = 0;
        }
        String query = "SELECT COUNT(partner_name) FROM tpb_partners WHERE block_incom_stp = 'Y' AND (acct_no = ?  or  new_acct_no=?)";
        LOGGER.debug("SELECT COUNT(partner_name) FROM tpb_partners WHERE block_incom_stp = 'Y' AND (acct_no = '{}'  or  new_acct_no='{}')", acct, acct);
        try {
            int count = this.jdbcPartnersTemplate.queryForObject(query, new Object[]{acct, acct}, Integer.class
            );
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

    public int updateComplianceStatus(String reference) {
        try {
            return this.jdbcTemplate.update("update transfers set compliance='P' where reference=?", new Object[]{reference});
        } catch (DataAccessException ex) {
            LOGGER.info("updateComplianceStatus: {}", ex.getMessage());
            return -1;
        }
    }

    public int insertIntoKprinterPool(String reference, String swiftBody) {
        int finalRes = 0;
        String sql = "INSERT INTO kprinter_pool (reference, swiftBody, status) VALUES(?, ?,?)";
        try {
            finalRes = this.jdbcTemplate.update(sql, new Object[]{reference, swiftBody, 'I'}, int.class);
        } catch (DataAccessException ex) {
            LOGGER.info("Data access exceptions: {}", ex.getMessage());
            finalRes = 0;
        }
        return finalRes;
    }

    public List<Map<String, Object>> queryKprinterPendingTransactions() {
        String sql = "SELECT * FROM kprinter_pool WHERE status = 'I'";
        List<Map<String, Object>> finalRes = null;
        try {
            finalRes = this.jdbcTemplate.queryForList(sql);
        } catch (DataAccessException ex) {
            LOGGER.info("Data access exceptions: {}", ex.getMessage());
        }
        return finalRes;
    }

    public void updateTransactionStatus(Object reference) {
        String sql = "UPDATE kprinter_pool SET status = 'FGS' WHERE reference = ?";
        try {
            this.jdbcTemplate.update(sql, reference);
        } catch (DataAccessException ex) {
            LOGGER.info("Data access exceptions: {}", ex.getMessage());
        }
    }

    public List<Map<String, Object>> fireGetInitiatedKprinterTxnAjax(String reference) {
        List<Map<String, Object>> finalRes = null;
        try {
            String sql = "SELECT reference,swift_message,currency FROM transfers WHERE reference= ? or txid=? or batch_reference=? ";
            finalRes = jdbcTemplate.queryForList(sql, reference, reference, reference);
        } catch (DataAccessException dae) {
            LOGGER.info("Data access Exception kprinter issues... {}", dae);
        }
        return finalRes;
    }

    public Integer fireInitiateKprinterTxnToWF(String reference, String swift_message, String reason, String currency, String username) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT INTO swift_transfers(reference, swift_message, initiated_by, initiated_dt, status,message, currency) values(?,?,?,?,?,?,?)",
                    reference, swift_message, username, DateUtil.now(), "P", reason, currency);
        } catch (DataAccessException e) {
            LOGGER.info(e.getMessage());
            result = -1;
        }
        return result;
    }

    public String fireGetInitiateKprinterTxnWFAjax(String currUser, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from swift_transfers  where status='P' and initiated_by<>'" + currUser + "'";
            totalRecords = jdbcTemplate.queryForObject(mainSql, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(reference,' ',swift_message,' ',status,' ',message,' ',currency,' ',initiated_by) LIKE ? and status='P' and initiated_by<>'" + currUser + "' ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM swift_transfers " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from  swift_transfers  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select * from swift_transfers t where status='P' and initiated_by<>'" + currUser + "'  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public int insertTransctionToSwift(String reference, String swift_message, String url) {
        int result = 0;
        try {
            result = jdbcTemplate.update("INSERT INTO swift_msg_queue (reference, body,url) VALUES (?, ?,?)", reference, swift_message, url);
            LOGGER.info("Swift message for {} has been insert with:{}... URL..{}", reference, result, url);
        } catch (DataAccessException ex) {
            LOGGER.info("Swift message for {} failed to insert with:{} url:.. {}", reference, -1, ex, url);
        }
        return result;
    }

    public void updateSwiftTransfers(String reference, String approver) {
        try {
            jdbcTemplate.update("UPDATE swift_transfers SET status='C', approved_by=?, approval_dt=?, response_code='0', comments='SUCCESS' WHERE reference=?", approver, DateUtil.now(), reference);
        } catch (DataAccessException ex) {
            LOGGER.info("Exception Failed on update swift transfer table ... reference..{}", reference);
        }
    }


    public String getBankGlsAjax(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "SELECT count(*) from GL_ACCOUNT";
            totalRecords = jdbcRUBIKONTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(GL_ACCT_NO,' ',ACCT_DESC)";
                totalRecordwithFilter = jdbcRUBIKONTemplate.queryForObject("SELECT count(*) FROM GL_ACCOUNT " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "SELECT GL_ACCT_NO, ACCT_DESC  FROM GL_ACCOUNT  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcRUBIKONTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "SELECT GL_ACCT_NO, ACCT_DESC FROM GL_ACCOUNT ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcRUBIKONTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public GeneralJsonResponse getRejectedTransactionsAjax(Map<String, String> customQuery) {
        GeneralJsonResponse response = new GeneralJsonResponse();
        List<Map<String,Object>> outputData = null;
        String fromDate = customQuery.get("fromDate");
        String toDate = customQuery.get("toDate");
        String bankCode = customQuery.get("bankName");String sql = "-1";


        switch (bankCode){
            case "ALL":
                    sql = "select (select create_dt  from transfers tt where tt.direction='INCOMING' and tt.message_type='202' and tt.txid=t.reference limit 1) returnedDate,t.* from transfers t where reference in (select txid from transfers where direction='INCOMING' and message_type='202') and direction ='OUTGOING' order by returnedDate desc";
                    outputData = jdbcTemplate.queryForList(sql);
                break;
            default:
                sql = "select (select create_dt  from transfers tt where tt.direction='INCOMING' and tt.message_type='202' and tt.txid=t.reference limit 1) returnedDate,t.* from transfers t where reference in (select txid from transfers where direction='INCOMING' and message_type='202') and direction ='OUTGOING' and beneficiaryBIC=? order by returnedDate desc";
                outputData = jdbcTemplate.queryForList(sql,bankCode);
                break;
        }
        response.setStatus("SUCCESS");
        response.setResult(outputData);
        return response;

    }

    public int insertMultipleGLPostingData(Map<String, String> customeQuery, String initiatedBy, String branchNo) throws JsonProcessingException {
        Type listType = new TypeToken<List<FinanceMultipleGLMapping>>() {}.getType();
        List<FinanceMultipleGLMapping> itemArray = new Gson().fromJson( customeQuery.get("itemArray"), listType);
        String sql_summary =  "INSERT INTO vendor_transfers_summary (gl_account, txn_code, amount, currency, has_vat, reference, batch_reference, status, cbs_status, created_by, create_dt) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
          String batchRef = "BTR"+DateUtil.now("yyyyMMddHHssMM");
            LOGGER.info("check....{} and initiated date... {} by .. {} with reference... {}",itemArray, DateUtil.now(),initiatedBy,batchRef);
            String refernce = "-1";
            Integer result =0;
          String code = "-1";
          Integer i =1;
            for(FinanceMultipleGLMapping data : itemArray){
                    refernce =  batchRef+"_"+i;
                    code = data.getVat().equalsIgnoreCase("1") ? "011" : "012";
                    try {
                        jdbcTemplate.update(sql_summary,data.getGlAcctNo(),code,data.getAmount(),customeQuery.get("currency"),data.getVat(),refernce,batchRef,"L","L","SYSTEM",DateUtil.now());
                    }catch (Exception e){
                        LOGGER.info("exception found", e,null);
                    }
                    i++;
            }
        String sql_transfer = "INSERT INTO vendor_transfers (txn_type, sourceAcct, destinationAcct, amount, taxable_amount, charge, currency, beneficiaryName," +
                "beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, code, status, purpose, direction, initiated_by, " +
                "create_dt, branch_no, cbs_status, message) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//            LOGGER.info(sql_transfer.replace("?","'{}'"),"103", "-1",customeQuery.get("beneficiaryAccount"),customeQuery.get("taxableAmount"),customeQuery.get("taxableAmount"),"0",customeQuery.get("currency"),customeQuery.get("beneficiaryName"),customeQuery.get("beneficiaryBIC"),customeQuery.get("beneficiaryContact"),
//                    customeQuery.get("senderBic"),customeQuery.get("senderPhone"),customeQuery.get("senderAddress"),customeQuery.get("senderName"),batchRef,"103","L",customeQuery.get("description"),"OUT-GOING",initiatedBy,DateUtil.now(),branchNo,"L","Logged");
        result = jdbcTemplate.update(sql_transfer,"103", "-1",customeQuery.get("beneficiaryAccount").split("==")[1],customeQuery.get("taxableAmount"),customeQuery.get("taxableAmount"),"0",customeQuery.get("currency"),customeQuery.get("beneficiaryName"),customeQuery.get("beneficiaryBIC").split("==")[0],customeQuery.get("beneficiaryContact"),
                customeQuery.get("senderBic"),customeQuery.get("senderPhone"),customeQuery.get("senderAddress"),customeQuery.get("senderName"),batchRef,"103","L",customeQuery.get("description"),"OUT-GOING",initiatedBy,DateUtil.now(),branchNo,"L","Logged");
        return result;
    }


    public Boolean isCrossExchangeRate(String transactionCurrency,String account) {
        boolean result = false;
        try {
            String localCurrency ="TZS";
            String accountCurrency = this.jdbcRUBIKONTemplate.queryForObject("SELECT CRNCY_CD_ISO  from TPBLIVE.V_ACCOUNTS A WHERE A.ACCT_NO  =? OR A.OLD_ACCT_NO =?", new Object[]{account,account}, String.class);

            List<String> groupCurrencyList = Arrays.asList(transactionCurrency,accountCurrency);

            LOGGER.info("Account:{} localCurrency:{}",account,localCurrency);
            LOGGER.info("Account:{} transactionCurrency:{}",account,transactionCurrency);
            LOGGER.info("Account:{} accountCurrency:{}",account,accountCurrency);
            if(!transactionCurrency.equals(accountCurrency) &&  !groupCurrencyList.contains(localCurrency)){
                result = true;
            }
        } catch (DataAccessException ex) {
            LOGGER.info(ex.getMessage(), ex);

        }
        return result;
    }
}
