package com.controller;

import com.DTO.GeneralJsonResponse;
import com.DTO.tausi.AgentDepositNotifyRootResp;
import com.DTO.tausi.AgentDetailsRootResp;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.repository.TausiRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import com.service.HttpClientService;
import philae.api.BnUser;
import philae.api.UsRole;

@Controller
public class TausiController {

    @Autowired
    TausiRepository tausiRepository;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    SYSENV sysenv;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TausiController.class);
    @Autowired
    private SYSENV sYSENV;

    @GetMapping("/tausiPaymentsDashboard")
    public String tausiPaymentsDashboard(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","TAUSI TRANSACTIONS DASHBOARD MODULE");
        model.addAttribute("tausiModulePermissions", tausiRepository.getLoanModulePermissions("/tausiPaymentsDashboard", httpSession.getAttribute("roleId").toString()));
        return "/modules/tausi/dashboard";
    }

    @GetMapping("/tausiPaymentsPosting")
    public String tausiPaymentsPosting(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","VALIDATE TAUSI AGENT DETAILS FOR MAPPING");
        return "/modules/tausi/agentDetails";
    }

    @PostMapping("fireValidateAgentDetails")
    @ResponseBody
    public String validateAgent(@RequestParam Map<String, String> customeQuery) throws JsonProcessingException {
        String txnId = DateUtil.now("yyyyMMddhhMMss");

        XMLGregorianCalendar gregorianCalendar = null;
        try {
            gregorianCalendar = DateUtil.dateToGregorianCalendar(DateUtil.now("yyyy-MM-dd'T'HH:mm:ss"), "yyyy-MM-dd'T'HH:mm:ss");
        } catch (DatatypeConfigurationException | ParseException e) {
            LOGGER.warn("Error parsing date: {}", e);
        }

        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<tausiReq>\n" +
                "    <agentCode>"+customeQuery.get("agent_code")+"</agentCode>\n" +
                "    <agentReference>"+customeQuery.get("agent_reference")+"</agentReference>\n" +
                "    <transactionId>ad"+txnId+"</transactionId>\n" +
                "    <requestTime>"+gregorianCalendar+"</requestTime>\n" +
                "</tausiReq>";

            return HttpClientService.sendXMLRequest(payload,sysenv.KIPAYMENT_TAUSI_AGENT_DETAILS_URL);
    }

    @PostMapping("fireMapAgentDetails")
    @ResponseBody
    public GeneralJsonResponse fireMapAgentDetails(@RequestParam Map<String,String> customeQuery){
        GeneralJsonResponse response = new GeneralJsonResponse();
        int count = tausiRepository.checkIfAgentMapped(customeQuery.get("agentCode"),customeQuery.get("agentReference"),customeQuery.get("agentAccount"));

        String txnId = "an"+DateUtil.now("yyyyMMddhhMMss");

        XMLGregorianCalendar gregorianCalendar = null;
        try {
            gregorianCalendar = DateUtil.dateToGregorianCalendar(DateUtil.now("yyyy-MM-dd'T'HH:mm:ss"), "yyyy-MM-dd'T'HH:mm:ss");
        } catch (DatatypeConfigurationException | ParseException e) {
            LOGGER.warn("Error parsing date: {}", e);
        }
        if(count<1) {
            if (1 == tausiRepository.mapTausiAgent(customeQuery.get("agentAccount"), customeQuery.get("agentAccountName"), customeQuery.get("status"), customeQuery.get("agentCode"), customeQuery.get("agentName"), customeQuery.get("agentReference"), customeQuery.get("description"), customeQuery.get("requestTime"), customeQuery.get("transactionId"))) {
                response.setResult("200");
                response.setStatus("success");
//                String payload  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                        "<tausiReq>\n" +
//                        "    <agentCode>"+customeQuery.get("agentCode")+"</agentCode>\n" +
//                        "    <agentReference>"+customeQuery.get("agentReference")+"</agentReference>\n" +
//                        "    <active>true</active>\n" +
//                        "    <transactionId>"+txnId+"</transactionId>\n" +
//                        "    <requestTime>"+gregorianCalendar+"</requestTime>\n" +
//                        "</tausiReq>";
//
//                String notifyresp = HttpClientService.sendXMLRequest(payload,"http://172.21.2.12:8083/api/tausiAgentAcctStatusNotify.php");
//                LOGGER.info("Account notify status ... {}  agent reference... {}",notifyresp,customeQuery.get("agentReference"));
            } else {
                response.setResult("99");
                response.setStatus("Failed");
            }
        }else{
            response.setResult("26");
            response.setStatus("Another agent exists");
        }

            return response;
    }

    @GetMapping("/tausiMappedAccounts")
    public String tausiMappedAccounts(Model model, HttpSession httpSession){
        model.addAttribute("pageTitle","PLEASE VERIFY AND NOTIFY TAUSI FOR AGENT WITH NOT NOTIFIED STATUS");
        return "/modules/tausi/tausiMappedAccounts";
    }


    @RequestMapping(value = "/getTausiMappedAccountsAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTausiMappedAccountsAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String draw = customeQuery.get("draw");
        String start = customeQuery.get("start");
        String rowPerPage = customeQuery.get("length");
        String searchValue = customeQuery.get("search[value]") != null ? customeQuery.get("search[value]").trim() : "";
        String columnIndex = customeQuery.get("order[0][column]");
        String columnName = customeQuery.get("columns[" + columnIndex + "][data]");
        String columnSortOrder = customeQuery.get("order[0][dir]");
        return tausiRepository.getTausiMappedAccountsAjax(draw, start, rowPerPage, searchValue, columnIndex, columnName, columnSortOrder);
    }

    @PostMapping(value = "fireTausiAccountDetailsModal")
    public String getAcctDetailsModal(@RequestParam Map<String,String> customeQuery, Model model){
        model.addAttribute("pageTitle","PREVIEW AGENT DETAILS WITH AGENT CODE " + customeQuery.get("agentCode") + " AND AGENT REFERENCE " + customeQuery.get("agentReference"));
        model.addAttribute("agentDeta", tausiRepository.getAgentDeta(customeQuery.get("agentCode"), customeQuery.get("agentReference")));
        return "/modules/tausi/modal/agentDetailsForNotify";
    }

    @PostMapping("fireNotifyTausiAcctStatus")
    @ResponseBody
    public String submitNotifyTausiAcctStatus(@RequestParam Map<String, String> customeQuery) throws JsonProcessingException {
        Map<String,Object> agentDeta =  tausiRepository.getAgentDeta(customeQuery.get("agentCode"), customeQuery.get("agentReference"));
        String txnId = DateUtil.now("yyyyMMddhhMMss");
//        XMLGregorianCalendar gregorianCalendar = null;
//        try {
//            gregorianCalendar = DateUtil.dateToGregorianCalendar(DateUtil.now("yyyy-MM-dd'T'HH:mm:ss"), "yyyy-MM-dd'T'HH:mm:ss");
//        } catch (DatatypeConfigurationException | ParseException e) {
//            LOGGER.warn("Error parsing date: {}", e);
//        }
        String req = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<tausiReq>\n" +
                "    <agentCode>"+agentDeta.get("agentCode")+"</agentCode>\n" +
                "    <agentReference>"+agentDeta.get("agentReference")+"</agentReference>\n" +
                "    <active>true</active>\n" +
                "    <transactionId>an+"+txnId+"</transactionId>\n" +
                "    <requestTime>"+DateUtil.now("yyyy-MM-dd'T'HH:mm:ss")+"</requestTime>\n" +
                "</tausiReq>";

        String apiResponse = HttpClientService.sendXMLRequest(req,sysenv.KIPAYMENT_TAUSI_NOTIFY_ACCOUNT_STATUS_URL);

        LOGGER.warn("apiResponse: {} {}", agentDeta.get("agentCode"),apiResponse);

        AgentDetailsRootResp response = jacksonMapper.readValue(apiResponse,AgentDetailsRootResp.class);

        if(response.getStatusCode()==21000) {
            int updateResponse = tausiRepository.updateAgentAcctAfterNotify(customeQuery.get("agentCode"), customeQuery.get("agentReference") , customeQuery.get("transactionId"),response.getData().getTransactionId());
            LOGGER.info("is updated .. {}", updateResponse);
        }
        return apiResponse;
    }


    @PostMapping(value = "fireTausiAccountDepositModal")
    public String fireTausiAccountDepositModal(@RequestParam Map<String,String> customeQuery, Model model){
        model.addAttribute("pageTitle","YOU ARE ABOUT TO INITIATE TRANSACTION TO AGENT WITH CODE " + customeQuery.get("agentCode") + " AND AGENT REFERENCE " + customeQuery.get("agentReference"));
        model.addAttribute("agentDeta", tausiRepository.getAgentDeta(customeQuery.get("agentCode"), customeQuery.get("agentReference") ));
        return "/modules/tausi/modal/agentDetailsForDeposit";
    }

    @PostMapping("fireInitiateAgentAccount")
    @ResponseBody
    public GeneralJsonResponse initiateAgentTransaction(@RequestParam Map<String,String> customeQuery, HttpSession session){
        GeneralJsonResponse response = new GeneralJsonResponse();
        String inititator = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String sourceAcct = sysenv.TRANSFER_AWAITING_EFT_LEDGER.replace("***",branchNo);
        String agentCode = customeQuery.get("agentCode");
        String agentReference = customeQuery.get("agentReference");

        Map<String,Object> agentData = tausiRepository.getAgentDeta(agentCode,agentReference);
        String transRef = "TAF" + DateUtil.now("yyyyMMddhhMMss");

        int result = tausiRepository.initiageAgentTransaction(agentCode,agentReference, (String) agentData.get("accountName"),transRef,sourceAcct , customeQuery.get("agentAccount") ,customeQuery.get("amount").replaceAll(",",""), inititator, branchNo);
        if(1==result){
            response.setResult("200");
            response.setStatus("Transaction initiated successfully");
        }else{
            response.setResult("99");
            response.setStatus("Transaction initiation failed");
        }
        return response;
    }


    @PostMapping("fireInitiateAgentAccountByTeller")
    @ResponseBody
    public String fireInitiateAgentAccountByTeller(@RequestParam Map<String,String> customeQuery, HttpSession session){
        String result = "{\"status\":\"99\",\"message\":\"General failure\"}";

        String inititator = session.getAttribute("username") + "";
        String branchNo = session.getAttribute("branchCode") + "";
        String agentCode = customeQuery.get("agentCode");
        String agentReference = customeQuery.get("agentReference");

        String postingRole = (String) session.getAttribute("postingRole");
        if (postingRole != null) {
            //check if the role is allowed to process this transactions
            BnUser user = (BnUser) session.getAttribute("userCorebanking");
            philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
            for (UsRole role : user.getRoles().getRole()) {
                if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                    result = tausiRepository.tellerPostTausiAgentDepositTOCBS(customeQuery.get("agentCode"), customeQuery.get("agentReference"),customeQuery.get("agentAccount") ,customeQuery.get("amount").replaceAll(",",""), branchNo,inititator, role,customeQuery.get("depositorName"));
                    break;
                } else {
                    result = "{\"status\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                }
            }

        }

        return result;
    }


    @GetMapping("initiatedTausiTransactionForApproval")
    public String getInitiatedTxns(@RequestParam Map<String,String> customeQuery, Model model,HttpSession session){
        model.addAttribute("pageTitle","TAUSI AGENTS TRANSACTIONS");
        model.addAttribute("fromDate", DateUtil.previosDay(2));
        model.addAttribute("toDate",DateUtil.tomorrow());
        model.addAttribute("branches", tausiRepository.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("hqCheck",session.getAttribute("branchCode"));
        return "/modules/tausi/agentTransactions";
    }


    @PostMapping("fireGetAgentTransactions")
    @ResponseBody
    public GeneralJsonResponse getAgentTransactions(@RequestParam Map<String,String> customeQuery){
        GeneralJsonResponse response = new GeneralJsonResponse();
        response.setStatus("200");
        response.setResult(tausiRepository.getAgentTransactions(customeQuery));
        return response;
    }


    @PostMapping(value = "firePreviewAgentTransaction")
    public String firePreviewAgentTransaction(@RequestParam Map<String,String> customeQuery, Model model){
        model.addAttribute("pageTitle","YOU ARE ABOUT TO APPROVE TRANSACTION TO AGENT WITH CODE " + customeQuery.get("agentCode") + " AND AGENT REFERENCE " + customeQuery.get("agentReference"));
        model.addAttribute("txnDeta", tausiRepository.getTransaction(customeQuery.get("agentCode"), customeQuery.get("agentReference") , customeQuery.get("transRef") ));
        return "/modules/tausi/modal/agentTransaction";
    }

    @RequestMapping(value = "fireApproveAgentTransaction", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveAgentDeposit(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            String aprover = session.getAttribute("username") + "";

            String postingRole = (String) session.getAttribute("postingRole");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        result = tausiRepository.processTausiAgentDepositTOCBS(customeQuery.get("agentCode"), customeQuery.get("agentReference"), customeQuery.get("transRef"),aprover, role);
                        break;
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }



    @PostMapping(value = "firePreviewNotifyAgentDeposie")
    public String firePreviewNotifyAgentDeposie(@RequestParam Map<String,String> customeQuery, Model model){
        model.addAttribute("pageTitle","YOU ARE ABOUT TO NOTIFY TAUSI ABOUT AGENT DEPOSIT WITH CODE " + customeQuery.get("agentCode") + " AND AGENT REFERENCE " + customeQuery.get("agentReference"));
        model.addAttribute("txnDeta", tausiRepository.getTransaction(customeQuery.get("agentCode"), customeQuery.get("agentReference") , customeQuery.get("transRef") ));
        return "/modules/tausi/modal/agentTransactionNotify";
    }


    @RequestMapping(value = "fireApproveAgentTransactionNotify", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireApproveAgentTransactionNotify(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String notifier = session.getAttribute("username") + "";

            String postingRole = (String) session.getAttribute("postingRole");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        Map<String,Object> txnDeta= tausiRepository.getTransaction(customeQuery.get("agentCode"), customeQuery.get("agentReference"), customeQuery.get("transRef"));

                        String txnId = DateUtil.now("yyyyMMddhhMMss");


                        XMLGregorianCalendar approvedDt = null;
                        try {
                            approvedDt = DateUtil.dateToGregorianCalendar((String) txnDeta.get("branch_approved_dt"),"yyyy-MM-dd");
                        } catch (DatatypeConfigurationException | ParseException e) {
                            LOGGER.warn("Error parsing date: {}", e);
                        }
                        int intAmount = new BigDecimal(String.valueOf(txnDeta.get("amount"))).intValue();
                            String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                    "<tausiReq>\n" +
                                    "    <agentCode>"+txnDeta.get("agentCode")+"</agentCode>\n" +
                                    "    <agentReference>"+txnDeta.get("agentReference")+"</agentReference>\n" +
                                    "    <amount>"+intAmount+"</amount>\n" +
                                    "    <depositTime>"+approvedDt+"</depositTime>\n" +
                                    "    <transactionId>"+txnDeta.get("reference")+"</transactionId>\n" +
                                    "    <requestTime>"+DateUtil.now("yyyy-MM-dd'T'HH:mm:ss")+"</requestTime>\n" +
                                    "</tausiReq>";
                        String apiResponse = HttpClientService.sendXMLRequest(payload,sysenv.KIPAYMENT_TAUSI_NOTIFY_AGENT_STATUS_URL);

                        LOGGER.warn("apiResponse: {} {}", txnDeta.get("agentCode"),apiResponse);
                        AgentDepositNotifyRootResp response = jacksonMapper.readValue(apiResponse, AgentDepositNotifyRootResp.class);

                        if(response.getStatusCode()==21000) {
                            int updateResponse = tausiRepository.updateAgentTransactionAfterNotify(customeQuery.get("agentCode"), customeQuery.get("agentReference") ,customeQuery.get("transRef"),response.getStatusCode(),response.getDescription(),notifier);
                            LOGGER.info("is updated .. {}", updateResponse);
                        }
                        result = "{\"result\":"+response.getStatusCode()+",\"message\":"+response.getDescription()+"}";
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }
        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

    @GetMapping("tausiRequestsForSettlements")
    public String tausiRequestsForSettlements(@RequestParam Map<String,String> customeQuery, Model model,HttpSession session){
        model.addAttribute("pageTitle","TRANSACTIONS FROM TAUSI FOR SETTLEMENT");
        model.addAttribute("fromDate", DateUtil.previosDay(2));
        model.addAttribute("toDate",DateUtil.tomorrow());
        model.addAttribute("branches", tausiRepository.branches(session.getAttribute("branchCode") + ""));
        model.addAttribute("hqCheck",session.getAttribute("branchCode"));
        return "/modules/tausi/transactionsForSettlement";
    }

    @RequestMapping(value = "/fireGetAgentTransactionsForSettlementAjax", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public GeneralJsonResponse getTausiRequestsSettlementAjax(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session){

        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        String status = customeQuery.get("status");
        List<Map<String,Object>> data = tausiRepository.fireGetAgentTransactionsForSettlementAjax(status,fromDate,toDate);
        GeneralJsonResponse response = new GeneralJsonResponse();
        response.setStatus("200");
        response.setResult(data);
        return response;
    }


    @PostMapping(value = "firePreviewASettlementB4Approve")
    public String firePreviewASettlementB4Approve(@RequestParam Map<String,String> customeQuery, Model model){
        model.addAttribute("pageTitle","YOU ARE ABOUT TO APPROVE SETTLEMENT TRANSACTION FROM AGENT USING CONTROL NUMBER " + customeQuery.get("controlNo"));
        model.addAttribute("controlNoData",tausiRepository.validateControlNoFromGePG(customeQuery.get("controlNo"),customeQuery.get("transRef")));
        model.addAttribute("txnDeta", tausiRepository.getTransaction(customeQuery.get("agentCode"), customeQuery.get("agentReference") , customeQuery.get("transRef") ));
        return "/modules/tausi/modal/previewAgentTransactionForSettlement";
    }


    @RequestMapping(value = "fireApproveSettlementAgentTransaction", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String approveAgentSettlementToLGA(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            String aprover = session.getAttribute("username") + "";

            String postingRole = (String) session.getAttribute("postingRole");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        result = tausiRepository.processTausiAgentTransactionForSettlement(customeQuery.get("agentCode"), customeQuery.get("agentReference"), customeQuery.get("transRef"),aprover, role);
                        break;
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }


    @PostMapping(value = "firePreviewCancelMispostedTransaction")
    public String firePreviewCancelMispostedTransaction(@RequestParam Map<String,String> customeQuery, Model model){
        model.addAttribute("pageTitle","YOU ARE ABOUT CANCEL AGENT DEPOSIT TRANSACTION WITH CODE " + customeQuery.get("agentCode") + " AND AGENT REFERENCE " + customeQuery.get("agentReference"));
        model.addAttribute("txnDeta", tausiRepository.getTransaction(customeQuery.get("agentCode"), customeQuery.get("agentReference") , customeQuery.get("transRef") ));
        return "/modules/tausi/modal/agentMispostedTransaction";
    }


    @RequestMapping(value = "fireApproveCancelMispostedTransaction", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String fireApproveCancelMispostedTransaction(@RequestParam Map<String, String> customeQuery, HttpServletRequest request, HttpSession session) {
        String result = result = "{\"result\":\"35\",\"message\":\"Your Role is not allowed to perform posting: \"}";
        try {
            String aprover = session.getAttribute("username") + "";

            String postingRole = (String) session.getAttribute("postingRole");
            if (postingRole != null) {
                //check if the role is allowed to process this transactions
                BnUser user = (BnUser) session.getAttribute("userCorebanking");
                philae.ach.BnUser user2 = (philae.ach.BnUser) session.getAttribute("achUserCorebanking");
                for (UsRole role : user.getRoles().getRole()) {
                    if (postingRole.equalsIgnoreCase(role.getUserRoleId().toString())) {
                        int updateResult = tausiRepository.flagMispostedAgentTransaction(customeQuery.get("agentCode"),aprover, customeQuery.get("agentReference"), customeQuery.get("transRef"));
                        if(1==updateResult){
                            result = "{\"result\":\"200\",\"message\":\"Transaction Cancelled Successfully\"}";
                        }else{
                            result = "{\"result\":\"99\",\"message\":\"Failed to cancel transaction\"}";
                        }
                    } else {
                        result = "{\"result\":\"35\",\"message\":\"Your Role Has no Permission to perform This Transaction. Also confirm that you have selected Posting Role!!!\"}";
                    }
                }

            }

        } catch (Exception ex) {
            result = "{\"result\":\"99\",\"message\":\"General Error occured: " + ex.getMessage() + " \"}";
            LOGGER.info(null, ex);
        }
        return result;
    }

}
