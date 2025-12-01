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
public class EftPacs004002OrgnlTxRef {

    public String senderName;//sender name
    public String senderBICorBEI;//sender bic
    public String senderAccount;//sender account
    public String senderBIC;//sender bic

    public String beneficiaryBIC;//beneficiary bic
    public String beneficiaryName;//beneficiary name
    public String beneficiaryBICorBEI;//beneficiary bic
    public String beneficiaryAcct;//beneficiary account

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

    @Override
    public String toString() {
        return "EftPacs004002OrgnlTxRef{" + "senderName=" + senderName + ", senderBICorBEI=" + senderBICorBEI + ", senderAccount=" + senderAccount + ", senderBIC=" + senderBIC + ", beneficiaryBIC=" + beneficiaryBIC + ", beneficiaryName=" + beneficiaryName + ", beneficiaryBICorBEI=" + beneficiaryBICorBEI + ", beneficiaryAcct=" + beneficiaryAcct + '}';
    }
    
}
