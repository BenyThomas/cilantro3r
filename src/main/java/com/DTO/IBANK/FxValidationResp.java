/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.math.BigDecimal;
import java.util.Date;
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
    "responseCode",
    "approvedEchangeRate",
    "systemRate",
    "expireDate",
    "message"
})
@XmlRootElement(name = "fxValidationResponse")
public class FxValidationResp {

    @XmlElement(name = "responseCode", required = true)
    public String responseCode;
    @XmlElement(name = "approvedEchangeRate", required = true)
    public BigDecimal approvedEchangeRate;
    @XmlElement(name = "message", required = true)
    public String message;
    public BigDecimal systemRate;
    public Date expireDate;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public BigDecimal getApprovedEchangeRate() {
        return approvedEchangeRate;
    }

    public void setApprovedEchangeRate(BigDecimal approvedEchangeRate) {
        this.approvedEchangeRate = approvedEchangeRate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getSystemRate() {
        return systemRate;
    }

    public void setSystemRate(BigDecimal systemRate) {
        this.systemRate = systemRate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    @Override
    public String toString() {
        return "FxValidationResp{" + "responseCode=" + responseCode + ", approvedEchangeRate=" + approvedEchangeRate + ", message=" + message + ", systemRate=" + systemRate + ", expireDate=" + expireDate + '}';
    }

}
