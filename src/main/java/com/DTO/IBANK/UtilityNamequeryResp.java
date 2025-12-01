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
 * @author melleji.mollel
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "responseCode",
    "customerName",
    "message",
    "utilityCode"
})
@XmlRootElement(name = "namequeryResp")
public class UtilityNamequeryResp {

    @XmlElement(name = "responseCode", required = true)
    public String responseCode;
    @XmlElement(name = "customerName", required = true)
    public String customerName;
    @XmlElement(name = "message", required = true)
    public String message;
    @XmlElement(name = "utilityCode", required = true)
    public String utilityCode;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUtilityCode() {
        return utilityCode;
    }

    public void setUtilityCode(String utilityCode) {
        this.utilityCode = utilityCode;
    }

    @Override
    public String toString() {
        return "namequeryResp{" + "responseCode=" + responseCode + ", customerName=" + customerName + ", message=" + message + ", utilityCode=" + utilityCode + '}';
    }

}
