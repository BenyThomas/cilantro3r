/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository.reports;

import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.repository.BanksRepo;
import com.repository.EftRepo;
import com.queue.QueueProducer;
import com.service.JasperService;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JasperPrint;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class EftReportsRepo {
    
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
    JasperService jasperService;
    
    @Autowired
    BanksRepo banksRepo;
    
    @Autowired
    EftRepo eftRepo;
    
    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource cilantroDataSource;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EftReportsRepo.class);
    
    public String getEftBulkPaymentsOnWorkFlowAjax(String direction, String fromDate, String todate, String branchNo, String draw, String start, String rowPerPage, String searchValue, String columnIndex, String columnName, String columnSortOrder) {
        List<Map<String, Object>> results = null;
        String mainSql;
        int totalRecordwithFilter = 0;
        int totalRecords = 0;
        String jsonString = null;
        String branchString = "";
        try {
            if (branchNo.equalsIgnoreCase("all")) {
                branchString = " And branch_no='" + branchNo + "'";
            }
            //totalRecords = jdbcTemplate.queryForObject("SELECT COUNT(txnid) FROM cbstransactiosn where ttype=? and txndate>=? and txndate<=?and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=?) and txn_status LIKE '%Success%'", new Object[]{ttype, txndate + " 00:00:00", txndate + " 23:59:59", ttype, txndate + " 00:00:00", txndate + " 23:59:59"}, Integer.class);
            mainSql = "select count(*) from transfer_eft_batches a where a.direction=? and a.create_dt>='" + fromDate + " 00:00:00' and a.create_dt<='" + todate + " 23:59:59'  and a.txn_type = '005'" + branchString;
            totalRecords = jdbcTemplate.queryForObject(mainSql, new Object[]{direction}, Integer.class);
            String searchQuery = "";
            if (!searchValue.equals("")) {
                searchValue = "'%" + searchValue + "%'";
                //searchQuery = " WHERE concat(txnid,' ',txn_type,' ',ttype,' ',txndate,' ',sourceaccount,' ',destinationaccount,' ',amount,' ',terminal) LIKE ? AND ttype=? and txndate>=? AND txndate<=?  and txnid  not in (select txnid from thirdpartytxns where ttype=? AND txndate>=? and txndate<=? ) AND  txn_status LIKE '%Success%' and txn_type=? and and b.txnid not in (select c.txnid from cbstransactiosn c where c.ttype = '" + ttype + "'  AND c.txndate >= '" + txndate + " 00:00:00' AND c.txndate <= '" + txndate + " 23:59:59'  AND c.txn_status not like '%success%')";
                searchQuery = " WHERE  concat(sourceAcct,' ',sender_name,' ',batch_reference,' ',totalAmount,' ',noOfTxns,' ',debit_mandate) LIKE ? and a.direction=? and a.create_dt>='" + fromDate + " 00:00:00' and a.create_dt<='" + todate + " 23:59:59'  and a.txn_type = '005'" + branchString;
                totalRecordwithFilter = jdbcTemplate.queryForObject("SELECT count(id) FROM transfer_eft_batches  " + searchQuery, new Object[]{searchValue, direction}, Integer.class);
            } else {
                totalRecordwithFilter = totalRecords;
            }
            if (!searchQuery.equals("")) {
                mainSql = "select a.narration purpose, a.initiated_by,(select b.sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select b.sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name,a.batch_reference,a.total_amount totalAmount,a.number_of_txns noOfTxns,a.debit_mandate debit_mandate,(select b.status from transfers b where b.batch_reference=a.batch_reference limit 1) as status, (select b.cbs_status from transfers b where b.batch_reference=a.batch_reference limit 1) as cbs_status from transfer_eft_batches a where and a.direction=? and a.create_dt>='" + fromDate + " 00:00:00' and a.create_dt<='" + todate + " 23:59:59'  and a.txn_type = '005' " + branchString + " GROUP by a.batch_reference  " + searchQuery + " ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{searchValue, direction});
                
            } else {
                mainSql = "select a.narration purpose,a.initiated_by,(select b.sourceAcct from transfers b where b.batch_reference=a.batch_reference limit 1) sourceAcct,(select b.sender_name from transfers b where b.batch_reference=a.batch_reference limit 1) sender_name,a.batch_reference,a.total_amount totalAmount,a.number_of_txns noOfTxns,a.debit_mandate debit_mandate,(select b.status from transfers b where b.batch_reference=a.batch_reference limit 1) as status, (select b.cbs_status from transfers b where b.batch_reference=a.batch_reference limit 1) as cbs_status from transfer_eft_batches a where and a.direction=? and a.create_dt>='" + fromDate + " 00:00:00' and a.create_dt<='" + todate + " 23:59:59'  and a.txn_type = '005' " + branchString + " GROUP by a.batch_reference  ORDER BY " + columnName + " " + columnSortOrder + " limit " + start + "," + rowPerPage;
                results = this.jdbcTemplate.queryForList(mainSql, new Object[]{direction});
            }

            //Java objects to JSON string - compact-print - salamu - Pomoja.
            jsonString = this.jacksonMapper.writeValueAsString(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("RequestBody: ", ex);
        }
        String json = "{\"draw\":" + draw + ",\"iTotalRecords\":\"" + totalRecords + "\",\"iTotalDisplayRecords\":\"" + totalRecordwithFilter + "\",\"aaData\":" + jsonString + "}";
//        System.out.println("ACCOUNT BALANCES: " + json);
        return json;
    }
    
    public String getEftOutwardTranstion(String batchReference, String exporterFileType, HttpServletResponse response, String destName) {
        
        String reportFileTemplate = "/iReports/eft/eft-outward-transaction-report-batches.jasper";
        try {
            Map<String, Object> parameters = new HashMap<>();
            List<Map<String, Object>> batchdetails = eftRepo.getEftBatch(batchReference);
            LOGGER.info("BATCH DETAILS:{}", batchdetails);
            if (batchdetails != null) {
                for (Map<String, Object> details : batchdetails) {
                    String status = "-1";
                    if (details.get("status").toString().equalsIgnoreCase("I")) {
                        status = "Initiated";
                    }
                    if (details.get("status").toString().equalsIgnoreCase("P")) {
                        status = "pending HQ";
                    }
                    if (details.get("status").toString().equalsIgnoreCase("C")) {
                        status = "Completed";
                    }
                    if (details.get("status").toString().equalsIgnoreCase("S")) {
                        status = "Settled";
                    }
                    parameters.put("BRANCH_NAME", details.get("branchName") + "");
                    parameters.put("CREATE_DATE", details.get("create_dt") + "");
                    parameters.put("INITIATED_BY", details.get("initiated_by") + "");
                    parameters.put("BRANCH_APPROVER", details.get("approved_by") + "");
                    parameters.put("HQ_APPROVER", details.get("authorized_by") + "");
                    parameters.put("MANDATE", details.get("debit_mandate") + "");
                    parameters.put("BATCH_REFERENCE", details.get("batch_reference") + "");
                    parameters.put("INITIATED_DATE", details.get("create_dt") + "");
                    parameters.put("BRANCH_APPROVER_DATE", details.get("approved_dt") + "");
                    parameters.put("HQ_APPROVED_DATE", details.get("authorized_dt") + "");
                    parameters.put("BATCH_AMOUNT", details.get("total_amount") + "");
                    parameters.put("BATCH_STATUS", status);
                }
            }
            parameters.put("DEPARTMENT_NAME", "IT AND OPERATIONS");
            parameters.put("PRINTED_DT", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
            parameters.put("TXN_DATE", DateUtil.now("yyyy-MM-dd HH:mm:ss"));
            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, cilantroDataSource.getConnection());
            return jasperService.exportFileOption(print, exporterFileType, response, destName);
        } catch (Exception ex) {
            Logger.getLogger(ReconReportsRepo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
            
        }
        
    }
    
}
