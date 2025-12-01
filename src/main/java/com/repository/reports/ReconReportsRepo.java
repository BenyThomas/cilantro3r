/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository.reports;

import com.DTO.Reports.recon.ReconSummaryIreport;
import com.helper.DateUtil;
import com.repository.Recon_M;
import com.service.JasperService;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JasperPrint;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class ReconReportsRepo {

    @Autowired
    JasperService jasperService;
    @Autowired
    Recon_M reconRepo;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("amgwConnection")
    HikariDataSource cilantroDataSource;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ReconReportsRepo.class);

//    public String getReconSummaryReport(String exporterFileType, HttpServletResponse response, String destName, String txnType, String ttype, String txnDate, String printedBy) {
////        LOGGER.info("RECON DATA:{}", reconRepo.getCummulativeReconciliation(txnDate, txnType, ttype));
//        LOGGER.info("RECON DATA: RECON DATE:{} TXN_TYPE:{} TTYPE:{}", txnDate, txnType, ttype);
//        String reportFileTemplate = "/iReports/recon/adjusted-reconciliation-report.jasper";
//        List<CummulativeReconciliationObject> cuRecon = reconRepo.getCummulativeReconciliation(txnDate, txnType, ttype);
//        try {
//            Map<String, Object> parameters = new HashMap<>();
//            parameters.put("CASHBOOK_OPENING_BALANCE", cuRecon.get(0).getCashBookOpeningBalance());
//            parameters.put("REFUND_FOSA", cuRecon.get(0).getCashBookRefundFosa());
//            parameters.put("CASHBOOK_CUSTOMER_WITHDRAWS", cuRecon.get(0).getCashBookCustomerWithraws());
//            parameters.put("CASHBOOK_WITHDRAWS_CHARGES", cuRecon.get(0).getCashBookCustomerWithrawsCharges());
//            parameters.put("LEDGER_FEES", cuRecon.get(0).getBankLedgerFees());
//            parameters.put("BANK_STATEMENT_CLOSING_BALANCE", cuRecon.get(0).getBankClosingBalance());
//            parameters.put("CASHBOOK_CLOSING_BALANCE", cuRecon.get(0).getCashBookClosingBalance());
//            parameters.put("RECON_DATE", txnDate);
//            parameters.put("UNCREDITED_CHEQUE", cuRecon.get(0).getBankUncreditedCheques());
//            parameters.put("UNCREDIT_DEPOSITS", cuRecon.get(0).getBankUncreditedDeposits());
//            parameters.put("WITHDRAWS_NOT_IN_CASHBOOK", cuRecon.get(0).getBankCashWithdrawsNotInFosa());
//            parameters.put("BANK_TRANSACTION_CHARGES", cuRecon.get(0).getBankTransactionCharges());
//            parameters.put("PREVIOUS_TRANSACTION_WITHDRAWS_CHARGES", cuRecon.get(0).getBankPreviousTransactionCharges());
//            BigDecimal totalAddBack = cuRecon.get(0).getBankTransactionCharges().add(cuRecon.get(0).getBankCashWithdrawsNotInFosa().add(cuRecon.get(0).getBankUncreditedCheques().add(cuRecon.get(0).getBankUncreditedDeposits())));
//            parameters.put("TOTAL_ADD_BACK_EXCEPTIONS_TO_BANK", totalAddBack);
//            parameters.put("WITHDRAWS_NOT_IN_BANK", cuRecon.get(0).getBankUncommissionedCustomerWithdraws());
//            parameters.put("WITHDRAWS_CHARGES_NOT_IN_BANK", cuRecon.get(0).getBankUncommissionedCustomerWithdrawsCharges());
//            BigDecimal depositsNotInCashbook = cuRecon.get(0).getBankDepositsNotInCashbook();
//            parameters.put("DEPOSITS_NOT_IN_CASHBOOK", depositsNotInCashbook);
//            BigDecimal differenceInClosingBL = cuRecon.get(0).getBankOpeningBalance().subtract(cuRecon.get(0).getCashBookOpeningBalance());//.subtract(cuRecon.get(0).getCashBookRefundFosaLessBack());
////            BigDecimal cashbookDifferenceBank = differenceInClosingBL.subtract(cuRecon.get(0).getCashBookRefundFosaLessBack());
////            parameters.put("DIFFERENCE_IN_CASHBOOK_AND_BANK_STATEMENT", cashbookDifferenceBank);
//            BigDecimal totalLessBack = depositsNotInCashbook.add(cuRecon.get(0).getBankUncommissionedCustomerWithdraws()).add(cuRecon.get(0).getBankUncommissionedCustomerWithdrawsCharges());
//            parameters.put("LESS_TOTAL_EXCEPTIONS_ON_BANK", totalLessBack);
//            parameters.put("ADJUSTED_BANK_CLOSING_BALANCE", cuRecon.get(0).getBankClosingBalance().add(totalAddBack.subtract(totalLessBack)));
//            parameters.put("PRINTED_BY", printedBy);
//            parameters.put("PRINTED_DATE", DateUtil.now());
//            parameters.put("BANK_ACCOUNT", "180207000052");
//            parameters.put("ACCOUNT_NAME", "TANESCO SACCOS LTD SETTLEMENT A/C");
//            JasperPrint print = jasperService.jasperPrint(reportFileTemplate, parameters, new JRBeanCollectionDataSource(cuRecon));
//            return jasperService.exportFileOption(print, exporterFileType, response, destName);
//        } catch (IOException ex) {
//            Logger.getLogger(ReconReportsRepo.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//
//        } catch (Exception ex) {
//            LOGGER.info(null, ex);
//            Logger.getLogger(ReconReportsRepo.class.getName()).log(Level.SEVERE, null, ex);
//
//            return null;
//
//        }
//
//    }
    public Collection reconSummaryCummulativeReport(String ttype, String txn_type, String txnDate, String txnType) {
        try {
            String sqlQuery = "SELECT  * from recon_tracker where date(recondt)=? and txn_type=?";
            StopWatch watch = new StopWatch();
            watch.start();
            List<ReconSummaryIreport> data = this.jdbcTemplate.query(sqlQuery, new Object[]{txnDate, txnType}, (ResultSet rs, int rowNum) -> {
                ReconSummaryIreport row = new ReconSummaryIreport(rs.getString("txn_type"), rs.getString("description"), rs.getString("cbsopeningbalance"), rs.getString("cbstxnsCount"), rs.getString("cbstxnsVolume"), rs.getString("cbsClosingBalance"), rs.getString("cbsCharge"), rs.getString("thirdpartyOpeningBalance"), rs.getString("thirdPartytxnsCount"), rs.getString("thirdPartytxnsVolume"), rs.getString("thirdpartyClosingBalance"), rs.getString("thirdpartyCharge"), rs.getString("diffOpeningBalance"), rs.getString("difftxnCount"), rs.getString("difftxnVolume"), rs.getString("diffchargeVolume"), rs.getString("diffCloasingBalance"), rs.getString("first_status"), rs.getString("second_status"), rs.getString("initiated_by"), rs.getString("recon_initiator"), rs.getString("confirmed_by"), rs.getString("recondt"), rs.getString("create_dt"), rs.getString("modified_dt"));
                return row;
            });
            watch.stop();
            return data;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String getAdjustedReconReport() {

        return null;

    }

    public List<Map<String, Object>> getThirdPartyCreditsNotInCBS(String txnType, String ttype, String reconDate) {
        String sql = "select\n"
                + "count(*) txnCount,sum(amount) txnVolume\n"
                + "from\n"
                + "	thirdpartytxns\n"
                + "where\n"
                + "	ttype =?\n"
                + "	and txndate >= ? \n"
                + "	and txndate <=? \n"
                + "	and txn_type =?\n"
                + "	and mnoTxns_status IN ('Refund~ Money returned to TPB Disbursement account','Reversed','Reversed to customer account','SUCCESSFUL','Success','Successfully Reversed','successfully','Cash Movement Between Accounts')\n"
                + "	and trim(txnid) not in (\n"
                + "	select\n"
                + "		trim(txnid)\n"
                + "	from\n"
                + "		cbstransactiosn\n"
                + "	where\n"
                + "	ttype =?\n"
                + "	and txn_type =?\n"
                + "	and txndate >= ? \n"
                + "	and txndate <=?)";

        return this.jdbcTemplate.queryForList(sql, txnType, ttype, reconDate + " 23:59:59", txnType, ttype, reconDate + " 23:59:59");

    }

    public List<Map<String, Object>> getCBSCreditsNotInThirdparty(String txnType, String ttype, String reconDate) {
        String sql = "	\n"
                + "select\n"
                + "	count(b.txnid) as txnCount,\n"
                + "	IFNULL(SUM(cast(b.amount as decimal(18))), 0) as txnVolume\n"
                + "from\n"
                + "	cbstransactiosn b\n"
                + "where\n"
                + "	trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(a.txnid)\n"
                + "	from\n"
                + "		thirdpartytxns a\n"
                + "	where\n"
                + "		 a.ttype = ?\n"
                + "	and a.txn_type = ?\n"
                + "	and a.txndate >= ?\n"
                + "	and a.txndate <= ?)\n"
                + "	and b.ttype = ?\n"
                + "	and b.txn_type = ?\n"
                + "	and b.txndate >= ?\n"
                + "	and b.txndate <= ?\n"
                + "	and b.dr_cr_ind ='CR'\n"
                + "	and trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(c.txnid)\n"
                + "	from\n"
                + "		cbstransactiosn c\n"
                + "	where\n"
                + "		 c.ttype = ?\n"
                + "		and c.txn_type = ?\n"
                + "		and c.txndate >= ?\n"
                + "		and c.txndate <= ?\n"
                + "		and c.dr_cr_ind ='DR')";

        return this.jdbcTemplate.queryForList(sql, txnType, ttype, reconDate + " 23:59:59", txnType, ttype, reconDate + " 23:59:59");
    }
      public List<Map<String, Object>> getCBSdebitsNotInThirdparty(String txnType, String ttype, String reconDate) {
        String sql = "	\n"
                + "select\n"
                + "	count(b.txnid) as txnCount,\n"
                + "	IFNULL(SUM(cast(b.amount as decimal(18))), 0) as txnVolume\n"
                + "from\n"
                + "	cbstransactiosn b\n"
                + "where\n"
                + "	trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(a.txnid)\n"
                + "	from\n"
                + "		thirdpartytxns a\n"
                + "	where\n"
                + "		 a.ttype = ?\n"
                + "	and a.txn_type = ?\n"
                + "	and a.txndate >= ?\n"
                + "	and a.txndate <= ?)\n"
                + "	and b.ttype = ?\n"
                + "	and b.txn_type = ?\n"
                + "	and b.txndate >= ?\n"
                + "	and b.txndate <= ?\n"
                + "	and b.dr_cr_ind ='DR'\n"
                + "	and trim(b.txnid) not in (\n"
                + "	select\n"
                + "		trim(c.txnid)\n"
                + "	from\n"
                + "		cbstransactiosn c\n"
                + "	where\n"
                + "		 c.ttype = ?\n"
                + "		and c.txn_type = ?\n"
                + "		and c.txndate >= ?\n"
                + "		and c.txndate <= ?\n"
                + "		and c.dr_cr_ind ='CR')";

          return this.jdbcTemplate.queryForList(sql, txnType, ttype, reconDate + " 23:59:59", txnType, ttype, reconDate + " 23:59:59");
    }

}
