/*
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.DTO.CashMovementRequestObj;
import com.DTO.EMkopoReq;
import com.DTO.IBANK.PaymentReq;
import com.DTO.swift.RTGSAdviceMessage;
import com.DTO.swift.SwiftMessageObject;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.APIResponse;
import com.helper.DateUtil;
import com.helper.MaiString;
import com.models.Transfers;
import com.online.core.request.SupportDoc;
import com.repository.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperPrint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.client.RestTemplate;

/**
 * @author melleji.mollel
 */
@Service
public class TransferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferService.class);
    @Autowired
    SYSENV systemVariables;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    RtgsRepo rtgsRepo;

    @Autowired
    private MailService mailInit;

    @Autowired
    XapiWebService xapiWebService;
    @Autowired
    JasperService jasperService;

    @Autowired
    SYSENV SYSENV;
    @Autowired
    ReportRepo reconRepo;

    @Autowired
    TransfersRepository transfersRepository;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    WebserviceRepo webserviceRepo;

    @Autowired
    SwiftRepository swiftRepository;

    public String runUpdateSwiftInfo() {
        LOGGER.debug("running: runUpdateSwiftInfo");

        String response = "-1";
        String reference = "-1";
        String amount = "-1";
        try {
            List<Map<String, Object>> trns = rtgsRepo.getPendingSwiftTransaction();
            for (Map<String, Object> trn : trns) {
                reference = trn.get("reference") + "";
                amount = trn.get("amount") + "";
                LOGGER.debug("reference: " + reference);
                LOGGER.debug("amount: " + amount);
                response = HttpClientService.sendTxnToAPINoLogs("", "https://kprinter.tcbbank.co.tz/brg/swift/xwsSpoolRTGSInfo/" + reference + "/" + amount);
                RTGSAdviceMessage rtgs = objectMapper.readValue(response, RTGSAdviceMessage.class);
                if (rtgs.getValue_Amount() != null) {
                    int update_result = rtgsRepo.updateSwiftInfo(reference, amount, rtgs.getFileCRC());
                    LOGGER.debug("reference: {}, update_result:{}", reference, update_result);

                }
            }
        } catch (JsonProcessingException ex) {
            LOGGER.debug("runUpdateSwiftInfo AN ERROR OCCURRED: PAYLOAD {}\nERROR:{}", response, ex.getMessage());
        }
        return response;

    }

    public String sendFileSwift() {
        LOGGER.info("running: sendFileSwift");

        String response = "-1";
        String reference = "-1";
        String body = "-1";

        List<Map<String, Object>> trns = rtgsRepo.getSwiftQueueTransaction();
        for (Map<String, Object> trn : trns) {
            reference = trn.get("reference") + "";
            body = trn.get("body") + "";
            String url = trn.get("url") + "";
            String id = trn.get("id") + "";
            LOGGER.info("queue trn: {}", trn);
            response = HttpClientService.sendKprinterRequest(body, url);
//test ulongo wa wadau
            if (!response.equals("-1")) {
                String arrayStr[] = response.split("\\|");

                if (arrayStr.length >= 2) {
                    if ("OK".equals(arrayStr[0]) || "DUPLICATE".equals(arrayStr[0])) {
                        int update_result = rtgsRepo.deleteSwiftQueueFile(id);
                        LOGGER.info("reference: {}, delete_result:{} table id:{}", reference, update_result, id);
                    }
                } else {
                    LOGGER.info("failed on kprinter and not deleted record reference: {}, table id:{}", reference, id);
                }
            }
        }

//        responseBody = "OK|Success - File generated|" + Utils.now() + "|" + swiftFile + "|logged:" + insertResult;


        return response;

    }

    public String runTriggerAlert() throws ParseException {
        LOGGER.info("running: runTriggerAlert");

        String response = "-1";
        String reference = "-1";
        String dueTime = "3";
        List<Map<String, Object>> trns = rtgsRepo.getDuePendingSwiftTransactionBystatus("P", "C", dueTime);
        if (!trns.isEmpty()) {
            response = sendAlert(trns, "Awaiting IBD approval on Cilantro, please log into the Cilatro to release below transactions");
        }
        trns = rtgsRepo.getDuePendingSwiftTransactionBystatus("C", "C", dueTime);
        if (!trns.isEmpty()) {
            response = sendAlert(trns, "Awaiting IBD approval on SWIFT system, please check due time for your action");
        }
        return response;

    }

    public String sendAlert(List<Map<String, Object>> trns, String msgBody) throws ParseException {
        String resp = "";
        try {
            Map<String, Object> form = new HashMap<>();
            form.put("mailSubject", "SWIFT Outgoing delay - Alert");
            form.put("mailFrom", "e-reports@tcbbank.co.tz");
            form.put("mailTo", "ibd@tcbbank.co.tz");
            form.put("mailCC", "bs@tcbbank.co.tz,operations@tcbbank.co.tz");
            form.put("mailBCC", null);

            LOGGER.info("mailSubject", form.get("mailSubject"));
            LOGGER.info("mailFrom: {}", form.get("mailFrom"));
            LOGGER.info("mailTo: {}", form.get("mailTo"));
            LOGGER.info("Mail Request: {}", form);
            //prepare attachements
// message_type, txn_type, sourceAcct, destinationAcct, amount, currency, beneficiaryName,beneficiaryBIC,  sender_name, reference, txid,  code, purpose,  create_dt,hq_approved_dt, branch_no

            String td = "";
            for (Map<String, Object> trn : trns) {
                String txn_type;
                if (trn.get("txn_type").equals("001")) {
                    txn_type = "TISS";
                } else if (trn.get("txn_type").equals("004")) {
                    txn_type = "TT";
                } else {
                    txn_type = trn.get("txn_type") + "";
                }
                String txid;
                if (trn.get("txid") == null) {
                    txid = trn.get("reference") + "";
                } else {
                    txid = trn.get("txid") + "";
                }
                String branch_approved_dt = trn.get("branch_approved_dt") + "";
                if (branch_approved_dt == null) {
                    branch_approved_dt = trn.get("create_dt") + "";
                }
                td += "  <tr>\n"
                        + "    <td>" + trn.get("reference") + "</td>\n"
                        + "    <td>" + txid + "</td>\n"
                        + "    <td>" + trn.get("branch_no") + "</td>\n"
                        + "    <td>" + txn_type + "</td>\n"
                        + "    <td>" + trn.get("sourceAcct") + " (" + trn.get("sender_name") + ")</td>\n"
                        + "    <td>" + trn.get("beneficiaryBIC") + "</td>\n"
                        + "    <td>" + MaiString.formatCurrency(new BigDecimal(trn.get("amount") + "")) + "</td>\n"
                        + "    <td>" + trn.get("purpose") + "</td>\n"
                        + "    <td>" + trn.get("create_dt") + "</td>\n"
                        + "    <td>" + branch_approved_dt + "</td>\n"
                        + "    <td>" + DateUtil.getDifferenceBtwTime(DateUtil.strToDate(trn.get("create_dt") + "", "yyyy-MM-dd HH:mm:ss"), DateUtil.strToDate(branch_approved_dt, "yyyy-MM-dd HH:mm:ss")) + "</td>\n"
                        + "    <td>" + trn.get("hq_approved_dt") + "</td>\n"
                        + "    <td>" + DateUtil.getDifferenceBtwTime(DateUtil.strToDate(trn.get("hq_approved_dt") + "", "yyyy-MM-dd HH:mm:ss")) + "</td>\n"
                        + "  </tr>\n";
            }

            List<Map<String, Object>> attachments = new ArrayList<>();
            String htmlBody = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "    <head>\n"
                    + "        <title>SWIFT Outgoing delay - Alert</title>"
                    + "<style>\n"
                    + "table, th, td {\n"
                    + "  border: 1px solid black;\n"
                    + "  border-collapse: collapse;\n"
                    + "}"
                    + "th, td {\n"
                    + "  padding: 10px;\n"
                    + "}"
                    + "#customers {\n"
                    + "  font-family: Arial, Helvetica, sans-serif;\n"
                    + "  border-collapse: collapse;\n"
                    + "  width: 100%;\n"
                    + "}\n"
                    + "\n"
                    + "#customers td, #customers th {\n"
                    + "  border: 1px solid #006ea4;"
                    + "font-size: 11px;\n"
                    + "  padding: 8px;\n"
                    + "}\n"
                    + "\n"
                    + "#customers tr:nth-child(even){background-color: #f2f2f2;}\n"
                    + "\n"
                    + "#customers tr:hover {background-color: #ddd;}\n"
                    + "\n"
                    + "#customers th {\n"
                    + "  padding-top: 12px;\n"
                    + "  padding-bottom: 12px;\n"
                    + "  text-align: left;\n"
                    + "  background-color: #2A9D47;\n"
                    + "  color: white;\n"
                    + "}\n"
                    + "</style>"
                    + "    </head>\n"
                    + "    <body class=\"vertical-layout\">\n"
                    + "        <div>\n"
                    + "           <p><h3 style='color:red'>Alert!</h3><p>"
                    + "           <p>" + msgBody + "<p>"
                    + "    <table id='customers'>\n"
                    + "  <tr>\n"
                    + "    <th>REFERENCE</th>\n"
                    + "    <th>TXNID</th>\n"
                    + "    <th>BRANCH</th>\n"
                    + "    <th>TYPE</th>\n"
                    + "    <th>SENDER ACCT</th>\n"
                    + "    <th>RECEIVER BIC</th>\n"
                    + "    <th>AMOUNT</th>\n"
                    + "    <th>DETAILS</th>\n"
                    + "    <th>LOG DATE</th>\n"
                    + "    <th>BRANCH APPROVED AT</th>\n"
                    + "    <th>DELAY AT BRANCH</th>\n"
                    + "    <th>IBD APPROVED AT</th>\n"
                    + "    <th>DELAY AT IBD</th>\n"
                    + "  </tr>\n"
                    + td
                    + "</table>"
                    + "</div>\n"
                    + "    </body>\n"
                    + "</html>";
            LOGGER.debug("Mail Body: {}", htmlBody);
            resp = htmlBody;
            mailInit.sendHtmlEmail(htmlBody, form, attachments);
            LOGGER.info("Mail successfully sent.");
        } catch (Exception ex) {
            LOGGER.error("sendeAlert:MessagingException", ex);
        }
        return resp;
    }

    public String sendEmailAlertToPV(CashMovementRequestObj trns) throws ParseException {
        String resp = "";
        try {
            Map<String, Object> form = new HashMap<>();
            String mailSubject = trns.getCode() + " FUND MOVEMENT - Alert";
            form.put("mailSubject",mailSubject);
            form.put("mailFrom", "e-reports@tcbbank.co.tz");
            form.put("mailTo", "validation@tcbbank.co.tz,dormant@tcbbank.co.tz");
            form.put("mailCC", "bs@tcbbank.co.tz,ebanking@tcbbank.co.tz");
            form.put("mailBCC", null);

            LOGGER.info("mailSubject", form.get("mailSubject"));
            LOGGER.info("mailFrom: {}", form.get("mailFrom"));
            LOGGER.info("mailTo: {}", form.get("mailTo"));
            LOGGER.info("Mail Request: {}", form);
            //prepare attachements

            String msgBody = "";
            String txn_type ="cash movement";

            msgBody += "  <tr>\n"
                    + "    <td>CODE:</td>\n"
                    + "    <td>" + trns.getCode() + "</td>\n"
                    + "  </tr>\n";

            msgBody += "  <tr>\n"
                    + "    <td>REFERENCE:</td>\n"
                    + "    <td>" + trns.getReference() + "</td>\n"
                    + "  </tr>\n";

            msgBody += "  <tr>\n"
                    + "    <td>AMOUNT:</td>\n"
                    + "    <td>" +MaiString.formatCurrency(trns.getAmount()) + "</td>\n"
                    + "  </tr>\n";
            msgBody += "  <tr>\n"
                    + "    <td>DEBIT ACCOUT:</td>\n"
                    + "    <td>" + trns.getSourceAcc() +" ( "+ trns.getSourceAccName() +" )"+  "</td>\n"
                    + "  </tr>\n";

            msgBody += "  <tr>\n"
                    + "    <td>CREDIT ACCOUT:</td>\n"
                    + "    <td>" + trns.getDestinationAcc() +" ( "+ trns.getDestinationAccName() +" )"+  "</td>\n"
                    + "  </tr>\n";
            msgBody += "  <tr>\n"
                    + "    <td>NARRATION:</td>\n"
                    + "    <td>" + trns.getComments() + "</td>\n"
                    + "  </tr>\n";
            msgBody += "  <tr>\n"
                    + "    <td>INITIATED BY:</td>\n"
                    + "    <td>" + trns.getIniatedBy() + "</td>\n"
                    + "  </tr>\n";
            msgBody += "  <tr>\n"

                    + "    <td>INITIATED DATE:</td>\n"
                    + "    <td>" + trns.getIniatedDate() + "</td>\n"
                    + "  </tr>\n";

            msgBody += "  <tr>\n"
                    + "    <td>APPROVED BY:</td>\n"
                    + "    <td>" + trns.getApprovedBy() + "</td>\n"
                    + "  </tr>\n";
            msgBody += "  <tr>\n"
                    + "    <td>APPROVED DATE:</td>\n"
                    + "    <td>" + trns.getApprovedDate() + "</td>\n"
                    + "  </tr>\n";

            List<Map<String, Object>> attachments = new ArrayList<>();
            String htmlBody = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "    <head>\n"
                    + "        <title>" +trns.getCode() + " FUND MOVEMENT - Alert</title>"
                    + "<style>\n"
                    + "table, th, td {\n"
                    + "  border: 1px solid black;\n"
                    + "  border-collapse: collapse;\n"
                    + "}"
                    + "th, td {\n"
                    + "  padding: 10px;\n"
                    + "}"
                    + "#customers {\n"
                    + "  font-family: Arial, Helvetica, sans-serif;\n"
                    + "  border-collapse: collapse;\n"
                    + "  width: 100%;\n"
                    + "}\n"
                    + "\n"
                    + "#customers td, #customers th {\n"
                    + "  border: 1px solid #006ea4;"
                    + "font-size: 11px;\n"
                    + "  padding: 8px;\n"
                    + "}\n"
                    + "\n"
                    + "#customers tr:nth-child(even){background-color: #f2f2f2;}\n"
                    + "\n"
                    + "#customers tr:hover {background-color: #ddd;}\n"
                    + "\n"
                    + "#customers th {\n"
                    + "  padding-top: 12px;\n"
                    + "  padding-bottom: 12px;\n"
                    + "  text-align: left;\n"
                    + "  background-color: #2A9D47;\n"
                    + "  color: white;\n"
                    + "}\n"
                    + "</style>"
                    + "    </head>\n"
                    + "    <body class=\"vertical-layout\">\n"
                    + "        <div>\n"
                    + "           <p><h3 style='color:red'>Alert!</h3><p>"
                    + "           <p>DEAR TEAM, THERE IS PENDING TRANSACTION THAT NEED'S YOUR APPROVAL<p>"
                    + "           <p>NAVIGATE TO: Payments > Mobile Cash Movement > PENDING AT POSTING & VALIDATION.<p>"
                    + "    <table id='customers'>\n"
                    + "  <tr>\n"
                    + "    <th>ITEM</th>\n"
                    + "    <th>DETAILS</th>\n"
                    + "  </tr>\n<tbody>"+  msgBody
                    + "</tbody></table>"
                    + "</div>\n"
                    + "    </body>\n"
                    + "</html>";
            LOGGER.debug("Mail Body: {}", htmlBody);
            resp = htmlBody;
            mailInit.sendHtmlEmail(htmlBody, form, attachments);
            LOGGER.info("Mail for reference... {} is sent successfully., Date ... {}",trns.getReference(), DateUtil.now());
        } catch (Exception ex) {
            LOGGER.error("sendeAlert:MessagingException", ex);
        }
        return resp;
    }

    public String runDownloadIBFiles() {

        String response = "-1";
        String reference = "-1";
        String amount = "-1";
        String lastId = rtgsRepo.getLastPT("IB_FILE");
        LOGGER.info("running: runDownloadIBFiles: lastId={}", lastId);

        List<Map<String, Object>> trns = rtgsRepo.getSwiftTransactionByChannel(lastId);
        for (Map<String, Object> trn : trns) {
            List<SupportDoc> onlines = rtgsRepo.getGetFilesFromOnline(trn.get("txid") + "");
            if (!onlines.isEmpty()) {
                for (SupportDoc online : onlines) {
                    online.setTxnId(trn.get("reference") + "");
                    int insert = rtgsRepo.SaveFilesOnline(online);
                    LOGGER.info("running: runDownloadIBFiles:txid->{}[{}], insertResult:->{}", trn.get("reference"), trn.get("txid"), insert);
                }
            }
            rtgsRepo.updateLastPT("IB_FILE", trn.get("id") + "");
        }

        return response;

    }

    public void procChargForCustomerServic(Map<String, String> form, String exporterFileType, HttpServletResponse response) throws IOException, ParseException {
        String ttype = form.get("ttype") + "";
        String reference = form.get("reference") + "";
        String debitAccount = form.get("account") + "";
        String branchCode = form.get("branchCode") + "";
        String username = form.get("username") + "";
        String currency = "TZS";
        BigDecimal charge = new BigDecimal("0.0");
        BigDecimal price = new BigDecimal(SYSENV.CUSTOMER_SERVICE_CHARGE_BALANCE_ENQUIRY);
        BigDecimal bPage = new BigDecimal("0.0");
        String incomeLegder = SYSENV.CUSTOMER_SERVICE_CHARGE_INCOME_LEDGER.replace("***", branchCode);
        String narration = reference + " " + ttype + " customer service Charge";
        JasperPrint print = null;
        Integer page = 0;
        int result = -1;
        String destName = null;
        switch (ttype) {
            case "X1":
                print = jasperService.printCustomerinformationBalance(form);
                page = print.getPages().size();
                //Print after debiting an account
                price = new BigDecimal(SYSENV.CUSTOMER_SERVICE_CHARGE_BALANCE_ENQUIRY);
                bPage = new BigDecimal(page.toString());
                charge = price.multiply(bPage);
                incomeLegder = SYSENV.CUSTOMER_SERVICE_CHARGE_INCOME_LEDGER.replace("***", branchCode);
                narration = reference + " " + ttype + " customer service Charge";
                result = reconRepo.insertChargeTransaction(reference, debitAccount, currency, charge, page, "EST", price, username, incomeLegder, branchCode);
                LOGGER.info("{} insert result: {}", reference, result);
                if (result == 1) {
                    int xapiResult = -1;
                    if(!SYSENV.ACTIVE_PROFILE.equals("prod")){
                        xapiResult = xapiWebService.postCharge(debitAccount,reference,incomeLegder,new BigDecimal("0.0"),charge,narration,currency,SYSENV.CUSTOMER_SERVICE_CHARGE_SCHEME,SYSENV.CUSTOMER_SERVICE_CHARGE_CODE,branchCode);
                    }
                    LOGGER.info("Txid:{} xapiResult:  {}", reference, xapiResult);
                    String responseMsg = "success";
                    if (xapiResult == 51) {
                        responseMsg = "Insufficient funds";
                    } else if (xapiResult == 26) {
                        responseMsg = "Duplicate";
                    } else {
                        responseMsg = "General failure";
                    }
                    result = reconRepo.updateChargeTransaction(reference, "C", "C", String.valueOf(xapiResult), responseMsg);
                    LOGGER.info("{} update result {}", reference, result);
                    if (xapiResult == 0) {
                        destName = "statement_1_" + DateUtil.now("yyyyMMddHHmmss");
                        jasperService.exportFileOption(print, exporterFileType, response, destName);
                    }
                }
                //when failed to post
                break;
            case "X2":
                print = jasperService.printCustomerinformationStatement(form);
                page = print.getPages().size();
                //Print after debiting an account

                charge = new BigDecimal("0.0");
                price = new BigDecimal(SYSENV.CUSTOMER_SERVICE_CHARGE_STATEMENT_PER_PAGE);
                bPage = new BigDecimal(page.toString());
                charge = price.multiply(bPage);
                incomeLegder = SYSENV.CUSTOMER_SERVICE_CHARGE_INCOME_LEDGER.replace("***", branchCode);
                narration = reference + " " + ttype + " customer service Charge";
                result = reconRepo.insertChargeTransaction(reference, debitAccount, currency, charge, page, "EST", price, username, incomeLegder, branchCode);
                LOGGER.info("{} insert result: {}", reference, result);
                if (result == 1) {
                    int xapiResult = -1;
                    if(!SYSENV.ACTIVE_PROFILE.equals("prod")){
                        xapiResult = xapiWebService.postCharge(debitAccount,reference,incomeLegder,new BigDecimal("0.0"),charge,narration,currency,SYSENV.CUSTOMER_SERVICE_CHARGE_SCHEME,SYSENV.CUSTOMER_SERVICE_CHARGE_CODE,branchCode);
                    }
                    LOGGER.info("Txid:{} xapiResult  {}", reference, xapiResult);
                    String responseMsg = "success";
                    if (xapiResult == 51) {
                        responseMsg = "Insufficient funds";
                    } else if (xapiResult == 26) {
                        responseMsg = "Duplicate";
                    } else {
                        responseMsg = "General failure";
                    }
                    result = reconRepo.updateChargeTransaction(reference, "C", "C", String.valueOf(xapiResult), responseMsg);
                    LOGGER.info("{} update result {}", reference, result);
                    if (xapiResult == 0) {
                        destName = "statement_1_" + DateUtil.now("yyyyMMddHHmmss");
                        jasperService.exportFileOption(print, exporterFileType, response, destName);
                    }
                }
                break;
        }
    }

    public int procChargeForVisaCard( String reference, String AccountNo,String branchCode, String currency) {

        String debitAccount = AccountNo;
        BigDecimal charge;
        String incomeLegder;
        String narration = reference + " VISA Card Charge";
        charge = new BigDecimal(SYSENV.VISA_CARD_REQUEST_CHARGE);
        incomeLegder = SYSENV.VISA_CARD_REQUEST_CHARGE_INCOME_LEDGER.replace("***", branchCode);
        String branchId = this.jdbcRUBIKONTemplate.queryForObject("SELECT BU_ID FROM BUSINESS_UNIT bu WHERE bu.BU_CD =?",  new Object[]{branchCode}, String.class);
        int xapiResult = -1;
        // if (!SYSENV.ACTIVE_PROFILE.equals("prod")) {
        xapiResult = xapiWebService.postCharge(debitAccount, reference, incomeLegder, new BigDecimal("0.0"), charge, narration, currency, SYSENV.VISA_CARD_REQUEST_CHARGE_SCHEME, SYSENV.VISA_CARD_REQUEST_CHARGE_CODE,branchId);
        // }
        return xapiResult;

    }

    public ResponseEntity<APIResponse<Transfers>> saveEmkopoData(EMkopoReq eMkopoReq){
        boolean existMkopo = transfersRepository.existsByTxid(eMkopoReq.getReference());
        System.out.print("========submitted reference" + existMkopo);
        if(existMkopo){
            APIResponse<Transfers> failedResponse = new APIResponse<>(
                    false,
                    "Transaction with Reference Number " + eMkopoReq.getReference() + " already exists",
                    null
            );
            return new ResponseEntity<>(failedResponse, HttpStatus.CONFLICT);

        }

        try{
            String randomRef = generateRandomRf();
            LocalDateTime localDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = localDateTime.format(formatter);

            Transfers transfer = new Transfers();
            transfer.setTxid(eMkopoReq.getReference());
            transfer.setTxn_type("001");
            transfer.setSourceAcct(eMkopoReq.getSourceAccount());
            transfer.setDestinationAcct(eMkopoReq.getDestinationAccount());
            transfer.setAmount(eMkopoReq.getAmount());
            transfer.setCurrency(eMkopoReq.getCurrency());
            transfer.setBeneficiaryName(eMkopoReq.getBeneficiaryName());
            transfer.setBeneficiaryBIC(eMkopoReq.getBeneficiaryBIC());
            transfer.setBeneficiary_contact(eMkopoReq.getBeneficiaryContact());
            transfer.setSenderBIC(eMkopoReq.getSenderBIC());
            transfer.setSender_phone(eMkopoReq.getSenderPhone());
            transfer.setSenderAddress(eMkopoReq.getSenderAddress());
            transfer.setSenderName(eMkopoReq.getSenderName());
            transfer.setPurpose(eMkopoReq.getPurpose() + " ref " + eMkopoReq.getReference());
            transfer.setInitiatedBy(eMkopoReq.getInitiatorId());
            transfer.setReference("EMKOPO-" + eMkopoReq.getReference());
            transfer.setStatus("I");
            transfer.setCbsStatus("P");
            transfer.setMessage("E-Mkopo Received");
            transfer.setComments("E-Mkopo Submittion");
            transfer.setCallbackurl(eMkopoReq.getCallBackUrl());
            transfer.setCode("E-MKOPO");
            transfer.setMessage_type("103");
            transfer.setCallbackurl(eMkopoReq.getCallBackUrl());

            PaymentReq paymentReq = new PaymentReq();
            paymentReq.setReference(eMkopoReq.getReference());
            paymentReq.setType(transfer.getTxn_type());
            paymentReq.setSenderAccount(eMkopoReq.getSenderBIC());
            paymentReq.setSenderName(eMkopoReq.getSenderName());
            paymentReq.setAmount(new BigDecimal(eMkopoReq.getAmount()));
            paymentReq.setCurrency(eMkopoReq.getCurrency());
            paymentReq.setBeneficiaryAccount(eMkopoReq.getBeneficiaryBIC());
            paymentReq.setBeneficiaryName(eMkopoReq.getBeneficiaryName());
            paymentReq.setBeneficiaryBIC(eMkopoReq.getBeneficiaryBIC());
            paymentReq.setSenderAddress(eMkopoReq.getSenderAddress());
            paymentReq.setBeneficiaryContact(eMkopoReq.getBeneficiaryContact());
            paymentReq.setSenderPhone("phoneMkopo");
            paymentReq.setDescription(eMkopoReq.getPurpose());
            paymentReq.setIntermediaryBank(eMkopoReq.getSenderBIC());
            paymentReq.setSpecialRateToken(eMkopoReq.getReference());
            paymentReq.setInitiatorId(eMkopoReq.getInitiatorId());
            paymentReq.setCustomerBranch(eMkopoReq.getBranchCode());
            paymentReq.setBatchReference(randomRef);
            paymentReq.setCorrespondentBic(eMkopoReq.getBeneficiaryBIC());
            paymentReq.setCallbackUrl(eMkopoReq.getCallBackUrl());
            paymentReq.setCreateDt(String.valueOf(formattedDate));

            try{
//                notifySenderCallBack(transfer);
                System.out.println("=========here test txn--->");
                webserviceRepo.transferPaymentEMKOPOSwiftMessage(paymentReq, transfer.getTxid());
            }catch (Exception callbackError){
                APIResponse<Transfers> errorCallBackResponse = new APIResponse<>(
                        false,
                        "callback failed: " + callbackError.getMessage(),
                        null
                );

                return new ResponseEntity<>(errorCallBackResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            APIResponse<Transfers> successResponse = new APIResponse<>(
                    true,
                    "Success",
                    transfer
            );
            return new ResponseEntity<>(successResponse, HttpStatus.OK);


        } catch (Exception e) {
            APIResponse<Transfers> errorResponse = new APIResponse<>(
                    false,
                    "Error while saving E-Mkopo: " + e.getMessage(),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public void notifySenderCallBack(Transfers transfer){
        try{
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> callBackData = new HashMap<>();
            callBackData.put("responseCode", 0);
            callBackData.put("reference", transfer.getTxid());
            callBackData.put("status", "SUCCESS");
            callBackData.put("swiftMessage", null);
            callBackData.put("message", "Transaction Received SuccessFully");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(callBackData, headers);

            ResponseEntity<String> response =  restTemplate.postForEntity(transfer.getCallbackurl(), request, String.class);

            if(!response.getStatusCode().is2xxSuccessful()){
                throw new Exception(response.getBody());
            }

        } catch (Exception e) {
            System.out.println("Error in notifySenderCallBack=======>" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String generateRandomRf() {
        return "EMKOPO" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
}
