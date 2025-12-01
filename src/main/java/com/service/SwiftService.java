/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.DTO.IBANK.PaymentReq;
import com.DTO.Teller.FundTransferReq;
import com.DTO.Teller.RTGSTransferForm;
import com.DTO.Teller.RTGSTransferFormFinance;
import com.DTO.swift.other.FundFINTransferReq;
import com.helper.DateUtil;
import com.helper.StringUtils;
import com.models.Transfers;
import com.prowidesoftware.swift.model.MessageIOType;
import com.prowidesoftware.swift.model.SwiftBlock2;
import com.prowidesoftware.swift.model.SwiftBlock3;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.prowidesoftware.swift.model.mt.mt2xx.MT202;
import com.prowidesoftware.swift.model.mt.mt9xx.MT940;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author melleji.mollel
 */
public class SwiftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftService.class);

    public static String createMT103(FundTransferReq req) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(req.getTranDate());
        BigDecimal amt = req.getAmount();
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT103 m = new MT103();
            m.setSender(req.getSenderBIC());
            m.setReceiver(req.getReceiverBIC());
            SwiftBlock3 block3 = new SwiftBlock3();
            if (req.getMsgType().equals("LOCAL")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));
            block3.append(new Field108(req.getReference()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(req.getReference()));
            //Field 23B: Bank Operation Code
            //CRED This message contains a credit transfer where there is no SWIFT Service Level involved.
            m.addField(new Field23B(req.getBankOperationalCode()));

            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);
            //50K:ORDERING CUSTOMER - ACCOUNT-NAME AND ADDRESS
            Field50K f50K = new Field50K().setAccount(req.getSourceAcct())
                    .setNameAndAddress(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine1(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine2(StringUtils.getMTLineSplitterLimitedTo70Chars(req.getSenderAddress()));
            //.setNameAndAddressLine2(req.getSenderAddress());
            m.addField(f50K);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(req.getSenderBIC());
            m.addField(f52A);
            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(req.getSenderCorrespondent());
            m.addField(f53A);

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
            Field59 f59 = new Field59();
            f59.setAccount(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getDestinationAcct()))
                    .setNameAndAddressLine1(StringUtils.getMTLineSplitterLimitedTo70Chars(req.getBeneficiaryName()));
            m.addField(f59);
            //70: REMITANNCE INFORMATION
            m.addField(new Field70("/ROC/TRANSFER")); //35 charachers
            //71A: DETAILS OF CHARGES
//            m.addField(new Field71A(req.getDetailsOfCharge()));
            //72: DETAILS OF CHARGES
//            Field72 field72 = new Field72();
//            field72.setNarrative(StringUtils.getMTLineSplitter(req.getDescription()));
//            field72.setNarrativeLine1(description);
//            field72.setNarrativeLine2(description2);
//            field72.setNarrativeLine3(description3);
            m.addField(new Field71A("SHA"));
            m.addField(new Field72("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", "")));
//            m.addField(field72);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT103(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        //check initiated date time if its systemCUT-OFF
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT103 m = new MT103();
            //BLOCK 2
            SwiftBlock2 block2 = new SwiftBlock2() {
            };
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));


//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            //Field 23B: Bank Operation Code
            //CRED This message contains a credit transfer where there is no SWIFT Service Level involved.
            m.addField(new Field23B("CRED"));

            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

            //50K:ORDERING CUSTOMER - ACCOUNT-NAME AND ADDRESS
            Field50K f50K = new Field50K().setAccount(req.getSenderAccount())
                    .setNameAndAddress(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine1(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine2(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderPhone()))
                    .setNameAndAddressLine3(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderAddress()));
            m.addField(f50K);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);
            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //56: INTERMEDIARY BANK
            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
                Field56A f56A = new Field56A();
                f56A.setBIC(req.getIntermediaryBank());
                m.addField(f56A);
            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }
            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
            Field59 f59 = new Field59();
            f59.setAccount(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getBeneficiaryAccount()));
            f59.setNameAndAddressLine1(StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
            m.addField(f59);
            //70: REMITANNCE INFORMATION
            String description = req.getDescription();
            if (!description.contains("/ROC/")) {
                description = description.replaceAll("[^a-zA-Z0-9 ]", " ");
            }
            description = StringUtils.getMTLineSplitter(req.getDescription());

            Field70 field70 = new Field70();
            field70.setNarrative(description);
            m.addField(field70);
//            field70.setNarrativeLine2(description);
            //71A: DETAILS OF CHARGES
            m.addField(new Field71A("SHA"));
            //72: DETAILS OF CHARGES

//            Field72 field72 = new Field72();
//            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()));
//            m.addField(field72);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT103ForTissVPN(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        //check initiated date time if its systemCUT-OFF
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT103 m = new MT103();
            //BLOCK 2
            SwiftBlock2 block2 = new SwiftBlock2() {
            };
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));
            block3.append(new Field108(reference));


//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            //Field 23B: Bank Operation Code
            //CRED This message contains a credit transfer where there is no SWIFT Service Level involved.
            m.addField(new Field23B("CRED"));

            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

            //50K:ORDERING CUSTOMER - ACCOUNT-NAME AND ADDRESS
            Field50K f50K = new Field50K().setAccount(req.getSenderAccount())
                    .setNameAndAddress(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine1(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine2(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderPhone()))
                    .setNameAndAddressLine3(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderAddress()));
            m.addField(f50K);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);
            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //56: INTERMEDIARY BANK
            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
                Field56A f56A = new Field56A();
                f56A.setBIC(req.getIntermediaryBank());
                m.addField(f56A);
            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }
            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
            Field59 f59 = new Field59();
            f59.setAccount(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getBeneficiaryAccount()));
            f59.setNameAndAddressLine1(StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
            m.addField(f59);
            //70: REMITANNCE INFORMATION
            String description = req.getDescription();
            if (!description.contains("/ROC/")) {
                description = description.replaceAll("[^a-zA-Z0-9 ]", " ");
            }
            description = StringUtils.getMTLineSplitter(req.getDescription());

            Field70 field70 = new Field70();
            field70.setNarrative(description);
            m.addField(field70);
//            field70.setNarrativeLine2(description);
            //71A: DETAILS OF CHARGES
            m.addField(new Field71A("SHA"));
            //72: DETAILS OF CHARGES

//            Field72 field72 = new Field72();
//            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()));
//            m.addField(field72);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createMT103FromOnlineReq(PaymentReq req) {

        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Calendar.getInstance().getTime());
        BigDecimal amt = req.getAmount();
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT103 m = new MT103();
            //BLOCK 2
            SwiftBlock2 block2 = new SwiftBlock2() {
            };
            m.setSender(req.getSenderBic());//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (req.getType().equals("001")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(req.getReference()));
            //Field 23B: Bank Operation Code
            //CRED This message contains a credit transfer where there is no SWIFT Service Level involved.
            m.addField(new Field23B("CRED"));

            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

            //50K:ORDERING CUSTOMER - ACCOUNT-NAME AND ADDRESS
            Field50K f50K = new Field50K().setAccount(req.getSenderAccount())
                    .setNameAndAddress(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine1(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine2(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderPhone()))
                    .setNameAndAddressLine3(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderAddress()));
            m.addField(f50K);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(req.getSenderBic());
            m.addField(f52A);
            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(req.getCorrespondentBic());
            m.addField(f53A);
            //56: INTERMEDIARY BANK
            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
                Field56A f56A = new Field56A();
                f56A.setBIC(req.getIntermediaryBank());
                m.addField(f56A);
            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (req.getType().equalsIgnoreCase("004")) {
                m.addField(new Field57A(req.getBeneficiaryBIC()));
                m.setReceiver(req.getCorrespondentBic());
            }
            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
            Field59 f59 = new Field59();
            f59.setAccount(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getBeneficiaryAccount()));
            f59.setNameAndAddressLine1(StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
            m.addField(f59);
            //70: REMITANNCE INFORMATION
            String description = req.getDescription();
            if (!description.contains("/ROC/")) {
                description = description.replaceAll("[^a-zA-Z0-9 ]", " ");
            }
            description = StringUtils.getMTLineSplitter(req.getDescription());

            Field70 field70 = new Field70();
            field70.setNarrative(description);
            m.addField(field70);
//            field70.setNarrativeLine2(description);
            //71A: DETAILS OF CHARGES
            m.addField(new Field71A("SHA"));
            //72: DETAILS OF CHARGES

//            Field72 field72 = new Field72();
//            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()));
//            m.addField(field72);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT202(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT202 m = new MT202();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            m.addField(new Field21(req.getRelatedReference()));
            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

//            //52A: Ordering Institution (Payer's Bank)
//            String senderName = req.getSenderName();
//            Field52D f52D = new Field52D()
//                    .setComponent2(req.getSenderAccount() + "\n" + senderBic + "\n" + StringUtils.getMTLineSplitter(req.getSenderName()));
////                    .setComponent3(senderBic)
////                    .setComponent1(req.getSenderName());
//            m.addField(f52D);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);

            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //            //56: INTERMEDIARY BANK
            //            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
            //                Field56A f56A = new Field56A();
            //                f56A.setBIC(req.getIntermediaryBank());
            //                m.addField(f56A);
            //            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
//            Field58D f58D = new Field58D()
//                    .setComponent2(req.getBeneficiaryAccount() + "\n" + StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
//            m.addField(f58D);
            Field58A f58A = new Field58A()
                    .setComponent2(req.getBeneficiaryBIC().split("==")[0]);
            m.addField(f58A);

            //72: REMITANNCE INFORMATION
            Field72 field72 = new Field72();
            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", ""));
            m.addField(field72);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT202Mirathi(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT202 m = new MT202();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            m.addField(new Field21(req.getRelatedReference()));
            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

//            //52A: Ordering Institution (Payer's Bank)
//            String senderName = req.getSenderName();
//            Field52D f52D = new Field52D()
//                    .setComponent2(req.getSenderAccount() + "\n" + senderBic + "\n" + StringUtils.getMTLineSplitter(req.getSenderName()));
////                    .setComponent3(senderBic)
////                    .setComponent1(req.getSenderName());
//            m.addField(f52D);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);

            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //            //56: INTERMEDIARY BANK
            //            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
            //                Field56A f56A = new Field56A();
            //                f56A.setBIC(req.getIntermediaryBank());
            //                m.addField(f56A);
            //            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
//            Field58D f58D = new Field58D()
//                    .setComponent2(req.getBeneficiaryAccount() + "\n" + StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
//            m.addField(f58D);

            Field58A f58A = new Field58A()
                    .setComponent2(req.getBeneficiaryBIC().split("==")[0]);
            m.addField(f58A);

            //72: REMITANNCE INFORMATION
            Field72 field72 = new Field72();
            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", ""));
            m.addField(field72);
            content = m.message();
            content = content.replaceAll("58A:/", "58A:");
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT202Atm(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT202 m = new MT202();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            m.addField(new Field21(req.getRelatedReference()));
            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

//            //52A: Ordering Institution (Payer's Bank)
//            String senderName = req.getSenderName();
//            Field52D f52D = new Field52D()
//                    .setComponent2(req.getSenderAccount() + "\n" + senderBic + "\n" + StringUtils.getMTLineSplitter(req.getSenderName()));
////                    .setComponent3(senderBic)
////                    .setComponent1(req.getSenderName());
//            m.addField(f52D);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);

            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //            //56: INTERMEDIARY BANK
            //            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
            //                Field56A f56A = new Field56A();
            //                f56A.setBIC(req.getIntermediaryBank());
            //                m.addField(f56A);
            //            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
//            Field58D f58D = new Field58D()
//                    .setComponent2(req.getBeneficiaryAccount() + "\n" + StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
//            m.addField(f58D);
            Field58A f58A = new Field58A()
                    .setComponent2(req.getBeneficiaryBIC().split("==")[0]);
            m.addField(f58A);

            //72: REMITANNCE INFORMATION
            Field72 field72 = new Field72();
            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", ""));
            m.addField(field72);
            content = m.message();
            content = content.replaceAll("58A:/", "58A:");
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT202CollectionWithAccount(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT202 m = new MT202();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            m.addField(new Field21(req.getRelatedReference()));
            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

//            //52A: Ordering Institution (Payer's Bank)
//            String senderName = req.getSenderName();
//            Field52D f52D = new Field52D()
//                    .setComponent2(req.getSenderAccount() + "\n" + senderBic + "\n" + StringUtils.getMTLineSplitter(req.getSenderName()));
////                    .setComponent3(senderBic)
////                    .setComponent1(req.getSenderName());
//            m.addField(f52D);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);

            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //            //56: INTERMEDIARY BANK
            //            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
            //                Field56A f56A = new Field56A();
            //                f56A.setBIC(req.getIntermediaryBank());
            //                m.addField(f56A);
            //            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
//            Field58D f58D = new Field58D()
//                    .setComponent2(req.getBeneficiaryAccount() + "\n" + StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
//            m.addField(f58D);
            Field58A f58A = new Field58A()
                    .setAccount(req.getBeneficiaryAccount())
                    .setComponent2(req.getBeneficiaryBIC().split("==")[0]);
            m.addField(f58A);

            //72: REMITANNCE INFORMATION
            Field72 field72 = new Field72();
            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", ""));
            m.addField(field72);
            content = m.message();
            content = content.replaceAll("58A:/", "58A:");

        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }
    public static String createTellerMT202CollectionWithNoAccount(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT202 m = new MT202();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            m.addField(new Field21(req.getRelatedReference()));
            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

//            //52A: Ordering Institution (Payer's Bank)
//            String senderName = req.getSenderName();
//            Field52D f52D = new Field52D()
//                    .setComponent2(req.getSenderAccount() + "\n" + senderBic + "\n" + StringUtils.getMTLineSplitter(req.getSenderName()));
////                    .setComponent3(senderBic)
////                    .setComponent1(req.getSenderName());
//            m.addField(f52D);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);

            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //            //56: INTERMEDIARY BANK
            //            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
            //                Field56A f56A = new Field56A();
            //                f56A.setBIC(req.getIntermediaryBank());
            //                m.addField(f56A);
            //            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
//            Field58D f58D = new Field58D()
//                    .setComponent2(req.getBeneficiaryAccount() + "\n" + StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
//            m.addField(f58D);
            Field58A f58A = new Field58A()
                    .setComponent2(req.getBeneficiaryBIC().split("==")[0]);
            m.addField(f58A);

            //72: REMITANNCE INFORMATION
            Field72 field72 = new Field72();
            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", ""));
            m.addField(field72);
            content = m.message();
            content = content.replaceAll("58A:/", "58A:");

        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createTellerMT202CashWithdrawal(RTGSTransferForm req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT202 m = new MT202();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            m.addField(new Field21(req.getRelatedReference()));
            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

//            //52A: Ordering Institution (Payer's Bank)
//            String senderName = req.getSenderName();
//            Field52D f52D = new Field52D()
//                    .setComponent2(req.getSenderAccount() + "\n" + senderBic + "\n" + StringUtils.getMTLineSplitter(req.getSenderName()));
////                    .setComponent3(senderBic)
////                    .setComponent1(req.getSenderName());
//            m.addField(f52D);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);

            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //            //56: INTERMEDIARY BANK
            //            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
            //                Field56A f56A = new Field56A();
            //                f56A.setBIC(req.getIntermediaryBank());
            //                m.addField(f56A);
            //            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }

            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
//            Field58D f58D = new Field58D()
//                    .setComponent2(req.getBeneficiaryAccount() + "\n" + StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
//            m.addField(f58D);
            Field58A f58A = new Field58A()
                    .setComponent2(req.getBeneficiaryBIC().split("==")[0]);
            m.addField(f58A);

            //72: REMITANNCE INFORMATION
            Field72 field72 = new Field72();
            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()).replaceAll("[^a-zA-Z0-9 ]", ""));
            m.addField(field72);
            content = m.message();
            content = content.replaceAll("58A:/", "58A:");

        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createFinanceMT103(RTGSTransferFormFinance req, Date tranDate, String senderBic, String msgType, String reference, String correspondentBank) {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tranDate);
        BigDecimal amt = new BigDecimal(req.getAmount());
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT103 m = new MT103();
            m.setSender(senderBic);//req.getBeneficiaryBIC());
            m.setReceiver(req.getBeneficiaryBIC().split("==")[0]);
            SwiftBlock3 block3 = new SwiftBlock3();
            //SETTING BLOCK 3 FOR LOCAL & EAST AFRICA TRANSACTIONS
            if (msgType.equals("LOCAL") || msgType.equals("EAPS")) {
                block3.append(new Field103("TIS"));
            }
            block3.append(new Field121(UUID.randomUUID().toString()));

//            block3.append(new Field103(req.getMsgType()));
            m.getSwiftMessage().addBlock(block3);
            //Transaction reference
            m.addField(new Field20(reference));
            //Field 23B: Bank Operation Code
            //CRED This message contains a credit transfer where there is no SWIFT Service Level involved.
            m.addField(new Field23B("CRED"));

            Field32A f32A = new Field32A()
                    .setDate(calendar)
                    .setCurrency(req.getCurrency())
                    .setAmount(amount);
            m.addField(f32A);

            //50K:ORDERING CUSTOMER - ACCOUNT-NAME AND ADDRESS
            Field50K f50K = new Field50K().setAccount(req.getSenderAccount())
                    .setNameAndAddress(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine1(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderName()))
                    .setNameAndAddressLine2(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderPhone()))
                    .setNameAndAddressLine3(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getSenderAddress()));
            m.addField(f50K);
            //52A: ODERING INSTITUTION
            Field52A f52A = new Field52A().setBIC(senderBic);
            m.addField(f52A);
            //53: SENDER'S CORRENSPONDENT
            Field53A f53A = new Field53A().setBIC(correspondentBank);
            m.addField(f53A);
            //56: INTERMEDIARY BANK
            if (req.getIntermediaryBank() != null && !req.getIntermediaryBank().equalsIgnoreCase(req.getBeneficiaryBIC().split("==")[0]) && req.getIntermediaryBank().length() > 0) {
                Field56A f56A = new Field56A();
                f56A.setBIC(req.getIntermediaryBank());
                m.addField(f56A);
            }
            //57A: IF INTERNATIONAL,EAST AFRICA, BENEFICIARY BANK SHOULD INDICATED AT THIS FIELD
            if (msgType.equals("INTERNATIONAL") || msgType.equals("EAPS")) {
                m.addField(new Field57A(req.getBeneficiaryBIC().split("==")[0]));
                m.setReceiver(correspondentBank);
            }
            //59: BENEFICIARY CUSTOMER - ACCOUNT -NAME AND ADDRESS
            Field59 f59 = new Field59();
            f59.setAccount(StringUtils.getMTLineSplitterLimitedTo35Chars(req.getBeneficiaryAccount()));
            f59.setNameAndAddressLine1(StringUtils.getMTLineSplitter(req.getBeneficiaryName()));
            m.addField(f59);
            //70: REMITANNCE INFORMATION
            String description = StringUtils.getMTLineSplitter(req.getDescription());
//            if (!description.contains("/ROC/")) {
//                description = description.replaceAll("[^a-zA-Z0-9] ", "");
//            }
            Field70 field70 = new Field70();
            description = req.getDescription();
            if (!description.contains("/ROC/")) {
                description = description.replaceAll("[^a-zA-Z0-9 ]", " ");
            }
            description = StringUtils.getMTLineSplitter(req.getDescription());

            field70.setNarrative(description);
            m.addField(field70);
//            field70.setNarrativeLine2(description);
            //71A: DETAILS OF CHARGES
            m.addField(new Field71A("SHA"));
            //72: DETAILS OF CHARGES

//            Field72 field72 = new Field72();
//            field72.setNarrative("/REC/" + StringUtils.getMTLineSplitter(req.getDescription()));
//            m.addField(field72);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String createFinanceMT940() {
        String content = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Calendar.getInstance().getTime());
        BigDecimal amt = new BigDecimal("0.00");
        String amount = String.format("%.2f", amt).replace(".", ",");
        try {
            final MT940 m = new MT940();
            m.setSender("TAPBTZTZ");//req.getBeneficiaryBIC());
            m.setReceiver("SCBLTZTZ");

            //FIELD 20: Transaction reference
            m.addField(new Field20(DateUtil.now("yyyyMMdd") + "TPB"));
            //FIELD  25: Account Identification
            m.addField(new Field25("110208000101"));
            //FIELD 28C: Statement Number/Sequence Number
            m.addField(new Field28C("1/1"));
            //FIELD 60: Opening Balance
            Field60F field60F = new Field60F()
                    .setDCMark("C")
                    .setDate("200703")
                    .setCurrency("TZS")
                    .setAmount("676493000,00");
            m.append(field60F);
            //FIELD 61 – Statement Line
            Field61 field61 = new Field61()
                    .setEntryDate(DateUtil.now("yyMMdd"))
                    .setDCMark("D")
                    .setAmount("676493000,00")
                    .setReferenceForTheAccountOwner("06003072037006057")
                    .setReferenceOfTheAccountServicingInstitution("06003072037006057")
                    .setSupplementaryDetails("TPB/HEAD OFFICE.269/2020Batch [060831148] - Account No [110208000101]");
            m.addField(field61);
            //FIELD 62 CLOSING BALANCE
            Field62F field62 = new Field62F()
                    .setDCMark("C")
                    .setDate(DateUtil.now("yyMMdd"))
                    .setAmount("0,00")
                    .setCurrency("TZS");
            m.addField(field62);
            content = m.message();
        } catch (Exception ex) {
            LOGGER.info("Error creating MT103 message ", ex);
        }
        return content;
    }

    public static String processMT202Incoming(String message) {

        return null;

    }

    public static FundFINTransferReq readMT103(String fin) throws ParseException {
        LOGGER.info("=====================Swift Message==================");
        LOGGER.info("Swift Message: {}", fin);
        LOGGER.info("=====================Swift Message=================");
        FundFINTransferReq trfMsg = null;
        SwiftBlock3 block3 = null;
        Field103 fied103 = null;
        String fied103Value = null;
        String isLocalOrInternational = "local";
        try {
            SwiftMessage sm = SwiftMessage.parse(fin);
            if (sm.isServiceMessage()) {
                sm = SwiftMessage.parse(sm.getUnparsedTexts().getAsFINString());
            }
            MessageIOType ioType = sm.getDirection();

            LOGGER.info("INPUT/OUTPUT Type.isIncoming: {}", ioType.isIncoming());
            LOGGER.info("INPUT/OUTPUT Type.isOutgoing(): {}", ioType.isOutgoing());
            if (ioType.isIncoming()) {
                if (sm.isType(103)) {
                    MT103 mt = new MT103(sm);
                    LOGGER.info("Allowed Message Type: {}", mt.getMessageType());
                    block3 = sm.getBlock3();
                    if (block3 != null) {
                        fied103 = (Field103) block3.getFieldByName("103");
                        if (fied103 != null) {
                            fied103Value = fied103.getValue();
                            if (fied103Value != null && fied103Value.contains("TIS")) {
                                isLocalOrInternational = "local";
                                //   LOGGER.info("Local Transafer Type: {}, Value: {}", fied103.getName(), fied103.getValue());
                            } else {
                                isLocalOrInternational = "international";
                                LOGGER.info("fied103Value is null, it is INTERNATIONAL transfer");
                            }
                        } else {
                            isLocalOrInternational = "international";
                            LOGGER.info("fied103 is null, it is INTERNATIONAL transfer");
                        }
                    } else {
                        LOGGER.info("Block3 is null, it is INTERNATIONAL transfer");
                        isLocalOrInternational = "international";
                    }

                    Field59 f59 = mt.getField59();
                    Field59A f59a = mt.getField59A();
                    Field59F f59f = mt.getField59F();
                    Field32A f32a = mt.getField32A();
                    Field20 f20 = mt.getField20();
                    Field23B f23b = mt.getField23B();
                    Field50K f50k = mt.getField50K();
                    Field50F f50f = mt.getField50F();
                    Field53A f53a = mt.getField53A();
                    Field52A f52a = mt.getField52A();
                    Field54A f54a = mt.getField54A();

                    Field33B f33b = mt.getField33B();
                    Field59 f50b = mt.getField59();
                    Field70 f70 = mt.getField70();
                    Field71A F71a = mt.getField71A();
                    String trxDesc = "";
                    String senderAccount = "";
                    String senderName = "";
                    String correspondendBank = "-1";
                    String receiverAccount = "-1";
                    String receiverNameAndAddress = "-1";

                    trfMsg = new FundFINTransferReq();
                    if (f70 != null) {
                        trxDesc = f70.getValue();
                    }
                    //ording customer name and address
                    if (f50k != null) {
                        senderName = f50k.getNameAndAddressLine1();
                        senderAccount = f50k.getAccount();
                    } else {
                        //the name of the ordering customer (on the first line(s) after "1/"), the street name (on the next line after "2/") and the ISO country code, a slash and the town (on the next line after "3/").
                        if (f50f != null) {
                            senderName = f50f.getNameAndAddress1();
                            senderAccount = f50f.getLine(1);
                        }
                    }
                    if (f53a != null) {
                        correspondendBank = f53a.getValue();
                        trfMsg.setSenderCorrespondent(correspondendBank);
                    } else {
                        if (f54a != null) {
                            correspondendBank = f54a.getValue();
                            trfMsg.setSenderCorrespondent(correspondendBank);
                        }
                    }
                    if (f59 != null) {
                        receiverAccount = f59.getAccount();
                        receiverNameAndAddress = f59.getNameAndAddressLine1();

                        trfMsg.setReceiverAccount(receiverAccount);
                        trfMsg.setReceiverName(receiverNameAndAddress);
                    } else {
                        if (f59a != null) {
                            receiverAccount = f59a.getAccount();
                            trfMsg.setReceiverAccount(receiverAccount);
                        } else {
                            if (f59f != null) {
                                receiverAccount = f59f.getAccount();
                                trfMsg.setReceiverAccount(receiverAccount);
                            }
                        }
                    }
                    trfMsg.setReceiverBic(sm.getReceiver());
                    trfMsg.setSenderBic(sm.getSender());
                    trfMsg.setMsgType("103");
                    trfMsg.setCurrency(f32a.currencyString());
                    trfMsg.setTranAmount(f32a.getAmount());
                    trfMsg.setTranDate(DateUtil.formatDate(f32a.getDate(), "yyMMdd", "yyyy-MM-dd"));
                    trfMsg.setBankRef(f20.getReference());
                    trfMsg.setTranDesc(trxDesc);
                    trfMsg.setSenderName(senderName);
                    trfMsg.setSenderAccount(senderAccount);
                    if (sm.getCorrespondentBIC() != null) {
                        trfMsg.setSenderCorrespondent(sm.getCorrespondentBIC().toString());
                    }
                    trfMsg.setIsLocalOrInternational(isLocalOrInternational);
                    LOGGER.info("Parsed ReceiverBic: {}", trfMsg.getReceiverBic());
                    LOGGER.info("Parsed SenderBic: {}", trfMsg.getSenderBic());
                    LOGGER.info("Parsed MsgType: {}", trfMsg.getMsgType());
                    LOGGER.info("Parsed Currency: {}", trfMsg.getCurrency());
                    LOGGER.info("Parsed TranAmount: {}", trfMsg.getTranAmount());
                    LOGGER.info("Parsed TranDate: {}", trfMsg.getTranDate());
                    LOGGER.info("Parsed TranDesc: {}", trfMsg.getTranDesc());
                    LOGGER.info("Parsed SenderName: {}", trfMsg.getSenderName());
                    LOGGER.info("Parsed SenderAccount: {}", trfMsg.getSenderAccount());
                    LOGGER.info("Parsed ReceiverAccount: {}", trfMsg.getReceiverAccount());
                    LOGGER.info("Parsed ReceiverName: {}", trfMsg.getReceiverName());
                    LOGGER.info("Parsed SenderCorrespondendBank: {}", trfMsg.getSenderCorrespondent());
                } else {
                    LOGGER.info("Not MT103 could not be processed, It is MT{}", sm.getType());
                }
            } else {
                LOGGER.info("It is outgoing message: ");
            }
            return trfMsg;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SwiftService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return trfMsg;
    }
}
