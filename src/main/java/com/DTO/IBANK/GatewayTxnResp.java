/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.IBANK;

import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author melleji.mollel Mar 16, 2021 8:48:57 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "SRV",
    "SRCSYS",
    "TRANSTYP",
    "TRANSID",
    "SRCACC",
    "PSPCODE",
    "DSTSP",
    "SPACC",
    "SPCODE",
    "SPCUSTREF",
    "SPCUSTNAME",
    "TRANSAMT",
    "CUSTPHONE",
    "RESULTCODE",
    "RESULTDESC"
})
@XmlRootElement(name = "RESULT")
public class GatewayTxnResp {

    @XmlElement(name = "SRV", required = true)
    protected String SRV;
    @XmlElement(name = "SRCSYS", required = true)
    protected String SRCSYS;
    @XmlElement(name = "TRANSTYP", required = true)
    protected String TRANSTYP;
    @XmlElement(name = "TRANSID", required = true)
    protected String TRANSID;
    @XmlElement(name = "SRCACC", required = true)
    protected String SRCACC;
    @XmlElement(name = "PSPCODE", required = true)
    protected String PSPCODE;
    @XmlElement(name = "DSTSP", required = true)
    protected String DSTSP;
    @XmlElement(name = "SPACC", required = true)
    protected String SPACC;
    @XmlElement(name = "SPCODE", required = true)
    protected String SPCODE;
    @XmlElement(name = "SPCUSTREF", required = true)
    protected String SPCUSTREF;
    @XmlElement(name = "SPCUSTNAME", required = true)
    protected String SPCUSTNAME;
    @XmlElement(name = "TRANSAMT", required = true)
    protected String TRANSAMT;
    @XmlElement(name = "CUSTPHONE", required = true)
    protected String CUSTPHONE;
    @XmlElement(name = "RESULTCODE", required = true)
    protected String RESULTCODE;
    @XmlElement(name = "RESULTDESC", required = true)
    protected String RESULTDESC;

    public String getSRV() {
        return SRV;
    }

    public void setSRV(String SRV) {
        this.SRV = SRV;
    }

    public String getSRCSYS() {
        return SRCSYS;
    }

    public void setSRCSYS(String SRCSYS) {
        this.SRCSYS = SRCSYS;
    }

    public String getTRANSTYP() {
        return TRANSTYP;
    }

    public void setTRANSTYP(String TRANSTYP) {
        this.TRANSTYP = TRANSTYP;
    }

    public String getTRANSID() {
        return TRANSID;
    }

    public void setTRANSID(String TRANSID) {
        this.TRANSID = TRANSID;
    }

    public String getSRCACC() {
        return SRCACC;
    }

    public void setSRCACC(String SRCACC) {
        this.SRCACC = SRCACC;
    }

    public String getPSPCODE() {
        return PSPCODE;
    }

    public void setPSPCODE(String PSPCODE) {
        this.PSPCODE = PSPCODE;
    }

    public String getDSTSP() {
        return DSTSP;
    }

    public void setDSTSP(String DSTSP) {
        this.DSTSP = DSTSP;
    }

    public String getSPACC() {
        return SPACC;
    }

    public void setSPACC(String SPACC) {
        this.SPACC = SPACC;
    }

    public String getSPCODE() {
        return SPCODE;
    }

    public void setSPCODE(String SPCODE) {
        this.SPCODE = SPCODE;
    }

    public String getSPCUSTREF() {
        return SPCUSTREF;
    }

    public void setSPCUSTREF(String SPCUSTREF) {
        this.SPCUSTREF = SPCUSTREF;
    }

    public String getSPCUSTNAME() {
        return SPCUSTNAME;
    }

    public void setSPCUSTNAME(String SPCUSTNAME) {
        this.SPCUSTNAME = SPCUSTNAME;
    }

    public String getTRANSAMT() {
        return TRANSAMT;
    }

    public void setTRANSAMT(String TRANSAMT) {
        this.TRANSAMT = TRANSAMT;
    }

    public String getCUSTPHONE() {
        return CUSTPHONE;
    }

    public void setCUSTPHONE(String CUSTPHONE) {
        this.CUSTPHONE = CUSTPHONE;
    }

    public String getRESULTCODE() {
        return RESULTCODE;
    }

    public void setRESULTCODE(String RESULTCODE) {
        this.RESULTCODE = RESULTCODE;
    }

    public String getRESULTDESC() {
        return RESULTDESC;
    }

    public void setRESULTDESC(String RESULTDESC) {
        this.RESULTDESC = RESULTDESC;
    }

    @Override
    public String toString() {
        return "GatewayTxnResp{" + "SRV=" + SRV + ", SRCSYS=" + SRCSYS + ", TRANSTYP=" + TRANSTYP + ", TRANSID=" + TRANSID + ", SRCACC=" + SRCACC + ", PSPCODE=" + PSPCODE + ", DSTSP=" + DSTSP + ", SPACC=" + SPACC + ", SPCODE=" + SPCODE + ", SPCUSTREF=" + SPCUSTREF + ", SPCUSTNAME=" + SPCUSTNAME + ", TRANSAMT=" + TRANSAMT + ", CUSTPHONE=" + CUSTPHONE + ", RESULTCODE=" + RESULTCODE + ", RESULTDESC=" + RESULTDESC + '}';
    }

    
}
