/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.service.HttpClientService;
import com.service.JasperService;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import philae.ach.UsRole;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class GovExpenditurePensionRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("partners")
    JdbcTemplate jdbcPartnersTemplate;

    @Autowired
    @Qualifier("gwBrinjalDbConnection")
    JdbcTemplate jdbcBrinjalTemplate;

    @Autowired
    JasperService jasperService;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    ObjectMapper jacksonMapper;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EftRepo.class);

    /*
    GET EFT dashboard 
     */
    public List<Map<String, Object>> getGovExpendPensionModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING BATCH : {}", e.getMessage());
        }
        return result;
    }

    public String getPendingBatchesForProcessing(String accountNo, String searchValue, String status, String draw) {
        String request = "{\n"
                + "  \"accountNo\": \"" + accountNo + "\"\n,"
                + "  \"status\": \"" + status + "\"\n,"
                + "  \"searchValue\": \"%" + searchValue + "%\"\n"
                + "}";
        LOGGER.info("REQUEST:{}", request);
        String result = HttpClientService.sendTxnToAPI(request, systemVariables.BRINJAL_API_URL + "/esb/govExpenditure/queryMusePendingBatches");
        LOGGER.info("RESPONSE:{}", result);
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        if (result != null && !result.equalsIgnoreCase("-1") && !result.equalsIgnoreCase("[]")) {
            JSONObject resultObject = new JSONObject(result.replace("[", "").replace("]", ""));
            totalRecords = resultObject.length();
            if (!searchValue.equals("")) {
                totalRecordwithFilter = resultObject.length();
            } else {
                totalRecordwithFilter = totalRecords;
            }
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + result + "}";
        LOGGER.info("RESULTS:{}", result);
        return json;
    }

    public String previewBatchTransactionsList(String reference, String account, String status, String draw, String start,
                                               String rowPerPage, String searchValue, String columnIndex, String columnName,
                                               String columnSortOrder) {
        String request = "{\n"
                + "  \"accountNo\": \"" + account + "\"\n,"
                + "  \"messageId\": \"" + reference + "\"\n"
                + "}";
        LOGGER.info("REQUEST:{}", request);
        String result = HttpClientService.sendTxnToAPI(request, systemVariables.BRINJAL_API_URL + "/esb/govExpenditure/queryMuseBatchTransactionList");
        LOGGER.info("RESPONSE:{}", result);
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        if (result != null && !result.equalsIgnoreCase("-1")) {
            if (result.equals("[]")) {
                result = "{}";
            }
            JSONObject resultObject = new JSONObject(result.replace("[", "").replace("]", ""));
            totalRecords = resultObject.length();
            if (!searchValue.equals("")) {
                totalRecordWithFilter = resultObject.length();
            } else {
                totalRecordWithFilter = totalRecords;
            }
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + result + "}";
        LOGGER.info("RESULTS:{}", result);
        return json;
    }

    public Integer uploadTransactionDocument(String reference, MultipartFile file) {
        int result;
        try {
            result = jdbcTemplate.update("INSERT INTO transfer_document (txnReference,supportingDoc,file_name,file_size) VALUES (?,?,?,?)",
                    reference, file.getBytes(), file.getOriginalFilename(), file.getSize());
            LOGGER.info("INSERTING FILE :{} FILE NAME: {} SIZE: {}", result, file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            LOGGER.info("ERROR ON INSERTING SUPPORTING DOCUMENT: {}", e.getMessage());
            result = -1;
        }
        return result;
    }

    public List<com.online.core.request.SupportDoc> getBatchDocument(String txnReference) {
        try {
            return jdbcTemplate.query("SELECT txnReference, supportingDoc, file_name FROM transfer_document WHERE txnReference = ?",
            new Object[]{txnReference},
            (ResultSet rs, int rowNum) -> {
                com.online.core.request.SupportDoc row = new com.online.core.request.SupportDoc();
                row.setFileBlob(rs.getBytes("supportingDoc"));
                row.setFileName(rs.getString("file_name"));
                row.setTxnId(rs.getString("txnReference"));
                return row;
            });
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Get batch document exception: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public String processMuseERMSToCoreBanking(String batchReference, String eventCode, UsRole usRole) {
        String request = "{\n" +
                "  \"batchReference\": \"" + batchReference + "\",\n" +
                "  \"eventCode\": \"" + eventCode + "\",\n" +
                "  \"userRole\": {\n" +
                "    \"branchCode\": \"" + usRole.getBranchCode() + "\",\n" +
                "    \"branchId\": \"" + usRole.getBranchId() + "\",\n" +
                "    \"branchName\": \"" + usRole.getBranchName() + "\",\n" +
                "    \"buRoleId\": \"" + usRole.getBuRoleId() + "\",\n" +
                "    \"role\": \"" + usRole.getRole() + "\",\n" +
                "    \"roleId\": \"" + usRole.getRoleId() + "\",\n" +
                "    \"supervisor\": \"" + usRole.getSupervisor() + "\",\n" +
                "    \"userId\": \"" + usRole.getUserId() + "\",\n" +
                "    \"userName\": \"" + usRole.getUserName() + "\",\n" +
                "    \"userRole\": \"" + usRole.getRole() + "\",\n" +
                "    \"userRoleId\": \"" + usRole.getRoleId() + "\"\n" +
                "  }\n" +
                "}";
        LOGGER.info("REQUEST:{}", request);
        String result = HttpClientService.sendTxnToAPI(request, systemVariables.BRINJAL_API_URL + "/esb/govExpenditure/processMuseERMSToCoreBanking");
        LOGGER.info("RESPONSE:{}", result);

        return result;
    }

    public String getTotalAmountFromTransfers(String reference) {
        String[] args;
        StringBuilder inSql = new StringBuilder();
        if (reference.contains(",")) {
            args = reference.split(",");
            for (String arg : args) {
                LOGGER.info("ARG:{}", arg);
                inSql.append("'").append(arg).append("',");
            }
            inSql.deleteCharAt(inSql.length() - 1);
            try {
                return jdbcTemplate.queryForObject(String.format(
                        "SELECT SUM(amount) amount FROM transfers WHERE reference in (%s)", inSql),
                        args, String.class);
            } catch (EmptyResultDataAccessException e) {
                LOGGER.info("Get amount from transfers exception: {}", e.getMessage());
                return "";
            }
        } else {
            try {
                return jdbcTemplate.queryForObject("SELECT amount FROM transfers WHERE reference = ?",
                        new Object[]{reference}, String.class);
            } catch (EmptyResultDataAccessException e) {
                LOGGER.info("Get amount from transfers exception: {}", e.getMessage());
                return "";
            }
        }
    }

    public String approveAuthorizedTransactions(String[] batchReferences) {
        try {
            String inSql = String.join(",", Collections.nCopies(batchReferences.length, "?"));
            int result = jdbcTemplate.update(
                    String.format("UPDATE pensioners_batch_summary set status = 'A' where reference in (%s)", inSql),
                    batchReferences
            );
            String message = "";
            if (result > 0) message = "Batch authorization approved!";
            else message = "Failed authorization approval!";
            return "{\n\"result\": 0,\n\"message\": \"" + message + "\"\n}";
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Manager approve authorized transactions (Update pensioners_batch_summary exception): {}", e.getMessage());
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public String complyTransactionWithReason(String batchReference, String status, String[] trackingNos, String reason, Boolean all) {
        try {
            String inSql = String.join(",", Collections.nCopies(trackingNos.length, "?"));
            String[] args = ArrayUtils.addAll(new String[]{reason}, trackingNos);
            int result;
            if (status.equals("I")) {
                if (all) {
                    result = jdbcTemplate.update("UPDATE pensioners_batch_summary set status = 'CoP' where reference = ?",
                            new Object[]{batchReference}
                    );
                } else {
                    result = jdbcTemplate.update(
                            String.format("UPDATE pensioners_payroll set message = 'success', comments = ?, status = 'I' where trackingNo in (%s)", inSql),
                            args
                    );
                }
            } else {
                if (all) {
                    result = jdbcTemplate.update("UPDATE pensioners_payroll set message = 'success', comments = ?, status = 'I' where batchReference = ? and cbs_status = 'F'",
                            new Object[]{reason, batchReference}
                    );
                } else {
                    result = jdbcTemplate.update(
                            String.format("UPDATE pensioners_payroll set message = 'success', comments = ?, status = 'I' where trackingNo in (%s)", inSql),
                            args
                    );
                }
            }

            String message;
            if (result > 0) {
                if (trackingNos.length > 1)
                    message = "Transactions have been updated to comply successfully!";
                else
                    message = "Transaction has been updated to comply successfully!";
            } else message = "Failed authorization!";
            return "{\n\"result\": 0,\n\"message\": \"" + message + "\"\n}";
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Officer comply transactions (update pensioners_payroll exception): {}", e.getMessage());
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public String approveCompliance(String[] batchReferences) {
        try {
            String inSql = String.join(",", Collections.nCopies(batchReferences.length, "?"));
            int result = jdbcTemplate.update(
                    String.format("UPDATE pensioners_batch_summary set status = 'Co' where reference in (%s)", inSql),
                    batchReferences
            );
            String message;
            if (result > 0) message = "Batch has complied for processing successfully!";
            else message = "Failed to comply batch!";
            return "{\n\"result\": 0,\n\"message\": \"" + message + "\"\n}";
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Manager comply batch (update pensioners_batch_summary exception): {}", e.getMessage());
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public List<Map<String, Object>> getMuseInstitutions(String branchCode) {
        List<Map<String, Object>> results = null;
        String sqlQuery = "SELECT acct_name, acct_no, acct_type, muse_code, currency, branch FROM muse_partners where acct_type = 'DIS'";
        if (!branchCode.isEmpty() && !branchCode.equals("060")) {
            sqlQuery = "SELECT acct_name, acct_no, acct_type, muse_code, currency, branch FROM muse_partners where acct_type = 'DIS' and branch = ?";
            try {
                results = jdbcPartnersTemplate.queryForList(sqlQuery, new String[]{branchCode});
            } catch (EmptyResultDataAccessException e) {
                LOGGER.info("Get MUSE institutions from muse partners exception: {}", e.getMessage());
            }
        } else {
            try {
                results = jdbcPartnersTemplate.queryForList(sqlQuery);
            } catch (EmptyResultDataAccessException e) {
                LOGGER.info("Get MUSE institutions from muse partners exception: {}", e.getMessage());
            }
        }
        return results;
    }

    public List<String> getBatches(String account) {
        try {
            return jdbcBrinjalTemplate.queryForList("SELECT messageId FROM BATCH_SUMMARY where funderAcct = ?",
                    String.class, new Object[]{account});
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Get amount from transfers exception: {}", e.getMessage());
            return null;
        }
    }

    // PAYMENT REPORT
    public String getPaymentSummaryReport(String exporterFileType, HttpServletResponse response, String destName,
                                          String institution, String paymentType, String fromDate, String toDate,
                                          String batchRef, String printedBy) {
        Connection conn = null;
        try {
            String reportFileTemplate = "/iReports/payment/MUSE-outward-transaction-report-batches.jasper";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("BATCH_REFERENCE", batchRef);
            if (jdbcBrinjalTemplate.getDataSource() != null)
                conn = jdbcBrinjalTemplate.getDataSource().getConnection();
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, conn);
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (Exception ex) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            LOGGER.info(null, ex);
            java.util.logging.Logger.getLogger(GovExpenditurePensionRepo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public byte[] getSupportingDocument(String ref) {
        byte[] result;
        try {
            result = this.jdbcTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? limit 1", new Object[]{ref}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollback... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public String convertCurrency(String batchReference, String accountNo) {
        int result = -1;
        String errMsg = "";
        try {
            result = jdbcBrinjalTemplate.update("UPDATE batch_summary set currency = currency + '|TZS' where messageId = ?",
                    batchReference);
            LOGGER.info("Converted currency to TZS for batch {} with account {}", batchReference, accountNo);
        } catch (Exception e) {
            errMsg = e.getMessage();
            LOGGER.info("ERROR Converting currency for batch {} with account {}, error {}",  batchReference, accountNo,
                    errMsg);
        }
        if (result > 0) {
            return "{\"responseCode\": 0, \"message\": \"Success\"}";
        } else {
            return "{\"responseCode\": 99, \"message\": \"Failed!, "+errMsg+"\"}";
        }
    }

    public String getBulkBatches(String fromDate, String toDate, String accountNo, String draw, String start,
                                 String rowPerPage, String searchValue, String columnIndex, String columnName,
                                 String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        if (fromDate == null) {
            fromDate = DateUtil.now("YYYY-MM-dd");
        }
        if (toDate == null) {
            toDate = DateUtil.now("YYYY-MM-dd");
        }
        try {
            mainSql = "select count(*) from batch_summary where created_date>=? and created_date<=? and funderAcct=?";
            LOGGER.info(mainSql.replace("?", "'{}'"), fromDate, toDate, accountNo);
            totalRecords = jdbcBrinjalTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, accountNo}, Integer.class);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(currency,' ',funderAcct,' ',funderName,' ',messageId,' ',payerAcct,' ',payerName,' ',paymentType,' ',transferRef,' ',totalAmount,' ',sender,' ',status,' ',branchCode) LIKE ? and created_date>=? and created_date<=? and funderAcct=?";
                totalRecordWithFilter = jdbcBrinjalTemplate.queryForObject("select count(*) from pensioners_batch_summary" + searchQuery, new Object[]{searchValue, fromDate, toDate, accountNo}, Integer.class);
                mainSql = "select * from batch_summary " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, accountNo});

            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "select * from batch_summary where created_date>=? and created_date<=? and funderAcct=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcBrinjalTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, accountNo});
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }
}
