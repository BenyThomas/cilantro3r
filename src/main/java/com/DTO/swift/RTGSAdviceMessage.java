/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.swift;

import java.math.BigDecimal;
import java.sql.Date;

/**
 *
 * @author samichael
 */
public class RTGSAdviceMessage {

    private String Cleared = null;
    private String RefNo;
    private String Message_Type;
    private String Sender;
    private String Receiver;
    private String Traffic_Type;
    private String Senders_Reference;
    private String Related_Reference = null;
    private String Transaction_Type_Code = null;
    private String Network_Delivery_Status;
    private String Value_Date;
    private String Value_Currency;
    private BigDecimal Value_Amount;
    private String Service_Code;
    private String Trailer_1 = null;
    private String Trailer_2 = null;
    private String Trailer_3 = null;
    private byte[] PDF;
    private String FileCRC;
    private String Byte_Length = null;
    private String Notification;
    private String MIR = null;
    private String Status;
    private Date Dated;
    private String Timed;
    private String LastModifiedBy;
    private String Ordering_Account;
    private String Beneficiary_Customer_Account;

    // Getter Methods 
    public String getCleared() {
        return Cleared;
    }

    public String getRefNo() {
        return RefNo;
    }

    public String getMessage_Type() {
        return Message_Type;
    }

    public String getSender() {
        return Sender;
    }

    public String getReceiver() {
        return Receiver;
    }

    public String getTraffic_Type() {
        return Traffic_Type;
    }

    public String getSenders_Reference() {
        return Senders_Reference;
    }

    public String getRelated_Reference() {
        return Related_Reference;
    }

    public String getTransaction_Type_Code() {
        return Transaction_Type_Code;
    }

    public String getNetwork_Delivery_Status() {
        return Network_Delivery_Status;
    }

    public String getValue_Date() {
        return Value_Date;
    }

    public String getValue_Currency() {
        return Value_Currency;
    }

    public BigDecimal getValue_Amount() {
        return Value_Amount;
    }

    public String getService_Code() {
        return Service_Code;
    }

    public String getTrailer_1() {
        return Trailer_1;
    }

    public String getTrailer_2() {
        return Trailer_2;
    }

    public String getTrailer_3() {
        return Trailer_3;
    }

    public byte[] getPDF() {
        return PDF;
    }

    public String getFileCRC() {
        return FileCRC;
    }

    public String getByte_Length() {
        return Byte_Length;
    }

    public String getNotification() {
        return Notification;
    }

    public String getMIR() {
        return MIR;
    }

    public String getStatus() {
        return Status;
    }

    public Date getDated() {
        return Dated;
    }

    public String getTimed() {
        return Timed;
    }

    public String getLastModifiedBy() {
        return LastModifiedBy;
    }

    public String getOrdering_Account() {
        return Ordering_Account;
    }

    public String getBeneficiary_Customer_Account() {
        return Beneficiary_Customer_Account;
    }

    // Setter Methods 
    public void setCleared(String Cleared) {
        this.Cleared = Cleared;
    }

    public void setRefNo(String RefNo) {
        this.RefNo = RefNo;
    }

    public void setMessage_Type(String Message_Type) {
        this.Message_Type = Message_Type;
    }

    public void setSender(String Sender) {
        this.Sender = Sender;
    }

    public void setReceiver(String Receiver) {
        this.Receiver = Receiver;
    }

    public void setTraffic_Type(String Traffic_Type) {
        this.Traffic_Type = Traffic_Type;
    }

    public void setSenders_Reference(String Senders_Reference) {
        this.Senders_Reference = Senders_Reference;
    }

    public void setRelated_Reference(String Related_Reference) {
        this.Related_Reference = Related_Reference;
    }

    public void setTransaction_Type_Code(String Transaction_Type_Code) {
        this.Transaction_Type_Code = Transaction_Type_Code;
    }

    public void setNetwork_Delivery_Status(String Network_Delivery_Status) {
        this.Network_Delivery_Status = Network_Delivery_Status;
    }

    public void setValue_Date(String Value_Date) {
        this.Value_Date = Value_Date;
    }

    public void setValue_Currency(String Value_Currency) {
        this.Value_Currency = Value_Currency;
    }

    public void setValue_Amount(BigDecimal Value_Amount) {
        this.Value_Amount = Value_Amount;
    }

    public void setService_Code(String Service_Code) {
        this.Service_Code = Service_Code;
    }

    public void setTrailer_1(String Trailer_1) {
        this.Trailer_1 = Trailer_1;
    }

    public void setTrailer_2(String Trailer_2) {
        this.Trailer_2 = Trailer_2;
    }

    public void setTrailer_3(String Trailer_3) {
        this.Trailer_3 = Trailer_3;
    }

    public void setPDF(byte[] PDF) {
        this.PDF = PDF;
    }

    public void setFileCRC(String FileCRC) {
        this.FileCRC = FileCRC;
    }

    public void setByte_Length(String Byte_Length) {
        this.Byte_Length = Byte_Length;
    }

    public void setNotification(String Notification) {
        this.Notification = Notification;
    }

    public void setMIR(String MIR) {
        this.MIR = MIR;
    }

    public void setStatus(String Status) {
        this.Status = Status;
    }

    public void setDated(Date Dated) {
        this.Dated = Dated;
    }

    public void setTimed(String Timed) {
        this.Timed = Timed;
    }

    public void setLastModifiedBy(String LastModifiedBy) {
        this.LastModifiedBy = LastModifiedBy;
    }

    public void setOrdering_Account(String Ordering_Account) {
        this.Ordering_Account = Ordering_Account;
    }

    public void setBeneficiary_Customer_Account(String Beneficiary_Customer_Account) {
        this.Beneficiary_Customer_Account = Beneficiary_Customer_Account;
    }
}
