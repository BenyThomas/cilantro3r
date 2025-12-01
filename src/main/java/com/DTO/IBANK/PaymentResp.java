/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "responseCode",
    "reference",
    "receipt",
    "message",
    "availableBalance",
    "ledgerBalance"
})
@XmlRootElement(name = "paymentResponse")
public class PaymentResp {

    @XmlElement(name = "responseCode", required = true)
    public String responseCode;
    @XmlElement(name = "message", required = true)
    public String message;
    @XmlElement(name = "reference", required = true)
    public String reference;
    @XmlElement(name = "availableBalance", required = true)
    public BigDecimal availableBalance;
    @XmlElement(name = "ledgerBalance", required = true)
    public BigDecimal ledgerBalance;
    @XmlElement(name = "receipt", required = true)
    public String receipt;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    @Override
    public String toString() {
        return "PaymentResp{" + "responseCode=" + responseCode + ", message=" + message + ", reference=" + reference + ", availableBalance=" + availableBalance + ", ledgerBalance=" + ledgerBalance + ", receipt=" + receipt + '}';
    }

}
