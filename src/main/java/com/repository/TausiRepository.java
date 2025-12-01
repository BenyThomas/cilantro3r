package com.repository;

import com.DTO.GeneralJsonResponse;
import com.DTO.GepGPaymentV2;
import com.DTO.ReplayIncomingTransactionReq;
import com.DTO.ResponseFromKipayment;
import com.DTO.tausi.AgentTransactionSettlement;
import com.DTO.tausi.GePGLookupV2;
import com.DTO.tausi.GePGLookupV2Response;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.queue.QueueProducer;
import com.service.CorebankingService;
import com.service.HttpClientService;
import com.service.XMLParserService;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import philae.api.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class TausiRepository {
    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TausiRepository.class);
    @Autowired
    QueueProducer queProducer;

    @Autowired
    SYSENV DBENV;

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;

    public List<Map<String, Object>> getLoanModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> finalRes = null;
        try {
            finalRes = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING Tausi Permissions : {}", e.getMessage());
        }
        return finalRes;
    }


    public int mapTausiAgent(String agentAccount, String agentAccountName, String status, String agentCode, String agentName, String agentReference, String description, String requestTime, String transactionId) {
        String sql = "INSERT INTO tausi_agent_mapping(agentAccount, accountName, status, agentCode, agentReference, agentName, description, requestTime, transactionId, createdBy, createdDate) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LOGGER.info(sql.replace("?", "'{}'"), agentAccount, agentAccountName, status, agentCode, agentReference, agentName, description, requestTime, transactionId, "created by system", DateUtil.now());
        int finalResp = 0;
        try {
            finalResp = jdbcTemplate.update(sql, agentAccount, agentAccountName, status, agentCode, agentReference, agentName, description, requestTime, transactionId, "created by system", DateUtil.now());
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return finalResp;
    }


    public int checkIfAgentMapped(String agentCode, String agentReference, String agentAccount) {
        int exist = -1;
        try {
            exist = jdbcTemplate.queryForObject("SELECT count(*) FROM tausi_agent_mapping WHERE agentCode=? and agentReference=? and agentAccount=? ", new Object[]{agentCode, agentReference, agentAccount}, Integer.class);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return exist;
    }

    public String getTausiMappedAccountsAjax(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM tausi_agent_mapping";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE  concat(agentAccount,' ',accountName,' ',status,' ',recStatus,' ',agentCode,' ',agentReference,' ',agentName,' ',createdBy,' ',notified) LIKE ? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM tausi_agent_mapping  " + searchQuery, new Object[]{searchValue}, Integer.class);
                mainSql = "SELECT * FROM tausi_agent_mapping  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT * FROM tausi_agent_mapping  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    public Map<String, Object> getAgentDeta(String agentCode, String agentReference) {
        Map<String, Object> result = null;
        try {
            String qwele = "SELECT * FROM tausi_agent_mapping WHERE agentCode=? AND agentReference=? ";
            result = jdbcTemplate.queryForMap(qwele, agentCode, agentReference);
            LOGGER.info("agent details ... {}", result);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return result;
    }

    public int updateAgentAcctAfterNotify(String agentCode, String agentReference, String transactionId, String tausiId) {
        String sql = "UPDATE tausi_agent_mapping SET notified=1, notifiedBy=?,notifiedDate=?, recStatus='C', comments=? WHERE agentCode=? AND agentReference=? AND transactionId=?";
        int finalResp = 0;
        try {
            finalResp = jdbcTemplate.update(sql, "SYSTEM", DateUtil.now(), tausiId, agentCode, agentReference, transactionId);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return finalResp;
    }

    public int initiageAgentTransaction(String agentCode, String agentReference,String agentName,String txnReference, String sourceAcct, String agentAccount, String amount, String initiatedBy, String branchNo) {
        String sql = "INSERT INTO tausi_agent_transactions(agentCode, agentReference,agentName, amount, currency, sourceAcct, destinationAcct, reference, status, comments, initiated_by, branch_no, cbs_status, callbackurl)" +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
        int finalResp = 0;
        try {
            finalResp = jdbcTemplate.update(sql, agentCode, agentReference,agentName, amount, "TZS", sourceAcct, agentAccount, txnReference, "L", "Transaction Logged awaiting approval", initiatedBy, branchNo, "L", "-1");
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return finalResp;
    }

    public List<Map<String, Object>> branches(String branchCode) {
        List<Map<String, Object>> result = null;
        try {
            if (branchCode.equalsIgnoreCase("060")) {
                String sql = "SELECT * from branches";
                result = this.jdbcTemplate.queryForList(sql);
            } else {
                String sql = "SELECT * from branches where code=?";
                result = this.jdbcTemplate.queryForList(sql, branchCode);
            }
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
        return result;
    }

    public List<Map<String, Object>> getAgentTransactions(Map<String, String> customeQuery) {
        String mainSql;
        List<Map<String, Object>> results = null;
        String branchCode = customeQuery.get("branchCode");
        String fromDate = customeQuery.get("fromDate");
        String toDate = customeQuery.get("toDate");
        String status = customeQuery.get("status");
        switch (branchCode) {
            case "ALL":
                //Select on all branches
                switch (status) {
                    case "INITIATED":
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='L' and c.cbs_status='L' and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, fromDate, toDate);
                        break;
                    case "SUCCESS":
                        //where b.status='C'
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='C' and c.cbs_status='C' and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, fromDate, toDate);
                        break;
                    case "FAILED":
                        //cbs status ='F'?
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='F' and c.cbs_status='F' and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, fromDate, toDate);
                        break;
                    case "APPROVED_NOT_NOTIFIED":
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='C' and c.cbs_status='C' and notified='0' and c.create_dt>=? and c.create_dt<=?";
                        String fromDt = DateUtil.previosDay(1, fromDate);
                        String toDt = DateUtil.previosDay(1, fromDate);
                        results = this.jdbcTemplate.queryForList(mainSql, fromDate, toDate);
                        break;
                }
                break;

            default:
                switch (status) {
                    //"INITIATED" "SUCCESS" "FAILED"
                    case "INITIATED":
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='L' and c.cbs_status='L' and b.code=? and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, branchCode, fromDate, toDate);
                        break;
                    case "SUCCESS":
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='C' and c.cbs_status='C' and b.code=? and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, branchCode, fromDate, toDate);
                        break;
                    case "FAILED":
                        mainSql = "select c.*,b.name as branch_name from tausi_agent_transactions c left join branches b on b.code = c.branch_no where c.status='F' and c.cbs_status='F' and b.code=? and c.create_dt>=? and c.create_dt<=?";

                        results = this.jdbcTemplate.queryForList(mainSql, branchCode, fromDate, toDate);
                        break;
                }

                break;
        }
        return results;
    }

    public Map<String, Object> getTransaction(String agentCode, String agentReference, String transRef) {
        Map<String, Object> result = null;
        try {
            String sql = "SELECT * FROM tausi_agent_transactions WHERE agentCode=? AND agentReference=? AND reference=?";
            result = jdbcTemplate.queryForMap(sql, agentCode, agentReference, transRef);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return result;
    }

    public String processTausiAgentDepositTOCBS(String agentCode, String agentReference, String transRef, String aprover, UsRole role) {
        String result = "{\"result\":\"1\",\"message\":\"Transaction is being Processed please confirm on RUBIKON\"}";


        Map<String, Object> txn = getTransaction(agentCode, agentReference, transRef);
        String message1;
        String responseCode;
        String identifier = "api:postGLToDepositTransfer";
        if (txn != null) {
            TxRequest transferReq = new TxRequest();

            transferReq.setReference(transRef);
            transferReq.setAmount(new BigDecimal(txn.get("amount").toString()));
            transferReq.setNarration("Agent deposit amount " + " " + txn.get("amount") + " B/O " + txn.get("destinatinationAcct") + " " + txn.get("reference") + " FROM: " + txn.get("sourceAcct"));
            transferReq.setCurrency((String) txn.get("currency"));
            transferReq.setDebitAccount((String) txn.get("sourceAcct"));
            transferReq.setCreditAccount((String) txn.get("destinationAcct"));
            transferReq.setUserRole(role);
            PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
            postOutwardReq.setRequest(transferReq);
            String txnRequest = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
            //process the Request to CBS

            XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(txnRequest, identifier), XaResponse.class);
            String approveDate = null;

            approveDate = DateUtil.now();
            String sql = "-1";
            if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                sql = "update tausi_agent_transactions set status = 'C', cbs_status='C',comments='Completed Successfully',branch_approved_by=?,branch_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
                LOGGER.info(sql.replace("?", "'{}'"), aprover, approveDate, transRef, agentCode, agentReference);
                jdbcTemplate.update(sql, aprover, approveDate, transRef, agentCode, agentReference);
                result = "{\"result\":\"200\",\"message\":" + cbsResponse.getMessage() + "}";
            } else {
                sql = "update tausi_agent_transactions set comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
                LOGGER.info(sql.replace("?", "'{}'"), cbsResponse.getMessage(), aprover, approveDate, transRef, agentCode, agentReference);
                jdbcTemplate.update(sql, cbsResponse.getMessage(), aprover, approveDate, transRef, agentCode, agentReference);
                result = "{\"result\":" + cbsResponse.getResult() + ",\"message\":" + cbsResponse.getMessage() + "}";
            }

        }


        return result;
    }
    public String tellerPostTausiAgentDepositTOCBS(String agentCode, String agentReference, String agentAccount,String amount,String branchNo, String inititator, UsRole role,String depositorName) {
        String result = "{\"status\":\"1\",\"message\":\"Transaction is being Processed please confirm on RUBIKON\"}";

        Map<String,Object> agentData = getAgentDeta(agentCode,agentReference);

        String transRef = "TAF" + DateUtil.now("yyyyMMddhhMMss");
        int saveTransaction = initiageAgentTransaction(agentCode,agentReference, (String) agentData.get("accountName"),transRef,"DRAWER" , agentAccount ,amount, inititator, branchNo);

       if(1==saveTransaction){
           String identifier = "api:postCashPayment";

//            TODO Change has been made to use teller drawer
               CpRequest transferReq = new CpRequest();

               transferReq.setReference(transRef);
               transferReq.setAmount(new BigDecimal(amount));
               transferReq.setNarration("Tausi Agent deposit amount " + " " + amount + " B/O " + agentAccount + "  reference" + transRef +" by " + depositorName);
               transferReq.setCurrency("TZS");
               transferReq.setAccount(agentAccount);
               transferReq.setUserRole(role);
               transferReq.setDrawer(role.getDrawers().getDrawer().size()>0?role.getDrawers().getDrawer().get(0):null);
//            for(TlDrawer drawer :role.getDrawers().getDrawer()){
//                for(CnCurrency currency :drawer.getCurrencies().getCurrency()){
//                }
//            }
               PostCashPayment cashRequest = new PostCashPayment();
               cashRequest.setRequest(transferReq);
               String txnRequest = XMLParserService.jaxbGenericObjToXML(cashRequest, Boolean.FALSE, Boolean.TRUE);
               //process the Request to CBS

               XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCoreTestCM(txnRequest, identifier), XaResponse.class);
               String approveDate = null;

               approveDate = DateUtil.now();
               String sql = "-1";
               if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                   sql = "update tausi_agent_transactions set status = 'C', cbs_status='C',comments='Completed Successfully',branch_approved_by=?,branch_approved_dt=?,hq_approved_by='SYSTEM',hq_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
                   LOGGER.info(sql.replace("?", "'{}'"),inititator, approveDate,approveDate, transRef, agentCode, agentReference);
                   jdbcTemplate.update(sql, inititator, approveDate,approveDate, transRef, agentCode, agentReference);
                   result = "{\"status\":\"200\",\"message\":\"" + cbsResponse.getMessage() + "\"}";
               } else {
                   sql = "update tausi_agent_transactions set comments=?,branch_approved_by=?,branch_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
                   LOGGER.info(sql.replace("?", "'{}'"), cbsResponse.getMessage(), inititator, approveDate, transRef, agentCode, agentReference);
                   jdbcTemplate.update(sql, cbsResponse.getMessage(), inititator, approveDate, transRef, agentCode, agentReference);
                   result = "{\"status\":\"" + cbsResponse.getResult() + "\",\"message\":\"" + cbsResponse.getMessage() + "\"}";
               }

       }else{
           result = "{\"status\":99,\"message\":\"Failed to save transaction into database.\"}";
       }
        return result;
    }


    public int updateAgentTransactionAfterNotify(String agentCode, String agentReference, String transRef, int statusCode, String description, String notifier) {
        String sql = "UPDATE tausi_agent_transactions SET hq_approved_by=?, hq_approved_dt=?, notified_by=?, notify_date=?,message=?, notified=1 WHERE agentCode=? AND agentReference=? AND reference=?";
        int finalResp = 0;
        try {
            finalResp = jdbcTemplate.update(sql, notifier, DateUtil.now(), notifier, DateUtil.now(), "resp:[ " + statusCode + " , " + description + " ]", agentCode, agentReference, transRef);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return finalResp;
    }


    public String getAgentAccount(String agentCode, String agentReference) {
        String result = null;
        try {
            String qwele = "SELECT agentAccount FROM tausi_agent_mapping WHERE agentCode=? AND agentReference=? limit 1";
            result = jdbcTemplate.queryForObject(qwele, new Object[]{agentCode, agentReference}, String.class);
            LOGGER.info("agent account ... {}", result);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return result;
    }

    public List<Map<String, Object>> fireGetAgentTransactionsForSettlementAjax(String status, String fromDate, String toDate) {
        List<Map<String, Object>> results = null;
        String mainSql = "SELECT * FROM tausi_agent_transactions where dr_cr_ind='DR' and status=? and cbs_status=? and create_dt>=? and create_dt<=?";
        try {
            results = jdbcTemplate.queryForList(mainSql, status, status, fromDate, toDate);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return results;

    }

    public GePGLookupV2Response validateControlNoFromGePG(String controlNo, String reference) {
        String spCode = "-1";
        String currency = "TZS";
        String SpName = "NILL";
        String BillPayOpt = "NILL";

        GePGLookupV2Response lookupV2Response = new GePGLookupV2Response();

        GePGLookupV2 lookupV2 = new GePGLookupV2();
        lookupV2.setRequestId(DateUtil.now("yyyyMMddhhHHss"));
        lookupV2.setControlNo(controlNo);

        String xmlResponse = HttpClientService.sendJsonRequest(lookupV2.toString(), DBENV.GePG_V2_KIPAYMENT_LOOKUP_URL);

        if (!xmlResponse.equalsIgnoreCase("-1")) {

            String responseCode = XMLParserService.getDomTagText("BillStsCode", xmlResponse);
            if (responseCode.equalsIgnoreCase("7101") || responseCode.equalsIgnoreCase("0")) {
                spCode = XMLParserService.getDomTagText("SpCode", xmlResponse);
                currency = XMLParserService.getDomTagText("CCy", xmlResponse);
                SpName = XMLParserService.getDomTagText("SpName", xmlResponse);
                BillPayOpt = XMLParserService.getDomTagText("BillPayOpt", xmlResponse);



                Map<String, Object> verifyEgaPartner = findEgaPartnerBySpCode(spCode, currency);

                lookupV2Response.setSpCode(spCode);
                lookupV2Response.setSpName(SpName);
                lookupV2Response.setBillPayOption(BillPayOpt);
                lookupV2Response.setEgaPartnerName(verifyEgaPartner.get("partner_name").toString());
                lookupV2Response.setEgaPartnerSpCode(verifyEgaPartner.get("partner_code").toString());
                lookupV2Response.setEgaPartnerAcctNo(verifyEgaPartner.get("acct_no").toString());

                jdbcTemplate.update("update tausi_agent_transactions set controlNoResponse=?, destinationAcct=?, spCode=? where reference=?", xmlResponse,verifyEgaPartner.get("acct_no"), spCode, reference);

            } else {
                lookupV2Response.setSpCode("INVALID");
                lookupV2Response.setSpName("INVALID");
                lookupV2Response.setBillPayOption("NOT KNOWN");
                lookupV2Response.setEgaPartnerName("INVALID");
                lookupV2Response.setEgaPartnerSpCode("INVALID");
                lookupV2Response.setEgaPartnerAcctNo("INVALID");
                jdbcTemplate.update("update tausi_agent_transactions set controlNoResponse=?, spCode=? where reference=?", xmlResponse, spCode, reference);

            }
        } else {
            lookupV2Response.setSpCode("-1");
            lookupV2Response.setSpName("-1");
            lookupV2Response.setBillPayOption("-1");
            lookupV2Response.setEgaPartnerName("-1");
            lookupV2Response.setEgaPartnerSpCode("-1");
            lookupV2Response.setEgaPartnerAcctNo("-1");
            jdbcTemplate.update("update tausi_agent_transactions set controlNoResponse=?, spCode=? where reference=?", xmlResponse, spCode, reference);

        }

        LOGGER.info("check response ... {}",lookupV2Response);
        return lookupV2Response;
    }

    public Map<String, Object> findEgaPartnerBySpCode(String spCode, String currency) {
        Map<String, Object> item = null;
        try {
            String sql = "select TOP 1 acct_no,partner_name,currency,partner_status,partner_code,acct_no from ega_partners  where partner_status ='A' AND partner_code=? AND currency=?";
            LOGGER.info(sql.replace("?", "'{}'"), spCode, currency);
            item = jdbcPartnersTemplate.queryForMap(sql, spCode, currency);
        } catch (Exception e) {
            LOGGER.info(null, e);
        }
        return item;

    }


    public String processTausiAgentTransactionForSettlement(String agentCode, String agentReference, String transRef, String aprover, UsRole role) throws JsonProcessingException {
        String result = "{\"result\":\"202\",\"message\":\"Transaction is being Processed please confirm on RUBIKON\"}";


        Map<String, Object> txn = getTransaction(agentCode, agentReference, transRef);
        String message1;
        String responseCode;
        String identifier = "api:postDepositToGLTransfer";
        if (txn != null) {

            //consume api ya gepg for payment.
            LOGGER.info("7101: proc gepg, we have an account internally so the transaction will be executed within tpb");
            String request = "<request>"
                    + "<COMMAND>BillPayment</COMMAND>"
                    + "<CONTROL_NO>" + txn.get("controlNumber") + "</CONTROL_NO>"
                    + "<ID>" + txn.get("reference") + "</ID>"
                    + "<CUSTACCOUNT>" + txn.get("sourceAcct") + "</CUSTACCOUNT>"
                    + "<CURRENCY>" +txn.get("currency") + "</CURRENCY>"
                    + "<AMOUNT>" + txn.get("amount")+ "</AMOUNT>"
                    + "<MOBILE></MOBILE>"
                    + "</request>";
            request = request.replace("null", "");
            request = request.replace("&", "and");
            request = request.replace("'", "");
            request = request.replaceAll("\uFFFD", "");

            String response = HttpClientService.sendXMLRequest(request, DBENV.GePG_KIPAYMENT_VALIDATE_CONTROL_NO_URL);
            if (response.contains("RESPONSE")) {
                /*<RESPONSE><CODE>96</CODE>
                    <MESSAGE>Invalid control</MESSAGE>
                    <AVAILABLEBALANCE>0.0</AVAILABLEBALANCE>
                    </RESPONSE>
                 */
                int code = Integer.parseInt(XMLParserService.getDomTagText("CODE", response));
                if (code == 0) {
                   String sql = "update tausi_agent_transactions set status = 'C', cbs_status='C',comments='Completed Successfully both cbs and gepg', notified='1',branch_approved_by=?,branch_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
                   String approveDate = DateUtil.now();
                    LOGGER.info(sql.replace("?", "'{}'"), aprover, approveDate, transRef, agentCode, agentReference);
                    jdbcTemplate.update(sql, aprover, approveDate, transRef, agentCode, agentReference);
                    result = "{\"result\":\"200\",\"message\":\"Transaction completed both GePG and Rubikon, Confirm on Rubikon\"}";
                }else{
                    String sql = "update tausi_agent_transactions set status = 'F', cbs_status='F',comments='Failed payment on gepg', notified='0',branch_approved_by=?,branch_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
                    String approveDate = DateUtil.now();
                    LOGGER.info(sql.replace("?", "'{}'"), aprover, approveDate, transRef, agentCode, agentReference);
                    jdbcTemplate.update(sql, aprover, approveDate, transRef, agentCode, agentReference);
                    result = "{\"result\":\"201\",\"message\":\"Failed payment on gepg code"+code+"\"}";
                    LOGGER.info("error: {}",result);

                }
            }else {
                result = "{\"result\":\"201\",\"message\":\"Failed payment on gepg \"}";
                LOGGER.info("error: {}",result);
            }
        }else{
            LOGGER.info("Failed to retrieve transaction from tausi agent transactions.");
            result = "{\"result\":\"201\",\"message\":\"Failed to gent details from ega partners.\"}";
            LOGGER.info("error: {}",result);

        }

        return result;
    }

    private String insertTransactionToEgaTransactions(String controlNo,String beneficiaryName,String BillPayOpt, TxRequest req, Map<String, Object> txn) {

            String sql = "EXEC ega_log_Echannels_transaction ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
            LOGGER.info(sql.replace("?", "'{}'"),controlNo, req.getAmount(), "TAPBTZTZ", req.getCreditAccount(), beneficiaryName, txn.get("agentName"), "", "", BillPayOpt, "", "1", req.getCurrency(), "NILL", req.getNarration(), "CASH", req.getReference(), req.getReference(), req.getNarration(), "-1", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "L", "-1", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "L", req.getReference(), 0.0);
            List<Map<String, Object>> data = jdbcPartnersTemplate.queryForList(sql, controlNo, req.getAmount(), "TAPBTZTZ", req.getCreditAccount(), beneficiaryName, txn.get("agentName"), "", "", BillPayOpt, "", "1", req.getCurrency(), "NILL", req.getNarration(), "CASH", req.getReference(), req.getReference(), req.getNarration(), "-1", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "L", "-1", DateUtil.now("yyyy-MM-dd HH:mm:ss"), "L", req.getReference(), 0.0);
            LOGGER.info("RESULT FROM INSERT INTO EGA TRANSACTIONS:{}", data);
            String reference = "-1";
            if (!data.isEmpty()) {
                reference = data.get(0).get("txnid").toString();
            }
            return reference;

    }

    public int flagMispostedAgentTransaction(String agentCode,String aprover, String agentReference, String transRef) {
        int result =0;
        try{
            String sql = "update tausi_agent_transactions set status = 'F', cbs_status='R',comments='Transaction Cancelled',hq_approved_by=?,hq_approved_dt=?  where  reference=? and agentCode =? and agentReference=?";
            LOGGER.info(sql.replace("?", "'{}'"), aprover, DateUtil.now(), transRef, agentCode, agentReference);
           result = jdbcTemplate.update(sql, aprover, DateUtil.now(), transRef, agentCode, agentReference);
        }catch (Exception e){
            LOGGER.info(null,e);
        }

        return  result;
    }
}
