/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

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
@XmlType(name = "utilityNamequeryReq", propOrder = {
    "utilityCode",
    "smartCard",
    "reference"
})
@XmlRootElement(name = "utilityNamequeryReq")
public class UtilityNamequeryReq {

    @XmlElement(name = "utilityCode", required = true)
    public String utilityCode;
    @XmlElement(name = "smartCard", required = true)
    public String smartCard;
    @XmlElement(name = "reference", required = true)
    public String reference;

    public String getUtilityCode() {
        return utilityCode;
    }

    public void setUtilityCode(String utilityCode) {
        this.utilityCode = utilityCode;
    }

    public String getSmartCard() {
        return smartCard;
    }

    public void setSmartCard(String smartCard) {
        this.smartCard = smartCard;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "UtilityNamequery{" + "utilityCode=" + utilityCode + ", smartCard=" + smartCard + '}';
    }

}
