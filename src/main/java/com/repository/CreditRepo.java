/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.psssf.LoanVerificationReq;
import com.DTO.psssf.PensionStatementResponse;
import com.DTO.psssf.PensionVerificationForm;
import com.DTO.psssf.PensionerLoanVerificationResponse;
import com.config.SYSENV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.queue.QueueProducer;
import com.service.HttpClientService;
import com.service.JasperService;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Base64;


/**
 * @author Melleji.Mollel
 */
@Repository
public class CreditRepo {

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
    JasperService jasperService;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CreditRepo.class);


    /*
    GET CREDIT  dashboard MODULES
     */
    public List<Map<String, Object>> getCreditModulePermissions(String moduleURL, String roleId) {
        List<Map<String, Object>> result = null;
        try {
            result = this.jdbcTemplate.queryForList("select a.name,a.url from payment_permissions a INNER JOIN payment_module_permission b on b.permission_id=a.id INNER JOIN payment_modules c on c.id=b.module_id INNER JOIN payment_permission_role d on d.payment_permission_id=a.id where c.module_dashboard_url=? and d.role_id=?", moduleURL, roleId);
        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING CREDIT MODULES:{}", e.getMessage());
        }
        return result;
    }

    /*
     *GET SPECIAL RATES REQUESTS ON WORK-FLOW
     */
    public String getPensionersDetailsAjax(String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        try {
            mainSql = "SELECT count(*) FROM pensioners";
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "%" + searchValue + "%";
                searchQuery = " WHERE  concat(saving_acct,' ',saving_old_acct,' ',loan_acct,' ',loan_repayment_acct,' ',acct_name,' ',disbursed_amt,' ',last_repayment_amt,' ',loan_start_dt,' ',loan_maturity_dt,' ',branch_no,' ',branch_name,' ',loan_reference,' ',loan_code,' ',loan_prod_description,' ',loan_currency,' ',cust_id,' ',rim_no,' ',loan_balance) LIKE ? ";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(saving_acct) FROM pensioners  " + searchQuery, new Object[]{searchValue}, Integer.class);
                mainSql = "SELECT * FROM pensioners  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                totalRecordwithFilter = totalRecords;
                mainSql = "SELECT * FROM pensioners  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
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

    public List<Map<String, Object>> getWastaafuLoanAccountsDetails(String lastPtID) {
        String jsonString = null;
        List<Map<String, Object>> rs = null;

        String json = null;
        try {
            String mainSql = "SELECT ACCT_ID, ACCT_NO AS LOAN_ACCT,\n"
                    + "LN_FEE_ACCT_NO AS SAVING_ACCT,\n"
                    + "(SELECT OLD_ACCT_NO FROM V_ACCOUNTS L WHERE L.ACCT_NO =LN_FEE_ACCT_NO AND ROWNUM=1) AS SAVING_OLD_ACCT,\n"
                    + "DISBRSMNT_SETLMNT_ACCT_NO AS REPAYMENT_ACCT,\n"
                    + "(SELECT LAST_PAYMENT_AMT FROM V_LOAN_ACCOUNT_SUMMARY LS WHERE LS.ACCT_NO =vla.ACCT_NO) AS LAST_PAYMENT_AMT\n"
                    + ",(SELECT NVL(TOTAL_AMT,0) FROM V_LN_EVENT_SUMMARY VLE where VLE.ACCT_NO=vla.ACCT_NO AND REC_ST='N'  AND EVENT_TYPE LIKE '%REPAYMENT%'  AND ROWNUM=1) AS REAL_REPAYMENT_AMT\n"
                    + ",(SELECT to_char(DUE_DATE,'yyyy-mm-dd HH:mm:ss') FROM V_LN_EVENT_SUMMARY VLE where VLE.ACCT_NO=vla.ACCT_NO AND REC_ST='N'  AND EVENT_TYPE LIKE '%REPAYMENT%'  AND ROWNUM=1) AS NEXT_REPAYEMENT_DATE\n"
                    + ",ACCT_NM,NVL(DISBURSEMENT_LIMIT,0) DISBURSEMENT_LIMIT,to_char(MATURITY_DT,'yyyy-mm-dd HH:mm:ss') MATURITY_DT,to_char(START_DT,'yyyy-mm-dd HH:mm:ss') START_DT,BU_NO,BU_NM,REF_NO,\n"
                    + "PROD_CD,PROD_DESC,CRNCY_CD_ISO AS CURRENCY,CUST_ID,CUST_NO,NVL(CLEARED_BAL,0) CLEARED_BAL FROM V_LOAN_ACCOUNTS vla\n"
                    + "WHERE vla.PROD_CD IN ('121','104','136') AND VLA.REC_ST <>'L' AND  ACCT_ID>=? order by ACCT_ID ASC FETCH FIRST 10000 ROWS ONLY"; //"select * from tp_transaction  where msisdn like '%" + input + "%' OR txReceipt like '%" + input + "%' OR txid like '%$input%' OR txsourceAccount like '%" + input + "%' OR txdestinationAccount like '%" + input + "%' order by txdate desc limit 1000";
            rs = this.jdbcRUBIKONTemplate.queryForList(mainSql, lastPtID);
//            LOGGER.info("RUBIKON OBJECT: {} ", rs);
            //AFTER GETTING THE RESULTS INSERT INTO PENSIONERS TABLE
            try {
                jsonString = this.jacksonMapper.writeValueAsString(rs);
                //LOGGER.info("RequestBody");
            } catch (JsonProcessingException ex) {
                LOGGER.info("EXCEPTION ON GETTING WASTAAFU LOAN ACCOUNTS: {} ", ex);
            }
            json = jsonString;
        } catch (Exception e) {
            rs = null;
            LOGGER.info("EXCEPTION ON GETTING WASTAAFU LOAN ACCOUNTS:{}", e);
        }
        return rs;
    }

    public void insertWastaafuLoans() {
        String lastPtid = getLastPTID("WASTAAFU");
        List<Map<String, Object>> rs = getWastaafuLoanAccountsDetails(lastPtid);
        if (rs != null) {
            for (int i = 0; i < rs.size(); i++) {
                String nextRepaymentDate = rs.get(i).get("NEXT_REPAYEMENT_DATE") + "";
                if (rs.get(i).get("NEXT_REPAYEMENT_DATE") != null) {
                    String sqlQuery = "INSERT INTO pensioners(saving_acct, saving_old_acct, loan_acct, loan_repayment_acct, acct_name, disbursed_amt, last_repayment_amt, loan_start_dt, loan_maturity_dt, branch_no, branch_name, loan_reference, loan_code, loan_prod_description, loan_currency, cust_id, rim_no, loan_balance,next_repayment_dt,next_repayment_amt,last_ptid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE next_repayment_amt=?";
                    int res = jdbcTemplate.update(sqlQuery,
                            rs.get(i).get("SAVING_ACCT") + "", rs.get(i).get("SAVING_OLD_ACCT") + "".replace("-", ""), rs.get(i).get("LOAN_ACCT") + "", rs.get(i).get("REPAYMENT_ACCT") + "", rs.get(i).get("ACCT_NM") + "", rs.get(i).get("DISBURSEMENT_LIMIT") + "", rs.get(i).get("LAST_PAYMENT_AMT") + "", rs.get(i).get("START_DT") + "", rs.get(i).get("MATURITY_DT") + "", rs.get(i).get("BU_NO") + "", rs.get(i).get("BU_NM") + "", rs.get(i).get("REF_NO") + "", rs.get(i).get("PROD_CD") + "", rs.get(i).get("PROD_DESC") + "", rs.get(i).get("CURRENCY") + "", rs.get(i).get("CUST_ID") + "", rs.get(i).get("CUST_NO") + "", rs.get(i).get("CLEARED_BAL") + "", nextRepaymentDate, rs.get(i).get("REAL_REPAYMENT_AMT") + "", rs.get(i).get("ACCT_ID") + "", rs.get(i).get("REAL_REPAYMENT_AMT") + "");
                    jdbcTemplate.update("UPDATE last_ptid set lastPtid=?,modified_dt=? where name='WASTAAFU'", rs.get(i).get("ACCT_ID") + "", DateUtil.now());
                }

            }
        }
    }

    public String getLastPTID(String name) {
        List<Map<String, Object>> result = null;
        String lastPtid = "0";
        try {
            result = this.jdbcTemplate.queryForList("SELECT * FROM last_ptid where name=?", name);

        } catch (Exception e) {
            LOGGER.info("ERROR ON GETTING LAST-PTID: {}", e.getMessage());
        }
        if (result != null) {
            lastPtid = result.get(0).get("lastPtid") + "";
        }
        return lastPtid;
    }

    public String getPensionersVerificationAjax(Map<String, String> customeQuery) {
        String verificationReq = "{\n" +
                "\"ssn\":\"" + customeQuery.get("chequeNo") + "\"\n" +
                ",\"pensioner_id\":\"" + customeQuery.get("pensionID") + "\"\n" +
                "}";
        String finalRes = HttpClientService.sendTxnToAPI(verificationReq, systemVariable.PENSIONERS_VERIFICATION_URL);

        return finalRes;
    }

    public String fireviewPensionerStatementModalAjax(Map<String, String> customeQuery) {
        String verificationReq = "{\n" +
                "\"pensioner_id\":\"" + customeQuery.get("pensionID") + "\"\n" +
                "}";
        String finalRes = HttpClientService.sendTxnToAPI(verificationReq, systemVariable.PENSIONERS_STATEMENT_URL);

        return finalRes;
    }

    public String viewPensionerStatementJasper(PensionStatementResponse resp, Map<String, String> data, String printedBy,HttpServletResponse response) {
        String reportFileTemplate = "/iReports/psssf/pensioner_statement.jasper";
         String exporterFileType = "html";
         String destName = "hellowordl";
        try{
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("PENSIONER_ID", data.get("pensionID"));
        parameters.put("CHECK_NUMBER", data.get("ssn"));
        parameters.put("FULL_NAME", data.get("fullName"));
        parameters.put("DOB", data.get("dob"));
        parameters.put("RETIREMENT_DATE", data.get("retirementDate"));
        parameters.put("MONTHLY_PAYMENT", data.get("mpAmount"));
        parameters.put("LOAN_STATUS", "Active");
        parameters.put("LAST_PAYMENT", data.get("lastPayment"));
        parameters.put("PHONE", data.get("phone"));
        parameters.put("BANK_NAME", data.get("bankName"));
        parameters.put("BANK_ACCOUNT", data.get("bankAccount"));
        parameters.put("PENSIONER_TYPE", data.get("pensionerType"));
        parameters.put("PENSIONER_STATUS", data.get("pensionerStatus"));
        parameters.put("PENSION_MODE", data.get("pensionMode"));
        parameters.put("MONTHLY_PAYMENT", "0.00");
        parameters.put("PRINTED_BY", printedBy);
        parameters.put("PRINTED_DATE", DateUtil.now());
        JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, new JRBeanCollectionDataSource(resp.getData()));//new JRBeanCollectionDataSource(resp.getData()));

        String jaspResp = "{\n" +
                    "\"jasper\":\"" + Base64.encodeBase64String(jasperService.exportPdfToStream(print).toByteArray()) + "\"\n" +
                    "}";
        return jaspResp;
        }catch(IOException exception) {
            LOGGER.info("Exception on pssf statement... {}", exception.getMessage());
            LOGGER.info(null, exception);
            return null;
        }
    }

//    @Transactional
    public int insertPensionersDocumentsAndLoanReq(String branchCode, String reference, MultipartFile clearanceDocfile, MultipartFile changeBankAccDocFile, String initiatedBy, String pensionerID, String pensionerName, PensionVerificationForm pform) throws IOException {
        String callbackUrl = systemVariable.PENSIONERS_CALLBACK_URL;
        int result=0;int result2=0; int finalResult=0;
            LOGGER.info("checking ..ref .. {}, and . clearanceDocfile size.. {} and changeBankAccDocFile size .... {} and inititatedBy.. {} and pensionerID.... {} and data form ... {}",reference, clearanceDocfile.getSize(), changeBankAccDocFile.getSize(),initiatedBy,pensionerID, pform);
            String pensionerDocInsertSql = "INSERT INTO pensioners_documents (reference, clearanceDoc, clearanceFileName, clearanceFileSize, changeBankAccDoc, changeBankAccDocFileName, changeBankAccDocFileSize,create_dt,created_by) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
            result = jdbcTemplate.update(pensionerDocInsertSql, reference, clearanceDocfile.getBytes(), clearanceDocfile.getOriginalFilename(), clearanceDocfile.getSize(),changeBankAccDocFile.getBytes(), changeBankAccDocFile.getOriginalFilename(), changeBankAccDocFile.getSize(), DateUtil.now(),initiatedBy);
            LOGGER.info("INSERTING PENSIONER DOCUMENT FILES RESPONSE ....: {}", result);

        if(result==1) {
                String pensionerVeriSql = "INSERT INTO pensioners_requests (reference, pensionerId, pensioner_name,pensioner_rubikon_name, loanAmount, monthlyInst, period, narration, bankType, accNumber, initiatedBy, submittedBy, create_dt, status,branch_code, callbackurl) VALUES(?, ?, ?,?, ?,?, ?,?, ?, ?, ?, ?, ?,?, ?, ?)";
            return  jdbcTemplate.update(pensionerVeriSql, reference, pensionerID, pensionerName,pform.getRubikonActNM(), pform.getLoanAmount().replace(",",""), pform.getMonthlyInst().replace(",",""), pform.getPeriod(), pform.getNarration(), pform.getBankType(), pform.getAccNumber(), initiatedBy, null, DateUtil.now(), "I", branchCode, callbackUrl);
        }else{
            return finalResult;
        }
    }

    public String sendPensionerVerificationReq(String payload) {
        String finalRes = HttpClientService.sendTxnToAPI(payload, systemVariable.PENSIONERS_LOAN_VERIFICATION_URL);
                LOGGER.info("laon verification response ... {}", finalRes);
        return finalRes;
    }

    public String firePensionerLoanReqWFAjax(String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from pensioners_requests a";
            totalRecords = jdbcTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(reference,' ',pensionerId,' ',loanAmount,' ',monthlyInst,' ',period,' ',narration,' ',bankType,' ',accNumber,' ',initiatedBy,' ',status,' ',submittedBy) LIKE ?";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM pensioners_requests " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.*, (SELECT name from branches b where b.code = a.branch_code)  branchName from  pensioners_requests a " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select a.*, (SELECT name from branches b where b.code = a.branch_code)  branchName from pensioners_requests a ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info("sql ...{}", mainSql);
                results = jdbcTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }


    public String getSwiftAdviceAttachment(String ref) {
        try {
            String sql = "SELECT messageInPdf FROM transfer_advices WHERE senderReference = ? LIMIT 1";
            LOGGER.info("=============log select====<" + sql);
            byte[] result = this.jdbcTemplate.queryForObject(sql, new Object[]{ref}, (rs, rowNum) -> rs.getBytes(1));

            if (result != null) {
                return java.util.Base64.getEncoder().encodeToString(result);
            } else {
                LOGGER.warn("No PDF found for reference: {}", ref);
                return null;
            }
        } catch (DataAccessException e) {
            LOGGER.error("Error fetching PDF for reference: {} - {}", ref, e.getMessage());
            return null;
        }
    }


    //download supporting document
    public  byte[] getLoanVerDocuments(String ref) {
        byte[] result = null;
        try {
            String sql = "select clearanceDoc  from   pensioners_documents    where   reference = ? limit 1";
            result = (byte[]) this.jdbcTemplate.queryForObject(sql, new Object[]{ref}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked PENSIONER... {}", result, e.getMessage());
            return result;
        }
        return result;
    }

    public  byte[] firePrevChangeAccSupportingDocument(String ref) {
        byte[] result = null;
        try {
            String sql = "select changeBankAccDoc  from   pensioners_documents    where   reference = ? limit 1";
            result = (byte[]) this.jdbcTemplate.queryForObject(sql, new Object[]{ref}, (rs, rowNum) -> rs.getBytes(1));
        } catch (DataAccessException e) {
            result = "96".getBytes();
            LOGGER.error("Result assigned - {}, Rollbacked PENSIONER... {}", result, e.getMessage());
            return result;
        }
        return result;
    }


    public LoanVerificationReq loanVerificationsData(String reference) {
        LoanVerificationReq loanVerificationReq=null;
        try{
            String sql = "SELECT a.*, b.clearanceDoc, b.changeBankAccDoc, (SELECT name from branches c where c.code=a.branch_code) branchName FROM pensioners_requests a JOIN pensioners_documents b on b.reference=a.reference WHERE a.reference=?";
            LOGGER.info(sql.replace("?", "'{}'"),reference);
            return jdbcTemplate.queryForObject(sql, new Object[]{reference},
                    (ResultSet rs, int rowNum)->{
                        LoanVerificationReq lvo = new LoanVerificationReq();
                        lvo.setReference(rs.getString("reference"));
                        lvo.setAccNumber(rs.getString("accNumber"));
                        lvo.setBankType(rs.getString("bankType"));
                        lvo.setLoanAmount(rs.getString("loanAmount"));
                        lvo.setInitiatedBy(rs.getString("initiatedBy"));
                        lvo.setCreate_dt(rs.getString("create_dt"));
                        lvo.setNarration(rs.getString("narration"));
                        lvo.setMonthlyInst(rs.getString("monthlyInst"));
                        lvo.setPensionerId(rs.getString("pensionerId"));
                        lvo.setPensionerName(rs.getString("pensioner_name"));
                        lvo.setPensionerRubikonName(rs.getString("pensioner_rubikon_name"));
                        lvo.setPeriod(rs.getString("period"));
                        lvo.setSubmittedBy(rs.getString("submittedBy"));
                        lvo.setBranchCode(rs.getString("branch_code"));
                        lvo.setBranchName(rs.getString("branchName"));
                        lvo.setChangeBankAccDoc(rs.getBytes ("changeBankAccDoc"));
                        lvo.setClearanceDoc(rs.getBytes("clearanceDoc"));
                        return lvo;
                    });
        }catch (DataAccessException dae){
            LOGGER.info("Data access exception on getting loan verification details object ... {}", dae);
            return null;
        }
    }

    public void updateLoanRecord(String reference, String username) {
        try {
            String sql = "UPDATE pensioners_requests SET status='P', submittedBy=?, submitted_dt=?, pensioner_status='P' WHERE reference=?";
            int result = jdbcTemplate.update(sql,username,DateUtil.now(), reference);
        }catch(DataAccessException ex){
            LOGGER.info("FAILED TO UPDATE ... {}",ex);
        }
    }

    public String processPensionerCallBack(String payLoad) throws JsonProcessingException {
            PensionerLoanVerificationResponse callback = this.jacksonMapper.readValue(payLoad, PensionerLoanVerificationResponse.class);
            String result = "default -1";
            if (callback != null) {
                if (callback.getResponseCode().equalsIgnoreCase("200")) {
                    jdbcTemplate.update("update pensioners_requests set status='C',comments=?,pensioner_status='C',psssf_approved_by=? where  reference=?",callback.getComments(),callback.getApprover(), callback.getReference());
                    result = "{\"result\":\"200\",\"message\":\"RECEIVED\"}";
                } else {
                    //not success
                    jdbcTemplate.update("update pensioners_requests set status='F',comments=?, pensioner_status=?, psssf_approved_by=? where  reference=?",callback.getComments(),callback.getStatus(),callback.getApprover(), callback.getReference());
                    result = "{\"result\":\"99\",\"message\":\"RECEIVED\"}";
                }
            } else {
                //null
                result = "{\"result\":\"-1\",\"message\":\"General Failure null\"}";
            }
            return result;
    }

    public int returnLoanForAmendment(String reference, String username) {
        int result = -1;
        try {
            String sql = "UPDATE pensioners_requests SET status='RA', returned_by=?, returned_dt=? WHERE reference=?";
             result = jdbcTemplate.update(sql, username,DateUtil.now(),reference);
        }catch(DataAccessException ex){
            LOGGER.info("FAILED TO UPDATE return for amendment... {}",ex);
        }
        return result;
    }

    public String firePensionerAmendmentWFAjax(String roleId, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;

        try {
            mainSql = "select count(*) from pensioners_requests where status='RA' ";
            totalRecords = jdbcTemplate.queryForObject(mainSql,Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                searchQuery = " WHERE  concat(reference,' ',pensionerId,' ',loanAmount,' ',monthlyInst,' ',period,' ',narration,' ',bankType,' ',accNumber,' ',initiatedBy,' ',status,' ',submittedBy) LIKE ? AND status='RA'";
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM pensioners_requests " + searchQuery, new Object[]{searchValue}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.*, (SELECT name from branches b where b.code = a.branch_code)  branchName from  pensioners_requests a " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = jdbcTemplate.queryForList(mainSql, new Object[]{searchValue});

            } else {
                mainSql = "select a.*, (SELECT name from branches b where b.code = a.branch_code)  branchName from pensioners_requests a where status='RA'  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
//                LOGGER.info("sql ...{}", mainSql);
                results = jdbcTemplate.queryForList(mainSql);
            }
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        return "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
    }

    public int updatePensionersDocumentsAndLoanReq(String branchCode,String reference, MultipartFile clearanceDocfile, MultipartFile changeBankAccDocFile, String initiatedBy, String pensionerID,String pensionerName, PensionVerificationForm pform) {
        int result=0, result1=0, result2=0, finalResponse=0;
        try {

            if(!clearanceDocfile.isEmpty()) {
                LOGGER.info("Going to update clearance file with reference... {}", reference);
                String clearanceDocUpdateSql = "UPDATE pensioners_documents SET clearanceDoc=?, clearanceFileName=?, clearanceFileSize=? WHERE reference=?";
                LOGGER.info(clearanceDocUpdateSql.replace("?", "'{}'"), clearanceDocfile.getOriginalFilename(), clearanceDocfile.getSize(), clearanceDocfile.getBytes(), reference);
                 result = jdbcTemplate.update(clearanceDocUpdateSql, clearanceDocfile.getOriginalFilename(), clearanceDocfile.getSize(), clearanceDocfile.getBytes(),reference);
            }
            if(!changeBankAccDocFile.isEmpty()){
                LOGGER.info("Going to update changeBankAccDoc file with reference... {}", reference);
                String changeBankAccDocUpdateSql = "UPDATE pensioners_documents SET changeBankAccDoc=?, changeBankAccDocFileName=?, changeBankAccDocFileSize=? WHERE reference=?";
                LOGGER.info(changeBankAccDocUpdateSql.replace("?", "'{}'"), changeBankAccDocFile.getOriginalFilename(), changeBankAccDocFile.getSize(), changeBankAccDocFile.getBytes(), reference);
                 result1 = jdbcTemplate.update(changeBankAccDocUpdateSql, changeBankAccDocFile.getOriginalFilename(), changeBankAccDocFile.getSize(), changeBankAccDocFile.getBytes(),reference);
            }

            String pensionerAmendSql = "UPDATE pensioners_requests SET pensioner_name=?, pensioner_rubikon_name=?, branch_code=?, accNumber=?, loanAmount=?, monthlyInst=?, period=?, narration=?, accNumber=?, status='I' WHERE reference=?";
            LOGGER.info(pensionerAmendSql.replace("?", "'{}'"), pform.getPensionerName(),pform.getRubikonActNM(),pform.getBranchCode(),pform.getAccNumber(), pform.getLoanAmount().replace(",",""), pform.getMonthlyInst(),pform.getPeriod(), pform.getNarration(),pform.getAccNumber(),reference);

            result2 = jdbcTemplate.update(pensionerAmendSql,pform.getPensionerName(),pform.getRubikonActNM(),pform.getBranchCode(),pform.getAccNumber() ,pform.getLoanAmount().replace(",",""), pform.getMonthlyInst(),pform.getPeriod(), pform.getNarration(),pform.getAccNumber(),reference);
            LOGGER.info("UPDATED PENSIONER REQUEST RESPONSE :{} ", result2);

        } catch (Exception e) {
            LOGGER.info("ERROR ON UPDATING EITHER DOCUMENT OF DETAILS FOR PENSIONER : {}", e.getMessage());
        }
        if((result==1) || (result1==1) || (result2==1)){
            LOGGER.info("Everithing is ok.. {}, result1... {}, result2... {}", result,result1,result2);
            finalResponse=1;
        }else{
            LOGGER.info("Somewhere misbehaved.. {}, result1... {}, result2... {}", result,result1,result2);
            finalResponse=0;
        }
        return finalResponse;
    }


    public List<Map<String, Object>> firePensionerReportsAjax(String txnStatus, String fromDate, String toDate) {
        String mainSql;
        List<Map<String, Object>> results=null;

        switch (txnStatus) {
            case "C":
                //SUCCESS
                mainSql = "select pr.*, (SELECT name from branches b where b.code = pr.branch_code)  branchName from pensioners_requests pr where pr.status=? and pr.pensioner_status=? and pr.create_dt>=? and pr.create_dt<=?";
                results = jdbcTemplate.queryForList(mainSql, new Object[]{txnStatus,txnStatus,fromDate, toDate});
                break;
            case "RA":
            case "I":
                //INITIATED
                //RETURNED FOR AMENDMENT
                mainSql = "select pr.*, (SELECT name from branches b where b.code = pr.branch_code)  branchName from pensioners_requests pr where pr.status=?  and pr.create_dt>=? and pr.create_dt<=?";
                results = jdbcTemplate.queryForList(mainSql, new Object[]{txnStatus,fromDate, toDate});
                break;
            case "P":
                //PENDING AT PSSSF
                mainSql = "select pr.*,(SELECT name from branches b where b.code = pr.branch_code)  branchName from pensioners_requests pr where pr.status='P' and pr.pensioner_status='P' and pr.create_dt>=? and pr.create_dt<=?";
                results = jdbcTemplate.queryForList(mainSql, new Object[]{fromDate, toDate});
                break;
            default:
                mainSql = "select pr.*,(SELECT name from branches b where b.code = pr.branch_code)  branchName from pensioners_requests pr where pr.status=? and pr.pensioner_status=? and pr.create_dt>=? and pr.create_dt<=?";
                results = jdbcTemplate.queryForList(mainSql, new Object[]{"I","L",fromDate, toDate});
        }
        return results;
    }

    public String getUserByUserName(String username) {
        String resp = null;
        String sql = "select concat(first_name,' ',middle_name,' ',last_name) from users where username='"+username+"'";
        try {
            Object o = (String) jdbcTemplate.queryForObject(sql, String.class);
            resp = (String) o;
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }
        return resp;
    }

    public List<Map<String,Object>> getBranches() {
        List<Map<String,Object>> results=null;
        try{
            return  jdbcTemplate.queryForList("SELECT * from branches");
        }catch(DataAccessException exception){
            LOGGER.info("Database access exceptions.... {}", exception);
            return results;
        }
    }

    public String getAccountDetails(String accountNo) {
        String jsonString = null;
        String json = null;
        try {
            List<Map<String, Object>> findAll;
            String mainSql = "select c.cust_no,c.cust_nm, a.acct_no,cu.CRNCY_CD_ISO currency, (SELECT BU_NM FROM BUSINESS_UNIT bu WHERE bu.BU_ID =a.MAIN_BRANCH_ID) branchName,\n"
                    + "' ' ACCT_DESC,REPLACE((select REPLACE(AD.ADDR_LINE_1,'None','')  from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','') ADDR_LINE_1,\n"
                    + "REPLACE((select REPLACE(AD.ADDR_LINE_2,'None','') from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','')  ADDR_LINE_2,\n"
                    + "REPLACE((select REPLACE(AD.ADDR_LINE_3,'None','') from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','') ADDR_LINE_3,\n"
                    + "REPLACE((select REPLACE(AD.ADDR_LINE_4,'None','') from customer_address ca, address ad where CA.ADDR_ID=AD.ADDR_ID and CA.CUST_ID=c.cust_id AND rownum=1 ),'null','') ADDR_LINE_4, cu.crncy_cd, DAS.CLEARED_BAL\n"
                    + " from account a, customer c, currency cu, deposit_account_summary das\n"
                    + "where a.cust_id=c.cust_id\n"
                    + "and CU.CRNCY_ID=A.CRNCY_ID\n"
                    + "and das.acct_no=a.acct_no\n"
                    + "and (a.acct_no='" + accountNo + "' OR a.OLD_ACCT_NO ='" + accountNo + "')"; //"select * from tp_transaction  where msisdn like '%" + input + "%' OR txReceipt like '%" + input + "%' OR txid like '%$input%' OR txsourceAccount like '%" + input + "%' OR txdestinationAccount like '%" + input + "%' order by txdate desc limit 1000";
//            LOGGER.info("getAccountDetails:{}", mainSql);
            findAll = this.jdbcRUBIKONTemplate.queryForList(mainSql);
            //Java objects to JSON string - compact-print - salamu - Pomoja.

            try {
                jsonString = this.jacksonMapper.writeValueAsString(findAll);
                //LOGGER.info("RequestBody");
            } catch (JsonProcessingException ex) {
                LOGGER.info("EXCEPTION ON GETTING ACCOUNT DETAILS: ", ex);
            }
            json = jsonString;
            return json;
        } catch (Exception e) {
            LOGGER.info("EXCEPTION ON GETTING ACCOUNT DETAILS: ", e);
            return json;
        }

    }

    public String verifyPensioner(String nin, String accountNumber, String firstName, String middleName, String lastName,
                                  String accountStatus, String accountStatusDesc, String fingerImage, String fingerCode,
                                  String phoneNumber) {
        String msgId = "TCB" + DateUtil.now("yyyyMMddHHmmss");
        String createdAt = DateUtil.now("yyyy-MM-dd'T'HH:mm:ss");
        String msg = "{" +
                "        \"messageHeader\": {" +
                "            \"sender\": \"TAPBTZTZ\"," +
                "            \"receiver\": \"TPPS\"," +
                "            \"msgType\": \"PENSIONER_VERIFICATION\"," +
                "            \"msgId\": \"" + msgId + "\"," +
                "            \"createdAt\": \"" + createdAt + "\"" +
                "        }," +
                "        \"messageDetail\": {" +
                "            \"nin\": \"" + nin + "\"," +
                "            \"accountNumber\": \"" + accountNumber + "\"," +
                "            \"firstName\": \"" + firstName + "\"," +
                "            \"middleName\": \"" + middleName + "\"," +
                "            \"lastName\": \"" + lastName + "\"," +
                "            \"accountStatus\": \"" + accountStatus + "\"," +
                "            \"accountStatusDesc\": \"" + accountStatusDesc + "\"," +
                "            \"phoneNumber\": \"" + phoneNumber + "\"," +
                "            \"fingerCode\": \"" + fingerCode + "\"," +
                "            \"fingerImage\": \"" + fingerImage + "\"" +
                "        }" +
                "}";
        String msgToBeSigned = msg.replaceAll("\\s+", "");
        LOGGER.info("Message to be signed... {}", msgToBeSigned);
        try {
            String signature = SignRequest.generateSignature256(msgToBeSigned, "bank.12345",
                    "tcbbank", systemVariable.MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH);
//            String signature = SignRequest.generateSignature256(msgToBeSigned,
//                    "passpass", systemVariable.MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH);
            String request = "{\n" +
                    "    \"message\": {\n" +
                    "        \"messageHeader\": {\n" +
                    "            \"sender\": \"TAPBTZTZ\",\n" +
                    "            \"receiver\": \"TPPS\",\n" +
                    "            \"msgType\": \"PENSIONER_VERIFICATION\",\n" +
                    "            \"msgId\": \"" + msgId + "\",\n" +
                    "            \"createdAt\": \"" + createdAt + "\"\n" +
                    "        }," +
                    "        \"messageDetail\": {\n" +
                    "            \"nin\": \"" + nin + "\",\n" +
                    "            \"accountNumber\": \"" + accountNumber + "\",\n" +
                    "            \"firstName\": \"" + firstName + "\",\n" +
                    "            \"middleName\": \"" + middleName + "\",\n" +
                    "            \"lastName\": \"" + lastName + "\",\n" +
                    "            \"accountStatus\": \"" + accountStatus + "\",\n" +
                    "            \"accountStatusDesc\": \"" + accountStatusDesc + "\",\n" +
                    "            \"phoneNumber\": \"" + phoneNumber + "\",\n" +
                    "            \"fingerCode\": \"" + fingerCode + "\",\n" +
                    "            \"fingerImage\": \"" + fingerImage + "\"\n" +
                    "        }\n" +
                    "    },\n" +
                    "    \"digitalSignature\": \"" + signature + "\"\n" +
                    "}";
            LOGGER.info("PENSIONER VERIFICATION REQUEST... {}", request);
            String domain;
            if (systemVariable.ACTIVE_PROFILE.equalsIgnoreCase("prod")) {
                domain = "172.21.1.13";
            } else {
                domain = "172.21.2.12";
            }
            String url = "http://" + domain + ":8547/mof/api/v1/request";
            String headers = "service-code=SRVC0022";
            String msgToBeSent = request.replaceAll("\\s+", "").replaceAll("\\n+", "");
            LOGGER.info("Message to be sent... {}", msgToBeSent);
            return HttpClientService.sendTxnToAPI(msgToBeSent, url, headers);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
