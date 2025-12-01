/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.math.BigDecimal;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "fxToken",
    "senderAccount",
    "amount",
    "sendingCurrency",})
@XmlRootElement(name = "fxValidationRequest")
public class FxValidationReq {

    @XmlElement(name = "fxToken", required = true)
    public String fxToken;
    @XmlElement(name = "senderAccount", required = true)
    public String senderAccount;
    @XmlElement(name = "amount", required = true)
    public BigDecimal amount;
    @XmlElement(name = "sendingCurrency", required = true)
    public String sendingCurrency;

    public String getFxToken() {
        return fxToken;
    }

    public void setFxToken(String fxToken) {
        this.fxToken = fxToken;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSendingCurrency() {
        return sendingCurrency;
    }

    public void setSendingCurrency(String sendingCurrency) {
        this.sendingCurrency = sendingCurrency;
    }

    @Override
    public String toString() {
        return "FxValidation{" + "fxToken=" + fxToken + ", senderAccount=" + senderAccount + ", amount=" + amount + ", sendingCurrency=" + sendingCurrency + '}';
    }

}
