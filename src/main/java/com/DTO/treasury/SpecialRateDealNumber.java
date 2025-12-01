/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.treasury;

import javax.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel
 */
public class SpecialRateDealNumber {

    @NotBlank(message = "Please Enter Sender account Number")
    public String senderAccount;
    @NotBlank(message = "Please Enter Sender Account Name")
    public String accountName;
    @NotBlank(message = "Please Enter Transaction Amount")
    public String amount;
    @NotBlank(message = "Please Enter transaction currency")
    public String currency;
    @NotBlank(message = "Please Enter FX Type")
    public String fxType;
    @NotBlank(message = "Please Enter Currency conversion ")
    public String currencyConversion;
    @NotBlank(message = "Please Enter Rubikon Debit Rate")
    public String rubikonDebitRate;
    @NotBlank(message = "Please Enter Rubikon Credit Rate")
    public String rubikonCreditRate;
    @NotBlank(message = "Please Enter Requested debit rate Number")
    public String requestedDebitRate;
    @NotBlank(message = "Please Enter Requested Credit rate Number")
    public String requestedCreditRate;
    @NotBlank(message = "Please Enter Special Expire Time ")
    public String validsTo;
    @NotBlank(message = "Please Enter customer phone number to be used to deliver DEAL number upon approval")
    public String phone;
    public String email;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getFxType() {
        return fxType;
    }

    public void setFxType(String fxType) {
        this.fxType = fxType;
    }

    public String getCurrencyConversion() {
        return currencyConversion;
    }

    public void setCurrencyConversion(String currencyConversion) {
        this.currencyConversion = currencyConversion;
    }

    public String getRubikonDebitRate() {
        return rubikonDebitRate;
    }

    public void setRubikonDebitRate(String rubikonDebitRate) {
        this.rubikonDebitRate = rubikonDebitRate;
    }

    public String getRubikonCreditRate() {
        return rubikonCreditRate;
    }

    public void setRubikonCreditRate(String rubikonCreditRate) {
        this.rubikonCreditRate = rubikonCreditRate;
    }

    public String getRequestedDebitRate() {
        return requestedDebitRate;
    }

    public void setRequestedDebitRate(String requestedDebitRate) {
        this.requestedDebitRate = requestedDebitRate;
    }

    public String getRequestedCreditRate() {
        return requestedCreditRate;
    }

    public void setRequestedCreditRate(String requestedCreditRate) {
        this.requestedCreditRate = requestedCreditRate;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getValidsTo() {
        return validsTo;
    }

    public void setValidsTo(String validsTo) {
        this.validsTo = validsTo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "SpecialRateDealNumber{" + "senderAccount=" + senderAccount + ", accountName=" + accountName + ", amount=" + amount + ", currency=" + currency + ", fxType=" + fxType + ", currencyConversion=" + currencyConversion + ", rubikonDebitRate=" + rubikonDebitRate + ", rubikonCreditRate=" + rubikonCreditRate + ", requestedDebitRate=" + requestedDebitRate + ", requestedCreditRate=" + requestedCreditRate + ", validsTo=" + validsTo + ", phone=" + phone + ", email=" + email + '}';
    }

}
