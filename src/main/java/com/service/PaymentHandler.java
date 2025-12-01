/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import jakarta.transaction.Transactional;

import com.config.SYSENV;
import com.helper.DateUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author melleji.mollel
 */
@Service
public class PaymentHandler {

    @Value("${spring.refundRetryUrl.gateway}")
    public String refundRetryUrl;
    @Value("${spring.refundRetryUrl.mkoba}")
    public String mkobaRefundUrl;
    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("mkobadb")
    JdbcTemplate jdbcMKOBATemplate;
    @Autowired
    XapiWebService xapiWebService;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PaymentHandler.class);

    @Autowired
    SYSENV env;

    public String processRefundRetryGW(String request, String txnid, String approvedBy, String approvedDate,String request2) {
        LOGGER.info("REFUND/RETRY REQUEST: {}", request);
        String responseCode = null;
        String status = "unknown";
        String isRetry = "no";
        String gatewayStatus = "-1";
        String retryRefundResponse = "-1";
        if (request.contains("<TrxStsCode>7101</TrxStsCode>")) {
            isRetry = "retry";
        }
//        if (request.contains("RECEPT") && request.contains("STASCODE")&& request.contains("TRANSID")) {
//            String url = "https://cilantro.tcbbank.co.tz:8443/api/mpesaCallback";
//            isRetry = "refund";
//            retryRefundResponse = HttpClientService.sendXMLRequest(request, url);
//        }
//        else if (request2.contains("RECEPT") && request2.contains("METER")&& request2.contains("EWURA") && request2.contains("TOKEN")){
//            String url ="http://172.20.1.26:8090/esb/payment/lukuCallback";
//            isRetry="refund";
//            retryRefundResponse = HttpClientService.sendXMLRequest(request2, url);
//        }

        retryRefundResponse = HttpClientService.sendXMLRequest(request, refundRetryUrl);



        if (!retryRefundResponse.equals("-1")) {
            //for LUKU recon
            if (retryRefundResponse.contains("gepgPmtPstReqAck") && retryRefundResponse.contains("TrxStsCode")) {
                responseCode = XMLParserService.getDomTagText("TrxStsCode", retryRefundResponse);
                if (responseCode.equalsIgnoreCase("7101")) {
                    status = "success";
                    gatewayStatus = "Success";
                    if (isRetry.equals("retry")) {
                        //will tall automatically bu downloadng
                        // updateSuspensReconTables(txnid, "retry");
                    } else {
                        //TODO eliatosha told me to commenct apa
//                        updateSuspensReconTables(txnid, "refund");
                    }
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                } else {
                    status = "Failed";
                    gatewayStatus = "Failed:" + responseCode;
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                }
            }else if(retryRefundResponse.contains("lukuCallback") && retryRefundResponse.contains("responseCode")&& retryRefundResponse.contains("message")){
                //<lukuCallback><responseCode>0</responseCode><message>Successfully Reversed</message></lukuCallback>
                responseCode = XMLParserService.getDomTagText("responseCode", retryRefundResponse);
                if (responseCode.equalsIgnoreCase("0")) {
                    status = "success";
                    gatewayStatus = "Success";
//                    updateSuspensReconTables(txnid, "refund");
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                }
            }else  if (retryRefundResponse.contains("MPESA") && retryRefundResponse.contains("RESPONSECODE")&& retryRefundResponse.contains("MESSAGE")) {
                responseCode = XMLParserService.getDomTagText("RESPONSECODE", retryRefundResponse);
                if (responseCode.equalsIgnoreCase("0")) {
                    status = "success";
                    gatewayStatus = "Success";
//                    if (isRetry.equals("refund"))  {
//                        updateSuspensReconTables(txnid, "refund");
//                    }
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                } else {
                    status = "Failed";
                    gatewayStatus = "Failed:" + responseCode;
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                }
            } else {
                responseCode = XMLParserService.getDomTagText("responsecode", retryRefundResponse);
                //update the retry table
                if (responseCode.equalsIgnoreCase("0")) {
                    status = "success";
                    gatewayStatus = "Success";
                    if (request.contains("B2C_REVERSAL")) {
                        //the transaction is refunded to customer account. make the transaction as reversed on cbs transaction table
                        updateReconTables(txnid, "refund");
                    }
                    if (request.contains("CBS_RETRY")) {
                        //the transaction is retried  to customer account. make the transaction as successfully reprocessed thirdpartytxns table
                        updateReconTables(txnid, "retry");
                    }
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                } else {
                    status = "Failed";
                    gatewayStatus = "Failed";
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                }
            }
        } else {
            status = "Timout please Try again";
            gatewayStatus = "Timeout";
            updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);

        }
        return status;

    }

    public String processRefundMkoba(String request, String txnid, String approvedBy, String approvedDate) {
        System.out.println("MKOBA REFUND REQUEST: " + request);
        String responseCode = null;
        String status = "unknown";
        String gatewayStatus = "-1";
        /* List<Map<String, Object>> getTxnTypeMK = this.jdbcMKOBATemplate.queryForList("select * from vg_group_transaction where transid =? limit 1", txnid);
        if (!getTxnTypeMK.isEmpty()) {
            String retryRefundResponse = HttpClientService.sendXMLRequest(request, mkobaRefundUrl);
            if (retryRefundResponse.contains("ThirdPartyresultType") && retryRefundResponse.contains("Reversed")) {
                LOGGER.info("Txnid refund:->{} ==> response:-> {}", txnid, retryRefundResponse);

                status = "success";
                gatewayStatus = "Success";
                //the transaction is refunded to customer account. make the transaction as reversed on cbs transaction table
                updateReconTables(txnid, "refund");
                updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);

            } else {
                status = "Timout please Try again";
                gatewayStatus = "Timeout";
                // updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);

            }
        } else */
        {
            List<Map<String, Object>> cbsTrans = this.jdbcTemplate.queryForList("select * from cbstransactiosn where txnid =?", txnid);
            if (!cbsTrans.get(0).get("sourceaccount").equals("-1")) {
                String suspenseGLAccount = cbsTrans.get(0).get("contraaccount") + "";
                String txid = txnid;
                String amountCr = cbsTrans.get(0).get("amount") + "";
                String account = cbsTrans.get(0).get("sourceaccount") + "";
                String currency = cbsTrans.get(0).get("currency") + "";
                String narration = "REV~" + cbsTrans.get(0).get("description");
                int cbsResult = xapiWebService.postGLToDepositTransfer(suspenseGLAccount, txid, amountCr, account, currency, narration);
                LOGGER.info("cbsResult for {} is {}", txid, cbsResult);
                if (cbsResult == 0) {
                    LOGGER.info("Txnid refund:->{} ==> response:-> {}", txnid, cbsResult);
                    status = "success";
                    gatewayStatus = "Success";
                    //the transaction is refunded to customer account. make the transaction as reversed on cbs transaction table
                    updateReconTables(txnid, "refund");
                    updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
                    int resultId = updateVgGroupTransaction(txnid, "Transaction reversed by "+approvedBy);
                    LOGGER.info("Mkoba update result:{}",resultId);



                }
            }
        }

        return status;

    }

    public String processRetryMkoba(String request, String txnid, String approvedBy, String approvedDate) {
        System.out.println("MKOBA RETRY REQUEST: " + request);
        String responseCode = null;
        String status = "unknown";
        String gatewayStatus = "-1";
        String retryRefundResponse = HttpClientService.sendXMLRequest(request, mkobaRefundUrl);
        if (retryRefundResponse.contains("sessionid") && retryRefundResponse.contains("responsecode")) {
            int responsecode = parserMkobaResponse(retryRefundResponse);
            LOGGER.info("Txnid retried:->{} ==> response:-> {}", txnid, responsecode);

            if (responsecode == 0) {
                status = "success";
                gatewayStatus = "Success";
                //the transaction is refunded to customer account. make the transaction as reversed on cbs transaction table
                updateReconTables(txnid, "retried");
                updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);
            }
        } else {
            status = "Timout please Try again";
            gatewayStatus = "Timeout";
            // updateRetryTxns(txnid, status, approvedBy, approvedBy, approvedDate, gatewayStatus, responseCode);

        }
        return status;

    }

    static int parserMkobaResponse(String XMLData) {
        int ResponseCode = -1;

        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(XMLData));

            Element classElement = document.getRootElement();

            List<Element> ResponseList = classElement.getChildren();

            for (int temp = 0; temp < ResponseList.size(); temp++) {
                Element Response = (Element) ResponseList.get(temp);
                String Name = Response.getName();

                if (Name.equals("sessionid")) {

                    String textValue = Response.getContent(0).getValue();
                    System.out.println("SessionID " + textValue);
                }

                if (Name.equals("responsecode")) {
                    String textValue = Response.getContent(0).getValue();
                    ResponseCode = Integer.valueOf(textValue).intValue();
                    System.out.println("responsecode " + textValue);
                }

            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return ResponseCode;
    }

    @Async("dbPoolExecutor")
    @Transactional
    public CompletableFuture<Integer> updateRetryTxns(String txnid, String status, String checkerID, String approverID, String approvedDate, String gatewayStatus, String gwResponseCode) {
        Integer result, result1 = 0;
        result = jdbcTemplate.update("update retry set status=?,approver_id=?,approver_date=?,gatewayStatus=?,gwResponseCode=?,approver_comments='Approved',modified=? where txnid=?", status, approverID, approvedDate, gatewayStatus, gwResponseCode, approvedDate, txnid);
//
        return CompletableFuture.completedFuture(result);
    }

    @Async("dbPoolExecutor")
    @Transactional
    public CompletableFuture<Integer> updateReconTables(String txnid, String indicator) {
        Integer result = 0;
        Integer result1 = 0;
        if (indicator.equalsIgnoreCase("refund")) {
            result = jdbcTemplate.update("update cbstransactiosn set txn_status='Refunded to customer' where txnid=?", txnid);

        }
        if (indicator.equalsIgnoreCase("retry")) {
            result1 = jdbcTemplate.update("update thirdpartytxns set mnoTxns_status='successfully Reprocessed' where txnid=?", txnid);

        }
//
        return CompletableFuture.completedFuture(result);
    }

    @Async("dbPoolExecutor")
    @Transactional
    public CompletableFuture<Integer> updateSuspensReconTables(String txnid, String indicator) {
        Integer result = 0;
        Integer result1 = 0;
        if (indicator.equalsIgnoreCase("refund")) {
            result = jdbcTemplate.update("update suspe_cbstransactiosn set txn_status='Refunded to customer' where txnid=?", txnid);

        }
        if (indicator.equalsIgnoreCase("retry")) {
            result = jdbcTemplate.update("update suspe_cbstransactiosn set txn_status='retried' where txnid=?", txnid);

        }
        return CompletableFuture.completedFuture(result);
    }


    public int updateVgGroupTransaction(String txnid,String message) {
        Integer result = -1;
        try {
            result = jdbcMKOBATemplate.update("update vg_group_transaction set transstatus='-99',description=? where transid=?",message, txnid);
        }catch (DataAccessException ex) {
            LOGGER.info("updateVgGroupTransaction:->", ex);
        }
        return result;
    }

    public String retryAirtelVikobaTxn(String request,String requestI, String approverId, String approvalDate) {
        String url = env.VIKOBA_OPERATIONS_URL;
        String response = HttpClientService.sendXMLRequest(request,url);
//        LOGGER.info("AIRTEL VIKOBA retry response ... {}",response);
        String gatewayStatus= "-1";
        String status= "-1";
            String responseCode = XMLParserService.getDomTagText("responseCode", response);
            String txnid = XMLParserService.getDomTagText("ThirdPartyReference", request);
            if(responseCode.equalsIgnoreCase("0")){
                gatewayStatus = "success";
                status = "success";
            }else{
                gatewayStatus = "failed";
                status = "fail";
            }
              updateRetryTxns(txnid, status, approverId, approverId, approvalDate, gatewayStatus, responseCode);
            LOGGER.info("update response... {}, txnid..{}",updateRetryTxns(txnid, status, approverId, approverId, approvalDate, gatewayStatus, responseCode),txnid);

        return response;
    }
}
