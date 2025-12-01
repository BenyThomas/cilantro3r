package com.controller.tips;

import com.config.SYSENV;
import com.controller.itax.CMSpartners.JsonResponse;
import com.controller.tips.FraudRegistrationForm;
import com.DTO.Teller.FormJsonResponse;
import com.DTO.tips.TipsJsonResponse;
import com.DTO.tips.TipsPaymentFullObject;
import com.controller.User;
import com.core.event.AuditLogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.helper.DateUtil;
import com.repository.Recon_M;
import com.repository.RtgsRepo;
import com.repository.tips.TipsRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
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
import philae.api.BnUser;
import philae.api.UsRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TipsController {

    @Autowired
    TipsRepository tipsRepository;

    @Autowired
    RtgsRepo rtgsRepo;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    User userController;

    @Autowired
    Recon_M reconRepo;

    @Autowired
    SYSENV systemVariable;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TipsController.class);

    private final Validator factory = Validation.buildDefaultValidatorFactory().getValidator();

    @GetMapping("/tipsDashboard")
    public String tipsPaymentsDashboard(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "TIPS PAYMENTS");
        model.addAttribute("tipsPaymentsPermissions", tipsRepository.getTIPSModulePermissions("/tipsDashboard", session.getAttribute("roleId").toString()));
        return "modules/tips/tipsDashboard";
    }

    @GetMapping("/initiateTransfer")
    public String initiateTransfer(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery, HttpServletRequest httpRequest) {
        System.out.println("SESSION ROLE: " + session.getAttribute("roleId").toString());
        model.addAttribute("banks", rtgsRepo.getBanksListForTipsOnly());
        //audit trails
        model.addAttribute("pageTitle", "INITIATE NEW TIPS TRANSFER: ");
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, httpRequest.getRemoteHost(), "/initiateTipsTransfer", "SUCCESS", "Viewed rtgs dashboard"));
        return "modules/tips/initiateTransfer";
    }


    @RequestMapping(value = "/queryBeneficiaryAccountDetails", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String queryBeneficiaryAccountDetails(@RequestParam Map<String, String> customeQuery, HttpServletRequest request,
                                                 HttpSession session
    ) {
        String accountNo = customeQuery.get("accountNo");
        String institutionCode = customeQuery.get("institutionCode");
        String institutionCategory = customeQuery.get("institutionCategory");
        if (!Objects.equals(institutionCategory, "CONTROLNO")) {
            institutionCategory = tipsRepository.getInstitutionCategory(institutionCode);
        }

        String acctDetails = tipsRepository.getAccountDetails(accountNo, institutionCategory, institutionCode);

        LOGGER.info("BENEFICIARY ACCOUNT DETAILS: {}", acctDetails);
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/queryBeneficiaryAccountDetails", "SUCCESS", "Query beneficially account details:.. {} " + acctDetails));

        return acctDetails;
    }

    @RequestMapping(value = "/submitTipsTransfer", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public FormJsonResponse submitTipsTransfer(@RequestParam("supportingDoc") MultipartFile[] files, @RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session, HttpServletRequest httpRequest) throws Exception {

        TIPSTransferForm tipsTransferForm = new TIPSTransferForm();
       
        tipsTransferForm.setBatchReference(customeQuery.get("batchReference"));
        tipsTransferForm.setBeneficiaryAccount(customeQuery.get("beneficiaryAccount"));
        tipsTransferForm.setBeneficiaryBIC(customeQuery.get("beneficiaryBIC"));
        tipsTransferForm.setBeneficiaryContact(customeQuery.get("beneficiaryContact"));
        tipsTransferForm.setBeneficiaryName(customeQuery.get("beneficiaryName"));
        tipsTransferForm.setChannel(customeQuery.get("channel"));
        tipsTransferForm.setChargeDetails(customeQuery.get("chargeDetails"));
        tipsTransferForm.setComments(customeQuery.get("comments"));
        tipsTransferForm.setCorrespondentBic(customeQuery.get("correspondentBic"));
        tipsTransferForm.setCurrency(customeQuery.get("currency"));
        tipsTransferForm.setCurrencyConversion(customeQuery.get("currencyConversion"));
        tipsTransferForm.setDescription(customeQuery.get("description"));
        tipsTransferForm.setFxType(customeQuery.get("fxType"));
        tipsTransferForm.setIntermediaryBank(customeQuery.get("intermediaryBank"));
        tipsTransferForm.setMessage(customeQuery.get("message"));
        tipsTransferForm.setMessageType(customeQuery.get("messageType"));
        tipsTransferForm.setReference(customeQuery.get("reference"));
        tipsTransferForm.setRelatedReference(customeQuery.get("relatedReference"));
        tipsTransferForm.setRequestingRate(customeQuery.get("requestingRate"));
        tipsTransferForm.setResponseCode(customeQuery.get("responseCode"));
        tipsTransferForm.setRubikonRate(customeQuery.get("rubikonRate"));
        tipsTransferForm.setSenderAccount(customeQuery.get("senderAccount"));
        tipsTransferForm.setSenderAddress(customeQuery.get("senderAddress"));
        tipsTransferForm.setSenderBic(customeQuery.get("senderBic"));
        tipsTransferForm.setSenderName(customeQuery.get("senderName"));
        tipsTransferForm.setSenderPhone(customeQuery.get("senderPhone"));
        tipsTransferForm.setSwiftMessage(customeQuery.get("swiftMessage"));
        tipsTransferForm.setTransactionType(customeQuery.get("transactionType"));
        if (customeQuery.get("amount") != null) {
            tipsTransferForm.setAmount(customeQuery.get("amount").replace(",", ""));
        } else {
            tipsTransferForm.setAmount(customeQuery.get("amount"));

        }
        String violError = "";
        FormJsonResponse response = new FormJsonResponse();
        LOGGER.info("TIPS Transfer form: ....{} ", tipsTransferForm);

        Set<ConstraintViolation<TIPSTransferForm>> violations = this.factory.validate(tipsTransferForm);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<TIPSTransferForm> violation : violations) {
                violError = violError + violation.getMessage() + "<br/>";
                errors.put( violation.getPropertyPath().toString(), violation.getMessage());
                LOGGER.info("FIELD[{}], TEMPLATE[{}], MESSAGE[{}]",violation.getPropertyPath().toString(), violation.getMessageTemplate(), violation.getMessage());
            }
            String errorMsg = String.format("The input has validation failed. [Row Data is '%s'],<br>[<b>Error message</b>:: '%s']", tipsTransferForm, violError.replace("#;", ""));
            response.setValidated(false);
            response.setErrorMessages(errors);
            response.setJsonString(violError);
            LOGGER.info("TIPS TRANSFER FORM error message:.....{}", violError);
        } else {
            DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
            String msgType = "LOCAL";
            String branchCode = session.getAttribute("branchCode").toString();//get the branch code from the session
            String txnType = "0101";
            String formattedDate = df.format(Calendar.getInstance().getTime());
            LOGGER.info("The formatted date is: ... {}",formattedDate);
            String initiatedBy = session.getAttribute("username").toString();
            String reference = branchCode + "TiPS" + formattedDate;

            String swiftMessage = "NA";

            try{
                //save the supporting documents
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        rtgsRepo.saveSupportingDocuments(reference, file);
                    }
                }
                if((new BigDecimal(tipsTransferForm.getAmount())).compareTo(new BigDecimal(systemVariable.TIPS_MAXIMUM_TRANSFER_LIMIT)) < 0){
                        //save transaction
                    int saveResp = tipsRepository.saveTipsTransfer(tipsTransferForm,reference, txnType, initiatedBy, swiftMessage, branchCode, files);
    //                LOGGER.info("TIPS REQUEST INITIATED: {}", tipsTransferForm.toString());
                    if (saveResp != -1) {
                        response.setValidated(true);
                        response.setJsonString("Transaction is successfully Initiated");
                    } else {
                        response.setJsonString("Failed to initiate TIPS transaction. Please try again!");
                    }
                }else{
                    response.setJsonString("This transaction is exceeding the maximum limit is which is " + systemVariable.TIPS_MAXIMUM_TRANSFER_LIMIT);
                }
            }catch (DataAccessException e){
                LOGGER.info("Exception in data access: ..{}",e.getMessage());
            }

        }
        return response;
    }

    @GetMapping(value = "/outwardTipsTransferOnWF")
    public String outwardTipsTransferOnWF(Model model, HttpSession session, HttpServletRequest request) {
        LOGGER.info("BRANCH APPROVER WORK-FLOW: AUTHORIZE TIPS TRANSACTIONS ON WORKFLOW");
        model.addAttribute("pageTitle", "AUTHORIZE TIPS TRANSACTIONS ON WORKFLOW");
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/outwardTipsTransferOnWF-> view TIPS Transactions on workflow ", "SUCCESS", "Viewed TIPS dashboard"));

        return "modules/tips/outwardTipsTransferOnWF";
    }

    @PostMapping(value = "/outwardTipsTransferOnWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String outwardTipsTransferOnWFAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
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
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/getRTGSTxnOnWorkFlowAjax-> populates transactions to workflow ", "SUCCESS", "get populated transactions to workflow"));

        return tipsRepository.getoutwardTipsTransferOnWFAjax(roleId, branchNo, draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }


    @RequestMapping(value = "/previewTipsSwiftMsg")
    public String previewTIPSMessage(@RequestParam Map<String, String> customeQuery, Model model, HttpServletRequest request, HttpSession session) {
        System.out.println("CHECKING SOURCE SWIFT MESSAGE: ");
        model.addAttribute("pageTitle", "AUTHORIZATION OF TIPS TRANSACTION WITH REFERENCE: " + customeQuery.get("reference"));
        model.addAttribute("supportingDocs", rtgsRepo.getSwiftMessageSupportingDocs(customeQuery.get("reference")));
        model.addAttribute("reference", customeQuery.get("reference"));
        model.addAttribute("returnReason", rtgsRepo.getReturnCodes());
        //audit trails
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/previewTipsSwiftMsg-> preview swift generated message", "SUCCESS", "preview swift generated message"));

        return "modules/tips/modals/previewTIPSSwiftMsg";
    }


    /*
     *BRANCH AUTHORIZE TIPS TRANSACTION FROM CUSTOMER ACCOUNT TO OUTWARD WAITING LEDGER
     */
    @RequestMapping(value = "/authorizeTIPSonWorkFlow", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String authorizeTIPSonWorkFlow(HttpSession httpsession, @RequestParam Map<String, String> customeQuery, HttpSession session, HttpServletRequest request) {
        String result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
                String postingRole = (String) httpsession.getAttribute("postingRole");
                String txid = customeQuery.get("reference");
                if (postingRole != null) {
                    //check if the role is allowed to process this transactions
                    BnUser user = (BnUser) httpsession.getAttribute("userCorebanking");

                    philae.ach.BnUser user2 = (philae.ach.BnUser) httpsession.getAttribute("achUserCorebanking");
                    for (UsRole role : user.getRoles().getRole()) {
                        if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                            philae.ach.UsRole achRole = userController.getAchRole(user2, postingRole);

                            //get transaction based on reference
                            TipsPaymentFullObject tipsTxn = tipsRepository.getTipsTransaction(txid);
//                            LOGGER.info("You are about to authorize the following TIPS transaction: ...{}", tipsTxn);

                            result = tipsRepository.processTIPSTransactionToCoreBanking(tipsTxn, role, achRole);
                            //audit trails
                            String username = session.getAttribute("username") + "";
                            String branchNo = session.getAttribute("branchCode") + "";
                            String roleId = session.getAttribute("roleId") + "";
                            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeTIPSonWorkFlow", "SUCCESS", "branch Authorize payments"));
                            break;
                        } else {
                            //audit trails
                            String username = session.getAttribute("username") + "";
                            String branchNo = session.getAttribute("branchCode") + "";
                            String roleId = session.getAttribute("roleId") + "";
                            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeRTGSonWorkFlow", "Failed", "Cannot authorize payment while the role is not selected for posting"));

                            result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                        }
                    }

                }

        } catch (Exception ex) {
            //audit trails
            String username = session.getAttribute("username") + "";
            String branchNo = session.getAttribute("branchCode") + "";
            String roleId = session.getAttribute("roleId") + "";
            this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/authorizeRTGSonWorkFlow", "Failed", "Exception occured during authorization: MESSAGE->" + ex.getMessage()));

            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }


    @GetMapping(value = "/inwardReversalOnWF")
    public String inwardReversalOnWF(Model model, HttpSession session, HttpServletRequest request) {
//        LOGGER.info("BRANCH APPROVER WORK-FLOW: INWARD AND OUTWARD TIPS TRANSACTIONS");
        model.addAttribute("pageTitle", "INWARD AND OUTWARD TIPS TRANSACTIONS");
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        model.addAttribute("startDate", DateUtil.previosDay(10));
        model.addAttribute("endDate", DateUtil.now("yyyy-MM-dd"));
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/outwardTipsTransferOnWF-> view TIPS Transactions on workflow ", "SUCCESS", "Viewed TIPS dashboard"));

        return "modules/tips/inwardReversalOnWF";
    }

    @PostMapping(value = "/inwardReversalOnWFAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String inwardReversalOnWFAjax(@RequestParam Map<String,String> customeQuery) {
        String finalResponse;
        String direction = customeQuery.get("tipsDirection");
        String fromDate = customeQuery.get("fromDate") + " 00:00:00";
        String toDate = customeQuery.get("toDate")+" 23:59:59";
//        LOGGER.info("TIPS TRANSACTION DIRECTION..{}",direction);
          return tipsRepository.getInwardReversalOnWFAjax(direction,fromDate,toDate);
    }

    @RequestMapping(value = "/getTipsTransactionDetails", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TipsJsonResponse getTipsTransactionDetails(@RequestParam Map<String,String> customeQuery, Model model){
        //get the transaction details using reference
        String reference = customeQuery.get("reverseTipsTxnReference");
//        LOGGER.info("Your trying to fetch tips transaction using reference....{}",reference);

        TipsPaymentFullObject finalResponse  =  tipsRepository.getTipsTransactionByReference(reference);
        LOGGER.info("TIPS RETURNED TRANSACTION FOR REVERSAL: ...{}",finalResponse);
        TipsJsonResponse tipsJsonResponse = new TipsJsonResponse();
        tipsJsonResponse.setStatus("SUCCESS");
        tipsJsonResponse.setData(finalResponse);
        return tipsJsonResponse;
    }



    @PostMapping("/initiateTipsTransferReversal")
    @ResponseBody
    public String initiateTipsTransferReversal(@RequestParam Map<String,String> customeQuery){
        String transReference = customeQuery.get("reverseTipsTxnReference");
        String reversalReason = customeQuery.get("reverseTxnReason");

        return tipsRepository.initiateTipsTransferReversal(transReference,reversalReason);

    }

    //TIPS REPORTS
    @GetMapping(value = "/tipsTransactionsReport")
    public String tipsTransactionsReport(Model model, HttpSession session, HttpServletRequest request) {
        model.addAttribute("pageTitle", "TIPS TRANSACTIONS REPORT");
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";
        this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/tipsTransactionsReport-> view TIPS Transactions Reports ", "SUCCESS", "Viewed TIPS Report"));
        Gson gson = new Gson();
        Type resultType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> result = null;
        try{
            result = gson.fromJson(tipsRepository.getFspParticipants(), resultType);
        }catch (Exception e){
            LOGGER.info("fsp participants exception.... {}",e);
        }
        model.addAttribute("fspParticipants",result);
        model.addAttribute("startDate",DateUtil.previosDay(2));
        model.addAttribute("endDate",DateUtil.tomorrow());

        return "modules/tips/tipsTransactionsReport";
    }

    @PostMapping(value = "/tipsTransactionsReportAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String tipsTransactionsReportAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) throws Exception {
        String finalResult = "Failed : -1";

        String direction = customeQuery.get("tipsDirection");
        String institution = customeQuery.get("institution");
        String fromDate = customeQuery.get("fromDate") + " 00:00:00";
        String toDate = customeQuery.get("toDate")+" 23:59:59";
         finalResult = tipsRepository.tipsTransactionsReportAjax(institution,direction,fromDate,toDate);
        return finalResult;
    }

    @PostMapping(value = "/confirmTxnStatusOnBOT", produces = "application/xml")
    public String confirmTxnStatusOnBOT(@RequestParam Map<String, String> customeQuery, Model model) throws Exception {
        String reference= customeQuery.get("reference");
        model.addAttribute("pageTitle", "TIPS BOT TRANSACTION DETAILS WITH REFERENCE: " + reference);
        model.addAttribute("reference", reference);

        model.addAttribute("transactionDetails",tipsRepository.getTransactionDetailsFromBOT(reference));
        return "modules/tips/modals/botTransactionDetails";
    }

    @PostMapping(value = "/initiateTipsTxnReversal")
    public String initiateTipsTxnReversal(@RequestParam Map<String, String> customeQuery, Model model) {
        String reference= customeQuery.get("reference");
        model.addAttribute("pageTitle", "INITIATE REVERSAL ON TIPS TXN WITH REFERENCE: " + reference);
        model.addAttribute("reference", reference);
        return "modules/tips/modals/initiateTipsReversal";
    }

    @PostMapping(value = "/reverseRequestedTipsTransaction")
    public String reverseRequestedTipsTransaction(@RequestParam Map<String, String> customeQuery, Model model) {
        String bankReference= customeQuery.get("bankReference");
        String reversalRef= customeQuery.get("reversalRef");
        model.addAttribute("pageTitle", "INITIATE REVERSAL ON TIPS TXN WITH BANK REFERENCE: " + bankReference + " REVERSAL REFERENCE: "+ reversalRef);
        model.addAttribute("bankReference", bankReference);
        model.addAttribute("reversalRef", reversalRef);

        return "modules/tips/modals/reverseRequestedTipsTransaction";
    }

    @PostMapping(value = "/tipsAuthorizeRequestedReversal")
    @ResponseBody
    public String tipsAuthorizeRequestedReversal(@RequestParam Map<String, String> customeQuery, Model model) {
        String bankReference= customeQuery.get("bankReference");
        String reversalRef= customeQuery.get("reversalRef");

        return tipsRepository.authorizeRequestedReversal(bankReference,reversalRef);
    }



    @PostMapping(value = "/fireCancelTipsTxnReversal")
    public String fireCancelTipsTxnReversal(@RequestParam Map<String, String> customeQuery, Model model) {
        String bankReference= customeQuery.get("bankReference");
        String reversalRef= customeQuery.get("reversalRef");
        model.addAttribute("pageTitle", "CANCEL REVERSAL ON TIPS TXN WITH BANK REFERENCE: " + bankReference + " REVERSAL REFERENCE: "+ reversalRef);
        model.addAttribute("bankReference", bankReference);
        model.addAttribute("reversalRef", reversalRef);

        return "modules/tips/modals/transferReversalCancellation";
    }


    @PostMapping(value = "/fireAuthorizeReversalCancellation")
    @ResponseBody
    public String tipsAuthorizeReversalCancellation(@RequestParam Map<String, String> customeQuery, Model model) {
        String bankReference= customeQuery.get("bankReference");
        String reversalRef= customeQuery.get("reversalRef");

        return tipsRepository.tipsAuthorizeReversalCancellation(bankReference,reversalRef);
    }


    @RequestMapping(value = "/tipsFraudRegistration")
    public String tipsFraudRegistration(Model model) {
        model.addAttribute("pageTitle", "FRAUD REGISTRATION");
        model.addAttribute("startDate",DateUtil.previosDay(10));
        model.addAttribute("endDate",DateUtil.tomorrow());
        return "modules/tips/tipsFraudRegistration";
    }

    @PostMapping("/registerTipsFraud")
    @ResponseBody
    public TipsJsonResponse registerTipsFraud(@Valid FraudRegistrationForm formData, BindingResult bindingResult, HttpSession session, HttpServletRequest request){
        String username = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String roleId = session.getAttribute("roleId") + "";

        TipsJsonResponse tipsJsonResponse = new TipsJsonResponse();
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(
                            Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (error1, error2) -> {
                                return error1;
                            })
                    );

            tipsJsonResponse.setStatus("FAIL");
            tipsJsonResponse.setData(errors);
        } else {
            if (1 == tipsRepository.registerTipsFraud(formData)) {
                this.applicationEventPublisher.publishEvent(new AuditLogEvent(username, roleId, branchNo, request.getRemoteHost(), "/registerTipsFraud", "SUCCESS", "registered Tips Fraud"));
                tipsJsonResponse.setStatus("SUCCESS");
                tipsJsonResponse.setData("Succussfully saved into the database");
            } else {
                tipsJsonResponse.setStatus("ERROR");
                tipsJsonResponse.setData("failed to submit data into the database");
            }
        }
        return tipsJsonResponse;
    }


    @PostMapping(value = "/getTCBTipsFraudsAjax", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTCBTipsFraudsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String finalResult = "Failed: -1";

        String fsp = customeQuery.get("currentFsp");
        String fromDate = customeQuery.get("fromDate") + " 00:00:00";
        String toDate = customeQuery.get("toDate")+" 23:59:59";

        finalResult = tipsRepository.getTCBTipsFraudsAjax(fsp,fromDate,toDate);
        return finalResult;
    }


    //download transfer advice
    @RequestMapping(value = "/fireDownloadTransferAdvice")
    @ResponseBody
    public String printTransactionReceipt(HttpServletResponse response, @RequestParam Map<String, String> customeQuery) {
        try {
            String reference = customeQuery.get("bankReference");
            byte[] databyte = tipsRepository.printTransactionReceipt(reference);

            byte[] file = databyte;
            response.setContentType("application/pdf");

            response.getOutputStream().write(file);
            response.getOutputStream().close();
//            LOGGER.info("supporting document... DOWNLOADED SUCCESSFULLY {}", reference);
            return "OK";
        } catch (Exception ex) {
            LOGGER.info("exception on download tips transfer advice copy:{}", ex);
        }
        return "OK";
    }

    @GetMapping("/resolveMissingTipsTransactions")
    public String resolveMissingTipsTransactions(Model model, HttpSession session, @RequestParam Map<String, String> customeQuery) {
        model.addAttribute("pageTitle", "RESOLVE TIPS PAYMENTS MISSING ON TIPS REPORT");
        return "modules/tips/resolveMissingTipsTransactions";
    }

    @PostMapping("/resolveMissingTipsTransactionsAjax")
    @ResponseBody
    public JsonResponse resolveMissingTipsTransactionsAjax(@RequestParam Map<String, String> customeQuery, HttpSession session) {
        String reference = customeQuery.get("reference");
        String branchCode = session.getAttribute("branchCode").toString();
        return tipsRepository.validateMissingTipsTransaction(reference,branchCode);
    }
}
