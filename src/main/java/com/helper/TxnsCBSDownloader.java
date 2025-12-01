/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.repository.SftpRepo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.transaction.Transactional;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author melleji.mollel
 */
public class TxnsCBSDownloader implements Runnable {

    static ChannelSftp channelSftp = null;
    static Session session = null;
    static Channel channel = null;
    public String txnType;
    public String ttype;
    public String dburl;
    public String dbusername;
    public String dbpassword;
    public String dbDriverClassName;
    public String cbsQuery;
    public boolean isW2B;
    public boolean isB2W;
    public String acctQuery;
    public String dbaccturl;
    public String dbacctusername;
    public String dbacctpassword;
    public String dbacctDriverClassName;
    public String txndate;
    public String thirdPartyQuery;
    public boolean isAllwed;
    public String cbsAcct;
    public String lastPTID;
    public JdbcTemplate jdbcTemplate;
    //sftp variable
    public String lineSplit;
    public String recordPosition;
    public String sftpDir;
    public String fileDestinationDir;
    public String fileNameStartWith;
    public String fileDateFormat;
    public String txnStatus;
    public String sftpHost;
    public String sftpusername;
    public String sftppassword;
    public int sftpport;
    public String pathSeparator;
    public Connection gwdbConn;
    public Connection mkobadbConn;
    public Connection cbsdbConn;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SftpRepo.class);

    @Autowired
    private Environment env;

    @Override
    public void run() {
        try {
            Connection con = null;
            PreparedStatement preparedStatement = null;
             LOGGER.info("Starting downloading cbs data .. "+getTxnType()+" - "+getCbsQuery().replace("?", "'{}'"), this.cbsAcct,"2021-01-01",this.lastPTID);

            if (getTxnType().equals("LUKUEX") || getTxnType().equals("GEPGEX") || getTxnType().equals("GEPGOLDEX")) {
                con = genericDBConnection(getDburl(), getDbusername(), getDbpassword(), getDbDriverClassName());
                if (con==null) {
                    LOGGER.info("failed to establish connection .. {}",getDburl());
                    return;
                }
                preparedStatement = con.prepareStatement(getCbsQuery());
                preparedStatement.setString(1, this.cbsAcct);
                 LOGGER.info(getCbsQuery().replace("?", "'{}'"), this.cbsAcct);
            } else {

                con = genericDBConnection(getDburl(), getDbusername(), getDbpassword(), getDbDriverClassName());
                if (con==null) {
                    LOGGER.info("failed to establish connection .. {}",getDburl());
                    return;
                }
                preparedStatement = con.prepareStatement(getCbsQuery());
                preparedStatement.setString(1, this.cbsAcct);
                preparedStatement.setString(2, "2021-01-01");
                preparedStatement.setString(3, this.lastPTID);
            }
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                if (getTxnType().equals("LUKUEX")) {
                    String sourceAccount = "-1";
                    String receiptNo = "-1";
                    String docode = "-1";
                    String docodeDesc = "-1";
                    String destinationAcct = "-1";
                    String terminal = "-1";
                    String currency = "TZS";
                    String txnStatus = "Success";
                    //int insertResult = syncDownloadCoreTransactions(rs.getString("lastPTID"), rs.getString("txnid"), getTxnType(), getTtype(), rs.getString("txndate"), sourceAccount, destinationAcct, rs.getString("amount"), rs.getString("description"), destinationAcct, currency, txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), rs.getString("txnid"), "1132", "LUKU");
                } else if (getTxnType().equals("GEPGEX")) {
                    String sourceAccount = "-1";
                    String receiptNo = "-1";
                    String docode = "-1";
                    String docodeDesc = "-1";
                    String destinationAcct = "-1";
                    String terminal = "-1";
                    String currency = "TZS";
                    String txnStatus = "Success";
                    int insertResult = syncDownloadCoreTransactions(rs.getString("lastPTID"), rs.getString("txnid"), getTxnType(), getTtype(), rs.getString("txndate"), sourceAccount, destinationAcct, rs.getString("amount"), rs.getString("description"), destinationAcct, currency, txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), rs.getString("txnid"), "-1", "GEPG");
                } else if (getTxnType().equals("GEPGOLDEX")) {
                    String sourceAccount = "-1";
                    String receiptNo = "-1";
                    String docode = "-1";
                    String docodeDesc = "-1";
                    String destinationAcct = "-1";
                    String terminal = "-1";
                    String currency = "TZS";
                    String txnStatus = "Success";
                    int insertResult = syncDownloadCoreTransactions(rs.getString("lastPTID"), rs.getString("txnid"), getTxnType(), getTtype(), rs.getString("txndate"), sourceAccount, destinationAcct, rs.getString("amount"), rs.getString("description"), destinationAcct, currency, txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), rs.getString("txnid"), "-1", "GEPG");
                } else {
                    //download LUKU suspended transaction
                    String sourceAccount = "-1";
                    String receiptNo = "-1";
                    String docode = "-1";
                    String txndate = rs.getString("txndate");
                    String docodeDesc = "-1";
                    String destinationAcct = "-1";
                    String terminal = "-1";
                    String currency = "TZS";
                    String txnStatus = "Success";

                    String brinjalUrl = "jdbc:mysql://192.168.220.9:16000/brinjaldb?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
                    String brinjalUsername = "reconuser";
                    String brinjalPassword = "R3contugo";
                    String brinjalDriverClassName = "com.mysql.cj.jdbc.Driver";

                    if (isB2W) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("A".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("R".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }

                        if (getTxnType().equalsIgnoreCase("LUKU") && ((rs.getString("txnid").startsWith("T") || rs.getString("txnid").startsWith("MOB")))) {

                            Connection brinjalConn = genericDB2Connection(brinjalUrl, brinjalUsername, brinjalPassword, brinjalDriverClassName);
                            LOGGER.info("The properties used here are hardcoded");
                            String query = "select create_date txndate,reference as receiptNo,sourceAcct,destinationAcct,-1 as docode from transactions t where t.reference =?";

                            Map<String, String> result = getAcct(query, rs.getString("txnid"), brinjalConn);
                            destinationAcct = result.get("destinationAcct"); //getDestinationAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                            sourceAccount = result.get("sourceAcct");
                            receiptNo = result.get("receiptNo");
                            docode = result.get("docode");
                            txndate = result.get("txndate");
                            brinjalConn.close();
                        } else {
                            //get destination account
                            Connection cond2 = genericDB2Connection(getDbaccturl(), getDbacctusername(), getDbacctpassword(), getDbacctDriverClassName());

                            Map<String, String> result = getAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                            destinationAcct = result.get("destinationAcct"); //getDestinationAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                            sourceAccount = result.get("sourceAcct");
                            receiptNo = result.get("receiptNo");
                            docode = result.get("docode");
                            txndate = result.get("txndate") != null ? result.get("txndate") : txndate;
                            cond2.close();
                        }

                        LOGGER.info("txnid:{} txntype:{} txndate:{} ttype:{}",rs.getString("txnid"),txnType,txndate,ttype);

                        if (receiptNo != null && receiptNo.equals("-1")) {
                            receiptNo = rs.getString("txnid");
                            //get receipt from narration
                            if (rs.getString("txnid").length() == 20) {
                                String descArry[] = rs.getString("description") == null ? "NA".split(":") : rs.getString("description").split(":");
                                if (descArry.length == 5) {
                                    receiptNo = descArry[4];
                                }
                            }
                            sourceAccount = rs.getString("accountNo");
                            destinationAcct = rs.getString("description");
                        }
                        terminal = destinationAcct;
                    }

                    if (isW2B) {
                        if ("DR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("CR".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }
                        if ("A".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Success";
                        }
                        if ("R".equalsIgnoreCase(rs.getString("status"))) {
                            txnStatus = "Reversed";
                        }

                        if (getTxnType().equalsIgnoreCase("LUKU") && ((rs.getString("txnid").startsWith("T") || rs.getString("txnid").startsWith("MOB")))) {

                            Connection brinjalConn = genericDB2Connection(brinjalUrl, brinjalUsername, brinjalPassword, brinjalDriverClassName);
                            String query = "select create_date txndate,reference as receiptNo,sourceAcct,destinationAcct,-1 as docode from transactions t where t.reference =?";

                            Map<String, String> result = getAcct(query, rs.getString("txnid"), brinjalConn);
                            destinationAcct = result.get("destinationAcct"); //getDestinationAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                            sourceAccount = result.get("sourceAcct");
                            receiptNo = result.get("receiptNo");
                            docode = result.get("docode");
                            txndate = result.get("txndate");
                            brinjalConn.close();
                        } else {
                            Connection cond2 = genericDB2Connection(getDbaccturl(), getDbacctusername(), getDbacctpassword(), getDbacctDriverClassName());

                            Map<String, String> result = getAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                            destinationAcct = result.get("destinationAcct"); //getDestinationAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                            sourceAccount = result.get("sourceAcct");
                            receiptNo = result.get("receiptNo");
                            docode = result.get("docode");
                            txndate = result.get("txndate") != null ? result.get("txndate") : txndate;
                            cond2.close();
                            if (receiptNo != null && receiptNo.equals("-1")) {
                                receiptNo = rs.getString("txnid");
                                //get receipt from narration
                                if (rs.getString("txnid").length() == 20) {
                                    String descArry[] = rs.getString("description") == null ? "NA".split(":") : rs.getString("description").split(":");
                                    if (descArry.length == 5) {
                                        receiptNo = descArry[4];
                                    }
                                }
                                destinationAcct = rs.getString("accountNo");
                                sourceAccount = rs.getString("description");
                            }
                        }
                        terminal = sourceAccount;
                    }
                    if (!(isW2B) && !(isB2W)) {
                        terminal = sourceAccount = rs.getString("accountNo");
                        destinationAcct = rs.getString("contra_account");
                    }
                    currency = rs.getString("currency");
                    if ("852".equalsIgnoreCase(rs.getString("currency"))) {
                        currency = "TZS";
                    }
                    if ("841".equalsIgnoreCase(rs.getString("currency"))) {
                        currency = "USD";
                    }
                    int insertResult = downloadCoreTransactions(rs.getString("lastPTID"), rs.getString("txnid"), getTxnType(), getTtype(), txndate, sourceAccount, destinationAcct, rs.getString("amount"), rs.getString("description"), terminal, currency, txnStatus, rs.getString("postBalance"), rs.getString("contra_account"), rs.getString("status"), receiptNo, docode, docodeDesc);
                }
            }

            con.close();
        } catch (SQLException ex) {
            LOGGER.info("", ex);
        }

    }

    public String getDburl() {
        return dburl;
    }

    public void setDburl(String dburl) {
        this.dburl = dburl;
    }

    public String getLastPTID() {
        return lastPTID;
    }

    public void setLastPTID(String lastPTID) {
        this.lastPTID = lastPTID;
    }

    public String getDbusername() {
        return dbusername;
    }

    public void setDbusername(String dbusername) {
        this.dbusername = dbusername;
    }

    public String getDbpassword() {
        return dbpassword;
    }

    public void setDbpassword(String dbpassword) {
        this.dbpassword = dbpassword;
    }

    public String getDbDriverClassName() {
        return dbDriverClassName;
    }

    public Connection getGwdbConn() {
        return gwdbConn;
    }

    public void setGwdbConn(Connection gwdbConn) {
        this.gwdbConn = gwdbConn;
    }

    public Connection getMkobadbConn() {
        return mkobadbConn;
    }

    public void setMkobadbConn(Connection mkobadbConn) {
        this.mkobadbConn = mkobadbConn;
    }

    public void setDbDriverClassName(String dbDriverClassName) {
        this.dbDriverClassName = dbDriverClassName;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getTxndate() {
        return txndate;
    }

    public void setTxndate(String txndate) {
        this.txndate = txndate;
    }

    public String getTtype() {
        return ttype;
    }

    public void setTtype(String ttype) {
        this.ttype = ttype;
    }

    public String getDbaccturl() {
        return dbaccturl;
    }

    public void setDbaccturl(String dbaccturl) {
        this.dbaccturl = dbaccturl;
    }

    public String getDbacctusername() {
        return dbacctusername;
    }

    public static ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    public static void setChannelSftp(ChannelSftp channelSftp) {
        TxnsCBSDownloader.channelSftp = channelSftp;
    }

    public static Session getSession() {
        return session;
    }

    public static void setSession(Session session) {
        TxnsCBSDownloader.session = session;
    }

    public static Channel getChannel() {
        return channel;
    }

    public static void setChannel(Channel channel) {
        TxnsCBSDownloader.channel = channel;
    }

    public String getLineSplit() {
        return lineSplit;
    }

    public void setLineSplit(String lineSplit) {
        this.lineSplit = lineSplit;
    }

    public String getRecordPosition() {
        return recordPosition;
    }

    public void setRecordPosition(String recordPosition) {
        this.recordPosition = recordPosition;
    }

    public String getSftpDir() {
        return sftpDir;
    }

    public void setSftpDir(String sftpDir) {
        this.sftpDir = sftpDir;
    }

    public String getFileDestinationDir() {
        return fileDestinationDir;
    }

    public void setFileDestinationDir(String fileDestinationDir) {
        this.fileDestinationDir = fileDestinationDir;
    }

    public String getFileNameStartWith() {
        return fileNameStartWith;
    }

    public void setFileNameStartWith(String fileNameStartWith) {
        this.fileNameStartWith = fileNameStartWith;
    }

    public String getFileDateFormat() {
        return fileDateFormat;
    }

    public void setFileDateFormat(String fileDateFormat) {
        this.fileDateFormat = fileDateFormat;
    }

    public String getTxnStatus() {
        return txnStatus;
    }

    public void setTxnStatus(String txnStatus) {
        this.txnStatus = txnStatus;
    }

    public String getSftpHost() {
        return sftpHost;
    }

    public void setSftpHost(String sftpHost) {
        this.sftpHost = sftpHost;
    }

    public String getSftpusername() {
        return sftpusername;
    }

    public void setSftpusername(String sftpusername) {
        this.sftpusername = sftpusername;
    }

    public String getSftppassword() {
        return sftppassword;
    }

    public void setSftppassword(String sftppassword) {
        this.sftppassword = sftppassword;
    }

    public int getSftpport() {
        return sftpport;
    }

    public void setSftpport(int sftpport) {
        this.sftpport = sftpport;
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator;
    }

    public void setDbacctusername(String dbacctusername) {
        this.dbacctusername = dbacctusername;
    }

    public String getDbacctpassword() {
        return dbacctpassword;
    }

    public void setDbacctpassword(String dbacctpassword) {
        this.dbacctpassword = dbacctpassword;
    }

    public String getDbacctDriverClassName() {
        return dbacctDriverClassName;
    }

    public String getCbsAcct() {
        return cbsAcct;
    }

    public boolean isIsW2B() {
        return isW2B;
    }

    public void setIsW2B(boolean isW2B) {
        this.isW2B = isW2B;
    }

    public boolean isIsB2W() {
        return isB2W;
    }

    public void setIsB2W(boolean isB2W) {
        this.isB2W = isB2W;
    }

    public void setCbsAcct(String cbsAcct) {
        this.cbsAcct = cbsAcct;
    }

    public void setDbacctDriverClassName(String dbacctDriverClassName) {
        this.dbacctDriverClassName = dbacctDriverClassName;
    }

    public String getCbsQuery() {
        return cbsQuery;
    }

    public void setCbsQuery(String cbsQuery) {
        this.cbsQuery = cbsQuery;
    }

    public String getThirdPartyQuery() {
        return thirdPartyQuery;
    }

    public void setThirdPartyQuery(String thirdPartyQuery) {
        this.thirdPartyQuery = thirdPartyQuery;
    }

    public boolean isIsAllwed() {
        return isAllwed;
    }

    public void setIsAllwed(boolean isAllwed) {
        this.isAllwed = isAllwed;
    }

    public Connection getCbsdbConn() {
        return cbsdbConn;
    }

    public void setCbsdbConn(Connection cbsdbConn) {
        this.cbsdbConn = cbsdbConn;
    }

    public Connection genericDBConnection(String dbUrl, String dbUsername, String dbPassword, String dbDriverClassName) {
        Connection connection;
        try {
            String userName = dbUsername;
            String passwrd = dbPassword;
            String connectionUrl = dbUrl;
            Class.forName(dbDriverClassName);
            Connection conn = DriverManager.getConnection(connectionUrl, userName, passwrd);
            return conn;

        } catch (ClassNotFoundException | SQLException ex) {
            System.out.println("CORE BANKING EXCEPTION: " + ex.getMessage());
            connection = null;
        }
        return connection;
    }

    public Connection genericDB2Connection(String dbUrl, String dbUsername, String dbPassword, String dbDriverClassName) {
        Connection connection;
        try {
            String userName = dbUsername;
            String passwrd = dbPassword;
            String connectionUrl = dbUrl;
            if (!dbDriverClassName.equalsIgnoreCase("mariadb")) {
                Class.forName(dbDriverClassName);
            }
            Connection conn = DriverManager.getConnection(connectionUrl, userName, passwrd);
            return conn;

        } catch (ClassNotFoundException | SQLException ex) {
            System.out.println("CORE BANKING EXCEPTION: " + ex.getMessage());
            connection = null;
        }
        return connection;
    }

    public String getAcctQuery() {
        return acctQuery;
    }

    public void setAcctQuery(String acctQuery) {
        this.acctQuery = acctQuery;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Connection dbDestinationAcctConnection(String dbUrl, String dbUsername, String dbPassword, String dbDriverClassName) {
        Connection connection;
        try {
            String userName = dbUsername;
            String passwrd = dbPassword;
            String connectionUrl = dbUrl;
            Class.forName(dbDriverClassName);
            Connection conn = DriverManager.getConnection(connectionUrl, userName, passwrd);
            return conn;

        } catch (ClassNotFoundException | SQLException ex) {
            connection = null;
        }
        return connection;
    }
//downloading Core-banking transactions

    @Transactional
    public Integer downloadCoreTransactions(String lastPT, String txnid, String txn_type, String ttype, String txndate, String sourceAccount, String destinationAcct, String amount, String description, String terminal, String currency, String txnStatus, String postBalance, String contraAcct, String dr_cr_ind, String receiptNo, String docode, String docodeDesc) {
        Integer result = 0;
        try {
            if (ttype.equals("MKOBA")) {
                docodeDesc = mkobaDocodeDesc(docode);
            }
            result = jdbcTemplate.update("UPDATE txns_types set lastPtid=? where ttype=? and code=?",
                    lastPT, ttype, txn_type);
            result = jdbcTemplate.update("insert  into cbstransactiosn(thirdparty_reference,txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind,docode,docode_desc) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?,docode=?,docode_desc=?",
                    receiptNo, txnid, txn_type, ttype, txndate, sourceAccount, destinationAcct, amount, description, terminal, currency, txnStatus, postBalance, contraAcct, dr_cr_ind, docode, docodeDesc, txnStatus, postBalance, dr_cr_ind, docode);

        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        return result;
    }

    public Integer syncDownloadCoreTransactions(String lastPT, String txnid, String txn_type, String ttype, String txndate, String sourceAccount, String destinationAcct, String amount, String description, String terminal, String currency, String txnStatus, String postBalance, String contraAcct, String dr_cr_ind, String receiptNo, String docode, String docodeDesc) {
        Integer result = 0;
        try {

            result = jdbcTemplate.update("UPDATE txns_types set lastPtid=? where ttype=? and code=?",
                    lastPT, ttype, txn_type);
            txnid = "" + txnid.trim();
            if (dr_cr_ind.equals("CR")) {
                result = jdbcTemplate.update("insert  into suspe_cbstransactiosn(thirdparty_reference,txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind,docode,docode_desc) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?,docode=?,docode_desc=?",
                        receiptNo, txnid, txn_type, ttype, txndate, sourceAccount, destinationAcct, amount, description, terminal, currency, txnStatus, postBalance, contraAcct, dr_cr_ind, docode, docodeDesc, txnStatus, postBalance, dr_cr_ind, docode);
            } else {
                result = jdbcTemplate.update("delete from suspe_cbstransactiosn where ttype=? and txn_type=? and  txnid=?", ttype, txn_type, txnid);
            }
//                LOGGER.info("delete from suspe_cbstransactiosn where ttype=? and txn_type=? and  txnid=?".replace("?", "'{}'"), ttype, txn_type, txnid);
//                LOGGER.info("insert  into suspe_cbstransactiosn(thirdparty_reference,txnid,txn_type,ttype,txndate,sourceaccount,destinationaccount,amount,description,terminal,currency,txn_status,post_balance,contraaccount,dr_cr_ind,docode,docode_desc) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update txn_status=?,post_balance=?,docode=?,docode_desc=?".replace("?", "'{}'"),
//                        receiptNo, txnid, txn_type, ttype, txndate, sourceAccount, destinationAcct, amount, description, terminal, currency, txnStatus, postBalance, contraAcct, dr_cr_ind, docode, docodeDesc, txnStatus, postBalance, dr_cr_ind, docode);
//
        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        return result;
    }

    public String mkobaDocodeDesc(String docode) {
        String result = "-1";
        switch (docode) {
            case "101":
                result = "Family Group Opening";
                break;
            case "102":
                result = "Vicoba Group Creation";
                break;
            case "111":
                result = "Member Contribution";
                break;
            case "112":
                result = "Member Balance";
                break;
            case "115":
                result = "Family Group Transfer";
                break;
            case "121":
                result = "Buy shares";
                break;
            case "122":
                result = "Loan Payment";
                break;
            case "123":
                result = "Social Fund";
                break;
            case "124":
                result = "Penalty Payment";
                break;
            case "222":
                result = "Wakala contribution deposit";
                break;
            case "333":
                result = "Wakala share purchase";
                break;
            case "444":
                result = "Wakala loan repayment";
                break;
            case "555":
                result = "Wakala punishment payment";
                break;
            case "666":
                result = "Wakala socialfund deposit";
                break;
            case "777":
                result = "From other network deposit";
                break;
            case "999":
                result = "Bonus";
                break;
            case "1111":
                result = "contribute for other member";
                break;
            case "1122":
                result = "Group  Balance";
                break;
            case "1142":
                result = "Opening an account on cbs";
                break;
            case "1143":
                result = "Opening an account on cbs";
                break;
            case "1144":
                result = "Opening an account on cbs";
                break;
            case "1211":
                result = "Buy share other network";
                break;
            case "1222":
                result = "Loan Payment other member";
                break;
            case "1233":
                result = "Social Fund other member";
                break;
            case "1244":
                result = "Other Member Penalty Payment";
                break;
            case "1251":
                result = "Vicoba Member balance";
                break;
            case "1252":
                result = "Vicoba Group balance";
                break;
            case "1253":
                result = "Taarifa zaidi za kifedha";
                break;
            case "1254":
                result = "other member balance";
                break;
            case "1255":
                result = "other group balance";
                break;
            case "1275":
                result = "add bank account";
                break;
            case "12612":
                result = "Social fund Withdraw";
                break;
            case "12613":
                result = "Dividend";
                break;
            case "12614":
                result = "Other loan";
                break;
            case "12716":
                result = "Set Penalt";
                break;
            case "12721":
                result = "initiate share price change";
                break;
            case "12722":
                result = "loan interest change";
                break;
            case "12723":
                result = "loan gurantor  change";
                break;
            case "12724":
                result = "loan factor change";
                break;
            case "12725":
                result = "approval mode settings";
                break;
            case "12932":
                result = "transfer meting collect balnc";
                break;
            case "122001":
            case "122002":
            case "122003":
            case "122004":
            case "126114":
                result = "Emergency Loan";
                break;
            case "126111":
                result = "Development Loan";
                break;
            case "126112":
                result = "Education Loan";
                break;
            case "126113":
                result = "Medical Loan";
                break;
            default:
                result = "Undefined";
                break;
        }
        return result;
    }

    //downloading transactions from a file (SFTP)
    public Integer downloadSFTFilesTxns(String txn_type, String ttype, String txnid, String txndate, String sourceAccount, String receiptNo, String amount, String charge, String description, String currency, String mnoTxns_status, String txdestinationaccount, String acct_no, String status, String post_balance, String previous_balance, String file_name) {
        Integer result = 0;
        try {
            result = jdbcTemplate.update("INSERT INTO thirdpartytxns(txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    txn_type, ttype, txnid, txndate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name);

        } catch (DataAccessException e) {
            e.printStackTrace();
            result = 0;
        }
        return result;
    }

    //get destination/source account from gateway
    public Map<String, String> getAcct(String query, String txnid, Connection con) {

        String sourceAccount = "-1";
        String docode = "-1";
        String destinationAcct = "-1";
        String thirdpartyReference = "-1";
        String txndate = null;
        Map<String, String> result = new HashMap<>();
        ;
        try {
            //String qry = "select * from tp_transaction where txid=?";
            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1, txnid);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                result.put("sourceAcct", rs.getString("sourceAcct"));
                result.put("destinationAcct", rs.getString("destinationAcct"));
                result.put("receiptNo", rs.getString("receiptNo"));
                result.put("docode", rs.getString("docode"));
                result.put("txndate", rs.getString("txndate"));
                //System.out.println("QUERY:" + query);
                // System.out.println("FROM GATEWAY: [sourceAcct= " + sourceAccount + " destinationAcct=" + rs.getString("destinationAcct") + " ReceiptNo: " + rs.getString("receiptNo") + "]");
            } else {
                result.put("sourceAcct", sourceAccount);
                result.put("destinationAcct", destinationAcct);
                result.put("receiptNo", thirdpartyReference);
                result.put("docode", docode);
                result.put("txndate", txndate);
            }
        } catch (SQLException ex) {
            result.put("sourceAcct", sourceAccount);
            result.put("destinationAcct", destinationAcct);
            result.put("receiptNo", thirdpartyReference);
            result.put("docode", docode);
            result.put("txndate", txndate);
            LOGGER.info("EXCEPTION ON GETTING SOURCE AND DESTINATION ACCT FROM GATEWAY: {} ", ex);
            System.out.println("reference: " + query);
        }
        return result;
    }

    public String getDestinationAcct(String query, String txnid, Connection con) {
        String destinationAccount = "-1";
        try {
            //String qry = "select * from tp_transaction where txid=?";
            PreparedStatement preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1, txnid);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                destinationAccount = rs.getString("destinationAcct");
            }
        } catch (SQLException ex) {
            Logger.getLogger(TxnsCBSDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return destinationAccount;
    }

    public void getFiles(String T_TYPE, String sourceDir, String destDir) {
        try {
            JSch jsch = new JSch();
            System.out.println("TYPE:" + T_TYPE);
            System.out.println("SFTPUSERNAME: " + sftpusername);
            System.out.println("SFTPHOST: " + sftpHost);
            System.out.println("SFTPPORT: " + sftpport);
            System.out.println("SFTPPASSWORD: " + sftppassword);
            session = jsch.getSession(sftpusername, sftpHost, sftpport);
            session.setPassword(sftppassword);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect(60000);
            channelSftp = (ChannelSftp) channel;
            System.out.println("LPWD: " + sourceDir);
            channelSftp.cd(sourceDir);
            channelSftp.ls(sourceDir);
            System.out.println("Transaction Type: " + T_TYPE + " Current Working directly: " + "Local working Directory: " + fileDestinationDir);
            recursiveFolderDownload(T_TYPE, sourceDir, destDir);
        } catch (Exception ex) {
            Logger.getLogger(TxnsCBSDownloader.class.getName()).log(Level.SEVERE, (String) null, (Throwable) ex);
        }
    }

    private void recursiveFolderDownload(String txn_type, String sourcePath, String destinationPath) throws SftpException, IOException {
        //System.out.println("ls:" + channelSftp.ls(sourcePath));
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sftpDir);
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir() && item.getFilename().startsWith(fileNameStartWith)) {
//                System.out.println("ITEM FILE NAME:" + item.getFilename());
                System.out.println("FROM SERVER FILE NAME: " + item.getFilename() + " []from DB FILE NAME:" + fileNameStartWith + "TXN_TYPES: " + txn_type + " TTYPE:" + ttype + " lineSplit:" + lineSplit);
                if (item.getFilename().startsWith(fileNameStartWith)) {
                    if (!(new File(fileDestinationDir + item.getFilename())).exists() || item.getAttrs().getMTime() > Long.valueOf((new File(fileDestinationDir + item.getFilename())).lastModified() / 1000L).intValue()) {
                        new File(fileDestinationDir + item.getFilename());
                        InputStream stream = channelSftp.get(sftpDir + "/" + item.getFilename());
                        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                        Date dt = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        // System.out.println("HEREEEEEEEEEEEE:");
//                    System.out.println("FILE NAME:" + item.getFilename() + " EXPECTED FILE NAME:" + fileNameStartWith + " TXN_TYPE:" + txn_type + " TTYPE: " + ttype);
                        int i = 0;
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] transactions;
                            if (ttype.contains("HALOTEL")) {
                                transactions = line.split(Pattern.quote(lineSplit));

                            } else {
                                transactions = line.split(lineSplit);
                            }
                            try {
                                if (i != 0) {
                                    String[] recordIndexNo = recordPosition.split(",");

//                                if (item.getFilename().startsWith(fileNameStartWith)) {
                                    System.out.println("QUERY: " + getThirdPartyQuery());
                                    System.out.println("TXNID :" + transactions[Integer.parseInt(recordIndexNo[0])].trim());

                                    String sourceAccount = "-1";
                                    String txdestinationaccount = "-1";
                                    String acct_no = "-1";
                                    String txnid = transactions[Integer.parseInt(recordIndexNo[0])].trim();
                                    String txdate = formatDate(transactions[Integer.parseInt(recordIndexNo[1])].trim(), fileDateFormat, "yyyy-MM-dd HH:mm:ss");
                                    String receiptNo = transactions[Integer.parseInt(recordIndexNo[2])].trim();
                                    String amount = transactions[Integer.parseInt(recordIndexNo[3])].trim();
                                    String charge = transactions[Integer.parseInt(recordIndexNo[4])].trim();
                                    String description = transactions[Integer.parseInt(recordIndexNo[5])].trim();
                                    String currency = transactions[Integer.parseInt(recordIndexNo[6])].trim();
                                    String mnoTxns_status = transactions[Integer.parseInt(recordIndexNo[7])].trim();
                                    String status = transactions[Integer.parseInt(recordIndexNo[8])].trim();
                                    sourceAccount = transactions[Integer.parseInt(recordIndexNo[9])].trim();
                                    acct_no = txdestinationaccount = transactions[Integer.parseInt(recordIndexNo[10])].trim();
                                    String post_balance = transactions[Integer.parseInt(recordIndexNo[11])].trim();
                                    String previous_balance = transactions[Integer.parseInt(recordIndexNo[12])].trim();
                                    String file_name = item.getFilename();

                                    if (isB2W) {
                                        System.out.println("isB2C");
                                        currency = "TSH";
                                        charge = "0.00";
                                        //get destination account
                                        Connection cond2 = genericDBConnection(getDbaccturl(), getDbacctusername(), getDbacctpassword(), getDbacctDriverClassName());
                                        //sourceAccount = getAcct(getThirdPartyQuery(), transactions[Integer.parseInt(recordIndexNo[0])].trim(), cond2);
                                        Map<String, String> result = getAcct(getThirdPartyQuery(), transactions[Integer.parseInt(recordIndexNo[0])].trim(), cond2);
                                        // destinationAcct = result.get("destinationAcct"); //getDestinationAcct(getThirdPartyQuery(), rs.getString("txnid"), cond2);
                                        sourceAccount = result.get("sourceAcct");
                                        //receiptNo = result.get("receiptNo");
                                        cond2.close();
                                        acct_no = sourceAccount;

                                    }
                                    if (isW2B) {
                                        System.out.println("isC2B");
                                        currency = "TSH";
                                        charge = "0.00";
                                        //get source account
//                                        Connection cond2 = genericDBConnection(getDbaccturl(), getDbacctusername(), getDbacctpassword(), getDbacctDriverClassName());
//                                        acct_no = txdestinationaccount = getDestinationAcct(getThirdPartyQuery(), transactions[Integer.parseInt(recordIndexNo[0])].trim(), cond2);
//                                        cond2.close();
//                                        System.out.println("Destination Account:" + txdestinationaccount);
                                    }
                                    if (!isW2B && !isB2W) {
                                        sourceAccount = transactions[Integer.parseInt(recordIndexNo[9])].trim();
                                        txdestinationaccount = transactions[Integer.parseInt(recordIndexNo[10])].trim();
                                        acct_no = txdestinationaccount;
                                    }
                                    System.out.println("txnid:" + Integer.parseInt(recordIndexNo[0]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[0])].trim());
                                    System.out.println("txdate:" + Integer.parseInt(recordIndexNo[1]) + "=====>" + formatDate(transactions[Integer.parseInt(recordIndexNo[1])].trim(), fileDateFormat, "yyyy-MM-dd HH:mm:ss"));
                                    System.out.println("receiptNo:" + Integer.parseInt(recordIndexNo[2]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[2])].trim());
                                    System.out.println("amount:" + Integer.parseInt(recordIndexNo[3]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[3])].trim());
                                    System.out.println("charge:" + Integer.parseInt(recordIndexNo[4]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[4])].trim());
                                    System.out.println("description:" + Integer.parseInt(recordIndexNo[5]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[5])].trim());
                                    System.out.println("currency:" + Integer.parseInt(recordIndexNo[6]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[6])].trim());
                                    System.out.println("mnoTxns_status:" + Integer.parseInt(recordIndexNo[7]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[7])].trim());
                                    System.out.println("status:" + Integer.parseInt(recordIndexNo[8]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[8])].trim());
                                    System.out.println("txdestinationaccount:" + Integer.parseInt(recordIndexNo[10]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[10])].trim());
                                    System.out.println("post_balance:" + Integer.parseInt(recordIndexNo[11]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[11])].trim());
                                    System.out.println("previous_balance:" + Integer.parseInt(recordIndexNo[12]) + "=====>" + transactions[Integer.parseInt(recordIndexNo[12])].trim());

                                    if (status.equalsIgnoreCase("PST") || status.equalsIgnoreCase("PST1") || status.equalsIgnoreCase("PST2") || status.equalsIgnoreCase("PST3") || status.equalsIgnoreCase("PST4") || status.equalsIgnoreCase("PST5") || status.equalsIgnoreCase("PST6") || status.equalsIgnoreCase("PST7") || status.equalsIgnoreCase("PST8") || status.equalsIgnoreCase("PST9")) {
                                        status = "Success";
                                    }
                                    if (status.equalsIgnoreCase("DCL") || status.equalsIgnoreCase("SDL") || status.equalsIgnoreCase("SDL1") || status.equalsIgnoreCase("SDL2") || status.equalsIgnoreCase("SDL3") || status.equalsIgnoreCase("SDL4") || status.equalsIgnoreCase("SDL5") || status.equalsIgnoreCase("SDL6") || status.equalsIgnoreCase("SDL7")) {
                                        status = "Failed";
                                    }
                                    if (status.equalsIgnoreCase("SBM") || status.equalsIgnoreCase("STO") || status.equalsIgnoreCase("HLD") || status.equalsIgnoreCase("SDT")) {
                                        status = "Pending";
                                    }
                                    if (status.equalsIgnoreCase("RFD")) {
                                        status = "Refund~ Money returned to TPB Disbursement account";
                                    }
                                    if (status.equalsIgnoreCase("TS")) {
                                        status = "Success";
                                    }
                                    if (status.equalsIgnoreCase("TF")) {
                                        status = "Failed";
                                    }
                                    if (status.equalsIgnoreCase("Accepted")) {
                                        status = "success";
                                    }
                                    if (status.equalsIgnoreCase("Completed")) {
                                        status = "success";
                                    }
                                    System.out.println("FILE NAME: " + file_name + "TXNID: " + txnid + " TXNDATE: " + txdate + " receiptNo: " + receiptNo + " Amount: " + amount + " Charge: " + charge + " DESCRIPTION: " + description + " CURRENCY: " + currency + " mnoTxns_status:" + mnoTxns_status + " status:" + status + " sourceAccount:" + sourceAccount + " txdestinationaccount:" + txdestinationaccount);

                                    int sftTxns = downloadSFTFilesTxns(txn_type, ttype, txnid, txdate, sourceAccount, receiptNo, amount, charge, description, currency, mnoTxns_status, txdestinationaccount, acct_no, status, post_balance, previous_balance, file_name);
                                    System.out.println("INSERTING SFTP FILE RESULTS: " + sftTxns);
                                    // }
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.println("Do not process this line of code" + e);
                                continue;
                            } catch (SQLException ex) {
                                Logger.getLogger(TxnsCBSDownloader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            i++;
                        }
//
//                    System.out.println("DESTINATION PATH: " + destinationPath);
//                    if (txn_type.equalsIgnoreCase("TIGO_C2B") || txn_type.equalsIgnoreCase("TIGO_B2C")) {
//                        channelSftp.get(sourcePath + pathSeparator + item.getFilename(), "/home/mnoReconFiles/TIGO/" + item.getFilename());
//                        channelSftp.rename(sourcePath + pathSeparator + item.getFilename(), "/home/tpb/processed/" + item.getFilename());
//                    } else if (txn_type.equalsIgnoreCase("AIRTEL_C2B") || txn_type.equalsIgnoreCase("AIRTEL_B2C")) {
//                        System.out.println("Source path on Rename: " + sourcePath + pathSeparator + item.getFilename());
//                        System.out.println("Destination path on Rename: /home/mnoReconFiles/" + txn_type + pathSeparator + item.getFilename());
//                        channelSftp.rename(sourcePath + pathSeparator + item.getFilename(), "/home/mnoReconFiles/AIRTEL/" + item.getFilename());
//                    } else if (txn_type.equalsIgnoreCase("HALOTEL_C2B") || txn_type.equalsIgnoreCase("HALOTEL_B2C")) {
//                        channelSftp.rename(sourcePath + pathSeparator + item.getFilename(), "/home/mnoReconFiles/HALOTEL/" + item.getFilename());
//                    } else if (txn_type.equalsIgnoreCase("UTILITY_LUKU")) {
//                        channelSftp.get(sourcePath + pathSeparator + item.getFilename(), "/home/mnoReconFiles/LUKU/" + item.getFilename());
//                        channelSftp.rename(sourcePath + pathSeparator + item.getFilename(), "/001002/outbox/" + item.getFilename());
//                    } else {
//                        channelSftp.rename(sourcePath + pathSeparator + item.getFilename(), "/home/mnoReconFiles/" + txn_type + pathSeparator + item.getFilename());
//                    }
                    }
                    channelSftp.get(sourcePath + item.getFilename(), destinationPath + item.getFilename());
                    channelSftp.rm(sourcePath + item.getFilename());
                } else {
                    System.out.println("NOT A FILE FOR: " + txn_type + " TTYPE:" + ttype);
                }
                try {
//                    System.out.println("SOURCE PATH: " + sourcePath + pathSeparator + item.getFilename());
//                    System.out.println("DESTINATION PATH: " + destinationPath + pathSeparator + item.getFilename());
                    recursiveFolderDownload(txn_type, sourcePath, destinationPath);
                } catch (Exception ex) {
                    System.out.println("WARNING: " + ex.getMessage());
                }

            }
        }
    }

    //format date
    public static String formatDate(String date, String initDateFormat, String endDateFormat) {
        SimpleDateFormat formatter = new SimpleDateFormat(endDateFormat);
        try {
            Date initDate = (new SimpleDateFormat(initDateFormat)).parse(date);
            return formatter.format(initDate);

        } catch (ParseException ex) {
            Logger.getLogger(TxnsCBSDownloader.class
                    .getName()).log(Level.SEVERE, (String) null, ex);
            return null;
        }
    }
}
