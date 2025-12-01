/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel
 */
public class AddAccountToIBProfile {

    @NotBlank(message = "Please Enter Account Number")
    public String accountNo;
    @NotBlank(message = "Account name is required")
    public String accountName;
    public String oldAccount;
    @NotBlank(message = "Account Currency")
    public String accountCurrency;
    @NotBlank(message = "Account product code is required")
    public String acctProdCode;
    @NotBlank(message = "Account description is required")
    public String acctDescription;
    @NotBlank(message = "Account category if required")
    public String acctCategory;
    @NotBlank(message = "Account Status is required")
    public String acctStatus;
    @NotBlank(message = "Daily account Limit is required")
    public String acctLimit;
    @NotBlank(message = "LImit without approval is required")
    public String limitWithoutApproval;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getOldAccount() {
        return oldAccount;
    }

    public void setOldAccount(String oldAccount) {
        this.oldAccount = oldAccount;
    }

    public String getAccountCurrency() {
        return accountCurrency;
    }

    public void setAccountCurrency(String accountCurrency) {
        this.accountCurrency = accountCurrency;
    }

    public String getAcctProdCode() {
        return acctProdCode;
    }

    public void setAcctProdCode(String acctProdCode) {
        this.acctProdCode = acctProdCode;
    }

    public String getAcctDescription() {
        return acctDescription;
    }

    public void setAcctDescription(String acctDescription) {
        this.acctDescription = acctDescription;
    }

    public String getAcctCategory() {
        return acctCategory;
    }

    public void setAcctCategory(String acctCategory) {
        this.acctCategory = acctCategory;
    }

    public String getAcctStatus() {
        return acctStatus;
    }

    public void setAcctStatus(String acctStatus) {
        this.acctStatus = acctStatus;
    }

    public String getAcctLimit() {
        return acctLimit;
    }

    public void setAcctLimit(String acctLimit) {
        this.acctLimit = acctLimit;
    }

    public String getLimitWithoutApproval() {
        return limitWithoutApproval;
    }

    public void setLimitWithoutApproval(String limitWithoutApproval) {
        this.limitWithoutApproval = limitWithoutApproval;
    }

    @Override
    public String toString() {
        return "AddAccountToIBProfile{" + "accountNo=" + accountNo + ", accountName=" + accountName + ", oldAccount=" + oldAccount + ", accountCurrency=" + accountCurrency + ", acctProdCode=" + acctProdCode + ", acctDescription=" + acctDescription + ", acctCategory=" + acctCategory + ", acctStatus=" + acctStatus + ", acctLimit=" + acctLimit + ", limitWithoutApproval=" + limitWithoutApproval + '}';
    }
    
}
