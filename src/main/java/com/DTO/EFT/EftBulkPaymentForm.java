/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

import javax.validation.constraints.NotBlank;


/**
 *
 * @author melleji.mollel
 */

public class EftBulkPaymentForm {
  @NotBlank(message = "Please Enter Sender account number")
  public String senderAccount;
  
  @NotBlank(message = "Please Enter Sender Name")
  public String senderName;
  
  @NotBlank(message = "Please Enter Amount")
  public String amount;
  
  @NotBlank(message = "Please Select Debit Mandate")
  public String mandate;
  
  public String getSenderAccount() {
    return this.senderAccount;
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
  
  public String getMandate() {
    return this.mandate;
  }
  
  public void setMandate(String mandate) {
    this.mandate = mandate;
  }
  
  public String toString() {
    return "EftBulkPaymentForm{senderAccount=" + this.senderAccount + ", senderName=" + this.senderName + ", amount=" + this.amount + ", mandate=" + this.mandate + '}';
  }
}
