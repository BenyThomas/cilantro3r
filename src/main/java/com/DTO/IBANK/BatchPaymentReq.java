/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.math.BigDecimal;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 *
 * @author Melleji.Mollel
 */
@XmlType(name = "", propOrder = {
    "batchReference",
    "noOfTxns",
    "batchType",
    "totalAmt",
    "mandate",
    "sourceAccount",
    "currency",
    "purpose",
    "branchCode",
    "paymentRequest"
})
@XmlRootElement(name = "batchPaymentRequest")
@XmlAccessorType(XmlAccessType.FIELD)

public class BatchPaymentReq {

    @XmlElement(name = "batchReference", required = true)
    public String batchReference;
    @XmlElement(name = "noOfTxns", required = true)
    public String noOfTxns;
    @XmlElement(name = "totalAmt", required = true)
    public BigDecimal totalAmt;
    @XmlElement(name = "mandate", required = true)
    public String mandate;
    @XmlElement(name = "batchType", required = true)
    public String batchType;
    @XmlElement(name = "sourceAccount", required = true)
    public String sourceAccount;
    @XmlElement(name = "branchCode", required = true)
    public String branchCode;
    @XmlElement(name = "purpose", required = true)
    public String purpose;
    @XmlElement(name = "currency", required = true)
    public String currency;
    @XmlElement(name = "paymentRequest", required = true)
    public List<PaymentReq> paymentRequest = null;

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public BigDecimal getTotalAmt() {
        return totalAmt;
    }

    public void setTotalAmt(BigDecimal totalAmt) {
        this.totalAmt = totalAmt;
    }

    public String getMandate() {
        return mandate;
    }

    public void setMandate(String mandate) {
        this.mandate = mandate;
    }

    public List<PaymentReq> getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(List<PaymentReq> paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

  
    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    public String getNoOfTxns() {
        return noOfTxns;
    }

    public void setNoOfTxns(String noOfTxns) {
        this.noOfTxns = noOfTxns;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "BatchPaymentReq{" + "batchReference=" + batchReference + ", noOfTxns=" + noOfTxns + ", totalAmt=" + totalAmt + ", mandate=" + mandate + ", batchType=" + batchType + ", sourceAccount=" + sourceAccount + ", branchCode=" + branchCode + ", purpose=" + purpose + ", currency=" + currency + ", paymentRequest=" + paymentRequest + '}';
    }

}
