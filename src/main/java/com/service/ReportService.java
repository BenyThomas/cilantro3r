/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.config.SYSENV;
import com.helper.SftpUtils;
import com.helper.TxnsCBSDownloader;
import com.helper.TxnsSFTPDownloader;
import com.repository.Recon_M;
import com.repository.ReportRepo;
import com.repository.SftpRepo;
import com.zaxxer.hikari.HikariDataSource;
import static java.lang.Integer.parseInt;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 * @author MELLEJI
 */
@Component
public class ReportService {

    @Autowired
    ReportRepo reportRepo;
    @Autowired
    SftpRepo sftpRepo;
    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("dbPoolExecutor")
    ThreadPoolTaskExecutor exec;
    @Autowired
    @Qualifier("dbPoolExecutor")
    ThreadPoolTaskExecutor exec2;
    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcTemplateCbs;

    @Autowired
    @Qualifier("gwReconConnection")
    HikariDataSource gwReconDatasource;

    @Autowired
    @Qualifier("gwConnection")
    HikariDataSource gwDatasource;

    @Autowired
    SftpUtils sftpUtils;

    @Autowired
    @Qualifier("mkobaConnection")
    HikariDataSource mkobaDatasource;

    @Autowired
    @Qualifier("cbsConnection")
    HikariDataSource coreBankingDatasource;

    @Autowired
    @Qualifier("txManagerMaster")
    PlatformTransactionManager txManager;

    @Value("${spring.gw.datasource.allowed.ttype}")
    public String GATEWAY_DATASOURCES;
    @Value("${spring.gwRecon.datasource.allowed.ttype}")
    public String OLD_RECON_DATASOURCES;
    @Value("${spring.gwSftp.datasource.allowed.ttype}")
    public String SFTP_DATASOURCES;

    @Autowired
    @Qualifier("threadPoolExecutor")
    TaskExecutor taskExecutor;
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("Y-MM-dd");
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpRepo.class);
    @Autowired
    SYSENV DBENV;

    @Autowired
    Recon_M reconM;

    public void sysncCBSBalance(String balanceDate) {
        List<Map<String, Object>> accouts = reconM.getAccountConfig();
        for (Map<String, Object> accout : accouts) {
            String accountConfigId = accout.get("ID") + "";
            String accountGroupId = accout.get("account_group_id") + "";
            String aAccountNo = accout.get("a_account_no") + "";
            String bAccountNo = accout.get("b_account_no") + "";
            String accountName = accout.get("account_name") + "";
            String txn_type = accout.get("txn_type") + "";
            String ttype = accout.get("ttype") + "";
            BigDecimal openingBalance = null;
            BigDecimal closingBalance = null;
            if (aAccountNo.equals("NOACCT")) {
                openingBalance = new BigDecimal("0.0");
                closingBalance = new BigDecimal("0.0");
            } else {
                if (txn_type.equals("WALLET2BANK")) {
                    ttype = "C2B";
                } else if (txn_type.equals("BANK2WALLET")) {
                    ttype = "B2C";
                }
                openingBalance = reconM.getCBSOpeningBalance2(aAccountNo, balanceDate, ttype);
                closingBalance = reconM.getCBSClosingBalance2(aAccountNo, balanceDate, ttype);
                LOGGER.info("AccountNo: {}, openingBalance:{}, closingBalance: {}", aAccountNo, openingBalance, closingBalance);
            }
            int result = reconM.updateCBSBalanceForAccountHistory(accountGroupId, accountConfigId, aAccountNo, bAccountNo, accountName, openingBalance, closingBalance, balanceDate);
            LOGGER.info("updateCBSBalanceForAccountHistory:result=>{}", result);
        }
    }

    public void sysncMNOBalance(String balanceDate) {
        List<Map<String, Object>> accouts = reconM.getAccountConfig();
        for (Map<String, Object> accout : accouts) {
            String accountConfigId = accout.get("ID") + "";
            String accountGroupId = accout.get("account_group_id") + "";
            String aAccountNo = accout.get("a_account_no") + "";
            String bAccountNo = accout.get("b_account_no") + "";
            String accountName = accout.get("account_name") + "";
            String txn_type = accout.get("txn_type") + "";
            String ttype = accout.get("ttype") + "";
            String file_static_balance = accout.get("file_static_balance") + "";
            BigDecimal openingBalance = null;
            BigDecimal closingBalance = null;
            if (aAccountNo.equals("NOACCT")) {
                openingBalance = new BigDecimal(file_static_balance);
                closingBalance = new BigDecimal(file_static_balance);
            } else if (bAccountNo.equals("NOACCT")) {
                openingBalance = new BigDecimal("0.0");
                closingBalance = new BigDecimal("0.0");
            } else {
                if (txn_type.equals("M-PESA") || ttype.equals("MKOBA")) {
                    openingBalance = reconM.getMPESAMKOBAOpeningBalance(txn_type, ttype, balanceDate);
                    closingBalance = reconM.getMPESAMKOBAClosingBalance(txn_type, ttype, balanceDate);
                } else if (txn_type.equals("TIGO") && ttype.equals("C2B")) {
                    openingBalance = new BigDecimal("0.0");
                    closingBalance = reconM.getTIGOClosingBalance(txn_type, ttype, balanceDate);
                } else {
                    openingBalance = reconM.getMNOOpeningBalance(txn_type, ttype, balanceDate);
                    closingBalance = reconM.getMNOClosingBalance(txn_type, ttype, balanceDate);
                }
            }
            int result = reconM.updateMNOBalanceForAccountHistory(accountGroupId, accountConfigId, aAccountNo, bAccountNo, accountName, openingBalance, closingBalance, balanceDate);
            LOGGER.info("updateCBSBalanceForAccountHistory:result=>{}", result);
        }
    }

    //@Scheduled(cron = "0 45 7-13 * * *")
//disabled for reason
    @Scheduled(fixedDelay = 60000L) //disabled for a while
    public void downloadTxnsFromDBSources() {
        if (DBENV.ACTIVE_PROFILE.equals("testing....")) {
            System.out.println("====================DOWNLOAD FROM SFTP SOURCES [old recon]===============");
            for (Map<String, Object> map : getSFTPSettings()) {
                //System.out.println(OLD_RECON_DATASOURCES+"=vs="+map.get("ttype"));

                //HAPA FOR TESTING IS REMOVED FOR A WHILE
                if (OLD_RECON_DATASOURCES.contains(map.get("ttype").toString())) {
                    if (OLD_RECON_DATASOURCES.contains("KOBA")) {
                        // System.out.println("TXN TYPE:" + map.get("ttype").toString());
                    } //
                    TxnsSFTPDownloader downloadSFTPHelper = new TxnsSFTPDownloader();
                    downloadSFTPHelper.setSftpDir(map.get("sftp_dir").toString());
                    downloadSFTPHelper.setLineSplit(map.get("lineSplit").toString());
                    downloadSFTPHelper.setFileNameStartWith(map.get("fileNameStartWith").toString());
                    downloadSFTPHelper.setFileDestinationDir(map.get("fileDestinationDir").toString());
                    downloadSFTPHelper.setFileDateFormat(map.get("fileDateFormat").toString());
                    downloadSFTPHelper.setRecordPosition(map.get("recordPosition").toString());
                    downloadSFTPHelper.setSftpHost(map.get("sftphost").toString());
                    downloadSFTPHelper.setSftpport(Integer.parseInt(map.get("sftpport").toString()));
                    downloadSFTPHelper.setSftpusername(map.get("sftpusername").toString());
                    downloadSFTPHelper.setSftppassword(map.get("sftppassword").toString());
                    downloadSFTPHelper.setTxnType(map.get("code").toString());
                    downloadSFTPHelper.setTtype(map.get("ttype").toString());
                    downloadSFTPHelper.setIsW2B(Boolean.parseBoolean(map.get("isW2B").toString()));
                    downloadSFTPHelper.setIsB2W(Boolean.parseBoolean(map.get("isB2W").toString()));
                    downloadSFTPHelper.setDburl(map.get("dburl").toString());
                    downloadSFTPHelper.setDbusername(map.get("dbusername").toString());
                    downloadSFTPHelper.setDbpassword(map.get("dbpassword").toString());
                    downloadSFTPHelper.setDbDriverClassName(map.get("dbdriverClassName").toString());
                    downloadSFTPHelper.setCbsQuery(map.get("queryD").toString());
                    downloadSFTPHelper.setCbsAcct(map.get("cbs_account").toString());
                    downloadSFTPHelper.setDbaccturl(map.get("dbaccturl").toString());
                    downloadSFTPHelper.setDbacctusername(map.get("dbacctusername").toString());
                    downloadSFTPHelper.setDbacctpassword(map.get("dbacctpassword").toString());
                    downloadSFTPHelper.setDbacctDriverClassName(map.get("dbacctdriverClassName").toString());
                    downloadSFTPHelper.setThirdPartyQuery(map.get("queryAcct").toString());
                    downloadSFTPHelper.setLastPTID(map.get("lastPtid").toString());
                    downloadSFTPHelper.setIsAllwed(Boolean.parseBoolean(map.get("isAllowed").toString()));
                    downloadSFTPHelper.setGwLastPTID(map.get("gwLastPTID").toString());
                    downloadSFTPHelper.setJdbcTemplate(jdbcTemplate);
                    exec2.execute(downloadSFTPHelper);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOGGER.info("InterruptedException: ", ex.getMessage());
                }
                } //FOR A WHILE ILI KUTEST

            }
        }
    }

    @Scheduled(cron = "0 6 7-8 * * *")
//      @Scheduled(fixedDelay = 60000L)
    public void downloadSFTP() {
        if (DBENV.ACTIVE_PROFILE.equals("prod")) {
            for (Map<String, Object> map : getSFTPSettings()) {
                String txnType = map.get("code").toString().trim();
                if (txnType.contains("M-PESA")) {
//                System.out.println("SFTP DOWNLOADING MPESA BALANCES======?>");
                    String chargeDescription = map.get("charge_description").toString().trim();
                    String fileDateFormat = map.get("fileDateFormat").toString();
                    String txnShortCode = map.get("shortcode").toString().trim();
                    String ttype = map.get("ttype").toString().trim();
                    String lineSplit = map.get("lineSplit").toString().trim();
                    String recordPosition = map.get("recordPosition").toString().trim();
                    String fileNameStartWith = map.get("fileNameStartWith").toString().trim();
                    String SFTPUSER = map.get("sftpusername").toString().trim();
                    String SFTPHOST = map.get("sftphost").toString().trim();
                    int SFTPPORT = parseInt(map.get("sftpport").toString().trim());
                    String SFTPPASS = map.get("sftppassword").toString().trim();
                    String PATHSEPARATOR = "/";
                    String SFTPWORKINGDIR = map.get("sftp_dir").toString().trim();
                    String destinationPath = map.get("fileDestinationDir").toString().trim();
                    taskExecutor.execute(() -> {
                        try {
                            String message = sftpUtils.getSFTPTxns(chargeDescription, txnShortCode, ttype, txnType, lineSplit, recordPosition, fileNameStartWith, SFTPUSER, SFTPHOST, SFTPPORT, SFTPPASS, PATHSEPARATOR, SFTPWORKINGDIR, destinationPath, fileDateFormat);
                            System.out.println("COMPLETED :" + message);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("ERROR ON RUNNING sftp: " + e.getMessage());
                        }
                    });
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ex) {
                        System.out.println("ERROR ON RUNNING sftp: " + ex.getMessage());
                    }
                }
            }
        }
    }

    // @Scheduled(cron = "0 0 1-13 * * *")
    @Scheduled(fixedDelay = 60000 * 60L * 2L)
//  @Scheduled(fixedDelay = 60000 )
    public void downloadTxnsFromCBSSources() {
        if (DBENV.ACTIVE_PROFILE.equals("prod")) {
            for (Map<String, Object> map : getSFTPSettings()) {
//                System.out.println("====================DOWNLOAD FROM CORE BANKING===============");
                //List<String> reconS
                LOGGER.info("The running active thread ... {} and maxsize ... {}",exec.getActiveCount(),exec.getMaxPoolSize());
                if(map.get("status").equals("A")) {
                    LOGGER.info("Starting downloading data from cbs ...{} and .. {}",map.get("ttype").toString(),map.get("code").toString());
                    TxnsCBSDownloader downloadHelper = new TxnsCBSDownloader();
                    downloadHelper.setTxnType(map.get("code").toString());
                    downloadHelper.setTtype(map.get("ttype").toString());
                    downloadHelper.setIsW2B(Boolean.parseBoolean(map.get("isW2B").toString()));
                    downloadHelper.setIsB2W(Boolean.parseBoolean(map.get("isB2W").toString()));
                    downloadHelper.setDburl(map.get("dburl").toString());
                    downloadHelper.setDbusername(map.get("dbusername").toString());
                    downloadHelper.setDbpassword(map.get("dbpassword").toString());
                    downloadHelper.setDbDriverClassName(map.get("dbdriverClassName").toString());
                    downloadHelper.setCbsQuery(map.get("queryD").toString());
                    downloadHelper.setCbsAcct(map.get("cbs_account").toString());
                    downloadHelper.setDbaccturl(map.get("dbaccturl").toString());
                    downloadHelper.setDbacctusername(map.get("dbacctusername").toString());
                    downloadHelper.setDbacctpassword(map.get("dbacctpassword").toString());
                    downloadHelper.setDbacctDriverClassName(map.get("dbacctdriverClassName").toString());
                    downloadHelper.setThirdPartyQuery(map.get("queryAcct").toString());
                    downloadHelper.setLastPTID(map.get("lastPtid").toString());
                    downloadHelper.setIsAllwed(Boolean.parseBoolean(map.get("isAllowed").toString()));
                    downloadHelper.setJdbcTemplate(jdbcTemplate);
                    exec.execute(downloadHelper);
                }
            }
        }
    }
//get sft settings

    public List<Map<String, Object>> getSFTPSettings() {
        return jdbcTemplate.queryForList("select * from txns_types order by ttype desc");
//        return jdbcTemplate.queryForList("select * from txns_types where name = 'TIPS'"); //for tips transactions
    }

    public Connection genericDB3Connection() {
        String dbUrl = "jdbc:mysql://172.20.1.20:3306/recon?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String dbUsername = "report_user";
        String dbPassword = "r3p0rt_pa$$2o2o";
        String dbDriverClassName = "com.mysql.cj.jdbc.Driver";
        Connection connection = null;
        try {
            String userName = dbUsername;
            String passwrd = dbPassword;
            String connectionUrl = dbUrl;
            Class.forName(dbDriverClassName);
            connection = DriverManager.getConnection(connectionUrl, userName, passwrd);
            System.out.println("CONNECTION TO GW: " + connection);

            return connection;
        } catch (ClassNotFoundException | SQLException ex) {
            System.out.println("connection to GW: " + ex.getMessage());
            ex.printStackTrace();
            connection = null;
        }
        return connection;
    }

    //download UTILITIES TRANSACTIONS FROM GATEWAY
    public String downloadUtilitiesTxnsGw(String doCodes, String txn_type, String ttype) {
        return null;

    }

}
