package com.repository.loan;

import com.DTO.GeneralJsonResponse;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.helper.DateUtil;
import com.service.HttpClientService;

import com.service.XMLParserService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Repository
public class LoanCreditRepository {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LoanCreditRepository.class);

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    SYSENV systemVariables;

    @Autowired
    @Qualifier("gwBrinjalDbConnection")
    JdbcTemplate jdbcBrinjalTemplate;

    @Autowired
    ObjectMapper jacksonMapper;

    public List<Map<String,Object>> getLoanModulePermissions(String moduleURL, String roleId) {
        List<Map<String,Object>> finalRes = null;
        try {
            finalRes = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING Loan/credit Permissions : {}", e.getMessage());
        }
        return  finalRes;
    }

    public String getScoringEngineAjax(String loanType) {
        String res = null;

        String loan_SCORING_ENGINE_url = systemVariables.loan_SCORING_ENGINE_URL;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&loanType="
                        + loanType
                       ;
        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal("",loan_SCORING_ENGINE_url,headers,"POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
//                LOGGER.info("LOAN SCORING ENGINE RESPONSE:....{}", res);
        return res;
    }

    public String getScoredCustomerLimit(String loanType, String fromDate, String toDate) {
        String res = null;

        String loan_SCORED_CUSTOMER_LIMITS_url = systemVariables.loan_SCORED_CUSTOMER_LIMITS_URL;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        +"&loanType="
                        +loanType
                        + "&fromDate="
                        + fromDate
                        + "&toDate="
                        + toDate;

        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal("",loan_SCORED_CUSTOMER_LIMITS_url,headers,"POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("final response: for loan_SCORED_CUSTOMER_LIMITS_URL .....{}", res);
        return ""+res+"";
    }

    public String customerLoansAjax(String loanType,String fromDate, String toDate) {
        String res = null;

        String loan_OUTSTANDING_LOANS_URL = systemVariables.loans_OUTSTANDINGLOANS_URL;
        String requestBody = "{\"loanType\":\""+loanType+"\",\"fromDate\": \""+fromDate+"\", \"toDate\": \""+toDate+"\"}";

        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal(requestBody,loan_OUTSTANDING_LOANS_URL,"","POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("final response: .........{}", res);
        return res;
    }

    public String loansReportAjax(String loanType,String reportType, String fromDate, String toDate) {
        String res = null;

        String loans_PORTFOLIO_POSITION_URL = systemVariables.loans_PORTFOLIO_POSITION_URL;
        String requestBody = "{\"loanType\":\""+loanType+"\",\"fromDate\": \""+fromDate+"\", \"toDate\": \""+toDate+"\"}";

        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal(requestBody,loans_PORTFOLIO_POSITION_URL,"","POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("final response: .........{}", res);

        return res;
    }

    public String outstandingLoanAjax(String loanType,String reportType, String fromDate, String toDate) {
        String res = null;

        String outstandingLoan_URL = systemVariables.outstandingLOAN_URL;
        String requestBody = "{\"loanType\":\""+loanType+"\",\"fromDate\": \""+fromDate+"\", \"toDate\": \""+toDate+"\"}";

        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal(requestBody,outstandingLoan_URL,"","POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("final response: .........{}", res);
        return res;
    }

    public String fireRepaymentSummaryPerAccountAjax(String loanType, String reportType, String fromDate, String toDate) {
        String res = null;

        String loan_REPAYMENT_SUMMARY_PER_ACCOUNT_URL = systemVariables.loan_REPAYMENT_SUMMARY_PER_ACCOUNT_URL;
        String requestBody = "{\"loanType\":\""+loanType+"\",\"fromDate\": \""+fromDate+"\", \"toDate\": \""+toDate+"\"}";

        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal(requestBody,loan_REPAYMENT_SUMMARY_PER_ACCOUNT_URL,"","POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public String fireRepaymentSummaryPerLoanAjax(String loanType,String reportType, String fromDate, String toDate) {
        String res = null;

        String loan_REPAYMENT_SUMMARY_PER_LOAN_URL = systemVariables.loan_REPAYMENT_SUMMARY_PER_LOAN_URL;
        String requestBody = "{\"loanType\":\""+loanType+"\",\"fromDate\": \""+fromDate+"\", \"toDate\": \""+toDate+"\"}";

        Map<String,String> response = null;
        try {
            response = HttpClientService.sendTxnToBrinjal(requestBody,loan_REPAYMENT_SUMMARY_PER_LOAN_URL,"","POST" );
            res=response.get("responseBody");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public String previewCustomerLoansAjax(String accountNumber) {
        String loan_CUSTOMER_LOANS_URL = systemVariables.loan_CUSTOMER_LOANS_URL;
        String resp = null;
        String headers =
                "&Authorization="
                        + 000000
                        + ""
                        + "&accountNo="
                        + accountNumber ;
         resp=HttpClientService.sendTipsXMLRequest(headers, loan_CUSTOMER_LOANS_URL);

         return resp;
    }

    public List<Map<String, Object>> getLoanTypes() throws Exception {
        String loan_TYPES_URL = systemVariables.loan_TYPES;
        String resp = null;
        String headers =
                "&Authorization="
                        + 000000 ;
        resp=HttpClientService.sendTxnToBrinjal("",loan_TYPES_URL,headers,"POST").get("responseBody");

        Gson gson = new Gson();
        Type resultType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> result = null;
        try{
            result = gson.fromJson(resp, resultType);
        }catch (Exception e){
            LOGGER.info(null,e);
            result = null;
        }
        return result;
    }

    public GeneralJsonResponse fireGetDailLoanBalanceFromBrinjal(String loanType, String reportType, String fromDate, String toDate) {
        GeneralJsonResponse response = new GeneralJsonResponse();
        try {
            String sql = "SELECT * FROM ln_customer_loans_daily_balance where loanType=? and date(balanceDate)=?";
            LOGGER.info(sql.replace("?","'{}'"),loanType,toDate);
            List<Map<String, Object>> data = jdbcBrinjalTemplate.queryForList(sql,loanType,toDate);
            response.setStatus("200");
            response.setResult(data);
        }catch (Exception e){
            LOGGER.info(null,e);
        }
        return response;
    }

    public List<Map<String,Object>> getCustomeActiveLoans(String loanType, String account) {
        List<Map<String, Object>> data = null;
        try {
            String sql = "SELECT * FROM ln_customer_loans where status ='C' and repaymentStatus in('P','PA') and loanType=? and account=?";
            LOGGER.info(sql.replace("?","'{}'"),loanType,account);
            data = jdbcBrinjalTemplate.queryForList(sql,loanType,account);
        }catch (Exception e){
            LOGGER.info(null,e);
        }
        return data;
    }

    public String initiateLoanRepayment(String loanType,BigDecimal amount, String narration,String msisdn,String cbsReference,String repaymentDate,String reference,String loanId,String debitAccount, String maker) {
        String result = "FAIL";
        try{
            String sql = "INSERT INTO manual_loan_repayments(loanType, debitAccount, amount, cbsReference,repaymentDate, reference, nisogezeLoanId, msisdn, descriptions, createdBy) VALUES(?,?,?,?,?,?,?,?,?,?)";
            LOGGER.info(sql.replace("?","'{}'"),loanType,debitAccount,amount,cbsReference,repaymentDate, reference,loanId,msisdn,narration,maker);
           int insert_result = jdbcTemplate.update(sql,loanType,debitAccount,amount,cbsReference,repaymentDate, reference,loanId,msisdn,narration,maker);
           if(insert_result == 1){
               result = "SUCCESS";
           }
        }catch(Exception e){
            LOGGER.info(null,e);
        }
        return result;
    }

    public String loanRepaymentTxnsForApproval(String roleId, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from manual_loan_repayments  where status='LOGGED'";
            totalRecords = jdbcTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(loanType,' ',debitAccount,' ',amount,' ',cbsReference,' ',reference,' ',msisdn) LIKE ? and status='LOGGED'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM manual_loan_repayments " + searchQuery, new Object[]{searchValue}, Integer.class);

                mainSql = "select * from  manual_loan_repayments  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, searchValue);

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "select * from manual_loan_repayments t where status='LOGGED' ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql);
            }

            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public Map<String,Object> getTransactionByReferenceNo(String reference) {
        String query = "select * from manual_loan_repayments  where reference=?";
        Map<String,Object> data = null;
        try{
            data = jdbcTemplate.queryForMap(query, reference);
        }catch(Exception e){
            LOGGER.info(null,e);
        }
        return data;
    }

    public void updateTransactionStatus(String reference,String approver) {
        String query = "update manual_loan_repayments set status='COMPLETED', approvedDate=?, approvedBy=? where reference=?";
        try{
            jdbcTemplate.update(query,DateUtil.now(), approver, reference);
        }catch(Exception e){
            LOGGER.info(null,e);
        }
    }


    public void logLoanRepaymentSummary(String batchNo, String status) {
        String sql = "INSERT INTO manual_loan_repay_summary(batchReference, status) VALUES(?, ?)";
        LOGGER.info(sql.replace("?","'{}'"),batchNo,status);
        jdbcTemplate.update(sql,batchNo,status);
    }

    public void logNisogezeLoanRepayment(String loanType, String debitAcct, BigDecimal amount, String cbsReference, String reference, String loanId, String batchRef, String phoneNo, String descriptions, String now, String repaymentDate, String username,String repaymentType, String strategy) {
        String sql = "INSERT IGNORE INTO manual_loan_repayments (loanType,debitAccount,amount,cbsReference,reference,nisogezeLoanId,batchReference,msisdn,descriptions,status,createDate,repaymentDate,createdBy,repaymentType,repaymentStrategy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//        LOGGER.info(sql.replace("?","'{}'"),loanType,debitAcct,amount,cbsReference,reference,loanId,batchRef,phoneNo,descriptions,"LOGGED",DateUtil.now(),repaymentDate,username);
        jdbcTemplate.update(sql,loanType,debitAcct,amount,cbsReference,reference,loanId,batchRef,phoneNo,descriptions,"LOGGED",DateUtil.now(),repaymentDate,username,repaymentType,strategy);
    }


    public void logLoanRepayment(java.lang.String loanType, java.lang.String debitAcct, BigDecimal amount, java.lang.String descriptions, java.lang.String reference, java.lang.String batchRef, String username) {
        String sql = "INSERT IGNORE INTO manual_loan_repayments (loanType,debitAccount,amount,reference,batchReference,descriptions,status,createDate,createdBy) VALUES(?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql,loanType,debitAcct,amount,reference,batchRef,descriptions,"LOGGED",DateUtil.now(),username);

    }
}
