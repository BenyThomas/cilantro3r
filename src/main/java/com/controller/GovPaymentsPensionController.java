/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.InstitutionDetails;
import com.core.event.AuditLogEvent;
import com.entities.LoanRepaymentReq;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.helper.DateUtil;
import com.repository.GovExpenditurePensionRepo;
import com.repository.PensionRepo;
import com.repository.User_M;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import philae.api.BnUser;
import philae.api.UsRole;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class GovPaymentsPensionController {

    @Autowired
    GovExpenditurePensionRepo govExpenditurePensionRepo;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    User userController;

    @Autowired
    User_M user_m;

    @Autowired
    HttpSession httpSession;

    @Autowired
    PensionRepo pensionRepo;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GovPaymentsPensionController.class);

    /*
    EXPENDITURE PENSION DASHBOARD
     */
    @RequestMapping(value = "/govExpenditurePensionDashboard", method = RequestMethod.GET)
    public String govExpenditurePensionDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "GOV EXPENDITURE & PENSION PAYMENTS [MODULE]");
        model.addAttribute("paymentsPermissions", govExpenditurePensionRepo.getGovExpendPensionModulePermissions("/govExpenditurePensionDashboard", session.getAttribute("roleId").toString()));
        return "modules/govExpendPension/govExpendPensionDashboard";
    }

    //EXPENDITURE PENSION PAYMENTS
    @RequestMapping(value = "/bulkGovExpenditurePensionPayments", method = RequestMethod.GET)
    public String bulkGovExpenditurePensionPayments(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        model.addAttribute("pageTitle", "BULK PAYMENTS");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";

        //get the workflows per user role.
        List<Map<String,Object>> authorizedModules = user_m.getAuthorizedModules(roleId,"/govExpenditurePensionDashboard");
        ArrayList<Integer> workFlowArray = new ArrayList<>();
        for (Map<String, Object> authorizedModule : authorizedModules) {
            workFlowArray.add((Integer) authorizedModule.get("work_flow_id"));
        }
//        LOGGER.info("Work flow array List... {}", workFlowArray);
        LOGGER.info("Workflow access allowed per GExpenditure_MODULE... {}",authorizedModules);
        httpSession.setAttribute("authorizedGPPWFArray",workFlowArray);
        model.addAttribute("workFlowArray",workFlowArray);
        model.addAttribute("authorizedModules",authorizedModules);
        model.addAttribute("roleId", roleId);
//        model.addAttribute("roles", "Validation Manager");
//        BnUser user = (BnUser) session.getAttribute("userCorebanking");
//        StringBuilder roles = new StringBuilder();
//        for (UsRole role : user.getRoles().getRole()) {
//            roles.append(role.getRole());
//            roles.append(",");
//        }
//        if (roles.length() > 0) {
//            model.addAttribute("roles", roles.substring(0, roles.length() - 1));
//        }
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/bulkGovExpenditurePensionPayments", "SUCCESS", "View bulk gov expenditure pension payments"));
        return "modules/govExpendPension/bulkPayments";
    }

    //EXPENDITURE PENSION PAYMENTS
    @RequestMapping(value = "/bulkPaymentsReport", method = RequestMethod.GET)
    public String bulkPaymentsReport(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("pageTitle", "BULK PAYMENTS REPORT");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";

        model.addAttribute("roleId", roleId);

        List<InstitutionDetails> institutions = new ArrayList<>();
        List<Map<String, Object>> institutionList = govExpenditurePensionRepo.getMuseInstitutions(branchNo);
        if (!institutionList.isEmpty())
            for (Map<String, Object> map : institutionList) {
                InstitutionDetails details = new InstitutionDetails();
                details.setAccountName((String) map.get("acct_name"));
                details.setAccountNo((String) map.get("acct_no"));
                institutions.add(details);
            }
        model.addAttribute("institutions", institutions);

        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(),
                "/museReports", "SUCCESS", "View bulk gov expenditure report"));
        return "modules/govExpendPension/bulkPaymentsReport";
    }

    @RequestMapping(value = "/replayReturn", method = RequestMethod.GET)
    public String bulkGovExpenditurePensionBatches(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest request) {
        model.addAttribute("pageTitle", "PENSION BATCHES");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";

        //get the workflows per user role.
        List<Map<String,Object>> authorizedModules = user_m.getAuthorizedModules(roleId,"/govExpenditurePensionDashboard");
        ArrayList<Integer> workFlowArray = new ArrayList<>();
        for (Map<String, Object> authorizedModule : authorizedModules) {
            workFlowArray.add((Integer) authorizedModule.get("work_flow_id"));
        }
        LOGGER.info("Workflow access allowed per Gov expenditure module... {}",authorizedModules);
        httpSession.setAttribute("authorizedGPPWFArray",workFlowArray);
        model.addAttribute("workFlowArray",workFlowArray);
        model.addAttribute("authorizedModules",authorizedModules);
        model.addAttribute("roleId", roleId);
        List<InstitutionDetails> institutions = new ArrayList<>();
//        List<Map<String, Object>> institutionList = govExpenditurePensionRepo.getMuseInstitutions(branchNo);
//        if (!institutionList.isEmpty())
//            for (Map<String, Object> map : institutionList) {
                InstitutionDetails details = new InstitutionDetails();
                details.setAccountName("PSSSF");
                details.setAccountNo("PSSSF");
                institutions.add(details);
//            }
        model.addAttribute("institutions", institutions);
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/bulkGovExpenditurePensionPayments", "SUCCESS", "View bulk gov expenditure pension payments"));
        return "modules/govExpendPension/replayReturn";
    }

    //MUSE ONLINE PAYMENTS
    @RequestMapping(value = "/onlineGovExpenditurePensionPayments", method = RequestMethod.GET)
    public String onlineGovExpenditurePensionPayments(Model model, HttpSession session,
                                                      @RequestParam Map<String, String> customeQuery,
                                                      HttpServletRequest request) {
        model.addAttribute("pageTitle", "ONLINE PAYMENTS");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        model.addAttribute("roleId", roleId);
        List<InstitutionDetails> institutions = new ArrayList<>();
        List<Map<String, Object>> institutionList = govExpenditurePensionRepo.getMuseInstitutions(branchNo);
        if (!institutionList.isEmpty())
            for (Map<String, Object> map : institutionList) {
                InstitutionDetails details = new InstitutionDetails();
                details.setAccountName((String) map.get("acct_name"));
                details.setAccountNo((String) map.get("acct_no"));
                institutions.add(details);
            }
        model.addAttribute("institutions", institutions);
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/bulkGovExpenditureOnlinePayments", "SUCCESS", "View MUSE online payments"));
        return "modules/govExpendPension/onlinePayments";
    }

    @RequestMapping(value = "/tempPensionBatches", method = RequestMethod.GET)
    public String getTempPensionBatches(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("pageTitle", "TEMPORARY PENSION BATCHES");
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        model.addAttribute("roleId", roleId);
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/bulkGovExpenditureOnlinePayments", "SUCCESS", "View MUSE online payments"));
        return "modules/govExpendPension/pensionBatches";
    }

    @RequestMapping(value = "/getTempPensionBatchesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTempPensionBatchesAjax(@RequestParam Map<String, String> params) {
        String draw = params.get("draw");
        String fromDate = params.get("fromDate");
        String toDate = params.get("toDate");
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";

        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        return pensionRepo.getTempBatchesForProcessing(fromDate, toDate, draw, start,rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = {"/getInstitutionsAccounts"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<InstitutionDetails> getInstitutionDetails(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        List<InstitutionDetails> institution = new ArrayList<>();
        String branchNo = session.getAttribute("branchCode") + "";
        if (!params.isEmpty() && params.containsKey("paymentType")) {
            if (params.get("paymentType").equals("PENSION")) {
                InstitutionDetails details = new InstitutionDetails();
                details.setAccountNo("PSSSF");
                details.setAccountName("PSSSF");
                institution.add(details);
            } else {
                List<Map<String, Object>> institutionList = govExpenditurePensionRepo.getMuseInstitutions(branchNo);
                if (!institutionList.isEmpty())
                    for (Map<String, Object> map : institutionList) {
                        InstitutionDetails details = new InstitutionDetails();
                        details.setAccountName((String) map.get("acct_name"));
                        details.setAccountNo((String) map.get("acct_no"));
                        institution.add(details);
                    }
            }
        }
        String username = session.getAttribute("username") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getPendingGovPensionBulkPayments", "SUCCESS", "View pending gov pension bulk payments"));
        return institution;
    }

    //get pending bulk payments from brinjal gateway
    @RequestMapping(value = "/getPendingBatchesPerAccountAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPendingBatchesPerAccountAjax(@RequestParam Map<String, String> params) {
        String draw = params.get("draw");
        String accountNo = params.get("institutionDetails");
        String status = params.get("status");
        String searchValue = params.get("searchValue") != null ? params.get("searchValue").trim() : "";

        return govExpenditurePensionRepo.getPendingBatchesForProcessing(accountNo, searchValue, status, draw);
    }

    //get bulk payment batches report
    @RequestMapping(value = "/getBulkPaymentsReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getBulkPaymentsReportAjax(@RequestParam Map<String, String> params) {
        String draw = params.get("draw");
        String fromDate = params.get("fromDate");
        String toDate = params.get("toDate");
        String accountNo = params.get("institutionDetails");
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";

        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        return govExpenditurePensionRepo.getBulkBatches(fromDate, toDate, accountNo, draw, start, rowPerPage, searchValue,
                columnIndex, columnName, columnSortOrder);
    }

    /*
    PREVIEW BATCH TRANSACTIONS LIST
     */
    @RequestMapping(value = "/previewSwiftMhsg")
    public String previewRTGSMessage(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", customeQuery.get("institutionName").toUpperCase() + "BATCH REF:" + customeQuery.get("reference") + " NO OF TXNS:" + customeQuery.get("noOfTxns") + " TOTAL AMOUNT:" + customeQuery.get("totalAmount"));
        model.addAttribute("institutionAcct", customeQuery.get("institutionAcct"));
        model.addAttribute("institutionName", customeQuery.get("institutionName"));
        model.addAttribute("batchReference", customeQuery.get("institutionName"));
//        model.addAttribute("swiftMessage", rtgsRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("swift_message") + "");
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/previewSwiftMsg-> preview swift generated message", "SUCCESS", "preview swift generated message"));

        return "modules/rtgs/modals/previewSwiftMsg";
    }

    @RequestMapping(value = "/previewBatchTransactionsList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String previewBatchTransactionsList(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String reference = params.get("reference");
        String account = params.get("account");
        String status = params.get("status");
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";

        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        return govExpenditurePensionRepo.previewBatchTransactionsList(reference, account, status, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/previewTempBatchTransactions", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String previewTempBatchTransactions(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String reference = params.get("reference");
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";

        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        return pensionRepo.previewTempBatchTransactions(reference, draw, start, rowPerPage, searchValue, columnIndex,
                columnName, columnSortOrder);
    }

    @RequestMapping(value = "/uploadDoc", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String uploadDoc(@RequestParam("file") MultipartFile[] files, @RequestParam Map<String, String> params,
                            HttpServletRequest request, HttpSession session, HttpServletRequest httpRequest) throws Exception {
        int result = -1;
        String reference = params.get("reference");
        //save the supporting document(s)
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                result = govExpenditurePensionRepo.uploadTransactionDocument(reference, file);
            }
        }
        return "{\"result\":" + result + "}";
    }

    @RequestMapping(value = "/paymentReports", method = RequestMethod.GET)
    public String workflowPayments(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("pageTitle", "PAYMENTS REPORT");
        model.addAttribute("roles", session.getAttribute("roleId"));
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/bulkGovExpenditurePensionPayments", "SUCCESS", "View bulkGovExpenditurePensionPayments "));
        return "modules/govExpendPension/workflowPayments";
    }

    @RequestMapping(value = "/getBatchDoc", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getBatchDoc(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String response = "";
        String reference = params.get("reference");
        List<com.online.core.request.SupportDoc> docs = govExpenditurePensionRepo.getBatchDocument(reference);
        if (!docs.isEmpty()) {
            com.online.core.request.SupportDoc doc = docs.get(0);
            String base64Doc = Base64.encodeBase64String(doc.getFileBlob());
            response = "{\"fileName\": \"" + doc.getFileName() + "\", \"reference\": \"" + doc.getTxnId() + "\", \"fileContent\": \"" + base64Doc + "\"}";
        }
        return response;
    }

    @RequestMapping(value = "/processBatch", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processMuseERMSToCoreBanking(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String batchReference = params.get("reference");
        String eventCode = params.get("eventCode");
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        String postingRole = (String) session.getAttribute("postingRole");
        if (postingRole == null)
            return "{\"result\": -1, \"status\": \"No role selected\", \"message\": \"Please select role\"}";
        BnUser user = (BnUser) session.getAttribute("userCorebanking");
        philae.ach.UsRole achRole = new philae.ach.UsRole();
        philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
        for (UsRole role : user.getRoles().getRole()) {
            if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                achRole = userController.getAchRole(user2, postingRole);
            }
        }
        applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/processMuseERMSToCoreBanking", "SUCCESS", "Process MUSE ERMS to core banking"));
        return govExpenditurePensionRepo.processMuseERMSToCoreBanking(batchReference, eventCode, achRole);
    }

    @MessageMapping(value = {"batchProcessingAsyncUrl"})
    public String batchProcessingAsyncUrl(@Payload String message) throws Exception {
        return message;
    }

    /*
    PENSION PAYROLL PAYMENT DATA
     */
    @RequestMapping(value = "/pensionPayroll", method = RequestMethod.GET)
    public String pensionPayRollSummary(Model model, HttpSession session, @RequestParam Map<String, String> params) {
        model.addAttribute("pageTitle", "PENSION PAYROLL SUMMARY DATA");
        return "modules/pension/pensionBatchSummary";
    }

    /*
    GET INWARD EFT BATCHES AJAX /getInwardEFTAjax
     */
    @RequestMapping(value = "/getPensionPayrollAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPensionPayrollAjax(@RequestParam Map<String, String> customeQuery, HttpSession session) {
        String draw = customeQuery.get("draw");
        String pensionName = customeQuery.get("institutionName");
        String fromDate = customeQuery.get("fromDate").equals("") ? "2023-01-01 00:00:00" : customeQuery.get("fromDate") + " 00:00:00";
        String toDate = customeQuery.get("toDate").equals("") ? "2023-12-31 23:59:59" : customeQuery.get("toDate") + " 23:59:59";
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        String roleId = session.getAttribute("roleId") + "";
        ArrayList<Integer> authorizedDPPWFArray = (ArrayList<Integer>) httpSession.getAttribute("authorizedGPPWFArray");
        LOGGER.info("Authorized pension workflows in get payroll ajax.... {}", authorizedDPPWFArray);
        String status = "-";
        if (roleId.length() > 0) {
            if (authorizedDPPWFArray.contains(5)) {
                //BATCH RECEIVED FROM PENSIN NEEDS VALIDATION FROM CREDIT OFFICER
                status = "R";
            } else if (authorizedDPPWFArray.contains(12)) {
                //IT NEEDS AUTHORISE BATCH -  CREDIT APPROVER
                status = "V";
            } else if (authorizedDPPWFArray.contains(13)) {
                //IT NEEDS APPROVAL FROM POSTING AND VALIDATION - MAKER
                status = "A";
            } else if (authorizedDPPWFArray.contains(14)) {
                //IT NEEDS APPROVAL FROM POSTING AND VALIDATION - MAKER
                status = "P";
            }
        }

        return pensionRepo.getPensionPayrollAjax(pensionName, status, fromDate, toDate, draw, start, rowPerPage,
                searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getPensionBatchesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPensionBatchesAjax(@RequestParam Map<String, String> customeQuery) {
        String draw = customeQuery.get("draw");
        String pensionName = customeQuery.get("institutionName");
        String fromDate = customeQuery.get("fromDate").equals("") ? "2023-01-01 00:00:00" : customeQuery.get("fromDate") + " 00:00:00";
        String toDate = customeQuery.get("toDate").equals("") ? "2023-12-31 23:59:59" : customeQuery.get("toDate") + " 23:59:59";
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return pensionRepo.getPensionBatchesAjax(pensionName, fromDate, toDate, draw, start, rowPerPage, searchValue,
                columnIndex, columnName, columnSortOrder);
    }

    /*
     *Preview PENSION PAYROLL DETAILS
     */
    @RequestMapping(value = "/viewPensionPayrollDetails")
    public String viewPensionPayrollDetails(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        String roleId = session.getAttribute("roleId") + "";
        String status = "-";
        if (roleId.length() > 0) {
            if (roleId.contains("39")) {
                status = "R";
            } else if (roleId.contains("45")) {
                status = "V";
            }
//            if (roleId.contains("43") || roleId.contains("21")) {
//                status = "V";
//            }
//            if (roleId.contains("44") || roleId.contains("21")) {
//                status = "CoP";
//            }
            else if (roleId.contains("25") || roleId.contains("21")) {
                status = "A";
            } else if (roleId.contains("28") || roleId.contains("21")) {
                status = "P";
            }
        }

        String title;
        boolean isProcessActionAllowed = false;
        boolean isReturnActionAllowed = false;
        if (params.get("status").equalsIgnoreCase("I")) {
            isProcessActionAllowed = true;
            title = "PENSION PAYROLL DETAILS WITH REFERENCE: " + params.get("batchReference") + " WITH VALID ACCOUNTS";
        } else {
            isReturnActionAllowed = true;
            title = "PENSION PAYROLL DETAILS WITH REFERENCE: " + params.get("batchReference") + " WITH INVALID ACCOUNTS";
        }
        model.addAttribute("pageTitle", title);
        model.addAttribute("batchReference", params.get("batchReference"));
        model.addAttribute("status", params.get("status"));
        model.addAttribute("isProcessActionAllowed", isProcessActionAllowed);
        model.addAttribute("isReturnActionAllowed", isReturnActionAllowed);
        model.addAttribute("level", status);
        return "modules/pension/modals/previewPensionPayrollDetails";
    }

    /*
    GET PENSION PAYROLL DATA AJAX
     */
    @RequestMapping(value = "/getPensionPayrollDetailsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPensionPayrollDetailsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        String roleId = session.getAttribute("roleId") + "";
        String status = "-";
        ArrayList<Integer> authorizedDPPWFArray = (ArrayList<Integer>) httpSession.getAttribute("authorizedGPPWFArray");

        if (roleId.length() > 0) {
//            if (roleId.contains("39")) {
            if (authorizedDPPWFArray.contains(5)) {
                status = "R";
//            } else if (roleId.contains("45")) {
            } else if (authorizedDPPWFArray.contains(12)) {
                status = "V";
            }
//            if (roleId.contains("43") || roleId.contains("21")) {
//                status = "V";
//            }
//            if (roleId.contains("44") || roleId.contains("21")) {
//                status = "CoP";
//            }
//            else if (roleId.contains("25") || roleId.contains("21")) {
            else if (authorizedDPPWFArray.contains(13)) {
                status = "A";
//            } else if (roleId.contains("28") || roleId.contains("21")) {
            } else if (authorizedDPPWFArray.contains(14)) {
                status = "P";
            }
        }
        return pensionRepo.getPensionPayrollDetailsAjax(customeQuery.get("batchReference"), customeQuery.get("status"),
                status, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getPensionersPayrollAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPensionersPayrollAjax(@RequestParam Map<String, String> params, HttpSession session) {
        String draw = params.get("draw");
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";
        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");

        return pensionRepo.getPensionPayrollDetailsAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName,
                columnSortOrder);
    }

    /*
     *PROCESS PENSION PAYROLL TO BENEFICIARIES
     */
    @RequestMapping(value = "/processPensionPayrollToCoreBanking", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processPensionPayrollToCbs(@RequestParam Map<String, Object> params, HttpServletResponse response, HttpSession session) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) session.getAttribute("postingRole");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        String data = (String) params.get("data");
                        String tissRef = (String) params.get("tissRef");
                        String tissAmount = (String) params.get("tissAmount");
                        String narration = (String) params.get("narration");
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<HashMap<String, Object>>>() {
                        }.getType();
                        List<HashMap<String, Object>> listOfBatches = gson.fromJson(data, listType);
                        result = pensionRepo.processPensionPayroll(tissRef, tissAmount, listOfBatches, role);
                        break;
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }
            }
        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occurred: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    @RequestMapping(value = "/downloadPensionBatchFile", method = {RequestMethod.POST, RequestMethod.GET}, produces = "application/download;charset=UTF-8")
    @ResponseBody
    public String generatePensionFile(@RequestParam Map<String, String> params, HttpSession session, HttpServletResponse response) {
        String postingRole = (String) session.getAttribute("postingRole");
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) session.getAttribute("userCorebanking");
            for (UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    return pensionRepo.generatePensionPayrollBatchFile(role, response);
                } else {
                    return "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }
        } else
            return "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        return "{\"result\":\"99\",\"message\":\"General error!\"}";
    }

    @RequestMapping(value = {"/getAmountFromReference"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAmountFromReference(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String reference = params.get("reference");
        String amount = govExpenditurePensionRepo.getTotalAmountFromTransfers(reference);
        if (!amount.isEmpty())
            return "{\"result\":0,\"amount\":\"" + amount + "\"}";
        else
            return "{\"result\":99,\"amount\":\"-\"}";
    }

    @RequestMapping(value = {"/authorizeTransactionsWithReason"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String authorizeTransactionsWithReason(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String references = params.get("batchReferences");
        String reason = params.get("reason");
        String[] batchReferences = new Gson().fromJson(references, String[].class);
        return pensionRepo.authorizeTransactionsWithReason(batchReferences, reason);
    }

    @RequestMapping(value = {"/approveAuthorizedTransactions"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveAuthorizedTransactions(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String references = params.get("batchReferences");
        String[] batchReferences = new Gson().fromJson(references, String[].class);
        return govExpenditurePensionRepo.approveAuthorizedTransactions(batchReferences);
    }

    @RequestMapping(value = {"/complyTransactionWithReason"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String complyTransactionWithReason(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String reference = params.get("batchReference");
        String arr = params.get("trackingNos");
        String status = params.get("status");
        Boolean selectAll = Boolean.valueOf(params.get("all"));
        String[] trackingNos = new Gson().fromJson(arr, String[].class);
        return govExpenditurePensionRepo.complyTransactionWithReason(reference, status, trackingNos, "", selectAll);
    }

    @RequestMapping(value = {"/returnTransactionWithReason"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String returnTransactionWithReason(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String reference = params.get("batchReference");
        String arr = params.get("bankReferences");
        Boolean selectAll = Boolean.valueOf(params.get("all"));
        String[] bankReferences = new Gson().fromJson(arr, String[].class);
        return pensionRepo.returnTransactionWithReason(reference, bankReferences, selectAll, session.getAttribute("username") + "");
    }


    @RequestMapping(value = {"/approveCompliance"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveCompliance(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String arr = params.get("batchReferences");
        String[] batchReferences = new Gson().fromJson(arr, String[].class);
        return govExpenditurePensionRepo.approveCompliance(batchReferences);
    }

    @RequestMapping(value = {"/getPaymentReportsAjax"}, method = RequestMethod.POST)
    @ResponseBody
    public String getPaymentReportsAjax(@RequestParam Map<String, String> params, HttpServletResponse response, HttpSession session) {
        String paymentType = params.get("paymentType");
        String institution = params.get("institution");
        String fromDate = params.get("fromDate");
        String toDate = params.get("toDate");
        String batch = params.get("batch");
        String printedBy = session.getAttribute("username") + "";
        return govExpenditurePensionRepo.getPaymentSummaryReport("preview", response, DateUtil.now("yyyyMMddHHmmss"),
                institution, paymentType, fromDate, toDate, batch, printedBy);
    }

    @RequestMapping(value = {"/queryBatchesBasedOnInstitution"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<String> queryBatchesBasedOnInstitution(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        List<String> options = new ArrayList<>();
        String branchNo = session.getAttribute("branchCode") + "";
        if (!params.isEmpty() && params.containsKey("paymentType")) {
            if (params.get("paymentType").equals("MUSE")) {
                String account = params.get("account");
                options = govExpenditurePensionRepo.getBatches(account);
            }
        }
        String username = session.getAttribute("username") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getPendingGovPensionBulkPayments", "SUCCESS", "View pending gov pension bulk payments"));
        return options;
    }

    @RequestMapping(value = "/previewMUSESupportingDocument", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewSupportingDocument(@RequestParam Map<String, String> params) throws IOException {
        byte[] imageContent = govExpenditurePensionRepo.getSupportingDocument(params.get("reference"));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
    }

    @RequestMapping(value = {"/processTempBatches"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processTempBatches(@RequestParam Map<String, String> params) {
        String reference = params.get("batchReference");
        LOGGER.info("Submitted request {}", reference);
        if (reference.contains("_od"))
            return pensionRepo.processPensionersOverdraft(reference);
        else
            return pensionRepo.processTempBatches(reference);
    }

    @RequestMapping(value = {"/api/processTempBatches"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processTempBatchesApi(@RequestParam Map<String, String> params) {
        String reference = params.get("batchReference");
        LOGGER.info("Submitted request {}", reference);
        if (reference.contains("_od"))
            return pensionRepo.processPensionersOverdraft(reference);
        else
            return pensionRepo.processTempBatches(reference);
    }

    @PostMapping(value = "/api/batchCallback", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String batchCallback(@RequestBody HashMap<String, String> data, HttpSession session) {
        LOGGER.info("POST Batch Callback Body: {}", data);
//        applicationEventPublisher.publishEvent(new BatchCallbackEvent(this, data));
        return pensionRepo.updateTransactionByRef(data);
    }

    @PostMapping(value = "/api/pension/od/repay", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String overdraftStatusCallback(@RequestBody HashMap<String, String> data) {
        LOGGER.info("POST Overdraft Status Callback Body: {}", data);
        return pensionRepo.updatePensionPayrollOdStatus(data);
    }

    @PostMapping(value = "/api/processPensionersOverdraft", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processPensionersOverdraft(@RequestParam HashMap<String, String> params) {
        return pensionRepo.processTempBatches(params.get("batchReference"));
    }

    @PostMapping(value = "/api/processPensionersOverdraftNoNetPay", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String processPensionersOverdraftNoNetPay(@RequestBody List<LoanRepaymentReq> list) {
        return pensionRepo.processPensionersOverdraft2(list);
    }

    @GetMapping(value = "/api/reprocessFailedBatches", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String reprocessFailedBatches(@RequestParam(value = "payLoad") List<String> payLoad) {
        int count = 0;
        List<String> successBatches = new ArrayList<>();
        for (String batchReference: payLoad) {
            String response = pensionRepo.reprocessFailedBatches(batchReference);
            if (response.equals("{\"success\":true}")) {
                count++;
                successBatches.add(batchReference);
            }
        }
        return "Successful batches: " + count + ", List [" + String.join(",", successBatches) + "]";
    }

    @PostMapping(value = "/api/convertCurrency", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String convertCurrency(@RequestParam HashMap<String, String> params) {
        return govExpenditurePensionRepo.convertCurrency(params.get("batchReference"), params.get("account"));
    }

    @PostMapping(value = "/api/updateOdLoanStatus", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateOdLoanStatus() {
        return pensionRepo.getListOfOverdraftPensioners();
    }

}
