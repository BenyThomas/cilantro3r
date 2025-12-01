/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.queue;

import com.DTO.EFT.EftPacs00800102Req;
import com.DTO.ReplayIncomingTransactionReq;
import com.DTO.IBANK.BatchPaymentReq;
import com.DTO.IBANK.PaymentReq;
import com.DTO.RemittanceToQueue;
import com.DTO.Teller.RTGSTransferForm;
import com.DTO.batch.BatchPayemntReq;
import com.DTO.pension.PensionPayrollToCoreBanking;
import com.DTO.pension.PsssfBatchRequest;
import com.DTO.swift.other.Mt950ObjectReq;
import java.util.Random;

import com.config.SYSENV;
import com.service.HttpClientService;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 *
 * @author melleji.mollel
 */
@Service
public class QueueProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueProducer.class);
    private static final String QUEUE_GEPG_REMITTANCE_TO_BOT = "queue.gepg.remittance.to.bot";
    private static final String QUEUE_GEPG_REMITTANCE_TO_SWIFT = "queue.gepg.remittance.to.swift";
    private static final String QUEUE_RTGS_FILE_REQ = "queue.rtgs.file.req";
    private static final String QUEUE_BOT_TISS_VPN = "queue.bot.tiss.vpn";
    private static final String QUEUE_RTGS_WITH_HOLDING_TAX = "queue.rtgs.tax.service";
    private static final String QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER = "queue.eft.batch.customer.to.branch.ledger";
    private static final String QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ = "queue.eft.batch.branch.to.hq.ledger";
    private static final String FROM_CBS_TO_TACH_QUEUE = "FROM_CBS_TO_TACH_QUEUE";
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
    private static final String QUEUE_OTP_EMAIL = "queue.email.otp";
    private static final String QUEUE_SEND_SMS = "queue.send.sms";
    private static final String TRANSACTIONS_ASSIGNED_TO_CHANNELMANAGER_QUEUE = "TRANSACTIONS_ASSIGNED_TO_CHANNELMANAGER_QUEUE";
    static final String CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE = "CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE";
    static final String PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL = "PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL";
    static final String DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE = "DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE";
    static final String DOWNLOAD_DATA_FROM_CILANTRO_AND_BRINJAL_AND_INSERT_INTO_CHANNELMANAGER = "DOWNLOAD_DATA_FROM_CILANTRO_AND_BRINJAL_AND_INSERT_INTO_CHANNELMANAGER";
    private final Random rand = new Random();


    @Autowired
    @Qualifier("JmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    @Qualifier("JmsTemplateEFT")
    private JmsTemplate jmsTemplateEFT;

    @Autowired
    SYSENV sysenv;

    public void sendToQueueGePGRemittance(RemittanceToQueue req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_GEPG_REMITTANCE_TO_BOT, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_GEPG_REMITTANCE_TO_BOT), req);
        LOGGER.info("Submitted to {}", QUEUE_GEPG_REMITTANCE_TO_BOT);
    }

    public void sendToQueueGePGToSwift(RemittanceToQueue req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_GEPG_REMITTANCE_TO_SWIFT, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_GEPG_REMITTANCE_TO_SWIFT), req);
        LOGGER.info("Submitted to {}", QUEUE_GEPG_REMITTANCE_TO_SWIFT);
    }

    public void sendToQueueRTGSToSwift(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_RTGS_FILE_REQ, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_RTGS_FILE_REQ), req);
        LOGGER.info("Submitted to {}", QUEUE_RTGS_FILE_REQ);
    }

    public int sendToQueueRTGSToSwiftFromKprinterPool(String req) {
        int response = 1;
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_RTGS_FILE_REQ, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_RTGS_FILE_REQ), req);
        LOGGER.info("Submitted to {}", QUEUE_RTGS_FILE_REQ);
        return response;
    }

    /*
    BOT TISS VPN
     */
    public void sendToQueueRTGSToBoTVPN(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_BOT_TISS_VPN, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_BOT_TISS_VPN), req);
        LOGGER.info("Submitted to {}", QUEUE_BOT_TISS_VPN);
    }

    public void sendToQueueWithHodlingTaxPosting(RemittanceToQueue req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_RTGS_WITH_HOLDING_TAX, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_RTGS_WITH_HOLDING_TAX), req);
        LOGGER.info("Submitted to {}", QUEUE_RTGS_WITH_HOLDING_TAX);
    }

    public void sendToQueueEftFrmcustAcctToBrnchEFTLedger(RemittanceToQueue req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER), req);
        LOGGER.info("Submitted to {}", QUEUE_EFT_BATCH_TXN_FRM_CUSTOMER_TO_BRANCH_LEDGER);
    }

    public void sendToQueueEftFrmBrnchLedgeToHqLedger(RemittanceToQueue req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ), req);
        LOGGER.info("Submitted to {} with ref: {}", QUEUE_EFT_BATCH_TXN_FRM_BRANCH_TO_HQ, req.getReferences());
    }

    public void sendToQueueEftFrmCBSToTACH(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", FROM_CBS_TO_TACH_QUEUE, req);
        jmsTemplateEFT.convertAndSend(new ActiveMQQueue(FROM_CBS_TO_TACH_QUEUE), req);
        LOGGER.info("Submitted to {}", FROM_CBS_TO_TACH_QUEUE);
    }

    public void sendToQueueEftIncomingToCBS(EftPacs00800102Req req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_EFT_INCOMING_TO_CBS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_EFT_INCOMING_TO_CBS), req);
        LOGGER.info("Submitted to {}", QUEUE_EFT_INCOMING_TO_CBS);
    }

    public void sendToQueueEftReplayToCoreBanking(ReplayIncomingTransactionReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_EFT_REPLAY_TO_CBS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_EFT_REPLAY_TO_CBS), req);
        LOGGER.info("Submitted to {}", QUEUE_EFT_REPLAY_TO_CBS);
    }

    public void sendToQueueRTGSReplayToCoreBanking(ReplayIncomingTransactionReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", QUEUE_RTGS_REPLAY_TO_CBS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(QUEUE_RTGS_REPLAY_TO_CBS), req);
        LOGGER.info("Submitted to {}", QUEUE_RTGS_REPLAY_TO_CBS);
    }

    public void sendToQueueBatchTransaction(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_PAYMENTS_FROM_IBANK, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_PAYMENTS_FROM_IBANK), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_PAYMENTS_FROM_IBANK);
    }

    public void sendToQueueBatchTransactionFromBranch(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_PAYMENTS_FROM_BRANCH, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_PAYMENTS_FROM_BRANCH), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_PAYMENTS_FROM_BRANCH);
    }

    public void sendToQueueSingleTxnFrmBranchToHQ(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK), req);
        LOGGER.info("Submitted to {}", PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK);
    }

    public void sendToQueueProcessBatchTxnCharge(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_TRANSACTION_CHARGE, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_TRANSACTION_CHARGE), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_TRANSACTION_CHARGE);
    }

    public void sendToQueueProcessBatchTxnChargeFromBranch(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_TRANSACTION_CHARGE_FROM_BRANCH, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_TRANSACTION_CHARGE_FROM_BRANCH), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_TRANSACTION_CHARGE_FROM_BRANCH);
    }

    public void sendToQueueProcessBatchMultipleEntries(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_TRANSACTION_MULTIPLE_ENTRIES, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_TRANSACTION_MULTIPLE_ENTRIES), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_TRANSACTION_MULTIPLE_ENTRIES);
    }

    public void sendToQueueProcessSingleEntriesToCBS(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING), req);
        LOGGER.info("Submitted to {}", PROCESS_SINGLE_BATCH_PAYMENTS_FROM_IBANK_TO_CORE_BANKING);
    }

    public void sendToQueueProcessMultipleDebits(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS);
    }

    public void sendToQueueProcessMultipleDebitsPosting(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_PAYMENTS_FROM_MULTIPLE_DEBITS_POSTING);
    }

    public void sendToQueueProcessBookTransfer(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BOOK_TRANSFER, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BOOK_TRANSFER), req);
        LOGGER.info("Submitted to {}", PROCESS_BOOK_TRANSFER);
    }
    public void sendToQueueProcessMultipleDebitsToSuspense(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_MULTIPLE_DEBITS_TO_SUSPENSE_LEDGER, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_MULTIPLE_DEBITS_TO_SUSPENSE_LEDGER), req);
        LOGGER.info("Submitted to {}", PROCESS_MULTIPLE_DEBITS_TO_SUSPENSE_LEDGER);
    }
    public void sendToQueueWalletTransferReversal(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_WALLET_TRANSFER_REVERSAL, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_WALLET_TRANSFER_REVERSAL), req);
        LOGGER.info("Submitted to {}", PROCESS_WALLET_TRANSFER_REVERSAL);
    }

    public void sendToQueueOutwardReversal(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT), req);
        LOGGER.info("Submitted to {}", PROCESS_OUTWARD_REVERSAL_AFTER_SETTLEMENT);
    }

    public void sendToQueueOutwardAcknowledgementByTACH(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_OUTWARD_ACKNOWLEDGEMENT_BY_TACH, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_OUTWARD_ACKNOWLEDGEMENT_BY_TACH), req);
        LOGGER.info("Submitted to {}", PROCESS_OUTWARD_ACKNOWLEDGEMENT_BY_TACH);
    }

    public void sendToQueueOutwardAcknowledgementToEmkoPo(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO), req);
        LOGGER.info("Submitted to {}", PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_EMKOPO);
    }

    public void sendToQueueOutwardAcknowledgementToInternetBanking(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK), req);
        LOGGER.info("Submitted to {}", PROCESS_CALLBACK_ACKNOWLEDGEMENT_TO_IBANK);
    }

    public void sendToQueueChargeSpilitingExereciseDuty(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_EXERCISE_DUTY_CHARGE_SPILITING, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_EXERCISE_DUTY_CHARGE_SPILITING), req);
        LOGGER.info("Submitted to {}", PROCESS_EXERCISE_DUTY_CHARGE_SPILITING);
    }

    public void sendToQueueChargeSpilitingValueAddedTax(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_VALUE_ADDED_TAX_SPILITING, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_VALUE_ADDED_TAX_SPILITING), req);
        LOGGER.info("Submitted to {}", PROCESS_VALUE_ADDED_TAX_SPILITING);
    }

    public void sendToQueueChargeSpilitingINCOME(BatchPaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_POSTING_INCOME_TO_CORE_BANKING, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_POSTING_INCOME_TO_CORE_BANKING), req);
        LOGGER.info("Submitted to {}", PROCESS_POSTING_INCOME_TO_CORE_BANKING);
    }

    public void sendToQueueLukuPaymentCallback(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_LUKU_CALLBACK, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_LUKU_CALLBACK), req);
        LOGGER.info("Submitted to {}", PROCESS_LUKU_CALLBACK);
    }

    public void sendToQueueMPESAPaymentCallback(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW), req);
        LOGGER.info("Submitted to {}", CILANTRO_PROCESS_MPESA_CALLBACK_FROM_GW);
    }

    public void sendToQueueIbankOrMobAmmendimentCallback(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT), req);
        LOGGER.info("Submitted to {}", PROCESS_IBANK_OR_MOB_CALLBACK_FOR_AMMENDIMENT);
    }

    public void sendToQueueMT202IncomingMessage(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_INCOMING_MT202_MESSAGES, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_INCOMING_MT202_MESSAGES), req);
        LOGGER.info("Submitted to {}", PROCESS_INCOMING_MT202_MESSAGES);
    }

    public void sendToQueueMT103RefundToCustomer(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER), req);
        LOGGER.info("Submitted to {}", PROCESS_INCOMING_MT103_REFUND_TO_CUSTOMER);
    }

    public void sendToQueueRTGSIncomingForLoggingToDB(RTGSTransferForm req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_INCOMING_RTGS_TO_CILANTRODB, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_INCOMING_RTGS_TO_CILANTRODB), req);
        LOGGER.info("Submitted to {}", PROCESS_INCOMING_RTGS_TO_CILANTRODB);
    }

    public void sendToQueueMT103StpOutgoing(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_STP_MT103_OUTWARD_TRANSACTIONS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_STP_MT103_OUTWARD_TRANSACTIONS), req);
        LOGGER.info("Submitted to {}", PROCESS_STP_MT103_OUTWARD_TRANSACTIONS);
    }

    public void sendToQueueMTSTPSwiftInwardMessage(RTGSTransferForm req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", INCOMING_SWIFT_STP_INWARD_TRANSACTION, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(INCOMING_SWIFT_STP_INWARD_TRANSACTION), req);
        LOGGER.info("Submitted to {}", INCOMING_SWIFT_STP_INWARD_TRANSACTION);
    }

    public void sendToQueuePsssfPensionPayroll(PsssfBatchRequest req) {
        LOGGER.debug("sending with convertAndSend() to {} <{}>", PENSION_PAYROLL_BATCH_TRANSACTIONS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PENSION_PAYROLL_BATCH_TRANSACTIONS), req);
        LOGGER.info("Submitted to {}", PENSION_PAYROLL_BATCH_TRANSACTIONS);
    }

    public void sendToQueuePsssfPensionerAccountVerification(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PENSION_PAYROLL_PENSIONER_ACCOUNT_VERIFICATION, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PENSION_PAYROLL_PENSIONER_ACCOUNT_VERIFICATION), req);
        LOGGER.info("Submitted to {}", PENSION_PAYROLL_PENSIONER_ACCOUNT_VERIFICATION);
    }

    public void sendToQueuePsssfPensionerUpdateRecordsPerCbs(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE), req);
        LOGGER.info("Submitted to {}", PENSION_PAYROLL_PENSIONER_ACCOUNT_NAMEQUERY_UPDATE);
    }

    public void sendToQueuepensionPayrollToCoreBanking(PensionPayrollToCoreBanking req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING), req);
        LOGGER.info("Submitted to {} with ref:{}", PENSION_PAYROLL_PENSIONER_PROCESS_TO_COREBANKING, req.getReference());
    }

    public void sendToQueuePensionPayrollUpdateTransactions(String req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS), req);
        LOGGER.info("Submitted to {} with ref:{}", PENSION_PAYROLL_PENSIONER_ACCOUNT_UPDATE_AFTER_PAYMENTS, req);
    }

    public void sendToQueueMT950EntriesForReport(Mt950ObjectReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_MT950_ENTRIES_FOR_REPORTS, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_MT950_ENTRIES_FOR_REPORTS), req);
        LOGGER.info("Submitted to {}", PROCESS_MT950_ENTRIES_FOR_REPORTS);
    }

    public void sendToQueueProcessBatchPaymentsToBoT(PaymentReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", PROCESS_BATCH_PAYMENTS_TO_BOT, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_BATCH_PAYMENTS_TO_BOT), req);
        LOGGER.info("Submitted to {}", PROCESS_BATCH_PAYMENTS_TO_BOT);
    }

    public void sendQueueOTPEmailConsumer(String msg) {
        StopWatch watch = new StopWatch();
        watch.start();
        jmsTemplate.convertAndSend(QUEUE_OTP_EMAIL, msg);
        LOGGER.info("Submitted to {} <{}>", QUEUE_OTP_EMAIL, msg);
        watch.stop();
        LOGGER.info("{}ms producer's queue duration", watch.getTotalTimeMillis());
    }


    public void sendQueueSendSMS(String payload) {

        StopWatch watch = new StopWatch();
        watch.start();
        LOGGER.info("[{}] receives payload - [{}]", QUEUE_SEND_SMS, payload);
        String[] payloadArray = payload.split("\\^");
        String msgText;
        String msgTo;
        String msgId = String.valueOf(System.currentTimeMillis()).substring(3, 13) + "" + rand.nextInt(100000);
        LOGGER.info("[{}] parse payload - [payloadLength]={}", QUEUE_SEND_SMS, payloadArray.length);
        if (payloadArray.length >= 2) {
            msgText = payloadArray[0];
            msgTo = payloadArray[1];
            HttpClientService.sendSMS(msgText, msgTo, msgId, sysenv.TPB_SMSC_GATEWAY_URL);
        } else {
            LOGGER.error("[{}] Invalid payload - [{}]", QUEUE_SEND_SMS, payload);
        }
        watch.stop();
        LOGGER.info("{}ms consumer's queue duration", watch.getTotalTimeMillis());
    }

    public void downloadGeneralReconDataToReconTracker() {
        LOGGER.info("sending with convertAndSend() to {}", DOWNLOAD_GENERAL_RECON_DATA_TO_RECON_TRACKER);
        jmsTemplate.convertAndSend(new ActiveMQQueue(DOWNLOAD_GENERAL_RECON_DATA_TO_RECON_TRACKER), "Downloading recon data to recon tracker");
        LOGGER.info("Submitted to {}", DOWNLOAD_GENERAL_RECON_DATA_TO_RECON_TRACKER);
    }

    public void sendToQueueBOTClosingBalance(Mt950ObjectReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", SEND_TO_QUEUE_BOT_CLOSING_BALANCE, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(SEND_TO_QUEUE_BOT_CLOSING_BALANCE), req);
        LOGGER.info("Submitted to {} <{}>", SEND_TO_QUEUE_BOT_CLOSING_BALANCE, req);
    }

    public void insertIntoTempFromPensioners(String[] batchReferences) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", INSERT_INTO_TEMP_FROM_PENSIONERS, batchReferences);
        jmsTemplate.convertAndSend(new ActiveMQQueue(INSERT_INTO_TEMP_FROM_PENSIONERS), batchReferences);
        LOGGER.info("Submitted to {} <{}>", INSERT_INTO_TEMP_FROM_PENSIONERS, batchReferences);
    }

    public void resolveReconException() {
        LOGGER.info("sending with convertAndSend() to {} <{}>", RESOLVE_RECON_EXCEPTION, "");
        jmsTemplate.convertAndSend(new ActiveMQQueue(RESOLVE_RECON_EXCEPTION), "");
        LOGGER.info("Submitted to {} <{}>", RESOLVE_RECON_EXCEPTION, "");
    }

    public void insertDuplicateTxnReconTracker() {
        LOGGER.info("sending with convertAndSend() to {} <{}>", INSERT_DUPLICATE_TXN_INTO_RECON_TRACKER, "");
        jmsTemplate.convertAndSend(new ActiveMQQueue(INSERT_DUPLICATE_TXN_INTO_RECON_TRACKER), "");
        LOGGER.info("Submitted to {} <{}>", INSERT_DUPLICATE_TXN_INTO_RECON_TRACKER, "");
    }

    public void checkQueuedTxnsInPhlQueue() {
        LOGGER.info("sending with convertAndSend() to {} <{}>", CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE, "");
        jmsTemplate.convertAndSend(new ActiveMQQueue(CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE), "Running garbage collector for queued transactions");
        LOGGER.info("Submitted to {} <{}>", CHECK_CILANT_QUEUED_TXNS_IN_PHL_QUEUE, "");
    }

//    public void downloadAirtelVikobaToCbsTransactionsTable() {
//        LOGGER.info("sending with convertAndSend() to {} <{}>", DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE, "");
//        jmsTemplate.convertAndSend(new ActiveMQQueue(DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE), "Running garbage collector for downloading airtel vikoba transactions to cbs transactions table in cilantro");
//        LOGGER.info("Submitted to {} <{}>", DOWNLOAD_AIRTEL_VIKOBA_TRANSACTIONS_TO_CBS_TRANSACTIONS_TABLE, "");
//    }

    public void logBatchTransactionsAssignedToCMQueue(BatchPayemntReq req) {
        LOGGER.info("sending with convertAndSend() to {} <{}>", TRANSACTIONS_ASSIGNED_TO_CHANNELMANAGER_QUEUE, req);
        jmsTemplate.convertAndSend(new ActiveMQQueue(TRANSACTIONS_ASSIGNED_TO_CHANNELMANAGER_QUEUE), req);
        LOGGER.info("Submitted to {} <{}>", TRANSACTIONS_ASSIGNED_TO_CHANNELMANAGER_QUEUE, req);
    }


//    public void sendToQueueGabbageCollectorAwaitingCallBack() {
//        LOGGER.info("sending with convertAndSend() Transfer Failed, Setlled core banking, Recon is needed to move the fund from branch GL to BoT GL {} <{}>", PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL, "");
//        jmsTemplate.convertAndSend(new ActiveMQQueue(PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL),"Running garbage collector for transactions in eft branch gl");//send to local database
//        LOGGER.info("Submitted to .... {} <{}>", PROCESS_FAILED_TO_SETTLE_CBS_NEED_MOVE_FUND_TO_BOT_GL, "");
//    }

    public void downloadDataEnhanceFromBothCilantroAndBrinjalToChannelManager() {
        LOGGER.info("sending with convertAndSend() to {} <{}>", DOWNLOAD_DATA_FROM_CILANTRO_AND_BRINJAL_AND_INSERT_INTO_CHANNELMANAGER,"");
        jmsTemplate.convertAndSend(new ActiveMQQueue(DOWNLOAD_DATA_FROM_CILANTRO_AND_BRINJAL_AND_INSERT_INTO_CHANNELMANAGER),"Running schedure to download data from both cilantro and brinjal");
        LOGGER.info("Submitted to {} <{}>", DOWNLOAD_DATA_FROM_CILANTRO_AND_BRINJAL_AND_INSERT_INTO_CHANNELMANAGER,"");
    }
}
