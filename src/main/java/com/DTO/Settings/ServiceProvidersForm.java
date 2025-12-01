/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Settings;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author melleji.mollel Feb 16, 2021 12:08:30 PM
 */
public class ServiceProvidersForm {

    @NotBlank(message = "Please Enter Service Providers Name")
    public String spName;

    @NotBlank(message = "Please Enter Service Providers Address")
    public String spAddress;

    @NotBlank(message = "Please Enter Service providers Contact Phone Number")
    public String spPhone;

    @NotBlank(message = "Please Select Service Provider's Bank")
    public String spBankSwiftCode;

    @NotBlank(message = "Please Enter Service Providers Account Number")
    public String spBankAccount;

    @NotBlank(message = "Please Enter Service Providers Activities/Facility with the Bank")
    public String spFacility;

    public String intermediaryBank;

    public String identifier;

    public String getSpName() {
        return this.spName;
    }

    public void setSpName(String spName) {
        this.spName = spName;
    }

    public String getSpAddress() {
        return this.spAddress;
    }

    public void setSpAddress(String spAddress) {
        this.spAddress = spAddress;
    }

    public String getSpPhone() {
        return this.spPhone;
    }

    public void setSpPhone(String spPhone) {
        this.spPhone = spPhone;
    }

    public String getSpBankSwiftCode() {
        return this.spBankSwiftCode;
    }

    public void setSpBankSwiftCode(String spBankSwiftCode) {
        this.spBankSwiftCode = spBankSwiftCode;
    }

    public String getSpBankAccount() {
        return this.spBankAccount;
    }

    public void setSpBankAccount(String spBankAccount) {
        this.spBankAccount = spBankAccount;
    }

    public String getSpFacility() {
        return this.spFacility;
    }

    public void setSpFacility(String spFacility) {
        this.spFacility = spFacility;
    }

    public String getIntermediaryBank() {
        return this.intermediaryBank;
    }

    public void setIntermediaryBank(String intermediaryBank) {
        this.intermediaryBank = intermediaryBank;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String toString() {
        return "ServiceProvidersForm{spName=" + this.spName + ", spAddress=" + this.spAddress + ", spPhone=" + this.spPhone + ", spBankSwiftCode=" + this.spBankSwiftCode + ", spBankAccount=" + this.spBankAccount + ", spFacility=" + this.spFacility + ", intermediaryBank=" + this.intermediaryBank + ", identifier=" + this.identifier + '}';
    }
}
