/*
 * To change this license header; public choose License Headers in Project Properties.
 * To change this template file; public choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.math.BigDecimal;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel #
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "paymentRequest", propOrder = {
    "reference",
    "senderAccount",
    "batchReference",
    "senderName",
    "senderPhone",
    "senderAddress",
    "amount",
    "currency",
    "beneficiaryAccount",
    "beneficiaryName",
    "beneficiaryBIC",
    "beneficiaryContact",
    "description",
    "intermediaryBank",
    "specialRateToken",
    "initiatorId",
    "chargeCategory",
    "type",
    "customerBranch",
    "boundary",
    "callbackUrl"
})
public class PaymentBatchEntriesReq {

    @XmlElement(name = "reference", required = true)
    public String reference;
    @XmlElement(name = "senderAccount", required = true)
    public String senderAccount;
    @XmlElement(name = "senderName", required = true)
    public String senderName;
    @XmlElement(name = "amount", required = true)
    public BigDecimal amount;
    @XmlElement(name = "currency", required = true)
    public String currency;
    @XmlElement(name = "beneficiaryAccount", required = true)
    public String beneficiaryAccount;
    @XmlElement(name = "beneficiaryName", required = true)
    public String beneficiaryName;
    @XmlElement(name = "beneficiaryBIC", required = true)
    public String beneficiaryBIC;
    @XmlElement(name = "senderAddress", required = true)
    public String senderAddress;
    @XmlElement(name = "beneficiaryContact", required = true)
    public String beneficiaryContact;
    @XmlElement(name = "senderPhone", required = true)
    public String senderPhone;
    @XmlElement(name = "description", required = true)
    public String description;
    @XmlElement(name = "intermediaryBank", required = true)
    public String intermediaryBank;
    @XmlElement(name = "specialRateToken", required = true)
    public String specialRateToken;
    @XmlElement(name = "initiatorId", required = true)
    public String initiatorId;
    @XmlElement(name = "chargeCategory", required = true)
    public String chargeCategory;
    @XmlElement(name = "type", required = true)
    public String type;//is EFT/RTGS/
    @XmlElement(name = "boundary", required = true)
    public String boundary;
    @XmlElement(name = "callbackUrl", required = true)
    public String callbackUrl;
    @XmlElement(name = "customerBranch", required = true)
    public String customerBranch;
    @XmlElement(name = "batchReference", required = true)
    public String batchReference;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
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

    public String getBeneficiaryAccount() {
        return beneficiaryAccount;
    }

    public void setBeneficiaryAccount(String beneficiaryAccount) {
        this.beneficiaryAccount = beneficiaryAccount;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryBIC() {
        return beneficiaryBIC;
    }

    public void setBeneficiaryBIC(String beneficiaryBIC) {
        this.beneficiaryBIC = beneficiaryBIC;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getBeneficiaryContact() {
        return beneficiaryContact;
    }

    public void setBeneficiaryContact(String beneficiaryContact) {
        this.beneficiaryContact = beneficiaryContact;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIntermediaryBank() {
        return intermediaryBank;
    }

    public void setIntermediaryBank(String intermediaryBank) {
        this.intermediaryBank = intermediaryBank;
    }

    public String getSpecialRateToken() {
        return specialRateToken;
    }

    public void setSpecialRateToken(String specialRateToken) {
        this.specialRateToken = specialRateToken;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getChargeCategory() {
        return chargeCategory;
    }

    public void setChargeCategory(String chargeCategory) {
        this.chargeCategory = chargeCategory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBoundary() {
        return boundary;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCustomerBranch() {
        return customerBranch;
    }

    public void setCustomerBranch(String customerBranch) {
        this.customerBranch = customerBranch;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    @Override
    public String toString() {
        return "PaymentReq{" + "reference=" + reference + ", senderAccount=" + senderAccount + ", senderName=" + senderName + ", amount=" + amount + ", currency=" + currency + ", beneficiaryAccount=" + beneficiaryAccount + ", beneficiaryName=" + beneficiaryName + ", beneficiaryBIC=" + beneficiaryBIC + ", senderAddress=" + senderAddress + ", beneficiaryContact=" + beneficiaryContact + ", senderPhone=" + senderPhone + ", description=" + description + ", intermediaryBank=" + intermediaryBank + ", specialRateToken=" + specialRateToken + ", initiatorId=" + initiatorId + ", chargeCategory=" + chargeCategory + ", type=" + type + ", boundary=" + boundary + ", callbackUrl=" + callbackUrl + ", customerBranch=" + customerBranch + ", batchReference=" + batchReference + '}';
    }

}
