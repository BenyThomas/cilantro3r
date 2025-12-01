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
 * @author melleji.mollel Mar 1, 2021 8:57:30 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "responseCode",
    "message",
    "availableBalance",
    "ledgerBalance"
})
@XmlRootElement(name = "fxResponse")
public class FxResponse {

    @XmlElement(name = "responseCode", required = true)
    public String responseCode;
    @XmlElement(name = "message", required = true)
    public String message;
    @XmlElement(name = "availableBalance", required = true)
    public String availableBalance;
    @XmlElement(name = "ledgerBalance", required = true)
    public String ledgerBalance;

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

    public String getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(String availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(String ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    @Override
    public String toString() {
        return "FxResponse{" + "responseCode=" + responseCode + ", message=" + message + ", availableBalance=" + availableBalance + ", ledgerBalance=" + ledgerBalance + '}';
    }

}
