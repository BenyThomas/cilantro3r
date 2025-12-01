/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.CashMovementRequestObj;
import com.DTO.Ebanking.CreateCardRequest;
import com.DTO.GeneralJsonResponse;
import com.DTO.Teller.CustomJsonResponse;
import com.DTO.psssf.LoanVerificationReq;
import com.DTO.psssf.PensionLookupReq;
import com.DTO.psssf.PensionStatement;
import com.DTO.psssf.PensionStatementResponse;
import com.DTO.psssf.PensionVerificationForm;
import com.DTO.recon.AdjustedRecon;
import com.config.SYSENV;
import com.core.event.AuditLogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.helper.DateUtil;
import com.repository.CreditRepo;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import com.repository.reports.ReconReportsRepo;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jpos.q2.cli.LOGGER_BENCHMARK;
import org.json.JSONArray;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import static com.sun.xml.bind.v2.util.EditDistance.editDistance;

/**
 * @author Dell
 */
@Controller
public class Credit {

    @Autowired
    CreditRepo creditRepo;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Credit.class);
    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    SYSENV systemVariable;

    /*
    CREDIT PAYMENT DASHBOARD
     */
    @RequestMapping(value = "/creditDashboard", method = RequestMethod.GET)
    public String eftPaymentsDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "WASTAAFU MODULE");
        model.addAttribute("paymentsPermissions", creditRepo.getCreditModulePermissions("/creditDashboard", session.getAttribute("roleId").toString()));
        return "modules/credit/creditDashboard";
    }

    /*
     *GET PENSIONERS DETAILS
     */
    @RequestMapping(value = "/pensionersDetails", method = RequestMethod.GET)
    public String pensionersDetails(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENSIONERS LOAN DETAILS ");
        model.addAttribute("paymentsPermissions", creditRepo.getCreditModulePermissions("/creditDashboard", session.getAttribute("roleId").toString()));
        return "modules/credit/pensionersDetails";
    }

    /*
     * GET PENSIONERS AJAX
     */
    @RequestMapping(value = "/getPensionersDetailsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPensionersDetailsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return creditRepo.getPensionersDetailsAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/firePensionerVerification", method = RequestMethod.GET)
    public String pensionerVerification(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENSIONERS VERIFICATONS DASHBOARD");
        return "modules/credit/pensionersVerification";
    }

    /*
     * GET PENSIONERS VERIFICATIONS AJAX
     */
    @RequestMapping(value = "/firePensionerVerificationAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse firePensionerVerificationAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) throws JsonProcessingException {
        GeneralJsonResponse gjr = new GeneralJsonResponse();
        String finalRes = creditRepo.getPensionersVerificationAjax(customeQuery);
        PensionLookupReq resp = jacksonMapper.readValue(finalRes, PensionLookupReq.class);

        if (resp.getCode().equalsIgnoreCase("200")) {
            gjr.setStatus("200");
            gjr.setResult(resp.getDetails());
        } else if((resp.getCode().equalsIgnoreCase("400") || (resp.getCode().equalsIgnoreCase("404")))) {
            //No record found
            gjr.setStatus("400");
            gjr.setResult(null);
        }else{
            //general failure
            gjr.setStatus("99");
            gjr.setResult(null);
        }
        return gjr;
    }

    /*
     * GET PENSIONERS STATEMENT AJAX
     */
    @RequestMapping(value = "/fireviewPensionerStatementModalAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String pensionerStatementModalAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, HttpServletResponse response) throws JsonProcessingException {
        String finalRes = creditRepo.fireviewPensionerStatementModalAjax(customeQuery);
        PensionStatementResponse resp = jacksonMapper.readValue(finalRes, PensionStatementResponse.class);

        if (resp.getCode().equalsIgnoreCase("200")) {
           return creditRepo.viewPensionerStatementJasper(resp,customeQuery, session.getAttribute("username")+"", response);
        } else {
            return null;
        }
    }

    @RequestMapping(value = "/firePensionerLoanRequestAjax", method =RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String firePensionerLoanRequestAjax(@RequestParam Map<String, String> customeQuery, Model model){
        String pensionerID = customeQuery.get("pensionerID");
        String pensionerName = customeQuery.get("pensionerName");
        model.addAttribute("pageTitle","PENSIONER LOAN REQUEST");
        model.addAttribute("pensionerID",pensionerID);
        model.addAttribute("pensionerName",pensionerName);
        model.addAttribute("branches",creditRepo.getBranches());
        return "modules/credit/modal/loanVerificationRequest";
    }

    @RequestMapping(value = "/fireverifyPensionerLoanAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CustomJsonResponse fireverifyPensionerLoanAjax(@Valid PensionVerificationForm pform, BindingResult bindingResult, @RequestParam(value = "clearanceDoc", required = false) MultipartFile clearanceDocfile, @RequestParam(value = "changeBankAccDoc", required = false) MultipartFile changeBankAccDocFile, HttpSession session,@RequestParam Map<String, String> customeQuery) throws IOException {
        CustomJsonResponse response = new CustomJsonResponse();
        String pensionerID = customeQuery.get("pensionerID");
        String pensionerName = customeQuery.get("pensionerName");
        if (bindingResult.hasErrors()) {
            //Get error message
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> {
                                return error1;
                            })
                    );
            response.setCode(96);
            response.setData(errors);
            return response;

        } else {
            String createdBy = creditRepo.getUserByUserName(session.getAttribute("username") + "");
            LOGGER.info("pension loan initiator .... {}", createdBy);
            String reference = pensionerID+'p'+ DateUtil.now("yyyMMddHHmmss");
            String branchCode = pform.getBranchCode();

//            if ((!clearanceDocfile.isEmpty()) || (!changeBankAccDocFile.isEmpty())) {
                int result2 = creditRepo.insertPensionersDocumentsAndLoanReq(branchCode,reference,clearanceDocfile,changeBankAccDocFile, createdBy,pensionerID, pensionerName,pform);
//            }

            response.setCode(result2);
            response.setData("success");
            return response;
        }
    }

    @GetMapping(value = "/firePensionerWF")
    public String firePensionerWF(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENSIONERS REQUESTS DASHBOARD TO PSSSF");
        return "modules/credit/firePensionerWF";
    }

    @PostMapping(value = "/firePensionerLoanReqWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String firePensionerLoanReqWFAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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

        return creditRepo.firePensionerLoanReqWFAjax(roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/firePreviewLoanReqForVerification")
    public String firePreviewLoanReqForVerification(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        String reference=customeQuery.get("reference");
        model.addAttribute("pageTitle", "AUTHORIZATION OF LOAN VERIFICATIONS WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("reference", reference);
        model.addAttribute("supportingDocs", creditRepo.getLoanVerDocuments(reference) );
        model.addAttribute("loanDetails", creditRepo.loanVerificationsData(reference));
        return "modules/credit/modal/loanVerificationApproval";
    }


    @PostMapping(value = "/fireReturnAmendLoanReq", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireReturnAmendLoanReq(@RequestParam Map<String, String> customeQuery, HttpSession session) {
        String reference=customeQuery.get("reference");
        String result = "{\"responseCode\":\"99\",\"message\":\"General Failure: \"}";
        String username = session.getAttribute("username") + "";

         if(1==creditRepo.returnLoanForAmendment(reference,username)){
             result = "{\"responseCode\":\"0\",\"message\":\"Returned For Amendment: \"}";
         }
        return result;
    }

    /*
     *BRANCH AUTHORIZE TIPS TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/fireAuthLoanRequestToPensioners", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAuthLoanRequestToPensioners(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
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
                        String approver = creditRepo.getUserByUserName(session.getAttribute("username") + "");
                        LOGGER.info("Approver for loan req with reference.. {} IS.... {}", reference, approver);
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";
                        LoanVerificationReq obj = creditRepo.loanVerificationsData(reference);
                        String payload = " {\n" +
                                            "\"reference_no\":\""+reference+"\",\n" +
                                            "\"pensioner_id\":"+obj.getPensionerId() +",\n"+
                                            "\"loanAmount\":\""+obj.getLoanAmount()+"\",\n" +
                                            "\"monthlyInst\":\""+obj.getMonthlyInst()+"\",\n" +
                                            "\"period\":\""+obj.getPeriod()+"\",\n" +
                                            "\"narration\":\""+obj.getNarration()+"\",\n" +
                                            "\"bankType\":\""+obj.getBankType()+"\",\n" +
                                            "\"accNumber\":\""+obj.getAccNumber()+"\",\n" +
                                            "\"submittedBy\":\""+approver+"\",\n" +
                                            "\"clearanceDoc\":\""+Base64.encodeBase64String(obj.getClearanceDoc())+"\",\n" +
                                            "\"changeBankAccDoc\":\""+ Base64.encodeBase64String(obj.getChangeBankAccDoc())+"\",\n" +
                                            "\"call_back_url\":\""+systemVariable.PENSIONERS_CALLBACK_URL+"\"\n" +
                                          "}";


                        String finalRes = creditRepo.sendPensionerVerificationReq(payload);
//                        JsonObject jsonObject = new JsonObject(finalRes);
                        ObjectMapper objectMapper = new ObjectMapper();

                        String payload2  = " {\n" +
                                "\"reference_no\":\""+reference+"\",\n" +
                                "\"pensioner_id\":"+obj.getPensionerId() +",\n"+
                                "\"loanAmount\":\""+obj.getLoanAmount()+"\",\n" +
                                "\"monthlyInst\":\""+obj.getMonthlyInst()+"\",\n" +
                                "\"period\":\""+obj.getPeriod()+"\",\n" +
                                "\"narration\":\""+obj.getNarration()+"\",\n" +
                                "\"bankType\":\""+obj.getBankType()+"\",\n" +
                                "\"accNumber\":\""+obj.getAccNumber()+"\",\n" +
                                "\"submittedBy\":\""+approver+"\",\n" +
                                "\"clearanceDoc\":\"22\",\n" +
                                "\"changeBankAccDoc\":\"33\",\n" +
                                "\"call_back_url\":\""+systemVariable.PENSIONERS_CALLBACK_URL+"\"\n" +
                                "}";
                        int  code = objectMapper.readTree(finalRes).findValue("code").asInt();
                        LOGGER.info("check payload2 ... {} and  code .... {}",payload2, code);
                        if(code==200){
                            creditRepo.updateLoanRecord(reference,approver);
                            result = "{\"result\":"+200+",\"message\":\"Received successfully. \"}";
                        }else{
                            result = "{\"result\":"+96+",\"message\":\"Error occured on authorization loan verification \"}";
                            LOGGER.info("Error with pensioners... {}",finalRes);
                        }
                        break;
                    } else {
                        //audit trails
                        String username = session.getAttribute("username") + "";
                        String branchNo = session.getAttribute("branchCode") + "";
                        String roleId = session.getAttribute("roleId") + "";

                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            //audit trails
            String username = session.getAttribute("username") + "";
            String branchNo = session.getAttribute("branchCode") + "";
            String roleId = session.getAttribute("roleId") + "";
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    /**Change of account modal**/
    @RequestMapping(value = "/firePensionerChangeAcctModal")
    public String firePensionerChangeAcctModal(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        String pensionerID=customeQuery.get("pensionerID");
        model.addAttribute("pageTitle", "CHANGING PENSIONER ACCOUNT WITH PENSIONER ID: " + pensionerID);
        model.addAttribute("pensionerID", pensionerID);
        return "modules/credit/modal/pensionerChangeAcctModal";
    }


    @RequestMapping(value = "/firePrevLoanReqSupportingDocument", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewSupportingDocument(@RequestParam Map<String, String> customeQuery) throws IOException {
        byte[] imageContent = creditRepo.getLoanVerDocuments(customeQuery.get("reference"));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<byte[]>(imageContent, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/firePrevChangeAccSupportingDocument", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> firePrevChangeAccSupportingDocument(@RequestParam Map<String, String> customeQuery) throws IOException {
        byte[] imageContent = creditRepo.firePrevChangeAccSupportingDocument(customeQuery.get("reference"));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<byte[]>(imageContent, headers, HttpStatus.OK);
    }

    /**RETURN FOR AMENDMENT***/
    @GetMapping(value = "/firePensionerAmendmentWF")
    public String firePensionerAmendmentWF(Model model, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENSIONERS REQUESTS RETURNED FOR AMENDMENT DASHBOARD");
        return "modules/credit/returnedForAmendPensionerWF";
    }

    @PostMapping(value = "/firePensionerAmendmentWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String firePensionerAmendmentWFAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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

        return creditRepo.firePensionerAmendmentWFAjax(roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);


    }

    /***AMEND REQUEST**/
    @RequestMapping(value = "/firePreviewLoanReqForAmendment")
    public String firePreviewLoanReqForAmendment(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        String reference=customeQuery.get("reference");
        model.addAttribute("pageTitle", "AMENDMENT OF LOAN REQUEST WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("reference", reference);
        model.addAttribute("supportingDocs", creditRepo.getLoanVerDocuments(reference) );
        model.addAttribute("branches",creditRepo.getBranches());
        model.addAttribute("loanDetails", creditRepo.loanVerificationsData(reference));
        return "modules/credit/modal/loanAmendmentModal";
    }


    @RequestMapping(value = "/fireReSubmitLoanReqAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CustomJsonResponse fireReSubmitLoanReqAjax(@Valid PensionVerificationForm pform, BindingResult bindingResult, @RequestParam(value = "clearanceDoc", required = false) MultipartFile clearanceDocfile, @RequestParam(value = "changeBankAccDoc", required = false) MultipartFile changeBankAccDocFile, HttpSession session,@RequestParam Map<String, String> customeQuery) {
        CustomJsonResponse response = new CustomJsonResponse();
        String pensionerID = customeQuery.get("pensionerID");
        String pensionerName = customeQuery.get("pensionerName");
        String reference = customeQuery.get("reference");
        if (bindingResult.hasErrors()) {
            //Get error message
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> {
                                return error1;
                            })
                    );
            response.setCode(96);
            response.setData(errors);
            return response;

        } else {
            String submittedBy = session.getAttribute("username") + "";

            String branchCode = pform.getBranchCode();
            LOGGER.info("Going to amend loan request with reference... {} , pensionerID... {} and pensionerName... {}", reference, pensionerID, pensionerName);
//            if ((!clearanceDocfile.isEmpty()) || (!changeBankAccDocFile.isEmpty())) {
                LOGGER.info("which file has changed ... clearanceDoc is empty?... {} or changeAccDoc. is empty?.. {}", clearanceDocfile.isEmpty(), changeBankAccDocFile.isEmpty());
                if(1==creditRepo.updatePensionersDocumentsAndLoanReq(branchCode,reference,clearanceDocfile,changeBankAccDocFile, submittedBy,pensionerID, pensionerName,pform)){
                    response.setCode(0);
                    response.setData("Loan Details Amended Successfully");
                    return  response;
                }else{
                    response.setCode(99);
                    response.setData("Failed to amend loan details");
                    return response;
                }
//            }
        }
    }


    @RequestMapping("/firePensionerReports")
    public String fireMobileMovementReport(HttpSession httpSession, Model model){
        model.addAttribute("pageTitle","PENSIONERS LOAN REPORTS");
        model.addAttribute("fromDate", DateUtil.previosDay(5));
        model.addAttribute("toDate", DateUtil.tomorrow());
        return "modules/credit/pensionerReports";
    }

    @RequestMapping(value = "/firePensionerReportsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse firePensionerReportsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        GeneralJsonResponse jsonResponse = new GeneralJsonResponse();
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        String txnStatus = customeQuery.get("txnStatus");
        List<Map<String,Object>> response = creditRepo.firePensionerReportsAjax(txnStatus,fromDate,toDate);
        jsonResponse.setStatus("SUCCESS");
        jsonResponse.setResult(response);

        return jsonResponse;
    }

    @RequestMapping(value = "/fireQueryPensionerAcctDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String queryPensionerAcctDetails(@RequestParam Map<String, String> customeQuery) {
        String accountNo = customeQuery.get("accountNo") + "";
        String pensionerName = customeQuery.get("pensionerName");
        String acctDeatils = creditRepo.getAccountDetails(customeQuery.get("accountNo"));

        System.out.println("ACCOUNT DETAILS: " + acctDeatils);
//        LOGGER.info("checking similarity... {}... for pensioner name: ... {} against rubikon name... {}..", string_lock_matcher(pensionerName,pensionerName),pensionerName,pensionerName);
        return acctDeatils;
    }




    public static int string_lock_matcher(String s, String t) {

        int totalw = word_count(s);
        int total = 100;
        int perw = total / totalw;
        int gotperw = 0;

        if (!s.equals(t)) {

            for (int i = 1; i <= totalw; i++) {
                if (simple_match(split_string(s, i), t) == 1) {
                    gotperw = ((perw * (total - 10)) / total) + gotperw;
                } else if (front_full_match(split_string(s, i), t) == 1) {
                    gotperw = ((perw * (total - 20)) / total) + gotperw;
                } else if (anywhere_match(split_string(s, i), t) == 1) {
                    gotperw = ((perw * (total - 30)) / total) + gotperw;
                } else {
                    gotperw = ((perw * smart_match(split_string(s, i), t)) / total) + gotperw;
                }
            }
        } else {
            gotperw = 100;
        }
        return gotperw;
    }

    public static int anywhere_match(String s, String t) {
        int x = 0;
        if (t.contains(s)) {
            x = 1;
        }
        return x;
    }

    public static int front_full_match(String s, String t) {
        int x = 0;
        String tempt;
        int len = s.length();

        //----------Work Body----------//
        for (int i = 1; i <= word_count(t); i++) {
            tempt = split_string(t, i);
            if (tempt.length() >= s.length()) {
                tempt = tempt.substring(0, len);
                if (s.contains(tempt)) {
                    x = 1;
                    break;
                }
            }
        }
        //---------END---------------//
        if (len == 0) {
            x = 0;
        }
        return x;
    }

    public static int simple_match(String s, String t) {
        int x = 0;
        String tempt;
        int len = s.length();


        //----------Work Body----------//
        for (int i = 1; i <= word_count(t); i++) {
            tempt = split_string(t, i);
            if (tempt.length() == s.length()) {
                if (s.contains(tempt)) {
                    x = 1;
                    break;
                }
            }
        }
        //---------END---------------//
        if (len == 0) {
            x = 0;
        }
        return x;
    }

    public static int smart_match(String ts, String tt) {

        char[] s = new char[ts.length()];
        s = ts.toCharArray();
        char[] t = new char[tt.length()];
        t = tt.toCharArray();


        int slen = s.length;
        //number of 3 combinations per word//
        int combs = (slen - 3) + 1;
        //percentage per combination of 3 characters//
        int ppc = 0;
        if (slen >= 3) {
            ppc = 100 / combs;
        }
        //initialising an integer to store the total % this class genrate//
        int x = 0;
        //declaring a temporary new source char array
        char[] ns = new char[3];
        //check if source char array has more then 3 characters//
        if (slen < 3) {
        } else {
            for (int i = 0; i < combs; i++) {
                for (int j = 0; j < 3; j++) {
                    ns[j] = s[j + i];
                }
                if (cross_full_match(ns, t) == 1) {
                    x = x + 1;
                }
            }
        }
        x = ppc * x;
        return x;
    }

    /**
     *
     * @param s
     * @param t
     * @return
     */
    public static int  cross_full_match(char[] s, char[] t) {
        int z = t.length - s.length;
        int x = 0;
        if (s.length > t.length) {
            return x;
        } else {
            for (int i = 0; i <= z; i++) {
                for (int j = 0; j <= (s.length - 1); j++) {
                    if (s[j] == t[j + i]) {
                        // x=1 if any charecer matches
                        x = 1;
                    } else {
                        // if x=0 mean an character do not matches and loop break out
                        x = 0;
                        break;
                    }
                }
                if (x == 1) {
                    break;
                }
            }
        }
        return x;
    }

    public static String split_string(String s, int n) {

        int index;
        String temp;
        temp = s;
        String temp2 = null;

        int temp3 = 0;

        for (int i = 0; i < n; i++) {
            int strlen = temp.length();
            index = temp.indexOf(" ");
            if (index < 0) {
                index = strlen;
            }
            temp2 = temp.substring(temp3, index);
            temp = temp.substring(index, strlen);
            temp = temp.trim();

        }
        return temp2;
    }

    public static int word_count(String s) {
        int x = 1;
        int c;
        s = s.trim();
        if (s.isEmpty()) {
            x = 0;
        } else {
            if (s.contains(" ")) {
                for (;;) {
                    x++;
                    c = s.indexOf(" ");
                    s = s.substring(c);
                    s = s.trim();
                    if (s.contains(" ")) {
                    } else {
                        break;
                    }
                }
            }
        }
        return x;
    }



}
