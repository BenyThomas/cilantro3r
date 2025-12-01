/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.repository;

import com.DTO.Teller.RTGSTransferForm;
import com.DTO.swift.SwiftMessageObject;
import com.DTO.swift.other.TransferAdviceReq;
import com.DTO.swift.other.Mt950ObjectReq;
import com.DTO.swift.other.Mt950StatementEntries;
import com.config.SYSENV;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.DateUtil;
import com.helper.SignRequest;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.MessageIOType;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.Tag;
import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.field.Field32A;
import com.prowidesoftware.swift.model.mt.mt1xx.MT102;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.queue.QueueProducer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author melleji.mollel
 */
@Repository
public class SwiftRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftRepository.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Autowired
    QueueProducer queProducer;

    @Autowired
    SignRequest sign;

    @Autowired
    SYSENV systemVariable;
    AtomicInteger fg = new AtomicInteger();

    public String processBoTRTGSIncoming(String payloadReq, String channel) {
        try {
            String payload = payloadReq.split("\\|")[0];
            String signature = payloadReq.split("\\|")[1];
            SwiftParser parser = new SwiftParser(payload);
            SwiftMessage mt = parser.message();
            String mtJson = mt.toJson();
            LOGGER.info("REQUEST FROM {} IN JSON FORMAT:{}", channel, mtJson);
            if (payload.equals("processed OK")) {
                String responseM = "<RESULT>0</RESULT><MESSAGE>SUCCESS</MESSAGE>";
                LOGGER.info("REQUEST FROM BOT:{} \nRESPONSE TO BOT:{}", payloadReq, responseM);
                return responseM;
            }


            SwiftMessageObject swMessageObject = objectMapper.readValue(mtJson, SwiftMessageObject.class);
            if (swMessageObject.getData().getBlock2() != null
                    && (
                    swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("011")
                            || swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("019")
            )
            ) {
                saveBotCallbackSwiftMessageInTransferAdvices(payload, channel, "INCOMING");
            } else {
                saveSwiftMessageInTransferAdvices(payload, channel, "INCOMING");//save the message in the database for reportings
            }
            //LOG DAILY TOKEN FOR TRANSACTIONS
            LOGGER.info("Block 2 :{}", swMessageObject.getData().getBlock2());
            if (swMessageObject.getData().getBlock2() != null) {
                LOGGER.info("Block 2 Message Type :{}", swMessageObject.getData().getBlock2().getMessageType());
            }
            if (swMessageObject.getData().getBlock2() != null && swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("999")) {
                //check if its STARTS OF THE DAY SOD then save the token to configuration table
                saveBoTTokenForDay(swMessageObject);
            } else if (swMessageObject.getData().getBlock2() != null && (swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("103") || swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("202"))) {
                //process transaction based on message type
                processMTSwiftMessageToCoreBanking(payload, signature, channel);

            } else if (swMessageObject.getData().getBlock2() != null && (swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("011") || swMessageObject.getData().getBlock2().getMessageType().equalsIgnoreCase("019"))) {
                //process transaction based on message type
                processTISSVPNCallback(payload, signature, channel);
            } else {
                LOGGER.info("MESSAGE RECEIVED CANNOT BE LOCATED TO APPROPRIATE TYPE:{}", payload);
            }
            String responseM = "<RESULT>0</RESULT><MESSAGE>SUCCESS</MESSAGE>";
            LOGGER.info("REQUEST FROM BOT:{} \nRESPONSE TO BOT:{}", payloadReq, responseM);
            return responseM;
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

    public void saveBoTTokenForDay(SwiftMessageObject swMessageObject) {
        String token = "-1";
        int result = -1;
        for (int i = 0; i < swMessageObject.getData().getBlock4().getTags().size(); i++) {
            if (swMessageObject.getData().getBlock4().getTags().get(i).getName().equalsIgnoreCase("21")) {
                String f21Value = swMessageObject.getData().getBlock4().getTags().get(i).getValue();
                if (f21Value.contains("TZSSOD")) {
                    token = swMessageObject.getData().getBlock4().getTags().get(2).getValue().split("\r\n")[2];
                    //SAVE TOKEN TO DATABASE
                    try {
                        result = this.jdbcTemplate.update("UPDATE system_configuration set DEV_VALUE=?,DR_VALUE=?,PROD_VALUE=?,MODIFIED_DT=? WHERE NAME='BOT.tiss.daily.token'", token, token, token, DateUtil.now());
//        this.jdbcTemplate.queryForObject("select supportingDoc from transfer_document where txnReference=? and id=? limit 1", new Object[]{ref, id}, (rs, rowNum) -> rs.getBytes(1));
                    } catch (DataAccessException e) {
                        LOGGER.error("Error getting Transaction day of week callender", e);
                    }
                }
            }
        }

    }

    public int saveBoTIsoDailyToken(String token) {
        int result = -1;
        //SAVE TOKEN TO DATABASE
        try {
            result = this.jdbcTemplate.update("UPDATE system_configuration set DEV_VALUE=?, DR_VALUE=?, PROD_VALUE=?, MODIFIED_DT=? WHERE NAME='bot.iso.daily.token'", token, token, token, DateUtil.now());
        } catch (DataAccessException e) {
            LOGGER.error("Error setting token in db", e);
        }
        return result;
    }

    public String getBoTIsoDailyToken() {
        String result = "";
        //SAVE TOKEN TO DATABASE
        try {
            result = this.jdbcTemplate.queryForObject("SELECT PROD_VALUE from system_configuration WHERE NAME='bot.iso.daily.token'", String.class);
        } catch (DataAccessException e) {
            LOGGER.error("Error setting token in db", e);
        }
        return result;
    }

    /*
    SAVE THE MESSAGE TO DATABASE BEFORE PROCESSING TO CUSTOMER ACCOUNT
     */
    public int saveSwiftMessageInTransferAdvices(String payload, String channel, String direction) {
        int result = 0;
        try {
            TransferAdviceReq adviceReq = new TransferAdviceReq();
            SwiftMessage sm = SwiftMessage.parse(payload);
            if (sm.isServiceMessage()) {
                sm = SwiftMessage.parse(sm.getUnparsedTexts().getAsFINString());
            }
            MessageIOType ioType = sm.getDirection();
            if (ioType.isIncoming() || ioType.isOutgoing()) {
                adviceReq.setSenderBank(sm.getSender());
                adviceReq.setReceiverBank(sm.getReceiver());
                adviceReq.setMessageType(sm.getBlock2().getMessageType());
                adviceReq.setMessageInPdf(generatePDFMessageFromMTMessage(payload, sm.getBlock2().getMessageType()));
                adviceReq.setDirection(direction);
                adviceReq.setSwiftMessage(payload);
                adviceReq.setChannel(channel);
                //BLOCK 3
                if (sm.getBlock3() != null) {
                    for (Tag tag : sm.getBlock3().getTags()) {
                        if (tag.getName().equalsIgnoreCase("103")) {
                            adviceReq.setServiceCode(tag.getValue());//get service code
                        }
                    }
                }
                //if message type is not mt103,202,204,205
                if (!sm.isType(103) || !sm.isType(202) || !sm.isType(204) || !sm.isType(205)) {
                    adviceReq.setTransDate(DateUtil.now());
                }
                //get message input fields
                if (sm.getBlock4() != null) {
                    for (Tag tag : sm.getBlock4().getTags()) {
                        Field field = tag.asField();
                        //get senders reference
                        if (tag.getName().equalsIgnoreCase("20")) {
                            adviceReq.setSenderReference(tag.getValue());//sender reference number
                        }
                        if (tag.getName().equalsIgnoreCase("21")) {
                            adviceReq.setRelatedReference(tag.getValue());//related reference
                        }
                        if (tag.getName().equalsIgnoreCase("32A")) {
                            adviceReq.setTransDate(DateUtil.formatDate(field.getValueDisplay(1, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd") + " " + DateUtil.now("HH:mm:ss"));//TRANS DATE
                            LOGGER.info("check the field for amount ... {}, reference 1... {} and 2 ... {}", field.getValueDisplay(3, Locale.UK), adviceReq.getRelatedReference(), adviceReq.getSenderReference());
                            adviceReq.setAmount(new BigDecimal(field.getValueDisplay(3, Locale.UK).replace(",", "")));//AMOUNT
                            adviceReq.setCurrency(field.getValueDisplay(2, Locale.UK));
                        }
                    }
                }
            }
            if (direction.equalsIgnoreCase("OUTGOING")) {
                adviceReq.setCbsStatus("C");
                adviceReq.setCbsMessage("Transaction Sent Via TISS VPN");
                adviceReq.setStatus("C");
            } else {
                adviceReq.setCbsStatus("P");
                adviceReq.setCbsMessage("Pending posting Core banking, File just Received");
                adviceReq.setStatus("P");
            }

            result = this.jdbcTemplate.update("INSERT INTO transfer_advices(valueDate,channel,swiftMessage,messageType, senderBank, receiverBank, direction, messageInPdf, senderReference, relatedReference, transDate, currency, amount, serviceCode, cbsStatus, cbsMessage, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update status='P'", DateUtil.formatDate(adviceReq.getTransDate(), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"), adviceReq.getChannel(), adviceReq.getSwiftMessage(), adviceReq.getMessageType(), adviceReq.getSenderBank(), adviceReq.getReceiverBank(), adviceReq.getDirection(), adviceReq.getMessageInPdf(), adviceReq.getSenderReference(), adviceReq.getRelatedReference(), adviceReq.getTransDate(), adviceReq.getCurrency(), adviceReq.getAmount(), adviceReq.getServiceCode(), adviceReq.getCbsStatus(), adviceReq.getCbsMessage(), adviceReq.getStatus());
        } catch (IOException | ParseException | DataAccessException e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING:");
            LOGGER.info(null, e);
        }
        return result;
    }

    public int saveBotCallbackSwiftMessageInTransferAdvices(String payload, String channel, String direction) {
        int result = 0;
        try {
            TransferAdviceReq adviceReq = new TransferAdviceReq();
            SwiftMessage sm = SwiftMessage.parse(payload);
            if (sm.isServiceMessage()) {
                sm = SwiftMessage.parse(sm.getUnparsedTexts().getAsFINString());
            }
            adviceReq.setSenderBank(sm.getSender());
            adviceReq.setReceiverBank(sm.getReceiver());
            adviceReq.setMessageType(sm.getBlock2().getMessageType());
            adviceReq.setMessageInPdf(generatePDFMessageFromMTMessage(payload, sm.getBlock2().getMessageType()));
            adviceReq.setDirection(direction);
            adviceReq.setSwiftMessage(payload);
            adviceReq.setChannel(channel);
            //BLOCK 3
            if (sm.getBlock3() != null) {
                for (Tag tag : sm.getBlock3().getTags()) {
                    if (tag.getName().equalsIgnoreCase("103")) {
                        adviceReq.setServiceCode(tag.getValue());//get service code
                    }
                }
            }
            //if message type is not mt103,202,204,205
            adviceReq.setTransDate(DateUtil.now());

            //get message input fields
            if (sm.getBlock4() != null) {
                for (Tag tag : sm.getBlock4().getTags()) {
                    Field field = tag.asField();
                    //get senders reference
                    if (tag.getName().equalsIgnoreCase("108")) {
                        adviceReq.setSenderReference(tag.getValue() + "_" + DateUtil.now("yyyyMMdd"));//sender reference number
                    }
                    if (tag.getName().equalsIgnoreCase("108")) {
                        adviceReq.setRelatedReference(tag.getValue() + "_" + DateUtil.now("yyyyMMdd"));//related reference
                    }
                }
            }

            if (direction.equalsIgnoreCase("OUTGOING")) {
                adviceReq.setCbsStatus("C");
                adviceReq.setCbsMessage("Transaction Sent Via TISS VPN");
                adviceReq.setStatus("C");
            } else {
                adviceReq.setCbsStatus("P");
                adviceReq.setCbsMessage("Pending posting Core banking, File just Received");
                adviceReq.setStatus("P");
            }

            result = this.jdbcTemplate.update("INSERT INTO transfer_advices(valueDate,channel,swiftMessage,messageType, senderBank, receiverBank, direction, messageInPdf, senderReference, relatedReference, transDate, currency, amount, serviceCode, cbsStatus, cbsMessage, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update status='P'", DateUtil.formatDate(adviceReq.getTransDate(), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"), adviceReq.getChannel(), adviceReq.getSwiftMessage(), adviceReq.getMessageType(), adviceReq.getSenderBank(), adviceReq.getReceiverBank(), adviceReq.getDirection(), adviceReq.getMessageInPdf(), adviceReq.getSenderReference(), adviceReq.getRelatedReference(), adviceReq.getTransDate(), adviceReq.getCurrency(), adviceReq.getAmount(), adviceReq.getServiceCode(), adviceReq.getCbsStatus(), adviceReq.getCbsMessage(), adviceReq.getStatus());
        } catch (IOException | ParseException | DataAccessException e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING:");
            LOGGER.info(null, e);
        }
        return result;
    }

    public int updateTransferAdvicesStatus(String botStatus, String senderReference) {
        int result = 0;
        try {
            result = this.jdbcTemplate.update("UPDATE transfer_advices SET  botStatus =?, botStatusTime=? where senderReference = ?  ", botStatus, DateUtil.now(), senderReference);
        } catch (DataAccessException e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING:");
            LOGGER.info(null, e);
        }
        return result;
    }


    public int updateTransfersTransactionsStatus(String status, String cbsStatus, String message, String senderReference) {
        int result = 0;
        try {
            jdbcTemplate.update("UPDATE transfers set status=?,cbs_status=? ,message=?  where reference=?", status, cbsStatus, message, senderReference);

        } catch (DataAccessException e) {
            LOGGER.info("AN ERROR OCCURED DURING PROCESSING:");
            LOGGER.info(null, e);
        }
        return result;
    }

    public byte[] generatePDFMessageFromMTMessage(String payload, String messageType) {
        try {
            LOGGER.info("MESSAGE TYPE:{}", messageType);
            SwiftMessage sm = SwiftMessage.parse(payload);

            LOGGER.info("MESSAGE PAYLOAD====>:{}", sm);

            String messageInPdf = "";
            messageInPdf += "***************************[TANZANIA COMERCIAL BANK- PAYMENT ADVICE]*************************";
            messageInPdf += "\n MESSAGE TYPE:" + messageType;
            messageInPdf += "\n SENDER BANK: " + sm.getSender();
            messageInPdf += "\n RECEIVER BANK: " + sm.getReceiver();
            messageInPdf += "\n ORDERING INSTITUTION:" + sm.getCorrespondentBIC().getBic11() + "  (" + sm.getCorrespondentBIC().getInstitution() + ")\n";

            messageInPdf += "*************************************Start of Message****************************************";
            //get block 2
            messageInPdf += "\nBasic Header: LT Ident:" + sm.getSender() + " Sess. no: " + sm.getBlock1().getSessionNumber() + " Seq no:" + sm.getBlock1().getSequenceNumber();
            messageInPdf += "\nAppli. Header: Receiver:" + sm.getReceiver() + " Priority:" + sm.getBlock2().getMessagePriority();
            //get block 3
            messageInPdf += "\nUser Header: ";
            if (sm.getBlock3() != null) {
                for (Tag tag : sm.getBlock3().getTags()) {
                    Field field = tag.asField();
                    messageInPdf += tag.getName() + ":  ";
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            messageInPdf += field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " ";
                        }
                    }
                    messageInPdf += "\n              ";

                }
            }
            messageInPdf += "\n";

            //get block 4 message
            if (!messageType.equalsIgnoreCase("950")) {
                String customerName = "";
                for (Tag tag : sm.getBlock4().getTags()) {
                    Field field = tag.asField();
                    messageInPdf += tag.getName() + " - " + Field.getLabel(field.getName(), messageType, null, Locale.UK).toUpperCase() + "\n";
                    if (tag.getName().equalsIgnoreCase("50K")) {//format customer name and address
                        String senderAddess = " ";
                        customerName = "";
                        for (int component = 1; component <= field.componentsSize(); component++) {
                            if (field.getComponent(component) != null) {
                                if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                    messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                    customerName += field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                    senderAddess += "            " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                    senderAddess += "            " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                            }
                        }
                        messageInPdf += "       Name And Address : " + customerName + senderAddess;

                    } else if (tag.getName().equalsIgnoreCase("50A")) {//format ORDERING CUSTOMER  NAME and ADDRESS
                        String senderAddess = " ";
                        customerName = "";
                        for (int component = 1; component <= field.componentsSize(); component++) {
                            if (field.getComponent(component) != null) {
                                if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                    messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                    customerName += field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                    senderAddess += "            " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                    senderAddess += "            " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                            }
                        }
                        messageInPdf += "       Name And Address : " + customerName + senderAddess;

                    } else if (tag.getName().equalsIgnoreCase("50F")) {//format customer name and address
                        String senderAddess = " ";
                        customerName = "";
                        for (int component = 1; component <= field.componentsSize(); component++) {
                            if (field.getComponent(component) != null) {
                                if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                    messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name of the Ordering Customer")) {
                                    customerName += field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Address Line")) {
                                    senderAddess += "                        " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Country and Town")) {
                                    senderAddess += "                        " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                            }
                        }
                        messageInPdf += "       Name And Address : " + customerName + senderAddess;

                    } else if (tag.getName().equalsIgnoreCase("59F")) {//format BENEFICIARY CUSTOMER
                        customerName = "";
                        String beneficiaryAddres = " ";
                        for (int component = 1; component <= field.componentsSize(); component++) {
//                            LOGGER.info("FIELD 59F:{}" + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK));
                            if (field.getComponent(component) != null) {
                                if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                    messageInPdf += "        " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                    customerName += field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 1")) {
                                    customerName += field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                    beneficiaryAddres += "                        " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                    beneficiaryAddres += "                        " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                            }
                        }
                        messageInPdf += "       Name And Address : " + customerName + beneficiaryAddres;

                    } else if (tag.getName().equalsIgnoreCase("59")) {//format BENEFICIARY CUSTOMER
                        String beneficiaryAddres = " ";
                        customerName = "";
                        for (int component = 1; component <= field.componentsSize(); component++) {

                            if (field.getComponent(component) != null) {
                                if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                    messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                    customerName += field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";

                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                    beneficiaryAddres += "                        " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                                if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                    beneficiaryAddres += "                        " + field.getValueDisplay(component, Locale.UK).replace("/", "") + "\n";
                                }
                            }

                        }
                        messageInPdf += "       Name And Address : " + customerName + beneficiaryAddres;

                    } else {
                        for (int component = 1; component <= field.componentsSize(); component++) {
                            if (field.getComponent(component) != null) {
                                messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                            }

                        }
                    }
                }
                if (sm.getBlock5() != null) {
                    for (Tag tag : sm.getBlock5().getTags()) {
                        if (tag.getName().equalsIgnoreCase("CHK")) {
                            messageInPdf += "Msg. Trailer: CHK - Checksum: " + tag.getValue() + "\n";
                        }
                    }
                }
            } else {
                //format mt950 to reduce number of papers
                Mt950ObjectReq mt950Req = new Mt950ObjectReq();
                boolean beforeField61 = false;
                String filed61Contents = "";
                int i = 1;
                List<Mt950StatementEntries> statementEntries = new ArrayList();
                for (Tag tag : sm.getBlock4().getTags()) {
                    Mt950StatementEntries stmentEntries = new Mt950StatementEntries();
                    Field field = tag.asField();
                    //
                    if (tag.getName().equalsIgnoreCase("20")) {
                        mt950Req.setReference(tag.getValue());//sender reference number
                    }
                    if (tag.getName().equalsIgnoreCase("25")) {
                        mt950Req.setAccount(tag.getValue());//account
                    }
                    if (tag.getName().equalsIgnoreCase("28C")) {
                        mt950Req.setStatementNo(field.getValueDisplay(1, Locale.UK));//statement number
                        mt950Req.setSequenceNo(field.getValueDisplay(2, Locale.UK));//sequence number
                    }
                    if (tag.getName().equalsIgnoreCase("60F")) {
                        mt950Req.setIsOpeningDebitOrCredit(field.getValueDisplay(1, Locale.UK));//DR OR CREDIT OPENING BALANCE
                        mt950Req.setTransDate(DateUtil.formatDate(field.getValueDisplay(2, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd"));//TRANS DATE
                        mt950Req.setCurrency(field.getValueDisplay(3, Locale.UK));//Currency
                        mt950Req.setOpeningBalance(new BigDecimal(field.getValueDisplay(4, Locale.UK).replace(",", "")));
                        mt950Req.setClosingBalance(new BigDecimal(field.getValueDisplay(4, Locale.UK).replace(",", "")));
                    }
                    if (tag.getName().equalsIgnoreCase("60M")) {
                        mt950Req.setIsOpeningDebitOrCredit(field.getValueDisplay(1, Locale.UK));//DR OR CREDIT OPENING BALANCE
                        mt950Req.setTransDate(DateUtil.formatDate(field.getValueDisplay(2, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd"));//TRANS DATE
                        mt950Req.setCurrency(field.getValueDisplay(3, Locale.UK));//Currency
                        if (mt950Req.getIsOpeningDebitOrCredit().equalsIgnoreCase("D")) {
                            mt950Req.setOpeningBalance(new BigDecimal("-" + field.getValueDisplay(4, Locale.UK).replace(",", "")));
                            mt950Req.setClosingBalance(new BigDecimal("-" + field.getValueDisplay(4, Locale.UK).replace(",", "")));
                        } else {
                            mt950Req.setOpeningBalance(new BigDecimal(field.getValueDisplay(4, Locale.UK).replace(",", "")));
                            mt950Req.setClosingBalance(new BigDecimal(field.getValueDisplay(4, Locale.UK).replace(",", "")));
                        }
                    }
                    if (tag.getName().equalsIgnoreCase("62F")) {
                        mt950Req.setIsOpeningDebitOrCredit(field.getValueDisplay(1, Locale.UK));//DR OR CREDIT OPENING BALANCE
                        mt950Req.setTransDate(DateUtil.formatDate(field.getValueDisplay(2, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd"));//TRANS DATE
                        mt950Req.setCurrency(field.getValueDisplay(3, Locale.UK));//Currency
                        if (mt950Req.getIsOpeningDebitOrCredit().equalsIgnoreCase("D")) {
                            mt950Req.setOpeningBalance(new BigDecimal("-" + field.getValueDisplay(4, Locale.UK).replace(",", "")));
                            mt950Req.setClosingBalance(new BigDecimal("-" + field.getValueDisplay(4, Locale.UK).replace(",", "")));
                        } else {
                            mt950Req.setOpeningBalance(new BigDecimal(field.getValueDisplay(4, Locale.UK).replace(",", "")));
                            mt950Req.setClosingBalance(new BigDecimal(field.getValueDisplay(4, Locale.UK).replace(",", "")));
                        }
                        queProducer.sendToQueueBOTClosingBalance(mt950Req);
                    }
                    //check if tag is not 61

                    if (tag.getName().equalsIgnoreCase("61")) {
                        BigDecimal previousBalance = mt950Req.getClosingBalance();
                        beforeField61 = true;
                        String senderReceiver = field.getValueDisplay(10, Locale.UK);
                        if (senderReceiver == null) {
                            senderReceiver = "_________________";
                        }
                        filed61Contents += "\t\t\t\t\t\t" + i + ",\t\t\t" + field.getValueDisplay(1, Locale.UK) + "\t\t\t\t\t" + field.getValueDisplay(3, Locale.UK) + "\t\t\t\t\t\t\t\t" + field.getValueDisplay(7, Locale.UK) + " \t\t\t\t\t\t" + field.getValueDisplay(8, Locale.UK) + "\t\t\t\t\t\t\t\t\t\t" + senderReceiver + "\t\t\t\t" + field.getValueDisplay(5, Locale.UK) + "\t\t\t\t\t\t\t\n";
                        i++;

                        stmentEntries.setAmount(new BigDecimal(field.getValueDisplay(5, Locale.UK).replace(",", "")));
                        stmentEntries.setRelatedReference(field.getValueDisplay(8, Locale.UK));
                        String beneficiaryBank = "";
                        String senderBank = "";
                        if (!senderReceiver.contains("_")) {
                            beneficiaryBank = senderReceiver.split(" ")[1];
                            senderBank = senderReceiver.split(" ")[0];
                        }
                        String debitOrCredit = field.getValueDisplay(3, Locale.UK);
                        if (debitOrCredit.equalsIgnoreCase("C")) {
                            stmentEntries.setPrevoiusBalance(previousBalance);
                            stmentEntries.setPostBalance(previousBalance.add(stmentEntries.getAmount()));
                            mt950Req.setClosingBalance(stmentEntries.getPostBalance());
                        }
                        if (debitOrCredit.equalsIgnoreCase("D")) {
                            stmentEntries.setPrevoiusBalance(previousBalance);
                            stmentEntries.setPostBalance(previousBalance.subtract(stmentEntries.getAmount()));
                            mt950Req.setClosingBalance(stmentEntries.getPostBalance());
                        }
                        stmentEntries.setBeneficiaryBank(beneficiaryBank);
                        stmentEntries.setCurrency(mt950Req.getCurrency());
                        stmentEntries.setDrCrIndicator(debitOrCredit);
                        stmentEntries.setMessageType(field.getValueDisplay(7, Locale.UK));
                        stmentEntries.setSenderBank(senderBank);
                        stmentEntries.setTransDate(field.getValueDisplay(1, Locale.UK));
                        statementEntries.add(stmentEntries);
                    } else {
                        //ADD FIELD 61 CONTENTS BEFORE MOVING TO ANOTHER FILED
                        if (beforeField61 == true && !tag.getName().equalsIgnoreCase("61")) {
                            messageInPdf += "61: \tS/N\t\t\tDate\t\t\t\t\t\t\t\tDebit/Credit\t\t\tType \t\t\t\t\tReference  \t\t\t\t\t\t\t\t   Sender& Receiver  \t\t\t\t\t    Amount  \t\t\t\t\t\n";
                            messageInPdf += filed61Contents;
                            mt950Req.setStatementEntries(statementEntries);
                        }
                        messageInPdf += tag.getName() + " - " + Field.getLabel(field.getName(), messageType, null, Locale.UK).toUpperCase() + "\n";
                        for (int component = 1; component <= field.componentsSize(); component++) {
                            if (field.getComponent(component) != null) {
                                messageInPdf += "       " + field.getComponentLabel(component) + " : " + field.getValueDisplay(component, Locale.UK) + " \n";
                            }
                        }

                    }

                }
                /*
                    SEND TO QUEUE MT950 ENTRIES FOR SAVING THEM TO CORE BANKING
                 */
                if (mt950Req.getStatementEntries() != null) {
                    queProducer.sendToQueueMT950EntriesForReport(mt950Req);
                } else {
                    LOGGER.info("MT950 HAS NO ENTRIES TO BE LOGGED************");
                }
            }

            messageInPdf += "*************************************End of Message****************************************";
            LOGGER.info(messageInPdf);
            //instantiate the document
            Document document = new Document();
            //add fonts
            Font font = FontFactory.getFont(FontFactory.COURIER, 8, Font.NORMAL + Font.UNDEFINED);
            //create the dataoutput stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //instantiate the  pdfwriter with document and output stream where the document will be written
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            //instantiate paraghraph and write the string to the paragraph with appropriate fonts instantiated above
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_MIDDLE);
            p.add(new Chunk(messageInPdf, font));
            p.add(new Chunk("", font));
            document.add(p);
            document.close();
            writer.close();
            //get the output stream to be saved in the database and retun a byte[] object

            return outputStream.toByteArray();
        } catch (Exception ex) {
            LOGGER.info(null, ex);
        }
        return null;
    }

    public String processMTSwiftMessageToCoreBanking(String payload, String signature, String channel) {
        RTGSTransferForm transferReq = new RTGSTransferForm();
        transferReq.setSwiftMessage(payload);
        Locale locale = Locale.getDefault();
        String response = "-1";
        try {
            SwiftMessage sm = SwiftMessage.parse(payload);
            transferReq.setBeneficiaryBIC(sm.getReceiver());
            transferReq.setSenderBic(sm.getSender());
            if (sm.getBlock2() != null) {
                //get message type
                transferReq.setMessageType(sm.getBlock2().getMessageType());
            }
            transferReq.setChannel(channel);
            //check if transaction is international or local
            boolean isItLocal = false;
            if (sm.getBlock3() != null) {
                for (Tag tag : sm.getBlock3().getTags()) {
                    Field field = tag.asField();
                    if (tag.getName().equalsIgnoreCase("103") && field.getValueDisplay(1, Locale.UK).equalsIgnoreCase("TIS")) {
                        //the transfer is local tiss
                        isItLocal = true;
                    }
                }
            }
            if (isItLocal) {
                transferReq.setTransactionType("001");//001 stands for local transfer and 004 stands for internetional transfer
            } else {
                transferReq.setTransactionType("004");//001 stands for local transfer and 004 stands for internetional transfer

            }
            if (!sm.isType(103)) {
                transferReq.setSenderAccount(sm.getSender());
                transferReq.setBeneficiaryAccount(sm.getReceiver());
                transferReq.setBeneficiaryName(sm.getReceiver());
                transferReq.setSenderName(sm.getSender());
            }
            for (Tag tag : sm.getBlock4().getTags()) {
                Field field = tag.asField();
                //get senders reference
                if (tag.getName().equalsIgnoreCase("20")) {
                    transferReq.setReference(tag.getValue());//sender reference number
                    transferReq.setBatchReference(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("21")) {
                    transferReq.setBatchReference(tag.getValue());
                    transferReq.setRelatedReference(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("32A")) {
                    if (sm.isType(103)) {
                        MT103 mt = new MT103(sm);
                        Field32A f32a = mt.getField32A();
                        String amount = f32a.getAmount();
//                        LOGGER.info("AMOUNT SUBMITED====2:{}", amount);
                        transferReq.setAmount(amount.replace(",", "."));
                        String transdate = f32a.getDate();
                        transferReq.setTransactionDate(transdate);
//                        LOGGER.info("AMOUNT SUBMITED====3:{}", transferReq.getAmount());

                    } else if (sm.isType(202)) {
                        MT102 mt = new MT102(sm);
                        Field32A f32a = mt.getField32A();
                        String amount = f32a.getAmount();
//                        LOGGER.info("AMOUNT SUBMITED====2:{}", amount);
                        transferReq.setAmount(amount.replace(",", "."));
                        String transdate = f32a.getDate();
                        transferReq.setTransactionDate(transdate);
                    } else {
                        String amount = field.getValueDisplay(3, locale);
//                        LOGGER.info("AMOUNT SUBMITED====2:{}", amount);
                        transferReq.setAmount(amount);
                    }

                    transferReq.setCurrency(field.getValueDisplay(2, Locale.UK));
                    transferReq.setTransactionDate(DateUtil.formatDate(field.getValueDisplay(1, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd"));
                }
                if (tag.getName().equalsIgnoreCase("50K")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String senderAddess = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                transferReq.setSenderName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            transferReq.setSenderAddress(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("50A")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String senderAddess = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                transferReq.setSenderName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            transferReq.setSenderAddress(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("50F")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String senderAddess = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name of the Ordering Customer")) {
                                transferReq.setSenderName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Address Line")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Country and Town")) {
                                senderAddess += " " + field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            transferReq.setSenderAddress(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("53A")) {
                    transferReq.setCorrespondentBic(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("54A")) {
                    transferReq.setCorrespondentBic(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("52A")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                        }
                    }
                }
                if (tag.getName().equalsIgnoreCase("56")) {
                    transferReq.setIntermediaryBank(field.getValueDisplay(1, Locale.UK).replace("/", ""));
                }
                if (tag.getName().equalsIgnoreCase("59")) {
                    transferReq.setBeneficiaryAccount(field.getValueDisplay(1, Locale.UK) == null ? "" : field.getValueDisplay(1, Locale.UK).replace("/", ""));
                    transferReq.setBeneficiaryName(field.getValueDisplay(2, Locale.UK) == null ? "" : field.getValueDisplay(2, Locale.UK));
                }
                if (tag.getName().equalsIgnoreCase("59F")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String beneficiaryAddres = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setBeneficiaryAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                transferReq.setBeneficiaryName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                beneficiaryAddres += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                beneficiaryAddres += " " + field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
//                            transferReq.setB(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("70")) {
                    transferReq.setDescription(field.getValueDisplay(1, Locale.UK).replace("//", ""));
                }
                if (tag.getName().equalsIgnoreCase("71A")) {
                    transferReq.setChargeDetails(field.getValueDisplay(1, Locale.UK));
                }
                if (tag.getName().equalsIgnoreCase("72")) {
                    if (transferReq.getDescription() == null) {
                        transferReq.setDescription(field.getValueDisplay(1, Locale.UK).replace("//", ""));
                    } else {
                        transferReq.setComments(field.getValueDisplay(1, Locale.UK));
                    }
                }
            }
            //check channel if its BOT THEN VALIDATE SIGNATURE
            if (channel.equalsIgnoreCase("BOT-VPN")) {
                boolean isMessageValid = sign.verifySignature(signature, payload, systemVariable.PUBLIC_TISS_VPN_KEYPASS, systemVariable.PUBLIC_TISS_VPN_KEY_ALIAS, systemVariable.PUBLIC_TISS_VPN_KEY_FILE_PATH);

                if (isMessageValid) {
                    //log the transaction in transfer table and process it
                    transferReq.setMessage("Message is valid from BoT and is being processed");
                    transferReq.setComments("Message is valid from BoT and received ready for processing");
                    transferReq.setResponseCode("-1");
                    queProducer.sendToQueueRTGSIncomingForLoggingToDB(transferReq);//log the transaction into CILANTRO DATABASE
                    //check if its rejection MT202 and process reversal
                    if (sm.isType(202)) {
                        List<Map<String, Object>> txn = getSwiftMessage(transferReq.getRelatedReference());
                        if (!txn.isEmpty()) {
                            //REFUND THE TRANSACTION TO CUSTOMER
                            queProducer.sendToQueueMT103RefundToCustomer(transferReq.getRelatedReference() + "^" + transferReq.getDescription().replace("\r\n", " ") + "^" + transferReq.getAmount() + "^" + txn.get(0).get("amount"));
                        }
                    }
                    if (sm.isType(103)) {
                        //PROCESS THE TRANSACTION TO CORE BANKING FROM BOT TISS VPN
                        queProducer.sendToQueueMTSTPSwiftInwardMessage(transferReq);
                    }
                    response = "<TRANSACTION ASSIGNED TO QUEUE FOR PROCESSING: Sender Reference:" + transferReq.getReference() + ">";
                } else {
                    transferReq.setMessage("Message Cannot be validated,Please confirm with Sender if the transaction is Geneuine");
                    transferReq.setComments("Message Cannot be validated,Please confirm with Sender if the transaction is Geneuine");
                    transferReq.setResponseCode("999");
                    queProducer.sendToQueueRTGSIncomingForLoggingToDB(transferReq);//log the transaction into CILANTRO DATABASE
                    //donot process the transaction to core baking
//                    queProducer.sendToQueueMTSTPSwiftInwardMessage(transferReq);
                    response = "<****************DONOT PROCESS, SIGNATURE IS INVALID*****************************\nTRANSACTION CANNOT BE PROCESSED BECAUSE CERTIFICATE CANNOT BE VALIDATED: Sender Reference:" + transferReq.getReference() + "\n*******************************WARNING******************************>";
                }
            } else {
                transferReq.setMessage("Message Received not Processed");
                transferReq.setComments("Message Received not Processed");
                transferReq.setResponseCode("-1");
                queProducer.sendToQueueRTGSIncomingForLoggingToDB(transferReq);//log the transaction into CILANTRO DATABASE
                //processing payments from swift
                //check if its rejection MT202 and process reversal
                if (sm.isType(202)) {
                    List<Map<String, Object>> txn = getSwiftMessage(transferReq.getRelatedReference());
                    if (txn != null && !txn.isEmpty()) {
                        //REFUND THE TRANSACTION TO CUSTOMER
                        LOGGER.info("PROCESSING REFUND TO CUSTOMER WITH REFERENCE: {}", transferReq.getRelatedReference());
                        queProducer.sendToQueueMT103RefundToCustomer(transferReq.getRelatedReference() + "^" + transferReq.getDescription().replace("\r\n", " ") + "^" + transferReq.getAmount() + "^" + txn.get(0).get("amount"));
                    } else {
                        response = "<****************DOES NOT EXIST IN TRANSFERS TABLE: PROCESS INSTITUTION 2 INSTITUTION PAYMENTS*****************************Sender Reference:" + transferReq.getReference() + "***************************************************>";
                        LOGGER.info(response);
                    }
                } else if (transferReq.getSenderBic() != null & (transferReq.getSenderBic().contains("CITIUS") || transferReq.getSenderBic().equals("BKTRUS"))) {
                    //PROCESS THE TRANSACTION TO CORE BANKING FROM BOT TISS VPN
                    response = "Transaction blocked to be posted manually by the IBD teamm: Sender Reference:" + transferReq.getReference() + ">";
                    LOGGER.info(response);

                } else if (sm.isType(103)) {
                    //PROCESS THE TRANSACTION TO CORE BANKING FROM BOT TISS VPN
                    queProducer.sendToQueueMTSTPSwiftInwardMessage(transferReq);
                    response = "<TRANSACTION ASSIGNED TO QUEUE FOR PROCESSING: Sender Reference:" + transferReq.getReference() + ">";
                    LOGGER.info(response);

                } else {
                    response = "<*********THIS MESSAGE IS NOT MEANT FOR STP: Sender Reference:" + transferReq.getReference() + ">";
                }
            }
            //process Posting in core banking
            return response;
        } catch (Exception ex) {
            LOGGER.info("EXCEPTION OCCURED DURING PROCESSING BOT INCOMING MESSAGE WITH REFERENCE: {} : ERROR MESSAGE:{} ", transferReq.getReference(), ex.getMessage());
            LOGGER.info(null, ex);

            return null;

        }

    }


    public String processTISSVPNCallback(String payload, String signature, String channel) {
        RTGSTransferForm transferReq = new RTGSTransferForm();
        transferReq.setSwiftMessage(payload);
        Locale locale = Locale.getDefault();
        String response = "-1";
        try {
            SwiftMessage sm = SwiftMessage.parse(payload);
            transferReq.setBeneficiaryBIC(sm.getReceiver());
            transferReq.setSenderBic(sm.getSender());
            if (sm.getBlock2() != null) {
                //get message type
                transferReq.setMessageType(sm.getBlock2().getMessageType());
            }
            transferReq.setChannel(channel);

            transferReq.setTransactionType("001");//001 stands for local transfer and 004 stands for internetional transfer

            for (Tag tag : sm.getBlock4().getTags()) {
                Field field = tag.asField();
                //get senders reference
                if (tag.getName().equalsIgnoreCase("108")) {
                    transferReq.setReference(tag.getValue());//sender reference number
                    transferReq.setBatchReference(tag.getValue());
                    transferReq.setRelatedReference(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("432")) {
                    transferReq.setResponseCode(tag.getValue());
                }

            }
            //check channel if its BOT THEN VALIDATE SIGNATURE
            if (channel.equalsIgnoreCase("BOT-VPN")) {
                boolean isMessageValid = sign.verifySignature(signature, payload, systemVariable.PUBLIC_TISS_VPN_KEYPASS, systemVariable.PUBLIC_TISS_VPN_KEY_ALIAS, systemVariable.PUBLIC_TISS_VPN_KEY_FILE_PATH);

                if (isMessageValid) {
                    //log the transaction in transfer table and process it
                    transferReq.setMessage("Message is valid from BoT and is being processed");
                    transferReq.setComments("Message is valid from BoT and received ready for processing");
                    LOGGER.info(transferReq.toString());
                    //check if its rejection MT019 and process reversal
                    if (sm.getBlock2().getMessageType().equals("019")) {
                        List<Map<String, Object>> txn = getSwiftMessage(transferReq.getRelatedReference());
                        if (!txn.isEmpty() && txn != null) {
                            //REFUND THE TRANSACTION TO CUSTOMER
                            updateTransfersTransactionsStatus("S", "F", "Transaction failed - Cbs:success, tissvpn: rejected", transferReq.getReference());
                            updateTransferAdvicesStatus("TISSVPN_REJECTED", transferReq.getReference());
                            //TODO: DIS-ABLED FOR THE MOMENT ANALYSIZING THE RISK
                            // queProducer.sendToQueueMT103RefundToCustomer(transferReq.getRelatedReference() + "^Responsse code:" + transferReq.getResponseCode()+ "^" + txn.get(0).get("amount") + "^" + txn.get(0).get("amount"));
                        }
                    } else if (sm.getBlock2().getMessageType().equals("011")) {
                        List<Map<String, Object>> txn = getSwiftMessage(transferReq.getRelatedReference());
                        if (!txn.isEmpty() && txn != null) {
                            //REFUND THE TRANSACTION TO CUSTOMER
                            updateTransfersTransactionsStatus("C", "C", "Transaction successfully - Cbs:success, tissvpn: success", transferReq.getReference());
                            updateTransferAdvicesStatus("TISSVPN_RECEIVED", transferReq.getReference());
                        }
                    }
                    response = "<TRANSACTION ASSIGNED TO QUEUE FOR PROCESSING: Sender Reference:" + transferReq.getReference() + ">";
                } else {
                    transferReq.setMessage("Message Cannot be validated,Please confirm with Sender if the transaction is Geneuine");
                    transferReq.setComments("Message Cannot be validated,Please confirm with Sender if the transaction is Geneuine");
                    transferReq.setResponseCode("999");

                    response = "<****************DONOT PROCESS, SIGNATURE IS INVALID*****************************\nTRANSACTION CANNOT BE PROCESSED BECAUSE CERTIFICATE CANNOT BE VALIDATED: Sender Reference:" + transferReq.getReference() + "\n*******************************WARNING******************************>";
                }
            }
            //process Posting in core banking
            LOGGER.info("response:{} \ntransferReq:{}", response, transferReq);

            return response;
        } catch (Exception ex) {
            LOGGER.info("EXCEPTION OCCURED DURING PROCESSING BOT INCOMING MESSAGE WITH REFERENCE: {} : ERROR MESSAGE:{} ", transferReq.getReference(), ex.getMessage());
            LOGGER.info(null, ex);

            return null;

        }

    }


    public List<Map<String, Object>> getSwiftMessage(String reference) {
        LOGGER.info("GOING TO CHECK IF THE MESSAGE IS RETURNED WITH REFERENCE: {}", reference);
        try {
            return this.jdbcTemplate.queryForList("SELECT * FROM transfers where reference=?", reference);
        } catch (DataAccessException ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSACTION " + fg.incrementAndGet());
            return null;
        }
    }

    public List<Map<String, Object>> getTTIncoming() {
        try {
            return this.jdbcTemplate.queryForList("select swift_message,reference,sourceAcct,destinationAcct  from transfers  where direction ='INCOMING' and txn_type in ('004') and message_type =103 and beneficiary_contact is null order by create_dt asc");
        } catch (DataAccessException ex) {
            LOGGER.info("EXCEPTION ON GETTING TRANSACTION " + fg.incrementAndGet());
            return null;
        }
    }

    public RTGSTransferForm swiftMessageToObject(String payload) {
        RTGSTransferForm transferReq = new RTGSTransferForm();
        // transferReq.setSwiftMessage(payload);
        Locale locale = Locale.getDefault();
        String response = "-1";
        try {
            SwiftMessage sm = SwiftMessage.parse(payload);
            transferReq.setBeneficiaryBIC(sm.getReceiver());
            transferReq.setSenderBic(sm.getSender());
            if (sm.getBlock2() != null) {
                //get message type
                transferReq.setMessageType(sm.getBlock2().getMessageType());
            }

            //check if transaction is international or local
            boolean isItLocal = false;
            if (sm.getBlock3() != null) {
                for (Tag tag : sm.getBlock3().getTags()) {
                    Field field = tag.asField();
                    if (tag.getName().equalsIgnoreCase("103") && field.getValueDisplay(1, Locale.UK).equalsIgnoreCase("TIS")) {
                        //the transfer is local tiss
                        isItLocal = true;
                    }
                }
            }
            if (isItLocal) {
                transferReq.setTransactionType("001"); //001 stands for local transfer and 004 stands for international transfer
            } else {
                transferReq.setTransactionType("004"); //001 stands for local transfer and 004 stands for international transfer

            }
            if (!sm.isType(103)) {
                transferReq.setSenderAccount(sm.getSender());
                transferReq.setBeneficiaryAccount(sm.getReceiver());
                transferReq.setBeneficiaryName(sm.getReceiver());
                transferReq.setSenderName(sm.getSender());
            }
            for (Tag tag : sm.getBlock4().getTags()) {
                Field field = tag.asField();

                //   LOGGER.info("TAG:-> {} : {} ",tag.getName(),tag.getValue());

                //get senders reference
                if (tag.getName().equalsIgnoreCase("20")) {
                    transferReq.setReference(tag.getValue());//sender reference number
                    transferReq.setBatchReference(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("21")) {
                    transferReq.setBatchReference(tag.getValue());
                    transferReq.setRelatedReference(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("32A")) {
                    if (sm.isType(103)) {
                        MT103 mt = new MT103(sm);
                        Field32A f32a = mt.getField32A();
                        String amount = f32a.getAmount();
//                        LOGGER.info("AMOUNT SUBMITED====2:{}", amount);
                        transferReq.setAmount(amount.replace(",", "."));
//                        LOGGER.info("AMOUNT SUBMITED====3:{}", transferReq.getAmount());

                    } else if (sm.isType(202)) {
                        MT102 mt = new MT102(sm);
                        Field32A f32a = mt.getField32A();
                        String amount = f32a.getAmount();
//                        LOGGER.info("AMOUNT SUBMITED====2:{}", amount);
                        transferReq.setAmount(amount.replace(",", "."));
                    } else {
                        String amount = field.getValueDisplay(3, locale);
//                        LOGGER.info("AMOUNT SUBMITED====2:{}", amount);
                        transferReq.setAmount(amount);
                    }

                    transferReq.setCurrency(field.getValueDisplay(2, Locale.UK));
                    transferReq.setTransactionDate(DateUtil.formatDate(field.getValueDisplay(1, Locale.UK), "dd-MMM-yyyy", "yyyy-MM-dd"));
                }
                if (tag.getName().equalsIgnoreCase("50K")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String senderAddess = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                transferReq.setSenderName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            transferReq.setSenderAddress(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("50A")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String senderAddess = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                transferReq.setSenderName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            transferReq.setSenderAddress(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("50F")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String senderAddess = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setSenderAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name of the Ordering Customer")) {
                                transferReq.setSenderName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Address Line")) {
                                senderAddess += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Country and Town")) {
                                senderAddess += " " + field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            transferReq.setSenderAddress(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("53A")) {
                    transferReq.setCorrespondentBic(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("54A")) {
                    transferReq.setCorrespondentBic(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("52A")) {
                    transferReq.setBeneficiaryContact(tag.getValue());
                    transferReq.setSenderPhone(tag.getValue());
                }
                if (tag.getName().equalsIgnoreCase("56")) {
                    transferReq.setIntermediaryBank(field.getValueDisplay(1, Locale.UK).replace("/", ""));
                }
                if (tag.getName().equalsIgnoreCase("59")) {
                    transferReq.setBeneficiaryAccount(field.getValueDisplay(1, Locale.UK).replace("/", ""));
                    transferReq.setBeneficiaryName(field.getValueDisplay(2, Locale.UK));
                }
                if (tag.getName().equalsIgnoreCase("59F")) {
                    for (int component = 1; component <= field.componentsSize(); component++) {
                        if (field.getComponent(component) != null) {
                            String beneficiaryAddres = " ";
                            if (field.getComponentLabel(component).equalsIgnoreCase("Account")) {
                                transferReq.setBeneficiaryAccount(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address")) {
                                transferReq.setBeneficiaryName(field.getValueDisplay(component, Locale.UK).replace("/", ""));
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 2")) {
                                beneficiaryAddres += field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
                            if (field.getComponentLabel(component).equalsIgnoreCase("Name And Address 3")) {
                                beneficiaryAddres += " " + field.getValueDisplay(component, Locale.UK).replace("/", "");
                            }
//                            transferReq.setB(senderAddess);
                        }
                    }

                }
                if (tag.getName().equalsIgnoreCase("70")) {
                    transferReq.setDescription(field.getValueDisplay(1, Locale.UK).replace("//", ""));
                }
                if (tag.getName().equalsIgnoreCase("71A")) {
                    transferReq.setChargeDetails(field.getValueDisplay(1, Locale.UK));
                }
                if (tag.getName().equalsIgnoreCase("72")) {
                    if (transferReq.getDescription() == null) {
                        transferReq.setDescription(field.getValueDisplay(1, Locale.UK).replace("//", ""));
                    } else {
                        transferReq.setComments(field.getValueDisplay(1, Locale.UK));
                    }
                }
            }

            //process Posting in core banking
            return transferReq;
        } catch (Exception ex) {
            LOGGER.info("EXCEPTION OCCURED DURING PROCESSING BOT INCOMING MESSAGE WITH REFERENCE: {} : ERROR MESSAGE:{} ", transferReq.getReference(), ex.getMessage());
            LOGGER.info(null, ex);
            return null;

        }

    }

    public int updateBicSwiftTT(String bic, String reference, String sourceAcct, String destinationAcct) {
        String token = "-1";
        int result = -1;

        //SAVE TOKEN TO DATABASE
        try {
            result = this.jdbcTemplate.update("UPDATE transfers set beneficiary_contact=? WHERE reference=? and sourceAcct=? and destinationAcct=?", bic, reference, sourceAcct, destinationAcct);
        } catch (DataAccessException e) {
            LOGGER.error("updateBicSwiftTT", e);
        }


        return result;
    }


    public String tissVpnTransferAdviceWork(String senderReference) {
        try {
            String sql = "SELECT messageInPdf FROM transfer_advices WHERE senderReference = ?";
            // Query as byte array
            byte[] pdfBytes = jdbcTemplate.queryForObject(
                    sql,
                    new Object[]{senderReference},
                    byte[].class
            );
            // Convert to Base64 string
            if (pdfBytes != null) {
                return Base64.encodeBase64String(pdfBytes);
            } else {
                return null; // or throw exception
            }
        } catch (Exception ex) {
            LOGGER.info(null, ex);
            return null;
        }
    }

}
