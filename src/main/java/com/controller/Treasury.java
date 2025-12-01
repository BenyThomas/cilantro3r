/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.Teller.FormJsonResponse;
import com.DTO.treasury.SpecialRateDealNumber;
import com.config.SYSENV;
import com.helper.DateUtil;
import com.repository.TreasuryRepo;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author melleji.mollel
 */
@Controller
public class Treasury {

    @Autowired
    SYSENV systemVariables;
    @Autowired
    TreasuryRepo treasuryRepo;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Eft.class);

    /*
     *TREASURY PAYMENT DASHBOARD
     */
    @RequestMapping(value = "/treasuryDashboard", method = RequestMethod.GET)
    public String treasuryDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "TREASURY | TRADE FINANCE");
        model.addAttribute("paymentsPermissions", treasuryRepo.getTreasuryModulePermissions("/treasuryDashboard", session.getAttribute("roleId").toString()));
        return "modules/treasury/treasuryDashboard";
    }

    /*
    TREASURY SPECIAL RATES REQUESTS
     */
    @RequestMapping(value = "/rtgsSpecialRatesOnWorkFlow", method = RequestMethod.GET)
    public String rtgsSpecialRatesOnWorkFlow(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "RTGS TRANSACTIONS THAT REQUIRES SPECIAL RATES");
        model.addAttribute("paymentsPermissions", treasuryRepo.getTreasuryModulePermissions("/treasuryDashboard", session.getAttribute("roleId").toString()));
        return "modules/treasury/rtgsSpecialRatesOnWorkFlow";
    }

    /* 
     * TREASURY SPECIAL RATES REQUESTS AJAX
     */
    @RequestMapping(value = "/getRtgsSpecialRatesOnWorkFlowAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRtgsSpecialRatesOnWorkFlowAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return treasuryRepo.getRtgsSpecialRatesOnWorkFlowAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
     *preview special rate transaction
     */
    @RequestMapping(value = "/previewSpecialRateTxn")
    public String previewRTGSMessage(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "SPECIAL RATE WITH A SWIFT MESSAGE  WITH REFERENCE: " + customeQuery.get("reference"));
//        model.addAttribute("supportingDocs", rtgsRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("conversionCurrency", customeQuery.get("conversionCurrency"));
        model.addAttribute("rubikonRate", customeQuery.get("systemRate"));
        model.addAttribute("requestedRate", customeQuery.get("requestedRate"));
        model.addAttribute("fxType", customeQuery.get("fxType"));
        model.addAttribute("swiftMessage", treasuryRepo.getSwiftMessage(customeQuery.get("reference")).get(0).get("swift_message"));
        return "modules/treasury/modals/previewSpecialRateTxnReq";
    }

    /*
    BRANCH AUTHORIZE TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/approveSpecialRate", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveSpecialRate(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        String reference = customeQuery.get("reference");
        String approvedRate = customeQuery.get("approvedRate");
        String remarks = customeQuery.get("references");
        //if (postingRole != null) {
        int res = treasuryRepo.saveApprovedSpecialRate(approvedRate, httpsession.getAttribute("username").toString(), reference, remarks);
        if (res == 1) {
            //send the sms to BRANCH NOTIFYING THE TRANSACTIONS 
            result = result = "{\"result\":\"0\",\"message\":\"Rate successfully submitted!! \"}";
        } else {
            result = result = "{\"result\":\"99\",\"message\":\"Failed to submit special rate please try again !!!: \"}";

        }
        //}
        return result;
    }

    @RequestMapping(value = "/bookTransfer", method = RequestMethod.GET)
    public String rtgsTransfer(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        System.out.println("SESSION ROLE: " + session.getAttribute("roleId").toString());
        LOGGER.info("BOOK TRANSFER MODULE: ");
//        model.addAttribute("banks", rtgsRepo.getBanksList());
        return "modules/treasury/bookTransfer";

    }

    /*
     *TREASURY PAYMENT DASHBOARD
     */
    @RequestMapping(value = "/specialRateForm", method = RequestMethod.GET)
    public String specialRateForm(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "GENERATE SPECIAL RATE DEAL NUMBER");
        model.addAttribute("paymentsPermissions", treasuryRepo.getTreasuryModulePermissions("/treasuryDashboard", session.getAttribute("roleId").toString()));
        return "modules/treasury/specialRateForm";
    }

    //
    /*
    *GET EXCHANGE RATE FROM CORE BANKING....
     */
    @RequestMapping(value = "/getCoreBankingRates", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String coreBankingExchangeRate(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String result = "";
        String currency = customeQuery.get("sendingCurrency");
        return treasuryRepo.getEchangeRateFrmCBS(customeQuery.get("accountNo"), currency);

    }

    @RequestMapping(value = "/initiateSpecialRateDealNumberGeneration", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse createIBclientProfileAccount(@Valid SpecialRateDealNumber specialRate, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, BindingResult result) throws Exception {
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
            String dealNumber = systemVariables.getDealNumberGenerated();
            String reference = DateUtil.now("yyyMMddHHmmss");
            System.out.println("SPECIAL RATE DATA: {}" + specialRate.toString());
            int res = treasuryRepo.createSpecialRateDealNo(specialRate, reference, session.getAttribute("username") + "", dealNumber);
            if (res != -1) {
                response.setValidated(true);
                response.setJsonString("Special Rate deal Number generated successfuly: DEAL NO:" + dealNumber + " upon appoval please communicate the deal Number to customer");
            } else {
                response.setValidated(false);
                response.setJsonString("An error occured during creation of client profile");
            }
        }
        return response;
    }

    /*
    TREASURY SPECIAL RATES REQUESTS
     */
    @RequestMapping(value = "/pendingDealNumbers", method = RequestMethod.GET)
    public String pendingDealNumbers(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "DEAL NUMBERS PENDING FOR APPROVALS");
        model.addAttribute("paymentsPermissions", treasuryRepo.getTreasuryModulePermissions("/treasuryDashboard", session.getAttribute("roleId").toString()));
        return "modules/treasury/pendingDealNumbers";
    }

    /* 
     * TREASURY SPECIAL RATES REQUESTS AJAX
     */
    @RequestMapping(value = "/getPendingDealNumbersAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPendingDealNumbersAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return treasuryRepo.getPendingDealNumbers(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/previewDealNumberForApproval")
    public String previewDealNumberForApproval(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "APPROVE A DEAL NUMBER FOR GIVEN RATE BELOW ");
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("dealNumberDetails", treasuryRepo.getDealNumberForApproval(customeQuery.get("reference")));
        return "modules/treasury/modals/previewDealNumberForApproval";
    }

    @RequestMapping(value = "/approveDealNumber", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveDealNumber(HttpSession session, @RequestParam Map<String, String> customeQuery) {
        String reference = customeQuery.get("reference");
        String result = "";
        try {
            String type = customeQuery.get("status");

            int res = treasuryRepo.approveDealNumber(reference, type, session.getAttribute("username") + "");
            if (res != -1) {
                result = "{\"result\":\"0\",\"message\":\"Successfully " + type + " the Deal Number!! \"}";
            } else {
                result = "{\"result\":\"99\",\"message\":\"An error occured during approign deal number!! \"}";
            }
        } catch (Exception e) {
            result = "{\"result\":\"96\",\"message\":\"General Error Occured \"}";

        }
        return result;
    }
}
