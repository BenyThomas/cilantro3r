/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.CashMovementRequestObj;
import com.DTO.GeneralJsonResponse;
import com.DTO.RemittanceToQueue;
import com.DTO.Reports.AuditTrails;
import com.DTO.Teller.FundTransferReq;
import com.DTO.Teller.MobileMovement;
import com.DTO.Teller.VikobaFundMovement;
import com.DTO.Teller.VikobaUpdateRequest;
import com.config.SYSENV;
import com.controller.itax.CMSpartners.JsonResponse;
import com.core.event.AuditLogEvent;
import com.helper.CSVHelper;
import com.helper.DateUtil;
import com.helper.MaiString;
import com.models.ThirdPartyTxn;
import com.repository.RtgsRepo;
import com.repository.SftpRepo;
import com.repository.TransactionRepo;
import com.queue.QueueProducer;
import com.service.FileReaderService;
import com.service.HttpClientService;
import com.service.SwiftService;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.service.XapiWebService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import philae.api.BnUser;
import philae.api.UsRole;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class Transactions {

    @Autowired
    TransactionRepo transactionRepo;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SftpRepo.class);
    //    @Value("${sender.swiftcode}")
//    public String senderBIC;
//    @Value("${bot.swiftcode}")
//    public String BOTSwiftCode;
    @Autowired
    SYSENV systemVariable;

    @Autowired
    QueueProducer queProducer;

    @Autowired
    XapiWebService xapiWebService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    RtgsRepo rtgsRepo;

    @Autowired
    FileReaderService fileReaderService;

    @RequestMapping(value = "/searchGwTxn")
    public String searchGwTxn(Model model, HttpSession session) {
        AuditTrails.setComments("View  Search Transaction on Gateway/Core banking /Mkoba");
        AuditTrails.setFunctionName("/searchGwTxn");
        model.addAttribute("pageTitle", "SEARCH GATEWAY TRANSACTIONS");
        return "pages/transactions/searchGatewayTxns";
    }

    @RequestMapping(value = "/suspiciousTransactions")
    public String suspiciousTransactions(Model model, HttpSession session) {
        AuditTrails.setComments("View Suspicious Report");
        AuditTrails.setFunctionName("/suspiciousTransactions");
        model.addAttribute("pageTitle", "SUSPICIOUS TRANSACTIONS");
        return "pages/cbsSuccessThirdPartyFailed";
    }

    @RequestMapping(value = "/lookupMobileRegistration", method = RequestMethod.GET)
    public String lookupMobileRegistration(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, RedirectAttributes redirectAttributes) {
        System.out.println(customeQuery.toString());
        if (customeQuery.size() > 0) {
            model.addAttribute("last10Txns", transactionRepo.getLast10TransactionsGW(customeQuery.get("msisdn")));
            model.addAttribute("gwRecords", transactionRepo.getLookupRegistrationOnGW(customeQuery.get("msisdn")));
            model.addAttribute("rubikonRecords", transactionRepo.getLookupRegistrationOnRubikon(customeQuery.get("msisdn")));
        }
        //get user session for testing....
//        BnUser user = (BnUser) session.getAttribute("userCorebanking");
//        System.out.println("USER ROLE:" + user.getBranchName() + user.getRole() + user.getStaffName());

        AuditTrails.setComments("View mobile registered user on core banking & gateway with last 10 transactions performed: msisdn=" + customeQuery.get("msisdn"));
        AuditTrails.setFunctionName("/lookupMobileRegistration");
        return "pages/transactions/lookupMobileRegistration";
    }

    @RequestMapping(value = "/bulkSMS", method = RequestMethod.GET)
    //--- filter

    public String sendBulkSMS(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, RedirectAttributes redirectAttributes) {
        System.out.println(customeQuery.toString());
        if (customeQuery.size() > 0) {
            model.addAttribute("gwRecords", transactionRepo.getLookupRegistrationOnGW(customeQuery.get("msisdn")));
            model.addAttribute("rubikonRecords", transactionRepo.getLookupRegistrationOnRubikon(customeQuery.get("msisdn")));
        }

        AuditTrails.setComments("Sending Bulk sms to customers msisdn: " + customeQuery.get("msisdn"));
        AuditTrails.setFunctionName("/bulkSMS");
        //---

        return "pages/transactions/bulkSMS";
    }

    @RequestMapping(value = "/getSearchGwTxnAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSearchGwTxnAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String input = customeQuery.get("input");
        String searchon = customeQuery.get("searchon");
        String json = null;
        try {
            json = transactionRepo.getGwSearchTransactions(input, searchon);
            //audit trail;
            AuditTrails.setComments("Search Transaction on [" + searchon + "] with Reference/msisdn/receipt=[" + customeQuery.get("input"));
            AuditTrails.setFunctionName("/getSearchGwTxnAjax");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return json;
    }

    @RequestMapping(value = "/syncRubikonTxns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String syncRubikonTxns(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String json = transactionRepo.syncTxnsNotInRubikon(customeQuery.get("txnid").toString(), customeQuery.get("sourceAcct").toString(), customeQuery.get("destinationAcct").toString(), customeQuery.get("txn_type").toString(), session.getAttribute("ttype").toString());
        //audit trail;
        AuditTrails.setComments("Download a skiped transaction from core baking; with sourceAcct:" + customeQuery.get("sourceAcct").toString() + " Reference: " + customeQuery.get("txnid").toString() + " DestinationAcct: " + customeQuery.get("destinationAcct"));
        AuditTrails.setFunctionName("/syncRubikonTxns");
        return "{\"result\":\"" + json + "\"}";
    }

    @RequestMapping(value = "/syncRubikonTxnSearch", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String syncRubikonTxnsSearch(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        // if
        LOGGER.info("syncRubikonTxn search params... {}", customeQuery);
        String json = transactionRepo.syncTxnsSearchNotInRubikon(customeQuery.get("txnid").toString(), customeQuery.get("sourceAcct").toString(), customeQuery.get("destinationAcct").toString(), customeQuery.get("txn_type").toString(), customeQuery.get("ttype").toString());
        //audit trail;
        AuditTrails.setComments("Download a skiped transaction from core baking; with sourceAcct:" + customeQuery.get("sourceAcct").toString() + " Reference: " + customeQuery.get("txnid").toString() + " DestinationAcct: " + customeQuery.get("destinationAcct"));
        AuditTrails.setFunctionName("/syncRubikonTxns");
        return "{\"result\":\"" + json + "\"}";
    }

    @RequestMapping(value = "/syncMkobaNewOpenedAcct", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String syncMkobaNewOpenedAcct(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        int json = transactionRepo.syncMkobaAcctOpeningTxns(customeQuery.get("txnid").toString());
        //audit trail;
        AuditTrails.setComments("Resolved Exception of new accounts opened on core banking which doesnot belong to Third party: references=[" + customeQuery.get("txnid") + "]");
        AuditTrails.setFunctionName("/syncMkobaNewOpenedAcct");
        return "{\"result\":\"code:" + json + " Synced successfully\"}";
    }

    @RequestMapping(value = "/syncMkobaNoSignatories", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String syncMkobaNoSignatories(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        int json = transactionRepo.syncMkobaAccountsWithNoSignatories(customeQuery.get("txnid").toString());
        //audit trail;
        AuditTrails.setComments("Resolved Exception of transactions from thirdparty which group account is not opened yet: references=[" + customeQuery.get("txnid") + "]");
        AuditTrails.setFunctionName("/syncMkobaNewOpenedAcct");
        return "{\"result\":\"code:" + json + " Request is being reprocessed\"}";
    }

    //cash movement view.....
    @RequestMapping(value = "/remittance")
    public String remittance(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        AuditTrails.setComments("View Remittance");
        AuditTrails.setFunctionName("/remittance");
//        model.addAttribute("controlAccts", transactionRepo.getCashMovementAccts());
//        model.addAttribute("transferType", transactionRepo.getTransferTypes());

        return "pages/transactions/remittance";
    }
    //cash movement view.....

    @RequestMapping(value = "/mobileCashMovement")
    public String mobileCashMovement(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        String roleId = session.getAttribute("roleId") + "";
        model.addAttribute("roleId",roleId);
        return "modules/mobileMovement/mobileCashMovement";
    }

    @RequestMapping(value = "/initiateMobileCashMovement")
    public String initiateMobileCashMovement(HttpServletRequest request, HttpSession session, Model model) {
        Calendar cal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        cal.add(Calendar.DATE, 0);
        String transDate = dateFormat.format(cal.getTime());
        model.addAttribute("pageTitle", "INITIATE MOBILE CASH MOVEMENT");
        model.addAttribute("controlAccounts", transactionRepo.getCashMovementAccts());
        model.addAttribute("defaultNarr","TRANSFER FROM COLLECTION TO DISBURSEMENT ACCOUNT  [ Enter transaction receipt here... ]  " + transDate);
        return "pages/transactions/initiateMobileCashMovement";
    }

    //initiate cashmovement....
    @PostMapping(value = "/fireSubmitInitiatedMobileCashMovement")
    @ResponseBody
    public JsonResponse submitInitiatedMobileCashMovement(HttpSession httpsession, @Valid MobileMovement customeQuery, @RequestParam("supportingDoc") MultipartFile file, BindingResult bindingResult) throws IOException {

        JsonResponse response = new JsonResponse();
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
            String sourceAcctDestAcct = customeQuery.getSourceAcctDestAcct();
            String sourceAcct = sourceAcctDestAcct.split("==")[0];
            String sourceAcctName = sourceAcctDestAcct.split("==")[1];
            String desitinationAcct = sourceAcctDestAcct.split("==")[2];
            String desitinationAcctName = sourceAcctDestAcct.split("==")[3];
            String indicator = customeQuery.getIndicator();
            if(indicator.equalsIgnoreCase("viaBOT")){
                desitinationAcct = "1-000-00-1111-1111001";
            }
            String amount = customeQuery.getAmount().replace(",","");
            String reference = customeQuery.getReference();

            if (!file.isEmpty()) {
                transactionRepo.saveSupportingDoc(reference, file);
            }
            String code = sourceAcctDestAcct.split("==")[4];
            String status = "I";
            String cbsStatus = "P";
            String comments = customeQuery.getComments();
            String initiatedBy = httpsession.getAttribute("username").toString();
            AuditTrails.setComments("Initiate cash Movement of transactions");
            AuditTrails.setFunctionName("/initiateCashMovement");

            if (1 == transactionRepo.saveInitiateCashmovement(sourceAcct,sourceAcctName, desitinationAcct,desitinationAcctName, amount, reference, code, indicator, status,cbsStatus, comments, initiatedBy)) {
                response.setStatus("SUCCESS");
                response.setResult("Transaction successfully initiated please check on INITIATED TRANSACTIONS");
            } else {
                response.setStatus("ERROR");
                response.setResult("failed to submit data into the database");
            }
        }
        return response;
    }

    @RequestMapping(value = "/gepgMNORemittanceTxns")
    public String gepgMNORemittanceTxns(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        AuditTrails.setComments("View gepgMNORemittanceTxns ");
        AuditTrails.setFunctionName("/cashMovement");

        model.addAttribute("transferTypes", transactionRepo.getRTGSTransferTypes((String) session.getAttribute("roleId")));
        model.addAttribute("pageTitle", "APPROVE GEPG/MNO REMITTANCE TXNS");
        return "pages/transactions/gepgMNORemittanceTxns";
    }

    @RequestMapping(value = "/gepgRemittance")
    public String gepgRemittance(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        AuditTrails.setComments("GePG remittance");
        AuditTrails.setFunctionName("/gepgRemittance");

        model.addAttribute("transferTypes", transactionRepo.getRTGSTransferTypes((String) session.getAttribute("roleId")));
        model.addAttribute("pageTitle", "INITIATE GEPG REMITTANCE TO BOT");
        return "pages/transactions/gepgRemittance";
    }

    @RequestMapping(value = "/getGepgMNORemittanceTxnsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getGepgMNORemittanceTxnsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return transactionRepo.getGepgMNORemittanceTxnsAjax(mno, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getGepgAcountsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getGepgAcountsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return transactionRepo.getGepgAccountsAjax(mno, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getGepgAccountBalancesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getGepgAccountBalancesAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String accts = customeQuery.get("accts");
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return transactionRepo.getGepgAccountsAjax(accts, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/previewGePGBalancesForRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String previewGePGBalancesForRemittance(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String accts = customeQuery.get("accts");
        System.out.println("ACCOUNTS:" + accts);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return transactionRepo.getGepgAccountBalanceAJax(accts, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    //initiate gepgRemittance
    @RequestMapping(value = "/initiateGePGRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String initiateGePGRemittance(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) throws IOException {
        DateFormat df = new SimpleDateFormat("yyMMddHH"); // Just the year, with 2 digits
        String formattedDate = df.format(Calendar.getInstance().getTime());
        String[] inputString = customeQuery.get("accts").split("__");
        String customNarration = customeQuery.get("customNarration");
        LOGGER.info("ABOUT TO INITITIATE GEPG REMITTANCE: customNarration->{}", customNarration);

        if (customNarration == null || customNarration.trim().equals("")) {
            LOGGER.info("ERRORR:=> NARRATION IS REQUIRED.. customNarration->{}", customNarration);
            return "{\"result\":\"ERRORR:=> NARRATION IS REQUIRED..\"}";
        } else {
            if (customNarration.trim().length() > 30) {
                LOGGER.info("ERRORR:=> NARRATION LENGTH IS GREATER THAN 30 CHARACTERS.. customNarration->{}[{}]", customNarration, customNarration.trim().length());
                return "{\"result\":\"ERRORR:=> NARRATION LENGTH IS GREATER THAN 30 CHARACTERS.\"}";
            }
            customNarration = customNarration.replace("&", "");
            customNarration = customNarration.replace("@", "");
            customNarration = customNarration.replace("/", "");
            customNarration = customNarration.replace("-", "");
            customNarration = customNarration.trim();

            String sourceAcct;
            String desitinationAcct;
            String amount;
            String reference;
            String currency;
            String senderName;
            String spcode;
            String txn_type = "003";
            String narration = "TRANSFER";
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            DateFormat dateFormat2 = new SimpleDateFormat("dd");
            DateFormat dateFormat3 = new SimpleDateFormat("/MM/yyyy");
            Calendar c = Calendar.getInstance();
            LOGGER.info("ABOUT TO INITITIATE GEPG REMITTANCE: {}", inputString.toString());
            //check if its monday
            if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                //get sunday date
                c.add(Calendar.DATE, -1);
                String sundayDate = dateFormat.format(c.getTime());
                //get last week monday date as well
                c.add(Calendar.DATE, -6);
                String previousMondayDate = dateFormat2.format(c.getTime());
                narration = "collection DD " + previousMondayDate + "-" + sundayDate;
            } else {
                Calendar ce = Calendar.getInstance();
                Calendar cc = Calendar.getInstance();
                ce.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                ce.add(Calendar.DATE, -2);
                String previousDate = dateFormat2.format(ce.getTime());
                String today = dateFormat2.format(cc.getTime());
                narration = "collection DD " + previousDate + "-" + today + dateFormat3.format(cc.getTime());
            }
            List<FundTransferReq> gepgRemittance = new ArrayList<>();
            for (String s : inputString) {
                FundTransferReq row = new FundTransferReq();
                sourceAcct = s.split("==")[0];
                senderName = s.split("==")[1];
                senderName = senderName.replace("&", " and");
                senderName = senderName.replace("@", "");
                senderName = senderName.replace("/", "");
                senderName = senderName.replace("-", "");
                senderName = senderName.trim();

                currency = s.split("==")[2];
                amount = s.split("==")[3];
                System.out.println("BRANCH CODE:" + httpsession.getAttribute("branchCode").toString());
                List<Map<String, Object>> accountPartner=  transactionRepo.getBOTAccountGePG(sourceAcct, currency);
                if(!accountPartner.isEmpty()) {
                    spcode = accountPartner.get(0).get("partner_code").toString();
                    desitinationAcct = accountPartner.get(0).get("bot_acct").toString();
                    //replace spcode

                    spcode = spcode.replace("SP99", "");
                    //new gepg transaction
                    spcode = spcode.replace("SP98", "");

                    reference = "STP" + spcode + currency.substring(0, 2) + formattedDate;

                    //rewrite the reference number so it can be of 16 length
                    reference = MaiString.pad16LengthReference(reference);

                    //set the list into arraylist
                    row.setSenderName(senderName);
                    row.setDestinationAcct(desitinationAcct);
                    row.setBeneficiaryName(senderName);
                    row.setAmount(new BigDecimal(amount));
                    row.setSourceAcct(sourceAcct);
                    row.setReference(reference);
                    row.setTxnType(txn_type);
                    row.setCurrency(currency);
                    row.setInitiatedBy(httpsession.getAttribute("username").toString());
                    row.setBranchNo(httpsession.getAttribute("branchCode").toString());
                    row.setStatus("I");
                    //old way
                    //row.setDescription(narration);
                    row.setDescription(customNarration);
                    row.setSenderBIC(systemVariable.SENDER_BIC);
                    row.setBankOperationalCode("CRED");
                    row.setReceiverBIC(systemVariable.BOT_SWIFT_CODE);
                    row.setDetailsOfCharge("OUR");
                    row.setMsgType("LOCAL");
                    row.setSenderAddress(" ");
                    row.setSenderPhone("  ");
                    row.setSenderCorrespondent(systemVariable.BOT_SWIFT_CODE);
                    row.setTranDate(Calendar.getInstance().getTime());

                    String swiftmessage = SwiftService.createMT103(row);
                    row.setSwiftMessage(swiftmessage);//generate swiftmessage
                    gepgRemittance.add(row);
                }
            }
            LOGGER.info("gepgRemittance size:{} ", gepgRemittance);
            int[] result = transactionRepo.insertGepgRemittance(gepgRemittance);
            LOGGER.info("RESULTS FROM INSERTING DATA:{} ", result);
            return "{\"result\":\"successfully initiated GePG Remittance\"}";
        }
    }

    @RequestMapping(value = "/approveGePGMNORemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String authorizeGePGRemittance(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"-1\",\"message\":\"Please select your posting role:35\"}";
        String references = customeQuery.get("references");
        if (postingRole != null) {
            System.out.println("here we are with posting role on session: roleID:" + postingRole);
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
            for (UsRole role : user.getRoles().getRole()) {
//                System.out.println("BRANCHCODE: " + role.getBranchCode());
//                System.out.println("ROLES: " + role.getUserRoleId());
//                System.out.println("ROLE IN SESSION: " + postingRole);
//                System.out.println("LIMITS: " + role.getLimits().getLimit());
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    System.out.println("THE ROLE EXIST ON THE SESSION ROLES");
                    for (String s : references.split(",")) {
                        //System.out.println("REFERENCE FOR TXN:"+s);
                        RemittanceToQueue remittanceToQue = new RemittanceToQueue();
                        remittanceToQue.setBnUser(user);
                        remittanceToQue.setUsRole(role);
                        remittanceToQue.setReferences(s);
                        queProducer.sendToQueueGePGRemittance(remittanceToQue);
                    }
                    result = "{\"result\":\"0\",\"message\":\"Your Request is being processed. Please confirm the transactions on RUBIKON .\"}";
                    break;

                } else {
                    result = "{\"result\":\"-1\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
//        System.out.println("here we are.......");
        return result;
    }

    @RequestMapping(value = "/authorizeGePGPstngVldtinRemittance", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String authorizeGePGPostingValidationRemittance(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"-1\",\"message\":\"Please select your posting role:35\"}";
        String references = customeQuery.get("references");
        LOGGER.info("authorizeGePGPstngVldtinRemittance->Reference: {}", references);
        if (postingRole != null) {
            System.out.println("here we are with posting role on session: roleID:" + postingRole);
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
            for (UsRole role : user.getRoles().getRole()) {
//                System.out.println("BRANCHCODE: " + role.getBranchCode());
//                System.out.println("ROLES: " + role.getUserRoleId());
//                System.out.println("ROLE IN SESSION: " + postingRole);
//                System.out.println("LIMITS: " + role.getLimits().getLimit());
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    System.out.println("THE ROLE EXIST ON THE SESSION ROLES");

                    for (String s : references.split(",")) {
                        //System.out.println("REFERENCE FOR TXN:"+s);
                        RemittanceToQueue remittanceToQue = new RemittanceToQueue();
                        remittanceToQue.setBnUser(user);
                        remittanceToQue.setUsRole(role);
                        remittanceToQue.setReferences(s);
                        queProducer.sendToQueueGePGToSwift(remittanceToQue);
                    }
                    result = "{\"result\":\"0\",\"message\":\"Your Request is being process. Please check the transactions on the VERIFIER STAGE ON SWIFT.\"}";
                    break;
                } else {
                    result = "{\"result\":\"-1\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    //download supporting document
    @RequestMapping(value = "/downloadSupportingDoc")
    @ResponseBody
    public String downloadSupportingDoc(HttpServletResponse response, @RequestParam Map<String, String> customeQuery) {
        try {
            String reference = customeQuery.get("reference");
            byte[] databyte = transactionRepo.getSupportingDocument(reference, customeQuery.get("fileName"));

            byte[] file = databyte;
            response.setContentType("application/pdf");
            response.setHeader("Content-disposition", "attachment; filename=" + reference + ".pdf");
            response.setHeader("Content-Length", String.valueOf(file.length));
            response.getOutputStream().write(file);
            response.getOutputStream().close();
            LOGGER.info("supporting document... DOWNLOADED SUCCESSFULLY {}", reference);
            return "OK";
        } catch (Exception ex) {
            LOGGER.info("exception on download supporting document:{}", ex);
        }
        return "OK";
    }

    @RequestMapping(value = "/onlineRemittance")
    public String onlineRemittance(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        AuditTrails.setComments("Approve online banking  remittances");
        AuditTrails.setFunctionName("/onlineRemittance");
        model.addAttribute("transferTypes", transactionRepo.getRTGSTransferTypes((String) session.getAttribute("roleId")));
        model.addAttribute("pageTitle", "APPROVE TPB ONLINE BANKING TRANSACTIONS");
        return "pages/transactions/onlineRemittance";
    }

    @RequestMapping(value = "/getBranchRemittance")
    public String getBranchRemittance(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        AuditTrails.setComments("view branch  remittances");
        AuditTrails.setFunctionName("/onlineRemittance");
        model.addAttribute("transferTypes", transactionRepo.getRTGSTransferTypes((String) session.getAttribute("roleId")));
        System.out.println("HERE ON BRANCH REMITTANCE TAB:");
        System.out.println("TRANSFER TYPES:" + transactionRepo.getRTGSTransferTypes((String) session.getAttribute("roleId")));
        model.addAttribute("pageTitle", "APPROVE BRANCH REMITTANCE TRANSACTIONS");
        return "pages/transactions/ApproveBranchRemittance";
    }

    //get online initiated transactions ready for approval at IBD LEVEL
    @RequestMapping(value = "/getOnlineRemittanceAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getOnlineRemittance(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return transactionRepo.getOnlineRemittanceAjax(mno, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }
    //get online initiated transactions ready for approval at IBD LEVEL

    @RequestMapping(value = "/getBranchRemittanceAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getBranchRemittanceAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        // AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        //  AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");
        return transactionRepo.getBranchRemittanceAjax(mno, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireMobileMovementWF")
    public String fireMobileMovementWF( HttpSession session, Model model) {

        model.addAttribute("pageTitle", "INITIATED MOBILE CASH MOVEMENT");
//        model.addAttribute("controlAccounts", transactionRepo.getCashMovementAccts());
        return "modules/mobileMovement/mobileCashMovementTxnWF";
    }

    @PostMapping(value = "/fireMobileMovementWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireMobileMovementWFAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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

        return transactionRepo.fireMobileMovementWFAjax(roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/firePreviewCashMovementTxn")
    public String firePreviewCashMovementTxn(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        String reference=customeQuery.get("reference");
        model.addAttribute("pageTitle", "AUTHORIZATION OF CASH MOVEMENT TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("reference", reference);
        model.addAttribute("supportingDocs", transactionRepo.getCMSupportingDocument(reference) );
        model.addAttribute("transactionDetails", transactionRepo.cashMovementRequestTxnObj(reference));
        return "modules/mobileMovement/modal/previewMobileTxn";
    }



    /*
     *BRANCH AUTHORIZE TIPS TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/fireAuthMobileMovemntToPV", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthMobileMovemntToPV(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String txid = customeQuery.get("reference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {

                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";

                        result = transactionRepo.fireAuthMobileMovemntToPV(customeQuery.get("reference"), role, username,branchNo);

                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthMobileMovemntToPV", "SUCCESS", "Authorize mobile movement payments"));
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


    @RequestMapping(value = "/fireGetCMTxnsForPVWF")
    public String fireGetCMTxnsForPVWF( HttpSession session, Model model) {
        model.addAttribute("pageTitle", "PENDING MOBILE CASH MOVEMENT AT POSTING AND VALIDATION");
        return "modules/mobileMovement/fireGetCMTxnsForPVWF";
    }

    @PostMapping(value = "/fireGetCMTxnsForPVWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireGetCMTxnsForPVWFAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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

        return transactionRepo.fireGetCMTxnsForPVWFAjax(roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/firePreviewCashMovementTxnPVWF")
    public String firePreviewCashMovementTxnPVWF(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("pageTitle", "AUTHORIZATION OF CASH MOVEMENT TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", transactionRepo.getCMSupportingDocument(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("transactionDetails", transactionRepo.cashMovementRequestTxnObj(customeQuery.get("reference")));
        return "modules/mobileMovement/modal/previewMobileTxnPVWF";
    }

    /*
     *BRANCH AUTHORIZE TIPS TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/fireAuthMobileMovemntPVWFToGL", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthMobileMovemntPVWFToGL(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
        int finalRes ;
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String reference = customeQuery.get("reference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {

                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";
                        CashMovementRequestObj obj = transactionRepo.cashMovementRequestTxnObj(reference);
                        //String destinationGL, String reference, Double amount, String sourceGl, String currency, String narration
                        finalRes = xapiWebService.postGlToGLTransfer(obj.getDestinationAcc(),obj.getReference(),obj.getAmount(),obj.getSourceAcc(),"TZS",obj.getComments());
                        if(finalRes ==0){
                            transactionRepo.updateCashMovementTxn(obj,username);
                            result = "{\"result\":"+finalRes+",\"message\":\"Success, mobile transaction posted successifully. \"}";
                        }else{
                            result = "{\"result\":"+finalRes+",\"message\":\"Error occured on channel manager when authorizing mobile movement transaction \"}";
                            LOGGER.info("Error with channel manager... {}",finalRes);
                        }
                        LOGGER.info("Mobile movement final response... {}",finalRes);
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthMobileMovemntPVWFToGL", "SUCCESS", "Authorize mobile movement payments"));
                        break;
                    } else {
                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthMobileMovemntPVWFToGL", "Failed", "Cannot authorize payment while the role is not selected for posting"));

                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

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

    @RequestMapping("/fireMobileMovementReport")
    public String fireMobileMovementReport(HttpSession httpSession, Model model){
        model.addAttribute("pageTitle","MOBILE CASH MOVEMENT TRANSACTION REPORT");
        model.addAttribute("controlAccounts", transactionRepo.getCashMovementAccts());
        model.addAttribute("fromDate", DateUtil.previosDay(5));
        model.addAttribute("toDate", DateUtil.tomorrow());
        return "modules/mobileMovement/mobileCashMovementTxnsReport";
    }


    @RequestMapping(value = "/fireMobileMovementReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireMobileMovementReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        GeneralJsonResponse jsonResponse = new GeneralJsonResponse();
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        String txnStatus = customeQuery.get("txnStatus");
        String sourceAcctDestAcct = customeQuery.get("sourceAcctDestAcct");
        String sourceAcct = sourceAcctDestAcct.split("==")[0];
        String destAcct = sourceAcctDestAcct.split("==")[1];
        String code = sourceAcctDestAcct.split("==")[2];
//        LOGGER.info("source and destination acct are... {}, and ....{} respectively", sourceAcct,destAcct);
        List<Map<String,Object>> response = transactionRepo.fireMobileMovementReportAjax(txnStatus,sourceAcct,destAcct,code, fromDate, toDate);
        LOGGER.info("The final response for cash movement search is: {}", response);
        jsonResponse.setStatus("SUCCESS");
        jsonResponse.setResult(response);

        return jsonResponse;
    }

    @RequestMapping(value = "/firePreviewCMSupportingDocument", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> previewSupportingDocument(@RequestParam Map<String, String> customeQuery) throws IOException {
        byte[] imageContent = transactionRepo.getCMSupportingDocument(customeQuery.get("reference"), customeQuery.get("id"));//get image from DAO based on id
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<byte[]>(imageContent, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/fireDiscardRemittanceTransaction", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String discardRemittanceTransaction(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        return transactionRepo.discardRemittanceTransaction(customeQuery.get("reference"));
    }

    @RequestMapping("/fireMuseStatements")
    public String fireMuseStatement(HttpSession httpSession, Model model){
        model.addAttribute("pageTitle","MUSE STATEMENTS");
        model.addAttribute("fromDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("toDate", DateUtil.now("yyyy-MM-dd"));
        LOGGER.info("checking for muse partners ... {}", transactionRepo.getMusePartners());
        model.addAttribute("partners", transactionRepo.getMusePartners());
        return "modules/muse/museStatement";
    }

    @PostMapping(value = "/fireMuseStatementsAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireMuseStatementsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        GeneralJsonResponse gr = new GeneralJsonResponse();
        gr.setStatus("0");
        gr.setResult(transactionRepo.getMuseStatementsCBSAjax(customeQuery.get("accountNo"),customeQuery.get("fromDate"),customeQuery.get("toDate")));
        return gr;
    }

    @PostMapping(value = "/firePushMuseStatementsAjax")
    @ResponseBody
    public String firePushMuseStatementsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        Random rand = new Random();
        String tableId = String.format("%04d", rand.nextInt(10000));

        String url = systemVariable.MUSE_STATEMENT_URL+"?acctNo="+customeQuery.get("accountNo")+"&startDate="+customeQuery.get("fromDate")+"&endDate="+customeQuery.get("toDate")+"&sendReq=Y&tableId="+tableId+"&apiType=MUSE";

        String payload = "";
        LOGGER.info("payload... {}", url);
        String response = HttpClientService.sendXMLRequest(url, url);
        LOGGER.info("Muse response .... {}", response);
     return response;
    }

    @RequestMapping(value = "/vikobaFundMovement")
    public String vikobaFundMovement(HttpServletRequest request, HttpSession session, Model model) {
        //Audit trails
        String roleId = session.getAttribute("roleId") + "";
        model.addAttribute("pageTitle","VIKOBA FUND MOVEMENT");
        model.addAttribute("roleId",roleId);
        return "modules/mobileMovement/vikobaCashMovement";
    }

    @RequestMapping(value = "/fireInitiateVikobaMovement")
    public String initiateVikobaMovement(HttpServletRequest request, HttpSession session, Model model) {
        Calendar cal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        cal.add(Calendar.DATE, 0);
        String transDate = dateFormat.format(cal.getTime());
        model.addAttribute("pageTitle", "INITIATE VIKOBA FUND MOVEMENT");
//        model.addAttribute("controlAccounts", transactionRepo.getCashMovementAccts());
//        model.addAttribute("defaultNarr","TRANSFER FROM COLLECTION TO DISBURSEMENT ACCOUNT  [ Enter transaction receipt here... ]  " + transDate);
        return "modules/mobileMovement/initiateVikobaMovement";
    }

    @PostMapping(value = "/fireSubmitInitiateVikobaFundMovement")
    @ResponseBody
    public JsonResponse fireSubmitInitiatedVikobaFundMovement(HttpSession httpsession, @Valid VikobaFundMovement customeQuery, BindingResult bindingResult) throws IOException {

        JsonResponse response = new JsonResponse();
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

            String updateControl = customeQuery.getUpdateControl();
            String mno = customeQuery.getMno();
            String amount = customeQuery.getAmount();
            String mnoReceipt = customeQuery.getMnoReceipt();

//            if (!file.isEmpty()) {
//                transactionRepo.saveSupportingDoc(transactionID, file);
//            }
            String status = "I";
            String cbsStatus = "P";
            String initiatedBy = httpsession.getAttribute("username").toString();
            AuditTrails.setComments("Initiate vikoba fund movement");
            AuditTrails.setFunctionName("/fireSubmitInitiateVikobaFundMovement");

            if (1 == transactionRepo.saveInitiateVikobaFundmovement(mnoReceipt,mno,amount, status,cbsStatus, initiatedBy,updateControl)) {
                response.setStatus("SUCCESS");
                response.setResult("Transaction successfully initiated please check on TRANSACTIONS WF");
            } else {
                response.setStatus("ERROR");
                response.setResult("failed to submit data into the database");
            }
        }
        return response;
    }


    @RequestMapping(value = "/fireVikobaMovementWF")
    public String fireVikobaMovementWF( HttpSession session, Model model) {

        model.addAttribute("pageTitle", "INITIATED VIKOBA MOVEMENT");
        return "modules/mobileMovement/vikobaFundMovementTxnWF";
    }

    @PostMapping(value = "/fireVikobaFundMovementWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireVikobaFundMovementWFAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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

        return transactionRepo.fireVikobaFundMovementWFAjax(roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/firePreviewVikobaFundMovementTxn")
    public String firePreviewVikobaFundMovementTxn(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        String reference=customeQuery.get("reference");
        model.addAttribute("pageTitle", "AUTHORIZATION OF VIKOBA FUND MOVEMENT TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("reference", reference);
//        model.addAttribute("supportingDocs", transactionRepo.getCMSupportingDocument(reference) );
        model.addAttribute("transactionDetails", transactionRepo.cashMovementRequestTxnObj(reference));
        return "modules/mobileMovement/modal/previewVikobaTxn";
    }

    /*
     *BRANCH AUTHORIZE TIPS TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/fireAuthVikobaFundMovemnt", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthVikobaFundMovemnt(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String txid = customeQuery.get("reference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {

                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";
                        String reference = customeQuery.get("reference");
                        VikobaUpdateRequest updateRequest = transactionRepo.getVikobaUpdateRequest(reference);

                        result = transactionRepo.fireAuthVikobaTxn(updateRequest, role, username,branchNo);

                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthMobileMovemntToPV", "SUCCESS", "Authorize mobile movement payments"));
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


    @RequestMapping(value = "/fireGetVikobaBalancePerMno")
    public String fireGetVikobaBalancePerMno( HttpSession session, Model model) {

        model.addAttribute("pageTitle", "VIKOBA FUND MOVEMENT BALANCES");
        model.addAttribute("balanceDate", DateUtil.previosDay(1));
        return "modules/mobileMovement/vikobaFundMovementBalances";
    }

    @RequestMapping(value = "/fireGetVikobaBalancePerMnoAjax",method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireGetVikobaBalancePerMnoAjax(@RequestParam Map<String, String> customeQuery){
        return transactionRepo.getVikobaBalancePerMnoAjax(customeQuery);
    }


    @RequestMapping(value = "/fireVikobaTxnRetries")
    public String fireVikobaTxnRetries( HttpSession session, Model model) {
        model.addAttribute("pageTitle", "VIKOBA TRANSACTION RETRIES");
        return "modules/mobileMovement/vikobaTxnRetries";
    }

    @RequestMapping(value = "/fireVikobaFundMovementReport")
    public String fireVikobaFundMovementReport( HttpSession session, Model model) {
        model.addAttribute("pageTitle", "VIKOBA FUND MOVEMENT REPORTS");
        model.addAttribute("fromDate", DateUtil.previosDay(5));
        model.addAttribute("toDate", DateUtil.tomorrow());
        return "modules/mobileMovement/vikobaFundMovementReport";
    }

    @PostMapping(value = "/fireRetryVikobaTxnAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireRetryVikobaTxnAjax( HttpSession session, @RequestParam Map<String, String> customeQuery) {
        String  response= transactionRepo.CheckTxnBasedOnMnoAndRetry(customeQuery.get("mno"),customeQuery.get("reference"),customeQuery.get("ttype"));
        return response;
    }


    @RequestMapping("/fireMultipleWalletTransactions")
    public String fireMultipleWalletTransactions(HttpSession httpSession, Model model){
        model.addAttribute("pageTitle","MULTIPLE WALLET STATEMENTS");
        model.addAttribute("fromDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("toDate", DateUtil.now("yyyy-MM-dd"));
        return "modules/multiplewallet/filterForm";
    }

    @PostMapping(value = "/fireMultipleWalletTransactionsAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireMultipleWalletTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        GeneralJsonResponse gr = new GeneralJsonResponse();
        gr.setStatus("0");
        gr.setResult(transactionRepo.getMultipleWalletTransactionsAjax(customeQuery.get("fromDate"),customeQuery.get("toDate")));
        return gr;
    }


    @RequestMapping("/fireUploadReconFiles")
    public String fireUploadReconFiles(HttpSession httpSession, Model model){
        model.addAttribute("pageTitle","UPLOAD RECON FILES");
        model.addAttribute("fromDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("toDate", DateUtil.now("yyyy-MM-dd"));
        return "modules/reconfiles/uploadForm";
    }

    @PostMapping(value = "/fireUploadReconFilesAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireUploadReconFilesAjax(@RequestParam(value = "file", required = true) MultipartFile file,@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        GeneralJsonResponse gr = new GeneralJsonResponse();
        List<ThirdPartyTxn> trns = new ArrayList<>();
        LOGGER.info("Uploaded File:{}, type:{}",file.getOriginalFilename(),file.getContentType());
        if (file == null || file.isEmpty()) {
            ThirdPartyTxn trn = new ThirdPartyTxn();
            trn.setTxnid("Uploaded file is empty.");
            trns.add(trn);
            //throw new IllegalArgumentException("Uploaded file is empty.");
        }
        try{
            trns=   fileReaderService.processFile(customeQuery.get("type"),file);

            }
            catch(Exception e){
                LOGGER.error(e.getMessage(),e);
            }
        gr.setStatus("0");
        gr.setResult(trns);
        return gr;
    }
}
