/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.config.SYSENV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import javax.xml.stream.XMLStreamReader;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import philae.ach.TaResponse;
import philae.api.XaResponse;

/**
 * @author samichael
 */
@Service
public class XapiWebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XapiWebService.class);

        @Autowired
    SYSENV systemVariable;


    public int postDepositToGLTransfer(String suspenseGLAccount, String txid, BigDecimal amount, String account, String currency, String narration) {
        LOGGER.info("Transafer amount from deposit Account to GL account: {}", txid);
        int status = -1;
        XaResponse xiResponse = null;
        StopWatch watch = new StopWatch();
        watch.start();
        String userRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <api:postDepositToGLTransfer>\n"
                + "         <request>\n"
                + "            <reference>" + txid + "</reference>\n"
                + "            <debitAccount>" + account + "</debitAccount>\n"
                + "            <creditAccount>" + suspenseGLAccount + "</creditAccount>\n"
                + "            <currency>" + currency + "</currency>\n"
                + "            <amount>" + (amount) + "</amount>\n"
                + "            <narration>" + narration + "</narration>\n"
                + "            <userRole>\n"
                + "               <branchCode>060</branchCode>\n"
                + "               <branchId>-5</branchId>\n"
                + "               <branchName>IT AND OPERATIONS</branchName>\n"
                + "               <buRoleId>5559</buRoleId>\n"
                + "               <drawers>\n"
                + "                  <currencies>\n"
                + "                     <currencyCode>?</currencyCode>\n"
                + "                     <currencyId>?</currencyId>\n"
                + "                     <currencyName>?</currencyName>\n"
                + "                     <decimalPlaces>?</decimalPlaces>\n"
                + "                  </currencies>\n"
                + "                  <drawerAccount>?</drawerAccount>\n"
                + "                  <drawerId>?</drawerId>\n"
                + "                  <drawerNumber>?</drawerNumber>\n"
                + "                  <openDate>?</openDate>\n"
                + "                  <status>?</status>\n"
                + "               </drawers>\n"
                + "               <limits>\n"
                + "                  <creditLimit>?</creditLimit>\n"
                + "                  <currency>?</currency>\n"
                + "                  <debitLimit>?</debitLimit>\n"
                + "                  <role>?</role>\n"
                + "                  <roleId>?</roleId>\n"
                + "               </limits>\n"
                + "               <role>Branch Supervisor</role>\n"
                + "               <roleId>144</roleId>\n"
                + "               <supervisor>Y</supervisor>\n"
                + "               <userId>2755</userId>\n"
                + "               <userName>STP002</userName>\n"
                + "               <userRoleId>31578</userRoleId>\n"
                + "            </userRole>\n"
                + "         </request>\n"
                + "      </api:postDepositToGLTransfer>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        userRequest = userRequest.replace("\n", "");

        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!soapResponse.equals("-1")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.trace("Final response xml... {}", sxml);
            //convert xml to java object
            xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);
        }
        if (xiResponse != null) {
            LOGGER.info("CBS Response - TransType:postDepositToGLTransfer, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
            status = xiResponse.getResult();
        } else {
            status = 96;
        }
        watch.stop();
        LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis(), txid);
        return status;
    }

    public int postGLToDepositTransfer(String suspenseGLAccount, String txid, String amount, String account, String currency, String narration) {
        LOGGER.info("Transafer amount from Gl Account to deposit account: {}", txid);
        int status = -1;
        XaResponse xiResponse = null;
        StopWatch watch = new StopWatch();
        watch.start();
        String userRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n"
                + "    <soapenv:Header/>\n"
                + "    <soapenv:Body>\n"
                + "        <api:postGLToDepositTransfer>\n"
                + "            <!--Optional:-->\n"
                + "            <request>\n"
                + "                <reference>" + txid + "</reference>\n"
                + "                <!--Optional:-->\n"
                + "                <debitAccount>" + suspenseGLAccount + "</debitAccount>\n"
                + "                <!--Optional:-->\n"
                + "                <creditAccount>" + account + "</creditAccount>\n"
                + "                <!--Optional:-->\n"
                + "                <currency>" + currency + "</currency>\n"
                + "                <!--Optional:-->\n"
                + "                <amount>" + new BigDecimal(amount) + "</amount>\n"
                + "                <!--Optional:-->\n"
                + "                <narration>" + narration + "</narration>\n"
                + "                <!--Optional:-->\n"
                + "               <userRole>\n"
                + "               <branchCode>060</branchCode>\n"
                + "               <branchId>-5</branchId>\n"
                + "               <branchName>IT AND OPERATIONS</branchName>\n"
                + "               <buRoleId>5559</buRoleId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <role>Branch Supervisor</role>\n"
                + "                    <!--Optional:-->\n"
                + "                    <roleId>144</roleId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <supervisor>Y</supervisor>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userId>2755</userId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userName>STP002</userName>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userRoleId>31578</userRoleId>\n"
                + "                </userRole>\n"
                + "            </request>\n"
                + "        </api:postGLToDepositTransfer>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        userRequest = userRequest.replace("\n", "");
        LOGGER.info("{}",userRequest);
        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!soapResponse.equals("-1")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.trace("Final response xml... {}", sxml);
            //convert xml to java object
            xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);
        }
        if (xiResponse != null) {
            LOGGER.info("CBS Response - TransType:postGLToDepositTransfer, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
            status = xiResponse.getResult();
        } else {
            status = 96;
        }
        watch.stop();
        LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis(), txid);
        return status;
    }



    public XaResponse postTransferPayment(String payor, String payee, String txid, BigDecimal amount, String currency, String narration) {
        LOGGER.info("Transafer amount to deposit account: {}", txid);
        LOGGER.info("Debit account: {}", payor);
        LOGGER.info("Credit account: {}", payee);
        LOGGER.info("amount: {}", amount);
        LOGGER.info("currency: {}", currency);
        LOGGER.info("narration: {}", narration);
        int status = -1;
        XaResponse xiResponse = null;
        StopWatch watch = new StopWatch();
        watch.start();
        String userRequest = ""
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <api:postTransferPayment>\n"
                + "         <!--Optional:-->\n"
                + "         <request>\n"
                + "            <reference>" + txid + "</reference>\n"
                + "            <!--Optional:-->\n"
                + "            <payorAccount>" + payor + "</payorAccount>\n"
                + "            <!--Optional:-->\n"
                + "            <payeeAccount>" + payee + "</payeeAccount>\n"
                + "            <!--Optional:-->\n"
                + "            <currency>" + currency + "</currency>\n"
                + "            <!--Optional:-->\n"
                + "            <amount>" + amount + "</amount>\n"
                + "            <!--Optional:-->\n"
                + "            <narration>" + narration + "</narration>\n"
                + "            <!--Optional:-->\n"
                + "               <userRole>\n"
                + "               <branchCode>060</branchCode>\n"
                + "               <branchId>-5</branchId>\n"
                + "               <branchName>IT AND OPERATIONS</branchName>\n"
                + "               <buRoleId>5559</buRoleId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <role>Branch Supervisor</role>\n"
                + "                    <!--Optional:-->\n"
                + "                    <roleId>144</roleId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <supervisor>Y</supervisor>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userId>2755</userId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userName>STP002</userName>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userRoleId>31578</userRoleId>\n"
                + "                </userRole>\n"
                + "         </request>\n"
                + "      </api:postTransferPayment>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        userRequest = userRequest.replace("\n", "");
        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!soapResponse.equals("-1")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.trace("Final response xml... {}", sxml);
            //convert xml to java object
            xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);
        }
        if (xiResponse != null) {
            LOGGER.info("CBS Response - TransType:postTransferPayment, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
            status = xiResponse.getResult();
        } else {
            status = 96;
            xiResponse = new XaResponse();
            xiResponse.setResult(96);
        }
        watch.stop();
        LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis(), txid);
        return xiResponse;
    }

    public int postGlToGLTransfer(String destinationGL, String reference, BigDecimal amount, String sourceGl, String currency, String narration) {
        LOGGER.info("Transafer amount from deposit Account to GL account: {}", reference);
        int status = -1;
        XaResponse xiResponse = null;
        StopWatch watch = new StopWatch();
        watch.start();
        String userRequest =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <api:postGLToGLTransfer>\n" +
                        "         <request>\n" +
                        "            <reference>" + reference + "</reference>\n" +
                        "            <debitAccount>" + sourceGl + "</debitAccount>\n" +
                        "            <creditAccount>" + destinationGL + "</creditAccount>\n" +
                        "            <currency>" + currency + "</currency>\n" +
                        "            <amount>" + amount + "</amount>\n" +
                        "            <narration>" + narration + "</narration>\n" +
                        "            <userRole>\n" +
                        "               <limits>\n" +
                        "                  <!--Zero or more repetitions:-->\n" +
                        "                  <limit>\n" +
                        "                     <creditLimit>?</creditLimit>\n" +
                        "                     <currency>?</currency>\n" +
                        "                     <debitLimit>?</debitLimit>\n" +
                        "                     <role>?</role>\n" +
                        "                     <roleId>?</roleId>\n" +
                        "                  </limit>\n" +
                        "               </limits>\n" +
                        "               <drawers>\n" +
                        "                  <!--Zero or more repetitions:-->\n" +
                        "                  <drawer>\n" +
                        "                     <currencies>\n" +
                        "                        <!--Zero or more repetitions:-->\n" +
                        "                        <currency>\n" +
                        "                           <code>?</code>\n" +
                        "                           <id>?</id>\n" +
                        "                           <name>?</name>\n" +
                        "                           <points>?</points>\n" +
                        "                        </currency>\n" +
                        "                     </currencies>\n" +
                        "                     <drawerAccount>?</drawerAccount>\n" +
                        "                     <drawerId>?</drawerId>\n" +
                        "                     <drawerNumber>?</drawerNumber>\n" +
                        "                     <openDate>?</openDate>\n" +
                        "                     <status>?</status>\n" +
                        "                  </drawer>\n" +
                        "               </drawers>\n" +
                        "               <branchCode>060</branchCode>\n" +
                        "               <branchId>-5</branchId>\n" +
                        "               <branchName>IT AND OPERATIONS</branchName>\n" +
                        "               <buRoleId>5559</buRoleId>\n" +
                        "               <role>Branch Supervisor</role>\n" +
                        "               <roleId>144</roleId>\n" +
                        "               <supervisor>Y</supervisor>\n" +
                        "               <userId>2755</userId>\n" +
                        "               <userName>STP002</userName>\n" +
                        "               <userRoleId>31578</userRoleId>\n" +
                        "            </userRole>\n" +
                        "         </request>\n" +
                        "      </api:postGLToGLTransfer>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

        userRequest = userRequest.replace("\n", "");

        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!soapResponse.equals("-1")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.trace("Final response xml... {}", sxml);
            //convert xml to java object
            xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);
        }
        if (xiResponse != null) {
            LOGGER.info("CBS Response - TransType:postGlToGLTransfer, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
            status = xiResponse.getResult();
        } else {
            status = 96;
        }
        watch.stop();
        LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis(), reference);
        return status;
    }


    public int postCharge(String debitAccount, String reference,String incomeLegder, BigDecimal amount, BigDecimal chargeAmount, String narration, String currency, String chargeScheme,String chargeCode,String branchNo) {
        LOGGER.info("Transafer:{} amount:{} from post charge Account to GL account: {}, chargeScheme:{}, chargeCode:{}",debitAccount,amount, reference,chargeScheme,chargeCode);

        int status = -1;
        try{
        XaResponse xiResponse = null;
        StopWatch watch = new StopWatch();
        watch.start();
        String userRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                "\t<soapenv:Header/>\n" +
                "\t<soapenv:Body>\n" +
                "\t\t<api:postCharge>\n" +
                "\t\t\t<request>\n" +
                "\t\t\t\t<reference>"+reference+"</reference>\n" +
                "\t\t\t\t<account>"+debitAccount+"</account>\n" +
                "\t\t\t\t<incomeLedger>"+incomeLegder+"</incomeLedger>\n" +
                "\t\t\t\t<currency>"+currency+"</currency>\n" +
                "\t\t\t\t<amount>"+amount+"</amount>\n" +
                "\t\t\t\t<charge>"+chargeAmount+"</charge>\n" +
                "\t\t\t\t<narration>"+narration+"</narration>\n" +
                "\t\t\t\t<scheme>"+chargeScheme+"</scheme>\n" +
                "\t\t\t\t<code>"+chargeCode+"</code>\n" +
                "\t\t\t\t<waivable>false</waivable>\n" +
                "\t\t\t\t<reversal>false</reversal>\n" +
                "\t\t\t\t<splitAccounts>\n" +
                "\t\t\t\t\t<entry>\n" +
                "\t\t\t\t\t\t<key>?</key>\n" +
                "\t\t\t\t\t\t<value>?</value>\n" +
                "\t\t\t\t\t</entry>\n" +
                "\t\t\t\t</splitAccounts>\n" +
                "\t\t\t\t<userRole>\n" +
                "\t\t\t\t\t<branchCode>"+branchNo+"</branchCode>\n" +
                "\t\t\t\t\t<branchId>"+branchNo+"</branchId>\n" +
                "\t\t\t\t\t<branchName>FINANCE</branchName>\n" +
                "\t\t\t\t\t<buRoleId>5559</buRoleId>\n" +
                "\t\t\t\t\t<role>Branch Supervisor</role>\n" +
                "\t\t\t\t\t<roleId>144</roleId>\n" +
                "\t\t\t\t\t<supervisor>Y</supervisor>\n" +
                "\t\t\t\t\t<userId>2755</userId>\n" +
                "\t\t\t\t\t<userName>STP002</userName>\n" +
                "\t\t\t\t\t<userRoleId>31578</userRoleId>\n" +
                "\t\t\t\t</userRole>\n" +
                "\t\t\t\t<force>false</force>\n" +
                "\t\t\t</request>\n" +
                "\t\t</api:postCharge>\n" +
                "\t</soapenv:Body>\n" +
                "</soapenv:Envelope>";

        userRequest = userRequest.replace("\n", "");
        userRequest = userRequest.replace("\t", "");

        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!soapResponse.equals("-1") && !soapResponse.equals("")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.trace("Final response xml... {}", sxml);
            //convert xml to java object
            xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);
        }
        if (xiResponse != null) {
            LOGGER.info("CBS Response - TransType:postGlToGLTransfer, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
            status = xiResponse.getResult();
        } else {
            status = 96;
        }
        watch.stop();
        LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis(), reference);
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
        }
        return status;
    }


    public static  String batchPost(String xmlBody) {
        int status = -1;
        try{
            XaResponse xiResponse = null;
            StopWatch watch = new StopWatch();
            watch.start();
            String userRequest =xmlBody;
            userRequest = userRequest.replace("\n", "");
            userRequest = userRequest.replace("\t", "");

            //send http request to wsdl server
            String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, "http://172.25.2.228:3800/philae/xws/xapi", "xapi", "x@pi#81*");

            if(true)  return soapResponse;

            if (!soapResponse.equals("-1") && !soapResponse.equals("")) {
                //parser soap xml to get clean xml
                XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
                //byte data to string xml
                String sxml = XMLParserService.xmlsrToString(xmlr);
                LOGGER.trace("Final response xml... {}", sxml);
                //convert xml to java object
                xiResponse = XMLParserService.jaxbXMLToObject(sxml, XaResponse.class);
            }
            if (xiResponse != null) {
                LOGGER.info("CBS Response - TransType:batchPost, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
                status = xiResponse.getResult();
            } else {
                status = 96;
            }
            watch.stop();
            LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis());
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
        }
        return "status";
    }

    public TaResponse accountBalance(String account, String txid) {
        LOGGER.info("accountBalance account: {} ->{}", account,txid);
        LOGGER.info("Debit account: {}", account);
        int status = -1;
        TaResponse xiResponse = null;
        StopWatch watch = new StopWatch();
        watch.start();
        String userRequest = ""
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <api:queryDepositBalance>\n"
                + "         <!--Optional:-->\n"
                + "         <request>" +
                "           <reference>"+txid+"</reference>\n" +
                "            <!--Optional:-->\n" +
                "            <account>"+account+"</account>"
                + "            <!--Optional:-->\n"
                + "               <userRole>\n"
                + "               <branchCode>060</branchCode>\n"
                + "               <branchId>-5</branchId>\n"
                + "               <branchName>IT AND OPERATIONS</branchName>\n"
                + "               <buRoleId>5559</buRoleId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <role>Branch Supervisor</role>\n"
                + "                    <!--Optional:-->\n"
                + "                    <roleId>144</roleId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <supervisor>Y</supervisor>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userId>2755</userId>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userName>STP002</userName>\n"
                + "                    <!--Optional:-->\n"
                + "                    <userRoleId>31578</userRoleId>\n"
                + "                </userRole>\n"
                + "         </request>\n"
                + "      </api:queryDepositBalance>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        userRequest = userRequest.replace("\n", "");
        userRequest = userRequest.replaceAll("\uFFFD", "");
        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(userRequest, this.systemVariable.CORE_BANKING_POSTING_URL, "xapi", "x@pi#81*");
        if (!soapResponse.equals("-1")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.trace("Final response xml... {}", sxml);
            //convert xml to java object
            xiResponse = XMLParserService.jaxbXMLToObject(sxml, TaResponse.class);
        }
        if (xiResponse != null) {
            LOGGER.info("CBS Response - TransType:queryDepositBalance, Message: " + xiResponse.getMessage() + ", Result: " + xiResponse.getResult() + ",  AvailableBalance: " + xiResponse.getAvailableBalance() + ", LedgerBalance: " + xiResponse.getLedgerBalance() + ", TxnId: " + xiResponse.getTxnId());
            status = xiResponse.getResult();
        } else {
            xiResponse = new TaResponse();
            xiResponse.setResult(91);
            status = 91;
        }
        watch.stop();
        LOGGER.info("{}ms Closing  CBS connection.... For TxId: {}", watch.getTotalTimeMillis(), txid);
        return xiResponse;
    }

}
