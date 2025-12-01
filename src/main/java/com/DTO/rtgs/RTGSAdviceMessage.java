/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.rtgs;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

/**
 *
 * @author samichael
 */
public class RTGSAdviceMessage implements Serializable {

    private String trailer_3 = null;
    private String traffic_Type = null;
    private BigDecimal value_Amount;
    private byte[] pdf;
    private String byte_Length = null;
    private String cleared = null;
    private String trailer_2 = null;
    private String receiver;
    private String trailer_1 = null;
    private String notification;
    private String mir = null;
    private String fileCRC;
    private Date dated;
    private String sender;
    private String value_Date;
    private String timed;
    private String refNo;
    private String service_Code;
    private String message_Type;
    private String status;
    private String beneficiary_Customer_Account;
    private String value_Currency;
    private String related_Reference = null;
    private String lastModifiedBy;
    private String transaction_Type_Code = null;
    private String ordering_Account;
    private String senders_Reference;
    private String network_Delivery_Status;
    private String partner_code;
    private String to_email;
    private String to_email_status;
    private String to_email_date;

    // Getter Methods
    public String getTrailer_3() {
        return trailer_3;
    }

    public void setTrailer_3(String trailer_3) {
        this.trailer_3 = trailer_3;
    }

    public String getTraffic_Type() {
        return traffic_Type;
    }

    public void setTraffic_Type(String traffic_Type) {
        this.traffic_Type = traffic_Type;
    }

    public BigDecimal getValue_Amount() {
        return value_Amount;
    }

    public void setValue_Amount(BigDecimal value_Amount) {
        this.value_Amount = value_Amount;
    }

    public byte[] getPdf() {
        return pdf;
    }

    public void setPdf(byte[] pdf) {
        this.pdf = pdf;
    }

    public String getByte_Length() {
        return byte_Length;
    }

    public void setByte_Length(String byte_Length) {
        this.byte_Length = byte_Length;
    }

    public String getCleared() {
        return cleared;
    }

    public void setCleared(String cleared) {
        this.cleared = cleared;
    }

    public String getTrailer_2() {
        return trailer_2;
    }

    public void setTrailer_2(String trailer_2) {
        this.trailer_2 = trailer_2;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getTrailer_1() {
        return trailer_1;
    }

    public void setTrailer_1(String trailer_1) {
        this.trailer_1 = trailer_1;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getMir() {
        return mir;
    }

    public void setMir(String mir) {
        this.mir = mir;
    }

    public String getFileCRC() {
        return fileCRC;
    }

    public void setFileCRC(String fileCRC) {
        this.fileCRC = fileCRC;
    }

    public Date getDated() {
        return dated;
    }

    public void setDated(Date dated) {
        this.dated = dated;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getValue_Date() {
        return value_Date;
    }

    public void setValue_Date(String value_Date) {
        this.value_Date = value_Date;
    }

    public String getTimed() {
        return timed;
    }

    public void setTimed(String timed) {
        this.timed = timed;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getService_Code() {
        return service_Code;
    }

    public void setService_Code(String service_Code) {
        this.service_Code = service_Code;
    }

    public String getMessage_Type() {
        return message_Type;
    }

    public void setMessage_Type(String message_Type) {
        this.message_Type = message_Type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBeneficiary_Customer_Account() {
        return beneficiary_Customer_Account;
    }

    public void setBeneficiary_Customer_Account(String beneficiary_Customer_Account) {
        this.beneficiary_Customer_Account = beneficiary_Customer_Account;
    }

    public String getValue_Currency() {
        return value_Currency;
    }

    public void setValue_Currency(String value_Currency) {
        this.value_Currency = value_Currency;
    }

    public String getRelated_Reference() {
        return related_Reference;
    }

    public void setRelated_Reference(String related_Reference) {
        this.related_Reference = related_Reference;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getTransaction_Type_Code() {
        return transaction_Type_Code;
    }

    public void setTransaction_Type_Code(String transaction_Type_Code) {
        this.transaction_Type_Code = transaction_Type_Code;
    }

    public String getOrdering_Account() {
        return ordering_Account;
    }

    public void setOrdering_Account(String ordering_Account) {
        this.ordering_Account = ordering_Account;
    }

    public String getSenders_Reference() {
        return senders_Reference;
    }

    public void setSenders_Reference(String senders_Reference) {
        this.senders_Reference = senders_Reference;
    }

    public String getNetwork_Delivery_Status() {
        return network_Delivery_Status;
    }

    public void setNetwork_Delivery_Status(String network_Delivery_Status) {
        this.network_Delivery_Status = network_Delivery_Status;
    }

    public String getPartner_code() {
        return partner_code;
    }

    public void setPartner_code(String partner_code) {
        this.partner_code = partner_code;
    }

    public String getTo_email() {
        return to_email;
    }

    public void setTo_email(String to_email) {
        this.to_email = to_email;
    }

    public String getTo_email_status() {
        return to_email_status;
    }

    public void setTo_email_status(String to_email_status) {
        this.to_email_status = to_email_status;
    }

    public String getTo_email_date() {
        return to_email_date;
    }

    public void setTo_email_date(String to_email_date) {
        this.to_email_date = to_email_date;
    }

    @Override
    public String toString() {
        return "RTGSAdviceMessage{" + "trailer_3=" + trailer_3 + ", traffic_Type=" + traffic_Type + ", value_Amount=" + value_Amount + ", pdf=" + pdf + ", byte_Length=" + byte_Length + ", cleared=" + cleared + ", trailer_2=" + trailer_2 + ", receiver=" + receiver + ", trailer_1=" + trailer_1 + ", notification=" + notification + ", mir=" + mir + ", fileCRC=" + fileCRC + ", dated=" + dated + ", sender=" + sender + ", value_Date=" + value_Date + ", timed=" + timed + ", refNo=" + refNo + ", service_Code=" + service_Code + ", message_Type=" + message_Type + ", status=" + status + ", beneficiary_Customer_Account=" + beneficiary_Customer_Account + ", value_Currency=" + value_Currency + ", related_Reference=" + related_Reference + ", lastModifiedBy=" + lastModifiedBy + ", transaction_Type_Code=" + transaction_Type_Code + ", ordering_Account=" + ordering_Account + ", senders_Reference=" + senders_Reference + ", network_Delivery_Status=" + network_Delivery_Status + ", partner_code=" + partner_code + ", to_email=" + to_email + ", to_email_status=" + to_email_status + ", to_email_date=" + to_email_date + '}';
    }

}
