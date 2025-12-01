/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Teller;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author melleji.mollel
 */
public class FundTransferReq {

    public String sourceAcct;
    public String destinationAcct;
    public String reference;
    public String txnType;
    public BigDecimal amount;
    public String senderName;
    public String senderBIC;
    public String currency;
    public String beneficiaryName;
    public String initiatedBy;
    public String status;
    public String description;
    public String branchNo;
    public String ReceiverBIC;
    public String msgType;
    public String senderPhone;
    public String senderAddress;
    public String senderCorrespondent;
    public String bankOperationalCode;
    public String detailsOfCharge;
    private Date tranDate;
    private String swiftMessage;
    
    


    public String getSwiftMessage() {
        return swiftMessage;
    }

    public void setSwiftMessage(String swiftMessage) {
        this.swiftMessage = swiftMessage;
    }

    public String getSourceAcct() {
        return sourceAcct;
    }

    public void setSourceAcct(String sourceAcct) {
        this.sourceAcct = sourceAcct;
    }

    public String getDestinationAcct() {
        return destinationAcct;
    }

    public void setDestinationAcct(String destinationAcct) {
        this.destinationAcct = destinationAcct;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBranchNo() {
        return branchNo;
    }

    public void setBranchNo(String branchNo) {
        this.branchNo = branchNo;
    }

    public String getSenderBIC() {
        return senderBIC;
    }

    public void setSenderBIC(String senderBIC) {
        this.senderBIC = senderBIC;
    }

    public String getReceiverBIC() {
        return ReceiverBIC;
    }

    public void setReceiverBIC(String ReceiverBIC) {
        this.ReceiverBIC = ReceiverBIC;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getSenderCorrespondent() {
        return senderCorrespondent;
    }

    public void setSenderCorrespondent(String senderCorrespondent) {
        this.senderCorrespondent = senderCorrespondent;
    }

    public String getBankOperationalCode() {
        return bankOperationalCode;
    }

    public void setBankOperationalCode(String bankOperationalCode) {
        this.bankOperationalCode = bankOperationalCode;
    }

    public String getDetailsOfCharge() {
        return detailsOfCharge;
    }

    public void setDetailsOfCharge(String detailsOfCharge) {
        this.detailsOfCharge = detailsOfCharge;
    }

    public Date getTranDate() {
        return tranDate;
    }

    public void setTranDate(Date tranDate) {
        this.tranDate = tranDate;
    }

    @Override
    public String toString() {
        return "FundTransferReq{" + "sourceAcct=" + sourceAcct + ", destinationAcct=" + destinationAcct + ", reference=" + reference + ", txnType=" + txnType + ", amount=" + amount + ", senderName=" + senderName + ", senderBIC=" + senderBIC + ", currency=" + currency + ", beneficiaryName=" + beneficiaryName + ", initiatedBy=" + initiatedBy + ", status=" + status + ", description=" + description + ", branchNo=" + branchNo + ", ReceiverBIC=" + ReceiverBIC + ", msgType=" + msgType + ", senderPhone=" + senderPhone + ", senderAddress=" + senderAddress + ", senderCorrespondent=" + senderCorrespondent + ", bankOperationalCode=" + bankOperationalCode + ", detailsOfCharge=" + detailsOfCharge + ", tranDate=" + tranDate + ", swiftMessage=" + swiftMessage + '}';
    }


    
}
