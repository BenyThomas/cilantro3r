/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO;

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
    "TRANSID",
    "RECEPT",
    "AMOUNT",
    "STASCODE",
    "STATUS"
})
@XmlRootElement(name = "MPESA")
public class MpesaCallback {

    @XmlElement(name = "TRANSID", required = true)
    public String TRANSID;
    @XmlElement(name = "RECEPT", required = true)
    public String RECEPT;
    @XmlElement(name = "AMOUNT", required = true)
    public String AMOUNT;
    @XmlElement(name = "STASCODE", required = true)
    public String STASCODE;
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

    public String getAMOUNT() {
        return AMOUNT;
    }

    public void setAMOUNT(String AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    public String getSTASCODE() {
        return STASCODE;
    }

    public void setSTASCODE(String STASCODE) {
        this.STASCODE = STASCODE;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }

    @Override
    public String toString() {
        return "MpesaCallback{" + "TRANSID=" + TRANSID + ", RECEPT=" + RECEPT + ", AMOUNT=" + AMOUNT + ", STASCODE=" + STASCODE + ", STATUS=" + STATUS + '}';
    }
}
