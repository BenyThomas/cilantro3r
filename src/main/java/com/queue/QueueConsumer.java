/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.queue;

import com.DTO.*;
import com.DTO.EFT.EftPacs00800102Req;
import com.DTO.IBANK.BatchPaymentReq;
import com.DTO.IBANK.PaymentCallbackResp;
import com.DTO.IBANK.PaymentReq;
import com.DTO.Teller.RTGSTransferForm;
import com.DTO.mx.MxMessageRequest;
import com.DTO.mx.Txn;
import com.DTO.pension.PensionPayrollToCoreBanking;
import com.DTO.pension.PsssfBatchBeneficiary;
import com.DTO.pension.PsssfBatchRequest;
import com.DTO.recon.BankStReconDataReq;
import com.DTO.recon.LedgerReconDataReq;
import com.DTO.recon.Requests.GeneralReconConfig;
import com.DTO.stawi.StawiBondLookupResponse;
import com.DTO.stawi.StawiBondNotificationRequest;
import com.DTO.swift.other.KipaymentGepgRequest;
import com.DTO.swift.other.Mt950ObjectReq;
import com.DTO.swift.other.TpbPartnerReference;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.prowidesoftware.swift.model.SwiftBlock3;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.mt2xx.MT202;
import com.prowidesoftware.swift.model.mx.MxPacs00200103;
import com.prowidesoftware.swift.model.mx.dic.PaymentTransactionInformation26;
import com.repository.*;
import com.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import philae.ach.PostInwardTransfer;
import philae.ach.ProcessOutwardRtgsTransfer;
import philae.ach.TaResponse;
import philae.ach.TaTransfer;
import philae.api.*;

import jakarta.annotation.Nonnull;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.queue.QueueProducer.CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE;

/**
 * @author melleji.mollel
 */
@Component
public class QueueConsumer {

    @Autowired
    QueueProducer queueProducer;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    ObjectMapper jacksonMapper;
    @Autowired
    QueueProducer queProducer;

    @Autowired
    SYSENV systemVariable;
    @Autowired
    EftRepo eftRepo;

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;

    @Autowired
    SwiftRepository swiftRepository;
    @Autowired
    FullNameAnagramService fullNameAnagramService;
    @Autowired
    SignRequest sign;

    @Autowired
    Recon_M recon_m;

    @Autowired
    RtgsRepo rtgsRepo;

    @Autowired
    CreditRepo creditRepo;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;
    private static final String QUEUE_RTGS_FILE_REQ = "queue.rtgs.file.req";
    private static final String QUEUE_BOT_TISS_VPN = "queue.bot.tiss.vpn";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueProducer.class);
    private static final String QUEUE_GEPG_REMITTANCE_TO_BOT = "queue.gepg.remittance.to.bot";
    private static final String QUEUE_GEPG_REMITTANCE_TO_SWIFT = "queue.gepg.remittance.to.swift";
    private static final String QUEUE_RTGS_WITH_HOLDING_TAX = "queue.rtgs.tax.service";
    private static final String QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER = "queue.eft.batch.customer.to.branch.ledger";
    private static final String QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ = "queue.eft.batch.branch.to.hq.ledger";
    private static final String FROM_TACH_TO_CBS_QUEUE = "FROM_TACH_TO_CBS_QUEUE";
    private static final String QUEUE_EFT_INCOMING_TO_CBS = "QUEUE_EFT_INCOMING_TO_CBS";
    private static final String QUEUE_EFT_OUTGOING_FROM_IBANK_TO_CBS = "QUEUE_EFT_OUTGOING_FROM_IBANK_TO_CBS";
    private static final String QUEUE_EFT_REPLAY_TO_CBS = "QUEUE_EFT_REPLAY_TO_CBS";
    private static final String PROCESS_BATCH_PAYMENTS_FROM_IBANK = "PROCESS_BATCH_PAYMENTS_FROM_IBANK";
    private static final String PROCESS_BATCH_PAYMENTS_FROM_BRANCH = "PROCESS_BATCH_PAYMENTS_FROM_BRANCH";
    private static final String PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK = "PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK";
    private static final String PROCESS_BATCH_TRANSACTION_CHARGE = "PROCESS_BATCH_TRANSACTION_CHARGE";
    private static final String PROCESS_BATCH_TRANSACTION_CHARGE_FROM_BRANCH = "PROCESS_BATCH_TRANSACTION_CHARGE_FROM_BRANCH";
    private static final String PROCESS_BATCH_TRANSACTION_MULTIPLE_ENTRIES = "PROCESS_BATCH_TRANSACTION_MULTIPLE_ENTRIES";
    private static final String PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING = "PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING";
    private static final String PROCESS_WALLET_TRANSFER_REVERSAL = "PROCESS_WALLET_TRANSFER_REVERSAL";
    private static final String PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT = "PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT";
    private static final String PROCESS_OUTWARD_ACKNOWLEDGEMENT_BY_TACH = "PROCESS_OUTWARD_ACKNOWLEDGEMENT_BY_TACH";
    private static final String PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK = "PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK";
    private static final String PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO = "PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO";
    private static final String PROCESS_EXERCISE_DUTY_CHARGE_SPILITING = "PROCESS_EXERCISE_DUTY_CHARGE_SPILITING";
    private static final String PROCESS_VALUE_ADDED_TAX_SPILITING = "PROCESS_VALUE_ADDED_TAX_SPILITING";
    private static final String PROCESS_POSTING_INCOME_TO_CORE_BANKING = "PROCESS_POSTING_INCOME_TO_CORE_BANKING";
    private static final String PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS = "PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS";
    private static final String PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING = "PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING";
    private static final String PROCESS_BOOK_TRANSFER = "PROCESS_BOOK_TRANSFER";
    private static final String PROCESS_LUKU_CALLBACK = "PROCESS_LUKU_CALLBACK";
    private static final String CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW = "CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW";
    private static final String PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT = "PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT";
    private static final String PROCESS_INCOMING_MT202_MESSAGES = "PROCESS_INCOMING_MT202_MESSAGES";
    private static final String PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER = "PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER";
    private static final String PROCESS_INCOMING_RTGS_TO_CILANTRODB = "PROCESS_INCOMING_RTGS_TO_CILANTRODB";
    private static final String PROCESS_STP_MT103_OUTWARD_TRANSACTIONS = "PROCESS_STP_MT103_OUTWARD_TRANSACTIONS";
    private static final String PENSION_PAYROLL_BATCH_TRANSACTIONS = "PENSION_PAYROLL_BATCH_TRANSACTIONS";
    private static final String PENSION_PAYROLL_PENSIONER_ACCOUNT_VERIFICATION = "PENSION_PAYROLL_PENSIONER_ACCOUNT_VERIFICATION";
    private static final String PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE = "PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE";
    private static final String PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING = "PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING";
    private static final String PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS = "PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS";
    private static final String INCOMING_SWIFT_STP_INWARD_TRANSACTION = "INCOMING_SWIFT_STP_INWARD_TRANSACTION";
    private static final String PROCESS_MT950_ENTRIES_FOR_REPORTS = "PROCESS_MT950_ENTRIES_FOR_REPORTS";
    private static final String QUEUE_RTGS_REPLAY_TO_CBS = "QUEUE_RTGS_REPLAY_TO_CBS";
    private static final String PROCESS_MULTIPLE_DEBITS_TO_SUSPENSE_LEDGER = "PROCESS_MULTIPLE_DEBITS_TO_SUSPENSE_LEDGER";
    private static final String PROCESS_BATCH_PAYMENTS_TO_BOT = "PROCESS_BATCH_PAYMENTS_TO_BOT";
    private static final String DOWNLOAD_GENERAL_RECON_DATA_TO_RECON_TRACKER = "DOWNLOAD_GENERAL_RECON_DATA_TO_RECON_TRACKER";
    private static final String SEND_TO_QUEUE_BOT_CLOSING_BALANCE = "SEND_TO_QUEUE_BOT_CLOSING_BALANCE";
    private static final String INSERT_INTO_TEMP_FROM_PENSIONERS = "INSERT_INTO_TEMP_FROM_PENSIONERS";
    private static final String RESOLVE_RECON_EXCEPTION = "RESOLVE_RECON_EXCEPTION";
    private static final String INSERT_DUPLICATE_TXN_INTO_RECON_TRACKER = "INSERT_DUPLICATE_TXN_INTO_RECON_TRACKER";
    private static final String DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE = "DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE";
    //private static final String PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL = "PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL";
    AtomicInteger fg = new AtomicInteger();

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("gwBrinjalDbConnection")
    JdbcTemplate jdbcBrinjalTemplate;

    @Autowired
    @Qualifier("gwAirtelVikobaDBConnection")
    JdbcTemplate jdbcAirtelVikobaTemplate;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    AllowedSTPAccountRepository allowedSTPAccountRepository;
    @Autowired
    private StawiBondNotificationClient stawiBondNotificationClient;

    @JmsListener(destination = QUEUE_GEPG_REMITTANCE_TO_BOT, containerFactory = "queueListenerFactory")
    public void processGePGRemittance(@Payload RemittanceToQueue req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        LOGGER.info("request to cbs:{}", req);
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";

        try {
            if (!req.getReferences().equals("0")) {
                //get the MESSAGE DETAILS FROM THE QUEUE
                List<Map<String, Object>> txn = getSwiftMessage(req.getReferences());
                //generate the request to RUBIKON
                TxRequest transferReq = new TxRequest();
                transferReq.setReference(req.getReferences());
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration((String) txn.get(0).get("purpose"));
                transferReq.setCurrency((String) txn.get(0).get("currency"));
                if (txn.get(0).get("txn_type").equals("003")) {
                    transferReq.setDebitAccount((String) txn.get(0).get("sourceAcct"));
                    transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                }
                transferReq.setUserRole(req.getUsRole());
                PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postDepositToGLTransfer"), XaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                    //do not update the transaction status
//                return result;
                }
                if (cbsResponse.getResult() == 0) {
                    jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", req.getBnUser().getUserName(), DateUtil.now(), req.getReferences());
                    result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Transaction has been submitted to SWIFT VERIFIER STAGE ON  Workflow. \"}";
                    LOGGER.info("RTGS SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), req.getUsRole().getBranchCode(), req.getUsRole().getUserName());
                    String messageRequest = (String) txn.get(0).get("swift_message");

                } else {
                    if (cbsResponse.getResult() == 26) {
                        result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + ":Duplicate on cbs, Transaction has not been submitted to SWIFT VERIFIER STAGE ON  Workflow. \"}";
                        jdbcTemplate.update("update transfers set status='P',cbs_status='F',comments='duplicate on cbs',branch_approved_by=?,branch_approved_dt=? where  reference=?", req.getBnUser().getUserName(), DateUtil.now(), req.getReferences());
                    } else {
                        jdbcTemplate.update("update transfers set comments=?,status='P',cbs_status='F' where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getReferences());
                    }

                    if (cbsResponse.getResult() == 96) {
                        throw new RuntimeException("[" + QUEUE_GEPG_REMITTANCE_TO_BOT + "] - error cbs response - [" + cbsResponse.getResult() + "]");
                    }
                    result = "{\"result\":\"" + cbsResponse.getResult() + "\",\"message\":\" An error occured during processing: " + cbsResponse.getMessage() + " \"}";
                    //failed on posting CBS
                    LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", txn.get(0).get("reference"), txn.get(0).get("amount"), req.getUsRole().getBranchCode(), req.getUsRole().getUserName(), cbsResponse.getResult());
                    //update the transaction status
                }
//            return result;
            }
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, req.getUsRole().getBranchCode(), req.getUsRole().getUserName());
        }
//        return result;

    }

    public List<Map<String, Object>> getSwiftMessage(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfers where reference=?", reference);
        } catch (DataAccessException ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSACTION " + fg.incrementAndGet());
            return null;
        }
    }

    public List<Map<String, Object>> getTransactionsToBeReplayed(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfers where reference =? and cbs_status<>'C'", reference);
        } catch (DataAccessException ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSACTION " + fg.incrementAndGet());
            return null;
        }
    }

    public List<Map<String, Object>> getEFTMessageByBatchReference(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfers where batch_reference2=?", reference);
        } catch (Exception ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSACTION " + fg.incrementAndGet());
            return null;
        }
    }

    public List<Map<String, Object>> getSwiftMessageWithTax(String reference) {
        try {
            return this.jdbcTemplate.queryForList("SELECT a.*,b.tax_amount,b.tax_ledger FROM transfers a INNER JOIN transfer_with_tax_finance b ON b.txnReference=a.reference WHERE a.reference=?", reference);
        } catch (Exception ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSACTION WITH TAX: " + ex.getMessage());
            return null;
        }
    }

    /*   @JmsListener(destination = QUEUE_RTGS_FILE_REQ, containerFactory = "queueListenerFactory")
    public void queueRTGSFileConsumer(@Payload String payload, @Headers MessageHeaders headers, org.springframework.messaging.Message message, Session session) throws JMSException {
        LOGGER.info("Loop:" + fg.incrementAndGet());
        LOGGER.info("[{}] receives payload - [{}]", QUEUE_RTGS_FILE_REQ, payload);
        String[] payloadArray = payload.split("\\^");
        String reqPayload;
        String url;
        String resp;
        LOGGER.info("[{}] parse payload - [payloadLength]={}", QUEUE_RTGS_FILE_REQ, payloadArray.length);
        if (payloadArray.length >= 2) {
            reqPayload = payloadArray[0];
            url = payloadArray[1];
            LOGGER.info("[{}] parse payload - [ReqPayload]={}", QUEUE_RTGS_FILE_REQ, reqPayload);
            LOGGER.info("[{}] parse payload - [url]={}", QUEUE_RTGS_FILE_REQ, url);

            resp = HttpClientService.sendXMLRequest(reqPayload, url);

            if (resp.equals("-1")) {
                LOGGER.info("[{}] - error http response - {} ", QUEUE_RTGS_FILE_REQ, resp);
                throw new RuntimeException("[" + QUEUE_RTGS_FILE_REQ + "] - error http response - [" + resp + "]");
            }
        } else {
            LOGGER.error("[{}] Invalid payload - [{}]", QUEUE_RTGS_FILE_REQ, payload);
        }
    }*/
    @JmsListener(destination = QUEUE_RTGS_FILE_REQ, containerFactory = "queueListenerFactory")
    public void queueRTGSFileConsumerf(@Payload String payload, @Headers MessageHeaders headers, org.springframework.messaging.Message message, Session session) throws JMSException {
        LOGGER.info("Loop:" + fg.incrementAndGet());
        String[] payloadArray = payload.split("\\^");
        String reqPayload;
        String url;
        String ref;
        String resp;
        LOGGER.info("[{}] parse payload - [payloadLength]={}", QUEUE_RTGS_FILE_REQ, payloadArray.length);
        if (payloadArray.length >= 3) {
            reqPayload = payloadArray[0];
            url = payloadArray[1];
            ref = payloadArray[2];
            try {
                int result = jdbcTemplate.update("INSERT INTO swift_msg_queue (reference, body,url) VALUES (?, ?,?)", ref, reqPayload, url);
                LOGGER.info("Swift message for {} has been insert with:{}", ref, result);
            } catch (DataAccessException ex) {
                LOGGER.info("Swift message for {} failed to insert with:{}", ref, -1, ex);
            }
        } else {
            LOGGER.error("[{}] Invalid payload - [{}]", QUEUE_RTGS_FILE_REQ, payload);
        }
    }

    @JmsListener(destination = QUEUE_BOT_TISS_VPN, containerFactory = "queueListenerFactory")
    public void queueRTGSTransactionToBOTConsumer(@Payload String payload, @Headers MessageHeaders headers, org.springframework.messaging.Message message, Session session) throws JMSException {
        LOGGER.info("[{}] receives payload - [{}]", QUEUE_RTGS_FILE_REQ, payload);
        String[] payloadArray = payload.split("\\^");
        String reqPayload;
        String url;
        String resp;
        LOGGER.info("[{}] parse payload - [payloadLength]={}", QUEUE_RTGS_FILE_REQ, payloadArray.length);
        if (payloadArray.length >= 2) {
            reqPayload = payloadArray[0];
            url = payloadArray[1];
            LOGGER.info("[{}] parse payload - [ReqPayload]={}", QUEUE_RTGS_FILE_REQ, reqPayload);
            LOGGER.info("[{}] parse payload - [url]={}", QUEUE_RTGS_FILE_REQ, url);
            resp = HttpClientService.sendXMLRequest(reqPayload, url);
            if (resp.equals("-1")) {
                LOGGER.info("[{}] - error http response - {} ", QUEUE_RTGS_FILE_REQ, resp);
                throw new RuntimeException("[" + QUEUE_RTGS_FILE_REQ + "] - error http response - [" + resp + "]");
            }
        } else {
            LOGGER.error("[{}] Invalid payload - [{}]", QUEUE_RTGS_FILE_REQ, payload);
        }
    }

    //generate mt103 for gepg transactions
    @JmsListener(destination = QUEUE_GEPG_REMITTANCE_TO_SWIFT, containerFactory = "queueListenerFactory")
    public void processGePGTxnsToSwift(@Payload RemittanceToQueue req, @Headers MessageHeaders headers, org.springframework.messaging.Message message, Session session) throws JMSException {
        LOGGER.info("Loop:" + fg.incrementAndGet());
        LOGGER.info("[{}] receives payload - [{}]", QUEUE_GEPG_REMITTANCE_TO_SWIFT, req);
        String result = "-1";
        //String[] payloadArray = payload.split("\\");
        if (!req.getReferences().equals("0")) {
            String reqPayload = (String) getSwiftMessage(req.getReferences()).get(0).get("swift_message");

            if (systemVariables.IS_GEPG_ALLOWED_THROUGH_TISS_VPN) {
                //SEND TRANSACTION TO BOT
                //Create signature
                String signedRequestXML = signTISSVPNRequest(reqPayload, systemVariables.SENDER_BIC);
                String response = null;
                try {
                    response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariables.BOT_TISS_VPN_URL, req.getReferences(), systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, systemVariables.getSysConfiguration("BOT.tiss.daily.token", "prod"), systemVariables.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH, systemVariables.PRIVATE_TISS_VPN_KEYPASS);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }

                if (response != null && !response.equalsIgnoreCase("-1")) {
                    //get message status and update transfers table with response to IBD approval
                    String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
                    if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                        // insert into reports for
                        swiftRepository.saveSwiftMessageInTransferAdvices(reqPayload, "BOT-VPN", "OUTGOING");
                        jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success file generated tiss vpn',hq_approved_by=?,hq_approved_dt=?, swift_status='BOT-TISS-VPN' where  reference=?", req.getBnUser().getUserName(), DateUtil.now(), req.getReferences());

                        //the message is successfully on BOT ENDPOINT
                        result = "{\"result\":\"0\",\"message\":\"" + reqPayload + ":Transaction is ACCEPTED By BOT successfully. Await settlement to receipient Bank. \"}";
                    } else {
                        result = "{\"result\":\"96\",\"message\":\"" + reqPayload + ":Transaction is REJECTED By BOT \"}";

                    }
                }

            } else {
                String resp;
                LOGGER.info("[{}] parse payload - [ReqPayload]={}", QUEUE_GEPG_REMITTANCE_TO_SWIFT, reqPayload);
                LOGGER.info("[{}] parse payload - [url]={}", QUEUE_GEPG_REMITTANCE_TO_SWIFT, systemVariables.KPRINTER_URL);
                resp = HttpClientService.sendXMLRequest(reqPayload, systemVariables.KPRINTER_URL);
                if (resp.equals("-1")) {
                    LOGGER.info("[{}] - error http response - {} ", QUEUE_GEPG_REMITTANCE_TO_SWIFT, resp);
                    throw new RuntimeException("[" + QUEUE_GEPG_REMITTANCE_TO_SWIFT + "] - error http response - [" + resp + "]");
                } else {
                    if (resp.split("\\|")[0].equals("OK")) {
                        jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success file generated swift',hq_approved_by=?,hq_approved_dt=? where  reference=?", req.getBnUser().getUserName(), DateUtil.now(), req.getReferences());
                    } else if (resp.split("\\|")[0].equals("DUPLICATE")) {
                        jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success file generated swift:duplicate',hq_approved_by=?,hq_approved_dt=? where  reference=?", req.getBnUser().getUserName(), DateUtil.now(), req.getReferences());
                    } else if (resp.split("\\|")[0].equals("NOT_OK")) {
                        jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Failed',hq_approved_by=?,hq_approved_dt=? where  reference=?", req.getBnUser().getUserName(), DateUtil.now(), req.getReferences());
                    }
                }
            }
        }

    }

    /*
     Process RTGS Transactions and POST TAX TO CBS
     */
    @JmsListener(destination = QUEUE_RTGS_WITH_HOLDING_TAX, containerFactory = "queueListenerFactory")
    public void financePostWithHoldingTax(@Payload RemittanceToQueue req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        //get the MESSAGE DETAILS FROM THE QUEUE
        List<Map<String, Object>> txn = getSwiftMessageWithTax(req.getReferences());
        //generate the request to RUBIKON
        if (txn != null) {
            BigDecimal taxAmount = new BigDecimal(txn.get(0).get("tax_amount").toString());
            BigDecimal bg1, bg2;
            bg1 = new BigDecimal("0");
            bg2 = taxAmount;
            int res = bg1.compareTo(bg2);
            if (res < 0) {
                TxRequest transferReq = new TxRequest();
                transferReq.setReference(txn.get(0).get("reference") + "  ");
                transferReq.setAmount(new BigDecimal(txn.get(0).get("tax_amount").toString()));
                transferReq.setNarration(txn.get(0).get("purpose") + " B/O " + (String) txn.get(0).get("beneficiaryName") + (String) txn.get(0).get("reference"));
                transferReq.setCurrency((String) txn.get(0).get("currency"));
                if (txn.get(0).get("txn_type").equals("002") || ((String) txn.get(0).get("txn_type")).equals("001") || ((String) txn.get(0).get("txn_type")).equals("003")) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_TISS_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    transferReq.setCreditAccount(txn.get(0).get("tax_ledger").toString());
                }
                if (txn.get(0).get("txn_type").equals("004")) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_TT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    transferReq.setCreditAccount(txn.get(0).get("tax_ledger").toString());
                }
                transferReq.setUserRole(req.getUsRole());
                PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToGLTransfer"), XaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("POSTING TAX: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : Trans Reference {}", txn.get(0).get("reference"));
                    //do not update the transaction status
                    LOGGER.info("[{}] - error http response - {} ", QUEUE_RTGS_WITH_HOLDING_TAX, cbsResponse);
                    throw new RuntimeException("[" + QUEUE_RTGS_WITH_HOLDING_TAX + "] - error http response - [" + cbsResponse + "]");
                }
                switch (cbsResponse.getResult()) {
                    case 0:
                    case 26:
                        LOGGER.info("TAX POSTED SUCCESSFULLY TO GL:  {} : trans Reference {}", txn.get(0).get("reference"), txn.get(0).get("tax_ledger").toString());
                        jdbcTemplate.update("update transfers set message='With-holding Tax posted successfully',comments='Success' where  reference=?", req.getReferences());
                        break;
                    case 53:
                    case 96:
                        LOGGER.info("TAX POSTED SUCCESSFULLY TO GL:  {} : trans Reference {}", txn.get(0).get("reference"), txn.get(0).get("tax_ledger").toString());
                        jdbcTemplate.update("update transfers set message='With-holding Tax posted Failed',comments=? where  reference=?", cbsResponse.getMessage(), req.getReferences());
                        break;
                    default:
                        LOGGER.info("TAX NOT POSTED  TO:  {} : trans Reference: {} AMOUNT: {}", txn.get(0).get("tax_ledger").toString(), txn.get(0).get("reference"), txn.get(0).get("tax_ledger").toString());
                        throw new RuntimeException("[" + QUEUE_RTGS_WITH_HOLDING_TAX + "] - error http response - [" + cbsResponse + "]");
                }
            } else {
                LOGGER.info("ITS ZERO TAX!!!!!!!!!! REFERENCE: {}", txn.get(0).get("reference"));

            }
        } else {
            throw new RuntimeException("[" + QUEUE_RTGS_WITH_HOLDING_TAX + "] - error http response");
        }
    }

    @JmsListener(destination = QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER, containerFactory = "queueListenerFactory")
    public void eftFrmCustomerAcctToBranchEFTLedger(@Payload RemittanceToQueue req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException, ParseException {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        //get the MESSAGE DETAILS FROM THE QUEUE
        List<Map<String, Object>> txn = getSwiftMessage(req.getReferences());
        //generate the request to RUBIKON
        if (txn != null) {
            if (!txn.get(0).get("beneficiaryBIC").equals(systemVariables.SENDER_BIC)) {
                TaTransfer transferReq = new TaTransfer();
                transferReq.setReference((String) txn.get(0).get("reference"));
                transferReq.setTxnRef((String) txn.get(0).get("reference"));
                try {
                    transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(txn.get(0).get("create_dt").toString(), "yyyy-MM-dd HH:mm:ss"));
                } catch (DatatypeConfigurationException ex) {
                    java.util.logging.Logger.getLogger(QueueConsumer.class.getName()).log(Level.SEVERE, null, ex);
                }
                transferReq.setEmployeeId(0L);
                transferReq.setSupervisorId(0L);
                transferReq.setTransferType("EFT");
                transferReq.setCurrency((String) txn.get(0).get("currency"));
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setExchangeRate(new BigDecimal("0.0"));
                transferReq.setReceiverBank((String) txn.get(0).get("beneficiaryBIC"));
                transferReq.setReceiverAccount((String) txn.get(0).get("destinationAcct"));
                transferReq.setReceiverName((String) txn.get(0).get("beneficiaryName"));
                transferReq.setSenderBank(systemVariables.SENDER_BIC);
                transferReq.setSenderAccount((String) txn.get(0).get("sourceAcct"));
                transferReq.setSenderName((String) txn.get(0).get("sender_name"));
                transferReq.setDescription((String) txn.get(0).get("purpose") + " B/O " + (String) txn.get(0).get("beneficiaryName"));
                transferReq.setTxnId(Long.parseLong(txn.get(0).get("id").toString()));
                transferReq.setContraAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                transferReq.setReversal(Boolean.FALSE);
                if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains((String) txn.get(0).get("sourceAcct"))) {//waived accounts
                    transferReq.setScheme("T99");
                } else {
                    transferReq.setScheme("T01");
                }
                // prepare a role for posting online transfer
                // achRole.setBranchCode(paymentReq.getCustomerBranch());
                transferReq.setUserRole(req.getUsAchRole());
                ProcessOutwardRtgsTransfer postOutwardReq = new ProcessOutwardRtgsTransfer();
                postOutwardReq.setTransfer(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, "ach:processOutwardEftTransfer"), TaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", req.getReferences());
                    //do not update the transaction status
                }
                if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                    LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                    jdbcTemplate.update("update transfer_eft_batches set status='P',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());
                }
                if (cbsResponse.getResult() == 51) {
                    LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                    jdbcTemplate.update("update transfer_eft_batches set status='P',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());
                }
                if (cbsResponse.getResult() == 96) {
                    LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set cbs_status='F',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                    jdbcTemplate.update("update transfer_eft_batches set status='F',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());
                }
                if (cbsResponse.getResult() == 14) {
                    LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set cbs_status='F',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                    jdbcTemplate.update("update transfer_eft_batches set status='F',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());
                }
                if (cbsResponse.getResult() == 13) {
                    LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set cbs_status='F',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                    jdbcTemplate.update("update transfer_eft_batches set status='F',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());
                }
                if (cbsResponse.getResult() == 53) {
                    LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set cbs_status='F',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                    jdbcTemplate.update("update transfer_eft_batches set status='F',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());
                } else {
                    LOGGER.info("FAILED TO DEBIT CUSTOMER :  CUST ACCT: {} : Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    throw new RuntimeException("[" + QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER + "] - error http response - [" + cbsResponse + "]");
                }
            } else {
                //POST book transfer
                jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", "internal transfer : -1not posted at this stage", req.getUsAchRole().getUserName(), DateUtil.now(), req.getReferences());
                jdbcTemplate.update("update transfer_eft_batches set status='P',approved_by=?,approved_dt=?  where  batch_reference=?", req.getUsAchRole().getUserName(), DateUtil.now(), txn.get(0).get("batch_reference").toString());

            }
        } else {
            throw new RuntimeException("[" + QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER + "] - error http response");
        }
    }

    @JmsListener(destination = QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ, containerFactory = "queueListenerFactory")
    public void eftFrmBranchLedgerToHqLedger(@Payload RemittanceToQueue req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        //get the MESSAGE DETAILS FROM THE QUEUE
        LOGGER.info("QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ receiver trans ref:-> {}", req.getReferences());
        List<Map<String, Object>> txn = getSwiftMessage(req.getReferences());
        String status = "F";

        //generate the request to RUBIKON
        if (!txn.isEmpty()) {
            LOGGER.info("QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ yes trans ref:-> {} has been found in database", req.getReferences());

            //if (!txn.get(0).get("beneficiaryBIC").toString().equalsIgnoreCase("TAPBTZTZ")) {
            String identifier = "api:postGLToGLTransfer";
            TxRequest transferReq = new TxRequest();
            transferReq.setReference(txn.get(0).get("reference") + "  ");
            transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
            transferReq.setNarration(txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + " " + txn.get(0).get("reference"));
            transferReq.setCurrency((String) txn.get(0).get("currency"));
            if (txn.get(0).get("txn_type").equals("005") && !txn.get(0).get("beneficiaryBIC").toString().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
                transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                transferReq.setCreditAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                status = "C";
            } else if (txn.get(0).get("txn_type").equals("005") && txn.get(0).get("beneficiaryBIC").toString().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
//                transferReq.setDebitAccount(txn.get(0).get("sourceAcct") + "");
                identifier = "api:postGLToDepositTransfer";
                transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                transferReq.setCreditAccount(txn.get(0).get("destinationAcct") + "");
                status = "S";
            } else {
                transferReq.setDebitAccount(systemVariables.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                transferReq.setCreditAccount("000");
            }
            transferReq.setUserRole(req.getUsRole());
            PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
            postOutwardReq.setRequest(transferReq);
            String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
            //process the Request to CBS

            XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, identifier), XaResponse.class);
            if (cbsResponse == null) {
                LOGGER.info("POSTING TAX: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                //do not update the transaction status
                LOGGER.info("[{}] - error http response - {} ", QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ, cbsResponse);
                throw new RuntimeException("[" + QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ + "] - error http response - [" + cbsResponse + "]");
            }
            if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                LOGGER.info("TXN POSTED SUCCESSFULLY FROM : BRANCH LEDGER ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", systemVariables.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                jdbcTemplate.update("update transfers set status=?,cbs_status='C',comments='posted from EFT waiting GL to Cust acct/BOT GL Success[" + cbsResponse.getResult() + "]',hq_approved_by=?,hq_approved_dt=? where  batch_reference=? and reference=?", status, req.getUsRole().getUserName(), DateUtil.now(), (String) txn.get(0).get("batch_reference"), req.getReferences());
                jdbcTemplate.update("update transfer_eft_batches set status='C',authorized_by=?,authorized_dt=?  where  batch_reference=?", req.getUsRole().getUserName(), DateUtil.now(), (String) txn.get(0).get("batch_reference"));

            } else {
                LOGGER.info("FAILED TO DEBIT CUSTOMER :  BRANCH LEDGER ACCT: {} : Reference: {} AMOUNT: {} BatchReference: {}", systemVariables.TRANSFER_AWAITING_EFT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                throw new RuntimeException("[" + QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER + "] - error http response - [" + cbsResponse + "]");
            }
            //}
        } else {
            throw new RuntimeException("[" + QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER + "] - error transaction not found in transfers table" + req.getReferences());
        }
    }

    //TODO: Process incoming EFT transaction
    @JmsListener(destination = FROM_TACH_TO_CBS_QUEUE, containerFactory = "queueListenerEFTFactory")
    public void parseTACHMsgToCBS(@Payload String req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException, Exception {
        eftRepo.parseIso20022Message(req);
    }

    @JmsListener(destination = QUEUE_EFT_INCOMING_TO_CBS, containerFactory = "queueListenerFactory")
    public void processEFTIncomingToCBS(@Payload EftPacs00800102Req req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        //get the MESSAGE DETAILS FROM THE QUEUE
        TaTransfer transferReq = new TaTransfer();
        //CHECK ACCOUNT NAME IF IT MATCHES
        //NAME QUERY
//        String accountNameQuery;
//        accountNameQuery = corebanking.accountNameQuery(req.getCdtTrfTxInf().getBeneficiaryAcct()).toUpperCase();//get account Name
        double distance = 53L;
        String message1 = "No saving account";
        String responseCode = "53";
        String cbsName = "--NiLL--";
        boolean isTANESCO = false;
        String sql = "SELECT c.CUST_NM, AC.ACCT_NO,AC.ACCT_NM, CUR.CRNCY_CD_ISO AS CURRENCY_CODE, AC.REC_ST,ASR.REF_DESC FROM  ACCOUNT AC JOIN CURRENCY CUR ON AC.CRNCY_ID = CUR.CRNCY_ID JOIN ACCOUNT_STATUS_REF ASR ON  AC.REC_ST = ASR.REF_KEY JOIN CUSTOMER c ON c.CUST_ID=AC.CUST_ID WHERE (REPLACE(AC.OLD_ACCT_NO, '-', '') = ?   OR AC.ACCT_NO = ?)   AND ROWNUM = 1";
        List<Map<String, Object>> getData = this.jdbcRUBIKONTemplate.queryForList(sql, formatAccountNo(req.getCdtTrfTxInf().getBeneficiaryAcct().replace("-", "")), formatAccountNo(req.getCdtTrfTxInf().getBeneficiaryAcct().replace("-", "")));
        if (!getData.isEmpty()) {
            LOGGER.info("Beneficiary name check ... {} cbs name .. {}  for ref... {}", req.getCdtTrfTxInf().getBeneficiaryName().toUpperCase().trim(), String.valueOf(getData.get(0).get("ACCT_NM")).toUpperCase().trim(), req.getCdtTrfTxInf().getEndToEndId());
            String incomingName = req.getCdtTrfTxInf().getBeneficiaryName().toLowerCase().trim();
            String databaseName = String.valueOf(getData.get(0).get("ACCT_NM"));
            distance = namesMatch(incomingName, databaseName) * 100;


            cbsName = String.valueOf(getData.get(0).get("CUST_NM")).toUpperCase().trim();

            message1 = "Account exist:" + getData.get(0).get("ACCT_NO") + " ACCOUNT NAME:" + getData.get(0).get("ACCT_NM");
            if (String.valueOf(getData.get(0).get("REC_ST")).equalsIgnoreCase("A") || String.valueOf(getData.get(0).get("REC_ST")).equalsIgnoreCase("D")) {
                message1 = "success";
                responseCode = "0";
            } else {
                responseCode = "999";
                message1 = String.valueOf(getData.get(0).get("REF_DESC"));
            }
        } else {
            //todo check tanesco saccoss accounts
            sql = "select * from saccoss_customers_mapping where accountNo=?";
            List<Map<String, Object>> tanescoVerify = this.jdbcTemplate.queryForList(sql, req.getCdtTrfTxInf().getBeneficiaryAcct());
            if (!tanescoVerify.isEmpty()) {
                distance = namesMatch(req.getCdtTrfTxInf().getBeneficiaryName(), String.valueOf(tanescoVerify.get(0).get("accountName")).toLowerCase().trim()) * 100;
                cbsName = String.valueOf(tanescoVerify.get(0).get("accountName")).toUpperCase().trim();
                LOGGER.info("\n*******************************ITS A TANESCO SACCOSS BENEFICIARY *************************** \nACCOUNT NO:{}\nTRANSFER NAME:{} \nVALIDATED NAME:{},\nreference:{}\n*******************************ITS A TANESCO SACCOSS BENEFICIARY ***************************", req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getBeneficiaryName(), tanescoVerify.get(0).get("accountName"), req.getCdtTrfTxInf().getEndToEndId());
                isTANESCO = true;
                message1 = "success";
                responseCode = "0";
            }

        }

        if (formatAccountNo(req.getCdtTrfTxInf().getBeneficiaryAcct()).equals("173086000001")) {
            message1 = "its Stawi Bond Collection Account ";
            responseCode = "312";
            LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE Stawi Bond Collection Account : RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
            jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());

        } else {
            if (formatAccountNo(req.getCdtTrfTxInf().getBeneficiaryAcct()).startsWith("999") || formatAccountNo(req.getCdtTrfTxInf().getBeneficiaryAcct()).length() == 6) {
                message1 = "its Meant For Cash Collection (CMS) account. Please post transaction using control number";
                responseCode = "999";
                LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A GEPG ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
                jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());

            } else if (distance < 78) {
                if (isTANESCO) {
                    //transaction meant for TANESCO SACCOSS SET CODE TO TANESCO_SCCOSS AND STATUS AQ WITH RESPONSE CODE 222
                    jdbcTemplate.update("update transfers set status='S',code='TANESCO_SACCOSS',cbs_status='AQ',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getTxId(), req.getMsgId());
                } else {
                    //todo process
                    responseCode = "777";
                    //validate tanesco saccos
                    message1 = "Account name doesn't match by: " + String.format("%.2f", distance) + "% rubikon name...: " + cbsName.toUpperCase() + " : Beneficiary name Meant:" + req.getCdtTrfTxInf().getBeneficiaryName().toUpperCase();
                    LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE BENEFICIARY NAME DOESNT MATCH CORE BANKING NAME: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
                    jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
                }
            } else if (isGePGaccount(req.getCdtTrfTxInf().getBeneficiaryAcct(), req.getCdtTrfTxInf().getCurrency()) == 0) {
                message1 = "its GePG account. Please post transaction using control number";
                responseCode = "2";
                LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A GEPG ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
                jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());

            } else if (isCMSAccount(req.getCdtTrfTxInf().getBeneficiaryAcct()) == 0) {
                message1 = "its CMS account. Please post transaction using control number";
                responseCode = "2";
                LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A CMS ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
                jdbcTemplate.update("update transfers set status='S',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());

            } else {
                //process single transactions to core banking ....
            /*
            new approach update cbs status as AQ awaiting processing
            when processing change status to Q meaning its placed on queue
             */
                message1 = "Transaction validated and queued ready for processing";
                responseCode = "766";
                LOGGER.info("TRANSACTION QUEUED FOR PROCESSING: RESULT:{} , RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
                if (isTANESCO) {
                    //transaction meant for TANESCO SACCOSS SET CODE TO TANESCO_SCCOSS AND STATUS AQ WITH RESPONSE CODE 222
                    jdbcTemplate.update("update transfers set status='S',code='TANESCO_SACCOSS',cbs_status='AQ',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getTxId(), req.getMsgId());
                } else {
                    jdbcTemplate.update("update transfers set status='S',cbs_status='AQ',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());

                }
//            transferReq.setTxnRef(req.getCdtTrfTxInf().getEndToEndId());
//            transferReq.setCreateDate(req.getIntrBkSttlmDt());
//            transferReq.setEmployeeId(0L);
//            transferReq.setSupervisorId(0L);
//            transferReq.setTransferType("EFT");
//            transferReq.setCurrency(req.getCdtTrfTxInf().getCurrency());
//            transferReq.setAmount(req.getCdtTrfTxInf().getAmount());
//            transferReq.setReceiverBank(req.getCdtTrfTxInf().getBeneficiaryBIC());
//            transferReq.setReceiverAccount(formatAccountNo(req.getCdtTrfTxInf().getBeneficiaryAcct()));
//            transferReq.setReceiverName(req.getCdtTrfTxInf().getBeneficiaryName());
//            transferReq.setSenderBank(req.getCdtTrfTxInf().getSenderBIC());
//            transferReq.setSenderAccount(req.getCdtTrfTxInf().getSenderAccount());
//            transferReq.setSenderName(req.getCdtTrfTxInf().getSenderName());
//            transferReq.setDescription(req.getCdtTrfTxInf().getPurpose() + " B/O " + req.getCdtTrfTxInf().getSenderName());
//            transferReq.setScheme("");
//            transferReq.setTxnId(Long.parseLong(DateUtil.nowLong()));
//            transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);
//            PostInwardTransfer postInwardEft = new PostInwardTransfer();
//            postInwardEft.setTransfer(transferReq);
//            String inwardEFTXml = XMLParserService.jaxbGenericObjToXML(postInwardEft, Boolean.FALSE, Boolean.TRUE);
//            //process the Request to CBS
//            TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTTESTToCore(inwardEFTXml, "ach:postInwardTransfer"), TaResponse.class);
//            if (cbsResponse == null) {
//                LOGGER.info("POSTING TXN: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
//                //do not update the transaction status
//                LOGGER.info("[{}] - error http response - {} ", QUEUE_EFT_INCOMING_TO_CBS, cbsResponse);
//                throw new RuntimeException("[" + QUEUE_EFT_INCOMING_TO_CBS + "] - error http response - [" + cbsResponse + "]");
//            }
//            if (cbsResponse.getResult() == 0) {
//                LOGGER.info("TXN POSTED SUCCESSFULLY FROM : BOT LEDGER ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER, req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount(), req.getMsgId());
//                jdbcTemplate.update("update transfers set status='S',cbs_status='C',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//            } else if (cbsResponse.getResult() == 26) {
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//                LOGGER.info("TXN POSTED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", req.getCdtTrfTxInf().getSenderAccount(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount(), req.getMsgId());
//            } else if (cbsResponse.getResult() == 96) {
//                LOGGER.info("TRANSACTION POSTING FAILED: CBS RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", cbsResponse.getResult(), cbsResponse.getMessage(), req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//            } else if (cbsResponse.getResult() == 53) {
//                LOGGER.info("TRANSACTION POSTING FAILED: CBS RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", cbsResponse.getResult(), cbsResponse.getMessage(), req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//            } else if (cbsResponse.getResult() == 13) {
//                LOGGER.info("TRANSACTION POSTING FAILED: CBS RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", cbsResponse.getResult(), cbsResponse.getMessage(), req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//            } else if (cbsResponse.getResult() == 14) {
//                LOGGER.info("TRANSACTION POSTING FAILED: CBS RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", cbsResponse.getResult(), cbsResponse.getMessage(), req.getMsgId(), req.getCdtTrfTxInf().getEndToEndId(), req.getCdtTrfTxInf().getAmount());
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//            } else {
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//                jdbcTemplate.update("update transfers set status='S',cbs_status='F',comments=?,branch_approved_by='SYSTEM',message=?,response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  txid=? and batch_reference=?", cbsResponse.getMessage(), cbsResponse.getMessage(), cbsResponse.getResult(), DateUtil.now(), DateUtil.now(), req.getCdtTrfTxInf().getEndToEndId(), req.getMsgId());
//            }
            }
        }
    }

    @JmsListener(destination = QUEUE_EFT_REPLAY_TO_CBS, containerFactory = "queueListenerFactory")
    public void processEFTReplayToCBS(@Payload ReplayIncomingTransactionReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        List<Map<String, Object>> txn = getTransactionsToBeReplayed(req.getReference());
        String message1;
        String responseCode;
        String identifier = "api:postGLToGLTransfer";
        if (txn != null) {
            if ((req.getIdentifier().equalsIgnoreCase("GL2ACCT") || req.getIdentifier().equalsIgnoreCase("replay")) && isGePGaccount(txn.get(0).get("destinationAcct") + "", txn.get(0).get("currency") + "") == 0) {
                message1 = "its GePG account. Please post transaction using control number";
                responseCode = "2";
                LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A GEPG ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get(0).get("txid"), txn.get(0).get("batch_reference"), txn.get(0).get("amount"));
                jdbcTemplate.update("update transfers set cbs_status='F',comments=?,response_code=? where reference=?", message1, responseCode, txn.get(0).get("txid"));
            } else if (req.getIdentifier().equalsIgnoreCase("GL2ACCT") && isCMSAccount(txn.get(0).get("destinationAcct") + "") == 0) {
                message1 = "its CMS account. Please post transaction using control number";
                responseCode = "2";
                LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A CMS ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get(0).get("txid"), txn.get(0).get("batch_reference"), txn.get(0).get("amount"));
                jdbcTemplate.update("update transfers set cbs_status='F',comments=?,response_code=? where reference=?", message1, responseCode, txn.get(0).get("txid"));

            } else if (req.getIdentifier().equalsIgnoreCase("replay") && isCMSAccount(txn.get(0).get("destinationAcct") + "") == 0) {
                message1 = "its CMS account. Please post transaction using control number";
                responseCode = "2";
                LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE ITS A CMS ACCOUNT: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, txn.get(0).get("txid"), txn.get(0).get("batch_reference"), txn.get(0).get("amount"));
                jdbcTemplate.update("update transfers set cbs_status='F',comments=?,response_code=? where reference=?", message1, responseCode, txn.get(0).get("txid"));

            } else {
                TxRequest transferReq = new TxRequest();
                if (req.getIdentifier().equalsIgnoreCase("GL2ACCT")) {
                    identifier = "api:postGLToDepositTransfer";
                }
                if (req.getIdentifier().equalsIgnoreCase("replay")) {
                    identifier = "api:postGLToDepositTransfer";
                    req.setDestinationAcct((String) txn.get(0).get("destinationAcct"));
                }
                transferReq.setReference((String) txn.get(0).get("reference"));
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration(txn.get(0).get("destinationAcct") + " " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("sender_name") + " " + txn.get(0).get("reference") + " FROM: " + txn.get(0).get("sender_name"));
                transferReq.setCurrency((String) txn.get(0).get("currency"));
                transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                if (txn.get(0).get("code").equals("HPENSION")) {
                    transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);
                }
                transferReq.setCreditAccount(formatAccountNo(req.getDestinationAcct()));
                transferReq.setUserRole(req.getUsRole());
                PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                LOGGER.info("Tracing EFT transaction to be replayed with reference: ... {} and destination Account... {}", postOutwardReq.getRequest().getReference(), postOutwardReq.getRequest().getCreditAccount());
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                String sql = "SELECT * FROM DEPOSIT_ACCOUNT_HISTORY WHERE ACCT_NO=? AND DR_CR_IND ='CR' AND (TRAN_REF_TXT=? OR TRAN_DESC LIKE '%" + req.getReference() + "%')";
                LOGGER.info(sql.replace("?", "'{}'"), req.getDestinationAcct(), req.getReference());
                List<Map<String, Object>> checkiInAcct = jdbcRUBIKONTemplate.queryForList(sql, req.getDestinationAcct(), req.getReference());
                LOGGER.info("Final response on checking if transaction already in customer account for EFT replay... {}", checkiInAcct);
                if (checkiInAcct.isEmpty()) {
                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);

                    if (cbsResponse == null) {
                        LOGGER.info("POSTING BATCH TRANSACTION FROM CUSTOMER ACCT TO BRANCH LEDGER: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                        //do not update the transaction status
                        LOGGER.info("[{}] - error http response - {} ", QUEUE_EFT_REPLAY_TO_CBS, cbsResponse);
                        throw new RuntimeException("[" + QUEUE_EFT_REPLAY_TO_CBS + "] - error http response - [" + cbsResponse + "]");
                    }
                    if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                        LOGGER.info("TXN REPLAYED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                        jdbcTemplate.update("update transfers set cbs_status='C',comments='Replayed to :" + req.getDestinationAcct() + ", but intended to destination : " + txn.get(0).get("destinationAcct").toString() + "',hq_approved_by=?,hq_approved_dt=?  where  reference=?", req.getUsRole().getUserName(), DateUtil.now(), req.getReference());

                    } else {
                        jdbcTemplate.update("update transfers set cbs_status='F',comments='Replayed to :" + req.getDestinationAcct() + ", but intended to destination : " + txn.get(0).get("destinationAcct").toString() + "',hq_approved_by=?,hq_approved_dt=?  where  reference=?", req.getUsRole().getUserName(), DateUtil.now(), req.getReference());
                        LOGGER.info("FAILED TO DEBIT CUSTOMER :  CUST ACCT: {} : Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
//                throw new RuntimeException("[" + QUEUE_EFT_REPLAY_TO_CBS + "] - error http response - [" + cbsResponse + "]");
                    }
                } else {
                    LOGGER.info("TXN CAN NOT BE REPLAYED, IT ALREADY CREDITED TO CUSTOMER ACCOUNT ...{}  With Reference... {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                    jdbcTemplate.update("update transfers set cbs_status='C',comments='Attempt  to replay to.. :" + req.getDestinationAcct() + ", but intended to destination : " + txn.get(0).get("destinationAcct").toString() + "',hq_approved_by=?,hq_approved_dt=?  where  reference=?", req.getUsRole().getUserName(), DateUtil.now(), req.getReference());

                }
            }
        } else {
            LOGGER.info("EFT Transaction cannot be reprocessed as it is not found or already posted with ref: {}", req.getReference());
        }
    }

    @JmsListener(destination = QUEUE_RTGS_REPLAY_TO_CBS, containerFactory = "queueListenerFactory")
    public void processRTGSReplayToCBS(@Payload ReplayIncomingTransactionReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        LOGGER.info("REFERENCE SUBMITTED FOR RETRY:{}", req.getReference());
        List<Map<String, Object>> txn = getTransactionsToBeReplayed(req.getReference());
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        if (txn != null) {
            if (req.getDesicionCode().equalsIgnoreCase("replay")) {
            }
            if (req.getIdentifier().equalsIgnoreCase("GL2ACCT")) {
                identifier = "api:postGLToDepositTransfer";
            }
            if (req.getIdentifier().equalsIgnoreCase("replay")) {
                identifier = "api:postGLToDepositTransfer";
                transferReq.setCreditAccount(formatAccountNo((String) txn.get(0).get("destinationAcct")));
            }
            transferReq.setReference((String) txn.get(0).get("txid"));
            transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
            transferReq.setNarration(txn.get(0).get("destinationAcct") + " " + txn.get(0).get("purpose") + " B/O "
                    + txn.get(0).get("beneficiaryName") + " " + txn.get(0).get("reference") + " FROM: " + txn.get(0).get("sender_name"));
            transferReq.setCurrency((String) txn.get(0).get("currency"));
            transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
            if (txn.get(0).get("code").equals("HPENSION")) {
                transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_EFT_BOT_LEDGER);
            }
            transferReq.setCreditAccount(formatAccountNo(req.getDestinationAcct()));
            transferReq.setUserRole(req.getUsRole());
            PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
            postOutwardReq.setRequest(transferReq);
            String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
            //process the Request to CBS

            XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
            if (cbsResponse == null) {
                LOGGER.info("POSTING BATCH TRANSACTION FROM CUSTOMER ACCT TO BRANCH LEDGER: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", txn.get(0).get("reference"));
                //do not update the transaction status
                LOGGER.info("[{}] - error http response - {} ", QUEUE_RTGS_REPLAY_TO_CBS, null);
                throw new RuntimeException("[" + QUEUE_RTGS_REPLAY_TO_CBS + "] - error http response - [" + null + "]");
            }
            if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                LOGGER.info("TXN REPLAYED SUCCESSFULLY FROM : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
                jdbcTemplate.update("update transfers set cbs_status='C',comments='Replayed to :" + req.getDestinationAcct() + ", but intended to destination : " + txn.get(0).get("destinationAcct").toString() + "',hq_approved_by=?,hq_approved_dt=?  where  reference=?", req.getUsRole().getUserName(), DateUtil.now(), req.getReference());

            } else {
                jdbcTemplate.update("update transfers set cbs_status='F',comments='Replayed to :" + req.getDestinationAcct() + ", but intended to destination : " + txn.get(0).get("destinationAcct").toString() + "',hq_approved_by=?,hq_approved_dt=?  where  reference=?", req.getUsRole().getUserName(), DateUtil.now(), req.getReference());
                LOGGER.info("FAILED TO DEBIT CUSTOMER :  CUST ACCT: {} : Reference: {} AMOUNT: {} BatchReference: {}", txn.get(0).get("sourceAcct"), txn.get(0).get("reference"), txn.get(0).get("amount").toString(), txn.get(0).get("batch_reference").toString());
//                throw new RuntimeException("[" + QUEUE_EFT_REPLAY_TO_CBS + "] - error http response - [" + cbsResponse + "]");
            }
        } else {
            LOGGER.info("RTGS Transaction cannot be reprocessed as it is not found or already posted with ref: {}", req.getReference());
        }
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

    @JmsListener(destination = PROCESS_BATCH_PAYMENTS_FROM_IBANK, containerFactory = "queueListenerFactory")
    public void processIbankBatchTransactions(@Payload BatchPaymentReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postDepositToGLTransfer";
        TxRequest transferReq = new TxRequest();
        //        identifier = "api:postGLToDepositTransfer";
        transferReq.setReference(req.getBatchReference());
        transferReq.setAmount(req.getTotalAmt());
        transferReq.setNarration(req.getPurpose() + " No of Txns: " + req.getNoOfTxns());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(req.getSourceAccount());
        if (req.getPaymentRequest().get(0).getSpRate() != null) {
            transferReq.setDebitFxRate(new BigDecimal(req.getPaymentRequest().get(0).getSpRate()));
            transferReq.setCreditFxRate(new BigDecimal(req.getPaymentRequest().get(0).getSpRate()));
        } else {
            transferReq.setDebitFxRate(BigDecimal.ZERO);
            transferReq.setCreditFxRate(BigDecimal.ZERO);
        }

        transferReq.setCreditAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING BATCH TRANSACTION FROM CUSTOMER ACCT TO BRANCH LEDGER: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BATCH_PAYMENTS_FROM_IBANK, null);
            throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_FROM_IBANK + "] - error http response - [" + null + "]");
        }

        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("INSERT INTO transfer_eft_batches(txn_type, sourceAccount, batch_reference, total_amount, number_of_txns, branch_no, narration, debit_mandate, status, direction, initiated_by, approved_by, authorized_by, create_dt, approved_dt, authorized_dt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update status='P'", "005", req.getSourceAccount(), req.getBatchReference(), req.getTotalAmt(), req.getNoOfTxns(), req.getBranchCode(), req.getPurpose(), req.getMandate(), "C", "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now());
            if (req.getBatchType().equalsIgnoreCase("111")) {
                queProducer.sendToQueueSingleTxnFrmBranchToHQ(req);//INTERNAL TRANSFER
            } else {
                //check if account is waived from charges
                if (!systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getSourceAccount())) {
                    queProducer.sendToQueueProcessBatchTxnCharge(req);
                } else {
                    //process the waived transactions
                    queProducer.sendToQueueSingleTxnFrmBranchToHQ(req);
                }

            }
        } else if (cbsResponse.getResult() == 51) {
            jdbcTemplate.update("INSERT INTO transfer_eft_batches(txn_type, sourceAccount, batch_reference, total_amount, number_of_txns, branch_no, narration, debit_mandate, status, direction, initiated_by, approved_by, authorized_by, create_dt, approved_dt, authorized_dt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update status='P'", "005", req.getSourceAccount(), req.getBatchReference(), req.getTotalAmt(), req.getNoOfTxns(), req.getBranchCode(), req.getPurpose(), req.getMandate(), "F", "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now());
            LOGGER.info("Account has insufficient fund:{} to process a transaction with Ref:{} ", req.getSourceAccount(), req.getBatchReference());
        } else if (cbsResponse.getResult() == 53) {
            jdbcTemplate.update("INSERT INTO transfer_eft_batches(txn_type, sourceAccount, batch_reference, total_amount, number_of_txns, branch_no, narration, debit_mandate, status, direction, initiated_by, approved_by, authorized_by, create_dt, approved_dt, authorized_dt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update status='P'", "005", req.getSourceAccount(), req.getBatchReference(), req.getTotalAmt(), req.getNoOfTxns(), req.getBranchCode(), req.getPurpose(), req.getMandate(), "F", "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now());

            LOGGER.info("Account is invalid:{} cannot process a transfer with ref:{} ", req.getSourceAccount(), req.getBatchReference());
        } else if (cbsResponse.getResult() == 13 || cbsResponse.getResult() == 14) {
            jdbcTemplate.update("INSERT INTO transfer_eft_batches(txn_type, sourceAccount, batch_reference, total_amount, number_of_txns, branch_no, narration, debit_mandate, status, direction, initiated_by, approved_by, authorized_by, create_dt, approved_dt, authorized_dt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update status='P'", "005", req.getSourceAccount(), req.getBatchReference(), req.getTotalAmt(), req.getNoOfTxns(), req.getBranchCode(), req.getPurpose(), req.getMandate(), "F", "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now());

            LOGGER.info("amount is invalid:{} cannot process a transfer with ref:{} ", req.getSourceAccount(), req.getBatchReference());
        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_FROM_IBANK + "] - error http response - [" + cbsResponse + "]");
        }

    }

    @JmsListener(destination = PROCESS_BATCH_PAYMENTS_FROM_BRANCH, containerFactory = "queueListenerFactory")
    public void processBatchTransactionFromBranch(@Payload BatchPaymentReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
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
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING BATCH TRANSACTION FROM CUSTOMER ACCT TO BRANCH LEDGER: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BATCH_PAYMENTS_FROM_BRANCH, null);
            throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_FROM_BRANCH + "] - error http response - [" + null + "]");
        }

        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("INSERT INTO transfer_eft_batches(txn_type, sourceAccount, batch_reference, total_amount, number_of_txns, branch_no, narration, debit_mandate, status, direction, initiated_by, approved_by, authorized_by, create_dt, approved_dt, authorized_dt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update status='P'", "005", req.getSourceAccount(), req.getBatchReference(), req.getTotalAmt(), req.getNoOfTxns(), req.getBranchCode(), req.getPurpose(), req.getMandate(), "P", "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now());
            if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getSourceAccount())) {//waived accounts
                jdbcTemplate.update("UPDATE transfer_eft_batches set charge=?,status='P'  where batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), req.getBatchReference());
                jdbcTemplate.update("UPDATE  transfers SET status='P', cbs_status = 'C' where  batch_reference=?", req.getBatchReference());
            } else {
                jdbcTemplate.update("UPDATE  transfers SET status='P', cbs_status = 'C' where  batch_reference=?", req.getBatchReference());
                queProducer.sendToQueueProcessBatchTxnChargeFromBranch(req);
            }

        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_FROM_BRANCH + "] - error http response - [" + cbsResponse + "]");
        }

    }

    @JmsListener(destination = PROCESS_BATCH_TRANSACTION_CHARGE, containerFactory = "queueListenerFactory")
    public void processIbankBatchTransactionsCharge(@Payload BatchPaymentReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postDepositToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getBatchReference() + " ");
        BigDecimal chargeAmount = systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns()));
        transferReq.setAmount(chargeAmount);
        transferReq.setNarration("Transfer Charges. No of Txns: " + req.getNoOfTxns());
        if (req.getBatchType().equalsIgnoreCase("005")) {
            transferReq.setNarration("EFT Batch transfer Charge. No of Txns: " + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("TIS Batch transfer Charge. No of Txns:" + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("Transfer to Wallet  batch Charge. No of Txns:" + req.getNoOfTxns());
        }

        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(req.getSourceAccount());
        transferReq.setCreditAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING CHARGE TRANSACTIONS{}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BATCH_TRANSACTION_CHARGE, null);
            throw new RuntimeException("[" + PROCESS_BATCH_TRANSACTION_CHARGE + "] - error http response - [" + null + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("UPDATE transfer_eft_batches set charge=? where batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), req.getBatchReference());
            queProducer.sendToQueueSingleTxnFrmBranchToHQ(req);
            //check if there is a waiver
            if (!systemVariable.WAIVED_ACCOUNTS_LISTS.contains(req.getSourceAccount())) {
                queProducer.sendToQueueChargeSpilitingINCOME(req);
                queProducer.sendToQueueChargeSpilitingExereciseDuty(req);
                queProducer.sendToQueueChargeSpilitingValueAddedTax(req);
            }

        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_BATCH_TRANSACTION_CHARGE + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_BATCH_TRANSACTION_CHARGE_FROM_BRANCH, containerFactory = "queueListenerFactory")
    public void processBatchTransactionsChargeFromBranch(@Payload BatchPaymentReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postDepositToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getBatchReference() + " ");

        BigDecimal chargeAmount = systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns()));
        transferReq.setAmount(chargeAmount);
        transferReq.setNarration("Transfer Charges. No of Txns: " + req.getNoOfTxns());
        if (req.getBatchType().equalsIgnoreCase("005")) {
            transferReq.setNarration("EFT Batch transfer Charge. No of Txns: " + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("TIS Batch transfer Charge. No of Txns:" + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("Transfer to Wallet  batch Charge. No of Txns:" + req.getNoOfTxns());
        }

        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(req.getSourceAccount());
        transferReq.setCreditAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING CHARGE TRANSACTIONS {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BATCH_TRANSACTION_CHARGE, cbsResponse);
            throw new RuntimeException("[" + PROCESS_BATCH_TRANSACTION_CHARGE + "] - error http response - [" + cbsResponse + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            queProducer.sendToQueueChargeSpilitingINCOME(req);
            queProducer.sendToQueueChargeSpilitingExereciseDuty(req);
            queProducer.sendToQueueChargeSpilitingValueAddedTax(req);

            jdbcTemplate.update("UPDATE transfer_eft_batches set charge=?,status='P' where batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), req.getBatchReference());
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("UPDATE  transfers SET status='P' where  batch_reference=?", req.getBatchReference());
        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_BATCH_TRANSACTION_CHARGE + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_EXERCISE_DUTY_CHARGE_SPILITING, containerFactory = "queueListenerFactory")
    public void processChargeSplittingExerciseDuty(@Payload BatchPaymentReq req,
                                                   @Headers MessageHeaders headers, Message message,
                                                   Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getBatchReference() + "  ");
        BigDecimal chargeAmount = systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns()));
        BigDecimal amount = eftRepo.getChargeSpliting(chargeAmount).get("exerciseDuty");
        transferReq.setAmount(amount);
        transferReq.setNarration("Transfer Charges. No of Txns: " + req.getNoOfTxns());
        if (req.getBatchType().equalsIgnoreCase("005")) {
            transferReq.setNarration("Exercise Duty <EFT Batch transfer Charge. No of Txns: " + req.getNoOfTxns() + "> reference:" + req.getBatchReference());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("Exercise Duty <TIS Batch transfer Charge. No of Txns:" + req.getNoOfTxns() + "> reference:" + req.getBatchReference());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("Exercise Duty <Transfer to Wallet batch Charge. No of Txns:" + req.getNoOfTxns() + "> reference:" + req.getBatchReference());
        }
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setCreditAccount(systemVariable.EXERCISE_DUTY_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING CHARGE TRANSACTIONS {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_EXERCISE_DUTY_CHARGE_SPILITING, cbsResponse);
            throw new RuntimeException("[" + PROCESS_EXERCISE_DUTY_CHARGE_SPILITING + "] - error http response - [" + cbsResponse + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("UPDATE transfer_eft_batches set charge=? where batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), req.getBatchReference());
        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_EXERCISE_DUTY_CHARGE_SPILITING + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_VALUE_ADDED_TAX_SPILITING, containerFactory = "queueListenerFactory")
    public void processChargeSplitVAT(@Payload BatchPaymentReq req,
                                      @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getBatchReference() + "   ");
        BigDecimal chargeAmount = systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns()));
        BigDecimal amount = eftRepo.getChargeSpliting(chargeAmount).get("VAT");
        transferReq.setAmount(amount);
        transferReq.setNarration("Transfer Charges. No of Txns: " + req.getNoOfTxns());
        if (req.getBatchType().equalsIgnoreCase("005")) {
            transferReq.setNarration("EFT Batch transfer Charge. No of Txns: " + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("TIS Batch transfer Charge. No of Txns:" + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("Transfer to Wallet  batch Charge. No of Txns:" + req.getNoOfTxns());
        }
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setCreditAccount(systemVariable.VAT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING CHARGE TRANSACTIONS {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_VALUE_ADDED_TAX_SPILITING, null);
            throw new RuntimeException("[" + PROCESS_VALUE_ADDED_TAX_SPILITING + "] - error http response - [" + null + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
//            jdbcTemplate.update("UPDATE transfer_eft_batches set charge=? where batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), req.getBatchReference());

        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_VALUE_ADDED_TAX_SPILITING + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_POSTING_INCOME_TO_CORE_BANKING, containerFactory = "queueListenerFactory")
    public void processChargeSplitINCOME(@Payload BatchPaymentReq req,
                                         @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getBatchReference() + "     ");
        BigDecimal chargeAmount = systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns()));
        BigDecimal amount = eftRepo.getChargeSpliting(chargeAmount).get("income");
        transferReq.setAmount(amount);
        transferReq.setNarration("Transfer Charges. No of Txns: " + req.getNoOfTxns());
        if (req.getBatchType().equalsIgnoreCase("005")) {
            transferReq.setNarration("EFT  Batch transfer Charge. No of Txns: " + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("TIS Batch transfer Charge. No of Txns:" + req.getNoOfTxns());
        }
        if (req.getBatchType().equalsIgnoreCase("001")) {
            transferReq.setNarration("Transfer to Wallet  batch Charge. No of Txns:" + req.getNoOfTxns());
        }
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setCreditAccount(systemVariable.EFT_INCOME_LEDGER.replace("***", req.getBranchCode()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getBranchCode()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING CHARGE TRANSACTIONS{}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_POSTING_INCOME_TO_CORE_BANKING, null);
            throw new RuntimeException("[" + PROCESS_POSTING_INCOME_TO_CORE_BANKING + "] - error http response - [" + null + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
//            jdbcTemplate.update("UPDATE transfer_eft_batches set charge=? where batch_reference=?", systemVariable.EFT_TXN_CHARGE.multiply(new BigDecimal(req.getNoOfTxns())), req.getBatchReference());
        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_POSTING_INCOME_TO_CORE_BANKING + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK, containerFactory = "queueListenerFactory")
    public void processSingleBatchTransactionFrmBatch(@Payload BatchPaymentReq req,
                                                      @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        for (int i = 0; i < req.getPaymentRequest().size(); i++) {
            PaymentReq paymentReq = new PaymentReq();
            paymentReq.setAmount(req.getPaymentRequest().get(i).getAmount());
            paymentReq.setBatchReference(req.getBatchReference());
            paymentReq.setBeneficiaryAccount(req.getPaymentRequest().get(i).getBeneficiaryAccount().trim());
            paymentReq.setBeneficiaryBIC(req.getPaymentRequest().get(i).getBeneficiaryBIC());
            paymentReq.setBeneficiaryContact(req.getPaymentRequest().get(i).getBeneficiaryContact());
            paymentReq.setBeneficiaryName(req.getPaymentRequest().get(i).getBeneficiaryName());
            paymentReq.setBoundary(req.getPaymentRequest().get(i).getBoundary());
            paymentReq.setCallbackUrl(req.getPaymentRequest().get(i).getCallbackUrl());
            paymentReq.setChargeCategory(req.getPaymentRequest().get(i).getChargeCategory());
            paymentReq.setCurrency(req.getPaymentRequest().get(i).getCurrency());
            paymentReq.setCustomerBranch(req.getPaymentRequest().get(i).getCustomerBranch());
            paymentReq.setDescription(req.getPaymentRequest().get(i).getDescription());
            paymentReq.setInitiatorId(req.getPaymentRequest().get(i).getInitiatorId());
            paymentReq.setIntermediaryBank(req.getPaymentRequest().get(i).getIntermediaryBank());
            paymentReq.setReference(req.getPaymentRequest().get(i).getReference());
            paymentReq.setSenderAccount(req.getPaymentRequest().get(i).getSenderAccount());
            paymentReq.setSenderAddress(req.getPaymentRequest().get(i).getSenderAddress());
            paymentReq.setSenderName(req.getPaymentRequest().get(i).getSenderName());
            paymentReq.setSenderPhone(req.getPaymentRequest().get(i).getSenderPhone());
            paymentReq.setSpecialRateToken(req.getPaymentRequest().get(i).getSpecialRateToken());
            paymentReq.setType(req.getPaymentRequest().get(i).getType());
            if (req.getPaymentRequest().get(i).getSpRate() != null) {
                paymentReq.setSpRate(req.getPaymentRequest().get(i).getSpRate());
            } else {
                paymentReq.setSpRate("0");
            }
            if (req.getPaymentRequest().get(i).getType().equalsIgnoreCase("111")) {
                queProducer.sendToQueueProcessBookTransfer(paymentReq);
            } else {
                queProducer.sendToQueueProcessSingleEntriesToCBS(paymentReq);
            }
        }
    }

    @JmsListener(destination = PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS, containerFactory = "queueListenerFactory")
    public void processBatchBatchTransactionMultipleDebit(@Payload BatchPaymentReq req,
                                                          @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        //get the MESSAGE DETAILS FROM THE QUEUE
        for (int i = 0; i < req.getPaymentRequest().size(); i++) {
            PaymentReq paymentReq = new PaymentReq();
            paymentReq.setAmount(req.getPaymentRequest().get(i).getAmount());
            paymentReq.setBatchReference(req.getBatchReference());
            paymentReq.setBeneficiaryAccount(req.getPaymentRequest().get(i).getBeneficiaryAccount().trim());
            paymentReq.setBeneficiaryBIC(req.getPaymentRequest().get(i).getBeneficiaryBIC());
            paymentReq.setBeneficiaryContact(req.getPaymentRequest().get(i).getBeneficiaryContact());
            paymentReq.setBeneficiaryName(req.getPaymentRequest().get(i).getBeneficiaryName());
            paymentReq.setBoundary(req.getPaymentRequest().get(i).getBoundary());
            paymentReq.setCallbackUrl(req.getPaymentRequest().get(i).getCallbackUrl());
            paymentReq.setChargeCategory(req.getPaymentRequest().get(i).getChargeCategory());
            paymentReq.setCurrency(req.getPaymentRequest().get(i).getCurrency());
            paymentReq.setCustomerBranch(req.getPaymentRequest().get(i).getCustomerBranch());
            paymentReq.setDescription(req.getPaymentRequest().get(i).getDescription());
            paymentReq.setInitiatorId(req.getPaymentRequest().get(i).getInitiatorId());
            paymentReq.setIntermediaryBank(req.getPaymentRequest().get(i).getIntermediaryBank());
            paymentReq.setReference(req.getPaymentRequest().get(i).getReference());
            paymentReq.setSenderAccount(req.getPaymentRequest().get(i).getSenderAccount());
            paymentReq.setSenderAddress(req.getPaymentRequest().get(i).getSenderAddress());
            paymentReq.setSenderName(req.getPaymentRequest().get(i).getSenderName());
            paymentReq.setSenderPhone(req.getPaymentRequest().get(i).getSenderPhone());
            paymentReq.setSpecialRateToken(req.getPaymentRequest().get(i).getSpecialRateToken());
            paymentReq.setType(req.getPaymentRequest().get(i).getType());
            if (req.getPaymentRequest().get(i).getSpRate() != null) {
                paymentReq.setSpRate(req.getPaymentRequest().get(i).getSpRate());
            } else {
                paymentReq.setSpRate("0");
            }
            if (req.getPaymentRequest().get(i).getBeneficiaryBIC().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
                //BOOK TRANSFER
                paymentReq.setType("111");
//                queProducer.sendToQueueProcessBookTransfer(paymentReq);
                queProducer.sendToQueueProcessMultipleDebitsToSuspense(paymentReq);
                //process single debits

            } else {
                queProducer.sendToQueueProcessMultipleDebitsPosting(paymentReq);// TRANSFER TO OTHER BANKS
            }
        }
    }

    @JmsListener(destination = PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING, containerFactory = "queueListenerFactory")
    public void processEFTBatchPaymnetsMultipleDebits(@Payload PaymentReq paymentReq,
                                                      @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        try {
            TaTransfer transferReq = new TaTransfer();
            transferReq.setReference(paymentReq.getReference());
            transferReq.setTxnRef(paymentReq.getReference());
            transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(DateUtil.now(), "yyyy-MM-dd HH:mm:ss"));
            transferReq.setEmployeeId(0L);
            transferReq.setSupervisorId(0L);
            transferReq.setTransferType("EFT");
            transferReq.setCurrency(paymentReq.getCurrency());
            transferReq.setAmount(paymentReq.getAmount());
            transferReq.setExchangeRate(new BigDecimal("0.0"));
            if (paymentReq.getSpRate() != null) {
                transferReq.setDebitFxRate(new BigDecimal(paymentReq.getSpRate()));
                transferReq.setCreditFxRate(new BigDecimal(paymentReq.getSpRate()));
            } else {
                transferReq.setDebitFxRate(BigDecimal.ZERO);
                transferReq.setCreditFxRate(BigDecimal.ZERO);
            }

            transferReq.setReceiverBank(paymentReq.getBeneficiaryBIC());
            transferReq.setReceiverAccount(paymentReq.getBeneficiaryAccount());
            transferReq.setReceiverName(paymentReq.getBeneficiaryName());
            transferReq.setSenderBank(systemVariables.SENDER_BIC);
            transferReq.setSenderAccount(paymentReq.getSenderAccount());
            transferReq.setSenderName(paymentReq.getSenderName());
            transferReq.setDescription(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName());
            transferReq.setTxnId(Long.parseLong(DateUtil.now("yyyMMdd")));
            transferReq.setContraAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", paymentReq.getCustomerBranch()));
            transferReq.setReversal(Boolean.FALSE);
            if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(paymentReq.getSenderAccount())) {//waived accounts
                transferReq.setScheme("T99");
            } else {
                transferReq.setScheme("T01");
            }
            // prepare a role for posting online transfer
            // achRole.setBranchCode(paymentReq.getCustomerBranch());
            transferReq.setUserRole(systemVariable.achUserRole(paymentReq.getCustomerBranch()));
            ProcessOutwardRtgsTransfer postOutwardReq = new ProcessOutwardRtgsTransfer();
            postOutwardReq.setTransfer(transferReq);
            String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
            //process the Request to CBS
            TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, "ach:processOutwardEftTransfer"), TaResponse.class);
            if (cbsResponse == null) {
                //do not update the transaction status
                LOGGER.info("[{}] - error http response - {} ", PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING, cbsResponse);
                throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING + "] - error http response - [" + cbsResponse + "]");
            }
            if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                // assign queue to post to BoT
                queProducer.sendToQueueProcessBatchPaymentsToBoT(paymentReq);
                //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                LOGGER.info("FAILED TO PROCESS REVERSAL OF TXNS WITH REFERENCE: ");
                String messageType = "";
                String swiftMessage = "";
                if (paymentReq.getType().equalsIgnoreCase("111")) {
                    messageType = "BT";
                } else if (paymentReq.getType().equalsIgnoreCase("005")) {
                    messageType = "pacs.008.001.03";
                } else if (paymentReq.getType().equalsIgnoreCase("001")) {
                    messageType = "103";
                    paymentReq.setSenderBic(systemVariable.SENDER_BIC);
                    paymentReq.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    if (paymentReq.getCurrency().equalsIgnoreCase("USD")
                            && !paymentReq.getBeneficiaryBIC().contains("TZ")) {
                        paymentReq.setCorrespondentBic(systemVariable.USD_CORRESPONDEND_BANK);
                    } else if (paymentReq.getCurrency().equalsIgnoreCase("EUR")) {
                        paymentReq.setCorrespondentBic(systemVariable.EURO_CORRESPONDEND_BANK);
                    } else {
                        paymentReq.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    }
                    swiftMessage = SwiftService.createMT103FromOnlineReq(paymentReq);
                }

                jdbcTemplate.update("update transfers set  status=?, response_code=?, cbs_status=?, message=? where reference=? and batch_reference=?", "C", cbsResponse.getResult(), "C", "Success", paymentReq.getReference(), paymentReq.getBatchReference());
            } else if (cbsResponse.getResult() == 53 || cbsResponse.getResult() == 14 || cbsResponse.getResult() == 13) {
                //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                LOGGER.info("FAILED TO PROCESS REVERSAL OF TXNS WITH REFERENCE: ");
                String messageType = "";
                String swiftMessage = "";
                if (paymentReq.getType().equalsIgnoreCase("111")) {
                    messageType = "BT";
                } else if (paymentReq.getType().equalsIgnoreCase("005")) {
                    messageType = "pacs.008.001.03";
                } else if (paymentReq.getType().equalsIgnoreCase("001")) {
                    messageType = "103";
                    paymentReq.setSenderBic(systemVariable.SENDER_BIC);
                    paymentReq.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    if (paymentReq.getCurrency().equalsIgnoreCase("USD")
                            && !paymentReq.getBeneficiaryBIC().contains("TZ")) {
                        paymentReq.setCorrespondentBic(systemVariable.USD_CORRESPONDEND_BANK);
                    } else if (paymentReq.getCurrency().equalsIgnoreCase("EUR")) {
                        paymentReq.setCorrespondentBic(systemVariable.EURO_CORRESPONDEND_BANK);
                    } else {
                        paymentReq.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    }
                    swiftMessage = SwiftService.createMT103FromOnlineReq(paymentReq);
                }
//                jdbcTemplate.update("INSERT INTO transfers(swift_message,message_type,txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", swiftMessage, messageType, paymentReq.getType(), paymentReq.getSenderAccount(), paymentReq.getBeneficiaryAccount(), paymentReq.getAmount(), paymentReq.getCurrency(), paymentReq.getBeneficiaryName(), paymentReq.getBeneficiaryBIC(), paymentReq.getBeneficiaryContact(), systemVariable.SENDER_BIC, paymentReq.getSenderPhone(), paymentReq.getSenderAddress(), paymentReq.getSenderName(), paymentReq.getReference(), paymentReq.getReference(), paymentReq.getReference(), paymentReq.getBatchReference(), paymentReq.getBatchReference(), "IB", "F", cbsResponse.getResult(), paymentReq.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), paymentReq.getCustomerBranch(), "F", "Failed", paymentReq.getCallbackUrl());
                jdbcTemplate.update("update transfers set  status=?, response_code=?, cbs_status=?, message=? where reference=? and batch_reference=?", "F", cbsResponse.getResult(), "F", "Failed", paymentReq.getReference(), paymentReq.getBatchReference());

            } else {
                LOGGER.info("FAILED TO MULTIPLE DEBIT ENTRY  OF TXNS WITH REFERENCE: " + paymentReq.getReference());
                throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING + "] - error http response - [" + cbsResponse + "]");
            }
        } catch (DatatypeConfigurationException | ParseException ex) {
            java.util.logging.Logger.getLogger(QueueConsumer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @JmsListener(destination = PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING, containerFactory = "queueListenerFactory")
    public void processSingleBatchTransactionFrmBatchToCBS(@Payload PaymentReq req,
                                                           @Headers MessageHeaders headers, Message message, Session session) throws JMSException, Exception {
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        if (req.getBeneficiaryBIC().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
            //BOOK TRANSFER
            req.setType("006");
            if (req.getSpRate() != null) {
                transferReq.setDebitFxRate(new BigDecimal(req.getSpRate()));
                transferReq.setCreditFxRate(new BigDecimal(req.getSpRate()));
            } else {
                transferReq.setDebitFxRate(BigDecimal.ZERO);
                transferReq.setCreditFxRate(BigDecimal.ZERO);
            }

            transferReq.setDebitAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getCustomerBranch()));
            transferReq.setCreditAccount(req.getBeneficiaryAccount());
            identifier = "api:postGLToDepositTransfer";
        } else {
            transferReq.setDebitAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getCustomerBranch()));

            transferReq.setCreditAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getCustomerBranch()));

            if (req.getType().equalsIgnoreCase("001") && req.getCurrency().equalsIgnoreCase("TZS")) {
                transferReq.setCreditAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", req.getCustomerBranch()));
            }
            if (req.getType().equalsIgnoreCase("001") && req.getCurrency().equalsIgnoreCase("USD")) {
                transferReq.setCreditAccount(systemVariable.TRANSFER_AWAITING_TT_LEDGER.replace("***", req.getCustomerBranch()));
            }
            if (req.getType().equalsIgnoreCase("001") && req.getCurrency().equalsIgnoreCase("EUR")) {
                transferReq.setCreditAccount(systemVariable.TRANSFER_MIRROR_TT_BHF_LEDGER.replace("***", req.getCustomerBranch()));
            }
        }
        transferReq.setReference(req.getReference());
        transferReq.setAmount(req.getAmount());
        transferReq.setNarration(req.getDescription() + " batch reference:" + req.getBatchReference());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setUserRole(systemVariable.apiUserRole(req.getCustomerBranch()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("POSTING BATCH TRANSACTION FROM CUSTOMER ACCT TO BRANCH LEDGER: FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING, cbsResponse);
            throw new RuntimeException("[" + PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING + "] - error http response - [" + cbsResponse + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //check if the transfer is TISS
            String reference = req.getReference();
            String swiftMessage = "NA";

            if (req.getType().equalsIgnoreCase("001")) {
                String stpReference = "STP" + req.getReference().substring(0, 3) + req.getReference().substring(6);
                req.setReference(stpReference);
                //set senderBic
                req.setSenderBic(systemVariable.SENDER_BIC);
                req.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                if (req.getCurrency().equalsIgnoreCase("USD")
                        && !req.getBeneficiaryBIC().contains("TZ")) {
                    req.setCorrespondentBic(systemVariable.USD_CORRESPONDEND_BANK);
                } else if (req.getCurrency().equalsIgnoreCase("EUR")) {
                    req.setCorrespondentBic(systemVariable.EURO_CORRESPONDEND_BANK);
                } else {
                    req.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                }
                swiftMessage = SwiftService.createMT103FromOnlineReq(req);
            }
            //PROCESS STP
            if (systemVariables.IS_TISS_VPN_ALLOWED && req.getType().equalsIgnoreCase("001")) {
                //SEND TRANSACTION TO BOT
                //Create signature
                String signedRequestXML = signTISSVPNRequest(swiftMessage, systemVariables.SENDER_BIC);
                //log the swift advice message for review on cilantro transfer advises table
                swiftRepository.saveSwiftMessageInTransferAdvices(swiftMessage, "BOT-VPN", "OUTGOING");
                String response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariables.BOT_TISS_VPN_URL, req.getReference(), systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, systemVariables.getSysConfiguration("BOT.tiss.daily.token", "prod"), systemVariables.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH,systemVariables.PRIVATE_TISS_VPN_KEYPASS);
                LOGGER.info("REQUEST TO BOT:{}, RESPONSE FROM BOT:{}", signedRequestXML, response);
                if (response != null && !response.equalsIgnoreCase("-1")) {
                    //get message status and update transfers table with response to IBD approval
                    String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
                    if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                        //the message is successfully on BOT ENDPOINT
                        queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
                    } else {
                        queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^REJECTED");
                    }
                } //else is needed here

            }
            else {
                if (req.getType().equalsIgnoreCase("001") || req.getType().equalsIgnoreCase("004")) {
                    queProducer.sendToQueueRTGSToSwift(swiftMessage + "^" + systemVariables.KPRINTER_URL + "^" + req.getReference());
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
                }
            }
//            queueProducer.sendToQueueMT103StpOutgoing(req);
            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
//            jdbcTemplate.update("INSERT IGNORE INTO transfers(swift_message,txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", swiftMessage, req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), reference, req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "C", "Posted from brach waiting to cust acct/bot GL cbs Success[" + cbsResponse.getResult() + "]", req.getCallbackUrl());
            jdbcTemplate.update("UPDATE transfers SET response_code =?,  status='C', message=?, comments=?, cbs_status='C' WHERE reference=? or txid=?", cbsResponse.getResult(), cbsResponse.getMessage(), cbsResponse.getMessage(), req.getReference(), req.getReference());

        } else if (cbsResponse.getResult() == 53) {
            //check if the transfer is TISS
            String swiftMessage = "NA";
            if (req.getType().equalsIgnoreCase("001")) {
                //set senderBic
                req.setSenderBic(systemVariable.SENDER_BIC);
                req.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                if (req.getCurrency().equalsIgnoreCase("USD")
                        && !req.getBeneficiaryBIC().contains("TZ")) {
                    req.setCorrespondentBic(systemVariable.USD_CORRESPONDEND_BANK);
                } else if (req.getCurrency().equalsIgnoreCase("EUR")) {
                    req.setCorrespondentBic(systemVariable.EURO_CORRESPONDEND_BANK);
                } else {
                    req.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                }
                swiftMessage = SwiftService.createMT103FromOnlineReq(req);
            }
            //check if its muse and send a callback asap

//            jdbcTemplate.update("INSERT INTO transfers (swift_message, txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message, callbackurl) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", swiftMessage, req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "F", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "F", "Failed to post from branch waiting gl to customer acct/bot GL[" + cbsResponse.getResult() + "]", req.getCallbackUrl());
            jdbcTemplate.update("UPDATE transfers SET response_code =?,  status='C', message=?, comments=?, cbs_status='F' WHERE reference=? or txid=?", cbsResponse.getResult(), cbsResponse.getMessage(), cbsResponse.getMessage(), req.getReference(), req.getReference());

        } else {
            LOGGER.info("FAILED TO PROCESS BATCH ENTRY WITH REFERENCE: " + req.getReference());
            throw new RuntimeException("[" + PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_BOOK_TRANSFER, containerFactory = "queueListenerFactory")
    public void processBookTransfer(@Payload PaymentReq req,
                                    @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        String identifier = "api:postGLToDepositTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getReference());
        transferReq.setAmount(req.getAmount());
        transferReq.setNarration(req.getDescription() + " batch Reference:" + req.getBatchReference());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getCustomerBranch()));
        transferReq.setCreditAccount(req.getBeneficiaryAccount());
        transferReq.setUserRole(systemVariable.apiUserRole(req.getCustomerBranch()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("BOOK TRANSFER BT: trans Reference {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BOOK_TRANSFER, null);
            throw new RuntimeException("[" + PROCESS_BOOK_TRANSFER + "] - error http response - [" + null + "]");
        }
        switch (cbsResponse.getResult()) {
            case 0:
            case 26:
                //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
//                jdbcTemplate.update("INSERT IGNORE INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE message='Success'", req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "C", "Success", req.getCallbackUrl());
                jdbcTemplate.update("UPDATE transfers SET response_code =?,  status='C', message='success', cbs_status='C' WHERE reference=? or txid=?", cbsResponse.getResult(), req.getReference(), req.getReference());
                if (req.getCallbackUrl() != null) {
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");//send callback to IBANK
                }
                break;
            case 51:
            case 53:
            case 14:
            case 13:
//                jdbcTemplate.update("INSERT IGNORE INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "F", "Invalid Amount", req.getCallbackUrl());
                //                jdbcTemplate.update("INSERT IGNORE INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "F", "Insufficient Amount", req.getCallbackUrl());
                jdbcTemplate.update("UPDATE transfers SET response_code =?,  status='C', message=?, comments=?, cbs_status='F' WHERE reference=? or txid=?", cbsResponse.getResult(), cbsResponse.getMessage(), cbsResponse.getMessage(), req.getReference(), req.getReference());
                if (req.getCallbackUrl() != null) {
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^FAILED");//send callback to IBANK
                }
                break;
            default:
                LOGGER.info("FAILED TO PROCESS BT WITH REFERENCE: " + req.getReference());
                throw new RuntimeException("[" + PROCESS_BOOK_TRANSFER + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_MULTIPLE_DEBITS_TO_SUSPENSE_LEDGER, containerFactory = "queueListenerFactory")
    public void processMultipleDebitsToSuspenseAccount(@Payload PaymentReq req,
                                                       @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        String identifier = "api:postDepositToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getReference());
        transferReq.setAmount(req.getAmount());
        transferReq.setNarration(req.getDescription() + " batch Reference:" + req.getBatchReference());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setCreditAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getCustomerBranch()));
        transferReq.setDebitAccount(req.getSenderAccount());
        transferReq.setUserRole(systemVariable.apiUserRole(req.getCustomerBranch()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("BOOK TRANSFER BT: trans Reference {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BOOK_TRANSFER, null);
            throw new RuntimeException("[" + PROCESS_BOOK_TRANSFER + "] - error http response - [" + null + "]");
        }
        switch (cbsResponse.getResult()) {
            case 0:
                queProducer.sendToQueueProcessBookTransfer(req);//send book transfer payments
                //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
//                jdbcTemplate.update("INSERT IGNORE INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE message='Success'", req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "C", "Success", req.getCallbackUrl());
                jdbcTemplate.update("UPDATE transfers SET status='C', message='success', cbs_status='C' WHERE reference=? or txid=?", req.getReference(), req.getReference());
                if (req.getCallbackUrl() != null) {
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");//send callback to IBANK
                }
                break;
            case 51:
            case 53:
            case 26:
            case 14:
            case 13:
//                jdbcTemplate.update("INSERT IGNORE INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE message='Failed'", req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "F", "Invalid Amount", req.getCallbackUrl());
                //                jdbcTemplate.update("INSERT IGNORE INTO transfers(txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE message='Success'", req.getType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), systemVariable.SENDER_BIC, req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), "IB", "C", cbsResponse.getResult(), req.getDescription(), "OUTGOING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), req.getCustomerBranch(), "F", "Insufficient Amount", req.getCallbackUrl());
                jdbcTemplate.update("UPDATE transfers SET response_code =?,  status='C', message=?, cbs_status='F' WHERE reference=? or txid=?", cbsResponse.getResult(), cbsResponse.getMessage(), req.getReference(), req.getReference());
                if (req.getCallbackUrl() != null) {
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^FAILED");//send callback to IBANK
                }
                break;
            default:
                LOGGER.info("FAILED TO PROCESS BT WITH REFERENCE: " + req.getReference());
                throw new RuntimeException("[" + PROCESS_BOOK_TRANSFER + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_WALLET_TRANSFER_REVERSAL, containerFactory = "queueListenerFactory")
    public void reverseWalletTransferTxn(@Payload PaymentReq paymentReq,
                                         @Headers MessageHeaders headers, Message message, Session session) throws JMSException, ParseException {
        //get the MESSAGE DETAILS FROM THE QUEUE

        TxRequest transferReq = new TxRequest();
        transferReq.setAmount(paymentReq.getAmount());
        if (paymentReq.getType().equalsIgnoreCase("061")) {
            transferReq.setScheme("X01");
            transferReq.setDebitAccount(systemVariable.MPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] MPESAB2C");
        } else if (paymentReq.getType().equalsIgnoreCase("062")) {
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ]AIRTELB2C");
            transferReq.setDebitAccount(systemVariable.AIRTELB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("063")) {
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] TIGO2C");
            transferReq.setScheme("X01");
            transferReq.setDebitAccount(systemVariable.TIGOPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("064")) {
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] HALOPESAB2C");
            transferReq.setScheme("X01");
            transferReq.setDebitAccount(systemVariable.HALOPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("065")) {
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] EZYPESAB2C");
            transferReq.setDebitAccount(systemVariable.EZYPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        }
        transferReq.setCreditAccount(paymentReq.getSenderAccount());
        transferReq.setDebitFxRate(BigDecimal.ZERO);
        transferReq.setCreditFxRate(BigDecimal.ZERO);
        transferReq.setCurrency("TZS");
        transferReq.setReference(paymentReq.getReference());
        transferReq.setUserRole(systemVariable.apiUserRole(paymentReq.getCustomerBranch()));
        PostGLToDepositTransfer postOutwardReq = new PostGLToDepositTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, "api:postGLToDepositTransfer"), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", paymentReq.getReference());
            //do not update the transaction status
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            LOGGER.info("TXN REVERSED  SUCCESSFULLY : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", paymentReq.getSenderAccount(), paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getBatchReference());
            jdbcTemplate.update("update transfers set status='F',cbs_status='R',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), transferReq.getUserRole().getUserName(), DateUtil.now(), paymentReq.getReference());
        } else {
            LOGGER.info("TXN FAILED TO BE REVERSED : SOURCE ACCT {} :Reference: {} AMOUNT: {} BatchReference: {}", paymentReq.getSenderAccount(), paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getBatchReference());
            throw new RuntimeException("[" + PROCESS_WALLET_TRANSFER_REVERSAL + "] - error http response - [" + cbsResponse + "]");
        }
    }

    @JmsListener(destination = PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT, containerFactory = "queueListenerFactory")
    public void processOutwardEFTReversalAfterSettlement(@Payload String reversalString,
                                                         @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        List<Map<String, Object>> txn = getSwiftMessage(reversalString.split("\\^")[0]);
        if (txn != null && !txn.isEmpty()) {
            //Fix duplicate reversal
            String purpose = txn.get(0).get("purpose") + "";
            String status = txn.get(0).get("status") + "";
            String cbs_status = txn.get(0).get("cbs_status") + "";
            if (purpose.contains("REV~")) {
                LOGGER.info("TRANSACTION ALREADY REVERSED : trans Reference {} REASON: {}", reversalString.split("\\^")[0], reversalString.split("\\^")[1]);
            }
            if (status.equalsIgnoreCase("C") && cbs_status.equalsIgnoreCase("C")) {
                LOGGER.info("TRANSACTION has status C and cbs status C : trans Reference {} REASON: {}", reversalString.split("\\^")[0], reversalString.split("\\^")[1]);
            } else {
                String identifier = "api:postGLToDepositTransfer";
                TxRequest transferReq = new TxRequest();
                transferReq.setReference(txn.get(0).get("reference") + "  ");
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration("REV~ " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + " Reason: " + reversalString.split("\\^")[1]);
                transferReq.setCurrency(txn.get(0).get("currency") + "");
                transferReq.setDebitAccount(systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER.replace("***", txn.get(0).get("branch_no") + ""));
                transferReq.setCreditAccount(txn.get(0).get("sourceAcct").toString());
                transferReq.setUserRole(systemVariable.apiUserRole(txn.get(0).get("branch_no") + ""));
                PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("TRANSACTION REVERSED SUCCESSFULLY : trans Reference {} REASON: {}", reversalString.split("\\^")[0], reversalString.split("\\^")[1]);
                    //do not update the transaction status
                    LOGGER.info("[{}] - error http response - {} ", PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT, cbsResponse);
                    throw new RuntimeException("[" + PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT + "] - error http response - [" + cbsResponse + "]");
                }
                if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                    //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                    jdbcTemplate.update("UPDATE transfers set status='RS',comments='Transaction reversed successfuly',purpose=? where reference=?", "REV~ " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + " Reason: " + reversalString.split("\\^")[1], reversalString.split("\\^")[0]);
                } else {
                    LOGGER.info("FAILED TO PROCESS REVERSAL OF TXNS WITH REFERENCE: " + reversalString.split("\\^")[0]);
                    throw new RuntimeException("[" + PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT + "] - error http response - [" + cbsResponse + "]");
                }
            }
        } else {
            LOGGER.error("[" + PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT + "] - transaction not found for reversal reference: " + reversalString.split("\\^")[0]);
        }
    }

    @JmsListener(destination = PROCESS_OUTWARD_ACKNOWLEDGEMENT_BY_TACH, containerFactory = "queueListenerFactory")
    public void processOutwardEFTAcknowledgementByTACH(@Payload String ackString,
                                                       @Headers MessageHeaders headers, Message message, Session session) {
        String OriginalMessageType = ackString.split("\\^")[2];
//        String status = ackString.split("\\^")[1];
        String fileExtention = ackString.split("\\^")[3];
        String batchReference = ackString.split("\\^")[0];
        MxPacs00200103 mx = MxPacs00200103.parse(ackString.split("\\^")[4]);
        switch (OriginalMessageType) {
            case "pacs.004.001.02": //inward Acknowledgement transactions - after returning transaction
                if (fileExtention.equalsIgnoreCase("V")) {
                    String status = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getGrpSts().value();
                    if (status.equalsIgnoreCase("ACCP")) {
                        //update the status to CA( Accepted successfully)
                        jdbcTemplate.update("UPDATE transfers set status='CA',message=?,modified_dt=now() ,returned_dt=now() where batch_reference2=?", "Accepted", batchReference);
                    }

                    if (status.equalsIgnoreCase("RJCT")) {
                        //update the status to CA( Accepted successfully)
                        jdbcTemplate.update("UPDATE transfers set status='CF',message=?,modified_dt=now(),returned_dt=now() where batch_reference2=?", "Rejected", batchReference);
                    }
                    if (status.equalsIgnoreCase("PART")) {
                        //update the status to CA( Accepted successfully)
                        for (PaymentTransactionInformation26 req : mx.getFIToFIPmtStsRpt().getTxInfAndSts()) {
                            jdbcTemplate.update("UPDATE transfers set status='CF',message=?,modified_dt=now(),returned_dt=now() where txid=?", eftRepo.eftErrorCodes(req.getStsRsnInf().get(0).getRsn().getCd()) + req.getStsRsnInf().get(0).getRsn().getCd(), req.getOrgnlEndToEndId());
                        }
                    }
                } else if (fileExtention.equalsIgnoreCase("R")) {
                    String status = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getGrpSts().value();
                    if (status.equalsIgnoreCase("ACSC")) {
                        jdbcTemplate.update("UPDATE transfers set status='CAC',modified_dt=now(),returned_dt=now(), message=? where batch_reference2=?", "Accepted and Received", batchReference);
                    }
                } else if (fileExtention.equalsIgnoreCase("RC")) {
                    String status = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getGrpSts().value();
                    if (status.equalsIgnoreCase("CLRD")) {
                        jdbcTemplate.update("UPDATE transfers set status='CA',modified_dt=now(),returned_dt=now(), message=? where batch_reference2=?", "Submitted successfully to TACH awaiting callback", batchReference);
                    }
                }
                break;
            case "pacs.008.001.02"://outward transactions
                if (fileExtention.equalsIgnoreCase("V")) {
                    String status = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getGrpSts().value();
                    //update the status to CA( Accepted successfully)
                    if (status.equalsIgnoreCase("ACCP")) {
                        jdbcTemplate.update("UPDATE transfers set status='CA',message=? where batch_reference2=?", "Accepted", batchReference);
                    }
                    if (status.equalsIgnoreCase("RJCT")) {
                        //update the status to CA( Accepted successfully)
                        String reasonCode = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getStsRsnInf().get(0).getRsn().getCd();
                        LOGGER.info("Allowed Rev Code:{} => Incoming Rev Code: {}", systemVariable.BOT_EFT_REVERSAL_CODES, reasonCode);

                        if (systemVariable.BOT_EFT_REVERSAL_CODES.contains(reasonCode)) {
                            jdbcTemplate.update("UPDATE transfers set status='CF',message=? where batch_reference2=?", eftRepo.eftErrorCodes(reasonCode), batchReference);
                            for (Map<String, Object> r : getEFTMessageByBatchReference(batchReference)) {
                                if (!r.get("beneficiaryBIC").toString().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
                                    LOGGER.info("REVERSING  OUTGOING TRANSACTION AFTER ACKNOWLEDGEMENT FROM TACH :REFERENCE:{} REASON CODE: {} REASON DESCRIPTION:{}", r.get("reference"), reasonCode, eftRepo.eftErrorCodes(reasonCode));
                                    queProducer.sendToQueueOutwardReversal(r.get("reference") + "^" + eftRepo.eftErrorCodes(reasonCode));//REVERSE THE TRANSACTION TO CUSTOMER ACCOUNT
                                    if (r.get("code").toString().equalsIgnoreCase("IB")) {
                                        queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(r.get("reference") + "^FAILED");//send callback to IBANK
                                    }
                                } else {
                                    LOGGER.info("TRANSACTION CANNOT BE REVERSED TACH :REFERENCE:{} REASON CODE: {} REASON DESCRIPTION:{}", r.get("reference"), reasonCode, eftRepo.eftErrorCodes(reasonCode));
                                }
                            }
                        } else {
                            jdbcTemplate.update("UPDATE transfers set status='CF',message=?,comments='File can not be reversed' where batch_reference2=?", eftRepo.eftErrorCodes(reasonCode), batchReference);
                        }
                    }
                }

                if (fileExtention.equalsIgnoreCase("C")) {
                    String status = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getGrpSts().value();
                    if (status.equalsIgnoreCase("RJCT")) {
                        //update the status to CA( Accepted successfully)
                        String reasonCode = mx.getFIToFIPmtStsRpt().getOrgnlGrpInfAndSts().getStsRsnInf().get(0).getRsn().getCd();
                        LOGGER.info("Allowed Rev Code:{} => Incoming Rev Code: {}", systemVariable.BOT_EFT_REVERSAL_CODES, reasonCode);

                        if (systemVariable.BOT_EFT_REVERSAL_CODES.contains(reasonCode)) {
                            jdbcTemplate.update("UPDATE transfers set status='CF',message=? where batch_reference2=?", eftRepo.eftErrorCodes(reasonCode), batchReference);
                            for (Map<String, Object> r : getEFTMessageByBatchReference(batchReference)) {
                                if (!r.get("beneficiaryBIC").toString().equalsIgnoreCase(systemVariable.SENDER_BIC)) {
                                    LOGGER.info("REVERSING  OUTGOING TRANSACTION AFTER ACKNOWLEDGEMENT FROM TACH :REFERENCE:{} REASON CODE: {} REASON DESCRIPTION:{}", r.get("reference"), reasonCode, eftRepo.eftErrorCodes(reasonCode));
                                    queProducer.sendToQueueOutwardReversal(r.get("reference") + "^" + eftRepo.eftErrorCodes(reasonCode));//REVERSE THE TRANSACTION TO CUSTOMER ACCOUNT
                                    if (r.get("code").toString().equalsIgnoreCase("IB") || r.get("code").toString().equalsIgnoreCase("MOB")) {
                                        queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(r.get("reference") + "^FAILED");//send callback to IBANK
                                    }
                                } else {
                                    LOGGER.info("TRANSACTION CANNOT BE REVERSED TACH :REFERENCE:{} REASON CODE: {} REASON DESCRIPTION:{}", r.get("reference"), reasonCode, eftRepo.eftErrorCodes(reasonCode));
                                }
                            }
                        } else {
                            jdbcTemplate.update("UPDATE transfers set status='CF',message=?,comments='File can not be reversed' where batch_reference2=?", eftRepo.eftErrorCodes(reasonCode), batchReference);
                        }
                    }
                }

                if (fileExtention.equalsIgnoreCase("R")) {
                    //update the status to CA( Accepted successfully)
                    jdbcTemplate.update("UPDATE transfers set status='CAC',message=? where batch_reference2=?", "Accepted/Settled", batchReference);
                }
                if (fileExtention.equalsIgnoreCase("RC")) {
                    //update the status to CA( Accepted successfully)
                    for (Map<String, Object> r : getEFTMessageByBatchReference(batchReference)) {
                        jdbcTemplate.update("UPDATE transfers set status='S',message=? where batch_reference2=? and reference=?", "Settled and Cleared", batchReference, r.get("reference"));
                        if (r.get("code").toString().equalsIgnoreCase("IB")) {
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(r.get("reference") + "^SUCCESS");//send callback to IBANK
                        }
                    }
                }
                break;
        }

    }


    @JmsListener(destination = PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO, containerFactory = "queueListenerFactory")
    public void processCallbackToEmKopo(@Payload String reference,
                                        @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        List<Map<String, Object>> txn = getSwiftMessage(reference.split("\\^")[0]);

        LOGGER.info("=======>>>> txn call back" + txn);

        if (txn != null) {
            System.out.println("==================this reference" + txn.get(0).get("reference").toString());
            String responseCode = "0";
            if (!reference.split("\\^")[1].equalsIgnoreCase("success")) {
                responseCode = "99";
            }
            String reqxml;
            String swiftDoc = creditRepo.getSwiftAdviceAttachment(txn.get(0).get("reference").toString());

            System.out.println("==========Swift Doc");
            System.out.println(swiftDoc);

            System.out.println(txn.get(0).get("callbackUrl"));
            System.out.println("===========end Swift Doc");

            if (txn.get(0).get("reference").toString().startsWith("STP")) {
                reqxml = "<paymentCallback>"
                        + "<reference>" + txn.get(0).get("txid") + "</reference>\n"
                        + "<responseCode>" + responseCode + "</responseCode>\n"
                        + "<status>" + reference.split("\\^")[1] + "</status>\n"
                        + "<message>" + reference.split("\\^")[1] + "</message>\n"
                        + "<swiftMessage>" + txn.get(0).get("swift_message") + "</swiftMessage>\n"
                        + "<adviceAttachment>" + swiftDoc + "</adviceAttachment>\n"
                        + "</paymentCallback>";
            } else {
                reqxml = "<paymentCallback>"
                        + "<reference>" + txn.get(0).get("txid") + "</reference>\n"
                        + "<responseCode>" + responseCode + "</responseCode>\n"
                        + "<status>" + reference.split("\\^")[1] + "</status>\n"
                        + "<message>" + reference.split("\\^")[1] + "</message>\n"
                        + "<swiftMessage>" + txn.get(0).get("swift_message") + "</swiftMessage>\n"
                        + "<adviceAttachment>" + swiftDoc + "</adviceAttachment>\n"
                        + "</paymentCallback>";
            }
            //process the Request to CBS
            PaymentCallbackResp callbackResponse = XMLParserService.jaxbXMLToObject(HttpClientService.sendXMLRequest(reqxml, txn.get(0).get("callbackUrl") + ""), PaymentCallbackResp.class);
            if (callbackResponse == null) {
                LOGGER.info("ERROR ON SENDING CALLBACK ON EMKOPO>>>>>>>>>>>>");
                //do not update the transaction status
                LOGGER.info("[{}] - error http response - {} ", PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO, null);
                throw new RuntimeException("[" + PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO + "] - error http response - [" + null + "]");
            }
            if (callbackResponse.getResponseCode() == 0 || callbackResponse.getResponseCode() == 26) {
                //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                LOGGER.info("CALLBACK SEND SUCCESSFULLY TO EMKOPO: REFERENCE:{}", reference);
                jdbcTemplate.update("UPDATE  transfers set status=? where reference=?", callbackResponse.getStatus(), reference);

            } else {
                LOGGER.info("FAILED TO PROCESS CALLBACK TO IBANK WITH REFERENCE: " + reference);
                throw new RuntimeException("[" + PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO + "] - error http response - [" + callbackResponse + "]");
            }
        }
    }

    @JmsListener(destination = PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK, containerFactory = "queueListenerFactory")
    public void processCallbackToIbank(@Payload String reference,
                                       @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        List<Map<String, Object>> txn = getSwiftMessage(reference.split("\\^")[0]);
        if (txn != null) {
            String responseCode = "0";
            if (!reference.split("\\^")[1].equalsIgnoreCase("success")) {
                responseCode = "99";
            }
            String reqxml;
            if (txn.get(0).get("reference").toString().startsWith("STP")) {
                reqxml = "<paymentCallback>"
                        + "<ibToken>CVAGZLHEPDMEMBEGMYXKIQQCSLXZWGRODMPKQXSXAMSSLCLBDSCQCHOVAKFWNIFBWREOEJDPDJRCEVPGYXOCIUNPMICTXKUCPJFVVUKKBKLJBIDBJZXAJEEOGDDAZSXFZSUVLJMTZUKFRQYSXRQLGONPWCLKRVLKCDZQFYRYDZBPWVHAEYUAIEGRKVNDWBHYHREPFSWX</ibToken>\n"
                        + "<reference>" + txn.get(0).get("txid") + "</reference>\n"
                        + "<responseCode>" + responseCode + "</responseCode>\n"
                        + "<status>" + reference.split("\\^")[1] + "</status>\n"
                        + "<message>" + reference.split("\\^")[1] + "</message>\n"
                        + "<batchReference>" + txn.get(0).get("batch_reference") + "</batchReference>\n"
                        + "<amount>" + txn.get(0).get("amount") + "</amount>\n"
                        + "<currency>" + txn.get(0).get("currency") + "</currency>\n"
                        + "<sourceAccount>" + txn.get(0).get("sourceAcct") + "</sourceAccount>\n"
                        + "<narration>" + txn.get(0).get("purpose") + "</narration>\n"
                        + "<beneficiaryAccount>" + txn.get(0).get("destinationAcct") + "</beneficiaryAccount>\n"
                        + "<settlementReference>" + txn.get(0).get("reference") + "</settlementReference>\n"
                        + "<transtype>" + txn.get(0).get("txn_type") + "</transtype>"
                        + "<units>" + txn.get(0).get("units") + "</units>"
                        + "<availableBalance>0.00</availableBalance>\n"
                        + "<ledgerBalance>0.00</ledgerBalance>\n"
                        + "</paymentCallback>";
            } else {
                reqxml = "<paymentCallback>"
                        + "<ibToken>CVAGZLHEPDMEMBEGMYXKIQQCSLXZWGRODMPKQXSXAMSSLCLBDSCQCHOVAKFWNIFBWREOEJDPDJRCEVPGYXOCIUNPMICTXKUCPJFVVUKKBKLJBIDBJZXAJEEOGDDAZSXFZSUVLJMTZUKFRQYSXRQLGONPWCLKRVLKCDZQFYRYDZBPWVHAEYUAIEGRKVNDWBHYHREPFSWX</ibToken>\n"
                        + "<reference>" + txn.get(0).get("reference") + "</reference>\n"
                        + "<responseCode>" + responseCode + "</responseCode>\n"
                        + "<status>" + reference.split("\\^")[1] + "</status>\n"
                        + "<message>" + reference.split("\\^")[1] + "</message>\n"
                        + "<batchReference>" + txn.get(0).get("batch_reference") + "</batchReference>\n"
                        + "<amount>" + txn.get(0).get("amount") + "</amount>\n"
                        + "<currency>" + txn.get(0).get("currency") + "</currency>\n"
                        + "<sourceAccount>" + txn.get(0).get("sourceAcct") + "</sourceAccount>\n"
                        + "<narration>" + txn.get(0).get("purpose") + "</narration>\n"
                        + "<beneficiaryAccount>" + txn.get(0).get("destinationAcct") + "</beneficiaryAccount>\n"
                        + "<settlementReference>" + txn.get(0).get("txid") + "</settlementReference>\n"
                        + "<transtype>" + txn.get(0).get("txn_type") + "</transtype>"
                        + "<units>" + txn.get(0).get("units") + "</units>"
                        + "<availableBalance>0.00</availableBalance>\n"
                        + "<ledgerBalance>0.00</ledgerBalance>\n"
                        + "</paymentCallback>";
            }
            //process the Request to CBS
            PaymentCallbackResp callbackResponse = XMLParserService.jaxbXMLToObject(HttpClientService.sendXMLRequest(reqxml, txn.get(0).get("callbackUrl") + ""), PaymentCallbackResp.class);
            if (callbackResponse == null) {
                LOGGER.info("ERROR ON SENDING CALLBACK ON IBANK>>>>>>>>>>>>");
                //do not update the transaction status
                LOGGER.info("[{}] - error http response - {} ", PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK, null);
                throw new RuntimeException("[" + PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK + "] - error http response - [" + null + "]");
            }
            if (callbackResponse.getResponseCode() == 0 || callbackResponse.getResponseCode() == 26) {
                //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                LOGGER.info("CALLBACK SEND SUCCESSFULLY TO IBANK: REFERENCE:{}", reference);
                jdbcTemplate.update("UPDATE  transfers set ibankStatus=? where reference=?", callbackResponse.getStatus(), reference);

            } else {
                LOGGER.info("FAILED TO PROCESS CALLBACK TO IBANK WITH REFERENCE: " + reference);
                throw new RuntimeException("[" + PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK + "] - error http response - [" + callbackResponse + "]");
            }
        }
    }

    @JmsListener(destination = PROCESS_LUKU_CALLBACK, containerFactory = "queueListenerFactory")
    public void processLukuCallback(@Payload String payloadReq,
                                    @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        LukuPaymentCallBack callBack = XMLParserService.jaxbXMLToObject(payloadReq, LukuPaymentCallBack.class);
        List<Map<String, Object>> txn = getSwiftMessage(callBack.getTRANSID());
        if (txn != null) {
            if (callBack.getSTATCODE().equalsIgnoreCase("7101") || callBack.getSTATCODE().equalsIgnoreCase("0")) {
                String identifier = "api:postGLToDepositTransfer";
                TxRequest transferReq = new TxRequest();
                transferReq.setReference(txn.get(0).get("reference") + "  ");
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration(txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + "[" + txn.get(0).get("sourceAcct").toString() + ">" + txn.get(0).get("destinationAcct") + " ] LUKU");
                transferReq.setCurrency(txn.get(0).get("currency") + "");
                transferReq.setDebitAccount(systemVariable.LUKU_SUSPENSE_ACCOUNT.replace("***", txn.get(0).get("branch_no") + ""));
                transferReq.setCreditAccount(systemVariable.LUKU_COLLECTION_ACCOUNT);
                transferReq.setUserRole(systemVariable.apiUserRole(txn.get(0).get("branch_no") + ""));
                PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("TRANSACTION FAILED : Trans Reference {}", callBack.getTRANSID());
                    //do not update the transaction status
                    LOGGER.info("[{}] - error http response - {} ", PROCESS_LUKU_CALLBACK, null);
                    throw new RuntimeException("[" + PROCESS_LUKU_CALLBACK + "] - error http response - [" + null + "]");
                }
                if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                    //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                    jdbcTemplate.update("UPDATE transfers set status='C',comments='Completed Successfully',message='Transaction success',txid=?,batch_reference2=?,units=? where reference=?", callBack.getTOKEN(), callBack.getTOKEN(), callBack.getUNITS(), callBack.getTRANSID());
                    queueProducer.sendToQueueOutwardAcknowledgementToInternetBanking(callBack.getTRANSID() + "^SUCCESS");//PROCESS CALLBACK TO IBANK
                } else {
                    LOGGER.info("FAILED TO PROCESS CALLBACK OF TXNS WITH REFERENCE: " + callBack.getTRANSID());
                    throw new RuntimeException("[" + PROCESS_LUKU_CALLBACK + "] - error http response - [" + cbsResponse + "]");
                }

            } else {
                //reverse the transaction to customer account
                String identifier = "api:postGLToDepositTransfer";
                TxRequest transferReq = new TxRequest();
                transferReq.setReference(txn.get(0).get("reference") + "  ");
                transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                transferReq.setNarration("REV~" + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + "[" + txn.get(0).get("sourceAcct").toString() + ">" + txn.get(0).get("destinationAcct") + " ] LUKU");
                transferReq.setCurrency(txn.get(0).get("currency") + "");
                transferReq.setDebitAccount(systemVariable.LUKU_SUSPENSE_ACCOUNT.replace("***", txn.get(0).get("branch_no") + ""));
                transferReq.setCreditAccount(txn.get(0).get("sourceAcct").toString());
                transferReq.setUserRole(systemVariable.apiUserRole(txn.get(0).get("branch_no") + ""));
                PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("TRANSACTION FAILED  : trans Reference {}", callBack.getTRANSID());
                    //do not update the transaction status
                    LOGGER.info("[{}] - error http response - {} ", PROCESS_LUKU_CALLBACK, null);
                    throw new RuntimeException("[" + PROCESS_LUKU_CALLBACK + "] - error http response - [" + null + "]");
                }
                if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                    //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                    jdbcTemplate.update("UPDATE transfers set status='F',comments='Transaction failed on gateway',message='Transaction is Successfully reversed',cbs_status='R',txid=? where reference=?", callBack.getTOKEN(), callBack.getTRANSID());
                } else {
                    LOGGER.info("FAILED TO PROCESS CALLBACK OF TXNS WITH REFERENCE: " + callBack.getTRANSID());
                    throw new RuntimeException("[" + PROCESS_LUKU_CALLBACK + "] - error http response - [" + cbsResponse + "]");
                }

            }
        } else {
            throw new RuntimeException("[" + PROCESS_LUKU_CALLBACK + "] - error http response");
        }
    }

    @JmsListener(destination = CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW, containerFactory = "queueListenerFactory")
    public void processMPESACallback(@Payload String payloadReq,
                                     @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        MpesaCallback mpesaCallback = XMLParserService.jaxbXMLToObject(payloadReq, MpesaCallback.class);
        LOGGER.info("Parsed MPESA Callback: {}", mpesaCallback);
        List<Map<String, Object>> txn = getSwiftMessage(mpesaCallback.getTRANSID());
        if (txn != null && txn.size() > 0) {
            if (!mpesaCallback.getSTASCODE().equalsIgnoreCase("0")) {
                /*
                *   Transfer to Mpesa	061	Transfer to wallet
                    Transfer to Airtel Money	062	Transfer to wallet
                    Transfer to Tigo Pesa	063	Transfer to wallet
                    Transfer to Halopesa	064	Transfer to wallet
                    Transfer to EzyPesa	065	Transfer to wallet
                * */
                //reverse the transaction to customer account
                String purpose = txn.get(0).get("purpose") + "";
                if (!purpose.startsWith("REV")) {
                    String txnType = txn.get(0).get("txn_type") + "";
                    String identifier = "api:postDepositToGLTransfer";
                    TxRequest transferReq = new TxRequest();
                    transferReq.setReference(txn.get(0).get("reference") + "");
                    transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                    transferReq.setNarration("REV~" + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + "[" + txn.get(0).get("sourceAcct").toString() + ">" + txn.get(0).get("destinationAcct") + " ] LUKU");
                    transferReq.setCurrency(txn.get(0).get("currency") + "");
                    if (txnType.equals("061")) {
                        transferReq.setCreditAccount(systemVariable.MPESAB2C_LEDGER.replace("***", txn.get(0).get("branch_no") + ""));
                    } else if (txnType.equals("062")) {
                        transferReq.setCreditAccount(systemVariable.AIRTELB2C_LEDGER.replace("***", txn.get(0).get("branch_no") + ""));
                    } else if (txnType.equals("063")) {
                        transferReq.setCreditAccount(systemVariable.TIGOPESAB2C_LEDGER.replace("***", txn.get(0).get("branch_no") + ""));
                    } else if (txnType.equals("064")) {
                        transferReq.setCreditAccount(systemVariable.HALOPESAB2C_LEDGER.replace("***", txn.get(0).get("branch_no") + ""));
                    } else if (txnType.equals("065")) {
                        transferReq.setCreditAccount(systemVariable.TIGOPESAB2C_LEDGER.replace("***", txn.get(0).get("branch_no") + ""));
                    } else {
                        transferReq.setCreditAccount("No Gl account please check. processMPESACallback");
                    }
                    transferReq.setDebitAccount(txn.get(0).get("sourceAcct").toString());
                    transferReq.setReversal("true");
                    transferReq.setUserRole(systemVariable.apiUserRole(txn.get(0).get("branch_no") + ""));
                    PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                    postOutwardReq.setRequest(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, identifier), XaResponse.class);
                    if (cbsResponse == null) {
                        LOGGER.info("TRANSACTION FAILED  : trans Reference {}", mpesaCallback.getTRANSID());
                        //do not update the transaction status
                        LOGGER.info("[{}] - error http response - {} ", CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW, null);
                        throw new RuntimeException("[" + CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW + "] - error http response - [" + null + "]");
                    }
                    if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                        //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                        jdbcTemplate.update("UPDATE transfers set status='F',comments='Transaction failed on gateway',message='Transaction is Successfully reversed',cbs_status='R',purpose=CONCAT('REV~',purpose),txid=? where reference=?", mpesaCallback.getRECEPT(), mpesaCallback.getTRANSID());
                    } else {
                        LOGGER.info("FAILED TO PROCESS CALLBACK OF TXNS WITH REFERENCE: " + mpesaCallback.getTRANSID());
                        throw new RuntimeException("[" + CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW + "] - error http response - [" + cbsResponse + "]");
                    }
                } else {
                    LOGGER.info("Transaction has already reversed:{}", txn);
                }
            } else {
                //transaction success
                jdbcTemplate.update("UPDATE transfers set status='C',comments='Completed Successfully',message='Transaction success',txid=?,batch_reference2=?,units=? where reference=?", mpesaCallback.getRECEPT(), mpesaCallback.getRECEPT(), new BigDecimal("0.00"), mpesaCallback.getTRANSID());
                queueProducer.sendToQueueOutwardAcknowledgementToInternetBanking(txn.get(0).get("reference") + "^SUCCESS");//PROCESS CALLBACK TO IBANK

            }
        }
    }

    @JmsListener(destination = PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT, containerFactory = "queueListenerFactory")
    public void processIbankOrMobAmendmentCallback(@Payload String payloadReq,
                                                   @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        LOGGER.info("QUEUE->{}, Parameters: {}", PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT, payloadReq);
        // reference + "^" + username + "^" + comments
        String[] arrBody = payloadReq.split("\\^");
        if (arrBody.length == 4) {
            String reference = arrBody[0];
            String username = arrBody[1];
            String comments = arrBody[2];
            String returnReason = arrBody[3];
            List<Map<String, Object>> txn = getSwiftMessage(reference);
            if (txn != null) {
                if (txn.get(0).get("code").equals("IB") || txn.get(0).get("code").equals("MOB")) {
                    String reqxml = "<paymentCallback>"
                            + "<ibToken>CVAGZLHEPDMEMBEGMYXKIQQCSLXZWGRODMPKQXSXAMSSLCLBDSCQCHOVAKFWNIFBWREOEJDPDJRCEVPGYXOCIUNPMICTXKUCPJFVVUKKBKLJBIDBJZXAJEEOGDDAZSXFZSUVLJMTZUKFRQYSXRQLGONPWCLKRVLKCDZQFYRYDZBPWVHAEYUAIEGRKVNDWBHYHREPFSWX</ibToken>\n"
                            + "<reference>" + reference + "</reference>\n"
                            + "<responseCode>" + returnReason + "</responseCode>\n"
                            + "<status>amendment</status>\n"
                            + "<batchReference>" + txn.get(0).get("batch_reference") + "</batchReference>\n"
                            + "<amount>" + txn.get(0).get("amount") + "</amount>\n"
                            + "<sourceAccount>" + txn.get(0).get("sourceAcct") + "</sourceAccount>\n"
                            + "<beneficiaryAccount>" + txn.get(0).get("destinationAcct") + "</beneficiaryAccount>\n"
                            + "<settlementReference>" + txn.get(0).get("txid") + "</settlementReference>\n"
                            + "<transtype>" + txn.get(0).get("txn_type") + "</transtype>"
                            + "<units>" + txn.get(0).get("units") + "</units>"
                            + "<availableBalance>0.00</availableBalance>\n"
                            + "<ledgerBalance>0.00</ledgerBalance>\n"
                            + "<comments>" + comments + "</comments>\n"
                            + "</paymentCallback>";
                    //process the Request to CBS
                    String httpResponse = HttpClientService.sendXMLRequest(reqxml, txn.get(0).get("callbackurl") + "");
                    if (httpResponse.contains("PaymentCallbackResp") && httpResponse.contains("responseCode")) {
                        //do nothing i.e callback reached successfully.
                    } else {
                        throw new RuntimeException("[" + PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT + "] - error http response - [" + httpResponse + "]");
                    }
                } else {
                    LOGGER.info("QUEUE->{}, Tried to notify third part application but failed because transaction source is neither IB nor MOB", PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT);

                }
            }
        }
    }

    /*
     * CHECK IF THE ACCOUNT IS GEPG ACCOUNT
     */
    public int isGePGaccount(String accountNo, String currency) {
        int result = 404;
        if (accountNo == null) {
            return 404;
        }

        if (accountNo.contains("CCA0240000032") || accountNo.contains("024-0000032")
                || accountNo.contains("0240000032")
                || accountNo.contains("4600000812802")
                || accountNo.contains("4600000812801")
                || accountNo.contains("4900000494802")
                || accountNo.contains("4600000811002")
                || accountNo.contains("4600000811001")) {
            result = 0;
        } else {
            try {
                String query = "SELECT COUNT(acct_no) FROM ega_partners WHERE acct_no=? and currency=?";
                int count = this.jdbcPartnersTemplate.queryForObject(query, new Object[]{accountNo, currency}, Integer.class);
                if (count > 0) {
                    result = 0;
                }
            } catch (DataAccessException e) {
                result = -1;
                LOGGER.error("It is not GePG account: Rollback... {}", e.getMessage());
                return result;
            }
        }
        return result;
    }

    public String getCMSDestinationAcct(String accountNo, String currency) {
        String destinationAccount = "-1";
        try {
            String query = "SELECT COUNT(acct_no) FROM ega_partners WHERE acct_no=? and currency=?";
            this.jdbcPartnersTemplate.queryForObject(query, new Object[]{accountNo, currency}, Integer.class);
        } catch (DataAccessException e) {
            LOGGER.error("IT IS NOT A CMS ACCOUNT{}", e.getMessage());
        }
        return destinationAccount;
    }

    /*
     * PROCESS MT 202 INCOMING
     */
    @JmsListener(destination = PROCESS_INCOMING_MT202_MESSAGES, containerFactory = "queueListenerFactory")
    public void processMT202Incoming(@Payload String payloadReq, @Headers MessageHeaders headers, Message message, Session session) throws JMSException, IOException, ParseException {
        SwiftMessage sm = SwiftMessage.parse(payloadReq.split("\\^")[1]);
        SwiftBlock3 block3;
        Field103 field103;
        String field103Value;
        //GET MT202
        if (sm.isServiceMessage()) {
            sm = SwiftMessage.parse(sm.getUnparsedTexts().getAsFINString());
            RTGSTransferForm transferReq = new RTGSTransferForm();
            transferReq.setBeneficiaryBIC(sm.getReceiver());
            transferReq.setSenderBic(sm.getSender());

            MT202 mt = new MT202(sm);
            //DESCRIPTION
            Field72 f72 = mt.getField72();//get description
            if (f72 != null) {
                transferReq.setDescription(f72.getValue().replace("\r\n", " "));
            }
            //REFERENCE
            Field20 f20 = mt.getField20();//get transaction reference
            transferReq.setReference(f20.getValue() + "");
            Field21 f21 = mt.getField21();//get related reference
            transferReq.setRelatedReference(f21.getValue() + "");
            //AMOUNT,CURRENCY,DATE
            Field32A f32a = mt.getField32A();
            transferReq.setAmount(f32a.getAmount());//amount
            transferReq.setCurrency(f32a.getCurrency());
            transferReq.setTransactionDate(DateUtil.formatDate(f32a.getDate(), "yyMMdd", "yyyy-MM-dd"));
            //BENEFICIARY BIC
            Field58A f58a = mt.getField58A();
            String beneficiaryBic = "-1";
            if (f58a != null) {
                beneficiaryBic = f58a.getBIC();
            }
            transferReq.setBeneficiaryAccount(beneficiaryBic);
            transferReq.setSenderAccount(sm.getSender());//sender account
            transferReq.setBatchReference(f20.getValue() + "");//batch reference
            transferReq.setSenderName(sm.getSender());//sender name
            transferReq.setBeneficiaryName(beneficiaryBic);//beneficiary name
            transferReq.setSenderAddress("0");
            //check message if its FROM LOCAL OR INTERNATIONAL
            block3 = sm.getBlock3();
            String txnType;
            if (block3 != null) {
                field103 = (Field103) block3.getFieldByName("103");
                if (field103 != null) {
                    field103Value = field103.getValue();
                    if (field103Value != null && field103Value.contains("TIS")) {
                        txnType = "001";
                        //   LOGGER.info("Local Transfer Type: {}, Value: {}", field103.getName(), field103.getValue());
                    } else {
                        txnType = "004";
                    }
                } else {
                    txnType = "004";

                }
            } else {
                txnType = "004";

            }
            transferReq.setMessage("Received and being processed");
            transferReq.setResponseCode("-1");
            transferReq.setTransactionType(txnType);
            transferReq.setMessageType("202");
            transferReq.setSwiftMessage(payloadReq.split("\\^")[1]);
            transferReq.setSenderPhone("0");
            //CHECK IF ITS MT103 returned with a reason
            LOGGER.info("INSERT MT202 INTO TRANSFER TABLE: REFERENCE:{}", f20.getValue());
            queProducer.sendToQueueRTGSIncomingForLoggingToDB(transferReq);//log the transaction into CILANTRO DATABASE
            LOGGER.info("GOING TO CHECK IF ITS RETURNED MT103: REFERENCE:{} RELATED REFERENCE:{}, AMOUNT: {}", f20.getValue(), f21.getValue(), f32a.getAmount());
            List<Map<String, Object>> txn = getSwiftMessage(f21.getValue());
            if (!txn.isEmpty()) {
                //REFUND THE TRANSACTION TO CUSTOMER
                queProducer.sendToQueueMT103RefundToCustomer(f21.getValue() + "^" + f72.getValue().replace("\r\n", " ") + "^" + transferReq.getAmount() + "^" + txn.get(0).get("amount"));
            }
        }
    }

    @JmsListener(destination = PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER, containerFactory = "queueListenerFactory")
    public void processMT103RefundToCustomer(@Payload String req, @Headers MessageHeaders headers, Message message, Session session) {
        List<Map<String, Object>> txn = getSwiftMessage(req.split("\\^")[0]);
        LOGGER.info("Receives at {} payload {}", PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER, req);

        String incomingAmount = "0";
        if (req.length() > 2) {
            incomingAmount = req.split("\\^")[2];
        }
        String loggedAmount = "0";
        if (req.length() > 3) {
            loggedAmount = req.split("\\^")[3];
        }

        LOGGER.info("parsed at incomingAmount:{},  loggedAmount:{}", incomingAmount, loggedAmount);

        BigDecimal bigIncomingAmount = new BigDecimal(incomingAmount);
        BigDecimal bigLoggedAmount = new BigDecimal(loggedAmount);

        if (bigIncomingAmount.compareTo(bigLoggedAmount) == 0) {
            if (!txn.isEmpty()) {
                String identifier = "api:postGLToDepositTransfer";
                String purpose = txn.get(0).get("purpose") + "";
                String direction = txn.get(0).get("direction") + "";
                if (purpose.contains("REV~")) {
                    LOGGER.info("TRANSACTION ALREADY REVERSED : trans Reference {} REASON: {}", req.split("\\^")[0], req.split("\\^")[1]);
                } else if (direction.equalsIgnoreCase("incoming")) {
                    LOGGER.info("TRANSACTION IS INCOMING AND IT CAN NOT BE REVERSED : trans Reference {} REASON: {}", req.split("\\^")[0], req.split("\\^")[1]);
                    jdbcTemplate.update("UPDATE transfers set status='C',cbs_status='C',code='SWIFT',comments='Incoming transaction can not be reversed',message=? where txid=?", " Transaction is incoming Orignal Ref: " + req.split("\\^")[0], req.split("\\^")[0]);
                } else {
                    TxRequest transferReq = new TxRequest();
                    transferReq.setReference(txn.get(0).get("reference") + "   ");
                    transferReq.setValueDate(txn.get(0).get("value_date ") + "");
                    transferReq.setAmount(new BigDecimal(txn.get(0).get("amount").toString()));
                    transferReq.setNarration("REV~ " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + " Reason: " + req.split("\\^")[1]);
                    transferReq.setCurrency(txn.get(0).get("currency") + "");
                    if (txn.get(0).get("txn_type").equals("002") || txn.get(0).get("txn_type").equals("001") || txn.get(0).get("txn_type").equals("003")) {
                        transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                    }
                    if (txn.get(0).get("txn_type").equals("004")) {
                        transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER.replace("***", txn.get(0).get("branch_no").toString()));
                        //TODO: CHANGING GL BASED ON CURRENCY.
                        switch ((String) txn.get(0).get("currency")) {
                            case "USD":
                            case "GBP":
                                transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                                break;
                            case "KES":
                            case "UGX":
                                transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);// BOTNostroAccount);
                                break;
                            case "EUR":
                                transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);// BHF NOSTRO ACCOUNT;
                                break;
                            case "ZAR":
                                transferReq.setDebitAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                                break;
                            default:
                                transferReq.setDebitAccount("0-000-00-0000-0000000");
                                break;
                        }
                    }
                    if (StringUtils.countMatches(txn.get(0).get("sourceAcct").toString(), "-") >= 4) {
                        identifier = "api:postGLToGLTransfer";
                    }
                    transferReq.setCreditAccount(txn.get(0).get("sourceAcct").toString());
                    transferReq.setUserRole(systemVariable.apiUserRole(txn.get(0).get("branch_no") + ""));
                    PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                    postOutwardReq.setRequest(transferReq);
                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                    //process the Request to CBS
                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
                    if (cbsResponse == null) {
                        LOGGER.info("TRANSACTION REVERSED SUCCESSFULLY : trans Reference {} REASON: {}", req.split("\\^")[0], req.split("\\^")[1]);
                        //do not update the transaction status
                        LOGGER.info("[{}] - error http response - {} ", PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT, null);
                        throw new RuntimeException("[" + PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT + "] - error http response - [" + null + "]");
                    }
                    if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                        //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
                        String purpose2 = "REV~ " + txn.get(0).get("purpose") + " B/O " + txn.get(0).get("beneficiaryName") + " Reason: " + req.split("\\^")[1];
                        jdbcTemplate.update("UPDATE transfers set status='RS',comments='Transaction reversed successfuly',message=? where reference=?", purpose2, req.split("\\^")[0]);
                        jdbcTemplate.update("UPDATE transfers set status='C',cbs_status='C',code='IB',comments='Transaction Processed successfuly',message=? where txid=?", cbsResponse.getMessage() + " Refunded to customer Orignal Ref: " + req.split("\\^")[0], req.split("\\^")[0]);
                        //SEND CALLBACK TO MOBILE APP & INTERNET BANKING
                        String reference = req.split("\\^")[0];
                        String returnReason = req.split("\\^")[1];
                        if (txn.get(0).get("callbackurl") != null) {
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.split("\\^")[0] + "^" + returnReason);
                        }

                    } else {
                        LOGGER.info("FAILED TO PROCESS REVERSAL OF TXNS WITH REFERENCE: " + req.split("\\^")[0]);
                        throw new RuntimeException("[" + PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT + "] - error http response - [" + cbsResponse + "]");
                    }
                }
            }
        } else {
            LOGGER.info("FAILED TO PROCESS amounts are not equals:-> incomingAmount: {}, loggedAmount: {} ", incomingAmount, loggedAmount);
        }
    }

    @JmsListener(destination = PROCESS_INCOMING_RTGS_TO_CILANTRODB, containerFactory = "queueListenerFactory")
    public void insertIncomingRTGSToLocalDB(@Payload RTGSTransferForm req, @Headers MessageHeaders headers, Message message, Session session) {
        jdbcTemplate.update("INSERT INTO transfers(value_date,swift_message,message_type,txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact, senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId, batch_reference, batch_reference2, code, status, response_code, purpose, direction, initiated_by, branch_approved_by, hq_approved_by, create_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status, message,callbackurl,corresponding_bic) VALUES  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON duplicate key update message=?",
                req.getTransactionDate(), req.getSwiftMessage(), req.getMessageType(), req.getTransactionType(), req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getCurrency(), req.getBeneficiaryName(), req.getBeneficiaryBIC(), req.getBeneficiaryContact(), req.getSenderBic(), req.getSenderPhone(), req.getSenderAddress(), req.getSenderName(), req.getReference(), req.getRelatedReference(), req.getReference(), req.getBatchReference(), req.getBatchReference(), " ", "C", req.getResponseCode(), req.getDescription(), "INCOMING", "SYSTEM", "SYSTEM", "SYSTEM", DateUtil.now(), DateUtil.now(), DateUtil.now(), "000", "F", req.getMessage(), "", req.getCorrespondentBic(), req.getMessage());
    }

    //    /*
//    PROCESS MT103 MESSAGE TO CUSTOMER ACCOUNT
//     */
//    @JmsListener(destination = PROCESS_INCOMING_MT202_MESSAGES, containerFactory = "queueListenerFactory")
//    public void processMT103Incoming(@Payload String payloadReq, @Headers MessageHeaders headers, Message message, Session session) throws JMSException, IOException, ParseException {
//        SwiftMessage sm = SwiftMessage.parse(payloadReq.split("\\^")[1]);
//        SwiftBlock3 block3 = null;
//        Field103 fied103 = null;
//        String fied103Value = null;
//        //GET MT202
//        if (sm.isServiceMessage()) {
//            sm = SwiftMessage.parse(sm.getUnparsedTexts().getAsFINString());
//            RTGSTransferForm transferReq = new RTGSTransferForm();
//            transferReq.setBeneficiaryBIC(sm.getReceiver());
//            transferReq.setSenderBic(sm.getSender());
//
//            MT103 mt = new MT103(sm);
//            //DESCRIPTION
//            Field72 f72 = mt.getField72();//get description
//            if (f72 != null) {
//                transferReq.setDescription(f72.getValue().toString().replace("\r\n", " "));
//            }
//            //REFERENCE
//            Field20 f20 = mt.getField20();//get transaction reference
//            transferReq.setReference(f20.getValue() + "");

    /// /            Field21 f21 = mt.getField21();//get related reference
//            String relatedReference=f20.getValue() + "";
//            if (f21 != null) {
//                relatedReference=f21.getValue()+"";
//            }
//            transferReq.setRelatedReference(relatedReference);//RELATED REFERENCE
//            //AMOUNT,CURRENCY,DATE
//            Field32A f32a = mt.getField32A();
//            transferReq.setAmount(f32a.getAmount());//amount
//            transferReq.setCurrency(f32a.getCurrency());//currency
//            transferReq.setTransactionDate(DateUtil.formatDate(f32a.getDate(), "yyMMdd", "yyyy-MM-dd"));
//            //BENEFICIARY BIC
//            Field58A f58a = mt.getField58A();
//            String beneficiaryBic = "-1";
//            if (f58a != null) {
//                beneficiaryBic = f58a.getBIC();
//            }
//            transferReq.setBeneficiaryAccount(beneficiaryBic);
//            transferReq.setSenderAccount(sm.getSender());//sender account
//            transferReq.setBatchReference(f20.getValue() + "");//bartch reference
//            transferReq.setSenderName(sm.getSender());//sender name
//            transferReq.setBeneficiaryName(beneficiaryBic);//beneficiary name
//            transferReq.setSenderAddress("0");
//            //check message if its FROM LOCAL OR INTERNATIONAL
//            block3 = sm.getBlock3();
//            String txnType = "-1";
//            if (block3 != null) {
//                fied103 = (Field103) block3.getFieldByName("103");
//                if (fied103 != null) {
//                    fied103Value = fied103.getValue();
//                    if (fied103Value != null && fied103Value.contains("TIS")) {
//                        txnType = "001";
//                        //   LOGGER.info("Local Transafer Type: {}, Value: {}", fied103.getName(), fied103.getValue());
//                    } else {
//                        txnType = "004";
//                    }
//                } else {
//                    txnType = "004";
//
//                }
//            } else {
//                txnType = "004";
//
//            }
//            transferReq.setTransactionType(txnType);
//            transferReq.setMessageType("202");
//            transferReq.setSwiftMessage(payloadReq.split("\\^")[1]);
//            transferReq.setSenderPhone("0");
//            //CHECK IF ITS MT103 returned with a reason
//            LOGGER.info("INSERT MT202 INTO TRANSFER TABLE: REFERENCE:{}", f20.getValue());
//            queProducer.sendToQueueRTGSIncomingForLoggingToDB(transferReq);//log the transaction into CILANTRO DATABASE
//            LOGGER.info("GOING TO CHECK IF ITS RETURNED MT103: REFERENCE:{} RELATED REFERENCE:{}, AMOUNT: {}", f20.getValue(), f21.getValue(), f32a.getAmount());
//            List<Map<String, Object>> txn = getSwiftMessage(f21.getValue());
//            if (!txn.isEmpty() && txn != null) {
//                //REFUND THE TRANSACTION TO CUSTOMER
//                queProducer.sendToQueueMT103RefundToCustomer(f21.getValue() + "^" + f72.getValue().replace("\r\n", " "));
//            }
//        }
    @JmsListener(destination = PROCESS_STP_MT103_OUTWARD_TRANSACTIONS, containerFactory = "queueListenerFactory")
    public void processSTPMT103Outgoing(@Payload PaymentReq req, @Headers MessageHeaders headers, Message message, Session session) throws JMSException, IOException, Exception {
        //get the MESSAGE DETAILS FROM THE QUEUE
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getReference() + " ");
        transferReq.setAmount(req.getAmount());
        transferReq.setNarration(req.getDescription() + " B/O " + req.getBeneficiaryName() + " REF: " + req.getReference());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(systemVariable.TRANSFER_AWAITING_TISS_LEDGER.replace("***", req.getCustomerBranch()));
        transferReq.setCreditAccount(systemVariable.TRANSFER_MIRROR_TISS_BOT_LEDGER.replace("***", req.getCustomerBranch()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getCustomerBranch()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("ERROR ON POSTING STP 103 TRANSACTION{}", req);
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_STP_MT103_OUTWARD_TRANSACTIONS, cbsResponse);
            throw new RuntimeException("[" + PROCESS_STP_MT103_OUTWARD_TRANSACTIONS + "] - error http response - [" + cbsResponse + "]");
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            //SEND TO SWIFT MESSAGE
            List<Map<String, Object>> txn = getSwiftMessage(req.getReference());
            String swiftStatus = null;
            if (txn != null && !txn.isEmpty()) {
                String messageRequest = (String) txn.get(0).get("swift_message");
                //CHECK IF BOT TISS VPN IS ALLOWED
                if (
                    //this is for global for rtgs transaction will settle over tiss vpn
                        (systemVariables.IS_TISS_VPN_ALLOWED && req.getType().equalsIgnoreCase("001"))
                                ||
                                //this is special valued customer we do stp 24 hrs
                                (systemVariables.ALLOWED_STP_ACCOUNT_THROUGH_TISS_VPN && allowedSTPAccountRepository.existsAllowedSTPAccountsByAcctNo(req.getSenderAccount()))

                ) {
                    swiftStatus = "BOT-TISS-VPN";
                    //SEND TRANSACTION TO BOT
                    //Create signature
                    String signedRequestXML = signTISSVPNRequest(messageRequest, systemVariables.SENDER_BIC);
                    //log the swift advise message on transfer advises
                    swiftRepository.saveSwiftMessageInTransferAdvices(messageRequest, "BOT-VPN", "OUTGOING");
                    String response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariables.BOT_TISS_VPN_URL, req.getReference(), systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, systemVariables.getSysConfiguration("BOT.tiss.daily.token", "prod"), systemVariables.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH,systemVariables.PRIVATE_TISS_VPN_KEYPASS);
                    LOGGER.info("REQUEST TO BOT:{}, REPONSE FROM BOT:{}", signedRequestXML, response);
                    if (response != null && !response.equalsIgnoreCase("-1")) {
                        //get message status and update transfers table with response to IBD approval
                        String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
                        if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                            swiftRepository.updateTransferAdvicesStatus("TISSVPN_ACCEPTED", req.getReference());
                            //the message is successfully on BOT ENDPOINT
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
                        } else {
                            swiftRepository.updateTransferAdvicesStatus("TISSVPN_REJECTED", req.getReference());
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^REJECTED");
                            //
                        }
                    }//else is needed here
                } else {
                    swiftStatus = "SWIFT-SUBMITTED";
                    swiftRepository.updateTransferAdvicesStatus("SWIFT_SUBMITTED", req.getReference());
                    queProducer.sendToQueueRTGSToSwift(messageRequest + "^" + systemVariables.KPRINTER_URL + "^" + req.getReference());
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
                }

            } else {
                LOGGER.info("FAILED TO GET TRANSACTION USING DETAILS ... {}", txn);
            }

            //INSERT BATCH TRANSACTION TO EFT BATCH TABLE
            jdbcTemplate.update("UPDATE transfers set status='C',branch_approved_by='SYSTEM', hq_approved_by='SYSTEM', branch_approved_dt=?, hq_approved_dt=? ,message='Its STP Transfer to other Banks',swift_status=?  where reference=?", DateUtil.now(), DateUtil.now(), swiftStatus, req.getReference());

        } else {
            LOGGER.info("FAILED TO PROCESS BATCH: " + req.getBatchReference());
            throw new RuntimeException("[" + PROCESS_STP_MT103_OUTWARD_TRANSACTIONS + "] - error http response - [" + cbsResponse + "]");
        }
    }

    public String signTISSVPNRequest(String rawXML, String bankCode) {
        String rawXmlsigned = null;
        try {

            String signature = sign.CreateSignature(rawXML, systemVariables.PRIVATE_TISS_VPN_KEYPASS, systemVariables.PRIVATE_TISS_VPN_KEY_ALIAS, systemVariables.PRIVATE_TISS_VPN_KEY_FILE_PATH);

            rawXmlsigned = rawXML + "|" + signature;
            LOGGER.info("\nTISS VPN  SIGNED REQUEST :{}\n{}", bankCode, rawXmlsigned);
        } catch (Exception ex) {
            LOGGER.error("Generating digital signature...{}", ex.getMessage());
        }
        return rawXmlsigned;
    }

    @JmsListener(destination = PENSION_PAYROLL_BATCH_TRANSACTIONS, containerFactory = "queueListenerFactoryPensioners")
    public void processValidationOfData(@Payload PsssfBatchRequest req, @Headers MessageHeaders headers, Message message, Session session) {
        int i = 0;
        for (PsssfBatchBeneficiary beneficiary : req.getBeneficiaries()) {
            String payload = beneficiary.getACCOUNT() + "^" + beneficiary.getNAME() + "^" + beneficiary.getID() + "^" + req.getBatchId() + "^" + beneficiary.getPENSIONER_ID() + "^" + i;
            LOGGER.info("TRACKING LOOP PRODUCED TO QUEUE:{}", i);
            queueProducer.sendToQueuePsssfPensionerAccountVerification(payload);
            i++;
            //String sql = " SELECT AC.ACCT_NO,AC.ACCT_NM,CUR.CRNCY_CD_ISO AS CURRENCY_CODE,AC.REC_ST,ASR.REF_DESC FROM ACCOUNT AC JOIN CURRENCY CUR ON AC.CRNCY_ID = CUR.CRNCY_ID JOIN ACCOUNT_STATUS_REF ASR ON AC.REC_ST =ASR.REF_KEY WHERE (AC.OLD_ACCT_NO = ? OR AC.ACCT_NO=?) AND ROWNUM = 1";
        }
    }

    @JmsListener(destination = PENSION_PAYROLL_PENSIONER_ACCOUNT_VERIFICATION, containerFactory = "queueListenerFactoryPensioners")
    public void validatePensionPayrollData(@Payload String req, @Headers MessageHeaders headers, Message message, Session session) {
        String account = req.split("\\^")[0];
        String accountName = req.split("\\^")[1];
        String trackingNo = req.split("\\^")[2];
        String batchRef = req.split("\\^")[3];
        String pensionerId = req.split("\\^")[4];
        String trackingSequence = req.split("\\^")[5];
        String cbsName = "-1";
        String resMessage = "No saving account";
        String status = "F";
        String respCode = "53";
        double percentageMatch = 0L;
        String sql = "SELECT c.CUST_NM, AC.ACCT_NO,AC.ACCT_NM, CUR.CRNCY_CD_ISO AS CURRENCY_CODE, AC.REC_ST,ASR.REF_DESC FROM  ACCOUNT AC JOIN CURRENCY CUR ON AC.CRNCY_ID = CUR.CRNCY_ID JOIN ACCOUNT_STATUS_REF ASR ON  AC.REC_ST = ASR.REF_KEY JOIN CUSTOMER c ON c.CUST_ID=AC.CUST_ID WHERE (REPLACE(AC.OLD_ACCT_NO, '-', '') = ?   OR AC.ACCT_NO = ?)   AND ROWNUM = 1";
        List<Map<String, Object>> getData = this.jdbcRUBIKONTemplate.queryForList(sql, account.replace("-", ""), account.replace("-", ""));
        if (!getData.isEmpty()) {
            percentageMatch = namesMatch(accountName.toLowerCase().trim(), String.valueOf(getData.get(0).get("ACCT_NM")).toLowerCase().trim()) * 100;

            cbsName = String.valueOf(getData.get(0).get("CUST_NM")).toUpperCase().trim();
            resMessage = "Account exist:" + getData.get(0).get("ACCT_NO") + " ACCOUNT NAME:" + getData.get(0).get("ACCT_NM");
            if (String.valueOf(getData.get(0).get("REC_ST")).equalsIgnoreCase("A") || String.valueOf(getData.get(0).get("REC_ST")).equalsIgnoreCase("D")) {
                respCode = "0";
                status = "I";
                resMessage = "success";
            } else {
                respCode = "0";
                status = "F";
                resMessage = String.valueOf(getData.get(0).get("REF_DESC"));
            }

        }
        //UPDATE LOCAL DATABASE & NOTIFY PSSSF
        String reqToUpdate = account + "^" + accountName + "^" + trackingNo + "^" + batchRef + "^" + pensionerId + "^" + cbsName + "^" + resMessage + "^" + status + "^" + respCode + "^" + percentageMatch + "^" + trackingSequence;
        queueProducer.sendToQueuePsssfPensionerUpdateRecordsPerCbs(reqToUpdate);
    }

    @JmsListener(destination = PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE, containerFactory = "queueListenerFactoryPensioners")
    public void processUpdateOfPayrollPernsionerData(@Payload String req, @Headers MessageHeaders headers, Message message, Session session) {
        String account = req.split("\\^")[0];
        String accountName = req.split("\\^")[1];
        String trackingNo = req.split("\\^")[2];
        String batchRef = req.split("\\^")[3];
        String pensionerId = req.split("\\^")[4];
        String cbsName = req.split("\\^")[5];
        String resMessage = req.split("\\^")[6];
        String status = req.split("\\^")[7];
        String respCode = req.split("\\^")[8];
        String percentageMatch = req.split("\\^")[9];
        String trackingSequence = req.split("\\^")[10];
//       UPDATE PENSION PAYROLL
        try {
            String sql = "UPDATE pensioners_payroll set cbs_name=?,percentage_match=?,message=?,comments=?,status='V',cbs_status=? where batchReference=? and account=?";
            jdbcTemplate.update(sql, cbsName, percentageMatch, resMessage, resMessage, status, batchRef, account);
            LOGGER.info("TRACKING NO:" + trackingSequence + " >>>" + sql.replace("?", "'{}'"), cbsName, percentageMatch, resMessage, resMessage, status, trackingNo, batchRef, account);
        } catch (DataAccessException ex) {
            throw new RuntimeException("[" + PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE + "] - error http response - [" + ex.getMessage() + "]");
        }
    }

    //    @JmsListener(destination = PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING, containerFactory = "queueListenerFactoryPensioners")
//    public void processPensionPayrollToCoreBanking(@Payload PensionPayrollToCoreBanking pensionPayroll, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
//        LOGGER.info("Receives batch reference:{} to process to cbs", pensionPayroll.getBatchReference());
//        if (pensionPayroll.getBatchReference() != null) {
//            List<Map<String, Object>> txn = getPensionPayrollEntries(pensionPayroll.getBatchReference());
//            List<PensionPayrollToCoreBanking> pensionPayrollList = new ArrayList<>();
//            if (txn != null && !txn.isEmpty()) {
//                for (int i = 0; i < txn.size(); i++) {
//                    PensionPayrollToCoreBanking pdata = new PensionPayrollToCoreBanking();
//                    pdata.setAmount(new BigDecimal(txn.get(i).get("amount") + ""));
//                    pdata.setBeneficiaryAccount(txn.get(i).get("account") + "");
//                    pdata.setCurrency(txn.get(i).get("currency") + "");
//                    pdata.setNarration(txn.get(i).get("description") + "");
//                    pdata.setReference(txn.get(i).get("bankReference").toString());
//                    pdata.setUserRoles(pensionPayroll.getUserRoles());
//                    pdata.setTrackingNo(txn.get(i).get("trackingNo") + "");
//                    pdata.setBeneficiaryName(txn.get(i).get("name") + "");
//                    pdata.setBatchReference(txn.get(i).get("batchReference") + "");
//                    pensionPayrollList.add(pdata);
//                    LOGGER.info("Loop Size: {}, Loop: {} with reference:{}", txn.size(), i + 1, txn.get(i).get("bankReference").toString());
//                }
//            }
//            LOGGER.info("Pension payroll Size: {}, ", pensionPayrollList.size());
//            if (!pensionPayrollList.isEmpty()) {
//                for (PensionPayrollToCoreBanking item: pensionPayrollList) {
//                    String identifier = "api:postGLToDepositTransfer";
//                    TxRequest transferReq = new TxRequest();
//                    transferReq.setReference(item.getReference());
//                    transferReq.setAmount(item.getAmount());
//                    transferReq.setNarration(item.getNarration() + " B/O " + item.getBeneficiaryName() + " " + item.getTrackingNo());
//                    transferReq.setCurrency(item.getCurrency());
//                    transferReq.setDebitAccount(systemVariable.EFT_HQ_TRANSFER_AWAITING);
//                    transferReq.setCreditAccount(item.getBeneficiaryAccount());
//                    transferReq.setUserRole(item.getUserRoles());
//                    PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
//                    postOutwardReq.setRequest(transferReq);
//
//                    String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
//                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, identifier), XaResponse.class);
//                    // simulate
//                    // cbsResponse = new XaResponse();
//                    //cbsResponse.setResult(0);
//                    if (cbsResponse == null) {
//                        LOGGER.info("TRANSACTION FAILED: Trans Reference {} REASON: {}", item.getReference(), "Timeout");
//                        //do not update the transaction status
//                        LOGGER.info("[{}] - error http response - {} ", PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING, null);
//                        throw new RuntimeException("[" + PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING + "] - error http response - [" + null + "]");
//                    } else if (cbsResponse.getResult() == 0) {
//                        String pensionUpdate = item.getReference() + "^Success^0^" + item.getBeneficiaryAccount() + "^" + item.getBatchReference() + "^C";
//                        queueProducer.sendToQueuePensionPayrollUpdateTransactions(pensionUpdate);
//
//                    } else {
//                        String pensionUpdate = item.getReference() + "^" + cbsResponse.getMessage() + "^" + cbsResponse.getResult() + "^" + item.getBeneficiaryAccount() + "^" + item.getBatchReference() + "^F";
//                        queueProducer.sendToQueuePensionPayrollUpdateTransactions(pensionUpdate);
//                    }
//                }
//            }
//        }
//    }
    @JmsListener(destination = PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING, containerFactory = "queueListenerFactoryPensioners")
    public void processPensionPayrollToCoreBanking(@Payload PensionPayrollToCoreBanking pensionPayroll, @Headers MessageHeaders headers, Message message, Session session) throws JMSException {
        LOGGER.info("Receives batch reference:{} to process to cbs", pensionPayroll.getBatchReference());
        if (pensionPayroll.getBatchReference() != null) {
            List<Map<String, Object>> txn = getPensionPayrollEntries(pensionPayroll.getBatchReference());
            if (txn != null && !txn.isEmpty()) {
                for (Map<String, Object> stringObjectMap : txn) {
                    String pensionUpdate = stringObjectMap.get("bankReference") + "^Success^0^" + stringObjectMap.get("account") + "^CM";
                    queueProducer.sendToQueuePensionPayrollUpdateTransactions(pensionUpdate);
                }
            }
        }
    }

    public List<Map<String, Object>> getPensionPayrollEntries(String batchReference) {
        try {
            return this.jdbcTemplate.queryForList("select * from pensioners_payroll where batchReference=? and status='V' and cbs_status='I'", batchReference);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON pensioners_payroll TABLE: {}", ex.getMessage());
            return null;
        }
    }

    //    @JmsListener(destination = PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS, containerFactory = "queueListenerFactoryPensioners")
//    public void processPensionerPayrollUpdateAfterPayment(@Payload String req, @Headers MessageHeaders headers, Message message, Session session) {
//        String bankReference = req.split("\\^")[0];
//        String respMessage = req.split("\\^")[1];
//        String responseCode = req.split("\\^")[2];
//        String account = req.split("\\^")[3];
//        String batchReference = req.split("\\^")[4];
//        String status = req.split("\\^")[5];

    /// /       UPDATE PENSION PAYROLL TRANSACTIONS
//        try {
//            String sql = "UPDATE pensioners_payroll set message=?,status='S',responseCode=?,cbs_status=? where bankReference=? and account=?";
//            jdbcTemplate.update(sql, respMessage, responseCode, status, bankReference, account);
//            LOGGER.info("TRANSACTION UPDATE AFTER CBS: " + sql.replace("?", "'{}'"), bankReference, respMessage, responseCode, status, bankReference, account);
//        } catch (DataAccessException ex) {
//            throw new RuntimeException("[" + PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS + "] - error http response - [" + ex.getMessage() + "]");
//
//        }
//
//    }
    @JmsListener(destination = PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS, containerFactory = "queueListenerFactoryPensioners")
    public void processPensionerPayrollUpdateAfterPayment(@Payload String req, @Headers MessageHeaders headers, Message message, Session session) {
        String bankReference = req.split("\\^")[0];
        String respMessage = req.split("\\^")[1];
        String responseCode = req.split("\\^")[2];
        String account = req.split("\\^")[3];
        String status = req.split("\\^")[4];
//       UPDATE PENSION PAYROLL TRANSACTIONS
        try {
            String sql = "UPDATE pensioners_payroll set message=?,status='S',responseCode=?,cbs_status=? where bankReference=? and account=?";
            jdbcTemplate.update(sql, respMessage, responseCode, status, bankReference, account);
            LOGGER.info("TRANSACTION UPDATE AFTER CBS: " + sql.replace("?", "'{}'"), respMessage, responseCode, status, bankReference, account);
        } catch (DataAccessException ex) {
            throw new RuntimeException("[" + PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS + "] - error http response - [" + ex.getMessage() + "]");
        }
    }

    /*
    PROCESS STP PAYMENTS TO CORE BANKING
     */
    @JmsListener(destination = INCOMING_SWIFT_STP_INWARD_TRANSACTION, containerFactory = "queueListenerFactoryIncomingSTP")
    public void processRTGSRemittanceToCoreBanking(@Payload RTGSTransferForm req, @Headers MessageHeaders headers, Message message, Session session) {
        boolean processPayment = true;
        boolean checkGepg = false;
        LOGGER.info("BANEFICIARY ACCOUNT:{}", req.getBeneficiaryAccount());
        if (req.getBeneficiaryAccount() == null) {
            req.setBeneficiaryAccount("");
        }
        int result;
        try {
            String contraAcct = "0-000-0000-0000-000000";
            String chargeScheme;
            //generate the request to RUBIKON

            /*
            GET THE CHARGING SCHEME
             */
            if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getBeneficiaryAccount())) {//waived accounts
                chargeScheme = "T99";
            } else {
                chargeScheme = "T02";
            }
            if (req.getTransactionType().equalsIgnoreCase("001")) {
                chargeScheme = "T100";
                contraAcct = systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER;
            } else if (req.getTransactionType().equalsIgnoreCase("004")) {
                if (req.getSenderBic() != null) {
                    LOGGER.info("req.getCurrency():{}", req.getCurrency());
                    LOGGER.info("req.getSenderBic():{}", req.getSenderBic());
                    LOGGER.info("req.getCorrespondentBic():{}", req.getCorrespondentBic());
                    if (req.getCorrespondentBic() == null) {
                        req.setCorrespondentBic("");
                    }
                    if ((req.getCurrency() != null && req.getCurrency().equalsIgnoreCase("USD") && req.getSenderBic().contains("SCBL")) || req.getCorrespondentBic().contains("SCBLUS")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getBeneficiaryAccount())) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER;
                    } else if ((req.getCurrency().equalsIgnoreCase("USD") && req.getSenderBic().contains("BHFB")) || req.getCorrespondentBic().contains("BHFB")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getBeneficiaryAccount())) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER;
                    } else if (req.getCurrency().equalsIgnoreCase("EUR")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getBeneficiaryAccount())) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER;
                    } else if (req.getCurrency().equalsIgnoreCase("ZAR")) {
                        if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getBeneficiaryAccount())) {//waived accounts
                            chargeScheme = "T99";
                        } else {
                            chargeScheme = "T02";
                        }
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER;
                    } else {
                        contraAcct = "0-000-000-00000-000000";

                    }
                }
            }

            //setCharge Scheme
            req.setChargeDetails(chargeScheme);
            //CHECK IF ITS A GePG ACCOUNT
            LOGGER.info("GOING TO CHECK IF ACCOUNT is GePG account:{}", req.getBeneficiaryAccount());
            //check TTCL expenditure acct and substitute with collection account (TZS)
            if (req.getBeneficiaryAccount().equalsIgnoreCase("420400000088") && (req.getDescription().contains("99494"))) {
                req.setBeneficiaryAccount("4202080000001");
            }
            //check TTCL expenditure acct and substitute with collection account (USD)

            if (req.getBeneficiaryAccount().equalsIgnoreCase("420428000036") && (req.getDescription().contains("99494"))) {
                req.setBeneficiaryAccount("420242000001");
            }
            if (systemVariables.TCB_STAWI_BOND_ACCOUNTS.contains(req.getBeneficiaryAccount())) {
                LOGGER.info("Stawi Bond detected. Beneficiary Account: {}", req.getBeneficiaryAccount());

                final String cdsNumber = extractCDSNumber(nullToEmpty(req.getDescription()));
                if (!isCDSNumber(cdsNumber)) {
                    jdbcTemplate.update(
                            "update transfers set status='C',response_code='445',cbs_status='F'," +
                                    "comments='Stawi Bond purchase detected; invalid CDS number provided'," +
                                    "branch_approved_by=?,branch_approved_dt=? where reference=?",
                            "SYSTEM", DateUtil.now(), req.getReference()
                    );
                    return; // <-- STOP here
                }

                StawiBondLookupResponse lookup = stawiBondNotificationClient.lookup(cdsNumber);
                if (lookup == null || !"00".equals(nullToEmpty(lookup.getResponseCode()))) {
                    jdbcTemplate.update(
                            "update transfers set status='C',response_code='444',cbs_status='F'," +
                                    "comments='Stawi Bond purchase detected; CDS verification failed'," +
                                    "branch_approved_by=?,branch_approved_dt=? where reference=?",
                            "SYSTEM", DateUtil.now(), req.getReference()
                    );
                    return; // <-- STOP here
                }
                double distance = namesMatch(req.senderName,lookup.getResponse().getInvestorName()) * 100;
                if (distance <78){
                    jdbcTemplate.update(
                            "update transfers set status='C',response_code='444',cbs_status='F'," +
                                    "comments='Stawi Bond purchase detected; Name Verification failed'," +
                                    "branch_approved_by=?,branch_approved_dt=? where reference=?",
                            "SYSTEM", DateUtil.now(), req.getReference()
                    );
                    return;
                }

                StawiBondNotificationRequest body = new StawiBondNotificationRequest();
                body.setCustomerName(nullToEmpty(lookup.getResponse().getInvestorName()));
                body.setChannel("RTGS");
                body.setReference(nullToEmpty(req.getReference()));
                body.setCurrency(nullToEmpty(req.getCurrency()));
                body.setNarration(nullToEmpty(req.getDescription()));
                body.setAmount(req.getAmount());
                body.setSourceAccount(contraAcct);
                body.setDseAccount(cdsNumber);
                body.setPhoneNumber(lookup.getResponse() != null ? nullToEmpty(lookup.getResponse().getInvestorPhoneNumber()) : "");

                ResponseEntity<StawiBondLookupResponse> res = stawiBondNotificationClient.send(body);

                String rc = (res != null && res.getBody() != null) ? nullToEmpty(res.getBody().getResponseCode()) : "998";
                if ("00".equals(rc)) {
                    jdbcTemplate.update(
                            "update transfers set status='C',response_code='0',cbs_status='C',comments='Success'," +
                                    "branch_approved_by=?,branch_approved_dt=? where reference=?",
                            "SYSTEM", DateUtil.now(), req.getReference()
                    );
                    return; // <-- STOP here
                } else {
                    jdbcTemplate.update(
                            "update transfers set status='C',response_code=?,cbs_status='F'," +
                                    "comments='Stawi Bond purchase: failed during Lavender posting'," +
                                    "branch_approved_by=?,branch_approved_dt=? where reference=?",
                            rc, "SYSTEM", DateUtil.now(), req.getReference()
                    );
                    return; // <-- STOP here
                }
            }


            int isGePGaccountResult = isGePGaccount(req.getBeneficiaryAccount(), req.getCurrency());
            if (isGePGaccountResult == 0) {
                //ITS A GePG TRANSACTION
                LOGGER.info("BEN ACCOUNT IS GePG ACCOUNT:{}", req.getBeneficiaryAccount());
                result = sendPostToSwiftGePGAPI(req, contraAcct, chargeScheme);
                if (result == 0) {
                    //update transfer table set transaction as successfully posted to gepg
                    jdbcTemplate.update("update transfers set status='C',response_code='0',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", "SYSTEM", DateUtil.now(), req.getReference());
                } else {
                    //update transfer table set transaction as unsuccessfully  posted to gepg
                    jdbcTemplate.update("update transfers set status='C',response_code='0',cbs_status='F',comments='Failed to process payment to GePG',branch_approved_by=?,branch_approved_dt=? where  reference=?", "SYSTEM", DateUtil.now(), req.getReference());
                }
            } else {
                LOGGER.info("GOING TO CHECK IF ACCOUNT isCMSAccount:{}", req.getBeneficiaryAccount());
                if (isCMSAccount(req.getBeneficiaryAccount()) == 0) {//check if its cms account
                    if (req.getBeneficiaryAccount().equals("170227000078")) {
                        LOGGER.info("It is AIRTEL MOBILE COMMERCE TRUST ACCOUNT {}", req.getBeneficiaryAccount());
                        //170227000078
                        //AIRTEL MOBILE COMMERCE (T) LTD;
                        result = airtelTrustAgentNamequery(req);
                        //if name query exists process the payment to core banking
                        if (result == 0) {
                            //process the transaction to core banking then process to airtel
                            String agentNo = req.getDescription().replaceAll("\\D+", "");
                            agentNo = filterTILLNO(agentNo);
                            String trxNarration = "TILL:" + agentNo + " DPS:" + req.getSenderName() + " DTL:Being payment of float";
                            req.setDescription(trxNarration);
                            //process core banking
                            int cbsResult = processInwardRTGSToCBS(req);
                            if (cbsResult == 0) {//success on core banking post on airtel trust account
                                result = processAirtelTrustAccount(req);
                                //TRANSACTION POSTED BOTH AIRTEL AND CORE BANKING
                                if (result == 0) {
                                    //UPDATE as success
                                    jdbcTemplate.update("update transfers set message='[FLOAT PURCHASED SUCCESSFULLY] and transaction posted on both CBS and AIRTEL',status='C',cbs_status='C',comments='[FLOAT PURCHASED SUCCESSFULLY] and transaction posted on both CBS and AIRTEL',branch_approved_by='SYSTEM',branch_approved_dt=?,response_code=?  where  reference=?", DateUtil.now(), "0", req.getReference());
                                    jdbcTemplate.update("update transfer_advices set cbsMessage='[FLOAT PURCHASED SUCCESSFULLY] and transaction posted on both CBS and AIRTEL',status='C',cbsStatus='C'  where  senderReference=?", req.getReference());

                                } else {
                                    //update as failed
                                    jdbcTemplate.update("update transfers set message='[FAILED TO PURCHASE FLOAT] transaction posted on core baking but failed to Purchase float on AIRTEL SIDE',status='C',cbs_status='F',comments='[FAILED TO PURCHASE FLOAT] transaction posted on core baking but failed to Purchase float on AIRTEL SIDE',branch_approved_by='SYSTEM',branch_approved_dt=?,response_code=?  where  reference=?", DateUtil.now(), result, req.getReference());
                                    //FA FAILED TO PURCHASE FLOAT ON AIRTEL
                                    jdbcTemplate.update("update transfer_advices set cbsMessage='[FAILED TO PURCHASE FLOAT] transaction posted on core baking but failed to Purchase float on AIRTEL SIDE',status='C',cbsStatus='F'  where  senderReference=?", req.getReference());
                                }
                            } else {
                                //transaction failed on core banking
                                jdbcTemplate.update("update transfer_advices set cbsMessage='It is meant for purchase of float on AIRTEL TRUST Account',status='C',cbsStatus='C'  where  senderReference=?", req.getReference());
                                jdbcTemplate.update("update transfers set message='It is meant for purchase of float on AIRTEL TRUST Account',status='C',cbs_status='F',comments='It is meant for purchase of float on AIRTEL TRUST Account',branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?  where  reference=?", cbsResult, DateUtil.now(), req.getReference());
                            }
                        } else {
                            jdbcTemplate.update("update transfer_advices set cbsMessage='It is meant for purchase of float on AIRTEL TRUST Account',status='F',cbsStatus='F'  where  senderReference=?", req.getReference());
                            //transaction failed validation on airtel, log as failed and do not process to core banking
                            jdbcTemplate.update("update transfers set message='[ TILL DOESNOT EXIST] It is meant for purchase of float on AIRTEL TRUST Account',status='C',cbs_status='F',comments='[ TILL DOESNOT EXIST] It is meant for purchase of float on AIRTEL TRUST Account',branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?  where  reference=?", result, DateUtil.now(), req.getReference());
                        }
                    } else {
                        //transaction is blocked because its a cms account which requires control number
                        jdbcTemplate.update("update transfer_advices set cbsMessage='It is meant for purchase of float on AIRTEL TRUST Account',status='B',cbsStatus='B'  where  senderReference=?", req.getReference());
                        LOGGER.info("It is CMS partner and blocked to STP: {}", req.getBeneficiaryAccount());
                        jdbcTemplate.update("update transfers set message='[ ITS CMS ACCOUNT IT REQUIRES CONTROL NO] It is meant for cash collection',status='C',cbs_status='F',comments='[ ITS CMS ACCOUNT IT REQUIRES CONTROL NO] It is meant for cash collection',branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?  where  reference=?", "899", DateUtil.now(), req.getReference());
                    }
                } else {
                    //process transaction as normal incoming to core banking
                    String description = "-1";
                    if (req.getDescription() != null) {
                        description = req.getDescription().replaceAll("\\D+", "");
                    }

                    if (req.getBeneficiaryAccount().startsWith("999")) {
                        //Check receiver account if it is cms account.
                        TpbPartnerReference cmsReference = validateCMSReference(req.getBeneficiaryAccount());
                        if (cmsReference != null) {
                            //assign cms account
                            req.setBeneficiaryAccount(cmsReference.getProfileID());
                            //build cms narrations
                            req.setDescription("REF:" + cmsReference.getReference() + " DPS:" + req.getSenderName() + " CST:" + cmsReference.getName() + " DTL:" + req.getDescription());
                        }
                    }
                    if (description.startsWith("999")) {
                        //Check receiver account if it is cms account.
                        TpbPartnerReference cmsReference = validateCMSReference(description);
                        if (cmsReference != null) {
                            //assign cms account
                            req.setBeneficiaryAccount(cmsReference.getProfileID());
                            //build cms narrations
                            req.setDescription("REF:" + cmsReference.getReference() + " DPS:" + req.getSenderName() + " CST:" + cmsReference.getName() + " DTL:" + req.getDescription());
                        }
                    }
                    LOGGER.info("beneficiary account check for cashme ...{}", req.getBeneficiaryAccount());

                    String cashMeAcctCheck = req.getBeneficiaryAccount().toUpperCase();
                    String cashMeDescCheck = description.toUpperCase();

//                    String patternStr = "\\b\\w*CM99\\w*\\b"; // Regex pattern to match any word containing "CM99"
//
//                    Pattern pattern = Pattern.compile(patternStr);
//                    Matcher matcher = pattern.matcher(input);
//
//                    while (matcher.find()) {
//                        String match = matcher.group();
//                        System.out.println("Match found: " + match);
//                    }

                    if (cashMeDescCheck.startsWith("CM99") || cashMeAcctCheck.startsWith("CM99")) {
                        //this is cashme
                        String cashMeCtrlNo = req.getDescription().toUpperCase().startsWith("CM999") ? req.getDescription().toUpperCase() : req.getBeneficiaryAccount().toUpperCase();
                        LOGGER.info("checking the control number from desc ... {}, from benAcct ... {}", req.getDescription(), req.getBeneficiaryAccount());
                        CashMeLookupResp cashMeReference = validateCashMeCtrNo(cashMeCtrlNo);
                        LOGGER.info("CASH ME LOOKUP RESP ... {}", cashMeReference);
                        if (cashMeReference != null) {
                            //assign cms account
                            req.setBeneficiaryAccount("182227000017");
                            //build cms narrations
                            req.setDescription("REF:" + cashMeReference.getTrxnRef() + " DPS:" + req.getSenderName() + " CST:" + cashMeReference.getAcctName() + " DTL:" + req.getDescription());
                        }
                    }
                    //process payment to core banking0000
                    //before posting validate beneficiary account
//                    String accountNameQuery;
                    AccountNameQuery nameQuery = corebanking.accountNameQuery(req.getBeneficiaryAccount());//.toUpperCase();//get account Name

                    JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
                    // Calculate similarity without considering case sensitivity
                    double distance = namesMatch(req.getBeneficiaryName().toLowerCase().trim(), nameQuery.getAccountName().toLowerCase().trim()) * 100;

                    LOGGER.info("beneficiary incoming name:{} dbName:{}, distance:{}", req.getBeneficiaryName().toLowerCase().trim(), nameQuery.getAccountName().toLowerCase().trim(), distance);
                    String message1;
                    String responseCode;
                    if (distance > 78) {
                        if (req.getCurrency().equalsIgnoreCase("TZSF") && !nameQuery.getAccountCurrency().equalsIgnoreCase("TZS")) {
                            responseCode = "888";
                            message1 = "Transaction is not allowed from TZS to other currencies. Please Return or communicate to Tresuary";
                            jdbcTemplate.update("update transfers set status='C',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  reference=? ", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getReference());
                        } else {
                            //PROCESS TO CORE BAKING
                            int resultCbs = processInwardRTGSToCBS(req);
                            if (resultCbs == 0) {
                                jdbcTemplate.update("update transfers set message='Success',status='C',cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?  where  reference=?", DateUtil.now(), req.getReference());
                                jdbcTemplate.update("update transfer_advices set cbsMessage='Success',status='C',cbsStatus='C'  where  senderReference=?", req.getReference());

                                if (cashMeAcctCheck.startsWith("CM99") || cashMeDescCheck.startsWith("CM99")) {
                                    //notify cash me
                                    String payload = "<paymentReq><responseCode>0</responseCode><responseMessage>Success</responseMessage><collAcctNo>" + cashMeAcctCheck + "</collAcctNo><collAcctName>" + req.getBeneficiaryName() + "</collAcctName><custAcctNo>" + req.getSenderAccount() + "</custAcctNo><custName>" + req.getSenderName() + "</custName><trxnType>CR</trxnType> <trxnDate>" + DateUtil.now() + "</trxnDate><currency>TZS</currency><amount>" + req.getAmount() + "</amount><details>deposit</details><trxnRef>" + req.getReference() + "</trxnRef><depositorName>" + req.getSenderName() + "</depositorName></paymentReq>";
                                    HttpClientService.sendXMLRequest(payload, systemVariable.KIPAYMENT_CASHME_DEPOSIT_NOTIFY_URL);
                                    //TODO ..insert into banks_transaction
                                    String sql = "EXEC bank_insert_transaction ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
                                    LOGGER.info(sql.replace("?", "'{}'"), req.getBeneficiaryAccount(), req.getSenderAccount(), "CILANTRO SYSTEM", "C", req.getAmount(), req.getBeneficiaryName(), "0", req.getCurrency(), req.getSenderPhone(), req.getSenderName(), "CASH", cashMeAcctCheck, req.getReference(), req.getDescription(), cashMeAcctCheck, req.getSenderName());
                                    List<Map<String, Object>> data = jdbcPartnersTemplate.queryForList(sql, req.getBeneficiaryAccount(), req.getSenderAccount(), "CILANTRO SYSTEM", "C", req.getAmount(), req.getBeneficiaryName(), "0", req.getCurrency(), req.getSenderPhone(), req.getSenderName(), "CASH", cashMeAcctCheck, req.getReference(), req.getDescription(), cashMeAcctCheck, req.getSenderName());
                                    LOGGER.info("INSERTING CASHME TO BANK TRANSACTIONS RESP:  {}", data);
                                    //'$tcbClientCollectAcct','$acctNo','$authorizedby','L', $valtdAmount,'$acctName','0','$Currency','$depositorMobilePhone','$depositorName','CASH','$txid','$txid','$narrations','$collectAcct','$depositorName'";
                                }

                            } else {
                                jdbcTemplate.update("update transfers set message='FAILED',status='C',cbs_status='F',comments='Failed',branch_approved_by='SYSTEM',branch_approved_dt=?  where  reference=?", DateUtil.now(), req.getReference());
                                jdbcTemplate.update("update transfer_advices set cbsMessage='failed',status='F',cbsStatus='F'  where  senderReference=?", req.getReference());

                            }
                        }
                    } else {
                        responseCode = "777";
                        message1 = "Account name doesn't match by: " + String.format("%.2f", distance) + "% rubikon name: " + nameQuery.getAccountName().toUpperCase() + " : Beneficiary name Meant:" + req.getBeneficiaryName().toUpperCase();
                        LOGGER.info("TRANSACTION CANNOT BE POSTED TO CBS BECAUSE BENEFICIARY NAME DOESNT MATCH CORE BANKING NAME: RESULT:{} ,CBS RESPONSE MESSAGE: {}, BATCH Reference {}, TXN REFERENCE:{} , AMOUNT: {}", responseCode, message1, req.getReference(), req.getRelatedReference(), req.getAmount());
                        jdbcTemplate.update("update transfers set status='C',cbs_status='F',message=?,comments=?,branch_approved_by='SYSTEM',response_code=?,branch_approved_dt=?,hq_approved_by= 'SYSTEM',hq_approved_dt=? where  reference=? ", message1, message1, responseCode, DateUtil.now(), DateUtil.now(), req.getReference());
                    }

                }
            }

        } catch (Exception ex) {
            LOGGER.error("RTGS INCOMING EXCEPTION FAILED: {} REFERENCE: {}", ex.getMessage(), req.getReference());
            LOGGER.error(null, ex);
        }
    }

    public int sendPostToSwiftGePGAPI(RTGSTransferForm req, String contraAcct, String chargeScheme) {
        StopWatch watch = new StopWatch();
        watch.start();
        String description;
        int responseCode = -1;
        if (req.getDescription() == null) {
            LOGGER.info("IT'S A GePG ACCOUNT BUT NO CONTROL NUMBER SET ON FIELD 70 .. {}", req.getDescription());
            responseCode = -1;
        } else {
            description = req.getDescription().replaceAll("\\D+", "");
            String heslb_old_account = "CCA0240000032,024-0000032,0240000032";
            String heslb_new_account = "110208000002";
            if (req.getBeneficiaryAccount().contains("CCA0240000032") || req.getBeneficiaryAccount().contains("024-0000032") || req.getBeneficiaryAccount().contains("0240000032")) {
                req.setBeneficiaryAccount(heslb_new_account);
            }
            //LIVE
            String url = systemVariables.GEPG_PAYMENT_MIDDLEWARE_URL;

            String urlParameters = "currency=" + req.getCurrency() + "&amount=" + req.getAmount() + "&accountNo=" + req.getBeneficiaryAccount() + "&accountName=" + req.getBeneficiaryName() + "&senderBank=" + req.getSenderBic() + "&senderAccount=" + req.getSenderAccount() + "&senderName=" + req.getSenderName() + "&ControlNo=" + req.getDescription() + "&tranId=" + req.getReference() + "&tranRef=" + req.getReference() + "&contraAccount=" + contraAcct + "&chargeScheme=" + chargeScheme;
            LOGGER.info("REQUEST TO KIPAYMENT:{}?{}", url, urlParameters);
            String response = HttpClientService.sendFormData(urlParameters, url);
            LOGGER.info("RESPONSE FROM KIPAYMENT:{}", response);
            if (!response.equals("-1")) {
                try {
                    //print result
                    KipaymentGepgRequest gepgResp = jacksonMapper.readValue(response, KipaymentGepgRequest.class);
                    long rspCode = gepgResp.getResponseCode();
                    if (rspCode == 77 || rspCode == 0) {
                        System.out.println("SUCCESS");
                        LOGGER.info("SUCCESS - {}", req.getReference());
                        responseCode = 0;
                    } else {
                        System.out.println("FAILED");
                        LOGGER.info("FAILED - {}", req.getReference());
                        responseCode = 1;
                    }
                } catch (JsonProcessingException ex) {
                    LOGGER.info("sendPostToGePGAPI", ex);
                }
            }
        }
        watch.stop();
        LOGGER.info("{}ms Closing  http swift connection.... For REFERENCE: {}", watch.getTotalTimeMillis(), req.getReference());

        return responseCode;
    }

    public int processInwardRTGSToCBS(RTGSTransferForm req) {
        LOGGER.info("CHARGE DETAILS: {},", req.getChargeDetails());
        int result = -1;
        LOGGER.info("TRANSACTION DATE: {}", req.getTransactionDate());
        AccountNameQuery nameQuery = corebanking.accountNameQuery(req.getBeneficiaryAccount());//.toUpperCase();//get account Name
        if (req.getCurrency().equalsIgnoreCase("TZS") && !nameQuery.getAccountCurrency().equalsIgnoreCase("TZS")) {
            String message = "Transaction is not allowed from TZS to " + nameQuery.getAccountCurrency() + ". Please Return to the sender";
            jdbcTemplate.update("update transfers set status='C',response_code='888',cbs_status='F',comments=?,branch_approved_by=?,branch_approved_dt=? where  reference=?", message, "SYSTEM", DateUtil.now(), req.getReference());

        } else {
            LOGGER.info("TRANSACTION CURRENCY:{} DESTINATION ACCOUNT CURRENCY:{}", req.getTransactionDate());
            try {
//            boolean processPayment = true;
//            boolean checkGepg = false;
                TaTransfer transferReq = new TaTransfer();
                String contraAcct = "0-000-0000-0000-000000";
                String chargeScheme = req.getChargeDetails();
                //generate the request to RUBIKON
            /*
            GET CONTRA ACCOUNT NUMBER
             */
                //CHAGE SCHEME
//            if (systemVariables.WAIVED_ACCOUNTS_LISTS.contains(req.getBeneficiaryAccount())) {//waived accounts
//                chargeScheme = "T99";
//            } else {
//                chargeScheme = "T02";
//            }

                transferReq.setReference(req.getReference());
                transferReq.setTxnRef(req.getReference());
                transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(req.getTransactionDate(), "yyyy-MM-dd"));
                transferReq.setValueDate(DateUtil.dateToGregorianCalendar(req.getTransactionDate(), "yyyy-MM-dd"));
                transferReq.setEmployeeId(0L);
                transferReq.setSupervisorId(0L);
                transferReq.setTransferType("RTGS");
                transferReq.setCurrency(req.getCurrency());
                transferReq.setAmount(new BigDecimal(req.getAmount()));
                transferReq.setDebitFxRate(new BigDecimal("0"));
                transferReq.setCreditFxRate(new BigDecimal("0"));
                transferReq.setReceiverBank(req.getBeneficiaryBIC());
                transferReq.setReceiverAccount(req.getBeneficiaryAccount());
                transferReq.setReceiverName(req.getBeneficiaryName());
                transferReq.setSenderBank(req.getSenderBic());
                transferReq.setSenderAccount(req.getSenderAccount());
                transferReq.setSenderName(req.getSenderName());
                transferReq.setDescription(req.getDescription() + " FROM:  " + req.getSenderName() + " REF:" + req.getReference());
                transferReq.setTxnId(Long.valueOf(DateUtil.now("yyyyMMddHHmmss")));

                if (req.getTransactionType().equalsIgnoreCase("001")) {
                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                    transferReq.setScheme("T100");
                } else if (req.getTransactionType().equalsIgnoreCase("004")) {
                    if (req.getCurrency().equalsIgnoreCase("USD") && req.getSenderBic().contains("SCBL")) {
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER;
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                    } else if (req.getCurrency().equalsIgnoreCase("USD") && req.getSenderBic().contains("BHFB")) {
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER;
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                    } else if (req.getCurrency().equalsIgnoreCase("EUR")) {
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER;
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                    } else if (req.getCurrency().equalsIgnoreCase("ZAR")) {
                        contraAcct = systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER;
                        transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                    } else {
                        //CHECK CORRESPONDENT BANK
                        if (req.getCurrency().equalsIgnoreCase("USD") && req.getCorrespondentBic().contains("SCBLU")) {
                            contraAcct = systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER;
                            transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SCB_LEDGER);
                        } else if (req.getCorrespondentBic().contains("BHFB")) {
                            contraAcct = systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER;
                            transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_BHF_LEDGER);
                        } else if (req.getCurrency().equalsIgnoreCase("ZAR")) {
                            contraAcct = systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER;
                            transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TT_SBZAZA_LEDGER);
                        } else if (req.getCorrespondentBic().contains("TANZTZ")) {
                            contraAcct = systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER;
                            transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                        } else {
                            contraAcct = "-1";
                            transferReq.setContraAccount("-1");
                        }
                    }
                } else {
//                processPayment = false;
                    transferReq.setContraAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
                }
                transferReq.setReversal(Boolean.FALSE);
                transferReq.setUserRole(systemVariable.achUserRole("000"));
                LOGGER.info("CHARGE DETAILS 2:{}", req.getChargeDetails());
                transferReq.setScheme(chargeScheme);
                PostInwardTransfer postOutwardReq = new PostInwardTransfer();
                postOutwardReq.setTransfer(transferReq);
                LOGGER.info("CHARGE DETAILS 2:{}", postOutwardReq);

                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, "ach:postInwardTransfer"), TaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : Reference {}", req.getReference());
                    //do not update the transaction status
                } else {
                    result = cbsResponse.getResult();
                }
                if (cbsResponse.getResult() == 0) {
                    jdbcTemplate.update("update transfers set status='C',response_code='0',cbs_status='C',comments='Success',branch_approved_by=?,branch_approved_dt=? where  reference=?", "SYSTEM", DateUtil.now(), req.getReference());
                    LOGGER.info("RTGS SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", req.getReference(), req.getAmount(), "000", "SYSTEM");
                } else {
                    if (cbsResponse.getResult() == 26) {
                        jdbcTemplate.update("update transfers set message=?,status='P',cbs_status='C',comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=?", cbsResponse.getMessage(), cbsResponse.getMessage() + " : " + cbsResponse.getResult(), "SYSTEM", DateUtil.now(), req.getReference());
                    } else {
                        jdbcTemplate.update("update transfers set message=?,response_code=?,cbs_status='F',comments=?,branch_approved_by=?,branch_approved_dt=? where  reference=?", cbsResponse.getMessage(), cbsResponse.getResult(), cbsResponse.getMessage() + " : " + cbsResponse.getResult(), "SYSTEM", DateUtil.now(), req.getReference());
                    }
                    //failed on posting CBS
                    LOGGER.info("RTGS FAILED: transReference: {} Amount: {} BranchCode: {} POSTED BY: {} CBS RESPONSE: {}", req.getReference(), req.getAmount(), "000", "SYSTEM", cbsResponse.getResult());
                    //update the transaction status
                }

            } catch (Exception ex) {
                LOGGER.info(null, ex);
                LOGGER.info("AN ERROR OCCURED DURING PROCESSING TRANSACTION TO CORE BAKING:BENEFICIARY ACCOUNT:{} SENDER REFERENCE: {}", req.getBeneficiaryAccount(), req.getReference());
            }
        }
        return result;
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

    public int airtelTrustAgentNamequery(RTGSTransferForm trf) {
        StopWatch watch = new StopWatch();
        watch.start();
        int responseCode = -1;
        String agentNo = trf.getDescription() + "";

        String reference = trf.getReference();
        String amount = trf.getAmount();
        agentNo = agentNo.replaceAll("\\D+", "");
        agentNo = filterTILLNO(agentNo);

        String agentNameQReq = "<verify>\n"
                + "<provider>airtel</provider>\n"
                + "<tillNo>" + agentNo + "</tillNo>\n"
                + "<msisdn>" + agentNo + "</msisdn>\n"
                + "</verify>";
        LOGGER.info("AIRTEL TRUST NAMEQUERY REQ:{}", agentNameQReq);
        String agentNameQResp = HttpClientService.sendXMLRequest(agentNameQReq, systemVariable.AIRTEL_NAMEQUERY_URL);
        LOGGER.info("AIRTEL TRUST NAMEQUERY RESPONSE:{}", agentNameQResp);
        String agentNameQRespCode = XMLParserService.getDomTagText("responseCode", agentNameQResp);
        if (agentNameQRespCode.equals("0")) {
            responseCode = 0;
        } else {
            //Agent Not found: failed on name query
            responseCode = 501;
        }
        watch.stop();
        LOGGER.info("{}ms duration taken For Reference: {}", watch.getTotalTimeMillis(), trf.getReference());
        return responseCode;
    }

    public int processAirtelTrustAccount(RTGSTransferForm trf) {
        StopWatch watch = new StopWatch();
        watch.start();
        int responseCode = -1;
        String agentNo = trf.getDescription() + "";

        String reference = trf.getReference();
        String amount = trf.getAmount();
        agentNo = agentNo.replaceAll("\\D+", "");
        agentNo = filterTILLNO(agentNo);
        String agentNameQReq = "<verify>\n"
                + "<provider>airtel</provider>\n"
                + "<tillNo>" + agentNo + "</tillNo>\n"
                + "<msisdn>" + agentNo + "</msisdn>\n"
                + "</verify>";
        String agentNameQResp = HttpClientService.sendXMLRequest(agentNameQReq, systemVariable.AIRTEL_NAMEQUERY_URL);
        String agentNameQRespCode = XMLParserService.getDomTagText("responseCode", agentNameQResp);
        if (agentNameQRespCode.equals("0")) {
            String agentNameQRespName = XMLParserService.getDomTagText("SAname", agentNameQResp);
            String trustPaymentReq = "<postPaymentRequest>"
                    + "<provider>airtel</provider>"
                    + "<SAname>" + agentNameQRespName + "</SAname>"
                    + "<tillNo>" + agentNo + "</tillNo>"
                    + "<reference>" + reference + "</reference>"
                    + "<amount>" + amount + "</amount>"
                    + "<msisdn>" + agentNo + "</msisdn>"
                    + "</postPaymentRequest>";
            String trustPaymentResp = HttpClientService.sendXMLRequest(trustPaymentReq, systemVariable.AIRTEL_TRUSTGATEWAY_URL);
            String trustPaymentRespCode = XMLParserService.getDomTagText("responseCode", trustPaymentResp);
            if (trustPaymentRespCode.equals("0")) {
                responseCode = 0;
            } else {
                //fail to post payment on the gateway
                responseCode = 503;
            }
        } else {
            responseCode = 501;
        }
        return responseCode;
    }

    public TpbPartnerReference validateCMSReference(String recAcct) {
        TpbPartnerReference refer = null;
        StopWatch watch = new StopWatch();
        watch.start();

        String agentNameQReq = "";
        String httpResponse = HttpClientService.sendXMLRequest(agentNameQReq, systemVariable.KIPAYMENT_CMS_NAMEQUERY_URL + "?referenceNo=" + recAcct);
        if (httpResponse.contains("profileID")) {
            String profileID = XMLParserService.getDomTagText("profileID", httpResponse);
            String reference = XMLParserService.getDomTagText("reference", httpResponse);
            String name = XMLParserService.getDomTagText("name", httpResponse);
            String message = XMLParserService.getDomTagText("message", httpResponse);
            String mobile = XMLParserService.getDomTagText("mobile", httpResponse);
            String detail = XMLParserService.getDomTagText("detail", httpResponse);
            String acctProdCode = XMLParserService.getDomTagText("acctProdCode", httpResponse);
            String notifyFlag = XMLParserService.getDomTagText("notifyFlag", httpResponse);
            refer = new TpbPartnerReference();
            refer.setName(name);
            refer.setReference(reference);
            refer.setProfileID(profileID);
            refer.setNotifyFlag(notifyFlag);
            refer.setMessage(message);
            refer.setDetail(detail);
            refer.setAcctProdCode("0");
            refer.setNotifyFlag("1");
        }
        watch.stop();
        LOGGER.info("{}ms duration taken For TxId: {}", watch.getTotalTimeMillis(), recAcct);
        return refer;
    }

    public CashMeLookupResp validateCashMeCtrNo(String recAcct) {
        LOGGER.info("checking cashme .. {}", recAcct);
        CashMeLookupResp lookupResp = null;
        StopWatch watch = new StopWatch();
        watch.start();

        String agentNameQReq = "";
        String httpResponse = HttpClientService.sendXMLRequest(agentNameQReq, systemVariable.KIPAYMENT_CASHME_NAMEQUERY_URL + "?referenceNo=" + recAcct);

        if (httpResponse.contains("acctNameQryResp")) {
            String profileID = XMLParserService.getDomTagText("trxnRef", httpResponse);
            String reference = XMLParserService.getDomTagText("trxnRef", httpResponse);
            String responseCode = XMLParserService.getDomTagText("responseCode", httpResponse);
            String name = XMLParserService.getDomTagText("acctName", httpResponse);
            String responseMessage = XMLParserService.getDomTagText("responseMessage", httpResponse);
            String accountNo = XMLParserService.getDomTagText("acctNo", httpResponse);


            String message = XMLParserService.getDomTagText("responseMessage", httpResponse);
            String mobile = "-1";
            String detail = XMLParserService.getDomTagText("acctName", httpResponse);
            lookupResp = new CashMeLookupResp();
            lookupResp.setAcctName(name);
            lookupResp.setCurrency("TZS");
            lookupResp.setAcctNo(accountNo);
            lookupResp.setBankBic("CASHMETZTZ");
            lookupResp.setResponseCode(responseCode);
            lookupResp.setTrxnRef(reference);
            lookupResp.setMinDeposit(BigDecimal.valueOf(1000));
            lookupResp.setAcctName(name);
            lookupResp.setResponseMessage(responseMessage);
        }
        watch.stop();
        LOGGER.info("{}ms duration taken For TxId: {}", watch.getTotalTimeMillis(), recAcct);
        return lookupResp;
    }

    public static String filterTILLNO(String number) {
        LOGGER.info("{filterTILLNO:Incoming {}", number);
        if (number != null & number.length() == 13) {
            number = number.substring(4, 13);
        } else if (number != null & number.length() == 12) {
            number = number.substring(3, 12);
        } else if (number != null & number.length() == 10) {
            number = number.substring(1, 10);
        } else {
            number = number;
        }
        LOGGER.info("{filterTILLNO:Outgoing {}", number);
        return number;
    }

    /*
    PROCESS MT950 TO DATABASE FOR REPORTS
     */
    @JmsListener(destination = PROCESS_MT950_ENTRIES_FOR_REPORTS, containerFactory = "queueListenerFactoryIncomingSTP")
    public void processMT950toLocalDatabaseForReports(@Payload Mt950ObjectReq req, @Headers MessageHeaders headers, Message message, Session session) {
        StopWatch timer = new StopWatch();
        String sql = "INSERT INTO mt950statements(account, currency, statementNo, sequenceNo, transDate, reference, relatedReference, messageType, debitOrCredit, senderBank, beneficiaryBank, previousBalance, postBalance,transAmount) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, req.getAccount());
                ps.setString(2, req.getCurrency());
                ps.setString(3, req.getStatementNo());
                ps.setString(4, req.getSequenceNo());
                ps.setString(5, req.getTransDate());
                ps.setString(6, req.getReference());
                ps.setString(7, req.getStatementEntries().get(i).getRelatedReference());
                ps.setString(8, req.getStatementEntries().get(i).getMessageType());
                ps.setString(9, req.getStatementEntries().get(i).getDrCrIndicator());
                ps.setString(10, req.getStatementEntries().get(i).getSenderBank());
                ps.setString(11, req.getStatementEntries().get(i).getBeneficiaryBank());
                ps.setBigDecimal(12, req.getStatementEntries().get(i).getPrevoiusBalance());
                ps.setBigDecimal(13, req.getStatementEntries().get(i).getPostBalance());
                ps.setBigDecimal(14, req.getStatementEntries().get(i).getAmount());

            }

            @Override
            public int getBatchSize() {
                return req.getStatementEntries().size();
            }
        });
    }

    //TODO: check muse gepg and force to post through tiss if gepg
    @JmsListener(destination = PROCESS_BATCH_PAYMENTS_TO_BOT, containerFactory = "queueListenerFactory")
    public void processBatchPaymentsToBoT(@Payload PaymentReq req, @Headers MessageHeaders headers, Message message,
                                          Session session) throws JMSException {
        String creditGL;
        String txnType;
        String messageType;
        String identifier = "api:postGLToGLTransfer";
        TxRequest transferReq = new TxRequest();
        transferReq.setReference(req.getReference());
        transferReq.setAmount(req.getAmount());
        transferReq.setNarration(req.getDescription() + " Batch Reference:" + req.getBatchReference());
        transferReq.setCurrency(req.getCurrency());
        transferReq.setDebitAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", req.getCustomerBranch()));
        transferReq.setUserRole(systemVariable.apiUserRole(req.getCustomerBranch()));

        //check if narration does not contain /ROC/ that, or you can use req.getIsGepg() for gepg transaction.
        //if (!req.getDescription().contains("/ROC/99") && transferReq.getAmount().compareTo(new BigDecimal(20000000)) < 0 && transferReq.getCurrency().equalsIgnoreCase("TZS")) {
        if (req.getIsGepg().equalsIgnoreCase("Y")) {
            txnType = "001";
            messageType = "103";
            creditGL = systemVariable.TRANSFER_MIRROR_TISS_BOT_LEDGER;
        } else {
            if ((transferReq.getAmount().compareTo(new BigDecimal(20000000)) < 0 && transferReq.getCurrency().equalsIgnoreCase("TZS"))) {
                creditGL = systemVariable.TRANSFER_MIRROR_EFT_BOT_LEDGER;
                txnType = "005";
                messageType = "pacs.008.001.003";
            } else {
                txnType = "001";
                messageType = "103";
                if (req.getCurrency().equalsIgnoreCase("USD")
                        && !req.getBeneficiaryBIC().contains("TZ")) {
                    req.setCorrespondentBic(systemVariable.USD_CORRESPONDEND_BANK);
                    txnType = "004";
                    creditGL = systemVariable.TRANSFER_MIRROR_TT_SCB_LEDGER;
                } else if (req.getCurrency().equalsIgnoreCase("EUR")) {
                    req.setCorrespondentBic(systemVariable.EURO_CORRESPONDEND_BANK);
                    txnType = "004";
                    creditGL = systemVariable.TRANSFER_MIRROR_TT_BHF_LEDGER;
                } else {
                    req.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    creditGL = systemVariable.TRANSFER_MIRROR_TISS_BOT_LEDGER;
                }
            }
        }
        req.setType(txnType);
        req.setSenderBic(systemVariable.SENDER_BIC);
        String swiftMsg = SwiftService.createMT103FromOnlineReq(req);
        transferReq.setCreditAccount(creditGL);

        PostGLToGLTransfer postOutwardReq = new PostGLToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
        //process the Request to CBS
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("BOOK TRANSFER BT: trans Reference {}", req.getBatchReference());
            //do not update the transaction status
            LOGGER.info("[{}] - error http response - {} ", PROCESS_BOOK_TRANSFER, cbsResponse);
            throw new RuntimeException("[" + PROCESS_BOOK_TRANSFER + "] - error http response - [" + cbsResponse + "]");
        }
        switch (cbsResponse.getResult()) {
            case 0:
            case 26:
                if (systemVariables.IS_TISS_VPN_ALLOWED && req.getType().equalsIgnoreCase("001")) {
                    // THIS IS OLD STANDARD
                    //SEND TRANSACTION TO BOT
                    //Create signature
                    String signedRequestXML = signTISSVPNRequest(swiftMsg, systemVariables.SENDER_BIC);

                    String response = null;
                    try {
                        response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariables.BOT_TISS_VPN_URL, req.getReference(), systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, systemVariables.getSysConfiguration("BOT.tiss.daily.token", "prod"), systemVariables.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH,systemVariables.PRIVATE_TISS_VPN_KEYPASS);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    LOGGER.info("REQUEST TO BOT:{}, RESPONSE FROM BOT:{}", signedRequestXML, response);
                    if (response != null && !response.equalsIgnoreCase("-1")) {
                        //get message status and update transfers table with response to IBD approval
                        String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
                        if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                            //the message is successfully on BOT ENDPOINT
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
                        } else {
                            queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^REJECTED");
                            //
                        }
                    }//else is needed here

//                    MxMessageRequest mxMessageRequest = new MxMessageRequest();
//                    mxMessageRequest.setSender("TAPBTZTZXXX");
//                    mxMessageRequest.setReceiver(req.getBeneficiaryBIC() + "XXX");
//                    mxMessageRequest.setPriority("Normal");
//                    if (!Objects.equals(systemVariables.ACTIVE_PROFILE, "prod")) {
//                        mxMessageRequest.setService("tanz.rtgs!p");
//                    } else {
//                        mxMessageRequest.setService("tanz.rtgs.p");
//                    }
//                    mxMessageRequest.setClearingSystem("TIS");
//                    mxMessageRequest.setServicePrtry("TTC:103");
//                    mxMessageRequest.setChoicePrtry("RTGSFIToFICustomerCredit");
//                    String benBic = req.getBeneficiaryBIC().toLowerCase();
//                    mxMessageRequest.setReceiverDn("o=" + benBic + ",o=swift");
//                    mxMessageRequest.setReceiverBic(req.getBeneficiaryBIC());
//                    mxMessageRequest.setMessageIdentifier(messageType);
//                    mxMessageRequest.setRequestSubType("swift.iap.02");
//                    mxMessageRequest.setSettlementMethod("CLRG");
//                    mxMessageRequest.setPurposeCode("OTHR");
//                    Txn txn = new Txn();
//                    txn.setReference(req.getReference());
//                    txn.setPriority("HIGH");
//                    txn.setCurrency(req.getCurrency());
//                    txn.setAmount(req.getAmount().toString());
//                    txn.setChargeBearer("shared");
//                    txn.setBeneficiaryAddress("");
//                    txn.setBeneficiaryName(req.getBeneficiaryName());
//                    txn.setBeneficiaryAccount(req.getBeneficiaryAccount());
//                    txn.setOrdererName(req.getSenderName());
//                    txn.setOrdererAddress(req.getSenderAddress());
//                    txn.setOrdererAccount(req.getSenderAccount());
//                    txn.setPurposeCode("CASH");
//                    txn.setRemittanceInformation(req.getDescription());
//                    List<Txn> txnList = new ArrayList<>();
//                    txnList.add(txn);
//                    mxMessageRequest.setTxnList(txnList);
//                    String response;
//                    try {
//                        String json = jacksonMapper.writeValueAsString(mxMessageRequest);
//                        response = HttpClientService.sendJsonRequest(json, systemVariables.KIPAYMENT_API_URL);
//                        LOGGER.info("REQUEST TO KIPAYMENT:{}, RESPONSE FROM KIPAYMENT:{}", req, response);
//                        if (response != null && !response.equalsIgnoreCase("-1")) {
//                            // get message status and update transfers table with response to IBD approval
//                            String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
//                            if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
//                                // the message is successful on BOT ENDPOINT
//                                queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
//                            } else {
//                                queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^REJECTED");
//                            }
//                        }
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
                }
                else {
                    if (req.getType().equalsIgnoreCase("004") || req.getType().equalsIgnoreCase("001")) {
                        queProducer.sendToQueueRTGSToSwift(swiftMsg + "^" + systemVariables.KPRINTER_URL + "^" + req.getReference());
                        queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");
                    }
                }
                //UPDATE BATCH TRANSACTION TO EFT BATCH TABLE
                jdbcTemplate.update("UPDATE transfers set status = 'C', cbs_status = 'C', message = 'Success', hq_approved_by = 'SYSTEM', hq_approved_dt = ?, txn_type = ?, message_type = ? where reference = ?", DateUtil.now(), txnType, messageType, req.getReference());
                if (req.getCallbackUrl() != null) {
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^SUCCESS");//send callback to IBANK
                }
                break;
            case 51:
            case 53:
            case 13:
            case 14:
                //UPDATE BATCH TRANSACTION TO EFT BATCH TABLE
                jdbcTemplate.update("UPDATE transfers set status = 'F', cbs_status = 'F', message = 'Failed to process to BoT mirror suspense', hq_approved_by = 'SYSTEM', hq_approved_dt = ?, txn_type = ?, message_type = ? where reference = ?", DateUtil.now(), txnType, messageType, req.getReference());
                if (req.getCallbackUrl() != null) {
                    queProducer.sendToQueueOutwardAcknowledgementToInternetBanking(req.getReference() + "^FAILED");//send callback to IBANK
                }
                break;
            default:
                LOGGER.info("FAILED TO PROCESS BATCH PAYMENTS TO BOT WITH REFERENCE: " + req.getReference());
                throw new RuntimeException("[" + PROCESS_BATCH_PAYMENTS_TO_BOT + "] - error http response - [" + cbsResponse + "]");
        }
    }

    public List<LedgerReconDataReq> getGeneralCBSTransactionsForRecon(String reconDate, GeneralReconConfig config) {
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
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String endDate = dateFormat.format(cal.getTime());
            cstmt.setDate(2, java.sql.Date.valueOf(reconDate));
            cstmt.setDate(3, java.sql.Date.valueOf(endDate));
            cstmt.executeQuery();
            ResultSet cursor = cstmt.getObject(1, ResultSet.class);
            int i = 0;
            while (cursor.next()) {
//                System.out.println("Name = " + cursor.getString(1));
//                System.out.println("Name = " + cursor.getString(2));
//                System.out.println("Name = " + cursor.getString(3));
//                System.out.println("Name = " + cursor.getString(4));
//                System.out.println("Name = " + cursor.getString(5));
//                System.out.println("Name = " + cursor.getString(6));
//                System.out.println("Name = " + cursor.getString(7));
//                System.out.println("Name = " + cursor.getString(8));
                LedgerReconDataReq ltxn = new LedgerReconDataReq();
                ltxn.setBenAccount(cursor.getString("GL_ACCT_NO"));
                ltxn.setNarration(cursor.getString("TRAN_DESC"));
                ltxn.setDrcrInd(cursor.getString("DR_CR_IND"));
                ltxn.setSourceAcct(cursor.getString("GL_ACCT_NO"));
                ltxn.setTxnType(config.getTxnType());
                try {
                    ltxn.setTransDate(DateUtil.formatDate(cursor.getString("SYS_CREATE_TS"), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (i == 0) {
                    ltxn.setAmount(new BigDecimal(cursor.getString("STMNT_BAL").replace("-", "")));
                    ltxn.setExceptionType("LEDGER_CLOSING_BALANCE");
                    ltxn.setStatus("VERIFIED");
                    ltxn.setReference(cursor.getString("TRAN_REF_TXT") + "_12");
                    ltxn.setTxnType(config.getTxnType());
                    //insert here
                    recon_m.insertCbsTxnIntoReconTracker(ltxn);
                }
                ltxn.setReference(cursor.getString("TRAN_REF_TXT"));
                ltxn.setStatus("PENDING");
                ltxn.setAmount(cursor.getBigDecimal("TXN_AMT"));

                list.add(ltxn);
                i++;
            }
            connection.close();
            LOGGER.info("BANK STATEMENT DTA.. {}", list);
        } catch (Exception e) {
            LOGGER.info("Exception found: ... {}", e.getMessage());
            System.out.println("connection to GW: " + e.getMessage());
        }
        return list;
    }


    @JmsListener(destination = DOWNLOAD_GENERAL_RECON_DATA_TO_RECON_TRACKER, containerFactory = "queueListenerFactory")
    public void downloadGeneralReconDataToReconTracker() throws JMSException {
        int reconSize = systemVariables.GENERAL_RECON_SIZE;
        List<GeneralReconConfig> reconConfigs = recon_m.getReconConfigs();
        try {
            for (GeneralReconConfig config : reconConfigs) {
                for (int i = reconSize; i > 0; i--) {
                    Calendar cal = Calendar.getInstance();
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    cal.add(Calendar.DATE, -i);
                    String reconDate = dateFormat.format(cal.getTime());
//                    LOGGER.info("Recon date... {}", reconDate);

                    List<LedgerReconDataReq> cbsTransactions = recon_m.getGeneralCBSTransactionsForRecon2(reconDate, config);

                    List<String> cbsRefs = new ArrayList<>();
                    List<String> cbsRefs2 = new ArrayList<>();

                    for (LedgerReconDataReq refs : cbsTransactions) {
                        cbsRefs.add(refs.getReference().trim());
                        cbsRefs2.add(refs.getReference().trim());
                    }

                    List<BankStReconDataReq> bankStatementData = recon_m.getGeneralBankStatementTransactionsForRecon(reconDate, config);
                    List<String> bankRefs = new ArrayList<>();

                    for (BankStReconDataReq re : bankStatementData) {
                        if (re.getReference() != null && !re.getReference().isEmpty()) {
                            bankRefs.add(re.getReference().trim());
                        }
                    }
//                    LOGGER.info("BANK  DATA FULL:{}", bankStatementData);
                    cbsRefs.removeAll(bankRefs);
                    bankRefs.removeAll(cbsRefs2);
//                    LOGGER.info("CBS DATA NOT IN BANK STATEMENT:{}", cbsRefs);
//                    Loop through CBSTransactions if cbsRefs is not empty
                    if (!cbsRefs.isEmpty()) {
                        // log the exceptions
                        for (LedgerReconDataReq txn : cbsTransactions) {
                            if (cbsRefs.contains(txn.getReference())) {
                                if (txn.getDrcrInd().equalsIgnoreCase("DR")) {
                                    txn.setExceptionType("DEBITS_IN_LEDGER_NOT_IN_BANK_ST");
                                }
                                if (txn.getDrcrInd().equalsIgnoreCase("CR")) {
                                    txn.setExceptionType("CREDITS_IN_LEDGER_NOT_IN_BANK_ST");
                                }
                                recon_m.insertCbsTxnIntoReconTracker(txn);
                            }
                        }
                    }
//                    LOGGER.info("BANK STATEMENT DATA NOT IN LEDGER:{}", bankRefs);
                    if (!bankRefs.isEmpty()) {
                        // log the exceptions
                        for (BankStReconDataReq txn : bankStatementData) {
                            if (bankRefs.contains(txn.getReference())) {
                                if (txn.getDrcrInd().equalsIgnoreCase("DR")) {
                                    txn.setExceptionType("DEBITS_IN_BANK_ST_NOT_IN_LEDGER");
                                }
                                if (txn.getDrcrInd().equalsIgnoreCase("CR")) {
                                    txn.setExceptionType("CREDITS_IN_BANK_ST_NOT_IN_LEDGER");
                                }
                                recon_m.insertIntoReconTracker(txn);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @JmsListener(destination = SEND_TO_QUEUE_BOT_CLOSING_BALANCE, containerFactory = "queueListenerFactory")
    public void sendToQueueBOTClosingBalance(@Payload Mt950ObjectReq req) throws JMSException {
        List<GeneralReconConfig> reconConfigs = recon_m.getReconConfigs();
        for (GeneralReconConfig config : reconConfigs) {
            LOGGER.info("Request BANK_CLOSING_BALANCE {}", req);
            LOGGER.info("Request Config {}", config);
            if (config.getTxnType().contains("BOT") && config.getThirdPartyAcct().equalsIgnoreCase(req.getAccount())) {
                String sql = "INSERT IGNORE INTO reconciliation_tracker (created_by, created_date, last_modified_by, last_modified_date, rec_status, account, amount, reconTtype, closedBy, closedDate, initiatedBy, initiatedDate, mirrorAccount, narration, reconDate, reconType, status, transDate, transReference, exceptionType) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                LOGGER.info(sql.replace("?", "'{}'"), "SYSTEM", DateUtil.now(), "SYSTEM_USER",
                        DateUtil.now("yyyy-MM-dd HH:mm:ss"), "VERIFIED", req.getAccount(),
                        req.getClosingBalance(), config.getTxnType(), "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"),
                        "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"), config.getGlAcct(),
                        "CLOSING BALANCE AS AT " + req.getTransDate(), req.getTransDate(), config.getTxnType(),
                        "VERIFIED", req.getTransDate(), req.getReference() + req.getTransDate(), "BANK_CLOSING_BALANCE");
                try {
                    this.jdbcTemplate.update(sql, new Object[]{"SYSTEM", DateUtil.now(), "SYSTEM_USER",
                            DateUtil.now("yyyy-MM-dd HH:mm:ss"), "VERIFIED", req.getAccount(),
                            req.getClosingBalance(), config.getTxnType(), "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"),
                            "SYSTEM", DateUtil.now("yyyy-MM-dd HH:mm:ss"), config.getGlAcct(),
                            "CLOSING BALANCE AS AT " + req.getTransDate(), req.getTransDate(), config.getTxnType(),
                            "VERIFIED", req.getTransDate(), req.getReference() + req.getTransDate(), "BANK_CLOSING_BALANCE"});
                } catch (DataAccessException dae) {
                    LOGGER.info("Data access exception.. {}", dae);
                }
            }
        }
    }

    @JmsListener(destination = INSERT_INTO_TEMP_FROM_PENSIONERS, containerFactory = "queueListenerFactory")
    public void insertIntoTempFromPensioners(@Payload String[] batchReferences) throws JMSException {
        for (String batchRef : batchReferences) {
            BigDecimal transactionCount = BigDecimal.ZERO;
            BigDecimal sum = BigDecimal.ZERO;
            Map<String, Object> map;
            String mainSql_0 = "SELECT ifnull(sum(amount), 0) totalAmount, count(*) totalCount FROM pensioners_payroll where od_loan_status = '1'" +
                    " and batchReference = ?";
            try {
                map = jdbcTemplate.queryForMap(mainSql_0, batchRef);
                sum = new BigDecimal(map.get("totalAmount") + "");
                transactionCount = new BigDecimal(map.get("totalCount") + "");
                LOGGER.info("Temp batch sum: {} and count: {}", sum, transactionCount.intValue());
            } catch (Exception e) {
                e.printStackTrace();
            }

            jdbcTemplate.update("INSERT INTO tmp_batch_transaction (reference, callbackUrl, createDt, endRecId," +
                            " itemCount, result, startRecId, `timestamp`, totalAmount, updateDt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    batchRef + "_od", systemVariable.LOCAL_CALLBACK_URL + "/api/pension/od/repay",
                    DateUtil.now("yyyy-MM-dd HH:mm:ss"), 0, transactionCount, "", "0", DateUtil.now(), sum, null);
            LOGGER.info("Temp batch sum>>>>>......: {} and count: {}", sum, transactionCount.intValue());
            if (transactionCount.intValue() > 0) {
                String mainSql = "INSERT INTO tmp_batch_transaction_item (txnRef, amount, batch," +
                        " drAcct, createDt, currency, crAcct, module, narration, recId, recSt, reverse," +
                        " tries, `timestamp`) SELECT bankReference, amount, '" + batchRef + "_od', '"
                        + systemVariable.CREDIT_TRANSFER_AWAITING_LEDGER + "', '" + DateUtil.now("yyyy-MM-dd HH:mm:ss") +
                        "', 'TZS', account, 'XAPI', description, trackingNo, 'P', 'N', '0', '" +
                        DateUtil.now("yyyy-MM-dd HH:mm:ss") + "' FROM pensioners_payroll where od_loan_status = '1'" +
                        " and batchReference = ?";
                LOGGER.info(mainSql.replace("?", "'{}'"), batchRef);
                try {
                    jdbcTemplate.update(mainSql, batchRef);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @JmsListener(destination = RESOLVE_RECON_EXCEPTION, containerFactory = "queueListenerFactory")
    public void resolveReconException() throws JMSException {
        List<Map<String, Object>> list;
        List<GeneralReconConfig> reconConfigs = recon_m.getReconConfigs();
        for (GeneralReconConfig config : reconConfigs) {
            String sql = "select * from reconciliation_tracker where reconType = ? and reconDate <= ? and status = 'PENDING'";
            list = jdbcTemplate.queryForList(sql, new Object[]{config.getTxnType(), DateUtil.now("yyyy-MM-dd")});
            for (Map<String, Object> map : list) {
                String exceptionType = (String) map.get("exceptionType");
                switch (exceptionType) {
                    case "DEBITS_IN_LEDGER_NOT_IN_BANK_ST":
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
                                + map.get("transDate") + "')) where transReference =? and amount=? and reconTtype= ? and"
                                + " exceptionType='CREDITS_IN_BANK_ST_NOT_IN_LEDGER'";
                        jdbcTemplate.update(sql, map.get("transReference"), map.get("amount"), map.get("reconTtype"));
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate" +
                                " = case when transDate >= (select txndate from thirdpartytxns where amount = ? and" +
                                " (txnid = ? or receiptNo = ?) and identifier in ('CR','IN') limit 1) then transDate" +
                                " else (select txndate from thirdpartytxns where amount = ? and (txnid = ? or receiptNo" +
                                " = ?) and identifier in ('CR','IN') limit 1) END, closedDate = case when transDate >=" +
                                " (select txndate from thirdpartytxns where amount = ? and (txnid = ? or receiptNo = ?)" +
                                " and identifier in ('CR','IN') limit 1) then transDate else (select txndate" +
                                " from thirdpartytxns where amount = ? and (txnid = ? or receiptNo = ?) and identifier" +
                                " in ('CR','IN') limit 1) END where transReference = ? and amount = ? and reconTtype" +
                                " = ? and transReference in (select txnid from thirdpartytxns where amount = ? and" +
                                " (txnid = ? or receiptNo = ?) and identifier in ('CR','IN'))";
//                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
//                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
//                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
//                                + map.get("transDate") + "')) where transReference = ? and amount= ? and " +
//                                "reconTtype= ? and transReference in (select txnid from thirdpartytxns where amount = ? "
//                                + "and (txnid = ? or receiptNo = ?) and identifier in ('CR','IN'))";
//                        LOGGER.info(sql.replace("?", "'{}'"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("transReference"), map.get("amount"),
//                                map.get("reconTtype"), map.get("amount"), map.get("transReference"), map.get("transReference"));
                        jdbcTemplate.update(sql, map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("transReference"), map.get("amount"),
                                map.get("reconTtype"), map.get("amount"), map.get("transReference"), map.get("transReference"));
                        break;
                    case "CREDITS_IN_LEDGER_NOT_IN_BANK_ST":
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
                                + map.get("transDate") + "')) where transReference =? and amount =? and reconTtype= ? and"
                                + " exceptionType='DEBITS_IN_LEDGER_NOT_IN_BANK_ST'";
                        jdbcTemplate.update(sql, map.get("transReference"), map.get("amount"), map.get("reconTtype"));
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate" +
                                " = case when transDate >= (select txndate from thirdpartytxns where amount = ? and" +
                                " (txnid = ? or receiptNo = ?) and identifier in ('DR','OUT') limit 1) then transDate" +
                                " else (select txndate from thirdpartytxns where amount = ? and (txnid = ? or receiptNo" +
                                " = ?) and identifier in ('DR','OUT') limit 1) END, closedDate = case when transDate >=" +
                                " (select txndate from thirdpartytxns where amount = ? and (txnid = ? or receiptNo = ?)" +
                                " and identifier in ('DR','OUT') limit 1) then transDate else (select txndate" +
                                " from thirdpartytxns where amount = ? and (txnid = ? or receiptNo = ?) and identifier" +
                                " in ('DR','OUT') limit 1) END where transReference = ? and amount = ? and reconTtype" +
                                " = ? and transReference in (select txnid from thirdpartytxns where amount = ? and" +
                                " (txnid = ? or receiptNo = ?) and identifier in ('DR','OUT'))";
//                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
//                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
//                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
//                                + map.get("transDate") + "')) where transReference = ? and amount = ? and reconTtype= ?"
//                                + " and transReference in (select txnid from thirdpartytxns where amount = ? and "
//                                + "(txnid = ? or receiptNo = ?) and identifier in ('DR','OUT'))";
//                        LOGGER.info(sql.replace("?", "'{}'"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("transReference"), map.get("transReference"), map.get("amount"),
//                                map.get("reconTtype"), map.get("amount"), map.get("transReference"), map.get("transReference"));
//
                        jdbcTemplate.update(sql, map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"),
                                map.get("transReference"), map.get("transReference"), map.get("amount"),
                                map.get("reconTtype"), map.get("amount"), map.get("transReference"), map.get("transReference"));
                        break;
                    case "CREDITS_IN_BANK_ST_NOT_IN_LEDGER":
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
                                + map.get("transDate") + "')) where transReference = ? and amount = ? and reconTtype = ?"
                                + " and exceptionType='DEBITS_IN_BANK_ST_NOT_IN_LEDGER'";
                        jdbcTemplate.update(sql, map.get("transReference"), map.get("amount"), map.get("reconTtype"));
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate" +
                                " = case when transDate >= (select txndate from cbstransactiosn where amount = ? and" +
                                " txnid = ? and dr_cr_ind = 'DR' limit 1) then transDate else (select txndate from" +
                                " cbstransactiosn where amount = ? and  txnid = ? and dr_cr_ind = 'DR' limit 1) END," +
                                " closedDate = case when transDate >= (select txndate from cbstransactiosn where amount" +
                                " = ? and  txnid = ? and dr_cr_ind = 'DR' limit 1) then transDate else (select txndate" +
                                " from cbstransactiosn where amount = ? and  txnid = ? and dr_cr_ind = 'DR' limit 1) END" +
                                " where transReference = ? and amount = ? and reconTtype = ? and transReference in (" +
                                " select txnid from cbstransactiosn where amount = ? and txnid = ? and dr_cr_ind = 'DR')";
//                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
//                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
//                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
//                                + map.get("transDate") + "')) where transReference = ? and amount = ? and reconTtype= ? "
//                                + "and transReference in (select txnid from cbstransactiosn where amount = ? and " +
//                                "txnid = ? and dr_cr_ind ='DR')";
//                        LOGGER.info(sql.replace("?", "'{}'"), map.get("amount"), map.get("transReference"),
//                                map.get("amount"), map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("amount"), map.get("transReference"), map.get("transReference"), map.get("amount"),
//                                map.get("reconTtype"), map.get("amount"), map.get("transReference"));
                        jdbcTemplate.update(sql, map.get("amount"), map.get("transReference"), map.get("amount"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"), map.get("amount"),
                                map.get("transReference"), map.get("transReference"), map.get("amount"), map.get("reconTtype"),
                                map.get("amount"), map.get("transReference"));
                        break;
                    case "DEBITS_IN_BANK_ST_NOT_IN_LEDGER":
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
                                + map.get("transDate") + "')) where transReference = ? and amount = ? and reconTtype = ?"
                                + " and exceptionType='CREDITS_IN_LEDGER_NOT_IN_BANK_ST'";
                        jdbcTemplate.update(sql, map.get("transReference"), map.get("amount"), map.get("reconTtype"));
                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate" +
                                " = case when transDate >= (select txndate from cbstransactiosn where amount = ? and" +
                                " txnid = ? and dr_cr_ind = 'CR' limit 1) then transDate else (select txndate from" +
                                " cbstransactiosn where amount = ? and  txnid = ? and dr_cr_ind = 'CR' limit 1) END," +
                                " closedDate = case when transDate >= (select txndate from cbstransactiosn where amount" +
                                " = ? and  txnid = ? and dr_cr_ind = 'CR' limit 1) then transDate else (select txndate" +
                                " from cbstransactiosn where amount = ? and  txnid = ? and dr_cr_ind = 'CR' limit 1) END" +
                                " where transReference = ? and amount = ? and reconTtype = ? and transReference in (" +
                                " select txnid from cbstransactiosn where amount = ? and txnid = ? and dr_cr_ind = 'CR')";
//                        sql = "update reconciliation_tracker set status = 'SUCCESS', closedBy = 'SYSTEM', initiatedDate"
//                                + " = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('" + map.get("transDate")
//                                + "')), closedDate = IF(date('" + map.get("transDate") + "') < transDate, transDate, date('"
//                                + map.get("transDate") + "')) where transReference = ? and amount = ? and reconTtype = ? "
//                                + "and transReference in (select txnid from cbstransactiosn where amount = ? and txnid"
//                                + " = ? and dr_cr_ind ='CR')";
//                        LOGGER.info(sql.replace("?", "'{}'"), map.get("amount"), map.get("transReference"),
//                                map.get("amount"), map.get("transReference"), map.get("amount"), map.get("transReference"),
//                                map.get("amount"), map.get("transReference"), map.get("transReference"), map.get("amount"),
//                                map.get("reconTtype"), map.get("amount"), map.get("transReference"));
                        jdbcTemplate.update(sql, map.get("amount"), map.get("transReference"), map.get("amount"),
                                map.get("transReference"), map.get("amount"), map.get("transReference"), map.get("amount"),
                                map.get("transReference"), map.get("transReference"), map.get("amount"), map.get("reconTtype"),
                                map.get("amount"), map.get("transReference"));
                        break;
                }
            }
        }
    }

    @JmsListener(destination = INSERT_DUPLICATE_TXN_INTO_RECON_TRACKER, containerFactory = "queueListenerFactory")
    public void insertDuplicateTxnReconTracker() throws JMSException {
        List<Map<String, Object>> list;
        List<GeneralReconConfig> reconConfigs = recon_m.getReconConfigs();
        for (GeneralReconConfig config : reconConfigs) {
            // thirdpartytxns
            String sql = "select * from thirdpartytxns t where t.txn_type = ? and t.txnid in (select c.txnid from " +
                    "cbstransactiosn c where c.txn_type = ? group by c.txnid having count(c.txnid) = 1) having count(t.txnid) > 1";
            list = jdbcTemplate.queryForList(sql, config.getTxnType(), config.getTxnType());
            for (Map<String, Object> map : list) {
                String identifier = (String) map.get("identifier");
                String exceptionType;
                if (identifier != null && identifier.equals("IN"))
                    exceptionType = "CREDITS_IN_BANK_ST_NOT_IN_LEDGER";
                else
                    exceptionType = "DEBITS_IN_BANK_ST_NOT_IN_LEDGER";
                String innerSql = "INSERT IGNORE INTO reconciliation_tracker (created_by, created_date, " +
                        "last_modified_by, last_modified_date, rec_status, account, amount, reconTtype, closedBy, " +
                        "closedDate, initiatedBy, initiatedDate, mirrorAccount, narration, reconDate, reconType, " +
                        "status, transDate, transReference, exceptionType) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try {
                    this.jdbcTemplate.update(innerSql, new Object[]{"SYSTEM", DateUtil.now(), "SYSTEM_USER", null,
                            map.get("status"), map.get("sourceAcct"), map.get("amount"), map.get("txn_type"), null, null,
                            null, null, map.get("txdestinationaccount"), map.get("description"), map.get("txndate"),
                            map.get("ttype"), "DUPLICATE", map.get("txndate"), map.get("txnid"), exceptionType});
                } catch (DataAccessException dae) {
                    LOGGER.info("Data access exception.. {}", dae.getMessage());
                }
            }
            // cbstransactiosn
            String sql2 = "select * from cbstransactiosn c where c.txn_type = ? and c.txnid in (select t.txnid from " +
                    "thirdpartytxns t where t.txn_type = ? group by t.txnid having count(t.txnid) = 1) having count(c.txnid) > 1";
            list = jdbcTemplate.queryForList(sql2, config.getTxnType(), config.getTxnType());
            for (Map<String, Object> map : list) {
                String innerSql = "INSERT IGNORE INTO reconciliation_tracker (created_by, created_date, " +
                        "last_modified_by, last_modified_date, rec_status, account, amount, reconTtype, closedBy, " +
                        "closedDate, initiatedBy, initiatedDate, mirrorAccount, narration, reconDate, reconType, " +
                        "status, transDate, transReference, exceptionType) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                map.get("identifier");
                try {
                    this.jdbcTemplate.update(innerSql, new Object[]{"SYSTEM", DateUtil.now(), "SYSTEM_USER", null,
                            map.get("status"), map.get("sourceAcct"), map.get("amount"), map.get("txn_type"), null, null,
                            null, null, map.get("txdestinationaccount"), map.get("description"), map.get("txndate"),
                            map.get("ttype"), "DUPLICATE", map.get("txndate"), map.get("txnid"),});
                } catch (DataAccessException dae) {
                    LOGGER.info("Data access exception.. {}", dae.getMessage());
                }
            }
        }
    }

    @JmsListener(destination = CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE, containerFactory = "queueListenerFactory")
    public void validatingAndUpdatingQueuedTransactions() {
        //select references, validate on phl, then update on cilantro
        String sql = "SELECT txid FROM transfers WHERE cbs_status='QI' AND direction='INCOMING' LIMIT 700";
        List<Map<String, Object>> arr = jdbcTemplate.queryForList(sql);
        StringBuilder sb = new StringBuilder();
        String[] data;
        for (Map<String, Object> map : arr) {
            sb.append("'" + map.get("txid") + "',");
        }

        String inSql = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";

        //Opening connection to cbs
        if ((!inSql.isEmpty()) && (inSql.length() > 0)) {
            String phlSql = "SELECT TXN_REF FROM CHANNELMANAGER.PHL_QUEUE pq WHERE pq.TXN_REF IN (%s) AND pq.RESULT='00' AND ROWNUM <= 700";
            String phlSqlFinal = phlSql.replace("%s", inSql);

            List<Map<String, Object>> phlArry = jdbcRUBIKONTemplate.queryForList(phlSqlFinal);

            if ((!phlArry.isEmpty()) && (phlArry.size() > 0)) {
                LOGGER.info("Going to update the following txns with references.... {}, dated ...{}", phlArry, DateUtil.now("yyyy-MM-dd hh:MM:ss"));
                StringBuilder sb2 = new StringBuilder();
                for (Map<String, Object> map2 : phlArry) {
                    sb2.append("'" + map2.get("TXN_REF") + "',");
                }
                String phlQInSql = sb2.length() > 0 ? sb2.substring(0, sb2.length() - 1) : "";

                String updateSql = "update transfers set comments='Settled', response_code='00', cbs_status='C' WHERE txid in(%s) AND direction='INCOMING'";
                String phlQInSqlFinal = updateSql.replace("%s", phlQInSql);
                jdbcTemplate.update(phlQInSqlFinal);
            }
        }
    }

//    @JmsListener(destination = PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL, containerFactory = "queueListenerFactory")
//    public void processFailedToSettleCbsAwaitReconMoveToBOTGL(@Payload String menuVariable, @Headers MessageHeaders headers, Message message, Session session) {
//        try {
//            //select entries in transaction table
//            String date = DateUtil.now("yyyy-MM-dd");
//            //LOGGER.info("Date is ....{}", date);
//            String sql = "\n" +
//                    "select * from transactions where thirdparty_remarks ='COMMITTED' AND cbs_remarks ='FAILED' and cbs_status ='F' and thirdparty_status ='C'\n" +
//                    "\n" +
//                    "AND  date(last_modified_date) < ? AND status_description LIKE '%Transfer Failed to Setlled core banking, Recon is needed to move the fund from branch GL to BoT GL%' limit 200";
//            LOGGER.info(sql.replace("?","{}"),date);
//            List<Map<String,Object>> transArray = jdbcBrinjalTemplate.queryForList(sql,date);
//            if((!transArray.isEmpty()) && (transArray.size()>0)){
//                for (Map<String, Object> map : transArray) {
//
//                    String identifier = "api:postGLToGLTransfer";
//                    TxRequest transferReq = new TxRequest();
//                    transferReq.setReference(map.get("reference").toString());
//                    transferReq.setNarration(" Settle fund from branch eft gl " + systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", map.get("branchCode").toString()) + " To TIPS GL: "+ systemVariable.TIPS_SETTLEMENT_ACCOUNT + " REF:" + map.get("reference"));
//                    transferReq.setCurrency("TZS");
//                    transferReq.setDebitAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", map.get("branchCode").toString()));
//                    transferReq.setCreditAccount(systemVariable.TIPS_SETTLEMENT_ACCOUNT);
//                    transferReq.setAmount(new BigDecimal(map.get("amount").toString()));
//                    transferReq.setReversal("false");
//                    transferReq.setScheme("T1089");
//                    transferReq.setCreditFxRate(new BigDecimal(0));
//                    transferReq.setDebitFxRate(new BigDecimal(0));
//                    transferReq.setUserRole(systemVariable.apiUserRole(map.get("branchCode").toString()));
//                    PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
//                    postOutwardReq.setRequest(transferReq);
//                    String requestToCore = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
//                    //process the Request to CBS
//                    XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(requestToCore, identifier), XaResponse.class);
//
//                    LOGGER.info("Auto system request to core .... {} and response .... {} and message ... {}",requestToCore, cbsResponse.getResult(),cbsResponse.getMessage());
//                   if(cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26 ){
//                        String updateSql = "UPDATE transactions SET cbs_status='C', cbs_remarks='Success', status_description='COMMITTED BY STSTEM', response_code='0', status='CC' WHERE reference=?";
//                        jdbcBrinjalTemplate.update(updateSql,transferReq.getReference());
//                    }
//                }
//
//           }
//        } catch (Exception e) {
//            LOGGER.info("Exception occured on getting transactions from transaction table:{}", e.getMessage());
//            LOGGER.info(null, e);
//        }
//    }


    @JmsListener(destination = DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE, containerFactory = "queueListenerFactory")
    public void processDownloadAirtelVikobaTransactionsToCilantoCbsTable(@Payload String menuVariable, @Headers MessageHeaders headers, Message message, Session session) {
        try {
            //select entries in transaction table

            Calendar cal = Calendar.getInstance();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            cal.add(Calendar.DATE, -2);
            String fromDate = dateFormat.format(cal.getTime());
            String toDate = DateUtil.now("yyyy-MM-dd");
            String sql = "\n" +
                    "select 'WALLET2VIKOBA' txn_type,(select type from vg_trans_type a where a.trans_type_id=vgt.transtype) as docode_desc,vgt.transtype as docode,'AIRTEL-VIKOBA' ttype,'C2B transaction' description,vgt.transid txnid,vgt.receipt thirpartyReference,vgt.transate txndate,'CR' as dr_cr_ind, vgt.sourceaccount,vgt.destinationaccount,vgt.transamount amount,'0' as postBalance,'0' as previousBalance,'0' charge,\n" +
                    "case when vgt.transstatus ='0' then 'success'\n" +
                    "     when vgt.transstatus ='-1' then 'unknown error' else 'FAILED' END AS status from vg_group_transaction vgt  where vgt.transtype in ('101',\n" +
                    "'102',\n" +
                    "'111',\n" +
                    "'121',\n" +
                    "'122',\n" +
                    "'123',\n" +
                    "'124',\n" +
                    "'222',\n" +
                    "'333',\n" +
                    "'444',\n" +
                    "'555',\n" +
                    "'666',\n" +
                    "'777',\n" +
                    "'999',\n" +
                    "'1111',\n" +
                    "'1211',\n" +
                    "'1222',\n" +
                    "'1233',\n" +
                    "'12932') and vgt.transate>=? and vgt.transate<=?" +
                    "union all\n" +
                    "select 'VIKOBA2WALLET' txn_type, (select type from vg_trans_type c where c.trans_type_id=vgt1.transtype) as docode_desc, vgt1.transtype as docode,'AIRTEL-VIKOBA' ttype, 'B2C transaction' description,vgt1.transid txnid,vgt1.receipt thirpartyReference,vgt1.transate txndate,'DR' as dr_cr_ind, vgt1.sourceaccount,vgt1.destinationaccount,vgt1.transamount amount,'0' as postBalance,'0' as previousBalance,'0' charge,\n" +
                    "case when vgt1.transstatus ='0' then 'success'\n" +
                    "     when vgt1.transstatus ='-1' then 'unknown error' else 'FAILED' END AS status\n" +
                    "from vg_group_transaction vgt1 where vgt1.transtype in('115',\n" +
                    "'12612',\n" +
                    "'12613',\n" +
                    "'12614',\n" +
                    "'122001',\n" +
                    "'122002',\n" +
                    "'122003',\n" +
                    "'122004',\n" +
                    "'126111',\n" +
                    "'126112',\n" +
                    "'126113',\n" +
                    "'126114') and vgt1.transate>=? and vgt1.transate<=?";
//            LOGGER.info(sql.replace("?", "'{}'"), fromDate, toDate, fromDate, toDate);
            List<Map<String, Object>> transArray = jdbcAirtelVikobaTemplate.queryForList(sql, fromDate, toDate, fromDate, toDate);
            if ((!transArray.isEmpty()) && (transArray.size() > 0)) {
                vikobaVikobaTransactionsToCbsTransactionsTable(transArray);
            }

            //DOWNLOAD BALANCES PER DAY
            Integer recon_size = systemVariable.VIKOBA_GENERAL_RECON_SIZE;
            for (int i = 1; i <= recon_size; i++) {
                cal.add(Calendar.DATE, -i);
                String reconBalanceDate = dateFormat.format(cal.getTime());
                String query = "select a.clocingBalance as collectionOB,a.openingBalance as collectionCB, a.transactionDay as balanceDate, (select b.openingBalance\n" +
                        "from vg_group_disbursement_previous_day_balances b where b.transactionDay =?) as disbursementOB,\n" +
                        "(select b.closingBalance from vg_group_disbursement_previous_day_balances b where b.transactionDay =?) as disbursementCB\n" +
                        "from vg_group_collection_previous_day_balances a where a.transactionDay =?";

                List<Map<String, Object>> reconOCB = jdbcAirtelVikobaTemplate.queryForList(query, new Object[]{reconBalanceDate, reconBalanceDate, reconBalanceDate});
                LOGGER.info("vikoba balances OB and CB's ... {} for date ... {}", reconOCB, reconBalanceDate);
                if (reconOCB != null) {
                    jdbcTemplate.update("update cbstransactiosn set prevoius_balance=?, post_balance =? where ttype ='AIRTEL-VIKOBA' and txn_type ='WALLET2VIKOBA' and date(txndate)=?", reconOCB.get(0).get("collectionOB"), reconOCB.get(0).get("collectionCB"), reconOCB.get(0).get("balanceDate"));
                    jdbcTemplate.update("update cbstransactiosn set prevoius_balance=?, post_balance =? where ttype ='AIRTEL-VIKOBA' and txn_type ='VIKOBA2WALLET' and date(txndate)=?", reconOCB.get(0).get("disbursementOB"), reconOCB.get(0).get("disbursementCB"), reconOCB.get(0).get("balanceDate"));
                }
            }
        } catch (Exception e) {
            LOGGER.info("Exception occured on getting airtel vikoba transactions from vg_group_transactions table:{}", e.getMessage());
            LOGGER.info(null, e);
        }
    }


    public void vikobaVikobaTransactionsToCbsTransactionsTable(List<Map<String, Object>> map) {

        String sql = "INSERT IGNORE INTO cbstransactiosn (txnid,thirdparty_reference,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,charge,description,terminal,currency,txn_status,prevoius_balance,post_balance,pan,contraaccount,dr_cr_ind,branch,docode,docode_desc) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {

                ps.setString(1, map.get(i).get("txnid").toString());
                ps.setString(2, map.get(i).get("thirpartyReference").toString());
                ps.setString(3, map.get(i).get("txn_type").toString());
                ps.setString(4, map.get(i).get("ttype").toString());
                ps.setString(5, map.get(i).get("txndate").toString());
                ps.setString(6, map.get(i).get("sourceaccount").toString());
                ps.setString(7, map.get(i).get("destinationaccount").toString());
                ps.setString(8, map.get(i).get("amount").toString());
                ps.setString(9, map.get(i).get("charge").toString());
                ps.setString(10, map.get(i).get("description").toString());
                ps.setString(11, map.get(i).get("txnid").toString());
                ps.setString(12, "TZS");
                ps.setString(13, map.get(i).get("status").toString());
                ps.setString(14, map.get(i).get("previousBalance").toString());
                ps.setString(15, map.get(i).get("postBalance").toString());
                ps.setString(16, map.get(i).get("thirpartyReference").toString());
                ps.setString(17, map.get(i).get("destinationaccount").toString());
                ps.setString(18, map.get(i).get("dr_cr_ind").toString());
                ps.setString(19, map.get(i).get("postBalance").toString());
                ps.setString(20, map.get(i).get("docode").toString());
                ps.setString(21, map.get(i).get("docode_desc").toString());

            }

            //
            @Override
            public int getBatchSize() {
                return map.size();
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
    private boolean isCDSNumber(String in) {
        Pattern p = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
        if (in == null) return false;
        return p.matcher(in).find();
    }

    private String extractCDSNumber(String in) {
        LOGGER.info("The Provided CDS number {}",in);
        Pattern p = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
        if (in == null) return null;
        Matcher m = p.matcher(in);
        return m.find() ? m.group(1) : null;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String nullSafeLower(String s) { return nullToEmpty(s).toLowerCase(); }
    private static String nullSafeUpper(String s) { return nullToEmpty(s).toUpperCase(); }
    private static String safeRef(RTGSTransferForm req) { return req == null ? "" : nullToEmpty(req.getReference()); }
}
