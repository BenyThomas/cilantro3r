/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

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
    "TRANSID",
    "RECEPT",
    "METER",
    "OWNER",
    "TOKEN",
    "UNITS",
    "AMOUNT",
    "TAX",
    "FIXED",
    "STATCODE",
    "STATUS"
})
@XmlRootElement(name = "LUKU")
public class LukuPaymentCallBack {

    @XmlElement(name = "TRANSID", required = true)
    public String TRANSID;
    @XmlElement(name = "RECEPT", required = true)
    public String RECEPT;
    @XmlElement(name = "METER", required = true)
    public String METER;
    @XmlElement(name = "OWNER", required = true)
    public String OWNER;
    @XmlElement(name = "TOKEN", required = true)
    public String TOKEN;
    @XmlElement(name = "UNITS", required = true)
    public String UNITS;
    @XmlElement(name = "AMOUNT", required = true)
    public String AMOUNT;
    @XmlElement(name = "TAX", required = true)
    public String TAX;
    @XmlElement(name = "FIXED", required = true)
    public String FIXED;
    @XmlElement(name = "STATCODE", required = true)
    public String STATCODE;
    @XmlElement(name = "STATUS", required = true)
    public String STATUS;

    public String getTRANSID() {
        return TRANSID;
    }

    public void setTRANSID(String TRANSID) {
        this.TRANSID = TRANSID;
    }

    public String getRECEPT() {
        return RECEPT;
    }

    public void setRECEPT(String RECEPT) {
        this.RECEPT = RECEPT;
    }

    public String getMETER() {
        return METER;
    }

    public void setMETER(String METER) {
        this.METER = METER;
    }

    public String getOWNER() {
        return OWNER;
    }

    public void setOWNER(String OWNER) {
        this.OWNER = OWNER;
    }

    public String getTOKEN() {
        return TOKEN;
    }

    public void setTOKEN(String TOKEN) {
        this.TOKEN = TOKEN;
    }

    public String getUNITS() {
        return UNITS;
    }

    public void setUNITS(String UNITS) {
        this.UNITS = UNITS;
    }

    public String getAMOUNT() {
        return AMOUNT;
    }

    public void setAMOUNT(String AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    public String getTAX() {
        return TAX;
    }

    public void setTAX(String TAX) {
        this.TAX = TAX;
    }

    public String getFIXED() {
        return FIXED;
    }

    public void setFIXED(String FIXED) {
        this.FIXED = FIXED;
    }

    public String getSTATCODE() {
        return STATCODE;
    }

    public void setSTATCODE(String STATCODE) {
        this.STATCODE = STATCODE;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }

    @Override
    public String toString() {
        return "LukuPaymentCallBack{" + "TRANSID=" + TRANSID + ", RECEPT=" + RECEPT + ", METER=" + METER + ", OWNER=" + OWNER + ", TOKEN=" + TOKEN + ", UNITS=" + UNITS + ", AMOUNT=" + AMOUNT + ", TAX=" + TAX + ", FIXED=" + FIXED + ", STATCODE=" + STATCODE + ", STATUS=" + STATUS + '}';
    }
    

}
