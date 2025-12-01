/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.EFT.EftBulkPaymentReq;
import com.DTO.EFT.EftBulkPaymentForm;
import com.DTO.EFT.EftBulkPaymentResp;
import com.DTO.EFT.EftResponseMessage;
import com.DTO.GeneralJsonResponse;
import com.helper.DateUtil;
import com.helper.EftCSVHelper;
import com.repository.EftRepo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import philae.api.BnUser;
import philae.api.UsRole;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class Eft {

    @Autowired
    EftRepo eftRepo;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Eft.class);

    /*
    EFT PAYMENT DASHBOARD
     */
    @RequestMapping(value = "/eftPaymentsDashboard", method = RequestMethod.GET)
    public String eftPaymentsDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "EFT PAYMENTS");
        model.addAttribute("paymentsPermissions", eftRepo.getEFTModulePermissions("/eftPaymentsDashboard", session.getAttribute("roleId").toString()));
        return "modules/eft/eftDashboard";
    }

    /*
    DOWNLOAD EFT SAMPLE FILE
     */
    @RequestMapping(value = "/downloadEftBulkSampleFile", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadEftBulkSampleFile(@RequestParam Map<String, String> customeQuery) throws IOException {
        File file = new File("/home/apps/cilantro/reportTemplate/eftBulksample.csv");
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=eftBulksample.csv");
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /*
    BULK PAYMENTS 
     */
    @RequestMapping(value = "/bulkPayments", method = RequestMethod.GET)
    public String bulkPayments(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENDING TRANSACTION AT IBD/HQ");
        return "modules/eft/bulkPayments";
    }

    /*
    UPLOAD BULK PAYMENTS/EFT
     */
    @RequestMapping("/initiateEftBulkPayments")
    @ResponseBody
    public ResponseEntity<EftResponseMessage> initiateEftBulkPayments(@Valid EftBulkPaymentForm eftBulkPaymentForm, @RequestParam("batchFile") MultipartFile batchFIle, @RequestParam("supportingDocument") MultipartFile supportingDoc, HttpSession session, BindingResult result) {
        LOGGER.info("initiateEftBulkPayments:{} and FileName:{}", eftBulkPaymentForm, batchFIle.getOriginalFilename());
        String message;
        EftCSVHelper eftCSVHelper = new EftCSVHelper();
        boolean validated = false;
        String jsonString = "";
        Map<String, String> errorMessages = null;

        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String formattedDate = df.format(Calendar.getInstance().getTime());

        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            errorMessages = errors;
            LOGGER.info("ERROR OCCURED DURING FORM SUBMISSION:{} ", errors.toString());
            message = errors.toString();
        } else {
            try {
                //check if the file is CSV
                List<String> banks = eftRepo.getBanksForEft();
                EftBulkPaymentResp eftBulkPaymentResp = eftCSVHelper.csvToEftBulkValidation(batchFIle.getInputStream(), banks);
                LOGGER.info("Validation Response:  {} ", eftBulkPaymentResp);
                if (eftBulkPaymentResp.getStatusCode().equals("0")) {
                    message = "Supporting document is required in PDF formart!";
                    //check if the amount is greater than the allowed maximum amount of 20M per one transaction

                    //COMPARE THE AMOUNTS BETWEEN TOTAL FILE AMOUNT AND SUBMITTED ON THE FORM
                    BigDecimal csvAmt = eftCSVHelper.csvTotalAmount(batchFIle.getInputStream());
                    csvAmt.setScale(2, BigDecimal.ROUND_UP);
                    System.out.println("CSV FILE AMOUNT: " + csvAmt);
                    BigDecimal formAmt = new BigDecimal(eftBulkPaymentForm.getAmount());
                    System.out.println("FORM AMOUNT: " + formAmt);
                    formAmt.setScale(2, BigDecimal.ROUND_UP);
                    int res1 = csvAmt.compareTo(formAmt);
                    //COMPARE THE AMOUNTS BETWEEN THE APPROVAL DOCUMENT AMOUNT AND 
                    if (res1 == 0) {
                        //Generate batch reference
                        String branchCode = session.getAttribute("branchCode").toString();
                        String batchReference = branchCode + "E" + formattedDate;
                        System.out.println("BATCH REFERENCE: " + batchReference);
                        //total number of transactions
                        int totalBatchCount = eftCSVHelper.csvTotalNoOfTransactions(batchFIle.getInputStream());

                        List<EftBulkPaymentReq> eftBulkPayments = eftCSVHelper.csvToEftBulkPayment(batchFIle.getInputStream());

                        System.out.println("CHECK CONDITION OF AMOUNT >20M " + eftCSVHelper.isAmountGreaterThan20m());
                        if (!eftCSVHelper.isAmountGreaterThan20m()) {
                            int res = eftRepo.insertEftBulkPayments(eftBulkPaymentForm.getSenderAccount(), eftBulkPayments, eftBulkPaymentForm, batchReference, session.getAttribute("username").toString(), branchCode, totalBatchCount, supportingDoc, csvAmt);
                            if (res == 0) {
                                message = "Bulk payment initiated successfully and submitted to Chief cashier work-flow";
                                validated = true;
                            } else {
                                message = "An error occured during initiation please Try again!!!!!";
                                validated = false;
                            }
                        }
                    } else {
                        LOGGER.info("Amount you Have entered is not equal with the Total batch amounts per all indviduals transactions, !!!!");
                        message = "Amount you Have entered is not equal with the Total batch amounts per all indviduals transactions. Total Batch amount: " + csvAmt + "  not equal to : " + formAmt;
                    }
                    return ResponseEntity.status(HttpStatus.OK).body(new EftResponseMessage(message, validated, jsonString, errorMessages));

                } else {
                    message = eftBulkPaymentResp.getStatusMessage();

                }
            } catch (IOException e) {
                message = "Could not upload the file: " + batchFIle.getOriginalFilename() + "!";
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new EftResponseMessage(message, validated, jsonString, errorMessages));
            }
        }
        LOGGER.info("FINAL MESSAGE TO VIEW: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new EftResponseMessage(message, validated, jsonString, errorMessages));
    }

    @RequestMapping(value = "/bulkPaymentsOnWorkFlow", method = RequestMethod.GET)
    public String bulkPaymentsOnWorkFlow(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        LOGGER.info("BRANCH APPROVER WORK-FLOW-BULK-PAYMENTS: AUTHORIZE TRANSACTIONS ON WORKFLOW");
        model.addAttribute("pageTitle", "AUTHORIZE EFT BULK PAYMENTS TRANSACTIONS (TZS ONLY) ON WORKFLOW");
        return "modules/eft/eftBulkPaymentsOnWorkFlow";
    }

    @RequestMapping(value = "/getEftBulkPaymentsOnWorkFlowAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getEftBulkPaymentsOnWorkFlowAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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
        LOGGER.info("Gettting getEftBulkPaymentsOnWorkFlowAjax with branch code: {} by username: {}", session.getAttribute("branchCode"), session.getAttribute("username"));
        return eftRepo.getEftBulkPaymentsOnWorkFlowAjax(date,(String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    *Preview EFT BULK PAYMENT PER CUSTOMER WITH ALL DETAILS
     */
    @RequestMapping(value = "/previewEftBulkPayments")
    public String previewEftBulkPayments(@RequestParam Map<String, String> customeQuery, Model model) {
        LOGGER.info("PREVIEWING EFT BULK PAYMENTS WITH BATCH REFERENCE: {}", customeQuery.get("batchReference"));
        model.addAttribute("pageTitle", "BATCH TRANSACTION WITH BATCH REFERENCE:  " + customeQuery.get("batchReference"));
        model.addAttribute("supportingDocs", eftRepo.getSwiftMessageSupportingDocs(customeQuery.get("batchReference")));
        model.addAttribute("reference", customeQuery.get("batchReference"));
        model.addAttribute("totalAmount", customeQuery.get("batchAmount"));
        model.addAttribute("batchCount", eftRepo.getEftBatchEntriesCount(customeQuery.get("batchReference")));
        model.addAttribute("batchEntries", eftRepo.getEftBatchEntries(customeQuery.get("batchReference")));
        return "modules/eft/modals/previewEftBulkPayments";
    }

    /*
    Branch approve EFT BATCH TRANSACTION
     */
    @RequestMapping(value = "/approveEftBatchBulkPayment", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveEftBatchBulkPayment(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select Role for posting!!!!.: \"}";
        LOGGER.info("url: {approveEftBatchBulkPayment} BRANCH APPROVING EFT BATCH: APPROVED BY {}, BATCH REFERENCE: {}", httpsession.getAttribute("username"), customeQuery.get("reference"));
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = eftRepo.processEftFrmAcctToEFTAwaiting(customeQuery.get("reference"), role, Integer.parseInt(customeQuery.get("batchCount")));
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    GET EFT BULK PAYMENTS ON HQ WORK-FLOW
     */
    @RequestMapping(value = "/eftBulkPaymentsOnHQWorkFlow", method = RequestMethod.GET)
    public String eftBulkPaymentsOnHQWorkFlow(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        LOGGER.info("VIEW EFT BATCHES ON HQ WORK FLOW");
        model.addAttribute("pageTitle", "AUTHORIZE EFT BULK PAYMENTS TRANSACTIONS  ON WORKFLOW");
        return "modules/eft/eftBulkPaymentsOnHQWorkFlow";
    }

    @RequestMapping(value = "/getEftBulkPaymentsOnHQWorkFlowAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getEftBulkPaymentsOnHQWorkFlowAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return eftRepo.getEftBulkPaymentsOnHQWorkFlowAjax((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
     *HQ Preview EFT BULK PAYMENT PER CUSTOMER WITH ALL DETAILS
     */
    @RequestMapping(value = "/hqPreviewEftBatchPayments")
    public String hqPreviewEftBatchPayments(@RequestParam Map<String, String> customeQuery, Model model) {
        LOGGER.info("HQ PREVIEWING EFT BULK PAYMENTS WITH BATCH REFERENCE: {}", customeQuery.get("batchReference"));
        model.addAttribute("pageTitle", "BATCH TRANSACTION WITH BATCH REFERENCE:  " + customeQuery.get("batchReference"));
        model.addAttribute("supportingDocs", eftRepo.getSwiftMessageSupportingDocs(customeQuery.get("batchReference")));
        model.addAttribute("reference", customeQuery.get("batchReference"));
        model.addAttribute("batchCount", eftRepo.getEftBatchEntriesCount(customeQuery.get("batchReference")));
        model.addAttribute("totalAmount", customeQuery.get("batchAmount"));
        model.addAttribute("batchEntries", eftRepo.getEftBatchEntries(customeQuery.get("batchReference")));
        return "modules/eft/modals/hqPreviewEftBatchPayments";
    }

    /*
      HQ approve EFT BATCH TRANSACTION
     */
    @RequestMapping(value = "/approveBranchEftBatchPayment", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveBranchEftBatchPayment(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select Role for posting.\"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
            for (UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = eftRepo.processEftFrmBrnchEFTLedgerToHqEFTLedger(customeQuery.get("reference"), role, Integer.parseInt(customeQuery.get("batchCount")));
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    @RequestMapping(value = "/fireHqDiscardEftBatchTxn", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireHqDiscardEftBatchTxn(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select Role for posting.\"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
            for (UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = eftRepo.fireHqDiscardEftBatchTxn(customeQuery.get("reference"), role, Integer.parseInt(customeQuery.get("batchCount")));
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    INWARD EFT DISPLAY  /inwardEFT
     */
    @RequestMapping(value = "/inwardEFT", method = RequestMethod.GET)
    public String inwardEFT(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "INWARD EFT BATCH TRANSACTION SUMMARY AS AT: " + DateUtil.now("yyyy-MM-dd"));
        return "modules/eft/inwardEFTBatchSummary";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/getInwardEFTAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getInwardEFTAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("txndate") + " " + customeQuery.get("fromTime") + ":00";
        String todate = customeQuery.get("txndate") + " " + customeQuery.get("totime") + ":59";
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return eftRepo.getInwardEFTAjax(fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    *Preview EFT SUCCESS/FAILED TRANSACTIONS PER BANK
     */
    @RequestMapping(value = "/previewEftPerBankTxns")
    public String previewEftSuccessTxns(@RequestParam Map<String, String> customeQuery, Model model) {
        LOGGER.info("PREVIEWING EFT INCOMING SUCCESS TRANSACTIONS SENDER BIC: {} TXN STATUS: {}", customeQuery.get("senderBic"), customeQuery.get("txnStatus"));
        String title = "EFT SUCCESSFULLY TRANSACTIONS FOR BANK:   " + customeQuery.get("senderBic");
        if (customeQuery.get("txnStatus").equals("F")) {
            title = "EFT FAILED TRANSACTIONS FOR BANK:   " + customeQuery.get("senderBic");
        }
        model.addAttribute("pageTitle", title);
        model.addAttribute("senderBic", customeQuery.get("senderBic"));
        model.addAttribute("txnStatus", customeQuery.get("txnStatus"));
        boolean action = false;
        if (!customeQuery.get("txnStatus").equalsIgnoreCase("C")) {
            //this are failed transactions 
            action = true;
        }
        model.addAttribute("fromDate", customeQuery.get("txndate") + " " + customeQuery.get("fromTime") + ":00");
        model.addAttribute("toDate", customeQuery.get("txndate") + " " + customeQuery.get("toTime") + ":59");
        model.addAttribute("isActionAllowed", action);
        return "modules/eft/modals/previewEftInwardPerBankTxns";
    }

    /* 
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/getInwardEFTPerBankAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getInwardEFTPerBankAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return eftRepo.getInwardEFTSuccessPerBankAjax(customeQuery.get("fromDate"), customeQuery.get("toDate"), customeQuery.get("senderBic"), customeQuery.get("txnStatus"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    RETURN EFT TRANSACTION TO THE SENDER
     */
    @RequestMapping(value = "/returnInwardEftTransactions", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String returnInwardEftTransactions(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        LOGGER.info("transaction references to be returned: {}", customeQuery.get("txnid"));
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");

            String postingRole = (String) session.getAttribute("postingRole");
            String reference = customeQuery.get("reference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        LOGGER.info("return reasonCode:{} reference:{} ReasonDescription:{}", customeQuery.get("returnReason"), customeQuery.get("reference"), customeQuery.get("reason"));
                        result = eftRepo.generateReturnMessageToTach(customeQuery.get("returnReason"), customeQuery.get("reference"), role.getUserName(), customeQuery.get("reason"));
                        break;
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }
//        return eftRepo.generateReturnMessageToTach(customeQuery.get("references"));


    /*
    *REPLAY EFT TRANSACTION TO CORE BANKING
     */
    @RequestMapping(value = "/replayEFTTxnsToCoreBanking", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String replayEFTTxnsToCoreBanking(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, @RequestParam(value = "txnid", required = false) List<String> txnid) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");

            String postingRole = (String) session.getAttribute("postingRole");
            String reference = customeQuery.get("reference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        result = eftRepo.replayEFTIncomingTOCBS(customeQuery.get("ambiguousOption"), customeQuery.get("branchNo"), role, txnid);
                        break;
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    /*
    OUTWARD EFT DISPLAY  
     */
    @RequestMapping(value = "/eftTransactionsReports", method = RequestMethod.GET)
    public String eftTransactionsReports(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "EFT TRANSACTION REPORTS ");
        return "modules/eft/eftTransactionReports";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/getOutwardEFTBatchSummaryAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getOutwardEFTBatchSummary(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate") + " 00:00:00";
        String todate = customeQuery.get("toDate") + " 23:59:59";
        String txnType = customeQuery.get("txnType");
        String txnStatus = customeQuery.get("txnStatus");
        String amount = customeQuery.get("amount");
        String reference = customeQuery.get("reference");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return eftRepo.getOutwardEFTBatches(txnType,txnStatus,amount,reference,fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @GetMapping("/failedToReachTach")
    public String failedToReachTach(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","EFT OUTWARD TRANSACTIONS FAILED TO REACH TACH");
        return "/modules/eft/failedToReachTach";
    }

    @RequestMapping(value = "/fireGetFailedToReachTachAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String failedToReachTachAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return eftRepo.queryFailedToReachTachAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "firePushTransactionToTachAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String firePushTransactionToTachAjax(@RequestParam Map<String, String> customQuery){
        return eftRepo.pushTransactionToTachAjax(customQuery);
    }

    @PostMapping(value = "fireCancelEftTransactionAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String cancelEftTransactionAjax(@RequestParam Map<String, String> customQuery){
        return eftRepo.cancelTransactionToTachAjax(customQuery);
    }

    @PostMapping(value = "fireCancelEftHQWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireCancelEftHQWFAjax(@RequestParam Map<String, String> customQuery){
        return eftRepo.cancelTransactionHQWFAjax(customQuery);
    }

    @GetMapping("/eftReconReports")
    public String loansReport(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","EFT RECONCILIATION REPORTS");
        model.addAttribute("fromDate", DateUtil.previosDay(1));
        model.addAttribute("toDate",DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("banks",eftRepo.getEftBanks());
        model.addAttribute("branches",eftRepo.getBranches());
        return "/modules/eft/eftReconciliationReports";
    }

    @PostMapping(value = "fireGetEftReconReportAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse getEftReconReportAjax(@RequestParam Map<String, String> customQuery){
        return eftRepo.getEftReconReportAjax(customQuery);
    }
}
