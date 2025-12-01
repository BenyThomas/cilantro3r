/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.GeneralJsonResponse;
import com.DTO.Reports.AuditTrails;
import com.entities.ReportsForm;
import com.entities.ReportsForm2;
import com.entities.ReportsJsonResponse;
import com.entities.ReportsJsonResponse2;
import com.helper.DateUtil;
import com.repository.Recon_M;
import com.repository.ReportRepo;
import com.repository.TellerRepo;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 *
 * @author MELLEJI
 */
@Controller
public class Reports {

    @Autowired
    HttpSession httpSession;
    @Autowired
    ReportRepo reportRepo;
    @Autowired
    Recon_M reconRepo;
    @Value("${benchmark.recon.date}")
    public String benchMarkReconDate;
    @Autowired
    TellerRepo tellerRepo;

    @RequestMapping(value = "/thirdPartTxns")
    public String thirdPartTxns(Model model) {
        model.addAttribute("pageTitle", "THIRD PARTY TRANSACTION REPORTS");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(httpSession.getAttribute("roleId").toString(), httpSession.getAttribute("userId").toString()));
        AuditTrails.setComments("View Third party Transaction Report ");
        AuditTrails.setFunctionName("/thirdPartTxns");
        return "pages/reports/thirdPartyTransactionsReport";
    }

    /*
    get cbs transactions json format
     */
    @RequestMapping(value = "/getThirdPartTxnsReportAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse getThirdPartTxnsReportAjax(@Valid ReportsForm reportsForm, BindingResult result) {
        ReportsJsonResponse respone = new ReportsJsonResponse();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            respone.setJsonString(reportRepo.getThirdPartyTransactionsReport(reportsForm.getTxnType(), reportsForm.getFromdate(), reportsForm.getTodate()));
        }
        AuditTrails.setComments("View " + reportsForm.getTxnType() + " Third party Transaction Report from date:" + reportsForm.getFromdate() + " To date: " + reportsForm.getTodate());
        AuditTrails.setFunctionName("/getThirdPartTxnsReportAjax");
        return respone;
    }

    @RequestMapping(value = "/cbsTransactions")
    public String cbsTransactionsReport(Model model) {
        model.addAttribute("pageTitle", "CORE BANKING TRANSACTION REPORTS");
        AuditTrails.setComments("View Core banking Transaction Report ");
        AuditTrails.setFunctionName("/cbsTransactions");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(httpSession.getAttribute("roleId").toString(), httpSession.getAttribute("userId").toString()));
        return "pages/reports/cbsTransactionReport";
    }

    /*
    RTGS/EFT TRANSACTION REPORT
     */
    @RequestMapping(value = "/remittanceReport")
    public String remittanceReport(Model model) {
//        model.addAttribute("pageTitle", "CORE BANKING TRANSACTION REPORTS");
//        AuditTrails.setComments("View Core banking Transaction Report ");
//        AuditTrails.setFunctionName("/cbsTransactions");
        model.addAttribute("pageTitle", "TISS/TT TRANSACTION REPORT");
        String roleId = (String) httpSession.getAttribute("roleId");
        model.addAttribute("transfer_types", reportRepo.getRTGSTransferTypes(roleId));
        return "modules/reports/payments/remittanceReport";
    }

    /*
    RTGS/EFT TRANSACTION REPORT AJAX
     */
    @RequestMapping(value = "/getRremittanceReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getBranchRemittance(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {

        String fromDate = customeQuery.get("fromdate");
        String todate = customeQuery.get("todate");
        String txnType = customeQuery.get("txnType");
        String direction = customeQuery.get("direction");
        String mno = customeQuery.get("txnType");
        System.out.println("");
        session.setAttribute("mno", mno);
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return reportRepo.getRTGSRemittanceReportAjax(direction,(String) httpSession.getAttribute("branchCode"), txnType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    EFT TRANSACTION REPORT
     */
    @RequestMapping(value = "/eftTransactionsReport")
    public String eftTransactionsReport(Model model) {
        model.addAttribute("pageTitle", "EFT TRANSACTION REPORT");
        String roleId = (String) httpSession.getAttribute("roleId");
        model.addAttribute("transfer_types", reportRepo.getRTGSTransferTypes(roleId));
        return "modules/reports/payments/eftTransactionsReport";
    }

    /*
    RTGS/EFT TRANSACTION REPORT AJAX
     */
    @RequestMapping(value = "/getEftTransactionReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getEftTransactionReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String fromDate = customeQuery.get("fromdate");
        String todate = customeQuery.get("todate");
        String txnType = customeQuery.get("txnType");
        String mno = customeQuery.get("txnType");
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
        return reportRepo.getEftPaymentsReport((String) httpSession.getAttribute("branchCode"), txnType, fromDate, todate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    preview supporting document and swift message
     */
    @RequestMapping(value = "/previewSwiftMsgAndDocReport")
    public String previewSwiftMsgAndDocs(@RequestParam Map<String, String> customeQuery, Model model) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", "SWIFT MESSAGE FOR TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", reportRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        boolean classValue = false;

        model.addAttribute("txn_type", classValue);
        model.addAttribute("swiftMessage", reportRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("swift_message"));
        return "pages/transactions/modals/previewSwiftMsgAndDocReport";
    }

    /*
    get cbs transactions json format
     */
    @RequestMapping(value = "/getCbsTransactionsReportAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse getCbsTransactionsReportAjax(@Valid ReportsForm reportsForm, BindingResult result) {
        ReportsJsonResponse respone = new ReportsJsonResponse();
        if (result.hasErrors()) {
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            respone.setJsonString(reportRepo.getCbsTransactionsReport(reportsForm.getTxnType(), reportsForm.getFromdate(), reportsForm.getTodate()));
        }
        AuditTrails.setComments("View " + reportsForm.getTxnType() + " Core Banking  Transaction Report from date:" + reportsForm.getFromdate() + " To date: " + reportsForm.getTodate());
        AuditTrails.setFunctionName("/getCbsTransactionsReportAjax");
        return respone;
    }

    @RequestMapping(value = "/incomeTxnReport")
    public String incomeTransactionReport(Model model) {
        model.addAttribute("pageTitle", "INCOME TRANSACTIONS REPORT");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(httpSession.getAttribute("roleId").toString(), httpSession.getAttribute("userId").toString()));
        AuditTrails.setComments("View Income Transaction Report");
        AuditTrails.setFunctionName("/incomeTxnReport");
        return "pages/reports/incomeTransactionReport";
    }

    /*
    get cbs transactions json format
     */
    @RequestMapping(value = "/getIncomeTransactionReportAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 getIncomeTransactionReportAjax(@Valid ReportsForm2 reportsForm, BindingResult result) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            System.out.println("errorr---------");
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            respone.setJson2(reportRepo.getATMIncomeTransactionReportAjax(reportsForm.getTxnType(), reportsForm.getPeriod(), reportsForm.getMonth(), reportsForm.getYear()));
            respone.setJson(reportRepo.getPosIncomeTransactionReportAjax(reportsForm.getTxnType(), reportsForm.getPeriod(), reportsForm.getMonth(), reportsForm.getYear()));
        }
        AuditTrails.setComments("View " + reportsForm.getTxnType() + " Income Transaction Report: For the Period of :" + reportsForm.getPeriod() + " Month: " + reportsForm.getMonth() + " Year: " + reportsForm.getYear());
        AuditTrails.setFunctionName("/getIncomeTransactionReportAjax");
        return respone;
    }

    @RequestMapping(value = "/txnsTrends")
    public String transctionTrendReport(Model model) {
        model.addAttribute("pageTitle", "TRANSACTION TREND ANALYSIS ( FLOAT USAGE ANALYSIS DAILY/MONTHLY)");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(httpSession.getAttribute("roleId").toString(), httpSession.getAttribute("userId").toString()));
        AuditTrails.setComments("View  Income Transaction Report: For the Period of ");
        AuditTrails.setFunctionName("/getIncomeTransactionReportAjax");
        return "pages/reports/transctionTrendReport";
    }

    @RequestMapping(value = "/getTransactionTrendReportAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 getTransactionTrendReportAjax(@Valid ReportsForm2 reportsForm, BindingResult result) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            System.out.println("errorr---------");
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            respone.setJson(reportRepo.getTransactionTrendReportAjax(reportsForm.getTxnType(), reportsForm.getPeriod(), reportsForm.getMonth(), reportsForm.getYear()));

        }
        System.out.println("() -> " + respone);
        return respone;
    }

    @RequestMapping(value = "/exceptionTrends")
    public String exceptionTrendsReport(Model model) {
        model.addAttribute("pageTitle", "TRANSACTION FAILURE TREND ANALYSIS (UNSUCCESSFULLY TRANSACTIONS FOR BOTH ATM/POS)");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(httpSession.getAttribute("roleId").toString(), httpSession.getAttribute("userId").toString()));
        return "pages/reports/exceptionTrendsReport";
    }

    @RequestMapping(value = "/getExceptionTrendsReportAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 getExceptionTrendsReportAjax(@Valid ReportsForm2 reportsForm, BindingResult result) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        if (result.hasErrors()) {
            System.out.println("errorr---------");
            //Get error message
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)
                    );
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            respone.setJson2(reportRepo.getATMExceptionTransactionReportAjax(reportsForm.getTxnType(), reportsForm.getPeriod(), reportsForm.getMonth(), reportsForm.getYear()));
            respone.setJson(reportRepo.getPosExceptionTransactionReportAjax(reportsForm.getTxnType(), reportsForm.getPeriod(), reportsForm.getMonth(), reportsForm.getYear()));

        }
        System.out.println("(==) -> " + respone);
        return respone;
    }

    @RequestMapping(value = "/reconReport")
    public String reconciliationReport(Model model) {
        model.addAttribute("benchmarkDate", benchMarkReconDate);
        model.addAttribute("pageTitle", "RECONCILIATION REPORTS");
        model.addAttribute("txn_types", reconRepo.getReconTxntypes(httpSession.getAttribute("roleId").toString(), httpSession.getAttribute("userId").toString()));
        return "pages/reports/reconciliationReport";
    }

    @RequestMapping(value = "/queryReconCatgoryBasedOnReconType", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String queryReconCatgoryBasedOnReconType(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String reconType = customeQuery.get("reconType");
        String result = reportRepo.getReconCategoriesBasedOnReconType(reconType);
        return result;
    }

    @RequestMapping(value = "/getReconciliationReportAjax", method = RequestMethod.GET)
    public String messages(Model model, @Valid ReportsForm reportsForm, BindingResult result) {
        model.addAttribute("reconData", reportRepo.getReconciliationReportAjax(reportsForm.getTxnType(), reportsForm.getFromdate(), reportsForm.getTodate()));
        return "pages/reports/reconciliationReport";
    }

    @RequestMapping(value = "/reconciliationReports", method = RequestMethod.POST)
    @ResponseBody
    public String reconciliationReports(Model model, @Valid ReportsForm reportsForm, BindingResult result, @RequestParam Map<String, String> customeQuery, HttpServletResponse res) {
        String txnType = customeQuery.get("txnType");
        String reconCategory = customeQuery.get("reconCategory");
        String fromdate = customeQuery.get("fromdate");
        String todate = customeQuery.get("todate");
        String reportType = customeQuery.get("reportType");
        String format = customeQuery.get("reportFormat");
        String printedBy = httpSession.getAttribute("username") + "";
        return reportRepo.getReconSummaryReport(format, res, DateUtil.now("yyyyMMddHHmmss"), reconCategory, txnType, fromdate, todate, printedBy, reportType);
    }

    @RequestMapping(value = "/getAuditTrailAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAuditTrailAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String fromDate = customeQuery.get("fromdate");
        String toDate = customeQuery.get("todate");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        AuditTrails.setComments("View audit trail report fromDate: " + fromDate + " Todate: " + toDate);
        AuditTrails.setFunctionName("/getAuditTrailAjax");

        String result = reportRepo.getAuditTrailReportAjax(fromDate, toDate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
        return result;
    }

    ///auditTrail
    @RequestMapping(value = "/auditTrail")
    public String auditTrail(Model model) {
        AuditTrails.setComments("View Audit Trail url");
        AuditTrails.setFunctionName("/auditTrail");
        model.addAttribute("pageTitle", "Audit Trail Report");
        return "pages/reports/auditTrailReport";
    }

    @GetMapping(value = "/fireOnlineTransaction")
    public String fireOnlineTransaction(Model model) {
        model.addAttribute("pageTitle", "ONLINE TRANSACTIONS");
        return "modules/reports.payments/onlineTransactions";
    }


    @PostMapping(value = "/fireOnlineTransactionsAjax",produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireOnlineTransactionsAjax(@RequestParam Map<String, String> customeQuery) {
       GeneralJsonResponse jsonResponse = new GeneralJsonResponse();
       jsonResponse.setResult(reportRepo.getOnlineTransactions(customeQuery));
        return jsonResponse;
    }

}
