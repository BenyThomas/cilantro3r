/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 *
 * @author melleji.mollel
 */
public class EftBulkPaymentReq {

    @Pattern(regexp = "^([a-zA-Z0-9]{1,35})", message = "Invalid sender account")
    public String senderAccount;
    @Pattern(regexp = "^\\d+(\\d+)*(\\.\\d+?)?$", message = "Invalid amount")
    public String amount;
    @Pattern(regexp = "^([a-zA-Z0-9]{1,35})", message = "Invalid beneficiary account")
    public String beneficiaryAccount;
    @NotBlank
    public String beneficiaryName;
    @NotBlank
    public String beneficiaryBic;
    @NotBlank
    public String paymentPurpose;

    public EftBulkPaymentReq(String senderAccount, String amount, String beneficiaryAccount, String beneficiaryName, String beneficiaryBic, String paymentPurpose) {
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
        return this.senderAccount + "," + this.amount + "," + this.beneficiaryAccount + "," + this.beneficiaryName + "," + this.beneficiaryBic + "," + this.paymentPurpose;
    }

    public Boolean checkIfEmpty() {
        return this.senderAccount.isEmpty() && this.amount.isEmpty() && this.beneficiaryAccount.isEmpty()
                && this.beneficiaryName.isEmpty() && this.beneficiaryBic.isEmpty() && this.paymentPurpose.isEmpty();
    }
}
