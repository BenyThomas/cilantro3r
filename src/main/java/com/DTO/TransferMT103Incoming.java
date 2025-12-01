/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

/**
 *
 * @author melleji.mollel
 */
public class TransferMT103Incoming {

    private String senderBic;
    private String receiverBic;
    private String msgType; //TISS OR EFT use TISS
    private String bankRef;
    private String bankOperationCode; //CRED
    private String tranDate;
    private String currency;
    private String tranAmount;
    private String senderAccount;
    private String senderName;
    protected String senderPhoneNo;
    protected String senderEmailAddress;
    private String senderCorrespondent;
    private String receiverAccount;
    private String receiverName;
    private String tranDesc;
    private String detailOfCharge; //OUR
    private String isLocalOrInternational; //OUR
    private String contraAccount; //OUR
    private String chargeScheme; //OUR

    public String getSenderBic() {
        return senderBic;
    }

    public void setSenderBic(String senderBic) {
        this.senderBic = senderBic;
    }

    public String getReceiverBic() {
        return receiverBic;
    }

    public void setReceiverBic(String receiverBic) {
        this.receiverBic = receiverBic;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getBankRef() {
        return bankRef;
    }

    public void setBankRef(String bankRef) {
        this.bankRef = bankRef;
    }

    public String getBankOperationCode() {
        return bankOperationCode;
    }

    public void setBankOperationCode(String bankOperationCode) {
        this.bankOperationCode = bankOperationCode;
    }

    public String getTranDate() {
        return tranDate;
    }

    public void setTranDate(String tranDate) {
        this.tranDate = tranDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTranAmount() {
        return tranAmount;
    }

    public void setTranAmount(String tranAmount) {
        this.tranAmount = tranAmount;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhoneNo() {
        return senderPhoneNo;
    }

    public void setSenderPhoneNo(String senderPhoneNo) {
        this.senderPhoneNo = senderPhoneNo;
    }

    public String getSenderEmailAddress() {
        return senderEmailAddress;
    }

    public void setSenderEmailAddress(String senderEmailAddress) {
        this.senderEmailAddress = senderEmailAddress;
    }

    public String getSenderCorrespondent() {
        return senderCorrespondent;
    }

    public void setSenderCorrespondent(String senderCorrespondent) {
        this.senderCorrespondent = senderCorrespondent;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getTranDesc() {
        return tranDesc;
    }

    public void setTranDesc(String tranDesc) {
        this.tranDesc = tranDesc;
    }

    public String getDetailOfCharge() {
        return detailOfCharge;
    }

    public void setDetailOfCharge(String detailOfCharge) {
        this.detailOfCharge = detailOfCharge;
    }

    public String getIsLocalOrInternational() {
        return isLocalOrInternational;
    }

    public void setIsLocalOrInternational(String isLocalOrInternational) {
        this.isLocalOrInternational = isLocalOrInternational;
    }

    public String getContraAccount() {
        return contraAccount;
    }

    public void setContraAccount(String contraAccount) {
        this.contraAccount = contraAccount;
    }

    public String getChargeScheme() {
        return chargeScheme;
    }

    public void setChargeScheme(String chargeScheme) {
        this.chargeScheme = chargeScheme;
    }

}
