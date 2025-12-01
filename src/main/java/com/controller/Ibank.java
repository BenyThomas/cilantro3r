/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.GeneralJsonResponse;
import com.DTO.IBANK.AddAccountToIBProfile;
import com.DTO.IBANK.AddIbankProfile;
import com.DTO.IBANK.AddIbankSignatories;
import com.DTO.Teller.FormJsonResponse;
import com.helper.DateUtil;
import com.models.Customer;
//import com.models.IbankProfile;
import com.repository.IbankRepo;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import com.repository.WebserviceRepo;
import com.service.HttpClientService;
import com.service.XMLParserService;
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

/**
 * @author melleji.mollel
 */
@Controller
public class Ibank {

    @Autowired
    IbankRepo ibankRepo;


    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IbankRepo.class);

    /*
    IBANK DASHBOARD
     */
    @RequestMapping(value = "/ibankDashboard", method = RequestMethod.GET)
    public String ibankDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "IBANK DASHBOARD");
        List<Map<String, Object>> result = ibankRepo.getIbankModulePermissions("/ibankDashboard", session.getAttribute("roleId").toString());
        model.addAttribute("paymentsPermissions", result);
        LOGGER.info("paymentsPermissions", result);
        return "modules/ibank/ibankDashboard";
    }

    @RequestMapping(value = "/registerNewClient", method = RequestMethod.GET)
    public String registerNewClient(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "REGISTER NEW CLIENTS ON INTERNET BANKING");
        model.addAttribute("branches", ibankRepo.branches());
        return "modules/ibank/registerNewClient";
    }

    @RequestMapping(value = "/queryCustomerDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCustomerDeatils(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        System.out.println("ACCOUNT NO:" + customeQuery.get("accountNo"));
        String customerDetails = ibankRepo.getCustomerDetails(customeQuery.get("accountNo") + "");
        LOGGER.info("customer details " + customerDetails);
        return customerDetails;
    }

    @RequestMapping(value = "/getIBProfilesInitiated", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getIBProfilesInitiated(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return ibankRepo.getIBClientProfiles((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
     *Preview EFT BULK PAYMENT PER CUSTOMER WITH ALL DETAILS
     */
    @RequestMapping(value = "/newAccountForIBProfile")
    public String newAccountForIBProfile(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "ADD NEW ACCOUNT TO: " + customeQuery.get("clientName").toUpperCase() + " WITH TRACKING REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("profile_reference", customeQuery.get("reference"));
        model.addAttribute("client_name", customeQuery.get("clientName"));
        return "modules/ibank/modals/newAccount";
    }

    /*
     *Query Account Details
     */
    @RequestMapping(value = "/queryClientAccountDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String queryAccountDetails(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        System.out.println("ACCOUNT NO:" + customeQuery.get("accountNo"));
        return ibankRepo.getAccountDetails(customeQuery.get("accountNo") + "");
    }

    @RequestMapping(value = "/queryAccDetailsByRim", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Customer queryAccDetailsByRim(@RequestParam Map<String, String> customerQuery) {
        System.out.println("RIM NO:" + customerQuery.get("rimNo"));
        new Customer();
        Customer customer;
        customer = ibankRepo.getAccDetailsByRim(customerQuery.get("rimNo") + "");
        customer.setUsername((customer.getFirstName() + "." + customer.getLastName()).toLowerCase());
        LOGGER.info("SIGNATORY DETAILS: ", customer);
        return customer;
//        if (!Objects.equals(customer.getResponseCode(), "0")) {
//            System.out.println("4000000000   "+customer);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customer);
//        } else
//            System.out.println("20000000   "+customer);
//        return ResponseEntity.status(HttpStatus.OK).body(customer);
    }

    @RequestMapping(value = "/createIBclientProfile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse createIBClientProfile(@Valid AddIbankProfile profileForm, @RequestParam("ibform") MultipartFile[] files, @RequestParam Map<String, String> customeQuery,
                                                  HttpServletRequest request, HttpSession session, BindingResult result) throws Exception {
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
            String reference = "IBP" + session.getAttribute("branchCode") + DateUtil.now("yyyMMddmmss");
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ibankRepo.saveSupportingDocuments(reference, file, session.getAttribute("username") + "");
                }
            }
            System.out.println("profile  " + profileForm);
            int res = ibankRepo.saveIBClientProfile(profileForm, reference, session.getAttribute("username") + "");
            if (res != -1) {
                response.setValidated(true);
                response.setJsonString("Client profile is successfully created. Please proceed on adding accounts,signatories and mandates");
            } else {
                response.setValidated(false);
                response.setJsonString("An error occurred during creation of client profile");
            }
        }
        return response;
    }

    //seen
    @RequestMapping(value = "/createIBclientProfileAccount", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse createIBclientProfileAccount(@Valid AddAccountToIBProfile profileAccountForm, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, BindingResult result) throws Exception {
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
            String reference = customeQuery.get("profile_reference");
            int res = ibankRepo.saveIBClientProfileAccounts(profileAccountForm, reference, session.getAttribute("username") + "");
            if (res != -1) {
                response.setValidated(true);
                response.setJsonString("Account Added Successfuly to " + customeQuery.get("client_name"));
            } else {
                response.setValidated(false);
                response.setJsonString("An error occured during creation of client profile");
            }
        }
        return response;
    }

    /*
     *Queryg Accounts Attached To IBANK Profile
     */
    @RequestMapping(value = "/getAccountsAttachedToProfile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAccountsAttachedToProfile(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        System.out.println("PROFILE REFERENCE: " + customeQuery.get("profile_reference"));
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return ibankRepo.getAccountsAttchedToIBProfile(customeQuery.get("profile_reference") + "", draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/checkAccounts",produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String checkAccounts(@RequestParam Map<String, String> customerQuery) {
        int res = 0;
        String reference = customerQuery.get("reference");
        if (ibankRepo.getIBProfileAccounts(reference).size() > 0) {
            return "{\"result\":" + res + ",\"message\": \"proceed\"}";
        } else {
            res = 99;
            return "{\"result\":" + res + ",\"message\": \"Please add account to this profile\"}";
        }
    }

    /*
     *add signatories to account profile
     */
    @RequestMapping(value = "/addSignatoriesToProfile")
    public String addSignatoriesToProfile(@RequestParam Map<String, String> customeQuery, Model model) {
        String reference = customeQuery.get("reference");
        model.addAttribute("pageTitle", "ADD SIGNATORIES TO : " + customeQuery.get("clientName").toUpperCase() + " WITH TRACKING REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("profile_reference", reference);
        model.addAttribute("client_name", customeQuery.get("clientName"));
        model.addAttribute("categoryName", customeQuery.get("categoryName"));
        model.addAttribute("roles", ibankRepo.getIBRoles(customeQuery.get("categoryName")));
        model.addAttribute("accounts", ibankRepo.getIBProfileAccounts(reference));
        return "modules/ibank/modals/newSignatories";

    }

    //seen
    @RequestMapping(value = "/createSignatoryToIBProfile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse createSignatoryToIBProfile(@Valid AddIbankSignatories signatoryForm, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, BindingResult result,
                                                       @RequestParam(value = "role", required = false) List<String> role,
                                                       @RequestParam(value = "viewAccess", required = false) List<String> viewAccess,
                                                       @RequestParam(value = "transferAccess", required = false) List<String> transferAccess,
                                                       @RequestParam(value = "accountLimit", required = false) List<String> accountLimit) throws Exception {

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
            String reference = customeQuery.get("profile_reference");
            int res = ibankRepo.saveIbankSignatory(signatoryForm, reference, session.getAttribute("username") + "", role, transferAccess, viewAccess, accountLimit);
            if (res != -1) {
                response.setValidated(true);
                response.setJsonString("Account Added Successfuly to " + customeQuery.get("client_name"));
            } else {
                response.setValidated(false);
                response.setJsonString("An error occured during creation of client profile");
            }
        }
        return response;
    }

    /*
     *add mandate to internet banking profile
     */
    @RequestMapping(value = "/mandate")
    public String mandate(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "ADD SIGNATORIES TO : " + customeQuery.get("clientName").toUpperCase() + " WITH TRACKING REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("profile_reference", customeQuery.get("reference"));
        model.addAttribute("client_name", customeQuery.get("clientName"));
        model.addAttribute("categoryName", customeQuery.get("categoryName"));
        model.addAttribute("roles", ibankRepo.getIBRoles(customeQuery.get("categoryName")));
        model.addAttribute("accounts", ibankRepo.getIBProfileAccounts(customeQuery.get("reference")));
        return "modules/ibank/modals/mandate";
    }

    @RequestMapping(value = "/getSignatoriesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSignatoriesAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        System.out.println("PROFILE REFERENCE: " + customeQuery.get("profile_reference"));
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return ibankRepo.getSignatoryAjax(customeQuery.get("profile_reference") + "", draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getSignatoryAccountAccess", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSignatoryAccountAccess(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        System.out.println("PROFILE REFERENCE: " + customeQuery.get("profile_reference"));
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        String signatoryId = customeQuery.get("signatoryId") + "";
        return ibankRepo.getSignatoryAccountAccessAjax(customeQuery.get("profile_reference") + "", signatoryId, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/getSignatoryRoleAccess", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSignatoryRoleAccess(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        System.out.println("PROFILE REFERENCE: " + customeQuery.get("profile_reference"));
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        String signatoryId = customeQuery.get("signatoryId") + "";
        return ibankRepo.getSignatoryRoleAccessAjax(customeQuery.get("profile_reference") + "", signatoryId, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/initiatorSubmitProfileForApproval", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse initiatorSubmitProfileForApproval(@RequestParam Map<String, String> customerQuery, HttpServletRequest request, HttpSession session) {
        FormJsonResponse response = new FormJsonResponse();
        response.setValidated(false);
        response.setJsonString("An error occurred during processing");
        try {

            List<Map<String, Object>> accounts = ibankRepo.getIbankProfileAccountsList(customerQuery.get("reference"));
            List<Map<String, Object>> signatories = ibankRepo.getIbankProfileSignatoriesList(customerQuery.get("reference"));
            if (!accounts.isEmpty()) {
                if (!signatories.isEmpty()) {
                    int res = ibankRepo.initiatorSubmitProfileForApproval(customerQuery.get("reference") + "");
                    if (res == 1) {
                        response.setValidated(true);
                        response.setJsonString("Successfully submitted for approval");
                    }
                } else {
                    response.setValidated(false);
                    response.setJsonString("Please add at LEAST ONE signatory to this profile");
                }

            } else {
                response.setValidated(false);
                response.setJsonString("Please add ACCOUNT to this profile");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        return response;

    }

    /*
     *Preview IBANK PROFILES
     */
    @RequestMapping(value = "/pedingAtBranchIBProfiles")
    public String pedingAtBranchIBProfiles(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "INTERNET BANKING REGISTRATION REQUESTS THAT NEEDS BRANCH APPROVAL");
        return "modules/ibank/ibankProfilesPendingAtBranch";
    }

    @RequestMapping(value = "/getPendingAtBranchIBProfilesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPendingAtBranchIBProfilesAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return ibankRepo.getIbankProfilesPendingAtBranch((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    PREVIEW IBANK PROFILE DOCUMENT
     */
    @RequestMapping(value = "/previewIbProfileDocument", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewSupportingDocument(@RequestParam Map<String, String> customeQuery) throws IOException {
        byte[] imageContent = ibankRepo.getIbankProfileSupportingDocument(customeQuery.get("reference"));//get image from DAO based on id
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<byte[]>(imageContent, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/viewPendingIBProfileBranch")
    public String viewPendingIBProfileBranch(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "APPROVE PROFILE FOR FURTHER PROCESSING AT HQ LEVEL");
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("client_name", customeQuery.get("clientName"));
        model.addAttribute("profile", ibankRepo.getIbankProfile(customeQuery.get("reference")));
        model.addAttribute("accounts", ibankRepo.getIbankProfileAccountsList(customeQuery.get("reference")));
        model.addAttribute("signatories", ibankRepo.getIbankProfileSignatoriesList(customeQuery.get("reference")));
        model.addAttribute("accountAccess", ibankRepo.getIbankProfileSignatoriesAccountPerminssions(customeQuery.get("reference")));
        model.addAttribute("rroles", ibankRepo.getIbankProfileSignatoriesRolePerminssions(customeQuery.get("reference")));
//        System.out.println("XML REGISTRATION:\n" + ibankRepo.generateIBankRegistrationXML(customeQuery.get("reference")));
        return "modules/ibank/modals/branchApproveIbankProfile";
    }

    @RequestMapping(value = "/viewPendingIBProfileInitiator")
    public String viewPendingIBProfileInitiator(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "APPROVE PROFILE FOR FURTHER PROCESSING AT HQ LEVEL");
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("client_name", customeQuery.get("clientName"));
        model.addAttribute("profile", ibankRepo.getIbankProfile(customeQuery.get("reference")));
        model.addAttribute("accounts", ibankRepo.getIbankProfileAccountsList(customeQuery.get("reference")));
        model.addAttribute("signatories", ibankRepo.getIbankProfileSignatoriesList(customeQuery.get("reference")));
        model.addAttribute("accountAccess", ibankRepo.getIbankProfileSignatoriesAccountPerminssions(customeQuery.get("reference")));
        model.addAttribute("rroles", ibankRepo.getIbankProfileSignatoriesRolePerminssions(customeQuery.get("reference")));
        return "modules/ibank/modals/initiatorApproveIbankProfile";
    }

    @RequestMapping(value = "/approveIBProfileAtBranchLevel", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveIBProfileAtBranchLevel(@RequestParam Map<String, String> customerQuery, HttpSession session) {
        Timestamp approvedDate = new Timestamp(System.currentTimeMillis());
        int res = ibankRepo.branchApprovalToHq(customerQuery.get("reference"), session.getAttribute("username") + "", approvedDate);
        if (res != -1) {
            return "{\"result\":" + res + ",\"message\": \"Client profile is successfully submitted to HQ\"}";
        } else {
            return "{\"result\":" + res + ",\"message\": \"An error occurred during submission\"}";
        }

    }


    @RequestMapping(value = "/pedingAtHqIBProfiles")
    public String pedingAtHqIBProfiles(Model model) {
        model.addAttribute("pageTitle", "INTERNET BANKING REGISTRATION REQUESTS THAT NEEDS HQ APPROVAL");
        return "modules/ibank/ibankProfilesPendingAtHq";
    }

    @RequestMapping(value = "/getPendingAtHqIBProfilesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getPendingAtHqIBProfilesAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return ibankRepo.getIbankProfilesPendingAtHq((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/viewPendingIBProfileHq")
    public String viewPendingIBProfileHq(@RequestParam Map<String, String> customeQuery, Model model) {
        model.addAttribute("pageTitle", "APPROVE PROFILE FOR IB SERVICE ACCESS");
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("client_name", customeQuery.get("clientName"));
        model.addAttribute("profile", ibankRepo.getIbankProfile(customeQuery.get("reference")));
        model.addAttribute("accounts", ibankRepo.getIbankProfileAccountsList(customeQuery.get("reference")));
        model.addAttribute("signatories", ibankRepo.getIbankProfileSignatoriesList(customeQuery.get("reference")));
        model.addAttribute("accountAccess", ibankRepo.getIbankProfileSignatoriesAccountPerminssions(customeQuery.get("reference")));
        model.addAttribute("rroles", ibankRepo.getIbankProfileSignatoriesRolePerminssions(customeQuery.get("reference")));
        return "modules/ibank/modals/approveIbankHq";
    }

    @RequestMapping(value = "/registerIbProfile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveIBProfileAtHqLevel(@RequestParam Map<String, String> customerQuery, HttpSession session) {
        String reference = customerQuery.get("reference");
        String res = ibankRepo.registerIBProfile(reference);
        String response = "{\"result\":99,\"message\":\"General error\"}";

        //check null
        if (res != null) {
            String responseCode = XMLParserService.getDomTagText("responseCode", res);
            String responseMessage = XMLParserService.getDomTagText("message", res);
            Timestamp approvedDate = new Timestamp(System.currentTimeMillis());
            String username = session.getAttribute("username") + "";
            if (responseCode.equalsIgnoreCase("0")) {
                ibankRepo.finalizeIBRegistration(reference);
                ibankRepo.setHqApprover(reference, username, approvedDate);
                LOGGER.info("CUSTOMER IS SUCCESSFULLY CREATED ON IBANK PLATFORM: PROFILE_REFERENCE:{},", reference);
            }else if(responseCode.equalsIgnoreCase("2")){
                ibankRepo.finalizeIBRegistration(reference);
            } else {
                LOGGER.info("FAILED TO CREATE CUSTOMER: PROFILE_REFERENCE:{} response ... {},", reference,  res);
            }
//        LOGGER.info("responseCode {}, responseMessage {}",responseCode,responseMessage);
            response = "{\"result\":" + responseCode + ",\"message\":\"" + responseMessage + "\"}";
            LOGGER.info("response {}", response);
        }
        return response;
    }

    @RequestMapping(value = "/removeAccountFromIbProfile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeAccountFromIbProfile(@RequestParam Map<String, String> customerQuery) {
        int res = ibankRepo.removeAccFromProfile(customerQuery.get("reference"), customerQuery.get("accountNumber"));
        if (res != -1) {
            return "{\"result\":" + res + ",\"message\": \"Client profile is successfully REMOVED\",\"validated\":" + true + "}";
        } else {
            return "{\"result\":" + res + ",\"message\": \"An error occurred \",\"validated\":" + false + "}";
        }

    }


    @RequestMapping(value = "/removeSignatoryFromIbProfile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeSignatoryFromIbProfile(@RequestParam Map<String, String> customerQuery) {
        int res = ibankRepo.removeSignatoryFromProfile(customerQuery.get("reference"), customerQuery.get("rimNumber"));
        if (res != -1) {
            return "{\"result\":" + res + ",\"message\": \"Signatory successfully REMOVED\",\"validated\":" + true + "}";
        } else {
            return "{\"result\":" + res + ",\"message\": \"An error occurred \",\"validated\":" + false + "}";
        }

    }

    @RequestMapping(value = "/ibReports", method = RequestMethod.GET)
    public String reports(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "IBANK PROFILE REGISTRATIONS STATUS");
        model.addAttribute("fromDate", DateUtil.previosDay(5));
        model.addAttribute("toDate", DateUtil.tomorrow());
        return "modules/ibank/ibankRegistrationProfilesReport";
    }

    @RequestMapping(value = "/ibRegistrationReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse ibRegistrationReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        GeneralJsonResponse jsonResponse = new GeneralJsonResponse();
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        List<Map<String, Object>> response = ibankRepo.ibRegistrationReportAjax((String) session.getAttribute("branchCode"), fromDate, toDate);
        LOGGER.info("The final response for ib registration is: {}", response);
        jsonResponse.setStatus("SUCCESS");
        jsonResponse.setResult(response);
        return jsonResponse;
    }


    @GetMapping(value = "/fireUsersManagement")
    public String securityConsole(Model model) {
        model.addAttribute("pageTitle", "INTERNET BANKING USERS");
        return "modules/ibank/users";
    }


    @PostMapping(value = "/fireIBUsersManagementAjax",produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse getIbUsers(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,HttpSession session) {
        GeneralJsonResponse gr = new GeneralJsonResponse();
        gr.setStatus("0");
        gr.setResult(ibankRepo.getIbankUser(customeQuery.get("custName")));
        return gr;
    }

    @PostMapping("firechangeUserPasswordModalAjax")
    public String firechangeUserPasswordModalAjax(@RequestParam Map<String, String> customQuery, Model model){
        model.addAttribute("custId",customQuery.get("custId"));
        model.addAttribute("userName",customQuery.get("userName"));
        model.addAttribute("fullName",customQuery.get("fullName"));
        model.addAttribute("partnerCode",customQuery.get("partnerCode"));
        return "modules/ibank/modals/changePassword";
    }

    @PostMapping(value = "fireResetPasswordIBUserAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireResetPasswordIBUserAjax(@RequestParam Map<String, String> customQuery){
         return ibankRepo.customerResetPassword(customQuery);
    }

    @PostMapping("fireModifyUserIBDetailsModalAjax")
    public String fireModifyUserIBDetailsModalAjax(@RequestParam Map<String, String> customQuery, Model model){
        model.addAttribute("data",ibankRepo.getIbUserDetails(customQuery));
        return "modules/ibank/modals/ibUserDetails";
    }

    @PostMapping(value = "fireUpdateIBUsersDetailsAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireUpdateIBUsersDetailsAjax(@RequestParam Map<String, String> customQuery){
        return ibankRepo.updateIBUserDetails(customQuery);
    }

}
