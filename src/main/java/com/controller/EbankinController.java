/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.DTO.Ebanking.AmendVisaCardForm;
import com.DTO.Ebanking.CardRegistrationReq;
import com.DTO.GeneralJsonResponse;
import com.DTO.Teller.CustomJsonResponse;
import com.DTO.Teller.FormJsonResponse;
import com.DTO.ubx.FailedCardDTO;
import com.DTO.ubx.UbxResponse;
import com.DTO.visaCardTracingObject;
import com.controller.visaCardReport.VisaCardJsonResponse;
import com.dao.kyc.response.ors.ResponseDTO;
import com.helper.DateUtil;
import com.repository.EbankingRepo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import com.repository.VisaCardBINConfigurationRepository;
import com.repository.WebserviceRepo;
import com.service.BCXService;
import com.service.XMLParserService;
import com.service.XapiWebService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.DTO.Ebanking.CreateCardRequest;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * @author melleji.mollel
 */
@Controller
public class EbankinController {

    @Autowired
    EbankingRepo ebankingRepo;

    @Autowired
    WebserviceRepo webserviceRepo;

    @Autowired
    VisaCardBINConfigurationRepository visaCardBINConfigurationRepository;

    @Autowired
    BCXService bcxService;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EbankinController.class);

    /*
    EFT PAYMENT DASHBOARD
     */
    @RequestMapping(value = "/ebankingDashboard", method = RequestMethod.GET)
    public String eftPaymentsDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "ELECTRONIC BANKING SERVICES");
        model.addAttribute("paymentsPermissions", ebankingRepo.getEFTModulePermissions("/ebankingDashboard", session.getAttribute("roleId").toString()));
        return "modules/ebanking/ebankingDashboard";
    }

    @RequestMapping(value = "/createCardForm", method = RequestMethod.GET)
    public String rtgsTransfer(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENDING CARDS REQUESTS ON WORKFLOW");
        model.addAttribute("loggedInUser", session.getAttribute("username").toString());
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("visaBins",  visaCardBINConfigurationRepository.findByStatus("A"));

        return "modules/ebanking/createNewCardRequest";
    }

    @RequestMapping(value = "/createCardRequest", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CustomJsonResponse createIBClientProfile(@Valid CreateCardRequest profileForm, BindingResult bindingResult, @RequestParam("cardForm") MultipartFile[] files, HttpSession session) {
        CustomJsonResponse response = new CustomJsonResponse();
        LOGGER.info("createIBClientProfile: {}",profileForm);
            if (bindingResult.hasErrors()) {
                //Get error message
                Map<String, String> errors = bindingResult.getFieldErrors().stream()
                        .collect(
                                Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> error1)
                        );
                response.setCode(96);
                response.setData(errors);
            } else {
                if (profileForm.getCustomerName() != null && profileForm.getCustomerName().length() > 21) {
                    response.setCode(51);
                    response.setData(profileForm.getCustomerName() + " length is ["+profileForm.getCustomerName()+"] greater  21 characters");
                }else {

                    String reference = session.getAttribute("branchCode") + "CARD" + DateUtil.now("yyyMMddmmss");
                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            ebankingRepo.saveSupportingDocuments(reference, file, session.getAttribute("username") + "");
                        }
                    }
                    //remove  + on numbers
                    profileForm.setPhoneNumber((profileForm.getCountryCode() + profileForm.getPhoneNumber()).trim().replace("+", ""));
                    profileForm.setReference(reference);
                    profileForm.setCreatedBy(session.getAttribute("username") + "");
                    profileForm.setOriginatingBranch(session.getAttribute("branchCode") + "");

                    List<Map<String, Object>> cards = ebankingRepo.getCardExists(profileForm.getAccountNo());

                    if (cards.isEmpty()) {
                        int res = ebankingRepo.saveCardRequest(profileForm);
                        if (res != -1) {
                            response.setCode(200);
                            response.setData("Card reqquest Submited successfully");
                        } else {
                            response.setCode(404);
                            response.setData("An error occured during Card request Submited");
                        }
                    } else {
                        response.setCode(26);
                        response.setData("Another card with same account number exists in database, Please contact EBANKING OR CARD-CENTER team for support, code 1292 ");
                    }
                }
            }
        return response;
    }

    @RequestMapping(value = "/getCardsRequestsPendingAtBranch", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getIBProfilesInitiated(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return ebankingRepo.getInitiatedCardRequests((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/previewCardRequest")
    public String previewCardRequest(@RequestParam Map<String, String> customeQuery, Model model
    ) {
        model.addAttribute("pageTitle", "APPROVE CARD PENDING REQUEST WITH TRACKING REFERENCE:" + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", ebankingRepo.getSupportingDocument(customeQuery.get("reference")));
        model.addAttribute("card", ebankingRepo.getCardRequests(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        return "modules/ebanking/modals/previewCardRequest";
    }

    @RequestMapping(value = "/previewCardRequestSupportingDocument", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewSupportingDocument(@RequestParam Map<String, String> customeQuery) throws IOException {
        byte[] imageContent = ebankingRepo.getSupportingDocument(customeQuery.get("reference"), customeQuery.get("id"));//get image from DAO based on id
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(imageContent, headers, HttpStatus.OK);
    }

    /*
    Branch approve Card Request
     */
    @RequestMapping(value = "/branchApproveCardCreationRequest", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String branchApproveCardCreationRequest(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = ebankingRepo.branchApproveCardCreationRequest(customeQuery.get("reference"), (String) httpsession.getAttribute("username") + "");
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    Branch delete Card Request
    */
    @RequestMapping(value = "/fireReturnVisaCardForAmmendment", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireReturnVisaCardForAmmendment(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    ebankingRepo.deleteVisaSupportingDoc(customeQuery.get("reference"));
                    result = ebankingRepo.fireReturnVisaCardForAmmendment(customeQuery.get("reference"));
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }
        }
        return result;
    }

    /*
    CARDS REQUESTS ON HQ WORKFLOW VIEW
     */
    @RequestMapping(value = "/cardsOnHQWorkFlow", method = RequestMethod.GET)
    public String cardsOnHQWorkFlow(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "PENDING CARDS REQUESTS ON WORKFLOW READY FOR PAN GENERATION");
        return "modules/ebanking/cardRequestsOnHQWorkFlow";
    }

    /*
     *GET CARDS REQUEST PENDING AT HQ LEVEL
     */
    @RequestMapping(value = "/getCardsOnHQWorkFlowAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCardsOnHQWorkFlowAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return ebankingRepo.getCardsRequestReadyForPANGeneration((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    /*
    HQ preview card request and approve
     */
    @RequestMapping(value = "/HqApproveCardRequestView")
    public String HqApproveCardRequestView(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        model.addAttribute("pageTitle", "APPROVE CARD PENDING REQUEST WITH TRACKING REFERENCE:" + customeQuery.get("reference") + " AND GENERATE PAN");
        model.addAttribute("supportingDocs", ebankingRepo.getSupportingDocument(customeQuery.get("reference")));
        List<Map<String, Object>> card = ebankingRepo.getCardRequests(customeQuery.get("reference"));
        model.addAttribute("card", card);
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("accountNo", customeQuery.get("accountNo"));
        model.addAttribute("accountName", customeQuery.get("accountName"));
        model.addAttribute("rimNo", customeQuery.get("rimNo"));
        model.addAttribute("accountName", customeQuery.get("accountName"));
        model.addAttribute("originatingBranch", card.get(0).get("originating_branch"));
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        return "modules/ebanking/modals/hqApproveAndGeneratePAN";
    }

    /*
    Branch approve Card Request hqApproveAndGeneratePAN
     */
    @RequestMapping(value = "/HqApproveCardPANRequestGeneration", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String HqApproveCardPANRequestGeneration(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    //check if it is allowed to submitt for generating card
                    boolean is_allowed = ebankingRepo.checkCardIsAllowed(customeQuery.get("reference"), customeQuery.get("accountNo"));
                    if (is_allowed) {
                        result = ebankingRepo.HqApproveCardCreationRequest(customeQuery.get("rimNo"), customeQuery.get("reference"), customeQuery.get("accountNo"), customeQuery.get("accountName"), (String) httpsession.getAttribute("username") + "", customeQuery.get("branchCode"));
                        break;
                    } else {
                        result = "{\"result\":\"1292\",\"message\":\"You already submitted to ubx, please confirm\"}";
                    }
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }


    /*
    Branch approve Card Request hqApproveAndGeneratePAN
     */
    @RequestMapping(value = "/HqCancelCardPANRequestGeneration", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String HqCancelCardPANRequestGeneration(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = ebankingRepo.HqCancelCardCreationRequest(customeQuery.get("rimNo"), customeQuery.get("reference"), customeQuery.get("accountNo"), customeQuery.get("accountName"), (String) httpsession.getAttribute("username") + "");
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    CARD ISSUANCE BRANCH 
     */
    @RequestMapping(value = "/cardIssuance", method = RequestMethod.GET)
    public String cardIssuanceBranchLevel(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "CARD ISSUANCE TO CUSTOMER");
        return "modules/ebanking/cardIssuance";
    }

    @RequestMapping(value = "/getCardIssuanceAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCardIssuanceAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String cardType = customeQuery.get("mno");
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        String branchCode = (String) session.getAttribute("branchCode");
        return ebankingRepo.getCardIssuanceAjax(cardType, branchCode, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);

    }

    /*
    CARD ISSUANCE BRANCH 
     */
    @RequestMapping(value = "/hqCardManagement", method = RequestMethod.GET)
    public String hqCardManagement(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "CARD MANAGEMENT CONSOLE [RECEIVE FROM DZ CARD AND DISPATCH TO BRANCHES]");
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("hqCheck", session.getAttribute("branchCode"));
        model.addAttribute("startDate",DateUtil.previosDay(2));
        model.addAttribute("endDate",DateUtil.tomorrow());
        return "modules/ebanking/hqCardManagement";
    }

    @RequestMapping(value = "/getHqCardManagementAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse getCardManagementHQAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String branchCOde = customeQuery.get("branchCode");
        String statusCode = customeQuery.get("statusCode");
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        GeneralJsonResponse gs = new GeneralJsonResponse();
        List<Map<String,Object>> finalRes = ebankingRepo.getCardManagementHQAjax(branchCOde, statusCode,fromDate,toDate);
        gs.setStatus("SUCCESS");
        gs.setResult(finalRes);
        return gs;
    }

    /*
     *RECEIVE CARDS FROM PRINTING UNIT
     */
    @RequestMapping(value = "/receiveCardsFromPrintingUnitModal")
    public String receiveCardsFromPrintingUnit(HttpServletRequest request, HttpSession session, Model model, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("PAN", "");
        String pans = customeQuery.get("PANList");
        model.addAttribute("pageTitle", "ACKNOWLEDGE RECEIVING CARDS FROM PRINTING UNIT AS LISTED BELOW");
        model.addAttribute("card", ebankingRepo.getCardsBasedOnPANLists(pans));
        model.addAttribute("PANList", pans);
        return "modules/ebanking/modals/receiveCardsFromPrintingUnit";
    }

    /*
    Receive cards from printing unit
     */
    @RequestMapping(value = "/submitReceiveCardsFromPrintingUnitForm", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String SubmitReceiveCardsFromPrintingUnitForm(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = ebankingRepo.SubmitReceiveCardsFromPrintingUnitForm(customeQuery.get("PANLists"), httpsession.getAttribute("username") + "");
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }
        }
        return result;
    }

    /*
     *DISPATCH CARDS TO BRANCHES
     */
    @RequestMapping(value = "/dipatchCardsTobranchesModal")
    public String dispatchCardsToBranchesModal(HttpServletRequest request, HttpSession session, Model model, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("PAN", "");
        String pans = customeQuery.get("PANList");
        model.addAttribute("pageTitle", "DISPATCH CARDS TO BRANCH");
        model.addAttribute("card", ebankingRepo.getCardsBasedOnPANLists(pans));
        model.addAttribute("PANList", pans);
        return "modules/ebanking/modals/dipatchCardsTobranches";
    }

    /*
     *APPROVE CARD DISPATCH TO BRANCH
     */
    @RequestMapping(value = "/submitDipatchCardsTobranchesForm", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String submitDispatchCardsToBranchesForm(HttpSession httpsession, @RequestParam Map<String, String> customeQuery) {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = ebankingRepo.submitDipatchCardsTobranchesForm(customeQuery.get("PANLists"), httpsession.getAttribute("username") + "");
                    break;
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    ISSUE A CARD TO CUSTOMER MODAL
     */
    @RequestMapping(value = "/issueCardToCustomerModal")
    public String issueCardToCustomerModal(HttpServletRequest request, HttpSession session, Model model, @RequestParam Map<String, String> customeQuery) throws Exception {
        model.addAttribute("PAN", "");
        String pans = customeQuery.get("PANList");
        model.addAttribute("pageTitle", "ISSUE PAN TO CUSTOMER: " + pans.split("==")[0] + " TO ACCOUNT: " + pans.split("==")[1]);
        model.addAttribute("card", ebankingRepo.getCardsBasedOnPANLists(pans.split("==")[0]));
        model.addAttribute("PANList", pans);

        // -1 exception need to be handled here
//        String token = ebankingRepo.getInitialPINFromPostilionSwitch(pans);

//        model.addAttribute("token", token);
        model.addAttribute("terminalDetails", ebankingRepo.getPosTerminalDeails(session.getAttribute("branchCode") + ""));
        return "modules/ebanking/modals/issueCardToCustomerModal";
    }

    /*
     *SUBMIT CARD ISSUANCE TO CORE BANKING
     */
    @RequestMapping(value = "/submitCardIssunceModalForm", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String submitCardIssunceModalForm(HttpSession httpsession, @Valid CardRegistrationReq cardRegistrationReq, @RequestParam Map<String, String> customeQuery) throws Exception {
        String postingRole = (String) httpsession.getAttribute("postingRole");
        LOGGER.info("CARD ISSUANCE FORM:{}, customeQuery={}", cardRegistrationReq,customeQuery);
        String result = result = "{\"result\":\"35\",\"message\":\"Please select your operational Role.: \"}";
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            philae.ach.BnUser user = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
            for (philae.ach.UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    String token = ebankingRepo.getInitialPINFromPostilionSwitch(customeQuery.get("pans"));
                    if(token.equalsIgnoreCase("-1")) {
                        //failed to decrypt
                        visaCardTracingObject vso = new visaCardTracingObject();
                        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
                        String formattedDate = df.format(Calendar.getInstance().getTime());
                        vso.setAccountNo(customeQuery.get("accountNo"));
                        vso.setServiceType("VISA CARD ISSUING");
                        vso.setActionStatus("ACTIVE");
                        vso.setActionToTake("VALIDATE CARD PAN NUMBER, TO MATCH PHYSICAL CARD PAN");
                        vso.setCustomerName(customeQuery.get("customerName"));
                        vso.setPhone(customeQuery.get("customerPhoneNumber"));
                        vso.setReference("CI"+formattedDate);
                        vso.setCustomerRimNo(customeQuery.get("customerRimNo"));
                        vso.setErrorEncountered("GENERATING PASSWORD DECRYPTION FAILED, DURING CARD ISSUANCE");
                        vso.setSqlCheck(null);
                        webserviceRepo.trackVisaCardFailures(vso);
                        result = result = "{\"result\":\"-1\",\"message\":\"Failed to generate initial password, please try again .: \"}";
                    }else{
                        result = ebankingRepo.issueCardToCoreBanking(cardRegistrationReq,token, httpsession.getAttribute("username") + "");
                        break;
                    }
                } else {
                    result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }
        return result;
    }

    /*
    BRANCH CARD MANAGEMENT
     */
    @RequestMapping(value = "/branchCardManagement", method = RequestMethod.GET)
    public String branchCardManagement(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "CARD MANAGEMENT CONSOLE [RECEIVE FROM CARD CENTER]");
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        return "modules/ebanking/branchCardManagement";
    }

    @RequestMapping(value = "/getBranchCardManagementAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getBranchCardManagementAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String branchCOde = customeQuery.get("branchCode");
        String statusCode = customeQuery.get("statusCode");
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        System.out.println("BRANCH CODE:" + branchCOde + " STATUS CODE:" + statusCode);
        return ebankingRepo.getBranchCardManagementAjax(branchCOde, statusCode, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping("/fireChangeCardCollectingBranch")
    public String fireChangeCardCollectingBranch(@RequestParam Map<String,String> customeQuery, Model model, HttpSession session) {
        LOGGER.info("request:... {}",customeQuery.toString());
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("cardPan", customeQuery.get("pan"));
        model.addAttribute("cardReference",customeQuery.get("reference"));
        return "modules/ebanking/modals/changeCardCollectingBranchModal";
    }


    @PostMapping("/updateCardCollectingBranchAjax")
    @ResponseBody
    public String updateCardCollectingBranch(@RequestParam Map<String, String> customeQuery) {
        String cardPan = customeQuery.get("pan");
        String branchCode = customeQuery.get("branchCode");
        String cardReference = customeQuery.get("cardReference");
        String result = null;
        if(branchCode != null){
            result= ebankingRepo.updateCardCollectingBranch(cardPan, branchCode, cardReference);
        }else{
            result=   "Please select branch";
        }

        return result;
    }

    @PostMapping("/updateCustomerPhoneAjax")
    @ResponseBody
    public String updateCustomerPhone(@RequestParam Map<String, String> customeQuery) {
        String cardPan = customeQuery.get("pan");
        String customerPhone = customeQuery.get("customerPhoneVal");
        String cardReference = customeQuery.get("cardReference");
        String result = "-1";
        if(customerPhone != null) {
            result  = ebankingRepo.updateCustomerContact(customerPhone, cardPan, cardReference);
        }else{
            result = "Phone number is required";
        }
        return result;
    }

    @PostMapping("/fireRejectCardModal")
    public String fireRejectCardModal(@RequestBody String pan, Model model, HttpSession session) {
        model.addAttribute("cardPan", pan);
        return "modules/ebanking/modals/rejectCard";
    }

    @PostMapping("/rejectVisaCardAjax")
    @ResponseBody
    public String rejectCard(@RequestParam Map<String, String> customeQuery) {
        String cardPan = customeQuery.get("pan");
        String reason = customeQuery.get("cardRejectingReason");
        String cardReference = customeQuery.get("cardReference");

        String result = "-1";
                if(reason != null) {
                    result = ebankingRepo.rejectCard(cardPan, reason,cardReference);
                }else{
                    result = "Rejection reason is required";
                }
        return result;
    }

    @PostMapping("/updateCardPanAjax")
    @ResponseBody
    public String updateCardPanAjax(@RequestParam Map<String, String> customeQuery) {
        String cardPan = customeQuery.get("pan");
        String cardReference = customeQuery.get("cardReference");
        String newCardPan = customeQuery.get("changePan");
        LOGGER.info("Form details for changing customer contact are PAN and reference and PAN as follows.... {}.... and {}..... AND {}", cardPan, cardReference, newCardPan);

        String result = "-1";
        if(newCardPan != null) {
            result  = ebankingRepo.updateCardPanAjax(cardReference, cardPan,newCardPan );
        }else{
            result = "Card PAN can not be empty";
        }
        return result;
    }

    /*
        VISA CARD ISSUES
    */
    @RequestMapping(value = "/fireSolveVisaCardIssues", method = RequestMethod.GET)
    public String fireSolveVisaCardIssues(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "HEY.., LET US SOLVE VISA CARD ISSUES HERE.");
        model.addAttribute("fromDate",DateUtil.previosDay(3));
        model.addAttribute("toDate",DateUtil.tomorrow());
        return "modules/ebanking/solveVisaCardIssues";
    }

    @RequestMapping(value = "/fireSolveVisaCardIssuesAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireSolveVisaCardIssuesAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String serviceCode = customeQuery.get("serviceCode");
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        GeneralJsonResponse gs = new GeneralJsonResponse();
        List<Map<String,Object>> finalRes = ebankingRepo.fireSolveVisaCardIssuesAjax(serviceCode,fromDate,toDate);
        gs.setStatus("SUCCESS");
        gs.setResult(finalRes);
        return gs;
    }

    @PostMapping("/firePreviewVisaDetails")
    public String firePreviewVisaDetails(@RequestParam Map<String,String> customerQuery,Model model){
        String accountNo = customerQuery.get("accountNo");
        String rimNo = customerQuery.get("rimNo");
        String reference = customerQuery.get("reference");
        String serviceType = customerQuery.get("serviceType");
        model.addAttribute("requestSideDetails",ebankingRepo.fireCardDetailsRequestSide(reference,accountNo,rimNo));
        model.addAttribute("cardSideDetails",ebankingRepo.fireCardDetailsCardSide(accountNo,rimNo,serviceType));
        model.addAttribute("cardDetailsForPinReset",ebankingRepo.getcardDetailsForPinReset(accountNo));
        return "modules/ebanking/modals/previewVisaCardDetails";
    }

    @GetMapping("/fireGetCardDetails")
    public String fireGetCardDetails(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","GET VISA CARD DETAILS USING CUSTOMER ACCOUNT NUMBER");
        model.addAttribute("userRoleId",httpSession.getAttribute("roleId") + "" );
        return "/modules/ebanking/fireGetCardDetails";
    }


    @RequestMapping(value = "/fireGetCardDetailsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<Map<String,Object>> fireGetCardDetailsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {

        String accountNo = customeQuery.get("acctNo");
        List<Map<String, Object>> records = null;
        try {
           records= ebankingRepo.fireGetCardDetailsAjax(accountNo);
        } catch (Exception es) {
            LOGGER.info("Exception found...", es);
            return null;
        }
        return records;
    }


    @PostMapping("/fireChangeVisaCardDetails")
    public String fireChangeVisaCardDetails(@RequestParam Map<String,String> customeQuery, Model model, HttpSession session) {
        LOGGER.info("request: ... {}",customeQuery.toString());
        model.addAttribute("accountNo", customeQuery.get("accountNo"));
        model.addAttribute("cardReference",customeQuery.get("reference"));
        return "modules/ebanking/modals/fireChangeVisaCardDetails";
    }

    @PostMapping("/fireStatelessVisaCardUpdatingAjax")
    @ResponseBody
    public String fireStatelessVisaCardUpdatingAjax(@RequestParam Map<String, String> customeQuery) {
        String accountNo = customeQuery.get("accountNo");
        String cardReference = customeQuery.get("cardReference");
        String selectedValue = customeQuery.get("selectedValue");
        String enteredValue = customeQuery.get("enteredValue");
        String result = "Please Select Drop down and Fill Input field";
        LOGGER.info("TRACING... {}, ", customeQuery);
        if((selectedValue.equalsIgnoreCase("null")) || (enteredValue.equalsIgnoreCase(""))){
            result = result;
        }else {
            result=ebankingRepo.fireStatelessVisaCardUpdatingAjax(accountNo, selectedValue, enteredValue, cardReference);
        }
        return result;
    }


    /*
        Card returned for amendment
    */
    @RequestMapping(value = "/fireAmendVisaCard", method = RequestMethod.GET)
    public String fireAmendVisaCard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "CARDS RETURNED FOR AMENDMENT");
        return "modules/ebanking/cardReturnedForAmending";
    }


    @RequestMapping(value = "/fireAmendVisaCardAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireAmendVisaCardAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");

        return ebankingRepo.fireAmendVisaCardAjax((String) session.getAttribute("branchCode"), draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @RequestMapping(value = "/firePreviewAmendVisaCard")
    public String firePreviewAmendVisaCard(@RequestParam Map<String, String> customeQuery, Model model, HttpSession session) {
        model.addAttribute("pageTitle", "AMEND CARD REQUEST WITH TRACKING REFERENCE:" + customeQuery.get("reference") + " AND ACCOUNT NUMBER: "+customeQuery.get("customerAcctNo"));
        model.addAttribute("supportingDocs", ebankingRepo.getSupportingDocument(customeQuery.get("reference")));
        model.addAttribute("card", ebankingRepo.getCardRequests(customeQuery.get("reference")));
        model.addAttribute("branches", ebankingRepo.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("accountNumber", customeQuery.get("customerAcctNo"));
        return "modules/ebanking/modals/previewAmendmentCardRequest";
    }

    @RequestMapping(value = "/fireVisaCardChargeReport", method = RequestMethod.GET)
    public String fireVisaCardChargeReport(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "VISA CHARGE REPORT");
        model.addAttribute("startDate",DateUtil.previosDay(30));
        model.addAttribute("endDate",DateUtil.tomorrow());
        return "modules/ebanking/visaCardChargeReport";
    }

    @RequestMapping(value = "/fireGetVisaCardChargeReportAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse fireVisaCardChargeReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        GeneralJsonResponse gs = new GeneralJsonResponse();
        List<Map<String,Object>> finalRes = ebankingRepo.getVisaCardChargeReport(fromDate,toDate);
        gs.setStatus("SUCCESS");
        gs.setResult(finalRes);
        return gs;
    }

    @RequestMapping(value = "/fireUpdateVisaCardReq", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CustomJsonResponse fireUpdateVisaCardReq(Map<String,String> customeQuery, @Valid AmendVisaCardForm amendVisaCardForm, BindingResult bindingResult, @RequestParam("cardForm") MultipartFile[] files, HttpSession session) {
        CustomJsonResponse response = new CustomJsonResponse();

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
        } else {
            if (amendVisaCardForm.getCustomerName() != null && amendVisaCardForm.getCustomerName().length() > 21) {
                response.setCode(51);
                response.setData(amendVisaCardForm.getCustomerName() + " length is ["+amendVisaCardForm.getCustomerName()+"] greater  21 characters");
            } else {

                String reference = amendVisaCardForm.getReference();
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        //check if file exist
                        ebankingRepo.saveSupportingDocuments(reference, file, session.getAttribute("username") + "");
                    }
                }
                amendVisaCardForm.setReference(reference);
                amendVisaCardForm.setCreatedBy(session.getAttribute("username") + "");
                amendVisaCardForm.setOriginatingBranch(session.getAttribute("branchCode") + "");

                    int res = ebankingRepo.updateVisaCardRequest(amendVisaCardForm);
                    if (res != -1) {
                        response.setCode(200);
                        response.setData("Card request Updated successfully");
                    } else {
                        response.setCode(404);
                        response.setData("An error ocured during Card amending");
                    }

            }
        }
        return response;
    }


    @PostMapping(value = "/fireGenerateNewVisaPin", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireGenerateNewVisaPin(@RequestParam Map<String, String> customeQuery) {
        try{
            LOGGER.info("PIN reset for..... {}",customeQuery.toString());
            String payload = "<customerSelfService>\n" +
                    "     <custNumber>"+customeQuery.get("customerRim").trim()+"</custNumber>\n" +
                    "    <accountNo>"+ customeQuery.get("accountNo").trim()+"</accountNo>\n" +
                    "    <accountName>"+customeQuery.get("customerShortName")+"</accountName>\n" +
                    "    <msisdn>"+customeQuery.get("phone")+"</msisdn>\n" +
                    "    <imsi>"+customeQuery.get("pan").trim()+"</imsi>\n" +
                    "    <txType>VISA_PIN_RESET</txType>\n" +
                    "    <serviceCharge>0</serviceCharge>\n" +
                    "    <checksum>"+customeQuery.get("checksum")+"</checksum>\n" +
                    "</customerSelfService>";
            return  webserviceRepo.ibankRegistration(payload);

        }catch(Exception e){
            LOGGER.info("Exception found...", e);
            return "";
        }

    }

    @GetMapping("instantVisaCard")
    public String instantVisaCard(Model model){
        model.addAttribute("pageTitle","INSTANT VISA CARD");
        return "/modules/ebanking/instantVisaCard";
    }

    @PostMapping(value = "/fireGetFailedInstantVisaCardAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> fireGetFailedInstantVisaCardAjax(HttpServletRequest request) {
        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        // Fetch all failed cards (or slice them manually if not using pageable)
        List<FailedCardDTO> all = bcxService.getAllFailedCards().getData(); // Use your real method

        // Paginate manually (optional if you aren't doing DB-level pagination yet)
        List<FailedCardDTO> paginated = all.stream()
                .skip(start)
                .limit(length)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("draw", draw);
        result.put("recordsTotal", all.size());
        result.put("recordsFiltered", all.size());
        result.put("data", paginated);
        return result;
    }

//    @RequestMapping(value = "/fireCompleteFailInstantVisaCardAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public GeneralJsonResponse fireCompleteFailInstantVisaCardAjax(@RequestParam Map<String, String> customerQuery) {
//        String strCardId = customerQuery.get("cardId");
//        Long cardId = Long.valueOf(strCardId);
//        GeneralJsonResponse gs = new GeneralJsonResponse();
//        UbxResponse xr = bcxService.resumeCardProcess(cardId);
//        gs.setStatus("00".equalsIgnoreCase(xr.getResponseCode()) ? "SUCCESS" : "ERROR");
//        gs.setResult(xr.getResponseMessage());
//        return gs;
//    }
}
