package com.repository.tips;

import com.DTO.tips.ConfirmBotRoot;
import com.DTO.tips.ConfirmTransaction;
import com.DTO.tips.LookUpRequest;

import com.DTO.tips.TipsPaymentFullObject;
import com.DTO.tips.TipsPaymentRequest;
import com.DTO.tips.TipsPaymentResponse;
import com.DTO.tips.UntrackedTipsTxn;
import com.config.SYSENV;
import com.controller.itax.CMSpartners.JsonResponse;
import com.controller.tips.FraudRegistrationForm;
import com.controller.tips.TIPSTransferForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.queue.QueueProducer;
import com.repository.BanksRepo;
import com.repository.EftRepo;
import com.service.CorebankingService;
import com.service.HttpClientService;
import com.service.XMLParserService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import philae.api.PostDepositToGLTransfer;
import philae.api.TxRequest;
import philae.api.UsRole;
import philae.api.XaResponse;

import java.math.BigDecimal;
import java.sql.ResultSet;

import java.util.List;
import java.util.Map;

@Repository
public class TipsRepository {
    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    QueueProducer queProducer;

    @Autowired
    SYSENV systemVariable;

    @Autowired
    CorebankingService corebanking;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;

    @Autowired
    BanksRepo banksRepo;

    @Autowired
    CorebankingService coreBankingService;

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    @Qualifier("gwBrinjalDbConnection")
    JdbcTemplate jdbcBrinjalTemplate;

    @Autowired
    private Environment env;

    @Autowired
    SignRequest sign;

    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;
    //
//    @Autowired
//    TipsPaymentRequest tipsPaymentRequest;
//
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EftRepo.class);

    public List<Map<String, Object>> getTIPSModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    public String getAccountDetails(String accountNo, String institutionCategory, String institutionCode) {
        String lookupUrl = systemVariable.TIPS_LOOKUP_URL;
        String finalResponse = null;

        LookUpRequest lookUpRequest = new LookUpRequest();
        lookUpRequest.setCurrency("TZS");
        lookUpRequest.setAccountNo(accountNo);
        lookUpRequest.setInstitutionCode(institutionCode);
        lookUpRequest.setInstitutionCategory(institutionCategory);
        lookUpRequest.setReference(DateUtil.now("yyyyMMddHHmmssSSS"));

        try {
            LOGGER.info("TIPS LOOKUP for Querying beneficiary account details: ...{}", lookUpRequest.toString());
            finalResponse = HttpClientService.sendTipsXMLRequest(lookUpRequest.toString(), lookupUrl);

            JSONObject jsonObject = new JSONObject(finalResponse);

            if (!jsonObject.getString("responseCode").equalsIgnoreCase("0")) {
                finalResponse = "{\"responseCode\":\"" + jsonObject.getString("responseCode") + "\",\"message\":\"" + jsonObject.getString("message") + "\"}";
            }

            LOGGER.info("Final response for tips lookup , responseCode ....{} and message ... {}", jsonObject.getString("responseCode"), jsonObject.getString("message"));
        } catch (Exception e) {
            LOGGER.info("EXCEPTION ON GETTING BENEFICIARY ACCOUNT DETAILS: ", e);
            return finalResponse;
        }

        return finalResponse;
    }


    public Integer saveTipsTransfer(TIPSTransferForm tipsTransferForm, String reference, String txnType, String initiatedBy, String swiftMessage, String branchCode, MultipartFile[] files) {
        Integer res = -1;
        Integer res2 = -1;
        String initialStatus = "I";
        try {

            res = jdbcTemplate.update("INSERT INTO tips_transfers(message_type,sourceAcct, destinationAcct, amount, reference,txid,instrId,batch_reference, status, initiated_by,txn_type,purpose,sender_address,sender_phone,sender_name,swift_message,branch_no,cbs_status,beneficiary_contact,beneficiaryBIC,beneficiaryName,currency, compliance,direction) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    tipsTransferForm.getMessageType(), tipsTransferForm.getSenderAccount(), tipsTransferForm.getBeneficiaryAccount(), tipsTransferForm.getAmount(), reference, reference, reference, reference, initialStatus, initiatedBy, txnType, tipsTransferForm.getDescription(), tipsTransferForm.getSenderAddress(), tipsTransferForm.getSenderPhone(), tipsTransferForm.getSenderName(), swiftMessage, branchCode, "P", tipsTransferForm.getBeneficiaryContact(), tipsTransferForm.getBeneficiaryBIC(), tipsTransferForm.getBeneficiaryName(), tipsTransferForm.getCurrency(), "1", "OUTGOING");

            if (res != -1) {
                taskExecutor.execute(() -> {
                    try {
                        String ttype = "0101";

                        String message = systemVariable.SMS_NOTIFICATION_FOR_TXNS_ON_WORKFLOW.replace("{TXN_TYPE}", ttype)
                                .replace("{AMOUNT} ", " " + tipsTransferForm.getAmount() + " " + tipsTransferForm.getCurrency())
                                .replace("{CUSTOMER_NAME} ", " " + tipsTransferForm.getBeneficiaryName().toUpperCase())
                                .replace("{BENEFICIARY_BIC}", " " + tipsTransferForm.getBeneficiaryBIC().split("==")[0])
                                .replace("{REFERENCE}", " " + reference);
                        corebanking.sendSmsToApprovers(branchCode, message, reference);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            LOGGER.info("INITIATION RESULT: {}", res);

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
  GET Transactions on workflow in branch level
   */
    public String getoutwardTipsTransferOnWFAjax(String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        String status = "I";
        String APPLICATION_PROFILE = env.getProperty("spring.profiles.active");
        if (APPLICATION_PROFILE.equalsIgnoreCase("prod")) {
            if (roleId.equals("45")) {
                status = "PC";
            }
        } else {
            if (roleId.equals("43")) {
                status = "PC";
            }
        }
        try {
            mainSql = "select count(*) from tips_transfers  where status='I' and txn_type='0101' and cbs_status='P' AND  code<>'IB' and branch_no=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ',sourceAcct,' ',sender_name,' ',destinationAcct,' ',beneficiaryBIC,' ',beneficiaryName,' ',amount,' ',purpose,' ',initiated_by,' ',status,' ',cbs_status) LIKE ? and t.status='I' and txn_type='0101' and t.cbs_status='P' AND  t.code<>'IB' and t.branch_no=?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM tips_transfers " + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select t.*,(select name from banks b where  b.tips_bank_code=t.beneficiaryBic limit 1) as 'bank_name' from  tips_transfers t  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchNo});

            } else {
                mainSql = "select t.*,(select name from banks b where  b.tips_bank_code=t.beneficiaryBic limit 1) as 'bank_name' from tips_transfers t where  t.status='I' and t.txn_type='0101' and t.cbs_status='P' AND  t.code<>'IB' and t.branch_no=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.debug(mainSql.replace("?", "'" + branchNo + "'"));
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }


    public TipsPaymentFullObject getTipsTransaction(String txid) {
        try {
            String sql = "SELECT t.*,(select fsp_category from banks b where b.tips_bank_code = t.beneficiaryBIC limit 1) as fsp_category FROM tips_transfers t where t.txid=?";
            LOGGER.info(sql.replace("?", "'{}'"), txid);
            TipsPaymentFullObject txnFullDetails = this.jdbcTemplate.queryForObject(sql, new Object[]{txid},
                    (ResultSet rs, int rowNum) -> {
                        TipsPaymentFullObject txnDetails = new TipsPaymentFullObject();
                        txnDetails.setTxnType(rs.getString("txn_type"));
                        txnDetails.setAmount(rs.getString("amount"));
                        txnDetails.setAmount(rs.getString("amount"));
                        txnDetails.setSourceAcct(rs.getString("sourceAcct"));
                        txnDetails.setBeneficiaryName(rs.getString("beneficiaryName"));
                        txnDetails.setDestinationAcct(rs.getString("destinationAcct"));
                        txnDetails.setCurrency(rs.getString("currency"));
                        txnDetails.setBeneficiaryBic(rs.getString("beneficiaryBIC"));
                        txnDetails.setBeneficiaryContact(rs.getString("beneficiary_contact"));
                        txnDetails.setSenderPhone(rs.getString("sender_phone"));
                        txnDetails.setSenderAddress(rs.getString("sender_address"));
                        txnDetails.setSenderPhone(rs.getString("sender_phone"));
                        txnDetails.setSenderName(rs.getString("sender_name"));
                        txnDetails.setReference(rs.getString("reference"));
                        txnDetails.setTxid(rs.getString("txid"));
                        txnDetails.setBatchReference(rs.getString("batch_reference"));
                        txnDetails.setTxid(rs.getString("txid"));
                        txnDetails.setPurpose(rs.getString("purpose"));
                        txnDetails.setDirection(rs.getString("direction"));
                        txnDetails.setInitiatedBy(rs.getString("initiated_by"));
                        txnDetails.setBranchNo(rs.getString("branch_no"));
                        txnDetails.setCbsStatus(rs.getString("cbs_status"));
                        txnDetails.setFspCategory(rs.getString("fsp_category"));
                        return txnDetails;
                    });
            return txnFullDetails;
        } catch (Exception e) {
            LOGGER.info("ERROR FETCHING TIPS TRANSACTION....:{}", e);
            return null;
        }
    }

    /*
     Process TIPS Transactions
     */
    public String processTIPSTransactionToCoreBanking(TipsPaymentFullObject tipsTxn, UsRole role, philae.ach.UsRole achRole) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";
        String finalResponse = "-1";
        String jsonString = null;

        try {

            String ledger = tipsTxn.getSourceAcct();
            LOGGER.info("TIPS SOURCE ACCOUNT: {}", tipsTxn);
            int checkIfLedger = StringUtils.countMatches(ledger, "-");
            if (checkIfLedger >= 4) {
                /*
                 *SOURCE ACCOUNT IS GL-ACCOUNT
                 */
                result = "{\"result\":\"12\",\"message\":\" Source account can not be GL: " + ledger + "\"}";
            } else {
                //generate the request to RUBIKON
                TipsPaymentRequest tipsPaymentRequest = new TipsPaymentRequest();

                String tips_payment_url = systemVariable.TIPS_PAYMENT_URL;

                String callbackurl = systemVariable.TIPS_CALLBACK_URL;

                //check if its return payment then pick original reference from txid
                tipsPaymentRequest.setAmount(tipsTxn.getAmount());
                tipsPaymentRequest.setBenAccount(tipsTxn.getDestinationAcct());
                tipsPaymentRequest.setBeneficiaryName(tipsTxn.getBeneficiaryName());
                tipsPaymentRequest.setReference(tipsTxn.getReference());
                tipsPaymentRequest.setCurrency(tipsTxn.getCurrency());
                tipsPaymentRequest.setDescription(tipsTxn.getPurpose());
                tipsPaymentRequest.setSenderAccount(tipsTxn.getSourceAcct());
                tipsPaymentRequest.setBenInstitutionCategory(tipsTxn.getFspCategory()); //This is hardcoded
                tipsPaymentRequest.setChannelCode("06"); //hardcoded
                tipsPaymentRequest.setBenInstitutionCode(tipsTxn.getBeneficiaryBic());
                tipsPaymentRequest.setMsisdn(tipsTxn.getSenderPhone());
                tipsPaymentRequest.setSenderName(tipsTxn.getSenderName());
                tipsPaymentRequest.setUserRole(role);
                tipsPaymentRequest.setCallbackUrl(callbackurl);

                try {

                    LOGGER.info("TIPS PAYMENT REQ ...{},", tipsPaymentRequest.toString());
                    finalResponse = HttpClientService.sendTipsXMLRequest(tipsPaymentRequest.toString(), tips_payment_url);
                    LOGGER.info("Final response for tips transaction authorization... {}", finalResponse);
                    //check response
                    if (!finalResponse.equalsIgnoreCase("-1") && finalResponse != null) {
                        //jacksonmapper values
//                            jsonString = jacksonMapper.writeValueAsString(finalResponse);

                        TipsPaymentResponse tipsPaymentResponse = jacksonMapper.readValue(finalResponse, TipsPaymentResponse.class);
                        LOGGER.info("Mapped final response for tips transaction ... {}", tipsPaymentResponse);

                        if (tipsPaymentResponse.getResponseCode().equalsIgnoreCase("0")) {
                            //SUCCESS RESPONSE
                            String sql = "update tips_transfers set status='P',cbs_status='C',comments=?,message=?,reference=?,branch_approved_by=?,branch_approved_dt=? where  concat(txid,' ',reference) like ?";
                            LOGGER.info(sql.replace("?", "{}"), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getBankReference(), role.getUserName(), DateUtil.now(), tipsPaymentResponse.getReference());
                            jdbcTemplate.update(sql, tipsPaymentResponse.getMessage(), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getBankReference(), role.getUserName(), DateUtil.now(), "%" + tipsPaymentResponse.getReference() + "%");
                            result = "{\"result\":\"" + tipsPaymentResponse.getResponseCode() + "\",\"message\":\"" + tipsPaymentResponse.getMessage() + "\"}";
//                            LOGGER.info("TIPS SUCCESS: reference: {} , POSTED BY: {}", tipsPaymentResponse.getBankReference(), role.getUserName());
                        } else {
                            jdbcTemplate.update("update tips_transfers set comments=?,message=?,reference=?,branch_approved_by=?,branch_approved_dt=? where  concat(txid,' ',reference)  like ?", tipsPaymentResponse.getMessage(), tipsPaymentResponse.getMessage(), tipsPaymentResponse.getBankReference(), role.getUserName(), DateUtil.now(), tipsPaymentResponse.getReference());
                            result = "{\"result\":\"" + tipsPaymentResponse.getResponseCode() + "\",\"message\":\"" + tipsPaymentResponse.getMessage() + " \"}";
                            LOGGER.info("TIPS ERROR: reference: {} , POSTED BY: {}", tipsPaymentResponse.getBankReference(), role.getUserName());
                        }
                    }

                } catch (Exception e) {
                    LOGGER.info("EXCEPTION ON TIPS TRANSACTION AUTHORIZATION: ", e);
                    return finalResponse;
                }

            }

            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("TIPS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }


    //CALL BACK FOR TIPS TRANSACTIONS
    public String processTipsPaymentsFromCallBack(String payLoad) throws JsonProcessingException {
        TipsPaymentResponse tipsPaymentCallBack = this.jacksonMapper.readValue(payLoad, TipsPaymentResponse.class);
        String result = "default -1";
        if (tipsPaymentCallBack != null) {
            if (tipsPaymentCallBack.getResponseCode().equalsIgnoreCase("0")) {
                //COMMITTED OR SUCCESS
                jdbcTemplate.update("update tips_transfers set status='C',message='COMMITTED',hq_approved_by='SYSTEM',hq_approved_dt=? where  reference=?", DateUtil.now(), tipsPaymentCallBack.getBankReference());
                result = "{\"result\":\"0\",\"message\":\"SUCCESS\"}";
            } else {
                //not success
                jdbcTemplate.update("update tips_transfers set status='F',response_code=?, message=?,hq_approved_by='SYSTEM',hq_approved_dt=? where  reference=?", tipsPaymentCallBack.getResponseCode(), tipsPaymentCallBack.getMessage(), DateUtil.now(), tipsPaymentCallBack.getBankReference());
                result = "{\"result\":\"1\",\"message\":\"FAILED\"}";
            }
        } else {
            //null
            result = "{\"result\":\"-1\",\"message\":\"Tips responseCode callback is null\"}";
        }
        return result;
    }

    /**
     * REVERSAL TRANSACTIONS ON WORK FLOW
     **/
    public String getInwardReversalOnWFAjax(String txnDirection, String fromDate, String toDate) {
        String res = null;

        String tipsTxnsOnWF_URL = systemVariable.TIPS_TXNS_ON_WF;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&direction="
                        + txnDirection
                        + ""
                        + "&fromDate="
                        + fromDate
                        + ""
                        + "&toDate="
                        + toDate;
        Map<String, String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal("", tipsTxnsOnWF_URL, headers, "POST");
            res = response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //        LOGGER.info("TIPS RESPONSE FOR REVERSAL REQUEST:....{}", res);
        return res;
    }

    public TipsPaymentFullObject getTipsTransactionByReference(String reference) {
        List<Map<String, Object>> res = null;
        try {
            String sql = "SELECT * FROM tips_transfers WHERE reference=?";
            LOGGER.info(sql.replace("?", "'{}'"), reference);
            TipsPaymentFullObject txnFullDetails = this.jdbcTemplate.queryForObject(sql, new Object[]{reference},
                    (ResultSet rs, int rowNum) -> {
                        TipsPaymentFullObject txnDetails = new TipsPaymentFullObject();
                        txnDetails.setTxnType(rs.getString("txn_type"));
                        txnDetails.setAmount(rs.getString("amount"));
                        txnDetails.setAmount(rs.getString("amount"));
                        txnDetails.setSourceAcct(rs.getString("sourceAcct"));
                        txnDetails.setBeneficiaryName(rs.getString("beneficiaryName"));
                        txnDetails.setDestinationAcct(rs.getString("destinationAcct"));
                        txnDetails.setCurrency(rs.getString("currency"));
                        txnDetails.setBeneficiaryBic(rs.getString("beneficiaryBIC"));
                        txnDetails.setBeneficiaryContact(rs.getString("beneficiary_contact"));
                        txnDetails.setSenderPhone(rs.getString("sender_phone"));
                        txnDetails.setSenderAddress(rs.getString("sender_address"));
                        txnDetails.setSenderPhone(rs.getString("sender_phone"));
                        txnDetails.setSenderName(rs.getString("sender_name"));
                        txnDetails.setReference(rs.getString("reference"));
                        txnDetails.setTxid(rs.getString("txid"));
                        txnDetails.setBatchReference(rs.getString("batch_reference"));
                        txnDetails.setTxid(rs.getString("txid"));
                        txnDetails.setPurpose(rs.getString("purpose"));
                        txnDetails.setDirection(rs.getString("direction"));
                        txnDetails.setInitiatedBy(rs.getString("initiated_by"));
                        txnDetails.setBranchNo(rs.getString("branch_no"));
                        txnDetails.setCbsStatus(rs.getString("cbs_status"));
                        return txnDetails;
                    });
            return txnFullDetails;
        } catch (Exception e) {
//            LOGGER.info("ERROR FETCHING TIPS TRANSACTION FOR REVERSAL INITIATION....:{}", e);
            return null;
        }
    }

    public String initiateTipsTransferReversal(String transReference, String reversalReason) {
        String url = systemVariable.INITIATE_TIPS_TXN_REVERSAL;
        String finalRes = "-1";
        String headers =
                "&Authorization="
                        + 00000
                        + ""
                        + "&transReference="
                        + transReference
                        + ""
                        + "&reversalReason="
                        + reversalReason;
        finalRes = HttpClientService.sendTipsXMLRequest(headers, url);
//        LOGGER.info("The initiation of TIPS reversal has the following response... {} ", finalRes);
        return finalRes;
    }

    public String tipsTransactionsReportAjax(String institution, String direction, String fromDate, String toDate) throws Exception {
        String res = null;

        String tipsTxnsOnWF_URL1 = systemVariable.TIPS_TRANSER_REPORT_URL;
//        String tipsTxnsOnWF_URL = "http://localhost:8460/wakala/esb/tips/transferReport";
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&direction="
                        + direction
                        + ""
                        + "&fromDate="
                        + fromDate
                        + ""
                        + "&toDate="
                        + toDate
                        + ""
                        + "&institutionCode="
                        + institution;

        Map<String, String> response = HttpClientService.sendTxnToBrinjal("", tipsTxnsOnWF_URL1, headers, "POST");
//        LOGGER.info("TIPS RESPONSE FOR TIPS REPORT:....{}", response);
        res = response.get("responseBody");
        return res;
    }

    public ConfirmBotRoot getTransactionDetailsFromBOT(String reference) throws Exception {
        String res = null;
        String transfer_ENQUIRY_URL = systemVariable.BOT_TRANSFER_ENQUIRY;
        String benInstitutionCode = "504";
        String headers = "&transReference=" + reference + "&Authorization=" + 000;
        if(reference.startsWith("048-V1-")){//THIS IS FOR GATEWAY
            benInstitutionCode = "504";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        //TODO: THIS IS FOR TIGO
        if(reference.startsWith("048-V2-")){//TIGO
            benInstitutionCode = "501";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V22-")){//TIGO-TIGO
            benInstitutionCode = "501";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V23-")){//TIGO-AIRTEL
            benInstitutionCode = "504";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V24-")){//TIGO-HALOPESA
            benInstitutionCode = "506";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }

        //TODO: THIS IS FOR AITREL
        if(reference.startsWith("048-V3-")){//AIRTEL
            benInstitutionCode = "504";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V33-")){//AIRTEL-AIRTEL
            benInstitutionCode = "504";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V32-")){//AIRTEL-TIGO
            benInstitutionCode = "501";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }  if(reference.startsWith("048-V34-")){//AIRTEL-HALOPESA
            benInstitutionCode = "506";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }

        //TODO: THIS IS FOR HALOPESA
        if(reference.startsWith("048-V4-")){//HALOPESA
            benInstitutionCode = "506";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V44-")){//HALOPESA-HALOPESA
            benInstitutionCode = "506";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V42-")){//HALOPESA-TIGO
            benInstitutionCode = "501";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        if(reference.startsWith("048-V43-")){//HALOPESA-AIRTEL
            benInstitutionCode = "504";
            transfer_ENQUIRY_URL =systemVariable.VIKOBA_BOT_TRANSFER_ENQUIRY;
            headers = "&transReference=" + reference +"&beneficiaryInstitutionCode=" +benInstitutionCode+ "&Authorization=" + 000;;
        }
        LOGGER.info("tracing the reference: ...{}, url used ...{} the payload ... {}",reference,transfer_ENQUIRY_URL,headers);
        Map<String, String> response = HttpClientService.sendTxnToBrinjal("", transfer_ENQUIRY_URL, headers, "POST");

        res = response.get("responseBody");

        return jacksonMapper.readValue(res, ConfirmBotRoot.class);
    }

    public String authorizeRequestedReversal(String bankReference, String reversalRef) {
        String res = null;
        String authorize_TIPS_REVERSAL_URL = systemVariable.AUTHORIZE_TIPS_REVERSAL;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&bankReference="
                        + bankReference
                        + ""
                        + "&reversalReference="
                        + reversalRef;

        res = HttpClientService.sendTipsXMLRequest(headers, authorize_TIPS_REVERSAL_URL);
        LOGGER.info("TIPS RESPONSE FOR TIPS REVERSAL AUTHORIZATION :....{}", res);
        return res;
    }


    public int registerTipsFraud(FraudRegistrationForm fraudRegistrationForm) {
        int res = -1;
        String finalRes = null;
        String register_TIPS_FRAUD = systemVariable.REGISTER_TIPS_FRAUD_URL;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&fsp="
                        + fraudRegistrationForm.getFsp()
                        + ""
                        + "&fullName="
                        + fraudRegistrationForm.getFullName()
                        + ""
                        + "&identityType="
                        + fraudRegistrationForm.getIdentityType()
                        + ""
                        + "&identityValue="
                        + fraudRegistrationForm.getIdentityValue()
                        + ""
                        + "&identifierType="
                        + fraudRegistrationForm.getIdentifierType()
                        + ""
                        + "&identifierValue="
                        + fraudRegistrationForm.getIdentifierValue()
                        + ""
                        + "&reasons="
                        + fraudRegistrationForm.getReasons();

        finalRes = HttpClientService.sendTipsXMLRequest(headers, register_TIPS_FRAUD);
        LOGGER.info("RESPONSE FROM FRAUD REGISTRATION :....{}", res);
        return res;
    }

    public String getTCBTipsFraudsAjax(String fsp, String fromDate, String toDate) {
        String res = null;
        String tcb_TIPS_FRAUD_URL = systemVariable.GET_TCB_TIPS_FRAUDS_URL;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&fsp="
                        + fsp
                        + ""
                        + "&fromDate="
                        + fromDate
                        + ""
                        + "&toDate="
                        + toDate;
        res = HttpClientService.sendTipsXMLRequest(headers, tcb_TIPS_FRAUD_URL);
        LOGGER.info("TIPS RESPONSE FOR TIPS FRAUDS :....{}", res);
        return res;
    }

    public String getInstitutionCategory(String institutionCode) {
        String finalRes = null;
        try {
            String sql = "SELECT fsp_category FROM banks WHERE tips_bank_code=?";
            finalRes = this.jdbcTemplate.queryForObject(sql, new Object[]{institutionCode}, String.class);
        } catch (DataAccessException dae) {
            LOGGER.info("Data access exception on getting fsp_category:... {}", dae);
        }
        LOGGER.info("Returned fsp_category for tips lookup is: ... {}", finalRes);
        return finalRes;
    }

    public String fireGeneralReconTabs() {
        try {

        } catch (DataAccessException dataAccessException) {
            LOGGER.info("Failed to access data.. {}", dataAccessException.getMessage());
        }
        return "";
    }


    public String tipsAuthorizeReversalCancellation(String bankReference, String reversalRef) {
        String res = null;
        String cancel_TIPS_REVERSAL_URL = systemVariable.CANCEL_TIPS_REVERSAL;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&bankReference="
                        + bankReference
                        + ""
                        + "&reversalReference="
                        + reversalRef;

        res = HttpClientService.sendTipsXMLRequest(headers, cancel_TIPS_REVERSAL_URL);
        LOGGER.info("TIPS RESPONSE FOR TIPS CANCEL AUTHORIZATION :....{}", res);
        return res;
    }

    public byte[] printTransactionReceipt(String reference) {
        byte[] result = null;
        try {
            String res = null;
            String printTipsAdviceURL = systemVariable.PRINT_TIPS_ADVISE_URL;
            String headers =
                    "&Authorization="
                            + 000000
                            + ""
                            + "&transReference="
                            + reference;
            Map<String, String> response = HttpClientService.sendTxnToBrinjal("", printTipsAdviceURL, headers, "POST");

            res = response.get("responseBody");

//            LOGGER.info("TIPS RESPONSE FOR TIPS PRINTING ADVISE :....{}", res);
            result = Base64.decodeBase64(res);
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked... {}", result, e.getMessage());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getFspParticipants() throws Exception {
        String res = null;
        String fsp_PARTICIPANTS = systemVariable.FSP_PARTICIPANTS;
        String headers =
                "&Authorization="
                        + 000000;
        res = HttpClientService.sendTxnToBrinjal("", fsp_PARTICIPANTS, headers, "POST").get("responseBody");
        return res;
    }

    public JsonResponse validateMissingTipsTransaction(String reference, String branchCode) {
        JsonResponse response = new JsonResponse();

        //validate in brinjal transactions table
        String sql = "SELECT * FROM transactions WHERE reference=?";
        List<Map<String, Object>> brinjalData = jdbcBrinjalTemplate.queryForList(sql, reference);
        String TIPS_SETTLEMENT_GL = systemVariable.TIPS_SETTLEMENT_ACCOUNT;
        LOGGER.info(sql.replace("?","'{}'"),reference);
        LOGGER.info("Brinjal Transactions table check ... {}, data... {}",reference, brinjalData);
        if (brinjalData == null || brinjalData.isEmpty()) {
            //validate in channelmanger logs
            UntrackedTipsTxn cmLogsData = new UntrackedTipsTxn();
            sql = "SELECT CONTRA, ACCOUNT ,AMOUNT ,TXN_REF, DESCRIPTION ,CURRENCY  FROM CHANNELMANAGER.PHL_TXN_LOG WHERE TXN_REF =?";
            cmLogsData = jdbcRUBIKONTemplate.queryForObject(sql, new Object[]{reference}, (ResultSet rs, int rowNum) -> {
                UntrackedTipsTxn utt = new UntrackedTipsTxn();
                utt.setContraAccount(rs.getString("CONTRA"));
                utt.setCustomerAccount(rs.getString("ACCOUNT"));
                utt.setAmount(rs.getString("AMOUNT"));
                utt.setReference(rs.getString("TXN_REF"));
                utt.setDescriptions(rs.getString("DESCRIPTION"));
                utt.setCurrency(rs.getString("CURRENCY"));
                return utt;
            });

            LOGGER.info("cmlog check ... {}, data... {}",reference, cmLogsData);
            if (cmLogsData != null) {
                if (cmLogsData.getContraAccount().equalsIgnoreCase(TIPS_SETTLEMENT_GL)) {
                    LOGGER.info("settlement gl validation ... {}, data ... {}",reference, cmLogsData.getContraAccount());
                    //validate in customer account if debited and not reversed
                    sql = "SELECT * FROM DEPOSIT_ACCOUNT_HISTORY WHERE ACCT_NO =? AND TXN_AMT =? AND TRAN_REF_TXT =?";
                    List<Map<String, Object>> validateCBS = jdbcRUBIKONTemplate.queryForList(sql, cmLogsData.getCustomerAccount(), cmLogsData.getAmount(), reference);
                    LOGGER.info("validate if need refund for reference.. {} and size ... {}", reference, validateCBS.size());
                    if (validateCBS.size() == 1) {
                        //reverse transaction

                        String identifier = "api:postGLToDepositTransfer";
                        TxRequest transferReq = new TxRequest();
                        transferReq.setReference(reference);
                        transferReq.setNarration("REV~" + cmLogsData.getDescriptions());
                        transferReq.setCurrency(cmLogsData.getCurrency());
                        transferReq.setDebitAccount(TIPS_SETTLEMENT_GL);
                        transferReq.setCreditAccount(cmLogsData.getCustomerAccount());
                        transferReq.setAmount(new BigDecimal(cmLogsData.getAmount()));
                        transferReq.setReversal("false");
                        transferReq.setScheme("T1089");
                        transferReq.setCreditFxRate(new BigDecimal(0));
                        transferReq.setDebitFxRate(new BigDecimal(0));
                        transferReq.setUserRole(systemVariable.apiUserRole("060"));
                        PostDepositToGLTransfer postOutwardReq = new PostDepositToGLTransfer();
                        postOutwardReq.setRequest(transferReq);
                        String requestToCore = XMLParserService.jaxbGenericObjToXML(postOutwardReq, Boolean.FALSE, Boolean.TRUE);
                        //process the Request to CBS
                        XaResponse cbsResponse = XMLParserService.jaxbXMLToObject(corebanking.processRequestToCore(requestToCore, identifier), XaResponse.class);

                        LOGGER.info("response result from rubikon .... {} and message ... {}", cbsResponse.getResult(), cbsResponse.getMessage());
                        if (cbsResponse.getResult() == 0 || cbsResponse.getResult() == 26) {
                            //notify user on successfully reversal
                            response.setStatus("SUCCESS_REVERSAL");
                            response.setResult("Message from Rubikon is: " + cbsResponse.getMessage() +" for the attempt to reverse transaction to customer account: "+cmLogsData.getCustomerAccount() +" Reference: "+ reference);
                        } else {
                            //notify user on failure to reverse
                            response.setStatus("FAILED_REVERSAL");
                            response.setResult("Transaction failed to reverse into customer account: "+cmLogsData.getCustomerAccount() +" Reference: "+ reference);
                        }
                    } else {
                        //two entries found in customer account with same reference.
                        response.setStatus("AMBIGIOUS_REVERSAL");
                        response.setResult("Seems Transaction was reversed by system, Please confirm on customer account: "+cmLogsData.getCustomerAccount() +" Reference: "+ reference);
                    }

                } else {
                    response.setStatus("NOT_TIPS_REVERSAL");
                    response.setResult("Transaction Found but posted in GL that is not TIPS SETTLEMENT GL: "+cmLogsData.getContraAccount() +" Reference: "+reference);
                    LOGGER.info("Reference ... {} posted in other GL other than ....{}", reference, TIPS_SETTLEMENT_GL);
                }
            } else {
                //not found in logs
                LOGGER.info("NOT found in cbs reference ....{}", reference);
                response.setStatus("NOT_FOUND");
                response.setResult("Transaction Not Found in CBS");
            }
        } else {
            //found in brinjal, use confirm bot in TIPS REPORT
            response.setStatus("TRANSACTION_FOUND");
            response.setResult("Transaction Found in TIPS REPORT. PLEASE USE CONFIRM BOT BUTTON.");
        }
        return response;
    }
}
