/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.GeneralJsonResponse;
import com.DTO.IBANK.AddAccountToIBProfile;
import com.DTO.IBANK.AddIbankProfile;
import com.DTO.IBANK.AddIbankSignatories;
import com.DTO.auth.UserModal;
import com.DTO.recon.Requests.GeneralReconConfig;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.MaiString;
import com.models.Customer;
import com.models.IBUserDetails;
import com.queue.QueueProducer;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.service.HttpClientService;
import com.service.XMLParserService;
import com.sms.SmsBody;
import com.sms.SmsConstants;
import com.sms.SmsService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Melleji.Mollel
 */
@Repository
public class IbankRepo {

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
    PasswordEncoder passwordEncoder;

    @Autowired
    QueueProducer queueProducer;

//    @Autowired
//    ISO20022Service iso20022Service;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;

    @Autowired
    BanksRepo banksRepo;
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    @Qualifier("tpbonline")
    JdbcTemplate jdbcTPBONLINETemplate;

    @Autowired
    SmsService smsService;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IbankRepo.class);

    /*
    GET IBANK dashboard
     */
    public List<Map<String, Object>> getIbankModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            LOGGER.info("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?".replace("?", "'{}'"), moduleURL, roleId);
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);

        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK MODULES: {}", e.getMessage());
        }
        return result;
    }

    /*
    get Customer details from core banking
     */
    public String getCustomerDetails(String accountNo) {
        List<Map<String, Object>> result = null;
        String jsonString = null;
        try {
            result = this.jdbcRUBIKONTemplate.queryForList("SELECT * FROM V_CUSTOMER a, V_ACCOUNT_DETAIL b WHERE a.CUST_ID =b.CUST_ID AND b.ACCT_NO =?", accountNo);
            jsonString = this.jacksonMapper.writeValueAsString(result);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK MODULES: {}", e.getMessage());
        }
        return jsonString;
    }

    /*
    get Customer details from core banking
     */
    public String getAccountDetails(String accountNo) {
        String jsonString = null;
        try {
            String sql = "SELECT * FROM V_ACCOUNTS va WHERE va.ACCT_NO =? OR OLD_ACCT_NO =?";
            List<Map<String, Object>> result = this.jdbcRUBIKONTemplate.queryForList(sql, accountNo, accountNo);
            jsonString = this.jacksonMapper.writeValueAsString(result);
            LOGGER.info("GETTING USER : {},  ACCOUNT: {}", sql, accountNo);
            LOGGER.info("RESULTS : {}", jsonString);
            return jsonString;

        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING ACCOUNT DETAILS: {}", e.getMessage());
            return null;
        }
    }

    public Customer getAccDetailsByRim(String rimNumber) {
        Customer customer = new Customer();
        try {
            return jdbcRUBIKONTemplate.queryForObject("SELECT FIRST_NM,MIDDLE_NM,LAST_NM,CUST_NM,CUST_NO FROM V_CUSTOMER vc WHERE vc.CUST_NO =?", new Object[]{rimNumber}, new RowMapper<Customer>() {
                @Override
                public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Customer customer = new Customer();
                    customer.setResponseCode("0");
                    customer.setFirstName(rs.getString("FIRST_NM"));
                    customer.setMiddleName(rs.getString("MIDDLE_NM"));
                    customer.setLastName(rs.getString("LAST_NM"));
                    customer.setFullName(rs.getString("CUST_NM"));
                    customer.setRimNumber(rs.getString("CUST_NO"));
                    customer.setCustomerId(rs.getString("CUST_NO"));
                    return customer;
                }
            });

        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING ACCOUNT DETAILS: {}", e.getMessage());
            customer.setResponseCode("99");
            return customer;
        }
    }

    public List<Map<String, Object>> getCustomerAccountDetails(String accountNo) {
        List<Map<String, Object>> result = null;
        LOGGER.info("GOING TO QUERY CUSTOMER DETAILS USING ACCOUNT NO: {}", accountNo);
        try {
            String sql = "SELECT * FROM V_CUSTOMER a JOIN V_ACCOUNT_DETAIL b ON a.CUST_ID =b.CUST_ID WHERE (b.ACCT_NO =? OR b.OLD_ACCT_NO =?)";
            result = this.jdbcRUBIKONTemplate.queryForList(sql, accountNo, accountNo);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING ACCOUNT DETAILS: {}", e.getMessage());
        }
        LOGGER.info("IBANK ACCOUNT DETAILS FOR PROFILE CREATION...:{}", result);
        return result;
    }

    public List<Map<String, Object>> getCustomerAccountsByRIM(String custRIM) {
//        SELECT * V_ACCOUNTS va WHERE va.CUST_NO ='0000412279' AND REC_ST ='A' AND PROD_ID NOT IN (170,190,200,220)
        List<Map<String, Object>> result = null;
        String jsonString = null;
        try {
            result = this.jdbcRUBIKONTemplate.queryForList("SELECT * FROM V_ACCOUNTS va WHERE va.CUST_NO =? AND PROD_ID NOT IN (170,190,200,220)", custRIM);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING ACCOUNT DETAILS: {}", e.getMessage());
        }
        System.out.println("ACCOUNT DETAILS:" + jsonString);
        return result;
    }

    public List<Map<String, Object>> branches() {
        try {
            return this.jdbcTemplate.queryForList("SELECT * from branches");
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public String getIBClientProfiles(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(*) FROM ib_profiles where recruiting_branch_code=? and status='I'";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ', account,' ', i.name,' ', rim_no,' ', category,' ', address1,' ', city,' ', created_by) LIKE ? AND recruiting_branch_code=? and status='I'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(i.id) FROM ib_profiles i" + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select b.name as branch,i.reference,i.account,i.name,i.rim_no,i.custid" + ",i.category,i.address1,i.city,i.created_by,i.create_dt,i.status " + "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, branchNo);
                LOGGER.info("Datatable Response: {}", results);

            } else {
                mainSql = "select b.name as branch,i.reference,i.account,i.name,i.rim_no,i.custid" + ",i.category,i.address1,i.city,i.created_by,i.create_dt,i.status " + "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code where i.recruiting_branch_code=?  and i.status='I' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
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

    public Integer saveIBClientProfile(AddIbankProfile profileData, String reference, String createdBy) {
        Integer res = -1;
        String initialStatus = "I";
        LOGGER.info("GOING TO SAVE IB PROFILES: {} TRACKING REFERENCE: {}", profileData, reference);
        try {
            String sql = "INSERT INTO ib_profiles(reference,account,name,rim_no,custid,short_name,category,address1,address2,address3,address4,city," + "state,email,account_branch_id,recruiting_branch_code,recruited_by,status,created_by,comments,mandate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update reference=?";
            res = jdbcTemplate.update(sql, reference, profileData.getAccountNo(), profileData.getCustomerName(), profileData.getCustomerRim(), profileData.getCustomerId(), profileData.getCustShortName(), profileData.getCustCategory(), profileData.getAddress1(), profileData.getAddress2(), profileData.getAddress3(), profileData.getAddress4(), profileData.getCity(), profileData.getState(), profileData.getEmail(), profileData.getBranchId(), profileData.getRecruitingBrach(), createdBy, "I", createdBy, profileData.getComments(), profileData.getMandate(), reference);
            //LOGGER.info(sql.replace("?", "'{}'"), reference, profileData.getAccountNo(), profileData.getCustomerName(), profileData.getCustomerRim(), profileData.getCustomerId(), profileData.getCustShortName(), profileData.getCustCategory(), profileData.getAddress1(), profileData.getAddress2(), profileData.getAddress3(), profileData.getAddress4(), profileData.getCity(), profileData.getState(), profileData.getEmail(), profileData.getBranchId(), profileData.getRecruitingBrach(), createdBy, "I", createdBy, profileData.getComments(), profileData.getMandate(), reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer tqsUpdateMkoba(String txnid, String receipt, String desc) {
        Integer res = -1;
        String initialStatus = "I";
        LOGGER.info("mkoba tqs: {} TRACKING REFERENCE: {}", txnid, receipt);
        try {
            String sql = "update cbstransactiosn set thirdparty_reference=?,docode_desc= ? where txnid=?";
            res = jdbcTemplate.update(sql, receipt, desc, txnid);
            LOGGER.info(sql.replace("?", "'{}'"), receipt, desc, txnid);
            LOGGER.info("mkoba tqs update cbs: {} TRACKING REFERENCE: {} result:{}", txnid, receipt, res);

            //sql = "update thirdpartytxns set receiptNo=? where txnid=?";
            // LOGGER.info(sql.replace("?", "'{}'"), receipt, txnid);

            // 1788 kihura
            // 1471
            // res = jdbcTemplate.update(sql, receipt, txnid);
            //  LOGGER.info("{} mkoba tqs update cbs: {} TRACKING REFERENCE: {} result:{}", sql, txnid, receipt, res);

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveIBClientServiceRequest(String username, AddIbankProfile profileData, String reference, String createdBy, String serviceName) {
        Integer res = -1;
        String initialStatus = "I";
        try {
            res = jdbcTemplate.update("INSERT INTO ib_self_services_requests(username,reference,account,name,rim_no,custid,short_name,category,address1,address2,address3,address4,city,state,email,account_branch_id,recruiting_branch_code,recruited_by,status,requested_by,comments,mandate,service_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update reference=?", username, reference, profileData.getAccountNo(), profileData.getCustomerName(), profileData.getCustomerRim(), profileData.getCustomerId(), profileData.getCustShortName(), profileData.getCustCategory(), profileData.getAddress1(), profileData.getAddress2(), profileData.getAddress3(), profileData.getAddress4(), profileData.getCity(), profileData.getState(), profileData.getEmail(), profileData.getBranchId(), profileData.getRecruitingBrach(), profileData.getPfNo(), "I", createdBy, profileData.getComments(), profileData.getMandate(), serviceName, reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR IN REGISTRATION OF POPOTE MOBILE:...::", e.getMessage());
            res = -1;
        }
        return res;
    }

    /*
    INSERT SUPPORTING DOCUMENT
     */
    public Integer saveSupportingDocuments(String reference, MultipartFile file, String createdBy) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("INSERT INTO ib_documents(reference,document,fileName,created_by) VALUES (?,?,?,?)", reference, file.getBytes(), file.getOriginalFilename(), createdBy);
            LOGGER.info("INSERTING FILE :{} FILE NAME: {} SIZE: {}", result, file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON INSERTING SUPPORTING DOCUMENT: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    /*
            ADD ACCOUNT TO INTERNET BANKING PROFILE
     */
    public Integer saveIBClientProfileAccounts(AddAccountToIBProfile profileData, String reference, String createdBy) {
        Integer res = -1;
        String initialStatus = "I";
        try {
            res = jdbcTemplate.update("INSERT INTO ib_profile_accounts(profile_reference,account_name, account_no, old_account_no, account_currency, account_prod_code, account_type, account_category, account_status, account_limit, limit_without_approval, created_by, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update profile_reference=?", reference, profileData.getAccountName(), profileData.getAccountNo(), profileData.getOldAccount(), profileData.getAccountCurrency(), profileData.getAcctProdCode(), profileData.getAcctDescription(), profileData.getAcctCategory(), profileData.getAcctStatus(), profileData.getAcctLimit(), profileData.getLimitWithoutApproval(), createdBy, "I", reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public String getAccountsAttchedToIBProfile(String profileReference, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        LOGGER.info("PROFILE REFERENCE: " + profileReference);
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(*) FROM ib_profile_accounts where profile_reference=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{profileReference}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(profile_reference,' ', account_no,' ', old_account_no,' ', account_currency,' ', account_prod_code,' ', account_type,' ', account_category,' ', account_status,' ', account_limit,' ', limit_without_approval,' ', created_by,' ', modified_dt,' ', status) LIKE ? AND profile_reference=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM ib_profile_accounts b" + searchQuery, new Object[]{searchValue, profileReference}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from ib_profile_accounts  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, profileReference);
            } else {
                mainSql = "select * from ib_profile_accounts where profile_reference=?   ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), profileReference);
                results = this.jdbcTemplate.queryForList(mainSql, profileReference);
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

//    public List<Map<String, Object>> getAccountsAttachedToIBProfile(String profileReference) {
//        String mainSql = "select * from ib_profile_accounts where profile_reference=?";
//        return this.jdbcTemplate.queryForList(mainSql, profileReference);
//    }

    public List<Map<String, Object>> getIBRoles(String category) {
        List<Map<String, Object>> result = null;
        if (category.equalsIgnoreCase("PER")) {
            try {
                result = this.jdbcTemplate.queryForList("select * from ib_roles where category=?", category);
            } catch (Exception e) {
                LOGGER.info("ERROR ON GETTING IB ROLES: {}", e.getMessage());
            }
        } else {
            try {
                result = this.jdbcTemplate.queryForList("select * from ib_roles where category<>'PER'");
            } catch (Exception e) {
                LOGGER.info("ERROR ON GETTING IB ROLES: {}", e.getMessage());
            }
        }

        return result;
    }

    public List<Map<String, Object>> getIBProfileAccounts(String profile_reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select * from ib_profile_accounts where profile_reference=?", profile_reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IB ROLES: {}", e.getMessage());
        }

        return result;
    }

    /*
     * ADD SIGNATORY TO IBANK
     */
    public Integer saveIbankSignatory(AddIbankSignatories signatoryData, String reference, String createdBy, List<String> role, List<String> transferAccess, List<String> viewAccess, List<String> accountsLimit) {
        Integer res = -1;
        String initialStatus = "I";
        try {
            int signatoryId = 0;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("INSERT INTO ib_signatories(profile_reference, cust_rim_no, fullname, username, phone, email_address, created_by)  VALUES(?,?,?,?,?,?,?)", new String[]{"id"});
                    ps.setString(1, reference);
                    ps.setString(2, signatoryData.getCustomerId());
                    ps.setString(3, signatoryData.getFullName());
                    ps.setString(4, signatoryData.getUsername());
                    ps.setString(5, signatoryData.getPhoneNumber());
                    ps.setString(6, signatoryData.getEmail());
                    ps.setString(7, createdBy);
                    return ps;
                }
            }, keyHolder);
            signatoryId = keyHolder.getKey().intValue();
            for (int i = 0; i < role.size(); i++) {
                res = jdbcTemplate.update("insert into ib_signatories_roles(role_id,signatory_id,profile_reference,created_by) values(?,?,?,?)", role.get(i), signatoryId, reference, createdBy);
            }
            for (int i = 0; i < viewAccess.size(); i++) {
                LOGGER.info("VIEW ACCESS: ACCOUNT ID:{} account limit: {}", viewAccess.get(i), accountsLimit.get(i));
                res = jdbcTemplate.update("insert into ib_user_account_access(account_id,signatory_id,transfer_access,view_access,account_limit,profile_reference,created_by) values(?,?,?,?,?,?,?)", viewAccess.get(i), signatoryId, "I", "A", accountsLimit.get(i), reference, createdBy);

            }
            if (transferAccess != null) {
                for (int i = 0; i < transferAccess.size(); i++) {
                    res = jdbcTemplate.update("update ib_user_account_access set transfer_access='A' where account_id=?", transferAccess.get(i).split("==")[0]);
                }
            }
            //seen
            for (String account : accountsLimit) {

            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public Integer saveIbankIbankSignatoryMobile(AddIbankSignatories signatoryData, String reference, String createdBy, String accountNo) {
        Integer res = -1;
        String initialStatus = "I";
        try {
            int signatoryId = 0;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update("delete from ib_signatories where cust_rim_no=?", signatoryData.getCustomerId());
            jdbcTemplate.update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("INSERT INTO ib_signatories(profile_reference, cust_rim_no, fullname, username, phone, email_address, created_by)  VALUES(?,?,?,?,?,?,?) on duplicate key update profile_reference=?", new String[]{"id"});
                    ps.setString(1, reference);
                    ps.setString(2, signatoryData.getCustomerId());
                    ps.setString(3, signatoryData.getFullName());
                    ps.setString(4, signatoryData.getUsername());
                    ps.setString(5, signatoryData.getPhoneNumber());
                    ps.setString(6, signatoryData.getEmail());
                    ps.setString(7, createdBy);
                    ps.setString(8, reference);
                    return ps;
                }
            }, keyHolder);
            signatoryId = keyHolder.getKey().intValue();
            res = jdbcTemplate.update("insert into ib_signatories_roles(role_id,signatory_id,profile_reference,created_by) values(?,?,?,?)", "5", signatoryId, reference, createdBy);
//get account lists from local
            List<Map<String, Object>> accounts = this.jdbcTemplate.queryForList("SELECT * FROM ib_profile_accounts where profile_reference=?", reference);
            for (int i = 0; i < accounts.size(); i++) {
                if (accountNo.equalsIgnoreCase(accounts.get(i).get("account_no").toString()) || accountNo.equalsIgnoreCase(accounts.get(i).get("old_account_no").toString())) {
                    res = jdbcTemplate.update("insert into ib_user_account_access(account_id,signatory_id,transfer_access,view_access,account_limit,profile_reference,created_by) values(?,?,?,?,?,?,?)", accounts.get(i).get("id"), signatoryId, "A", "A", "30000000", reference, createdBy);
                    LOGGER.info("SIGNATORY IS SUCCEFFULY ADDED TO : PROFILE_REFERENCE={}, SIGNATORY NAME:{},USERNAME:{},TRANSFER ACCESS:A,VIEW ACCESS:A", reference, signatoryData.getFullName(), signatoryData.getUsername());

                } else {
                    LOGGER.info("SIGNATORY IS SUCCEFFULY ADDED TO : PROFILE_REFERENCE={}, SIGNATORY NAME:{},USERNAME:{},TRANSFER ACCESS:I,VIEW ACCESS:A", reference, signatoryData.getFullName(), signatoryData.getUsername());
                    res = jdbcTemplate.update("insert into ib_user_account_access(account_id,signatory_id,transfer_access,view_access,account_limit,profile_reference,created_by) values(?,?,?,?,?,?,?)", accounts.get(i).get("id"), signatoryId, "I", "A", "0", reference, createdBy);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage());
            res = -1;
        }
        return res;
    }

    public String getSignatoryAjax(String profileReference, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        LOGGER.info("PROFILE REFERENCE: " + profileReference);
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(*) FROM ib_signatories where profile_reference=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{profileReference}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(profile_reference,' ', cust_rim_no,' ', fullname,' ', username,' ', phone,' ', email_address,' ', create_dt,' ', created_by) LIKE ? AND profile_reference=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM ib_profile_accounts b" + searchQuery, new Object[]{searchValue, profileReference}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from ib_signatories  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, profileReference);
            } else {
                mainSql = "select * from ib_signatories where profile_reference=?   ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), profileReference);
                results = this.jdbcTemplate.queryForList(mainSql, profileReference);
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

    public String getSignatoryAccountAccessAjax(String profileReference, String signatoryId, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        LOGGER.info("PROFILE REFERENCE: " + profileReference);
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(a.account_no) FROM ib_profile_accounts a inner join ib_user_account_access b on a.id=b.account_id where a.profile_reference=? and b.signatory_id=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{profileReference, signatoryId}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(profile_reference,' ', cust_rim_no,' ', fullname,' ', username,' ', phone,' ', email_address,' ', create_dt,' ', created_by) LIKE ? AND a.profile_reference=? and b.signatory_id=? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(a.id) FROM ib_profile_accounts a inner join ib_user_account_access b on a.id=b.account_id " + searchQuery, new Object[]{searchValue, profileReference, signatoryId}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.*,b.account_limit signatoryLimit,b.transfer_access,b.view_access from ib_profile_accounts a inner join ib_user_account_access b on a.id=b.account_id   " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, profileReference, signatoryId);
            } else {
                mainSql = "select a.*,b.account_limit signatoryLimit,b.transfer_access,b.view_access from ib_profile_accounts a inner join ib_user_account_access b on a.id=b.account_id where a.profile_reference=?   and b.signatory_id=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), profileReference, signatoryId);
                results = this.jdbcTemplate.queryForList(mainSql, profileReference, signatoryId);
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

    public String getSignatoryRoleAccessAjax(String profileReference, String signatoryId, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        LOGGER.info("PROFILE REFERENCE: " + profileReference);
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(a.id) FROM ib_signatories a inner join ib_signatories_roles b on a.id=b.signatory_id INNER JOIN ib_roles c on c.id=b.role_id where a.profile_reference=? and b.signatory_id=? and b.profile_reference=a.profile_reference";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{profileReference, signatoryId}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(fullname,' ', username) LIKE ? AND a.profile_reference=? and b.signatory_id=?  and a.profile_reference=b.profile_reference";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(a.id) FROM ib_signatories a inner join ib_signatories_roles b on a.id=b.signatory_id INNER JOIN ib_roles c on c.id=b.role_id " + searchQuery, new Object[]{searchValue, profileReference, signatoryId}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.*,c.* from ib_signatories a inner join ib_signatories_roles b on a.id=b.signatory_id INNER JOIN ib_roles c on c.id=b.role_id  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, searchValue, profileReference, signatoryId);
            } else {
                mainSql = "select a.*,c.* from ib_signatories a inner join ib_signatories_roles b on a.id=b.signatory_id INNER JOIN ib_roles c on c.id=b.role_id where a.profile_reference=?   and b.signatory_id=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), profileReference, signatoryId);
                results = this.jdbcTemplate.queryForList(mainSql, profileReference, signatoryId);
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

    public Integer initiatorSubmitProfileForApproval(String reference) {
        Integer result = -1;
        try {
            LOGGER.info("reference:  ", reference);
            result = jdbcTemplate.update("UPDATE ib_profiles set status='P' WHERE reference=? ", reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON UPDATING IBANK PROFILE: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public Integer branchApprovalToHq(String reference, String approverId, Timestamp approvedDate) {
        Integer result = -1;
        try {
            LOGGER.info("reference:  {}, approved by: {}, on {}", reference, approverId, approvedDate);
            result = jdbcTemplate.update("UPDATE ib_profiles set status='PHQ', branch_approved_by=?,branch_approved_date=? WHERE reference=? ", approverId, approvedDate, reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON APPROVING IBANK PROFILE: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public Integer finalizeIBRegistration(String reference) {
        Integer result = -1;
        try {
            result = jdbcTemplate.update("UPDATE ib_profiles set status='C' WHERE reference=? ", reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON UPDATING IBANK PROFILE: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public String getIbankProfilesPendingAtBranch(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "SELECT count(*) FROM ib_profiles where recruiting_branch_code=? and status='P'";
            LOGGER.info(mainSql.replace("?", "'{}'"), branchNo);

//            LOGGER.info(mainSql.replace("?","'{}'"));
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(reference,' ', account,' ', i.name,' ', rim_no,' ', category,' ', address1,' ', city,' ', created_by) LIKE ? AND recruiting_branch_code=? and status='P'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(i.id) FROM ib_profiles i" + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select b.name as branch,i.reference,i.account,i.name,i.rim_no,i.custid," + "i.category,i.address1,i.city,i.created_by,i.create_dt,i.status " + "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);

                results = this.jdbcTemplate.queryForList(mainSql, searchValue, branchNo);
                LOGGER.info("Datatable Response: {}", results);

            } else {
                mainSql = "select b.name as branch,i.reference,i.account,i.name,i.rim_no,i.custid" + ",i.category,i.address1,i.city,i.created_by,i.create_dt,i.status " + "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code where i.recruiting_branch_code=?  and i.status='P' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;

                LOGGER.info(mainSql.replace("?", "'{}'"), branchNo);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
                LOGGER.info("Datatable Response: {}", results);
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

    /*
    GET IBANK PROFILE SUPPORTING DOCUMENT
     */
    public byte[] getIbankProfileSupportingDocument(String ref) {
        byte[] result = null;
        try {
            result = (byte[]) this.jdbcTemplate.queryForObject("select document  FROM ib_documents where reference=? limit 1", new Object[]{ref}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public List<Map<String, Object>> getIbankProfile(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM ib_profiles where reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK PROFILE: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getIbankProfileAccountsList(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM ib_profile_accounts where profile_reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK PROFILE: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getIbankProfileSignatoriesList(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM ib_signatories where profile_reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK PROFILE: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getIbankProfileSignatoriesAccountPerminssions(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT fullname,account_no,transfer_access,view_access,c.account_limit FROM ib_signatories a inner join ib_profile_accounts b on b.profile_reference=a.profile_reference inner join ib_user_account_access c on c.account_id=b.id where c.signatory_id=a.id and  a.profile_reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK PROFILE: {}", e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getIbankProfileSignatoriesRolePerminssions(String reference) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("SELECT fullname signatoryName,c.name FROM ib_signatories a INNER JOIN ib_signatories_roles b on b.signatory_id=a.id INNER JOIN ib_roles c on c.id=b.role_id where a.profile_reference=?", reference);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING IBANK PROFILE: {}", e.getMessage());
        }
        return result;
    }

    public String generateIBankRegistrationXML(String reference) {
        List<Map<String, Object>> result = null;
        List<Map<String, Object>> result2 = null;
        String regXML = "<registrationRequest>";

        try {
            result = this.jdbcTemplate.queryForList("SELECT * from ib_profiles where reference=?", reference);
            LOGGER.info("PROFILE:" + result);
            String profileXml = "";

            for (Map<String, Object> r : result) {
                profileXml = "<clientName>" + StringEscapeUtils.escapeXml(r.get("name") + "") + "</clientName>\n" + "<clientId>" + r.get("custid") + "</clientId>\n" + "<clientRim>" + r.get("rim_no") + "</clientRim>\n" + "<clientShortName>" + StringEscapeUtils.escapeXml(r.get("short_name") + "") + "</clientShortName>\n" + "<clientCategory>" + StringEscapeUtils.escapeXml(r.get("category") + "") + "</clientCategory>\n" + "<clientAddress1>" + StringEscapeUtils.escapeXml(r.get("address1") + "") + "</clientAddress1>\n" + "<clientAddress2>" + StringEscapeUtils.escapeXml(r.get("address2") + "") + "</clientAddress2>\n" + "<clientAddress3>" + StringEscapeUtils.escapeXml(r.get("address3") + "") + "</clientAddress3>\n" + "<clientAddress4>" + StringEscapeUtils.escapeXml(r.get("address4") + "") + "</clientAddress4>\n" + "<clientCity>" + StringEscapeUtils.escapeXml(r.get("city") + "") + "</clientCity>\n" + "<clientState>" + StringEscapeUtils.escapeXml(r.get("state") + "") + "</clientState>\n" + "<clientEmail>" + r.get("email") + "</clientEmail>\n" + "<clientParentBranch>" + r.get("account_branch_id") + "</clientParentBranch>\n" + "<clientRecruitingBranch>" + r.get("recruiting_branch_code") + "</clientRecruitingBranch>\n" + "<clientTrackingreference>" + r.get("reference") + "</clientTrackingreference>\n" + "<clientMandate>" + r.get("mandate") + "</clientMandate>\n" + "<clientToken>CVAGZLHEPDMEMBEGMYXKIQQCSLXZWGRODMPKQXSXAMSSLCLBDSCQCHOVAKFWNIFBWREOEJDPDJRCEVPGYXOCIUNPMICTXKUCPJFVVUKKBKLJBIDBJZXAJEEOGDDAZSXFZSUVLJMTZUKFRQYSXRQLGONPWCLKRVLKCDZQFYRYDZBPWVHAEYUAIEGRKVNDWBHYHREPFSWX</clientToken>\n" + "";
            }
            regXML += "<profile>\n" + profileXml + "</profile>";
            //get client accounts
            String account = "";
            String accounts = "";
            result = this.jdbcTemplate.queryForList("SELECT * from ib_profile_accounts where profile_reference=?", reference);
            for (Map<String, Object> rr : result) {
                account += "<account>\n<clientAccountNo>" + rr.get("account_no") + "</clientAccountNo>\n" + "<clientOldAccountNo>" + rr.get("old_account_no") + "</clientOldAccountNo>\n" + "<clientAccountName>" + StringEscapeUtils.escapeXml(rr.get("account_name") + "") + "</clientAccountName>\n" + "<clientAccountCurrency>" + rr.get("account_currency") + "</clientAccountCurrency>\n" + "<clientAccountProdCode>" + rr.get("account_prod_code") + "</clientAccountProdCode>\n" + "<clientAccountProdDescription>" + rr.get("account_type") + "</clientAccountProdDescription>\n" + "<clientAccountProdCategory>" + rr.get("account_category") + "</clientAccountProdCategory>\n" + "<clientAccountStatus>" + rr.get("account_status") + "</clientAccountStatus>\n" + "<clientAccountLimit>" + rr.get("account_limit") + "</clientAccountLimit>\n" + "<clientAccountLimitWithoutApproval>" + rr.get("limit_without_approval") + "</clientAccountLimitWithoutApproval>\n" + "</account>";
            }
            accounts += "<accounts>" + account + "</accounts>";
            regXML += accounts;
            //get Signatories
            result = this.jdbcTemplate.queryForList("SELECT * from ib_signatories where profile_reference=?", reference);
            String signatory = "";
            for (Map<String, Object> rr : result) {
                result2 = this.jdbcTemplate.queryForList("SELECT b.role_id,b.name roleName from ib_signatories_roles a inner join ib_roles b on a.role_id=b.id where a.profile_reference=? and signatory_id=?", reference, rr.get("id"));
                signatory += "<user>";
                signatory += "<username>" + rr.get("username") + "</username>\n" + "<fullName>" + StringEscapeUtils.escapeXml(rr.get("fullname") + "") + "</fullName>\n" + "<email>" + rr.get("email_address") + "</email>\n" + "<phoneNumber>" + rr.get("phone") + "</phoneNumber>\n" + "<custid>" + rr.get("cust_rim_no") + "</custid>\n" + "<custRimNo>" + rr.get("cust_rim_no") + "</custRimNo>\n";
                //Roles
                String role = "";
                for (Map<String, Object> re : result2) {
                    role += "<role>\n" + "<roleId>" + re.get("role_id") + "</roleId>\n" + "<roleName>" + re.get("roleName") + "</roleName>" + "</role>";
                }
                signatory += "<roles>" + role + "</roles>";

                //USER ACCOUNT ACCESS
                result2 = this.jdbcTemplate.queryForList("SELECT b.account_no,a.* from ib_user_account_access a inner join ib_profile_accounts b on a.account_id=b.id where b.profile_reference=? and a.signatory_id=?", reference, rr.get("id"));
                String access = "";
                for (Map<String, Object> re : result2) {
                    access += "<accountAccess>\n" + "<accountNo>" + re.get("account_no") + "</accountNo>\n" + "<transferAccoess>" + re.get("transfer_access") + "</transferAccoess>" + "<viewAccoess>" + re.get("view_access") + "</viewAccoess>" + "<tansferLimit>" + re.get("account_limit") + "</tansferLimit>" + "</accountAccess>";
                }
                signatory += access + "</user>";
            }
            regXML += signatory;
        } catch (Exception e) {
            LOGGER.info("ERROR GENERATING PROFILE XML: {}", e.getMessage());
        }
        return regXML + "\n</registrationRequest>";
    }

    public String generateIBankpasswordResetXML(String reference) {
        List<Map<String, Object>> result = null;
        List<Map<String, Object>> result2 = null;
        String regXML = "<ibankClientPasswordReset></ibankClientPasswordReset>";
        try {
            result = this.jdbcTemplate.queryForList("SELECT * from ib_self_services_requests where reference=?", reference);
            LOGGER.info("PROFILE:" + result);
            String profileXml = "";
            for (Map<String, Object> r : result) {
                profileXml = "<clientName>" + r.get("name") + "</clientName>\n" + "<clientId>" + r.get("custid") + "</clientId>\n" + "<clientRim>" + r.get("rim_no") + "</clientRim>\n" + "<clientShortName>" + r.get("short_name") + "</clientShortName>\n" + "<clientCategory>" + r.get("category") + "</clientCategory>\n" + "<clientAddress1>" + r.get("address1") + "</clientAddress1>\n" + "<clientAddress2>" + r.get("address2") + "</clientAddress2>\n" + "<clientAddress3>" + r.get("address3") + "</clientAddress3>\n" + "<clientAddress4>" + r.get("address4") + "</clientAddress4>\n" + "<clientCity>" + r.get("city") + "</clientCity>\n" + "<clientState>" + r.get("state") + "</clientState>\n" + "<clientEmail>" + r.get("email") + "</clientEmail>\n" + "<username>" + r.get("username") + "</username>\n" + "<clientParentBranch>" + r.get("account_branch_id") + "</clientParentBranch>\n" + "<clientRecruitingBranch>" + r.get("recruiting_branch_code") + "</clientRecruitingBranch>\n" + "<clientTrackingreference>" + r.get("reference") + "</clientTrackingreference>\n" + "<clientMandate>" + r.get("mandate") + "</clientMandate>\n" + "<ibToken>CVAGZLHEPDMEMBEGMYXKIQQCSLXZWGRODMPKQXSXAMSSLCLBDSCQCHOVAKFWNIFBWREOEJDPDJRCEVPGYXOCIUNPMICTXKUCPJFVVUKKBKLJBIDBJZXAJEEOGDDAZSXFZSUVLJMTZUKFRQYSXRQLGONPWCLKRVLKCDZQFYRYDZBPWVHAEYUAIEGRKVNDWBHYHREPFSWX</ibToken>\n" + "";
            }
            regXML = "<ibankClientPasswordReset>\n" + profileXml + "</ibankClientPasswordReset>";
        } catch (Exception e) {
            LOGGER.info("ERROR GENERATING PROFILE XML: {}", e.getMessage());
        }
        return regXML;
    }


    public String getIbankProfilesPendingAtHqOld(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        //get report for HQ/IBD USER
        try {
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            if (branchNo.equalsIgnoreCase("060")) {
                mainSql = "SELECT count(*) FROM ib_profiles where status='PHQ'";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            } else {
                mainSql = "SELECT count(*) FROM ib_profiles where recruiting_branch_code=? and status='PHQ'";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            }
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
//                searchQuery = " WHERE  concat(reference,' ', account,' ', name,' ', rim_no,' ', custid,' ', short_name,' ', category,' ', address1,' ', address2,' ', address3,' ', address4,' ', city,' ', state,' ', email,' ', account_branch_id,' ', recruiting_branch_code,' ', created_by) LIKE ?  and status='PHQ'";
                if (branchNo.equalsIgnoreCase("060")) {
                    searchQuery = " WHERE  concat(reference,' ', account,' ', name,' ', rim_no,' ', custid,' ', short_name,' ', category,' ', address1,' ', address2,' ', address3,' ', address4,' ', city,' ', state,' ', email,' ', account_branch_id,' ', recruiting_branch_code,' ', created_by) LIKE ?  and status='PHQ'";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM ib_profiles b" + searchQuery, new Object[]{searchValue}, Integer.class);

                } else {
                    searchQuery = " WHERE  concat(reference,' ', account,' ', name,' ', rim_no,' ', custid,' ', short_name,' ', category,' ', address1,' ', address2,' ', address3,' ', address4,' ', city,' ', state,' ', email,' ', account_branch_id,' ', recruiting_branch_code,' ', created_by) LIKE ? AND recruiting_branch_code=? and status='PHQ'";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(b.id) FROM ib_profiles b" + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);

                }
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select * from ib_profiles b " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);

                results = this.jdbcTemplate.queryForList(mainSql, searchValue, branchNo);
                LOGGER.info("Datatable Response: {}", results);

            } else {
                if (branchNo.equalsIgnoreCase("060")) {
                    mainSql = "select * from ib_profiles where  status='PHQ' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{});
                } else {
                    mainSql = "select * from ib_profiles where recruiting_branch_code=?  and status='PHQ' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
                }
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


    public String getIbankProfilesPendingAtHq(String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            if (branchNo.equalsIgnoreCase("060")) {
                mainSql = "SELECT count(*) FROM ib_profiles where status='PHQ'";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            } else {
                mainSql = "SELECT count(*) FROM ib_profiles where recruiting_branch_code=? and status='PHQ'";
                totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{branchNo}, Integer.class);
            }
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";

                if (branchNo.equalsIgnoreCase("060")) {
                    searchQuery = " WHERE  concat(reference,' ', account,' ', i.name,' ', rim_no,' ', category,' ', address1,' ', city,' ', created_by,' ',branch_approved_by,' ',branch_approved_date) LIKE ? AND recruiting_branch_code=? and status='PHQ'";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(i.id) FROM ib_profiles i" + searchQuery, new Object[]{searchValue}, Integer.class);
                } else {
                    searchQuery = " WHERE  concat(reference,' ', account,' ', i.name,' ', rim_no,' ', category,' ', address1,' ', city,' ', created_by,' ',branch_approved_by,' ',branch_approved_date) LIKE ? AND recruiting_branch_code=? and status='PHQ'";
                    totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT COUNT(i.id) FROM ib_profiles i" + searchQuery, new Object[]{searchValue, branchNo}, Integer.class);
                }

            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select b.name as branch,i.reference,i.account,i.name,i.rim_no,i.custid," + "i.category,i.address1,i.city,i.created_by,i.create_dt,i.branch_approved_by,i.branch_approved_date,i.status " + "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code" + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);

                results = this.jdbcTemplate.queryForList(mainSql, searchValue, branchNo);
                LOGGER.info("Datatable Response: {}", results);

            } else {
                if (branchNo.equalsIgnoreCase("060")) {
                    mainSql = "select b.name as branch,i.reference,i.account,i.name,i.rim_no,i.branch_approved_by,i.branch_approved_date,i.custid" + ",i.category,i.address1,i.city,i.created_by,i.create_dt,i.status " +
                            "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code where i.status='PHQ' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{});
                    LOGGER.info("Datatable Response: {}", results);
                } else {
                    mainSql = "select b.name as branch,i.reference,i.account,i.branch_approved_by,i.branch_approved_date,i.name,i.rim_no,i.custid" + ",i.category,i.address1,i.city,i.created_by,i.create_dt,i.status " + "from ib_profiles i inner join branches b on i.recruiting_branch_code=b.code where i.recruiting_branch_code=?  and i.status='PHQ' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                    results = this.jdbcTemplate.queryForList(mainSql, new Object[]{branchNo});
                    LOGGER.info("Datatable Response: {}", results);
                }
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }

        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
        return json;

    }

    public String registerIBProfile(String reference) {
        String ibankResponse = "";
        try {
            String xmlForIBankRegistration = generateIBankRegistrationXML(reference);
            LOGGER.info("XML {},", xmlForIBankRegistration);
            ibankResponse = HttpClientService.sendXMLRequest(xmlForIBankRegistration, systemVariable.IBANK_REGISTRATION_URL);
            return ibankResponse;
        } catch (Exception e) {
            LOGGER.info("ERROR ON CREATING IBANK PROFILE: {}", e.getMessage());
            return ibankResponse;
        }
    }

    public Integer removeAccFromProfile(String reference, String accountNumber) {
        Integer result = -1;
        try {
            String sql = "delete from ib_profile_accounts where profile_reference=? and account_no=?";
            LOGGER.info("REMOVING PROFILE ACCOUNT: {},reference: {},accountNumber: {}", sql, reference, accountNumber);
            result = jdbcTemplate.update(sql, reference, accountNumber);
        } catch (Exception e) {
            LOGGER.info("ERROR ON REMOVING PROFILE ACCOUNT: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public Integer removeSignatoryFromProfile(String reference, String rimNumber) {
        Integer result = -1;
        try {
            String sql = "delete from ib_signatories where profile_reference=? and cust_rim_no=?";
            LOGGER.info("REMOVING SIGNATORY: {},reference: {},accountNumber: {}", sql, reference, rimNumber);
            result = jdbcTemplate.update(sql, reference, rimNumber);
        } catch (Exception e) {
            LOGGER.info("ERROR ON REMOVING SIGNATORY: {}", e.getMessage());
            result = -1;
        }
        return result;
    }


    public List<Map<String, Object>> ibRegistrationReportAjax(String branchCode, String fromDate, String toDate) {
        String mainSql;
        List<Map<String, Object>> results = null;

        if (branchCode.equalsIgnoreCase("060")) {
            mainSql = "select a.reference,a.account,a.name,a.rim_no,a.category,b.name as branchName,a.create_dt,a.created_by,a.hq_approved_date,a.status from ib_profiles a inner join branches b on a.recruiting_branch_code=b.code where a.create_dt>=? and a.create_dt<=?";
            results = this.jdbcTemplate.queryForList(mainSql, fromDate, toDate);
        } else {
            mainSql = "select a.reference,a.account,a.name,a.rim_no,a.category,b.name as branchName,a.create_dt,a.created_by,a.hq_approved_date,a.status from ib_profiles a inner join branches b on a.recruiting_branch_code=b.code where recruiting_branch_code=? and a.create_dt>=? and a.create_dt<=?";
//                mainSql = "select * from ib_profiles where recruiting_branch_code=? and  create_dt>=? and create_dt<=?";
            results = this.jdbcTemplate.queryForList(mainSql, branchCode, fromDate, toDate);
        }
        return results;
    }

    public Integer setHqApprover(String reference, String username, Timestamp approvedDate) {

        Integer result = -1;
        try {
            result = jdbcTemplate.update("UPDATE ib_profiles set status='C',hq_approved_by=?,hq_approved_date=? WHERE reference=? ", username, approvedDate, reference);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("ERROR ON UPDATING IBANK PROFILE: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public List<Map<String,Object>> getIbankUser(String custName) {
        LOGGER.info("SELECT * FROM USERS WHERE USERNAME LIKE '%"+custName+"%' OR FULL_NAME LIKE '%"+custName+"%'");
        return jdbcTPBONLINETemplate.queryForList("SELECT * FROM USERS WHERE USERNAME LIKE '%"+custName+"%' OR FULL_NAME LIKE '%"+custName+"%' ");
    }


    public String customerResetPassword(Map<String, String> customQuery) {
        LOGGER.info("customerResetPasswordService:Req..... :-> {}", customQuery);
        String resp = "-1";

                String newPassword = MaiString.randomString(8);
                String username = customQuery.get("userName");
                String custId = customQuery.get("custId");
                UserModal user = getUserByUsername(username, custId);
                if (user != null) {
                    if (userPasswordUpdate(newPassword,username ) == 1) {
                        LOGGER.info("Password has been updated successfully...... {}", username);
                        String parameters = "-1=-1&PASSWORD=" + newPassword + "&PARTNER_CODE=" + user.getPartnerCode() + "&USERNAME=" + user.getUsername();
                        if (user.getNoficationChannel().equals("SMS") || user.getNoficationChannel().equals("BOTH")) {
                            String msgText = smsService.smsBodyBuilder(1014, "en", parameters, SmsConstants.SMS_CHANNEL_CD);
                            LOGGER.info("FINAL SMS TO SEND .... {}", msgText.replace("{USERNAME}",username).replace("{PARTNER_CODE}",user.getPartnerCode()).replace("{PASSWORD}",newPassword));
                            msgText = msgText.replace("{USERNAME}",username).replace("{PARTNER_CODE}",user.getPartnerCode()).replace("{PASSWORD}",newPassword);
                            queueProducer.sendQueueSendSMS(msgText + "^" + user.getMsisdn());
                        }
                        if (user.getNoficationChannel().equals("EMAIL") || user.getNoficationChannel().equals("BOTH")) {
                            //EMAIL
                            String emailText = smsService.smsBodyBuilder(1014, "en", parameters, SmsConstants.EMAIL_CHANNEL_CD);
                            emailText = emailText.replace("{USERNAME}",username).replace("{PARTNER_CODE}",user.getPartnerCode()).replace("{PASSWORD}",newPassword);

                            queueProducer.sendQueueOTPEmailConsumer(user.getEmail() + "^" + emailText);
                        }

                        resp = "{\"responseCode\":\"0\",\"message\":\"Success\"}";
                    } else {

                        resp = "{\"responseCode\":\"96\",\"message\":\"failed to submit data into the database.\"}";

                    }

                } else {

                    resp = "{\"responseCode\":\"96\",\"message\":\"Username not found.\"}";

                }

        LOGGER.info("customerResetPasswordService:Resp[{}]:-> {}", resp);
        return resp;
    }


    public UserModal getUserByUsername(String username, String custId) {
        try {
            UserModal userModal
                    = this.jdbcTPBONLINETemplate.queryForObject("select NOTIFICATION_CHANNEL,LANG,a.ID, USERNAME,IS_STAFF,FULL_NAME, PASSWORD, EMAIL,  ENABLED,"
                            + " ACCOUNT_EXPIRED, CREDENTIALS_EXPIRED, ACCOUNT_LOCKED,"
                            + " a.DOMAIN_ID, PARTNER_CODE, LAST_LOGIN_TIME, a.BRANCH_NO, MSISDN,"
                            + " CREATED_BY,CUST_ID, a.CREATED_ON, ACCOUNT_EXPIRES_ON, CREDENTIALS_EXPIRES_ON,DOB,c.NAME as DOMAIN_NAME,b.bu_name as BRANCH_NAME,b.is_hq as IS_HEAD_OFFICE   from USERS  a left join TPB_BRANCHES b on a.BRANCH_NO=b.bu_no left join DOMAIN_CONTROLLER c on a.DOMAIN_ID=c.ID  where a.USERNAME=? or CUST_ID=?", new Object[]{username, custId},
                    (ResultSet rs, int rowNum) -> {
                        UserModal userDt = new UserModal();
                        userDt.setId(rs.getString("id"));
                        userDt.setEmail(rs.getString("email"));
                        userDt.setUsername(rs.getString("username"));
                        userDt.setFullname(rs.getString("FULL_NAME"));
                        userDt.setPassword(rs.getString("PASSWORD"));
                        userDt.setMsisdn(rs.getString("MSISDN"));
                        userDt.setEnabled(rs.getBoolean("ENABLED"));
                        userDt.setAccountExpired(rs.getBoolean("ACCOUNT_EXPIRED"));
                        userDt.setAccountExpiredTime(rs.getString("ACCOUNT_EXPIRES_ON"));
                        userDt.setCredentialsExpired(rs.getBoolean("CREDENTIALS_EXPIRED"));
                        userDt.setCredentialsExpiredTime(rs.getString("CREDENTIALS_EXPIRES_ON"));
                        userDt.setAccountLocked(rs.getInt("ACCOUNT_LOCKED"));
                        userDt.setDomainId(rs.getInt("DOMAIN_ID"));
                        userDt.setPartnerCode(rs.getString("PARTNER_CODE"));
                        userDt.setLastLoginTime(rs.getString("LAST_LOGIN_TIME"));
                        userDt.setBranchNo(rs.getString("BRANCH_NO"));
                        userDt.setCreatedBy(rs.getString("CREATED_BY"));
                        userDt.setCreatedOn(rs.getString("CREATED_ON"));
                        userDt.setDob(rs.getString("DOB"));
                        userDt.setBranchName(rs.getString("BRANCH_NAME"));
                        userDt.setDomainName(rs.getString("DOMAIN_NAME"));
                        userDt.setLang(rs.getString("LANG"));
                        userDt.setCustId(rs.getString("CUST_ID"));
                        userDt.setNoficationChannel(rs.getString("NOTIFICATION_CHANNEL"));

                        return userDt;
                    });
            return userModal;
        } catch (DataAccessException e) {
            LOGGER.info("Error in getUserByUsername {}", e.getMessage());
            return null;
        }
    }


    public int userPasswordUpdate(String password, String username) {
        int result = -1;
        try {
            result = jdbcTPBONLINETemplate.update("UPDATE users SET  PASSWORD=?, CREDENTIALS_EXPIRED=1,ACCOUNT_LOCKED=0 WHERE USERNAME=?",
                    passwordEncoder.encode(password), username);
        } catch (DataAccessException e) {
            result = -1;
            LOGGER.error("Rollbacked... {}", e);
            return result;
        }
        return result;
    }


    public IBUserDetails getIbUserDetails(Map<String, String> customQuery) {

        try {
            String sql = "SELECT  USERNAME, FULL_NAME, EMAIL, ENABLED, ACCOUNT_EXPIRED, CREDENTIALS_EXPIRED, ACCOUNT_LOCKED, CUST_ID, MSISDN  FROM USERS WHERE PARTNER_CODE=? AND USERNAME=?";
          IBUserDetails details =  jdbcTPBONLINETemplate.queryForObject(sql, new Object[]{customQuery.get("partnerCode"),customQuery.get("userName")},( ResultSet rs, int rowNum)->{
                IBUserDetails userDetails  = new IBUserDetails();
                userDetails.setUserName(rs.getString("USERNAME"));
                userDetails.setFullName(rs.getString("FULL_NAME"));
                userDetails.setEmail(rs.getString("EMAIL"));
                userDetails.setContactNumber(rs.getString("MSISDN"));
                userDetails.setAccountExpires(rs.getInt("ACCOUNT_EXPIRED"));
                userDetails.setAccountLocked(rs.getInt("ACCOUNT_LOCKED"));
                userDetails.setCredentialsExpires(rs.getInt("CREDENTIALS_EXPIRED"));
                userDetails.setEnableAccount(rs.getInt("ENABLED"));
                return userDetails;
            });

            return details;
        } catch (DataAccessException dae) {
            LOGGER.info("Data access exception for getting ib user details for modifications... {}", dae);
            return null;
        }
    }

    public String updateIBUserDetails(Map<String, String> customQuery) {
        String sql = "UPDATE USERS SET FULL_NAME=?, EMAIL=?, MSISDN=?, ENABLED=?, ACCOUNT_EXPIRED=?, CREDENTIALS_EXPIRED=?, ACCOUNT_LOCKED=? WHERE USERNAME=?";
        int updateResult=0;
        String  resp = "{\"responseCode\":\"96\",\"message\":\"General failure.\"}";

        try{
            LOGGER.info(sql.replace("?", "'{}'"),customQuery.get("fullName"),customQuery.get("email"),customQuery.get("contactNo"),customQuery.get("enableAccount"),customQuery.get("accountExpires"),customQuery.get("credentialExpires"),customQuery.get("accountLocked"),customQuery.get("username"));
           updateResult= jdbcTPBONLINETemplate.update(sql,new Object[]{customQuery.get("fullName"),customQuery.get("email"),customQuery.get("contactNo"),customQuery.get("enableAccount"),customQuery.get("accountExpires"),customQuery.get("credentialExpires"),customQuery.get("accountLocked"),customQuery.get("username")});
        }catch (DataAccessException ex){
            LOGGER.info("Data access exception check ... {}", ex);
        }
        if(updateResult==1){
            resp = "{\"responseCode\":\"0\",\"message\":\"User Details Updated Successfully.\"}";
        }else{
            resp = "{\"responseCode\":\"96\",\"message\":\"Failed to update user details.\"}";
        }
        return resp;
    }
}
