package com.controller.salary;

import com.DTO.GeneralJsonResponse;
import com.DTO.Teller.VikobaUpdateRequest;
import com.core.event.AuditLogEvent;
import com.helper.DateUtil;
import com.repository.salary.SalaryProcessingRepository;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import philae.api.BnUser;
import philae.api.UsRole;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Period;
import java.time.Year;
import java.util.*;

@Controller
public class SalaryProcessingController {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SalaryProcessingController.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    SalaryProcessingRepository salaryProcessingRepository;

    @GetMapping("/salaryProcessingDashboard")
    public String salaryProcessingDashboard(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","STAFF SALARY PROCESSING DASHBOARD");
        model.addAttribute("salaryProcessingPermissions", salaryProcessingRepository.getSalaryProcessingModulePermissions("/salaryProcessingDashboard", httpSession.getAttribute("roleId").toString()));
        return "/modules/salary/salaryDashboard";
    }

    @GetMapping("/currentMonthSalary")
    public String getCurrentMonthSalary(Model model, HttpSession httpSession){

        model.addAttribute("pageTitle","STAFF SALARY PROCESSING DASHBOARD" );
        List<String> arrayList = new ArrayList<>();
        for(int fromYear=2023; fromYear <= Calendar.getInstance().get(Calendar.YEAR) ; fromYear++){
            arrayList.add(String.valueOf(fromYear));
        }
//        model.addAttribute("approvalAllowed", salaryProcessingRepository.checkIfPendingForApproval(month,year));
        model.addAttribute("years", arrayList);
        return "/modules/salary/currentMonthSalary";
    }

    @PostMapping(value = "/fireGetCurrentMonthStaffSalaryAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireGetCurrentMonthStaffSalaryAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {

        String month = customeQuery.get("month");
        String year = customeQuery.get("year");
        String username = String.valueOf(session.getAttribute("username"));
        String branchNo = String.valueOf(session.getAttribute("branchCode"));
        String roleId = String.valueOf(session.getAttribute("roleId"));
        GeneralJsonResponse response = new GeneralJsonResponse();
        response.setResult(salaryProcessingRepository.fireGetCurrentMonthStaffSalaryAjax(roleId, branchNo,month,year));
        return response;
    }


    @PostMapping("/firePreviewPayrollMonthlySalary")
    public String firePreviewPayrollMonthlySalary(@RequestParam Map<String, String> customeQuery, Model model){
        String batchReference = customeQuery.get("batchReference");
//        model.addAttribute("salaries",salaryProcessingRepository.gePensionersSalaryPerBatchReference(batchReference));
        Map<String,Object> data =  salaryProcessingRepository.computeSalarySummaryBasedOnBatchReference(batchReference);
        Map<String,Object> batchSummary = salaryProcessingRepository.computeSalarySummaryBasedOnBatchReference(batchReference);
        double amount = Double.parseDouble(data.get("payroll_amount").toString());
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        model.addAttribute("batchReference",batchReference);
        model.addAttribute("batchStatus", batchSummary.get("status"));
        model.addAttribute("pageTitle","PREVIEW PAYROLL SALARY FOR "+data.get("month") + " "+ data.get("year") + " LUMP SUM AMOUNT = ( " + formatter.format(amount) +" )  TOTAL STAFF ( " +data.get("no_of_employees")+ " )" );
        return "/modules/salary/modal/previewCurrentMonthSalary";
    }

    @PostMapping("/firePreviewSuccessfullyMonthlySalary")
    public String getSuccessfullyPayrollBatch(@RequestParam Map<String, String> customeQuery, Model model){
        String batchReference = customeQuery.get("batchReference");
        String status ="C";
        List<Map<String,Object>> data =  salaryProcessingRepository.computeSalarySummaryBasedOnBatchRefAndStatus(status,batchReference);
        Map<String,Object> batchSummary = salaryProcessingRepository.computeSalarySummaryBasedOnBatchReference(batchReference);

        BigDecimal totalAmount = BigDecimal.ZERO;
        if(!data.isEmpty()){
            for(Map<String,Object> item :data){
                totalAmount = totalAmount.add((BigDecimal) item.get("credit_to_dqa"));
            }

        }
        double amount = Double.parseDouble(totalAmount.toString());
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        model.addAttribute("batchReference",batchReference);
        model.addAttribute("pageTitle","PREVIEW SUCCESSFULLY PAYROLL SALARY FOR "+ batchSummary.get("month") + " "+ batchSummary.get("year")+ " LUMP SUM AMOUNT = ( " + formatter.format(amount) +" )  TOTAL STAFF ( " +batchSummary.get("no_of_employees")+ " )" );
        return "/modules/salary/modal/previewSuccessfullyPayrollSalary";
    }
    @PostMapping("/firePreviewRejectedMonthlySalary")
    public String getRejectedPayrollInBatch(@RequestParam Map<String, String> customeQuery, Model model){
        String batchReference = customeQuery.get("batchReference");
        String status ="R";
        List<Map<String,Object>> data =  salaryProcessingRepository.computeSalarySummaryBasedOnBatchRefAndStatus(status,batchReference);
        Map<String,Object> batchSummary = salaryProcessingRepository.computeSalarySummaryBasedOnBatchReference(batchReference);

        BigDecimal totalAmount = BigDecimal.ZERO;
//        if(!data.isEmpty()){
//            for(Map<String,Object> item :data){
//                totalAmount = totalAmount.add((BigDecimal) item.get("credit_to_dqa"));
//            }
//
//        }
        double amount = Double.parseDouble(totalAmount.toString());
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        model.addAttribute("batchReference",batchReference);
        model.addAttribute("pageTitle","PREVIEW REJECTED PAYROLL SALARY FOR "+ batchSummary.get("month") + " "+ batchSummary.get("year")+ " LUMP SUM AMOUNT = ( " + formatter.format(amount) +" )  TOTAL STAFF ( " +batchSummary.get("no_of_employees")+ " )" );
        return "/modules/salary/modal/previewRejectedPayrollSalary";
    }

    @PostMapping("/firePreviewInQueueMonthlySalary")
    public String firePreviewInQueueMonthlySalary(@RequestParam Map<String, String> customeQuery, Model model){
        String batchReference = customeQuery.get("batchReference");
        String status ="S";

        List<Map<String,Object>> data =  salaryProcessingRepository.computeSalarySummaryBasedOnBatchRefAndStatus(status,batchReference);
        Map<String,Object> batchSummary = salaryProcessingRepository.computeSalarySummaryBasedOnBatchReference(batchReference);
        BigDecimal totalAmount = BigDecimal.ZERO;

        double amount = Double.parseDouble(totalAmount.toString());
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        model.addAttribute("batchReference",batchReference);
        model.addAttribute("pageTitle","PREVIEW IN QUEUE SALARY FOR "+ batchSummary.get("month") + " "+ batchSummary.get("year")+ " LUMP SUM AMOUNT = ( " + formatter.format(amount) +" )  TOTAL STAFF ( " +batchSummary.get("no_of_employees")+ " )" );
        return "/modules/salary/modal/previewQueuePayrollSalary";
    }


    @PostMapping(value = "/firePreviewPayrollPerBatchAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String firePreviewPayrollPerBatchAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = String.valueOf(session.getAttribute("username"));
        String branchNo = String.valueOf(session.getAttribute("branchCode"));
        String roleId = String.valueOf(session.getAttribute("roleId"));
        String batchReference = customeQuery.get("batchReference");

        return salaryProcessingRepository.firePreviewPayrollBatchTransactionsAjax(batchReference, roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "/firePreviewSuccessfullyMonthlySalaryAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String firePreviewCSuccessfullyMonthlySalaryAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = String.valueOf(session.getAttribute("username"));
        String branchNo = String.valueOf(session.getAttribute("branchCode"));
        String roleId = String.valueOf(session.getAttribute("roleId"));
        String batchReference = customeQuery.get("batchReference");
        String status = "C";
        return salaryProcessingRepository.firePreviewPayrollBatchTransactionsBasedOnStatusAjax(batchReference,status, roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "/firePreviewRejectedMonthlySalaryAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRejectedMonthlyPayroll(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = String.valueOf(session.getAttribute("username"));
        String branchNo = String.valueOf(session.getAttribute("branchCode"));
        String roleId = String.valueOf(session.getAttribute("roleId"));
        String batchReference = customeQuery.get("batchReference");
        String status = "R";
        return salaryProcessingRepository.firePreviewPayrollBatchTransactionsBasedOnStatusAjax(batchReference,status, roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "/firePreviewInQueuePayrollSalaryAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getInQueueMonthlyPayroll(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        //audit trails
        String username = String.valueOf(session.getAttribute("username"));
        String branchNo = String.valueOf(session.getAttribute("branchCode"));
        String roleId = String.valueOf(session.getAttribute("roleId"));
        String batchReference = customeQuery.get("batchReference");
        String status = "S";
        return salaryProcessingRepository.firePreviewPayrollBatchTransactionsBasedOnStatusAjax(batchReference,status, roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/fireAuthCurrentMonthSalary", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthCurrentMonthSalary(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String postingRole = (String) httpsession.getAttribute("postingRole");
            String batchReference = customeQuery.get("batchReference");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {

                        //audit trails
                        String username = String.valueOf(session.getAttribute("username"));
                        String branchNo = String.valueOf(session.getAttribute("branchCode"));
                        String roleId = String.valueOf(session.getAttribute("roleId"));
                        //PROCESSING SALARY TO BRINJAL
                        salaryProcessingRepository.processSalaryToBrinjal(batchReference,username);
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthCurrentMonthSalary", "SUCCESS", "Authorize payroll batch"));
                         result = "{\"result\":\"0\",\"message\":\"Transaction are initiated: \"}";

                        break;
                    } else {
                        //audit trails
                        String username = String.valueOf(session.getAttribute("username"));
                        String branchNo = String.valueOf(session.getAttribute("branchCode"));
                        String roleId = String.valueOf(session.getAttribute("roleId"));
                        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthCurrentMonthSalary", "Failed", "Cannot authorize salary while the role is not selected for posting"));

                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            //audit trails
            String username = String.valueOf(session.getAttribute("username"));
            String branchNo = String.valueOf(session.getAttribute("branchCode"));
            String roleId = String.valueOf(session.getAttribute("roleId"));
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/fireAuthCurrentMonthSalary", "Failed", "Exception occured during authorization: MESSAGE->" + ex.getMessage()));

            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }


}
