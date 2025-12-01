package com.repository.salary;

import com.DTO.batch.BatchPayemntReq;
import com.DTO.batch.BatchTxnEntries;
import com.DTO.batch.BatchTxnsEntry;
import com.DTO.salary.LoanRepaymentResp;
import com.DTO.salary.PAYROLL;
import com.DTO.salary.TRANSACTION;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.service.CorebankingService;
import com.service.HttpClientService;
import com.service.XMLParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class SalaryProcessingRepository {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SalaryProcessingRepository.class);

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    CorebankingService coreBankingService;

    public List<Map<String,Object>> getSalaryProcessingModulePermissions(String moduleURL, String roleId) {
        List<Map<String,Object>> finalRes = null;
        try {
            String sql = "select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?";
//            LOGGER.info(sql.replace("?","'{}'"),moduleURL, roleId);
            finalRes = this.jdbcTemplate.queryForList(sql, moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING Salary Module Permissions : {}", e.getMessage());
        }
        return  finalRes;
    }

    public String insertPayrollSalary(PAYROLL PAYROLL) {
        String batchRef = "PB"+ DateUtil.now("yyyyMMddHHmmSS");
//        String year = DateUtil.now("yyyy");
//        String month = DateUtil.now("MMMM");
        BigDecimal totalAmt = BigDecimal.ZERO;
        String response = null;

        for(TRANSACTION transaction : PAYROLL.getTRANSACTIONS().getTRANSACTION()){
                    totalAmt = totalAmt.add(new BigDecimal(transaction.getCREDIT_TO_DQA()));
                }
                LOGGER.info("checking batch amount ...{} and sum of individual txn ...{} in batch ref... {}", PAYROLL.getPAYROLL_SUMMARY().getPAYROLL_AMOUNT(), totalAmt, batchRef);
                if(totalAmt.compareTo(new BigDecimal(PAYROLL.getPAYROLL_SUMMARY().getPAYROLL_AMOUNT())) == 0){

                    try {
                        String sql1 = "INSERT INTO payroll_summary (batch_reference, month, year, no_of_employees, payroll_amount,initiated_by) VALUES(?,?,?,?,?,?)";
                        LOGGER.info(sql1.replace("?","'{}'"),batchRef,PAYROLL.getPAYROLL_SUMMARY().getMONTH(),PAYROLL.getPAYROLL_SUMMARY().getYEAR(), PAYROLL.getPAYROLL_SUMMARY().getNO_OF_EMPLOYEES(), PAYROLL.getPAYROLL_SUMMARY().getPAYROLL_AMOUNT(),PAYROLL.getPAYROLL_SUMMARY().getINITIATOR());
                        jdbcTemplate.update(sql1,batchRef,PAYROLL.getPAYROLL_SUMMARY().getMONTH(),PAYROLL.getPAYROLL_SUMMARY().getYEAR(), PAYROLL.getPAYROLL_SUMMARY().getNO_OF_EMPLOYEES(), PAYROLL.getPAYROLL_SUMMARY().getPAYROLL_AMOUNT(),PAYROLL.getPAYROLL_SUMMARY().getINITIATOR());
                        String sql2 = "INSERT INTO payroll_transactions (pf_no, staff_name, account_no, batch_reference, reference, credit_to_dqa, branch_no, payer_account, month, year)" +
                                "VALUES(?, ?, ?, ?, ?, ?,?,?,?,?)";
                        LOGGER.info(sql2.replace("?","'{}'"), PAYROLL.getTRANSACTIONS().getTRANSACTION().get(0).getPF_NO());
                        int res[] =  this.jdbcTemplate.batchUpdate(sql2,
                                new BatchPreparedStatementSetter() {
                                    @Override
                                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                                        ps.setString(1, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getPF_NO());
                                        ps.setString(2, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getNAME());
                                        ps.setString(3, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getACCOUNT());
                                        ps.setString(4, batchRef);
                                        ps.setString(5, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getREFERENCE());
                                        ps.setString(6, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getCREDIT_TO_DQA());
                                        ps.setString(7, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getBRANCHNO());
                                        ps.setString(8, PAYROLL.getTRANSACTIONS().getTRANSACTION().get(i).getPAYER_ACCT());
                                        ps.setString(9, PAYROLL.getPAYROLL_SUMMARY().getMONTH().toUpperCase());
                                        ps.setString(10, PAYROLL.getPAYROLL_SUMMARY().getYEAR());
                                    }

                                    @Override
                                    public int getBatchSize() {
                                        return PAYROLL.getTRANSACTIONS().getTRANSACTION().size();
                                    }


                                });
                        LOGGER.info("Salary Transaction Logged successfully {}", res);
                        if(res.length>0){
                            response= "<RESPONSE>" +
                                    "<BATCH_REFERENCE>"+batchRef+"</BATCH_REFERENCE>" +
                                    "<ITEM_COUNT>"+ PAYROLL.getTRANSACTIONS().getTRANSACTION().size()+"</ITEM_COUNT>" +
                                    "<STATUS>SUCCESS</STATUS>" +
                                    "<RECEIVED_DT>"+DateUtil.now("yyyy-MM-dd hh:MM:ss")+"</RECEIVED_DT>" +
                                    "</RESPONSE>";
                        }else{
                            response= "<RESPONSE>" +
                                    "<BATCH_REFERENCE>"+batchRef+"</BATCH_REFERENCE>" +
                                    "<ITEM_COUNT>"+ PAYROLL.getTRANSACTIONS().getTRANSACTION().size()+"</ITEM_COUNT>" +
                                    "<STATUS>FAILED TO RECORD SALARY</STATUS>" +
                                    "<RECEIVED_DT>"+DateUtil.now("yyyy-MM-dd hh:MM:ss")+"</RECEIVED_DT>" +
                                    "</RESPONSE>";
                        }
                    } catch (DataAccessException e) {
                        LOGGER.error("Salary Rollbacked... {}", e.getMessage());
                        response = "<RESPONSE>" +
                                "<BATCH_REFERENCE>"+batchRef+"</BATCH_REFERENCE>" +
                                "<ITEM_COUNT>"+ PAYROLL.getTRANSACTIONS().getTRANSACTION().size()+"</ITEM_COUNT>" +
                                "<STATUS>GENERAL FAILURE</STATUS>" +
                                "<RECEIVED_DT>"+DateUtil.now("yyyy-MM-dd hh:MM:ss")+"</RECEIVED_DT>" +
                                "</RESPONSE>";
                    }

                }
                return response;
    }

    public List<Map<String, Object>>  fireGetCurrentMonthStaffSalaryAjax(String roleId, String branchNo, String month, String year) {
        List<Map<String, Object>> results = null;
        try {
            String   mainSql = "select t.*," +
                        "(SELECT IFNULL(sum(pt.credit_to_dqa),0) FROM payroll_transactions pt WHERE pt.status='S' and pt.batch_reference=t.batch_reference) AS inQueueTotalAmt," +
                        "(SELECT count(pt.id) FROM payroll_transactions pt WHERE pt.status='S' and pt.batch_reference=t.batch_reference) AS inQueueTotal," +
                        "(SELECT IFNULL(sum(pt1.credit_to_dqa),0) FROM payroll_transactions pt1 WHERE pt1.status='C' and pt1.batch_reference=t.batch_reference) AS successTotalAmt," +
                        "(SELECT count(pt1.id) FROM payroll_transactions pt1 WHERE pt1.status='C' and pt1.batch_reference=t.batch_reference) AS successTotal," +
                        "(SELECT IFNULL(sum(pt2.credit_to_dqa),0) FROM payroll_transactions pt2 WHERE pt2.status='F' and pt2.batch_reference=t.batch_reference) AS failedTotalAmt," +
                        "(SELECT count(pt2.id) FROM payroll_transactions pt2 WHERE pt2.status='F' and pt2.batch_reference=t.batch_reference) AS failedTotal " +
                        " from payroll_summary t where t.month=? and t.year=?";
            LOGGER.info(mainSql.replace("?","'{}'"),month,year);
                results = jdbcTemplate.queryForList(mainSql,month,year);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return results;

    }


    public Map<String,Object> computeSalarySummaryBasedOnBatchReference(String batchReference) {
        String query ="SELECT * FROM payroll_summary WHERE batch_reference=?";
        Map<String,Object> result = jdbcTemplate.queryForMap(query,batchReference);
        return result;
    }

    public List<Map<String,Object>> computeSalarySummaryBasedOnBatchRefAndStatus(String status, String batchReference) {
        List<Map<String, Object>> result = null;
        try {
            String query = "SELECT * FROM payroll_transactions WHERE status=? and batch_reference=?";
            result = jdbcTemplate.queryForList(query, status, batchReference);
        }catch (Exception e){
            LOGGER.info(null,e);
        }
        return result;
    }


    public String firePreviewPayrollBatchTransactionsAjax(String batchReference,String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {

        List<Map<String, Object>> results;
        String mainSql;
        int totalRecords = 0;
        int totalRecordWithFilter = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from payroll_transactions where batch_reference=? ";
            totalRecords = jdbcTemplate.queryForObject(mainSql, Integer.class,batchReference);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE concat(pf_no,' ',staff_name,' ',account_no,' ',batch_reference,' ',reference,' ',status,' ',create_date,' ',credit_to_dqa,' ',branch_no,' ',payer_account,' ',month,' ',year,' ',institution) LIKE ? and batch_reference=?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("select count(*) from payroll_transactions" + searchQuery, Integer.class,searchValue,batchReference);
                mainSql = "select * from payroll_transactions " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, searchValue, batchReference);

            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "select * from payroll_transactions where batch_reference=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, batchReference);
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("check this: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";

    }

    public void processSalaryToBrinjal(String batchReference,String username) {
        List<Map<String, Object>> result = null;
        int noOfTxns = 0;
        BigDecimal totalAmt = BigDecimal.ZERO;
        try {
            String query ="SELECT * FROM payroll_transactions WHERE status='I' AND batch_reference=?";
            List<Map<String,Object>> payrollTransactions = jdbcTemplate.queryForList(query,batchReference);
            List<BatchTxnEntries> batchTransactions = new ArrayList<>();
            if (!payrollTransactions.isEmpty()) {
                Map<String,Object> batchSummary = computeSalarySummaryBasedOnBatchReference(batchReference);
                for (Map<String, Object> transaction : payrollTransactions) {

                    BatchTxnEntries batchTxnEntry = new BatchTxnEntries();
                    batchTxnEntry.setAmount(new BigDecimal(String.valueOf(batchSummary.get("payroll_amount"))));
                    batchTxnEntry.setBatch(batchReference);
                    batchTxnEntry.setBuId("-5");
                    batchTxnEntry.setCode("?");
                    batchTxnEntry.setCrAcct(String.valueOf(transaction.get("account_no")));
                    batchTxnEntry.setCurrency("TZS"); //HARDCODED
                    batchTxnEntry.setDrAcct(String.valueOf(transaction.get("payer_account")));
                    batchTxnEntry.setModule("PAYROLL-PROCESSING");
                    batchTxnEntry.setNarration(batchSummary.get("descriptions")+ " B/O " + transaction.get("staff_name"));
                    batchTxnEntry.setReverse("N");
                    batchTxnEntry.setTxnRef(String.valueOf(transaction.get("reference")));
                    batchTxnEntry.setScheme("?");
                    //update the entry after being pulled
                    int updateBatch = updatePayrollBatch("S",username,batchReference);
                    int updateResult = updatePayrollTransactionStatus("S","This is submitted for payroll processing", String.valueOf(transaction.get("reference")),batchReference);
                    LOGGER.info("update result for txn reference... {} is ... {} update batch... {}", String.valueOf(transaction.get("reference")),updateResult,updateBatch);
                    if(1==updateResult){
                        batchTransactions.add(batchTxnEntry);
                        noOfTxns++;
                    }
                }
                if (noOfTxns > 0 ) {
                    BatchPayemntReq bpayments = new BatchPayemntReq();
                    bpayments.setCallbackUrl(systemVariables.PAYROLL_BATCH_CALLBACK_URL);
                    bpayments.setItemCount(String.valueOf(noOfTxns));
                    bpayments.setReference(batchReference);
                    bpayments.setSerialize("false");
                    bpayments.setTotalAmount(String.valueOf(batchSummary.get("payroll_amount")));
                    BatchTxnsEntry bEntries = new BatchTxnsEntry();
                    bEntries.setTxn(batchTransactions);
                    bpayments.setTxns(bEntries);
                    //insert the batch into eft-batches table
                    String requestToCore = XMLParserService.jaxbGenericObjToXML(bpayments, true, true);
//                    String response = coreBankingService.processRequestToCore(requestToCore, "api:processBatch");
                    String response = HttpClientService.sendLoanRepaymentXMLRequest(requestToCore,systemVariables.BRINJAL_LOAN_REPAYMENT_URL);

                    LoanRepaymentResp  resp= XMLParserService.jaxbXMLToObject(response, LoanRepaymentResp.class);
                    if(resp.getResponseCode()==0){
                        int updateBatch = updatePayrollBatch("C",username,batchReference);
                        int updateResult = updatePayrollTransactionByBatchRef("C","Transaction processed successfully",batchReference);
                        LOGGER.info("update result payroll summary... {} with batch reference {} update payroll transactions... {}",updateBatch, batchReference,updateResult);

                    }
                    LOGGER.info("PAYROLL REQUEST TO BRINJAL with batch reference ...{} and response  ...{}", batchReference, response);
                }
            }
        } catch (Exception e) {
            LOGGER.info("AN ERROR OCCURRED DURING PROCESSING BATCH FOR PAYROLL:{}", e.getMessage());
            LOGGER.info(null, e);
        }
    }

    private int updatePayrollBatch(String status, String username,String batchReference) {
        String query = "UPDATE payroll_summary SET status = ?,approved_by=?, approved_date=? WHERE batch_reference=?";
        return jdbcTemplate.update(query, status,username,DateUtil.now("yyyy-MM-dd hh:MM:ss"),batchReference);
    }

    public int updatePayrollTransactionByBatchRef(String status,String statusDescriptions, String batchReference){
        String query = "UPDATE payroll_transactions SET status = ?, status_descriptions=? WHERE batch_reference=?";
        return jdbcTemplate.update(query,status,statusDescriptions,batchReference);
    }

    public int updatePayrollTransactionStatus(String status,String statusDescriptions,String reference, String batchReference){
        String query = "UPDATE payroll_transactions SET status = ?, status_descriptions=? WHERE reference=?";
        return jdbcTemplate.update(query,status,statusDescriptions,reference);
    }

    public void processPayrollCallback(String payload) {
        LOGGER.info("payroll callback in repo...{}",payload);
    }

    public String firePreviewPayrollBatchTransactionsBasedOnStatusAjax(String batchReference,String status, String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecords = 0;
        int totalRecordWithFilter = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from payroll_transactions where status=? and batch_reference=? ";
            totalRecords = jdbcTemplate.queryForObject(mainSql, Integer.class,status,batchReference);
            String searchQuery;
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE status=? and concat(pf_no,' ',staff_name,' ',account_no,' ',batch_reference,' ',reference,' ',status,' ',create_date,' ',credit_to_dqa,' ',branch_no,' ',payer_account,' ',month,' ',year,' ',institution) LIKE ? and batch_reference=?";
                totalRecordWithFilter = jdbcTemplate.queryForObject("select count(*) from payroll_transactions" + searchQuery, Integer.class,status, searchValue,batchReference);
                mainSql = "select * from payroll_transactions " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, searchValue, batchReference);

            } else {
                totalRecordWithFilter = totalRecords;
                mainSql = "select * from payroll_transactions where status=? and batch_reference=? ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql,status, batchReference);
            }
            jsonString = jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("check this: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordWithFilter + "\",\"aaData\":" + jsonString + "}";

    }
}
