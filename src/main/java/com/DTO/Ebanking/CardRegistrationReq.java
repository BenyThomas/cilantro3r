/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Ebanking;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 *
 * @author melleji.mollel
 */
public class CardRegistrationReq {

    //@NotBlank(message = "Account number is required")
    private String accountNo;

    //@NotBlank(message = "Customer name is required")
    //@Size(min = 3, max = 21, message = "Customer name must not exceed 100 characters")
    private String customerName;

    //@NotBlank(message = "Customer phone number is required")
    //@Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String customerPhoneNumber;

    //@NotBlank(message = "One-time password is required")
    //@Size(min = 4, max = 6, message = "OTP must be 4 to 6 digits")
    private String oneTimePassword;

    //@NotBlank(message = "PAN is required")
    //@Pattern(regexp = "^[0-9]{16,19}$", message = "PAN must be between 16 and 19 digits")
    private String pan;

    //@NotBlank(message = "Reference is required")
    private String reference;

    //@NotBlank(message = "Terminal ID is required")
    private String terminalId;

    //@NotBlank(message = "Terminal name is required")
    //@Size(min = 4, max = 50,message = "Terminal name must be between 4 and 12 characters")
    private String terminalName;

    //@NotBlank(message = "PAN Expiry Date is required")
   // @Pattern(regexp = "^\\d{2}(0[1-9]|1[0-2])$", message = "Expiry date must be in YYMM format")
    private String panExpireDate;

    public boolean isNotify() {
        return notify;
    }

    @JsonProperty("notify")
    public boolean notify;

    public String getPanExpireDate() {
        return panExpireDate;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public void setPanExpireDate(String panExpireDate) {
        this.panExpireDate = panExpireDate;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }

    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber;
    }

    public String getOneTimePassword() {
        return oneTimePassword;
    }

    public void setOneTimePassword(String oneTimePassword) {
        this.oneTimePassword = oneTimePassword;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    @Override
    public String toString() {
        return "CardRegistrationReq{" + "accountNo=" + accountNo + ", customerName=" + customerName + ", customerPhoneNumber=" + customerPhoneNumber + ", oneTimePassword=" + oneTimePassword + ", pan=" + pan + ", reference=" + reference + ", terminalId=" + terminalId + ", terminalName=" + terminalName + '}';
    }

}
