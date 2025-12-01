/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel Feb 26, 2021 11:15:52 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sourceAccount",
    "requesterName",
    "accountCurrency",
    "sendingCurrency",
    "beneficiaryAccount",
    "BeneficiaryName",
    "amount",
    "callbackUrl",
    "requestedBy",
    "partnerCode",
    "institutionName",
    "requesterPhone",
    "requesterEmail"
})
@XmlRootElement(name = "fxRequest")
public class FxRequest {

    @XmlElement(name = "sourceAccount", required = true)
    public String sourceAccount;
    @XmlElement(name = "accountCurrency", required = true)
    public String accountCurrency;
    @XmlElement(name = "sendingCurrency", required = true)
    public String SendingCurrency;
    @XmlElement(name = "amount", required = true)
    public String amount;
    @XmlElement(name = "callbackUrl", required = true)
    public String callbackUrl;
    @XmlElement(name = "requestedBy", required = true)
    public String requestedBy;
    @XmlElement(name = "partnerCode", required = true)
    public String partnerCode;
    @XmlElement(name = "institutionName", required = true)
    public String institutionName;
    @XmlElement(name = "requesterPhone", required = true)
    public String requesterPhone;
    @XmlElement(name = "requesterEmail", required = true)
    public String requesterEmail;
        @XmlElement(name = "beneficiaryAccount", required = true)
    public String beneficiaryAccount;
    @XmlElement(name = "BeneficiaryName", required = true)
    public String BeneficiaryName;

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getAccountCurrency() {
        return accountCurrency;
    }

    public void setAccountCurrency(String accountCurrency) {
        this.accountCurrency = accountCurrency;
    }

    public String getSendingCurrency() {
        return SendingCurrency;
    }

    public void setSendingCurrency(String SendingCurrency) {
        this.SendingCurrency = SendingCurrency;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public void setRequesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getBeneficiaryAccount() {
        return beneficiaryAccount;
    }

    public void setBeneficiaryAccount(String beneficiaryAccount) {
        this.beneficiaryAccount = beneficiaryAccount;
    }

    public String getBeneficiaryName() {
        return BeneficiaryName;
    }

    public void setBeneficiaryName(String BeneficiaryName) {
        this.BeneficiaryName = BeneficiaryName;
    }

    @Override
    public String toString() {
        return "FxRequest{" + "sourceAccount=" + sourceAccount + ", accountCurrency=" + accountCurrency + ", SendingCurrency=" + SendingCurrency + ", amount=" + amount + ", callbackUrl=" + callbackUrl + ", requestedBy=" + requestedBy + ", partnerCode=" + partnerCode + ", institutionName=" + institutionName + ", requesterPhone=" + requesterPhone + ", requesterEmail=" + requesterEmail + ", beneficiaryAccount=" + beneficiaryAccount + ", BeneficiaryName=" + BeneficiaryName + '}';
    }

   
}
