/*
 * To change this license header; public choose License Headers in Project Properties.
 * To change this template file; public choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.Teller;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 *
 * @author melleji.mollel #
 */
public class RTGSTransferForm {

    @NotBlank(message = "Please Enter Sender account number")
    public String senderAccount;
    @NotBlank(message = "Please Enter Sender Name")
    public String senderName;
    @NotEmpty
    @Pattern(regexp = "^(\\d+(?:[\\.\\,]\\d{0,4})?)", message = "Invalid amount")
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
    public String intermediaryBank;
    public String currencyConversion;
    public String rubikonRate;
    public String requestingRate;
    public String batchReference;
    public String fxType;
    public String transactionType;
    public String senderBic;
    public String reference;
    public String relatedReference;
    public String transactionDate;
    public String messageType;
    public String swiftMessage;
    public String chargeDetails;
    public String channel;
    public String comments;
    public String message;
    public String responseCode;
    public String correspondentBic;
    public String createDt;

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

    public String getCurrencyConversion() {
        return currencyConversion;
    }

    public void setCurrencyConversion(String currencyConversion) {
        this.currencyConversion = currencyConversion;
    }

    public String getRubikonRate() {
        return rubikonRate;
    }

    public void setRubikonRate(String rubikonRate) {
        this.rubikonRate = rubikonRate;
    }

    public String getRequestingRate() {
        return requestingRate;
    }

    public void setRequestingRate(String requestingRate) {
        this.requestingRate = requestingRate;
    }

    public String getFxType() {
        return fxType;
    }

    public void setFxType(String fxType) {
        this.fxType = fxType;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSenderBic() {
        return senderBic;
    }

    public void setSenderBic(String senderBic) {
        this.senderBic = senderBic;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getRelatedReference() {
        return relatedReference;
    }

    public void setRelatedReference(String relatedReference) {
        this.relatedReference = relatedReference;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSwiftMessage() {
        return swiftMessage;
    }

    public void setSwiftMessage(String swiftMessage) {
        this.swiftMessage = swiftMessage;
    }

    public String getChargeDetails() {
        return chargeDetails;
    }

    public void setChargeDetails(String chargeDetails) {
        this.chargeDetails = chargeDetails;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getCorrespondentBic() {
        return correspondentBic;
    }

    public void setCorrespondentBic(String correspondentBic) {
        this.correspondentBic = correspondentBic;
    }

    public String getCreateDt(){
        return createDt;
    }
    public void setCreateDt(String createDt){
        this.createDt = createDt;
    }

    @Override
    public String toString() {
        return "RTGSTransferForm{" + "senderAccount=" + senderAccount + ", senderName=" + senderName + ", amount=" + amount + ", currency=" + currency + ", beneficiaryAccount=" + beneficiaryAccount + ", beneficiaryName=" + beneficiaryName + ", beneficiaryBIC=" + beneficiaryBIC + ", senderAddress=" + senderAddress + ", beneficiaryContact=" + beneficiaryContact + ", senderPhone=" + senderPhone + ", description=" + description + ", intermediaryBank=" + intermediaryBank + ", currencyConversion=" + currencyConversion + ", rubikonRate=" + rubikonRate + ", requestingRate=" + requestingRate + ", batchReference=" + batchReference + ", fxType=" + fxType + ", transactionType=" + transactionType + ", senderBic=" + senderBic + ", reference=" + reference + ", relatedReference=" + relatedReference + ", transactionDate=" + transactionDate + ", messageType=" + messageType + ", swiftMessage=" + swiftMessage + ", chargeDetails=" + chargeDetails + ", channel=" + channel + ", comments=" + comments + ", message=" + message + ", responseCode=" + responseCode + ", correspondentBic=" + correspondentBic + '}';
    }

   

}
