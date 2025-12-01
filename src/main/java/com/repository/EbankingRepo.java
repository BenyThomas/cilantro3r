/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.Ebanking.AmendVisaCardForm;
import com.DTO.Ebanking.CardRegistrationReq;
import com.DTO.Ebanking.CreateCardRequest;
import com.DTO.ubx.UbxResponse;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.EncryptUtil;
import com.models.ubx.CardDetailsEntity;
import com.repository.ubx.CardDetailsRepository;
import com.service.BCXService;

import java.util.List;
import java.util.Map;

import com.service.TransferService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.json.JSONObject;
import com.helper.EncryptorGCMPassword;
import com.service.HttpClientService;

import java.util.ArrayList;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

/**
 * @author melleji.mollel
 */
@Repository
public class EbankingRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EbankingRepo.class);
    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    BCXService bcxService;

    @Autowired
    HttpSession httpSession;

    @Autowired
    SYSENV systemVariables;
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcTemplateCbs;

    @Autowired
    TransferService transferService;

    @Autowired
    CardDetailsRepository cardDetailsRepository;

    /*
    GET E-BANKING dashboard
     */
    public List<Map<String, Object>> getEFTModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING BATCH : {}", e.getMessage());
        }
        return result;
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

    public Integer saveCardRequest(CreateCardRequest cardReq) {
        Integer res = -1;
        LOGGER.info("Does name and username contain ng'ombe thing. for account... {}, .... {},....{}", cardReq.getAccountNo(), cardReq.getCustomerName(), cardReq.getCustShortName());

        String customerName = cardReq.getCustomerName();
        String shortName = cardReq.getCustShortName();

        if (cardReq.getCustomerName().contains("'")) {
            customerName = StringUtils.replace(cardReq.getCustomerName(), "'", "`");
            shortName = StringUtils.replace(cardReq.getCustShortName(), "'", "`");
            LOGGER.info("Final replaced names for account... {}, .... {},....{}", cardReq.getAccountNo(), customerName, shortName);
        }

        try {
            res = jdbcTemplate.update("INSERT INTO card(account_no, customer_name, custid, customer_rim_no, reference, customer_shortName, customer_category, address1, address2, address3, address4, status, PAN, stage, created_by,originating_branch,collecting_branch,phone,email,bin) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    cardReq.getAccountNo(), customerName, cardReq.getCustomerId(), cardReq.getCustomerRim(), cardReq.getReference(), shortName, cardReq.getCustCategory(), cardReq.getAddress1(), cardReq.getAddress2(), cardReq.getAddress3(), cardReq.getAddress4(), "I", cardReq.getCustomerRim(), "0", cardReq.getCreatedBy(), cardReq.getOriginatingBranch(), cardReq.getRecruitingBrach(), cardReq.getPhoneNumber().trim(), cardReq.getCustomerEmail(),cardReq.getCardType());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public String getInitiatedCardRequests(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        String loggedInnUser = httpSession.getAttribute("username").toString();
        //get report for HQ/IBD USER
        try {
            mainSql = "SELECT count(*) FROM card where originating_branch=? and status='I' and stage='0'";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE  concat(reference,' ', account_no,' ', customer_rim_no,' ', custid,' ', customer_shortName,' ', customer_category,' ', address1,' ', address2,' ', address3,' ', address4,' ', city,' ', state,' ', phone,' ', account_branch_id,' ', created_by) LIKE ? AND originating_branch=? and status='I' and stage='0'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM card b" + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from card b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, branchNo);
                LOGGER.info("Datatable Response: {}", results);
            } else {
                mainSql = "select * from card where originating_branch=?  and status='I' and  stage='0' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }

        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;

    }

    /*
    GET SWIFT MESSAGE SUPPORTING DOCUMENT
     */
    public List<Map<String, Object>> getSupportingDocument(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM  ib_documents  where reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTTING SUPPORTING DOCUMENT: {}", e.getMessage());
        }
        return result;
    }

    //get Card details
    public List<Map<String, Object>> getCardRequests(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select * from card  where reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING CARD REQUEST: {}", e.getMessage());
        }
        return result;
    }

    public byte[] getSupportingDocument(String ref, String id) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcTemplate.queryForObject("select document from ib_documents where reference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public String branchApproveCardCreationRequest(String reference, String approvedBy) {
        String result = "{\"result\":\"99\",\"message\":\" An Error occurred During card creation Request, Please try again after FIVE minutes\"}";
        int res = -1;

        try {
            res = jdbcTemplate.update("UPDATE card set stage='1',approver1=?,approver1_dt=?,status='P' where reference=?", approvedBy, DateUtil.now(), reference);
            if (res != -1) {
                result = "{\"result\":\"0\",\"message\":\"Card Request Successfully created.\"}";
            }
        } catch (Exception e) {
            LOGGER.info("Failed to approve card at branch level, database issue .... {}", e);
        }
        return result;

    }

    public String fireReturnVisaCardForAmmendment(String reference) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During returning card for amendment\"}";

        try {
            String status = "RA";
            int res = jdbcTemplate.update("UPDATE card SET stage='0', status='RA' where reference=?", reference);
            if (res == 1) {
                result = "{\"result\":\"0\",\"message\":\"Card Returned for amendment .\"}";
            }
        } catch (Exception e) {
            LOGGER.info("Exception found...  in returning card for amendment...{}", e);
        }
        return result;

    }

    /*
    INSERT SUPPORTING DOCUMENT
     */
    public Integer saveSupportingDocuments(String reference, MultipartFile file, String createdBy) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT INTO ib_documents(reference,document,fileName,created_by) VALUES (?,?,?,?)",
                    reference, file.getBytes(), file.getOriginalFilename(), createdBy);
            LOGGER.info("INSERTING FILE :{} FILE NAME: {} SIZE: {}", result, file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON INSERTING SUPPORTING DOCUMENT: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public String getCardsRequestReadyForPANGeneration(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(*) FROM card where  status='P' and stage='1'";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ', account_no) LIKE ?  and status='P' and stage='1'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM card b" + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from card b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);
                results = this.jdbcTemplate.queryForList(mainSql, searchValue);
                LOGGER.info("Datatable Response: {}", results);

            } else {
                mainSql = "select * from card where  status='P' and  stage='1' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{});
            }
            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }

        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;

    }

    public String HqApproveCardCreationRequest(String rimNo, String reference, String AccountNo, String customerFullName, String approvedBy, String branchCode) {
        String result = "{\"result\":\"99\",\"message\":\"A general Error occured during PAN creation\"}";
        //get card details so to get bin
        CardDetailsEntity cardDetails = cardDetailsRepository.findByReference(reference).orElseThrow(() -> new RuntimeException("No record found for reference: " + reference));
        int cbsResult = 0;
        //check charge deducted already
        boolean isVisaAlreadyCharge = isVisaAlreadyCharge(reference);
        if (isVisaAlreadyCharge == false) {

            if(cardDetails.getBin() != null && cardDetails.getBin().equals("402141028")) {
                //disable charge
            }else {
                cbsResult = transferService.procChargeForVisaCard(reference, AccountNo, branchCode, "TZS");
            }
            LOGGER.info("post card charge cbs response for account ... {} is ... {}", AccountNo, cbsResult);
            if (cbsResult == 0) {
                jdbcTemplate.update("UPDATE card SET charge=?, is_charged='1', gen_pan_attempt='1' WHERE reference=?", systemVariables.VISA_CARD_REQUEST_CHARGE, reference);
            }else if(cbsResult == 51){
                jdbcTemplate.update("UPDATE card SET gen_pan_attempt='1' WHERE reference=?", reference);
            }
        }
        if (cbsResult == 0) {
                LOGGER.info("posted charge successfully account number... {} and going to generate pan", AccountNo);
                String bin = systemVariables.TCB_BANK_VISA_CARDS_BIN;
                if (cardDetails.getBin() != null) {
                    bin = cardDetails.getBin();
                }
            String generateJsonCardCreationReq = "{\n"
                                        + "\"serviceType\": \"ISSUECARD\",\n"
                                        + "\"msisdn\": \"" + AccountNo + "\",\n"
                                        + "\"cif\": \"" + rimNo + "\",\n"
                                        + "\"fullName\": \"" + customerFullName + "\",\n"
                                        + "\"bin\":\"" + bin+ "\",\n"
                                        + "\"accountType\":\"10\"\n"
                                + "}";

            String clearReponse = bcxService.createPANRequestAndSendToBCX(generateJsonCardCreationReq);//create PAN from BCX WEBSERVICE
            if (!clearReponse.equalsIgnoreCase("-1")) {
                if (clearReponse.contains("Exception:")) {
                    jdbcTemplate.update("UPDATE card set approver2=?,approver2_dt=?,status='AP',PAN='ER1',is_allowed=?,collecting_branch=? where reference=?", approvedBy, DateUtil.now(), clearReponse, branchCode,reference);
                    result = "{\"result\":\"9112131\",\"message\":\"An Error occured: " + clearReponse + "\"}";
                    return result;
                }
                JSONObject jsonObject = new JSONObject(clearReponse);//get PVV DATA FROM POSTILION RESPONSE
                String responseCode = jsonObject.getString("responseCode");
                String message = jsonObject.getString("responseMessage");
                String PAN = "-1";
                int res = -1;

                Object PANObject = jsonObject.get("encPan");
                if (responseCode.equalsIgnoreCase("0") || responseCode.equalsIgnoreCase("200")) {
                    if (PANObject != null) {
                        try {
                            PAN = EncryptorGCMPassword.decrypt(PANObject.toString(), systemVariables.BCX_PAN_DECRYPT_KEY);
                            LOGGER.info("BEFORE TRYING TO DECRYPT PAN:{} LENGTH:{}", PAN,PAN.length());
                            PAN = PAN.trim().replace("\r", "")     // Remove carriage returns
                                    .replace("\t", "")     // Remove tabs
                                    .replaceAll("\\s+$", "") // Trim trailing whitespaces
                                    .replaceAll("[\\u0000-\\u001F]", "");
                            LOGGER.info("AFTER TRYING TO DECRYPT PAN:{} LENGTH:{}", PAN, PAN.length());
                        } catch (Exception ex) {
                            jdbcTemplate.update("UPDATE card set approver2=?,approver2_dt=?,status='AP',PAN=?,is_allowed=?,collecting_branch=? where reference=?", approvedBy, DateUtil.now(), PAN, responseCode, branchCode,reference);
                            LOGGER.info("UNABLE TO DECRYPT PAN:{} ", clearReponse);
                        }

                    }
                    LOGGER.info("jsonObject.get(encPan) ... {} acctno ... {}, PAN obj... {} and PAN ... {}", jsonObject.get("encPan"), AccountNo, PANObject.toString(), PAN);

                    try {
                        res = jdbcTemplate.update("UPDATE card set stage='2',approver2=?,approver2_dt=?,status='AP',PAN=?,collecting_branch=?, is_allowed=? where reference=?", approvedBy, DateUtil.now(), PAN, branchCode, responseCode, reference);
                        if (res != -1) {
                            result = "{\"result\":\"0\",\"message\":\"Card Request Successfully created." + PAN + "\"}";
                        }
                    } catch (Exception e) {
                        LOGGER.info("EXCEPTION OCCURED:{} ", e.getMessage());
                        jdbcTemplate.update("UPDATE card set approver2=?,approver2_dt=?,status='AP',PAN=?,is_allowed=?, collecting_branch=? where reference=?", approvedBy, DateUtil.now(), PAN, responseCode,branchCode, reference);
                        result = "{\"result\":\"91\",\"message\":\"A card is Created successfully But an error occured during processing. Contact Ebanking for further support\"}";
                    }
                } else {
                    //TESTING UPDATING THE DUMMY PAN
                    //PAN = "40260290090000201";
                    PAN = "96";
                    res = jdbcTemplate.update(
                            "UPDATE card set approver2=?, approver2_dt=?, status='AP', PAN=?, is_allowed=?, stage='2', collecting_branch=? where reference=?",
                            approvedBy,                 // 1
                            DateUtil.now(),             // 2
                            PAN,                        // 3
                            responseCode,               // 4
                            branchCode,                 // 5
                            reference                   // 6
                    );

                    result = "{\"result\":\"" + responseCode + "\",\"message\":\"" + message + " payload:" + clearReponse.replace("\"", "'") + ".Contact E-Banking for Support: ERROR CODE:\"}";

                }
            } else {
                jdbcTemplate.update("UPDATE card set approver2=?,approver2_dt=?,status='AP',PAN='96',is_allowed='96',stage='2', collecting_branch=? where reference=?", approvedBy, DateUtil.now(), branchCode,reference);
                result = "{\"result\":\"96\",\"message\":\"An Error occured during card creation BXC SIDE\"}";

            }
        } else {
            result = "{\"result\":\"" + cbsResult + "\",\"message\":\"An Error occured during posting charge to cbs\"}";

        }
        return result;
    }

    public String getCardIssuanceAjax(String cardType, String branchCode, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        //uncomissioned TPB
        mainSql = "Select count(*) from card where status='CD' AND collecting_branch=?";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchCode}, Integer.class);
        if (!searchValue.equals("")) {
            searchValue = "'%" + searchValue + "%'";
            //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
            searchQuery = " WHERE concat(customer_rim_no,' ',phone,' ',account_no,' ',customer_name,' ',PAN) LIKE ? AND collecting_branch=? AND status='CD'";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM card b" + searchQuery, new Object[]{searchValue, branchCode}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select * from card " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, branchCode});
        } else {
            mainSql = "select * from card where status='CD' AND collecting_branch =? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchCode});
        }
        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public List<Map<String, Object>> getCardManagementHQAjax(String branchCode, String statusCode, String fromDate, String toDate) {
        List<Map<String, Object>> results = null;
        String mainSql;
        switch (branchCode) {
            case "ALL":
                mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status=? and (date(c.approver2_dt)>=? and date(c.approver2_dt)<=?) ";
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{statusCode, fromDate, toDate});
                break;
            default:
                mainSql = "select c.*,b.name as branch_name from card c left join branches b on b.code = c.collecting_branch where c.status=? and c.collecting_branch=? and (date(c.approver2_dt)>=? and date(c.approver2_dt)<=?)";
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{statusCode, branchCode, fromDate, toDate});
                break;
        }
        return results;
    }
    //get Card details

    public List<Map<String, Object>> getCardsBasedOnPANLists(String panList) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.*,(select name from branches where code=a.originating_branch limit 1) branchName from card a where PAN IN (" + panList + ")");
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING CARD REQUEST: {}", e.getMessage());
        }
        return result;
    }

    /*
    RECEIVE CARDS FROM PRINTING UNIT
     */
    public String SubmitReceiveCardsFromPrintingUnitForm(String panLists, String receivedBy) {
        String result = "{\"result\":\"99\",\"message\":\"A general Error occured during confirming\"}";
        int res = -1;
        try {
            res = jdbcTemplate.update("UPDATE card set status='CP',received_from_printing_dt=?,received_from_printing=? where PAN in (" + panLists + ")", DateUtil.now(), receivedBy);
            if (res != -1) {
                result = "{\"result\":\"0\",\"message\":\"Card received successfully and they are ready for dispatch\"}";
            }
        } catch (Exception e) {
            LOGGER.info("EXCEPTION OCCURED DURING RECEIVING CARDS FROM PRINTING UNIT DZ CARD:{} ,PAN LIST:{}", e.getMessage(), panLists);
        }

        return result;

    }

    /* CANCEL CARD
     */
//(customeQuery.get("rimNo"), customeQuery.get("reference"), customeQuery.get("accountNo"), customeQuery.get("accountName"), (String) httpsession.getAttribute("username") + "");
    public String HqCancelCardCreationRequest(String rimNo, String reference, String accountNo, String accountName, String username) {
        String result = "{\"result\":\"99\",\"message\":\"A general Error occured during confirming\"}";
        int res = -1;
        try {
            res = jdbcTemplate.update("UPDATE card set status='CANCELLED',hq_received_dt=?,hq_received_by=? where account_no =? AND customer_rim_no =? AND reference=?", DateUtil.now(), username, accountNo, rimNo, reference);
            if (res != -1) {
                result = "{\"result\":\"0\",\"message\":\"Card cancelled successfully\"}";
            }
        } catch (Exception e) {
            LOGGER.info("HqCancelCardCreationRequest: {}", e.getMessage(), reference);
        }

        return result;

    }

    /*
    DISPATCH CARDS TO BRANCH
     */
    public String submitDipatchCardsTobranchesForm(String panLists, String receivedBy) {
        String result = "{\"result\":\"99\",\"message\":\"A general Error occured during confirming\"}";
        int res = -1;
        try {
            res = jdbcTemplate.update("UPDATE card set status='CD',dispatched_dt=?,dispatched_by=? where PAN in (" + panLists + ")", DateUtil.now(), receivedBy);
            if (res != -1) {
                result = "{\"result\":\"0\",\"message\":\"Cards dispatched to Branch Successfully\"}";
            }
        } catch (Exception e) {
            LOGGER.info("EXCEPTION OCCURED:{} ", e.getMessage());
        }

        return result;

    }

    public List<Map<String, Object>> getPosTerminalDeails(String branchCode) {
        List<Map<String, Object>> result = null;
        try {
            if (branchCode.equalsIgnoreCase("060")) {
                branchCode = "173";//FOR TESTING PURPOSES
            }
            String sql = "SELECT * FROM CHANNELMANAGER.PHT_TERMINAL pt WHERE pt.BU_NO =? AND OPERATOR ='B' AND REC_ST ='A' AND CHANNEL='POS' ORDER BY SYS_DATE  DESC FETCH FIRST 1 ROWS ONLY ";

            result = this.jdbcTemplateCbs.queryForList(sql, branchCode);
            LOGGER.info(sql.replace("?", "'{}'") + "\n{}", branchCode, result);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING CARD REQUEST: {}", e.getMessage());
        }
        return result;
    }

    //REQUEST INITIAL PIN FROM BCX
    public String getInitialPINFromPostilionSwitch(String payLoad) throws Exception {
        LOGGER.info("Payload: " + payLoad.split("=="));

        String pan = payLoad.split("==")[0];
        pan = pan.trim().replace("\r", "")     // Remove carriage returns
                .replace("\t", "")     // Remove tabs
                .replaceAll("\\s+$", "") // Trim trailing whitespaces
                .replaceAll("[\\u0000-\\u001F]", "");;
        LOGGER.info("trimmed pan: {}", pan);
        final String vPan = pan;
        //get bin for platnum card
        CardDetailsEntity cardDetails = cardDetailsRepository.findByPan(pan).orElseThrow(() -> new RuntimeException("No such pan found "+vPan));

        String EncPan = EncryptorGCMPassword.encrypt(pan.getBytes(), systemVariables.BCX_PAN_DECRYPT_KEY);
        String PIN = "-1";
        String bin = systemVariables.TCB_BANK_VISA_CARDS_BIN;
        if (cardDetails.getBin() != null) {
            bin = cardDetails.getBin();
        }
        String pinResetRequest = "{"
                + "\"serviceType\":\"PINRESET\","
                + "\"msisdn\":\"" + payLoad.split("==")[1] + "\","
                + "\"EncPan\":\"" + EncPan + "\","
                + "\"bin\":\"" + bin + "\""
                + "}";
        String clearReponse = bcxService.createPANInitialPINBCX(pinResetRequest);//create PAN from BCX WEBSERVICE
        if (!clearReponse.equalsIgnoreCase("-1")) {
            JSONObject jsonObject = new JSONObject(clearReponse);//get PVV DATA FROM POSTILION RESPONSE
            String responseCode = jsonObject.getString("responseCode");
            String message = jsonObject.getString("responseMessage");

            int res = -1;

            Object PINObject = jsonObject.get("encPin");
            if (responseCode.equalsIgnoreCase("0") || responseCode.equalsIgnoreCase("00") || responseCode.equalsIgnoreCase("200")) {
                if (PINObject != null) {
                    try {
                        PIN = EncryptorGCMPassword.decrypt(PINObject.toString(), systemVariables.BCX_PAN_DECRYPT_KEY);
//                        LOGGER.info("PIN:{} ", PIN);
                    } catch (Exception ex) {
                        LOGGER.info("UNABLE TO DECRYPT PIN:{} ", clearReponse);
                    }

                }
            }
        }
        return PIN.trim();
    }

    /*
    Issue Card to core banking
     */
    public String issueCardToCoreBanking(CardRegistrationReq cardRegistrationReq, String token, String receivedBy) {
        String result = "{\"result\":\"99\",\"message\":\"A general Error occured during confirming\"}";
        CardDetailsEntity cardDetails = cardDetailsRepository.findByReference(cardRegistrationReq.getReference()).orElseThrow(() -> new RuntimeException("No record found for reference: " + cardRegistrationReq.getReference()));
        int cbsResult = 0;
        //check charge deducted already
        boolean isVisaAlreadyCharge = isVisaAlreadyCharge(cardRegistrationReq.getReference());
        if (isVisaAlreadyCharge == false) {
            String branchNo  = this.jdbcTemplate.queryForObject( "SELECT collecting_branch FROM card WHERE reference=?", new Object[]{cardRegistrationReq.getReference()}, String.class);

            if(cardDetails.getBin() != null && cardDetails.getBin().equals("402141028")) {
                //disable charge
            }else {
                cbsResult = transferService.procChargeForVisaCard(cardRegistrationReq.getReference(), cardRegistrationReq.getAccountNo(), branchNo, "TZS");
            }
            if (cbsResult == 0) {
                jdbcTemplate.update("UPDATE card SET charge=?, is_charged='1', gen_pan_attempt='1' where reference=?", systemVariables.VISA_CARD_REQUEST_CHARGE, cardRegistrationReq.getReference());
            }else if(cbsResult == 51){
                jdbcTemplate.update("UPDATE card SET gen_pan_attempt='1' where reference=?", cardRegistrationReq.getReference());
            }
        }
        if (cbsResult == 0) {
        //generate request for card issuance to core banking
        String cardRegJson = "{\n"
                + "  \"accountNo\": \"" + cardRegistrationReq.getAccountNo() + "\",\n"
                + "  \"customerName\": \"" + cardRegistrationReq.getCustomerName() + "\",\n"
                + "  \"customerPhoneNumber\": \"" + cardRegistrationReq.getCustomerPhoneNumber() + "\",\n"
                + "  \"oneTimePassword\": \"" + token + "\",\n"
                + "  \"pan\": \"" + cardRegistrationReq.getPan() + "\",\n"
                + "  \"reference\": \"" + cardRegistrationReq.getReference() + "\",\n"
                + "  \"terminalId\": \"" + cardRegistrationReq.getTerminalId() + "\",\n"
                + "  \"terminalName\": \"" + cardRegistrationReq.getTerminalName() + "\"\n"
                + "}";

        LOGGER.info("CARD REGISTRATION REQUEST: {}", cardRegJson);
        String response = HttpClientService.sendTxnToAPI(cardRegJson, systemVariables.CARD_REGISTRATION_URL);
        LOGGER.info("CARD REGISTRATION RESPONSE: {}", response);
        if (!response.equalsIgnoreCase("-1")) {
            JSONObject jsonObject = new JSONObject(response);//get PVV DATA FROM POSTILION RESPONSE
            String responseCode = jsonObject.getString("responseCode");
            String message = jsonObject.getString("message");
            int res = -1;
            if (responseCode.equalsIgnoreCase("00") || responseCode.equalsIgnoreCase("0")) {
                try {
                    res = jdbcTemplate.update("UPDATE card set status='C',issued_dt =?,issued_by =? where PAN=?", DateUtil.now(), receivedBy, cardRegistrationReq.getPan());
                    if (res != -1) {
                        result = "{\"result\":\"0\",\"message\":\"Cards Issued Successfully\"}";
                    }
                } catch (Exception e) {
                    LOGGER.info("EXCEPTION OCCURED:{} ", e.getMessage());
                }
            } else if (responseCode.equalsIgnoreCase("14")) {
                //error occurred on core banking
                res = jdbcTemplate.update("UPDATE card set status='C', responseCode='14',issued_dt =?,issued_by =? where PAN=?", DateUtil.now(), receivedBy, cardRegistrationReq.getPan());
            } else {
                result = "{\"result\":\"" + responseCode + "\",\"message\":\"" + message + "\"}";
            }
        }}else{
            result = "{\"result\":\"" + cbsResult + "\",\"message\":\"An error occured during posting charge to cbs\"}";

        }
        return result;

    }
    public UbxResponse issueVisaCardToCoreBanking(CardRegistrationReq cardRegistrationReq, String token, String receivedBy) {
        String result = "{\"result\":\"99\",\"message\":\"A general Error occured during confirming\"}";
        //generate request for card issuance to core banking
        String cardRegJson = "{\n"
                + "  \"accountNo\": \"" + cardRegistrationReq.getAccountNo() + "\",\n"
                + "  \"customerName\": \"" + cardRegistrationReq.getCustomerName() + "\",\n"
                + "  \"customerPhoneNumber\": \"" + cardRegistrationReq.getCustomerPhoneNumber() + "\",\n"
                + "  \"oneTimePassword\": \"" + token + "\",\n"
                + "  \"pan\": \"" + cardRegistrationReq.getPan() + "\",\n"
                + "  \"panExpireDate\": \"" + cardRegistrationReq.getPanExpireDate() + "\",\n"
                + "  \"reference\": \"" + cardRegistrationReq.getReference() + "\",\n"
                + "  \"notify\": \"" + cardRegistrationReq.isNotify() + "\",\n"
                + "  \"terminalId\": \"" + receivedBy + "\",\n"
                + "  \"terminalName\": \"" + cardRegistrationReq.getTerminalName() + "\"\n"
                + "}";

        LOGGER.info("CARD REGISTRATION REQUEST: {}", cardRegJson);
        String response = HttpClientService.sendTxnToAPI(cardRegJson, systemVariables.CARD_REGISTRATION_URL);
        LOGGER.info("CARD REGISTRATION RESPONSE: {}", response);
        if (!response.equalsIgnoreCase("-1")) {
            JSONObject jsonObject = new JSONObject(response);//get PVV DATA FROM POSTILION RESPONSE
            String responseCode = jsonObject.getString("responseCode");
            String message = jsonObject.getString("message");
            return  new UbxResponse(message, responseCode);
        }
        return new UbxResponse("Failed to get Response from CBS","96");

    }

    public String getBranchCardManagementAjax(String branchCode, String statusCode, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String searchQuery = "";
        //uncomissioned TPB
        mainSql = "Select count(*) from card where status=? and originating_branch=?";
        totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{statusCode, branchCode}, Integer.class);
        if (!searchValue.equals("")) {
            searchValue = "'%" + searchValue + "%'";
            //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
            searchQuery = "WHERE  concat(customer_rim_no,' ',phone,' ',account_no,' ',customer_name,' ',PAN) LIKE ? AND status=? and originating_branch=?";
            totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.txnid) FROM cbstransactiosn b" + searchQuery, new Object[]{searchValue, statusCode, branchCode}, Integer.class);
        } else {
            totalRecordwithFilter = totalRecords;
        }
        if (!searchQuery.equals("")) {
            mainSql = "select * from card " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, statusCode, branchCode});

        } else {
            mainSql = "select * from  card where status=? and originating_branch=?  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
            results = this.jdbcTemplate.queryForList(mainSql, new Object[]{statusCode, branchCode});
        }
        //Java objects to JSON string - compact-print - salamu - Pomoja.
        String jsonString = null;
        try {
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;
    }

    /*
    ONLINE VISA CARD REGISTRATION
     */
    public int visaCardRegistration(String accountNo, String phoneNo, String branch) {
        int jsonString = -1;
        CreateCardRequest cardReq = new CreateCardRequest();
        try {
            //get account details on core banking
            List<Map<String, Object>> result = this.jdbcTemplateCbs.queryForList("SELECT * FROM V_CUSTOMER a,V_ACCOUNT_DETAIL b WHERE a.CUST_ID =b.CUST_ID AND (b.ACCT_NO =? OR b.OLD_ACCT_NO =?) AND ROWNUM=1", accountNo, accountNo);

            if (!result.isEmpty()) {
                for (Map<String, Object> customer : result) {
                    LOGGER.info("ACCOUNT NAME:{}", customer.get("ACCT_NM").toString());
                    cardReq.setAccountNo(accountNo);
                    cardReq.setAddress1(customer.get("ADDR_LINE_1").toString());
                    cardReq.setAddress2(customer.get("ADDR_LINE_2").toString());
                    cardReq.setAddress3(customer.get("ADDR_LINE_3").toString());
                    cardReq.setAddress4(customer.get("ADDR_LINE_4").toString());
                    cardReq.setBranchId(customer.get("MAIN_BRANCH_ID").toString());
                    cardReq.setCity(customer.get("CITY").toString());
                    cardReq.setCreatedBy(phoneNo);
                    cardReq.setCustCategory(customer.get("CUST_CAT_DESC").toString());
                    cardReq.setCustShortName(customer.get("CUST_SHORT_NM").toString());
                    cardReq.setCustomerId(customer.get("CUST_ID").toString());
                    cardReq.setCustomerRim(customer.get("CUST_NO").toString());
                    cardReq.setOriginatingBranch(customer.get("BU_NO").toString());
                    cardReq.setPhoneNumber(phoneNo);
                    cardReq.setRecruitingBrach(customer.get("BU_NO").toString());
                    cardReq.setReference(customer.get("BU_NO") + "VIS" + DateUtil.now("yyyyMMddhhmmss"));
                    cardReq.setState(customer.get("STATE").toString());
                    cardReq.setCustomerName(customer.get("FIRST_NM") + " " + customer.get("LAST_NM"));
                    String originBranch = cardReq.getOriginatingBranch();
                    String collectBranch = cardReq.getRecruitingBrach();
                    if (branch != null) {
                        originBranch = branch;
                        collectBranch = branch;
                    }
                    if (cardReq.getCustomerName() != null && cardReq.getCustomerName().length() > 20) {
                        jsonString = 845;
                        LOGGER.info(cardReq.getCustomerName() + " length is greater  20 characters");
                    } else {
                        jsonString = jdbcTemplate.update("INSERT INTO card(account_no, customer_name, custid, customer_rim_no, reference, customer_shortName, customer_category, address1, address2, address3, address4, status, PAN, stage, created_by,originating_branch,collecting_branch,phone,approver1,approver1_dt) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                cardReq.getAccountNo(), cardReq.getCustomerName(), cardReq.getCustomerId(), cardReq.getCustomerRim(), cardReq.getReference(), cardReq.getCustShortName(), cardReq.getCustCategory(), cardReq.getAddress1(), cardReq.getAddress2(), cardReq.getAddress3(), cardReq.getAddress4(), "P", cardReq.getCustomerRim(), "1", cardReq.getCreatedBy(), originBranch, collectBranch, cardReq.getPhoneNumber(), "SYSTEM", DateUtil.now());
                    }
                    break;
                }

            }
            return jsonString;

        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING ACCOUNT DETAILS: {}", e.getMessage());
            return -1;
        }
    }

    public List<Map<String, Object>> getCardExists(String acctNo) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {

            String sql = "select * from card where allow_multiple='N' and account_no =?";
            result = this.jdbcTemplate.queryForList(sql, acctNo);
        } catch (Exception e) {
            LOGGER.info("getCardExists: {}", e.getMessage());
        }
        return result;
    }


    public String updateCardCollectingBranch(String cardPan, String branchCode, String cardReference) {
        String finalResponse = null;
        try {
            String sql = "UPDATE card SET collecting_branch=? WHERE  reference=?";
            int check = this.jdbcTemplate.update(sql, new Object[]{branchCode, cardReference});
            LOGGER.info("The final result for updating card collecting branch is: ..{}", check);
            if (check == 1) {
                finalResponse = "SUCCESS";
            } else {
                finalResponse = "FAIL";
            }
        } catch (DataAccessException e) {
            LOGGER.error("Data access exception...{}", e.getMessage());
        }

        return finalResponse;
    }

    public String updateCustomerContact(String customerPhone, String cardPan, String cardReference) {
        String finalResponse = null;
        try {
            String sql = "UPDATE card SET phone=? WHERE PAN=? AND reference=?";
//            LOGGER.info(sql.replace("?","{}"),customerPhone,cardPan);
            int check = this.jdbcTemplate.update(sql, new Object[]{customerPhone, cardPan, cardReference});
            LOGGER.info("The final result for updating customer contact is: ..{}", check);
            if (check == 1) {
                finalResponse = "SUCCESS";
            } else {
                finalResponse = "FAIL";
            }
        } catch (DataAccessException e) {
            LOGGER.error("Data access exception...{}", e.getMessage());
        }

        return finalResponse;
    }

    public String updateCardPanAjax(String cardReference, String cardPan, String newCardPan) {
        String finalResponse = null;
        try {
            String sql = "UPDATE card SET PAN=? WHERE PAN=? AND reference=? ";
            int check = this.jdbcTemplate.update(sql, new Object[]{newCardPan, cardPan, cardReference});
            LOGGER.info("The final result for updating customer contact is: ..{}", check);
            if (check == 1) {
                finalResponse = "SUCCESS";
            } else {
                finalResponse = "FAIL";
            }
        } catch (DataAccessException e) {
            LOGGER.error("Data access exception...{}", e.getMessage());
        }

        return finalResponse;
    }

    public String rejectCard(String cardPan, String message, String cardReference) {
        String finalResponse = null;
        try {
            String sql = "UPDATE card SET status='R',reject_reason=? WHERE PAN=? AND reference=?";
//            LOGGER.info("sql..{}", sql.replace("?","{}"),message,cardPan);
            int check = this.jdbcTemplate.update(sql, new Object[]{message, cardPan, cardReference});
            LOGGER.info("The final result for REJECTING CARD IS: ..{}", check);
            if (check == 1) {
                finalResponse = "SUCCESS";
            } else {
                finalResponse = "Failed to reject card";
            }
        } catch (DataAccessException e) {
            finalResponse = "Technical error";
            LOGGER.error("Data access exception...{}", e.getMessage());
        }

        return finalResponse;
    }

    public boolean checkCardIsAllowed(String reference, String accountNo) {
        boolean finalResponse = false;
        try {
            String sql = "SELECT is_allowed FROM card WHERE reference=? AND account_no=?";
            LOGGER.info("Selecting is_allowed status using reference and account_no: {} ..{}", reference, accountNo);
            String check = this.jdbcTemplate.queryForObject(sql, new Object[]{reference, accountNo}, String.class);
            LOGGER.info("The CARD status Y means allowed: ..{}", check);
            if (check.equalsIgnoreCase("Y")) {
                finalResponse = true;
            } else {
                finalResponse = false;
            }
        } catch (DataAccessException e) {
            LOGGER.error("Data access exception...{}", e.getMessage(),e);
        }

        return finalResponse;
    }

    public boolean isVisaAlreadyCharge(String reference) {
        boolean finalResponse = false;
        try {
            String sql = "SELECT charge FROM card WHERE reference=?";
            LOGGER.info(sql, reference);
            Integer check = this.jdbcTemplate.queryForObject(sql, new Object[]{reference}, Integer.class);
            LOGGER.info("The CARD charge : ..{}", check);
            if (check > 0) {
                finalResponse = true;
            } else {
                finalResponse = false;
            }
        } catch (DataAccessException e) {
            LOGGER.error("Data access exception...", e.getMessage());
        }
        return finalResponse;
    }

    public List<Map<String, Object>> fireSolveVisaCardIssuesAjax(String serviceCode, String fromDate, String toDate) {
        List<Map<String, Object>> results = null;
        String mainSql;
        mainSql = "select * from visa_card_issues_tracker where service_type=? and (date(service_log_date)>=? and date(service_log_date)<=?) and action_status='ACTIVE'";
//                LOGGER.info(mainSql.replace("?","'{}'"),serviceCode,fromDate,toDate);
        results = this.jdbcTemplate.queryForList(mainSql, new Object[]{serviceCode, fromDate, toDate});

        return results;
    }

    public List<Map<String, Object>> fireCardDetailsCardSide(String accountNo, String rimNo, String serviceType) {
        List<Map<String, Object>> finalResp = null;
        String cardStatus = "CD";
        if (serviceType.equalsIgnoreCase("VISA_PIN_RESET")) {
            cardStatus = "C";
        }
        try {
            String sql = "SELECT * FROM card WHERE account_no=? AND customer_rim_no=? AND status=?";
            finalResp = jdbcTemplate.queryForList(sql, new Object[]{accountNo, rimNo, cardStatus});
        } catch (DataAccessException dae) {
            LOGGER.info("database access exception... {}", dae);
        }
        return finalResp;
    }

    public List<Map<String, Object>> getcardDetailsForPinReset(String accountNo) {
        List<Map<String, Object>> finalResp = null;
        try {
            String sql = "SELECT customer_rim_no, account_no, customer_shortName, PAN FROM card WHERE account_no=? AND status='C' ORDER BY id DESC limit 1";
            LOGGER.info(sql.replace("?", "'{}'"), accountNo);
            finalResp = jdbcTemplate.queryForList(sql, new Object[]{accountNo});
        } catch (DataAccessException dae) {
            LOGGER.info("database access exception... {}", dae);
        }
        return finalResp;
    }

    public Object fireCardDetailsRequestSide(String reference, String accountNo, String rimNo) {
        List<Map<String, Object>> finalResp = null;
        try {
            String sql = "SELECT * FROM visa_card_issues_tracker WHERE reference=? AND account_no=? AND customer_rim_no=? AND action_status='ACTIVE'";
            finalResp = jdbcTemplate.queryForList(sql, new Object[]{reference, accountNo, rimNo});
        } catch (DataAccessException dae) {
            LOGGER.info("database access exception request side... {}", dae);
        }
        return finalResp;
    }

    public List<Map<String, Object>> fireGetCardDetailsAjax(String accountNo) {
        String mainSql = "select c.*, (select name from branches b where b.code=c.originating_branch) as ob_name, (select name from branches b where b.code=c.collecting_branch) as cb_name from card c where account_no = ?";
        return this.jdbcTemplate.queryForList(mainSql, new Object[]{accountNo});
    }

    public String fireStatelessVisaCardUpdatingAjax(String accountNo, String selectedValue, String enteredValue, String cardReference) {
        String finalResponse = "Failed";
        String sql = null;
        try {
            if (selectedValue.equalsIgnoreCase("allow_multiple")) {
                sql = "UPDATE card SET allow_multiple='Y' WHERE account_no='" + accountNo + "'";
            } else if (selectedValue.equalsIgnoreCase("block_multiple")) {
                sql = "UPDATE card SET allow_multiple='N' WHERE account_no='" + accountNo + "'";
            } else {
                sql = "UPDATE card SET " + selectedValue + "='" + enteredValue + "' WHERE reference='" + cardReference + "'";
            }
            LOGGER.info("sql check... {}",sql);
            int check = this.jdbcTemplate.update(sql);
            LOGGER.info("The final result for updating visa card details: ..{}, action tiggerd .. {}, corresponding entered value ... {}", check, selectedValue, enteredValue);
            if (check >= 1) {
                finalResponse = "SUCCESS";
            } else {
                finalResponse = "FAIL, Failed to update card details";
            }
        } catch (DataAccessException e) {
            LOGGER.error("Data access exception...{}", e.getMessage());
        }

        return finalResponse;
    }


    public String fireAmendVisaCardAjax(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        String loggedInnUser = httpSession.getAttribute("username").toString();
        //get report for HQ/IBD USER
        try {
            mainSql = "SELECT count(*) FROM card where originating_branch=? and status='RA' and stage='0'";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE  concat(reference,' ', account_no,' ', customer_rim_no,' ', custid,' ', customer_shortName,' ', customer_category,' ', address1,' ', address2,' ', address3,' ', address4,' ', city,' ', state,' ', phone,' ', account_branch_id,' ', created_by) LIKE ? AND originating_branch=? and created_by='" + loggedInnUser + "' and status='RA' and stage='0'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM card b" + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from card b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, branchNo);
                LOGGER.info("Datatable Response: {}", results);
            } else {
                mainSql = "select * from card where originating_branch=? and status='RA' and  stage='0' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
            }

            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }

        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;

    }

    public int updateVisaCardRequest(AmendVisaCardForm cardReq) {
        Integer res = -1;
        try {
            String sql = "update card set status='I', stage='0', collecting_branch=?,phone=?,email=? where reference=? and account_no=?";
            res = jdbcTemplate.update(sql, cardReq.getRecruitingBrach(), cardReq.getPhoneNumber(), cardReq.getCustomerEmail(), cardReq.getReference(), cardReq.getAccountNo());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public void deleteVisaSupportingDoc(String reference) {
        try {
            jdbcTemplate.update("DELETE FROM ib_documents WHERE reference=?", reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON DELETING SUPPORTING DOCUMENT: {}", e.getMessage());
        }
    }

    public void updateChecksum(String accountNo, String genChecksum) {
        try {
            LOGGER.info("UPDATE visa_card_issues_tracker SET check_sum =?  WHERE account_no=?".replace("?", "'{}'"), genChecksum, accountNo);
            jdbcTemplate.update("UPDATE visa_card_issues_tracker SET check_sum =?  WHERE account_no=?", genChecksum, accountNo);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON UPDATING CHECKSUM {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> checkVisaCardFailure(String accountNo) {
        String query = "SELECT * FROM visa_card_issues_tracker WHERE account_no=?";
        return jdbcTemplate.queryForList(query, accountNo);
    }

    public List<Map<String, Object>> getVisaCardChargeReport(String fromDate, String toDate) {
        List<Map<String, Object>> results = null;
        String ledger = "1-***-00-4016-4016007";
        String mainSql = "select c.account_no as account,c.PAN as pan,c.customer_name,b.name as collecting_branch,REPLACE('1-***-00-4016-4016007','***',c.collecting_branch) as gl_account, '' as gl_name, c.reference,c.charge as amount from card c left join branches b on b.code = c.collecting_branch where (date(c.approver2_dt)>=? and date(c.approver2_dt)<=?)";
        LOGGER.info(mainSql.replace("?","'{}'"),fromDate, toDate);
                results = this.jdbcTemplate.queryForList(mainSql, fromDate, toDate);

        return results;
    }

}
