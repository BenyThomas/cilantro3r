/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

import java.math.BigDecimal;

/**
 *
 * @author melleji.mollel Feb 17, 2021 12:57:00 AM
 */
public class EftPacs004002TxnsInfoReq {

    public String returnTxnId;
    public String originalMsgId;
    public String originalMsgNmId;
    public String originalInstructionId;
    public String originalEndToEndId;
    public String originalTxid;
    public BigDecimal originalInterBankSettmntAmt;
    public String originalInterBankSettmntCcy;
    public BigDecimal returnedInterBankSettmntAmt;
    public String returnedInterBankSettmntCcy;
    public String chargeBearer;
    public String returnReasonInfomation;
    public String senderName;//sender name
    public String senderBICorBEI;//sender bic
    public String senderAccount;//sender account
    public String senderBIC;//sender bic

    public String beneficiaryBIC;//beneficiary bic
    public String beneficiaryName;//beneficiary name
    public String beneficiaryBICorBEI;//beneficiary bic
    public String beneficiaryAcct;//beneficiary account
    public String txnDate;//beneficiary account

    public String getReturnTxnId() {
        return returnTxnId;
    }

    public void setReturnTxnId(String returnTxnId) {
        this.returnTxnId = returnTxnId;
    }

    public String getOriginalMsgId() {
        return originalMsgId;
    }

    public void setOriginalMsgId(String originalMsgId) {
        this.originalMsgId = originalMsgId;
    }

    public String getOriginalMsgNmId() {
        return originalMsgNmId;
    }

    public void setOriginalMsgNmId(String originalMsgNmId) {
        this.originalMsgNmId = originalMsgNmId;
    }

    public String getOriginalInstructionId() {
        return originalInstructionId;
    }

    public void setOriginalInstructionId(String originalInstructionId) {
        this.originalInstructionId = originalInstructionId;
    }

    public String getOriginalEndToEndId() {
        return originalEndToEndId;
    }

    public void setOriginalEndToEndId(String originalEndToEndId) {
        this.originalEndToEndId = originalEndToEndId;
    }

    public String getOriginalTxid() {
        return originalTxid;
    }

    public void setOriginalTxid(String originalTxid) {
        this.originalTxid = originalTxid;
    }

    public BigDecimal getOriginalInterBankSettmntAmt() {
        return originalInterBankSettmntAmt;
    }

    public void setOriginalInterBankSettmntAmt(BigDecimal originalInterBankSettmntAmt) {
        this.originalInterBankSettmntAmt = originalInterBankSettmntAmt;
    }

    public String getOriginalInterBankSettmntCcy() {
        return originalInterBankSettmntCcy;
    }

    public void setOriginalInterBankSettmntCcy(String originalInterBankSettmntCcy) {
        this.originalInterBankSettmntCcy = originalInterBankSettmntCcy;
    }

    public BigDecimal getReturnedInterBankSettmntAmt() {
        return returnedInterBankSettmntAmt;
    }

    public void setReturnedInterBankSettmntAmt(BigDecimal returnedInterBankSettmntAmt) {
        this.returnedInterBankSettmntAmt = returnedInterBankSettmntAmt;
    }

    public String getReturnedInterBankSettmntCcy() {
        return returnedInterBankSettmntCcy;
    }

    public void setReturnedInterBankSettmntCcy(String returnedInterBankSettmntCcy) {
        this.returnedInterBankSettmntCcy = returnedInterBankSettmntCcy;
    }

    public String getChargeBearer() {
        return chargeBearer;
    }

    public void setChargeBearer(String chargeBearer) {
        this.chargeBearer = chargeBearer;
    }

    public String getReturnReasonInfomation() {
        return returnReasonInfomation;
    }

    public void setReturnReasonInfomation(String returnReasonInfomation) {
        this.returnReasonInfomation = returnReasonInfomation;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderBICorBEI() {
        return senderBICorBEI;
    }

    public void setSenderBICorBEI(String senderBICorBEI) {
        this.senderBICorBEI = senderBICorBEI;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getSenderBIC() {
        return senderBIC;
    }

    public void setSenderBIC(String senderBIC) {
        this.senderBIC = senderBIC;
    }

    public String getBeneficiaryBIC() {
        return beneficiaryBIC;
    }

    public void setBeneficiaryBIC(String beneficiaryBIC) {
        this.beneficiaryBIC = beneficiaryBIC;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryBICorBEI() {
        return beneficiaryBICorBEI;
    }

    public void setBeneficiaryBICorBEI(String beneficiaryBICorBEI) {
        this.beneficiaryBICorBEI = beneficiaryBICorBEI;
    }

    public String getBeneficiaryAcct() {
        return beneficiaryAcct;
    }

    public void setBeneficiaryAcct(String beneficiaryAcct) {
        this.beneficiaryAcct = beneficiaryAcct;
    }

    public String getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(String txnDate) {
        this.txnDate = txnDate;
    }

    @Override
    public String toString() {
        return "EftPacs004002TxnsInfoReq{" + "returnTxnId=" + returnTxnId + ", originalMsgId=" + originalMsgId + ", originalMsgNmId=" + originalMsgNmId + ", originalInstructionId=" + originalInstructionId + ", originalEndToEndId=" + originalEndToEndId + ", originalTxid=" + originalTxid + ", originalInterBankSettmntAmt=" + originalInterBankSettmntAmt + ", originalInterBankSettmntCcy=" + originalInterBankSettmntCcy + ", returnedInterBankSettmntAmt=" + returnedInterBankSettmntAmt + ", returnedInterBankSettmntCcy=" + returnedInterBankSettmntCcy + ", chargeBearer=" + chargeBearer + ", returnReasonInfomation=" + returnReasonInfomation + ", senderName=" + senderName + ", senderBICorBEI=" + senderBICorBEI + ", senderAccount=" + senderAccount + ", senderBIC=" + senderBIC + ", beneficiaryBIC=" + beneficiaryBIC + ", beneficiaryName=" + beneficiaryName + ", beneficiaryBICorBEI=" + beneficiaryBICorBEI + ", beneficiaryAcct=" + beneficiaryAcct + ", txnDate=" + txnDate + '}';
    }

   

}
