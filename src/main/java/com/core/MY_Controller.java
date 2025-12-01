/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.core;

import com.entities.Modules;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author MELLEJI
 */
@Component
public class MY_Controller implements Filter {

    public static final String PROXY_URL = "http://172.20.1.12:8082/api/gepgProxy.php";
    @Autowired
    HttpSession httpSession;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;

    @Override
    public void init(FilterConfig config) throws ServletException {
        HtmlCompressor compressor = new HtmlCompressor();
        compressor.setCompressCss(true);
        compressor.setCompressJavaScript(true);
    }

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MY_Controller.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        try {
            /*Logics*/
            HttpServletRequest request2 = (HttpServletRequest) request;

            if (request2.getRequestURI().equalsIgnoreCase("/")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/login")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/logout")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/permissionDenied")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/dashboard")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/assets/")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/selectOpRole")) {
                //select operational role view
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/setPostingRoleInSession")) {
                //set a posting role into session,(user already has a session
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/downloadSupportingDoc")) {
                //download supporting document
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getGepgAcountsAjax")) {
                //get GePG accounts
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getGepgAccountBalancesAjax")) {
                //get GePG accounts Balances from RUBIKON
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/syncRubikonTxnSearch")) {
                //Sync search transaction from RUBIKON
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewGePGBalancesForRemittance")) {
                //previewGePG Account Balances ready  For Remittance
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getOnlineRemittanceAjax")) {
                //get online banking transactions for Approval
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getGepgMNORemittanceTxnsAjax")) {
                //get GePG/MNO transactions on the queue
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getRTGSTxnOnWorkFlowAjax")) {
                //get RTGS transactions on the queue
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewSwiftMsg")) {
                //Preview Swfit message transactions
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewSupportingDocuments")) {
                //Preview Swfit message supporting documents transactions
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewSupportingDocument")) {
                //Preview Swfit message supporting documents transactions
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getBranchRemittanceAjax")) {
                //get BRANCH REMITTANCE TRANSACTIONS
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewEftPerBankTxns")) {
                //get EFT SUCCESS TRANSACTIONS DAILY
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getInwardEFTPerBankAjax")) {
                //get EFT SUCCESS AJAX DAILY TRANSACTIONS
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/coreBankingExchangeRate")) {
                //get CORE-BANKING-EXCHANGE-RATE
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewSpecialRateTxn")) {
                //VIEW SPECIAL RATE TRANSACTION
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/approveSpecialRate")) {
                //APPROVE SPECIAL RATE
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/queryLedgerDetails")) {
                //QUERY GL ACCOUNT DETAILS /
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/queryServiceProviderDetails")) {
                //QUERY SERVICE PROVIDER DETAILS
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/api/")) {
                //WEBSERVICE
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/onlineMandate")) {
                //PREVIEW RTGS MESSAGE AT FINANCE
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/downloadEftBulkSampleFile")) {
                //PREVIEW RTGS MESSAGE AT FINANCE
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/returnInwardEftTransactions")) {
                //RETUN INWARD FAILED EFT TRANSACTION
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/replayEFTTxnsToCoreBanking")) {
                //REPLAY EFT TRANSACTION TO CORE BANKING
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/queryCustomerDetails")) {
                //get customer details by account
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/approveIBProfileAtBranchLevel")) {
                //get customer details by account
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/queryAccDetailsByRim")) {
                //get customer details by account
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/queryClientAccountDetails")) {
                //get customer details by account
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/getAccountsAttachedToProfile")) {
                //get accounts attached to ib profile
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/getSignatoriesAjax")) {
                //get accounts attached to ib profile
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/getSignatoryAccountAccess")) {
                //get accounts attached to ib profile
                chain.doFilter(request2, response);//
            } else if (request2.getRequestURI().contains("/getSignatoryRoleAccess")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/initiatorSubmitProfileForApproval")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewIbProfileDocument")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getCoreBankingRates")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/recon-summary-report")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getBankListWithTransferTypes")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/downloadEftOutwardReport")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/editUser")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/downloadUsersReportMatrix")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getBanksAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/banksForm")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/addNewBank")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getCardsRequestsPendingAtBranch")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewCardRequest")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/branchApproveCardCreationRequest")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/queryControlNumberDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getCustomerKycAndAccountDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getAccountDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/queryReconCatgoryBasedOnReconType")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/reconciliationReports")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/downloadTransferAdvice")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/redumpPayment")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().startsWith("/fire")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/error")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("getTransferIncomingDestAcctExceptions")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("downloadTransferAdvice")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/unlockTerminal")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/HqCancelCardPANRequestGeneration")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireReprocessTransaction")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireGePGTransactions")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/firePortalAdminModule")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireVisaCardReport")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireChangeCardCollectingBranch")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/updateCardCollectingBranchAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getPensionPayrollAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/viewPensionPayrollDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getPensionPayrollDetailsAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getAmountFromReference")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/authorizeTransactionsWithReason")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/approveAuthorizedTransactions")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/complyTransactionWithReason")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/approveCompliance")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/processPensionPayrollToCoreBanking")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/returnTransactionWithReason")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/downloadPensionBatchFile")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/rejectVisaCardAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireVisaCardReportAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/queryBeneficiaryAccountDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/tipsDashboard")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/initiateTransfer")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/outwardTipsTransferOnWF")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewTipsSwiftMsg")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/authorizeTIPSonWorkFlow")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/inwardReversalOnWFAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getTipsTransactionDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/initiateTipsTransferReversal")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/tipsTransactionsReportAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireGetPartnerAccount")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireGetPartnerAccountAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/confirmTxnStatusOnBOT")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/updateCustomerPhoneAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/initiateTipsTxnReversal")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/reverseRequestedTipsTransaction")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/tipsFraudRegistration")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/LUKUInitiateBulkRefundRetry")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/tipsAuthorizeRequestedReversal")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/registerTipsFraud")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/LUKUsubmitInitiateBulkRefundRetry")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getTCBTipsFraudsAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/fireRejectCardModal")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewMUSESupportingDocument")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/cummulativeReconciliationReport")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/updateCardPanAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/viewCustomerAttachment")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/viewAccountAttachment")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getKYCCustomerDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/editCustomerDetails")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/assets/upload/region-district.json")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/viewPendingIBProfileInitiator")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getPendingAtHqIBProfilesAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/viewPendingIBProfileHq")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/registerIbProfile")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/removeAccountFromIbProfile")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/removeSignatoryFromIbProfile")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/updateAgentDevice")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getKYCReports")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/loadAppVersions")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/ibRegistrationReportAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/processTempBatches")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/checkAccounts")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/getPendingAtBranchIBProfilesAjax")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewTempBatchTransactions")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/returnTransactionWithReason")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/previewBatchAccountsList")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/viewMoFPWorkflowAttachments")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/api/")) {
                chain.doFilter(request2, response);
            } else if (request2.getRequestURI().contains("/enrollMobileBanking")) {
                chain.doFilter(request2, response);
            } else {
                boolean proceed = false;
                List<Modules> modules = (List) httpSession.getAttribute("modules");
                if (modules != null) {
                    for (Modules module : modules) {
                        for (Map<String, Object> getPermission : module.getGetPermissions()) {
                            if ((request2.getRequestURI().equalsIgnoreCase(getPermission.get("url").toString().trim())) || (request2.getRequestURI().equalsIgnoreCase(getPermission.get("url2").toString().trim())) || (request2.getRequestURI().equalsIgnoreCase(getPermission.get("sub_url").toString().trim()))) {
                                proceed = true;
                                break;
                            }
                        }
                        //check sub permissions
                        for (Map<String, Object> getSuPermission : module.getGetSubPermissions()) {
                            if ((request2.getRequestURI().equalsIgnoreCase(getSuPermission.get("url").toString().trim())) || (request2.getRequestURI().equalsIgnoreCase(getSuPermission.get("ajax_url").toString().trim()))) {
                                proceed = true;
                                break;
                            }
                        }
                        //check operations permissions
                        for (Map<String, Object> operationsPermission : module.getGetOperationsPermissions()) {
                            if ((request2.getRequestURI().equalsIgnoreCase(operationsPermission.get("url").toString().trim())) || (request2.getRequestURI().equalsIgnoreCase(operationsPermission.get("ajax_url").toString().trim()))) {
                                proceed = true;
                                break;
                            }
                        }
                        //check payments modules
                        for (Map<String, Object> paymentModulePermission : module.getPaymentsModules()) {
                            if ((request2.getRequestURI().equalsIgnoreCase(paymentModulePermission.get("module_dashboard_url").toString().trim()))) {
                                proceed = true;
                                break;
                            }
                        }
                        //check payments modules sub permissions
                        for (Map<String, Object> modulesSubpermission : module.getPaymentsPermissions()) {
                            if ((request2.getRequestURI().equalsIgnoreCase(modulesSubpermission.get("url").toString().trim())) || (request2.getRequestURI().equalsIgnoreCase(modulesSubpermission.get("ajax_url").toString().trim()))) {
                                proceed = true;
                                break;
                            }
                        }
                        //check kyc modules
                        if (module.getKycPermissions() != null) {
                            for (Map<String, Object> kycModulePermission : module.getKycPermissions()) {
                                if ((request2.getRequestURI().contains(kycModulePermission.get("url").toString().trim()))
                                        || (request2.getRequestURI().contains(kycModulePermission.get("ajax_url").toString().trim()))
                                        || (request2.getRequestURI().contains(kycModulePermission.get("sub_url").toString().trim()))) {
                                    proceed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!proceed) {
                    LOGGER.info("Requested page URL: " + request2.getRequestURI());
                    LOGGER.info("Is the User Allowed to Proceed ? :[ " + proceed + "]");
                }
                if (proceed) {
                    try {
                        //get the audit trail as allowed
                        //AuditTrails.setIpaddress(request.getRemoteAddr());
//                        System.out.println("REMOTE HOST ADDRESS: " + request.getServerName());

//                        AuditTrails.setStatus("success");
//                        saveAuditTrail(httpsession.getAttribute("username").toString(), httpsession.getAttribute("roleId").toString(), AuditTrails.getIpaddress(), AuditTrails.getFunctionName(), AuditTrails.getStatus(), AuditTrails.getComments());
                        chain.doFilter(request2, httpServletResponse);

                    } catch (Exception exception) {
                        httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "The Page you are looking is not found");
                        LOGGER.info(null, exception);
                    }
                } else {
                    //get the audit trail as blocked ......
//                    httpSession.setAttribute("error", "You Dont have permission to View this Page.");
//                    AuditTrails.setStatus("Denied Access");
                    //saveAuditTrail(httpsession.getAttribute("username").toString(), httpsession.getAttribute("roleId").toString(), AuditTrails.getIpaddress(), AuditTrails.getFunctionName(), AuditTrails.getStatus(), AuditTrails.getComments());
                    if (request2.getRequestURI().contains("%")) {
                        httpServletResponse.sendRedirect("error");
                    } else {
                        httpServletResponse.sendRedirect("error");

                    }
                }

            }
        } catch (IOException | ServletException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "The Page you are looking is not found");
//            httpServletResponse.sendRedirect("error");
//            LOGGER.error("EXCEPTION ON FILTER :{}", e);
        }

    }

    public String getLicense(String path) {
        InputStream is = null;
        String fileAsString = null;
        try {
            is = new FileInputStream(path);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }
            fileAsString = sb.toString();
            return fileAsString;
        } catch (IOException ex) {
            Logger.getLogger(MY_Controller.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MY_Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;

    }

    //LOG THE AUDIT TRAIL....
    public int saveAuditTrail(String username, String role_id, String ip_address, String function_name, String status, String comments) {
        int result = 0;
        try {
            result = jdbcTemplate.update("INSERT IGNORE INTO audit_logs(username, role_id, ip_address, function_name, status, comments) VALUES(?,?,?,?,?,?)",
                    username, role_id, ip_address, function_name, status, comments);
            return result;
        } catch (DataAccessException e) {
            LOGGER.debug(e.getMessage());
            return result;
        }
    }
}
