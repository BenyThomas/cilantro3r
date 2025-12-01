/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.GeneralJsonResponse;
import com.DTO.IBANK.Banks;
import com.DTO.IBANK.BanksListResp;
import com.DTO.stawi.StawiBondPostRequest;
import com.DTO.Teller.FinanceMultipleGlPosting;
import com.DTO.Teller.FormJsonResponse;
import com.DTO.Teller.RTGSTransferForm;
import com.DTO.Teller.RTGSTransferFormFinance;
import com.DTO.stawi.StawiBondLookupRequest;
import com.DTO.stawi.StawiBondLookupResponse;
import com.DTO.stawi.StawiBondNotificationRequest;
import com.config.SYSENV;
import com.controller.itax.CMSpartners.JsonResponse;
import com.core.event.AuditLogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.helper.DateUtil;
import com.models.Transfers;
import com.queue.QueueProducer;
import com.repository.RtgsRepo;
import com.repository.TransfersRepository;
import com.service.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import philae.api.BnUser;
import philae.api.UsRole;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class Rtgs {

    @Autowired
    SYSENV systemVariables;

    @Autowired
    public RtgsRepo rtgsRepo;

    @Autowired
    JasperService jasperService;

    @Autowired
    User userController;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private final Validator factory = Validation.buildDefaultValidatorFactory().getValidator();

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Rtgs.class);
    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcReconTemplate;
    @Autowired
    private StawiBondNotificationClient stawiBondNotificationClient;
    @Autowired
    private TransfersRepository transfersRepository;

    @RequestMapping(value = "/rtgsDashboard")
    public String rtgsDashboard(Model model, HttpSession session, HttpServletRequest httpRequest) {
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";

//        TransactionUtil.ProcessEFTandRTGSTransactions();

        model.addAttribute("pageTitle", "TISS/TT PAYMENTS");
        model.addAttribute("paymentsPermissions", rtgsRepo.getRTGSModulePermissions("/rtgsDashboard", session.getAttribute("roleId").toString()));
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/rtgsDashboard", "SUCCESS", "Viewed rtgs dashboard"));
        return "modules/rtgs/rtgsDashboard";
    }

    @RequestMapping(value = "/rtgsTransfer", method = RequestMethod.GET)
    public String rtgsTransfer(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest httpRequest) {
        System.out.println("SESSION ROLE: " + session.getAttribute("roleId").toString());
        LOGGER.info("TELLER  VIEWING  VIEW [modules/rtgs/rtgsTransfer]");
        model.addAttribute("banks", rtgsRepo.getBanksList());
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/rtgsTransfer->initiate RTGS Transfer", "SUCCESS", "Viewed rtgs dashboard"));
        return "modules/rtgs/rtgsTransfer";
    }

    @RequestMapping(value = "/rtgsTransferFinance", method = RequestMethod.GET)
    public String rtgsTransferFinance(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest httpRequest) {
        LOGGER.info("VIEWING FINANCE POSTING VIEW [modules/rtgs/financeInitiateRtgs]");
        model.addAttribute("banks", rtgsRepo.getBanksList());
        model.addAttribute("taxCategories", rtgsRepo.getTaxCategoryList());
        //get the Service providers Lists
        model.addAttribute("serviceProviders", rtgsRepo.getServiceProvidersLists());
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/rtgsTransferFinance-> Finance initiate RTGS TRANSFER", "SUCCESS", "Viewed rtgs dashboard"));

        return "modules/rtgs/financeInitiateRtgs";

    }

    @RequestMapping(value = "/initiateRTGSRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse initiateRTGSRemittance(@RequestParam("supportingDoc") MultipartFile[] files, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, HttpServletRequest httpRequest) throws Exception {
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        RTGSTransferForm rtgsTransferForm = new RTGSTransferForm();
        if (customeQuery.get("amount") != null) {
            rtgsTransferForm.setAmount(customeQuery.get("amount").replace(",", ""));
        } else {
            rtgsTransferForm.setAmount(customeQuery.get("amount"));
        }

        rtgsTransferForm.setBatchReference(customeQuery.get("batchReference"));
        rtgsTransferForm.setBeneficiaryAccount(customeQuery.get("beneficiaryAccount"));
        rtgsTransferForm.setBeneficiaryBIC(customeQuery.get("beneficiaryBIC"));
        rtgsTransferForm.setBeneficiaryContact(customeQuery.get("beneficiaryContact"));
        rtgsTransferForm.setBeneficiaryName(customeQuery.get("beneficiaryName").replace("[^a-zA-Z0-9]", ""));
        rtgsTransferForm.setChannel(customeQuery.get("channel"));
        rtgsTransferForm.setChargeDetails(customeQuery.get("chargeDetails"));
        rtgsTransferForm.setComments(customeQuery.get("comments"));
        rtgsTransferForm.setCorrespondentBic(customeQuery.get("correspondentBic"));
        rtgsTransferForm.setCurrency(customeQuery.get("currency"));
        rtgsTransferForm.setCurrencyConversion(customeQuery.get("currencyConversion"));
        rtgsTransferForm.setDescription(customeQuery.get("description").replace("&", " "));
        rtgsTransferForm.setFxType(customeQuery.get("fxType"));
        rtgsTransferForm.setIntermediaryBank(customeQuery.get("intermediaryBank"));
        rtgsTransferForm.setMessage(customeQuery.get("message"));
        rtgsTransferForm.setMessageType(customeQuery.get("messageType"));
        rtgsTransferForm.setReference(customeQuery.get("reference"));
        rtgsTransferForm.setRelatedReference(customeQuery.get("relatedReference"));
        rtgsTransferForm.setRequestingRate(customeQuery.get("requestingRate"));
        rtgsTransferForm.setResponseCode(customeQuery.get("responseCode"));
        rtgsTransferForm.setRubikonRate(customeQuery.get("rubikonRate"));
        rtgsTransferForm.setSenderAccount(customeQuery.get("senderAccount"));
        rtgsTransferForm.setSenderAddress(customeQuery.get("senderAddress"));
        rtgsTransferForm.setSenderBic(customeQuery.get("senderBic"));
        rtgsTransferForm.setSenderName(customeQuery.get("senderName"));
        rtgsTransferForm.setSenderPhone(customeQuery.get("senderPhone"));
        rtgsTransferForm.setSwiftMessage(customeQuery.get("swiftMessage"));
        rtgsTransferForm.setTransactionType(customeQuery.get("transactionType"));
        String violError = "";
        FormJsonResponse response = new FormJsonResponse();

        LOGGER.info("SENDER BIC:{} CORRESPONDENT BIC: {}, RTGS FORM: {}", systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, rtgsTransferForm);


        Set<ConstraintViolation<RTGSTransferForm>> violations = this.factory.validate(rtgsTransferForm);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<RTGSTransferForm> violation : violations) {
                violError = violError + violation.getMessage() + "<br/>";
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
                LOGGER.info("FIELD[{}], TEMPLATE[{}], MESSAGE[{}]", violation.getPropertyPath().toString(), violation.getMessageTemplate(), violation.getMessage());
            }
            String errorMsg = String.format("The input has validation failed. [Row Data is '%s'],<br>[<b>Error message</b>:: '%s']", rtgsTransferForm, violError.replace("#;", ""));
            response.setValidated(false);
            response.setErrorMessages(errors);
            response.setJsonString(violError);
            LOGGER.info("RTGS error message: {}", violError);
        } else {
            if(systemVariables.DISABLE_CROSS_CURRENCY.equals("Y") && rtgsRepo.isCrossExchangeRate(rtgsTransferForm.getCurrency(),rtgsTransferForm.getSenderAccount())){
                response.setValidated(false);
                response.setJsonString("The cross currency is disabled at the moment");
                return response;
            }
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            String transferCuttOff = systemVariables.EFT_TRANSACTION_SESSION_TIME.split(",")[1];
            List<Map<String, Object>> cuttoff = rtgsRepo.getTransferCuttOff("001");
            Date startTime = parser.parse("00:00");
            if (cuttoff != null) {
                transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
            }
            Date endTime = parser.parse(transferCuttOff);
            Date txnDate = parser.parse(DateUtil.now("HH:mm"));
            //CHECK IF SWIFT IS ALLOWED ON THIS DATE
            String dayNames[] = new DateFormatSymbols().getWeekdays();
            Calendar date2 = Calendar.getInstance();
            System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
            String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];
            String checkIfAllowed = rtgsRepo.getTransactionCalender(todayName, "001");//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY
            if (!(session.getAttribute("branchCode").equals("Select Branch..."))) {
                if (checkIfAllowed.equalsIgnoreCase("A")) {
                    if (txnDate.after(startTime) && txnDate.before(endTime)) {
                        response.setValidated(true);
                        String direction = "T";
                        String correspondentBank = systemVariables.BOT_SWIFT_CODE;
                        String msgType = "LOCAL";
                        String branchCode = session.getAttribute("branchCode").toString();//get the branchcode from the session
                        String txnType = "001";
                        String initiatedBy = session.getAttribute("username").toString();
                        //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                        if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("INTERNATIONAL")) {
                            direction = "R";
                            msgType = "INTERNATIONAL";
                            txnType = "004";
                            String address = rtgsTransferForm.getSenderAddress() + " TANZANIA";
                            rtgsTransferForm.setSenderAddress(address);
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                            }
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                            }
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("ZAR")) {
                                correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                            }
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("GBP")) {
                                correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                            }
                        }
                        //TIS TRANSACTION ALONG EAST AFRICA
                        if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("EAPS")) {
                            direction = "T";
                            msgType = "EAPS";
                            txnType = "001";
                        }

                        //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                        if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("EAPS") && (!rtgsTransferForm.getCurrency().equalsIgnoreCase("KES") || !rtgsTransferForm.getCurrency().equalsIgnoreCase("UGS"))) {
//                direction = "T";
//                msgType = "EAPS";
//                txnType = "001";
                            direction = "R";
                            msgType = "INTERNATIONAL";
                            txnType = "004";
                            String address = rtgsTransferForm.getSenderAddress() + " TANZANIA";
                            rtgsTransferForm.setSenderAddress(address);
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("USD")) {
                                correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                            }
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("EUR")) {
                                correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                            }
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("ZAR")) {
                                correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                            }
                            if (rtgsTransferForm.getCurrency().equalsIgnoreCase("GBP")) {
                                correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                            }
                        }
                        String reference = branchCode + direction + formattedDate;
                        if (rtgsTransferForm.getRelatedReference() == null || rtgsTransferForm.getRelatedReference().equals("")) {
                            rtgsTransferForm.setRelatedReference(reference);
                        }
//                  System.out.println("TXNREFERENCE: " + reference);
                        String transactionType = rtgsTransferForm.getTransactionType();
                        String swiftMessage = "";
                        if (transactionType.toUpperCase().equalsIgnoreCase("MIRATHI")) {
                            //CREATE MT202
                            swiftMessage = SwiftService.createTellerMT202Mirathi(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT202 MIRATHI:{} ", swiftMessage);
                        }

                        if (transactionType.toUpperCase().equalsIgnoreCase("MT202ATM")) {
                            //CREATE MT202
                            swiftMessage = SwiftService.createTellerMT202Atm(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT202 MT202ATM:{} ", swiftMessage);
                        }

                        if (transactionType.toUpperCase().equalsIgnoreCase("MT202COLLECTIONWITHACCOUNT")) {
                            //CREATE MT202
                            swiftMessage = SwiftService.createTellerMT202CollectionWithAccount(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT202 MT202 COLLECTION WITH ACCOUNT:{} ", swiftMessage);
                        }

                        if (transactionType.toUpperCase().equalsIgnoreCase("MT202COLLECTIONWITHNOACCOUNT")) {
                            //CREATE MT202
                            swiftMessage = SwiftService.createTellerMT202CollectionWithNoAccount(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT202  MT202 COLLECTION WITH NO ACCOUNT:{} ", swiftMessage);
                        }

                        if (transactionType.toUpperCase().equalsIgnoreCase("MT202CASHWITHDRAWAL")) {
                            //CREATE MT202
                            swiftMessage = SwiftService.createTellerMT202CashWithdrawal(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT202 CASH WITHDRAWAL:{} ", swiftMessage);
                        }

                        if ( transactionType.toUpperCase().equalsIgnoreCase("MT202TT") || transactionType.toUpperCase().equalsIgnoreCase("MT202TISS")) {
                            //CREATE MT202
                            swiftMessage = SwiftService.createTellerMT202(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT202:{} ", swiftMessage);
                        }
                        if (transactionType.toUpperCase().equalsIgnoreCase("TISS") || transactionType.toUpperCase().equalsIgnoreCase("TT")) {
                            //CREATE MT103
                            swiftMessage = SwiftService.createTellerMT103(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                            LOGGER.info("SWIFT MESSAGE MT103:{} ", swiftMessage);
                        }
                        LOGGER.info("SWIFT MESSAGE:{} ", swiftMessage);
                        //System.out.println("RTGSFORM: " + rtgsTransferForm.toString());
                        //save the supporting documents
                        for (MultipartFile file : files) {
                            if (!file.isEmpty()) {
                                rtgsRepo.saveSupportingDocuments(reference, file);
                            }
                        }
                        if (swiftMessage != null) {
                            String description=rtgsTransferForm.getDescription()+" "+customeQuery.get("paymentPurpose");
                            rtgsTransferForm.setDescription(description);
                            int res = rtgsRepo.saveinitiatedRTGSRemittance(rtgsTransferForm, reference, txnType, initiatedBy, swiftMessage, branchCode, files);
                            LOGGER.info("REQUEST INITIATED: {}", rtgsTransferForm.toString());
                            if (res != -1) {
                                response.setJsonString("Transaction is successfully Initiated");
                            } else {
                                response.setJsonString("an error occured during processing. Please Try again !!!!!!");
                            }
                        } else {
                            response.setValidated(false);
                            response.setJsonString("Error in creating swift swiftMessage->" + swiftMessage);
                        }
                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/initiateRTGSRemittance-> Transaction initiated with reference: " + reference, "SUCCESS", "Viewed rtgs dashboard"));

                    } else {
                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/initiateRTGSRemittance", "Failed", "Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time"));

                        response.setValidated(false);
                        response.setJsonString("Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time");

                    }
                } else {
                    //audit trails
                    String username = session.getAttribute("username") + "";
                    String branchNo = session.getAttribute("branchCode") + "";
                    String roleId = session.getAttribute("roleId") + "";
                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/initiateRTGSRemittance", "Failed", "Today SWIFT is not operational, Please post this payment on another day"));
                    response.setValidated(false);
                    response.setJsonString("Today SWIFT is not operational, Please post this payment on another day");
                }
            } else {
                response.setValidated(false);
                response.setJsonString("You dont have branch code in your user profile, please contact IT helpdesk");
            }

        }
        return response;
    }

    /*
    FINANCE INITIATE RTGS REMITTANCE
     */
    @RequestMapping(value = "/financeInitiateRTGSRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse financeInitiateRTGSRemittance(@Valid RTGSTransferFormFinance rtgsTransferFormFinance, @RequestParam("supportingDoc") MultipartFile[] files, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, BindingResult result) throws IOException, ParseException {
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        LOGGER.info("REQUEST: {}", rtgsTransferFormFinance.toString());
        FormJsonResponse response = new FormJsonResponse();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            response.setValidated(false);
            response.setErrorMessages(errors);
            LOGGER.info("ERROR OCCURRED DURING INITIATION: " + response);
        } else {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            String transferCuttOff = systemVariables.EFT_TRANSACTION_SESSION_TIME.split(",")[1];
            List<Map<String, Object>> cuttoff = rtgsRepo.getTransferCuttOff("001");
            Date startTime = parser.parse("00:00");
            if (cuttoff != null) {
                transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
            }
            Date endTime = parser.parse(transferCuttOff);
            Date txnDate = parser.parse(DateUtil.now("HH:mm"));
            //CHECK IF SWIFT IS ALLOWED ON THIS DATE
            String dayNames[] = new DateFormatSymbols().getWeekdays();
            Calendar date2 = Calendar.getInstance();
            System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
            String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];
            boolean hasVAT = Boolean.parseBoolean(customeQuery.get("vat"));
            String checkIfAllowed = rtgsRepo.getTransactionCalender(todayName, "001");//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY
            if (checkIfAllowed.equalsIgnoreCase("A")) {
                if (txnDate.after(startTime) && txnDate.before(endTime)) {
                    //CALCULATE WITH-HOLDING TAX
                    BigDecimal taxableAmount = new BigDecimal(rtgsTransferFormFinance.getTaxableAmount());
                    BigDecimal taxRate = new BigDecimal(rtgsTransferFormFinance.getTaxRate().split("==")[0]);
                    BigDecimal tax = taxRate.multiply(taxableAmount);
                    LOGGER.info("TAX AMOUNT: {}", tax);
                    //CALCULATE AMOUNT TO BE TRANSFERRED TO SERVICE PROVIDER
                    BigDecimal totalAmount = new BigDecimal(rtgsTransferFormFinance.getAmount());
                    BigDecimal amount = totalAmount.subtract(tax);
                    LOGGER.info("TOTAL AMOUNT: {} AMOUNT PAYABLE:{} AFTER TAX : {}", rtgsTransferFormFinance.getAmount(), amount, tax);
                    rtgsTransferFormFinance.setAmount(amount.toString());
                    //DISPLAY THE FINANCE FORM AFTER CALCULATION
                    response.setValidated(true);
                    String direction = "T";
                    String correspondentBank = systemVariables.BOT_SWIFT_CODE;
                    String msgType = "LOCAL";
                    String branchCode = session.getAttribute("branchCode").toString();//get the branchcode from the session
                    String txnType = "001";
                    String initiatedBy = session.getAttribute("username").toString();
                    //SET BENEFICIARY ACCOUNT FROM THE SUBMITTED PAYLOAD
                    rtgsTransferFormFinance.setBeneficiaryAccount(rtgsTransferFormFinance.getBeneficiaryAccount().split("==")[1]);
//            exit(0);
                    //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                    if (rtgsTransferFormFinance.getBeneficiaryBIC().split("==")[1].equals("INTERNATIONAL")) {
                        direction = "R";
                        msgType = "INTERNATIONAL";
                        txnType = "004";
                        String address = rtgsTransferFormFinance.getSenderAddress() + " TANZANIA";
                        rtgsTransferFormFinance.setSenderAddress(address);
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("USD")) {
                            correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                        }
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("EUR")) {
                            correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                        }
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("ZAR")) {
                            correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                        }
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("GBP")) {
                            correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                        }
                    }
                    //TIS TRANSACTION ALONG EAST AFRICA
                    if (rtgsTransferFormFinance.getBeneficiaryBIC().split("==")[1].equals("EAPS")) {
                        direction = "T";
                        msgType = "EAPS";
                        txnType = "001";
                    }
                    //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                    if (rtgsTransferFormFinance.getBeneficiaryBIC().split("==")[1].equals("EAPS") && (!rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("KES") || !rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("UGS"))) {
//                direction = "T";
//                msgType = "EAPS";
//                txnType = "001";
                        direction = "R";
                        msgType = "INTERNATIONAL";
                        txnType = "004";
                        String address = rtgsTransferFormFinance.getSenderAddress() + " TANZANIA";
                        rtgsTransferFormFinance.setSenderAddress(address);
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("USD")) {
                            correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                        }
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("EUR")) {
                            correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                        }
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("GBP")) {
                            correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                        }
                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("ZAR")) {
                            correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                        }
                    }
                    String reference = branchCode + direction + formattedDate;
                    String swiftMessage = SwiftService.createFinanceMT103(rtgsTransferFormFinance, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                    LOGGER.info("SWIFT MESSAGE:{} ", swiftMessage);
                    //System.out.println("RTGS FORM: " + rtgsTransferForm.toString());
                    if (amount.compareTo(new BigDecimal(systemVariables.AMOUNT_THAT_REQUIRES_COMPLIANCE)) > 0) {
                        rtgsRepo.updateComplianceStatus(reference);
                    }
                    //save the supporting documents
                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            rtgsRepo.saveSupportingDocuments(reference, file);
                        }
                    }
                    LOGGER.info("REQUEST INITIATED: {}", rtgsTransferFormFinance);
                    int res = rtgsRepo.saveInitiatedFinanceRTGSRemittance(rtgsTransferFormFinance, reference, txnType, initiatedBy, swiftMessage, branchCode, files, totalAmount, tax, hasVAT);
                    if (res != -1) {
                        response.setJsonString("Transaction is successfully Initiated");
                    } else {
                        response.setJsonString("an error occurred during processing. Please Try again !!!!!!");
                    }
                    //audit trails
                    String username = session.getAttribute("username") + "";
                    String branchNo = session.getAttribute("branchCode") + "";
                    String roleId = session.getAttribute("roleId") + "";
                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financeInitiateRTGSRemittance-> Transaction initiated with reference: " + reference, "SUCCESS", "Viewed rtgs dashboard"));

                } else {
                    //audit trails
                    String username = session.getAttribute("username") + "";
                    String branchNo = session.getAttribute("branchCode") + "";
                    String roleId = session.getAttribute("roleId") + "";
                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financeInitiateRTGSRemittance-> ", "Failed", "Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time"));
                    response.setJsonString("Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time");

                }
            } else {
                String username = session.getAttribute("username") + "";
                String branchNo = session.getAttribute("branchCode") + "";
                String roleId = session.getAttribute("roleId") + "";
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financeInitiateRTGSRemittance-> ", "Failed", "Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time"));

                response.setValidated(false);
                response.setJsonString("Today SWIFT is not operational, Please post this payment on another day");

            }
        }
        return response;
    }

    /*
    RTGS TRANSACTIONS ON WORK-FLOW
     */
    @RequestMapping(value = "/rtgsTxnOnWorkFlow", method = RequestMethod.GET)
    public String rtgsTxnOnWorkFlow(Model model, HttpSession session,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        LOGGER.info("BRANCH APPROVER WORK-FLOW: AUTHORIZE TRANSACTIONS ON WORKFLOW");
        model.addAttribute("pageTitle", "AUTHORIZE TRANSACTIONS ON WORKFLOW");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/rtgsTxnOnWorkFlow-> view Transactions on workflow ", "SUCCESS", "Viewed rtgs dashboard"));

        return "modules/rtgs/rtgsTransactionOnWorkFlow";
    }

    /*
    FINANCE RTGS TRANSACTIONS ON WORK-FLOW
     */
    @RequestMapping(value = "/rtgsTxnsOnWorkFlowFinance", method = RequestMethod.GET)
    public String rtgsTxnOnWorkFlowfinance(Model model, HttpSession session,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        LOGGER.info("FINANCE APPROVER WORK-FLOW: AUTHORIZE TRANSACTIONS ON WORKFLOW");
        model.addAttribute("pageTitle", "AUTHORIZE TRANSACTIONS ON WORKFLOW");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/rtgsTxnsOnWorkFlowFinance-> view Transactions(Finance) on workflow ", "SUCCESS", "View Transactions on finance workflow"));

        return "modules/rtgs/rtgsTxnsOnWorkFlowFinance";

    }

    /*
    GET RTGS TRANSACTIONS AJAX TO DATATABLE
     */
    @RequestMapping(value = "/getRTGSTxnOnWorkFlowAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRTGSTxnOnWorkFlow(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        Calendar cal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        cal.add(Calendar.DATE, -3);
        String date = dateFormat.format(cal.getTime());

        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSTxnOnWorkFlowAjax-> populates transactions to workflow ", "SUCCESS", "get populated transactions to workflow"));

        return rtgsRepo.getRTGSTxnOnWorkFlowAjax(date, roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    *Preview TISS Advice message
     */
    @RequestMapping(value = "/previewSwiftMsg")
    public String previewRTGSMessage(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", "SWIFT MESSAGE FOR TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", rtgsRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("returnReason", rtgsRepo.getReturnCodes());
        model.addAttribute("swiftMessage", rtgsRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("swift_message") + "");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/previewSwiftMsg-> preview swift generated message", "SUCCESS", "preview swift generated message"));

        return "modules/rtgs/modals/previewSwiftMsg";
    }

    /*
    FINANCE PREVIEW SWIFT MESSAGE AND AUTHORIZE PAYMENTS
     */
    @RequestMapping(value = "/financePreviewSwiftMsgAndAuthorize")
    public String financePreviewSwiftMsgAndAuthorize(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session, HttpServletRequest request
    ) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", "TRANSACTION  REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", rtgsRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("txnWithTax", rtgsRepo.getFinanceRTGSWithTax(customeQuery.get("reference")));
        model.addAttribute("swiftMessage", rtgsRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("swift_message"));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financePreviewSwiftMsgAndAuthorize-> finance preview swift generated message", "SUCCESS", "finance preview swift generated message"));

        return "modules/rtgs/modals/financePreviewSwiftMsgAndAuthorize";
    }

    /*
    *Preview TISS Advice message
     */
    @RequestMapping(value = "/previewSwiftMsgAndAuthorize")
    public String ibdPreviewRTGSMessage(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session, HttpServletRequest request
    ) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", "SWIFT MESSAGE FOR TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", rtgsRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        model.addAttribute("returnReason", rtgsRepo.getReturnCodes());
        model.addAttribute("reference", customeQuery.get("reference"));
        boolean classValue = false;
        if (rtgsRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("txn_type").equals("003")) {
            classValue = true;
        }
        model.addAttribute("txn_type", classValue);
        model.addAttribute("swiftMessage", rtgsRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("swift_message"));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financePreviewSwiftMsgAndAuthorize-> finance preview swift generated message", "SUCCESS", "finance preview swift generated message"));

        return "pages/transactions/modals/previewSwiftMsgAndAuthorize";
    }

    /*
    PREVIEW RTGS SUPPORTING DOCUMENT
     */
    @RequestMapping(value = "/previewSupportingDocuments")
    public String previewSupportingDocuments(@RequestParam Map<String, String> customeQuery, HttpSession session, Model model, HttpServletRequest request
    ) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", "SWIFT MESSAGE FOR TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("swiftMessage", rtgsRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financePreviewSwiftMsgAndAuthorize-> finance preview swift generated message", "SUCCESS", "finance preview swift generated message"));

        return "modules/rtgs/modals/previewSupportingDocuments";
    }

    /*
     *BRANCH AUTHORIZE TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/authorizeRTGSonWorkFlow", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String authorizeRTGSonWorkFlow(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            //get cut-off from database
            String transferCutOff = systemVariables.EFT_TRANSACTION_SESSION_TIME.split(",")[1];
            List<Map<String, Object>> cutoff = rtgsRepo.getTransferCuttOff("001");
            Date startTime = parser.parse("00:00");
            if (cutoff != null) {
                transferCutOff = cutoff.get(0).get("cutt_off_time") + "";
            }
            Date endTime = parser.parse(transferCutOff);
            Date txnDate = parser.parse(DateUtil.now("HH:mm"));
            if (txnDate.after(startTime) && txnDate.before(endTime)) {
                String postingRole = (String) httpsession.getAttribute("postingRole");
                String reference = customeQuery.get("reference");
                if (postingRole != null) {
                    //check if the role is allowed to process this transactions
                    BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                    LOGGER.info("The selected role for authorizing RTGSonWorkFlow is, 1...{} and userSessionRole..2.... {}", postingRole, user.getRoles().getRole());

                    philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                    for (UsRole role : user.getRoles().getRole()) {
                        if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                            philae.ach.UsRole achRole = userController.getAchRole(user2, postingRole);
                            result = rtgsRepo.processRTGSRemittanceToCoreBanking(customeQuery.get("reference"), role, achRole);
                            //audit trails
                            String username = session.getAttribute("username") + "";
                            String branchNo = session.getAttribute("branchCode") + "";
                            String roleId = session.getAttribute("roleId") + "";
                            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeRTGSonWorkFlow", "SUCCESS", "branch Authorize payments"));
                            break;
                        } else {
                            //audit trails
                            String username = session.getAttribute("username") + "";
                            String branchNo = session.getAttribute("branchCode") + "";
                            String roleId = session.getAttribute("roleId") + "";
                            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeRTGSonWorkFlow", "Failed", "Cannot authorize payment while the role is not selected for posting"));

                            result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                        }
                    }

                }
            } else {
                result = "{\"result\":\"102\",\"message\":\"Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time. \"}";
                //audit trails
                String username = session.getAttribute("username") + "";
                String branchNo = session.getAttribute("branchCode") + "";
                String roleId = session.getAttribute("roleId") + "";
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeRTGSonWorkFlow", "Failed", "Cannot approve transaction after cut-off"));

            }
        } catch (Exception ex) {
            //audit trails
            String username = session.getAttribute("username") + "";
            String branchNo = session.getAttribute("branchCode") + "";
            String roleId = session.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeRTGSonWorkFlow", "Failed", "Exception occured during authorization: MESSAGE->" + ex.getMessage()));

            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    /*
    FINANCE AUTHORIZE PAYMENT TO AWAITING LEDGER FROM OTHER SOURCES LEDGERS....
     */
    @RequestMapping(value = "/approveFinanceRTGSRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveFinanceRTGSRemittance(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        String references = customeQuery.get("references");
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            String transferCuttOff = systemVariables.EFT_TRANSACTION_SESSION_TIME.split(",")[1];
            List<Map<String, Object>> cuttoff = rtgsRepo.getTransferCuttOff("001");
            Date startTime = parser.parse("00:00");
            if (cuttoff != null) {
                transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
            }
            Date endTime = parser.parse(transferCuttOff);
            Date txnDate = parser.parse(DateUtil.now("HH:mm"));
            if (txnDate.after(startTime) && txnDate.before(endTime)) {
                if (postingRole != null) {
                    //check if the role is allowed to process this transactions
                    BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                    for (UsRole role : user.getRoles().getRole()) {
                        if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                            //audit trails
                            String username = session.getAttribute("username") + "";
                            String branchNo = session.getAttribute("branchCode") + "";
                            String roleId = session.getAttribute("roleId") + "";
                            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/approveFinanceRTGSRemittance", "SUCCESS", "Finance approve transaction on workflow" + customeQuery.get("reference")));

                            result = rtgsRepo.financeApproveRTGSFrmGLToSuspenseGL(customeQuery.get("reference"), role);

                            break;
                        } else {
                            //audit trails
                            String username = session.getAttribute("username") + "";
                            String branchNo = session.getAttribute("branchCode") + "";
                            String roleId = session.getAttribute("roleId") + "";
                            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/approveFinanceRTGSRemittance", "Failed", "You cannot approve payment while you have not selected the role:35"));

                            result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                        }
                    }

                }
            } else {
                //audit trails
                String username = session.getAttribute("username") + "";
                String branchNo = session.getAttribute("branchCode") + "";
                String roleId = session.getAttribute("roleId") + "";
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/approveFinanceRTGSRemittance", "Failed", "Cannot post transaction after cut-off"));

                result = "{\"result\":\"102\",\"message\":\"Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time. \"}";
            }
        } catch (Exception ex) {
            //audit trails
            String username = session.getAttribute("username") + "";
            String branchNo = session.getAttribute("branchCode") + "";
            String roleId = session.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/approveFinanceRTGSRemittance", "Failed", "Cannot process payment: Exception->" + ex.getMessage()));

            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    /*
    RETURN TRANSACTION FOR AMMENDMENT
     */
    @RequestMapping(value = "/returnRTGSForAmmendmend", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String returnRTGSForAmmendmend(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        String returnReason = customeQuery.get("returnReason");
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform this action. \"}";
        if (returnReason == null || returnReason.equalsIgnoreCase("")) {
            result = "{\"result\":\"33\",\"message\":\"Return reason required. \"}";
            //audit trails
            String username = httpsession.getAttribute("username") + "";
            String branchNo = httpsession.getAttribute("branchCode") + "";
            String roleId = httpsession.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/returnRTGSForAmmendmend", "Failed", "You have not selected a return reason"));

        } else {
            //Assistant Branch Manager ROLE ID=90,IBD Operator=153,IT Officer=89,Operations Officer=99,Validation Supervisor=100,Finance Officer=150,Branch Supervisor=144,Branch Manager=60,Chief Cashier=-87,Validation Officer=132,Branch Operation Manager=69
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String references = customeQuery.get("references");
            if (postingRole != null) {
                //allowed roles: Assistant Branch Manager ROLE ID=90,Branch Manager=60,Chief Cashier=-87,Branch Operation Manager=69
                System.out.println("here we are with posting role on session: roleID:" + postingRole);
                //check if the role is allowed to process this transactions
//            if (postingRole.equalsIgnoreCase("90") || postingRole.equalsIgnoreCase("60")||postingRole.equalsIgnoreCase("89")|| postingRole.equalsIgnoreCase("144")|| postingRole.equalsIgnoreCase("-87") || postingRole.equalsIgnoreCase("69")) {
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        result = rtgsRepo.returnTxnForAmmendmend(customeQuery.get("reference"), httpsession.getAttribute("username").toString(), customeQuery.get("comment"), returnReason);
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/returnRTGSForAmmendmend", "SUCCESS", "Returned transaction for ammendment with reference: " + customeQuery.get("reference")));

                        break;
                    } else {
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/returnRTGSForAmmendmend", "Failed", "You do not have a permission to return transaction for ammendment: " + customeQuery.get("reference")));
                        result = "{\"result\":\"38\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }
//            } else {
//                result = "{\"result\":\"101\",\"message\":\"Your Role is not allowed to Perfom this action. Please contact IT Hepldesk for Support. \"}";
//            }
            }
        }
        return result;
    }

    /*
    REJECT RTGS TRANSACTION
     */
    @RequestMapping(value = "/fireRejectRTGSTransaction", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String rejectRTGSTransaction(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        String returnReason = customeQuery.get("rejectReason");
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform this action. \"}";
        if (returnReason == null || returnReason.equalsIgnoreCase("")) {
            result = "{\"result\":\"33\",\"message\":\"Reject reason required. \"}";
            //audit trails
            String username = httpsession.getAttribute("username") + "";
            String branchNo = httpsession.getAttribute("branchCode") + "";
            String roleId = httpsession.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/returnRTGSForAmmendmend", "Failed", "You have not selected a return reason"));

        } else {
            //Assistant Branch Manager ROLE ID=90,IBD Operator=153,IT Officer=89,Operations Officer=99,Validation Supervisor=100,Finance Officer=150,Branch Supervisor=144,Branch Manager=60,Chief Cashier=-87,Validation Officer=132,Branch Operation Manager=69
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String references = customeQuery.get("references");
            if (postingRole != null) {
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        result = rtgsRepo.rejectRtgsTransaction(customeQuery.get("reference"), httpsession.getAttribute("username").toString(), customeQuery.get("rejectReason"));
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/returnRTGSForAmmendmend", "SUCCESS", "Returned transaction for ammendment with reference: " + customeQuery.get("reference")));

                        break;
                    } else {
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/returnRTGSForAmmendmend", "Failed", "You do not have a permission to return transaction for ammendment: " + customeQuery.get("reference")));
                        result = "{\"result\":\"38\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }
//            } else {
//                result = "{\"result\":\"101\",\"message\":\"Your Role is not allowed to Perfom this action. Please contact IT Hepldesk for Support. \"}";
//            }
            }
        }
        return result;
    }

    /*
     *IBD APPROVE BRANCH REMITTANCE
     */
    @RequestMapping(value = "/approveBranchRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveBranchRemittance(HttpSession httpsession,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        String references = customeQuery.get("references");
        if (postingRole != null) {
            //   philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
//                    for (UsRole role : user.getRoles().getRole()) {
//                        if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
//                            philae.ach.UsRole achRole = userController.getAchRole(user2, postingRole);
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
            philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");

            for (UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    philae.ach.UsRole achRole = userController.getAchRole(user2, postingRole);
                    result = rtgsRepo.processRTGSRemittanceFromTAToBOT(customeQuery.get("reference"), role,achRole);
                    //audit trails
                    String username = httpsession.getAttribute("username") + "";
                    String branchNo = httpsession.getAttribute("branchCode") + "";
                    String roleId = httpsession.getAttribute("roleId") + "";
                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/approveBranchRemittance", "SUCCESS", "IBD approve branch remittance transaction of reference: " + customeQuery.get("reference")));
                    break;
                } else {
                    //audit trails
                    String username = httpsession.getAttribute("username") + "";
                    String branchNo = httpsession.getAttribute("branchCode") + "";
                    String roleId = httpsession.getAttribute("roleId") + "";
                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/approveBranchRemittance", "Failed", "You do not have access to  approve branch remittance transaction of reference: " + customeQuery.get("reference")));

                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    Ammend the transaction
     */
    @RequestMapping(value = "/getRTGSTransactionForAmendmend", method = RequestMethod.GET)
    public String ammendRtgsTransfer(Model model, HttpSession session,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        model.addAttribute("transaction", rtgsRepo.getSwiftMessage(customeQuery.get("reference")));
        model.addAttribute("banks", rtgsRepo.getBanksList());
        model.addAttribute("pageTitle", "AMEND THE TRANSACTION RETURNED BY CHIEF CASHIER: TXN REFERENCE=" + customeQuery.get("reference"));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSTransactionForAmendmend", "SUCCESS", "View Transaction for ammendmend with reference: " + customeQuery.get("reference")));

        return "modules/rtgs/modals/amendRtgsTransfer";
    }

    /*
    Submit the transaction for ammendmend
     */
    @RequestMapping(value = "/amendRTGSTransaction", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse amendRTGSTransaction(@Valid RTGSTransferForm rtgsTransferForm,
            @RequestParam("supportingDoc") MultipartFile[] files,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session, BindingResult result) throws IOException {
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        FormJsonResponse response = new FormJsonResponse();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            response.setValidated(true);
            String direction = "T";
            String correspondentBank = systemVariables.BOT_SWIFT_CODE;
            String msgType = "LOCAL";
            String branchCode = session.getAttribute("branchCode").toString();//get the branchcode from the session
            String txnType = "001";
            String initiatedBy = session.getAttribute("username").toString();
            //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
            if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("INTERNATIONAL")) {
                direction = "R";
                msgType = "INTERNATIONAL";
                txnType = "004";
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("USD")) {
                    correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                }
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("EURO")) {
                    correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                }
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("GBP")) {
                    correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                }
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("ZAR")) {
                    correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                }
            }
            //TIS TRANSACTION ALONG EAST AFRICA
            if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("EAPS")) {
                direction = "T";
                msgType = "EAPS";
                txnType = "001";
            }
            //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
            if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("EAPS") && (!rtgsTransferForm.getCurrency().equalsIgnoreCase("KES") || !rtgsTransferForm.getCurrency().equalsIgnoreCase("UGS"))) {
//                direction = "T";
//                msgType = "EAPS";
//                txnType = "001";
                direction = "R";
                msgType = "INTERNATIONAL";
                txnType = "004";
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("USD")) {
                    correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                }
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("EURO")) {
                    correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                }
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("GBP")) {
                    correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                }
                if (rtgsTransferForm.getCurrency().equalsIgnoreCase("ZAR")) {
                    correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                }
            }
            String reference = customeQuery.get("reference");
            String cbsStatus = customeQuery.get("cbs_status");
            String swiftMessage = SwiftService.createTellerMT103(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
            LOGGER.info("SWIFT MESSAGE: " + swiftMessage);
            //Update the documents the supporting documents
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    rtgsRepo.saveSupportingDocuments(reference, file);
                }
            }
            int res = rtgsRepo.ammendRTGSTransaction(rtgsTransferForm, reference, txnType, initiatedBy, swiftMessage, branchCode, files, cbsStatus);

            if (res != -1) {
                //audit trails
                String username = session.getAttribute("username") + "";
                String branchNo = session.getAttribute("branchCode") + "";
                String roleId = session.getAttribute("roleId") + "";
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/amendRTGSTransaction", "SUCCESS", "You have successfully ammended a transaction with reference: " + customeQuery.get("reference")));

                response.setJsonString("Transaction is successfully Amended and Returned to respective WorkFlow");
            } else {
                //audit trails
                String username = session.getAttribute("username") + "";
                String branchNo = session.getAttribute("branchCode") + "";
                String roleId = session.getAttribute("roleId") + "";
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/amendRTGSTransaction", "Failed", "Failed to  ammended a transaction with reference: " + customeQuery.get("reference")));

                response.setJsonString("an error occured");
            }
        }
//        String acctDeatils = transactionRepo.getAccountDetails(customeQuery.get("accountNo"));
//        System.out.println("REMITTANCE DETAILS: " + response.toString());
        return response;
    }

    /*
    QUERY SERVICE PROVIDERS DETAILS
     */
    @RequestMapping(value = "/queryServiceProviderDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String queryServiceProviderDetails(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String acctDeatils = rtgsRepo.getServiceProvidersDetails(customeQuery.get("serviceProviderId"));
        LOGGER.info("SERVICE PROVIDER DETAILS: " + acctDeatils);
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/queryServiceProviderDetails", "SUCCESS", "Query service provider details:-> " + acctDeatils));

        return acctDeatils;
    }

    @RequestMapping(value = "/querySenderDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSenderDetails(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String ledger = customeQuery.get("accountNo");
        int checkIfLedger = StringUtils.countMatches(ledger, "-");
        String acctDeatils = rtgsRepo.getAccountDetails(customeQuery.get("accountNo"));

        if (checkIfLedger >= 4) {
            acctDeatils = rtgsRepo.getLedgerDetails(customeQuery.get("accountNo"));
        }
        System.out.println("ACCOUNT DETAILS: " + acctDeatils);
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/querySenderDetails", "SUCCESS", "Query sender account details:-> " + acctDeatils));

        return acctDeatils;
    }

    @RequestMapping(value = "/queryLedgerDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String queryLedgerDetails(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String acctDeatils = rtgsRepo.getLedgerDetails(customeQuery.get("accountNo"));
        System.out.println("ACCOUNT DETAILS: " + acctDeatils);
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/queryLedgerDetails", "SUCCESS", "Query GL account details:-> " + acctDeatils));

        return acctDeatils;
    }

    @RequestMapping(value = "/previewSupportingDocument", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewSupportingDocument(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) throws IOException {
        byte[] imageContent = rtgsRepo.getSupportingDocument(customeQuery.get("reference"), customeQuery.get("id"));//get image from DAO based on id
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/previewSupportingDocument", "SUCCESS", "preview supporting documents for transaction of reference:-> " + customeQuery.get("reference")));

        return new ResponseEntity<byte[]>(imageContent, headers, HttpStatus.OK);
    }

    /*
    GET TRANSACTIONS RETURNED FOR AMMENDMEND FROM CHIEF CASHIER
     */
    @RequestMapping(value = "/getRTGSForAmmendmend", method = RequestMethod.GET)
    public String getRTGSForAmmendmend(Model model, HttpSession session,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        model.addAttribute("pageTitle", "RETURNED TRANSACTIONS FOR AMMENDMEND FROM CHIEF CASHIER");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSForAmmendmend", "SUCCESS", "preview transaction returned for ammends from chief cashier"));

        return "modules/rtgs/rtgsForAmmendmend";
    }

    /*
    GET TRANSACTIONS RETURNED FOR AMMENDMEND FROM CHIEF CASHIER AJAX DATATABLE
     */
    @RequestMapping(value = "/getRTGSForAmmendmendAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRTGSForAmmendmendAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSForAmmendmendAjax", "SUCCESS", "get all transactions returned for ammendmend from Chief cashier"));

        return rtgsRepo.getRTGSForAmmendmendAjax((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    GET TRANSACTIONS RETURNED FOR AMMENDMEND FROM IBD
     */
    @RequestMapping(value = "/getRTGSForAmmendmendFromIBD", method = RequestMethod.GET)
    public String getRTGSForAmmendmendFromIBD(Model model, HttpSession session,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        model.addAttribute("pageTitle", "RETURNED TRANSACTIONS FOR AMMENDMEND FROM IBD, PLEASE RECTIFY AND RESUBMIT AGAIN");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSForAmmendmendFromIBD", "SUCCESS", "view transactions for ammendmend from IBD"));

        return "modules/rtgs/requireAmmendmendFrmIBD";
    }

    /*
    GET TRANSACTIONS RETURNED FOR AMMENDMEND FROM IBD HQ AJAX DATATABLE
     */
    @RequestMapping(value = "/getRTGSForAmmendmendFromIBDAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRTGSForAmmendmendFromIBDAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSForAmmendmendFromIBDAjax", "SUCCESS", "view transactions for ammendmend from IBD"));

        return rtgsRepo.getRTGSForAmmendmendFromIBDAjax((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    GET PENDING TRANSACTIONS AT IBD /HQ
     */
    @RequestMapping(value = "/getRTGSpendingAtIBD", method = RequestMethod.GET)
    public String getRTGSpendingAtIBD(Model model, HttpSession session,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        model.addAttribute("pageTitle", "PENDING TRANSACTION AT IBD/HQ");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        System.out.println("-=========> branchNo" + branchNo);
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSpendingAtIBD", "SUCCESS", "view pending transactions at IBD"));
        return "modules/rtgs/pendingTxnsAtIBD";
    }

    /*
    GET PENDING TRANSACTIONS AT IBD /HQ DATATABLE AJAX
     */
    @RequestMapping(value = "/getRTGSpendingAtIBDAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRTGSpendingAtIBDAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSpendingAtIBDAjax", "SUCCESS", "view pending transactions at IBD"));

        return rtgsRepo.getRTGSpendingAtIBDAjax((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    *GET EXCHANGE RATE FROM CORE BANKING....
     */
    @RequestMapping(value = "/coreBankingExchangeRate", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String coreBankingExchangeRate(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
            HttpSession session
    ) {
        String result = "";
        String currency = customeQuery.get("sendingCurrency");
        System.out.println("CURRENCY: " + currency);
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSpendingAtIBDAjax", "SUCCESS", "view pending transactions at IBD"));

        return rtgsRepo.getEchangeRateFrmCBS(customeQuery.get("accountNo"), currency);

    }

    /*
    BRANCH AUTHORIZE TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/reverseRejectRtgsPayment", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String reverseRejectRtgsPayment(HttpSession httpsession,
            @RequestParam Map<String, String> customeQuery, HttpServletRequest request
    ) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform this action posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String reference = customeQuery.get("reference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        philae.ach.UsRole achRole = userController.getAchRole(user2, postingRole);
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/reverseRejectRtgsPayment", "SUCCESS", "reject and reverse  transaction with reference:->" + customeQuery.get("reference")));

                        result = rtgsRepo.processReverseRTGSRemittanceToCoreBanking(customeQuery.get("reference"), role, achRole);
                        break;
                    } else {
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/reverseRejectRtgsPayment", "Failed", "You dont have permission to reject and reverse  transaction with reference:->" + customeQuery.get("reference")));
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }
        } catch (Exception ex) {
            //audit trails
            String username = httpsession.getAttribute("username") + "";
            String branchNo = httpsession.getAttribute("branchCode") + "";
            String roleId = httpsession.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/reverseRejectRtgsPayment", "Failed", "Exception occured during revesring the transactions:-> exception Messaged:" + ex.getMessage()));

            LOGGER.info(null, ex);
        }
        return result;
    }

    @Autowired
    QueueProducer queProducer;

    /*
    HQ RE-GENERATE SWIFT FILE
     */
    @RequestMapping(value = "/redumpPayment", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String redumpPayment(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        String result = result = "{\"result\":\"0\",\"message\":\"The file has been saved to kpinter pool.\"}";
        String reference = customeQuery.get("reference");

        List<Map<String, Object>> swiftf = rtgsRepo.getSwiftMessage(reference);
        //originallMsgNmId
        String swiftBody = swiftf.get(0).get("swift_message") + "";
        //rtgsRepo.insertIntoKprinterPool(reference,swiftBody);
        queProducer.sendToQueueRTGSToSwift(swiftBody + "^" + systemVariables.KPRINTER_URL + "^" + reference);
        return result;
    }

    @RequestMapping(value = {"/onlineMandate"}, method = {RequestMethod.GET})
    @ResponseBody
    public String onlineMandateForm(@RequestParam Map<String, String> customeQuery, HttpServletResponse res) throws IOException {
        String txnid = customeQuery.get("txnid");
        return null;//jasperService.procOnlineMandateForm(customeQuery, "pdf", res, "online_mandate_form");
    }

    /*
    GEPG ACCOUNTS
     */
    @RequestMapping(value = "/initiateGePGRemittance", method = RequestMethod.GET)
    public String initiateGePGRemittance(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        model.addAttribute("pageTitle", "GEPG ACCOUNT BALANCE TO BE REMITTED TO BOT");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/initiateGePGRemittance", "SUCCESS", "Initiate GePG remittance"));

        return "modules/rtgs/initiategepgSettlement";
    }
//      /*
//    GET GEPG ACCOUNT BALANCE FOR INITIATIONS
//     */
//    @RequestMapping(value = "/getInwardEFTAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public String getInwardEFTAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
//        String draw = customeQuery.get("draw");
//        String fromDate = customeQuery.get("txndate") + " " + customeQuery.get("fromTime") + ":00";
//        String todate = customeQuery.get("txndate") + " " + customeQuery.get("totime") + ":59";
//        String start = customeQuery.get("start");
//        String rowPerPage = customeQuery.get("length");
//        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
//        String columnIndex = customeQuery.get("order[0][column]");
//        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
//        String columnSortOrder = customeQuery.get("order[0][dir]");
//
//        return eftRepo.getInwardEFTAjax(fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
//    }

    @RequestMapping(value = {"/getBankListWithTransferTypes"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<Banks> getBankList(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session
    ) {
        String searchValue = customeQuery.get("term");
        if (searchValue == null) {
            searchValue = "";
        } else {
            searchValue = searchValue.replace("&", "");
            searchValue = searchValue.replace(";", "");
            searchValue = searchValue.replace("]", "");
            searchValue = searchValue.replace("{", "");
            searchValue = searchValue.replace("[", "");
            searchValue = searchValue.replace("}", "");
            searchValue = searchValue.replace("'", "");
            searchValue = searchValue.replace("@", "");
            searchValue = searchValue.replace("", "");
        }
//         //audit trails
//        String username = session.getAttribute("username") + "";
//        String branchNo = session.getAttribute("branchCode") + "";
//        String roleId = session.getAttribute("roleId") + "";
//        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getBankListWithTransferTypes", "SUCCESS", "View banks lists"));

        BanksListResp FG = rtgsRepo.getBankBinkList(customeQuery.get("bankType"), searchValue);
        return (FG.getBank());
    }


    /*
    INWARD RTGS INCOMING
     */
    @RequestMapping(value = "/inwardrtgsSummary", method = RequestMethod.GET)
    public String inwardEFT(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        model.addAttribute("pageTitle", "INCOMING RTGS TRANSACTIONS SUMMARY PER BANK  AS AT: " + DateUtil.now("yyyy-MM-dd"));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/inwardrtgsSummary", "SUCCESS", "View inward RTGS transactions "));

        return "modules/rtgs/inwardInwardRTGSSummary";
    }

    /*
    GET INWARD RTGS PER BANK AJAX
     */
    @RequestMapping(value = "/getInwardrtgsSummaryAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getInwardrtgsSummaryAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate") + " 00:00:00";
        String todate = customeQuery.get("toDate") + " 23:59:59";
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getInwardrtgsSummaryAjax", "SUCCESS", "View inward RTGS transactions "));

        return rtgsRepo.getInwardrtgsSummaryPerBIC(fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    *Preview RTGS SUCCESS/FAILED TRANSACTIONS PER BANK
     */
    @RequestMapping(value = "/previewRtgsIncomingTransactionsPerBank")
    public String previewRtgsPerBankTxns(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        LOGGER.info("PREVIEWING EFT INCOMING SUCCESS TRANSACTIONS SENDER BIC: {} TXN STATUS: {}", customeQuery.get("senderBic"), customeQuery.get("txnStatus"));
        String title = "RTGS SUCCESSFULLY TRANSACTIONS FOR BANK:   " + customeQuery.get("senderBic");
        if (customeQuery.get("txnStatus").equals("F")) {
            title = "RTGS FAILED TRANSACTIONS FOR BANK:   " + customeQuery.get("senderBic");
        }
        model.addAttribute("pageTitle", title);
        model.addAttribute("senderBic", customeQuery.get("senderBic"));
        model.addAttribute("txnStatus", customeQuery.get("txnStatus"));
        boolean action = false;
        if (!customeQuery.get("txnStatus").equalsIgnoreCase("C")) {
            //this are failed transactions
            action = true;
        }
        model.addAttribute("fromDate", customeQuery.get("fromDate") + " 00:00:00");
        model.addAttribute("toDate", customeQuery.get("toDate") + " 23:59:59");
        model.addAttribute("isActionAllowed", action);

        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getInwardrtgsSummaryAjax", "SUCCESS", "View inward RTGS transactions "));

        return "modules/rtgs/modals/previewRTGSInwardPerBankTxns";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/getRTGSInwardTransactionsPerBank", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRTGSInwardTransactionsPerBank(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return rtgsRepo.getInwardRTGSSuccessPerBankAjax(customeQuery.get("fromDate"), customeQuery.get("toDate"), customeQuery.get("senderBic"), customeQuery.get("txnStatus"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    *INITIATE GEPG HIGH VALUE
     */
    @RequestMapping(value = "/gepgRTGSForm", method = RequestMethod.GET)
    public String gepgRTGSForm(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        model.addAttribute("pageTitle", "INITIATE GePG TRANSACTION TO BOT");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/gepgRTGSForm", "SUCCESS", "View RTGS transfer form"));

        return "modules/rtgs/gepgRTGSForm";
    }

    /*
    *VALIDATE CONTROL NUMBER
     */
    @RequestMapping(value = "/queryControlNumberDetails", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getControlNumberDetails(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String controlNumber = customeQuery.get("controlNo");
        System.out.println("ACCOUNT DETAILS: " + controlNumber);
        String requestId = "STP" + session.getAttribute("branchCode").toString() + DateUtil.now("yyMMddss");
        String req = "<request><COMMAND>BillQryReqProxy2</COMMAND><CONTROL_NO>" + controlNumber + "</CONTROL_NO><ID>" + requestId + "</ID></request>";
        //GePG_KIPAYMENT_VALIDATE_CONTROL_NO_URL;
        String respXml = HttpClientService.sendXMLRequest(req, systemVariables.GePG_KIPAYMENT_VALIDATE_CONTROL_NO_URL);
        String gepgResponseCode = XMLParserService.getDomTagText("BillStsCode", respXml);
        if (gepgResponseCode.equals("7205")) {
            req = "<TipsLookupReq>" +
                    "<senderAcct></senderAcct>" +
                    "<accountNo>" + controlNumber + "</accountNo>" +
                    "<institution>NMIB</institution>" +
                    "<institutionCode>016</institutionCode>" +
                    "<institutionCategory>CONTROLNO</institutionCategory>" +
                    "<reference>" + requestId + "</reference>" +
                    "<amount>0.0</amount>" +
                    "<currency>TZS</currency>" +
                    "</TipsLookupReq>";
            respXml = HttpClientService.sendXMLRequest(req, systemVariables.GePG_V2_KIPAYMENT_LOOKUP_URL);
            respXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + respXml;
            try {
                JSONObject json = XML.toJSONObject(respXml);
                json.put("p2g", true);
                LOGGER.info("responseBody:{}", json);
                return json.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject respJson = XML.toJSONObject(respXml);
        respJson.put("beneficiaryAccount", rtgsRepo.getTRAccountNoUsingSpCode(XMLParserService.getDomTagText("SpCode", respXml)));
        if (systemVariables.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
            respJson.put("beneficiaryBic", "TANZTZTX==LOCAL");
        } else {
            respJson.put("beneficiaryBic", "TANZTZT0==LOCAL");
        }
        respJson.put("beneficiaryName", rtgsRepo.getTRAaccountNameUsingSpCode(XMLParserService.getDomTagText("SpCode", respXml)));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(),
                "/queryControlNumberDetails", "SUCCESS",
                "query gepg bill details: control no->" + customeQuery.get("controlNo")
                + " bill Details:" + respJson.toString().trim()));
        LOGGER.info("responseBody:{}", respJson);
        return respJson.toString();
    }

    /*

     */
    @RequestMapping(value = "/initiateGePGRTGS", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse initiateGePGTissTOBot(@Valid RTGSTransferForm rtgsTransferForm, @RequestParam("supportingDoc") MultipartFile[] files, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, BindingResult result) throws Exception {
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        LOGGER.info("SENDER BIC:{} CORRESPONDENT BIC: {}, RTGS FORM: {}", systemVariables.SENDER_BIC, systemVariables.BOT_SWIFT_CODE, rtgsTransferForm);

        FormJsonResponse response = new FormJsonResponse();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            response.setValidated(false);
            response.setErrorMessages(errors);
            LOGGER.info("ERROR OCCURED DURING INITIATION: " + response.toString());
            //audit trails
            String username = session.getAttribute("username") + "";
            String branchNo = session.getAttribute("branchCode") + "";
            String roleId = session.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/initiateGePGRTGS", "SUCCESS", "Initiate RTGS transfer failed due to errors with error:->" + errors.toString()));

        } else {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            String transferCuttOff = systemVariables.EFT_TRANSACTION_SESSION_TIME.split(",")[1];
            List<Map<String, Object>> cuttoff = rtgsRepo.getTransferCuttOff("001");
            Date startTime = parser.parse("00:00");
            if (cuttoff != null) {
                transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
            }
            Date endTime = parser.parse(transferCuttOff);
            Date txnDate = parser.parse(DateUtil.now("HH:mm"));
            if (txnDate.after(startTime) && txnDate.before(endTime)) {
                response.setValidated(true);
                String direction = "T";
                String correspondentBank = systemVariables.BOT_SWIFT_CODE;
                String msgType = "LOCAL";
                String branchCode = session.getAttribute("branchCode").toString();//get the branchcode from the session
                String txnType = "001";
                String initiatedBy = session.getAttribute("username").toString();
                //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
                if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("INTERNATIONAL")) {
                    direction = "R";
                    msgType = "INTERNATIONAL";
                    txnType = "004";
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("USD")) {
                        correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                    }
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("EUR")) {
                        correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                    }
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("ZAR")) {
                        correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                    }
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("GBP")) {
                        correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                    }
                }
                //TIS TRANSACTION ALONG EAST AFRICA
                if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("EAPS")) {
                    direction = "T";
                    msgType = "EAPS";
                    txnType = "001";
                }
                //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
                if (rtgsTransferForm.getBeneficiaryBIC().split("==")[1].equals("EAPS") && (!rtgsTransferForm.getCurrency().equalsIgnoreCase("KES") || !rtgsTransferForm.getCurrency().equalsIgnoreCase("UGS"))) {
//                direction = "T";
//                msgType = "EAPS";
//                txnType = "001";
                    direction = "R";
                    msgType = "INTERNATIONAL";
                    txnType = "004";
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("USD")) {
                        correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
                    }
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("EUR")) {
                        correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
                    }
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("ZAR")) {
                        correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
                    }
                    if (rtgsTransferForm.getCurrency().equalsIgnoreCase("GBP")) {
                        correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
                    }
                }
                String reference = branchCode + direction + formattedDate;
                rtgsTransferForm.setRelatedReference(reference);
//            System.out.println("TXNREFERENCE: " + reference);
                String transactionType = rtgsTransferForm.getTransactionType();
                String swiftMessage = "";
                if (transactionType.toUpperCase().equalsIgnoreCase("MIRATHI") || transactionType.toUpperCase().equalsIgnoreCase("MT202TT") || transactionType.toUpperCase().equalsIgnoreCase("MT202TISS")) {
                    //CREATE MT202
                    rtgsTransferForm.setMessageType("202");
                    swiftMessage = SwiftService.createTellerMT202(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                    LOGGER.info("SWIFT MESSAGE MT202:{} ", swiftMessage);
                }
                if (transactionType.toUpperCase().equalsIgnoreCase("TISS") || transactionType.toUpperCase().equalsIgnoreCase("TT")) {
                    //CREATE MT103
                    rtgsTransferForm.setMessageType("103");
                    swiftMessage = SwiftService.createTellerMT103(rtgsTransferForm, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
                    LOGGER.info("SWIFT MESSAGE MT103:{} ", swiftMessage);
                }
                LOGGER.info("SWIFT MESSAGE:{} ", swiftMessage);
                //System.out.println("RTGSFORM: " + rtgsTransferForm.toString());
                //save the supporting documents
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        rtgsRepo.saveSupportingDocuments(reference, file);
                    }
                }
                if (swiftMessage != null) {
                    int res = rtgsRepo.saveinitiatedRTGSRemittance(rtgsTransferForm, reference, txnType, initiatedBy, swiftMessage, branchCode, files);
                    LOGGER.info("REQUEST INITIATED: {}", rtgsTransferForm.toString());
                    if (res != -1) {
                        response.setJsonString("Transaction is successfully Initiated");
                    } else {
                        response.setJsonString("an error occured during processing. Please Try again !!!!!!");
                    }
                } else {
                    response.setValidated(false);
                    response.setJsonString("Error in creating swift swiftMessage->" + swiftMessage);
                }
                //audit trails
                String username = session.getAttribute("username") + "";
                String branchNo = session.getAttribute("branchCode") + "";
                String roleId = session.getAttribute("roleId") + "";
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/initiateGePGRTGS", "SUCCESS", "GePG initiated transactions with control no:" + rtgsTransferForm.getDescription() + " reference:" + reference));
            } else {
                response.setValidated(false);
                response.setJsonString("Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time");

            }
        }
        return response;
    }
//RTGS advice reports

    @RequestMapping(value = "/transferAdvices", method = RequestMethod.GET)
    public String transferAdvices(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        model.addAttribute("pageTitle", "TRANSFER ADVICE MESSAGES");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/transferAdvices", "SUCCESS", "View Transaction advices Reports "));
        return "modules/rtgs/reports/transferAdvices";
    }

    /*
    GET INWARD RTGS PER BANK AJAX
     */
    @RequestMapping(value = "/getTransferAdvicesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTransferAdvicesAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String messageType = customeQuery.get("messageType");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
        String direction = customeQuery.get("direction");
        String amount = customeQuery.get("amount");
        String currency = customeQuery.get("currency");
        String senderReference = customeQuery.get("senderReference");
        //check amount/currency/direction/
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        if (messageType == null) {
            messageType = "all";
        }
        if (direction != null) {
            searchValue = direction;
        }
        if (amount != null) {
            searchValue = amount;
        }
        if (currency != null) {
            searchValue = currency;
        }
        if (senderReference != null) {
            searchValue = senderReference;
        }
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return rtgsRepo.getTransferAdvicesReportAjax(messageType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    //download transfer advice
    //download supporting document
    @RequestMapping(value = "/downloadTransferAdvice")
    @ResponseBody
    public String downloadTransferAdviceCopy(HttpServletResponse response, @RequestParam Map<String, String> customeQuery) {
        try {
            String reference = customeQuery.get("senderReference");
            String senderBank = customeQuery.get("senderBank");
            String receiverBank = customeQuery.get("receiverBank");

//            String valueDate = DateUtil.formatDate(customeQuery.get("valueDate"), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
            byte[] databyte = rtgsRepo.getTransferAdviceCopy(reference, senderBank,receiverBank);

            byte[] file = databyte;
            response.setContentType("application/pdf");
            response.setHeader("Content-disposition", "attachment; filename=" + reference + ".pdf");
            response.setHeader("Content-Length", String.valueOf(file.length));
            response.getOutputStream().write(file);
            response.getOutputStream().close();
//            LOGGER.info("supporting document... DOWNLOADED SUCCESSFULLY {}", reference);
            return "OK";
        } catch (Exception ex) {
            LOGGER.info("exception on download transfer advice copy:{}", ex);
        }
        return "OK";
    }

    /*
    *REPLAY RTGS TRANSACTION TO CORE BANKING
     */
    @RequestMapping(value = "/replayRTGSTxnsToCoreBanking", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String replayRTGSTxnsToCoreBanking(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, @RequestParam(value = "txnid", required = false) List<String> txnid) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");

            String postingRole = (String) session.getAttribute("postingRole");
            String reference = customeQuery.get("reference");
            String accounts = customeQuery.get("accounts");
            String branchCode = customeQuery.get("branchCode");
            //overide destination account
            String modDestAcct = customeQuery.get("modDestAcct");

            LOGGER.info("replayRTGSTxnsToCoreBanking:->Req=> {}", customeQuery);

            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    for (philae.ach.UsRole role2 : user2.getRoles().getRole()) {
                        LOGGER.info("Posting role required: {}, user session role: {}", postingRole, role.getUserRoleId().toString());
                        if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                            result = rtgsRepo.replayRTGSIncomingTOCBS(reference, accounts, customeQuery.get("replayOption"), customeQuery.get("reason"), branchCode, role, role2, txnid, modDestAcct);
                            break;
                        } else {
                            result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                        }
                    }
                }

            }

        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    //GET TRANSFER INCOMING EXCEPTIONS ACCOUNTS
    @RequestMapping(value = "/getTransferIncomingDestAcctExceptions", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTransferIncomingDestAcctExceptions() {
        return rtgsRepo.getTransferIncomingDestAcctsExceptions();
    }


    @RequestMapping(value = "/fireInitiateKprinterTxn", method = RequestMethod.GET)
    public String fireInitiateKprinterTxn(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest httpRequest) {
        model.addAttribute("pageTitle", "INITIATE FAILED TRANSACTIONS TO REACH K-PRINTER");
        return "modules/kwamua/initiateKprinterTxn";
    }

    @PostMapping(value = "/fireGetInitiatedKprinterTxnAjax")
    @ResponseBody
    public List<Map<String,Object>> fireGetInitiatedKprinterTxnAjax(@RequestParam Map<String, String> customeQuery) {
        String reference = customeQuery.get("reference");
        return rtgsRepo.fireGetInitiatedKprinterTxnAjax(reference);
    }

    /*
     *KWAMUA Transactions TO K-PRINTER
     */
    @RequestMapping(value = "/fireInitiateKprinterTxnToWF", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireInitiateKprinterTxnToWF(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String reference = customeQuery.get("reference");
            String swift_message = customeQuery.get("swift_message");
            String reason = customeQuery.get("reason");
            String currency = customeQuery.get("currency");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {

                        String username = httpsession.getAttribute("username") + "";

                        if(1 ==rtgsRepo.fireInitiateKprinterTxnToWF(reference,swift_message, reason, currency, username)){
                            result = "{\"result\":\"0\",\"message\":\"Transaction initiated successifully : \"}";
                        } else{
                            result = "{\"result\":\"-1\",\"message\":\"Transaction already initiated, Please check on workflow: \"}";
                        }

                    } else {
                        //audit trails
                        String username = httpsession.getAttribute("username") + "";
                        String branchNo = httpsession.getAttribute("branchCode") + "";
                        String roleId = httpsession.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireInitiateKprinterTxnToWF", "Failed", "Cannot initiate swift transaction while the role is not selected for posting"));

                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            //audit trails
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }


    @GetMapping(value = "/fireGetInitiateKprinterTxnWF")
    public String fireGetInitiateKprinterTxnWF(Model model) {
        model.addAttribute("pageTitle", "SWIFT INITIATED TRANSACTIONS WORK FLOW");
        return "modules/kwamua/initiatedKprinterTxnWF";
    }

    @PostMapping(value="/fireGetInitiateKprinterTxnWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireGetInitiateKprinterTxnWFAjax(@RequestParam Map<String, String> customeQuery, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        String currUser = session.getAttribute("username").toString();
        return rtgsRepo.fireGetInitiateKprinterTxnWFAjax(currUser, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "/firePreviewPendingSwiftTXN")
    public String firePreviewPendingSwiftTXN(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("pageTitle", "AUTHORIZATION OF SWIFT TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        String finalSwiftMsg = customeQuery.get("swift_message");
        String currency = customeQuery.get("currency");
        finalSwiftMsg = finalSwiftMsg.replace(StringUtils.substringBetween(finalSwiftMsg, ":32A:", currency), DateUtil.now("yyMMdd"));
        model.addAttribute("swift_message", finalSwiftMsg);
        model.addAttribute("reference", customeQuery.get("reference"));
        return "modules/kwamua/modal/previewSwiftTxnONWF";
    }

    @PostMapping(value = "/fireAuthorizeSwiftTxnToKprinter", produces =  "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthorizeSwiftTxnToKprinter(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        String result = "{\"result\":\"99\",\"message\":\" Transaction failed to process: \"}";
        String swift_message = customeQuery.get("swift_message");
        String reference = customeQuery.get("reference");
        String username = session.getAttribute("username") + "";

        String url = systemVariables.KPRINTER_URL;
        //insert into swift queue
            if( 1==rtgsRepo.insertTransctionToSwift(reference,swift_message,url)){
                //update transfers and swift_transfers
                rtgsRepo.updateSwiftTransfers(reference,username);
                result = "{\"result\":\"0\",\"message\":\"Transaction processed successfully, check on K-PRINTER \"}";
            }else{
                result = "{\"result\":\"99\",\"message\":\"Transaction already posted, please confirm on K-PRINTER \"}";
            }

        return result;
    }

    @RequestMapping(value = "/rtgsBulkGlPosting")
    public String financePostingToMultipleGL(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("pageTitle", "FINANCE POSTING OF MULTIPLE GLS AT AN INSTANCE ( ONE TO MANY )");
        model.addAttribute("banks", rtgsRepo.getBanksList());
        model.addAttribute("taxCategories", rtgsRepo.getTaxCategoryList());
        //get the Service providers Lists
        model.addAttribute("serviceProviders", rtgsRepo.getServiceProvidersLists());
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/rtgsBulkGlPosting", "SUCCESS", "Finance posting to multiple gl at one instance"));

        return "modules/rtgs/financePostingToMultipleGL";
    }

    @PostMapping(value = "/fireInitiateMultipleRTGSRemittance")
    @ResponseBody
    public JsonResponse financeInitiateMultipleRTGSRemittance(@Valid FinanceMultipleGlPosting form, @RequestParam("supportingDoc") MultipartFile[] files, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, BindingResult bindingResult) throws ParseException, JsonProcessingException {
        JsonResponse response = new JsonResponse();
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> {
                                return error1;
                            })
                    );
            response.setStatus("FAIL");
            response.setResult(errors);
        } else {
            String initiatedBy = session.getAttribute("username") + "";
            String branchNo = session.getAttribute("branchCode") + "";
            if(1 == rtgsRepo.insertMultipleGLPostingData(customeQuery,initiatedBy,branchNo)){
             response.setStatus("SUCCESS");
             response.setResult("Transaction initiated successfully, awaiting approval");
            }else{
             response.setStatus("ERROR");
             response.setResult("Failed to Log transaction into database");
            }
//            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
//            String transferCuttOff = systemVariables.EFT_TRANSACTION_SESSION_TIME.split(",")[1];
//            List<Map<String, Object>> cuttoff = rtgsRepo.getTransferCuttOff("001");
//            Date startTime = parser.parse("00:00");
//            if (cuttoff != null) {
//                transferCuttOff = cuttoff.get(0).get("cutt_off_time") + "";
//            }
//            Date endTime = parser.parse(transferCuttOff);
//            Date txnDate = parser.parse(DateUtil.now("HH:mm"));
//            //CHECK IF SWIFT IS ALLOWED ON THIS DATE
//            String dayNames[] = new DateFormatSymbols().getWeekdays();
//            Calendar date2 = Calendar.getInstance();
//            System.out.println("Today is a " + dayNames[date2.get(Calendar.DAY_OF_WEEK)]);
//            String todayName = dayNames[date2.get(Calendar.DAY_OF_WEEK)];
//            boolean hasVAT = Boolean.parseBoolean(customeQuery.get("vat"));
//            String checkIfAllowed = rtgsRepo.getTransactionCalender(todayName, "001");//CHECK IF TRANSACTIONS ARE ALLOWED ON THIS DAY
//            if (checkIfAllowed.equalsIgnoreCase("A")) {
//                if (txnDate.after(startTime) && txnDate.before(endTime)) {
//                    //CALCULATE WITH-HOLDING TAX
//                    BigDecimal taxableAmount = new BigDecimal(rtgsTransferFormFinance.getTaxableAmount());
//                    BigDecimal taxRate = new BigDecimal(rtgsTransferFormFinance.getTaxRate().split("==")[0]);
//                    BigDecimal tax = taxRate.multiply(taxableAmount);
//                    LOGGER.info("TAX AMOUNT: {}", tax);
//                    //CALCULATE AMOUNT TO BE TRANSFERRED TO SERVICE PROVIDER
//                    BigDecimal totalAmount = new BigDecimal(rtgsTransferFormFinance.getAmount());
//                    BigDecimal amount = totalAmount.subtract(tax);
//                    LOGGER.info("TOTAL AMOUNT: {} AMOUNT PAYABLE:{} AFTER TAX : {}", rtgsTransferFormFinance.getAmount(), amount, tax);
//                    rtgsTransferFormFinance.setAmount(amount.toString());
//                    //DISPLAY THE FINANCE FORM AFTER CALCULATION
//                    response.setStatus("SUCCESS2");
//                    String direction = "T";
//                    String correspondentBank = systemVariables.BOT_SWIFT_CODE;
//                    String msgType = "LOCAL";
//                    String branchCode = session.getAttribute("branchCode").toString();//get the branchcode from the session
//                    String txnType = "001";
//                    String initiatedBy = session.getAttribute("username").toString();
//                    //SET BENEFICIARY ACCOUNT FROM THE SUBMITTED PAYLOAD
//                    rtgsTransferFormFinance.setBeneficiaryAccount(rtgsTransferFormFinance.getBeneficiaryAccount().split("==")[1]);
//                    //INTERNATIONAL RTGS TRANSACTIONS OUTSIDE TANZANIA AND EAST AFRICA BORDERS
//                    if (rtgsTransferFormFinance.getBeneficiaryBIC().split("==")[1].equals("INTERNATIONAL")) {
//                        direction = "R";
//                        msgType = "INTERNATIONAL";
//                        txnType = "004";
//                        String address = rtgsTransferFormFinance.getSenderAddress() + " TANZANIA";
//                        rtgsTransferFormFinance.setSenderAddress(address);
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("USD")) {
//                            correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
//                        }
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("EUR")) {
//                            correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
//                        }
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("ZAR")) {
//                            correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
//                        }
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("GBP")) {
//                            correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
//                        }
//                    }
//                    //TIS TRANSACTION ALONG EAST AFRICA
//                    if (rtgsTransferFormFinance.getBeneficiaryBIC().split("==")[1].equals("EAPS")) {
//                        direction = "T";
//                        msgType = "EAPS";
//                        txnType = "001";
//                    }
//                    //TT FOR EAPS TRANSACTION ALONG EAST AFRICA
//                    if (rtgsTransferFormFinance.getBeneficiaryBIC().split("==")[1].equals("EAPS") && (!rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("KES") || !rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("UGS"))) {
////                direction = "T";
////                msgType = "EAPS";
////                txnType = "001";
//                        direction = "R";
//                        msgType = "INTERNATIONAL";
//                        txnType = "004";
//                        String address = rtgsTransferFormFinance.getSenderAddress() + " TANZANIA";
//                        rtgsTransferFormFinance.setSenderAddress(address);
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("USD")) {
//                            correspondentBank = systemVariables.USD_CORRESPONDEND_BANK;
//                        }
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("EUR")) {
//                            correspondentBank = systemVariables.EURO_CORRESPONDEND_BANK;
//                        }
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("GBP")) {
//                            correspondentBank = systemVariables.GBP_CORRESPONDEND_BANK;
//                        }
//                        if (rtgsTransferFormFinance.getCurrency().equalsIgnoreCase("ZAR")) {
//                            correspondentBank = systemVariables.ZAR_CORRESPONDEND_BANK;
//                        }
//                    }
//                    String reference = "mugl"+initiatedBy.toLowerCase() + formattedDate;
//                    String swiftMessage = SwiftService.createFinanceMT103(rtgsTransferFormFinance, Calendar.getInstance().getTime(), systemVariables.SENDER_BIC, msgType, reference, correspondentBank);
//                    LOGGER.info("SWIFT MESSAGE:{} ", swiftMessage);
//                    //System.out.println("RTGS FORM: " + rtgsTransferForm.toString());
//                    if (amount.compareTo(new BigDecimal(systemVariables.AMOUNT_THAT_REQUIRES_COMPLIANCE)) > 0) {
//                        rtgsRepo.updateComplianceStatus(reference);
//                    }
//                    //save the supporting documents
//                    for (MultipartFile file : files) {
//                        if (!file.isEmpty()) {
//                            rtgsRepo.saveSupportingDocuments(reference, file);
//                        }
//                    }
//                    LOGGER.info("REQUEST INITIATED: {}", rtgsTransferFormFinance);
//                    int res = rtgsRepo.saveInitiatedFinanceRTGSRemittance(rtgsTransferFormFinance, reference, txnType, initiatedBy, swiftMessage, branchCode, files, totalAmount, tax, hasVAT);
//                    if (res != -1) {
//                        response.setJsonString("Transaction is successfully Initiated");
//                    } else {
//                        response.setJsonString("an error occurred during processing. Please Try again !!!!!!");
//                    }
//                    //audit trails
//                    String username = session.getAttribute("username") + "";
//                    String branchNo = session.getAttribute("branchCode") + "";
//                    String roleId = session.getAttribute("roleId") + "";
//                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financeInitiateRTGSRemittance-> Transaction initiated with reference: " + reference, "SUCCESS", "Viewed rtgs dashboard"));
//
//                } else {
//                    //audit trails
//                    String username = session.getAttribute("username") + "";
//                    String branchNo = session.getAttribute("branchCode") + "";
//                    String roleId = session.getAttribute("roleId") + "";
//                    this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financeInitiateRTGSRemittance-> ", "Failed", "Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time"));
//                    response.setJsonString("Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time");
//
//                }
//            } else {
//                String username = session.getAttribute("username") + "";
//                String branchNo = session.getAttribute("branchCode") + "";
//                String roleId = session.getAttribute("roleId") + "";
//                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/financeInitiateRTGSRemittance-> ", "Failed", "Transaction cut-off time is from 00:00 to 15:00 for both RTGS and EFT; This transaction cannot be processed at this time"));
//
//                response.setValidated(false);
//                response.setJsonString("Today SWIFT is not operational, Please post this payment on another day");
//            }
//
//            if (1 == settingsRepo.updateServiceProvider(spForm,spId, session.getAttribute("username")+"")) {
//                response.setStatus("SUCCESS");
//                response.setResult("Service Provider updated successfully");
//            } else {
//                response.setStatus("ERROR");
//                response.setResult("failed to submit data into the database");
//            }
        }
        return response;
    }

    @RequestMapping(value = "/bankGls", method = RequestMethod.GET)
    public String bankGls(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest httpRequest) {
        model.addAttribute("pageTitle", "LIST OF BANK GENERAL LEDGERS");
        return "modules/rtgs/bankGls";
    }

    @PostMapping(value = "/getBankGlsAjax")
    @ResponseBody
    public String getBankGlsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return rtgsRepo.getBankGlsAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @GetMapping("/returnedTransactionsFromOtherBanks")
    public String returnedTransactionsFromOtherBanks(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","RETURNED TRANSACTIONS FROM OTHER BANKS");
        model.addAttribute("banks", rtgsRepo.getBanksList());
        model.addAttribute("fromDate",DateUtil.previosDay(3));
        model.addAttribute("toDate",DateUtil.tomorrow());
        return "/modules/rtgs/returnedTransactionsFromOtherBanks";
    }

    @PostMapping(value = "/returnedTransactionsFromOtherBanksAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse returnedTransactionsFromOtherBanksAjax(@RequestParam Map<String, String> customQuery){
        return rtgsRepo.getRejectedTransactionsAjax(customQuery);
    }
    @PostMapping(value = "/fire/stawi/lookup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<StawiBondLookupResponse> stawiLookup(
            @RequestBody StawiBondLookupRequest body,
            HttpServletRequest request,
            HttpSession session) {

        if (body == null || body.getDseAccount() == null || body.getDseAccount().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new StawiBondLookupResponse("FAILED", "dseAccount is required", "96", null));
        }
        if (body.getTransactionReference() == null || body.getTransactionReference().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new StawiBondLookupResponse("FAILED", "transactionReference is required", "96", null));
        }

        // find the transfer using the transaction reference from the request
        Optional<Transfers> optionalTransfers = transfersRepository.findByReference(body.getTransactionReference());
        if (!optionalTransfers.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new StawiBondLookupResponse(
                            "FAILED",
                            "Transfer with reference " + body.getTransactionReference() + " not found",
                            "0404",
                            null));
        }
        Transfers transfer = optionalTransfers.get();

        // Call the client
        StawiBondLookupResponse resp = stawiBondNotificationClient.lookup(body.getDseAccount());

        // treat success as either responseCode == "00" OR status == "SUCCESS"
        boolean ok = ("00".equalsIgnoreCase(resp.getResponseCode()))
                || ("SUCCESS".equalsIgnoreCase(resp.getStatus()));

        if (ok && resp.getResponse() != null) {
            transfer.setSender_phone(resp.getResponse().getInvestorPhoneNumber());
            transfersRepository.save(transfer);

            String username = String.valueOf(session.getAttribute("username"));
            String branchNo = String.valueOf(session.getAttribute("branchCode"));
            String roleId   = String.valueOf(session.getAttribute("roleId"));
            this.applicationEventPublisher.publishEvent(
                    new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(),
                            "/fire/stawi/lookup", "SUCCESS",
                            "Lookup for dseAccount: " + body.getDseAccount() +
                                    " trxRef: " + body.getTransactionReference()));
            return ResponseEntity.ok(resp);
        } else {
            String username = String.valueOf(session.getAttribute("username"));
            String branchNo = String.valueOf(session.getAttribute("branchCode"));
            String roleId   = String.valueOf(session.getAttribute("roleId"));
            this.applicationEventPublisher.publishEvent(
                    new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(),
                            "/fire/stawi/lookup", "ERROR",
                            "Failed to Lookup dseAccount: " + body.getDseAccount() +
                                    " trxRef: " + body.getTransactionReference()));
            return ResponseEntity.ok(resp);
        }
    }


    @PostMapping(
            value = "/fire/stawi/notify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> stawiNotify(@RequestBody StawiBondPostRequest requestBody, HttpServletRequest request, HttpSession session, @RequestParam(value = "txnid", required = false) List<String> txnid) {
        Map<String, Object> map = new HashMap<>();
        String reference = requestBody.getTransactionReference();
        String cdsNumber = requestBody.getDseAccount();
        String comment = requestBody.getReason();
        Optional<Transfers> transfersOpt = transfersRepository.findByReference(reference);
        if (!transfersOpt.isPresent()){
            map.put("status", "Error");
            map.put("message","Transaction with reference " + reference + " not found");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
        // Call the client
        StawiBondLookupResponse resp = stawiBondNotificationClient.lookup(cdsNumber);

        // treat success as either responseCode == "00" OR status == "SUCCESS"
        boolean ok = ("00".equalsIgnoreCase(resp.getResponseCode()))
                || ("SUCCESS".equalsIgnoreCase(resp.getStatus()));
        if (!ok && resp.getResponse() == null) {
            map.put("status", "Error");
            map.put("message","CDS number " + cdsNumber + " Verification Failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }

        Transfers transfers = transfersOpt.get();
        StawiBondNotificationRequest body = new StawiBondNotificationRequest();
        body.setAmount(transfers.amount);
        body.setNarration(transfers.destinationAcct);
        body.setChannel("RTGS");
        body.setReference(reference);
        body.setCurrency(transfers.currency);
        body.setCustomerName(resp.getResponse().getInvestorName());
        body.setDseAccount(cdsNumber);
        body.setSourceAccount(systemVariables.TRANSFER_MIRROR_TISS_BOT_LEDGER);
        body.setPhoneNumber(transfers.sender_phone);

        LOGGER.info("Request Boday: {}", body);

        try {
            ResponseEntity<StawiBondLookupResponse> upstream = stawiBondNotificationClient.send(body);

            // audit
            String username = String.valueOf(session.getAttribute("username"));
            String branchNo = String.valueOf(session.getAttribute("branchCode"));
            String roleId   = String.valueOf(session.getAttribute("roleId"));
            if("00".equalsIgnoreCase(Objects.requireNonNull(upstream.getBody()).getResponseCode())){
                transfers.setCbsStatus("C");
                transfers.setComments(comment);
                transfersRepository.save(transfers);
            }
            this.applicationEventPublisher.publishEvent(
                    new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(),
                            "/fire/stawi/notify",
                            upstream.getStatusCode().is2xxSuccessful() ? "SUCCESS" : "FAILED",
                            "MTN-STAWI notify dse=" + body.getDseAccount() + ", amt=" + body.getAmount() + " " + body.getCurrency()));

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", upstream.getStatusCode().is2xxSuccessful() ? "SUCCESS" : "ERROR");
            payload.put("message", upstream.getStatusCode().is2xxSuccessful()
                    ? "Notification sent"
                    : "Upstream returned " + upstream.getStatusCodeValue());
            payload.put("providerStatus", upstream.getStatusCodeValue());
            payload.put("providerBody", upstream.getBody());
            return ResponseEntity.status(upstream.getStatusCode()).body(payload);

        } catch (Exception e) {
            // audit fail
            String username = String.valueOf(session.getAttribute("username"));
            String branchNo = String.valueOf(session.getAttribute("branchCode"));
            String roleId   = String.valueOf(session.getAttribute("roleId"));
            this.applicationEventPublisher.publishEvent(
                    new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(),
                            "/fire/stawi/notify", "FAILED", "MTN-STAWI notify error: " + e.getMessage()));
            map.put("status", "FAILED");
            map.put("message", "Failed to send notification");

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(map);
        }
    }

}
