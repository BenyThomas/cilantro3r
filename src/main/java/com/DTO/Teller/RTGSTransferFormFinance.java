/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Teller;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel
 */
public class RTGSTransferFormFinance {
  @NotBlank(message = "Please Enter  GL account (source of Fund)")
  public String senderAccount;
  
  @NotBlank(message = "Please Enter Sender Name")
  public String senderName;
  
  @NotBlank(message = "Please Enter Amount (With/Without VAT) i.e Total amount to Service Provider")
  public String amount;
  
  @NotBlank(message = "Please Select paying currency")
  public String currency;
  
  @NotBlank(message = "Please Enter Beneficiary account number")
  public String beneficiaryAccount;
  
  @NotBlank(message = "Please Enter Beneficiary Name")
  public String beneficiaryName;
  
  @NotBlank(message = "Please Select Beneficiary Bank")
  public String beneficiaryBIC;
  
  @NotBlank(message = "Please Enter Sender Address")
  public String senderAddress;
  
  @NotBlank(message = "Please Enter Beneficiary Contact")
  public String beneficiaryContact;
  
  @NotBlank(message = "Please Enter Sender Phone")
  public String senderPhone;
  
  @NotBlank(message = "Please Enter Payment purpose")
  public String description;
  
  @NotBlank(message = "Please Enter amount that can be used to compute with Holding tax")
  public String taxableAmount;
  
  @NotBlank(message = "Please Select  Tax Category")
  public String taxRate;
  
  public String intermediaryBank;
  
  public String getSenderAccount() {
    return this.senderAccount;
  }
  
  public String getIntermediaryBank() {
    return this.intermediaryBank;
  }
  
  public void setIntermediaryBank(String intermediaryBank) {
    this.intermediaryBank = intermediaryBank;
  }
  
  public void setSenderAccount(String senderAccount) {
    this.senderAccount = senderAccount;
  }
  
  public String getSenderName() {
    return this.senderName;
  }
  
  public void setSenderName(String senderName) {
    this.senderName = senderName;
  }
  
  public String getAmount() {
    return this.amount;
  }
  
  public void setAmount(String amount) {
    this.amount = amount;
  }
  
  public String getCurrency() {
    return this.currency;
  }
  
  public void setCurrency(String currency) {
    this.currency = currency;
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
  
  public String getBeneficiaryBIC() {
    return this.beneficiaryBIC;
  }
  
  public void setBeneficiaryBIC(String beneficiaryBIC) {
    this.beneficiaryBIC = beneficiaryBIC;
  }
  
  public String getSenderAddress() {
    return this.senderAddress;
  }
  
  public void setSenderAddress(String senderAddress) {
    this.senderAddress = senderAddress;
  }
  
  public String getBeneficiaryContact() {
    return this.beneficiaryContact;
  }
  
  public void setBeneficiaryContact(String beneficiaryContact) {
    this.beneficiaryContact = beneficiaryContact;
  }
  
  public String getSenderPhone() {
    return this.senderPhone;
  }
  
  public void setSenderPhone(String senderPhone) {
    this.senderPhone = senderPhone;
  }
  
  public String getDescription() {
    return this.description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getTaxableAmount() {
    return this.taxableAmount;
  }
  
  public void setTaxableAmount(String taxableAmount) {
    this.taxableAmount = taxableAmount;
  }
  
  public String getTaxRate() {
    return this.taxRate;
  }
  
  public void setTaxRate(String taxRate) {
    this.taxRate = taxRate;
  }
  
  public String toString() {
    return "RTGSTransferFormFinance{senderAccount=" + this.senderAccount + ", senderName=" + this.senderName + ", amount=" + this.amount + ", currency=" + this.currency + ", beneficiaryAccount=" + this.beneficiaryAccount + ", beneficiaryName=" + this.beneficiaryName + ", beneficiaryBIC=" + this.beneficiaryBIC + ", senderAddress=" + this.senderAddress + ", beneficiaryContact=" + this.beneficiaryContact + ", senderPhone=" + this.senderPhone + ", description=" + this.description + ", taxableAmount=" + this.taxableAmount + ", taxRate=" + this.taxRate + ", intermediaryBank=" + this.intermediaryBank + '}';
  }
}