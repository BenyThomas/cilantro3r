/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.AccountNameQuery;
import com.DTO.Ebanking.CreateCardRequest;
import com.DTO.IBANK.*;
import com.DTO.RemittanceToQueue;
import com.DTO.Teller.RTGSTransferForm;
import com.DTO.swift.other.FundFINTransferReq;
import com.DTO.tips.TipsPaymentRequest;
import com.DTO.tips.TipsPaymentResponse;
import com.DTO.visaCardTracingObject;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.queue.QueueProducer;
import com.repository.tips.TipsRepository;
import com.service.*;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import philae.ach.ProcessOutwardRtgsTransfer;
import philae.ach.TaResponse;
import philae.ach.TaTransfer;
import philae.api.PostDepositToGLTransfer;
import philae.api.PostTransferPayment;
import philae.api.TpRequest;
import philae.api.TxRequest;
import philae.api.XaResponse;

import javax.annotation.Nonnull;

/**
 * @author melleji.mollel
 */
@Repository
public class WebserviceRepo {

    @Autowired
    SYSENV systemVariable;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    IbankRepo ibankRepo;

    @Autowired
    TipsRepository tipsRepository;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;
    @Lazy
    @Autowired
    EbankingRepo ebankingRepo;
    @Autowired
    QueueProducer queueProducer;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebserviceRepo.class);

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CorebankingService corebankingService;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;
    private TransferService transferService;
    private TransfersRepository transfersRepository;

    @Autowired
    SwiftRepository swiftRepository;

    @Autowired
    AllowedSTPAccountRepository allowedSTPAccountRepository;

    @Autowired
    SignRequest sign;

    public String fxValidation(String payloadReq) {

        FxValidationReq fxReq = XMLParserService.jaxbXMLToObject(payloadReq, FxValidationReq.class);
        FxValidationResp fxResp = new FxValidationResp();
        List<Map<String, Object>> result = null;
        try {
            SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //get FX from special rates table
            String sql = "select * from transfer_special_rates where fxToken=? and senderAcct=? and currency =? and amount=?";
            // LOGGER.info(sql.replace("?", "'{}'"), fxReq.getFxToken(), fxReq.getSenderAccount(), fxReq.getSendingCurrency(), fxReq.getAmount());
            result = this.jdbcTemplate.queryForList("select * from transfer_special_rates where fxToken=? and senderAcct=? and currency =? and amount=?", fxReq.getFxToken(), fxReq.getSenderAccount(), fxReq.getSendingCurrency(), fxReq.getAmount());
            if (result != null) {
                //check validity date of the fxToken
                Date d1 = sdformat.parse(DateUtil.now());
                Date fxValidTo = sdformat.parse(result.get(0).get("valid_to").toString());
                if (d1.compareTo(fxValidTo) > 0) {
                    //FX SPECIAL RATE EXPIRED
                    fxResp.setApprovedEchangeRate(new BigDecimal(result.get(0).get("approved_rate").toString()));
                    fxResp.setMessage("Special Rate provided already expired.");
                    fxResp.setResponseCode("-2");
                    fxResp.setSystemRate(new BigDecimal(result.get(0).get("system_rate").toString()));
                    fxResp.setExpireDate(sdformat.parse(result.get(0).get("valid_to").toString()));
                } else {
                    //FX SPECIAL RATE IF VALID
                    fxResp.setApprovedEchangeRate(new BigDecimal(result.get(0).get("approved_rate").toString()));
                    fxResp.setMessage("Success");
                    fxResp.setResponseCode("0");
                    fxResp.setSystemRate(new BigDecimal(result.get(0).get("system_rate").toString()));
                    fxResp.setExpireDate(sdformat.parse(result.get(0).get("valid_to").toString()));
                }

            } else {
                //SPECIAL RATE IS NOT SET FOR THIS CUSTOMER
                fxResp.setApprovedEchangeRate(new BigDecimal("-1"));
                fxResp.setMessage("No special rates found for this Token");
                fxResp.setResponseCode("96");
                fxResp.setSystemRate(new BigDecimal("-1"));
                fxResp.setExpireDate(sdformat.parse("0000-00-00"));
            }
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            fxResp.setApprovedEchangeRate(new BigDecimal("-1"));
            fxResp.setMessage("An error occured during processing!!!");
            fxResp.setResponseCode("99");
            fxResp.setSystemRate(new BigDecimal("-1"));

        }
        String response = XMLParserService.jaxbGenericObjToXML(fxResp, Boolean.FALSE, Boolean.TRUE);
        LOGGER.info("FX VALIDATION REQ &RESP:\n {} \n{}", payloadReq, response);
        return response;

    }

    //    public String fxRequest(String payloadReq) {
//
//        FxRequest fxReq = XMLParserService.jaxbXMLToObject(payloadReq, FxRequest.class);
//        FxValidationResp fxResp = new FxValidationResp();
//        List<Map<String, Object>> result = null;
//
//        try {
//            SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            //get FX from special rates table
//            String sql = "select * from transfer_special_rates where fxToken=? and senderAcct=? and currency =? and amount=?";
//            LOGGER.info(sql.replace("?", "'{}'"), fxReq.getFxToken(), fxReq.getSenderAccount(), fxReq.getSendingCurrency(), fxReq.getAmount());
//            result = this.jdbcTemplate.queryForList("select * from transfer_special_rates where fxToken=? and senderAcct=? and currency =? and amount=?", fxReq.getFxToken(), fxReq.getSenderAccount(), fxReq.getSendingCurrency(), fxReq.getAmount());
//            if (result != null) {
//                //check validity date of the fxToken
//                Date d1 = sdformat.parse(DateUtil.now());
//                Date fxValidTo = sdformat.parse(result.get(0).get("valid_to").toString());
//                if (d1.compareTo(fxValidTo) > 0) {
//                    //FX SPECIAL RATE EXPIRED
//                    fxResp.setApprovedEchangeRate(new BigDecimal(result.get(0).get("approved_rate").toString()));
//                    fxResp.setMessage("Special Rate provided already expired.");
//                    fxResp.setResponseCode("-2");
//                    fxResp.setSystemRate(new BigDecimal(result.get(0).get("system_rate").toString()));
//                    fxResp.setExpireDate(sdformat.parse(result.get(0).get("valid_to").toString()));
//                } else {
//                    //FX SPECIAL RATE IF VALID
//                    fxResp.setApprovedEchangeRate(new BigDecimal(result.get(0).get("approved_rate").toString()));
//                    fxResp.setMessage("Success");
//                    fxResp.setResponseCode("0");
//                    fxResp.setSystemRate(new BigDecimal(result.get(0).get("system_rate").toString()));
//                    fxResp.setExpireDate(sdformat.parse(result.get(0).get("valid_to").toString()));
//                }
//
//            } else {
//                //SPECIAL RATE IS NOT SET FOR THIS CUSTOMER
//                fxResp.setApprovedEchangeRate(new BigDecimal("-1"));
//                fxResp.setMessage("No special rates found for this Token");
//                fxResp.setResponseCode("96");
//                fxResp.setSystemRate(new BigDecimal("-1"));
//                fxResp.setExpireDate(sdformat.parse("0000-00-00"));
//            }
//        } catch (Exception ex) {
//            LOGGER.info(null, ex);
//            fxResp.setApprovedEchangeRate(new BigDecimal("-1"));
//            fxResp.setMessage("An error occured during processing!!!");
//            fxResp.setResponseCode("99");
//            fxResp.setSystemRate(new BigDecimal("-1"));
//
//        }
//        String response = XMLParserService.jaxbGenericObjToXML(fxResp, Boolean.FALSE, Boolean.TRUE);
//        LOGGER.info("FX VALIDATION REQ &RESP:\n {} \n{}", payloadReq, response);
//        return response;
//
//    }
    public String banksList(String payloadReq) {
        BanksListReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, BanksListReq.class);
        List<Banks> banks = new ArrayList<>();
        List<Map<String, Object>> result = null;
        BanksListResp banksResp = new BanksListResp();
        try {
            if (banksReq.getBankType().equalsIgnoreCase("LOCAL") || banksReq.getBankType().equalsIgnoreCase("EAPS")) {
                String sql = "select * from banks where concat(name,' ',swift_code,' ',swift_code_test,' ',tips_bank_code,' ',fsp_category,' ',fsp_status) like ? and identifier IN ('LOCAL') ";
//                LOGGER.info("bank query sql...{}",sql.replace("?","{}"),"%" + banksReq.getSearchValue() + "%");
                result = this.jdbcTemplate.queryForList(sql, "%" + banksReq.getSearchValue() + "%");
//                LOGGER.info("bank query response.. {}",result);
            }
            if (banksReq.getBankType().equalsIgnoreCase("INTERNATIONAL")) {
                result = this.jdbcTemplate.queryForList("select * from banks where identifier IN ('EAPS','INTERNATIONAL') and concat(name,' ',swift_code,' ',swift_code_test,' ',fsp_category,' ',fsp_status) like ? ", "%" + banksReq.getSearchValue() + "%");
            }
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    Banks bank = new Banks();
                    bank.setName(result.get(i).get("name").toString());
                    if (systemVariable.ACTIVE_PROFILE.equals("prod")) {
                        bank.setSwiftCode(result.get(i).get("swift_code").toString());
                    } else {
                        bank.setSwiftCode(result.get(i).get("swift_code_test").toString());
                    }
                    bank.setTipsBankCode(result.get(i).get("tips_bank_code").toString());
                    bank.setFspCategory(result.get(i).get("fsp_category").toString());
                    bank.setIdentifier(result.get(i).get("identifier").toString());
                    bank.setFspStatus(result.get(i).get("fsp_status").toString());
                    banks.add(bank);
                }
                banksResp.setBankType(banksReq.getBankType());
                banksResp.setBank(banks);
            } else {
                banksResp.setBankType(banksReq.getBankType());
                banksResp.setBank(banks);
            }

        } catch (Exception ex) {
            banksResp.setBankType(banksReq.getBankType());
            banksResp.setBank(banks);
        }
        String response = XMLParserService.jaxbGenericObjToXML(banksResp, Boolean.FALSE, Boolean.TRUE);
//        LOGGER.info("BANKS REQ &RESP:\n {} \n{}", payloadReq, response);
        return response;

    }


    public String transferPayment(String payloadReq) {
        String identifier = "ach:processOutwardRtgsTransfer";
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        try {
            PaymentReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, PaymentReq.class);
            //String fspCode = this.getTipsFspDestinationCode(banksReq.getBeneficiaryBIC());

            if (banksReq != null) {
                if (banksReq.getDescription() == null || banksReq.getDescription().equals("")) {
                    paymentResponse.setMessage("Narration can not be empty/null");
                    paymentResponse.setReference(banksReq.getReference());
                    paymentResponse.setResponseCode("666");
                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                } else {
                    //check if transaction is allowed on the specific day
                    String dayNames[] = new DateFormatSymbols().getWeekdays();
                    Calendar date2 = Calendar.getInstance();
                    System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
                    String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];
                    String checkIfAllowed = getTransactionCalender(todayName, banksReq.getType());//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY

                    if (checkIfAllowed.equalsIgnoreCase("A")) {
                        String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                        String msgType = "LOCAL";
                        String txnType = "001";
                        RTGSTransferForm rtgsForm = new RTGSTransferForm();
                        rtgsForm.setAmount(banksReq.getAmount().toString());
                        rtgsForm.setBeneficiaryAccount(banksReq.getBeneficiaryAccount().trim());
                        rtgsForm.setBeneficiaryBIC(banksReq.getBeneficiaryBIC());
                        //replace TRA bic
                        if(banksReq.getBeneficiaryBIC().contains("TARATZTZ")){
                            rtgsForm.setBeneficiaryBIC("TANZTZTX");
                        }
                        rtgsForm.setBeneficiaryContact(banksReq.getBeneficiaryContact());
                        rtgsForm.setBeneficiaryName(banksReq.getBeneficiaryName());
                        rtgsForm.setCurrency(banksReq.getCurrency());
                        rtgsForm.setDescription(banksReq.getDescription());
                        rtgsForm.setSenderAccount(banksReq.getSenderAccount());
                        rtgsForm.setSenderName(banksReq.getSenderName());
                        rtgsForm.setSenderAddress(banksReq.getSenderAddress());
                        rtgsForm.setSenderPhone(banksReq.getSenderPhone());
                        rtgsForm.setIntermediaryBank(banksReq.getIntermediaryBank());
                        rtgsForm.setBatchReference(banksReq.getBatchReference());
                        rtgsForm.setRelatedReference(banksReq.getReference());
                        rtgsForm.setReference(banksReq.getReference());
                        banksReq.setRelatedReference(banksReq.getReference());
                        //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                        if (banksReq.getType().equals("004")) {
                            msgType = "INTERNATIONAL";
                            txnType = banksReq.getType();
                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                            }
                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                            }
                        }
                        //TIS TRANSACTION ALONG EAST AFRICA
                        if (banksReq.getType().equals("004") && (banksReq.getCurrency().equalsIgnoreCase("KES") || banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                            msgType = "EAPS";
                            txnType = "001";
                        }
                        //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                        if (banksReq.getType().equals("004") && (!banksReq.getCurrency().equalsIgnoreCase("KES") || !banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                            msgType = "INTERNATIONAL";
                            txnType = "004";
                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                            }
                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                            }
                        }
                        String stpReference = null;
                        //GENERATE SWIFT MESSAGE...
                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && ((banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("IND")) && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0))) {
                            //POST STP FOR INDIVIDUAL
                            //CHECK CURRENCY OF TRANSACTION
                            stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);

                        }

                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && ((banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("COR"))) && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                            //POST STP FOR CORPORATE
                            banksReq.setRelatedReference(banksReq.getReference());
                            stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);
                        }

                        String swiftMessage = SwiftService.createTellerMT103(rtgsForm, Calendar.getInstance().getTime(), systemVariable.SENDER_BIC, msgType, rtgsForm.getReference(), correspondentBank);
                        LOGGER.info("[[{}]]]", swiftMessage);
                        int res = -1;
                        String initialStatus = "I";//INITIAL STATUS OF THE TRANSACTION
                        //PROCESS TO CORE-BANKING AND THIRDPARTY SYSTEM IF ANY
                        SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
                        String transferCuttOff = systemVariable.EFT_TRANSACTION_SESSION_TIME;
                        List<Map<String, Object>> cuttoff = getTransferCuttOff("001");
                        Date startTime = parser.parse("00:00");
                        if (cuttoff != null) {
                            transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
                        }
                        Date endTime = parser.parse(transferCuttOff);
                        Date txnDate = parser.parse(DateUtil.now("HH:mm"));

                        //TODO: THIS is for high valued customer who wants to transact 24 hours
                        if(banksReq.getType().equals("001") && allowedSTPAccountRepository.existsAllowedSTPAccountsByAcctNo(rtgsForm.getSenderAccount())){
                            //add stp reference
                            stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);
                            //re-generate swift with new reference
                            swiftMessage = SwiftService.createTellerMT103ForTissVPN(rtgsForm, Calendar.getInstance().getTime(), systemVariable.SENDER_BIC, msgType, rtgsForm.getReference(), correspondentBank);

                            //saving into the database
                            res = saveIBPaymentsForSpecialCustomer(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                            if (res != -1) {
                                //post to core banking
                                response = this.processRtgsEftPayment(banksReq);//PROCESS THE TRANSACTION TO CORE BANKING.
                            }
                        }else {
                            //check cutt-off time.
                            if (txnDate.after(startTime) && txnDate.before(endTime)) {
                                LOGGER.info("GOING TO LOG TRANSACTION TO CILANTRO DB:{}", payloadReq);
                                res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                                //}
                            } else {
                                //TRANSACTION CUT-OFF REACHED
                                //OVERRIDE CUT-OFF FOR STP TRANSACTIONS
                                //CHECK TIME TO DISABLE
                                LOGGER.info("Transaction logging after cuttoff time...{}", banksReq);
    //                            if ((systemVariable.IS_TIPS_ALLOWED) && (!fspCode.equalsIgnoreCase("-1")) && (!banksReq.getIsGepg().equalsIgnoreCase("Y")) && ((banksReq.getType().equalsIgnoreCase("005") || (banksReq.getType().equalsIgnoreCase("001"))) && (banksReq.getCurrency().equalsIgnoreCase("TZS")) && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.TIPS_MAXIMUM_TRANSFER_LIMIT)) < 0))) {
    //                                res = saveTIPSPayments(rtgsForm, rtgsForm.getReference(), txnType, banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
    //                            } else
                                if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("IND") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                                    //POST STP FOR INDIVIDUAL
                                    res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                                } else if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("COR") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                                    //POST STP FOR CORPORATE
                                    res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                                } else {
                                    initialStatus = "BC";
                                    res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                                    res = 2;//CANNOT POST THIS TRANSACTION AT THIS TIME BECAUSE IT REQUIRES BACK-OFFICE APPROVAL
                                }
                            }
                            if (res == 2) {
                                //its cut-off
                                paymentResponse.setMessage("Transactions received on back-office, will be processed next-business day");
                                paymentResponse.setReference(banksReq.getReference());
                                paymentResponse.setResponseCode("102");
                                paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                paymentResponse.setReceipt(rtgsForm.getReference());
                                response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                            } else if (res != -1) {
                                if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("dev") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("uat")) {
                                    //check transactions with higher values
                                    BigDecimal transAmount = new BigDecimal(rtgsForm.getAmount());
                                    BigDecimal configAmount = new BigDecimal(systemVariable.AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS);
                                    if (transAmount.compareTo(configAmount) == 1) {
                                        paymentResponse.setMessage("Transaction received on bank, will be processed after compliance validations");
                                        paymentResponse.setReference(banksReq.getReference());
                                        paymentResponse.setResponseCode("103");
                                        paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                        paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                        paymentResponse.setReceipt(rtgsForm.getReference());
                                        response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                                    } else {
                                        response = this.processRtgsEftPayment(banksReq);//PROCESS THE TRANSACTION TO CORE BANKING.
                                    }
                                } else {
                                    //SIMULATION OF TRANSACTIONS
                                    paymentResponse.setMessage("SUCCESS");
                                    paymentResponse.setReference(banksReq.getReference());
                                    paymentResponse.setResponseCode("0");
                                    paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                    paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                    paymentResponse.setReceipt(banksReq.getReference());
                                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                                    //simulate swift message to swift system
                                    //queueProducer.sendToQueueRTGSToSwift(swiftMessage + "^" + systemVariable.KPRINTER_URL + "^" + banksReq.getReference());
                                    queueProducer.sendToQueueOutwardAcknowledgementToInternetBanking(banksReq.getReference() + "^SUCCESS");
                                }
                            } else {
                                paymentResponse.setMessage("General error on processing transaction");
                                paymentResponse.setReference(banksReq.getReference());
                                paymentResponse.setResponseCode("99");
                                paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                paymentResponse.setReceipt(rtgsForm.getReference());
                                response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                            }
                        }
                    } else {
                        //transaction is not allowed today
                        paymentResponse.setMessage("You cannot process this payment today, Kindly process this payment on another day");
                        paymentResponse.setReference(banksReq.getReference());
                        paymentResponse.setResponseCode("101");
                        paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                        paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                        paymentResponse.setReceipt(banksReq.getReference());
                        response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                    }
                }
            }

        } catch (ParseException ex) {
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }
        LOGGER.info("\nPAYMENT REQUEST:\n {}\nRESPONSE \n{}", payloadReq, response);
        return response;

    }

    public String transferPaymentMakeSwiftMessage(PaymentReq banksReq, String txnId) {
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        try {
            if (banksReq != null) {
                if (banksReq.getDescription() == null || banksReq.getDescription().equals("")) {
                    paymentResponse.setMessage("Narration can not be empty/null");
                    paymentResponse.setReference(banksReq.getReference());
                    paymentResponse.setResponseCode("666");
                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

                } else {
                    //check if transaction is allowed on the specific day
                    String dayNames[] = new DateFormatSymbols().getWeekdays();
                    Calendar date2 = Calendar.getInstance();
                    System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
                    String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];

                    String checkIfAllowed = getTransactionCalender(todayName, banksReq.getType());//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY

                    if (checkIfAllowed.equalsIgnoreCase("A")) {
                        String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                        String msgType = "LOCAL";
                        String txnType = "001";
                        RTGSTransferForm rtgsForm = new RTGSTransferForm();
                        rtgsForm.setAmount(banksReq.getAmount().toString());
                        rtgsForm.setBeneficiaryAccount(banksReq.getBeneficiaryBIC().trim());
                        rtgsForm.setBeneficiaryBIC(banksReq.getBeneficiaryBIC());
                        rtgsForm.setBeneficiaryContact(banksReq.getBeneficiaryContact());
                        rtgsForm.setBeneficiaryName(banksReq.getBeneficiaryName());
                        rtgsForm.setCurrency(banksReq.getCurrency());
                        rtgsForm.setDescription(banksReq.getDescription());
                        rtgsForm.setSenderAccount(banksReq.getSenderBic());
                        rtgsForm.setSenderName(banksReq.getSenderName());
                        rtgsForm.setSenderAddress(banksReq.getSenderAddress());
                        rtgsForm.setSenderPhone(banksReq.getSenderPhone());
//                        rtgsForm.setIntermediaryBank(banksReq.getIntermediaryBank());
                        rtgsForm.setBatchReference(banksReq.getBatchReference());
                        rtgsForm.setRelatedReference(txnId);
                        rtgsForm.setReference(banksReq.getReference());
                        rtgsForm.setCreateDt(String.valueOf(LocalDateTime.now()));

                        //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                        if (banksReq.getType().equals("004")) {
                            msgType = "INTERNATIONAL";
                            txnType = banksReq.getType();
                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                            }
                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                            }
                        }
                        //TIS TRANSACTION ALONG EAST AFRICA
                        if (banksReq.getType().equals("004") && (banksReq.getCurrency().equalsIgnoreCase("KES") || banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                            msgType = "EAPS";
                            txnType = "001";
                        }
                        //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                        if (banksReq.getType().equals("004") && (!banksReq.getCurrency().equalsIgnoreCase("KES") || !banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                            msgType = "INTERNATIONAL";
                            txnType = "004";
                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                            }
                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                            }
                        }
                        //GENERATE SWIFT MESSAGE...
                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") ) {
                            //POST STP FOR INDIVIDUAL
                            //CHECK CURRENCY OF TRANSACTION
                            String stpReference = null;
                            stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);

                            rtgsForm.setCreateDt(String.valueOf(LocalDateTime.now()));
                            banksReq.setCreateDt(String.valueOf(LocalDateTime.now()));

                        }
                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                            //POST STP FOR CORPORATE
                            String stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);

                            rtgsForm.setCreateDt(String.valueOf(LocalDateTime.now()));
                            banksReq.setCreateDt(String.valueOf(LocalDateTime.now()));
                        }


                        String swiftMessage = SwiftService.createTellerMT103(rtgsForm, Calendar.getInstance().getTime(), systemVariable.SENDER_BIC, msgType, rtgsForm.getReference(), correspondentBank);
                        LOGGER.info("[[{}]]]----> here swift Data", swiftMessage);
                        int res = -1;
                        String initialStatus = "I";//INITIAL STATUS OF THE TRANSACTION
                        //PROCESS TO CORE-BANKING AND THIRDPARTY SYSTEM IF ANY
                        SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
                        String transferCuttOff = systemVariable.EFT_TRANSACTION_SESSION_TIME;
                        List<Map<String, Object>> cuttoff = getTransferCuttOff("001");
                        Date startTime = parser.parse("00:00");
                        if (cuttoff != null) {
                            transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
                        }
                        Date endTime = parser.parse(transferCuttOff);
                        Date txnDate = parser.parse(DateUtil.now("HH:mm"));

                        if (txnDate.after(startTime) && txnDate.before(endTime)) {
                            LOGGER.info("======>>><<<GOING TO LOG TRANSACTION TO CILANTRO DB  **:{}", banksReq);

                            res = saveRUBICONPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());

                        } else {
                            //TRANSACTION CUT-OFF REACHED
                            //OVERRIDE CUT-OFF FOR STP TRANSACTIONS
                            //CHECK TIME TO DISABLE
                            LOGGER.info("Transaction logging after cuttoff time...{}", banksReq);
                            if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS")  && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                                //POST STP FOR INDIVIDUAL
                                res = saveRUBICONPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                            } else if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                                //POST STP FOR CORPORATE
                                res = saveRUBICONPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                            } else {
                                initialStatus = "BC";
                                res = saveRUBICONPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getType(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
                                res = 2;//CANNOT POST THIS TRANSACTION AT THIS TIME BECAUSE IT REQUIRES BACK-OFFICE APPROVAL
                            }
                        }

                        if (res == 2) {
                            //its cut-off
                            paymentResponse.setMessage("Transactions received on back-office, will be processed next-business day");
                            paymentResponse.setReference(banksReq.getReference());
                            paymentResponse.setResponseCode("102");
                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                            paymentResponse.setReceipt(rtgsForm.getReference());
                            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                        } else if (res != -1) {
                            System.out.println("=======>><<<" + true);
                            if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("dev") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("uat")) {
                                //check transactions with higher values
                                BigDecimal transAmount = new BigDecimal(rtgsForm.getAmount());
                                BigDecimal configAmount = new BigDecimal(systemVariable.AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS);
                                //if (transAmount.compareTo(configAmount) == 1) {
                                    paymentResponse.setMessage("Transaction received on bank, will be processed after compliance validations");
                                    paymentResponse.setReference(banksReq.getReference());
                                    paymentResponse.setResponseCode("0");
                                    paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                    paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                    paymentResponse.setReceipt(rtgsForm.getReference());
                                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

                            } else {
                                //SIMULATION OF TRANSACTIONS
                                paymentResponse.setMessage("SUCCESS");
                                paymentResponse.setReference(banksReq.getReference());
                                paymentResponse.setResponseCode("0");
                                paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                paymentResponse.setReceipt(banksReq.getReference());
                            }
                        } else {
                            LOGGER.info("======RES CODE on insert {}", res);

                            paymentResponse.setMessage("General error on processing transaction");
                            paymentResponse.setReference(banksReq.getReference());
                            paymentResponse.setResponseCode("99");
                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                            paymentResponse.setReceipt(rtgsForm.getReference());
                            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                        }
                    } else {
                        //transaction is not allowed today
                        paymentResponse.setMessage("You cannot process this payment today, Kindly process this payment on another day");
                        paymentResponse.setReference(banksReq.getReference());
                        paymentResponse.setResponseCode("101");
                        paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                        paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                        paymentResponse.setReceipt(banksReq.getReference());
                        response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                    }
                }
            }

        } catch (ParseException ex) {
            System.out.println("========> parsed empty" + ex.getMessage());
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }
        LOGGER.info("\nPAYMENT REQUEST:\n {}\nRESPONSE \n{}", banksReq, response);
        return response;

    }


    public String transferPaymentEMKOPOSwiftMessage(PaymentReq banksReq, String txnId) {
        EmkopoResp paymentResponse = new EmkopoResp();
        String response = "";
        try {
            if (banksReq != null) {
                if (banksReq.getDescription() == null || banksReq.getDescription().equals("")) {
                    paymentResponse.setMessage("Narration can not be empty/null");
                    paymentResponse.setReference(banksReq.getReference());
                    paymentResponse.setResponseCode("666");
                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

                    System.out.println("=========> this Response" + response);
                } else {
                    //check if transaction is allowed on the specific day
                    String dayNames[] = new DateFormatSymbols().getWeekdays();
                    Calendar date2 = Calendar.getInstance();
                    System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
                    String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];

                    String checkIfAllowed = getTransactionCalender(todayName, banksReq.getType());//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY

                    if (checkIfAllowed.equalsIgnoreCase("A")) {
                        String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                        String msgType = "LOCAL";
                        String txnType = "001";
                        RTGSTransferForm rtgsForm = new RTGSTransferForm();
                        rtgsForm.setAmount(banksReq.getAmount().toString());
                        rtgsForm.setBeneficiaryAccount(banksReq.getBeneficiaryBIC().trim());
                        rtgsForm.setBeneficiaryBIC(banksReq.getBeneficiaryBIC());
                        rtgsForm.setBeneficiaryContact(banksReq.getBeneficiaryContact());
                        rtgsForm.setBeneficiaryName(banksReq.getBeneficiaryName());
                        rtgsForm.setCurrency(banksReq.getCurrency());
                        rtgsForm.setDescription(banksReq.getDescription());
                        rtgsForm.setSenderAccount(banksReq.getSenderBic());
                        rtgsForm.setSenderName(banksReq.getSenderName());
                        rtgsForm.setSenderAddress(banksReq.getSenderAddress());
                        rtgsForm.setSenderPhone(banksReq.getSenderPhone());
//                        rtgsForm.setIntermediaryBank(banksReq.getIntermediaryBank());
                        rtgsForm.setBatchReference(banksReq.getBatchReference());
                        rtgsForm.setRelatedReference(txnId);
                        rtgsForm.setReference(banksReq.getReference());
                        rtgsForm.setCreateDt(String.valueOf(LocalDateTime.now()));


//                        banksReq.setRelatedReference(banksReq.getReference());
                        //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                        if (banksReq.getType().equals("004")) {
                            msgType = "INTERNATIONAL";
                            txnType = banksReq.getType();
                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                            }
                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                            }
                        }
                        //TIS TRANSACTION ALONG EAST AFRICA
                        if (banksReq.getType().equals("004") && (banksReq.getCurrency().equalsIgnoreCase("KES") || banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                            msgType = "EAPS";
                            txnType = "001";
                        }
                        //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                        if (banksReq.getType().equals("004") && (!banksReq.getCurrency().equalsIgnoreCase("KES") || !banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                            msgType = "INTERNATIONAL";
                            txnType = "004";
                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                            }
                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                            }
                        }
                        //GENERATE SWIFT MESSAGE...
                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") ) {
                            //POST STP FOR INDIVIDUAL
                            //CHECK CURRENCY OF TRANSACTION
                            String stpReference = null;
                            stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);
                            banksReq.setCreateDt("2");

                        }
                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                            //POST STP FOR CORPORATE
                            String stpReference = "STP" + banksReq.getReference().substring(0, 3) + banksReq.getReference().substring(6, banksReq.getReference().length());
                            rtgsForm.setReference(stpReference);
                            banksReq.setReference(stpReference);
                            banksReq.setCreateDt("3");
                        }


                        String swiftMessage = SwiftService.createTellerMT103(rtgsForm, Calendar.getInstance().getTime(), systemVariable.SENDER_BIC, msgType, rtgsForm.getReference(), correspondentBank);

//                        SwiftMessageObject swMessageObject = objectMapper.readValue(mtJson, SwiftMessageObject.class);
                        swiftRepository.saveSwiftMessageInTransferAdvices(swiftMessage, "BOT-VPN", "INCOMING");//save the message in the database for reportings

                        LOGGER.info("[[{}]]]----> here swift Data", swiftMessage);


                        int res = -1;
                        String initialStatus = "I";//INITIAL STATUS OF THE TRANSACTION
                        //PROCESS TO CORE-BANKING AND THIRDPARTY SYSTEM IF ANY
                        SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
                        String transferCuttOff = systemVariable.EFT_TRANSACTION_SESSION_TIME;
                        List<Map<String, Object>> cuttoff = getTransferCuttOff("001");
                        Date startTime = parser.parse("00:00");
                        if (cuttoff != null) {
                            transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
                        }
                        Date endTime = parser.parse(transferCuttOff);
                        Date txnDate = parser.parse(DateUtil.now("HH:mm"));

//                        if (txnDate.after(startTime) && txnDate.before(endTime)) {
                            LOGGER.info("GOING TO LOG TRANSACTION TO CILANTRO DB FROM E MIKOPO  **:{}", banksReq);

                            res = saveEMKOPOPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());

//                        } else {
//                            //TRANSACTION CUT-OFF REACHED
//                            //OVERRIDE CUT-OFF FOR STP TRANSACTIONS
//                            //CHECK TIME TO DISABLE
//                            LOGGER.info("Transaction logging after cuttoff time...{}", banksReq);
////                            if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS")  && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
//                                //POST STP FOR INDIVIDUAL
//                                res = saveEMKOPOPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
////                            } else if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
////                                //POST STP FOR CORPORATE
////                                res = saveEMKOPOPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
////                            } else {
////                                initialStatus = "BC";
////                                res = saveEMKOPOPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getType(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
////                                res = 2;//CANNOT POST THIS TRANSACTION AT THIS TIME BECAUSE IT REQUIRES BACK-OFFICE APPROVAL
////                            }
//                        }

//                        if (res == 2) {
//                            //its cut-off
//                            paymentResponse.setMessage("Transactions received on back-office, will be processed next-business day");
//                            paymentResponse.setReference(banksReq.getReference());
//                            paymentResponse.setResponseCode("102");
//                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
//                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
//                            paymentResponse.setReceipt(rtgsForm.getReference());
//                            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
//                        } else
                            if (res != -1) {
                            System.out.println("=======>><<<" + true);
//                            if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("dev") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("uat")) {
//                                //check transactions with higher values
//                                BigDecimal transAmount = new BigDecimal(rtgsForm.getAmount());
//                                BigDecimal configAmount = new BigDecimal(systemVariable.AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS);
//                                //if (transAmount.compareTo(configAmount) == 1) {
//                                paymentResponse.setMessage("Transaction received on bank, will be processed after compliance validations");
//                                paymentResponse.setReference(banksReq.getReference());
//                                paymentResponse.setResponseCode("0");
//                                paymentResponse.setAvailableBalance(BigDecimal.ZERO);
//                                paymentResponse.setLedgerBalance(BigDecimal.ZERO);
//                                paymentResponse.setReceipt(rtgsForm.getReference());
//                                response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
//
//                            } else {
                                //SIMULATION OF TRANSACTIONS
                                paymentResponse.setMessage("SUCCESS");
                                paymentResponse.setReference(banksReq.getReference());
                                paymentResponse.setResponseCode("0");
                                paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                                paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                                paymentResponse.setReceipt(banksReq.getReference());
                           // }
                        } else {
                            LOGGER.info("======RES CODE on insert {}", res);

                            paymentResponse.setMessage("General error on processing transaction");
                            paymentResponse.setReference(banksReq.getReference());
                            paymentResponse.setResponseCode("99");
                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                            paymentResponse.setReceipt(rtgsForm.getReference());
                            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                        }
                    } else {
                        //transaction is not allowed today
                        paymentResponse.setMessage("You cannot process this payment today, Kindly process this payment on another day");
                        paymentResponse.setReference(banksReq.getReference());
                        paymentResponse.setResponseCode("101");
                        paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                        paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                        paymentResponse.setReceipt(banksReq.getReference());
                        response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                    }
                }
            }

        } catch (ParseException ex) {
            System.out.println("========> parsed empty" + ex.getMessage());
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }
        LOGGER.info("\nPAYMENT REQUEST:\n {}\nRESPONSE \n{}", banksReq, response);
        return response;

    }

    public String bookTransferPayments(String payloadReq) {
        String identifier = "ach:processOutwardRtgsTransfer";
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        PaymentReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, PaymentReq.class);

        if (banksReq != null) {
            if (banksReq.getDescription() == null || banksReq.getDescription().equals("")) {
                paymentResponse.setMessage("Narration can not be empty/null");
                paymentResponse.setReference(banksReq.getReference());
                paymentResponse.setResponseCode("666");
                response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
            } else {
                //check if transaction is allowed on the specific day
                String dayNames[] = new DateFormatSymbols().getWeekdays();
                Calendar date2 = Calendar.getInstance();
                System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
                String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];
                String checkIfAllowed = getTransactionCalender(todayName, banksReq.getType());//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY
                if (checkIfAllowed.equalsIgnoreCase("A")) {
                    String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                    String msgType = "LOCAL";
                    String txnType = "111";
                    RTGSTransferForm rtgsForm = new RTGSTransferForm();
                    rtgsForm.setAmount(banksReq.getAmount().toString());
                    rtgsForm.setBeneficiaryAccount(banksReq.getBeneficiaryAccount().trim());
                    rtgsForm.setBeneficiaryBIC(banksReq.getBeneficiaryBIC());
                    rtgsForm.setBeneficiaryContact(banksReq.getBeneficiaryContact());
                    rtgsForm.setBeneficiaryName(banksReq.getBeneficiaryName());
                    rtgsForm.setCurrency(banksReq.getCurrency());
                    rtgsForm.setDescription(banksReq.getDescription());
                    rtgsForm.setSenderAccount(banksReq.getSenderAccount());
                    rtgsForm.setSenderName(banksReq.getSenderName());
                    rtgsForm.setSenderAddress(banksReq.getSenderAddress());
                    rtgsForm.setSenderPhone(banksReq.getSenderPhone());
                    rtgsForm.setIntermediaryBank(banksReq.getIntermediaryBank());
                    rtgsForm.setBatchReference(banksReq.getBatchReference());
                    rtgsForm.setRelatedReference(banksReq.getReference());
                    rtgsForm.setReference(banksReq.getReference());
                    banksReq.setRelatedReference(banksReq.getReference());
                    //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
//                        if (banksReq.getType().equals("004")) {
//                            msgType = "INTERNATIONAL";
//                            txnType = banksReq.getType();
//                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
//                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
//                            }
//                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
//                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
//                            }
//                        }
                    //TIS TRANSACTION ALONG EAST AFRICA
//                        if (banksReq.getType().equals("004") && (banksReq.getCurrency().equalsIgnoreCase("KES") || banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
//                            msgType = "EAPS";
//                            txnType = "001";
//                        }
                    //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
//                        if (banksReq.getType().equals("004") && (!banksReq.getCurrency().equalsIgnoreCase("KES") || !banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
//                            msgType = "INTERNATIONAL";
//                            txnType = "004";
//                            if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
//                                correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
//                            }
//                            if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
//                                correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
//                            }
//                        }
                    //GENERATE SWIFT MESSAGE...
//                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && ((banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("IND")) && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0))) {
//                            //POST STP FOR INDIVIDUAL
//                            //CHECK CURRENCY OF TRANSACTION
//                            String stpReference = null;
//
//                            if (systemVariable.IS_TIPS_ALLOWED && (banksReq.getType().equalsIgnoreCase("005") || (banksReq.getType().equalsIgnoreCase("001")) && banksReq.getCurrency().equalsIgnoreCase("TZS") && banksReq.getAmount().compareTo(new BigDecimal(systemVariable.TIPS_MAXIMUM_TRANSFER_LIMIT)) < 0)) {
//                                stpReference =  banksReq.getReference().substring(0, 5) + banksReq.getReference().substring(8, banksReq.getReference().length());
//                            }else{
//                                stpReference = "STP" + banksReq.getReference().substring(0, 5) + banksReq.getReference().substring(8, banksReq.getReference().length());
//                            }
//
//                            rtgsForm.setReference(stpReference);
//                            banksReq.setReference(stpReference);
//
//                        }
//                        if (banksReq.getType().equalsIgnoreCase("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && ((banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("COR"))) && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
//                            //POST STP FOR CORPORATE
//                            banksReq.setRelatedReference(banksReq.getReference());
//                            String stpReference = "STP" + banksReq.getReference().substring(0, 5) + banksReq.getReference().substring(8, banksReq.getReference().length());
//                            rtgsForm.setReference(stpReference);
//                            banksReq.setReference(stpReference);
//                        }
//                        String swiftMessage = SwiftService.createTellerMT103(rtgsForm, Calendar.getInstance().getTime(), systemVariable.SENDER_BIC, msgType, rtgsForm.getReference(), correspondentBank);
//                        LOGGER.info("[[{}]]]", swiftMessage);
                    int res = -1;
                    String initialStatus = "I";//INITIAL STATUS OF THE TRANSACTION
                    //PROCESS TO CORE-BANKING AND THIRDPARTY SYSTEM IF ANY
//                        SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
//                        String transferCuttOff = systemVariable.EFT_TRANSACTION_SESSION_TIME;
//                        List<Map<String, Object>> cuttoff = getTransferCuttOff("001");
//                        Date startTime = parser.parse("00:00");
//                        if (cuttoff != null) {
//                            transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
//                        }
//                        Date endTime = parser.parse(transferCuttOff);
//                        Date txnDate = parser.parse(DateUtil.now("HH:mm"));
//                        if (txnDate.after(startTime) && txnDate.before(endTime)) {
                    LOGGER.info("GOING TO LOG TRANSACTION TO CILANTRO DB:{}", payloadReq);
                    res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), "NA", banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
//                        } else {
//                            //TRANSACTION CUT-OFF REACHED
//                            //OVERRIDE CUT-OFF FOR STP TRANSACTIONS
//                            //CHECK TIME TO DISABLE
//                            if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("IND") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
//                                //POST STP FOR INDIVIDUAL
//                                res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
//                            } else if (banksReq.getType().equals("001") && banksReq.getCurrency().equalsIgnoreCase("TZS") && banksReq.getCustomerType() != null && banksReq.getCustomerType().equalsIgnoreCase("COR") && (banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || banksReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
//                                //POST STP FOR CORPORATE
//                                res = saveIBPayments(rtgsForm, rtgsForm.getReference(), banksReq.getType(), banksReq.getInitiatorId(), swiftMessage, banksReq.getCustomerBranch(), initialStatus, banksReq.getCallbackUrl());
//                            } else {
//                                res = 2;//CANNOT POST THIS TRANSACTION AT THIS TIME BECAUSE IT REQUIRES BACK-OFFICE APPROVAL
//                            }
//                        }
                    if (res == 2) {
                        //its cut-off
                        paymentResponse.setMessage("Transaction cut-off time is from 00:00 to 16:00 for both RTGS and EFT; This transaction cannot be processed at this time");
                        paymentResponse.setReference(banksReq.getReference());
                        paymentResponse.setResponseCode("102");
                        paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                        paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                        paymentResponse.setReceipt(rtgsForm.getReference());
                        response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                    } else if (res == 1) {
                        if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("uat") || systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("kajilo")) {
                            response = this.processBTToCoreBanking(banksReq);//PROCESS THE TRANSACTION TO CORE BANKING.
                        } else {
                            //SIMULATION OF TRANSACTIONS
                            paymentResponse.setMessage("SUCCESS");
                            paymentResponse.setReference(banksReq.getReference());
                            paymentResponse.setResponseCode("0");
                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                            paymentResponse.setReceipt(banksReq.getReference());
                            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                            //simulate swift message to swift system
                            queueProducer.sendToQueueOutwardAcknowledgementToInternetBanking(banksReq.getReference() + "^SUCCESS");
                        }
                    } else {
                        paymentResponse.setMessage("General error on processing transaction");
                        paymentResponse.setReference(banksReq.getReference());
                        paymentResponse.setResponseCode("99");
                        paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                        paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                        paymentResponse.setReceipt(rtgsForm.getReference());
                        response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                    }
                } else {
                    //transaction is not allowed today
                    paymentResponse.setMessage("You cannot process this payment today, Kindly process this payment on another day");
                    paymentResponse.setReference(banksReq.getReference());
                    paymentResponse.setResponseCode("101");
                    paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                    paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                    paymentResponse.setReceipt(banksReq.getReference());
                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                }
            }
        }

        LOGGER.info("\nPAYMENT REQUEST:\n {}\nRESPONSE \n{}", payloadReq, response);
        return response;

    }

    public String transfer2Wallet(String payloadReq) {
        LOGGER.info("RECEIVED REQUEST:{}", payloadReq);
        String identifier = "ach:processOutwardRtgsTransfer";
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        try {
            PaymentReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, PaymentReq.class);
            if (banksReq != null) {
                String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                int res = saveTransfer2WalletPayments(banksReq);
                if (res != -1) {
                    response = processTransferToWalletCoreBanking(banksReq);
                } else {
                    paymentResponse.setMessage("An error occured during processing!!!!!");
                    paymentResponse.setReference(banksReq.getReference());
                    paymentResponse.setResponseCode("99");
                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }
        LOGGER.info("\nPAYMENT REQUEST & RESPONSE:\n {} \n{}", payloadReq, response);
        return response;

    }

    public String transferPaymentTest(String payloadReq) {
        String identifier = "ach:processOutwardRtgsTransfer";
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        try {
            PaymentReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, PaymentReq.class);
            if (banksReq != null) {
                String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                String msgType = "LOCAL";
                String txnType = "001";
                RTGSTransferForm rtgsForm = new RTGSTransferForm();
                rtgsForm.setAmount(banksReq.getAmount().toString());
                rtgsForm.setBeneficiaryAccount(banksReq.getBeneficiaryAccount());
                rtgsForm.setBeneficiaryBIC(banksReq.getBeneficiaryBIC());
                rtgsForm.setBeneficiaryContact(banksReq.getBeneficiaryContact());
                rtgsForm.setBeneficiaryName(banksReq.getBeneficiaryName());
                rtgsForm.setCurrency(banksReq.getCurrency());
                rtgsForm.setDescription(banksReq.getDescription());
                rtgsForm.setSenderAccount(banksReq.getSenderAccount());
                rtgsForm.setSenderName(banksReq.getSenderName());
                rtgsForm.setSenderAddress(banksReq.getSenderAddress());
                rtgsForm.setSenderPhone(banksReq.getSenderPhone());
                rtgsForm.setIntermediaryBank(banksReq.getIntermediaryBank());
                rtgsForm.setBatchReference(banksReq.getBatchReference());
                //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                if (banksReq.getType().equals("004")) {
                    msgType = "INTERNATIONAL";
                    txnType = banksReq.getType();
                    if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                        correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                    }
                    if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                        correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                    }
                }
                //TIS TRANSACTION ALONG EAST AFRICA
                if (banksReq.getType().equals("004") && (banksReq.getCurrency().equalsIgnoreCase("KES") || banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                    msgType = "EAPS";
                    txnType = "001";
                }
                //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                if (banksReq.getType().equals("004") && (!banksReq.getCurrency().equalsIgnoreCase("KES") || !banksReq.getCurrency().equalsIgnoreCase("UGS"))) {
                    msgType = "INTERNATIONAL";
                    txnType = "004";
                    if (banksReq.getCurrency().equalsIgnoreCase("USD")) {
                        correspondentBank = systemVariable.USD_CORRESPONDEND_BANK;
                    }
                    if (banksReq.getCurrency().equalsIgnoreCase("EUR")) {
                        correspondentBank = systemVariable.EURO_CORRESPONDEND_BANK;
                    }
                }
                //GENERATE SWIFT MESSAGE...
                String swiftMessage = SwiftService.createTellerMT103(rtgsForm, Calendar.getInstance().getTime(), "TAPBTZT0", msgType, banksReq.getReference(), correspondentBank);
                queueProducer.sendToQueueRTGSToSwift(swiftMessage + "^" + systemVariable.KPRINTER_URL + "^" + banksReq.getReference());
                paymentResponse.setMessage("Success");
                paymentResponse.setReference(banksReq.getReference());
                paymentResponse.setResponseCode("0");
                response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }
        LOGGER.info("\nPAYMENT REQUEST & RESPONSE:\n {} \n{}", payloadReq, response);
        return response;

    }

    //BATCH PAYMENT REQUEST
    public String batchPaymentTransfer(String payloadReq) {
        String identifier = "ach:processOutwardRtgsTransfer";
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        String rejectionReason = "";
        String responseCode = "0";
        try {
            BatchPaymentReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, BatchPaymentReq.class);
            if (banksReq != null) {
                if (banksReq.getMandate().equalsIgnoreCase("D101") || banksReq.getMandate().equalsIgnoreCase("D102")) {//single debit
                    BigDecimal totalBatchAmt = new BigDecimal("0");
                   if(!banksReq.getBatchType().equalsIgnoreCase("006")) {

                       for (PaymentReq req : banksReq.getPaymentRequest()) {
                           totalBatchAmt = totalBatchAmt.add(req.getAmount());
                           if (banksReq.getBatchType().equalsIgnoreCase("005")) {
                               if (req.getAmount().compareTo(new BigDecimal("20000000")) > 0) {
                                   rejectionReason = "Single entry cannot exceeds 20M within the batch";
                                   responseCode = "99";
                               }
                           }
                       }
                   }else{
                       totalBatchAmt=banksReq.getTotalAmt();
                   }

                    if (banksReq.getTotalAmt().compareTo(totalBatchAmt) != 0) {
                        LOGGER.info("AMOUNT FROM BATCH... {}, AND CALCULATED AMOUNT ... {}",banksReq.getTotalAmt(), totalBatchAmt);
                        rejectionReason = "Batch total Amount should be equal to summation of individual entries";
                        responseCode = "99";
                    }
                    if (!banksReq.getCurrency().equalsIgnoreCase("TZS") && (banksReq.getBatchType().equalsIgnoreCase("005") || banksReq.getBatchType().equalsIgnoreCase("111"))) {
                        rejectionReason = "Transactions within EFT BATCH should be TZS only and not otherwise ";
                        responseCode = "99";
                    }
                    if (!responseCode.equalsIgnoreCase("99") && banksReq.getMandate().equalsIgnoreCase("D101")) {
                        //insert to transfers table
                        insertBatchTransactionsToTransfersTable(banksReq);
                        queueProducer.sendToQueueBatchTransaction(banksReq);
                        rejectionReason = "Success";

                    }
                    if (!responseCode.equalsIgnoreCase("99") && banksReq.getMandate().equalsIgnoreCase("D102")) {
                        insertBatchTransactionsToTransfersTable(banksReq);
                        queueProducer.sendToQueueProcessMultipleDebits(banksReq);
                        rejectionReason = "Success";

                    }
                }

            }
            paymentResponse.setMessage(rejectionReason);
            paymentResponse.setReference(banksReq.getBatchReference());
            paymentResponse.setResponseCode(responseCode);
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
        } catch (Exception ex) {
            ex.printStackTrace();
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }

        LOGGER.info("\nPAYMENT REQUEST & RESPONSE:\n {} \n{}", payloadReq, response);
        return response;
    }

    /*
     * SAVE IB/MOBILE PAYMENTS TO CILANTRO SYSTEM
     */
    public Integer saveIBPayments(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, String initialStatus, String callbackUrl) {
        Integer res = -1;
        Integer res2 = -1;
        String cbsStatus = "P";
        BigDecimal transAmount = new BigDecimal(rtgsTransferForm.getAmount());
        BigDecimal configAmount = new BigDecimal(systemVariable.AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS);
        if (transAmount.compareTo(configAmount) == 1) {
            cbsStatus = "BO";
            initialStatus = "P";
        }
        try {
            res = jdbcTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, initialStatus, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, cbsStatus, rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC(), rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), "IB", "OUTGOING", rtgsTransferForm.getBatchReference(), rtgsTransferForm.getRelatedReference(), reference, systemVariable.SENDER_BIC, callbackUrl);
            LOGGER.info(">>>>>>>TRANSACTION LOGGED TO CILANTRO DB, REFERENCE:{},SENDER ACCOUNT:{} DESTINATION ACCOUNT:{} INSERT RESPONSE:{}", reference, rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), res);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    //THIS IS FOR HIGH VALUED CUSTOMERS FOR 24 HOURS
    public Integer saveIBPaymentsForSpecialCustomer(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, String initialStatus, String callbackUrl) {
        Integer res = -1;
        Integer res2 = -1;
        String cbsStatus = "P";
        try {
            res = jdbcTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, initialStatus, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, cbsStatus, rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC(), rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), "IB", "OUTGOING", rtgsTransferForm.getBatchReference(), rtgsTransferForm.getRelatedReference(), reference, systemVariable.SENDER_BIC, callbackUrl);
            LOGGER.info(">>>>>>>Special TRANSACTION LOGGED TO CILANTRO DB, REFERENCE:{},SENDER ACCOUNT:{} DESTINATION ACCOUNT:{} INSERT RESPONSE:{}", reference, rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), res);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveRUBICONPayments(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, String initialStatus, String callbackUrl) {
        Integer res = -1;
        String cbsStatus = "P";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String sysCreatedDate = String.valueOf(LocalDateTime.now().format(formatter));
        String valueDate = String.valueOf(LocalDateTime.now().format(formatter));

        LOGGER.info("============get created date" + sysCreatedDate);
        BigDecimal transAmount = new BigDecimal(rtgsTransferForm.getAmount());
        BigDecimal configAmount = new BigDecimal(systemVariable.AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS);
        if (transAmount.compareTo(configAmount) == 1) {
            cbsStatus = "BO";
            initialStatus = "P";
        }
        try {
            res = jdbcTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl,create_dt,value_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, initialStatus, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, cbsStatus, rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC(), rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), "RUBICON", "OUTGOING", rtgsTransferForm.getBatchReference(), rtgsTransferForm.getRelatedReference(), reference, systemVariable.SENDER_BIC, callbackUrl, sysCreatedDate, valueDate);

            LOGGER.info("===<<<<<<<<<<< HERE RESPONSEEE {}", res);
            LOGGER.info(">>>>>>>TRANSACTION LOGGED TO CILANTRO DB, REFERENCE:{},SENDER ACCOUNT:{} DESTINATION ACCOUNT:{} INSERT RESPONSE:{}", reference, rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), res);
        } catch (Exception e) {
            LOGGER.info("=======>>,,,,,>>> Failed to Add: {}", e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveEMKOPOPayments(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, String initialStatus, String callbackUrl) {
        Integer res = -1;
        Integer res2 = -1;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String sysCreatedDate = String.valueOf(LocalDateTime.now().format(formatter));
        String valueDate = String.valueOf(LocalDateTime.now().format(formatter));

        String cbsStatus = "P";
        BigDecimal transAmount = new BigDecimal(rtgsTransferForm.getAmount());
        BigDecimal configAmount = new BigDecimal(systemVariable.AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS);
        if (transAmount.compareTo(configAmount) == 1) {
            cbsStatus = "BO";
            initialStatus = "P";
        }
        try {
            res = jdbcTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl,create_dt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, initialStatus, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, cbsStatus, rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC(), rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), "E-MKOPO", "OUTGOING", rtgsTransferForm.getBatchReference(), rtgsTransferForm.getRelatedReference(), reference, systemVariable.SENDER_BIC, callbackUrl, sysCreatedDate);

            LOGGER.info("===<<<<<<<<<<< HERE RESPONSEEE {}", res);
            LOGGER.info(">>>>>>>TRANSACTION LOGGED TO CILANTRO DB, REFERENCE:{},SENDER ACCOUNT:{} DESTINATION ACCOUNT:{} INSERT RESPONSE:{}", reference, rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), res);
        } catch (Exception e) {
            LOGGER.info("=======>>,,,,,>>> Failed to Add: {}", e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
     * SAVE IB/MOBILE TIPS PAYMENTS TO CILANTRO SYSTEM
     */
    public Integer saveTIPSPayments(RTGSTransferForm rtgsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, String initialStatus, String callbackUrl) {
        Integer res = -1;
        try {
            res = jdbcTemplate.update("INSERT INTO tips_transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), rtgsTransferForm.getAmount(), reference, initialStatus, initiatedBy, txnType, rtgsTransferForm.getDescription(), rtgsTransferForm.getSenderAddress(), rtgsTransferForm.getSenderPhone(), rtgsTransferForm.getSenderName(), swiftMessage, branchCode, "P", rtgsTransferForm.getBeneficiaryContact(), rtgsTransferForm.getBeneficiaryBIC(), rtgsTransferForm.getBeneficiaryName(), rtgsTransferForm.getCurrency(), "IB", "OUTGOING", rtgsTransferForm.getBatchReference(), rtgsTransferForm.getRelatedReference(), reference, systemVariable.SENDER_BIC, callbackUrl);
            LOGGER.info(">>>>>>>TIPS TRANSACTION LOGGED TO TIPS CILANTRO DB, REFERENCE:{},SENDER ACCOUNT:{} DESTINATION ACCOUNT:{} INSERT RESPONSE:{}", reference, rtgsTransferForm.getSenderAccount(), rtgsTransferForm.getBeneficiaryAccount(), res);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveTransfer2WalletPayments(PaymentReq req) {
        Integer res = -1;
        Integer res2 = -1;
        try {
            res = jdbcTemplate.update("INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getReference(), "I", req.getInitiatorId(), req.getType(), req.getDescription(), req.getSenderAddress(), req.getSenderPhone(), req.getSenderName(), "", req.getCustomerBranch(), "P", req.getBeneficiaryContact(), req.getBeneficiaryBIC(), req.getBeneficiaryName(), req.getCurrency(), "IB", "OUTGOING", req.getBatchReference(), req.getReference(), req.getReference(), systemVariable.SENDER_BIC, req.getCallbackUrl());
            LOGGER.info("INITIATION RESULT: {}", res);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
     Process RTGS Transactions
     */
    public String processRtgsEftPayment(PaymentReq paymentReq) {
        PaymentResp paymentResponse = new PaymentResp();

        //INSTANTIATE A PAYMENT RESPONSE
        try {
            String identifier = "ach:processOutwardRtgsTransfer";
            String txnType = "RTGS";
            if (paymentReq.getType().equalsIgnoreCase("005")) {
                identifier = "ach:processOutwardEftTransfer";
            }
            //generate the request to RUBIKON
            TaTransfer transferReq = new TaTransfer();
            transferReq.setReference(paymentReq.getReference());
            transferReq.setTxnRef(paymentReq.getReference());
            transferReq.setCreateDate(DateUtil.dateToGregorianCalendar(DateUtil.now(), "yyyy-MM-dd HH:mm:ss"));
            transferReq.setEmployeeId(0L);
            transferReq.setSupervisorId(0L);
            transferReq.setTransferType(txnType);
            transferReq.setCurrency(paymentReq.getCurrency());
            transferReq.setAmount(paymentReq.getAmount());
            transferReq.setExchangeRate(new BigDecimal("0"));//special rate if applicable
            transferReq.setReceiverBank(paymentReq.getBeneficiaryBIC());
            transferReq.setReceiverAccount(paymentReq.getBeneficiaryAccount());
            transferReq.setReceiverName(paymentReq.getBeneficiaryName());
            transferReq.setSenderBank(systemVariable.SENDER_BIC);
            transferReq.setSenderAccount(paymentReq.getSenderAccount());
            transferReq.setSenderName(paymentReq.getSenderName());
            transferReq.setDescription(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName());
            transferReq.setTxnId(Long.valueOf(DateUtil.now("yyyyMMdd")));/*todo: needs a work-around on how to generate txid*/

            if (paymentReq.getType().equalsIgnoreCase("001")) {
                transferReq.setScheme("T01");
                transferReq.setContraAccount(systemVariable.TRANSFER_AWAITING_TISS_LEDGER.replace("***", paymentReq.getCustomerBranch()));
                if (paymentReq.getIsGepg() != null && paymentReq.getIsGepg().equalsIgnoreCase("Y")) {
                    //waive charge for gepg interbank transaction
                    transferReq.setScheme("T0189");
                }
            } else if (paymentReq.getType().equalsIgnoreCase("004")) {
                transferReq.setScheme("T02");
                transferReq.setContraAccount(systemVariable.TRANSFER_AWAITING_TT_LEDGER.replace("***", paymentReq.getCustomerBranch()));
            } else if (paymentReq.getType().equalsIgnoreCase("005") && (paymentReq.getAmount().compareTo(new BigDecimal("20000000")) < 0)) {
                transferReq.setScheme("T01");
                transferReq.setContraAccount(systemVariable.TRANSFER_AWAITING_EFT_LEDGER.replace("***", paymentReq.getCustomerBranch()));
            } else if (paymentReq.getType().equalsIgnoreCase("005") && (paymentReq.getAmount().compareTo(new BigDecimal("20000000")) > 0)) {
                transferReq.setScheme("T01");
                paymentReq.setType("001");
                transferReq.setContraAccount(systemVariable.TRANSFER_AWAITING_TISS_LEDGER.replace("***", paymentReq.getCustomerBranch()));
            } else {
                transferReq.setScheme("T0189");
                transferReq.setContraAccount("0-000-00-0000-0000000");
            }

            transferReq.setReversal(Boolean.FALSE);
            //prepare a role for posting online transfer
            // achRole.setBranchCode(paymentReq.getCustomerBranch());
            transferReq.setUserRole(systemVariable.achUserRole(paymentReq.getCustomerBranch()));
            ProcessOutwardRtgsTransfer postOutwardReq = new ProcessOutwardRtgsTransfer();
            postOutwardReq.setTransfer(transferReq);
            String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
            //process the Request to CBS
            TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRTGSEFTToCore(outwardRTGSXml, identifier), TaResponse.class);
            if (cbsResponse == null) {
                LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", paymentReq.getReference());
                //donot update the transaction status
            }
            if (cbsResponse.getResult() == 0) {
                String comments = "Success";
                //STP TRANSACTIONS VIA MOB/INTERNET BANKING
                if(paymentReq.getType().equalsIgnoreCase("001") && allowedSTPAccountRepository.existsAllowedSTPAccountsByAcctNo(paymentReq.getSenderAccount())) {
                    comments = "Success - 24hrs customer stp account";
                    queueProducer.sendToQueueMT103StpOutgoing(paymentReq);
                }else{

                    if (paymentReq.getType().equalsIgnoreCase("001") && paymentReq.getCurrency().equalsIgnoreCase("TZS") && paymentReq.getCustomerType() != null && paymentReq.getCustomerType().equalsIgnoreCase("IND") && (paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                        //POST STP FOR INDIVIDUAL
                        queueProducer.sendToQueueMT103StpOutgoing(paymentReq);
                    }else if (paymentReq.getType().equalsIgnoreCase("001") && paymentReq.getCurrency().equalsIgnoreCase("TZS") && paymentReq.getCustomerType() != null && paymentReq.getCustomerType().equalsIgnoreCase("COR") && (paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
                        //POST STP FOR CORPORATE
                        queueProducer.sendToQueueMT103StpOutgoing(paymentReq);
                    }else{
                        LOGGER.info("do nothing this is not stp transaction...{} {}", paymentReq.getAmount(),paymentReq.getSenderAccount());
                        //do nothing
                    }

                }

                jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments=?,branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=? where  reference=?",comments, DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getReference());
                LOGGER.info("IBANK TRANSFER PAYMENT=> SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getCustomerBranch(), "SYSTEM-IBANK TRANSACTION");
                if (paymentReq.getType().equalsIgnoreCase("005")) {
                    jdbcTemplate.update("update transfers set status='P',txn_type=?,cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=?,batch_reference=? where  reference=?", paymentReq.getType(), DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getBatchReference(), paymentReq.getReference());
                    //process the transfer to HQ EFT GL
                    RemittanceToQueue remToque = new RemittanceToQueue();
                    remToque.setReferences(paymentReq.getReference());
                    remToque.setBnUser(null);
                    remToque.setUsRole(systemVariable.apiUserRole(paymentReq.getCustomerBranch()));
                    queueProducer.sendToQueueEftFrmBrnchLedgeToHqLedger(remToque);
                }

                ///
                //set payment response
                paymentResponse.setMessage(cbsResponse.getMessage());
                paymentResponse.setReference(paymentReq.getRelatedReference());
                paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
                paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
                paymentResponse.setReceipt(paymentReq.getReference());
            } else {
                jdbcTemplate.update("update transfers set txn_type=?, comments=?,status='F',response_code=?,branch_approved_by='SYSTEM',cbs_status='F',branch_approved_dt=? where  reference=?", paymentReq.getType(), cbsResponse.getMessage() + " : " + cbsResponse.getResult(), cbsResponse.getResult(), DateUtil.now(), paymentReq.getReference());
                //failed on posting CBS
                LOGGER.info("IBANK TRANSFER PAYMENT=>FAILED: RUBIKON RESPONSE:{}, transReference: {}, Amount: {}, BranchCode: {}, POSTED BY: {}", cbsResponse.getResult(), paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getCustomerBranch(), "SYSTEM-IBANK");
                //set payment response
                paymentResponse.setMessage(cbsResponse.getMessage());
                paymentResponse.setReference(paymentReq.getRelatedReference());
                paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
                paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
                paymentResponse.setReceipt(paymentReq.getReference());
            }
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("IBANK TRANSFER PAYMENT [AMBIGUOUS EXCEPTION]: {} BranchCode: {} USERNAME: {}", ex, paymentReq.getCustomerBranch(), "SYSTEM");
            //set payment response
            paymentResponse.setMessage("General Error occured");
            paymentResponse.setReference(paymentReq.getRelatedReference());
            paymentResponse.setResponseCode("91");
            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
            paymentResponse.setReceipt(paymentReq.getReference());
        }
        //}
        String response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
        return response;
    }

    public String processBTToCoreBanking(PaymentReq paymentReq) {
        PaymentResp paymentResponse = new PaymentResp();
        //check transaction currency and account currency
        AccountNameQuery accountNameQuery = corebanking.accountNameQuery(paymentReq.getBeneficiaryAccount());
        //disable tzs
        if (paymentReq.getCurrency().equalsIgnoreCase("TZS") && !accountNameQuery.getAccountCurrency().equalsIgnoreCase("TZS")) {
            paymentResponse.setMessage("Transaction not allowed from TZS to other currencies. Kindly contact your relationship manager for further inquiries");
            paymentResponse.setReference(paymentReq.getRelatedReference());
            paymentResponse.setResponseCode("95");
            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
            paymentResponse.setReceipt(paymentReq.getReference());
        } else

        {
            //check if transaction can be posted via tips
            //check if beneficiary is allowed on tips
//        String fspCode = this.getTipsFspDestinationCode(paymentReq.getBeneficiaryBIC());
//        LOGGER.info("Check if Returned fsp tips code is is not -1:: .... {}", fspCode);
//        if ((!fspCode.equalsIgnoreCase("-1")) && systemVariable.IS_TIPS_ALLOWED && (paymentReq.getType().equalsIgnoreCase("005") || (paymentReq.getType().equalsIgnoreCase("001")) && paymentReq.getCurrency().equalsIgnoreCase("TZS") && paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.TIPS_MAXIMUM_TRANSFER_LIMIT)) < 0)) {
//            paymentReq.setBeneficiaryBIC(fspCode);
//            //posting tips transaction start
//            return   this.processTIPSTransactionFromIBorMobToCoreBanking(paymentReq);
//        } else {

            //INSTANTIATE A PAYMENT RESPONSE
            try {
                String identifier = "api:postTransferPayment";
                String txnType = "RTGS";
//            if (paymentReq.getType().equalsIgnoreCase("005")) {
//                identifier = "ach:processOutwardEftTransfer";
//            }
                //generate the request to RUBIKON
                //get exchange rate

                TpRequest transferReq = new TpRequest();
                transferReq.setReference(paymentReq.getReference());
                transferReq.setReference(paymentReq.getReference());
                List<Map<String, Object>> jsonString = getEchangeRateFrmCBSAPI(paymentReq.getSenderAccount(), paymentReq.getCurrency());
                LOGGER.info("EXCHANGE RATES:{}", jsonString);
                if (paymentReq.getSpRate() == null || paymentReq.getSpRate().isEmpty()) {
                    transferReq.setCreditFxRate(BigDecimal.ZERO);
                    transferReq.setDebitFxRate(BigDecimal.ZERO);
                } else {
                    transferReq.setCreditFxRate(new BigDecimal(paymentReq.getSpRate()));
                    transferReq.setDebitFxRate(new BigDecimal(paymentReq.getSpRate()));
                }
                if (jsonString != null) {
                    LOGGER.info("not null");
                    for (int i = 0; i < jsonString.size(); i++) {
                        LOGGER.info("VALUES ON FX:TYPE:{} RATE:{} RATE2:{},FXTYPE:{}", jsonString.get(i).get("TYPE"), jsonString.get(i).get("RATE"), jsonString.get(i).get("RATE2"), jsonString.get(i).get("FXTYPE"));
//                    if (!jsonString.get(i).get("FXTYPE").toString().contains("NO DEFINED")) {
                        if (jsonString.get(i).get("TYPE").toString().toLowerCase().contains(jsonString.get(i).get("FXTYPE").toString().toLowerCase())) {
                            transferReq.setDebitFxRate(new BigDecimal(jsonString.get(i).get("RATE").toString()));
                            transferReq.setCreditFxRate(new BigDecimal(jsonString.get(i).get("RATE2").toString()));
                        }

                    }
                }
                transferReq.setAmount(paymentReq.getAmount());
                transferReq.setNarration(paymentReq.getDescription());
                transferReq.setScheme("T1089");
                transferReq.setCurrency(paymentReq.getCurrency());
                transferReq.setPayorAccount(paymentReq.getSenderAccount());
                transferReq.setPayeeAccount(paymentReq.getBeneficiaryAccount());
                transferReq.setUserRole(systemVariable.apiUserRole(paymentReq.getCustomerBranch()));

                //prepare a role for posting online transfer
                // achRole.setBranchCode(paymentReq.getCustomerBranch());
                PostTransferPayment postOutwardReq = new PostTransferPayment();
                postOutwardReq.setRequest(transferReq);
                String outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                //process the Request to CBS
                TaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, identifier), TaResponse.class);
                if (cbsResponse == null) {
                    LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", paymentReq.getReference());
                    //donot update the transaction status
                }
                if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                    //STP TRANSACTIONS VIA MOB/INTERNET BANKING
                    //if (amount1.compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1||amount1.compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL))==0) {

//                SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
//                Date maliciousHM = parser.parse("19:00");
//                Date currentHM = parser.parse(DateUtil.now("HH:mm"));
//                LOGGER.info("Current time transaction posted is...{}", currentHM);
//                LOGGER.info("Current time transaction posted is...{}", currentHM);
//                if (currentHM.before(maliciousHM)) {
//                    if (paymentReq.getType().equalsIgnoreCase("001") && paymentReq.getCurrency().equalsIgnoreCase("TZS") && paymentReq.getCustomerType() != null && paymentReq.getCustomerType().equalsIgnoreCase("IND") && (paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.IND_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
//                        //POST STP FOR INDIVIDUAL
//                        queueProducer.sendToQueueMT103StpOutgoing(paymentReq);
//                    }
//                    if (paymentReq.getType().equalsIgnoreCase("001") && paymentReq.getCurrency().equalsIgnoreCase("TZS") && paymentReq.getCustomerType() != null && paymentReq.getCustomerType().equalsIgnoreCase("COR") && (paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == -1 || paymentReq.getAmount().compareTo(new BigDecimal(systemVariable.CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL)) == 0)) {
//                        //POST STP FOR CORPORATE
//                        queueProducer.sendToQueueMT103StpOutgoing(paymentReq);
//                    }
//                } else {
//                    saveMaliciousTimeTransaction(paymentReq);
//                }
                    jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=? where  reference=?", DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getReference());
                    LOGGER.info("IBANK BT TRANSFER PAYMENT=> SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getCustomerBranch(), "SYSTEM-IBANK TRANSACTION");
//                if (paymentReq.getType().equalsIgnoreCase("005")) {
//                    jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=?,batch_reference=? where  reference=?", DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getBatchReference(), paymentReq.getReference());
//                    //process the transfer to HQ EFT GL
//                    RemittanceToQueue remToque = new RemittanceToQueue();
//                    remToque.setReferences(paymentReq.getReference());
//                    remToque.setBnUser(null);
//                    remToque.setUsRole(systemVariable.apiUserRole(paymentReq.getCustomerBranch()));
//                    queueProducer.sendToQueueEftFrmBrnchLedgeToHqLedger(remToque);
//                }

                    ///
                    //set payment response
                    paymentResponse.setMessage(cbsResponse.getMessage());
                    paymentResponse.setReference(paymentReq.getRelatedReference());
                    paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
                    paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                    paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
                    paymentResponse.setReceipt(paymentReq.getReference());
                } else {
                    jdbcTemplate.update("update transfers set comments=?,status='F',response_code=?,branch_approved_by='SYSTEM',cbs_status='F',branch_approved_dt=? where  reference=?", cbsResponse.getMessage() + " : " + cbsResponse.getResult(), cbsResponse.getResult(), DateUtil.now(), paymentReq.getReference());
                    //failed on posting CBS
                    LOGGER.info("IBANK TRANSFER PAYMENT=>FAILED: RUBIKON RESPONSE:{}, transReference: {}, Amount: {}, BranchCode: {}, POSTED BY: {}", cbsResponse.getResult(), paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getCustomerBranch(), "SYSTEM-IBANK");
                    //set payment response
                    paymentResponse.setMessage(cbsResponse.getMessage());
                    paymentResponse.setReference(paymentReq.getRelatedReference());
                    paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
                    paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                    paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
                    paymentResponse.setReceipt(paymentReq.getReference());
                }
            } catch (Exception ex) {
                LOGGER.error(null, ex);
                LOGGER.error("IBANK TRANSFER PAYMENT [AMBIGUOUS EXCEPTION]: {} BranchCode: {} USERNAME: {}", ex, paymentReq.getCustomerBranch(), "SYSTEM");
                //set payment response
                paymentResponse.setMessage("General Error occured");
                paymentResponse.setReference(paymentReq.getRelatedReference());
                paymentResponse.setResponseCode("91");
                paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                paymentResponse.setReceipt(paymentReq.getReference());
            }
        }
        String response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
        return response;
    }

    private int saveMaliciousTimeTransaction(PaymentReq req) {
        int res = -1;
        String maliciousMessage = "Posted within malicious time " + DateUtil.now();
        String initialStatus = "P";
        String branchCode = "-1";
        String txnDirection = "OUTGOING";
        try {
            String query = "INSERT INTO transfers(message_type,sourceAcct, destinationAcct, amount, reference,txid,instrId,batch_reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,message,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency, compliance,direction) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            res = jdbcTemplate.update(query, new Object[]{"-1", req.getSenderAccount(), req.getBeneficiaryAccount(), req.getAmount(), req.getReference(), req.getReference(), req.getReference(), req.getReference(), initialStatus, "SYSTEM", req.getType(), req.getDescription(), req.getSenderAddress(), req.getSenderPhone(), req.getSenderName(), "-1", req.getCustomerBranch(), "C", maliciousMessage, req.getBeneficiaryContact(), req.getBeneficiaryBIC(), req.getBeneficiaryName(), req.getCurrency(), "1", txnDirection});
        } catch (DataAccessException dae) {
            LOGGER.info("Error in database access... {}", dae.getMessage());
        }
        return res;
    }

    public String bookTransfer(String payload) {
        return null;

    }

    public String processTransferToWalletCoreBanking(PaymentReq paymentReq) {
        TxRequest transferReq = new TxRequest();
        PaymentResp paymentResponse = new PaymentResp();
        String identifier = "api:postDepositToGLTransfer";
        transferReq.setAmount(paymentReq.getAmount());
        if (paymentReq.getType().equalsIgnoreCase("061")) {
            transferReq.setScheme("X01");
            transferReq.setCreditAccount(systemVariable.MPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] MPESAB2C");
        } else if (paymentReq.getType().equalsIgnoreCase("062")) {
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ]AIRTELB2C");
            transferReq.setCreditAccount(systemVariable.AIRTELB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("063")) {
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] TIGO2C");
            transferReq.setScheme("X01");
            transferReq.setCreditAccount(systemVariable.TIGOPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("064")) {
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] HALOPESAB2C");
            transferReq.setScheme("X01");
            transferReq.setCreditAccount(systemVariable.HALOPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("065")) {
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] EZYPESAB2C");
            transferReq.setCreditAccount(systemVariable.EZYPESAB2C_LEDGER.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("066")) {//DSTV api:postTransferPayment
            identifier = "api:postTransferPayment";
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] DSTV");
            transferReq.setCreditAccount(systemVariable.DSTV_COLLECTION_ACCOUNT.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("067")) {//AZAMPAY
            identifier = "api:postTransferPayment";
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] AZAMPAY ");
            transferReq.setCreditAccount(systemVariable.AZAM_COLLECTION_ACCOUNT.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("068")) {//STARTIMES
            identifier = "api:postTransferPayment";
            transferReq.setScheme("X01");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] STARTIMES");
            transferReq.setCreditAccount(systemVariable.STARTIMES_COLLECTION_ACCOUNT.replace("***", paymentReq.getCustomerBranch()));
        } else if (paymentReq.getType().equalsIgnoreCase("069")) {//LUKU move to suspense first and wait for callback and move to GePG account
            transferReq.setScheme("X02");
            transferReq.setNarration(paymentReq.getDescription() + " B/O " + paymentReq.getBeneficiaryName() + "[" + paymentReq.getSenderAccount() + ">" + paymentReq.getBeneficiaryAccount() + " ] LUKU");
            transferReq.setCreditAccount(systemVariable.LUKU_SUSPENSE_ACCOUNT.replace("***", paymentReq.getCustomerBranch()));
        }
        transferReq.setDebitAccount(paymentReq.getSenderAccount());
        transferReq.setDebitFxRate(BigDecimal.ZERO);
        transferReq.setCreditFxRate(BigDecimal.ZERO);
        transferReq.setCurrency("TZS");
        transferReq.setReference(paymentReq.getReference());
        transferReq.setUserRole(systemVariable.apiUserRole(paymentReq.getCustomerBranch()));
        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
        postOutwardReq.setRequest(transferReq);
        String outwardRTGSXml = "-1";
        if (paymentReq.getType().equalsIgnoreCase("066") || paymentReq.getType().equalsIgnoreCase("067") || paymentReq.getType().equalsIgnoreCase("068")) {
            outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE).replace("creditAccount", "payeeAccount").replace("debitAccount", "payorAccount");

        } else {
            outwardRTGSXml = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);

        }
        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(outwardRTGSXml, identifier), XaResponse.class);
        if (cbsResponse == null) {
            LOGGER.info("FAILED TO GET RESPONSE FROM CHANNEL MANAGER : trans Reference {}", paymentReq.getReference());
            //donot update the transaction status
        }
        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
            jdbcTemplate.update("update transfers set status='P',cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=? where  reference=?", DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getReference());
            LOGGER.info("IBANK TRANSFER PAYMENT=> SUCCESS: transReference: {} Amount: {} BranchCode: {} POSTED BY: {}", paymentReq.getReference(), paymentReq.getAmount(), paymentReq.getCustomerBranch(), "SYSTEM-IBANK TRANSACTION");
            if (paymentReq.getType().equalsIgnoreCase("061") || paymentReq.getType().equalsIgnoreCase("062") || paymentReq.getType().equalsIgnoreCase("063") || paymentReq.getType().equalsIgnoreCase("064") || paymentReq.getType().equalsIgnoreCase("065") || paymentReq.getType().equalsIgnoreCase("066") || paymentReq.getType().equalsIgnoreCase("067") || paymentReq.getType().equalsIgnoreCase("068") || paymentReq.getType().equalsIgnoreCase("069")) {
//                       public String processTransferToWallet(String type, String msisdn, String amount, String reference, String beneficiaryName, String sourceAcct) {
                String gatewayPaymentResponse = processTransferToWallet(paymentReq.getType(), paymentReq.getBeneficiaryAccount(), paymentReq.getAmount().toString(), paymentReq.getReference(), paymentReq.getBeneficiaryName(), paymentReq.getSenderAccount(), paymentReq.getSenderPhone());//process payment to gateway
                if (gatewayPaymentResponse.equals("200") || gatewayPaymentResponse.equals("0") || gatewayPaymentResponse.equals("7101")) {
                    //transaction is successfully on gateway
                    jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=?,batch_reference=? where  reference=?", DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getBatchReference(), paymentReq.getReference());
                    //set payment response
                    paymentResponse.setMessage(cbsResponse.getMessage());
                    paymentResponse.setReference(paymentReq.getReference());
                    paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
                    paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                    paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
                    //send callback
                    if (paymentReq.getType().equalsIgnoreCase("061")) {
                        //MPESA do nothing on sync callback
                    } else if (paymentReq.getType().equalsIgnoreCase("069")) {
                        //LUKU do nothing on sync callback
                    } else {
                        queueProducer.sendToQueueOutwardAcknowledgementToInternetBanking(paymentReq.getReference() + "^SUCCESS");
                    }
                } else {
                    jdbcTemplate.update("update transfers set status='F',cbs_status='C',comments='Success',branch_approved_by='SYSTEM',branch_approved_dt=?,message=?,response_code=?,batch_reference=? where  reference=?", DateUtil.now(), cbsResponse.getMessage(), cbsResponse.getResult(), paymentReq.getBatchReference(), paymentReq.getReference());
                    paymentResponse.setMessage("Transaction failed on MNO Side, It will be reversed to your account. please try again Later");
                    paymentResponse.setReference(paymentReq.getReference());
                    paymentResponse.setResponseCode("91");
                    paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                    paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
                    queueProducer.sendToQueueWalletTransferReversal(paymentReq);
                }
            } else {
                //set payment response
                paymentResponse.setMessage(cbsResponse.getMessage());
                paymentResponse.setReference(paymentReq.getReference());
                paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
                paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
                paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
            }
        } else {
            paymentResponse.setMessage(cbsResponse.getMessage());
            paymentResponse.setReference(paymentReq.getReference());
            paymentResponse.setResponseCode(String.valueOf(cbsResponse.getResult()));
            paymentResponse.setAvailableBalance(cbsResponse.getAvailableBalance());
            paymentResponse.setLedgerBalance(cbsResponse.getLedgerBalance());
        }
        String response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
        return response;

    }

    public String processTransferToWallet(String type, String beneficiaryAcct, String amount, String reference, String beneficiaryName, String sourceAcct, String senderPhone) {
        String request = "";
        String response = "-1";
        try {
            switch (type) {
                case "061":
                    request = gatewayRequestXML("B2C", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "MPESA", "MPESA", "MPESA", beneficiaryAcct, beneficiaryName, amount, senderPhone, "https://cilantro.tcbbank.co.tz:8443/api/mpesaCallback");
                    break;
                case "062":
                    request = gatewayRequestXML("B2C", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "AIRTELMONEY", "AIRTELMONEY", "AIRTELMONEY", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "063":
                    request = gatewayRequestXML("B2C", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "TIGOPESA", "TIGOPESA", "TIGOPESA", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "064":
                    request = gatewayRequestXML("B2C", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "HALOPESA", "HALOPESA", "HALOPESA", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "065":
                    request = gatewayRequestXML("B2C", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "EZYPESA", "EZYPESA", "EZYPESA", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "066":
                    request = gatewayRequestXML("TV", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "DSTV", "DSTV", "DSTV", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "067":
                    request = gatewayRequestXML("TV", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "AZAM", "AZAM", "AZAMPAY", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "068":
                    request = gatewayRequestXML("TV", "IBANK", "SYNCH", reference, sourceAcct, "TPB", "STARTIMES", "STARTIMES", "STARTIMES", beneficiaryAcct, beneficiaryName, amount, senderPhone, "-1");
                    break;
                case "069":
                    request = gatewayRequestXML("LUKU", "IBANK", "ASYNCH", reference, sourceAcct, "TPB", "LUKU", "LUKU", "LUKU", beneficiaryAcct, beneficiaryName, amount, senderPhone, "https://cilantro.tcbbank.co.tz:8443/api/lukuCallback");
                    break;
            }
            String resFromGw = HttpClientService.sendXMLRequest(request, systemVariable.GW_PAYMENT_URL);
            if (!resFromGw.equalsIgnoreCase("-1")) {
                String resCode = "-1";//XMLParserService.getDomTagText("RESCODE", resFromGw) + "";
                String resDescription = "-1";
                if (resFromGw.contains("RESCODE")) {
                    resCode = XMLParserService.getDomTagText("RESCODE", resFromGw);
                }
                if (resFromGw.contains("RESULTCODE")) {
                    resCode = XMLParserService.getDomTagText("RESULTCODE", resFromGw);
                }
                if (resFromGw.contains("RESULTDESC")) {
                    resDescription = XMLParserService.getDomTagText("RESULTDESC", resFromGw);
                }
                if (resFromGw.contains("RESDESC")) {
                    resDescription = XMLParserService.getDomTagText("RESDESC", resFromGw);
                }
                //Set response code
                if (resCode.equalsIgnoreCase("0") || resCode.equalsIgnoreCase("200") || resDescription.equalsIgnoreCase("success") || resDescription.equalsIgnoreCase("RECEIVED")) {
                    response = "0";
                } else {
                    response = resCode;
                }
            } else {
                response = "-1";
            }
        } catch (Exception e) {
            response = "-1";
        }
        return response;

    }

    public String gatewayRequestXML(String srv, String srcsys, String transtyp, String transid, String srcacc, String pspcode, String dstsp, String spAcc, String spCode, String spcustRef, String spcustName, String transAmt, String custPhone, String callbackUrl) {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n"
                + "<REQ>\n"
                + "    <SRV>" + srv + "</SRV>\n"
                + "    <SRCSYS>" + srcsys + "</SRCSYS>\n"
                + "    <TRANSTYP>" + transtyp + "</TRANSTYP>\n"
                + "    <TRANSID>" + transid + "</TRANSID>\n"
                + "    <SRCACC>" + srcacc + "</SRCACC>\n"
                + "    <PSPCODE>" + pspcode + "</PSPCODE>\n"
                + "    <DSTSP>" + dstsp + "</DSTSP>\n"
                + "    <SPACC>" + spAcc + "</SPACC>\n"
                + "    <SPCODE>" + spCode + "</SPCODE>\n"
                + "    <SPCUSTREF>" + spcustRef + "</SPCUSTREF>\n"
                + "    <SPCUSTNAME>" + spcustName + "</SPCUSTNAME>\n"
                + "    <TRANSAMT>" + transAmt + "</TRANSAMT>\n"
                + "    <CUSTPHONE>" + custPhone + "</CUSTPHONE>\n"
                + "    <CALLBACKURL>" + callbackUrl + "</CALLBACKURL>\n"
                + "</REQ>";
        return xml;
    }

    public String utilityNameQuery(String payload) {
        UtilityNamequeryReq req = XMLParserService.jaxbXMLToObject(payload, UtilityNamequeryReq.class);
        LOGGER.info("REQ:{}", req);
        UtilityNamequeryResp response = new UtilityNamequeryResp();
        String request = "-1";
        try {
            switch (req.getUtilityCode()) {
                case "066"://DSTV
                    request = gatewayRequestXML("NAMEQRY", "IBANK", "SYNCH", req.getReference(), "-1", "TPB", "DSTV", "DSTV", "DSTV", req.getSmartCard(), "TPB", "-1", req.getSmartCard(), "-1");
                    break;
                case "067"://AZAM
                    request = gatewayRequestXML("NAMEQRY", "IBANK", "SYNCH", req.getReference(), "-1", "TPB", "AZAM", "AZAM", "AZAMPAY", req.getSmartCard(), "TPB", "-1", req.getSmartCard(), "-1");
                    break;
                case "068"://stattimes
                    request = gatewayRequestXML("NAMEQRY", "IBANK", "SYNCH", req.getReference(), "-1", "TPB", "STARTIMES", "STARTIMES", "STARTIMES", req.getSmartCard(), "TPB", "-1", req.getSmartCard(), "-1");
                    break;
                case "069"://LUKU
                case "4444":
                    request = gatewayRequestXML("NAMEQRY", "IBANK", "SYNCH", req.getReference(), "-1", "TPB", "LUKU", "LUKU", "LUKU", req.getSmartCard(), "TPB", "-1", req.getSmartCard(), "-1");
                    break;
                default:
            }
            GatewayTxnResp responseGW = XMLParserService.jaxbXMLToObject(HttpClientService.sendXMLRequest(request, systemVariable.GW_PAYMENT_URL), GatewayTxnResp.class);
            LOGGER.info("REQUEST TO GW: {},\n REPONSE FROM GATEWAY: {}", request, responseGW.toString());
            if (responseGW.getRESULTDESC().contains("SUCCESS") || responseGW.getRESULTDESC().contains("succes") || responseGW.getRESULTCODE().equalsIgnoreCase("0") || responseGW.getRESULTCODE().equalsIgnoreCase("7101")) {
                response.setCustomerName(responseGW.getSPCUSTNAME());
                response.setMessage("Success");
                response.setResponseCode("0");
                response.setUtilityCode(req.getUtilityCode());
            } else {
                response.setCustomerName(responseGW.getSPCUSTNAME());
                response.setMessage("Failed");
                response.setResponseCode("99");
                response.setUtilityCode(req.getUtilityCode());
            }
        } catch (Exception e) {
            response.setCustomerName("-1");
            response.setMessage("General Failure");
            response.setResponseCode("96");
            response.setUtilityCode(req.getUtilityCode());
        }
        String resp = XMLParserService.jaxbGenericObjToXML(response, Boolean.FALSE, Boolean.TRUE);
        LOGGER.info("REQ:{} \n RESP:{}", req, resp);
        return resp;
    }

    public String utilityPayments(String payloadReq) {
        String identifier = "ach:processOutwardRtgsTransfer";
        PaymentResp paymentResponse = new PaymentResp();
        String response = "";
        try {
            PaymentReq banksReq = XMLParserService.jaxbXMLToObject(payloadReq, PaymentReq.class);
            if (banksReq != null) {
                String correspondentBank = systemVariable.BOT_SWIFT_CODE;
                int res = saveTransfer2WalletPayments(banksReq);
                if (res != -1) {
                    response = processTransferToWalletCoreBanking(banksReq);
                } else {
                    paymentResponse.setMessage("An error occured during processing!!!!!");
                    paymentResponse.setReference(banksReq.getReference());
                    paymentResponse.setResponseCode("99");
                    response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            paymentResponse.setMessage("General Error occured. Please contact Customer service for support");
            paymentResponse.setReference("9999999999999");
            paymentResponse.setResponseCode("99");
            response = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);

        }
        LOGGER.info("\nPAYMENT REQUEST & RESPONSE:\n {} \n{}", payloadReq, response);
        return response;
    }

    public String lukuGatewayCallback(String payloadReq) {
        queueProducer.sendToQueueLukuPaymentCallback(payloadReq);
        return "<LUKU><RESPONSECODE>0</RESPONSECODE>\n<MESSAGE>Received Successfully</MESSAGE></LUKU>";
    }

    public String mpesaGatewayCallback(String payloadReq) {
        queueProducer.sendToQueueMPESAPaymentCallback(payloadReq);
        return "<MPESA><RESPONSECODE>0</RESPONSECODE>\n<MESSAGE>Received Successfully</MESSAGE></MPESA>";
    }

    public String ibankRegistration(String payLoad) {
        String custRim = XMLParserService.getDomTagText("custNumber", payLoad);
        String accountNo = XMLParserService.getDomTagText("accountNo", payLoad);
        String msisdn = XMLParserService.getDomTagText("msisdn", payLoad);
        String imsi = XMLParserService.getDomTagText("imsi", payLoad);
        String txType = XMLParserService.getDomTagText("txType", payLoad);
        String accountName = XMLParserService.getDomTagText("accountName", payLoad);
        String checksum = XMLParserService.getDomTagText("checksum", payLoad);
        String branch = XMLParserService.getDomTagText("branch", payLoad);
        String visaCardReference = null;
        if (payLoad.contains("visaCardReference")) {
            visaCardReference = XMLParserService.getDomTagText("visaCardReference", payLoad);
        }
        String genChecksum = DigestUtils.md5Hex(DigestUtils.md5Hex(imsi) + custRim + msisdn + txType + accountNo + accountName);
        System.out.println("CHECKSUM RECEIVED: " + checksum);
        System.out.println("CHECKSUM GENERATED: " + genChecksum);
        System.out.println("txType:... " + txType);
        String response = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        if (checksum.equalsIgnoreCase(genChecksum)) {
            //checksum matches
            switch (txType) {
                case "IBANK_REGISTRATION":
                    response = IbankRegistration(custRim, accountNo, msisdn);
                    break;
                case "IBANK_DEACTIVATION":
                    response = IbankDeactivation(custRim, accountNo, msisdn);
                    break;
                case "ATM_DEACTIVATION":
                    break;
                case "MOBILE_APP_REGISTRATION":
                    response = popoteAppRegistration(payLoad, custRim, accountNo, msisdn, "MOBILE");
                    break;
                case "MOBILE_APP_DEACTIVATION":
                    response = popoteAppDeactivation(payLoad, custRim, accountNo, msisdn, "MOBILE");
                    break;
                case "POPOTE_REGISTRATION":
                    break;
                case "VISA_CARD_REGISTRATION":
                    response = visaCardRequest(payLoad, custRim, accountNo, msisdn, branch, "VISA");
                    break;
                case "VISA_PIN_RESET":
                    response = visaCardPinReset(payLoad, genChecksum, custRim, accountNo, msisdn, "VISA",visaCardReference);
                    break;
                case "IBANK_PASSWORD_RESET":
                    response = ibankPasswordReset(custRim, accountNo, msisdn, "IBANK_PASSWORD_RESET");
                    break;

            }
        } else {
            //checksum unmatches
            ebankingRepo.updateChecksum(accountNo, genChecksum);
            response = xmlSelfServiceResponseToGW("96", "Checksum is incorrect", DateUtil.now("yyyyMMddhhmmss"),"-1");
        }

        return response;
    }

    public String mkobaTqsUpdate(String payLoad) {
        String response = "<response><responseCode>0</responseCode><responseMessage></responsemessage></response>";
        String txnid = XMLParserService.getDomTagText("txnid", payLoad);
        String receipt = XMLParserService.getDomTagText("receipt", payLoad);
        String status = XMLParserService.getDomTagText("status", payLoad);
        String desc = XMLParserService.getDomTagText("desc", payLoad);
        ibankRepo.tqsUpdateMkoba(txnid, receipt, desc);
        return response;
    }

    public String IbankRegistration(String custRim, String accountNo, String msisdn) {
        //CREATE USER LOCALLY
        LOGGER.info("GOING TO QUERY ACCOUNT DETAILS FROM CBS: ");
        List<Map<String, Object>> details = ibankRepo.getCustomerAccountDetails(accountNo);
        String resp = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        if (details != null) {
            AddIbankProfile ibankProfile = new AddIbankProfile();
            ibankProfile.setAccountNo(accountNo);
            ibankProfile.setAddress1(details.get(0).get("ADDR_LINE_1") + "");
            ibankProfile.setAddress2(details.get(0).get("ADDR_LINE_2") + "");
            ibankProfile.setAddress3(details.get(0).get("ADDR_LINE_3") + "");
            ibankProfile.setAddress4(details.get(0).get("ADDR_LINE_4") + "");
            ibankProfile.setBranchId(details.get(0).get("BU_NO") + "");
            ibankProfile.setCity(details.get(0).get("CITY") + "");
            ibankProfile.setComments("Registration via digital platform");
            ibankProfile.setCustCategory(details.get(0).get("CUST_CAT") + "");
            ibankProfile.setCustShortName(details.get(0).get("CUST_SHORT_NM") + "");
            ibankProfile.setCustomerId(details.get(0).get("CUST_ID") + "");
            ibankProfile.setCustomerName(details.get(0).get("CUST_NM") + "");
            ibankProfile.setCustomerRim(details.get(0).get("CUST_NO") + "");
            ibankProfile.setEmail("");
            ibankProfile.setMandate("0000");
            ibankProfile.setPfNo("0000");
            ibankProfile.setReceivedDate(DateUtil.now());
            ibankProfile.setRecruitingBrach(details.get(0).get("BU_NO") + "");
            ibankProfile.setState(details.get(0).get("ADDR_LINE_4") + "");
            String reference = "IBP" + details.get(0).get("BU_NO") + DateUtil.now("yyyMMddmmss");
            String customerFullName = details.get(0).get("CUST_NM").toString();

            LOGGER.info("Is first and last customer name not null?.... {} and ...{}", details.get(0).get("FIRST_NM"), details.get(0).get("LAST_NM"));

            String customerUsername = details.get(0).get("FIRST_NM") + "".trim() + "." + details.get(0).get("LAST_NM") + "".trim();

            String customerID = details.get(0).get("CUST_ID") + "";
            //create ib profile
            int profile = ibankRepo.saveIBClientProfile(ibankProfile, reference, msisdn);
            LOGGER.info("RESPONSE ON INSERTING REQUEST TO DB:{}", profile);
            resp = xmlSelfServiceResponseToGW("99", "General Failure", reference,"-1");
            if (profile == 1 || profile == 2) {
                LOGGER.info("CLIENT PROFILE CREATED SUCCESSFULLY: PROFILE_REFERENCE={}, ACCOUNT:{},RIM:{},", reference, accountNo, custRim);
                //add account to profile
                int savedAcct = -1;
                List<Map<String, Object>> accts = ibankRepo.getCustomerAccountsByRIM(custRim);
                for (Map<String, Object> acct : accts) {
                    AddAccountToIBProfile accounts = new AddAccountToIBProfile();
                    accounts.setAccountCurrency(acct.get("CRNCY_CD_ISO") + "");
                    accounts.setAccountName(acct.get("ACCT_NM") + "");
                    accounts.setAccountNo(acct.get("ACCT_NO") + "");
                    accounts.setAcctCategory(acct.get("PROD_CAT_TY") + "");
                    accounts.setAcctDescription(acct.get("PROD_DESC") + "");
                    accounts.setAcctLimit(acct.get("30000000") + "");
                    accounts.setAcctProdCode(acct.get("PROD_CD") + "");
                    accounts.setAcctStatus(acct.get("REC_ST") + "");
                    accounts.setLimitWithoutApproval(acct.get("5000000") + "");
                    accounts.setOldAccount(acct.get("OLD_ACCT_NO") + "");
                    accounts.setLimitWithoutApproval("5000000");
                    accounts.setAcctLimit("5000000");
                    savedAcct = ibankRepo.saveIBClientProfileAccounts(accounts, reference, msisdn);
                    LOGGER.info("ACCOUNT IS SUCCESSFULLY ADDED TO PROFILE: PROFILE_REFERENCE={}, ACCOUNT:{}, ACCOUNT NAME:{}", reference, acct.get("ACCT_NO"), acct.get("ACCT_NM"));

                }
                if (savedAcct != -1) {
                    AddIbankSignatories signatory = new AddIbankSignatories();
                    signatory.setAccountLimit("1000000");
                    signatory.setCustomerId(customerID);
                    signatory.setCustomerId(custRim);
                    signatory.setEmail("");
                    signatory.setFullName(customerFullName.toLowerCase());
                    signatory.setRole(msisdn);
                    signatory.setTransferAccess(reference);
                    signatory.setUsername((customerUsername.toLowerCase()).trim());
                    signatory.setViewAccess(reference);
                    signatory.setPhoneNumber(msisdn);
                    savedAcct = ibankRepo.saveIbankIbankSignatoryMobile(signatory, reference, msisdn, accountNo);
                    if (savedAcct != -1) {
                        String xmlForIBankRegistration = ibankRepo.generateIBankRegistrationXML(reference);
                        String ibankResponse = HttpClientService.sendXMLRequest(xmlForIBankRegistration, systemVariable.IBANK_REGISTRATION_URL);
                        String responseCode = XMLParserService.getDomTagText("responseCode", ibankResponse);
                        if (responseCode.equalsIgnoreCase("0")) {
                            ibankRepo.finalizeIBRegistration(reference);
                            LOGGER.info("CUSTOMER IS SUCCESSFULY CREATED ON IBANK PLATFORM: PROFIL_REFERENCE:{},", reference);
                            resp = xmlSelfServiceResponseToGW("0", "Success", reference,"-1");
                        } else {
                            resp = xmlSelfServiceResponseToGW(responseCode, "Failed registration on IBANK", reference,"-1");
                        }
                    }
                }
            }
        }
        return resp;
    }

    public String ibankPasswordReset(String custRim, String accountNo, String msisdn, String serviceName) {
        //CREATE USER LOCALLY
        LOGGER.info("GOING TO QUERY ACCOUNT DETAILS FROM CBS: ");
        List<Map<String, Object>> details = ibankRepo.getCustomerAccountDetails(accountNo);
        String resp = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        if (details != null) {
            AddIbankProfile ibankProfile = new AddIbankProfile();
            ibankProfile.setAccountNo(accountNo);
            ibankProfile.setAddress1(details.get(0).get("ADDR_LINE_1") + "");
            ibankProfile.setAddress2(details.get(0).get("ADDR_LINE_2") + "");
            ibankProfile.setAddress3(details.get(0).get("ADDR_LINE_3") + "");
            ibankProfile.setAddress4(details.get(0).get("ADDR_LINE_4") + "");
            ibankProfile.setBranchId(details.get(0).get("BU_NO") + "");
            ibankProfile.setCity(details.get(0).get("CITY") + "");
            ibankProfile.setComments("Registration via digital platform");
            ibankProfile.setCustCategory(details.get(0).get("CUST_CAT") + "");
            ibankProfile.setCustShortName(details.get(0).get("CUST_SHORT_NM") + "");
            ibankProfile.setCustomerId(details.get(0).get("CUST_ID") + "");
            ibankProfile.setCustomerName(details.get(0).get("CUST_NM") + "");
            ibankProfile.setCustomerRim(details.get(0).get("CUST_NO") + "");
            ibankProfile.setEmail("");
            ibankProfile.setMandate("0000");
            ibankProfile.setPfNo("0000");
            ibankProfile.setReceivedDate(DateUtil.now());
            ibankProfile.setRecruitingBrach(details.get(0).get("BU_NO") + "");
            ibankProfile.setState(details.get(0).get("ADDR_LINE_4") + "");
            String reference = "IBP" + details.get(0).get("BU_NO") + DateUtil.now("yyyMMddmmss");
            String customerFullName = details.get(0).get("CUST_NM").toString();
            String customerUsername = details.get(0).get("FIRST_NM").toString().trim() + "." + details.get(0).get("LAST_NM").toString().trim();
            String customerID = details.get(0).get("CUST_ID") + "";
            //create ib profile
            int profile = ibankRepo.saveIBClientServiceRequest(customerUsername, ibankProfile, reference, msisdn, serviceName);
            resp = xmlSelfServiceResponseToGW("99", "An error occured during processing customer request", reference,"-1");
            if (profile == 1) {
                LOGGER.info("CLIENT PASSWORD RESET CREATED SUCCESSFULLY WITH: PROFILE_REFERENCE={}, ACCOUNT:{},RIM:{},", reference, accountNo, custRim);
                String xmlForIBankRegistration = ibankRepo.generateIBankpasswordResetXML(reference);
                String ibankResponse = HttpClientService.sendXMLRequest(xmlForIBankRegistration, systemVariable.IBANK_PASSWORD_RESET_URL);
                String responseCode = XMLParserService.getDomTagText("responseCode", ibankResponse);
                if (responseCode.equalsIgnoreCase("0")) {
                    ibankRepo.finalizeIBRegistration(reference);
                    LOGGER.info("CUSTOMER PASSWORD IS SUCCESSFULLY RESET : PROFIL_REFERENCE:{},", reference);
                    resp = xmlSelfServiceResponseToGW("0", "Success", reference,"-1");
                } else {
                    resp = xmlSelfServiceResponseToGW(responseCode, "Ibank failed to process reset of customer password", reference,"-1");
                }
            }

        }
        return resp;
    }

    public String IbankDeactivation(String custRim, String accountNo, String msisdn) {
        //CREATE USER LOCALLY
        LOGGER.info("GOING TO QUERY ACCOUNT DETAILS FROM CBS: ");
        List<Map<String, Object>> details = ibankRepo.getCustomerAccountDetails(accountNo);
        String resp = xmlSelfServiceResponseToGW("99", "General Failure. Cant obtain connection from Core banking", DateUtil.now("yyyyMMddhhmmss"),"-1");
        if (details != null) {
            AddIbankProfile ibankProfile = new AddIbankProfile();
            ibankProfile.setAccountNo(accountNo);
            ibankProfile.setAddress1(details.get(0).get("ADDR_LINE_1") + "");
            ibankProfile.setAddress2(details.get(0).get("ADDR_LINE_2") + "");
            ibankProfile.setAddress3(details.get(0).get("ADDR_LINE_3") + "");
            ibankProfile.setAddress4(details.get(0).get("ADDR_LINE_4") + "");
            ibankProfile.setBranchId(details.get(0).get("BU_NO") + "");
            ibankProfile.setCity(details.get(0).get("CITY") + "");
            ibankProfile.setComments("Registration via digital platform");
            ibankProfile.setCustCategory(details.get(0).get("CUST_CAT") + "");
            ibankProfile.setCustShortName(details.get(0).get("CUST_SHORT_NM") + "");
            ibankProfile.setCustomerId(details.get(0).get("CUST_ID") + "");
            ibankProfile.setCustomerName(details.get(0).get("CUST_NM") + "");
            ibankProfile.setCustomerRim(details.get(0).get("CUST_NO") + "");
            ibankProfile.setEmail("");
            ibankProfile.setMandate("0000");
            ibankProfile.setPfNo("0000");
            ibankProfile.setReceivedDate(DateUtil.now());
            ibankProfile.setRecruitingBrach(details.get(0).get("BU_NO") + "");
            ibankProfile.setState(details.get(0).get("ADDR_LINE_4") + "");
            String reference = "IBP" + details.get(0).get("BU_NO") + DateUtil.now("yyyMMddmmss");
            String customerFullName = details.get(0).get("CUST_NM").toString();
            String customerUsername = details.get(0).get("FIRST_NM").toString().trim() + "." + details.get(0).get("LAST_NM").toString().trim();
            String customerID = details.get(0).get("CUST_ID") + "";
            //create ib profile
            int profile = ibankRepo.saveIBClientProfile(ibankProfile, reference, msisdn);
            resp = xmlSelfServiceResponseToGW("99", "General Failure", reference,"-1");
            if (profile == 1) {
                LOGGER.info("CLIENT PROFILE CREATED SUCCESSFULLY: PROFILE_REFERENCE={}, ACCOUNT:{},RIM:{},", reference, accountNo, custRim);
                //add account to profile
                int savedAcct = -1;
                List<Map<String, Object>> accts = ibankRepo.getCustomerAccountDetails(accountNo);
                for (Map<String, Object> acct : accts) {
                    AddAccountToIBProfile accounts = new AddAccountToIBProfile();
                    accounts.setAccountCurrency(acct.get("CRNCY_CD_ISO") + "");
                    accounts.setAccountName(acct.get("ACCT_NM") + "");
                    accounts.setAccountNo(acct.get("ACCT_NO") + "");
                    accounts.setAcctCategory(acct.get("PROD_CAT_TY") + "");
                    accounts.setAcctDescription(acct.get("PROD_DESC") + "");
                    accounts.setAcctLimit(acct.get("30000000") + "");
                    accounts.setAcctProdCode(acct.get("PROD_CD") + "");
                    accounts.setAcctStatus(acct.get("REC_ST") + "");
                    accounts.setLimitWithoutApproval(acct.get("5000000") + "");
                    accounts.setOldAccount(acct.get("OLD_ACCT_NO") + "");
                    accounts.setLimitWithoutApproval("5000000");
                    accounts.setAcctLimit("30000000");
                    savedAcct = ibankRepo.saveIBClientProfileAccounts(accounts, reference, msisdn);
                    LOGGER.info("ACCOUNT IS SUCCESSFULLY ADDED TO PROFILE: PROFILE_REFERENCE={}, ACCOUNT:{}, ACCOUNT NAME:{}", reference, acct.get("ACCT_NO"), acct.get("ACCT_NM"));
                }
                if (savedAcct != -1) {
                    AddIbankSignatories signatory = new AddIbankSignatories();
                    signatory.setAccountLimit("30000000");
                    signatory.setCustomerId(customerID);
                    signatory.setCustomerId(custRim);
                    signatory.setEmail("");
                    signatory.setFullName(customerFullName.toLowerCase());
                    signatory.setRole(msisdn);
                    signatory.setTransferAccess(reference);
                    signatory.setUsername(customerUsername.toLowerCase());
                    signatory.setViewAccess(reference);
                    signatory.setPhoneNumber(msisdn);
                    savedAcct = ibankRepo.saveIbankIbankSignatoryMobile(signatory, reference, msisdn, accountNo);
                    if (savedAcct != -1) {
                        String xmlForIBankRegistration = ibankRepo.generateIBankRegistrationXML(reference);
                        String ibankResponse = HttpClientService.sendXMLRequest(xmlForIBankRegistration, systemVariable.IBANK_REGISTRATION_URL);
                        String responseCode = XMLParserService.getDomTagText("responseCode", ibankResponse);
                        if (responseCode.equalsIgnoreCase("0")) {
                            ibankRepo.finalizeIBRegistration(reference);
                            LOGGER.info("CUSTOMER IS SUCCESSFULY CREATED ON IBANK PLATFORM: PROFIL_REFERENCE:{},", reference);
                            resp = xmlSelfServiceResponseToGW("0", "Success", reference,"-1");
                        } else {
                            resp = xmlSelfServiceResponseToGW(responseCode, "Failed registration on IBANK", reference,"-1");
                        }
                    }
                }
            }
        }
        return resp;

    }

    public String popoteAppRegistration(String payload, String custRim, String accountNo, String msisdn, String serviceName) {
        //CREATE USER LOCALLY
        List<Map<String, Object>> details = ibankRepo.getCustomerAccountDetails(accountNo);

        String resp = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        if ((!details.isEmpty()) && details != null) {

            AddIbankProfile ibankProfile = new AddIbankProfile();

            ibankProfile.setAccountNo(accountNo);
            ibankProfile.setAddress1(details.get(0).get("ADDR_LINE_1") + "");
            ibankProfile.setAddress2(details.get(0).get("ADDR_LINE_2") + "");
            ibankProfile.setAddress3(details.get(0).get("ADDR_LINE_3") + "");
            ibankProfile.setAddress4(details.get(0).get("ADDR_LINE_4") + "");
            ibankProfile.setBranchId(details.get(0).get("BU_NO") + "");
            ibankProfile.setCity(details.get(0).get("CITY") + "");
            ibankProfile.setComments("Registration via digital platform");
            ibankProfile.setCustCategory(details.get(0).get("CUST_CAT") + "");
            ibankProfile.setCustShortName(details.get(0).get("CUST_SHORT_NM") + "");
            ibankProfile.setCustomerId(details.get(0).get("CUST_ID") + "");
            ibankProfile.setCustomerName(details.get(0).get("CUST_NM") + "");
            ibankProfile.setCustomerRim(details.get(0).get("CUST_NO") + "");
            ibankProfile.setEmail("");
            ibankProfile.setMandate("0000");
            ibankProfile.setPfNo("0000");
            ibankProfile.setReceivedDate(DateUtil.now());
            ibankProfile.setRecruitingBrach(details.get(0).get("BU_NO") + "");
            ibankProfile.setState(details.get(0).get("ADDR_LINE_4") + "");

            String reference = "MOB" + details.get(0).get("BU_NO") + DateUtil.now("yyyMMddmmss");
            String customerFullName = details.get(0).get("CUST_NM").toString();
            LOGGER.info("Is first and last customer name not null?.... {} and ...{}", details.get(0).get("FIRST_NM"), details.get(0).get("LAST_NM"));

            String customerUsername = details.get(0).get("FIRST_NM") + "".trim() + "." + details.get(0).get("LAST_NM") + "".trim();
            String customerID = details.get(0).get("CUST_ID") + "";

            //create ib profile
            int profile = ibankRepo.saveIBClientServiceRequest(customerUsername, ibankProfile, reference, msisdn, serviceName);
            resp = xmlSelfServiceResponseToGW("99", "An error occurred during processing customer request", reference,"-1");
            if (profile == 1) {
                LOGGER.info("MOBILE APP CREATED SUCCESSFULLY WITH: PROFILE_REFERENCE={}, ACCOUNT:{},RIM:{},", reference, accountNo, custRim);
                String xmlForAppRegistration = payload;
                String ibankResponse = HttpClientService.sendXMLRequest(xmlForAppRegistration, systemVariable.POPOTE_APP_REGISTRATION_URL);
                String responseCode = XMLParserService.getDomTagText("responseCode", ibankResponse);
                if (responseCode.equalsIgnoreCase("0")) {
                    ibankRepo.finalizeIBRegistration(reference);
                    LOGGER.info("MOBILE APP PROFILE REGISTERED IS SUCCESSFULLY RESET : PROFIL_REFERENCE:{},", reference);
                    resp = xmlSelfServiceResponseToGW("0", "Success", reference,"-1");
                } else {
                    resp = xmlSelfServiceResponseToGW(responseCode, "MOBILE APP PROFILE REGISTRATION failed to process", reference,"-1");
                }
            } else if (profile == -1) {
                resp = xmlSelfServiceResponseToGW("-1", "Exception occurred in profile registration", reference,"-1");
            }

        }
        return resp;
    }

    public String popoteAppDeactivation(String payload, String custRim, String accountNo, String msisdn, String serviceName) {
        //CREATE USER LOCALLY
        LOGGER.info("GOING TO DEACTIVATE MOBILE APP: ");
        List<Map<String, Object>> details = ibankRepo.getCustomerAccountDetails(accountNo);
        String resp = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        if (details != null) {
            AddIbankProfile ibankProfile = new AddIbankProfile();
            ibankProfile.setAccountNo(accountNo);
            ibankProfile.setAddress1(details.get(0).get("ADDR_LINE_1") + "");
            ibankProfile.setAddress2(details.get(0).get("ADDR_LINE_2") + "");
            ibankProfile.setAddress3(details.get(0).get("ADDR_LINE_3") + "");
            ibankProfile.setAddress4(details.get(0).get("ADDR_LINE_4") + "");
            ibankProfile.setBranchId(details.get(0).get("BU_NO") + "");
            ibankProfile.setCity(details.get(0).get("CITY") + "");
            ibankProfile.setComments("Registration via digital platform");
            ibankProfile.setCustCategory(details.get(0).get("CUST_CAT") + "");
            ibankProfile.setCustShortName(details.get(0).get("CUST_SHORT_NM") + "");
            ibankProfile.setCustomerId(details.get(0).get("CUST_ID") + "");
            ibankProfile.setCustomerName(details.get(0).get("CUST_NM") + "");
            ibankProfile.setCustomerRim(details.get(0).get("CUST_NO") + "");
            ibankProfile.setEmail("");
            ibankProfile.setMandate("0000");
            ibankProfile.setPfNo("0000");
            ibankProfile.setReceivedDate(DateUtil.now());
            ibankProfile.setRecruitingBrach(details.get(0).get("BU_NO") + "");
            ibankProfile.setState(details.get(0).get("ADDR_LINE_4") + "");
            String reference = "MOB" + details.get(0).get("BU_NO") + DateUtil.now("yyyMMddmmss");
            String customerFullName = details.get(0).get("CUST_NM").toString();
            String customerUsername = details.get(0).get("FIRST_NM").toString().trim() + "." + details.get(0).get("LAST_NM").toString().trim();
            String customerID = details.get(0).get("CUST_ID") + "";
            //create ib profile
            int profile = ibankRepo.saveIBClientServiceRequest(customerUsername, ibankProfile, reference, msisdn, serviceName);
            resp = xmlSelfServiceResponseToGW("99", "An error occured during processing customer request", reference,"-1");
            if (profile == 1) {
                LOGGER.info("MOBILE APP DEACTIVATED SUCCESSFULLY WITH: PROFILE_REFERENCE={}, ACCOUNT:{},RIM:{},", reference, accountNo, custRim);
                String xmlForAppRegistration = payload;
                String ibankResponse = HttpClientService.sendXMLRequest(xmlForAppRegistration, systemVariable.POPOTE_APP_REGISTRATION_URL);
                String responseCode = XMLParserService.getDomTagText("responseCode", ibankResponse);
                if (responseCode.equalsIgnoreCase("0")) {
                    ibankRepo.finalizeIBRegistration(reference);
                    resp = xmlSelfServiceResponseToGW("0", "Success", reference,"-1");
                } else {
                    resp = xmlSelfServiceResponseToGW(responseCode, "MOBILE APP PROFILE DEACTIVATION failed to process", reference,"-1");
                }
            }

        }
        return resp;
    }

    public String visaCardRequest(String payload, String custRim, String accountNo, String msisdn, String branch, String serviceName) {
        //query account details
        String resp = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        //LOGGER.info("ACCOUNT DETAILS:{}", ibankRepo.getAccountDetails(accountNo));
        List<Map<String, Object>> cards = ebankingRepo.getCardExists(accountNo);
        if (cards.isEmpty()) {
            int visaReg = ebankingRepo.visaCardRegistration(accountNo, msisdn, branch);
            if (visaReg == 1) {
                resp = xmlSelfServiceResponseToGW("0", "Success", DateUtil.now("yyyyMMddhhmmss"),"-1");

            }
        } else {
            //TODO Change message, check if card has expired
            resp = xmlSelfServiceResponseToGW("26", "Record already exists", DateUtil.now("yyyyMMddhhmmss"),"-1");

        }

        return resp;
    }

    //visa card pin reset
    public String visaCardPinReset(String payload, String generatedChecksum, String custRim, String accountNo, String msisdn, String serviceName,String visaCardReference) {
        List<Map<String, Object>> result;
        String resp = xmlSelfServiceResponseToGW("99", "General Failure", DateUtil.now("yyyyMMddhhmmss"),"-1");
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        visaCardTracingObject vso = new visaCardTracingObject();
        vso.setAccountNo(accountNo);
        vso.setActionStatus("ACTIVE");
        vso.setPhone(msisdn);
        vso.setCustomerName(null);
        vso.setServiceType("VISA_PIN_RESET");
        vso.setCustomerRimNo(custRim);
        vso.setChecksum(generatedChecksum);
        List<Map<String, Object>> checkCard = ebankingRepo.checkVisaCardFailure(accountNo);
        LOGGER.info("Is this exception already occurred to this account number ....{}? ,.... {} ", accountNo, checkCard.isEmpty());
        try {
            String sql = null;
            if(visaCardReference!=null){
                sql = "select * from card where status='C' and reference=? and customer_rim_no =? order by id desc limit 1";
                result = jdbcTemplate.queryForList(sql, visaCardReference, custRim);
            }else{
                sql = "select * from card where status='C' and account_no=? and customer_rim_no =? order by id desc limit 1";
                result = jdbcTemplate.queryForList(sql, accountNo, custRim);
            }


            LOGGER.info("CARD DETAILS FOR PIN RESET:{}", result);
            String PAN;

            if (!result.isEmpty()) {
                PAN = result.get(0).get("PAN").toString().trim();
                String payloadForPINReset = PAN + "==" + accountNo;
                String PIN = ebankingRepo.getInitialPINFromPostilionSwitch(payloadForPINReset);
                if (!PIN.equalsIgnoreCase("-1")) {
                    //create a sms to customer
                    corebankingService.sendSMS((systemVariable.VISA_CARD_REGISTRATION_INITIAL_PIN).replace("{PIN}", PIN), msisdn, DateUtil.now("yyyyMMddHHmmssSSS"));
                    LOGGER.info("OTP FOR VISA CARD: {} CUSTOMER ACCOUNT:{} PHONE NUMBER:{}", PIN, accountNo, msisdn);
                    resp = xmlSelfServiceResponseToGW("0", "Success", DateUtil.now("yyyyMMddhhmmss"),PIN);
                } else {
                    //failed to decrypt
                    vso.setActionToTake("VALIDATE CARD PAN NUMBER, TO MATCH PHYSICAL CARD PAN");
                    vso.setReference("PD" + formattedDate);
                    vso.setErrorEncountered("GENERATING PASSWORD DECRYPTION FAILED");
                    vso.setSqlCheck(null);
                    if (checkCard.isEmpty()) {
                        LOGGER.info("Going to track new case .. under PIN description . for account... {}", accountNo);
                        trackVisaCardFailures(vso);
                    } else {
                        LOGGER.info("Going to update old case .. under PIN description . for account... {}", accountNo);
                        updateVisaCardFailures(vso);
                    }
                }
            } else {
                //track not found card details
                resp = xmlSelfServiceResponseToGW("99", "You do not hve a VISA card! Contact support for help.", DateUtil.now("yyyyMMddhhmmss"),"-1");
                vso.setActionToTake("MODIFY CUSTOMER DETAILS TO MATCH SQL QUERY");
                vso.setReference("NULI" + formattedDate);
                vso.setErrorEncountered("REQUEST DETAILS NOT MATCHING WITH DETAILS IN CARD TABLE");
                vso.setSqlCheck("select * from card where status='C' and account_no='" + accountNo + "' and customer_rim_no ='" + custRim + "' and phone='" + msisdn + "'");
                if (checkCard.isEmpty()) {
                    trackVisaCardFailures(vso);
                } else {
                    updateVisaCardFailures(vso);
                }
            }
        } catch (Exception e) {
            //track not found card details

//            vso.setActionToTake("MODIFY CUSTOMER DETAILS TO MATCH SQL QUERY");
//            vso.setReference("NULI"+formattedDate);
//            vso.setErrorEncountered("REQUEST DETAILS NOT MATCHING WITH DETAILS IN CARD TABLE");
//            vso.setSqlCheck("select * from card where status='C' and account_no='"+accountNo+"' and customer_rim_no ='"+custRim+"' and phone='"+msisdn+"'");
//            if(checkCard.isEmpty()){
//                trackVisaCardFailures(vso);
//            }else{
//                updateVisaCardFailures(vso);
//            }
            LOGGER.info("ERROR ON GETTING CARD DETAILS FOR PIN RESET:{}:ACCOUNT:{} PHONE NO:{} RIM:{}", e.getMessage(), accountNo, msisdn, custRim);
        }
        return resp;
    }

    public String xmlSelfServiceResponseToGW(String responseCode, String message, String reference,String pin) {
        return "<customerSelfServiceResponse>\n"
                + "	<responseCode>" + responseCode + "</responseCode>\n"
                + "	<message>" + message + "</message>\n"
                + "	<reference>" + reference + "</reference>\n"
                + "	<pin>" + pin + "</pin>\n"
                + "</customerSelfServiceResponse>";

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

    public String getTransactionCalender(String dayOfWeek, String transferType) {
        if (transferType.equalsIgnoreCase("004")) {
            transferType = "001";
        }
        String result = null;
        try {
            result = this.jdbcTemplate.queryForObject("SELECT status from transfer_calendar where day_of_week =? and transfer_type =?", new Object[]{dayOfWeek.toLowerCase(), transferType}, (rs, rowNum) -> rs.getString(1));
//        this.jdbcTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            LOGGER.error("Error getting Transaction day of week callender", e);

        }
        return result;
    }

//    //READ BOT INCOMING TISS MESSAGES
//    public String processBoTRTGSIncoming(String payloadReq) throws IOException, DocumentException, ParseException {
//        String payload = payloadReq.split("\\|")[0];
//        SwiftParser parser = new SwiftParser(payload);
//        SwiftMessage mt = parser.message();
//        String mtJson = mt.toJson();
//        //LOGGER.info("BOT REQUEST FROM BOT:{}", mtJson);
//        SwiftMessageObject swMessageObject = objectMapper.readValue(mtJson, SwiftMessageObject.class);
//        saveSwiftMessageInTransferAdvices(payload);//save the message in the database for reportings
//        //LOG DAILY TOKEN FOR TRANSACTIONS
//        if (swMessageObject.getData().getBlock2() != null && swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("999")) {
//            //check if its STARTS OF THE DAY SOD then save the token to configuration table
//            saveBoTTokenForDay(swMessageObject);
//        } else {
//            //process transaction based on message type
//        }
//        String responseM = "<RESULT>0</RESULT><MESSAGE>SUCCESS</MESSAGE>";
//        LOGGER.info("REQUEST FROM BOT:{} RESPONSE TO BOT:{}", payloadReq, responseM);
//        return responseM;
//
//    }

    /*
    SAVE THE MESSAGE TO DATABASE BEFORE PROCESSING TO CUSTOMER ACCOUNT
     */
//    public int saveSwiftMessageInTransferAdvices(String payload) {
//        LOGGER.info("REQUEST=====>:{}", payload);
//        int result = 0;
//        try {
//            Locale locale = Locale.getDefault();
//            TransferAdviceReq adviceReq = new TransferAdviceReq();
//            SwiftMessage sm = SwiftMessage.parse(payload);
//            if (sm.isServiceMessage()) {
//                sm = SwiftMessage.parse(sm.getUnparsedTexts().getAsFINString());
//            }
//            MessageIOType ioType = sm.getDirection();
//            if (ioType.isIncoming()) {
//                System.out.println("here we are =====");
//                adviceReq.setSenderBank(sm.getSender());
//                adviceReq.setReceiverBank(sm.getReceiver());
//                adviceReq.setMessageType(sm.getBlock2().getMessageType());
//                adviceReq.setMessageInPdf(generatePDFMessageFromMTMessage(payload, sm.getBlock2().getMessageType()));
//                adviceReq.setDirection("INCOMING");
//                //BLOCK 3
//                for (Tag tag : sm.getBlock3().getTags()) {
//                    if (tag.getName().equalsIgnoreCase("103")) {
//                        adviceReq.setServiceCode(tag.getValue());//get service code
//                    }
//                }
//                //if message type is not mt103,202,204,205
//                if (!sm.isType(103) || !sm.isType(202) || !sm.isType(204) || !sm.isType(205)) {
//                    adviceReq.setTransDate(DateUtil.now());
//                }
//                //get message input fields
//                for (Tag tag : sm.getBlock4().getTags()) {
//                    Field field = tag.asField();
//                    //get senders reference
//                    if (tag.getName().equalsIgnoreCase("20")) {
//                        adviceReq.setSenderReference(tag.getValue());//sender reference number
//                    }
//                    if (tag.getName().equalsIgnoreCase("21")) {
//                        adviceReq.setRelatedReference(tag.getValue());//related reference
//                    }
//                    if (tag.getName().equalsIgnoreCase("32A")) {
////                    DateUtil.formatDate(field.getValueDisplay(1, locale), "dd-MM-yyyy", "yyyy-MM-dd")
//                        System.out.println("DATE PARSED: " + field.getValueDisplay(1, Locale.UK));
//                        adviceReq.setTransDate(DateUtil.formatDate(field.getValueDisplay(1, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd") + " " + DateUtil.now("HH:mm:ss"));//TRANS DATE
//                        adviceReq.setAmount(new BigDecimal(field.getValueDisplay(3, Locale.UK).replace(",", "")));//AMOUNT
//                        adviceReq.setCurrency(field.getValueDisplay(2, Locale.UK));
//                    }
//                }
//            }
//            adviceReq.setCbsStatus("P");
//            adviceReq.setCbsMessage("Pending posting Core banking, File just Received");
//            adviceReq.setStatus("P");
//            result = this.jdbcTemplate.update("INSERT INTO transfer_advices(messageType, senderBank, receiverBank, direction, messageInPdf, senderReference, relatedReference, transDate, currency, amount, serviceCode, cbsStatus, cbsMessage, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", adviceReq.getMessageType(), adviceReq.getSenderBank(), adviceReq.getReceiverBank(), adviceReq.getDirection(), adviceReq.getMessageInPdf(), adviceReq.getSenderReference(), adviceReq.getRelatedReference(), adviceReq.getTransDate(), adviceReq.getCurrency(), adviceReq.getAmount(), adviceReq.getServiceCode(), adviceReq.getCbsStatus(), adviceReq.getCbsMessage(), adviceReq.getStatus());
//            LOGGER.info("GOING TO LOG SWIFT MESSAGE:{}", adviceReq.toString());
//        } catch (DocumentException | IOException | ParseException | DataAccessException e) {
//            LOGGER.info("AN ERROR OCCURED DURING PROCESSING:");
//            LOGGER.info(null, e);
//        }
//        return result;
//    }
//    public int saveBoTTokenForDay(SwiftMessageObject swMessageObject) {
//        String token = "-1";
//        int result = -1;
//        for (int i = 0; i < swMessageObject.getData().getBlock4().getTags().size(); i++) {
//            if (swMessageObject.getData().getBlock4().getTags().get(i).getName().equalsIgnoreCase("21")) {
//                String f21Value = swMessageObject.getData().getBlock4().getTags().get(i).getValue();
//                if (f21Value.contains("TZSSOD")) {
//                    token = swMessageObject.getData().getBlock4().getTags().get(2).getValue().split("\r\n")[2];
//                    //SAVE TOKEN TO DATABASE
//                    try {
//                        result = this.jdbcTemplate.update("UPDATE system_configuration set DEV_VALUE=?,DR_VALUE=?,PROD_VALUE=?,MODIFIED_DT=? WHERE NAME='BOT.tiss.daily.token'", token, token, token, DateUtil.now());
////        this.jdbcTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
//                    } catch (DataAccessException e) {
//                        LOGGER.error("Error getting Transaction day of week callender", e);
//                    }
//                }
//            }
//        }
//
//        return result;
//    }
//    /*
//    GENERATE PDF (HUMAN READABLE FORMAT) MT102,103,202,204,205,199,950,299, AND MORE FROM BOT VIA TISS VPN
//     */
//    public byte[] generatePDFMessageFromMTMessage(String payload, String messageType) throws IOException, DocumentException {
//        Locale locale = Locale.getDefault();
//        SwiftMessage sm = SwiftMessage.parse(payload);
//        String messageInPdf = "";
//        messageInPdf += "***************************[TANZANIA COMERCIAL BANK- PAYMENT ADVICE]*************************";
//        messageInPdf += "\n MESSAGE TYPE:" + sm.getBlock2().getMessageType();
//        messageInPdf += "\n SENDER BANK: " + sm.getSender();
//        messageInPdf += "\n RECEIVER BANK: " + sm.getReceiver();
//        messageInPdf += "\n CORRESPONDENT BIC:" + sm.getCorrespondentBIC().getBic11() + "  (" + sm.getCorrespondentBIC().getInstitution() + ")\n";
//
//        messageInPdf += "*************************************Start of Message****************************************";
//        //get block 2
//        messageInPdf += "\nBasic Header: LT Ident:" + sm.getSender() + " Sess. no: " + sm.getBlock1().getSessionNumber() + " Seq no:" + sm.getBlock1().getSequenceNumber();
//        messageInPdf += "\nAppli. Header: Receiver:" + sm.getReceiver() + " Priority:" + sm.getBlock2().getMessagePriority();
//        //get block 3
//        messageInPdf += "\nUser Header: ";
//        for (Tag tag : sm.getBlock3().getTags()) {
//            Field field = tag.asField();
//            messageInPdf += tag.getName() + ":  ";
//            for (int component = 1; component <= field.componentsSize(); component++) {
//                if (field.getComponent(component) != null) {
//                    messageInPdf += field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " ";
//                }
//            }
//            messageInPdf += "\n              ";
//
//        }
//        messageInPdf += "\n";
//
//        //get block 4 message
//        if (!messageType.equalsIgnoreCase("950")) {
//            for (Tag tag : sm.getBlock4().getTags()) {
//                Field field = tag.asField();
//
//                messageInPdf += tag.getName() + " - " + Field.getLabel(field.getName(), messageType, null, Locale.UK).toUpperCase() + "\n";
//                for (int component = 1; component <= field.componentsSize(); component++) {
//                    if (field.getComponent(component) != null) {
//                        messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
//                    }
//                }
//            }
//        } else {
//            //format mt950 to reduce number of papers
//            boolean beforeField61 = false;
//            String filed61Contents = "";
//            int i = 1;
//            for (Tag tag : sm.getBlock4().getTags()) {
//                Field field = tag.asField();
//                //check if tag is not 61
//
//                if (tag.getName().equalsIgnoreCase("61")) {
//
//                    beforeField61 = true;
//                    String senderReceiver = field.getValueDisplay(10, Locale.UK);
//                    if (senderReceiver == null) {
//                        senderReceiver = "_________________";
//                    }
//                    filed61Contents += "\t\t\t\t\t\t" + i + ",\t\t\t" + field.getValueDisplay(1, Locale.UK) + "\t\t\t\t\t" + field.getValueDisplay(3, Locale.UK) + "\t\t\t\t\t\t\t\t" + field.getValueDisplay(7, Locale.UK) + " \t\t\t\t\t\t" + field.getValueDisplay(8, Locale.UK) + "\t\t\t\t\t\t\t\t\t\t" + senderReceiver + "\t\t\t\t" + field.getValueDisplay(5, Locale.UK) + "\t\t\t\t\t\t\t\n";
//                    i++;
//                } else {
//                    //ADD FIELD 61 CONTENTS BEFORE MOVING TO ANOTHER FILED
//                    if (beforeField61 == true && !tag.getName().equalsIgnoreCase("61")) {
//                        messageInPdf += "61: \tS/N\t\t\tDate\t\t\t\t\t\t\t\tDebit/Credit\t\t\tType \t\t\t\t\tReference  \t\t\t\t\t\t\t\t   Sender& Receiver  \t\t\t\t\t    Amount  \t\t\t\t\t\n";
//                        messageInPdf += filed61Contents;
//                    }
//                    messageInPdf += tag.getName() + " - " + Field.getLabel(field.getName(), messageType, null, Locale.UK).toUpperCase() + "\n";
//                    for (int component = 1; component <= field.componentsSize(); component++) {
//                        if (field.getComponent(component) != null) {
//                            messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
//                        }
//                    }
//
//                }
//
//            }
//        }
//
//        messageInPdf += "*************************************End of Message****************************************";
//        Document document = new Document();
//        Font font = FontFactory.getFont(FontFactory.COURIER, 8, Font.NORMAL + Font.UNDEFINED);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
//        document.open();
//        Paragraph p = new Paragraph();
//        p.setAlignment(Element.ALIGN_MIDDLE);
//        p.add(new Chunk(messageInPdf, font));
//        p.add(new Chunk("", font));
//        document.add(p);
//        document.close();
//        writer.close();
//        return outputStream.toByteArray();
//    }
    public String processMT103(String payload) throws ParseException {
        FundFINTransferReq trfMsg = SwiftService.readMT103(payload);
        LOGGER.info("TRANSFER OBJECT:" + trfMsg);
        if (trfMsg != null) {
            String currencyIn = trfMsg.getCurrency();
            String amountIn = trfMsg.getTranAmount();
            String acctNoIn = trfMsg.getReceiverAccount();
            String acctNameIn = trfMsg.getReceiverName();
            String senderBankIn = trfMsg.getSenderBic();
            String senderAccountIn = trfMsg.getSenderAccount();
            String senderNameIn = trfMsg.getSenderName();
            String descriptionIn = trfMsg.getTranDesc();
            String recieverBic = trfMsg.getReceiverBic();
            String recieverAccount = trfMsg.getReceiverAccount() == null ? "" : trfMsg.getReceiverAccount();
            String recieverName = trfMsg.getReceiverName();
            String effectiveDate = trfMsg.getTranDate();
            long tranIdIn = 0L;
            String referenceIn = trfMsg.getBankRef();
            String response = "-1";
            String isLocalOrInternational = trfMsg.getIsLocalOrInternational();
            String chargeScheme = "";

//            String contraAccount = DBENV.MT103_BOT_DEFAULT_ACCOUNT;
//            if (isLocalOrInternational.equalsIgnoreCase("international")) {
//                chargeScheme = "T02";
//                //scb new york
//                if (currencyIn.equalsIgnoreCase("USD") && (senderBankIn.contains("SCBL") || trfMsg.getSenderCorrespondent().contains("SCBL"))) {
//                    contraAccount = DBENV.MT103_BOT_SCB_CORRESPONDING_ACCOUNT;
//                } //bhf bank
//                else if (currencyIn.equalsIgnoreCase("USD") && (senderBankIn.contains("BHFB") || trfMsg.getSenderCorrespondent().contains("BHFB"))) {
//                    contraAccount = DBENV.MT103_BOT_BHF_CORRESPONDING_ACCOUNT;
//                } else if (currencyIn.equalsIgnoreCase("EUR")) {
//                    contraAccount = DBENV.MT103_BOT_BHF_CORRESPONDING_ACCOUNT;
//                }
//
//            }
//            String idNumber = String.valueOf(System.currentTimeMillis()).substring(3, 13) + "" + rand.nextInt(10000);
//            LOGGER.info("FileName: " + fileName + " tranIdIn:" + tranIdIn + " senderBankIn:" + senderBankIn + " " + "referenceIn:" + referenceIn + " acctNoIn:" + acctNoIn + " acctNameIn:" + acctNameIn + " currencyIn:" + currencyIn + " amountIn: " + amountIn + " descriptionIn:" + descriptionIn);
//            LOGGER.info("tranIdIn:" + tranIdIn + " senderBankIn:" + senderBankIn + " " + "referenceIn:" + referenceIn + " acctNoIn:" + acctNoIn + " acctNameIn:" + acctNameIn + " currencyIn:" + currencyIn + " amountIn: " + amountIn + " descriptionIn:" + descriptionIn);
//            int checkSwiftTrxResult = apiModel.checkSwiftTrx(senderBankIn, referenceIn, amountIn);
//            LOGGER.info("CheckSwiftTrxResult: {}", checkSwiftTrxResult);
//            if (checkSwiftTrxResult == 404) {
//                //transaction not found in swift transaction
//                int isGePGaccountResult = apiModel.isGePGaccount(acctNoIn, currencyIn);
//                LOGGER.info("isGePGaccountResult: {}", isGePGaccountResult);
//                if (isGePGaccountResult == 0) {
//                    //gepg exist
//                    LOGGER.info("Now posting to gepg throug swift kipayment. Provider Acct: {}, Desc: {}, Amount: {}", recieverAccount, descriptionIn, amountIn);
//                    chargeScheme = "";
//                    statusFlag = XapiWebService.sendPostToSwiftGePGAPI(currencyIn, amountIn, recieverAccount, recieverName, senderBankIn, senderAccountIn, senderNameIn, descriptionIn, referenceIn, referenceIn, contraAccount, chargeScheme);
//                } else if (isGePGaccountResult == 404) {//not found in gepg
//                    if (apiModel.isCMSAccount(recieverAccount) == 0) {
//                        if (recieverAccount.equals("170227000078")) {
//                            LOGGER.info("It is AIRTEL MOBILE COMMERCE TRUST ACCOUNT {}", recieverAccount);
//                            //170227000078
//                            //AIRTEL MOBILE COMMERCE (T) LTD;
//                            trfMsg.setMsgType("RTGS");
//                            trfMsg.setChargeScheme(chargeScheme);
//                            trfMsg.setContraAccount(contraAccount);
//                            statusFlag = XapiWebService.processAirtelTrustAccount(trfMsg);
//                        } else {
//                            LOGGER.info("It is CMS partner and blocked to STP: {}", recieverAccount);
//                            statusFlag = 899;
//                        }
//                    } else {
//
//                        if (recieverAccount.startsWith("999")) {
//                            //Check receiver account if it is cms account.
//                            TpbPartnerReference cmsReference = XapiWebService.validateCMSReference(recieverAccount);
//                            if (cmsReference != null) {
//                                //assign cms account
//                                trfMsg.setReceiverAccount(cmsReference.getProfileID());
//                                String description = trfMsg.getTranDesc();
//                                //build cms narrations
//                                trfMsg.setTranDesc("REF:" + cmsReference.getReference() + " DPS:" + senderNameIn + " CST:" + cmsReference.getName() + " DTL:" + description);
//                            } else {
//                                String trxNarration = referenceIn + " " + descriptionIn + "  B/O " + senderNameIn;
//                                trfMsg.setTranDesc(trxNarration);
//                            }
//                        }
//                        //post to cbs
//                        LOGGER.info("Now posting to cbs", senderBankIn, referenceIn, amountIn);
//                        //posting
//                        //This was for TIB bank
//                        //statusFlag = XapiWebService.postGLToDepositTransfer(MT103_BOT_TIB_ACCOUNT, referenceIn, Double.valueOf(amountIn), acctNoIn, currencyIn, trxNarration);
//                        trfMsg.setMsgType("RTGS");
//                        trfMsg.setChargeScheme(chargeScheme);
//                        trfMsg.setContraAccount(contraAccount);
//                        //
//                        statusFlag = XapiWebService.postInwardTransfer(trfMsg);
//                    }
        } else {//error in getting gepg account.
//                    statusFlag = 999;
        }

        return null;
    }

    /**
     * Process TIPS Transactions
     */
    public String processTIPSTransactionFromIBorMobToCoreBanking(PaymentReq tipsTxn) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        String finalResponse = "Tips initial response: -1";
        String jsonString = null;
        PaymentResp paymentResponse = new PaymentResp();

        try {
            String ledger = tipsTxn.getSenderAccount();
            LOGGER.info("TIPS SOURCE ACCOUNT: {}", ledger);
            int checkIfLedger = StringUtils.countMatches(ledger, "-");
            if (checkIfLedger >= 4) {
                /*
                 *SOURCE ACCOUNT IS GL-ACCOUNT
                 */
                result = "{\"result\":\"12\",\"message\":\" Source account can not be GL: " + ledger + "\"}";
            } else {
                //generate the request to RUBIKON
                TipsPaymentRequest tipsPaymentRequest = new TipsPaymentRequest();

                String tips_payment_url = systemVariable.TIPS_PAYMENT_URL;

                String callbackurl = systemVariable.TIPS_CALLBACK_URL;
                //get institution category based on fspCode
                philae.api.UsRole role = systemVariable.apiUserRole(tipsTxn.getCustomerBranch());
                //check if its return payment then pick original reference from txid
                String beneficiaryCategory = tipsRepository.getInstitutionCategory(tipsTxn.getBeneficiaryBIC());

                tipsPaymentRequest.setAmount(tipsTxn.getAmount().toString());
                tipsPaymentRequest.setBenAccount(tipsTxn.getBeneficiaryAccount());
                tipsPaymentRequest.setBeneficiaryName(tipsTxn.getBeneficiaryName());
                tipsPaymentRequest.setReference(tipsTxn.getReference());
                tipsPaymentRequest.setCurrency(tipsTxn.getCurrency());
                tipsPaymentRequest.setDescription(tipsTxn.getDescription());
                tipsPaymentRequest.setSenderAccount(tipsTxn.getSenderAccount());
                tipsPaymentRequest.setBenInstitutionCategory(beneficiaryCategory);
                tipsPaymentRequest.setChannelCode(tipsTxn.getType());
                tipsPaymentRequest.setBenInstitutionCode(tipsTxn.getBeneficiaryBIC());
                tipsPaymentRequest.setMsisdn(tipsTxn.getSenderPhone());
                tipsPaymentRequest.setSenderName(tipsTxn.getSenderName());
                tipsPaymentRequest.setUserRole(role);
                tipsPaymentRequest.setCallbackUrl(callbackurl);

                try {

                    LOGGER.info("TIPS PAYMENT REQ ...{},", tipsPaymentRequest.toString());
                    finalResponse = HttpClientService.sendTipsXMLRequest(tipsPaymentRequest.toString(), tips_payment_url);
                    LOGGER.info("Final response for tips transaction authorization... {}", finalResponse);
                    //check response
                    if (!finalResponse.equalsIgnoreCase("-1") && finalResponse != null) {
                        //jacksonmapper values
                        TipsPaymentResponse tipsPaymentResponse = objectMapper.readValue(finalResponse, TipsPaymentResponse.class);
                        LOGGER.info("Mapped final response for tips transaction ... {}", tipsPaymentResponse);

                        if (tipsPaymentResponse.getResponseCode().equalsIgnoreCase("0")) {
                            //SUCCESS RESPONSE
                            String sql = "update tips_transfers set status='P',cbs_status='C',comments=?,message=?,reference=?,branch_approved_by=?,branch_approved_dt=? where  concat(txid,' ',reference) like ?";
                            //LOGGER.info(sql.replace("?", "{}"), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getBankReference(), role.getUserName(), DateUtil.now(), tipsPaymentResponse.getReference());
                            jdbcTemplate.update(sql, tipsPaymentResponse.getMessage(), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getBankReference(), role.getUserName(), DateUtil.now(), "%" + tipsPaymentResponse.getReference() + "%");

                            paymentResponse.setMessage(tipsPaymentResponse.getMessage());
                            paymentResponse.setReference(tipsPaymentResponse.getReference());
                            paymentResponse.setResponseCode("0");
                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                            paymentResponse.setReceipt(tipsPaymentResponse.getBankReference());
                            LOGGER.info("Final response for returning to IB....{}", paymentResponse);
                            result = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                            LOGGER.info("TIPS SUCCESS response...: reference: {} , POSTED BY: {}, FINAL RESPONSE ...{}", tipsPaymentResponse.getBankReference(), role.getUserName(), finalResponse);

                        } else {
                            jdbcTemplate.update("update tips_transfers set comments=?,message=?,reference=?,branch_approved_by=?,branch_approved_dt=? where  concat(txid,' ',reference)  like ?", tipsPaymentResponse.getMessage(), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getBankReference(), role.getUserName(), DateUtil.now(), tipsPaymentResponse.getReference());
                            paymentResponse.setMessage(tipsPaymentResponse.getMessage());
                            paymentResponse.setReference(tipsPaymentResponse.getReference());
                            paymentResponse.setResponseCode(tipsPaymentResponse.getResponseCode());
                            paymentResponse.setAvailableBalance(BigDecimal.ZERO);
                            paymentResponse.setLedgerBalance(BigDecimal.ZERO);
                            paymentResponse.setReceipt(tipsPaymentResponse.getBankReference());
                            result = XMLParserService.jaxbGenericObjToXML(paymentResponse, Boolean.FALSE, Boolean.TRUE);
                            LOGGER.info("TIPS ERROR: reference: {} , POSTED BY: {}", tipsPaymentResponse.getBankReference(), role.getUserName());
                        }
                    }

                } catch (Exception e) {
                    LOGGER.info("EXCEPTION ON TIPS TRANSACTION AUTO AUTHORIZATION: ", e);
                    return finalResponse;
                }

            }

            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("TIPS EXCEPTION FAILED: {} ", ex);
            result = finalResponse;
        }
        return result;
    }

    public List<Map<String, Object>> getEchangeRateFrmCBSAPI(String accountNo, String currency) {
        List<Map<String, Object>> result = null;
        String jsonString = null;

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
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=841 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=864 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=852 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=852 AND ver.CRNCY_ID=863 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=841 AND ver.CRNCY_ID=863 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=841 THEN 'BUYING'\n"
                + "\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=864 THEN 'BUYING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=863 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=863 AND ver.CRNCY_ID=864 THEN 'SELLING'\n"
                + "   WHEN (SELECT a.CRNCY_ID FROM ACCOUNT a WHERE a.ACCT_NO ='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')=864 AND ver.CRNCY_ID=863 THEN 'BUYING'\n"
                + "   ELSE 'NO DEFINED'\n"
                + "   END AS fxType\n"
                + "FROM V_EXCHANGE_RATE ver WHERE VER.RATE_TY_ID IN (11,29) AND CRNCY_CD_ISO =? ";
        try {
            LOGGER.info(mainSql.replace("?", currency));
            result = this.jdbcRUBIKONTemplate.queryForList(mainSql, currency);
            System.out.println("EXCHANGE RATE RESULTS: " + result);
//            jsonString = this.jacksonMapper.writeValueAsString(result);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    //get TIPS FSP DESTINATION
    public String getTipsFspDestinationCode(String swiftCode) {
        LOGGER.info("SWIFTCODE:{}", swiftCode);
        String sql = "select tips_bank_code from banks where swift_code=? OR swift_code_test= ?";
        String fspCode = "-1";
        try {
            fspCode = jdbcTemplate.queryForObject(sql, new Object[]{swiftCode, swiftCode}, String.class);
//            LOGGER.info("Selected FSP FOR TIPS POSTING: ...", fspCode);
        } catch (DataAccessException e) {
            LOGGER.info("Data access exception e: ...", e);
        }

        return fspCode;
    }


    public void trackVisaCardFailures(visaCardTracingObject vso) {
        try {
            String sql = "INSERT INTO visa_card_issues_tracker (service_type, account_no, phone, customer_name, customer_rim_no, reference, error_encountered, action_to_take, sql_check,action_status, check_sum) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            this.jdbcTemplate.update(sql, vso.getServiceType(), vso.getAccountNo(), vso.getPhone(), vso.getCustomerName(), vso.getCustomerRimNo(), vso.getReference(), vso.getErrorEncountered(), vso.getActionToTake(), vso.getSqlCheck(), vso.getActionStatus(), vso.getChecksum());
        } catch (Exception e) {
            LOGGER.info("tracking exception trapped itself... {}", e);
        }

    }

    public void updateVisaCardFailures(visaCardTracingObject vso) {
        try {
            String sql = "UPDATE visa_card_issues_tracker SET service_type=?, account_no=?, phone=?, customer_name=?, customer_rim_no=?, reference=?, error_encountered=?, action_to_take=?, sql_check=?,action_status=?, check_sum=? WHERE account_no=?";
            this.jdbcTemplate.update(sql, vso.getServiceType(), vso.getAccountNo(), vso.getPhone(), vso.getCustomerName(), vso.getCustomerRimNo(), vso.getReference(), vso.getErrorEncountered(), vso.getActionToTake(), vso.getSqlCheck(), vso.getActionStatus(), vso.getChecksum(), vso.getAccountNo());
        } catch (Exception e) {
            LOGGER.info("tracking exception trapped itself... {}", e);
        }

    }


    public void insertBatchTransactionsToTransfersTable(BatchPaymentReq req) {

        String sql = "INSERT INTO transfers(sourceAcct, destinationAcct, amount, reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency,code,direction,batch_reference,txid,instrId,senderBIC,callbackurl) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {
                String reference = req.getPaymentRequest().get(i).getReference();
                String swiftMessage = "NA";
                PaymentReq request1 = req.getPaymentRequest().get(i);
                if (req.getPaymentRequest().get(i).getType().equalsIgnoreCase("001")) {
                    if (!reference.startsWith("STP")) {
                        String stpReference = "STP" + reference.substring(0, 3) + reference.substring(6);
                        reference = stpReference;
                    }
                    request1.setReference(reference);
                    //set senderBic
                    request1.setSenderBic(systemVariable.SENDER_BIC);
                    request1.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    if (request1.getCurrency().equalsIgnoreCase("USD")
                            && !request1.getBeneficiaryBIC().contains("TZ")) {
                        request1.setCorrespondentBic(systemVariable.USD_CORRESPONDEND_BANK);
                    } else if (request1.getCurrency().equalsIgnoreCase("EUR")) {
                        request1.setCorrespondentBic(systemVariable.EURO_CORRESPONDEND_BANK);
                    } else {
                        request1.setCorrespondentBic(systemVariable.BOT_SWIFT_CODE);
                    }
                    swiftMessage = SwiftService.createMT103FromOnlineReq(request1);
                }
//                String bankReference = "PEN" + DateUtil.now("yyyyMMddHHmmss") + "_" + req.getPaymentRequest().get(i).getReference();
//                java.util.Date date= new Date();
//                Calendar cal = Calendar.getInstance();
//                cal.setTime(date);
//                int month = cal.get(Calendar.MONTH) + 1;
//                int year = cal.get(Calendar.YEAR);
                String code = "IB";
                if (req.getPaymentRequest().get(i).getCallbackUrl().contains("esb/govExpenditure/processMuseErmsCallabackFromBackOffice")) {
                    code = "MUSE";
                }
                if (req.getPaymentRequest().get(i).getCallbackUrl().contains("wH6H9irC")) {
                    code = "MOBILE";
                }
                if (req.getPaymentRequest().get(i).getCallbackUrl().contains("onwsc/paymentCallback")) {
                    code = "IB";
                }
                ps.setString(1, req.getPaymentRequest().get(i).getSenderAccount());
                ps.setString(2, req.getPaymentRequest().get(i).getBeneficiaryAccount());
                ps.setString(3, req.getPaymentRequest().get(i).getAmount().toString());
                ps.setString(4, reference);
                ps.setString(5, "L");
                ps.setString(6, "SYSTEM");
                ps.setString(7, req.getPaymentRequest().get(i).getType());
                ps.setString(8, req.getPaymentRequest().get(i).getDescription());
                ps.setString(9, req.getPaymentRequest().get(i).getSenderAddress());
                ps.setString(10, req.getPaymentRequest().get(i).getSenderPhone());
                ps.setString(11, req.getPaymentRequest().get(i).getSenderName());
                ps.setString(12, swiftMessage);
                ps.setString(13, req.getPaymentRequest().get(i).getCustomerBranch());
                ps.setString(14, "I");
                ps.setString(15, req.getPaymentRequest().get(i).getBeneficiaryContact());
                ps.setString(16, req.getPaymentRequest().get(i).getBeneficiaryBIC());
                ps.setString(17, req.getPaymentRequest().get(i).getBeneficiaryName());
                ps.setString(18, req.getPaymentRequest().get(i).getCurrency());
                ps.setString(19, code);
                ps.setString(20, "OUTGOING");
                ps.setString(21, req.getPaymentRequest().get(i).getBatchReference());
                ps.setString(22, req.getPaymentRequest().get(i).getRelatedReference());
                ps.setString(23, req.getPaymentRequest().get(i).getRelatedReference());
                ps.setString(24, req.getPaymentRequest().get(i).getSenderBic());
                ps.setString(25, req.getPaymentRequest().get(i).getCallbackUrl());
            }

            //
            @Override
            public int getBatchSize() {
                return req.getPaymentRequest().size();
            }
        });
    }

    public List<Map<String, Object>> getCustomerLoanAccountsByRIM(String payload) {
        String customerRim = XMLParserService.getDomTagText("customerNumber", payload);
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcRUBIKONTemplate.queryForList("SELECT * FROM V_ACCOUNTS va WHERE va.CUST_NO =? AND PROD_CAT_TY = 'LN' AND REC_ST = 'A'", customerRim);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING LOAN ACCOUNT DETAILS: {}", e.getMessage());
        }
        LOGGER.info("ACCOUNT DETAILS: {}", result);
        return result;
    }

    public Map<String, Object> getLoanAccountSummary(String payload) {
        String accountId = XMLParserService.getDomTagText("accountId", payload);
        Map<String, Object> result = null;
        try {
            result = this.jdbcRUBIKONTemplate.queryForObject("SELECT * FROM LOAN_ACCOUNT_SUMMARY las WHERE las.ACCT_ID = ?", (ResultSet rs, int rowNum) -> {
                Map<String, Object> loanSummary = new HashMap<>();
                loanSummary.put("accountId", rs.getString("ACCT_ID"));
                loanSummary.put("accountNo", rs.getString("ACCT_NO"));
                loanSummary.put("ledgerBal", rs.getString("LEDGER_BAL"));
                loanSummary.put("clearedBal", rs.getString("CLEARED_BAL"));
                loanSummary.put("debitInterestAccrued", rs.getString("DR_INT_ACCRUED"));
                loanSummary.put("debitInterestPerDay", rs.getString("DR_INT_PER_DAY"));
                loanSummary.put("lastDisbursementDate", rs.getString("LAST_DISBURSEMENT_DT"));
                loanSummary.put("lastDisbursementAmt", rs.getString("LAST_DISBURSEMENT_AMT"));
                loanSummary.put("debitCount", rs.getString("DR_COUNT"));
                loanSummary.put("creditCount", rs.getString("CR_COUNT"));
                loanSummary.put("totalDisbursement", rs.getString("TOTAL_DISBURSEMENT"));
                loanSummary.put("totalCost", rs.getString("TOTAL_COST"));
                loanSummary.put("totalCharges", rs.getString("TOTAL_CHRGS"));
                loanSummary.put("totalTax", rs.getString("TOTAL_TAX"));
                loanSummary.put("lastAccrualDate", rs.getString("LAST_ACCRUAL_DT"));
                loanSummary.put("nextAccrualDate", rs.getString("NEXT_ACCRUAL_DT"));
                loanSummary.put("nextPaymentDate", rs.getString("NEXT_PAYMENT_DT"));
                loanSummary.put("createDate", rs.getString("CREATE_DT"));
                loanSummary.put("loanFeesDueAccrued", rs.getString("LOAN_FEES_DUE_ACCRUED"));
                loanSummary.put("calculatedEffectiveInterestRate", rs.getString("CALCULATED_EFFECTIVE_INT_RATE"));
                return loanSummary;
            }, accountId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING LOAN ACCOUNT SUMMARY DETAILS: {}", e.getMessage());
        }
        LOGGER.info("LOAN ACCOUNT SUMMARY DETAILS: {}", result);
        return result;
    }

    public String testNewCertificates(String reqPayload, String reference) {
        String result = "{}";
//        Create signature
        String signedRequestXML = signTISSVPNRequest(reqPayload, systemVariable.SENDER_BIC);
        String response = null;
        try {
            response = HttpClientService.sendXMLRequestToBot(signedRequestXML, systemVariable.BOT_TISS_VPN_URL, reference, systemVariable.SENDER_BIC, systemVariable.BOT_SWIFT_CODE, systemVariable.getSysConfiguration("BOT.tiss.daily.token", "dev"), systemVariable.PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH,systemVariable.PRIVATE_TISS_VPN_KEYPASS);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        if (response != null && !response.equalsIgnoreCase("-1")) {
            //get message status and update transfers table with response to IBD approval
            String statusResponse = XMLParserService.getDomTagText("RespStatus", response);
            if (statusResponse.equalsIgnoreCase("ACCEPTED")) {
                // insert into reports for
                swiftRepository.saveSwiftMessageInTransferAdvices(reqPayload, "BOT-VPN", "OUTGOING");
                jdbcTemplate.update("update transfers set status='C',cbs_status='C',comments='Success file generated TISS VPN',hq_approved_by=?,hq_approved_dt=?, swift_status='BOT-TISS-VPN' where  reference=?", "SYSTEM", DateUtil.now(), reference);

                // The message is successful on BOT endpoint
                result = "{\"result\":\"0\",\"message\":\"" + reqPayload + ":Transaction is ACCEPTED By BOT successfully. Await settlement to recipient Bank. \"}";
            } else {
                result = "{\"result\":\"96\",\"message\":\"" + reqPayload + ":Transaction is REJECTED By BOT \"}";
            }
        }
        return result;
    }

    public String signTISSVPNRequest(String rawXML, String bankCode) {
        String rawXmlsigned = null;
        try {

            String signature = sign.CreateSignature(rawXML, systemVariable.PRIVATE_TISS_VPN_KEYPASS, systemVariable.PRIVATE_TISS_VPN_KEY_ALIAS, systemVariable.PRIVATE_TISS_VPN_KEY_FILE_PATH);

            rawXmlsigned = rawXML + "|" + signature;
            LOGGER.info("TISS VPN SIGNED REQUEST: {}\n{}", bankCode, rawXmlsigned);
        } catch (Exception ex) {
            LOGGER.error("Generating digital signature...{}", ex.getMessage());
        }
        return rawXmlsigned;
    }

    public Map<String, Object> getCardAccount(String cardNo) {
        Map<String, Object> res;
        LOGGER.info("Card no ... {}", cardNo);

        try {
            res = jdbcTemplate.queryForMap("SELECT custid, customer_name, customer_rim_no, account_no FROM card WHERE PAN = ? ",
                    cardNo);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = null;
        }
        return res;
    }
}
