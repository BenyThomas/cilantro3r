/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

import java.math.BigDecimal;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author melleji.mollel
 */
public class EftOutgoingBulkPaymentsReq {

    public String instrId;//transaction instruction id
    public String endToEndId;//transaction end to end id
    public String txId;//transaction id
    public String svcLvlCd;//SEPA
    public BigDecimal amount;//sending amount
    public String currency;//sedning currency
    public String chrgBr;//charge agreement
    
    public String senderName;//sender name
    public String senderBICorBEI;//sender bic
    public String senderAccount;//sender account
    public String senderBIC;//sender bic
    
    public String beneficiaryBIC;//beneficiary bic
    public String beneficiaryName;//beneficiary name
    public String beneficiaryBICorBEI;//beneficiary bic
    public String beneficiaryAcct;//beneficiary account
    
    public String purpose;//purpose of payments

    public String getInstrId() {
        return instrId;
    }

    public void setInstrId(String instrId) {
        this.instrId = instrId;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getSvcLvlCd() {
        return svcLvlCd;
    }

    public void setSvcLvlCd(String svcLvlCd) {
        this.svcLvlCd = svcLvlCd;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getChrgBr() {
        return chrgBr;
    }

    public void setChrgBr(String chrgBr) {
        this.chrgBr = chrgBr;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @Override
    public String toString() {
        return "EftPacs00800102CdtTrfTxInf{" + "instrId=" + instrId + ", endToEndId=" + endToEndId + ", txId=" + txId + ", svcLvlCd=" + svcLvlCd + ", amount=" + amount + ", currency=" + currency + ", chrgBr=" + chrgBr + ", senderName=" + senderName + ", senderBICorBEI=" + senderBICorBEI + ", senderAccount=" + senderAccount + ", senderBIC=" + senderBIC + ", beneficiaryBIC=" + beneficiaryBIC + ", beneficiaryName=" + beneficiaryName + ", beneficiaryBICorBEI=" + beneficiaryBICorBEI + ", beneficiaryAcct=" + beneficiaryAcct + ", purpose=" + purpose + '}';
    }
}
