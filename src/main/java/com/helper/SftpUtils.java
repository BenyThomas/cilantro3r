package com.helper;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.repository.SftpRepo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SftpUtils {

    static ChannelSftp channelSftp = null;
    static Session session = null;
    static Channel channel = null;
//    static String PATHSEPARATOR;//= "/";
//    static String SFTPHOST;//= "172.20.1.20";
//    static int SFTPPORT;//= 22;
//    static String SFTPUSER;//= "root";
//    static String SFTPPASS;//= "tpb.1234";
//    static String SFTPWORKINGDIR;//= "/home/";
//    static String LOCALDIRECTORY;//= "/home/dist/sftp";

//    public boolean isW2B;
//    public boolean isB2W;
//    public String chargeDescription;
//    public String txnShortCode;
//    public String ttype;
//    public String txnType;
//    public String lineSplit;
//    public String recordPosition;
//    public String fileNameStartWith;
    @Autowired
    SftpRepo sftpRepo;

    public String getSFTPTxns(String chargeDescription, String txnShortCode, String ttype, String txnType, String lineSplit, String recordPosition, String fileNameStartWith, String SFTPUSER, String SFTPHOST, int SFTPPORT, String SFTPPASS, String PATHSEPARATOR, String SFTPWORKINGDIR, String destinationPath, String fileDateFormat) throws IOException, SQLException, JAXBException, InterruptedException {

        getFiles(chargeDescription, txnShortCode, ttype, txnType, lineSplit, recordPosition, fileNameStartWith, SFTPUSER, SFTPHOST, SFTPPORT, SFTPPASS, PATHSEPARATOR, SFTPWORKINGDIR, destinationPath, fileDateFormat);
        channelSftp.disconnect();
        channel.disconnect();
        session.disconnect();
        return txnType + " SFTP TXNS DOWNLOADED============>";
    }

    public void getFiles(String chargeDescription, String txnShortCode, String ttype, String txnType, String lineSplit, String recordPosition, String fileNameStartWith, String SFTPUSER, String SFTPHOST, int SFTPPORT, String SFTPPASS, String PATHSEPARATOR, String SFTPWORKINGDIR, String destinationPath, String fileDateFormat) throws IOException, JAXBException, InterruptedException {
        try {
            JSch jsch = new JSch();
            System.out.println("TXN_TYPE:" + txnType);
            System.out.println("TTYPE:" + ttype);
            session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
            session.setPassword(SFTPPASS);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect(60000);
            channelSftp = (ChannelSftp) channel;
            System.out.println("LPWD:" + SFTPWORKINGDIR.trim());
            channelSftp.cd(SFTPWORKINGDIR);
            channelSftp.ls(SFTPWORKINGDIR);
//            System.out.println("Transaction Type: " + txnType + " Current Working directly: " + SFTPWORKINGDIR + "Local working Directory: " + destinationPath);
            recursiveFolderDownload(chargeDescription, txnShortCode, ttype, txnType, lineSplit, recordPosition, fileNameStartWith, PATHSEPARATOR, SFTPWORKINGDIR, destinationPath, fileDateFormat);
        } catch (JSchException ex) {
            Logger.getLogger(SftpUtils.class.getName()).log(Level.SEVERE, null, ex);

        } catch (SftpException ex) {
            Logger.getLogger(SftpUtils.class.getName()).log(Level.SEVERE, null, ex);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(SftpUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SftpUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void recursiveFolderDownload(String chargeDescription, String txnShortCode, String ttype, String txnType, String lineSplit, String recordPosition, String fileNameStartWith, String PATHSEPARATOR, String sourcePath, String destinationPath, String fileDateFormat) throws SftpException, FileNotFoundException, ClassNotFoundException, IOException, JAXBException, InterruptedException {
        System.out.println("ls:" + channelSftp.ls(sourcePath));
        System.out.println("fileNameStartWith: " + fileNameStartWith);
        System.out.println("txnType: " + txnType);
        System.out.println("ttype: " + ttype);
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sourcePath);
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir()) {
                if (!(new File(destinationPath + item.getFilename())).exists() || item.getAttrs().getMTime() > Long.valueOf((new File(destinationPath + item.getFilename())).lastModified() / 1000L).intValue()) {
                    new File(destinationPath);
                    if(item.getFilename().startsWith("TPB_B2C_")||item.getFilename().startsWith("TPB_C2B")){
                    if(sourcePath.contains(item.getFilename())){
                        sourcePath=sourcePath;
                    }else{
                      sourcePath=sourcePath+"/"+item.getFilename();  
                    }
                    System.out.println("FILE TO BE READ: "+sourcePath);
                    InputStream stream = channelSftp.get(sourcePath);
                    BufferedReader br = new BufferedReader(new InputStreamReader(stream));

                    Date dt = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                    if (item.getFilename().contains("B2C")) {
                        ttype="B2C";
                    }
                     if (item.getFilename().contains("C2B")) {
                        ttype="C2B";
                    }
                    int i = 0;
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] transactions;
                        transactions = line.split(lineSplit);

                        try {
                            String sourceAccount = "-1";
                            String destinationaccount = "-1";
                            if (i != 0) {
//                                switch (txnType) {
//                                    case "M-PESA":
                                        String[] recordIndexNo = recordPosition.split(lineSplit);
                                        String txnid = transactions[Integer.parseInt(recordIndexNo[0])].trim();
                                        String txdate = transactions[Integer.parseInt(recordIndexNo[1])].trim();//formatDate(transactions[Integer.parseInt(recordIndexNo[1])].trim().replace(".0",""), fileDateFormat, "yyyy-MM-dd HH:mm:ss");
                                        String receiptNo = transactions[Integer.parseInt(recordIndexNo[2])].trim();
                                        String amount = transactions[Integer.parseInt(recordIndexNo[3])].trim();
                                        String charge = transactions[Integer.parseInt(recordIndexNo[4])].trim();
                                        String description = transactions[Integer.parseInt(recordIndexNo[5])].trim();
                                        String currency = transactions[Integer.parseInt(recordIndexNo[6])].trim();
                                        String mnoTxns_status = transactions[Integer.parseInt(recordIndexNo[7])].trim();
                                        String status = transactions[Integer.parseInt(recordIndexNo[8])].trim();
                                        sourceAccount = transactions[Integer.parseInt(recordIndexNo[9])].trim();
                                        destinationaccount = transactions[Integer.parseInt(recordIndexNo[10])].trim();
                                        String post_balance = transactions[Integer.parseInt(recordIndexNo[11])].trim();
                                        String previous_balance = transactions[Integer.parseInt(recordIndexNo[12])].trim();
                                        String shortcode = transactions[Integer.parseInt(recordIndexNo[13])].trim();
                                        String file_name = item.getFilename();
                                        //check for mkoba and gateway transactions
                                        if (description.contains(chargeDescription) && txnShortCode.contains(shortcode)) {
                                            System.out.println("700500 GATEWAY CHARGES TRANSACTIONS========[" + "SHORT CODE: " + shortcode + "  MSISDN: " + sourceAccount + " DESTINATION ACCOUNT:" + destinationaccount + "RECEIPT NO: " + receiptNo + " BALANCE: " + post_balance + " AMOUNT: " + amount + " DESCRIPTION: " + description + " ");
                                            //call separate method to insert the charge if record doesnot exist
                                            if (shortcode.equalsIgnoreCase("700500")) {
                                                //gateway charges
                                                sftpRepo.saveMNOSftpTransactionsChages("M-PESA", ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
//                                            System.out.println("700500 GATEWAY CHARGES========[" + "SHORT CODE: " + shortcode + "  MSISDN: " + sourceAccount + " DESTINATION ACCOUNT:" + destinationaccount + "RECEIPT NO: " + receiptNo + " BALANCE: " + post_balance + " AMOUNT: " + amount + " DESCRIPTION: " + description + " ");

                                            } else if (shortcode.equalsIgnoreCase("245151")) {
                                                //mkoba charges
                                                if (fileNameStartWith.contains("B2C")) {
                                                    sftpRepo.saveMNOSftpTransactionsChages("MKOBA2WALLET", "MKOBA", txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);

                                                } else {
                                                    sftpRepo.saveMNOSftpTransactionsChages("WALLET2MKOBA", "MKOBA", txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
                                                }
                                            } else {
                                                //not MPESA transactions
                                                sftpRepo.saveMNOSftpTransactionsChages(txnType, ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
                                            }
                                        } else {
                                            charge = "0";
                                            //insert normal transaction here 
                                            if (shortcode.contains("700500")) {
                                                //get all mpesa transactions that are gateway transactions
//                                                System.out.println("700500 GATEWAY TRANSACTIONS========[" + "TXNDATE: " + txdate + "  MSISDN: " + sourceAccount + " DESTINATION ACCOUNT:" + destinationaccount + "RECEIPT NO: " + receiptNo + " BALANCE: " + post_balance + " AMOUNT: " + amount + " DESCRIPTION: " + description + " ");
                                                sftpRepo.saveMNOSftpTransactions("M-PESA", ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
                                            } else if (shortcode.contains("245151")) {
                                                if (fileNameStartWith.contains("B2C")) {
//                                                    System.out.println("245151 MKOBA2WAALET TRANSACTIONS========[" + "TXNDATE: " + txdate + "  MSISDN: " + sourceAccount + " DESTINATION ACCOUNT:" + destinationaccount + "RECEIPT NO: " + receiptNo + " BALANCE: " + post_balance + " AMOUNT: " + amount + " DESCRIPTION: " + description + " ");

                                                    sftpRepo.saveMNOSftpTransactionsChages("MKOBA2WALLET", "MKOBA", txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
                                                } else {
//                                                    System.out.println("245151 WALLET2MKOBA TRANSACTIONS========[" + "TXNDATE: " + txdate + "  MSISDN: " + sourceAccount + " DESTINATION ACCOUNT:" + destinationaccount + "RECEIPT NO: " + receiptNo + " BALANCE: " + post_balance + " AMOUNT: " + amount + " DESCRIPTION: " + description + " ");
                                                    sftpRepo.saveMNOSftpTransactions("WALLET2MKOBA", "MKOBA", txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
                                                }
                                            } else {
                                                //not MPESA transactions
                                                sftpRepo.saveMNOSftpTransactions(txnType, ttype, txnid, txdate, receiptNo, amount, charge, description, currency, mnoTxns_status, status, sourceAccount, destinationaccount, post_balance, previous_balance, shortcode, file_name);
                                            }
                                        }

                                //}
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("Do not process this line of code" + e);
                            continue;
                        }
                        i++;
                    }
                    }
                    //System.out.println("DESTINATION PATH: " + destinationPath);
                    channelSftp.get(sourcePath, destinationPath);
                    channelSftp.rm(sourcePath);
                }
                try {
//                    System.out.println("SOURCE PATH: " + sourcePath + "/" + item.getFilename().trim());
//                    System.out.println("DESTINATION PATH: " + destinationPath + item.getFilename().trim());
                    recursiveFolderDownload(chargeDescription, txnShortCode, ttype, txnType, lineSplit, recordPosition, fileNameStartWith, PATHSEPARATOR, sourcePath+"/"+item.getFilename().trim(), destinationPath.trim()+item.getFilename().trim(), fileDateFormat);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("WARNING: " + ex.getMessage() + "file: " + destinationPath);
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
            Logger.getLogger(TxnsSFTPDownloader.class
                    .getName()).log(Level.SEVERE, (String) null, ex);
            return null;
        }
    }

}
