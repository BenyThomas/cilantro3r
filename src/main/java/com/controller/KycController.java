/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.KYC.CreateMobileAgent;
import com.DTO.KYC.ors.AttachmentPayload;
import com.DTO.KYC.ors.PayloadDTO;
import com.DTO.KYC.ors.response.AttachmentAccessData;
import com.DTO.KYC.ors.response.AttachmentData;
import com.DTO.KYC.ors.response.ClassifierData;
import com.DTO.KYC.ors.response.CompanyData;
import com.DTO.KYC.zcsra.BiometricRequestPayload;
import com.DTO.KYC.zcsra.DemographicDataRequestPayload;
import com.DTO.KYC.zcsra.Names;
import com.DTO.Reports.AuditTrails;
import com.config.EndPoints;
import com.config.SYSENV;
import com.dao.kyc.response.ors.AllResponseDTO;
import com.dao.kyc.response.ors.AttachmentResponseDTO;
import com.dao.kyc.response.ors.ClassifierResponseDTO;
import com.dao.kyc.response.ors.ResponseDTO;
import com.entities.ReportsJsonResponse2;
import com.entities.StatusSelect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.helper.DateUtil;
import com.helper.ORSApiEndpoints;
import com.models.kyc.DemographicDataEntity;
import com.repository.KycRepo;
import com.security.JwtUtils;
import com.service.kyc.ors.AttachmentService;
import com.service.kyc.ors.CompanyService;
import com.service.kyc.ors.ORSService;
import com.service.kyc.zcsra.DemographicDataService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class KycController {

    @Autowired
    KycRepo kycRepo;
    @Autowired
    ORSService orsService;

    @Autowired
    JwtUtils jwtUtils;
    @Autowired
    SYSENV env;

    @Value("${gateway.kyc.service:-1}")
    public String gatewayKycService;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KycController.class);
    @Autowired
    private DemographicDataService demographicDataService;

    @Autowired
    AttachmentService attachmentService;
    @Autowired
    ORSApiEndpoints endPoint;

    @Autowired
    CompanyService companyService;

    //    KYC DASHBOARD
    @RequestMapping(value = "/kycDashboard", method = RequestMethod.GET)
    public String kycDashboard(Model model, HttpSession session, @RequestParam Map<String, String> params) {
        model.addAttribute("pageTitle", "KYC DASHBOARD");
        model.addAttribute("kycModules", kycRepo.getKycModules(session.getAttribute("roleId").toString()));
        return "modules/kyc/kycDashboard";
    }

    @RequestMapping(value = "/createMobileAgent", method = RequestMethod.GET)
    public String createMobileAgent(Model model, HttpSession session, @RequestParam Map<String, String> params) {
        model.addAttribute("pageTitle", "CREATE MOBILE DEVICE AGENT");
        return "modules/kyc/createMobileAgent";
    }

    @RequestMapping(value = "/championVisits", method = RequestMethod.GET)
    public String championVisits(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "STAFF CHAMPION VISITS");
        model.addAttribute("userId", session.getAttribute("userId").toString());
        model.addAttribute("roleId", session.getAttribute("roleId"));
        model.addAttribute("branches", kycRepo.branches());
        return "modules/kyc/championVisits";
    }

    @RequestMapping(value = {"/submitCreateMobileAgentForm"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 createMobileAgent(@Valid CreateMobileAgent mobileAgentForm, BindingResult result, HttpSession session, @RequestParam Map<String, String> params) {
        ReportsJsonResponse2 response = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setValidated(false);
            response.setErrorMessages(errors);
        } else {
            //save the records on the database
            mobileAgentForm.setCreatedBy(session.getAttribute("username").toString());
            mobileAgentForm.setCreatedDate(DateUtil.now());
            kycRepo.createMobileAgentToKycGw(mobileAgentForm);
            response.setValidated(true);
        }
        LOGGER.info("FORM DETAILS:{}", mobileAgentForm);

        return response;
    }

    //    GET CUSTOMERS
    @RequestMapping(value = "/manageCustomers", method = RequestMethod.GET)
    public String manageCustomers(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "REMOTE ACCOUNT/RIM CREATED FROM MOBILE AGENTS");
        model.addAttribute("branch", session.getAttribute("branchCode"));
        model.addAttribute("roles", session.getAttribute("roleId"));
        model.addAttribute("branches", kycRepo.branches());
        model.addAttribute("products", kycRepo.accountProducts());
        return "modules/kyc/manageCustomers";
    }

    //    GET CUSTOMERS AJAX
    @RequestMapping(value = "/getCustomersAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCustomers(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String fromDate = params.get("fromDate") + " 00:00:00";
        String toDate = params.get("toDate") + " 23:59:59";
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";
        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        String branch = params.get("branch");
        String category = params.get("category");
        String type = params.get("type");
        return kycRepo.getCustomerInfo(
                session.getAttribute("branchCode").toString(), branch, category, type, fromDate, toDate, draw, start,
                rowPerPage, searchValue, columnName, columnSortOrder);
    }

    //    GET CHAMPION VISITS AJAX
    @RequestMapping(value = "/getChampionVisitsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getChampionVisits(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String fromDate = params.get("fromDate") + " 00:00:00";
        String toDate = params.get("toDate") + " 23:59:59";
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = params.get("search[value]") != null ? params.get("search[value]").trim() : "";
        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        String branch = params.get("branch");
        return kycRepo.getChampionVisits(
                session.getAttribute("branchCode").toString(), branch, fromDate, toDate, draw, start, rowPerPage,
                searchValue, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getChampionVisitBlob/{id}/{col}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getChampionVisitBlob(@PathVariable Long id, @PathVariable String col) throws JsonProcessingException {
        return kycRepo.getChampionVisitBlob(id, col);
    }

    //    VIEW CUSTOMER DETAILS
    @RequestMapping(value = {"/getCustomerKycAndAccountDetails"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCustomerKycAndAccountDetails(@RequestParam Map<String, String> params, HttpSession session) throws IOException {
        return kycRepo.getCustomerKycAndAcctDetails(
                params.get("trackingNo"),
                session.getAttribute("username").toString(),
                session.getAttribute("branchCode").toString()
        );
    }

    //    VIEW ACCOUNT DETAILS
    @RequestMapping(value = {"/getAccountDetails"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAccountDetails(@RequestParam Map<String, String> params, HttpSession session) throws IOException {
        return kycRepo.getAcctDetails(
                params.get("customerNo"),
                params.get("accountNo"),
                params.get("username"));
//                session.getAttribute("username").toString());
    }

    //    VIEW CUSTOMER ATTACHMENTS
    @RequestMapping(value = {"/viewCustomerAttachment"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String viewCustomerAttachment(@RequestParam Map<String, String> params, HttpSession session) throws IOException {
        return kycRepo.viewCustomerAttachment(params.get("trackingNo"), session.getAttribute("username").toString());
    }

    //    VIEW KYC CUSTOMER DETAILS
    @RequestMapping(value = {"/getKYCCustomerDetails"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getKYCCustomerDetails(@RequestParam Map<String, String> params) throws IOException {
        return kycRepo.getKYCCustomerDetails(params.get("trackingNo"));
    }

    //    EDIT KYC CUSTOMER DETAILS
    @RequestMapping(value = {"/editCustomerDetails"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String editKYCCustomerDetails(@RequestParam Map<String, String> params, HttpSession session) throws Exception {
        try {
            Gson g = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> firstMap = g.fromJson(params.get("form"), mapType);
            Map<String, Object> secondMap = g.fromJson(kycRepo.getKYCCustomerDetails(params.get("trackingNo")), mapType);
            Map<String, MapDifference.ValueDifference<Object>> differenceMap = Maps.difference(firstMap, secondMap).entriesDiffering();
            if (differenceMap.isEmpty())
                return "{\"responseCode\":\"0\", \"message\":\"No value was edited!\"}";
            StringBuilder updateSql = new StringBuilder("update customers set last_modified_by = ?, last_modified_date = ?");
            ArrayList<Object> args = new ArrayList<>();
            for (Map.Entry<String, MapDifference.ValueDifference<Object>> entry : differenceMap.entrySet()) {
                updateSql.append(", ").append(entry.getKey()).append(" = ?");
                args.add(entry.getValue().leftValue());
            }
            updateSql.append(" where id = ?");
            return kycRepo.editKYCCustomerDetails(
                    params.get("trackingNo"),
                    session.getAttribute("username").toString(),
                    updateSql.toString(),
                    args
            );
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("Edit Customer Exception: {}", e.getMessage());
            return "{\"responseCode\":\"99\", \"message\":\"Oops! Something went wrong, please try again!\"}";
        }
    }

    //    PREVIEW MANAGE MOBILE AGENTS
    @RequestMapping(value = "/manageAgents")
    public String manageAgents(HttpSession session, Model model) {
        model.addAttribute("pageTitle", "REGISTERED MOBILE AGENTS FOR ACCOUNT OPENING PROCESS");
        model.addAttribute("branches", kycRepo.branches());
        model.addAttribute("roleId", session.getAttribute("roleId"));
        return "modules/kyc/manageMobileAgents";
    }

    @RequestMapping(value = "/getMobileAgentsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getMobileAgentsAjax(@RequestParam Map<String, String> customerQuery) {
        String draw = customerQuery.get("draw");
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customerQuery.get("order[0][dir]");
        String category = customerQuery.get("category");
        return kycRepo.getAgentsInfo(customerQuery.get("branch"), customerQuery.get("is_enabled"),
                customerQuery.get("is_agent"), category, draw, start, rowPerPage, searchValue, columnName, columnSortOrder);
    }

    //    PREVIEW GROUPS
    @RequestMapping(value = "/manageGroups")
    public String manageGroups(Model model) {
        model.addAttribute("pageTitle", "GROUP/BUSINESS ACCOUNT/RIM CREATED FROM MOBILE AGENTS");
        model.addAttribute("branches", kycRepo.branches());
        return "modules/kyc/manageGroups";
    }

    //    GET GROUPS AJAX
    @RequestMapping(value = "/getGroupsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getKYCGroupsAjax(@RequestParam Map<String, String> groupsQuery, HttpSession session) {
        String draw = groupsQuery.get("draw");
        String start = groupsQuery.get("start");
        String rowPerPage = groupsQuery.get("length");
        String searchValue = groupsQuery.get("search[value]") != null ? groupsQuery.get("search[value]").trim() : "";
        String columnIndex = groupsQuery.get("order[0][column]");
        String columnName = groupsQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = groupsQuery.get("order[0][dir]");
        return kycRepo.getGroupsInfo(
                session.getAttribute("branchCode").toString(),
                groupsQuery.get("category"), draw, start, rowPerPage, searchValue, columnName, columnSortOrder);
    }

    //    GET GROUP SIGNATORIES AJAX
    @RequestMapping(value = "/groups", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getGroupSignatoriesAjax(@RequestParam("trackingNo") String id, HttpSession session) {
        return kycRepo.getGroupSignatories(
                session.getAttribute("username").toString(),
                session.getAttribute("branchCode").toString(),
                id
        );
    }

    //    VIEW GROUP DETAILS
    @RequestMapping(value = {"/getGroupKycAndAccountDetails"}, method = {RequestMethod.GET}, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String getGroupKycAndAccountDetails(@RequestParam Map<String, String> groupQuery, HttpServletResponse res, HttpSession session) throws IOException {
        return kycRepo.getGroupKycDetails(
                "preview", res, DateUtil.now("yyyyMMddHHmm") + "ExceptionReport",
                groupQuery.get("trackingNo"), session.getAttribute("username").toString(),
                session.getAttribute("branchCode").toString()
        );
    }

    //    GET ACCOUNTS
    @RequestMapping(value = "/manageAccounts", method = RequestMethod.GET)
    public String manageAccounts(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "REMOTE ACCOUNTS CREATED FROM MOBILE AGENTS");
        model.addAttribute("branch", session.getAttribute("branchCode"));
        if (Objects.equals(session.getAttribute("branchCode").toString(), "060")) {
            model.addAttribute("branches", kycRepo.branches());
            model.addAttribute("agents", kycRepo.getAgents());
        }
        return "modules/kyc/manageAccounts";
    }

    //    VIEW ACCOUNTS
    @RequestMapping(value = "/getAccountsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAccountsAjax(@RequestParam Map<String, String> customerQuery, HttpSession session) {
        String draw = customerQuery.get("draw");
        String fromDate = customerQuery.get("fromDate") + " 00:00:00";
        String toDate = customerQuery.get("toDate") + " 23:59:59";
        String hasMobileChannel = customerQuery.get("hasMobileChannel");
        if (hasMobileChannel == null)
            hasMobileChannel = "";
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customerQuery.get("order[0][dir]");
        String type = customerQuery.get("type");
        String branch = customerQuery.get("branch");
        String agent = customerQuery.get("agent");
        String category = customerQuery.get("category");
        return kycRepo.getAccountsInfo(
                session.getAttribute("branchCode").toString(), branch, category, type, agent, fromDate, toDate,
                hasMobileChannel, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    //    ENABLE AGENT BY APPROVER
    @RequestMapping(value = {"/enableAgent"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String enableAgent(@RequestParam Map<String, String> agentQuery, HttpSession session) throws IOException {
        String userId = agentQuery.get("userId");
        return kycRepo.enableAgent(userId, (String) session.getAttribute("username"), DateUtil.now());
    }

    //    DISABLE AGENT BY APPROVER
    @RequestMapping(value = {"/disableAgent"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String disableAgent(@RequestParam Map<String, String> agentQuery, HttpSession session) throws IOException {
        String userId = agentQuery.get("userId");
        return kycRepo.disableAgent(userId, (String) session.getAttribute("username"), DateUtil.now());
    }

    //    CREATE RIM
    @RequestMapping(value = {"/createRim"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createRIM(@RequestParam Map<String, String> customerQuery) {
        String userId = customerQuery.get("userId");
        String category = customerQuery.get("category");
        return kycRepo.createRIM(gatewayKycService + "onwsc/cust/approveCustomerRim/" + userId, userId, category);
    }

    //    CREATE ACCOUNT
    @RequestMapping(value = {"/createAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createAccount(@RequestParam Map<String, String> customerQuery) throws IOException {
        String userId = customerQuery.get("userId");
        String productId = customerQuery.get("productId");
        return kycRepo.createAccount(gatewayKycService + "onwsc/acct/approveCustomerAccount/" + userId + "/" + productId);
    }

    //    ENROL MOBILE BANKING
    @RequestMapping(value = {"/enrollMobileBanking"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String enrollMobileBanking(@RequestParam Map<String, String> customerQuery) throws IOException {
        String acctNo = customerQuery.get("acctNo");
        return kycRepo.enrollMobileBanking(gatewayKycService + "onwsc/acct/enrolMobileChannel/" + acctNo);
    }

    @RequestMapping(value = {"/api/enrollMobileBanking"}, method = RequestMethod.GET)
    @ResponseBody
    public String enrollMobileBanking(@RequestParam(value = "payLoad") List<String> payLoad) throws IOException {
        int count = 0;
        List<String> successAccounts = new ArrayList<>();
        for (String acctNo: payLoad) {
            String response = kycRepo.enrollMobileBanking(gatewayKycService + "onwsc/acct/enrolMobileChannel/" + acctNo);
            if (response.equals("{\"success\":true}")) {
                count++;
                successAccounts.add(acctNo);
            }
        }
        return "Successful accounts: " + count + ", List [" + String.join(",", successAccounts) + "]";
    }

    @RequestMapping(value = {"/api/resetMobileChannelUser"}, method = RequestMethod.GET)
    @ResponseBody
    public String resetMobileChannelUser(@RequestParam(value = "payLoad") List<String> payLoad) throws IOException {
        int count = 0;
        List<String> successAccounts = new ArrayList<>();
        for (String acctNo: payLoad) {
            String response = kycRepo.resetMobileChannelUser(gatewayKycService + "onwsc/acct/resetMobileChannelUser", acctNo);
            if (response.equals("{\"success\":true}")) {
                count++;
                successAccounts.add(acctNo);
            }
        }
        return "Successful accounts: " + count + ", List [" + String.join(",", successAccounts) + "]";
    }

    //    GET CUSTOMER INFORMATION
    @RequestMapping(value = {"/getCustomerInfo"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCustomerInfo(@RequestParam Map<String, String> customerQuery) throws IOException {
        String custNo = customerQuery.get("custNo");
        return kycRepo.getCustomerInfo(custNo);
    }

    //    UPDATE AGENT DEVICE
    @RequestMapping(value = {"/updateAgentDevice"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateAgentDevice(@RequestParam Map<String, String> customerQuery) throws IOException {
        String id = customerQuery.get("id");
        if (kycRepo.updateAgentDevice(id) != -1)
            return "{\"success\":true}";
        else
            return "{\"success\":false}";
    }

    @RequestMapping(value = "/remoteDashboard")
    public String agencyTransactionsReport(Model model) {
        model.addAttribute("pageTitle", "REMOTE ACCOUNT OPENING REPORT");
        model.addAttribute("branches", kycRepo.branches());
        model.addAttribute("branchesReport", kycRepo.getBranchesReport());
        model.addAttribute("pendingBranches", kycRepo.getPendingBranches());
        model.addAttribute("nidaAccounts", kycRepo.getThisYearSuccessNIDAAccountsCount());
        model.addAttribute("nonNidaAccounts", kycRepo.getThisYearSuccessNonNIDAAccountsCount());
        model.addAttribute("totalAccounts", kycRepo.getThisYearSuccessAccountsCount());
        AuditTrails.setComments("View remote account opening report");
        AuditTrails.setFunctionName("/remoteDashboard");
        return "modules/kyc/dashboard";
    }

    @RequestMapping(value = {"/getKYCReports"}, method = RequestMethod.GET)
    @ResponseBody
    public String getKYCReports(@RequestParam Map<String, String> params, HttpServletResponse response, HttpSession session) {
        try {
            String fromDate = params.get("fromDate");
            String toDate = params.get("toDate");
            String format = params.get("format");
            String reportType = params.get("reportType");
            String branch = params.get("branch");
            String reportName = "RAO" + DateUtil.now("yyyyMMddHHmmss");
            Integer isAgent = null;
            if (params.get("is_agent") != null)
                isAgent = Integer.valueOf(params.get("is_agent"));
            String agent = params.get("agent");
            String printedBy = session.getAttribute("username") + "";
            return kycRepo.remoteAccountOpeningReport(format, response, reportName, reportType, fromDate, toDate, printedBy,
                    branch, isAgent, agent);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            return null;
        }
    }

    @RequestMapping(value = {"/api/getSuccessAccountsReport"}, method = RequestMethod.GET)
    @ResponseBody
    public String getSuccessAccountsReport(HttpServletResponse response, HttpSession session) {
        String reportName = "RAO" + DateUtil.now("yyyyMMddHHmmss");
        String printedBy = session.getAttribute("username") + "";
        return kycRepo.getSuccessAccountsReport("excel", response, reportName, printedBy);
    }

    //    MoFP dashboard
    @RequestMapping(value = "/mofpWorkflows", method = RequestMethod.GET)
    public String musePayments(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "MoFP ACCOUNTS");
        String roleId = session.getAttribute("roleId") + "";
        String workflowIds = kycRepo.getWorkflowRoles(roleId);
        model.addAttribute("roleId", roleId);
        model.addAttribute("workflowIds", workflowIds);
        List<StatusSelect> list = new ArrayList<>();
        String initialStatus = "Pending";
        if (roleId.contains("21") || roleId.contains("48") || workflowIds.contains("16")) {
            StatusSelect selectMap1 = new StatusSelect();
            selectMap1.setName("Pending");
            selectMap1.setValue("Pending (Received from BoT)");
            list.add(selectMap1);
            StatusSelect selectMap2 = new StatusSelect();
            selectMap2.setName("Received");
            selectMap2.setValue("Received");
            list.add(selectMap2);
            StatusSelect selectMap3 = new StatusSelect();
            selectMap3.setName("Awaiting Maker");
            selectMap3.setValue("Awaiting Maker");
            list.add(selectMap3);
            StatusSelect selectMap4 = new StatusSelect();
            selectMap4.setName("Awaiting Checker");
            selectMap4.setValue("Awaiting Checker");
            list.add(selectMap4);
            StatusSelect selectMap5 = new StatusSelect();
            selectMap5.setName("Accepted");
            selectMap5.setValue("Accepted");
            list.add(selectMap5);
            StatusSelect selectMap6 = new StatusSelect();
            selectMap6.setName("Closed");
            selectMap6.setValue("Closed");
            list.add(selectMap6);
            StatusSelect selectMap7 = new StatusSelect();
            selectMap7.setName("Cancelled");
            selectMap7.setValue("Cancelled");
            list.add(selectMap7);
            StatusSelect selectMap8 = new StatusSelect();
            selectMap8.setName("Rejected");
            selectMap8.setValue("Rejected");
            list.add(selectMap8);
            StatusSelect selectMap9 = new StatusSelect();
            selectMap9.setName("Updated");
            selectMap9.setValue("Updated");
            list.add(selectMap9);
        } else if (roleId.contains("24") || workflowIds.contains("17")) {
            initialStatus = "Received";
            StatusSelect selectMap2 = new StatusSelect();
            selectMap2.setName("Received");
            selectMap2.setValue("Received");
            list.add(selectMap2);
            StatusSelect selectMap3 = new StatusSelect();
            selectMap3.setName("Awaiting Maker");
            selectMap3.setValue("Awaiting Maker");
            list.add(selectMap3);
            StatusSelect selectMap4 = new StatusSelect();
            selectMap4.setName("Awaiting Checker");
            selectMap4.setValue("Awaiting Checker");
            list.add(selectMap4);
            StatusSelect selectMap5 = new StatusSelect();
            selectMap5.setName("Accepted");
            selectMap5.setValue("Accepted");
            list.add(selectMap5);
            StatusSelect selectMap6 = new StatusSelect();
            selectMap6.setName("Closed");
            selectMap6.setValue("Closed");
            list.add(selectMap6);
            StatusSelect selectMap7 = new StatusSelect();
            selectMap7.setName("Cancelled");
            selectMap7.setValue("Cancelled");
            list.add(selectMap7);
            StatusSelect selectMap8 = new StatusSelect();
            selectMap8.setName("Rejected");
            selectMap8.setValue("Rejected");
            list.add(selectMap8);
            StatusSelect selectMap9 = new StatusSelect();
            selectMap9.setName("Updated");
            selectMap9.setValue("Updated");
            list.add(selectMap9);
        } else if (roleId.contains("26") || workflowIds.contains("18")) {
            initialStatus = "Awaiting Maker";
            StatusSelect selectMap3 = new StatusSelect();
            selectMap3.setName("Awaiting Maker");
            selectMap3.setValue("Awaiting Maker");
            list.add(selectMap3);
            StatusSelect selectMap4 = new StatusSelect();
            selectMap4.setName("Awaiting Checker");
            selectMap4.setValue("Awaiting Checker");
            list.add(selectMap4);
            StatusSelect selectMap5 = new StatusSelect();
            selectMap5.setName("Accepted");
            selectMap5.setValue("Accepted");
            list.add(selectMap5);
            StatusSelect selectMap6 = new StatusSelect();
            selectMap6.setName("Closed");
            selectMap6.setValue("Closed");
            list.add(selectMap6);
            StatusSelect selectMap7 = new StatusSelect();
            selectMap7.setName("Cancelled");
            selectMap7.setValue("Cancelled");
            list.add(selectMap7);
            StatusSelect selectMap8 = new StatusSelect();
            selectMap8.setName("Rejected");
            selectMap8.setValue("Rejected");
            list.add(selectMap8);
            StatusSelect selectMap9 = new StatusSelect();
            selectMap9.setName("Updated");
            selectMap9.setValue("Updated");
            list.add(selectMap9);
        } else if (roleId.contains("27") || workflowIds.contains("19")) {
            initialStatus = "Awaiting Checker";
            StatusSelect selectMap4 = new StatusSelect();
            selectMap4.setName("Awaiting Checker");
            selectMap4.setValue("Awaiting Checker");
            list.add(selectMap4);
            StatusSelect selectMap5 = new StatusSelect();
            selectMap5.setName("Accepted");
            selectMap5.setValue("Accepted");
            list.add(selectMap5);
            StatusSelect selectMap6 = new StatusSelect();
            selectMap6.setName("Closed");
            selectMap6.setValue("Closed");
            list.add(selectMap6);
            StatusSelect selectMap7 = new StatusSelect();
            selectMap7.setName("Cancelled");
            selectMap7.setValue("Cancelled");
            list.add(selectMap7);
            StatusSelect selectMap8 = new StatusSelect();
            selectMap8.setName("Rejected");
            selectMap8.setValue("Rejected");
            list.add(selectMap8);
            StatusSelect selectMap9 = new StatusSelect();
            selectMap9.setName("Updated");
            selectMap9.setValue("Updated");
            list.add(selectMap9);
        }
        model.addAttribute("categories", list);
        model.addAttribute("initialStatus", initialStatus);
        return "modules/kyc/mofpAccounts";
    }

    //    VIEW MoFP ACCOUNTS WORKFLOWS
    @RequestMapping(value = "/getPendingAccountWorkflowAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPendingAccountWorkflowAjax(@RequestParam Map<String, String> customerQuery, HttpSession session) {
        String draw = customerQuery.get("draw");
        String fromDate = customerQuery.get("fromDate") + " 00:00:00";
        String toDate = customerQuery.get("toDate") + " 23:59:59";
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customerQuery.get("order[0][dir]");
        String branch = customerQuery.get("branch");
        return kycRepo.getAccountsWorkflows(
                session.getAttribute("branchCode").toString(), branch, customerQuery.get("status"), fromDate, toDate,
                draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    //    VIEW WORKFLOW ACCOUNTS
    @RequestMapping(value = "/previewBatchAccountsList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String previewBatchAccountsList(@RequestParam Map<String, String> customerQuery, HttpSession session) {
        String draw = customerQuery.get("draw");
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customerQuery.get("order[0][dir]");
        String workflowId = customerQuery.get("workflowId");
        return kycRepo.getWorkflowAccounts(session.getAttribute("branchCode").toString(), workflowId, draw, start,
                rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    //    VIEW MoFP WORKFLOW ATTACHMENTS
    @RequestMapping(value = {"viewMoFPWorkflowAttachments"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String viewMoFPWorkflowAttachments(@RequestParam Map<String, String> params) {
        String messageId = params.get("messageId");
        return "{\"jasper\":\"" + kycRepo.getAttachmentDocument(messageId) + "\"}";
    }

    //    OPEN MoFP Account
    @RequestMapping(value = {"/api/openMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String openMoFPAccount(@RequestBody(required = false) String payLoad) {
        return kycRepo.openMoFPAccountRequest(payLoad);
    }

    //    ACCEPT MoFP Account
    @RequestMapping(value = {"/api/acceptMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String acceptMoFPAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String id = params.get("id");
        String nbOfTxn = params.get("nbOfTxn");
        String username = session.getAttribute("username").toString();
        LOGGER.info("Params: {}", params);
        LOGGER.info("ID: {}", id);
        return kycRepo.acceptMoFPAccount(id, nbOfTxn, username);
    }

    //    ACCEPT MoFP Account request
    @RequestMapping(value = {"/api/acceptMoFPAccountRequest"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String acceptMoFPAccountRequest(@RequestParam Map<String, String> params, HttpSession session) {
        String id = params.get("id");
        String username = session.getAttribute("username").toString();
        return kycRepo.acceptMoFPAccountRequest(id, username);
    }

    //    SEND MoFP Account request to branch
    @RequestMapping(value = {"/api/sendMoFPAccountRequestToBranch"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String sendMoFPAccountRequestToBranch(@RequestParam Map<String, String> params, HttpSession session) {
        String id = params.get("id");
        String username = session.getAttribute("username").toString();
        return kycRepo.sendMoFPAccountRequestToBranch(id, username);
    }

    //    ASSIGN MoFP Account
    @RequestMapping(value = {"/api/assignMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String assignMoFPAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String username = session.getAttribute("username").toString();
        String id = params.get("id");
        String accountNo = params.get("accountNo");
        String nbOfTxn = params.get("nbOfTxn");
        return kycRepo.assignMoFPAccount(id, username, accountNo, nbOfTxn);
    }

    //    CONFIRM MoFP Account
    @RequestMapping(value = {"/api/confirmMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String confirmMoFPAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String id = params.get("id");
        String nbOfTxn = params.get("nbOfTxn");
        String username = session.getAttribute("username").toString();
        return kycRepo.confirmMoFPAccount(id, username, nbOfTxn);
    }

    //    UPDATE MoFP Account
    @RequestMapping(value = {"/api/updateMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateMoFPAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String id = params.get("id");
        String nbOfTxn = params.get("nbOfTxn");
        String comments = params.get("comments");
        String username = session.getAttribute("username").toString();
        return kycRepo.updateMoFPAccount(id, username, nbOfTxn, comments);
    }

    //    REJECT MoFP Account
    @RequestMapping(value = {"/api/rejectMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String rejectMoFPAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String username = session.getAttribute("username").toString();
        String id = params.get("id");
        String nbOfTxn = params.get("nbOfTxn");
        String comments = params.get("comments");
        return kycRepo.rejectMoFPAccount(id, username, nbOfTxn, comments);
    }

    //    CLOSE MoFP Account
    @RequestMapping(value = {"/api/closeMoFPAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String closeMoFPAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String username = session.getAttribute("username").toString();
        String id = params.get("id");
        String nbOfTxn = params.get("nbOfTxn");
        return kycRepo.closeMoFPAccount(id, username, nbOfTxn);
    }

    //    GET MoFP ACCOUNTS
    @RequestMapping(value = "/getMoFPAccountsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getMoFPAccountsAjax(@RequestParam Map<String, String> customerQuery, HttpSession session) {
        String draw = customerQuery.get("draw");
        String fromDate = customerQuery.get("fromDate");
        if (fromDate != null)
            fromDate += " 00:00:00";
        String toDate = customerQuery.get("toDate");
        if (toDate != null)
            toDate += " 23:59:59";
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customerQuery.get("order[0][dir]");
        String branch = customerQuery.get("branch");
        return kycRepo.getMoFPAccountsAjax(
                session.getAttribute("branchCode").toString(), branch, fromDate, toDate,
                draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    // SEARCH AGENT ACCOUNT
    @RequestMapping(value = {"/api/searchAgentAccount"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String searchAgentAccount(@RequestParam Map<String, String> params, HttpSession session) {
        String username = session.getAttribute("username").toString();
        String account = params.get("account");
        String url = gatewayKycService + "onwsc/acct/searchAccount";
        return kycRepo.searchAgentAccount(username, account, url);
    }

    // HEALTH ENDPOINT
    @RequestMapping(value = {"/api/health"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String healthEndpoint(@RequestParam Map<String, String> params) {
        return "{\"responseCode\": 200, \"response\": \"Success\"}";
    }

    // RECEIVE MoFP API MESSAGE
    @RequestMapping(value = {"/api/mofp/message"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String receiveMoFPMessage(@RequestBody(required = true) String payLoad, HttpServletRequest request) {
        LOGGER.info("Payload-> {}", payLoad);
        String headerAuth = request.getHeader("Authorization");
        String apiKey = request.getHeader("x-api-key");
        String messageType = request.getHeader("messageType");
        String mofpApiKey = "Y7hHwyxqQ9nqqtnRtL3j8q3ZoYnbGxd2qld9PunbxBplfVTE9Qy1vzpr0CHua33B2BQCOsvSFmA7Jnsue0upcI1RM0" +
                "oVlu72rb9RTZgXp3fv950NVcJw2ekGAc4HPkV3cu0AYw1XopALBmywQ08FmUKRYITE95kLwy53OZRM7LIp3Hgw5GZOQZEu9v7rackV" +
                "hPVOZ2x1f8yboDXPXkmPmih9mcOjhDnup8ugKMlbKhBfccSmA06F1Vrmmy21rD0B";
        if (Objects.equals(apiKey, mofpApiKey)) {
            LOGGER.info("Header Auth-> {}", headerAuth);
            LOGGER.info("Header API Key-> {}", apiKey);
            LOGGER.info("Header message type-> {}", messageType);
            String userJwtToken = jwtUtils.parseJwt(request);
            boolean validationResult = jwtUtils.validateJwtToken(userJwtToken);
            String xml = payLoad;
            if (payLoad.contains("|")) {
                String[] parts = payLoad.split("\\|");
                if (parts.length > 1) {
                    xml = parts[0];
                }
            }
            if (validationResult) {
                if (Objects.equals(messageType, "ACCOUNTOPENING") || Objects.equals(messageType, "AccountOpening")) {
                    return kycRepo.openMoFPAccountRequest(xml);
                } else if (messageType.contains("ACCOUNTMAINT") || Objects.equals(messageType, "AccountMaintenance")) {
                    return kycRepo.maintainMoFPAccountRequest(xml);
                } else if (Objects.equals(messageType, "ACCOUNTCLOSING") || messageType.contains("AccountClosing")) {
                    return kycRepo.closeMoFPAccountRequest(xml);
                } else if (Objects.equals(messageType, "CANCELLATION") || messageType.contains("Cancellation")) {
                    return kycRepo.cancelMoFPAccountRequest(xml);
                }
//                return kycRepo.sendRejectedResponse(xml);
            } else {
                LOGGER.info("Invalid token:-> {}", userJwtToken);
                throw new AccessDeniedException("Invalid token:->" + userJwtToken);
            }
        } else {
            LOGGER.info("Invalid API Key: -> {}", apiKey);
            throw new AccessDeniedException("Invalid API Key: -> " + apiKey);
        }
        return "OK";
    }

    // RECEIVE MoF REQUEST
    @RequestMapping(value = {"/api/v2/message"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String receiveMoFMessage(@RequestBody() String payLoad, HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        LOGGER.info("Payload-> {}", payLoad);
        String xml;
        if (payLoad.contains("|")) {
            String[] parts = payLoad.split("\\|");
            if (parts.length > 1) {
                xml = parts[0];
                if (xml.contains("BALANCEREQUEST")) {
                    return kycRepo.accountBalanceRequest(xml);
                } else if (xml.contains("REQUEST")) {
                    return kycRepo.bankStatementRequest(xml);
                }
            }
        }
        return null;
    }

    // RECEIVE MoF REQUEST
    @RequestMapping(value = {"/api/mof/message"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String receiveMoFMessage2(@RequestBody() String payLoad, HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        LOGGER.info("Payload-> {}", payLoad);
        String xml;
        if (payLoad.contains("|")) {
            String[] parts = payLoad.split("\\|");
            if (parts.length > 1) {
                xml = parts[0];
                if (xml.contains("BALANCEREQUEST")) {
                    return kycRepo.accountBalanceRequest(xml);
                } else if (xml.contains("REQUEST")) {
                    return kycRepo.bankStatementRequest(xml);
                }
            }
        }
        return null;
    }

    // RECEIVE MoF BALANCE REQUEST
    @RequestMapping(value = {"/api/mof/balance"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String receiveMoFBalanceRequest(@RequestBody() String payLoad, HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        LOGGER.info("Payload-> {}", payLoad);
        return kycRepo.accountBalanceRequest(payLoad);
    }
    // RECEIVE MoF STATEMENT REQUEST
    @RequestMapping(value = {"/api/mof/statement"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String receiveMoFStatementRequest(@RequestBody() String payLoad, HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        LOGGER.info("Payload-> {}", payLoad);
        return kycRepo.bankStatementRequest(payLoad);
    }
    // SEND MoF ACCOUNT(S) BALANCE
    @RequestMapping(value = {"/api/sendBalance"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String sendMoFBalance(@RequestParam Map<String, String> params, HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        kycRepo.sendAccountBalance(params.get("requestId"), params.get("account"));
        return "OK";
    }
    // SEND MoF TRANSACTION SUMMARY
    @RequestMapping(value = {"/api/sendSummary"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String sendMoFTransactionSummary(HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        kycRepo.sendTransactionSummary();
        return "OK";
    }
    // SEND MoF BANK STATEMENT
    @RequestMapping(value = {"/api/sendStatement"}, method = {RequestMethod.POST}, produces = "application/xml")
    @ResponseBody
    public String sendMoFAccountStatement(@RequestParam Map<String, String> params, HttpServletRequest request) {
        LOGGER.info("Request-> {}", request);
        kycRepo.sendBankStatement(null, params.get("account"));
        return "OK";
    }

    // SEND MoFP RESPONSE
    @RequestMapping(value = {"/api/sendResponse"}, method = {RequestMethod.GET})
    @ResponseBody
    public String sendResponse(@RequestParam(name = "type") int type, @RequestParam(name = "id") String id) throws Exception {
        return kycRepo.sendBotMoFPResponse(type, id, "");
    }

    // SEND MoFP ACCOUNT OPENING STATUS
    @RequestMapping(value = {"/api/sendAcctOpeningStatus"}, method = {RequestMethod.GET})
    @ResponseBody
    public String sendAcctOpeningStatus(@RequestParam(name = "id") Long id,
                                        @RequestParam(name = "tcbReference") String tcbReference,
                                        @RequestParam(name = "institutionName") String institutionName,
                                        @RequestParam(name = "addressee") String addressee) throws Exception {
        return kycRepo.sendMoFPAccountOpeningStatus(id, tcbReference, institutionName, addressee);
    }

    // SEND MoFP ACCOUNT MAINTENANCE STATUS
    @RequestMapping(value = {"/api/sendAcctMaintenanceStatus"}, method = {RequestMethod.GET})
    @ResponseBody
    public String sendAcctMaintenanceStatus(@RequestParam(name = "id") Long id,
                                            @RequestParam(name = "tcbReference") String tcbReference,
                                            @RequestParam(name = "institutionName") String institutionName,
                                            @RequestParam(name = "addressee") String addressee) throws Exception {
        return kycRepo.sendAcctMaintenanceStatus(id, tcbReference, institutionName, addressee);
    }

    // SEND MoFP ACCOUNT CLOSING STATUS
    @RequestMapping(value = {"/api/sendAcctClosingStatus"}, method = {RequestMethod.GET})
    @ResponseBody
    public String sendAcctClosingStatus(@RequestParam(name = "id") Long id,
                                        @RequestParam(name = "tcbReference") String tcbReference,
                                        @RequestParam(name = "institutionName") String institutionName,
                                        @RequestParam(name = "addressee") String addressee) throws Exception {
        return kycRepo.sendMoFPAccountClosingStatus(id, tcbReference, institutionName, addressee);
    }

    // GENERATE ACCOUNT LETTER
    @RequestMapping(value = {"/api/generateAcctLetter"}, method = {RequestMethod.GET})
    @ResponseBody
    public String generateAcctLetter(@RequestParam(name = "id") Long id,
                                        @RequestParam(name = "tcbReference") String tcbReference,
                                        @RequestParam(name = "institutionName") String institutionName,
                                        @RequestParam(name = "addressee") String addressee,
                                        @RequestParam(name = "type") String type) {
        return kycRepo.generateLetter(id, tcbReference, institutionName, addressee, type);
    }

    // SHOW ACCOUNT LETTER FROM MoFP
    @RequestMapping(value = {"/api/downloadStatusDoc"})
    @ResponseBody
    public String downloadStatusDoc(@RequestParam(name = "reference") String reference, HttpServletResponse response) {
        try {
            byte[] file = kycRepo.downloadStatusDoc(reference);
            response.setContentType("application/pdf");
            response.setHeader("Content-disposition", "attachment; filename=STATUS_DOCUMENT_" + reference + ".pdf");
            response.setHeader("Content-Length", String.valueOf(file.length));
            response.getOutputStream().write(file);
            response.getOutputStream().close();
            LOGGER.info("Supporting document {} downloaded SUCCESSFULLY!", reference);
            return "OK";
        } catch (Exception ex) {
            LOGGER.info("Exception on download supporting document: {}", ex);
        }
        return "OK";
    }

    @RequestMapping(value = {"/api/updateAgentBranch"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateAgentBranch(@RequestParam Map<String, String> customerQuery) {
        String id = customerQuery.get("id");
        String code = customerQuery.get("newBranch");
        if (kycRepo.updateAgentBranch(id, code) != -1)
            return "{\"success\":true}";
        else
            return "{\"success\":false}";
    }

    //    Load agent roles
    @RequestMapping(value = {"/api/loadAgentRoles"}, method = {RequestMethod.GET}, produces = "application/json")
    @ResponseBody
    public String loadAgentRoles() {
        return kycRepo.loadAgentRoles();
    }

    //    Edit agent role
    @RequestMapping(value = {"/api/editAgentRole"}, method = {RequestMethod.POST}, produces = "application/json")
    @ResponseBody
    public String editAgentRole(@RequestParam Map<String, String> params) {
        String userId = params.get("id");
        String roleId = params.get("roleId");
        return kycRepo.editAgentRole(userId, roleId);
    }

    //ZCSRA ENDPOINTS

    @GetMapping(value = "/zcsraWorkflows", produces = "application/json;charset=UTF-8")
    public String getZcsraWorkflows(Model model) {
        model.addAttribute("demographicData",null);
        return "modules/kyc/zcsra";
    }

    @PostMapping("/api/demographic/search")
    public String getDemographicData(@ModelAttribute("zanid") String zanid,Model model) {
        DemographicDataRequestPayload payload = new DemographicDataRequestPayload();
        payload.setZanid(zanid);
        payload.setApiKey(env.ZCSRA_API_KEY);
        DemographicDataEntity demographicData = demographicDataService.getDemographicDataByZanId(payload);
        model.addAttribute("demographicData",demographicData);
        return "modules/kyc/zcsra";
    }
    @GetMapping("/api/demographic/download/{zanid}")
    public ResponseEntity<byte[]> getDemographicDataInPdfForm(@ModelAttribute("zanid") String zanid) {
        DemographicDataEntity data  = demographicDataService.getDemographicDataByZanId(zanid);
        if (data == null){
            return ResponseEntity.notFound().build();
        }
        byte[] pdfBytes = demographicDataService.getDemographicDataInPdfForm(data);
        if (pdfBytes == null){
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "Attachment; filename=Demographic-data-for-" + zanid + ".pdf");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    @PostMapping("/api/getDemographicDataByBiometric")
    public DemographicDataEntity getDemographicDataByBiometric(@RequestBody BiometricRequestPayload payload) {
        return demographicDataService.getDemographicDataFromZCSRAByBiometric(payload);
    }
    @GetMapping("/api/getDemographicDataByZanID")
    public DemographicDataEntity getDemographicDataByZanId(@RequestParam(name = "id") String id) {
        DemographicDataRequestPayload payload = new DemographicDataRequestPayload();
        payload.setZanid(id);
        payload.setApiKey(env.ZCSRA_API_KEY);
        return demographicDataService.getDemographicDataByZanId(payload);
    }
    @GetMapping("/api/getDemographicByNames")
    public DemographicDataEntity getDemographicDataByNames(@RequestBody Names names) {
        return demographicDataService.getDemographicDataByNames(names);
    }

    //BRELA ORS ENDPOINTS


    @GetMapping(value = "/orsWorkflows")
    public String getOrsWorkflows(Model model) {
        return "modules/kyc/ors";
    }

    @PostMapping(value = "/api/getEntityDetails", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEntityDetails(@Valid @RequestBody PayloadDTO payloadDTO) {
        LOGGER.info("Payload: {}", payloadDTO);
        Map<String, Object> response = orsService.findEntityDetails(payloadDTO);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Entity details not found.");
        }
        response.put("attachmentServerUrl", endPoint.getAttachmentServerUrl()+endPoint.getAttachmentDownloadEndpoint());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/api/ors/download/{certNo}")
    public ResponseEntity<byte[]> downloadEntityInfoInPdf(@ModelAttribute("certNo") String certNo) {
        CompanyData data = orsService.getEntityInfo(certNo);
        if (data == null){
            return ResponseEntity.notFound().build();
        }
        byte[] pdfBytes = orsService.getEntityDetailsInPdf(data);
        if (pdfBytes == null){
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "Attachment; filename=Entity-Info-For-" + certNo + ".pdf");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }



}
