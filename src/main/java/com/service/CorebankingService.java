/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.DTO.AccountNameQuery;
import com.config.SYSENV;
import com.helper.DateUtil;
import static com.service.HttpClientService.sendXMLRequest;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 *
 * @author melleji.mollel
 */
@Service
public class CorebankingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorebankingService.class);
    @Autowired
    SYSENV systemVariable;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    public String processRequestToCore(String req, String reqName) {
//        LOGGER.info("GENERIC FUNCTION FOR RUBIKON webservices: {}");
        String response = "-1";
        StopWatch watch = new StopWatch();
        watch.start();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>"
                + "<" + reqName + ">"
                + req
                + "</" + reqName + ">"
                + "</soapenv:Body>\n"
                + "</soapenv:Envelope>";
        LOGGER.info("REQUEST TO CORE: " + request);
        String coreResponse = HttpClientService.sendXMLReqBasicAuth(request, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!coreResponse.equals("-1")) {
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(coreResponse, "body", "return");
            response = XMLParserService.xmlsrToString(xmlr).replace(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "").replace(" xsi:nil=\"true\"", "");
            System.out.println("RESPONSE DEBUGGING: " + response);
        }
        return response;
    }

    public String processRequestToCoreTestCM(String req, String reqName) {
//        LOGGER.info("GENERIC FUNCTION FOR RUBIKON webservices: {}");
        String response = "-1";
        StopWatch watch = new StopWatch();
        watch.start();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>"
                + "<" + reqName + ">"
                + req
                + "</" + reqName + ">"
                + "</soapenv:Body>\n"
                + "</soapenv:Envelope>";
        LOGGER.info("PREAPARED REQUEST: " + request);
        String coreResponse = HttpClientService.sendXMLReqBasicAuth(request, this.systemVariable.CORE_BANKING_TEST_POSTING_URL, "xapi", "x@pi#81*");
        if (!coreResponse.equals("-1")) {
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(coreResponse, "body", "return");
            response = XMLParserService.xmlsrToString(xmlr).replace(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "").replace(" xsi:nil=\"true\"", "");
            System.out.println("RESPONSE DEBUGGING: " + response);
        }
        return response;
    }

    public String processRTGSEFTToCore(String req, String reqName) {
//        LOGGER.info("GENERIC FUNCTION FOR RUBIKON webservices: {}");
        String response = "-1";
        StopWatch watch = new StopWatch();
        watch.start();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ach=\"http://ach.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>"
                + "<" + reqName + ">"
                + req
                + "</" + reqName + ">"
                + "</soapenv:Body>\n"
                + "</soapenv:Envelope>";
        LOGGER.info("REQUEST TO CORE BANKING: " + request);
        String coreResponse = HttpClientService.sendXMLReqBasicAuth(request, this.systemVariable.CORE_BANKING_POSTING_URL_RTGS, "tach", "t@ch#71*");
        if (!coreResponse.equals("-1")) {
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(coreResponse, "body", "return");
            response = XMLParserService.xmlsrToString(xmlr).replace(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "").replace(" xsi:nil=\"true\"", "");
            LOGGER.info("RESPONSE FROM CORE BANKING: " + response);
        }
        return response;
    }

    public String processRTGSEFTTESTToCore(String req, String reqName) {
//        LOGGER.info("GENERIC FUNCTION FOR RUBIKON webservices: {}");
        String response = "-1";
        StopWatch watch = new StopWatch();
        watch.start();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ach=\"http://ach.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>"
                + "<" + reqName + ">"
                + req
                + "</" + reqName + ">"
                + "</soapenv:Body>\n"
                + "</soapenv:Envelope>";
        LOGGER.info("REQUEST TO CORE BANKING: " + request);
        String coreResponse = HttpClientService.sendXMLReqBasicAuth(request, this.systemVariable.CORE_BANKING_POSTING_URL_RTGS_TACH, "tach", "t@ch#71*");
        LOGGER.info("RAW RESPONSE FROM CORE BANKING: " + coreResponse);
        if (!coreResponse.equals("-1")) {
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(coreResponse, "body", "return");
            response = XMLParserService.xmlsrToString(xmlr).replace(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "").replace(" xsi:nil=\"true\"", "");
            LOGGER.info("RESPONSE FROM: " + response);
        }
        return response;
    }

    public String sendSMS(String smsBody, String msisdn, String sessionId) {
        Random rand = new Random();
        String response = "-1";
        try {
            String msgToBeSent = smsBody.replace("&", "and");
            msgToBeSent = msgToBeSent.replace("'", "");
            String xmlMsg  = "<methodCall>"
                        + "<methodName>TPB.SENDSMS</methodName>"
                        + "<params>"
                        + "<param><value><string>" + msisdn+ "</string></value></param>"
                        + "<param><value><string>" + msgToBeSent + "</string></value></param>"
                        + "<param><value><string>" + sessionId + "</string></value></param>"
                        + "</params>"
                        + "</methodCall>";

            response = HttpClientService.sendXMLRequest(xmlMsg, this.systemVariable.SMSC_URL);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return response;
    }

    public void sendSmsToApprovers(String branchCode, String message, String reference) {
        List<Map<String, Object>> msisdns = getApproversMsisdns(branchCode);
        if (msisdns != null) {
            for (Map<String, Object> approverMsisdn : msisdns) {
                String response = "-1";
                String request = "<methodCall>"
                        + "<methodName>TPB.SENDSMS</methodName>"
                        + "<params>"
                        + "<param><value><string>" + approverMsisdn.get("phoneNo") + "</string></value></param>"
                        + "<param><value><string>" + message.replace("{APPROVER_NAME}", approverMsisdn.get("first_name").toString().toUpperCase() + " " + approverMsisdn.get("last_name").toString().toUpperCase()) + "</string></value></param>"
                        + "<param><value><string>" + reference + "</string></value></param>"
                        + "</params>"
                        + "</methodCall>";
                String smsResponse = HttpClientService.sendXMLRequest(request, this.systemVariable.SMSC_URL);
                LOGGER.info("REQUEST TO GATEWAY: {}", request);
                LOGGER.info("RAW RESPONSE FROM GATEWAY: {}", smsResponse);
                if (!smsResponse.equals("-1")) {
                    //XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(smsResponse, "body", "return");
                    //response = XMLParserService.xmlsrToString(xmlr).replace(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "").replace(" xsi:nil=\"true\"", "");
                    LOGGER.info("RESPONSE FROM GATEWAY:{} " + response);
                }
            }
        }
    }

    public List<Map<String, Object>> getApproversMsisdns(String branchCode) {
        List<Map<String, Object>> msisdns = null;
        try {
            msisdns = this.jdbcTemplate.queryForList("SELECT * from users where status ='ACTIVE' and branch_no=? and id in (select user_id from user_roles where role_id in (27,31,28))", branchCode);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON GETTING MSISDNS FOR APPROVERS: {}", ex.getMessage());
        }
        return msisdns;
    }

    public AccountNameQuery accountNameQuery(String account) {
        AccountNameQuery nameResp=new AccountNameQuery();
//        LOGGER.info("GENERIC FUNCTION FOR RUBIKON webservices: {}");
        String response = "-1";

        StopWatch watch = new StopWatch();
        watch.start();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ach=\"http://ach.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <ach:queryDepositAccount>\n"
                + "         <!--Optional:-->\n"
                + "         <query>\n"
                + "            <reference>AN" + DateUtil.now("yyyyMMddHHmmss") + "</reference>\n"
                + "            <accountNumber>" + account + "</accountNumber>\n"
                + "         </query>\n"
                + "      </ach:queryDepositAccount>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        LOGGER.info("REQUEST TO CORE BANKING: " + request);
        String coreResponse = HttpClientService.sendXMLReqBasicAuth(request, this.systemVariable.CORE_BANKING_POSTING_URL_RTGS_TACH, "tach", "t@ch#71*");
        LOGGER.info("RAW RESPONSE FROM CORE BANKING: " + coreResponse);
        if (!coreResponse.equals("-1")) {
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(coreResponse, "body", "return");
            String result = XMLParserService.getDomTagText("result", coreResponse);
            if (result.equalsIgnoreCase("0")) {
                response = XMLParserService.getDomTagText("accountName", coreResponse);
                nameResp.setAccountName(XMLParserService.getDomTagText("accountName", coreResponse));
                nameResp.setAccountCurrency(XMLParserService.getDomTagText("currency", coreResponse));
            }

        }
        LOGGER.info("ACCOUNT LOOKUP RESPONSE:{}",nameResp);
        return nameResp;
    }

    public String mobileNumberQuery(Long custId) {
        String response = "-1";

        StopWatch watch = new StopWatch();
        watch.start();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <api:queryCustomer>\n" +
                "         <!--Optional:-->\n" +
                "         <request>\n" +
                "            <reference>" + DateUtil.now("yyyyMMddHHmmss") +  "</reference>\n" +
                "            <!--Optional:-->\n" +
                "            <custId>" + custId + "</custId>\n" +
                "            <!--Optional:-->\n" +
                "            <custNo>?</custNo>\n" +
                "         </request>\n" +
                "      </api:queryCustomer>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        LOGGER.info("REQUEST TO CORE BANKING: " + request);
        String coreResponse = HttpClientService.sendXMLReqBasicAuth(request, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        LOGGER.info("RAW RESPONSE FROM CORE BANKING: " + coreResponse);
        if (!coreResponse.equals("-1")) {
            String customer = XMLParserService.getDomTagText("customer", coreResponse);
            if (customer != null) {
                response = XMLParserService.getDomTagText("mobileNumber", coreResponse);
            }
        }
        return response;
    }
}
