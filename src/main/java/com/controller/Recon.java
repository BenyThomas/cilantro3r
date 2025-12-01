/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.GeneralJsonResponse;
import com.DTO.Reports.AuditTrails;
import com.controller.visaCardReport.VisaCardJsonResponse;
import com.entities.JsonResponse;
import com.entities.Mail;
import com.entities.ReconDashboard;
import com.entities.ReconForm;
import com.entities.ReconFormJsonResponse;
import com.entities.RetryRefundRequest;
import com.helper.DateUtil;
import com.helper.PushThread;
import com.repository.Recon_M;
import com.repository.Settings_m;
import com.service.EmailSenderService;
import com.service.FileService;
import com.service.JasperService;
import com.service.PaymentHandler;
import com.service.ReconHandler;
import com.service.ReportService;
import com.service.TransferService;
import com.service.XapiWebService;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import philae.api.BnUser;

@Controller
public class Recon {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    Recon_M reconRepo;

    @Autowired
    FileService fileService;

    @Autowired
    ReconHandler reconHandler;
    String response = "-1";
    @Autowired
    private EmailSenderService emailService;

    @Autowired
    Settings_m settingRepo;

    @Autowired
    @Qualifier("dbPoolExecutor")
    ThreadPoolTaskExecutor exec;

    @Autowired
    PushThread pushThread;
    @Autowired
    HttpSession httpSession;
    private static String UPLOADED_FOLDER = "F://temp//";
    @Value("${file.storage.location}")
    public String uploadDir;

    @Autowired
    PaymentHandler paymentHandler;

    @Autowired
    JasperService jasperService;
    @Autowired
    XapiWebService XapiWebService;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;
    @Autowired
    @Qualifier("mkobadb")
    JdbcTemplate jdbcMKOBATemplate;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;
    @Autowired
    TransferService transferService;
    @Autowired
    ReportService reportService;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Recon.class);

    @RequestMapping(value = "/reconDashboard")
    public String dashboard(Model model, HttpSession session) {
        if ((session.getAttribute("reconDashboardData")) != null) {
        }

        AuditTrails.setComments("View Reconciliation Dashboard");
        AuditTrails.setFunctionName("/reconDashboard");
        //get the user in session
        BnUser user = (BnUser) httpSession.getAttribute("userCorebanking");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(session.getAttribute("roleId").toString(), session.getAttribute("userId").toString()));
        return "pages/reconDashboard";
    }

    @RequestMapping(value = "/cbsTxns")
    public String cbsTransactions(Model model) {
        AuditTrails.setComments("View core Banking Transactions with Transaction date:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/cbsTxns");
        model.addAttribute("pageTitle", "TRANSACTIONS AS AT :...................[" + httpSession.getAttribute("txndate") + "]");
        return "pages/cbsTransactions";
    }

    @RequestMapping(value = "/reportsTest")
    @ResponseBody
    public String sampleReportRecon(@RequestParam Map<String, String> customeQuery, Model model, HttpServletResponse response) throws Exception {
        model.addAttribute("pageTitle", "TRANSACTIONS AS AT :...................[" + httpSession.getAttribute("txndate") + "]");
        return jasperService.sampleReportRecon(customeQuery, "pdf", response, "sampleRecon");
    }

    /*
     *get cbs transactions json format
     */
    @RequestMapping(value = "/getCbsTxnsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String coreBankingTransactions(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        String exceptionType = customeQuery.get("exceptionType");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        //System.out.println("CUSTOM STRING: " + customeQuery.toString());
        // System.out.println("TXNDATE: " + session.getAttribute("txndate").toString());
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        AuditTrails.setComments("View " + mno + "Transactions  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/getCbsTxnsAjax");
        try {
            return reconRepo.getCoreBankingTxnsAjax(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder, exceptionType);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/cbsFailedTxns")
    public String cbsFailedTxns(Model model) {
        AuditTrails.setComments("View core Banking Reversed Transactions with Transaction date:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/cbsFailedTxns");
        model.addAttribute("pageTitle", "TRANSACTIONS AS AT :...................[" + httpSession.getAttribute("txndate") + "]");
        return "pages/cbsReversedTxns";
    }

    /*
     *get cbs failed transactions json format
     */
    @RequestMapping(value = "/getCbsFailedTxns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCbsFailedTxns(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        System.out.println("");
        String json = reconRepo.getCbsReversedTxnsList(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString());
        return json;
    }

    @RequestMapping(value = "/summaryReport")
    public String summaryReport(Model model, HttpSession session) {

        model.addAttribute("pageTitle", session.getAttribute("ttype") + " RECONCILIATION REPORT AS AT:" + session.getAttribute("txndate") + "");
        List<Map<String, Object>> reconSummary = reconRepo.getReconciliationSummaryReport(String.valueOf(session.getAttribute("ttype")), String.valueOf(session.getAttribute("txndate")));
        List<Map<String, Object>> reconExceptionSummary = reconRepo.getReconExceptionSummaryReport(String.valueOf(session.getAttribute("ttype")), String.valueOf(session.getAttribute("txndate")));
        model.addAttribute("reconDatas", reconSummary);
        model.addAttribute("exceptions", reconExceptionSummary);
        AuditTrails.setComments("View Transactions Reconciliation summary as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/summaryReport");
        return "pages/summaryReport";
    }

    @RequestMapping(value = "/thirdPartyTxns")
    public String thirdPartyTransactions(Model model, HttpSession session) {
        AuditTrails.setComments("View Third party Transactions  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/thirdPartyTxns");
        model.addAttribute("pageTitle", "THIRD PART TRANSACTIONS AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/thirdPartyTransactions";
    }

    @RequestMapping(value = "/getThirdPartyTxnsAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getThirdPartyTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        String exceptionType = customeQuery.get("exceptionType");
        AuditTrails.setComments("View " + mno + "Transactions  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/getThirdPartyTxnsAjax");
        String json = reconRepo.getThirdPartyTransactions(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), exceptionType);
        return json;
    }

    @RequestMapping(value = "/thirdPartyFailedTxns")
    public String thirdPartyFailedTransactions(Model model, HttpSession session) {
        AuditTrails.setComments("View Third Party Failed Transactions  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/thirdPartyFailedTxns");
        model.addAttribute("pageTitle", "THIRD PART FAILED TRANSACTIONS AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/thirdPartyFailedTransactions";
    }

    @RequestMapping(value = "/getThirdPartyFailedTxnsAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getThirdPartyFailedTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        AuditTrails.setComments("View " + mno + " Failed Transactions  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/getThirdPartyFailedTxnsAjax");

        String json = reconRepo.getThirdPartyFailedTransactions(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString());
        return json;
    }

    @RequestMapping(value = "/notInCbs")
    public String notInCbsTransactions(Model model, HttpSession session) {
        AuditTrails.setComments("View  Transactions That are not in core banking as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/notInCbs");

        model.addAttribute("pageTitle", "TRANSACTIONS NOT IN CORE BANKING  AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/notInCbsTransactions";
    }

    @RequestMapping(value = "/getNotInCbsTransactionsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getNotInCbsTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {

        String mno = customeQuery.get("mno");
        String exceptionType = customeQuery.get("exceptionType");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        AuditTrails.setComments("View " + mno + " Transactions That are not in core banking as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/notInCbs");

        String result = reconRepo.getNotInCbsTransactions(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder, exceptionType);
        return result;
    }

//    @RequestMapping(value = "/getNotInCbsTransactionsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public GeneralJsonResponse getNotInCbsTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
//        String mno = customeQuery.get("mno");
//        String exceptionType = customeQuery.get("exceptionType");
//        session.setAttribute("mno", mno);
//        GeneralJsonResponse gjr = new GeneralJsonResponse();
//        gjr.setStatus("SUCCESS");
//        gjr.setResult(reconRepo.getNotInCbsTransactions(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(),exceptionType));
//        return gjr;
//    }

    @RequestMapping(value = "/notInThirdPartyTxns")
    public String notInThirdPartyTransactions(Model model, HttpSession session) {
        AuditTrails.setComments("View Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/notInThirdPartyTxns");

        model.addAttribute("pageTitle", "TRANSACTIONS NOT IN THIRD PARTY  AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/notInThirdPartyTransactions";
    }

    @RequestMapping(value = "/getNotInThirdPartyTxnsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getNotInThirdPartyTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");

        return reconRepo.getNotInThirdPartyTransactions(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    @RequestMapping(value = "/gepgExceptionsNotInSettlement")
    public String gepgExceptionsNotInSettlement(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "TRANSACTIONS GEPG REFUND  AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/gepgExceptionsNotInSettlement";
    }

    @RequestMapping(value = "/getGepgExceptionsNotInSettlement", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getGepgExceptionsNotInSettlement(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return reconRepo.getGepgExceptionsNotInSettlement(session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    @RequestMapping(value = "/lukuExceptionsNotInSettlement")
    public String lukuExceptionsNotInSettlement(Model model, HttpSession session) {

        model.addAttribute("pageTitle", "TRANSACTIONS LUKU REFUND  AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/lukuExceptionsNotInSettlement";
    }

    @RequestMapping(value = "/getLukuExceptionsNotInSettlement", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getLukuExceptionsNotInSettlement(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return reconRepo.getLukuExceptionsNotInSettlement(session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    @RequestMapping(value = "/cbsSuccessThirdPartyFailed")
    public String cbsSuccessThirdPartyFailed(Model model, HttpSession session) {
        AuditTrails.setComments("View Transactions That are successfully on Core banking but failed on  Third Party  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/cbsSuccessThirdPartyFailed");
        model.addAttribute("pageTitle", "TRANSACTIONS NOT IN THIRD PARTY  AS AT :...................[" + session.getAttribute("txndate") + "]");
        return "pages/cbsSuccessThirdPartyFailed";
    }

    @RequestMapping(value = "/getCbsSuccessThirdPartyFailedAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCbsSuccessThirdPartyFailedAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");

        return reconRepo.getCbsSuccessThirdPartyFailed(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    @RequestMapping(value = "/thirdPartySuccessCbsFailed")
    public String thirdPartySuccessCBSFailed(Model model, HttpSession session) {
        AuditTrails.setComments("View Transactions That are successfully on Third Party but failed on Core banking as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/thirdPartySuccessCbsFailed");

        model.addAttribute("pageTitle", "TRANSACTIONS SUCCESSFULLY ON THIRD PARTY BUT FAILED ON CBS[" + session.getAttribute("txndate") + "]");
        return "pages/thirdPartySuccessCbsFailed";
    }

    @RequestMapping(value = "/getThirdPartySuccessCBSFailedAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getThirdPartySuccessCBSFailedAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        AuditTrails.setComments("View " + mno + " Transactions That are not in Third Party  as at:  " + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/getNotInThirdPartyTxnsAjax");

        return reconRepo.getThirdPartySuccessCBSFailed(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    /*
    Reprocess FAILED TRANSACTION/WRONG POSTED TRANSACTION
     */
    @RequestMapping(value = "/initiateConfirmation")
    public String reprocessTransaction(HttpServletRequest request, HttpSession session, Model model) {

        AuditTrails.setComments("Initiate confirmation to thirdparty" + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/initiateConfirmation");

        return "pages/modals/initiateConfirmationToThirdparty";
    }

    /*
     * INITIATE REFUND/RETRY FAILED TRANSACTION
     */
    @RequestMapping(value = "/initiateBulkRefundRetry")

    public String initiateBulkRefundRetry(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, Model model) {
        AuditTrails.setComments("Initiate Bulk Refund/Retry to gateway" + httpSession.getAttribute("txndate"));
        LOGGER.info("initiateBulkRefundRetry: {}", customeQuery);
        AuditTrails.setFunctionName("/initiateBulkRefundRetry");
        if( customeQuery.get("txnType")==null){
            model.addAttribute("txn_type", customeQuery.get("txn_type"));

        }else {
            model.addAttribute("txn_type", customeQuery.get("txnType"));
        }
        return "pages/modals/initiateBulkRefundRetry";
    }

    /*
     * SUBMIT INITIATE REFUND/RETRY FAILED TRANSACTION
     */
    @RequestMapping(value = "/submitInitiateBulkRefundRetry")
    public String submitInitiateBulkRefundRetry(@RequestParam("fileName") MultipartFile file, @RequestParam("txnid") List<String> txnid, RedirectAttributes redirectAttributes, @RequestParam Map<String, String> customeQuery) {
        fileService.uploadFile(file);
        LOGGER.info("submitInitiateBulkRefundRetry:File-> {}, CustomeQuery-> {}, Txnid:-> {}", file.getOriginalFilename(), customeQuery, txnid);
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        String fileName = formatDate.format(date) + file.getOriginalFilename().toLowerCase().replace(" ", "-");

        String usernameLogged = httpSession.getAttribute("username").toString();
        String ttype = httpSession.getAttribute("ttype").toString();
        String txn_type = customeQuery.get("txn_type") + "";
        LOGGER.info("ttype-> {}, txn_type:-> {} for txnid...{}", ttype, txn_type,txnid);
        txnid.forEach(txnReference -> {
            LOGGER.info(txnReference + " Reference - for retry: " + reconRepo.saveInitiatedRetryRefundTxns(fileName, usernameLogged, txnReference, "logged", customeQuery.get("reason"), ttype, txn_type));
        });
        AuditTrails.setComments("Submit bulk Retry/Refund with transaction references" + txnid.toString());
        AuditTrails.setFunctionName("/submitInitiateBulkRefundRetry");
        System.out.println("TRANSACTION REFERENCE: " + txnid.toString());
        return "pages/modals/initiateBulkRefundRetry";
    }

    /*
     * INITIATE REFUND/RETRY FAILED TRANSACTION
     */
    @RequestMapping(value = "/LUKUInitiateBulkRefundRetry")

    public String LUKUInitiateBulkRefundRetry(HttpServletRequest request, HttpSession session, Model model) {
        AuditTrails.setComments("Initiate Bulk Refund/Retry to gateway" + httpSession.getAttribute("txndate"));
        AuditTrails.setFunctionName("/initiateBulkRefundRetry");
        return "pages/modals/LUKUInitiateBulkRefundRetry";
    }

    /*
     * SUBMIT INITIATE REFUND/RETRY FAILED TRANSACTION
     */
    @RequestMapping(value = "/LUKUsubmitInitiateBulkRefundRetry")
    public String LUKUsubmitInitiateBulkRefundRetry(@RequestParam("fileName") MultipartFile file, @RequestParam("txnid") List<String> txnid, RedirectAttributes redirectAttributes, @RequestParam Map<String, String> customeQuery) {
        LOGGER.info("Refund failed LUKU transactions...with ID.. {}", txnid);
        fileService.uploadFile(file);
        LOGGER.info("LUKUsubmitInitiateBulkRefundRetry:File-> {}, CustomeQuery-> {}, Txnid:-> {}", file.getOriginalFilename(), customeQuery.toString(), txnid);
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        String fileName = formatDate.format(date) + file.getOriginalFilename().toLowerCase().replace(" ", "-");

        String usernameLogged = httpSession.getAttribute("username").toString();
        String ttype = "LUKUEX";
        String txn_type = "";// httpSession.getAttribute("txn_type").toString();
        LOGGER.info("ttype-> {}, txn_type:-> {}", ttype, txn_type);
        txnid.forEach(txnReference -> {
            LOGGER.info(txnReference + " Reference - for retry: " + reconRepo.saveInitiatedRetryRefundTxns(fileName, usernameLogged, txnReference, "logged", customeQuery.get("reason"), ttype, txn_type));
        });
        AuditTrails.setComments("Submit bulk Refund LUKU with transaction references" + txnid.toString());
        AuditTrails.setFunctionName("/submitInitiateBulkRefundRetry");
        System.out.println("TRANSACTION REFERENCE: " + txnid.toString());
        return "pages/modals/initiateBulkRefundRetry";
    }

    /*
     * Get initiated Transactions from rety table
     */
    @RequestMapping(value = "/retryRefundTxnsOnQueueView")
    public String retryRefundTxnsOnQueueView(Model model, HttpSession session) {
        AuditTrails.setComments("Approve Refund  transaction on the Queue");
        AuditTrails.setFunctionName("/retryRefundTxnsOnQueueView");
        model.addAttribute("pageTitle", "APPROVE TRANSACTION :...................[" + session.getAttribute("txndate") + "]");
        return "pages/approveRefundRetryTxnsOnQueue";
    }

    @RequestMapping(value = "/getRetryRefundTxnsOnQueueAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRetryRefundTxnsOnQueueAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String mno = customeQuery.get("mno");
        System.out.println("MNO:" + mno);
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        boolean isW2B = false;
        boolean isB2W = false;
        List<Map<String, Object>> txn_identifier = (List<Map<String, Object>>) httpSession.getAttribute("txntypes");
        for (Map<String, Object> tt : txn_identifier) {
            LOGGER.info("{} v/s {}", httpSession.getAttribute("ttype"), tt.get("ttype"));
            if (httpSession.getAttribute("ttype").equals(tt.get("ttype"))) {
                isW2B = (boolean) tt.get("isW2B");
                isB2W = (boolean) tt.get("isB2W");
            }
        }
        if (mno.equals("WALLET2MKOBA") ||mno.equals("WALLET2VIKOBA") ) {
            isB2W = false;
            isW2B = true;
        }
        if (mno.equals("MKOBA2WALLET") || mno.equals("VIKOBA2WALLET")) {
            isB2W = true;
            isW2B = false;
        }
        if (mno.equals("LUKU")) {
            isB2W = true;
            isW2B = false;
        }
        if (mno.equals("LUKUEX")) {
            isB2W = true;
            isW2B = false;
        }
        AuditTrails.setComments("View transactions that are on the queue");
        AuditTrails.setFunctionName("/getRetryRefundTxnsOnQueueAjax");
        String responseData = reconRepo.getInitiatedRefundRetryTxnsOnQueue(mno, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), isB2W, isW2B, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
        return responseData;
    }

    /*
     * Get initiated Transactions from rety table
     */
    @RequestMapping(value = "/approveRefundRetryOnQueue")
    public String approveRefundRetryOnQueue(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        model.addAttribute("reason", customeQuery.get("reason"));
        model.addAttribute("reason", customeQuery.get("reason"));
        model.addAttribute("txn_type", customeQuery.get("txn_type"));
        AuditTrails.setComments("Approve transactions that are on the queue");
        AuditTrails.setFunctionName("/approveRefundRetryOnQueue");
        model.addAttribute("pageTitle", "ARE YOU SURE YOU WANT TO APPROVE THIS REFUND/RETRY TXNS  AS :...................[" + session.getAttribute("txndate") + "] ?");
        return "pages/modals/approveRefundRetryTxns";
    }

    @RequestMapping(value = "/submitApprovalForRefundRetry", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String submitApprovalForRefundRetry(@RequestParam("txnid") List<String> txnid, @RequestParam("msisdn") List<String> msisdn, @RequestParam("docode") List<String> docode, @RequestParam("sourceAcct") List<String> sourceAcct, @RequestParam("destinationAcct") List<String> destinationAcct, @RequestParam("amount") List<String> amount, RedirectAttributes redirectAttributes, @RequestParam Map<String, String> customeQuery, HttpSession httpSession) {
        Date date = new Date(System.currentTimeMillis());
        String txn_type = customeQuery.get("txn_type") + "";
        LOGGER.info("submitApprovalForRefundRetry: ttype-> {} txn_type-> {}", httpSession.getAttribute("ttype"), customeQuery);
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        response = "{\"message\":\"Your Request/Requests are being processed please check on customer's Accounts\"}";
        String request2 = "-1";
        // taskExecutor.execute(() -> {
        try {
            for (int i = 0; i < txnid.size(); i++) {
                String request = "-1";
                System.out.println("");
                if (httpSession.getAttribute("ttype").equals("B2C")  && !docode.get(i).equals("IB001")) {
                    request = "<B2C_REVERSAL><transactionid>" + txnid.get(i) + "</transactionid><msisdn>" + msisdn.get(i) + "</msisdn><account>" + sourceAcct.get(i) + "</account><toaccount>" + destinationAcct.get(i) + "</toaccount><amount>" + amount.get(i) + "</amount></B2C_REVERSAL>";
                }else  if (httpSession.getAttribute("ttype").equals("B2C") && docode.get(i).equals("IB001")) {
                    //TODO: CHANGE PAYLOAD
                    request = "<MPESA>\n" +
                        "    <TRANSID>" + txnid.get(i) + "</TRANSID>\n" +
                        "    <RECEPT>failed</RECEPT>\n" +
                        "    <AMOUNT>failed</AMOUNT>\n" +
                        "    <STASCODE>TS28009</STASCODE>\n" +
                        "    <STATUS>failed</STATUS>\n" +
                        "</MPESA>";
                }
//                if (httpSession.getAttribute("ttype").equals("UTILITY") && !(txn_type.contains("LUKU"))) {
//                    request = "<B2C_REVERSAL><transactionid>" + txnid.get(i) + "</transactionid><msisdn>" + msisdn.get(i) + "</msisdn><account>" + sourceAcct.get(i) + "</account><toaccount>" + destinationAcct.get(i) + "</toaccount><amount>" + amount.get(i) + "</amount></B2C_REVERSAL>";
//                }
//
//                if (httpSession.getAttribute("ttype").equals("UTILITY") && (txn_type.equalsIgnoreCase("LUKUEX"))) {
//                    request = "<B2C_REVERSAL><transactionid>" + txnid.get(i) + "</transactionid><msisdn>" + msisdn.get(i) + "</msisdn><account>" + sourceAcct.get(i) + "</account><toaccount>" + destinationAcct.get(i) + "</toaccount><amount>" + amount.get(i) + "</amount></B2C_REVERSAL>";
//
//                    request = "<Gepg\n" + "    xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "    <gepgPymtResp>\n" + "        <PymtTrxInf>\n" + "            <TrxId>" + txnid.get(i) + "</TrxId>\n" + "            <RcptNum>" + txnid.get(i) + "</RcptNum>\n" + "            <TrxSts>GF</TrxSts>\n" + "            <TrxStsCode>7408</TrxStsCode>\n" + "        </PymtTrxInf>\n" + "    </gepgPymtResp>\n" + "    <gepgSignature>ACtuKRoRkTZuM5MOZOnPdYZXUDtFCZYrK7rtS2dcGsYICio5IvNsqUjsfvEQ/eir0vlaeK93u0GsWL0mH2LW3RTdsArbuFdNQbC+hssZGHbe4q0U6uRbZivJHao3aXEoAq3IQ8y112ZgP3MvBYxEEH6lDWGrAAF1nSk3oK4Ge7HzC05RS45wCyh+naVoHatOg8+mcv949aXltP0qsGp6oAaDl4wneSW2T41+j6B+jV4W3emHi908H5hx+B1nloF8tMdzF4BjUyyD8ew80WuQY5f34/qWWQsMsksO8W6WqS51KQ0UhQf/N2lNZ65EVmxfXBlFP/oIxyHKDrjwH8VOXw==</gepgSignature>\n" + "</Gepg>";
//                    String vTxnid = txnid.get(i)+"";
//
//                   /* if(vTxnid.startsWith("MOB") || vTxnid.startsWith("T0")){
//                        request2 = "<LUKU>\n" +
//                                "    <TRANSID>"+txnid.get(i)+"</TRANSID>\n" +
//                                "    <RECEPT>"+txnid.get(i)+"</RECEPT>\n" +
//                                "    <STATUS>GF</STATUS>\n" +
//                                "    <STATCODE>7408</STATCODE>\n" +
//                                "    <METER>"+txnid.get(i)+"</METER>\n" +
//                                "    <OWNER>TANESCO</OWNER>\n" +
//                                "    <TOKEN>-1</TOKEN>\n" +
//                                "    <UNITS>0</UNITS>\n" +
//                                "    <AMOUNT>0</AMOUNT>\n" +
//                                "    <TAX>0.00</TAX>\n" +
//                                "    <VAT>0.00</VAT>\n" +
//                                "    <EWURA>0.00</EWURA>\n" +
//                                "    <REA>0.00</REA>\n" +
//                                "    <RESERVEFIELD1>-1</RESERVEFIELD1>\n" +
//                                "    <RESERVEFIELD2>-1</RESERVEFIELD2>\n" +
//                                "    <RESERVEFIELD3>-1</RESERVEFIELD3>\n" +
//                                "    <CURENCY>TZS</CURENCY>\n" +
//                                "</LUKU>";
//                    }
//
//                    */
//                }
//                if (httpSession.getAttribute("ttype").equals("UTILITY") && (txn_type.contains("LUKU"))) {
//                    List<Map<String, Object>> checkMNOTrans = this.jdbcTemplate.queryForList("select * from thirdpartytxns where txn_type = 'LUKU' and  txnid =?", txnid.get(i));
//                    //LOGGER.info("select * from thirdpartytxns where txn_type = 'LUKU' and  txnid =?".replace("?", "'{}'"), txnid.get(i));
//                    if (!checkMNOTrans.isEmpty()) {
//                        request = "<Gepg\n" + "    xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "    <gepgPymtResp>\n" + "        <PymtTrxInf>\n" + "            <TrxId>" + txnid.get(i) + "</TrxId>\n" + "            <RcptNum>" + checkMNOTrans.get(0).get("receiptNo") + "</RcptNum>\n" + "            <Meter>" + checkMNOTrans.get(0).get("txdestinationaccount") + "</Meter>\n" + "            <Owner>TANESCO</Owner>\n" + "            <Token>" + checkMNOTrans.get(0).get("acct_no") + "</Token>\n" + "            <Units>null</Units>\n" + "            <Amount>" + checkMNOTrans.get(0).get("amount") + "</Amount>\n" + "            <Tax>0</Tax>\n" + "            <FixedCharge>\n" + "                <fixed amt=\"0.0\" tax=\"0.00\">VAT 18%</fixed>\n" + "                <fixed amt=\"0.0\" tax=\"0.00\">EWURA 1%</fixed>\n" + "                <fixed amt=\"0.0\" tax=\"0.00\">REA 3%</fixed>\n" + "            </FixedCharge>\n" + "            <DebtCollection/>\n" + "            <ReserveField1/>\n" + "            <ReserveField2/>\n" + "            <ReserveField3/>\n" + "            <Curency>TZS</Curency>\n" + "            <TrxSts>GS</TrxSts>\n" + "            <TrxStsCode>7101</TrxStsCode>\n" + "        </PymtTrxInf>\n" + "    </gepgPymtResp>\n" + "    <gepgSignature>GUczqTn86KC9EbfOTXaMDaYGtpnR8oSsedMxTHLHDaiVIVb5D6IEmJFLG1gCIm6GuTz9fUfE20VbMp3QPP9/eCR5Gh1DAGRYJL4oHm8RtGPKTQnsRHf31hJPbYuqjO4ArMhaOgc7sni/RhbHi42gWbmSqOHpd9zzbLi4zzTUXejnGsc0mYEih68xdtVEN9MCyudqT+v/TbGPB0HRO3STEuYZ9hk0iF657jQr2oQD7bh5RVvCm89VkyeDdo/IZOmr/aGfcAQWl2TxFXtieBpa2n2i6JcXu74wGJnwk1vkpzKegyknqpJEtfW1DOx4h4x+U0xI1s0MbLb7iDPKaWyuvQ==</gepgSignature>\n" + "</Gepg>";
//                    } else {
//                        LOGGER.info("Token not found for ->{}", txnid.get(i));
//
//                    }
//                }
                if (httpSession.getAttribute("ttype").equals("C2B")) {
                    request = "<CBS_RETRY><sessionid>" + txnid.get(i) + "</sessionid><msisdn>" + msisdn.get(i) + "</msisdn><imsi></imsi><account>" + sourceAcct.get(i) + "</account><toaccount>" + destinationAcct.get(i) + "</toaccount><amount>" + amount.get(i) + "</amount><trans_type>" + docode.get(i) + "</trans_type><processcode>570000</processcode><msgid>200</msgid></CBS_RETRY>";
                }

                if (httpSession.getAttribute("ttype").equals("MKOBA") && customeQuery.get("docode").equals("MKOBA2WALLET")) {
                    request = "<ns3:result\n" + "    xmlns:ns3=\"http://infowise.co.tz/broker/\"\n" + "    xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" + "    xmlns:response=\"http://infowise.co.tz/broker/response\"\n" + "    xmlns:request=\"http://infowise.co.tz/broker/request\">\n" + "    <ns3:resultType>7</ns3:resultType>\n" + "    <ns3:resultCode>7</ns3:resultCode>\n" + "    <ns3:resultDesc>Process service request successfully.</ns3:resultDesc>\n" + "    <ns3:originatorConversationID>11820214308161020_" + msisdn.get(i) + "_1_115_450000_null_" + destinationAcct.get(i) + "_" + sourceAcct.get(i) + "_1628615803118_1112213457820_1111_832385378067974_3080400_3080400</ns3:originatorConversationID>\n" + "    <ns3:conversationID>AG_20210810_000077c606b089f183fa</ns3:conversationID>\n" + "    <ns3:mpesaReceipt>" + txnid.get(i) + "</ns3:mpesaReceipt>\n" + "    <ns3:transactionID>" + txnid.get(i) + "</ns3:transactionID>\n" + "    <ns3:resultParameters>\n" + "        <ns3:parameter>\n" + "            <ns3:key>Amount</ns3:key>\n" + "            <ns3:value>" + amount.get(i) + "</ns3:value>\n" + "        </ns3:parameter>\n" + "        <ns3:parameter>\n" + "            <ns3:key>Transaction Datetime</ns3:key>\n" + "            <ns3:value>10/08/2021 20:16:49</ns3:value>\n" + "        </ns3:parameter>\n" + "    </ns3:resultParameters>\n" + "</ns3:result>";
                    String result = paymentHandler.processRefundMkoba(request, txnid.get(i), httpSession.getAttribute("username").toString(), formatDate.format(date));

                } else if (httpSession.getAttribute("ttype").equals("MKOBA") && customeQuery.get("docode").equals("WALLET2MKOBA")) {

                    String sqlRetry = "select * from tp_cbs_retry where sessionid=?  limit 1";
                    LOGGER.info(sqlRetry.replace("?","'{}'"),txnid.get(i));
                    List<Map<String, Object>> vgC2Bretry = jdbcMKOBATemplate.queryForList(sqlRetry, txnid.get(i));
                   // String acctStatus = jdbcRUBIKONTemplate.queryForObject("SELECT REC_ST FROM ACCOUNT WHERE ACCT_NO =?", String.class, vgC2Bretry.get(0).get("toaccount"));
                    //if (acctStatus.equals("A")) {
                        request = "<CBS_RETRY>\r\n<sessionid>" + vgC2Bretry.get(0).get("sessionid") + "</sessionid>\r\n" + "<msisdn>" + vgC2Bretry.get(0).get("msisdn") + "</msisdn>\r\n" + "<imsi>" + vgC2Bretry.get(0).get("imsi") + "</imsi>\r\n" + "<account>" + vgC2Bretry.get(0).get("account") + "</account>\r\n" + "<toaccount>" + vgC2Bretry.get(0).get("toaccount") + "</toaccount>\r\n" + "<amount>" + vgC2Bretry.get(0).get("amount") + "</amount>\r\n" + "<trans_type>" + vgC2Bretry.get(0).get("trans_type") + "</trans_type>\r\n" + "<processcode>" + vgC2Bretry.get(0).get("processcode") + "</processcode>\r\n" + "<msgid>" + vgC2Bretry.get(0).get("msgid") + "</msgid>\r\n" + "</CBS_RETRY>";
                        String result = paymentHandler.processRetryMkoba(request, txnid.get(i), httpSession.getAttribute("username").toString(), formatDate.format(date));
//                    } else {
//                        response = "{\"message\":\"Account: " + vgC2Bretry.get(0).get("toaccount") + " is not active thus can not be retried\"}";
//                    }
                }else if(httpSession.getAttribute("ttype").equals("AIRTEL-VIKOBA")){
                    if(customeQuery.get("txn_type").equals("WALLET2VIKOBA")){
                        String txnReceipt = findTxnReceiptByReference(txnid.get(i),httpSession.getAttribute("ttype")+"",customeQuery.get("txn_type"));
//                    LOGGER.info("checking vikoba txn receipt ... {} for reference ...{}", txnReceipt,txnid.get(i));
                        request = "<DepositRetryRequest>\n"+
                                "<ThirdPartyReference>"+txnid.get(i)+"</ThirdPartyReference>\n" +
                                "<ResultCode>0</ResultCode>\n"+
                                "<Receipt>"+txnReceipt+"</Receipt>\n"+
                                "</DepositRetryRequest>";
                    }else if(customeQuery.get("txn_type").equals("VIKOBA2WALLET")){
                        request="<getTransactionStatus>\n" +
                                    "<transactionID>"+txnid.get(i)+"</transactionID>\n" +
                                    "<mno>AIRTELMONEY</mno>\n" +
                                "\t</getTransactionStatus>";
                    }

                    String approverId = httpSession.getAttribute("username").toString();

                    String result = paymentHandler.retryAirtelVikobaTxn(request,txnid.get(i),approverId, formatDate.format(date));
                    LOGGER.info("checking retry response from ngazi cbs.. {} and its request is ... {}",result, request);

                } else {
                    String result = paymentHandler.processRefundRetryGW(request, txnid.get(i), httpSession.getAttribute("username").toString(), formatDate.format(date),request2);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR ON APROVING REFUND/RETRY: " + e.getMessage());
        }
        // });
        AuditTrails.setComments("Submit Approval of the  transactions that are on the queue");
        AuditTrails.setFunctionName("/submitApprovalForRefundRetry");
        LOGGER.info("submitApprovalForRefundRetry:->", response);
        return response;
    }

    private String findTxnReceiptByReference(String reference,String ttype, String txn_type) {
        String sql = "SELECT receiptNo from thirdpartytxns where txnid=? and txn_type=? and ttype=?";
        String result="-1";
        try{
            //LOGGER.info(sql.replace("?","'{}'"),reference,txn_type,ttype);
            result = jdbcTemplate.queryForObject(sql,new Object[]{reference,txn_type,ttype},String.class);
        }catch(Exception e){
            LOGGER.info("");
        }
        return result;
    }

    @RequestMapping(value = "/confirmBulkTxns", method = RequestMethod.GET)
    public String confirmBulkTxns(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, Model model) {
        System.out.println("TXNIDS [" + customeQuery.get("txnid") + "]");
        System.out.println(reconRepo.getConfirmCBSBulk(customeQuery.get("txnid")).toString());
        model.addAttribute("cbsTxns", reconRepo.getConfirmCBSBulk(customeQuery.get("txnid")));
        model.addAttribute("thirdPartTxns", reconRepo.getConfirmThirdPartyBulk(customeQuery.get("txnid")));
        AuditTrails.setComments("Confirm Bulk Transaction by emails from thirdparty side");
        AuditTrails.setFunctionName("/confirmBulkTxns");
        return "pages/modals/confirmBulkTransactions";
    }

    /*
    confirm transaction on CBS SIDE
     */
    @RequestMapping(value = "/confirmTransaction")
    public String confirmTransactionOnCBS(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, Model model) {
        if (customeQuery.get("txn_type") != null && customeQuery.get("txn_type").equals("LUKUEX")) {
            model.addAttribute("cbsTxns", reconRepo.getConfirmationOnSuspeCBS(customeQuery.get("txnid")));
            model.addAttribute("thirdPartTxn", reconRepo.getConfirmationThirdParty(customeQuery.get("txnid")));
            return "pages/modals/confirmSuspeTransactionLUKU";
        } else {
            model.addAttribute("cbsTxns", reconRepo.getConfirmationCBS(customeQuery.get("txnid")));
            model.addAttribute("thirdPartTxn", reconRepo.getConfirmationThirdParty(customeQuery.get("txnid")));
            AuditTrails.setComments("Confirm  Transaction on core banking side and on third party side");
            AuditTrails.setFunctionName("/confirmTransaction");
            return "pages/modals/confirmTransaction";
        }
    }

    /*
    confirm transaction on CBS SIDE
     */
    @RequestMapping(value = "/initiateAmbiguousTransaction")
    public String initiateAmbiguousTransaction(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, Model model) {
        model.addAttribute("cbsTxns", reconRepo.getConfirmationCBS(customeQuery.get("txnid")));
        model.addAttribute("thirdPartTxn", reconRepo.getConfirmationThirdParty(customeQuery.get("txnid")));
        AuditTrails.setComments("initiate ambiguous  Transaction on core banking side or  on third party side");
        AuditTrails.setFunctionName("/initiateAmbiguousTransaction");
        return "pages/modals/initiateAmbiguousTransaction";
    }

    @RequestMapping(value = "/initiateRetryRefundPost", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JsonResponse initiateRetryRefundPost(RetryRefundRequest retryReq, @RequestParam(value = "txnid", required = false) List<String> txnId, BindingResult result, HttpSession session, Model model) {
        JsonResponse respone = new JsonResponse();
        String txnids = "''";
        for (String item : txnId) {
            txnids += ",'" + item + "'";
        }
        AuditTrails.setComments("initiate refund/Retry  Transaction with references: " + txnId.toString());
        AuditTrails.setFunctionName("/initiateRetryRefundPost");
        return respone;

    }

    @RequestMapping(value = "/sendConfirmationEmail")
    public String sendConfirmationEmail(HttpServletRequest request, HttpSession session, Model model) {
        return "pages/modals/sendConfirmationEmail";
    }

    @RequestMapping(value = "/sendConfirmationEmailPost", method = RequestMethod.POST)
    @ResponseBody
    public String sendConfirmationEmail(@RequestParam(value = "txnid", required = false) List<String> txnid, @RequestParam(value = "sourceaccount", required = false) List<String> sourceaccount, @RequestParam(value = "destinationaccount", required = false) List<String> destinationaccount, @RequestParam(value = "amount", required = false) List<String> amount, @RequestParam(value = "txndate", required = false) List<String> txndate, @RequestParam(value = "txn_status", required = false) List<String> txn_status, HttpSession session, Model model) {
        JsonResponse respone = new JsonResponse();
        List<Map<String, Object>> transactions = new ArrayList<Map<String, Object>>();
        List<String> key = Arrays.asList("txnid,sourceaccount,destinationaccount,amount,txndate".split(","));
        List<String> values = null;

        //test email sends
        System.out.println("MNO SET: " + session.getAttribute("mno"));
        Mail mail = settingRepo.getMails(String.valueOf(session.getAttribute("mno")));
        System.out.println("MAILS:" + mail);
        //get the mail address from the table based on the mno
        mail.setFrom("melleji.mollel@tpbbank.co.tz");//replace with your desired email
        mail.setSubject("transaction confirm test");
        Map<String, Object> model2 = new HashMap<>();
        model2.put("name", "Melleji Mollel");
        model2.put("location", "TPB Bank Plc");
        model2.put("sign", "Business Solution Analyst");
        mail.setProps(model2);
        String senderName = session.getAttribute("username").toString();
        System.out.println("MAILS: " + mail.toString());
        emailService.sendEmail(mail, transactions, "TPB", "B2C", senderName);
        respone.setValidated(true);
        respone.setMessage("success");
        System.out.println(respone.toString());
        AuditTrails.setComments("Sending confirmation email to thirdparty: mail from" + mail.getMailTo() + " mail to: " + mail.getMailTo());
        AuditTrails.setFunctionName("/sendConfirmationEmailPost");
        return "Email Successfully Sent";
    }
//solve ambiguous of bulk transactions

    @RequestMapping(value = "/initiateBulkAmbiguousTxns", method = RequestMethod.POST)
    @ResponseBody
    public String bulkAmbiguousTransactions(@RequestParam(value = "txnid", required = false) List<String> txnid, @RequestParam(value = "identifier", required = false) List<String> identifier, @RequestParam Map<String, String> customeQuery, @RequestParam(value = "thirdpartyTxndate", required = false) List<String> txndate, HttpSession session, Model model) {
        String query = "";
        List<Map<String, Object>> transactions = new ArrayList<>();
        List<String> key = Arrays.asList("txnid,txndate".split(","));
        String ambiguousType = customeQuery.get("ambiguousOption");
        List<String> identity;
        switch (ambiguousType) {
            case "1":
                /*MATCH THIRD PARTY Transaction DATE WITH CBS DATE
                 */
                query = "update thirdpartytxns set txndate=(select txndate from cbstransactiosn where txnid=? limit 1) where txnid=?";
                for (int i = 0; i < txnid.size(); i++) {
                    System.out.println("Setting thirpartydate date ==:" + Arrays.toString(txndate.get(i).split(",")));

                    List<String> values = Arrays.asList((txnid.get(i) + "," + txnid.get(i)).split(","));
                    Map<String, Object> map2 = combineListsIntoOrderedMap(key, values);
                    transactions.add(map2);
                }
                AuditTrails.setComments("Match third party transaction date with core banking date: reference" + txnid.toString());
                AuditTrails.setFunctionName("/initiateBulkAmbiguousTxns");
                break;
            case "2":
                /*MATCH CBS Transaction DATE with THIRD PARTY DATE
                 */
                query = "update cbstransactiosn set txndate=(select txndate from thirdpartytxns where txnid=? limit 1) where txnid=?";
                for (int i = 0; i < txnid.size(); i++) {
                    System.out.println("Setting cbs date ==:" + Arrays.toString(txndate.get(i).split(",")));
                    List<String> values = Arrays.asList((txnid.get(i) + "," + txnid.get(i)).split(","));
                    Map<String, Object> map2 = combineListsIntoOrderedMap(key, values);
                    transactions.add(map2);

                }
                AuditTrails.setComments("Match core banking transaction date with third party date: reference" + txnid.toString());
                AuditTrails.setFunctionName("/initiateBulkAmbiguousTxns");
                break;
            case "3":
                /*SET THIRD PARTY Transaction as Success
                 */
                identity = identifier;
                //check exists on cbs
                if (identity.get(0).contains("existOnCBS")) {
                    query = "INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, terminal, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name, pan, identifier) SELECT txn_type, ttype, txnid, txndate, sourceaccount, txnid, amount, charge, description, currency, txn_status, terminal, destinationaccount, sourceaccount, txn_status, post_balance, prevoius_balance, 'From Rubikon confirmed on mno side', pan, dr_cr_ind from cbstransactiosn where  txnid=? or txnid=?";
                }
                //check  exists on  thirdparty
                if (identity.get(0).contains("existOnThirdParty")) {
                    query = "update thirdpartytxns set mnoTxns_status='success' where  txnid=? or txnid=?";
                }
                if (identity.get(0).contains(query)) {
                    //check if it exists on cbs or thirdparty
                    System.out.println("transaction exists on cbs side");
                }
                for (int i = 0; i < txnid.size(); i++) {
                    List<String> values = Arrays.asList((txnid.get(i) + "," + txnid.get(i)).split(","));
                    Map<String, Object> map2 = combineListsIntoOrderedMap(key, values);
                    transactions.add(map2);
                }
                AuditTrails.setComments("Set Transaction as success on thirdparty: reference" + txnid.toString());
                AuditTrails.setFunctionName("/initiateBulkAmbiguousTxns");
                break;
            case "4":
                /*SET THIRD PARTY Transaction as Reversed
                 */
                identity = identifier;
                //check exists on cbs
                if (identity.get(0).contains("existOnCBS")) {
                    query = "INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, terminal, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name, pan, identifier) SELECT txn_type, ttype, txnid, txndate, sourceaccount, txnid, amount, charge, description, currency, txn_status, terminal, destinationaccount, sourceaccount, 'Reversed As confirmed from MNO', post_balance, prevoius_balance, 'From Rubikon confirmed on mno side', pan, dr_cr_ind from cbstransactiosn where txnid=? OR txnid=?";
                }
                //check  exists on  thirdparty
                if (identity.get(0).contains("existOnThirdParty")) {
                    query = "update thirdpartytxns set mnoTxns_status='Reversed' where    txnid=? OR txnid=? ";
                }
                if (identity.get(0).contains(query)) {
                    //check if it exists on cbs or thirdparty
                    System.out.println("transaction exists on cbs side");
                }
                for (int i = 0; i < txnid.size(); i++) {
                    List<String> values = Arrays.asList((txnid.get(i) + "," + txnid.get(i)).split(","));
                    Map<String, Object> map2 = combineListsIntoOrderedMap(key, values);
                    transactions.add(map2);
                }
                AuditTrails.setComments("Set Transaction as Failed/Reversed on thirdparty: reference" + txnid.toString());
                AuditTrails.setFunctionName("/initiateBulkAmbiguousTxns");
                break;
            case "5":
                /*Cash movement on third party  Side
                 */
                identity = identifier;
                //check exists on cbs
                if (identity.get(0).contains("existOnCBS")) {
                    query = "update cbstransactiosn set txn_status='Cash Movement Between Accounts' where  txnid=? OR txnid=?";
                }
                //check  exists on  thirdparty
                if (identity.get(0).contains("existOnThirdParty")) {
                    query = "update thirdpartytxns set mnoTxns_status='Cash Movement Between Accounts' where   txnid=? OR txnid=? ";
                }
                for (int i = 0; i < txnid.size(); i++) {
                    List<String> values = Arrays.asList((txnid.get(i) + "," + txnid.get(i)).split(","));
                    Map<String, Object> map2 = combineListsIntoOrderedMap(key, values);
                    transactions.add(map2);
                }
                AuditTrails.setComments("Set Transaction as Cash Movement : reference" + txnid.toString());
                AuditTrails.setFunctionName("/initiateBulkAmbiguousTxns");
                break;
            case "6":
                /*Wrong destination Refunded by Third party
                 */
                query = "update thirdpartytxns set mnoTxns_status='Wrong destination Refunded by Third party' where   txnid=? or txnid=? ";
                for (int i = 0; i < txnid.size(); i++) {
                    List<String> values = Arrays.asList((txnid.get(i) + "," + txnid.get(i)).split(","));
                    Map<String, Object> map2 = combineListsIntoOrderedMap(key, values);
                    transactions.add(map2);
                }
                AuditTrails.setComments("Set Transaction as sent to wrong destination and it has been reversed : reference" + txnid.toString());
                AuditTrails.setFunctionName("/initiateBulkAmbiguousTxns");
                break;

        }
        return Arrays.toString(reconRepo.updateBulkAmbiguousTxns(query, transactions));
    }

    Map<String, Object> combineListsIntoOrderedMap(List<String> keys, List<String> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    @RequestMapping(value = "/processRefundRetry", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processRefundRetry(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, Model model) {
        System.out.println("CUSTOM QUERY: " + customeQuery.toString());
        if (!customeQuery.isEmpty()) {
            if (customeQuery.get("ttype").equals("C2B") && customeQuery.get("identifier").equals("Retry")) {
                /*
            process a retry transactions
                 */
            }
            if (customeQuery.get("ttype").equals("C2B") && customeQuery.get("identifier").equals("Reverse")) {
                /*
            process a reverse transactions
                 */
            }
            if (customeQuery.get("ttype").equals("B2C") && customeQuery.get("identifier").equals("Refund")) {
                /*
            process a refund transactions
                 */
                System.out.println("refund: " + customeQuery.toString());
            }
            if (customeQuery.get("ttype").equals("B2C") && customeQuery.get("identifier").equals("WrongTransfer")) {
                /*
            process a refund transactions
                 */
            }
        }
        AuditTrails.setComments("Process Refund/Retry ");
        AuditTrails.setFunctionName("/processRefundRetry");
        return "{}";
    }

    @RequestMapping(value = {"/getReconDashboard"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReconFormJsonResponse getReconData(@Valid ReconForm reconForm, BindingResult result) {
        ReconFormJsonResponse respone = new ReconFormJsonResponse();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            respone.setReconForm(reconForm);
            List<ReconDashboard> reconDashboard = reconRepo.getReconDashboard(reconForm.getTxnDate(), reconForm.getTxnType());
            List<Map<String, Object>> dashboardTabs = reconRepo.getReconDashboardTabs(reconForm.getTxnType());
            List<Map<String, Object>> txnTypes = reconRepo.getTxnsType(reconForm.getTxnType());
            int rowspan = txnTypes.size() + 1;
            httpSession.setAttribute("txndate", reconForm.getTxnDate());
            httpSession.setAttribute("ttype", reconForm.getTxnType());
            httpSession.setAttribute("txntypes", txnTypes);
            httpSession.setAttribute("rowspan", rowspan);
            httpSession.setAttribute("reconDashboardTabs", dashboardTabs);
//            System.out.println("dashboard data: "+reconDashboard);
            httpSession.setAttribute("reconDashboardData", reconDashboard);
            //Re reconciliation based on the type of transaction selected
            pushThread.setTxndate(reconForm.getTxnDate());
            pushThread.setTxnType(reconForm.getTxnType());
            pushThread.setUsername(String.valueOf(httpSession.getAttribute("username")));
            exec.execute(pushThread);
            AuditTrails.setComments("View reconciliation dashboard as for date: " + reconForm.getTxnDate() + " recon Type: " + reconForm.getTxnType());
            AuditTrails.setFunctionName("/getReconDashboard");
        }
        return respone;
    }

    @RequestMapping(value = "/reconExceptionReport")
    public String reconExceptionReport(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "EXCEPTION REPORTS[" + session.getAttribute("txndate") + "]");
        model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "pages/reconExceptionReport";
    }

    @RequestMapping(value = "/getReconExceptionReportsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getReconExceptionReportsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String exceptionReport = customeQuery.get("exceptionReport");
        String fromDate = customeQuery.get("fromDate") + " 00:00:00";
        String toDate = customeQuery.get("toDate") + " 23:59:59";
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        System.out.println("EXCEPTION REPORT CODE: " + exceptionReport + " FROM DATE:" + fromDate + " TODATE:" + toDate);
        return reconRepo.getReconExceptionReportsAjax(exceptionReport, fromDate, toDate, session.getAttribute("ttype").toString(), session.getAttribute("txndate").toString(), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    @RequestMapping(value = "/fireBulkTransactions")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    public String fireBulkTransactions(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        // model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "maitools/bulkTransactions";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/fireBulkTransactionsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireBulkTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
        String txnType = customeQuery.get("txnType");
        String ttype = customeQuery.get("ttype");
        String txnStatus = customeQuery.get("txnStatus");
        String amount = customeQuery.get("amount");
        String reference = customeQuery.get("reference");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return reconRepo.getBulkTransactionsAjax(ttype, txnType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireLukuGwTransactions")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    public String fireReconExceptionReport(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        // model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "maitools/lukuTransactionsGW";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/fireLukuGwTransactionsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireLukuGwTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
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

        return reconRepo.getLukuGwTransactionsAjax(txnType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireLukuSendSMS", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireLukuSendSMS(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        String txnid = customeQuery.get("txnid");
        String reportFormat = customeQuery.get("reportFormat");
        LOGGER.info("sEND SMS txid:{} -  type:{}", txnid, reportFormat);
        String response = reconRepo.procSendLukuSMS(txnid, reportFormat);
        LOGGER.info("sEND SMS txid:{} -  type:{}, Response: {}", txnid, reportFormat, response);

        return response;
    }

    @RequestMapping(value = "/fireSuspiciousTransactions")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    public String fireSuspiciousTransactions(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("amount", "400000");
        // model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "maitools/SuspiciousTransactions";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/fireSuspiciousTransactionsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireSuspiciousTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
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

        return reconRepo.getSuspiciousGwTransactionsAjax(amount, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireSwiftOutReport")
    @PreAuthorize("hasAuthority('/fireSwiftSTPReport')")
    public String fireSwiftOutReport(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("amount", "400000");
        return "maitools/swiftOutReport";
    }

    @RequestMapping(value = "/fireSwiftOutReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    // @PreAuthorize("hasAuthority('/fireSwiftSTPReport')")
    @ResponseBody
    public String fireSwiftOutReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
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
        try {
            return reconRepo.getSwiftOutReportAjax(txnType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
        } catch (Exception es) {
            es.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/fireSwiftSTPReport")
    @PreAuthorize("hasAuthority('/fireSwiftSTPReport')")
    public String fireSwiftSTPReport(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("amount", "400000");
        return "maitools/swiftSTPReport";
    }

    @RequestMapping(value = "/fireSwiftSTPReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireSwiftSTPReport')")
    @ResponseBody
    public String fireSwiftSTPReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
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
        return reconRepo.getSwiftSTPReportAjax(txnType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireSwiftRetunedReport")
    @PreAuthorize("hasAuthority('/fireSwiftSTPReport')")
    public String fireSwiftRetunedReport(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("amount", "400000");
        return "maitools/swiftReturnedReport";
    }

    @RequestMapping(value = "/fireSwiftRetunedReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireSwiftSTPReport')")
    @ResponseBody
    public String fireSwiftRetunedReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
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
        return reconRepo.getSwiftReturnedReportAjax(fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/fireTestAlert")
    @ResponseBody
    public String fireTestAlert(Model model, HttpSession session) throws ParseException {
        try {
            return transferService.runTriggerAlert();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }      //  return "OK";
    }

    @RequestMapping(value = "/fireCardIssue")
    @PreAuthorize("hasAuthority('/fireCardIssue')")
    public String fireCardIssue(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("heading", "CARD ISSUE REPORT");
        return "maitools/cardIssue";
    }

    @RequestMapping(value = "/fireInstantCardIssue")
    @PreAuthorize("hasAuthority('/fireCardIssue')")
    public String fireInstantCardIssue(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("heading", "CARD ISSUE REPORT");
        return "maitools/instantCardIssue";
    }

    @RequestMapping(value = "/fireCardIssueAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireCardIssue')")
    @ResponseBody
    public VisaCardJsonResponse fireCardIssueAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        VisaCardJsonResponse response = new VisaCardJsonResponse();

        String fromDate = customeQuery.get("fromDate");
        String todate = customeQuery.get("toDate");
        String txnStatus = customeQuery.get("txnStatus");
        try {
            List<Map<String, Object>> records = reconRepo.getfireCardIssueAjax(txnStatus, fromDate, todate);
            response.setStatus("SUCCESS");
            response.setResult(records);

            return response;

        } catch (Exception es) {
            es.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/fireMnoBalance")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    public String fireMnoBalance(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.yesterdayDefault());
        model.addAttribute("endDate", DateUtil.yesterdayDefault());
        // model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "maitools/mnoBalance";
    }

    @RequestMapping(value = "/fireMnoBalanceJasper")
    @ResponseBody
    public String fireMnoBalanceJasper(@RequestParam Map<String, String> customeQuery, Model model, HttpServletResponse response) throws Exception {
        LOGGER.info("fireMnoBalanceJasper: {}", customeQuery);

        return jasperService.mnoBalance(customeQuery, "html", response, "mno_balance");
    }

    @RequestMapping(value = "/fireMnoBalanceJasperPost")
    public String fireMnoBalanceJasperPost(@RequestParam Map<String, String> customeQuery, Model model, HttpServletResponse response) throws Exception {
        LOGGER.info("fireMnoBalanceJasper: {}", customeQuery);

        return jasperService.mnoBalance(customeQuery, customeQuery.get("exportType"), response, "mno_balance");
    }

    @RequestMapping(value = "/fireMnoBalanceAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireMnoBalanceAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String txnType = customeQuery.get("txnType");
        String ttype = customeQuery.get("ttype");
        String txnDate = customeQuery.get("txnDate");
        LOGGER.info("{}", customeQuery);
        String response = "-1";
        try {
            reportService.sysncCBSBalance(txnDate);
            response = "{\"status\":\"OK\"}";
        } catch (Exception ex) {
            LOGGER.info(null, ex);

        }
        LOGGER.info("FOA#: ", response);
        return response;

    }

    @RequestMapping(value = "/fireMnoBalanceMNOAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireMnoBalanceMNOAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String txnType = customeQuery.get("txnType");
        String ttype = customeQuery.get("ttype");
        String txnDate = customeQuery.get("txnDate");
        LOGGER.info("MNO{}", customeQuery);
        String response = "-1";
        try {
            reportService.sysncMNOBalance(txnDate);
            response = "{\"status\":\"OK\"}";
        } catch (Exception ex) {
            LOGGER.info(null, ex);

        }
        LOGGER.info("FOA#: {}", response);
        return response;

    }

    @RequestMapping(value = "/fireMkobaSolution")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    public String fireMkobaSolution(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        // model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "maitools/MkobaSolution";
    }

    @RequestMapping(value = "/fireMkobaSolutionAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireMkobaSolutionAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String account = customeQuery.get("account");
        String ttype = customeQuery.get("ttype");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        try {
            String accountStr = reconRepo.getMKOBAAccountGateway(account);
            return reconRepo.getfireMkobaSolutionAjax(ttype, accountStr, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
        } catch (Exception es) {
            es.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/fireMkobaSolutionBalanceAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireMkobaSolutionBalanceAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String account = customeQuery.get("account");

        try {
            String accountStr = reconRepo.getMKOBAAccountGateway(account);
            BigDecimal cbsBalance = reconRepo.getMKOBAAccountCBSBalance(accountStr);
            BigDecimal mkobaBalance = reconRepo.getMKOBAAccountGatewayBalance(accountStr);
            return "{\"cbsBalance\":" + cbsBalance + ", \"mkobaBalance\":" + mkobaBalance + ",\"accountStr\":\"" + accountStr + "\",\"account\":\"" + account + "\"}";
        } catch (Exception es) {
            es.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/fireCusTomerService")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    public String fireCusTomerService(Model model, HttpSession session) {
        model.addAttribute("startDate", DateUtil.now("yyyy-MM-dd"));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        // model.addAttribute("exceptions", reconRepo.getReconExceptionReportSetup());
        return "maitools/customerService";
    }

    @RequestMapping(value = "/fireCusTomerServiceAjax", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireCusTomerServiceAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpServletResponse resp, HttpSession session) {
        String account = customeQuery.get("account");

        try {
            String response = "Customer not available";
            response = jasperService.procCustomerinformation(customeQuery, "html", resp, "customer_info");
            return response;
        } catch (Exception es) {
            es.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/fireCusTomerServicePrint/{accountx}/{startDate}/{endDate}/{ttypex}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireCusTomerServicePrint(@PathVariable("accountx") String accountx,@PathVariable("startDate") String startDate,@PathVariable("endDate")  String endDate,@PathVariable("ttypex")  String ttypex,@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpServletResponse resp, HttpSession session) {
        String account = accountx;
        String ttype = ttypex;
        LOGGER.info("Print Balance/statement for customeQuery:{}",customeQuery);
        LOGGER.info("Print Balance/statement for accountx:{}",accountx);
        LOGGER.info("Print Balance/statement for startDate:{}",startDate);
        LOGGER.info("Print Balance/statement for endDate:{}",endDate);
        LOGGER.info("Print Balance/statement for ttypex:{}",ttypex);
        customeQuery.put("account", accountx);
        customeQuery.put("startDate", startDate);
        customeQuery.put("endDate", endDate);
        customeQuery.put("ttype", ttypex);
        customeQuery.put("reference", "REF"+String.valueOf(System.currentTimeMillis()));
        customeQuery.put("branchCode",session.getAttribute("branchCode") + "");
        customeQuery.put("username", session.getAttribute("username") + "");

        try {
            String response = "Customer not available";
            if (ttype.equals("X1")) {
                LOGGER.info("Print Balance for account:{}",account);
                transferService.procChargForCustomerServic(customeQuery, "pdf", resp);
            }else  if (ttype.equals("X2")) {
                LOGGER.info("Print statement for account:{}",account);
                transferService.procChargForCustomerServic(customeQuery, "pdf", resp);
            }
            return response;
        } catch (Exception es) {
            LOGGER.info(es.getMessage(),es);
            return null;
        }
    }

    @RequestMapping(value = "/fireCusTomerServiceCalculate", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAuthority('/fireLukuGwTransactions')")
    @ResponseBody
    public String fireCusTomerServiceCalculate(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpServletResponse resp, HttpSession session) {
        String account = customeQuery.get("account");
        String ttype = customeQuery.get("ttype");
        LOGGER.info("Print Calculate for customeQuery:{}",customeQuery);
        try {
            String response = "{\"status\":96,\"msg\":\"Error occured, cbs checking....\"}";
            if (ttype.equals("X1")) {
                LOGGER.info("calculate Balance for account:{}",account);
                response = jasperService.procCustomerinformationBalanceSize(customeQuery, "pdf", resp, "balance_"+account+"_");
                response = "{\"status\":0,\"msg\":\""+response+"\"}";
            }else  if (ttype.equals("X2")) {
                                LOGGER.info("calculate stamement for account:{}",account);
                                response = jasperService.procCustomerinformationStatementSize(customeQuery, "pdf", resp, "statement_"+account+"_");
                                           response = "{\"status\":0,\"msg\":\""+response+"\"}";
                 }
                LOGGER.info("response:{}",response);
            return response;
        } catch (Exception es) {
            es.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/generalReconciliationDashboard")
    public String generalReconciliationDashboard(Model model, HttpSession session) {
        AuditTrails.setComments("View General Reconciliation Dashboard");
        AuditTrails.setFunctionName("/generalReconciliationDashboard");
        model.addAttribute("pageTitle", "DAILY GENERAL RECONCILIATIONS");
        model.addAttribute("reconConfigs", reconRepo.getReconConfigs(session.getAttribute("roleId").toString()));
        return "pages/generalReconciliationDashboard";
    }

    @RequestMapping(value = "/cummulativeReconciliationReport", method = RequestMethod.POST)
    @ResponseBody
    public String cummulativeReconciliationReport(@RequestParam Map<String, String> customeQuery, HttpServletResponse res, HttpSession session) throws IOException {
        try {
            LOGGER.info(customeQuery.toString());
            httpSession.setAttribute("txnType",customeQuery.get("txnType"));
            httpSession.setAttribute("ttype", customeQuery.get("txnType"));
            String printedBy = session.getAttribute("username").toString();
            String reportFormat = customeQuery.get("reportFormat");
            String glAcct = reconRepo.getReconGl(customeQuery.get("txnType"));
            httpSession.setAttribute("glAcct",glAcct);
            httpSession.setAttribute("txndate",customeQuery.get("reconDate"));
            List<Map<String, Object>> generalReconTabs = reconRepo.getReconDashboardTabs(customeQuery.get("txnType"));
            httpSession.setAttribute("generalReconTabs",generalReconTabs);
            String result = reconRepo.getReconSummaryReport(reportFormat, res, DateUtil.now("yyyyMMddHHmm") + "ReconReport", customeQuery.get("reconDate"), printedBy);
            httpSession.setAttribute("jasperDisplay",result);
            httpSession.setAttribute("showDownload",true);

            return result;
        } catch (Exception dae) {
            LOGGER.info("Exception encountered...{}", dae);
            return null;
        }

    }


    @RequestMapping(value = "/fireDownlodGeneralReport", method = RequestMethod.GET)
    @ResponseBody
    public String fireDownlodGeneralReport(@RequestParam Map<String, String> customeQuery, HttpServletResponse res, HttpSession session) throws IOException {
        try {

            String reconDate = customeQuery.get("reconDate");
            String printedBy = session.getAttribute("username").toString();
            String reportFormat = customeQuery.get("reportFormat");
            return reconRepo.getReconSummaryReport(reportFormat, res, DateUtil.now("yyyyMMddHHmm") + "ReconReport", reconDate, printedBy);
        } catch (Exception dae) {
            LOGGER.info("Exception encountered...{}", dae);
            return null;
        }

    }


    @RequestMapping(value = "/fireCRStatementsTxnsNotInLedger")
    public String fireCRStatementsTxnsNotInLedger(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        String reconDate = customeQuery.get("reconDate");
        String exceptionType = customeQuery.get("exceptionType");
//        LOGGER.info("Exception type selected.... {} AND reconDate.. {}", customeQuery.get("exceptionType"), reconDate);
        //httpSession.setAttribute("exceptionType",exceptionType);
        model.addAttribute("exceptionType", exceptionType);
        model.addAttribute("pageTitle", "BANK STATEMENT CREDIT TRANSACTIONS NOT IN LEDGER BEFORE :.......[ " + reconDate + " ]");
        return "pages/tipsRecon/crStatementsTxnsNotInLedger";
    }

    @RequestMapping(value = "/fireDRStatementsTxnsNotInLedger")
    public String fireDRStatementsTxnsNotInLedger(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        String reconDate = customeQuery.get("reconDate");
        String exceptionType = customeQuery.get("exceptionType");
//        LOGGER.info("Exception type selected.... {} AND reconDate.. {} and correspondingGL ... {}", customeQuery.get("exceptionType"), reconDate, httpSession.getAttribute("glAcct"));
        httpSession.setAttribute("exceptionType",exceptionType);
        model.addAttribute("exceptionType", exceptionType);
        model.addAttribute("pageTitle", "BANK STATEMENT DEBIT TRANSACTIONS NOT IN LEDGER BEFORE :.......[ " + reconDate + " ]");
        return "pages/tipsRecon/drStatementsTxnsNotInLedger";
    }

    @RequestMapping(value = "/fireCRLegderTxnsNotInStatements")
    public String fireCRLegderTxnsNotInStatements(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        String reconDate = customeQuery.get("reconDate");
        String exceptionType = customeQuery.get("exceptionType");
//        LOGGER.info("Exception type selected.... {} AND reconDate.. {}", customeQuery.get("exceptionType"), reconDate);
        httpSession.setAttribute("exceptionType",exceptionType);
        model.addAttribute("exceptionType", exceptionType);
        model.addAttribute("pageTitle", "BANK LEGDER CREDIT  TRANSACTIONS NOT IN STATEMENT BEFORE :.......[" + reconDate + "]");
        return "pages/tipsRecon/crLegderTxnsNotInStatements";
    }

    @RequestMapping(value = "/fireDRLegderTxnsNotInStatements")
    public String fireDRLegderTxnsNotInStatements(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        String reconDate = customeQuery.get("reconDate");
        String exceptionType = customeQuery.get("exceptionType");
//        LOGGER.info("Exception type selected.... {} AND reconDate.. {}", customeQuery.get("exceptionType"), reconDate);
        httpSession.setAttribute("exceptionType",exceptionType);
        model.addAttribute("exceptionType", exceptionType);
        model.addAttribute("pageTitle", "BANK LEDGER DEBIT TRANSACTIONS NOT IN STATEMENT BEFORE :.......[" + reconDate + "]");
        return "pages/tipsRecon/drLegderTxnsNotInStatements";
    }

    @RequestMapping(value = "/fireGetTipsTxnsReconReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireGetTipsTxnsReconReport(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String exceptionType = customeQuery.get("exceptionType");
        String reconDate = customeQuery.get("reconDate");
//        LOGGER.info("Tips transactions with recon date:.. {} and exceptionType: ... {} and glAcct... {}", reconDate, exceptionType, httpSession.getAttribute("glAcct"));
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return reconRepo.fireGetTipsTxnsReconReport(exceptionType, reconDate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "/fireGeneralTxnReport")
    public String tipsTransactionsReport(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("pageTitle", "GENERAL TRANSACTIONS REPORT");
        model.addAttribute("startDate",DateUtil.previosDay(10));
        model.addAttribute("endDate",DateUtil.tomorrow());
        return "pages/tipsRecon/generalTransactionsReport";
    }


    @PostMapping(value = "/fireGeneralTxnReportAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireGeneralTxnReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) throws Exception {

        String direction = customeQuery.get("ledgerOrStatement");
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        GeneralJsonResponse jr = new GeneralJsonResponse();
        List<Map<String,Object>> finalRes = reconRepo.fireGeneralTxnReportAjax(direction,fromDate,toDate);
        jr.setStatus("SUCCESS");
        jr.setResult(finalRes);
        return jr;
    }

    @RequestMapping(value = "/fireConfirmBulkTipsTxns", method = RequestMethod.POST)
    public String fireConfirmBulkTipsTxns(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, Model model) {
//        LOGGER.info("TIPS transReferences..... {} and corresponding_GL... {}", customeQuery.get("txnid"), httpSession.getAttribute("glAcct"));
        String exceptionType = customeQuery.get("exceptionType");
        String  txnType = session.getAttribute("txnType").toString();
        String  glAcct = session.getAttribute("glAcct").toString();
            //Search transactions on thirdpart txns
        List<Map<String,Object>> thirdpartTxns = reconRepo.fireConfirmBulkTipsTxns(customeQuery.get("txnid"),txnType);
        LOGGER.info("thirdpartTxns check... {}",thirdpartTxns);
            model.addAttribute("statementTxns", thirdpartTxns);
            //search on cbs
        List<Map<String,Object>> cbsTxns =  reconRepo.fireConfirmBulkGeneralTxnsCBS(customeQuery.get("txnid"),glAcct);
        LOGGER.info("cbsTxns check... {}",cbsTxns);
        model.addAttribute("ledgerTxns",cbsTxns);
        model.addAttribute("exceptionType", exceptionType);
        AuditTrails.setFunctionName("/fireConfirmBulkTipsTxns");
        return "pages/tipsRecon/modals/confirmTIPSBulkTransactions";
    }


    @RequestMapping(value = "/fireSolveTIPSBulkAmbiguousTxns", method = RequestMethod.POST)
    @ResponseBody
    public String solveTIPSBulkAmbiguousTxns(@RequestParam(value = "txnid", required = false) List<String> txnid, @RequestParam Map<String, String> customeQuery, HttpSession session, Model model) {

        String inLists = txnid.stream().collect(Collectors.joining("','", "'", "'"));

        String ambiguousType = customeQuery.get("tipsAmbiguousOption");
        String exceptionType = customeQuery.get("exceptionType");
        String resorvedBy = session.getAttribute("username").toString();
        String recon = session.getAttribute("username").toString();
        String reconDate = session.getAttribute("txndate").toString();

//        LOGGER.info("received references.. {} and ambigous option is... {} and exceptionType .... {}", inLists, ambiguousType,exceptionType);
        return reconRepo.solveTIPSBulkAmbiguousTxns(resorvedBy, exceptionType, ambiguousType, inLists, reconDate);
    }

    @PostMapping(value = "/fireDownloadCBSDataForRecon", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireDownloadCBSDataForRecon(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) throws Exception {

        String mno = customeQuery.get("mno");
        String txnType = customeQuery.get("txnType");
        String txnDate = customeQuery.get("txnDate");
        LOGGER.info("Going to download cbs data with params mno... {}, txnType...{}, and txnDate ... {}", mno, txnType, txnDate);
       return reconRepo.fireDownloadCsbDataForRecontAjax(mno,txnType,txnDate);
    }
//    @RequestMapping(value = "/fireSimulateXBatch", method = RequestMethod.GET)
//    @ResponseBody
//    public String simulateXBatch( @RequestParam Map<String, String> customeQuery) {
//        LOGGER.info("Request:{}",customeQuery);
//        String batchId = customeQuery.get("batchId");
//        String startId = customeQuery.get("startId");
//        String batchLength = customeQuery.get("batchLength");
//        try {
//            return reportService.sysncPush(batchId, startId, batchLength);
//
//        }catch (Exception e){
//            LOGGER.info("{}",e);
//            e.printStackTrace();
//            return null;
//        }
//    }

}
