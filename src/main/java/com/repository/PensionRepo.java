/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.IBANK.PaymentReq;
import com.DTO.pension.PensionPayrollToCoreBanking;
import com.DTO.pension.PsssfBatchBeneficiary;
import com.DTO.pension.PsssfBatchRequest;
import com.DTO.psssf.LoanRepaymentResp;
import com.config.SYSENV;
import com.entities.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.models.PensionersPayroll;
import com.models.TmpBatchTransaction;
import com.queue.QueueProducer;
import com.service.*;
import com.zaxxer.hikari.HikariDataSource;
import net.sf.jasperreports.engine.JasperPrint;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import philae.api.UsRole;

import javax.annotation.Nonnull;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author melleji.mollel
 */
@Repository
public class PensionRepo {

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource cilantroDataSource;

    @Autowired
    TmpBatchTransactionRepository tmpBatchTransactionRepository;

    @Autowired
    PensionersPayrollRepository pensionersPayrollRepository;

    @Autowired
    QueueProducer queProducer;
    @Autowired
    ObjectMapper jacksonMapper;
    @Autowired
    JasperService jasperService;
    @Autowired
    SYSENV sysenv;
    @Autowired
    PensionersPayrollService pensionersPayrollService;

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job job;

    BigDecimal totalAmount = BigDecimal.ZERO;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PensionRepo.class);

    public String receivePayrollPaymentData(String payload) throws JsonProcessingException {

        try {
            LOGGER.info("receivePayrollPaymentData:{}",payload);

            PsssfBatchRequest psssfBatchRequest = jacksonMapper.readValue(payload, PsssfBatchRequest.class);

            //INSERT PAYROLL BATCH
            String sql = "INSERT INTO pensioners_batch_summary(noOfTxns, reference, batchDate, institutionName, batchDescription, totalAmount, created_by, status) VALUES (?,?,?,?,?,?,?,?)";
            jdbcTemplate.update(sql, psssfBatchRequest.getNoOfPensioners(), psssfBatchRequest.getBatchId(), psssfBatchRequest.getBatchDate(), psssfBatchRequest.getInstitution(), psssfBatchRequest.getBatchDescription(), psssfBatchRequest.getTotalAmount(), psssfBatchRequest.getUserid(), "R");
            //INSERT BENEFICIARY PAYROLL DATA
            insertPsssfPayrollEntries(psssfBatchRequest.getBeneficiaries(), psssfBatchRequest.getUserid(), psssfBatchRequest.getBatchId(), psssfBatchRequest.getInstitution());
            queProducer.sendToQueuePsssfPensionPayroll(psssfBatchRequest);
            //PREPARE QUEUE FOR PENSION ADVANCE VALIDATION

            return "{\n"
                    + "\"RespStatus\": \"Success\",\n"
                    + "\"RespCode\": \"00\",\n"
                    + "\"RespHeader\": {\n"
                    + "\"batchId\": \"" + psssfBatchRequest.getBatchId() + "\",\n"
                    + "\"BatchDate\": \"" + psssfBatchRequest.getBatchDate() + "\",\n"
                    + "\"Institution\": \"" + psssfBatchRequest.getInstitution() + "\",\n"
                    + "\"AccountCode\": \"TCB\",\n"
                    + "\"AccountDescription\": \""+ psssfBatchRequest.getBatchDescription() +"\",\n"
                    + "\"TotalAmount\": \"" + psssfBatchRequest.getTotalAmount() + "\"\n"
                    + "},\n"
                    + "\"RespBody\": {\n"
                    + "\"Message\": \"Batch " + psssfBatchRequest.getInstitution() + " was Successful submitted\"\n"
                    + "}\n"
                    + "}";

        }catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(e.getMessage(),e);
            return "OK";
        }

    }

    public void insertPsssfPayrollEntries(List<PsssfBatchBeneficiary> batchBeneficiaries, String createdBy, String batchReference, String institutionName) {
        String sql = "INSERT INTO pensioners_payroll(institution_id, name, currency, amount, account, channel_identifier, bankCode, pensioner_id, description, created_by, status,batchReference,trackingNo,bankReference,payroll_month,payroll_year) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {
                String bankReference = "PEN" + DateUtil.now("yyyyMMddHHmmss") + "_" + batchBeneficiaries.get(i).getID();
                java.util.Date date = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int month = cal.get(Calendar.MONTH) + 1;
                int year = cal.get(Calendar.YEAR);
                ps.setString(1, institutionName);
                ps.setString(2, batchBeneficiaries.get(i).getNAME());
                ps.setString(3, batchBeneficiaries.get(i).getCURRENCY());
                ps.setString(4, batchBeneficiaries.get(i).getAMOUNT());
                ps.setString(5, batchBeneficiaries.get(i).getACCOUNT());
                ps.setString(6, batchBeneficiaries.get(i).getCHANNELIDENTIFIER());
                ps.setString(7, batchBeneficiaries.get(i).getDESTINATIONCODE());
                ps.setString(8, batchBeneficiaries.get(i).getPENSIONER_ID());
                ps.setString(9, batchBeneficiaries.get(i).getNARRATION());
                ps.setString(10, createdBy);
                ps.setString(11, "R");
                ps.setString(12, batchReference);
                ps.setString(13, batchBeneficiaries.get(i).getID());
                ps.setString(14, bankReference);
                ps.setInt(15, month);
                ps.setInt(16, year);
            }

            @Override
            public int getBatchSize() {
                return batchBeneficiaries.size();
            }
        });
    }

    /*
    GET INWARD PENSION PAYMENT BATCH
     */
    public String getPensionPayrollAjax(String institutionName, String status, String fromDate, String toDate,
                                        String draw, String start, String rowPerPage, String searchValue,
                                        String columnIndex, String columnName, String columnSortOrder) {
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
            mainSql = "select count(*) from pensioners_batch_summary where batchDate>=? and batchDate<=? and institutionName=? and status=?";
            LOGGER.info(mainSql.replace("?", "'{}'"), fromDate, toDate, institutionName, status);
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, institutionName, status}, Integer.class);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(batchDate,' ',batchDescription,' ',totalAmount,' ',created_by) LIKE ? and batchDate>=? and batchDate<=? and institutionName=? and status=?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("select count(*) from pensioners_batch_summary" + searchQuery, new Object[]{searchValue, fromDate, toDate, institutionName, status}, Integer.class);
                mainSql = "select status,reference,totalAmount,noOfTxns,batchDescription,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successTxnVolume,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status ='F' and pp.create_dt >=? and  pp.create_dt <=?) failedCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and pp.cbs_status ='F' and pp.create_dt >=? and  pp.create_dt <=?) failedTxnVolume from pensioners_batch_summary a " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, institutionName, status});

            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "select status,reference,totalAmount,noOfTxns,batchDescription,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successTxnVolume,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status ='F' and pp.create_dt >=? and  pp.create_dt <=?) failedCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and pp.cbs_status ='F' and pp.create_dt >=? and pp.create_dt <=?) failedTxnVolume from pensioners_batch_summary a where batchDate>=? and batchDate<=? and institutionName=? and status=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, institutionName, status});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String getPensionBatchesAjax(String institutionName, String fromDate, String toDate, String draw, String start,
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
            mainSql = "select count(*) from pensioners_batch_summary where batchDate>=? and batchDate<=? and institutionName=? and status='P' or status='C'";
            LOGGER.info(mainSql.replace("?", "'{}'"), fromDate, toDate, institutionName);
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{fromDate, toDate, institutionName}, Integer.class);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(batchDate,' ',batchDescription,' ',totalAmount,' ',created_by) LIKE ? and batchDate>=? and batchDate<=? and institutionName=? and status='P' or status='C'";
                totalRecordWithFilter = jdbcTemplate.queryForObject("select count(*) from pensioners_batch_summary" + searchQuery, new Object[]{searchValue, fromDate, toDate, institutionName}, Integer.class);
                mainSql = "select status,reference,totalAmount,noOfTxns,batchDescription,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and pp.create_dt <=?) successCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successTxnVolume,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status ='F' and pp.create_dt >=? and pp.create_dt <=?) failedCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and pp.cbs_status ='F' and pp.create_dt >=? and pp.create_dt <=?) failedTxnVolume from pensioners_batch_summary a " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, institutionName});

            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "select status,reference,totalAmount,noOfTxns,batchDescription,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status <>'F' and pp.create_dt >=? and  pp.create_dt <=?) successTxnVolume,(select count(pp.id) from pensioners_payroll pp where pp.batchReference=a.reference and cbs_status ='F' and pp.create_dt >=? and  pp.create_dt <=?) failedCount,(select COALESCE(SUM(amount),0) from pensioners_payroll pp where pp.batchReference=a.reference and pp.cbs_status ='F' and pp.create_dt >=? and pp.create_dt <=?) failedTxnVolume from pensioners_batch_summary a where batchDate>=? and batchDate<=? and institutionName=? and status='P' or status='C' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate, institutionName});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    /*
    GET PAYROLL PENSION DETAILS
     */
    public String getPensionPayrollDetailsAjax(String draw, String start, String rowPerPage, String searchValue,
                                               String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM pensioners_payroll";
            totalRecords = jdbcTemplate.queryForObject(mainSql, Integer.class);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(name,' ',currency,' ',amount,' ',account,' ',pensioner_id,' ',description) LIKE ?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM pensioners_payroll" + searchQuery, new Object[]{searchValue}, Integer.class);
                mainSql = "SELECT * FROM pensioners_payroll" + searchQuery + "  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info(mainSql.replace("?", "'{}'"), searchValue);
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});
            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "SELECT * FROM pensioners_payroll ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }


    /*
    GET PAYROLL PENSION DETAILS BY BATCH REFERENCE
     */
    public String getPensionPayrollDetailsAjax(String batchReference, String status, String roleStatus, String draw,
                                               String start, String rowPerPage, String searchValue, String columnIndex,
                                               String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM pensioners_payroll where batchReference=? and cbs_status=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{batchReference, status}, Integer.class);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(name,' ',currency,' ',amount,' ',account,' ',pensioner_id,' ',description) LIKE ?  and  batchReference=? and cbs_status=?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM pensioners_payroll  " + searchQuery, new Object[]{searchValue, batchReference, status}, Integer.class);
                mainSql = "SELECT * FROM pensioners_payroll  " + searchQuery + "  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, batchReference, status});
            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "SELECT * FROM pensioners_payroll a where batchReference=? and cbs_status=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{batchReference, status});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

//    @Transactional
//    public String processPensionPayrollToCoreBanking(String tissRef, String tissAmount,
//                                                     List<HashMap<String, Object>> batchList, UsRole role) {
//        String result = "{\"result\":\"99\",\"message\":\"An Error occurred during processing-Timeout Please confirm on Rubikon: \"}";
//        double totalSuccessCount = 0, totalFailedCount = 0;
//        double successTxnVolume = 0, failedTxnVolume = 0;
//        List<String> batchReferences = new ArrayList<>();
//        for (HashMap<String, Object> map: batchList) {
//            double successCount = (Double) map.get("successCount");
//            double totalAmt = (Double) map.get("successTxnVolume");
//            String batchRef = (String) map.get("reference");
//            batchReferences.add(batchRef);
//            totalSuccessCount += successCount;
//            totalFailedCount += (Double) map.get("failedCount");
//            successTxnVolume += totalAmt;
//            failedTxnVolume += (Double) map.get("failedTxnVolume");
//
//            Map<String, Object> odPensionersMap = getListOfOverdraftPensioners(batchRef);
//            int odPensionersCount = odPensionersMap.get("COUNT(id)") != null ? Integer.parseInt(odPensionersMap.get("COUNT(id)").toString())
//                    : 0;
//            BigDecimal odPensionersSum = odPensionersMap.get("SUM(amount)") != null ? new BigDecimal(odPensionersMap.get("SUM(amount)").toString())
//                    : new BigDecimal(0);
//
//            successCount = successCount - odPensionersCount;
//            BigDecimal remainingAmt = new BigDecimal(totalAmt).subtract(odPensionersSum);
//            if (((Double) map.get("successCount")) < 5000) {
//                jdbcTemplate.update("INSERT INTO tmp_batch_transaction (reference, callbackUrl, createDt, endRecId," +
//                                " itemCount, startRecId, `timestamp`, totalAmount, updateDt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
//                        batchRef, sysenv.LOCAL_CALLBACK_URL + "/api/batchCallback", DateUtil.now("yyyy-MM-dd HH:mm:ss"), successCount, successCount, "0",
//                        DateUtil.now(), remainingAmt, null);
//                Integer firstId = jdbcTemplate.queryForObject("SELECT id FROM pensioners_payroll WHERE " +
//                        "batchReference = '"+batchRef+"' order by id limit 1", Integer.class);
//                if (firstId != null) {
//                    for (int j = 0; j < successCount; j++) {
//                        String mainSql = "INSERT INTO tmp_batch_transaction_item (txnRef, amount, batch, buId," +
//                                " drAcct, createDt, currency, crAcct, module, narration, recId, recSt, reverse," +
//                                " tries, `timestamp`) SELECT bankReference, amount, '" + batchRef + "', '" +
//                                role.getBranchId() + "', '" + sysenv.CREDIT_TRANSFER_AWAITING_LEDGER + "', '" +
//                                DateUtil.now("yyyy-MM-dd HH:mm:ss") + "', 'TZS', account, 'XAPI'," +
//                                " description, '" + j + "', 'P', 'N', '0', '" +
//                                DateUtil.now("yyyy-MM-dd HH:mm:ss") + "' FROM pensioners_payroll where id = ? " +
//                                "and od_loan_status = '0'";
//                        try {
//                            jdbcTemplate.update(mainSql, firstId + j);
//                        } catch (Exception e) {
//                            LOGGER.info("Skipped due to exception");
//                            LOGGER.info(null, e);
//                        }
//                    }
//                }
//            } else {
//                int iterations = (int) (successCount / 5000);
//                int remainder = (int) (successCount % 5000);
//                LOGGER.info("Success count: {}", successCount);
//                LOGGER.info("Iterations: {}", iterations);
//                LOGGER.info("Remainder: {}", remainder);
//                Integer firstId = jdbcTemplate.queryForObject("SELECT id FROM pensioners_payroll WHERE " +
//                        "batchReference = '"+batchRef+"' order by id limit 1", Integer.class);
//                for (int i = 0; i < iterations; i++) {
//                    int startRecId = i * 5000;
//                    int endRecId = i * 5000 + 5000;
//                    String reference = batchRef + "_" + i;
//                    try {
//                        jdbcTemplate.update("INSERT INTO tmp_batch_transaction (reference, callbackUrl, createDt, " +
//                                        "endRecId, itemCount, startRecId, `timestamp`, totalAmount, updateDt) " +
//                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
//                                reference, "", DateUtil.now("yyyy-MM-dd HH:mm:ss"), endRecId,
//                                5000, startRecId + 1, DateUtil.now(), remainingAmt, null);
//                    } catch (Exception e) {
//                        LOGGER.info("Skipped due to exception");
//                        LOGGER.info(null, e);
//                    }
//                    if (firstId != null) {
//                        for (int j = startRecId; j < endRecId; j++) {
//                            String mainSql = "INSERT INTO tmp_batch_transaction_item (txnRef, amount, batch, buId," +
//                                    " drAcct, createDt, currency, crAcct, module, narration, recId, recSt, reverse," +
//                                    " tries, `timestamp`) SELECT bankReference, amount, '" + reference + "', '" +
//                                    role.getBranchId() + "', '" + sysenv.CREDIT_TRANSFER_AWAITING_LEDGER + "', '" +
//                                    DateUtil.now("yyyy-MM-dd HH:mm:ss") + "', 'TZS', account, 'XAPI'," +
//                                    " description, '" + j + "', 'P', 'N', '0', '" +
//                                    DateUtil.now("yyyy-MM-dd HH:mm:ss") + "' FROM pensioners_payroll where" +
//                                    " id = ? and od_loan_status = '0'";
//                            try {
//                                jdbcTemplate.update(mainSql, firstId + j);
//                            } catch (Exception e) {
//                                LOGGER.info("Skipped due to exception");
//                                LOGGER.info(null, e);
//                            }
//                        }
//                    }
//                }
//                if (remainder > 0) {
//                    int startRecId = iterations * 5000 + 1;
//                    int endRecId = (int) successCount;
//                    String reference = batchRef + "_" + (iterations + 1);
//                    try {
//                        jdbcTemplate.update("INSERT INTO tmp_batch_transaction (reference, callbackUrl, createDt, endRecId," +
//                                        " itemCount, startRecId, `timestamp`, totalAmount, updateDt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
//                                reference, sysenv.LOCAL_CALLBACK_URL + "/api/batchCallback", DateUtil.now("yyyy-MM-dd HH:mm:ss"),
//                                successCount, remainder, startRecId, DateUtil.now(), remainingAmt, null);
//                    } catch (Exception e) {
//                        LOGGER.info("Skipped due to exception");
//                        LOGGER.info(null, e);
//                    }
//                    if (firstId != null) {
//                        for (int j = startRecId; j < endRecId; j++) {
//                            String mainSql = "INSERT INTO tmp_batch_transaction_item (txnRef, amount, batch, buId," +
//                                    " drAcct, createDt, currency, crAcct, module, narration, recId, recSt, reverse," +
//                                    " tries, `timestamp`) SELECT bankReference, amount, '" + reference + "', '" +
//                                    role.getBranchId() + "', '" + sysenv.CREDIT_TRANSFER_AWAITING_LEDGER + "', '" +
//                                    DateUtil.now("yyyy-MM-dd HH:mm:ss") + "', 'TZS', account, 'XAPI'," +
//                                    " description, '" + j + "', 'P', 'N', '0', '" +
//                                    DateUtil.now("yyyy-MM-dd HH:mm:ss") + "' FROM pensioners_payroll where" +
//                                    " id = ? and od_loan_status = '0'";
//                            try {
//                                jdbcTemplate.update(mainSql, firstId + j);
//                            } catch (Exception e) {
//                                LOGGER.info("Skipped due to exception");
//                                LOGGER.info(null, e);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        if (!batchReferences.isEmpty()) {
//            String[] arrBatchReferences = new String[batchReferences.size()];
//            batchReferences.toArray(arrBatchReferences);
//            String commaSeparatedBatchRefs = arrayToString(arrBatchReferences);
////            PensionPayrollToCoreBanking pData = new PensionPayrollToCoreBanking();
////            pData.setUserRoles(role);
////            pData.setBatchReference(commaSeparatedBatchRefs);
////            queProducer.sendToQueuepensionPayrollToCoreBanking(pData);
//
//            String[] args = new String[arrBatchReferences.length + 1];
//            args[0] = tissRef;
//            System.arraycopy(arrBatchReferences, 0, args, 1, arrBatchReferences.length);
//            try {
//                String inSql = String.join(",", Collections.nCopies(arrBatchReferences.length, "?"));
//                jdbcTemplate.update(
//                        String.format("UPDATE pensioners_batch_summary set status = 'P', tiss_ref = ? where reference in (%s)", inSql),
//                        args
//                );
//                jdbcTemplate.update("INSERT INTO pension_lumpsum_tiss (create_date, batches_refs, tiss_ref, total_amount," +
//                                " success_count, success_volume, failure_count, failure_volume, status) values (?,?,?,?,?,?,?,?,?)",
//                        DateUtil.now("yyyy-MM-dd"), commaSeparatedBatchRefs, tissRef, tissAmount, totalSuccessCount,
//                        successTxnVolume, totalFailedCount, failedTxnVolume, "P"
//                );
//                result = "{\"result\":\"0\",\"message\":\"Batches are being processed!\"}";
//            } catch (Exception ex) {
//                LOGGER.info("Validation supervisor split batches for processing: {}", ex.getMessage());
//            }
//        }
//        return result;
//    }

    //SPLIT BATCH INTO CHUNKS.
    public String processPensionPayroll(String tissRef, String tissAmount, List<HashMap<String, Object>> batchList,
                                        UsRole role) {
        String batchReferences = "";
        double totalSuccessCount = 0, totalFailedCount = 0;
        double successTxnVolume = 0, failedTxnVolume = 0;

        if (batchList.size() != 1) {
            return "{\"result\":\"92\",\"message\":\"Select one batch at a time.\"}";
        }

        for (HashMap<String, Object> map : batchList) {
            totalSuccessCount += (Double) map.get("successCount");
            totalFailedCount += (Double) map.get("failedCount");
            successTxnVolume += (Double) map.get("successTxnVolume");
            failedTxnVolume += (Double) map.get("failedTxnVolume");
            batchReferences = (String) map.get("reference");
        }

        pensionersPayrollService.processPayrollInChunks(batchReferences);

        pensionersPayrollService.processPayrollInChunksForLoanRepayment(batchReferences);

        try {
            jdbcTemplate.update("UPDATE pensioners_batch_summary set status = 'P', tiss_ref = ? where reference = ?", tissRef,batchReferences);
        } catch (Exception ex) {
            LOGGER.info("Validation supervisor split batches for processing: {}", ex.getMessage());
        }
        return "{\"result\":\"0\",\"message\":\"Payroll processed successfully!\"}";
    }

    public String processPensionPayrollOld(String tissRef, String tissAmount, List<HashMap<String, Object>> batchList,
                                           UsRole role) {
        List<String> batchReferences = new ArrayList<>();
        double totalSuccessCount = 0, totalFailedCount = 0;
        double successTxnVolume = 0, failedTxnVolume = 0;
        for (HashMap<String, Object> map : batchList) {
            totalSuccessCount += (Double) map.get("successCount");
            totalFailedCount += (Double) map.get("failedCount");
            successTxnVolume += (Double) map.get("successTxnVolume");
            failedTxnVolume += (Double) map.get("failedTxnVolume");
            String batchRef = (String) map.get("reference");
            batchReferences.add(batchRef);
        }
        String batchRefs = batchReferences.stream().map(str -> "'" + str + "'").collect(Collectors.joining(", "));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", UUID.randomUUID().toString())
                .addDate("date", new Date())
                .addLong("time", System.currentTimeMillis())
                .addString("batchRef", batchRefs).toJobParameters();

        JobExecution execution;
        try {
            execution = jobLauncher.run(job, jobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException e) {
            throw new RuntimeException(e);
        }
        System.out.println("STATUS :: " + execution.getStatus());
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            if (!batchReferences.isEmpty()) {
                String[] arrBatchReferences = new String[batchReferences.size()];
                batchReferences.toArray(arrBatchReferences);
                String commaSeparatedBatchRefs = arrayToString(arrBatchReferences);

                String[] args = new String[arrBatchReferences.length + 1];
                args[0] = tissRef;
                System.arraycopy(arrBatchReferences, 0, args, 1, arrBatchReferences.length);
                try {
                    String inSql = String.join(",", Collections.nCopies(arrBatchReferences.length, "?"));
                    jdbcTemplate.update(
                            String.format("UPDATE pensioners_batch_summary set status = 'P', tiss_ref = ? where reference in (%s)", inSql),
                            args
                    );
                    jdbcTemplate.update("INSERT INTO pension_lumpsum_tiss (create_date, batches_refs, tiss_ref, total_amount," +
                                    " success_count, success_volume, failure_count, failure_volume, status) values (?,?,?,?,?,?,?,?,?)",
                            DateUtil.now("yyyy-MM-dd"), commaSeparatedBatchRefs, tissRef, tissAmount, totalSuccessCount,
                            successTxnVolume, totalFailedCount, failedTxnVolume, "P"
                    );
                } catch (Exception ex) {
                    LOGGER.info("Validation supervisor split batches for processing: {}", ex.getMessage());
                }
            }
            return "{\"result\":\"0\",\"message\":\"Payroll processed successfully!\"}";
        } else {
            return "{\"result\":\"99\",\"message\":\"Failed to process payroll!\"}";
        }
    }

    /*
     *  QUERY BATCH
     *
     */
    public String queryBatch(String batchReference, String nbOfTxn, String startRecId, String endRecId) {
        QueryBatchResponse queryBatchResponse = new QueryBatchResponse();
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <api:queryBatch>\n" +
                "         <request>\n" +
                "            <reference>" + System.currentTimeMillis() + "</reference>\n" +
                "            <batch>" + batchReference + "</batch>\n" +
                "            <queryInfo>\n" +
                "               <endRecId>" + endRecId + "</endRecId>\n" +
                "               <itemCount>" + nbOfTxn + "</itemCount>\n" +
                "               <startRecId>" + startRecId + "</startRecId>\n" +
                "            </queryInfo>\n" +
                "         </request>\n" +
                "      </api:queryBatch>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        request = request.replace("\n", "");
        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(request, sysenv.CHANNEL_MANAGER_API_URL, "xapi", "x@pi#81*");
        if (soapResponse.contains("return")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.info("Final response xml... {}", sxml);
            //convert xml to java object
            queryBatchResponse.setResult(XMLParserService.getDomTagText("result", sxml));
            queryBatchResponse.setReference(XMLParserService.getDomTagText("reference", sxml));
            queryBatchResponse.setMessage(XMLParserService.getDomTagText("message", sxml));
            queryBatchResponse.setTxnId(XMLParserService.getDomTagText("txnId", sxml));
        }
        try {
            return jacksonMapper.writeValueAsString(queryBatchResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     *  PROCESS BATCH
     *
     */
    public String processBatch(String batchReference, BigDecimal totalAmount, String callbackUrl, List<Txn> txns) {
        QueryBatchResponse queryBatchResponse = new QueryBatchResponse();
        StringBuilder request = new StringBuilder("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <api:processBatch>\n" +
                "         <request>\n" +
                "            <reference>" + batchReference + "</reference>\n" +
                "            <itemCount>" + txns.size() + "</itemCount>\n" +
                "            <totalAmount>" + totalAmount + "</totalAmount>\n" +
                "            <callbackUrl>" + callbackUrl + "</callbackUrl>\n" +
                "            <txns>\n");
        for (Txn txn : txns) {
            request.append("<txn>\n" + "<amount>").append(txn.getAmount()).append("</amount>\n")
                    .append("<batch>").append(txn.getBatch()).append("</batch>\n").append("<buId>")
                    .append(txn.getBuId()).append("</buId>\n").append("<charge>").append(txn.getCharge())
                    .append("</charge>\n").append("<chgId>").append(txn.getChgId()).append("</chgId>\n")
                    .append("<code>").append(txn.getCode()).append("</code>\n").append("<crAcct>")
                    .append(txn.getCrAcct()).append("</crAcct>\n").append("<createDt>").append(txn.getCreateDt())
                    .append("</createDt>\n").append("<currency>").append(txn.getCurrency()).append("</currency>\n")
                    .append("<drAcct>").append(txn.getDrAcct()).append("</drAcct>\n").append("<millis>")
                    .append(txn.getMillis()).append("</millis>\n").append("<module>").append(txn.getModule())
                    .append("</module>\n").append("<narration>").append(txn.getNarration()).append("</narration>\n")
                    .append("<recId>").append(txn.getRecId()).append("</recId>\n").append("<recSt>")
                    .append(txn.getRecSt()).append("</recSt>\n").append("<reqRef>").append(txn.getReqRef())
                    .append("</reqRef>\n").append("<result>").append(txn.getResult()).append("</result>\n")
                    .append("<reverse>").append(txn.getReverse()).append("</reverse>\n").append("<scheme>")
                    .append(txn.getScheme()).append("</scheme>\n").append("<tries>").append(txn.getTries())
                    .append("</tries>\n").append("<txnId>").append(txn.getTxnId()).append("</txnId>\n")
                    .append("<txnRef>").append(txn.getTxnRef()).append("</txnRef>\n").append("</txn>\n");
        }
        request.append("</txns>\n" + "</request>\n" + "</api:processBatch>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>");
        request = new StringBuilder(request.toString().replace("\n", ""));
        //send http request to wsdl server
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(request.toString(), sysenv.CHANNEL_MANAGER_API_URL, "xapi", "x@pi#81*");
        if (soapResponse.contains("return")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.info("Final response xml... {}", sxml);
            //convert xml to java object
            queryBatchResponse.setResult(XMLParserService.getDomTagText("result", sxml));
            queryBatchResponse.setReference(XMLParserService.getDomTagText("reference", sxml));
            queryBatchResponse.setMessage(XMLParserService.getDomTagText("message", sxml));
            queryBatchResponse.setTxnId(XMLParserService.getDomTagText("txnId", sxml));
//            queryBatchResponse.setTxns(XMLParserService.getDomTagText("txns", sxml));
        }
        try {
            return jacksonMapper.writeValueAsString(queryBatchResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, Object>> getPensionPayrollEntries(String batchReference) {
        try {
            return this.jdbcTemplate.queryForList("select * from pensioners_payroll where batchReference=? and status='V' and cbs_status='I'", batchReference);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON pensioners_payroll TABLE: {}", ex.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getPensionPayrollEntries(String[] batchReferences) {
        try {
            String inSql = String.join(",", Collections.nCopies(batchReferences.length, "?"));
            return jdbcTemplate.queryForList(
                    String.format("select * from pensioners_payroll where batchReference in (%s) and status='V' and cbs_status='I'", inSql),
                    batchReferences
            );
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING TRANSACTION ON pensioners_payroll TABLE: {}", ex.getMessage());
            return null;
        }
    }

    public String arrayToString(String[] array) {
        String result = "";
        if (array.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : array) {
                sb.append(s).append(",");
            }
            result = sb.deleteCharAt(sb.length() - 1).toString();
        }
        return result;
    }

    public String arrayToStringWithQuotes(String[] array) {
        String result = "";
        if (array.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : array) {
                sb.append("'").append(s).append("'").append(",");
            }
            result = sb.deleteCharAt(sb.length() - 1).toString();
        }
        return result;
    }

    public String processMultiplePensionPayrollToCoreBanking(String[] batchReferences, UsRole role) {
        String result = "{\"result\":\"99\",\"message\":\"An Error occurred During processing-Timeout Please confirm on Rubikon: \"}";

        try {
            //get the MESSAGE DETAILS FROM THE QUEUE
            List<Map<String, Object>> txn = getPensionPayrollEntries(batchReferences);

            if (txn != null) {
                if (!txn.isEmpty()) {
                    for (Map<String, Object> stringObjectMap : txn) {
                        PensionPayrollToCoreBanking pdata = new PensionPayrollToCoreBanking();
                        pdata.setAmount(new BigDecimal(stringObjectMap.get("amount") + ""));
                        pdata.setBeneficiaryAccount(stringObjectMap.get("account") + "");
                        pdata.setCurrency(stringObjectMap.get("currency") + "");
                        pdata.setNarration(stringObjectMap.get("description") + "");
                        pdata.setReference(stringObjectMap.get("bankReference").toString());
                        pdata.setUserRoles(role);
                        pdata.setTrackingNo(stringObjectMap.get("trackingNo") + "");
                        pdata.setBeneficiaryName(stringObjectMap.get("name") + "");
                        pdata.setBatchReference(stringObjectMap.get("batchReference") + "");
                        queProducer.sendToQueuepensionPayrollToCoreBanking(pdata);
                    }
                    try {
                        String inSql = String.join(",", Collections.nCopies(batchReferences.length, "?"));
                        jdbcTemplate.update(
                                String.format("UPDATE pensioners_batch_summary set status = 'P' where reference in (%s)", inSql),
                                batchReferences
                        );
                        result = "{\"result\":\"0\",\"message\":\"Batches are being processed!\"}";
                    } catch (EmptyResultDataAccessException e) {
                        LOGGER.info("Validation officer process batch (update pensioners_batch_summary exception): {}", e.getMessage());
                    }
                }
            } else {
                result = "{\"result\":\"101\",\"message\":\"[EXCEPTION] An Error occurred During processing Please Try Again!!!!!!!: \"}";
            }
            return result;
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            LOGGER.error("RTGS EXCEPTION FAILED: {} BranchCode: {} USERNAME: {}", ex, role.getBranchCode(), role.getUserName());
            result = result;
        }
        return result;
    }

    public String generatePensionPayrollBatchFile(UsRole role, HttpServletResponse response) {
        String reportFileTemplate = "/iReports/pension/pension_payroll.jasper";
        List<Map<String, Object>> referenceList;
        try {
            referenceList = jdbcTemplate.queryForList("SELECT reference FROM pensioners_batch_summary WHERE status = 'P'");
            String tissRef = jdbcTemplate.queryForObject("SELECT tiss_ref FROM pension_lumpsum_tiss WHERE status = 'P' order by id desc limit 1", String.class);
            List<String> batchReferences = new ArrayList<>();
            if (!referenceList.isEmpty()) {
                for (Map<String, Object> map : referenceList) {
                    batchReferences.add((String) map.get("reference"));
                }
                String[] arrBatchReferences = new String[batchReferences.size()];
                batchReferences.toArray(arrBatchReferences);
                String commaSeparatedBatchRefs = arrayToStringWithQuotes(arrBatchReferences);
                try {
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put("PRINTED_BY", role.getUserName());
                    parameters.put("PRINTED_AT", DateUtil.now("dd/MM/yyyy HH:mm:ss"));
                    parameters.put("BATCH_REFERENCES", commaSeparatedBatchRefs);
                    parameters.put("TISS_REFERENCE", tissRef);
                    JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, cilantroDataSource.getConnection());
                    return jasperService.exportFileOption(print, "excel", response, "PEN_" + DateUtil.now("yyyyMMddHHmmss"));
                } catch (IOException ex) {
                    Logger.getLogger(PensionRepo.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    LOGGER.info(null, ex);
                    Logger.getLogger(PensionRepo.class.getName()).log(Level.SEVERE, null, ex);
                }
                String likeTerm = "'%" + arrayToString(arrBatchReferences) + "%'";
                //UPDATE pension_lumpsum_tiss
                String sql = "UPDATE pension_lumpsum_tiss set approved_by = ?, approved_date = ? where batches_refs like ?";
                jdbcTemplate.update(sql, role.getUserName(), DateUtil.now("yyyy-MM-dd HH:mm:ss"), likeTerm);
                String inSql = String.join(",", Collections.nCopies(arrBatchReferences.length, "?"));
                jdbcTemplate.update(
                        String.format("UPDATE pensioners_batch_summary set status = 'C' where reference in (%s)", inSql),
                        arrBatchReferences
                );
            }
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Failed to fetch batch (select from pensioners_batch_summary exception): {}", e.getMessage());
        }
        return null;
    }

    public String getTempBatchesForProcessing(String fromDate, String toDate, String draw, String start, String rowPerPage,
                                              String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        int totalRecordWithFilter;
        int totalRecords;
        String jsonString;
        try {
            totalRecords = jdbcTemplate.queryForObject("SELECT count(*) FROM tmp_batch_transaction where result <> '00'", Integer.class);
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                String searchQuery = " WHERE result <> '00' and reference LIKE ?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM tmp_batch_transaction " + searchQuery, new Object[]{searchValue}, Integer.class);
                results = this.jdbcTemplate.queryForList("SELECT * FROM tmp_batch_transaction " + searchQuery + "  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage, new Object[]{searchValue});
            } else {
                totalRecordWithFilter = totalRecords;
                results = this.jdbcTemplate.queryForList("SELECT * FROM tmp_batch_transaction where result <> '00' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            LOGGER.info("ERROR ON QUERYING tmp_batch_transaction TABLE: {}", ex.getMessage());
            return null;
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" +
                totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
        LOGGER.info("RESULTS:{}", results);
        return json;
    }

    public String processTempBatches(String batchReference) {
        try {
            TmpBatchTransaction summary =  tmpBatchTransactionRepository.findByReference(batchReference).orElseThrow(()->new EntityNotFoundException("Batch is not found"));
            summary.setResult("00");
            summary.setCompletedDt(new Date());
            summary.setMessage("Submitted awaiting response");
            tmpBatchTransactionRepository.save(summary);
            List<PensionersPayroll> list =  pensionersPayrollRepository.findBySubBatchReference(batchReference);
//            List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM tmp_batch_transaction_item WHERE batch = ?",
//                    batchReference);
            totalAmount = new BigDecimal(0);

            List<Txn> txns = list.stream().map(m -> {
                totalAmount = totalAmount.add(m.getAmount());
                Txn txn = new Txn();
                txn.setAmount(String.valueOf(m.getAmount()));
                txn.setTxnRef(String.valueOf(m.getBankReference()));
                txn.setCurrency(String.valueOf(m.getCurrency()));
                txn.setBatch(String.valueOf(m.getSubBatchReference()));
                txn.setResult("-1");
                txn.setBuId("-5");
                txn.setCrAcct(String.valueOf(m.getAccount()));
                txn.setDrAcct(String.valueOf(sysenv.CREDIT_TRANSFER_AWAITING_LEDGER));
                txn.setCreateDt(DateUtil.datetimeToStr(m.getCreateDt()));
                txn.setModule(String.valueOf("PENSION PAYROLL"));
                txn.setNarration(m.getDescription());
                txn.setRecId(String.valueOf(m.getId()));
                txn.setRecSt(m.getStatus());
                txn.setResult(m.getResponseCode());
                txn.setReverse("N");
                txn.setScheme("?");
                txn.setChannel(null);
                txn.setTries("1");
                return txn;
            }).collect(Collectors.toList());

            String response =  processBatch(batchReference, totalAmount, sysenv.LOCAL_CALLBACK_URL
                    + "/api/batchCallback", txns);

            QueryBatchResponse json = jacksonMapper.readValue(response,QueryBatchResponse.class);
            summary.setResult(json.getResult().equals("0") ? "00" : json.getResult());
            summary.setCompletedDt(new Date());
            summary.setMessage("Success fully processed");
            tmpBatchTransactionRepository.save(summary);
            return response;
        } catch (Exception e) {
            LOGGER.info("Failed to fetch and/or process batches, exception: {}",e.getMessage(), e);
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public String processTempBatchesOld(String batchReference){
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM tmp_batch_transaction_item WHERE batch = ?",
                    batchReference);
            totalAmount = new BigDecimal(0);
            List<Txn> txns = list.stream().map(m -> {
                totalAmount = totalAmount.add(new BigDecimal(m.get("amount").toString()));
                Txn txn = new Txn();
                txn.setAmount(String.valueOf(m.get("amount")));
                txn.setTxnRef(String.valueOf(m.get("txnRef")));
                txn.setCurrency(String.valueOf(m.get("currency")));
                txn.setBatch(String.valueOf(m.get("batch")));
                txn.setResult(String.valueOf(m.get("result")));
                txn.setBuId("-5"); //String.valueOf(m.get("buId"))
                txn.setCrAcct(String.valueOf(m.get("crAcct")));
                txn.setDrAcct(String.valueOf(m.get("drAcct")));
                txn.setCreateDt(String.valueOf(m.get("createDt")));
                txn.setModule(String.valueOf(m.get("module")));
                txn.setNarration(String.valueOf(m.get("narration")));
                txn.setRecId(String.valueOf(m.get("recId")));
                txn.setRecSt(String.valueOf(m.get("recSt")));
                txn.setResult(String.valueOf(m.get("result")));
                txn.setReverse(String.valueOf(m.get("reverse")));
                txn.setScheme("?");
                txn.setChannel(String.valueOf(m.get("channel")));
                txn.setTries(String.valueOf(m.get("tries")));
                return txn;
            }).collect(Collectors.toList());

            return processBatch(batchReference, totalAmount, sysenv.LOCAL_CALLBACK_URL
                    + "/api/batchCallback", txns);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Failed to fetch and/or process batches, exception: {}", e.getMessage());
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
    }

    public String previewTempBatchTransactions(String reference, String draw, String start, String rowPerPage,
                                               String searchValue, String columnIndex, String columnName,
                                               String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordWithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM tmp_batch_transaction_item where batch=?";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{reference}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(txnRef,' ',crAcct,' ',narration,' ',result,' ',tries,' ',amount) LIKE ? and batch=?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("SELECT count(*) FROM pensioners_payroll " + searchQuery, new Object[]{searchValue, reference}, Integer.class);
                mainSql = "SELECT * FROM tmp_batch_transaction_item " + searchQuery + "  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, reference});
            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "SELECT * FROM tmp_batch_transaction_item a where batch=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{reference});
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public String updateTransactionByRef(HashMap<String, String> data) {
        String sql = "UPDATE tmp_batch_transaction_item set recId = ?, result = ?, recSt = ?, tries = ?, reverse = ? where txnRef = ?";
        int update;
        String status;
        String recId = data.get("recId");
        String result = data.get("result");
        String recSt = data.get("recSt");
        String tries = data.get("tries");
        String reverse = data.get("reverse");
        String txnRef = data.get("txnRef");
        if (result.equals("00")) {
            status = "C";
        } else {
            status = "F";
        }
        try {
//            LOGGER.info(sql.replace("?", "'{}'"), recId, result, recSt, tries, reverse, txnRef);
//            int update0 = jdbcTemplate.update(sql, recId, result, recSt, tries, reverse, txnRef);

            sql = "UPDATE pensioners_payroll set status = ?, responseCode = ?, cbs_status = ?, approved_by = ?, approved_dt = ? where bankReference = ?";

            update = jdbcTemplate.update(sql, status, result, recSt, "SYSTEM", DateUtil.now(), txnRef);

            LOGGER.info(sql.replace("?", "'{}'") + ", update:{}", status, result, recSt, "SYSTEM", DateUtil.now(), txnRef, update);
        } catch (DataAccessException e) {
            LOGGER.info(null, e);
            update = 0;
        }
        if (update > 0) {
            return updateBatchSuccessCount(data.get("batch"));
        } else {
            return updateBatchFailureCount(data.get("batch"));
        }
    }

    public String updateBatchSuccessCount(String reference) {
        String sql = "UPDATE tmp_batch_transaction SET successCount = successCount + 1 where reference = ?";
        int update = jdbcTemplate.update(sql, reference);
        if (update > 0) {
            return "{\n\"result\": 0,\n\"message\": \"Success\"\n}";
        } else {
            return "{\n\"result\": 91,\n\"message\": \"Callback Failed!\"\n}";
        }
    }

    public String updateBatchFailureCount(String reference) {
        String sql = "UPDATE tmp_batch_transaction SET failureCount = failureCount + 1 where reference = ?";
        int update = jdbcTemplate.update(sql, reference);
        if (update > 0) {
            return "{\n\"result\": 0,\n\"message\": \"Success\"\n}";
        } else {
            return "{\n\"result\": 91,\n\"message\": \"Callback Failed!\"\n}";
        }
    }

    public String updatePensionPayrollOdStatus(HashMap<String, String> data) {
//        int update = jdbcTemplate.update("UPDATE pensioners_payroll SET od_loan_status = ? WHERE bankReference = ?", 0, data.get("txnRef"));
//        String sql = "UPDATE tmp_batch_transaction_item set recId = ?, result = ?, recSt = ?, tries = ?, reverse = ?," +
//                " updateDt = ? where txnRef = ?";
//        int update = jdbcTemplate.update(sql, data.get("recId"), data.get("result"), data.get("recSt"), data.get("tries"),
//                data.get("reverse"), DateUtil.now("yyyy-MM-dd HH:mm:ss"), data.get("txnRef"));
        //TODO:
        String status =null;
        if ( data.get("result").equals("00")) {
            status = "C";
        } else {
            status = "F";
        }
        String  sql = "UPDATE pensioners_payroll set status = ?, responseCode = ?, cbs_status = ?, approved_by = ?, approved_dt = ? where bankReference = ?";
        int update = jdbcTemplate.update(sql, status, data.get("result"), data.get("result"), "SYSTEM", DateUtil.now(), data.get("txnRef"));

        String reference;
        try {
            reference = jdbcTemplate.queryForObject("SELECT sub_batch_reference FROM pensioners_payroll WHERE bankReference = ?",
                    String.class, data.get("txnRef"));
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Failed to find batch for this txn reference, exception: {}", e.getMessage());
            return "{\n\"result\": 999,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
        if (update > 0) {
            if (data.get("result").equals("00")) {
                return updateBatchSuccessCount(reference);
            } else {
                return updateBatchFailureCount(reference);
            }
        } else {
            return "{\n\"result\": 99,\n\"message\": \"Callback Failed!\"\n}";
        }
    }


    public String getBatchDescription(String reference) {
        try {
            return jdbcTemplate.queryForObject("SELECT batchDescription FROM pensioners_batch_summary WHERE reference = ?",
                    String.class, reference);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Failed to find batch for this reference, exception: {}", e.getMessage());
            return null;
        }
    }

    public String authorizeTransactionsWithReason(String[] batchReferences, String reason) {
        try {
            String inSql = String.join(",", Collections.nCopies(batchReferences.length, "?"));
            int result = jdbcTemplate.update(
                    String.format("UPDATE pensioners_batch_summary set status = 'V' where reference in (%s)", inSql),
                    batchReferences
            );

            String queryResult = getListOfOverdraftPensioners();
            JSONObject object = new JSONObject(queryResult);
            if (Objects.equals(object.getString("responseCode"), "0") && object.getString("message").contains("Success")) {
               // updateTempBatchesAndTransactions(batchReferences);
                String message;
                if (result > 0) message = "Batch authorized!";
                else message = "Failed authorization!";
                return "{\n\"result\": 0,\n\"message\": \"" + message + "\"\n}";
            } else
                return queryResult;
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Officer authorize transactions (update pensioners_batch_summary exception): {}", e.getMessage());
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        }
    }


    public String getListOfOverdraftPensioners() {
        try {
            LocalDate date = LocalDate.now();
            String toDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fromDate = "2023-03-01";
            String requestBody = "{\"loanType\": \"PENSIONA\", \"fromDate\": \"" + fromDate + "\", \"toDate\": \"" + toDate + "\"}";
            Map<String, String> response = HttpClientService.sendTxnToBrinjal(requestBody,
                    sysenv.BRINJAL_OUTSTANDING_LOANS_URL, "", "POST");
            String res = response.get("responseBody");
            ODPensionersResp resp = jacksonMapper.readValue(res, ODPensionersResp.class);
            if (resp.getReponseCode().equals("0")) {
                if (!resp.getOutstandingLoans().isEmpty()) {
                    Double sum = resp.getOutstandingLoans().stream().mapToDouble(OutstandingLoan::getTotalOutstandingLoan)
                            .sum();
                    LOGGER.info("Sum of outstanding accounts is {}", sum);
                    int size = resp.getOutstandingLoans().size();
                    for (int j = 0; j < size; j++) {
                        OutstandingLoan outstandingLoan = resp.getOutstandingLoans().get(j);

                        String mainSql = "UPDATE pensioners_payroll set od_loan_status = '1' where account = ? and payroll_month = ? and payroll_year = ?";
                        try {
                            jdbcTemplate.update(mainSql, outstandingLoan.getAccountNo(), date.getMonthValue(), date.getYear());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return "{\"responseCode\": \"0\", \"message\":\"Success (" + size + " accounts)\"}";
                } else {
                    return "{\"responseCode\": \"0\", \"message\":\"No outstanding loans\"}";
                }
            } else {
                return "{\"responseCode\": \"99\", \"message\":\"Failed\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"responseCode\": \"99\", \"message\":\"Failed\"}";
        }
    }

    public void updateTempBatchesAndTransactions(String[] batchReferences) {
        queProducer.insertIntoTempFromPensioners(batchReferences);
    }

    public String processPensionersOverdraft(String batchReference) {

        TmpBatchTransaction summary =  tmpBatchTransactionRepository.findByReference(batchReference).orElseThrow(()->new EntityNotFoundException("Batch is not found"));
        List<PensionersPayroll> list =  pensionersPayrollRepository.findBySubBatchReference(batchReference);
        summary.setResult("00");
        summary.setCompletedDt(new Date());
        summary.setMessage("Submitted waiting final response");
        tmpBatchTransactionRepository.save(summary);

        BatchPayemntReq batchPayemntReq = new BatchPayemntReq();
        batchPayemntReq.setReference(batchReference);
        batchPayemntReq.setCallbackUrl(sysenv.LOCAL_CALLBACK_URL + "/api/pension/od/repay");
        batchPayemntReq.setItemCount(String.valueOf(summary.getItemCount()));
        batchPayemntReq.setTotalAmount(String.valueOf(summary.getTotalAmount()));
        BatchTxnsEntry batchTxnsEntry = new BatchTxnsEntry();
        List<BatchTxnEntries> txnEntries = new ArrayList<>();
        for (PensionersPayroll txn : list) {
            BatchTxnEntries batchTxnEntries = new BatchTxnEntries();
            batchTxnEntries.setAmount(txn.getAmount());
            batchTxnEntries.setBatch(batchReference);
            batchTxnEntries.setBuId("-5");
            batchTxnEntries.setCurrency(txn.getCurrency());
            batchTxnEntries.setCrAcct(txn.getAccount());
            batchTxnEntries.setCode("?");
            batchTxnEntries.setDrAcct(sysenv.CREDIT_TRANSFER_AWAITING_LEDGER);
            batchTxnEntries.setModule("PENSION PAYROLL");
            batchTxnEntries.setReverse("N");
            batchTxnEntries.setNarration(txn.getDescription());
            batchTxnEntries.setScheme("?");
            batchTxnEntries.setTxnRef(txn.getBankReference());
            txnEntries.add(batchTxnEntries);
        }
        batchTxnsEntry.setTxn(txnEntries);
        batchPayemntReq.setTxns(batchTxnsEntry);
        String jsonString;
        try {
            jsonString = jacksonMapper.writeValueAsString(batchPayemntReq);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try {
            Map<String, String> response = HttpClientService.sendTxnToBrinjal(jsonString, sysenv.BRINJAL_API_URL +
                    "/esb/loan/mandatoryPayments", "", "POST");

            LoanRepaymentResp json = jacksonMapper.readValue(response.get("responseBody"), LoanRepaymentResp.class);
            if(json != null) {
                summary.setResult(json.getResponseCode().equals("0") ? "00" : json.getResponseCode());
                summary.setMessage(json.getResponseCode().equals("0") ? "Success fully processed." : json.getMessage());
                summary.setCompletedDt(new Date());
                tmpBatchTransactionRepository.save(summary);
            }
            return response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String processPensionersOverdraft2(List<LoanRepaymentReq> list) {
        LOGGER.info("List<LoanRepaymentReq> list {}", list);
        try {
            JSONArray array = new JSONArray();
            for (LoanRepaymentReq req : list) {
                String jsonString;
                try {
                    jsonString = jacksonMapper.writeValueAsString(req);
                } catch (JsonProcessingException e) {
                    LOGGER.info(null, e);
                    throw new RuntimeException(e);
                }
                try {
                    Map<String, String> response = HttpClientService.sendTxnToBrinjal(jsonString, sysenv.BRINJAL_API_URL +
                            "/esb/loan/repayment", "", "POST");
                    LOGGER.info("Response body: {}", response.get("responseBody"));
                    array.put(response.get("responseBody"));
                } catch (Exception e) {
                    LOGGER.info("Exception in sending...");
                    LOGGER.info(null, e);
                    return null;
                }
            }
            return "{\"responseCode\": \"0\", \"message\": " + array + "}";
//            List<Map<String, Object>> txns = jdbcTemplate.queryForList("SELECT * FROM pensioners_payroll WHERE od_loan_status = 1");
//            BigDecimal totalAmt = jdbcTemplate.queryForObject("SELECT totalAmount FROM tmp_batch_transaction WHERE " +
//                    "reference = '" + batchReference + "'", BigDecimal.class);
//            BatchPayemntReq batchPayemntReq = new BatchPayemntReq();
//            batchPayemntReq.setReference(batchReference);
//            batchPayemntReq.setCallbackUrl(sysenv.LOCAL_CALLBACK_URL + "/api/pension/od/repay");
//            batchPayemntReq.setItemCount(String.valueOf(txns.size()));
//            batchPayemntReq.setTotalAmount(String.valueOf(totalAmt));
//            BatchTxnsEntry batchTxnsEntry = new BatchTxnsEntry();
//            List<BatchTxnEntries> txnEntries = new ArrayList<>();
//            for (Map<String, Object> txn : txns) {
//                BatchTxnEntries batchTxnEntries = new BatchTxnEntries();
//                batchTxnEntries.setAmount((BigDecimal) txn.get("amount"));
//                batchTxnEntries.setBatch(batchReference);
//                batchTxnEntries.setBuId("-5");
//                batchTxnEntries.setCurrency((String) txn.get("currency"));
//                batchTxnEntries.setCrAcct((String) txn.get("account"));
//                batchTxnEntries.setCode((String) txn.get("bankCode"));
//                batchTxnEntries.setDrAcct(sysenv.CREDIT_TRANSFER_AWAITING_LEDGER);
//                batchTxnEntries.setModule("PENSION PAYROLL");
//                batchTxnEntries.setReverse("N");
//                batchTxnEntries.setNarration((String) txn.get("description"));
//                batchTxnEntries.setScheme("?");
//                batchTxnEntries.setTxnRef((String) txn.get("bankReference"));
//                txnEntries.add(batchTxnEntries);
//            }
//            batchTxnsEntry.setTxn(txnEntries);
//            batchPayemntReq.setTxns(batchTxnsEntry);
//
//            String jsonString;
//            try {
//                jsonString = jacksonMapper.writeValueAsString(batchPayemntReq);
//            } catch (JsonProcessingException e) {
//                LOGGER.info(null, e);
//                throw new RuntimeException(e);
//            }
//            try {
//                Map<String, String> response = HttpClientService.sendTxnToBrinjal(jsonString, sysenv.BRINJAL_API_URL +
//                        "/esb/loan/mandatoryPaymentsNoNetpay", "", "POST");
//                return response.get("responseBody");
//            } catch (Exception e) {
//                LOGGER.info("Exception in sending...");
//                LOGGER.info(null, e);
//                return null;
//            }
        } catch (Exception m) {
            LOGGER.info(null, m);
            return null;
        }
    }

    public String returnTransactionWithReason(String batchReference, String[] bankReferences, Boolean all, String username) {
        try {
            String inSql = String.join(",", Collections.nCopies(bankReferences.length, "?"));
            BigDecimal sum;
            List<Map<String, Object>> list;
            List<PsssfBatchBeneficiary> beneficiaries = new ArrayList<>();
            try {
                Map<String, Object> response = this.jdbcTemplate.queryForMap("select * from transfers where reference" +
                        " = (select tiss_ref from pensioners_batch_summary where reference = ?)", batchReference);
                String destinationAcct = (String) response.get("sourceAcct");
                String beneficiaryBIC = (String) response.get("senderBIC");
                String beneficiaryName = (String) response.get("sender_name");
                String senderBIC = (String) response.get("beneficiaryBIC");
                String senderName = (String) response.get("beneficiaryName");
                String reference = "STP" + DateUtil.now("yyyyMMddHHmm");
                sum = this.jdbcTemplate.queryForObject(
                        String.format("select sum(amount) from pensioners_payroll where bankReference in (%s)",
                                inSql), bankReferences, BigDecimal.class);
                list = jdbcTemplate.queryForList(
                        String.format("select * from pensioners_payroll where bankReference in (%s)", inSql),
                        bankReferences);
                LOGGER.info(String.format("select * from pensioners_payroll where bankReference in (%s)", inSql).
                        replace("?", "'{}'"), bankReferences);
                for (Map<String, Object> map : list) {
                    PsssfBatchBeneficiary psssfBatchBeneficiary = new PsssfBatchBeneficiary();
                    psssfBatchBeneficiary.setACCOUNT((String) map.get("account"));
                    psssfBatchBeneficiary.setAMOUNT(String.valueOf(map.get("amount")));
                    psssfBatchBeneficiary.setPENSIONER_ID((String) map.get("pensioner_id"));
                    psssfBatchBeneficiary.setCURRENCY((String) map.get("currency"));
                    psssfBatchBeneficiary.setCHANNELIDENTIFIER((String) map.get("channel_identifier"));
                    psssfBatchBeneficiary.setNARRATION((String) map.get("description"));
                    psssfBatchBeneficiary.setDESTINATIONCODE((String) map.get("institution_id"));
                    psssfBatchBeneficiary.setID((String) map.get("bankReference"));
                    psssfBatchBeneficiary.setNAME((String) map.get("name"));
                    psssfBatchBeneficiary.setREASON((String) map.get("message"));
                    beneficiaries.add(psssfBatchBeneficiary);
                }

                Map<String, Object> map = jdbcTemplate.queryForMap("select reference, batchDescription, tiss_ref " +
                        "from pensioners_batch_summary where reference = ?", batchReference);
                String tissRef = (String) map.get("tiss_ref");

                String reason = tissRef + " - " + map.get("reference") + " - " + map.get("batchDescription");
                String type = "001";
                if (tissRef.length() > 16) {
                    type = "005";
                }

                String swiftMessage = SwiftService.createMT103FromOnlineReq(createPaymentRequest(sum, (String) map.get("tiss_ref"),
                        destinationAcct, beneficiaryBIC + "==", "255262321069", beneficiaryName,
                        "", "https://172.20.1.10/onwsc/paymentCallback", "", "TZS",
                        "060", reason, null, null, reference, sysenv.CREDIT_TRANSFER_AWAITING_LEDGER,
                        "Head Office 10th LAPF Towers,Bagamoyo Road, Opp Makumbusho Village,Kijitonyama," +
                                "P.O BOX 9300, Dar es salaam", senderName, "255765767683", "0", type, "0"));
                this.jdbcTemplate.update("INSERT INTO transfers (message_type, txn_type, sourceAcct, destinationAcct," +
                                " amount, charge, currency, beneficiaryName, beneficiaryBIC, beneficiary_contact," +
                                " senderBIC, sender_phone, sender_address, sender_name, reference, txid, instrId," +
                                " batch_reference, batch_reference2, code, supportingDocument, status, response_code," +
                                " comments, purpose, swift_message, direction, originallMsgNmId, initiated_by," +
                                " returned_by, modified_by, branch_approved_by, hq_approved_by, value_date, create_dt," +
                                " modified_dt, returned_dt, branch_approved_dt, hq_approved_dt, branch_no, cbs_status," +
                                " message, callbackurl, ibankstatus, units, swift_status, compliance_officer, compliance_dt," +
                                " compliance, corresponding_bic)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
                                " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "103", "001", sysenv.CREDIT_TRANSFER_AWAITING_LEDGER, destinationAcct, sum, null, "TZS",
                        beneficiaryName, beneficiaryBIC, null, senderBIC, null, null, senderName, reference, reference,
                        reference, reference, null, "-1", null, "I", "0", reason, "Return failed transactions to PSSSF",
                        swiftMessage, "OUTGOING", null, username, null, null, "SYSTEM", null, null, DateUtil.now(), null,
                        null, DateUtil.now(), null, "060", "P", "Success", "https://172.20.1.10/onwsc/paymentCallback",
                        null, null, null, null, null, null, "-1");
            } catch (Exception ex) {
                LOGGER.info("ERROR ON QUERYING FAILED TRANSACTIONS ON pensioners_payroll TABLE: {}", ex.getMessage());
                return null;
            }

            PsssfBatchRequest psssfBatchRequest = new PsssfBatchRequest();
            psssfBatchRequest.setBatchDate(DateUtil.now("yyyy-MM-dd"));
            psssfBatchRequest.setBatchDescription("Returned batch");
            psssfBatchRequest.setNoOfPensioners(String.valueOf(bankReferences.length));
            psssfBatchRequest.setInstitution("PSSSF");
            psssfBatchRequest.setTotalAmount(sum.toString());
            psssfBatchRequest.setUserid("SYSTEM");
            psssfBatchRequest.setBatchno(batchReference);
            psssfBatchRequest.setBeneficiaries(beneficiaries);

            jacksonMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            String jsonString = jacksonMapper.writeValueAsString(psssfBatchRequest);
            LOGGER.info("REQUEST: {}", jsonString);
            String response = HttpClientService.sendTxnToAPI(jsonString, sysenv.PSSSF_URL + "?r=api/tcb-payment/transaction-status");
            LOGGER.info("RESPONSE: {}", response);
            JSONObject resultObject = new JSONObject(response);
            String responseCode = resultObject.getString("responseCode");

            String message;
            if (responseCode.equals("200")) {
                if (bankReferences.length > 1)
                    message = "Transactions have been returned successfully!";
                else
                    message = "Transaction has been returned successfully!";
            } else message = "Failed to return!";
            return "{\n\"result\": \"" + responseCode + "\",\n\"message\": \"" + message + "\"\n}";
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Return transactions: {}", e.getMessage());
            return "{\n\"result\": 99,\n\"message\": \"" + e.getMessage() + "\"\n}";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public PaymentReq createPaymentRequest(BigDecimal amount, String batchReference, String beneficiaryAcct,
                                           String beneficiaryBIC, String beneficiaryContact, String beneficiaryName,
                                           String boundary, String callbackUrl, String chargeCategory, String currency,
                                           String customerBranch, String description, String initiatorId, String intermediaryBank,
                                           String reference, String senderAccount, String senderAddress, String senderName,
                                           String senderPhone, String specialRateToken, String type, String spRate) {
        PaymentReq paymentReq = new PaymentReq();
        paymentReq.setAmount(amount);
        paymentReq.setBatchReference(batchReference);
        paymentReq.setBeneficiaryAccount(beneficiaryAcct);
        paymentReq.setBeneficiaryBIC(beneficiaryBIC);
        paymentReq.setBeneficiaryContact(beneficiaryContact);
        paymentReq.setBeneficiaryName(beneficiaryName);
        paymentReq.setBoundary(boundary);
        paymentReq.setCallbackUrl(callbackUrl);
        paymentReq.setCorrespondentBic(sysenv.BOT_SWIFT_CODE);
        paymentReq.setChargeCategory(chargeCategory);
        paymentReq.setCurrency(currency);
        paymentReq.setCustomerBranch(customerBranch);
        paymentReq.setDescription(description);
        paymentReq.setInitiatorId(initiatorId);
        paymentReq.setIntermediaryBank(intermediaryBank);
        paymentReq.setReference(reference);
        paymentReq.setSenderAccount(senderAccount);
        paymentReq.setSenderAddress(senderAddress);
        paymentReq.setSenderBic(sysenv.SENDER_BIC);
        paymentReq.setSenderName(senderName);
        paymentReq.setSenderPhone(senderPhone);
        paymentReq.setSpecialRateToken(specialRateToken);
        paymentReq.setType(type);
        paymentReq.setSpRate(spRate);

        return paymentReq;
    }

    public String reprocessFailedBatches(String batchReference) {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.PHilae/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <api:reprocessBatch>\n" +
                "         <!--Optional:-->\n" +
                "         <request>\n" +
                "            <reference>RETRY_" + DateUtil.now("yyyyMMddHHmmss") + "</reference>\n" +
                "            <!--Optional:-->\n" +
                "            <batch>" + batchReference + "</batch>\n" +
                "         </request>\n" +
                "      </api:reprocessBatch>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
//response
//<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
//   <SOAP-ENV:Header/>
//   <S:Body>
//      <ns2:reprocessBatchResponse xmlns:ns2="http://api.PHilae/">
//         <return>
//            <result>0</result>
//            <reference>retry_1_MAS273620230824162512</reference>
//            <message>Success</message>
//            <availableBalance xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
//            <ledgerBalance xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
//            <txnId xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
//            <queryInfo>
//               <endRecId>1916342</endRecId>
//               <itemCount>5000</itemCount>
//               <startRecId>1911343</startRecId>
//            </queryInfo>
//            <txns/>
//         </return>
//      </ns2:reprocessBatchResponse>
//   </S:Body>
//</S:Envelope>
        String soapResponse = HttpClientService.sendXMLReqBasicAuth(request, sysenv.CHANNEL_MANAGER_API_URL, "xapi", "x@pi#81*");
        if (soapResponse.contains("return")) {
            //parser soap xml to get clean xml
            XMLStreamReader xmlr = XMLParserService.createXMLStreamReaderFromSOAPMessage(soapResponse, "body", "return");
            //byte data to string xml
            String sxml = XMLParserService.xmlsrToString(xmlr);
            LOGGER.info("Final response xml... {}", sxml);
            //convert xml to java object
            String result = XMLParserService.getDomTagText("result", sxml);
            String message = XMLParserService.getDomTagText("message", sxml);
            if (Objects.equals(result, "0") && Objects.equals(message, "Success")) {
                return "{\"success\":true}";
            }
        }
        return "{\"success\":false}";
    }
}
