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
public class EftPacs00400102Req {

    public String messageId;
    public XMLGregorianCalendar createDateTime;
    public String nbOfTxs;
    public BigDecimal totalReturnedIntrBnkSettlmntAmt;
    public XMLGregorianCalendar interBankSettlmntDate;
    public String settlementMthd;
    public String clearingSystem;
    public String instructingAgent;
    public List<EftPacs004002TxnsInfoReq> txInfo;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public XMLGregorianCalendar getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(XMLGregorianCalendar createDateTime) {
        this.createDateTime = createDateTime;
    }

    public String getNbOfTxs() {
        return nbOfTxs;
    }

    public void setNbOfTxs(String nbOfTxs) {
        this.nbOfTxs = nbOfTxs;
    }

    public BigDecimal getTotalReturnedIntrBnkSettlmntAmt() {
        return totalReturnedIntrBnkSettlmntAmt;
    }

    public void setTotalReturnedIntrBnkSettlmntAmt(BigDecimal totalReturnedIntrBnkSettlmntAmt) {
        this.totalReturnedIntrBnkSettlmntAmt = totalReturnedIntrBnkSettlmntAmt;
    }

    public XMLGregorianCalendar getInterBankSettlmntDate() {
        return interBankSettlmntDate;
    }

    public void setInterBankSettlmntDate(XMLGregorianCalendar interBankSettlmntDate) {
        this.interBankSettlmntDate = interBankSettlmntDate;
    }

    public String getSettlementMthd() {
        return settlementMthd;
    }

    public void setSettlementMthd(String settlementMthd) {
        this.settlementMthd = settlementMthd;
    }

    public String getClearingSystem() {
        return clearingSystem;
    }

    public void setClearingSystem(String clearingSystem) {
        this.clearingSystem = clearingSystem;
    }

    public String getInstructingAgent() {
        return instructingAgent;
    }

    public void setInstructingAgent(String instructingAgent) {
        this.instructingAgent = instructingAgent;
    }

    public List<EftPacs004002TxnsInfoReq> getTxInfo() {
        return txInfo;
    }

    public void setTxInfo(List<EftPacs004002TxnsInfoReq> txInfo) {
        this.txInfo = txInfo;
    }

    @Override
    public String toString() {
        return "EftPacs00400102Req{" + "messageId=" + messageId + ", createDateTime=" + createDateTime + ", nbOfTxs=" + nbOfTxs + ", totalReturnedIntrBnkSettlmntAmt=" + totalReturnedIntrBnkSettlmntAmt + ", interBankSettlmntDate=" + interBankSettlmntDate + ", settlementMthd=" + settlementMthd + ", clearingSystem=" + clearingSystem + ", instructingAgent=" + instructingAgent + ", txInfo=" + txInfo + '}';
    }
    

}
