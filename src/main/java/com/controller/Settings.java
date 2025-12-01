/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.Reports.AuditTrails;
import com.DTO.Settings.ServiceProvidersForm;
import com.DTO.Settings.*;
import com.DTO.Teller.FormJsonResponse;
import com.DTO.psssf.PensionerLoanVerificationResponse;
import com.config.SYSENV;
import com.controller.itax.CMSpartners.JsonResponse;
import com.core.MY_Controller;
import com.entities.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.helper.DateUtil;
import com.helper.MaiString;
import com.repository.Settings_m;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import com.service.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import philae.api.*;

/**
 *
 * @author MELLEJI
 */
@Controller
public class Settings extends MY_Controller {

    @Autowired
    Settings_m settingsRepo;
    @Autowired
    SYSENV systemVariable;

    @Autowired
    ObjectMapper jacksonMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    boolean available = true;
    String result = null;

    @RequestMapping(value = "/systemSettings")
    public String systemSettings(HttpSession session, Model model) {
        AuditTrails.setComments("View System settings");
        AuditTrails.setFunctionName("/systemSettings");
        return "settings/systemSettings";
    }

    //get Domain Controller
    @RequestMapping(value = "/getDomainControllerAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getDomainControllers(HttpSession session) {
        String json = settingsRepo.getDomainControllers();
        System.out.println("RESPONSE:" + json);
        AuditTrails.setComments("View System active domain controller configured");
        AuditTrails.setFunctionName("/getDomainControllerAjax");
        return json;
    }
    //get Escallation Matrix

    @RequestMapping(value = "/getEscallationMatrixAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getEscalationMatrix(HttpSession session) {
        String json = settingsRepo.getEscalationMatrix().toString();
        AuditTrails.setComments("View escalation matrix set");
        AuditTrails.setFunctionName("/getEscalationMatrixAjax");
        return json;
    }

    @RequestMapping(value = "/setupNewReconType")
    public String setNewReconType(HttpSession session, Model model) {
        model.addAttribute("reconTypes", settingsRepo.getReconTypes());
        AuditTrails.setComments("View setup recon types");
        AuditTrails.setFunctionName("/setupNewReconType");
        return "settings/reconTypeSetup";
    }

    @RequestMapping(value = "/reconTypeMappings")
    public String reconTypeMappings(HttpSession session, Model model) {
        model.addAttribute("reconTypes", settingsRepo.getReconTypes());
        AuditTrails.setComments("View  recon types mapping");
        AuditTrails.setFunctionName("/reconTypeMappings");
        return "settings/reconTypeMappings";
    }

    @RequestMapping(value = {"/saveReconType"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 saveReconType(@Valid ReconFormSetup reconForm, BindingResult result) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
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
            //save the records on the database
            settingsRepo.saveReconType(reconForm.getTxnType(), reconForm.getTtype());
            respone.setJson(settingsRepo.getReconTypes());
        }
        AuditTrails.setComments("Save new recon types Created on the system: recon type created=" + reconForm.getTxnType() + " main Component: " + reconForm.getTtype());
        AuditTrails.setFunctionName("/reconTypeMappings");
        return respone;
    }

    @RequestMapping(value = {"/getUnmappedReconTypes"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 getUnmappedReconTypes(@RequestParam Map<String, String> customeQuery) {
        String reconType = customeQuery.get("reconType");
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        respone.setJson(settingsRepo.getReportsSetup(reconType));
        respone.setJson2(settingsRepo.getMappedReports(reconType));
        AuditTrails.setComments("View un mapped recon Types");
        AuditTrails.setFunctionName("/getUnmappedReconTypes");
        return respone;
    }

    @RequestMapping(value = {"/saveReconMappings"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 saveReconMappings(@Valid ReconTypeMappingForm reconForm, BindingResult result, HttpSession session) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
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
            //save the records on the database
            settingsRepo.saveReconTypeReportsMapping(reconForm.getReconType(), reconForm.getReportType(), reconForm.getReportName(), session.getAttribute("username").toString());
            respone.setJson2(settingsRepo.getMappedReports(reconForm.getReconType()));
            respone.setJson(settingsRepo.getReconTypes());
        }
        AuditTrails.setComments("Save Recon mapping on the system, new mapped recon name:" + reconForm.getReconType() + " display name mapped to :" + reconForm.getReportName());
        AuditTrails.setFunctionName("/saveReconMappings");
        return respone;
    }

    @RequestMapping(value = "/getReconSetupAjax", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 getReconType(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
        respone.setJson(settingsRepo.getReconTypes());
        AuditTrails.setComments("view the recon setups");
        AuditTrails.setFunctionName("/getReconSetupAjax");
        return respone;
    }

    @RequestMapping(value = "/reconComponents")
    public String reconComponents(Model model, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("reconType", settingsRepo.getReconTypeById(customeQuery.get("type_id")));
        model.addAttribute("ComponentTypes", settingsRepo.getReconComponents(customeQuery.get("type_id")));
        AuditTrails.setComments("view recon components");
        AuditTrails.setFunctionName("/reconComponents");
        return "settings/reconComponents";
    }

    @RequestMapping(value = {"/saveReconComponents"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ReportsJsonResponse2 saveReconComponent(@Valid ReconComponentForm componentForm, BindingResult result, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        ReportsJsonResponse2 respone = new ReportsJsonResponse2();
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
            //save the records on the database
            System.out.println("here we are: " + componentForm.getName() + " code:" + componentForm.getCode() + " ttype: " + customeQuery.get("ttype"));
            settingsRepo.saveReconComponent(componentForm.getName(), componentForm.getCode(), customeQuery.get("ttype"));
        }
        AuditTrails.setComments("Save recon components, Name:" + componentForm.getName() + " Code:" + componentForm.getCode());
        AuditTrails.setFunctionName("/saveReconComponents");
        return respone;
    }

    @RequestMapping({"/manageServiceProviders"})
    public String manageServiceProviders(HttpSession session, Model model) {
        model.addAttribute("banks", settingsRepo.getBanksLists());
        return "settings/manageServiceProviders";
    }

    @RequestMapping({"/cuttOffExtensionForm"})
    public String cuttOffExtensionForm(HttpSession session, Model model) {
        return "settings/transfer-cutt-off";
    }

    @RequestMapping(value = {"/cuttOffExtension"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public FormJsonResponse cuttOffExtension(@Valid TransferCuttOff spForm, BindingResult result, HttpSession session) {
        FormJsonResponse respone = new FormJsonResponse();
        if (result.hasErrors()) {
            Map<String, String> errors = (Map<String, String>) result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
//                public int addTransferTimeExtension(String transferType, String extendedTime,String dayOfWeek,String modifiedBy) {
            int res = this.settingsRepo.addTransferTimeExtension(spForm.getTransferType(), spForm.getToTime(), spForm.getDayOfWeek(), session.getAttribute("username").toString());
            if (res == 1) {
                respone.setValidated(true);
                respone.setJsonString("{\"result\":" + res + ",\"message\":\"Success\"}");
            } else {
                respone.setValidated(false);
                respone.setJsonString("{\"result\":" + res + "\"message\":\"Failed\"}");
            }
        }
        return respone;
    }

    @RequestMapping({"/banksForm"})
    public String banksForm(HttpSession session, Model model) {
        model.addAttribute("banks", this.settingsRepo.getBanks());
        return "settings/banksForm";
    }

    @RequestMapping(value = {"/addNewBank"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public FormJsonResponse addNewBank(@Valid AddBank spForm, BindingResult result, HttpSession session) {
        FormJsonResponse respone = new FormJsonResponse();
        if (result.hasErrors()) {
            Map<String, String> errors = (Map<String, String>) result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
//                public int addTransferTimeExtension(String transferType, String extendedTime,String dayOfWeek,String modifiedBy) {
            int res = this.settingsRepo.addNewBank(spForm.getBankName().trim(), spForm.getSwiftCode().trim(), spForm.getIdentifier().trim(), session.getAttribute("username").toString());
            if (res == 1) {
                respone.setValidated(true);
                respone.setJsonString("{\"result\":" + res + ",\"message\":\"Success\"}");
            } else {
                respone.setValidated(false);
                respone.setJsonString("{\"result\":" + res + "\"message\":\"Failed\"}");
            }
        }
        return respone;
    }

    @RequestMapping(value = {"/getBanksAjax"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String getBanksAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = (customeQuery.get("search[value]") != null) ? ((String) customeQuery.get("search[value]")).trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return this.settingsRepo.getServiceProvidersList(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }




    @RequestMapping(value = {"/addServiceProvider"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public FormJsonResponse addServiceProvider(@Valid ServiceProvidersForm spForm, BindingResult result, HttpSession session) {
        FormJsonResponse respone = new FormJsonResponse();
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));
            respone.setValidated(false);
            respone.setErrorMessages(errors);
        } else {
            respone.setValidated(true);
            System.out.println("PERMISSIONFORM:" + spForm);
            int res = this.settingsRepo.saveServiceProvider(spForm, (String) session.getAttribute("username"));
            if (res == 1) {
                respone.setValidated(true);
                respone.setJsonString("{\"result\":" + res + ",\"message\":\"Success\"}");
            } else {
                respone.setValidated(false);
                respone.setJsonString("{\"result\":" + res + "\"message\":\"Failed\"}");
            }
        }
        return respone;
    }

    @RequestMapping(value = {"/getServiceProvidersAjax"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String getServiceProvidersAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = (customeQuery.get("search[value]") != null) ? ((String) customeQuery.get("search[value]")).trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return this.settingsRepo.getServiceProvidersList(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/terminalsList", method = RequestMethod.GET)
    public String terminalsList(Model model, HttpSession session) {
//        model.addAttribute("terminals", this.settingsRepo.listTerminals());
        model.addAttribute("roles", settingsRepo.terminalRoles());
        return "modules/ebanking/terminalsList";
    }

    @RequestMapping(value = "/terminalTransactions", method = RequestMethod.GET)
    public String terminalTransactions(Model model, HttpSession session) {
//        model.addAttribute("terminals", this.settingsRepo.listTerminals());
        model.addAttribute("roles", settingsRepo.terminalRoles());
        return "modules/ebanking/terminalTransactions";
    }

    @RequestMapping(value = "/getTerminalTransactionsAjax", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String getTerminalTransactionsAjax(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String draw = params.get("draw");
        String start = params.get("start");
        String rowPerPage = params.get("length");
        String searchValue = (params.get("search[value]") != null) ? params.get("search[value]").trim() : "";
        String columnIndex = params.get("order[0][column]");
        String columnName = params.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = params.get("order[0][dir]");
        return settingsRepo.getTerminalTransactions(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    LIST TERMINALS
     */
    @RequestMapping(value = "/getTerminalsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTerminalsAjax(@RequestParam Map<String, String> customerQuery, HttpServletRequest request, HttpSession session) {
        String draw = customerQuery.get("draw");
        String fromDate = customerQuery.get("createdDate") + " " + customerQuery.get("fromTime") + ":00";
        String toDate = customerQuery.get("createdDate") + " " + customerQuery.get("toTime") + ":59";
        String start = customerQuery.get("start");
        String rowPerPage = customerQuery.get("length");
        String searchValue = customerQuery.get("search[value]") != null ? customerQuery.get("search[value]").trim() : "";
        String columnIndex = customerQuery.get("order[0][column]");
        String columnName = customerQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customerQuery.get("order[0][dir]");
        String locked = customerQuery.get("locked");
        String blocked = customerQuery.get("blocked");
//        LtResponse listTerminalsResponse = this.settingsRepo.listTerminals(
//                session.getAttribute("username").toString(),
//                fromDate, toDate, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
//        List<AxTerminal> terminals = listTerminalsResponse.getTerminals().getTerminal();
//        String jsonString = new Gson().toJson(terminals);
//
//        return "{\"draw\":" + draw + ",\"aaData\":" + jsonString + "}";
        return this.settingsRepo.listTerminals(
                session.getAttribute("username").toString(), locked, blocked, fromDate, toDate,
                draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder
        );

    }


    @RequestMapping(value = "/unlockTerminal", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String unlockTerminal(@RequestParam("id") String id, HttpServletRequest request, HttpSession session) {
        return settingsRepo.unlockTerminal(id, (String) session.getAttribute("username"));
    }

    @RequestMapping(value = "/blockTerminal", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String blockTerminal(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String id = params.get("id");
        String mobileNumber = params.get("phone");
        String remarks = params.get("remarks");
        sendSms(mobileNumber, "Ndugu Wakala,\nMashine yako imefungiwa huduma ya uwakala wa Tanzania Commercial Bank (TCB).\nWasiliana na huduma kwa wateja kwa maelezo zaidi.");
        return settingsRepo.blockTerminal(id, remarks, (String) session.getAttribute("username"));
    }

    @RequestMapping(value = "/unblockTerminal", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String unblockTerminal(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String id = params.get("id");
        String mobileNumber = params.get("phone");
        sendSms(mobileNumber, "Ndugu Wakala,\nMashine yako imerudishiwa huduma ya uwakala wa Tanzania Commercial Bank (TCB).");
        return settingsRepo.unblockTerminal(id, (String) session.getAttribute("username"));
    }

    @RequestMapping(value = "/resetPassword", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String resetPassword(@RequestParam Map<String, String> params, HttpServletRequest request, HttpSession session) {
        String id = params.get("id");
        String mobileNumber = params.get("phone");
        sendSms(mobileNumber, "Ndugu Wakala,\nPIN yako ya mashine ya uwakala wa Tanzania Commercial Bank (TCB) imerisetiwa. Namba yako ya siri ni 0000.\nUnashauriwa kubadili PIN yako pindi utakapo anza kutumia mashine yako.");
        return settingsRepo.resetPassword(id, (String) session.getAttribute("username"));
    }

    @RequestMapping(value = {"/addTerminal"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String addTerminal(@RequestParam Map<String, String> customerQuery, HttpServletRequest request, HttpSession session) {
        AqResponse aqResponse = new AqResponse();
        CnAccount account = new Gson().fromJson(customerQuery.get("account"), CnAccount.class);
        //Simulation
/*
*
        CnAccount account = new CnAccount();
        account.setAccountName("Arthur Godwin Ndossi");
        account.setAccountNumber("110210000408");
        account.setAccountType("");
        account.setAcctId(23L);
        account.setCustCat("INDIVIDUAL");
        account.setProductId(34L);
        account.setShortName("ARTHUR NDOSSI");
        account.setClearedBalance(new BigDecimal(1000000000));
        account.setStatus("ACTIVE");
        CnBranch branch = new CnBranch();
        branch.setBuCode("110");
        branch.setBuId(24L);
        branch.setStatus("ACTIVE");
        branch.setBuName("MKWEPU");
        branch.setGlPrefix("");
        CnCurrency currency = new CnCurrency();
        currency.setCode("864");
        currency.setName("TZS");
        currency.setId(2L);
        currency.setPoints(2);
        account.setBranch(branch);
        account.setCurrency(currency);
*/
        aqResponse.setAccount(account);
        Long customerId = account.getCustId();
        String mobileNumber = MaiString.formatMobileNo(this.settingsRepo.getMobileNumber(customerId));
//        String mobileNumber = MaiString.formatMobileNo(customerQuery.get("phone"));
        try {
            int res = this.settingsRepo.saveTerminal(
                    session.getAttribute("username").toString(),
                    mobileNumber,
                    customerQuery.get("terminalId"),
                    customerQuery.get("role"),
                    aqResponse
            );
            LOGGER.info("Save Terminal Result: " + res);
            if (res == 0) {
                sendSms(mobileNumber, "Ndugu Wakala,\nUmefanikiwa kujiunga na huduma ya uwakala wa Tanzania Commercial Bank (TCB). Namba yako ya siri ya kuanzia ni 0000.\nUnashauriwa kubadili PIN yako pindi utakapo anza kutumia mashine yako.");
                return "{\"result\":" + res + ",\"message\":\"Success\"}";
            } else {
                return "{\"result\":" + res + ",\"message\":\"Failed due to connection timeout, Please try again later!\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            if (e instanceof DataIntegrityViolationException)
                message = "Terminal already exists";
            return "{\"result\":99,\"message\": \"" + message + "\"}";
        }
    }

    @RequestMapping(value = {"/editTerminal"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String editTerminal(@RequestParam Map<String, String> customerQuery, HttpServletRequest request, HttpSession session) {
        AqResponse aqResponse = searchAccount(customerQuery.get("account"));
        String mobileNumber = MaiString.formatMobileNo(customerQuery.get("phone"));
        try {
            int res = this.settingsRepo.editTerminal(
                    session.getAttribute("username").toString(),
                    customerQuery.get("id"),
                    customerQuery.get("terminal"),
                    customerQuery.get("firstname"),
                    customerQuery.get("middleName"),
                    customerQuery.get("lastname"),
                    mobileNumber,
                    customerQuery.get("email"),
                    customerQuery.get("appVersion"),
                    customerQuery.get("tin"),
                    customerQuery.get("region"),
                    customerQuery.get("district"),
                    customerQuery.get("ward"),
                    customerQuery.get("town"),
                    customerQuery.get("location"),
                    customerQuery.get("latitude"),
                    customerQuery.get("longitude"),
                    aqResponse
            );
            LOGGER.info("Edit Terminal Result: " + res);
            if (res == 0) {
                return "{\"result\":" + res + ",\"message\":\"Success\"}";
            } else {
                return "{\"result\":" + res + ",\"message\":\"Failed due to connection timeout, Please try again later!\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            if (e instanceof DataIntegrityViolationException)
                message = "Terminal already exists";
            return "{\"result\":99,\"message\": \"" + message + "\"}";
        }
    }

    @RequestMapping(value = {"/sendCallback"}, method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public void sendCallback() {
        available = false;
        result = "Response success!";
    }

    @RequestMapping(value = {"/testing"}, method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String testing() {
        while (available) {
            try {
                Thread.sleep(500);
                if (result != null) {
                    available = true;
                    String response = "{\"success\": \"true\", \"message\": \"" + result + "\"}";
                    result = null;
                    return response;
                }
            } catch (Exception e) {
                e.printStackTrace();
                available = false;
            }
        }
        return "{\"success\": \"false\", \"message\": \"\"}";
    }

    @RequestMapping(value = {"/searchAccount"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String searchAccount(@RequestParam Map<String, String> customerQuery, HttpServletRequest request, HttpSession session) {
        String accountNo = customerQuery.get("accountNo");
        return new Gson().toJson(searchAccount(accountNo), AqResponse.class);
    }

    private AqResponse searchAccount(String accountNo) {
        AqResponse response;
        try {
            response = this.settingsRepo.getTerminalAccount(accountNo);
        } catch (Exception e) {
            e.printStackTrace();
            response = new AqResponse();
            response.setResult(-1);
            response.setMessage("Sorry, no network connection... Please try again later!");
        }
        return response;
    }

    private void sendSms(String msgTo, String msgBody) {
        String request = "<methodCall>"
                + "<methodName>TPB.SENDSMS</methodName>"
                + "<params>"
                + "<param><value><string>" + msgTo + "</string></value></param>"
                + "<param><value><string>" + msgBody + "</string></value></param>"
                + "<param><value><string>" + System.currentTimeMillis() + "</string></value></param>"
                + "</params>"
                + "</methodCall>";
        String smsResponse = HttpClientService.sendXMLRequest(request, this.systemVariable.SMSC_URL);
        LOGGER.info("REQUEST TO GATEWAY: {}", request);
        LOGGER.info("RAW RESPONSE FROM GATEWAY: {}", smsResponse);
    }

    @RequestMapping(value = "/loginStatusReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loginStatusReport() {
        return settingsRepo.getLoginStatus();
    }

    @RequestMapping(value = "/lockStatusReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String lockStatusReport() {
        return settingsRepo.getLockedStatus();
    }

    @RequestMapping(value = "/blockStatusReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String blockStatusReport() {
        return settingsRepo.getBlockedStatus();
    }

    @RequestMapping(value = "/utilitiesReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String utilitiesReport(@RequestParam Map<String, String> param) {
        String startDate = param.get("startDate");
        return settingsRepo.getUtilitiesReport(startDate);
    }

    @RequestMapping(value = "/airtimeReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String airtimeReport(@RequestParam Map<String, String> param) {
        String startDate = param.get("startDate");
        return settingsRepo.getAirtimeReport(startDate);
    }

    @RequestMapping(value = "/tvReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String tvReport(@RequestParam Map<String, String> param) {
        String startDate = param.get("startDate");
        return settingsRepo.getTVReport(startDate);
    }

    @RequestMapping(value = "/agencyTransactionsReport")
    public String agencyTransactionsReport(Model model) {
        model.addAttribute("pageTitle", "AGENCY BANKING TRANSACTIONS REPORTS");
        model.addAttribute("txnTypes", settingsRepo.getTransactions(DateUtil.now("yyyy-MM-dd")));
        AuditTrails.setComments("View Agency Banking Transactions Report");
        AuditTrails.setFunctionName("/agencyTransactionsReport");
        return "modules/ebanking/agencyDashboard";
    }

    @RequestMapping(value = "/getAgencyTransactionsReport", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAgencyTransactionsReport(@RequestParam Map<String, String> reportQuery) {
        String fromDate = reportQuery.get("createdDate") + " " + reportQuery.get("fromTime") + ":00";
        return settingsRepo.getTransactionAjax(fromDate);
    }

    @RequestMapping(value = "/loadAppVersions", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loadAppVersions() {
        return settingsRepo.loadAppVersions();
    }

    @RequestMapping(value = "/api/loadRegions", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loadRegions() {
        return settingsRepo.loadRegions();
    }

    @RequestMapping(value = "/api/loadDistricts", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loadDistricts(@RequestParam Map<String, String> params) {
        return settingsRepo.loadDistrictsByRegion(params.get("region"));
    }

    //    VIEW TERMINAL ROLES
    @RequestMapping(value = {"/api/loadTerminalRoles"}, method = {RequestMethod.GET}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String loadTerminalRoles() {
        return settingsRepo.loadTerminalRoles();
    }

    //    EDIT TERMINAL ROLE
    @RequestMapping(value = {"/api/editTerminalRole"}, method = {RequestMethod.POST}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String editTerminalRole(@RequestParam Map<String, String> params) {
        String terminalId = params.get("id");
        String roleId = params.get("roleId");
        return settingsRepo.editTerminalRole(terminalId, roleId);
    }

    @RequestMapping(value = "/firePreviewEditSP")
    public String previewServiceProvider(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        model.addAttribute("pageTitle", "EDIT SERVICE PROVIDER DETAILS");
        model.addAttribute("banks", settingsRepo.getBanksLists());
        model.addAttribute("spData", settingsRepo.getSProviderData(customeQuery.get("sproviderId")));
        return "settings/modal/editServiceProvider";
    }

    @PostMapping(value = "/fireUpdateServiceProvider")
    @ResponseBody
    public JsonResponse fireUpdateServiceProvider(@Valid ServiceProvidersForm spForm, BindingResult bindingResult,@RequestParam Map<String, String> customeQuery, HttpSession session) {
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
            String spId = customeQuery.get("spId");
//            LOGGER.info("form parameters ...{} and spId is ... {}", spForm, customeQuery.get("spId"));
            if (1 == settingsRepo.updateServiceProvider(spForm,spId, session.getAttribute("username")+"")) {
                response.setStatus("SUCCESS");
                response.setResult("Service Provider updated successfully");
            } else {
                response.setStatus("ERROR");
                response.setResult("failed to submit data into the database");
            }
        }
        return response;
    }

    @RequestMapping(value = {"/api/searchTerminal"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public String searchTerminal(@RequestBody String payLoad, HttpServletRequest request, HttpSession session) throws JsonProcessingException {
        SearchAgentRequest searchAgentRequest = this.jacksonMapper.readValue(payLoad, SearchAgentRequest.class);
        return this.settingsRepo.searchTerminalNo(searchAgentRequest.getTerminal());
    }
}
