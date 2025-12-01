/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.pension;

import java.math.BigDecimal;
import philae.api.UsRole;

/**
 *
 * @author melleji.mollel
 */
public class PensionPayrollToCoreBanking {

    public String reference;
    public String beneficiaryAccount;
    public String narration;
    public UsRole userRoles;
    public BigDecimal amount;
    public String currency;
    public String beneficiaryName;
    public String batchReference;
    public String trackingNo;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getBeneficiaryAccount() {
        return beneficiaryAccount;
    }

    public void setBeneficiaryAccount(String beneficiaryAccount) {
        this.beneficiaryAccount = beneficiaryAccount;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
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

    public UsRole getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(UsRole userRoles) {
        this.userRoles = userRoles;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    @Override
    public String toString() {
        return "PensionPayrollToCoreBanking{" + "reference=" + reference + ", beneficiaryAccount=" + beneficiaryAccount + ", narration=" + narration + ", userRoles=" + userRoles + ", amount=" + amount + ", currency=" + currency + ", beneficiaryName=" + beneficiaryName + ", batchReference=" + batchReference + ", trackingNo=" + trackingNo + '}';
    }

    

}
