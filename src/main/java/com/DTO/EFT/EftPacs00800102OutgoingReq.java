/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.DTO.EFT;

import java.math.BigDecimal;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author melleji.mollel
 */
public class EftPacs00800102OutgoingReq {

    public String msgId;
    public XMLGregorianCalendar creDtTm;
    public String nbOfTxs;
    public BigDecimal ttlIntrBkSttlmAmt;
    public String ttlIntrBkSttlmCcy;
    public XMLGregorianCalendar intrBkSttlmDt;
    public String sttlmMtd;
    public String instdAgt;
    public String originalMsgNmId;
    public List<EftOutgoingBulkPaymentsReq> cdtTrfTxInf;

    public String getOriginalMsgNmId() {
        return originalMsgNmId;
    }

    public void setOriginalMsgNmId(String originalMsgNmId) {
        this.originalMsgNmId = originalMsgNmId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public XMLGregorianCalendar getCreDtTm() {
        return creDtTm;
    }

    public void setCreDtTm(XMLGregorianCalendar creDtTm) {
        this.creDtTm = creDtTm;
    }

    public String getNbOfTxs() {
        return nbOfTxs;
    }

    public void setNbOfTxs(String nbOfTxs) {
        this.nbOfTxs = nbOfTxs;
    }

    public BigDecimal getTtlIntrBkSttlmAmt() {
        return ttlIntrBkSttlmAmt;
    }

    public void setTtlIntrBkSttlmAmt(BigDecimal ttlIntrBkSttlmAmt) {
        this.ttlIntrBkSttlmAmt = ttlIntrBkSttlmAmt;
    }

    public String getTtlIntrBkSttlmCcy() {
        return ttlIntrBkSttlmCcy;
    }

    public void setTtlIntrBkSttlmCcy(String ttlIntrBkSttlmCcy) {
        this.ttlIntrBkSttlmCcy = ttlIntrBkSttlmCcy;
    }

    public XMLGregorianCalendar getIntrBkSttlmDt() {
        return intrBkSttlmDt;
    }

    public void setIntrBkSttlmDt(XMLGregorianCalendar intrBkSttlmDt) {
        this.intrBkSttlmDt = intrBkSttlmDt;
    }

    public String getSttlmMtd() {
        return sttlmMtd;
    }

    public void setSttlmMtd(String sttlmMtd) {
        this.sttlmMtd = sttlmMtd;
    }

    public String getInstdAgt() {
        return instdAgt;
    }

    public void setInstdAgt(String instdAgt) {
        this.instdAgt = instdAgt;
    }

    public List<EftOutgoingBulkPaymentsReq> getCdtTrfTxInf() {
        return cdtTrfTxInf;
    }

    public void setCdtTrfTxInf(List<EftOutgoingBulkPaymentsReq> cdtTrfTxInf) {
        this.cdtTrfTxInf = cdtTrfTxInf;
    }

    @Override
    public String toString() {
        return "EftPacs00800102OutgoingReq{" + "msgId=" + msgId + ", creDtTm=" + creDtTm + ", nbOfTxs=" + nbOfTxs + ", ttlIntrBkSttlmAmt=" + ttlIntrBkSttlmAmt + ", ttlIntrBkSttlmCcy=" + ttlIntrBkSttlmCcy + ", intrBkSttlmDt=" + intrBkSttlmDt + ", sttlmMtd=" + sttlmMtd + ", instdAgt=" + instdAgt + ", originalMsgNmId=" + originalMsgNmId + ", cdtTrfTxInf=" + cdtTrfTxInf + '}';
    }

}
