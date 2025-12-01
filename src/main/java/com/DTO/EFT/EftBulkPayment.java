/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

/**
 *
 * @author melleji.mollel
 */
public class EftBulkPayment {
    public String senderAccount;
    public String amount;
    public String beneficiaryAccount;
    public String beneficiaryName;
    public String beneficiaryBic;
    public String paymentPurpose;
    public EftBulkPayment(String senderAccount, String amount, String beneficiaryAccount, String beneficiaryName, String beneficiaryBic, String paymentPurpose) {
        this.senderAccount = senderAccount;
        this.amount = amount;
        this.beneficiaryAccount = beneficiaryAccount;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryBic = beneficiaryBic;
        this.paymentPurpose = paymentPurpose;
    }

    public String getSenderAccount() {
        return this.senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getBeneficiaryAccount() {
        return this.beneficiaryAccount;
    }

    public void setBeneficiaryAccount(String beneficiaryAccount) {
        this.beneficiaryAccount = beneficiaryAccount;
    }

    public String getBeneficiaryName() {
        return this.beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryBic() {
        return this.beneficiaryBic;
    }

    public void setBeneficiaryBic(String beneficiaryBic) {
        this.beneficiaryBic = beneficiaryBic;
    }

    public String getPaymentPurpose() {
        return this.paymentPurpose;
    }

    public void setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
    }

    public String toString() {
        return "EftBulkPayment{senderAccount=" + this.senderAccount + ", amount=" + this.amount + ", beneficiaryAccount=" + this.beneficiaryAccount + ", beneficiaryName=" + this.beneficiaryName + ", beneficiaryBic=" + this.beneficiaryBic + ", paymentPurpose=" + this.paymentPurpose + '}';
    }
}
